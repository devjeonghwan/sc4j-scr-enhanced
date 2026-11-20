package org.fogofwarlabs.sc4j.screnhanced.script;

import org.fogofwarlabs.sc4j.scr.game.extension.ScrInputModifiers;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrKeyInput;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrModCommandSubmitter;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrMouseInput;
import org.fogofwarlabs.sc4j.scr.game.trigger.eud.region.LocalView;
import org.fogofwarlabs.sc4j.screnhanced.script.input.ScriptInputCommand;
import org.fogofwarlabs.sc4j.screnhanced.script.input.ScriptInputState;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the local input that {@link ScriptInputPlugin} sends and consumes: the tracked presses, their releases, the positions, the consumption
 * and a turn without room.
 */
final class ScriptInputPluginTest {
    private static final int KEY_Q = 0x51;
    private static final int KEY_W = 0x57;

    /**
     * Records the submitted commands and refuses them while it has no room.
     */
    private static final class RecordingSubmitter implements ScrModCommandSubmitter {
        private final @NotNull List<ScriptInputCommand> commands;

        private boolean room;

        private RecordingSubmitter() {
            this.commands = new ArrayList<>();

            this.room = true;
        }

        @Override
        public boolean submitModCommand(byte @NotNull [] data) {
            if (!this.room) {
                return false;
            }

            ScriptInputCommand command = ScriptInputCommand.decode(data);

            Assertions.assertNotNull(command);
            this.commands.add(command);

            return true;
        }
    }

    @Test
    void on_key_input_should_submit_tracked_presses_with_their_releases_and_consume_when_asked() {
        ScriptInputState state = new ScriptInputState();
        RecordingSubmitter submitter = new RecordingSubmitter();
        ScriptInputPlugin plugin = new ScriptInputPlugin(() -> state, () -> submitter, new LocalView());

        state.track(new int[]{KEY_Q}, 0, false);

        Assertions.assertFalse(plugin.onKeyInput(new ScrKeyInput(KEY_W, true, 0)), "an untracked key");
        Assertions.assertFalse(plugin.onKeyInput(new ScrKeyInput(KEY_W, false, 0)));
        Assertions.assertFalse(plugin.onKeyInput(new ScrKeyInput(KEY_Q, true, ScrInputModifiers.SHIFT)), "a tracked key, not consumed");
        Assertions.assertFalse(plugin.onKeyInput(new ScrKeyInput(KEY_Q, false, 0)));

        state.track(new int[]{KEY_Q}, 0, true);

        Assertions.assertTrue(plugin.onKeyInput(new ScrKeyInput(KEY_Q, true, 0)), "a tracked key, consumed");
        Assertions.assertFalse(plugin.onKeyInput(new ScrKeyInput(KEY_Q, false, 0)), "a release is not consumed");
        Assertions.assertFalse(plugin.onKeyInput(new ScrKeyInput(KEY_Q, false, 0)), "a second release");
        Assertions.assertEquals(4, submitter.commands.size());
        Assertions.assertEquals(ScriptInputCommand.Kind.KEY, submitter.commands.getFirst().getKind());
        Assertions.assertEquals(KEY_Q, submitter.commands.getFirst().getCode());
        Assertions.assertTrue(submitter.commands.getFirst().isPressed());
        Assertions.assertEquals(ScrInputModifiers.SHIFT, submitter.commands.getFirst().getModifiers());
        Assertions.assertFalse(submitter.commands.getFirst().isPositioned(), "no position while the mouse is not tracked");
        Assertions.assertFalse(submitter.commands.get(1).isPressed());
    }

    @Test
    void on_key_input_should_carry_the_pointer_map_position_while_the_mouse_is_tracked() {
        ScriptInputState state = new ScriptInputState();
        RecordingSubmitter submitter = new RecordingSubmitter();
        LocalView view = new LocalView();
        ScriptInputPlugin plugin = new ScriptInputPlugin(() -> state, () -> submitter, view);

        state.track(new int[]{KEY_Q}, ScriptInputState.ALL_BUTTONS, false);

        plugin.onKeyInput(new ScrKeyInput(KEY_Q, true, 0));
        view.setView(640, 320, 100, 50);
        plugin.onKeyInput(new ScrKeyInput(KEY_Q, false, 0));

        Assertions.assertFalse(submitter.commands.getFirst().isPositioned(), "no position while the local view is unknown");
        Assertions.assertTrue(submitter.commands.get(1).isPositioned());
        Assertions.assertEquals(740, submitter.commands.get(1).getX());
        Assertions.assertEquals(370, submitter.commands.get(1).getY());
    }

    @Test
    void on_mouse_input_should_submit_tracked_buttons_and_consume_when_asked() {
        ScriptInputState state = new ScriptInputState();
        RecordingSubmitter submitter = new RecordingSubmitter();
        ScriptInputPlugin plugin = new ScriptInputPlugin(() -> state, () -> submitter, new LocalView());

        state.track(new int[0], 1 << ScrMouseInput.Button.RIGHT.ordinal(), true);

        Assertions.assertFalse(plugin.onMouseInput(new ScrMouseInput(ScrMouseInput.Button.LEFT, true, 0, 10, 20)), "an untracked button");
        Assertions.assertFalse(plugin.onMouseInput(new ScrMouseInput(ScrMouseInput.Button.LEFT, false, 0, 10, 20)));
        Assertions.assertTrue(plugin.onMouseInput(new ScrMouseInput(ScrMouseInput.Button.RIGHT, true, ScrInputModifiers.ALT, 300, 200)));
        Assertions.assertFalse(plugin.onMouseInput(new ScrMouseInput(ScrMouseInput.Button.RIGHT, false, 0, -5, 70_000)));
        Assertions.assertEquals(2, submitter.commands.size());

        ScriptInputCommand press = submitter.commands.getFirst();
        ScriptInputCommand release = submitter.commands.get(1);

        Assertions.assertEquals(ScriptInputCommand.Kind.MOUSE, press.getKind());
        Assertions.assertEquals(ScrMouseInput.Button.RIGHT.ordinal(), press.getCode());
        Assertions.assertEquals(ScrInputModifiers.ALT, press.getModifiers());
        Assertions.assertEquals(300, press.getX());
        Assertions.assertEquals(200, press.getY());
        Assertions.assertFalse(release.isPressed());
        Assertions.assertEquals(0, release.getX(), "positions are clamped to the command range");
        Assertions.assertEquals(ScriptInputCommand.MAXIMUM_COORDINATE, release.getY());
    }

    @Test
    void a_press_without_room_is_dropped_with_its_release_but_still_consumed() {
        ScriptInputState state = new ScriptInputState();
        RecordingSubmitter submitter = new RecordingSubmitter();
        ScriptInputPlugin plugin = new ScriptInputPlugin(() -> state, () -> submitter, new LocalView());

        state.track(new int[]{KEY_Q}, 0, true);
        submitter.room = false;

        Assertions.assertTrue(plugin.onKeyInput(new ScrKeyInput(KEY_Q, true, 0)));

        submitter.room = true;

        Assertions.assertFalse(plugin.onKeyInput(new ScrKeyInput(KEY_Q, false, 0)));
        Assertions.assertEquals(List.of(), submitter.commands);
    }

    @Test
    void a_game_without_a_submitter_or_a_script_sends_and_consumes_nothing() {
        ScriptInputState state = new ScriptInputState();
        RecordingSubmitter submitter = new RecordingSubmitter();
        ScriptInputPlugin replay = new ScriptInputPlugin(() -> state, () -> null, new LocalView());
        ScriptInputPlugin withoutScript = new ScriptInputPlugin(() -> null, () -> submitter, new LocalView());

        state.track(new int[]{KEY_Q}, ScriptInputState.ALL_BUTTONS, true);

        Assertions.assertFalse(replay.onKeyInput(new ScrKeyInput(KEY_Q, true, 0)));
        Assertions.assertFalse(replay.onMouseInput(new ScrMouseInput(ScrMouseInput.Button.LEFT, true, 0, 1, 1)));
        Assertions.assertFalse(withoutScript.onKeyInput(new ScrKeyInput(KEY_Q, true, 0)));
        Assertions.assertEquals(List.of(), submitter.commands);
    }
}
