package org.fogofwarlabs.sc4j.screnhanced.script.runtime;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.log.LogLevel;
import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.screnhanced.script.ScriptPackage;
import org.jetbrains.annotations.NotNull;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.TopLevel;

/**
 * Runs the scripts of a script package for one game.
 * <p>
 * The scripts run in Rhino's interpreter with deterministic globals: {@code Math} (computed with {@link StrictMath}), {@code random} (the
 * script RNG, seeded from the game seed and separate from the game's RNG) and per module {@code log}, {@code require}, {@code module} and
 * {@code exports}. No Java class, no {@code eval}, no function constructor from strings, no {@code Date} and no timer is available.
 * {@link #start()} loads the manifest's main script, which loads the others through {@code require}, and calls the {@link ScriptHook#START}
 * hooks; {@link #runFrame(int)} calls the {@link ScriptHook#FRAME} hooks. A hook runs in every module that has it, in load order.
 * <p>
 * Each call of {@link #start()} and {@link #runFrame(int)} has an instruction budget. A call that exceeds it, or overflows the Java stack,
 * stops the script: the runtime calls no hook for the rest of the game. An uncaught script error ends only the hook call (or the module
 * loading) it happened in. Every such event is written to the log output once, with the module name.
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

    /**
     * Creates a {@link ScriptRuntime} instance with the {@link #INSTRUCTION_BUDGET} that writes to the SC:R Enhanced log.
     *
     * @param scriptPackage the script package
     * @param seed          the seed of the script RNG, derived from the game seed
     * @return the created {@link ScriptRuntime} instance
     */
    public static @NotNull ScriptRuntime create(@NotNull ScriptPackage scriptPackage, long seed) {
        return create(scriptPackage, seed, INSTRUCTION_BUDGET, ScriptRuntime::writeToLogger);
    }

    /**
     * Creates a {@link ScriptRuntime} instance.
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
        if (instructionBudget <= 0) {
            throw Exceptions.newIllegalArgumentException("The instruction budget {0} is not positive.", instructionBudget);
        }

        ScriptContextFactory contextFactory = new ScriptContextFactory(instructionBudget);
        TopLevel global;

        try (Context context = contextFactory.enterOwnContext()) {
            global = ScriptGlobalUtils.createGlobal(context, new ScriptRandom(seed));
        }

        return new ScriptRuntime(scriptPackage.getManifest().getMain(), logOutput, contextFactory,
                                 new ScriptModuleLoader(scriptPackage.getScripts(), global, logOutput));
    }

    private static void writeToLogger(@NotNull LogLevel level, @NotNull String moduleName, @NotNull String message) {
        LOGGER.log(level, "{0}: {1}", moduleName, message);
    }


    private final @NotNull String mainName;
    private final @NotNull ScriptLogOutput logOutput;

    private final @NotNull ScriptContextFactory contextFactory;
    private final @NotNull ScriptModuleLoader moduleLoader;

    private boolean started;
    private boolean stopped;
    private boolean disposed;

    /**
     * Creates a {@link ScriptRuntime} instance.
     *
     * @param mainName       the name of the script that runs first
     * @param logOutput      the output of the runtime's warnings and errors
     * @param contextFactory the factory of the runtime's contexts
     * @param moduleLoader   the module loader
     */
    private ScriptRuntime(@NotNull String mainName, @NotNull ScriptLogOutput logOutput, @NotNull ScriptContextFactory contextFactory,
                          @NotNull ScriptModuleLoader moduleLoader) {
        this.mainName  = mainName;
        this.logOutput = logOutput;

        this.contextFactory = contextFactory;
        this.moduleLoader   = moduleLoader;

        this.started  = false;
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
            this.contextFactory.resetInstructionCount();

            if (this.runCall(context, this.mainName, LOAD_CALL_NAME, () -> this.moduleLoader.load(context, this.mainName))) {
                this.callHook(context, ScriptHook.START);
            }
        }
    }

    /**
     * Calls the {@link ScriptHook#FRAME} hooks with the frame number, within one instruction budget; does nothing once the script stopped.
     *
     * @param frame the frame number
     * @throws IllegalStateException if the runtime has not started or is disposed
     */
    public void runFrame(int frame) {
        this.checkNotDisposed();

        if (!this.started) {
            throw Exceptions.newIllegalStateException("The script runtime has not started.");
        }

        if (!this.hasHook(ScriptHook.FRAME)) {
            return;
        }

        try (Context context = this.contextFactory.enterOwnContext()) {
            this.contextFactory.resetInstructionCount();
            this.callHook(context, ScriptHook.FRAME, frame);
        }
    }

    /**
     * Calls a hook in every module that has it, in load order, until the script stops. Modules that load during the calls join from the
     * next call of the hook.
     */
    private void callHook(@NotNull Context context, @NotNull ScriptHook hook, @NotNull Object @NotNull ... arguments) {
        for (ScriptModule module : this.moduleLoader.getModules()) {
            Function function = module.getHook(hook);

            if (function == null) {
                continue;
            }

            Runnable call = () -> function.call(context, module.getScope(), module.getScope(), arguments.clone());

            if (!this.runCall(context, module.getName(), hook.getFunctionName(), call)) {
                return;
            }
        }
    }

    /**
     * Runs one call into the script and then the promise jobs it queued. An uncaught script error ends the call and is logged; an exceeded
     * instruction budget or a Java stack overflow stops the script.
     *
     * @param context    the current context
     * @param moduleName the name of the module the call belongs to
     * @param callName   the name of the call for the log
     * @param call       the call
     * @return {@code true} if the script has not stopped, {@code false} otherwise
     */
    private boolean runCall(@NotNull Context context, @NotNull String moduleName, @NotNull String callName, @NotNull Runnable call) {
        this.runGuarded(moduleName, callName, call);

        return this.runGuarded(moduleName, callName, context::processMicrotasks);
    }

    /**
     * Runs an action that calls into the script and handles what the script throws.
     *
     * @return {@code true} if the script has not stopped, {@code false} otherwise
     */
    private boolean runGuarded(@NotNull String moduleName, @NotNull String callName, @NotNull Runnable action) {
        if (this.stopped) {
            return false;
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
        } catch (RuntimeException exception) {
            this.logOutput.write(LogLevel.ERROR, moduleName, Exceptions.format("{0} failed: {1}", callName, exception));
        }

        return !this.stopped;
    }

    private void stop(@NotNull String moduleName, @NotNull String message) {
        this.stopped = true;

        this.logOutput.write(LogLevel.ERROR, moduleName, message);
    }

    private void checkNotDisposed() {
        if (this.disposed) {
            throw Exceptions.newIllegalStateException("The script runtime is disposed.");
        }
    }

    /**
     * Disposes resources.
     * <p>
     * The runtime forgets its modules; later calls of {@link #start()} and {@link #runFrame(int)} fail.
     */
    public void dispose() {
        this.disposed = true;
        this.stopped  = true;

        this.moduleLoader.clear();
    }
}
