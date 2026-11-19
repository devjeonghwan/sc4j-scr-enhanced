package org.fogofwarlabs.sc4j.screnhanced;

import org.fogofwarlabs.sc4j.engine.resource.graphics.SpriteFrame;
import org.fogofwarlabs.sc4j.engine.resource.graphics.SpriteSheet;
import org.fogofwarlabs.sc4j.scr.content.ScrRaceIds;
import org.fogofwarlabs.sc4j.scr.core.test.ExternalInput;
import org.fogofwarlabs.sc4j.scr.core.test.RequiresInstallation;
import org.fogofwarlabs.sc4j.scr.data.format.mpq.MpqArchive;
import org.fogofwarlabs.sc4j.scr.data.resource.ScrImageSheet;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrConsolePlacement;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrGamePlugin;
import org.fogofwarlabs.sc4j.scr.game.frame.GameSpeed;
import org.fogofwarlabs.sc4j.scr.game.replay.ReplayRuntime;
import org.fogofwarlabs.sc4j.scr.game.session.LiveGame;
import org.fogofwarlabs.sc4j.scr.game.session.setup.MeleeGameSetup;
import org.fogofwarlabs.sc4j.scr.game.sprite.SpriteImagePool;
import org.fogofwarlabs.sc4j.scr.game.state.GameData;
import org.fogofwarlabs.sc4j.scr.game.test.LiveMapGameScenario;
import org.fogofwarlabs.sc4j.scr.game.trigger.TriggerContext;
import org.fogofwarlabs.sc4j.screnhanced.graphics.ClassicConsoleGraphics;
import org.fogofwarlabs.sc4j.screnhanced.graphics.ClassicImageGraphics;
import org.fogofwarlabs.sc4j.screnhanced.graphics.ClassicTerrainGraphics;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Runs a map that the system property {@value #MAP_PROPERTY} names (a local check; the map stays outside the repository) with the classic memory
 * and graphics of SC:R Enhanced: the game runs its frames without an access failure, draws sprite images from GRPs in game memory, and the
 * console and terrain changes of the map are found: the written console transparency, resource icons with every icon frame if written, and
 * changed megatile pictures. With the system property {@value #OUTPUT_PROPERTY} it writes the first non-empty frame of every drawn sheet there
 * as a grayscale PNG of the palette indices.
 */
@RequiresInstallation
@EnabledIfSystemProperty(named = ClassicMapCheckTest.MAP_PROPERTY, matches = ".+")
final class ClassicMapCheckTest {
    /**
     * The system property that names the map.
     */
    static final @NotNull String MAP_PROPERTY = "sc4j.screnhanced.classicMap";
    /**
     * The system property that names the folder of the written sheets.
     */
    static final @NotNull String OUTPUT_PROPERTY = "sc4j.screnhanced.classicMapOutput";

    private static final @NotNull String SCENARIO_PATH = "staredit\\scenario.chk";
    private static final @NotNull String PLAYER_NAME = "tester";
    private static final int SEED = 0x1234;
    private static final int FRAMES = 600;
    /**
     * The number of resource icon frames the console draws: minerals, three gases and three supplies.
     */
    private static final int RESOURCE_ICON_FRAMES = 7;
    /**
     * A megatile whose references the checked map writes.
     */
    private static final int CHANGED_MEGATILE = 1;

    private static void writeSheet(@NotNull ScrImageSheet sheet, @NotNull Path file) throws IOException {
        int index = 0;

        while (index < sheet.getFrameCount() - 1 && isEmpty(sheet.getFrame(index).getFrame(0))) {
            index++;
        }

        SpriteSheet frameSheet = sheet.getFrame(index);
        SpriteFrame frame = frameSheet.getFrame(0);

        if (isEmpty(frame)) {
            return;
        }

        byte[] pixels = new byte[frame.getWidth() * frame.getHeight()];
        BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_BYTE_GRAY);

        frameSheet.getColor().copyBytes(frame.getPixelOffset(), pixels, 0, pixels.length);
        image.getRaster().setDataElements(0, 0, frame.getWidth(), frame.getHeight(), pixels);
        ImageIO.write(image, "png", file.toFile());
    }

    private static boolean isEmpty(@NotNull SpriteFrame frame) {
        return frame.getWidth() == 0 || frame.getHeight() == 0;
    }

    @Test
    void the_map_runs_and_draws_images_from_grps_in_game_memory() throws IOException {
        Path mapPath = Path.of(System.getProperty(MAP_PROPERTY));
        byte[] mapData = MpqArchive.open(mapPath).readFile(SCENARIO_PATH);
        MeleeGameSetup setup = MeleeGameSetup.createUseMapSettings(mapData, PLAYER_NAME, ScrRaceIds.TERRAN, 0, GameSpeed.FASTEST);

        try (GameData gameData = GameData.loadFrom(ExternalInput.INSTALLATION.getPath())) {
            LiveGame live = new LiveGame(setup, SEED);
            ReplayRuntime runtime = LiveMapGameScenario.startLive(gameData, live, mapPath);

            while (runtime.getGame().getCurrentFrame() < FRAMES) {
                LiveMapGameScenario.stepFrame(runtime, live);

                Assertions.assertNull(runtime.getFailure());

                TriggerContext triggers = runtime.getGame().getTriggerContext();

                Assertions.assertFalse(triggers.hasEudFailure(),
                                       () -> "EUD access failure at " + Integer.toHexString(triggers.getEudFailureAddress()));
            }

            ClassicImageGraphics graphics = null;

            for (ScrGamePlugin plugin : runtime.getGame().getPlugins()) {
                if (plugin instanceof ClassicImageGraphics candidate) {
                    graphics = candidate;
                }
            }

            Assertions.assertNotNull(graphics);

            SpriteImagePool pool = runtime.getGame().getSpriteSystem().getImagePool();
            Map<ScrImageSheet, Integer> sheets = new IdentityHashMap<>();

            for (int slot = 0; slot < pool.getCapacity(); slot++) {
                ScrImageSheet sheet = pool.isAllocated(slot) ? graphics.findImageSheet(pool.getImage(slot)) : null;

                if (sheet != null) {
                    sheets.putIfAbsent(sheet, slot);
                }
            }

            Assertions.assertFalse(sheets.isEmpty(), "no image draws a GRP from game memory");

            for (ScrGamePlugin plugin : runtime.getGame().getPlugins()) {
                if (plugin instanceof ClassicConsoleGraphics console) {
                    ScrConsolePlacement placement = console.findConsolePlacement();

                    Assertions.assertNotNull(placement, "no console dialog or transparency was written");
                    Assertions.assertTrue(placement.isMasked(), "the console transparency was not written");

                    SpriteSheet icons = console.findResourceIcons();

                    Assertions.assertTrue(icons == null || icons.getFrameCount() >= RESOURCE_ICON_FRAMES, "the written resource icons lack frames");
                }

                if (plugin instanceof ClassicTerrainGraphics terrain) {
                    Assertions.assertNotNull(terrain.findTerrainTile(CHANGED_MEGATILE), "the written megatile keeps its installed picture");
                }
            }

            String output = System.getProperty(OUTPUT_PROPERTY, "");

            if (!output.isBlank()) {
                Files.createDirectories(Path.of(output));

                for (Map.Entry<ScrImageSheet, Integer> entry : sheets.entrySet()) {
                    writeSheet(entry.getKey(), Path.of(output, "slot-" + entry.getValue() + ".png"));
                }
            }
        }
    }
}
