package org.fogofwarlabs.sc4j.screnhanced.script.input;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.scr.game.player.PlayerTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * The input of a script game in its simulation: the keys and mouse buttons the script tracks, and per player the keys and buttons down and the
 * last mouse position, which the players' input commands ({@link ScriptInputCommand}) change in the same order on every peer.
 * <p>
 * A press counts when its key or button is tracked, a release when its key or button is down, so every release the script receives follows a
 * press. A command with a position sets the player's mouse position. The display side reads the tracking to choose the local input it sends.
 */
public final class ScriptInputState {
    /**
     * The mask of every mouse button.
     */
    public static final int ALL_BUTTONS = (1 << ScriptInputCommand.BUTTON_COUNT) - 1;


    private static void checkPlayer(int player) {
        if (player < 0 || player >= PlayerTable.PLAYER_SLOTS) {
            throw Exceptions.newIllegalArgumentException("The player {0} is not from 0 to {1}.", player, PlayerTable.PLAYER_SLOTS - 1);
        }
    }

    private static void checkCode(int code, int count) {
        if (code < 0 || code >= count) {
            throw Exceptions.newIllegalArgumentException("The input code {0} is not from 0 to {1}.", code, count - 1);
        }
    }


    private final boolean @NotNull [] trackedKeys;
    private int trackedButtons;
    private boolean consuming;

    private final boolean @NotNull [] @NotNull [] pressedKeys;
    private final int @NotNull [] pressedButtons;
    private final boolean @NotNull [] mousePositionKnown;
    private final int @NotNull [] mouseX;
    private final int @NotNull [] mouseY;

    /**
     * Creates a {@link ScriptInputState} instance that tracks nothing, with no key or button down and no mouse position.
     */
    public ScriptInputState() {
        this.trackedKeys    = new boolean[ScriptInputCommand.KEY_COUNT];
        this.trackedButtons = 0;
        this.consuming      = false;

        this.pressedKeys        = new boolean[PlayerTable.PLAYER_SLOTS][ScriptInputCommand.KEY_COUNT];
        this.pressedButtons     = new int[PlayerTable.PLAYER_SLOTS];
        this.mousePositionKnown = new boolean[PlayerTable.PLAYER_SLOTS];
        this.mouseX             = new int[PlayerTable.PLAYER_SLOTS];
        this.mouseY             = new int[PlayerTable.PLAYER_SLOTS];
    }

    /**
     * Checks whether a key is tracked.
     *
     * @param virtualKey the Windows virtual-key code
     * @return {@code true} if the key is tracked, {@code false} otherwise or for a code outside {@code 0..0xFF}
     */
    public boolean isKeyTracked(int virtualKey) {
        return virtualKey >= 0 && virtualKey < ScriptInputCommand.KEY_COUNT && this.trackedKeys[virtualKey];
    }

    /**
     * Checks whether a mouse button is tracked.
     *
     * @param button the button: {@code 0} left, {@code 1} right, {@code 2} middle
     * @return {@code true} if the button is tracked, {@code false} otherwise or for no button
     */
    public boolean isButtonTracked(int button) {
        return button >= 0 && button < ScriptInputCommand.BUTTON_COUNT && (this.trackedButtons & 1 << button) != 0;
    }

    /**
     * @return {@code true} if a mouse button is tracked, so that key commands carry the mouse position, {@code false} otherwise
     */
    public boolean isMouseTracked() {
        return this.trackedButtons != 0;
    }

    /**
     * @return {@code true} if the local presses of the tracked keys and buttons reach neither the game nor later plugins, {@code false} otherwise
     */
    public boolean isConsuming() {
        return this.consuming;
    }

    /**
     * Checks whether a player's key is down.
     *
     * @param player     the player slot id
     * @param virtualKey the Windows virtual-key code
     * @return {@code true} if the key is down, {@code false} otherwise
     * @throws IllegalArgumentException if the player or the key is out of range
     */
    public boolean isKeyDown(int player, int virtualKey) {
        checkPlayer(player);
        checkCode(virtualKey, ScriptInputCommand.KEY_COUNT);

        return this.pressedKeys[player][virtualKey];
    }

    /**
     * Checks whether a player's mouse button is down.
     *
     * @param player the player slot id
     * @param button the button: {@code 0} left, {@code 1} right, {@code 2} middle
     * @return {@code true} if the button is down, {@code false} otherwise
     * @throws IllegalArgumentException if the player or the button is out of range
     */
    public boolean isButtonDown(int player, int button) {
        checkPlayer(player);
        checkCode(button, ScriptInputCommand.BUTTON_COUNT);

        return (this.pressedButtons[player] & 1 << button) != 0;
    }

    /**
     * Checks whether a player's mouse position is known: a command of the player carried one.
     *
     * @param player the player slot id
     * @return {@code true} if the mouse position is known, {@code false} otherwise
     * @throws IllegalArgumentException if the player is out of range
     */
    public boolean hasMousePosition(int player) {
        checkPlayer(player);

        return this.mousePositionKnown[player];
    }

    /**
     * Returns the map X of a player's last mouse position.
     *
     * @param player the player slot id
     * @return the map X in pixels, {@code 0} if unknown
     * @throws IllegalArgumentException if the player is out of range
     */
    public int getMouseX(int player) {
        checkPlayer(player);

        return this.mouseX[player];
    }

    /**
     * Returns the map Y of a player's last mouse position.
     *
     * @param player the player slot id
     * @return the map Y in pixels, {@code 0} if unknown
     * @throws IllegalArgumentException if the player is out of range
     */
    public int getMouseY(int player) {
        checkPlayer(player);

        return this.mouseY[player];
    }


    /**
     * Replaces the tracked keys and mouse buttons and whether their local presses are consumed. The keys and buttons down stay down.
     *
     * @param virtualKeys the Windows virtual-key codes of the tracked keys
     * @param buttons     the mask of the tracked buttons: bit {@code 0} left, {@code 1} right, {@code 2} middle
     * @param consume     {@code true} to consume the local presses of the tracked keys and buttons
     * @throws IllegalArgumentException if a key code is outside {@code 0..0xFF} or the mask has other bits than {@link #ALL_BUTTONS}
     */
    public void track(int @NotNull [] virtualKeys, int buttons, boolean consume) {
        for (int virtualKey : virtualKeys) {
            checkCode(virtualKey, ScriptInputCommand.KEY_COUNT);
        }

        if ((buttons & ~ALL_BUTTONS) != 0) {
            throw Exceptions.newIllegalArgumentException("The button mask {0} has other bits than {1}.", buttons, ALL_BUTTONS);
        }

        Arrays.fill(this.trackedKeys, false);

        for (int virtualKey : virtualKeys) {
            this.trackedKeys[virtualKey] = true;
        }

        this.trackedButtons = buttons;
        this.consuming      = consume;
    }

    /**
     * Applies an input command of a player: a press of a tracked key or button sets it down, a release of a key or button that is down sets
     * it up, and either sets the player's mouse position when the command has one. Any other command changes nothing.
     *
     * @param player  the player slot id
     * @param command the command
     * @return the event of the change, with the command's position, else for a key command the player's last mouse position while the mouse
     *         is tracked; {@code null} if the command changed nothing or the player is out of range
     */
    public @Nullable ScriptInputEvent apply(int player, @NotNull ScriptInputCommand command) {
        if (player < 0 || player >= PlayerTable.PLAYER_SLOTS) {
            return null;
        }

        int code = command.getCode();
        boolean mouse = command.getKind() == ScriptInputCommand.Kind.MOUSE;
        boolean down = mouse ? (this.pressedButtons[player] & 1 << code) != 0 : this.pressedKeys[player][code];

        if (command.isPressed() ? !(mouse ? this.isButtonTracked(code) : this.isKeyTracked(code)) : !down) {
            return null;
        }

        if (mouse) {
            this.pressedButtons[player] = command.isPressed() ? this.pressedButtons[player] | 1 << code : this.pressedButtons[player] & ~(1 << code);
        } else {
            this.pressedKeys[player][code] = command.isPressed();
        }

        if (command.isPositioned()) {
            this.mousePositionKnown[player] = true;
            this.mouseX[player]             = command.getX();
            this.mouseY[player]             = command.getY();
        }

        boolean positioned = command.isPositioned() || this.isMouseTracked() && this.mousePositionKnown[player];

        return new ScriptInputEvent(player, command, positioned, positioned ? this.mouseX[player] : 0, positioned ? this.mouseY[player] : 0);
    }
}
