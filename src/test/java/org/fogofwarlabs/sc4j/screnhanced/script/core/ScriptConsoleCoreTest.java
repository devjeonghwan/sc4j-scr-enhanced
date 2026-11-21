package org.fogofwarlabs.sc4j.screnhanced.script.core;

import org.fogofwarlabs.sc4j.scr.core.LcgRandom;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrConsoleArt;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrConsolePlacement;
import org.fogofwarlabs.sc4j.scr.game.state.Game;
import org.fogofwarlabs.sc4j.scr.game.terrain.GameMap;
import org.fogofwarlabs.sc4j.screnhanced.graphics.ClassicConsoleGraphics;
import org.fogofwarlabs.sc4j.screnhanced.graphics.ConsoleSettings;
import org.fogofwarlabs.sc4j.screnhanced.graphics.ImageFileScenario;
import org.fogofwarlabs.sc4j.screnhanced.script.ScriptMapScenario;
import org.fogofwarlabs.sc4j.screnhanced.script.ScriptResources;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Tests {@code core.console} and its upper layer ({@code console}, {@code ConsoleControl}, {@code ConsoleImage}) on a game without game data: the
 * settings they set in shared and in local code, the resource icons of each resource type, the refusals, and the classic console that shows them.
 */
final class ScriptConsoleCoreTest {
    private static final int MAP_TILE_SIZE = 4;
    private static final int GAS_ICONS = 3;
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

    private static @NotNull ScriptResources createResources() throws IOException {
        return ScriptMapScenario.createResources(Map.of("ui/console.png", ImageFileScenario.createPng(), "ui/gas.dds", ImageFileScenario.createDds()));
    }

    @Test
    void console_settings_follow_shared_and_local_code() throws IOException {
        ScriptGameScenario scenario = new ScriptGameScenario(createGame(), ERROR_FUNCTION + """
                var art = resources.getImage('ui/console.png');
                var gas = resources.getImage('ui/gas.dds');
                var minimap = console.getControl(ConsoleControl.Minimap);
                minimap.setPosition(10.4, 20.6);
                console.getControl(ConsoleControl.CommandCard).setVisible(false);
                console.replaceImage(ConsoleImage.Console, art);
                console.replaceImage(ConsoleImage.ConsoleRight, art);
                console.replaceImage(ConsoleImage.ConsoleTime, gas);
                console.setResourceIcon(ResourceType.Gas, gas);
                log.info(minimap === console.getControl(ConsoleControl.Minimap), minimap.getControl() === ConsoleControl.Minimap,
                         console.getControl(ConsoleControl.CommandCard).isVisible(), minimap.isVisible(), console.isVisible());
                log.info(error(function () { console.setResourceIcon(ResourceType.MineralsAndGas, gas); }),
                         error(function () { console.getControl(3); }), error(function () { core.console.setDialogVisible(7, true); }),
                         error(function () { core.console.replaceImage(7, -1); }), error(function () { core.console.setResourceIcon(7, -1); }));
                log.info(ConsoleImage.getCount(), ConsoleImage.Console.getId(), ConsoleImage.ConsoleLeft.getId(), ConsoleImage.ConsoleCenter.getId(),
                         ConsoleImage.ConsoleRight.getId(), ConsoleImage.ConsoleTime.getId(), ConsoleImage.ConsoleApm.getId(),
                         ConsoleImage.Conover.getId());
                function onLocalFrame(frame) {
                    console.setVisible(false);
                    console.setResourceIcon(ResourceType.Minerals, gas);
                    console.replaceImage(ConsoleImage.Console, null);
                }
                """, createResources());

        scenario.getRuntime().start();

        ConsoleSettings settings = scenario.getRuntime().getConsoleSettings();

        Assertions.assertEquals(List.of("true true false true true", "TypeError TypeError RangeError RangeError RangeError", "7 0 1 2 3 4 5 6"),
                                scenario.getInfoMessages(), scenario.getLines()::toString);
        Assertions.assertTrue(settings.isDialogPositionSet(ScrConsolePlacement.Dialog.MINIMAP));
        Assertions.assertEquals(10, settings.getDialogLeft(ScrConsolePlacement.Dialog.MINIMAP));
        Assertions.assertEquals(21, settings.getDialogTop(ScrConsolePlacement.Dialog.MINIMAP));
        Assertions.assertFalse(settings.isDialogVisible(ScrConsolePlacement.Dialog.COMMAND_CARD));
        Assertions.assertNotNull(settings.getArt(ScrConsoleArt.CONSOLE));
        Assertions.assertSame(settings.getArt(ScrConsoleArt.CONSOLE), settings.getArt(ScrConsoleArt.CONSOLE_RIGHT));
        Assertions.assertNotNull(settings.getArt(ScrConsoleArt.CONSOLE_TIME));
        Assertions.assertNull(settings.getArt(ScrConsoleArt.CONSOLE_LEFT));
        Assertions.assertNull(settings.getResourceIcon(0));

        for (int icon = 1; icon <= GAS_ICONS; icon++) {
            Assertions.assertNotNull(settings.getResourceIcon(icon), "every race's gas icon");
        }

        Assertions.assertNull(settings.getResourceIcon(GAS_ICONS + 1));

        ClassicConsoleGraphics graphics = new ClassicConsoleGraphics(() -> settings);
        ScrConsolePlacement placement = graphics.findConsolePlacement();

        Assertions.assertNotNull(placement);
        Assertions.assertTrue(placement.isDialogHidden(ScrConsolePlacement.Dialog.COMMAND_CARD));
        Assertions.assertEquals(10, placement.getDialogLeft(ScrConsolePlacement.Dialog.MINIMAP));
        Assertions.assertSame(settings.getArt(ScrConsoleArt.CONSOLE), graphics.findConsoleArt(ScrConsoleArt.CONSOLE));
        Assertions.assertFalse(graphics.isConsoleHidden());

        scenario.getRuntime().callLocalFrame(1);

        Assertions.assertTrue(graphics.isConsoleHidden(), "local code sets the local console");
        Assertions.assertNotNull(graphics.findResourceIconPicture(0));
        Assertions.assertNull(graphics.findConsoleArt(ScrConsoleArt.CONSOLE));
        Assertions.assertNotNull(graphics.findConsoleArt(ScrConsoleArt.CONSOLE_RIGHT), "the other files stay");
    }
}
