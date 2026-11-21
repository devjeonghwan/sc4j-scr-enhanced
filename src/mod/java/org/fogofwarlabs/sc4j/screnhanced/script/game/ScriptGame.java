package org.fogofwarlabs.sc4j.screnhanced.script.game;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.SimulationDecision;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.SimulationEvent;
import org.fogofwarlabs.sc4j.scr.game.frame.GameSpeed;
import org.fogofwarlabs.sc4j.scr.game.map.MapStringSection;
import org.fogofwarlabs.sc4j.scr.game.player.PlayerTable;
import org.fogofwarlabs.sc4j.scr.game.state.Game;
import org.fogofwarlabs.sc4j.scr.game.trigger.TriggerContext;
import org.fogofwarlabs.sc4j.scr.game.trigger.TriggerModifiers;
import org.fogofwarlabs.sc4j.scr.game.unit.UnitPool;
import org.fogofwarlabs.sc4j.screnhanced.graphics.ImageReplacements;
import org.fogofwarlabs.sc4j.screnhanced.script.ScriptResources;
import org.fogofwarlabs.sc4j.screnhanced.script.input.ScriptLocalInput;
import org.fogofwarlabs.sc4j.screnhanced.script.runtime.ScriptHook;
import org.fogofwarlabs.sc4j.screnhanced.script.sync.ScriptChannelOutbox;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Binds a script runtime to one SC:R {@link Game}: the {@code core} global, the prelude, the timers, the channels and the read-only and local
 * modes.
 * <p>
 * {@code core} is the lower layer of the game API. Its namespaces {@code game}, {@code player}, {@code unit}, {@code location},
 * {@code terrain}, {@code leaderboard}, {@code unitType}, {@code weaponType}, {@code flingyType}, {@code spriteType}, {@code imageType},
 * {@code upgradeType}, {@code techType}, {@code orderType}, {@code dat}, {@code resources}, {@code graphics}, {@code sync} and {@code local}
 * take and return ids and raw values (hit points, shields and energy in {@code 1/256} units, supply in half units, DAT values as the tables
 * hold them); a write takes the routine that the trigger action or the order system runs for it. A text that a routine shows as a map string
 * takes a registered map string ({@link #findStringIndex}). The graphics replacements ({@link ScriptGraphicsCore}) change only what is drawn
 * and count as writes. The channels ({@link ScriptSyncCore}) carry values from one peer to every peer. The local information
 * ({@link ScriptLocalCore}) is readable only in local code. The prelude ({@value #PRELUDE_RESOURCE}) runs once in a function scope of its own
 * and defines the upper layer as read-only globals over {@code core}.
 * <p>
 * A timer of {@code core.game.every} runs first at the frame its interval after the current game frame, then every interval; the timers of a
 * frame run in the order of their due frames, then of their registration. While the game is read-only, every {@code core} function that
 * changes the game state throws a {@code TypeError}. While the game is local, as in a local hook, it is also read-only, and adding or
 * cancelling a timer throws a {@code TypeError} as well.
 * <p>
 * The prelude also makes the event and decision objects of the hooks ({@link #createEvent}, {@link #createLocalEvent},
 * {@link #createDecision}); the values a decision hook left in a decision object go back into the decision with {@link #applyDecision}.
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
    /**
     * The value of a local event that its {@code consume()} sets.
     */
    private static final @NotNull String CONSUMED_PROPERTY = "consumed";
    private static final int CORE_ATTRIBUTES = ScriptableObject.READONLY | ScriptableObject.DONTENUM | ScriptableObject.PERMANENT;
    /**
     * The number of switches of the trigger state.
     */
    private static final int SWITCH_COUNT = 256;
    /**
     * The string index of no map string, which an empty text takes.
     */
    private static final int NO_STRING_INDEX = 0;

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
     * @param resources the resources of the map
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
        new ScriptTerrainCore(scriptGame).define(ScriptCoreUtils.createNamespace(context, global, core, "terrain"), global);
        new ScriptLeaderboardCore(scriptGame).define(ScriptCoreUtils.createNamespace(context, global, core, "leaderboard"), global);
        ScriptTypeCore.define(context, scriptGame, core, global);
        new ScriptDatCore(scriptGame).define(context, ScriptCoreUtils.createNamespace(context, global, core, "dat"), global);
        graphicsCore.defineResources(ScriptCoreUtils.createNamespace(context, global, core, "resources"), global);
        graphicsCore.defineGraphics(ScriptCoreUtils.createNamespace(context, global, core, "graphics"), global);
        scriptGame.sync.define(ScriptCoreUtils.createNamespace(context, global, core, "sync"));
        new ScriptLocalCore(scriptGame).define(ScriptCoreUtils.createNamespace(context, global, core, "local"), global);

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
    private final @NotNull ScriptSyncCore sync;

    private final @NotNull List<Timer> timers;
    private int nextTimerId;

    private final @NotNull Map<String, Integer> registeredTexts;

    private boolean readOnly;
    private boolean local;

    /**
     * Creates a {@link ScriptGame} instance without timers, image replacements, channels and registered texts, neither read-only nor local,
     * whose prelude has not run.
     *
     * @param game   the game
     * @param global the global scope
     */
    private ScriptGame(@NotNull Game game, @NotNull Scriptable global) {
        this.game   = game;
        this.global = global;
        this.bridge = null;

        this.imageReplacements = new ImageReplacements(() -> game.getUnitSystem().getUnitPool());
        this.sync              = new ScriptSyncCore(global);

        this.timers      = new ArrayList<>();
        this.nextTimerId = 1;

        this.registeredTexts = new HashMap<>();

        this.readOnly = false;
        this.local    = false;
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
     * @return the channel commands that wait for the local player's outgoing turn ({@code core.sync}), the same instance for the whole game
     */
    public @NotNull ScriptChannelOutbox getChannelOutbox() {
        return this.sync.getOutbox();
    }

    /**
     * Finds the handler of a channel ({@code core.sync}).
     *
     * @param channel the channel id
     * @return the handler, or {@code null} if no channel has the id
     */
    public @Nullable Function findChannelHandler(int channel) {
        return this.sync.findHandler(channel);
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
     * @return {@code true} if local code runs, which may not add or cancel timers, {@code false} otherwise
     */
    public boolean isLocal() {
        return this.local;
    }

    /**
     * Sets whether local code runs: adding and cancelling a timer then throws a {@code TypeError}. The caller also makes the game read-only.
     *
     * @param local {@code true} while local code runs
     */
    public void setLocal(boolean local) {
        this.local = local;
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
     * Checks that local code runs, which alone reads the local information.
     *
     * @throws EcmaError if no local code runs
     */
    void checkLocal() {
        if (!this.local) {
            throw ScriptRuntime.typeError("The local information is readable only in local code, the local hooks.");
        }
    }

    /**
     * Finds the local player: the local player of the trigger evaluation in a live game when it is a playable player.
     *
     * @return the slot id of the local player, or {@code -1} in a replay, for an observer and in a game without trigger state
     */
    int findLocalPlayer() {
        TriggerContext context = this.game.getTriggerContext();

        if (this.game.isReplayPlayback() || context == null) {
            return ScriptCoreUtils.NO_ID;
        }

        int player = context.getLocalPlayer();

        return player >= 0 && player < PlayerTable.PLAYABLE_PLAYER_SLOTS ? player : ScriptCoreUtils.NO_ID;
    }

    /**
     * Checks that no local code runs.
     *
     * @throws EcmaError if local code runs
     */
    private void checkShared() {
        if (this.local) {
            throw ScriptRuntime.typeError("Local code cannot change the timers of the script.");
        }
    }

    /**
     * Ends the creation of channels ({@code core.sync}), once the modules have loaded.
     */
    public void closeChannelCreation() {
        this.sync.closeCreation();
    }

    /**
     * Decodes the value of a channel message into new script values.
     *
     * @param context the current context
     * @param value   the encoded value
     * @return the value
     * @throws IllegalArgumentException if the bytes are not one well-formed value
     */
    public @Nullable Object decodeChannelValue(@NotNull Context context, byte @NotNull [] value) {
        return this.sync.decodeValue(context, value);
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
     * Creates the values of a local event: {@code key} or {@code button} (the input's code), {@code modifiers}, {@code positioned} with the
     * map position {@code x} and {@code y}, {@code text}, and {@code consumed}, {@code false} until a hook consumes the event.
     *
     * @param context the current context
     * @param input   the local input
     * @return the values
     */
    public @NotNull Scriptable createLocalValues(@NotNull Context context, @NotNull ScriptLocalInput input) {
        Scriptable values = context.newObject(this.global);
        ScriptHook hook = input.getHook();
        String codeName = hook == ScriptHook.LOCAL_MOUSE_DOWN || hook == ScriptHook.LOCAL_MOUSE_UP ? "button" : "key";

        ScriptableObject.putProperty(values, codeName, (double) input.getCode());
        ScriptableObject.putProperty(values, "modifiers", (double) input.getModifiers());
        ScriptableObject.putProperty(values, "positioned", input.isPositioned());
        ScriptableObject.putProperty(values, "x", (double) input.getX());
        ScriptableObject.putProperty(values, "y", (double) input.getY());
        ScriptableObject.putProperty(values, "text", input.getText());
        ScriptableObject.putProperty(values, CONSUMED_PROPERTY, false);

        return values;
    }

    /**
     * Creates the script object of a local event: the prelude's event object of the hook over the values ({@link #createLocalValues}), whose
     * {@code consume()} marks the values consumed.
     *
     * @param context  the current context
     * @param hookName the name of the local hook that receives the event
     * @param values   the values of the event
     * @return the created event object
     */
    public @NotNull Scriptable createLocalEvent(@NotNull Context context, @NotNull String hookName, @NotNull Scriptable values) {
        return this.callBridge(context, CREATE_EVENT_FUNCTION, hookName, values);
    }

    /**
     * Checks whether a hook consumed a local event.
     *
     * @param values the values of the event ({@link #createLocalValues})
     * @return {@code true} if a hook called the event's {@code consume()}, {@code false} otherwise
     */
    public boolean isConsumed(@NotNull Scriptable values) {
        return Boolean.TRUE.equals(ScriptableObject.getProperty(values, CONSUMED_PROPERTY));
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
     * Finds the map string index of a text for the routines that show a map string: the empty text has no map string, any other text is
     * registered in the map strings ({@link MapStringSection#registerString}) as UTF-8, the encoding the map strings decode first.
     * <p>
     * An identical text registers once and keeps its index for the whole game, so a text set every frame does not grow the map strings. The
     * indices follow the order of the first registrations, the same on every peer.
     *
     * @param text the text
     * @return the map string index, {@code 0} for the empty text
     * @throws EcmaError if the text holds a NUL character, the game has no map strings or no map string index is left
     */
    int findStringIndex(@NotNull String text) {
        if (text.isEmpty()) {
            return NO_STRING_INDEX;
        }

        Integer registered = this.registeredTexts.get(text);

        if (registered != null) {
            return registered;
        }

        if (text.indexOf('\0') >= 0) {
            throw ScriptRuntime.rangeError("A map string text holds no NUL character.");
        }

        MapStringSection strings = this.game.getInterfaceLink().getMessageLines().getMapStringSection();

        if (strings == null) {
            throw ScriptRuntime.typeError("The game has no map strings.");
        }

        int stringIndex;

        try {
            stringIndex = strings.registerString(text.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalStateException exception) {
            throw ScriptRuntime.rangeError("No map string index is left for a new text.");
        }

        this.registeredTexts.put(text, stringIndex);

        return stringIndex;
    }

    /**
     * Defines {@code core.game}: {@code getFrame()}, {@code getMapTileWidth()}, {@code getMapTileHeight()}, {@code getTileset()},
     * {@code getFrameDuration()} (milliseconds per frame at the fastest speed), {@code print(text)} (all players, as Display Text Message shows a
     * text with forced display), {@code every(frames, fn)} (the timer id), {@code cancelTimer(id)} (both refused in local code) and
     * {@code isReadOnly()}.
     * <p>
     * The trigger state: {@code isSwitchSet(index)}, {@code setSwitch(index, set)} and {@code toggleSwitch(index)} (switches {@code 0} to
     * {@value #SWITCH_COUNT} minus one, as Set Switch sets, clears and toggles them); {@code getCountdownTimer()},
     * {@code setCountdownTimer(seconds)}, {@code addCountdownTimer(seconds)} and {@code subtractCountdownTimer(seconds)} (Set Countdown Timer,
     * kept from {@code 0} to {@code 359999}), {@code isCountdownTimerPaused()} and {@code setCountdownTimerPaused(paused)} (Pause Timer and
     * Unpause Timer); {@code getElapsedTime()} (the seconds the Elapsed Time condition compares). Countdown and elapsed time are game seconds
     * of the trigger clock, which advances one second every sixteen frames, not frames.
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
            byte[] text = ScriptCoreUtils.readString(arguments, 0).getBytes(StandardCharsets.UTF_8);

            this.game.getInterfaceLink().getMessageLines().writeTriggerText(text, true, 0);

            return Context.getUndefinedValue();
        });
        ScriptCoreUtils.defineFunction(global, namespace, "every", 2, (context, scope, thisObject, arguments) -> {
            this.checkShared();

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
            this.checkShared();

            int id = ScriptCoreUtils.readInteger(arguments, 0);

            return this.timers.removeIf(timer -> timer.id == id);
        });
        ScriptCoreUtils.defineFunction(global, namespace, "isReadOnly", 0, (context, scope, thisObject, arguments) -> this.readOnly);

        this.defineTriggerStateFunctions(namespace, global);
    }

    /**
     * Defines the functions of {@code core.game} over the switches, the countdown timer and the elapsed time of the trigger state.
     */
    private void defineTriggerStateFunctions(@NotNull Scriptable namespace, @NotNull Scriptable global) {
        ScriptCoreUtils.defineFunction(global, namespace, "isSwitchSet", 1, (context, scope, thisObject, arguments) -> {
            int index = ScriptCoreUtils.requireId("switch", ScriptCoreUtils.readInteger(arguments, 0), SWITCH_COUNT);

            return this.requireTriggerContext().isSwitchSet(index);
        });
        ScriptCoreUtils.defineFunction(global, namespace, "setSwitch", 2, (context, scope, thisObject, arguments) -> {
            this.checkWritable();

            int index = ScriptCoreUtils.requireId("switch", ScriptCoreUtils.readInteger(arguments, 0), SWITCH_COUNT);
            boolean set = ScriptCoreUtils.readBoolean(arguments, 1);

            this.requireTriggerContext().setSwitch(index, set);

            return Context.getUndefinedValue();
        });
        ScriptCoreUtils.defineFunction(global, namespace, "toggleSwitch", 1, (context, scope, thisObject, arguments) -> {
            this.checkWritable();

            int index = ScriptCoreUtils.requireId("switch", ScriptCoreUtils.readInteger(arguments, 0), SWITCH_COUNT);

            this.requireTriggerContext().toggleSwitch(index);

            return Context.getUndefinedValue();
        });

        ScriptCoreUtils.defineFunction(global, namespace, "getCountdownTimer", 0,
                                       (context, scope, thisObject, arguments) -> (double) this.requireTriggerContext().getCountdownTimer());
        this.defineCountdownWrite(namespace, global, "setCountdownTimer", TriggerModifiers.SET_TO);
        this.defineCountdownWrite(namespace, global, "addCountdownTimer", TriggerModifiers.ADD);
        this.defineCountdownWrite(namespace, global, "subtractCountdownTimer", TriggerModifiers.SUBTRACT);
        ScriptCoreUtils.defineFunction(global, namespace, "isCountdownTimerPaused", 0,
                                       (context, scope, thisObject, arguments) -> this.requireTriggerContext().isCountdownPaused());
        ScriptCoreUtils.defineFunction(global, namespace, "setCountdownTimerPaused", 1, (context, scope, thisObject, arguments) -> {
            this.checkWritable();

            boolean paused = ScriptCoreUtils.readBoolean(arguments, 0);

            this.requireTriggerContext().setCountdownPaused(paused);

            return Context.getUndefinedValue();
        });
        ScriptCoreUtils.defineFunction(global, namespace, "getElapsedTime", 0,
                                       (context, scope, thisObject, arguments) -> (double) this.requireTriggerContext().getElapsedClockTicks());
    }

    /**
     * Defines a write of the countdown timer that runs the Set Countdown Timer routine with a modifier.
     */
    private void defineCountdownWrite(@NotNull Scriptable namespace, @NotNull Scriptable global, @NotNull String name, int modifier) {
        ScriptCoreUtils.defineFunction(global, namespace, name, 1, (context, scope, thisObject, arguments) -> {
            this.checkWritable();

            int seconds = ScriptCoreUtils.readInteger(arguments, 0);

            this.requireTriggerContext().modifyCountdownTimer(modifier, seconds);

            return Context.getUndefinedValue();
        });
    }

    /**
     * Runs the prelude: a script whose value is a function, called with the global scope, {@code core} and the DAT tables of the type objects
     * ({@link ScriptDatTables#createPreludeTables}), which returns the object of the functions {@value #CREATE_EVENT_FUNCTION} and
     * {@value #CREATE_DECISION_FUNCTION}, kept outside the scripts' reach.
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

        Scriptable datTables = ScriptDatTables.createPreludeTables(context, global);

        if (!(prelude.call(context, global, global, new Object[]{global, core, datTables}) instanceof Scriptable functions)) {
            throw Exceptions.newIllegalStateException("The script game prelude {0} returned no functions.", PRELUDE_RESOURCE);
        }

        this.bridge = functions;
    }
}
