package org.fogofwarlabs.sc4j.screnhanced.script.game;

import org.fogofwarlabs.sc4j.scr.core.LcgRandom;
import org.fogofwarlabs.sc4j.scr.core.test.RequiresInstallation;
import org.fogofwarlabs.sc4j.scr.game.state.Game;
import org.fogofwarlabs.sc4j.scr.game.terrain.GameMap;
import org.fogofwarlabs.sc4j.scr.game.test.GameResourcesExtension;
import org.fogofwarlabs.sc4j.scr.game.test.InstallationTestResources;
import org.fogofwarlabs.sc4j.scr.game.trigger.TriggerContext;
import org.fogofwarlabs.sc4j.screnhanced.script.ScriptManifest;
import org.fogofwarlabs.sc4j.screnhanced.script.ScriptPackage;
import org.fogofwarlabs.sc4j.screnhanced.script.runtime.ScriptRuntime;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Tests the game API of {@link ScriptGame} on a game without game data: the type constants with their original data names, the game's frame
 * and map, the timers of {@code game.every}, the read-only mode and the sealed {@code core}; and in a live melee game the switches, the
 * countdown timer and the elapsed time of the trigger state.
 */
final class ScriptGameTest {
    private static final int MAP_TILE_WIDTH = 4;
    private static final int MAP_TILE_HEIGHT = 3;
    private static final int MAP_TILESET = 2;
    /**
     * The frames the trigger state test runs after its start: three seconds of the trigger clock.
     */
    private static final int CLOCK_FRAMES = 48;

    private static @NotNull Game createGame() {
        return Game.create(null, new GameMap(MAP_TILE_WIDTH, MAP_TILE_HEIGHT, MAP_TILESET, new int[MAP_TILE_WIDTH * MAP_TILE_HEIGHT]),
                           new LcgRandom(0));
    }

    private static @NotNull ScriptGameScenario start(@NotNull Game game, @NotNull String mainSource) throws IOException {
        ScriptGameScenario scenario = new ScriptGameScenario(game, mainSource);

        scenario.getRuntime().start();

        return scenario;
    }

    @Test
    void constants_carry_the_original_data_names() throws IOException {
        ScriptGameScenario scenario = start(createGame(), """
                log.info(UnitType.TerranMarine.getId(), UnitType.TerranMarine.getName(), UnitType.HeroJimRaynorMarine.getId(),
                         UnitType.ZergZergling.getId(), UnitType.TerranSiegeTankTankMode.getId(), UnitType.get(37) === UnitType.ZergZergling);
                log.info(WeaponType.GaussRifle.getId(), UpgradeType.TerranInfantryWeapons.getId(), TechType.StimPacks.getId(),
                         Order.AttackMove.getId(), Order.PlayerGuard.getName());
                log.info(UnitType.getCount(), WeaponType.getCount(), UpgradeType.getCount(), TechType.getCount(), Order.getCount());
                function check(table, namespace) {
                    var matching = 0;
                    for (var id = 0; id < namespace.getCount(); id++) {
                        var identifier = namespace.getName(id).split(' ').map(function (word) {
                            return word.charAt(0).toUpperCase() + word.substring(1);
                        }).join('');
                        if (table[identifier] === table.get(id) && table.get(id).getId() === id) {
                            matching++;
                        }
                    }
                    return matching === namespace.getCount();
                }
                log.info(check(UnitType, core.unitType), check(WeaponType, core.weaponType), check(UpgradeType, core.upgradeType),
                         check(TechType, core.techType), check(Order, core.orderType));
                log.info(Object.isFrozen(UnitType), Object.isFrozen(UnitType.TerranMarine), UnitType.get(228), String(UnitType.ZergZergling));
                """);

        Assertions.assertEquals(List.of("0 Terran Marine 20 37 5 true",
                                        "0 7 0 14 PlayerGuard",
                                        "228 130 61 44 189",
                                        "true true true true true",
                                        "true true null Zerg Zergling"), scenario.getInfoMessages(), scenario.getLines()::toString);
    }

    @Test
    void game_reads_the_frame_and_the_map() throws IOException {
        Game game = createGame();

        game.storeCurrentFrame(7);

        ScriptGameScenario scenario = start(game, """
                log.info(game.getFrame(), game.getMapTileWidth(), game.getMapTileHeight(), game.getMapWidth(), game.getMapHeight(),
                         game.getTileset(), Time.seconds(10), Time.minutes(1), core.game.getFrameDuration());
                """);

        Assertions.assertEquals(List.of("7 4 3 128 96 2 238 1429 42"), scenario.getInfoMessages(), scenario.getLines()::toString);
    }

    @Test
    void timers_run_at_frame_start_in_due_and_registration_order() throws IOException {
        Game game = createGame();
        ScriptGameScenario scenario = start(game, """
                var third;
                function onStart() {
                    game.every(3, function () { log.info('first ' + game.getFrame()); });
                    game.every(2, function () { log.info('second ' + game.getFrame()); });
                    third = game.every(3, function () { log.info('third ' + game.getFrame()); });
                }
                function onFrame(frame) {
                    log.info('frame ' + frame);
                    if (frame === 6) {
                        log.info('cancel ' + third.cancel() + ' ' + third.cancel());
                    }
                }
                """);

        for (int frame = 1; frame <= 9; frame++) {
            game.storeCurrentFrame(frame);
            scenario.getRuntime().runFrame(frame);
        }

        Assertions.assertEquals(List.of("frame 1",
                                        "second 2", "frame 2",
                                        "first 3", "third 3", "frame 3",
                                        "second 4", "frame 4",
                                        "frame 5",
                                        "first 6", "second 6", "third 6", "frame 6", "cancel true false",
                                        "frame 7",
                                        "second 8", "frame 8",
                                        "first 9", "frame 9"), scenario.getInfoMessages(), scenario.getLines()::toString);
    }

    @Test
    void timers_run_without_a_frame_hook_and_their_errors_end_only_their_call() throws IOException {
        Game game = createGame();
        ScriptGameScenario scenario = start(game, """
                game.every(1, function () { throw new Error('broken'); });
                game.every(1, function () { log.info('ran ' + game.getFrame()); });
                """);

        game.storeCurrentFrame(1);
        scenario.getRuntime().runFrame(1);

        Assertions.assertEquals(List.of("ERROR main.js: Timer failed: Error: broken (main.js:1)", "INFO main.js: ran 1"), scenario.getLines());
    }

    @Test
    void read_only_mode_refuses_state_changes_with_a_type_error() throws IOException {
        Game game = createGame();
        ScriptGameScenario scenario = new ScriptGameScenario(game, """
                function attempt(name, change) {
                    try {
                        change();
                        log.info(name + ' changed');
                    } catch (error) {
                        log.info(name + ' ' + error.name);
                    }
                }
                function onFrame() {
                    log.info('read-only ' + game.isReadOnly() + ' ' + game.getFrame());
                    attempt('minerals', function () { core.player.setMinerals(0, 5); });
                    attempt('create', function () { core.unit.create(0, 0, 32, 32); });
                    attempt('kill', function () { core.unit.kill(1); });
                    attempt('location', function () { core.location.setBounds(0, 0, 0, 32, 32); });
                    attempt('data', function () { UnitType.TerranMarine.setMaxHp(1); });
                }
                """);

        scenario.getRuntime().start();
        scenario.getRuntime().setReadOnly(true);
        scenario.getRuntime().runFrame(0);

        Assertions.assertTrue(scenario.getRuntime().isReadOnly());
        Assertions.assertEquals(List.of("read-only true 0", "minerals TypeError", "create TypeError", "kill TypeError", "location TypeError",
                                        "data TypeError"), scenario.getInfoMessages(), scenario.getLines()::toString);
    }

    @Test
    void read_only_mode_refuses_the_trigger_writes_with_a_type_error() throws IOException {
        Game game = createGame();
        ScriptGameScenario scenario = new ScriptGameScenario(game, """
                var writes = {
                    switch: function () { game.setSwitch(0, true); },
                    toggle: function () { game.toggleSwitch(0); },
                    countdown: function () { game.setCountdownTimer(5); },
                    add: function () { game.addCountdownTimer(5); },
                    subtract: function () { game.subtractCountdownTimer(5); },
                    pause: function () { game.pauseCountdownTimer(); },
                    resume: function () { game.resumeCountdownTimer(); },
                    deaths: function () { game.getPlayer(0).setDeaths(UnitType.TerranMarine, 1); },
                    addDeaths: function () { game.getPlayer(0).addDeaths(UnitGroup.Men, 1); },
                    subtractDeaths: function () { game.getPlayer(0).subtractDeaths(UnitType.TerranMarine, 1); },
                    score: function () { game.getPlayer(0).setScore(ScoreType.Custom, 1); },
                    addScore: function () { game.getPlayer(0).addScore(ScoreType.Custom, 1); },
                    subtractScore: function () { game.getPlayer(0).subtractScore(ScoreType.Custom, 1); },
                    kill: function () { game.getPlayer(0).killUnits(UnitGroup.AnyUnit); },
                    remove: function () { game.getPlayer(0).removeUnits(UnitGroup.AnyUnit); },
                    alliance: function () { game.getPlayer(0).setAlliance(game.getPlayer(1), Alliance.Ally); },
                    script: function () { game.getPlayer(0).runAiScript('EnBk'); },
                    scriptAt: function () { core.player.runAiScriptAt(0, 0, 0); },
                    objectives: function () { game.getPlayer(0).setMissionObjectives('Objectives'); },
                    leaderboard: function () { game.showKillsLeaderboard('Kills', UnitGroup.Men); },
                    leaderboardAt: function () { core.leaderboard.showAt(18, 0, 0, 0, 'Here'); },
                    greed: function () { game.showGreedLeaderboard(10); },
                    computers: function () { game.setLeaderboardComputersIncluded(true); },
                    resources: function () { core.unit.setResources(1, 5); },
                    hangar: function () { core.unit.addHangarCount(1, 1); },
                    enable: function () { core.unit.enableDoodad(1); },
                    disable: function () { core.unit.disableDoodad(1); },
                    invincible: function () { core.unit.setInvincible(1, true); }
                };
                function onFrame() {
                    var refused = [];
                    for (var name in writes) {
                        try {
                            writes[name]();
                        } catch (error) {
                            if (error.name === 'TypeError' && error.message.indexOf('read-only') >= 0) {
                                refused.push(name);
                            }
                        }
                    }
                    var changed = Object.keys(writes).filter(function (name) {
                        return refused.indexOf(name) < 0;
                    });
                    log.info(changed.length === 0 ? 'all refused' : 'changed: ' + changed.join(' '));
                }
                """);

        scenario.getRuntime().start();
        scenario.getRuntime().setReadOnly(true);
        scenario.getRuntime().runFrame(0);

        Assertions.assertEquals(List.of("all refused"), scenario.getInfoMessages(), scenario.getLines()::toString);
    }

    @Test
    @RequiresInstallation
    @ExtendWith(GameResourcesExtension.class)
    void switches_and_the_countdown_timer_take_the_trigger_routines(@NotNull InstallationTestResources resources) throws IOException {
        LiveScriptGameScenario scenario = LiveScriptGameScenario.startMelee(resources.getGameData(), """
                var frames = 0;
                var started;
                function onStart() {
                    log.info(game.isSwitchSet(3), game.isSwitchSet(4), game.getCountdownTimer(), game.isCountdownTimerPaused());
                    game.setSwitch(3, true);
                    game.toggleSwitch(4);
                    game.toggleSwitch(3);
                    log.info(game.isSwitchSet(3), game.isSwitchSet(4), core.game.isSwitchSet(4));
                    game.setCountdownTimer(100);
                    game.addCountdownTimer(10);
                    game.subtractCountdownTimer(5);
                    log.info(game.getCountdownTimer());
                    game.subtractCountdownTimer(1000);
                    log.info(game.getCountdownTimer());
                    game.setCountdownTimer(400000);
                    log.info(game.getCountdownTimer());
                    game.setCountdownTimer(60);
                    game.pauseCountdownTimer();
                    log.info(game.isCountdownTimerPaused());
                    game.resumeCountdownTimer();
                    started = game.getElapsedTime();
                    try {
                        game.setSwitch(256, true);
                    } catch (error) {
                        log.info(error.name);
                    }
                }
                function onFrame() {
                    frames++;
                    if (frames === 48) {
                        log.info(game.getCountdownTimer(), game.getElapsedTime() - started, game.isCountdownTimerPaused());
                    }
                }
                """);

        for (int frame = 0; frame < CLOCK_FRAMES; frame++) {
            scenario.stepFrame();
        }

        Assertions.assertEquals(List.of("false false 0 false",
                                        "false true true",
                                        "105",
                                        "0",
                                        "359999",
                                        "true",
                                        "RangeError",
                                        "57 3 false"), scenario.getInfoMessages(), scenario.getLines()::toString);

        TriggerContext triggers = scenario.getGame().getTriggerContext();

        Assertions.assertNotNull(triggers);
        Assertions.assertFalse(triggers.isSwitchSet(3));
        Assertions.assertTrue(triggers.isSwitchSet(4));
        Assertions.assertEquals(57, triggers.getCountdownTimer());
    }

    @Test
    void core_and_the_globals_of_the_prelude_cannot_be_replaced() throws IOException {
        ScriptGameScenario scenario = start(createGame(), """
                function attempt(change) {
                    try {
                        change();
                        return 'ignored';
                    } catch (error) {
                        return error.name;
                    }
                }
                log.info(attempt(function () { core.unit.getType = null; }), attempt(function () { core.unit = null; }),
                         attempt(function () { core.extra = 1; }), attempt(function () { UnitType.TerranMarine = null; }),
                         attempt(function () { globalThis.game = null; }));
                log.info(typeof core.unit.getType, typeof core.extra, UnitType.TerranMarine.getId(), typeof game.getFrame,
                         Object.keys(core).length, Object.isFrozen(core.unit), Object.isFrozen(core));
                """);

        Assertions.assertEquals(List.of("ignored ignored ignored ignored ignored", "function undefined 0 function 0 true true"),
                                scenario.getInfoMessages(), scenario.getLines()::toString);
    }

    @Test
    void a_runtime_without_a_game_has_no_game_api() throws IOException {
        String manifest = "{\"name\": \"Test\", \"main\": \"main.js\", \"api\": \"1\"}";
        String main = "log.info(typeof core, typeof game, typeof UnitType, typeof Position, typeof Time);";
        ScriptPackage scriptPackage = ScriptPackage.create(ScriptManifest.parse(manifest.getBytes(StandardCharsets.UTF_8)),
                                                           Map.of("main.js", main.getBytes(StandardCharsets.UTF_8)), Map.of());
        StringBuilder log = new StringBuilder();
        ScriptRuntime runtime = ScriptRuntime.create(scriptPackage, ScriptGameScenario.SEED, ScriptRuntime.INSTRUCTION_BUDGET,
                                                     (level, moduleName, message) -> log.append(message));

        runtime.start();

        Assertions.assertEquals("undefined undefined undefined undefined undefined", log.toString());
        Assertions.assertThrows(IllegalStateException.class, () -> runtime.setReadOnly(true));
    }
}
