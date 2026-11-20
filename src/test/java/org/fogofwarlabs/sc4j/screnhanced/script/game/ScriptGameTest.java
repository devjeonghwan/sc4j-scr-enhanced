package org.fogofwarlabs.sc4j.screnhanced.script.game;

import org.fogofwarlabs.sc4j.scr.core.LcgRandom;
import org.fogofwarlabs.sc4j.scr.game.state.Game;
import org.fogofwarlabs.sc4j.scr.game.terrain.GameMap;
import org.fogofwarlabs.sc4j.screnhanced.script.ScriptManifest;
import org.fogofwarlabs.sc4j.screnhanced.script.ScriptPackage;
import org.fogofwarlabs.sc4j.screnhanced.script.runtime.ScriptRuntime;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Tests the game API of {@link ScriptGame} on a game without game data: the type constants with their original data names, the game's frame
 * and map, the timers of {@code game.every}, the read-only mode and the sealed {@code core}.
 */
final class ScriptGameTest {
    private static final int MAP_TILE_WIDTH = 4;
    private static final int MAP_TILE_HEIGHT = 3;
    private static final int MAP_TILESET = 2;

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
