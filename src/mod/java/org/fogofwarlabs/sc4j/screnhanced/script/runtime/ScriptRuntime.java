package org.fogofwarlabs.sc4j.screnhanced.script.runtime;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.log.LogLevel;
import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.SimulationDecision;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.SimulationEvent;
import org.fogofwarlabs.sc4j.scr.game.state.Game;
import org.fogofwarlabs.sc4j.screnhanced.graphics.ImageReplacements;
import org.fogofwarlabs.sc4j.screnhanced.script.ScriptPackage;
import org.fogofwarlabs.sc4j.screnhanced.script.ScriptResources;
import org.fogofwarlabs.sc4j.screnhanced.script.game.ScriptGame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.TopLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs the scripts of a script package for one game.
 * <p>
 * The scripts run in Rhino's interpreter with deterministic globals: {@code Math} (computed with {@link StrictMath}), {@code random} (the
 * script RNG, seeded from the game seed and separate from the game's RNG) and per module {@code log}, {@code require}, {@code module} and
 * {@code exports}. No Java class, no {@code eval}, no function constructor from strings, no {@code Date} and no timer is available.
 * {@link #start()} loads the manifest's main script, which loads the others through {@code require}, and calls the {@link ScriptHook#START}
 * hooks; {@link #runFrame(int)} calls the {@link ScriptHook#FRAME} hooks. A hook runs in every module that has it, in load order.
 * <p>
 * A runtime created with a game also has the game API ({@link ScriptGame}): the global {@code core} and the globals of the prelude, which
 * runs when the runtime is created. {@link #runFrame(int)} then first calls the event hooks of the queued events ({@link #queueEvent}) in
 * the order they happened, then the due timers of {@code game.every}, one call each, then the hooks; an event that happens during these
 * calls waits for the next frame. {@link #decide} calls the decision hooks at once with the game read-only and copies what a hook decided
 * back into the decision; {@link #setReadOnly(boolean)} makes the functions that change the game state throw.
 * <p>
 * Each call of {@link #start()}, {@link #runFrame(int)} and {@link #decide} has an instruction budget; a decision that a running call
 * causes, as when a hook issues an order, shares the budget of that call. A call that exceeds it, or overflows the Java stack, stops the
 * script: the runtime calls no hook for the rest of the game. An uncaught script error ends only the hook call (or the module loading) it
 * happened in; a decision hook that fails leaves the decision as it was. Every such event is written to the log output once, with the
 * module name.
 * <p>
 * Every method runs on the caller's thread; the runtime starts no thread. It is not thread-safe.
 */
public final class ScriptRuntime {
    /**
     * The instruction budget of one call that SC:R Enhanced gives a script.
     */
    public static final int INSTRUCTION_BUDGET = 5_000_000;

    private static final @NotNull Logger LOGGER = Logger.of(ScriptRuntime.class);
    private static final @NotNull String LOAD_CALL_NAME = "Loading";
    private static final @NotNull String TIMER_CALL_NAME = "Timer";

    /**
     * The result of one call into the script.
     */
    private enum CallResult {
        /**
         * The call returned.
         */
        COMPLETED,
        /**
         * The call threw a script error, which was logged.
         */
        FAILED,
        /**
         * The call stopped the script.
         */
        STOPPED
    }

    /**
     * One queued event with its hook and the frame it happened in.
     */
    private static final class QueuedEvent {
        private final @NotNull ScriptHook hook;
        private final @NotNull SimulationEvent event;
        private final int frame;

        private QueuedEvent(@NotNull ScriptHook hook, @NotNull SimulationEvent event, int frame) {
            this.hook  = hook;
            this.event = event;
            this.frame = frame;
        }
    }


    /**
     * Creates a {@link ScriptRuntime} instance with the {@link #INSTRUCTION_BUDGET} that writes to the SC:R Enhanced log.
     *
     * @param scriptPackage the script package
     * @param seed          the seed of the script RNG, derived from the game seed
     * @return the created {@link ScriptRuntime} instance
     */
    public static @NotNull ScriptRuntime create(@NotNull ScriptPackage scriptPackage, long seed) {
        return create(scriptPackage, null, ScriptResources.EMPTY, seed, INSTRUCTION_BUDGET, ScriptRuntime::writeToLogger);
    }

    /**
     * Creates a {@link ScriptRuntime} instance with the game API of a game and the {@link #INSTRUCTION_BUDGET}, which writes to the SC:R
     * Enhanced log.
     *
     * @param scriptPackage the script package
     * @param game          the game
     * @param resources     the resource file of the map
     * @param seed          the seed of the script RNG, derived from the game seed
     * @return the created {@link ScriptRuntime} instance
     */
    public static @NotNull ScriptRuntime create(@NotNull ScriptPackage scriptPackage, @NotNull Game game, @NotNull ScriptResources resources,
                                                long seed) {
        return create(scriptPackage, game, resources, seed, INSTRUCTION_BUDGET, ScriptRuntime::writeToLogger);
    }

    /**
     * Creates a {@link ScriptRuntime} instance without a game.
     *
     * @param scriptPackage     the script package
     * @param seed              the seed of the script RNG, derived from the game seed
     * @param instructionBudget the instruction budget of one call
     * @param logOutput         the output of the scripts' {@code log} and of the runtime's warnings and errors
     * @return the created {@link ScriptRuntime} instance
     * @throws IllegalArgumentException if the instruction budget is not positive
     */
    public static @NotNull ScriptRuntime create(@NotNull ScriptPackage scriptPackage, long seed, int instructionBudget,
                                                @NotNull ScriptLogOutput logOutput) {
        return create(scriptPackage, null, ScriptResources.EMPTY, seed, instructionBudget, logOutput);
    }

    /**
     * Creates a {@link ScriptRuntime} instance, with the game API of a game when one is given; the prelude of the game API runs before the
     * first call and outside its instruction budget.
     *
     * @param scriptPackage     the script package
     * @param game              the game, or {@code null} for a runtime without the game API
     * @param resources         the resource file of the map, which the game API reads
     * @param seed              the seed of the script RNG, derived from the game seed
     * @param instructionBudget the instruction budget of one call
     * @param logOutput         the output of the scripts' {@code log} and of the runtime's warnings and errors
     * @return the created {@link ScriptRuntime} instance
     * @throws IllegalArgumentException if the instruction budget is not positive
     */
    public static @NotNull ScriptRuntime create(@NotNull ScriptPackage scriptPackage, @Nullable Game game, @NotNull ScriptResources resources,
                                                long seed, int instructionBudget, @NotNull ScriptLogOutput logOutput) {
        if (instructionBudget <= 0) {
            throw Exceptions.newIllegalArgumentException("The instruction budget {0} is not positive.", instructionBudget);
        }

        ScriptContextFactory contextFactory = new ScriptContextFactory(instructionBudget);
        TopLevel global;
        ScriptGame scriptGame = null;

        try (Context context = contextFactory.enterOwnContext()) {
            global = ScriptGlobalUtils.createGlobal(context, new ScriptRandom(seed));

            if (game != null) {
                scriptGame = ScriptGame.install(context, global, game, resources);
            }
        }

        return new ScriptRuntime(scriptPackage.getManifest().getMain(), logOutput, contextFactory,
                                 new ScriptModuleLoader(scriptPackage.getScripts(), global, logOutput), scriptGame);
    }

    private static void writeToLogger(@NotNull LogLevel level, @NotNull String moduleName, @NotNull String message) {
        LOGGER.log(level, "{0}: {1}", moduleName, message);
    }


    private final @NotNull String mainName;
    private final @NotNull ScriptLogOutput logOutput;

    private final @NotNull ScriptContextFactory contextFactory;
    private final @NotNull ScriptModuleLoader moduleLoader;
    private final @Nullable ScriptGame scriptGame;

    private @NotNull List<QueuedEvent> queuedEvents;
    private @NotNull List<QueuedEvent> deliveredEvents;

    private boolean started;
    private boolean loaded;
    private boolean running;
    private boolean stopped;
    private boolean disposed;

    /**
     * Creates a {@link ScriptRuntime} instance.
     *
     * @param mainName       the name of the script that runs first
     * @param logOutput      the output of the runtime's warnings and errors
     * @param contextFactory the factory of the runtime's contexts
     * @param moduleLoader   the module loader
     * @param scriptGame     the game API, or {@code null} without a game
     */
    private ScriptRuntime(@NotNull String mainName, @NotNull ScriptLogOutput logOutput, @NotNull ScriptContextFactory contextFactory,
                          @NotNull ScriptModuleLoader moduleLoader, @Nullable ScriptGame scriptGame) {
        this.mainName  = mainName;
        this.logOutput = logOutput;

        this.contextFactory = contextFactory;
        this.moduleLoader   = moduleLoader;
        this.scriptGame     = scriptGame;

        this.queuedEvents    = new ArrayList<>();
        this.deliveredEvents = new ArrayList<>();

        this.started  = false;
        this.loaded   = false;
        this.running  = false;
        this.stopped  = false;
        this.disposed = false;
    }

    /**
     * @return {@code true} if the script stopped and no hook runs any more, {@code false} otherwise
     */
    public boolean isStopped() {
        return this.stopped;
    }

    /**
     * @return {@code true} if the game API's functions that change the game state throw, {@code false} otherwise
     * @throws IllegalStateException if the runtime has no game
     */
    public boolean isReadOnly() {
        return this.requireScriptGame().isReadOnly();
    }

    /**
     * Sets whether the game API's functions that change the game state throw a {@code TypeError}, as they must while a decision runs.
     *
     * @param readOnly {@code true} to make the game read-only
     * @throws IllegalStateException if the runtime has no game
     */
    public void setReadOnly(boolean readOnly) {
        this.requireScriptGame().setReadOnly(readOnly);
    }

    /**
     * @return the image graphics the script replaces, the same instance for the whole game
     * @throws IllegalStateException if the runtime has no game
     */
    public @NotNull ImageReplacements getImageReplacements() {
        return this.requireScriptGame().getImageReplacements();
    }

    /**
     * Checks whether a loaded module has a hook.
     *
     * @param hook the hook
     * @return {@code true} if the script has not stopped and a loaded module has the hook, {@code false} otherwise
     */
    public boolean hasHook(@NotNull ScriptHook hook) {
        return !this.stopped && this.moduleLoader.hasHook(hook);
    }


    /**
     * Loads the main script and calls the {@link ScriptHook#START} hooks, within one instruction budget.
     *
     * @throws IllegalStateException if the runtime has started already or is disposed
     */
    public void start() {
        this.checkNotDisposed();

        if (this.started) {
            throw Exceptions.newIllegalStateException("The script runtime has started already.");
        }

        this.started = true;

        try (Context context = this.contextFactory.enterOwnContext()) {
            boolean nested = this.enterCall();

            try {
                CallResult result = this.runCall(context, this.mainName, LOAD_CALL_NAME, () -> this.moduleLoader.load(context, this.mainName), true);

                this.loaded = true;

                if (result != CallResult.STOPPED) {
                    this.callHook(context, ScriptHook.START);
                }
            } finally {
                this.running = nested;
            }
        }
    }

    /**
     * Queues an event for its hook ({@link ScriptHook#findByKind}), to be called at the next {@link #runFrame(int)}. An event is not queued
     * once the script stopped, when no hook receives it, or when the main script has loaded and no module has its hook.
     *
     * @param event the event, whose values were captured when it happened
     * @param frame the frame it happened in
     * @throws IllegalStateException if the runtime has no game or is disposed
     */
    public void queueEvent(@NotNull SimulationEvent event, int frame) {
        this.checkNotDisposed();
        this.requireScriptGame();

        ScriptHook hook = ScriptHook.findByKind(event.getClass());

        if (hook == null || this.stopped || this.loaded && !this.moduleLoader.hasHook(hook)) {
            return;
        }

        this.queuedEvents.add(new QueuedEvent(hook, event, frame));
    }

    /**
     * Calls the decision hook of a decision ({@link ScriptHook#findByKind}) in every module that has it, in load order, with the game
     * read-only: each module receives a new decision object with the values the previous modules decided, and the values of a hook that
     * returns are copied back into the decision. Does nothing before {@link #start()}, once the script stopped, or when no module has the
     * hook.
     *
     * @param decision the decision
     * @throws IllegalStateException if the runtime has no game or is disposed
     */
    public void decide(@NotNull SimulationDecision decision) {
        this.checkNotDisposed();

        ScriptGame game = this.requireScriptGame();
        ScriptHook hook = ScriptHook.findByKind(decision.getClass());

        if (hook == null || !this.started || !this.hasHook(hook)) {
            return;
        }

        boolean readOnly = game.isReadOnly();

        try (Context context = this.contextFactory.enterOwnContext()) {
            boolean nested = this.enterCall();

            game.setReadOnly(true);

            try {
                for (ScriptModule module : this.moduleLoader.getModules()) {
                    Function function = module.getHook(hook);

                    if (function == null) {
                        continue;
                    }

                    Runnable call = () -> {
                        Scriptable object = game.createDecision(context, hook.getFunctionName(), decision);

                        function.call(context, module.getScope(), module.getScope(), new Object[]{object});
                        game.applyDecision(object, decision);
                    };

                    if (this.runCall(context, module.getName(), hook.getFunctionName(), call, !nested) == CallResult.STOPPED) {
                        return;
                    }
                }
            } finally {
                game.setReadOnly(readOnly);

                this.running = nested;
            }
        }
    }

    /**
     * Calls the hooks of the queued events, the due timers of the frame, then the {@link ScriptHook#FRAME} hooks with the frame number,
     * within one instruction budget; does nothing once the script stopped.
     *
     * @param frame the frame number
     * @throws IllegalStateException if the runtime has not started or is disposed
     */
    public void runFrame(int frame) {
        this.checkNotDisposed();

        if (!this.started) {
            throw Exceptions.newIllegalStateException("The script runtime has not started.");
        }

        boolean timers = this.scriptGame != null && this.scriptGame.hasTimers();

        if (this.stopped || this.queuedEvents.isEmpty() && !timers && !this.hasHook(ScriptHook.FRAME)) {
            this.queuedEvents.clear();

            return;
        }

        try (Context context = this.contextFactory.enterOwnContext()) {
            boolean nested = this.enterCall();

            try {
                if (!this.callEvents(context) || timers && !this.callTimers(context, frame)) {
                    return;
                }

                this.callHook(context, ScriptHook.FRAME, frame);
            } finally {
                this.running = nested;
            }
        }
    }

    /**
     * Starts a call into the script: a call while no other call runs gets a new instruction budget.
     *
     * @return {@code true} if another call is running, {@code false} otherwise
     */
    private boolean enterCall() {
        boolean nested = this.running;

        if (!nested) {
            this.contextFactory.resetInstructionCount();
        }

        this.running = true;

        return nested;
    }

    /**
     * Calls the hook of every queued event in order with its event object; the events queued during the calls wait for the next frame.
     *
     * @return {@code true} if the script has not stopped, {@code false} otherwise
     */
    private boolean callEvents(@NotNull Context context) {
        if (this.queuedEvents.isEmpty()) {
            return true;
        }

        List<QueuedEvent> events = this.queuedEvents;
        ScriptGame game = this.requireScriptGame();
        Object[] argument = new Object[1];

        this.queuedEvents    = this.deliveredEvents;
        this.deliveredEvents = events;

        try {
            for (QueuedEvent queued : events) {
                if (!this.moduleLoader.hasHook(queued.hook)) {
                    continue;
                }

                String hookName = queued.hook.getFunctionName();
                CallResult result = this.runGuarded(this.mainName, hookName,
                                                    () -> argument[0] = game.createEvent(context, hookName, queued.event, queued.frame));

                if (result == CallResult.STOPPED || result == CallResult.COMPLETED && !this.callHook(context, queued.hook, argument[0])) {
                    return false;
                }
            }
        } finally {
            events.clear();
        }

        return true;
    }

    /**
     * Calls the due timers of a frame one by one until none is due or the script stops. A timer that a call cancels or adds is seen by the
     * next pick.
     *
     * @return {@code true} if the script has not stopped, {@code false} otherwise
     */
    private boolean callTimers(@NotNull Context context, int frame) {
        ScriptGame game = this.requireScriptGame();
        Function timer = game.pollDueTimer(frame);

        while (timer != null) {
            Scriptable scope = ScriptableObject.getTopLevelScope(timer);
            Function function = timer;
            Runnable call = () -> function.call(context, scope, scope, new Object[0]);

            if (this.runCall(context, this.findModuleName(scope), TIMER_CALL_NAME, call, true) == CallResult.STOPPED) {
                return false;
            }

            timer = game.pollDueTimer(frame);
        }

        return true;
    }

    /**
     * Finds the module of a top-level scope.
     *
     * @return the module name, or the main script's name if no module has the scope
     */
    private @NotNull String findModuleName(@NotNull Scriptable scope) {
        for (ScriptModule module : this.moduleLoader.getModules()) {
            if (module.getScope() == scope) {
                return module.getName();
            }
        }

        return this.mainName;
    }

    /**
     * Calls a hook in every module that has it, in load order, until the script stops. Modules that load during the calls join from the
     * next call of the hook.
     *
     * @return {@code true} if the script has not stopped, {@code false} otherwise
     */
    private boolean callHook(@NotNull Context context, @NotNull ScriptHook hook, @NotNull Object @NotNull ... arguments) {
        for (ScriptModule module : this.moduleLoader.getModules()) {
            Function function = module.getHook(hook);

            if (function == null) {
                continue;
            }

            Runnable call = () -> function.call(context, module.getScope(), module.getScope(), arguments.clone());

            if (this.runCall(context, module.getName(), hook.getFunctionName(), call, true) == CallResult.STOPPED) {
                return false;
            }
        }

        return true;
    }

    /**
     * Runs one call into the script and then, unless the call runs inside another call, the promise jobs it queued. An uncaught script
     * error ends the call and is logged; an exceeded instruction budget or a Java stack overflow stops the script.
     *
     * @param context    the current context
     * @param moduleName the name of the module the call belongs to
     * @param callName   the name of the call for the log
     * @param call       the call
     * @param microtasks {@code true} to run the promise jobs after the call, {@code false} to leave them to the running call
     * @return the result of the call, {@link CallResult#STOPPED} if the promise jobs stopped the script
     */
    private @NotNull CallResult runCall(@NotNull Context context, @NotNull String moduleName, @NotNull String callName, @NotNull Runnable call,
                                        boolean microtasks) {
        CallResult result = this.runGuarded(moduleName, callName, call);

        if (result == CallResult.STOPPED || !microtasks) {
            return result;
        }

        return this.runGuarded(moduleName, callName, context::processMicrotasks) == CallResult.STOPPED ? CallResult.STOPPED : result;
    }

    /**
     * Runs an action that calls into the script and handles what the script throws.
     *
     * @return the result of the action
     */
    private @NotNull CallResult runGuarded(@NotNull String moduleName, @NotNull String callName, @NotNull Runnable action) {
        if (this.stopped) {
            return CallResult.STOPPED;
        }

        try {
            action.run();
        } catch (ScriptContextFactory.InstructionBudgetError error) {
            this.stop(moduleName, Exceptions.format("{0} ran more than the budget of instructions; the script stopped.", callName));
        } catch (StackOverflowError error) {
            this.stop(moduleName, Exceptions.format("{0} overflowed the stack; the script stopped.", callName));
        } catch (RhinoException exception) {
            String location = exception.sourceName() != null ? Exceptions.format(" ({0}:{1})", exception.sourceName(), exception.lineNumber()) : "";

            this.logOutput.write(LogLevel.ERROR, moduleName, Exceptions.format("{0} failed: {1}{2}", callName, exception.details(), location));

            return CallResult.FAILED;
        } catch (RuntimeException exception) {
            this.logOutput.write(LogLevel.ERROR, moduleName, Exceptions.format("{0} failed: {1}", callName, exception));

            return CallResult.FAILED;
        }

        return this.stopped ? CallResult.STOPPED : CallResult.COMPLETED;
    }

    /**
     * Stops the script and logs why, once: a call that a stopped nested call returns to stops again at its next instruction report.
     */
    private void stop(@NotNull String moduleName, @NotNull String message) {
        if (this.stopped) {
            return;
        }

        this.stopped = true;

        this.queuedEvents.clear();
        this.logOutput.write(LogLevel.ERROR, moduleName, message);
    }

    private @NotNull ScriptGame requireScriptGame() {
        if (this.scriptGame == null) {
            throw Exceptions.newIllegalStateException("The script runtime has no game.");
        }

        return this.scriptGame;
    }

    private void checkNotDisposed() {
        if (this.disposed) {
            throw Exceptions.newIllegalStateException("The script runtime is disposed.");
        }
    }

    /**
     * Disposes resources.
     * <p>
     * The runtime forgets its modules, events and timers; later calls of {@link #start()}, {@link #runFrame(int)}, {@link #queueEvent} and
     * {@link #decide} fail.
     */
    public void dispose() {
        this.disposed = true;
        this.stopped  = true;

        this.moduleLoader.clear();
        this.queuedEvents.clear();

        if (this.scriptGame != null) {
            this.scriptGame.clearTimers();
        }
    }
}
