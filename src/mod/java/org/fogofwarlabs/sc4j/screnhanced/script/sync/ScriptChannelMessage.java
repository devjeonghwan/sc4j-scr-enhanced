package org.fogofwarlabs.sc4j.screnhanced.script.sync;

import org.jetbrains.annotations.NotNull;

/**
 * A whole channel message of a player, joined from its mod commands ({@link ScriptChannelCommands}).
 */
public final class ScriptChannelMessage {
    private final int player;
    private final int channel;
    private final byte @NotNull [] value;

    /**
     * Creates a {@link ScriptChannelMessage} instance.
     *
     * @param player  the slot id of the sending player
     * @param channel the channel id
     * @param value   the encoded value ({@link ScriptValueUtils})
     */
    ScriptChannelMessage(int player, int channel, byte @NotNull [] value) {
        this.player  = player;
        this.channel = channel;
        this.value   = value;
    }

    /**
     * @return the slot id of the sending player
     */
    public int getPlayer() {
        return this.player;
    }

    /**
     * @return the channel id
     */
    public int getChannel() {
        return this.channel;
    }

    /**
     * @return the encoded value ({@link ScriptValueUtils}); the array is shared and must not be changed
     */
    public byte @NotNull [] getValue() {
        return this.value;
    }
}
