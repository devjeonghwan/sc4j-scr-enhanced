package org.fogofwarlabs.sc4j.screnhanced.script.core;

import org.fogofwarlabs.sc4j.scr.content.ScrUnitIds;
import org.fogofwarlabs.sc4j.scr.core.test.RequiresInstallation;
import org.fogofwarlabs.sc4j.scr.data.format.terrain.TerrainTileset;
import org.fogofwarlabs.sc4j.scr.game.map.MapStringSection;
import org.fogofwarlabs.sc4j.scr.game.player.PlayerRelations;
import org.fogofwarlabs.sc4j.scr.game.presentation.MessageLines;
import org.fogofwarlabs.sc4j.scr.game.state.Game;
import org.fogofwarlabs.sc4j.scr.game.test.GameResourcesExtension;
import org.fogofwarlabs.sc4j.scr.game.test.InstallationTestResources;
import org.fogofwarlabs.sc4j.scr.game.test.RunningGameScenario;
import org.fogofwarlabs.sc4j.scr.game.trigger.TriggerContext;
import org.fogofwarlabs.sc4j.scr.game.trigger.TriggerEvents;
import org.fogofwarlabs.sc4j.scr.game.trigger.eud.region.LocalView;
import org.fogofwarlabs.sc4j.scr.game.unit.Unit;
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
 * Resources routine, supply, alliances, printed texts, game results, the centering of the local view, the counters and unit removals of the
 * trigger routines, AI scripts, mission objectives as registered map strings, the local display routines, and the upgrade, technology and unit
 * availability state with its effect on the player's units and production.
 */
@RequiresInstallation
@ExtendWith(GameResourcesExtension.class)
final class ScriptPlayerCoreTest {
    /**
     * The score type of the custom score.
     */
    private static final int CUSTOM_SCORE = 7;
    /**
     * The frames the counter test runs after its start, so that the killed units are gone.
     */
    private static final int COUNTED_FRAMES = 24;

    @Test
    void upgrade_levels_refresh_the_existing_units_and_technologies_change_their_state(@NotNull InstallationTestResources resources) throws IOException {
        LiveScriptGameScenario scenario = LiveScriptGameScenario.startMelee(resources.getGameData(), """
                function onStart() {
                    var human = game.getHumanPlayers()[0];
                    var position = game.findUnit({type: UnitType.TerranCommandCenter, owner: human}).getPosition();
                    var vulture = game.createUnit(UnitType.TerranVulture, human, position);
                    var speed = vulture.getTopSpeed();
                    var thrusters = UpgradeType.IonThrusters;
                    log.info(human.getUpgradeLevel(thrusters), human.getMaxUpgradeLevel(thrusters), human.isUpgradeInProgress(thrusters));
                    human.setUpgradeLevel(thrusters, 1);
                    log.info(human.getUpgradeLevel(thrusters), vulture.getTopSpeed() > speed, core.player.getUpgradeLevel(human.getId(), thrusters.getId()));
                    human.setMaxUpgradeLevel(UpgradeType.TerranInfantryWeapons, 5);
                    log.info(human.getMaxUpgradeLevel(UpgradeType.TerranInfantryWeapons), game.getPlayer(2).getMaxUpgradeLevel(UpgradeType.TerranInfantryWeapons));
                    log.info(human.isTechResearched(TechType.StimPacks), human.isTechAvailable(TechType.Lockdown), human.isTechInProgress(TechType.StimPacks));
                    human.setTechResearched(TechType.StimPacks, true);
                    human.setTechAvailable(TechType.Lockdown, false);
                    log.info(human.isTechResearched(TechType.StimPacks), human.isTechAvailable(TechType.Lockdown));
                    human.setUnitAvailable(UnitType.TerranMarine, true);
                    log.info(human.isUnitAvailable(UnitType.TerranMarine));
                    try {
                        human.setUnitAvailable(UnitType.TerranMarine, false);
                    } catch (error) {
                        log.info(error.name);
                    }
                }
                """);

        Assertions.assertEquals(List.of("0 1 false", "1 true 1", "5 3", "false true false", "true false", "true", "TypeError"),
                                scenario.getInfoMessages(), scenario.getLines()::toString);
    }

    @Test
    void unit_availability_blocks_production_in_a_map_settings_game(@NotNull InstallationTestResources resources) throws IOException {
        LiveScriptGameScenario scenario = LiveScriptGameScenario.startUseMapSettings(resources.getGameData(), """
                var human;
                var frames = 0;
                function onStart() {
                    human = game.getHumanPlayers()[0];
                    var barracks = null;
                    for (var x = 256; barracks === null && x < game.getMapWidth(); x += 256) {
                        for (var y = 256; barracks === null && y < game.getMapHeight(); y += 256) {
                            barracks = game.createUnit(UnitType.TerranBarracks, human, Position.of(x, y));
                        }
                    }
                    log.info(barracks.getId(), human.isUnitAvailable(UnitType.TerranMarine));
                    human.setUnitAvailable(UnitType.TerranMarine, false);
                    log.info(human.isUnitAvailable(UnitType.TerranMarine));
                }
                function onFrame() {
                    frames++;
                    if (frames > 1) {
                        human.setUnitAvailable(UnitType.TerranMarine, true);
                    }
                }
                """);
        Assertions.assertFalse(scenario.getInfoMessages().isEmpty(), scenario.getLines()::toString);

        Game game = scenario.getGame();
        Unit barracks = game.getUnitSystem().requireUnitPool().getUnit(Integer.parseInt(scenario.getInfoMessages().getFirst().split(" ")[0]));
        boolean unavailable = game.getUnitProduction().canBuild(barracks, ScrUnitIds.TERRAN_MARINE);

        scenario.stepFrame();

        Assertions.assertFalse(unavailable);
        Assertions.assertTrue(game.getUnitProduction().canBuild(barracks, ScrUnitIds.TERRAN_MARINE));
        Assertions.assertEquals(List.of(barracks.getIndex() + " true", "false"), scenario.getInfoMessages(), scenario.getLines()::toString);
    }

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
                    if (calls === 2) {
                        game.findUnit({type: UnitType.ZergHatchery}).getOwner().centerView(Position.of(64, 64));
                    } else if (calls === 3) {
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

    @Test
    void counters_and_unit_removals_take_the_trigger_routines(@NotNull InstallationTestResources resources) throws IOException {
        LiveScriptGameScenario scenario = LiveScriptGameScenario.startMelee(resources.getGameData(), """
                var human, computer;
                var frames = 0;
                function onStart() {
                    human = game.getHumanPlayers()[0];
                    computer = game.findUnit({type: UnitType.ZergHatchery}).getOwner();
                    log.info(human.getId(), computer.getId());
                    human.setDeaths(UnitType.TerranMarine, 7);
                    human.addDeaths(UnitType.TerranMarine, 3);
                    human.subtractDeaths(UnitType.TerranMarine, 4);
                    computer.subtractDeaths(UnitType.ZergZergling, 4);
                    log.info(human.getDeaths(UnitType.TerranMarine), core.player.getDeaths(human.getId(), UnitType.TerranMarine.getId()),
                             computer.getDeaths(UnitType.ZergZergling), human.getDeaths(UnitGroup.Men));
                    human.setScore(ScoreType.Custom, 50);
                    human.addScore(ScoreType.Custom, 5);
                    human.subtractScore(ScoreType.Kills, 1);
                    log.info(human.getScore(ScoreType.Custom), human.getScore(ScoreType.Kills), human.getKills(UnitGroup.Men),
                             human.getResources(ResourceType.Minerals) === human.getMinerals(), human.getResources(ResourceType.MineralsAndGas));
                    var everywhere = game.getLocation(0);
                    everywhere.setBounds(0, 0, game.getMapWidth(), game.getMapHeight());
                    log.info(human.countUnits(UnitType.TerranSCV), human.countUnits(UnitGroup.Buildings, {completed: true}),
                             human.countUnits(UnitType.TerranSCV, {location: everywhere}), computer.countUnits(UnitType.ZergDrone),
                             human.countOpponents(), computer.countOpponents());
                    human.removeUnits(UnitType.TerranSCV, {count: 1});
                    human.killUnits(UnitType.TerranSCV, {location: everywhere, count: 2});
                    computer.killUnits(UnitType.ZergDrone);
                    log.info(human.countUnits(UnitType.TerranSCV));
                    try {
                        human.getDeaths(UnitType.TerranMarine.getGroundWeapon());
                    } catch (error) {
                        log.info(error.name);
                    }
                    try {
                        core.player.setScore(8, 0, 1);
                    } catch (error) {
                        log.info(error.name);
                    }
                }
                function onFrame() {
                    frames++;
                    if (frames === 24) {
                        log.info(human.countUnits(UnitType.TerranSCV), computer.countUnits(UnitType.ZergDrone), human.getDeaths(UnitType.TerranSCV),
                                 computer.getDeaths(UnitType.ZergDrone), human.getDeaths(UnitGroup.Men));
                    }
                }
                """);

        for (int frame = 0; frame < COUNTED_FRAMES; frame++) {
            scenario.stepFrame();
        }

        Assertions.assertEquals(List.of("0 1",
                                        "6 6 0 0",
                                        "55 0 0 true 50",
                                        "4 1 4 4 1 1",
                                        "4",
                                        "TypeError",
                                        "RangeError",
                                        "1 0 0 0 0"), scenario.getInfoMessages(), scenario.getLines()::toString);

        TriggerContext triggers = scenario.getGame().getTriggerContext();

        Assertions.assertNotNull(triggers);
        Assertions.assertEquals(6, triggers.getDeaths(0, ScrUnitIds.TERRAN_MARINE));
        Assertions.assertEquals(55, triggers.getScore(0, CUSTOM_SCORE));
        Assertions.assertEquals(1, triggers.countCommandedUnits(0, ScrUnitIds.TERRAN_SCV, false));
    }

    @Test
    void alliances_and_ai_scripts_take_the_trigger_routines(@NotNull InstallationTestResources resources) throws IOException {
        LiveScriptGameScenario scenario = LiveScriptGameScenario.startMelee(resources.getGameData(), """
                function onStart() {
                    var human = game.getHumanPlayers()[0];
                    var computer = game.findUnit({type: UnitType.ZergHatchery}).getOwner();
                    log.info(human.getAlliance(computer) === Alliance.Enemy, Alliance.AlliedVictory.getId(), String(Alliance.Ally));
                    human.setAlliance(computer, Alliance.AlliedVictory);
                    log.info(human.getAlliance(computer) === Alliance.AlliedVictory, computer.getAlliance(human) === Alliance.Enemy,
                             human.isAlliedWith(computer), human.countOpponents());
                    log.info(human.runAiScript('EnBk'), human.runAiScript('TMCx'), computer.runAiScript('EnBk', game.getLocation(0)));
                    try {
                        human.runAiScript('EnB');
                    } catch (error) {
                        log.info(error.name);
                    }
                    try {
                        game.getPlayer(8).setAlliance(human, Alliance.Ally);
                    } catch (error) {
                        log.info(error.name);
                    }
                }
                """);

        Assertions.assertEquals(List.of("true 2 Ally",
                                        "true true true 1",
                                        "true false true",
                                        "RangeError",
                                        "RangeError"), scenario.getInfoMessages(), scenario.getLines()::toString);
        Assertions.assertEquals(PlayerRelations.ALLIED_VICTORY, scenario.getGame().getPlayerTable().requirePlayerRelations().getAlliance(0, 1));
    }

    @Test
    void mission_objectives_register_each_text_once(@NotNull InstallationTestResources resources) throws IOException {
        LiveScriptGameScenario scenario = LiveScriptGameScenario.startMelee(resources.getGameData(), """
                function onStart() {
                    var human = game.getHumanPlayers()[0];
                    var computer = game.findUnit({type: UnitType.ZergHatchery}).getOwner();
                    human.setMissionObjectives('적 기지를 파괴하십시오.');
                    computer.setMissionObjectives('적 기지를 파괴하십시오.');
                    human.setMissionObjectives('적 기지를 파괴하십시오.');
                    game.getPlayer(2).setMissionObjectives('');
                    try {
                        human.setMissionObjectives('a' + String.fromCharCode(0) + 'b');
                    } catch (error) {
                        log.info(error.name);
                    }
                }
                """);
        TriggerContext triggers = scenario.getGame().getTriggerContext();
        MapStringSection strings = scenario.getGame().getInterfaceLink().getMessageLines().getMapStringSection();

        Assertions.assertNotNull(triggers);
        Assertions.assertNotNull(strings);
        Assertions.assertEquals(List.of("RangeError"), scenario.getInfoMessages(), scenario.getLines()::toString);
        Assertions.assertEquals(1, strings.getRegisteredStringCount());

        int stringIndex = triggers.getMissionObjectivesStringIndex(0);

        Assertions.assertEquals(stringIndex, triggers.getMissionObjectivesStringIndex(1));
        Assertions.assertEquals(0, triggers.getMissionObjectivesStringIndex(2));
        Assertions.assertEquals("적 기지를 파괴하십시오.", strings.getString(stringIndex));
    }

    @Test
    void display_routines_show_for_the_local_player_only(@NotNull InstallationTestResources resources) throws IOException {
        LiveScriptGameScenario scenario = LiveScriptGameScenario.startMelee(resources.getGameData(), """
                function onStart() {
                    var human = game.getHumanPlayers()[0];
                    var computer = game.findUnit({type: UnitType.ZergHatchery}).getOwner();
                    [computer, human].forEach(function (player) {
                        player.print('quiet', {forced: false});
                        player.pingMinimap(Position.of(320, 480));
                        player.playSound('sound/custom.wav');
                        player.showTalkingPortrait(UnitType.TerranMarine, 24);
                    });
                    human.setUnitSpeechMuted(true);
                }
                """);
        TriggerContext triggers = scenario.getGame().getTriggerContext();

        Assertions.assertNotNull(triggers);

        TriggerEvents events = triggers.getEvents();
        List<String> shown = new ArrayList<>();

        for (int index = 0; index < events.getCount(); index++) {
            byte[] path = events.getPath(index);

            shown.add(events.getKind(index) + " " + events.getFirst(index) + " " + events.getSecond(index) + " " + events.getFourth(index) +
                      (path == null ? "" : " " + new String(path, StandardCharsets.UTF_8)));
        }

        Assertions.assertEquals(List.of(TriggerEvents.TEXT + " 0 0 0",
                                        TriggerEvents.MINIMAP_PING + " 320 480 0",
                                        TriggerEvents.WAV + " 0 0 0 sound/custom.wav",
                                        TriggerEvents.TRANSMISSION + " 0 -1 1008",
                                        TriggerEvents.UNIT_SPEECH_MUTE + " 0 0 0"), shown, scenario.getLines()::toString);
    }
}
