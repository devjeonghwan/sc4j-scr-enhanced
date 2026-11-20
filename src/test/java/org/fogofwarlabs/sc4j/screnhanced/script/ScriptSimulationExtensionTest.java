package org.fogofwarlabs.sc4j.screnhanced.script;

import org.fogofwarlabs.sc4j.scr.core.test.RequiresInstallation;
import org.fogofwarlabs.sc4j.scr.data.format.replay.ReplayFile;
import org.fogofwarlabs.sc4j.scr.game.replay.ReplayRuntime;
import org.fogofwarlabs.sc4j.scr.game.state.Game;
import org.fogofwarlabs.sc4j.scr.game.test.GameResourcesExtension;
import org.fogofwarlabs.sc4j.scr.game.test.InstallationTestResources;
import org.fogofwarlabs.sc4j.scr.game.test.RunningGameScenario;
import org.fogofwarlabs.sc4j.screnhanced.graphics.ImageFileScenario;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Tests which games {@link ScriptSimulationExtension} gives a script plugin and the resources it gets: a live game reads them from its map and
 * stores them in its replay, and the replay played back reads them from there.
 */
@RequiresInstallation
@ExtendWith(GameResourcesExtension.class)
final class ScriptSimulationExtensionTest {
    /**
     * The script: at its start it replaces the image {@value #REPLACED_IMAGE_TYPE} with a GRP of the resources and prints whether it has the
     * GRP.
     */
    private static final @NotNull String SCRIPT = """
            function onStart() {
                graphics.replaceImage(ImageType.get(239), {sd: resources.getGrp('units/marine.grp')});
                game.print('ES started ' + resources.has('units/marine.grp'));
            }
            """;
    private static final int REPLACED_IMAGE_TYPE = 239;
    private static final int PLAYED_FRAMES = 8;

    @Test
    void a_replay_of_a_live_script_game_carries_the_resources(@NotNull InstallationTestResources resources,
                                                              @TempDir @NotNull Path folder) throws IOException {
        Map<String, byte[]> files = ScriptGraphicsTest.createResources();
        ScriptMapGameScenario scenario = ScriptMapGameScenario.startMelee(resources.getGameData(), folder, SCRIPT, files);

        scenario.stepFrames(PLAYED_FRAMES);

        ScriptSimulationPlugin livePlugin = ScriptMapGameScenario.findPlugin(scenario.getGame());
        byte[] section = scenario.getGame().getReplaySections().get(ScriptStorageUtils.RESOURCE_REPLAY_SECTION);

        Assertions.assertEquals(List.of("started true"), scenario.getPrints());
        Assertions.assertNotNull(livePlugin);
        Assertions.assertNotNull(livePlugin.getImageReplacements().findImageGraphics(REPLACED_IMAGE_TYPE));
        Assertions.assertNotNull(section, "the live game stores its resource file");

        ReplayFile replay = ReplayFile.parse(scenario.writeReplay());
        byte[] replaySection = replay.readModSection(ScriptStorageUtils.RESOURCE_REPLAY_SECTION);
        ScriptResources.Entry grp = ScriptStorageUtils.readResources(section, ScriptStorageUtils.RESOURCE_FILE_NAME).findEntry("units/marine.grp");

        Assertions.assertArrayEquals(section, replaySection);
        Assertions.assertNotNull(grp);
        Assertions.assertArrayEquals(ImageFileScenario.createGrp(0), Arrays.copyOfRange(section, grp.getOffset(), grp.getOffset() + grp.getSize()));

        ReplayRuntime playback = RunningGameScenario.createRuntime(replay, resources.getGameData());
        Game game = playback.getGame();
        ScriptSimulationPlugin plugin = ScriptMapGameScenario.findPlugin(game);

        Assertions.assertNotNull(plugin, "the replay's scenario holds the script");

        playback.start(0, 0);

        while (game.getCurrentFrame() < PLAYED_FRAMES) {
            Assertions.assertTrue(playback.step(0, game.getCurrentFrame() * playback.getFrameDuration()));
        }

        Assertions.assertFalse(plugin.getRuntime().isStopped());
        Assertions.assertNotNull(plugin.getImageReplacements().findImageGraphics(REPLACED_IMAGE_TYPE), "the replay's script reads the GRP");
    }
}
