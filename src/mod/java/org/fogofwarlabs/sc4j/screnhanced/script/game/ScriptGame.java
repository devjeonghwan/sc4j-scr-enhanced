package org.fogofwarlabs.sc4j.screnhanced.script.game;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.SimulationDecision;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.SimulationEvent;
import org.fogofwarlabs.sc4j.scr.game.frame.GameSpeed;
import org.fogofwarlabs.sc4j.scr.game.state.Game;
import org.fogofwarlabs.sc4j.scr.game.trigger.TriggerContext;
import org.fogofwarlabs.sc4j.scr.game.unit.UnitPool;
import org.fogofwarlabs.sc4j.screnhanced.graphics.ImageReplacements;
import org.fogofwarlabs.sc4j.screnhanced.script.ScriptResources;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Binds a script runtime to one SC:R {@link Game}: the {@code core} global, the prelude, the timers and the read-only mode.
 * <p>
 * {@code core} is the lower layer of the game API. Its namespaces {@code game}, {@code player}, {@code unit}, {@code location},
 * {@code unitType}, {@code weaponType}, {@code upgradeType}, {@code techType}, {@code orderType}, {@code imageType}, {@code resources} and
 * {@code graphics} take and return ids and raw values (hit points, shields and energy in {@code 1/256} units, supply in half units); a write
 * takes the routine that the trigger action or the order system runs for it. The graphics replacements ({@link ScriptGraphicsCore}) change
 * only what is drawn and count as writes. The prelude ({@value #PRELUDE_RESOURCE}) runs once in a function scope of its own and defines the
 * upper layer as read-only globals over {@code core}.
 * <p>
 * A timer of {@code core.game.every} runs first at the frame its interval after the current game frame, then every interval; the timers of a
 * frame run in the order of their due frames, then of their registration. While the game is read-only, every {@code core} function that
 * changes the game state throws a {@code TypeError}.
 * <p>
 * The prelude also makes the event and decision objects of the hooks ({@link #createEvent}, {@link #createDecision}); the values a decision
 * hook left in a decision object go back into the decision with {@link #applyDecision}.
 */
public final class ScriptGame {
    /**
     * The name of the lower layer's global.
     */
    public static final @NotNull String CORE_NAME = "core";

    /**
     * The resource of the prelude, next to this class.
     */
    private static final @NotNull String PRELUDE_RESOURCE = "prelude.js";
    /**
     * The functions of the object the prelude returns: they make the event and the decision objects of a hook from their values.
     */
    private static final @NotNull String CREATE_EVENT_FUNCTION = "createEvent";
    private static final @NotNull String CREATE_DECISION_FUNCTION = "createDecision";
    private static final int CORE_ATTRIBUTES = ScriptableObject.READONLY | ScriptableObject.DONTENUM | ScriptableObject.PERMANENT;
    /**
     * The display time of each character of a printed text and the shortest display time, those of the trigger texts.
     */
    private static final int TEXT_CHARACTER_MILLISECONDS = 50;
    private static final int MINIMUM_TEXT_MILLISECONDS = 4000;

    /**
     * One timer of {@code core.game.every}.
     */
    private static final class Timer {
        private final int id;
        private final int interval;
        private final @NotNull Function function;

        private int dueFrame;

        private Timer(int id, int interval, @NotNull Function function, int dueFrame) {
            this.id       = id;
            this.interval = interval;
            this.function = function;

            this.dueFrame = dueFrame;
        }
    }


    /**
     * Creates the {@code core} global of a game, then runs the prelude.
     *
     * @param context   the current context
     * @param global    the global scope
     * @param game      the game
     * @param resources the resource file of the map
     * @return the created {@link ScriptGame} instance
     * @throws IllegalStateException if the prelude cannot be read or fails
     */
    public static @NotNull ScriptGame install(@NotNull Context context, @NotNull ScriptableObject global, @NotNull Game game,
                                              @NotNull ScriptResources resources) {
        ScriptGame scriptGame = new ScriptGame(game, global);
        ScriptableObject core = (ScriptableObject) context.newObject(global);
        ScriptUnitCore unitCore = new ScriptUnitCore(scriptGame);
        ScriptGraphicsCore graphicsCore = new ScriptGraphicsCore(scriptGame, unitCore, resources);

        scriptGame.defineGameFunctions(ScriptCoreUtils.createNamespace(context, global, core, "game"), global);
        new ScriptPlayerCore(scriptGame).define(ScriptCoreUtils.createNamespace(context, global, core, "player"), global);
        unitCore.define(ScriptCoreUtils.createNamespace(context, global, core, "unit"), global);
        new ScriptLocationCore(scriptGame).define(ScriptCoreUtils.createNamespace(context, global, core, "location"), global);
        ScriptTypeCore.define(context, scriptGame, core, global);
        graphicsCore.defineResources(ScriptCoreUtils.createNamespace(context, global, core, "resources"), global);
        graphicsCore.defineGraphics(ScriptCoreUtils.createNamespace(context, global, core, "graphics"), global);

        for (Object name : core.getAllIds()) {
            ((ScriptableObject) core.get((String) name, core)).preventExtensions();
        }

        core.preventExtensions();
        ScriptableObject.defineProperty(global, CORE_NAME, core, CORE_ATTRIBUTES);
        scriptGame.runPrelude(context, global, core);

        return scriptGame;
    }


    private final @NotNull Game game;
    private final @NotNull Scriptable global;
    private @Nullable Scriptable bridge;

    private final @NotNull ImageReplacements imageReplacements;

    private final @NotNull List<Timer> timers;
    private int nextTimerId;

    private boolean readOnly;

    /**
     * Creates a {@link ScriptGame} instance without timers and image replacements, not read-only, whose prelude has not run.
     *
     * @param game   the game
     * @param global the global scope
     */
    private ScriptGame(@NotNull Game game, @NotNull Scriptable global) {
        this.game   = game;
        this.global = global;
        this.bridge = null;

        this.imageReplacements = new ImageReplacements(() -> game.getUnitSystem().getUnitPool());

        this.timers      = new ArrayList<>();
        this.nextTimerId = 1;

        this.readOnly = false;
    }

    /**
     * @return the game
     */
    @NotNull Game getGame() {
        return this.game;
    }

    /**
     * @return the image graphics the script replaces ({@code core.graphics}), the same instance for the whole game
     */
    public @NotNull ImageReplacements getImageReplacements() {
        return this.imageReplacements;
    }

    /**
     * @return {@code true} if the {@code core} functions that change the game state throw, {@code false} otherwise
     */
    public boolean isReadOnly() {
        return this.readOnly;
    }

    /**
     * Sets whether the {@code core} functions that change the game state throw a {@code TypeError}.
     *
     * @param readOnly {@code true} to make the game read-only
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * @return {@code true} if a timer is registered, {@code false} otherwise
     */
    public boolean hasTimers() {
        return !this.timers.isEmpty();
    }

    /**
     * @return the instruction limit of the animation scripts that a write runs, that of the current frame
     * @throws IllegalStateException if the game has no frame
     */
    int getInstructionLimit() {
        return this.game.requireFrame().getInstructionLimit();
    }

    /**
     * @return the trigger context of the game
     * @throws EcmaError if the game has no trigger context
     */
    @NotNull TriggerContext requireTriggerContext() {
        TriggerContext context = this.game.getTriggerContext();

        if (context == null) {
            throw ScriptRuntime.typeError("The game has no trigger state.");
        }

        return context;
    }


    /**
     * Checks that the game is not read-only.
     *
     * @throws EcmaError if the game is read-only
     */
    void checkWritable() {
        if (this.readOnly) {
            throw ScriptRuntime.typeError("The game state cannot change here: the game is read-only.");
        }
    }

    /**
     * Takes the next due timer of a frame: the timer with the earliest due frame up to the frame, the earliest registered on a tie. The timer
     * becomes due again its interval after the frame.
     *
     * @param frame the frame number
     * @return the function of the timer, or {@code null} if no timer is due
     */
    public @Nullable Function pollDueTimer(int frame) {
        Timer due = null;

        for (Timer timer : this.timers) {
            if (timer.dueFrame <= frame && (due == null || timer.dueFrame < due.dueFrame)) {
                due = timer;
            }
        }

        if (due == null) {
            return null;
        }

        due.dueFrame = frame + due.interval;

        return due.function;
    }

    /**
     * Removes every timer.
     */
    public void clearTimers() {
        this.timers.clear();
    }

    /**
     * Creates the script object of an event: the prelude's event object of the hook, with the event's captured values.
     *
     * @param context  the current context
     * @param hookName the name of the hook that receives the event
     * @param event    the event
     * @param frame    the frame the event happened in
     * @return the created event object
     * @throws IllegalStateException if the game has no unit pool
     */
    public @NotNull Scriptable createEvent(@NotNull Context context, @NotNull String hookName, @NotNull SimulationEvent event, int frame) {
        Scriptable values = ScriptSimulationUtils.createEventValues(context, this.global, this.getUnitPool(), event, frame);

        return this.callBridge(context, CREATE_EVENT_FUNCTION, hookName, values);
    }

    /**
     * Creates the script object of a decision: the prelude's decision object of the hook, with the decision's current values.
     *
     * @param context  the current context
     * @param hookName the name of the hook that receives the decision
     * @param decision the decision
     * @return the created decision object
     * @throws IllegalStateException if the game has no unit pool
     */
    public @NotNull Scriptable createDecision(@NotNull Context context, @NotNull String hookName, @NotNull SimulationDecision decision) {
        Scriptable values = ScriptSimulationUtils.createDecisionValues(context, this.global, this.getUnitPool(), decision);

        return this.callBridge(context, CREATE_DECISION_FUNCTION, hookName, values);
    }

    /**
     * Copies the values a decision hook left in a decision object ({@link #createDecision}) back into the decision.
     *
     * @param object   the decision object
     * @param decision the decision
     * @throws IllegalStateException if the game has no unit pool
     */
    public void applyDecision(@NotNull Scriptable object, @NotNull SimulationDecision decision) {
        ScriptSimulationUtils.applyDecision(object, this.getUnitPool(), decision);
    }

    private @NotNull UnitPool getUnitPool() {
        return this.game.getUnitSystem().requireUnitPool();
    }

    /**
     * Calls a function of the object the prelude returned with a hook name and values.
     *
     * @return the object the function returned
     * @throws IllegalStateException if the prelude has not run or returned no such function
     */
    private @NotNull Scriptable callBridge(@NotNull Context context, @NotNull String functionName, @NotNull String hookName,
                                           @NotNull Scriptable values) {
        Scriptable prelude = this.bridge;

        if (prelude == null || !(ScriptableObject.getProperty(prelude, functionName) instanceof Function function)) {
            throw Exceptions.newIllegalStateException("The script game prelude has no function {0}.", functionName);
        }

        if (!(function.call(context, this.global, prelude, new Object[]{hookName, values}) instanceof Scriptable object)) {
            throw Exceptions.newIllegalStateException("The script game prelude made no object for {0}.", hookName);
        }

        return object;
    }

    /**
     * Writes a text as a trigger text, the way Display Text Message shows it with forced display.
     *
     * @param text the text
     */
    void printText(@NotNull String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        this.game.getInterfaceLink().getMessageLines().writeTriggerText(bytes, Math.max(bytes.length * TEXT_CHARACTER_MILLISECONDS,
                                                                                         MINIMUM_TEXT_MILLISECONDS));
    }

    /**
     * Defines {@code core.game}: {@code getFrame()}, {@code getMapTileWidth()}, {@code getMapTileHeight()}, {@code getTileset()},
     * {@code getFrameDuration()} (milliseconds per frame at the fastest speed), {@code print(text)} (all players), {@code every(frames, fn)}
     * (the timer id), {@code cancelTimer(id)} and {@code isReadOnly()}.
     */
    private void defineGameFunctions(@NotNull Scriptable namespace, @NotNull Scriptable global) {
        ScriptCoreUtils.defineFunction(global, namespace, "getFrame", 0,
                                       (context, scope, thisObject, arguments) -> (double) this.game.getCurrentFrame());
        ScriptCoreUtils.defineFunction(global, namespace, "getMapTileWidth", 0,
                                       (context, scope, thisObject, arguments) -> (double) this.game.getMap().getWidth());
        ScriptCoreUtils.defineFunction(global, namespace, "getMapTileHeight", 0,
                                       (context, scope, thisObject, arguments) -> (double) this.game.getMap().getHeight());
        ScriptCoreUtils.defineFunction(global, namespace, "getTileset", 0,
                                       (context, scope, thisObject, arguments) -> (double) this.game.getMap().getTileset());
        ScriptCoreUtils.defineFunction(global, namespace, "getFrameDuration", 0,
                                       (context, scope, thisObject, arguments) -> (double) GameSpeed.FASTEST_DURATION);
        ScriptCoreUtils.defineFunction(global, namespace, "print", 1, (context, scope, thisObject, arguments) -> {
            this.printText(ScriptCoreUtils.readString(arguments, 0));

            return Context.getUndefinedValue();
        });
        ScriptCoreUtils.defineFunction(global, namespace, "every", 2, (context, scope, thisObject, arguments) -> {
            int interval = ScriptCoreUtils.readInteger(arguments, 0);
            Function function = ScriptCoreUtils.readFunction(arguments, 1);

            if (interval < 1) {
                throw ScriptRuntime.rangeError(Exceptions.format("The timer interval {0} is not positive.", interval));
            }

            Timer timer = new Timer(this.nextTimerId++, interval, function, this.game.getCurrentFrame() + interval);

            this.timers.add(timer);

            return (double) timer.id;
        });
        ScriptCoreUtils.defineFunction(global, namespace, "cancelTimer", 1, (context, scope, thisObject, arguments) -> {
            int id = ScriptCoreUtils.readInteger(arguments, 0);

            return this.timers.removeIf(timer -> timer.id == id);
        });
        ScriptCoreUtils.defineFunction(global, namespace, "isReadOnly", 0, (context, scope, thisObject, arguments) -> this.readOnly);
    }

    /**
     * Runs the prelude: a script whose value is a function, called with the global scope and {@code core}, which returns the object of the
     * functions {@value #CREATE_EVENT_FUNCTION} and {@value #CREATE_DECISION_FUNCTION}, kept outside the scripts' reach.
     */
    private void runPrelude(@NotNull Context context, @NotNull ScriptableObject global, @NotNull Scriptable core) {
        String source;

        try (InputStream input = ScriptGame.class.getResourceAsStream(PRELUDE_RESOURCE)) {
            if (input == null) {
                throw Exceptions.newIllegalStateException("The script game prelude {0} is missing.", PRELUDE_RESOURCE);
            }

            source = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw Exceptions.newIllegalStateException(exception, "The script game prelude {0} cannot be read.", PRELUDE_RESOURCE);
        }

        Script script = context.compileString(source, PRELUDE_RESOURCE, 1, null);

        if (!(script.exec(context, global) instanceof Function prelude)) {
            throw Exceptions.newIllegalStateException("The script game prelude {0} is not a function.", PRELUDE_RESOURCE);
        }

        if (!(prelude.call(context, global, global, new Object[]{global, core}) instanceof Scriptable functions)) {
            throw Exceptions.newIllegalStateException("The script game prelude {0} returned no functions.", PRELUDE_RESOURCE);
        }

        this.bridge = functions;
    }
}
