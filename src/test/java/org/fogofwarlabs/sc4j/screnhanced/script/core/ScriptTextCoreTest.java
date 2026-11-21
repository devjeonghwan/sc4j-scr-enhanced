package org.fogofwarlabs.sc4j.screnhanced.script.core;

import org.fogofwarlabs.sc4j.scr.core.LcgRandom;
import org.fogofwarlabs.sc4j.scr.game.presentation.MessageLines;
import org.fogofwarlabs.sc4j.scr.game.presentation.StatTextOverrides;
import org.fogofwarlabs.sc4j.scr.game.state.Game;
import org.fogofwarlabs.sc4j.scr.game.terrain.GameMap;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Tests {@code core.strings}, {@code core.messages} and their upper layer ({@code strings}, {@code messages}, {@code UnitType.setName}) on a game
 * without game data: the {@code stat_txt} strings the game's text lookup shows instead, in shared and in local code, and the cleared message lines.
 */
final class ScriptTextCoreTest {
    private static final int MAP_TILE_SIZE = 4;
    private static final int MARINE_NAME = 1;
    private static final int GHOST_NAME = 2;
    private static final int BUTTON_TEXT = 700;
    /**
     * Defines {@code error(fn)}, which returns the name of the error {@code fn} throws, or {@code "none"}.
     */
    private static final @NotNull String ERROR_FUNCTION = """
            function error(fn) {
                try {
                    fn();
                    return 'none';
                } catch (e) {
                    return e.name;
                }
            }
            """;

    private static @NotNull Game createGame() {
        return Game.create(null, new GameMap(MAP_TILE_SIZE, MAP_TILE_SIZE, 0, new int[MAP_TILE_SIZE * MAP_TILE_SIZE]), new LcgRandom(0));
    }

    @Test
    void stat_text_overrides_reach_the_game_text_lookup() throws IOException {
        Game game = createGame();
        ScriptGameScenario scenario = new ScriptGameScenario(game, ERROR_FUNCTION + """
                UnitType.get(0).setName('Space Marine');
                UnitType.get(1).setName('Spectre');
                strings.setStatText(700, 'Build');
                log.info(error(function () { strings.setStatText(0, 'x'); }), error(function () { core.strings.setStatText(65536, 'x'); }),
                         error(function () { core.strings.setStatText(1, 5); }));
                function onLocalFrame(frame) {
                    UnitType.get(1).setName(null);
                }
                """);
        StatTextOverrides overrides = game.getInterfaceLink().getStatTextOverrides();

        scenario.getRuntime().start();

        Assertions.assertEquals(List.of("RangeError RangeError TypeError"), scenario.getInfoMessages(), scenario.getLines()::toString);
        Assertions.assertEquals("Space Marine", overrides.find(MARINE_NAME));
        Assertions.assertEquals("Spectre", overrides.find(GHOST_NAME));
        Assertions.assertEquals("Build", overrides.find(BUTTON_TEXT));

        scenario.getRuntime().callLocalFrame(1);

        Assertions.assertNull(overrides.find(GHOST_NAME), "null shows the game's name again, also from local code");
        Assertions.assertEquals("Space Marine", overrides.find(MARINE_NAME));
    }

    @Test
    void messages_clear_should_empty_the_message_lines() throws IOException {
        Game game = createGame();
        ScriptGameScenario scenario = new ScriptGameScenario(game, """
                function onStart() {
                    messages.clear();
                }
                """);
        MessageLines lines = game.getInterfaceLink().getMessageLines();

        lines.writeByte(0, 'a');
        lines.writeByte(MessageLines.LINE_SIZE, 'b');
        lines.writeSystemLine("system".getBytes(StandardCharsets.UTF_8), 0);
        scenario.getRuntime().start();
        scenario.getRuntime().runFrame(0);

        for (int line = 0; line < MessageLines.LINE_COUNT; line++) {
            Assertions.assertEquals(0, lines.readByte(line * MessageLines.LINE_SIZE), "line " + line);
        }
    }
}
