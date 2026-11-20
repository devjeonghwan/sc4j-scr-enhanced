package org.fogofwarlabs.sc4j.screnhanced.script;

import org.fogofwarlabs.sc4j.scr.core.test.RequiresInstallation;
import org.fogofwarlabs.sc4j.scr.game.test.GameResourcesExtension;
import org.fogofwarlabs.sc4j.scr.game.test.InstallationTestResources;
import org.fogofwarlabs.sc4j.screnhanced.script.runtime.ScriptRuntime;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Tests the script of a map with a script package in a live melee game, run by the game's {@link ScriptSimulationPlugin}: the start and
 * frame hooks, the event hooks with their captured values, the decision hooks that change damage and reject orders, the read-only game in a
 * decision, and a map without a script package.
 */
@RequiresInstallation
@ExtendWith(GameResourcesExtension.class)
final class ScriptSimulationPluginTest {
    /**
     * Defines {@code spawn()}, which creates a Marine of the human player a little toward the map center from the human's Command Center and,
     * unless {@code withoutZergling} is set, a Zergling of the computer player next to the Marine; {@code onFrame} calls it once the
     * Command Center exists, then {@code afterSpawn(frame)} if the script has it.
     */
    private static final @NotNull String SPAWN_SCRIPT = """
            var human, computer, marine, zergling, spawnFrame, withoutZergling;
            function spawn() {
                human = game.getHumanPlayers()[0];
                computer = game.findUnit({type: UnitType.ZergHatchery}).getOwner();
                var commandCenter = game.findUnit({type: UnitType.TerranCommandCenter, owner: human});
                var directionX = commandCenter.getX() < game.getMapWidth() / 2 ? 1 : -1;
                var directionY = commandCenter.getY() < game.getMapHeight() / 2 ? 1 : -1;
                marine = game.createUnit(UnitType.TerranMarine, human,
                                         Position.of(commandCenter.getX() + 160 * directionX, commandCenter.getY() + 160 * directionY));
                if (!withoutZergling) {
                    zergling = game.createUnit(UnitType.ZergZergling, computer, Position.of(marine.getX() + 48 * directionX, marine.getY()));
                }
            }
            function onFrame(frame) {
                if (spawnFrame === undefined && game.findUnit({type: UnitType.TerranCommandCenter}) !== null) {
                    spawnFrame = frame;
                    spawn();
                }
                if (spawnFrame !== undefined && typeof afterSpawn === 'function') {
                    afterSpawn(frame);
                }
            }
            """;
    private static final int FIGHT_FRAMES = 400;

    @Test
    void a_map_script_starts_runs_its_frames_and_receives_the_start_units(@NotNull InstallationTestResources resources,
                                                                           @TempDir @NotNull Path folder) throws IOException {
        ScriptMapGameScenario scenario = ScriptMapGameScenario.startMelee(resources.getGameData(), folder, """
                var frames = [];
                var commandCenter = null;
                function onStart() {
                    frames.push('start ' + game.getFrame());
                }
                function onUnitCreated(event) {
                    if (event.getUnitType() === UnitType.TerranCommandCenter) {
                        commandCenter = event;
                    }
                }
                function onFrame(frame) {
                    frames.push(frame);
                    if (frames.length === 4) {
                        var unit = commandCenter.getUnit();
                        game.print('ES frames ' + frames.join(' ') + ', center ' + (unit === game.findUnit({type: UnitType.TerranCommandCenter})) +
                                   ' ' + unit.exists() + ' ' + commandCenter.getOwner().isHuman() + ' ' + commandCenter.getFrame());
                    }
                }
                """);

        scenario.stepFrames(4);

        ScriptSimulationPlugin plugin = ScriptMapGameScenario.findPlugin(scenario.getGame());

        Assertions.assertNotNull(plugin);

        ScriptRuntime runtime = plugin.getRuntime();

        Assertions.assertFalse(runtime.isStopped());
        Assertions.assertFalse(scenario.getGame().isSaveAllowed(), "a script game cannot be saved");
        Assertions.assertEquals(List.of("frames start 1 1 2 3, center true true true 0"), scenario.getPrints());
    }

    @Test
    void on_unit_died_receives_the_killer_and_the_weapon(@NotNull InstallationTestResources resources,
                                                         @TempDir @NotNull Path folder) throws IOException {
        ScriptMapGameScenario scenario = ScriptMapGameScenario.startMelee(resources.getGameData(), folder, SPAWN_SCRIPT + """
                var victim;
                function afterSpawn(frame) {
                    if (frame === spawnFrame) {
                        zergling.setHp(1);
                        victim = game.createUnit(UnitType.TerranMarine, human, marine.getPosition());
                        victim.kill();
                    }
                }
                function onUnitDied(event) {
                    if (event.getUnit() === zergling) {
                        game.print('ES zergling ' + (event.getKiller() === marine) + ' ' + (event.getKillerPlayer() === human) + ' ' +
                                   event.getWeapon() + ' ' + event.getUnitType() + ' ' + (event.getOwner() === computer) + ' ' +
                                   event.getUnit().exists());
                    }
                    if (event.getUnit() === victim) {
                        game.print('ES victim ' + event.getKiller() + ' ' + event.getKillerPlayer() + ' ' + event.getWeapon() + ' ' +
                                   victim.exists());
                    }
                }
                """);

        scenario.stepUntilPrinted("zergling", FIGHT_FRAMES);

        Assertions.assertEquals(List.of("victim null null null false", "zergling true true Gauss Rifle Zerg Zergling true false"),
                                scenario.getPrints());
    }

    @Test
    void on_before_damage_doubles_the_damage_of_a_weapon_hit(@NotNull InstallationTestResources resources,
                                                             @TempDir @NotNull Path folder) throws IOException {
        ScriptMapGameScenario scenario = ScriptMapGameScenario.startMelee(resources.getGameData(), folder, SPAWN_SCRIPT + """
                var printed = false;
                function onBeforeDamage(damage) {
                    if (damage.getTarget() === zergling && damage.getWeapon() === WeaponType.GaussRifle) {
                        damage.setAmount(damage.getAmount() * 2);
                    }
                }
                function onUnitDamaged(event) {
                    if (event.getUnit() === zergling && !printed) {
                        printed = true;
                        game.print('ES damaged ' + event.getAmount() + ' ' + (event.getSource() === marine) + ' ' + zergling.getHp());
                    }
                }
                """);

        scenario.stepUntilPrinted("damaged", FIGHT_FRAMES);

        Assertions.assertEquals(List.of("damaged 12 true 23"), scenario.getPrints());
    }

    @Test
    void on_before_damage_cancels_every_weapon_hit(@NotNull InstallationTestResources resources,
                                                   @TempDir @NotNull Path folder) throws IOException {
        ScriptMapGameScenario scenario = ScriptMapGameScenario.startMelee(resources.getGameData(), folder, SPAWN_SCRIPT + """
                var cancelled = 0;
                function onBeforeDamage(damage) {
                    damage.cancel();
                    cancelled++;
                }
                function afterSpawn(frame) {
                    if (frame === spawnFrame + 200) {
                        game.print('ES hit points ' + marine.getHp() + ' ' + zergling.getHp() + ' ' + (cancelled > 1));
                    }
                }
                """);

        scenario.stepUntilPrinted("hit points", FIGHT_FRAMES);

        Assertions.assertEquals(List.of("hit points 40 35 true"), scenario.getPrints());
    }

    @Test
    void on_before_order_rejects_an_order(@NotNull InstallationTestResources resources, @TempDir @NotNull Path folder) throws IOException {
        ScriptMapGameScenario scenario = ScriptMapGameScenario.startMelee(resources.getGameData(), folder, SPAWN_SCRIPT + """
                withoutZergling = true;
                var moved = false;
                function onBeforeOrder(order) {
                    if (order.getUnit() === marine && order.getOrder() === Order.Move) {
                        order.reject();
                    }
                }
                function onOrderIssued(event) {
                    if (event.getUnit() === marine && event.getOrder() === Order.Move) {
                        moved = true;
                    }
                }
                function afterSpawn(frame) {
                    if (frame === spawnFrame) {
                        marine.issueOrder(Order.Move, Position.of(marine.getX() + 64, marine.getY()));
                        game.print('ES order ' + (marine.getOrder() === Order.Move));
                    } else if (frame === spawnFrame + 2) {
                        game.print('ES issued ' + moved);
                    }
                }
                """);

        scenario.stepUntilPrinted("issued", FIGHT_FRAMES);

        Assertions.assertEquals(List.of("order false", "issued false"), scenario.getPrints());
    }

    @Test
    void a_state_change_in_a_decision_throws_and_leaves_the_decision(@NotNull InstallationTestResources resources,
                                                                     @TempDir @NotNull Path folder) throws IOException {
        ScriptMapGameScenario scenario = ScriptMapGameScenario.startMelee(resources.getGameData(), folder, SPAWN_SCRIPT + """
                var errors = [];
                var printed = false;
                function onBeforeDamage(damage) {
                    if (damage.getTarget() !== zergling) {
                        return;
                    }
                    try {
                        zergling.setHp(1);
                    } catch (error) {
                        errors.push(error.name + ' ' + game.isReadOnly());
                    }
                    damage.setAmount(0);
                    zergling.kill();
                }
                function onUnitDamaged(event) {
                    if (event.getUnit() === zergling && !printed) {
                        printed = true;
                        game.print('ES damaged ' + event.getAmount() + ' ' + errors[0] + ' ' + game.isReadOnly() + ' ' + zergling.getHp());
                    }
                }
                """);

        scenario.stepUntilPrinted("damaged", FIGHT_FRAMES);

        Assertions.assertEquals(List.of("damaged 6 TypeError true false 29"), scenario.getPrints());
    }

    @Test
    void a_map_without_a_script_package_gets_no_plugin(@NotNull InstallationTestResources resources,
                                                       @TempDir @NotNull Path folder) throws IOException {
        ScriptMapGameScenario scenario = ScriptMapGameScenario.startMelee(resources.getGameData(), folder, null);

        scenario.stepFrames(3);

        Assertions.assertNull(ScriptMapGameScenario.findPlugin(scenario.getGame()));
        Assertions.assertNull(scenario.getGame().getSimulationPlugins(), "a map without a script package gets no simulation plugin");
        Assertions.assertEquals(List.of(), scenario.getPrints());
    }
}
