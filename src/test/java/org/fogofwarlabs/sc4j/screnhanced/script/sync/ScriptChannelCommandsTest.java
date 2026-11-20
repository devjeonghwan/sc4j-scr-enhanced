package org.fogofwarlabs.sc4j.screnhanced.script.sync;

import org.fogofwarlabs.sc4j.scr.data.format.replay.ReplayModCommand;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Tests the mod commands of channel messages ({@link ScriptChannelCommands}): the split into commands with their header, the join per player,
 * and the commands that are no channel commands or do not continue a message.
 */
final class ScriptChannelCommandsTest {
    private static final int CHANNEL = 0x1234;

    private static byte @NotNull [] createValue(int length) {
        byte[] value = new byte[length];

        for (int index = 0; index < length; index++) {
            value[index] = (byte) index;
        }

        return value;
    }

    @Test
    void split_should_fill_commands_up_to_the_data_limit_with_a_header_each() {
        byte[] value = createValue(ScriptChannelCommands.MAXIMUM_PART_LENGTH * 2 + 1);
        List<byte[]> commands = ScriptChannelCommands.split(CHANNEL, value);

        Assertions.assertEquals(3, commands.size());
        Assertions.assertEquals(ReplayModCommand.MAXIMUM_DATA_LENGTH, commands.getFirst().length);
        Assertions.assertEquals(ScriptChannelCommands.HEADER_LENGTH + 1, commands.getLast().length);
        Assertions.assertArrayEquals(new byte[]{(byte) ScriptChannelCommands.TAG, 0x34, 0x12, 1},
                                     Arrays.copyOf(commands.getFirst(), ScriptChannelCommands.HEADER_LENGTH));
        Assertions.assertEquals(0, commands.getLast()[ScriptChannelCommands.HEADER_LENGTH - 1], "the last command has no more flag");
        Assertions.assertThrows(IllegalArgumentException.class, () -> ScriptChannelCommands.split(ScriptChannelCommands.CHANNEL_COUNT, value));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ScriptChannelCommands.split(0, new byte[0]));
    }

    @Test
    void read_should_join_the_commands_of_each_player_into_messages() {
        byte[] large = createValue(ScriptChannelCommands.MAXIMUM_PART_LENGTH + 10);
        List<byte[]> first = ScriptChannelCommands.split(CHANNEL, large);
        List<byte[]> second = ScriptChannelCommands.split(1, new byte[]{7});
        ScriptChannelCommands commands = new ScriptChannelCommands();

        Assertions.assertNull(commands.read(0, first.getFirst()));

        ScriptChannelMessage smallMessage = commands.read(3, second.getFirst());
        ScriptChannelMessage largeMessage = commands.read(0, first.getLast());

        Assertions.assertNotNull(smallMessage);
        Assertions.assertEquals(3, smallMessage.getPlayer());
        Assertions.assertEquals(1, smallMessage.getChannel());
        Assertions.assertArrayEquals(new byte[]{7}, smallMessage.getValue());
        Assertions.assertNotNull(largeMessage);
        Assertions.assertEquals(0, largeMessage.getPlayer());
        Assertions.assertEquals(CHANNEL, largeMessage.getChannel());
        Assertions.assertArrayEquals(large, largeMessage.getValue());
    }

    @Test
    void read_should_skip_other_commands_and_drop_a_message_that_is_not_continued() {
        byte[] large = createValue(ScriptChannelCommands.MAXIMUM_PART_LENGTH + 10);
        List<byte[]> broken = ScriptChannelCommands.split(CHANNEL, large);
        byte[] other = ScriptChannelCommands.split(1, new byte[]{9}).getFirst();
        ScriptChannelCommands commands = new ScriptChannelCommands();

        Assertions.assertNull(commands.read(0, new byte[]{(byte) 0xE1, 0x51, 1}), "another mod's command");
        Assertions.assertNull(commands.read(0, new byte[]{(byte) ScriptChannelCommands.TAG, 0, 0, 2, 0}), "unknown flags");
        Assertions.assertNull(commands.read(-1, other), "a player outside the slots");
        Assertions.assertNull(commands.read(0, broken.getFirst()));

        ScriptChannelMessage message = commands.read(0, other);

        Assertions.assertNotNull(message, "a command of another channel drops the open message");
        Assertions.assertArrayEquals(new byte[]{9}, message.getValue());

        ScriptChannelMessage rest = commands.read(0, broken.getLast());

        Assertions.assertNotNull(rest, "the rest of a dropped message reads as a message of its own");
        Assertions.assertEquals(10, rest.getValue().length);
    }
}
