package org.fogofwarlabs.sc4j.screnhanced.script.game;

import org.fogofwarlabs.sc4j.scr.core.test.RequiresInstallation;
import org.fogofwarlabs.sc4j.scr.game.test.GameResourcesExtension;
import org.fogofwarlabs.sc4j.scr.game.test.InstallationTestResources;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.List;

/**
 * Tests {@code core.unit} and the {@code Unit} objects of the prelude in a live melee game: reading the start units, shown and raw hit
 * points, creating and killing through the trigger routines, one object per unit, {@code exists()} after death, and the doodad state,
 * resource amount and hangar routines.
 */
@RequiresInstallation
@ExtendWith(GameResourcesExtension.class)
final class ScriptUnitCoreTest {
    /**
     * The frames the doodad test runs after its start, so that the disabled unit runs its DisableDoodad order.
     */
    private static final int DOODAD_FRAMES = 8;

    @Test
    void units_read_the_start_units_in_slot_order(@NotNull InstallationTestResources resources) throws IOException {
        LiveScriptGameScenario scenario = LiveScriptGameScenario.startMelee(resources.getGameData(), """
                function onStart() {
                    var human = game.getHumanPlayers()[0];
                    var ids = core.unit.getIds();
                    var sorted = ids.every(function (id, index) { return index === 0 || ids[index - 1] < id; });
                    var commandCenter = game.findUnit({type: UnitType.TerranCommandCenter, owner: human});
                    log.info(sorted, ids.length === game.getUnits().length, game.countUnits({type: UnitType.TerranSCV, owner: human}),
                             game.countUnits({type: UnitType.ZergDrone}), human.getUnits().length);
                    log.info(commandCenter.getOwner() === human, commandCenter.isCompleted(), commandCenter.getHp(),
                             commandCenter.getMaxHp(), core.unit.getHp(commandCenter.getId()), commandCenter.getShields(),
                             commandCenter.getMaxEnergy(), commandCenter.getX() > 0, commandCenter.getPosition().getY() === commandCenter.getY());
                    var drone = game.findUnit({type: UnitType.ZergDrone});
                    log.info(drone.getMaxHp(), drone.getOwner().isEnemyOf(human), drone.isBurrowed(), drone.isCloaked(), drone.isInvincible());
                }
                """);

        Assertions.assertEquals(List.of("true true 4 4 5",
                                        "true true 1500 1500 384000 0 0 true true",
                                        "40 true false false false"), scenario.getInfoMessages(), scenario.getLines()::toString);
    }

    @Test
    void units_show_hit_points_and_take_writes_through_the_trigger_routines(@NotNull InstallationTestResources resources) throws IOException {
        LiveScriptGameScenario scenario = LiveScriptGameScenario.startMelee(resources.getGameData(), """
                function onStart() {
                    var human = game.getHumanPlayers()[0];
                    var commandCenter = game.findUnit({type: UnitType.TerranCommandCenter, owner: human});
                    var marine = game.createUnit(UnitType.TerranMarine, human, commandCenter.getPosition());
                    var id = marine.getId();
                    log.info(marine.getType() === UnitType.TerranMarine, marine.getOwner() === human, marine.isCompleted(), marine.getHp(),
                             core.unit.getHp(id), core.unit.getMaxHp(id));
                    marine.setHp(12.5);
                    log.info(marine.getHp(), core.unit.getHp(id));
                    marine.setHp(1000);
                    core.unit.setHp(id, 0);
                    log.info(core.unit.getHp(id));
                    marine.setHp(40);
                    log.info(marine.setShields(10), marine.setEnergy(10), marine.getShields(), marine.getEnergy());
                    marine.setInvincible(true);
                    log.info(marine.isInvincible(), core.unit.getStatus(id) !== 0);
                    marine.issueOrder(Order.Move, Position.of(marine.getX() + 64, marine.getY()));
                    log.info(marine.getOrder().getName());
                    marine.setHpPercent(50);
                    log.info(marine.getHp(), marine.setShieldsPercent(50), marine.setEnergyPercent(50));
                    var zergling = game.createUnit(UnitType.ZergZergling, game.getPlayer(11), commandCenter.getPosition());
                    log.info(zergling.getOwner().getId(), zergling.setOwner(human), zergling.getOwner() === human);
                    log.info(game.createUnit(UnitType.TerranMarine, human, Position.of(-5000, -5000)) !== null);
                    try {
                        core.unit.create(228, 0, 0, 0);
                    } catch (error) {
                        log.info(error.name);
                    }
                }
                """);

        Assertions.assertEquals(List.of("true true true 40 10240 10240",
                                        "12.5 3200",
                                        "1",
                                        "false false 0 0",
                                        "true true",
                                        "Move",
                                        "20 false false",
                                        "11 true true",
                                        "true",
                                        "RangeError"), scenario.getInfoMessages(), scenario.getLines()::toString);
    }

    @Test
    void a_unit_keeps_one_object_that_stops_existing_when_it_dies(@NotNull InstallationTestResources resources) throws IOException {
        LiveScriptGameScenario scenario = LiveScriptGameScenario.startMelee(resources.getGameData(), """
                var marine;
                var removed;
                function onStart() {
                    var human = game.getHumanPlayers()[0];
                    var commandCenter = game.findUnit({type: UnitType.TerranCommandCenter, owner: human});
                    marine = game.createUnit(UnitType.TerranMarine, human, commandCenter.getPosition());
                    removed = game.createUnit(UnitType.TerranMarine, human, commandCenter.getPosition());
                    var units = new Map([[marine, 'kept']]);
                    log.info(game.getUnit(marine.getId()) === marine, game.findUnits({type: UnitType.TerranMarine}).indexOf(marine) >= 0,
                             units.get(game.getUnit(marine.getId())), marine.exists());
                    log.info(marine.kill(), marine.exists(), game.getUnit(marine.getId()), core.unit.exists(marine.getId()),
                             removed.remove(), removed.exists());
                    try {
                        marine.getHp();
                    } catch (error) {
                        log.info(error.message === 'The unit ' + marine.getId() + ' does not exist.');
                    }
                    try {
                        core.unit.getHp(marine.getId());
                    } catch (error) {
                        log.info(error.name);
                    }
                }
                function onFrame() {
                    log.info('frame ' + marine.exists() + ' ' + removed.exists() + ' ' + game.findUnits({type: UnitType.TerranMarine}).length);
                }
                """);

        scenario.stepFrame();
        scenario.stepFrame();

        Assertions.assertEquals(List.of("true true kept true",
                                        "true false null false true false",
                                        "true",
                                        "RangeError",
                                        "frame false false 0",
                                        "frame false false 0"), scenario.getInfoMessages(), scenario.getLines()::toString);
    }

    @Test
    void units_take_the_doodad_resource_and_hangar_routines(@NotNull InstallationTestResources resources) throws IOException {
        LiveScriptGameScenario scenario = LiveScriptGameScenario.startMelee(resources.getGameData(), """
                var doodad;
                var frames = 0;
                function onStart() {
                    var human = game.getHumanPlayers()[0];
                    var commandCenter = game.findUnit({type: UnitType.TerranCommandCenter, owner: human});
                    var mineral = game.findUnit(function (unit) {
                        var type = unit.getType().getId();
                        return type >= 176 && type <= 178;
                    });
                    log.info(mineral.getResources() > 0, mineral.setResources(1234), mineral.getResources(), core.unit.getResources(mineral.getId()),
                             commandCenter.setResources(5));
                    var carrier = game.createUnit(UnitType.ProtossCarrier, human, commandCenter.getPosition());
                    log.info(carrier.getHangarCount(), carrier.addHangarCount(3), carrier.getHangarCount(), carrier.addHangarCount(10),
                             carrier.getHangarCount(), commandCenter.addHangarCount(1));
                    doodad = game.createUnit(UnitType.TerranMarine, human, commandCenter.getPosition());
                    log.info(doodad !== null, doodad.isDisabled());
                    doodad.disableDoodad();
                    try {
                        mineral.setResources(65536);
                    } catch (error) {
                        log.info(error.name);
                    }
                }
                function onFrame() {
                    frames++;
                    if (frames === 8) {
                        log.info(doodad.isDisabled());
                        doodad.enableDoodad();
                        log.info(doodad.isDisabled());
                    }
                }
                """);

        for (int frame = 0; frame < DOODAD_FRAMES; frame++) {
            scenario.stepFrame();
        }

        Assertions.assertEquals(List.of("true true 1234 1234 false",
                                        "0 true 3 true 4 false",
                                        "true false",
                                        "RangeError",
                                        "true",
                                        "false"), scenario.getInfoMessages(), scenario.getLines()::toString);
    }
}
