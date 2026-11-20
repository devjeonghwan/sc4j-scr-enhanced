package org.fogofwarlabs.sc4j.screnhanced.script.input;

import org.fogofwarlabs.sc4j.scr.game.extension.ScrInputModifiers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the mod command data of the script input commands: the byte layout, the round trip and the data that is not an input command.
 */
final class ScriptInputCommandTest {
    private static final int KEY_Q = 0x51;
    private static final int RIGHT_BUTTON = 1;

    private static void assertCommand(@NotNull ScriptInputCommand expected, @NotNull ScriptInputCommand actual) {
        Assertions.assertEquals(expected.getKind(), actual.getKind());
        Assertions.assertEquals(expected.getCode(), actual.getCode());
        Assertions.assertEquals(expected.isPressed(), actual.isPressed());
        Assertions.assertEquals(expected.getModifiers(), actual.getModifiers());
        Assertions.assertEquals(expected.isPositioned(), actual.isPositioned());
        Assertions.assertEquals(expected.getX(), actual.getX());
        Assertions.assertEquals(expected.getY(), actual.getY());
    }

    @Test
    void encode_should_write_the_kind_code_flags_and_little_endian_position() {
        byte[] key = ScriptInputCommand.ofKey(KEY_Q, true, ScrInputModifiers.SHIFT | ScrInputModifiers.ALT).encode();
        byte[] positionedKey = ScriptInputCommand.ofKey(KEY_Q, false, 0, 0x1234, 0xFFFF).encode();
        byte[] mouse = ScriptInputCommand.ofMouse(RIGHT_BUTTON, true, ScrInputModifiers.CONTROL | 0x80, 300, 2).encode();

        Assertions.assertArrayEquals(new byte[]{(byte) 0xE1, 0x51, 0b1011}, key);
        Assertions.assertArrayEquals(new byte[]{(byte) 0xE1, 0x51, 0b1_0000, 0x34, 0x12, (byte) 0xFF, (byte) 0xFF}, positionedKey);
        Assertions.assertArrayEquals(new byte[]{(byte) 0xE2, 0x01, 0b1_0101, 0x2C, 0x01, 0x02, 0x00}, mouse);
    }

    @Test
    void decode_should_return_the_encoded_command() {
        ScriptInputCommand[] commands = {
                ScriptInputCommand.ofKey(0, false, 0),
                ScriptInputCommand.ofKey(0xFF, true, ScriptInputCommand.MODIFIER_MASK),
                ScriptInputCommand.ofKey(KEY_Q, true, ScrInputModifiers.CONTROL, 4095, 17),
                ScriptInputCommand.ofMouse(2, false, ScrInputModifiers.SHIFT, 0, ScriptInputCommand.MAXIMUM_COORDINATE)
        };

        for (ScriptInputCommand command : commands) {
            ScriptInputCommand decoded = ScriptInputCommand.decode(command.encode());

            Assertions.assertNotNull(decoded);
            assertCommand(command, decoded);
        }
    }

    @Test
    void decode_should_reject_data_that_is_not_an_input_command() {
        byte[][] rejected = {
                {},
                {(byte) 0xE1, 0x51},
                {0x53, 0x43, 0x34},
                {(byte) 0xE1, 0x51, 0b10_0001},
                {(byte) 0xE1, 0x51, 0b1_0001},
                {(byte) 0xE1, 0x51, 0b0001, 0, 0, 0, 0},
                {(byte) 0xE2, 0x01, 0b0001},
                {(byte) 0xE2, 0x03, 0b1_0001, 0, 0, 0, 0},
                {(byte) 0xE2, 0x01, 0b1_0001, 0, 0, 0, 0, 0}
        };

        for (byte[] data : rejected) {
            Assertions.assertNull(ScriptInputCommand.decode(data));
        }
    }

    @Test
    void factories_should_reject_codes_and_positions_out_of_range() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ScriptInputCommand.ofKey(ScriptInputCommand.KEY_COUNT, true, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ScriptInputCommand.ofMouse(ScriptInputCommand.BUTTON_COUNT, true, 0, 0, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ScriptInputCommand.ofMouse(0, true, 0, -1, 0));
        int outside = ScriptInputCommand.MAXIMUM_COORDINATE + 1;

        Assertions.assertThrows(IllegalArgumentException.class, () -> ScriptInputCommand.ofKey(KEY_Q, true, 0, 0, outside));
    }
}
