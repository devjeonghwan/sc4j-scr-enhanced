package org.fogofwarlabs.sc4j.screnhanced.script.game;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.scr.game.player.PlayerTable;
import org.fogofwarlabs.sc4j.screnhanced.script.input.ScriptInputCommand;
import org.fogofwarlabs.sc4j.screnhanced.script.input.ScriptInputEvent;
import org.fogofwarlabs.sc4j.screnhanced.script.input.ScriptInputState;
import org.jetbrains.annotations.NotNull;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * Defines {@code core.input}: the keys and mouse buttons whose changes the players send, and the input state of each player, the same on every
 * peer.
 * <p>
 * {@code track(keys, buttons, consume)} replaces the tracked keys (an array of Windows virtual-key codes from {@code 0} to {@code 255}), the
 * tracked mouse buttons (a mask: {@code 1} left, {@code 2} right, {@code 4} middle) and whether the tracked local presses reach the game
 * ({@code consume} keeps them from it); it counts as a write. {@code isKeyDown(player, key)}, {@code isButtonDown(player, button)} (button
 * {@code 0} left, {@code 1} right, {@code 2} middle), {@code hasMousePosition(player)}, {@code getMouseX(player)} and
 * {@code getMouseY(player)} ({@code -1} while unknown) read the input state of a player that the player's input commands changed.
 * <p>
 * The values of an input event are its {@code frame}, {@code player}, {@code key} or {@code button}, {@code modifiers}, and
 * {@code positioned} with the mouse position {@code x} and {@code y} in map pixels.
 */
final class ScriptInputCore {
    /**
     * Creates the values of an input event.
     *
     * @param context the current context
     * @param global  the global scope
     * @param event   the input event
     * @param frame   the frame the event's command ran in
     * @return the values
     */
    static @NotNull Scriptable createEventValues(@NotNull Context context, @NotNull Scriptable global, @NotNull ScriptInputEvent event, int frame) {
        ScriptInputCommand command = event.getCommand();
        Scriptable values = context.newObject(global);
        String codeName = command.getKind() == ScriptInputCommand.Kind.MOUSE ? "button" : "key";

        ScriptableObject.putProperty(values, "frame", (double) frame);
        ScriptableObject.putProperty(values, "player", (double) event.getPlayer());
        ScriptableObject.putProperty(values, codeName, (double) command.getCode());
        ScriptableObject.putProperty(values, "modifiers", (double) command.getModifiers());
        ScriptableObject.putProperty(values, "positioned", event.hasMousePosition());
        ScriptableObject.putProperty(values, "x", (double) event.getMouseX());
        ScriptableObject.putProperty(values, "y", (double) event.getMouseY());

        return values;
    }

    /**
     * Reads an argument that is an array of integers.
     *
     * @throws EcmaError if the argument is missing, not an array or has an element that is not an integer
     */
    private static int @NotNull [] readIntegerArray(@NotNull Object @NotNull [] arguments, int index) {
        if (index >= arguments.length || !(arguments[index] instanceof NativeArray array)) {
            throw ScriptRuntime.typeError(Exceptions.format("Argument {0} is not an array.", index + 1));
        }

        int[] integers = new int[(int) array.getLength()];

        for (int element = 0; element < integers.length; element++) {
            double value = array.get(element, array) instanceof Number number ? number.doubleValue() : Double.NaN;

            if (!(value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) || value != StrictMath.floor(value)) {
                throw ScriptRuntime.typeError(Exceptions.format("Element {0} of argument {1} is not an integer.", element, index + 1));
            }

            integers[element] = (int) value;
        }

        return integers;
    }

    private static int readPlayerId(@NotNull Object @NotNull [] arguments) {
        return ScriptCoreUtils.requireId("player", ScriptCoreUtils.readInteger(arguments, 0), PlayerTable.PLAYER_SLOTS);
    }


    private final @NotNull ScriptGame scriptGame;

    /**
     * Creates a {@link ScriptInputCore} instance.
     *
     * @param scriptGame the game binding
     */
    ScriptInputCore(@NotNull ScriptGame scriptGame) {
        this.scriptGame = scriptGame;
    }

    /**
     * Defines the functions of {@code core.input}.
     *
     * @param namespace the namespace object
     * @param global    the global scope
     */
    void define(@NotNull Scriptable namespace, @NotNull Scriptable global) {
        ScriptInputState state = this.scriptGame.getInputState();

        ScriptCoreUtils.defineFunction(global, namespace, "track", 3, (context, scope, thisObject, arguments) -> {
            this.scriptGame.checkWritable();

            int[] keys = readIntegerArray(arguments, 0);
            int buttons = ScriptCoreUtils.readInteger(arguments, 1);
            boolean consume = ScriptCoreUtils.readBoolean(arguments, 2);

            for (int key : keys) {
                ScriptCoreUtils.requireId("key", key, ScriptInputCommand.KEY_COUNT);
            }

            if ((buttons & ~ScriptInputState.ALL_BUTTONS) != 0) {
                throw ScriptRuntime.rangeError(Exceptions.format("The button mask {0} is not from 0 to {1}.", buttons, ScriptInputState.ALL_BUTTONS));
            }

            state.track(keys, buttons, consume);

            return Context.getUndefinedValue();
        });
        ScriptCoreUtils.defineFunction(global, namespace, "isKeyDown", 2, (context, scope, thisObject, arguments) -> {
            int player = readPlayerId(arguments);
            int key = ScriptCoreUtils.requireId("key", ScriptCoreUtils.readInteger(arguments, 1), ScriptInputCommand.KEY_COUNT);

            return state.isKeyDown(player, key);
        });
        ScriptCoreUtils.defineFunction(global, namespace, "isButtonDown", 2, (context, scope, thisObject, arguments) -> {
            int player = readPlayerId(arguments);
            int button = ScriptCoreUtils.requireId("button", ScriptCoreUtils.readInteger(arguments, 1), ScriptInputCommand.BUTTON_COUNT);

            return state.isButtonDown(player, button);
        });
        ScriptCoreUtils.defineFunction(global, namespace, "hasMousePosition", 1,
                                       (context, scope, thisObject, arguments) -> state.hasMousePosition(readPlayerId(arguments)));
        ScriptCoreUtils.defineFunction(global, namespace, "getMouseX", 1, (context, scope, thisObject, arguments) -> {
            int player = readPlayerId(arguments);

            return (double) (state.hasMousePosition(player) ? state.getMouseX(player) : ScriptCoreUtils.NO_ID);
        });
        ScriptCoreUtils.defineFunction(global, namespace, "getMouseY", 1, (context, scope, thisObject, arguments) -> {
            int player = readPlayerId(arguments);

            return (double) (state.hasMousePosition(player) ? state.getMouseY(player) : ScriptCoreUtils.NO_ID);
        });
    }
}
