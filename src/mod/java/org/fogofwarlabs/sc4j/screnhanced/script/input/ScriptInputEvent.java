package org.fogofwarlabs.sc4j.screnhanced.script.input;

import org.jetbrains.annotations.NotNull;

/**
 * A key or mouse button change of a player that the script receives ({@link ScriptInputState#apply}): the player's command with the
 * player's mouse position at the change.
 */
public final class ScriptInputEvent {
    private final int player;
    private final @NotNull ScriptInputCommand command;

    private final boolean positioned;
    private final int mouseX;
    private final int mouseY;

    /**
     * Creates a {@link ScriptInputEvent} instance.
     *
     * @param player     the player slot id
     * @param command    the command
     * @param positioned whether the mouse position is known
     * @param mouseX     the map X of the mouse in pixels
     * @param mouseY     the map Y of the mouse in pixels
     */
    ScriptInputEvent(int player, @NotNull ScriptInputCommand command, boolean positioned, int mouseX, int mouseY) {
        this.player  = player;
        this.command = command;

        this.positioned = positioned;
        this.mouseX     = mouseX;
        this.mouseY     = mouseY;
    }

    /**
     * @return the slot id of the player who changed the key or button
     */
    public int getPlayer() {
        return this.player;
    }

    /**
     * @return the command
     */
    public @NotNull ScriptInputCommand getCommand() {
        return this.command;
    }

    /**
     * @return {@code true} if the mouse position is known, {@code false} otherwise
     */
    public boolean hasMousePosition() {
        return this.positioned;
    }

    /**
     * @return the map X of the mouse in pixels, {@code 0} if unknown
     */
    public int getMouseX() {
        return this.mouseX;
    }

    /**
     * @return the map Y of the mouse in pixels, {@code 0} if unknown
     */
    public int getMouseY() {
        return this.mouseY;
    }
}
