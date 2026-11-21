package org.fogofwarlabs.sc4j.screnhanced.script.core;

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
 * Tests {@code core.vision}, the sight functions of {@code core.unit} and {@code core.player} and the {@code player.vision} and
 * {@code game.vision} groups of the prelude in a live melee game: the tiles and units each player sees, a reveal through the native sight
 * routine, the shared vision of the relations and the sight coverage, the shared and local code checks, and the same game state from the same
 * writes.
 */
@RequiresInstallation
@ExtendWith(GameResourcesExtension.class)
final class ScriptVisionCoreTest {
    /**
     * The frames the shared vision test runs: more than the hundred frames after which the game marks every tile unseen and the units' sight
     * reveals the tiles to the players they share it with.
     */
    private static final int VISIBILITY_RESET_FRAMES = 110;
    /**
     * The frames the determinism test runs after its start.
     */
    private static final int CHANGE_FRAMES = 4;
    private static final @NotNull String CHANGES = """
            function onStart() {
                var human = game.players.getHumans()[0];
                var hatchery = game.units.findOne({type: UnitType.ZergHatchery});
                game.vision.reveal(human, hatchery.getPosition(), 10, {air: true});
                human.vision.shareWith(hatchery.getOwner(), true);
            }
            """;

    @Test
    void players_see_their_own_units_and_a_reveal(@NotNull InstallationTestResources resources) throws IOException {
        LiveScriptGameScenario scenario = LiveScriptGameScenario.startMelee(resources.getGameData(), """
                var human;
                var hatchery;
                function onStart() {
                    human = game.players.getHumans()[0];
                    var commandCenter = game.units.findOne({type: UnitType.TerranCommandCenter, owner: human});
                    hatchery = game.units.findOne({type: UnitType.ZergHatchery});
                    var own = commandCenter.getPosition();
                    var other = hatchery.getPosition();
                    var tileX = Math.floor(own.getX() / 32);
                    var tileY = Math.floor(own.getY() / 32);
                    var bit = 1 << human.getId();
                    log.info(human.vision.canSee(own), human.vision.hasExplored(own), human.vision.canSee(other), human.vision.hasExplored(other),
                             core.vision.isVisible(human.getId(), own.getX(), own.getY()), (core.vision.getTileVisibility(tileX, tileY) & bit) === 0,
                             (core.terrain.getTileFlags(tileX, tileY) & 0xFFFF) === core.vision.getTileVisibility(tileX, tileY));
                    log.info(commandCenter.isVisibleTo(human), commandCenter.isDetectedBy(human), hatchery.isVisibleTo(human),
                             (core.unit.getVisibilityFlags(commandCenter.getId()) & bit) !== 0, commandCenter.getVisibleTo().indexOf(human) >= 0,
                             core.unit.getDetectionFlags(commandCenter.getId()) >= 0);
                    game.vision.reveal(human, other, 10, {air: true});
                    log.info(human.vision.canSee(other), human.vision.hasExplored(other));
                    try {
                        core.vision.reveal(0x100, 0, 0, 1, true);
                    } catch (error) {
                        log.info(error.name);
                    }
                    try {
                        core.vision.reveal(1, 0, 0, 12, true);
                    } catch (error) {
                        log.info(error.name);
                    }
                }
                function onFrame() {
                    log.info('frame ' + hatchery.isVisibleTo(human));
                }
                function onLocalFrame() {
                    log.info('local ' + human.vision.canSee(hatchery.getPosition()));
                    try {
                        game.vision.reveal(human, hatchery.getPosition(), 10);
                    } catch (error) {
                        log.info('local ' + error.name);
                    }
                }
                """);

        scenario.stepFrame();
        scenario.runLocalFrame();

        Assertions.assertEquals(List.of("true true false false true true true",
                                        "true true false true true true",
                                        "true true",
                                        "RangeError",
                                        "RangeError",
                                        "frame false",
                                        "frame true",
                                        "local true",
                                        "local TypeError"), scenario.getInfoMessages(), scenario.getLines()::toString);
    }

    @Test
    void shared_vision_sets_the_relations_and_the_sight_coverage(@NotNull InstallationTestResources resources) throws IOException {
        LiveScriptGameScenario scenario = LiveScriptGameScenario.startMelee(resources.getGameData(), """
                var human;
                var computer;
                function ids(players) {
                    return players.map(function (player) { return player.getId(); }).join(',');
                }
                function onStart() {
                    human = game.players.getHumans()[0];
                    computer = game.units.findOne({type: UnitType.ZergHatchery}).getOwner();
                    log.info(ids(human.vision.getSharedWith()) === String(human.getId()), core.player.getSharedVision(human.getId()) === 1 << human.getId(),
                             human.vision.isSharedWith(computer));
                    human.vision.shareWith(computer, true);
                    log.info(ids(human.vision.getSharedWith()) === ids([human, computer].sort(function (a, b) { return a.getId() - b.getId(); })),
                             human.vision.isSharedWith(computer), computer.vision.isSharedWith(human));
                    computer.vision.setSharedWith([computer, human]);
                    log.info(computer.vision.isSharedWith(human));
                }
                var seen = false;
                function onFrame() {
                    seen = seen || game.units.findOne({type: UnitType.ZergHatchery}).isVisibleTo(human);
                }
                function onLocalFrame() {
                    log.info('local seen ' + seen);
                    try {
                        human.vision.shareWith(computer, false);
                    } catch (error) {
                        log.info('local ' + error.name + ' ' + human.vision.isSharedWith(computer));
                    }
                }
                """);

        for (int frame = 0; frame < VISIBILITY_RESET_FRAMES; frame++) {
            scenario.stepFrame();
        }

        scenario.runLocalFrame();

        Assertions.assertEquals(List.of("true true false",
                                        "true true false",
                                        "true",
                                        "local seen true",
                                        "local TypeError true"), scenario.getInfoMessages(), scenario.getLines()::toString);
    }

    @Test
    void the_same_reveal_and_shared_vision_give_the_same_game_state(@NotNull InstallationTestResources resources) throws IOException {
        long first = runChanges(resources, CHANGES);
        long second = runChanges(resources, CHANGES);
        long unchanged = runChanges(resources, "");

        Assertions.assertEquals(first, second);
        Assertions.assertNotEquals(first, unchanged);
    }

    /**
     * Runs a melee game with a script for {@link #CHANGE_FRAMES} frames.
     *
     * @return the state hash of the game at the end
     */
    private static long runChanges(@NotNull InstallationTestResources resources, @NotNull String source) throws IOException {
        LiveScriptGameScenario scenario = LiveScriptGameScenario.startMelee(resources.getGameData(), source);

        for (int frame = 0; frame < CHANGE_FRAMES; frame++) {
            scenario.stepFrame();
        }

        return scenario.getGame().computeStateHash();
    }
}
