package org.fogofwarlabs.sc4j.screnhanced.script;

import org.fogofwarlabs.sc4j.scr.core.test.RequiresInstallation;
import org.fogofwarlabs.sc4j.scr.data.format.replay.ReplayFile;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrGamePlugin;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrInputModifiers;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrKeyInput;
import org.fogofwarlabs.sc4j.scr.game.replay.ReplayRuntime;
import org.fogofwarlabs.sc4j.scr.game.state.Game;
import org.fogofwarlabs.sc4j.scr.game.test.GameResourcesExtension;
import org.fogofwarlabs.sc4j.scr.game.test.InstallationTestResources;
import org.fogofwarlabs.sc4j.scr.game.test.RunningGameScenario;
import org.fogofwarlabs.sc4j.scr.game.trigger.TriggerContext;
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
 * decision with its refused trigger writes and its local display, the channel messages of shared code and of the local hooks that the
 * game's {@link ScriptInputPlugin} calls, live and in the replay, and a map without a script package.
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
    private static final int KEY_Q = 0x51;
    private static final int KEY_W = 0x57;
    /**
     * The switches the channel test reads: the 32 bits of the digest of the handler calls, then one switch per call.
     */
    private static final int CHANNEL_SWITCHES = 36;

    private static @NotNull ScriptInputPlugin findInputPlugin(@NotNull Game game) {
        for (ScrGamePlugin plugin : game.getPlugins()) {
            if (plugin instanceof ScriptInputPlugin inputPlugin) {
                return inputPlugin;
            }
        }

        return Assertions.fail("The game has no script input plugin.");
    }

    private static @NotNull String readSwitches(@NotNull Game game) {
        TriggerContext context = game.getTriggerContext();
        StringBuilder switches = new StringBuilder();

        Assertions.assertNotNull(context);

        for (int index = 0; index < CHANNEL_SWITCHES; index++) {
            switches.append(context.isSwitchSet(index) ? '1' : '0');
        }

        return switches.toString();
    }

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
    void trigger_writes_in_a_decision_throw_while_the_local_display_runs(@NotNull InstallationTestResources resources,
                                                                         @TempDir @NotNull Path folder) throws IOException {
        ScriptMapGameScenario scenario = ScriptMapGameScenario.startMelee(resources.getGameData(), folder, SPAWN_SCRIPT + """
                var refused = null;
                var displayed = false;
                var printed = false;
                function onBeforeDamage(damage) {
                    if (damage.getTarget() !== zergling || refused !== null) {
                        return;
                    }
                    var writes = [
                        function () { game.setSwitch(0, true); },
                        function () { game.setCountdownTimer(30); },
                        function () { human.setDeaths(UnitType.TerranMarine, 1); },
                        function () { human.setScore(ScoreType.Custom, 1); },
                        function () { computer.killUnits(UnitType.ZergDrone); },
                        function () { human.setAlliance(computer, Alliance.Ally); },
                        function () { computer.runAiScript('EnBk'); },
                        function () { human.setMissionObjectives('Objectives'); },
                        function () { game.showGreedLeaderboard(10); },
                        function () { zergling.disableDoodad(); }
                    ];
                    refused = writes.filter(function (write) {
                        try {
                            write();
                            return false;
                        } catch (error) {
                            return error.name === 'TypeError';
                        }
                    }).length + '/' + writes.length;
                    human.pingMinimap(marine.getPosition());
                    human.print('a decision text');
                    human.getDeaths(UnitType.TerranMarine);
                    human.countUnits(UnitType.TerranMarine);
                    displayed = true;
                }
                function onUnitDamaged(event) {
                    if (event.getUnit() === zergling && !printed) {
                        printed = true;
                        game.print('ES refused ' + refused + ' ' + displayed + ' ' + game.isSwitchSet(0) + ' ' + game.getCountdownTimer() + ' ' +
                                   human.getDeaths(UnitType.TerranMarine) + ' ' + computer.countUnits(UnitType.ZergDrone));
                    }
                }
                """);

        scenario.stepUntilPrinted("refused", FIGHT_FRAMES);

        Assertions.assertEquals(List.of("refused 10/10 true false 0 0 4"), scenario.getPrints());
    }

    @Test
    void channel_messages_of_shared_code_and_local_hooks_reach_the_handler_live_and_in_the_replay(@NotNull InstallationTestResources resources,
                                                                                                   @TempDir @NotNull Path folder) throws IOException {
        ScriptMapGameScenario scenario = ScriptMapGameScenario.startMelee(resources.getGameData(), folder, """
                var DIGEST_BITS = 32;
                var human;
                var digest = 0;
                var count = 0;
                var score = sync.createPlayerValue(0);
                var channel = sync.createChannel(function (player, value) {
                    var text = 'channel ' + (player === human) + ' ' + (typeof value === 'string' ? value : JSON.stringify(value)) + ' ' +
                               score.get(player);
                    game.print('ES ' + text);
                    for (var index = 0; index < text.length; index++) {
                        digest = (digest * 31 + text.charCodeAt(index)) | 0;
                    }
                    for (var bit = 0; bit < DIGEST_BITS; bit++) {
                        game.setSwitch(bit, (digest >>> bit & 1) === 1);
                    }
                    game.setSwitch(DIGEST_BITS + count++, true);
                });
                function onStart() {
                    human = game.getHumanPlayers()[0];
                    channel.send({start: [1, 'two', true, null]});
                    game.print('ES started');
                }
                function onLocalKeyDown(event) {
                    if (event.getKey() === Key.Q) {
                        score.set(7);
                        channel.send('key ' + event.isShift() + ' ' + event.getMousePosition());
                        event.consume();
                    }
                }
                function onLocalChat(event) {
                    if (event.getText().charAt(0) === '!') {
                        channel.send(event.getText());
                        event.consume();
                    }
                }
                """);

        scenario.stepUntilPrinted("channel", FIGHT_FRAMES);

        ScriptInputPlugin plugin = findInputPlugin(scenario.getGame());

        scenario.getGame().getInterfaceLink().getLocalView().setView(640, 320, 100, 50);
        Assertions.assertTrue(plugin.onKeyInput(new ScrKeyInput(KEY_Q, true, ScrInputModifiers.SHIFT)));
        Assertions.assertFalse(plugin.onKeyInput(new ScrKeyInput(KEY_W, true, 0)));
        Assertions.assertTrue(plugin.onChatInput("!go"));
        Assertions.assertFalse(plugin.onChatInput("hello"));

        scenario.stepUntilPrinted("channel true !go", FIGHT_FRAMES);

        List<String> expected = List.of("started",
                                        "channel true {\"start\":[1,\"two\",true,null]} 0",
                                        "channel true key true (740, 370) 7",
                                        "channel true !go 7");

        Assertions.assertEquals(expected, scenario.getPrints());
        Assertions.assertTrue(readSwitches(scenario.getGame()).endsWith("1110"), "one switch per handler call");

        int frames = scenario.getGame().getCurrentFrame();
        ReplayRuntime playback = RunningGameScenario.createRuntime(ReplayFile.parse(scenario.writeReplay()), resources.getGameData());
        Game game = playback.getGame();

        playback.start(0, 0);

        while (game.getCurrentFrame() < frames) {
            Assertions.assertTrue(playback.step(0, game.getCurrentFrame() * playback.getFrameDuration()));
        }

        Assertions.assertEquals(readSwitches(scenario.getGame()), readSwitches(game), "the replay calls the handler with the recorded commands");
        Assertions.assertFalse(findInputPlugin(game).onKeyInput(new ScrKeyInput(KEY_Q, true, 0)), "a replay calls no local hook");
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
