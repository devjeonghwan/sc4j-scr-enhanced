package org.fogofwarlabs.sc4j.screnhanced.script.sync;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.scr.data.format.replay.ReplayModCommand;
import org.fogofwarlabs.sc4j.scr.game.player.PlayerTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Splits channel messages into mod commands and joins the mod commands of each player back into messages.
 * <p>
 * The data of a command is the tag {@value #TAG}, the channel id as an unsigned little-endian 16-bit number, the flags (bit {@code 0}: more
 * commands of the message follow) and a part of the encoded value, at most {@value #MAXIMUM_PART_LENGTH} bytes. The commands of a message
 * follow each other in the sending player's command stream, since the sender submits its commands in order; a message is complete with the
 * command without the flag. A command with another tag is no channel command; a command that does not continue the player's open message for
 * the same channel drops that message.
 */
public final class ScriptChannelCommands {
    /**
     * The first byte of a channel command.
     */
    public static final int TAG = 0xE3;
    /**
     * The number of channel ids.
     */
    public static final int CHANNEL_COUNT = 0x10000;
    /**
     * The bytes before the value part: the tag, the channel id and the flags.
     */
    public static final int HEADER_LENGTH = 4;
    /**
     * The most value bytes one command carries.
     */
    public static final int MAXIMUM_PART_LENGTH = ReplayModCommand.MAXIMUM_DATA_LENGTH - HEADER_LENGTH;

    private static final int BYTE_MASK = 0xFF;
    private static final int TAG_OFFSET = 0;
    private static final int CHANNEL_OFFSET = 1;
    private static final int FLAGS_OFFSET = 3;
    private static final int MORE_FLAG = 1;

    /**
     * Splits the encoded value of a message into the data of its commands, in sending order.
     *
     * @param channel the channel id
     * @param value   the encoded value, not empty
     * @return the data of each command
     * @throws IllegalArgumentException if the channel id is outside {@code 0} to {@value #CHANNEL_COUNT} minus one or the value is empty
     */
    public static @NotNull List<byte @NotNull []> split(int channel, byte @NotNull [] value) {
        if (channel < 0 || channel >= CHANNEL_COUNT) {
            throw Exceptions.newIllegalArgumentException("The channel {0} is not from 0 to {1}.", channel, CHANNEL_COUNT - 1);
        }

        if (value.length == 0) {
            throw Exceptions.newIllegalArgumentException("A channel message has a value.");
        }

        List<byte[]> commands = new ArrayList<>();

        for (int offset = 0; offset < value.length; offset += MAXIMUM_PART_LENGTH) {
            int length = Math.min(MAXIMUM_PART_LENGTH, value.length - offset);
            byte[] data = new byte[HEADER_LENGTH + length];

            data[TAG_OFFSET]         = (byte) TAG;
            data[CHANNEL_OFFSET]     = (byte) channel;
            data[CHANNEL_OFFSET + 1] = (byte) (channel >> Byte.SIZE);
            data[FLAGS_OFFSET]       = (byte) (offset + length < value.length ? MORE_FLAG : 0);

            System.arraycopy(value, offset, data, HEADER_LENGTH, length);
            commands.add(data);
        }

        return commands;
    }


    private final @Nullable ByteArrayOutputStream @NotNull [] openValues;
    private final int @NotNull [] openChannels;

    /**
     * Creates a {@link ScriptChannelCommands} instance without open messages.
     */
    public ScriptChannelCommands() {
        this.openValues   = new ByteArrayOutputStream[PlayerTable.PLAYER_SLOTS];
        this.openChannels = new int[PlayerTable.PLAYER_SLOTS];
    }

    /**
     * Reads a mod command of a player: a channel command adds its part to the player's open message for its channel.
     *
     * @param player the slot id of the issuing player
     * @param data   the command data
     * @return the message the command completed, or {@code null} if the data is no channel command, the player is out of range or the message
     *         continues
     */
    public @Nullable ScriptChannelMessage read(int player, byte @NotNull [] data) {
        if (player < 0 || player >= PlayerTable.PLAYER_SLOTS || data.length < HEADER_LENGTH || (data[TAG_OFFSET] & BYTE_MASK) != TAG ||
            (data[FLAGS_OFFSET] & ~MORE_FLAG) != 0) {
            return null;
        }

        int channel = data[CHANNEL_OFFSET] & BYTE_MASK | (data[CHANNEL_OFFSET + 1] & BYTE_MASK) << Byte.SIZE;
        boolean more = (data[FLAGS_OFFSET] & MORE_FLAG) != 0;
        ByteArrayOutputStream open = this.openValues[player];

        if (open != null && this.openChannels[player] != channel) {
            open = null;
        }

        if (open == null) {
            if (!more) {
                this.openValues[player] = null;

                return new ScriptChannelMessage(player, channel, Arrays.copyOfRange(data, HEADER_LENGTH, data.length));
            }

            open = new ByteArrayOutputStream();

            this.openValues[player]   = open;
            this.openChannels[player] = channel;
        }

        open.write(data, HEADER_LENGTH, data.length - HEADER_LENGTH);

        if (more) {
            return null;
        }

        this.openValues[player] = null;

        return new ScriptChannelMessage(player, channel, open.toByteArray());
    }
}
