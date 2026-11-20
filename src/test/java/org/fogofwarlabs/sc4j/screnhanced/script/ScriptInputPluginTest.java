package org.fogofwarlabs.sc4j.screnhanced.script;

import org.fogofwarlabs.sc4j.base.log.LogLevel;
import org.fogofwarlabs.sc4j.scr.core.LcgRandom;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrInputModifiers;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrKeyInput;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrModCommandSubmitter;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrMouseInput;
import org.fogofwarlabs.sc4j.scr.game.state.Game;
import org.fogofwarlabs.sc4j.scr.game.terrain.GameMap;
import org.fogofwarlabs.sc4j.scr.game.trigger.eud.region.LocalView;
import org.fogofwarlabs.sc4j.screnhanced.script.runtime.ScriptLogOutput;
import org.fogofwarlabs.sc4j.screnhanced.script.runtime.ScriptRuntime;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tests how {@link ScriptInputPlugin} hands the local keys, mouse buttons and chat lines to the local hooks: the event values, the
 * consumption, the channel commands the hooks send, and a game without a submitter or a script.
 */
final class ScriptInputPluginTest {
    private static final int KEY_Q = 0x51;
    private static final int KEY_W = 0x57;
    private static final int MAP_TILE_SIZE = 4;
    private static final long SEED = 1;
    private static final @NotNull String MANIFEST = "{\"name\": \"Test\", \"main\": \"main.js\", \"api\": \"1\"}";
    /**
     * Logs every local event and consumes the presses of Q, the right button and the chat lines that start with {@code !}, which it also
     * sends on a channel.
     */
    private static final @NotNull String SCRIPT = """
            var channel = sync.createChannel(function () {});
            function describe(event) {
                return event.getMousePosition() + ' ' + event.isShift() + ' ' + event.isControl() + ' ' + event.isAlt();
            }
            function onLocalKeyDown(event) {
                log.info('key down ' + event.getKey().getName() + ' ' + describe(event));
                if (event.getKey() === Key.Q) {
                    event.consume();
                }
            }
            function onLocalKeyUp(event) {
                log.info('key up ' + event.getKey().getName() + ' ' + describe(event));
                event.consume();
            }
            function onLocalMouseDown(event) {
                log.info('mouse down ' + event.getButton().getName() + ' ' + describe(event));
                if (event.getButton() === MouseButton.Right) {
                    event.consume();
                }
            }
            function onLocalChat(event) {
                log.info('chat ' + event.getText() + ' ' + event.isConsumed());
                if (event.getText().charAt(0) === '!') {
                    event.consume();
                    channel.send(event.getText());
                }
            }
            """;

    /**
     * Records the submitted commands.
     */
    private static final class RecordingSubmitter implements ScrModCommandSubmitter {
        private final @NotNull List<byte[]> commands;

        private RecordingSubmitter() {
            this.commands = new ArrayList<>();
        }

        @Override
        public boolean submitModCommand(byte @NotNull [] data) {
            this.commands.add(data.clone());

            return true;
        }
    }

    /**
     * Creates a started runtime of the script over a game without game data; its info messages go to a list.
     */
    private static @NotNull ScriptRuntime startRuntime(@NotNull List<String> messages) throws IOException {
        ScriptPackage scriptPackage = ScriptPackage.create(ScriptManifest.parse(MANIFEST.getBytes(StandardCharsets.UTF_8)),
                                                           Map.of("main.js", SCRIPT.getBytes(StandardCharsets.UTF_8)), Map.of());
        Game game = Game.create(null, new GameMap(MAP_TILE_SIZE, MAP_TILE_SIZE, 0, new int[MAP_TILE_SIZE * MAP_TILE_SIZE]), new LcgRandom(0));
        ScriptLogOutput logOutput = (level, moduleName, message) -> messages.add(level == LogLevel.INFO ? message : level + " " + message);
        ScriptRuntime runtime = ScriptRuntime.create(scriptPackage, game, ScriptResources.EMPTY, SEED, ScriptRuntime.INSTRUCTION_BUDGET, logOutput);

        runtime.start();

        return runtime;
    }

    @Test
    void local_hooks_receive_the_keys_buttons_and_chat_lines_and_consume_when_asked() throws IOException {
        List<String> messages = new ArrayList<>();
        ScriptRuntime runtime = startRuntime(messages);
        RecordingSubmitter submitter = new RecordingSubmitter();
        LocalView view = new LocalView();
        ScriptInputPlugin plugin = new ScriptInputPlugin(() -> runtime, () -> submitter, view);

        Assertions.assertFalse(plugin.onKeyInput(new ScrKeyInput(KEY_W, true, ScrInputModifiers.SHIFT)), "a key the hook does not consume");

        view.setView(640, 320, 100, 50);

        Assertions.assertTrue(plugin.onKeyInput(new ScrKeyInput(KEY_Q, true, ScrInputModifiers.CONTROL | ScrInputModifiers.ALT)));
        Assertions.assertTrue(plugin.onKeyInput(new ScrKeyInput(KEY_Q, false, 0)), "a consumed release is reported as consumed");
        Assertions.assertFalse(plugin.onMouseInput(new ScrMouseInput(ScrMouseInput.Button.LEFT, true, 0, 10, 20)));
        Assertions.assertTrue(plugin.onMouseInput(new ScrMouseInput(ScrMouseInput.Button.RIGHT, true, ScrInputModifiers.SHIFT, 300, 200)));
        Assertions.assertFalse(plugin.onMouseInput(new ScrMouseInput(ScrMouseInput.Button.RIGHT, false, 0, 300, 200)), "no up hook");
        Assertions.assertFalse(plugin.onChatInput("hello"));
        Assertions.assertTrue(plugin.onChatInput("!ready"));
        Assertions.assertEquals(List.of("key down W null true false false",
                                        "key down Q (740, 370) false true true",
                                        "key up Q (740, 370) false false false",
                                        "mouse down Left (10, 20) false false false",
                                        "mouse down Right (300, 200) true false false",
                                        "chat hello false",
                                        "chat !ready false"), messages);
        Assertions.assertEquals(1, submitter.commands.size(), "the hook's send is submitted at once");
        Assertions.assertFalse(runtime.isReadOnly(), "the game is writable again after a local hook");
    }

    @Test
    void a_game_without_a_submitter_or_a_script_calls_no_local_hook() throws IOException {
        List<String> messages = new ArrayList<>();
        ScriptRuntime runtime = startRuntime(messages);
        RecordingSubmitter submitter = new RecordingSubmitter();
        ScriptInputPlugin replay = new ScriptInputPlugin(() -> runtime, () -> null, new LocalView());
        ScriptInputPlugin withoutScript = new ScriptInputPlugin(() -> null, () -> submitter, new LocalView());

        Assertions.assertFalse(replay.onKeyInput(new ScrKeyInput(KEY_Q, true, 0)));
        Assertions.assertFalse(replay.onMouseInput(new ScrMouseInput(ScrMouseInput.Button.RIGHT, true, 0, 1, 1)));
        Assertions.assertFalse(replay.onChatInput("!ready"));
        Assertions.assertFalse(withoutScript.onKeyInput(new ScrKeyInput(KEY_Q, true, 0)));
        Assertions.assertFalse(withoutScript.onChatInput("!ready"));
        Assertions.assertEquals(List.of(), messages);
        Assertions.assertEquals(List.of(), submitter.commands);
    }
}
