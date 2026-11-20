package org.fogofwarlabs.sc4j.screnhanced.script.runtime;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.log.LogLevel;
import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrModCommandSubmitter;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.SimulationDecision;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.SimulationEvent;
import org.fogofwarlabs.sc4j.scr.game.state.Game;
import org.fogofwarlabs.sc4j.screnhanced.graphics.ImageReplacements;
import org.fogofwarlabs.sc4j.screnhanced.script.ScriptPackage;
import org.fogofwarlabs.sc4j.screnhanced.script.ScriptResources;
import org.fogofwarlabs.sc4j.screnhanced.script.game.ScriptGame;
import org.fogofwarlabs.sc4j.screnhanced.script.input.ScriptLocalInput;
import org.fogofwarlabs.sc4j.screnhanced.script.sync.ScriptChannelCommands;
import org.fogofwarlabs.sc4j.screnhanced.script.sync.ScriptChannelMessage;
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
 * runs when the runtime is created. Channels are created only until the main script has loaded. {@link #runFrame(int)} then first delivers
 * the queued events and channel messages ({@link #queueEvent}, {@link #queueModCommand}) in the order they happened, then calls the due timers
 * of {@code game.every}, one call each, then the hooks; an event that happens during these calls waits for the next frame. A channel message
 * goes to its channel's handler with the sending player's slot id and the decoded value. {@link #decide} calls the decision hooks at once
 * with the game read-only and copies what a hook decided back into the decision; {@link #setReadOnly(boolean)} makes the functions that
 * change the game state throw. All of this is shared code, which runs the same on every peer.
 * <p>
 * {@link #callLocalHook} calls a local hook at once, on the local machine only, with the game read-only and local: no timer, no random number,
 * no new module. The values a script sends wait in the game's channel outbox until {@link #submitCommands} hands them to the local player's
 * submitter.
 * <p>
 * Each call of {@link #start()}, {@link #runFrame(int)}, {@link #decide} and {@link #callLocalHook} has an instruction budget; a decision that
 * a running call causes, as when a hook issues an order, shares the budget of that call. A shared call that exceeds it, or overflows the Java
 * stack, stops the script: the runtime calls no hook for the rest of the game. An uncaught script error ends only the hook call (or the module
 * loading) it happened in; a decision hook that fails leaves the decision as it was. A local call that fails in any of these ways ends only
 * that call and never stops the script. Every such event is written to the log output once, with the module name.
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
    private static final @NotNull String CHANNEL_CALL_NAME = "Channel handler";

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
     * One queued event, a {@link SimulationEvent} with its hook or a {@link ScriptChannelMessage} without one, and the frame it happened in.
     */
    private static final class QueuedEvent {
        private final @Nullable ScriptHook hook;
        private final @NotNull Object event;
        private final int frame;

        private QueuedEvent(@Nullable ScriptHook hook, @NotNull Object event, int frame) {
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
        ScriptRandom random = new ScriptRandom(seed);
        TopLevel global;
        ScriptGame scriptGame = null;

        try (Context context = contextFactory.enterOwnContext()) {
            global = ScriptGlobalUtils.createGlobal(context, random);

            if (game != null) {
                scriptGame = ScriptGame.install(context, global, game, resources);
            }
        }

        return new ScriptRuntime(scriptPackage.getManifest().getMain(), logOutput, contextFactory, random,
                                 new ScriptModuleLoader(scriptPackage.getScripts(), global, logOutput), scriptGame);
    }

    private static void writeToLogger(@NotNull LogLevel level, @NotNull String moduleName, @NotNull String message) {
        LOGGER.log(level, "{0}: {1}", moduleName, message);
    }


    private final @NotNull String mainName;
    private final @NotNull ScriptLogOutput logOutput;

    private final @NotNull ScriptContextFactory contextFactory;
    private final @NotNull ScriptRandom random;
    private final @NotNull ScriptModuleLoader moduleLoader;
    private final @Nullable ScriptGame scriptGame;

    private @NotNull List<QueuedEvent> queuedEvents;
    private @NotNull List<QueuedEvent> deliveredEvents;
    private final @NotNull ScriptChannelCommands channelCommands;

    private boolean started;
    private boolean loaded;
    private boolean running;
    private boolean local;
    private boolean stopped;
    private boolean disposed;

    /**
     * Creates a {@link ScriptRuntime} instance.
     *
     * @param mainName       the name of the script that runs first
     * @param logOutput      the output of the runtime's warnings and errors
     * @param contextFactory the factory of the runtime's contexts
     * @param random         the script RNG of the global scope
     * @param moduleLoader   the module loader
     * @param scriptGame     the game API, or {@code null} without a game
     */
    private ScriptRuntime(@NotNull String mainName, @NotNull ScriptLogOutput logOutput, @NotNull ScriptContextFactory contextFactory,
                          @NotNull ScriptRandom random, @NotNull ScriptModuleLoader moduleLoader, @Nullable ScriptGame scriptGame) {
        this.mainName  = mainName;
        this.logOutput = logOutput;

        this.contextFactory = contextFactory;
        this.random         = random;
        this.moduleLoader   = moduleLoader;
        this.scriptGame     = scriptGame;

        this.queuedEvents    = new ArrayList<>();
        this.deliveredEvents = new ArrayList<>();
        this.channelCommands = new ScriptChannelCommands();

        this.started  = false;
        this.loaded   = false;
        this.running  = false;
        this.local    = false;
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
     * Loads the main script, ends the creation of channels, then calls the {@link ScriptHook#START} hooks, within one instruction budget.
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

                if (this.scriptGame != null) {
                    this.scriptGame.closeChannelCreation();
                }

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

        if (hook != null && !this.stopped && (!this.loaded || this.moduleLoader.hasHook(hook))) {
            this.queuedEvents.add(new QueuedEvent(hook, event, frame));
        }
    }

    /**
     * Reads a mod command of a player: a channel command adds to the player's message ({@link ScriptChannelCommands}), and the message it
     * completes is queued, to go to its channel's handler at the next {@link #runFrame(int)} in the order of all queued events. A message is
     * not queued once the script stopped.
     *
     * @param player the slot id of the issuing player
     * @param data   the command data
     * @param frame  the frame the command ran in
     * @throws IllegalStateException if the runtime has no game or is disposed
     */
    public void queueModCommand(int player, byte @NotNull [] data, int frame) {
        this.checkNotDisposed();
        this.requireScriptGame();

        ScriptChannelMessage message = this.channelCommands.read(player, data);

        if (message != null && !this.stopped) {
            this.queuedEvents.add(new QueuedEvent(null, message, frame));
        }
    }

    /**
     * Calls a local hook in every module that has it, in load order, with one event object for the input, the game read-only and local: no
     * timer, random number or new module. Does nothing before {@link #start()}, once the script stopped, while another call runs, or when no
     * module has the hook.
     *
     * @param input the local input
     * @return {@code true} if a hook consumed the input, {@code false} otherwise
     * @throws IllegalStateException if the runtime has no game or is disposed
     */
    public boolean callLocalHook(@NotNull ScriptLocalInput input) {
        this.checkNotDisposed();

        ScriptGame game = this.requireScriptGame();
        ScriptHook hook = input.getHook();

        if (!this.started || this.running || !this.hasHook(hook)) {
            return false;
        }

        boolean readOnly = game.isReadOnly();

        try (Context context = this.contextFactory.enterOwnContext()) {
            this.enterCall();
            this.setLocal(game, true);

            try {
                Scriptable values = game.createLocalValues(context, input);
                Scriptable event = game.createLocalEvent(context, hook.getFunctionName(), values);

                this.callHook(context, hook, event);

                return game.isConsumed(values);
            } finally {
                this.setLocal(game, false);
                game.setReadOnly(readOnly);

                this.running = false;
            }
        }
    }

    private void setLocal(@NotNull ScriptGame game, boolean local) {
        this.local = local;

        game.setReadOnly(local);
        game.setLocal(local);
        this.random.setLocked(local);
        this.moduleLoader.setLocal(local);
    }

    /**
     * Hands the channel commands that wait in the game's outbox to the local player's submitter, in order until the outgoing turn has no room;
     * the rest waits for the next call. Without a submitter, as in a replay or for an observer, the waiting commands are dropped.
     *
     * @param submitter the local player's mod command submitter, or {@code null} if the game has none
     * @throws IllegalStateException if the runtime has no game or is disposed
     */
    public void submitCommands(@Nullable ScrModCommandSubmitter submitter) {
        this.checkNotDisposed();
        this.requireScriptGame().getChannelOutbox().submit(submitter);
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
     * Delivers the queued events in order: a channel message goes to its channel's handler, an event to its hook with its event object. The
     * events queued during the calls wait for the next frame.
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
                if (queued.event instanceof ScriptChannelMessage message) {
                    if (this.callChannel(context, game, message) == CallResult.STOPPED) {
                        return false;
                    }

                    continue;
                }

                ScriptHook hook = queued.hook;

                if (hook == null || !this.moduleLoader.hasHook(hook)) {
                    continue;
                }

                String hookName = hook.getFunctionName();
                SimulationEvent event = (SimulationEvent) queued.event;
                CallResult result = this.runGuarded(this.mainName, hookName,
                                                    () -> argument[0] = game.createEvent(context, hookName, event, queued.frame));

                if (result == CallResult.STOPPED || result == CallResult.COMPLETED && !this.callHook(context, hook, argument[0])) {
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
     * Calls the handler of a channel message with the sending player's slot id and the decoded value; a message for no channel is logged and
     * dropped, and a value that does not decode fails the call.
     *
     * @return the result of the call
     */
    private @NotNull CallResult callChannel(@NotNull Context context, @NotNull ScriptGame game, @NotNull ScriptChannelMessage message) {
        Function handler = game.findChannelHandler(message.getChannel());

        if (handler == null) {
            this.logOutput.write(LogLevel.WARNING, this.mainName,
                                 Exceptions.format("A message of player {0} for the channel {1}, which does not exist, is dropped.",
                                                   message.getPlayer(), message.getChannel()));

            return CallResult.COMPLETED;
        }

        Scriptable scope = ScriptableObject.getTopLevelScope(handler);
        Runnable call = () -> handler.call(context, scope, scope, new Object[]{(double) message.getPlayer(),
                                                                                game.decodeChannelValue(context, message.getValue())});

        return this.runCall(context, this.findModuleName(scope), CHANNEL_CALL_NAME, call, true);
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
            if (this.local) {
                this.logOutput.write(LogLevel.ERROR, moduleName, Exceptions.format("{0} ran more than the budget of instructions.", callName));

                return CallResult.FAILED;
            }

            this.stop(moduleName, Exceptions.format("{0} ran more than the budget of instructions; the script stopped.", callName));
        } catch (StackOverflowError error) {
            if (this.local) {
                this.logOutput.write(LogLevel.ERROR, moduleName, Exceptions.format("{0} overflowed the stack.", callName));

                return CallResult.FAILED;
            }

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
     * The runtime forgets its modules, events, timers and waiting channel commands; later calls of {@link #start()}, {@link #runFrame(int)},
     * {@link #queueEvent}, {@link #queueModCommand}, {@link #decide}, {@link #callLocalHook} and {@link #submitCommands} fail.
     */
    public void dispose() {
        this.disposed = true;
        this.stopped  = true;

        this.moduleLoader.clear();
        this.queuedEvents.clear();

        if (this.scriptGame != null) {
            this.scriptGame.clearTimers();
            this.scriptGame.getChannelOutbox().clear();
        }
    }
}
