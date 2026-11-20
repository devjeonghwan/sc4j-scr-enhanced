package org.fogofwarlabs.sc4j.screnhanced.script.game;

import org.fogofwarlabs.sc4j.scr.core.test.RequiresInstallation;
import org.fogofwarlabs.sc4j.scr.data.format.terrain.TerrainTileset;
import org.fogofwarlabs.sc4j.scr.game.presentation.MessageLines;
import org.fogofwarlabs.sc4j.scr.game.test.GameResourcesExtension;
import org.fogofwarlabs.sc4j.scr.game.test.InstallationTestResources;
import org.fogofwarlabs.sc4j.scr.game.test.RunningGameScenario;
import org.fogofwarlabs.sc4j.scr.game.trigger.eud.region.LocalView;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests {@code core.player} and the {@code Player} objects of the prelude in a live melee game: identity, resources through the Set
 * Resources routine, supply, alliances, printed texts, game results and the centering of the local view.
 */
@RequiresInstallation
@ExtendWith(GameResourcesExtension.class)
final class ScriptPlayerCoreTest {
    @Test
    void players_read_and_change_resources_supply_and_alliances(@NotNull InstallationTestResources resources) throws IOException {
        LiveScriptGameScenario scenario = LiveScriptGameScenario.startMelee(resources.getGameData(), """
                function onStart() {
                    var humans = game.getHumanPlayers();
                    var human = humans[0];
                    var computer = game.findUnit({type: UnitType.ZergHatchery}).getOwner();
                    log.info(humans.length, human === game.getPlayer(human.getId()), human.getName(), human.getRace(), computer.isHuman(),
                             computer.getRace(), game.getPlayers().length, game.getPlayer(12));
                    log.info(human.getMinerals(), human.getGas());
                    human.setMinerals(500);
                    human.addMinerals(25);
                    human.addGas(-10);
                    core.player.setGas(game.getPlayer(11).getId(), 99);
                    log.info(human.getMinerals(), human.getGas(), game.getPlayer(11).getGas());
                    human.subtractMinerals(25);
                    human.subtractGas(5);
                    log.info(human.getMinerals(), human.getGas());
                    log.info(computer.lose(), computer.win(), computer.lose(), computer.draw());
                    log.info(human.getSupplyUsed(), human.getSupplyProvided(), human.getSupplyMax(), core.player.getSupplyUsed(human.getId(), 1),
                             computer.getSupplyProvided());
                    log.info(human.isEnemyOf(computer), human.isAlliedWith(human), core.player.getAlliance(human.getId(), computer.getId()));
                    game.print('to all');
                    human.print('to the human');
                    computer.print('to the computer');
                }
                """);

        Assertions.assertEquals(List.of("1 true tester 1 false 0 12 null",
                                        "50 0",
                                        "525 -10 0",
                                        "500 -15",
                                        "true true false false",
                                        "4 10 200 8 9",
                                        "true true 0"), scenario.getInfoMessages(), scenario.getLines()::toString);

        MessageLines lines = scenario.getGame().getInterfaceLink().getMessageLines();
        List<String> texts = new ArrayList<>();

        for (int line = 0; line < MessageLines.LINE_COUNT; line++) {
            texts.add(new String(lines.getLine(line), StandardCharsets.UTF_8));
        }

        Assertions.assertTrue(texts.stream().anyMatch(text -> text.contains("to all")), texts::toString);
        Assertions.assertTrue(texts.stream().anyMatch(text -> text.contains("to the human")), texts::toString);
        Assertions.assertTrue(texts.stream().noneMatch(text -> text.contains("to the computer")), texts::toString);
    }

    @Test
    void center_view_centers_the_local_screen_for_the_local_player_only(@NotNull InstallationTestResources resources) throws IOException {
        LiveScriptGameScenario scenario = LiveScriptGameScenario.startMelee(resources.getGameData(), """
                var calls = 0;
                function onFrame() {
                    calls++;
                    if (calls === 1) {
                        game.findUnit({type: UnitType.ZergHatchery}).getOwner().centerView(Position.of(64, 64));
                    } else if (calls === 2) {
                        game.getHumanPlayers()[0].centerView(Position.of(1000, 800));
                    }
                }
                """);
        LocalView view = scenario.getGame().getInterfaceLink().getLocalView();
        int mapWidth = scenario.getGame().getMap().getWidth() * TerrainTileset.TILE_SIZE;
        int mapHeight = scenario.getGame().getMap().getHeight() * TerrainTileset.TILE_SIZE;

        view.setView(0, 0, 0, 0);
        view.setScreenGeometry(RunningGameScenario.VIEWPORT_WIDTH, RunningGameScenario.VIEWPORT_HEIGHT,
                               mapWidth - RunningGameScenario.VIEWPORT_WIDTH, mapHeight - RunningGameScenario.VIEWPORT_HEIGHT);
        scenario.stepFrame();

        Assertions.assertEquals(0, view.getScreenX(), "the computer's view is not local");
        Assertions.assertEquals(0, view.getScreenY());

        scenario.stepFrame();

        Assertions.assertEquals(view.calculateCenteredScreenX(1000), view.getScreenX());
        Assertions.assertEquals(view.calculateCenteredScreenY(800), view.getScreenY());
        Assertions.assertTrue(view.takeWritten(), "the client applies the centered screen");
        Assertions.assertEquals(List.of(), scenario.getLines());
    }
}
