package org.fogofwarlabs.sc4j.screnhanced.script.input;

import org.fogofwarlabs.sc4j.scr.game.extension.ScrInputModifiers;
import org.fogofwarlabs.sc4j.scr.game.player.PlayerTable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the input state of a script game: the tracking, the keys and buttons down of each player, the mouse positions and the events of the
 * applied commands.
 */
final class ScriptInputStateTest {
    private static final int KEY_Q = 0x51;
    private static final int KEY_W = 0x57;
    private static final int LEFT_BUTTON = 0;
    private static final int RIGHT_BUTTON = 1;
    private static final int FIRST_PLAYER = 0;
    private static final int SECOND_PLAYER = 3;

    @Test
    void track_should_replace_the_tracked_keys_buttons_and_consumption() {
        ScriptInputState state = new ScriptInputState();

        Assertions.assertFalse(state.isKeyTracked(KEY_Q));
        Assertions.assertFalse(state.isMouseTracked());
        Assertions.assertFalse(state.isConsuming());

        state.track(new int[]{KEY_Q, KEY_W}, 1 << RIGHT_BUTTON, true);

        Assertions.assertTrue(state.isKeyTracked(KEY_Q));
        Assertions.assertTrue(state.isKeyTracked(KEY_W));
        Assertions.assertFalse(state.isButtonTracked(LEFT_BUTTON));
        Assertions.assertTrue(state.isButtonTracked(RIGHT_BUTTON));
        Assertions.assertTrue(state.isMouseTracked());
        Assertions.assertTrue(state.isConsuming());

        state.track(new int[]{KEY_W}, 0, false);

        Assertions.assertFalse(state.isKeyTracked(KEY_Q));
        Assertions.assertTrue(state.isKeyTracked(KEY_W));
        Assertions.assertFalse(state.isMouseTracked());
        Assertions.assertFalse(state.isConsuming());
        Assertions.assertFalse(state.isKeyTracked(-1));
        Assertions.assertFalse(state.isButtonTracked(ScriptInputCommand.BUTTON_COUNT));
        Assertions.assertThrows(IllegalArgumentException.class, () -> state.track(new int[]{ScriptInputCommand.KEY_COUNT}, 0, false));
        Assertions.assertThrows(IllegalArgumentException.class, () -> state.track(new int[0], ScriptInputState.ALL_BUTTONS + 1, false));
    }

    @Test
    void apply_should_pair_presses_of_tracked_input_with_releases_per_player() {
        ScriptInputState state = new ScriptInputState();

        state.track(new int[]{KEY_Q}, 0, false);

        Assertions.assertNull(state.apply(FIRST_PLAYER, ScriptInputCommand.ofKey(KEY_W, true, 0)), "an untracked press");
        Assertions.assertNull(state.apply(FIRST_PLAYER, ScriptInputCommand.ofKey(KEY_Q, false, 0)), "a release without its press");

        ScriptInputEvent press = state.apply(SECOND_PLAYER, ScriptInputCommand.ofKey(KEY_Q, true, ScrInputModifiers.SHIFT));

        Assertions.assertNotNull(press);
        Assertions.assertEquals(SECOND_PLAYER, press.getPlayer());
        Assertions.assertEquals(ScrInputModifiers.SHIFT, press.getCommand().getModifiers());
        Assertions.assertFalse(press.hasMousePosition());
        Assertions.assertTrue(state.isKeyDown(SECOND_PLAYER, KEY_Q));
        Assertions.assertFalse(state.isKeyDown(FIRST_PLAYER, KEY_Q));

        state.track(new int[0], 0, false);

        Assertions.assertNotNull(state.apply(SECOND_PLAYER, ScriptInputCommand.ofKey(KEY_Q, false, 0)), "the release of a key down");
        Assertions.assertFalse(state.isKeyDown(SECOND_PLAYER, KEY_Q));
        Assertions.assertNull(state.apply(PlayerTable.PLAYER_SLOTS, ScriptInputCommand.ofKey(KEY_Q, true, 0)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> state.isKeyDown(PlayerTable.PLAYER_SLOTS, KEY_Q));
    }

    @Test
    void apply_should_keep_the_last_mouse_position_for_key_events_while_the_mouse_is_tracked() {
        ScriptInputState state = new ScriptInputState();

        state.track(new int[]{KEY_Q}, 1 << LEFT_BUTTON, false);

        ScriptInputEvent click = state.apply(FIRST_PLAYER, ScriptInputCommand.ofMouse(LEFT_BUTTON, true, 0, 320, 240));

        Assertions.assertNotNull(click);
        Assertions.assertTrue(state.isButtonDown(FIRST_PLAYER, LEFT_BUTTON));
        Assertions.assertTrue(state.hasMousePosition(FIRST_PLAYER));
        Assertions.assertFalse(state.hasMousePosition(SECOND_PLAYER));
        Assertions.assertNull(state.apply(FIRST_PLAYER, ScriptInputCommand.ofMouse(RIGHT_BUTTON, true, 0, 1, 1)), "an untracked button");
        Assertions.assertEquals(320, state.getMouseX(FIRST_PLAYER), "an ignored command keeps the position");

        ScriptInputEvent key = state.apply(FIRST_PLAYER, ScriptInputCommand.ofKey(KEY_Q, true, 0));

        Assertions.assertNotNull(key);
        Assertions.assertTrue(key.hasMousePosition());
        Assertions.assertEquals(320, key.getMouseX());
        Assertions.assertEquals(240, key.getMouseY());

        ScriptInputEvent positionedKey = state.apply(FIRST_PLAYER, ScriptInputCommand.ofKey(KEY_Q, false, 0, 96, 64));

        Assertions.assertNotNull(positionedKey);
        Assertions.assertEquals(96, positionedKey.getMouseX());
        Assertions.assertEquals(64, state.getMouseY(FIRST_PLAYER));

        state.track(new int[]{KEY_Q}, 0, false);

        ScriptInputEvent unpositioned = state.apply(FIRST_PLAYER, ScriptInputCommand.ofKey(KEY_Q, true, 0));

        Assertions.assertNotNull(unpositioned);
        Assertions.assertFalse(unpositioned.hasMousePosition(), "no position while the mouse is not tracked");

        ScriptInputEvent release = state.apply(FIRST_PLAYER, ScriptInputCommand.ofMouse(LEFT_BUTTON, false, 0, 8, 9));

        Assertions.assertNotNull(release, "the release of a button down after the tracking changed");
        Assertions.assertFalse(state.isButtonDown(FIRST_PLAYER, LEFT_BUTTON));
        Assertions.assertEquals(8, release.getMouseX());
    }
}
