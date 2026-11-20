package org.fogofwarlabs.sc4j.screnhanced.script.game;

import org.fogofwarlabs.sc4j.scr.core.LcgRandom;
import org.fogofwarlabs.sc4j.scr.game.state.Game;
import org.fogofwarlabs.sc4j.scr.game.terrain.GameMap;
import org.fogofwarlabs.sc4j.screnhanced.graphics.ImageFileScenario;
import org.fogofwarlabs.sc4j.screnhanced.graphics.ImageGraphics;
import org.fogofwarlabs.sc4j.screnhanced.graphics.ImageReplacements;
import org.fogofwarlabs.sc4j.screnhanced.script.ScriptMapScenario;
import org.fogofwarlabs.sc4j.screnhanced.script.ScriptResources;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests {@code core.resources}, {@code core.graphics} and their upper layer ({@code resources}, {@code graphics}, {@code ImageType}) on a game
 * without game data: resource handles, their checks and errors, the image type replacements they set, and the read-only game.
 */
final class ScriptGraphicsCoreTest {
    private static final int MAP_TILE_SIZE = 4;
    private static final int MAP_TILESET = 0;
    private static final @NotNull String GRP_NAME = "units/marine.grp";
    private static final @NotNull String HD_ANIM_NAME = "units/marine.anim";
    private static final @NotNull String HD2_ANIM_NAME = "units/marine-hd2.anim";
    private static final int REPLACED_IMAGE_TYPE = 239;
    private static final int OTHER_IMAGE_TYPE = 240;
    /**
     * Defines {@code error(fn)}, which returns the name of the error {@code fn} throws, or {@code "none"}.
     */
    private static final @NotNull String ERROR_FUNCTION = """
            function error(fn) {
                try {
                    fn();
                    return 'none';
                } catch (e) {
                    return e.name;
                }
            }
            """;

    private static @NotNull Game createGame() {
        return Game.create(null, new GameMap(MAP_TILE_SIZE, MAP_TILE_SIZE, MAP_TILESET, new int[MAP_TILE_SIZE * MAP_TILE_SIZE]), new LcgRandom(0));
    }

    /**
     * Creates the resources of a GRP, an HD anim and an HD2 anim.
     */
    private static @NotNull ScriptResources createResources() throws IOException {
        Map<String, byte[]> files = new LinkedHashMap<>();

        files.put(GRP_NAME, ImageFileScenario.createGrp(0));
        files.put(HD_ANIM_NAME, ImageFileScenario.createAnim(ImageFileScenario.HD_SCALE));
        files.put(HD2_ANIM_NAME, ImageFileScenario.createAnim(ImageFileScenario.HD2_SCALE));

        return ScriptMapScenario.createResources(files);
    }

    @Test
    void resource_handles_are_checked_once_and_kept_per_name() throws IOException {
        ScriptGameScenario scenario = new ScriptGameScenario(createGame(), ERROR_FUNCTION + """
                var grp = resources.getGrp('units/marine.grp');
                log.info(grp === resources.getGrp('units/marine.grp'), grp.getName(), String(grp), resources.has('units/marine.grp'),
                         resources.has('units/none.grp'), grp.getId() === resources.getAnim('units/marine.anim').getId());
                log.info(error(function () { resources.getGrp('units/none.grp'); }), error(function () { resources.getGrp('units/marine.anim'); }),
                         error(function () { resources.getAnim('units/marine.grp'); }), error(function () { core.resources.getGrp(1); }));
                """, createResources());

        scenario.getRuntime().start();

        Assertions.assertEquals(List.of("true units/marine.grp units/marine.grp true false false", "Error Error Error TypeError"),
                                scenario.getInfoMessages(), scenario.getLines()::toString);
    }

    @Test
    void replace_image_sets_the_graphics_of_an_image_type() throws IOException {
        ScriptGameScenario scenario = new ScriptGameScenario(createGame(), ERROR_FUNCTION + """
                var grp = resources.getGrp('units/marine.grp');
                var hd = resources.getAnim('units/marine.anim');
                var hd2 = resources.getAnim('units/marine-hd2.anim');
                graphics.replaceImage(ImageType.get(239), {sd: grp, hd: hd, hd2: hd2});
                graphics.replaceImage(ImageType.get(240), {hd2: hd2});
                graphics.restoreImage(ImageType.get(240));
                log.info(error(function () { graphics.replaceImage(ImageType.get(240), {hd: hd2}); }),
                         error(function () { graphics.replaceImage(ImageType.get(240), {}); }),
                         error(function () { graphics.replaceImage(ImageType.get(240), {sd: hd}); }),
                         error(function () { graphics.replaceImage(239, {sd: grp}); }),
                         error(function () { core.graphics.replaceImage(240, 99, -1, -1); }));
                log.info(ImageType.getCount(), ImageType.get(211) === ImageType.Image211, ImageType.Image239 === ImageType.get(239),
                         ImageType.get(239).getName());
                """, createResources());

        scenario.getRuntime().start();

        ImageReplacements replacements = scenario.getRuntime().getImageReplacements();
        ImageGraphics replaced = replacements.findImageGraphics(REPLACED_IMAGE_TYPE);

        Assertions.assertEquals(List.of("RangeError RangeError TypeError TypeError RangeError", "999 true true Image239"),
                                scenario.getInfoMessages(), scenario.getLines()::toString);
        Assertions.assertNotNull(replaced);
        Assertions.assertNotNull(replaced.getGrp());
        Assertions.assertEquals(GRP_NAME, replaced.getGrp().getName());
        Assertions.assertNotNull(replaced.getHdAnim());
        Assertions.assertNotNull(replaced.getHd2Anim());
        Assertions.assertEquals(HD2_ANIM_NAME, replaced.getHd2Anim().getName());
        Assertions.assertNull(replacements.findImageGraphics(OTHER_IMAGE_TYPE));
    }

    @Test
    void graphics_changes_throw_while_the_game_is_read_only() throws IOException {
        ScriptGameScenario scenario = new ScriptGameScenario(createGame(), ERROR_FUNCTION + """
                function onFrame() {
                    var grp = resources.getGrp('units/marine.grp');
                    log.info(error(function () { graphics.replaceImage(ImageType.get(239), {sd: grp}); }),
                             error(function () { graphics.restoreImage(ImageType.get(239)); }), grp.getName());
                }
                """, createResources());

        scenario.getRuntime().start();
        scenario.getRuntime().setReadOnly(true);
        scenario.getRuntime().runFrame(0);

        Assertions.assertEquals(List.of("TypeError TypeError units/marine.grp"), scenario.getInfoMessages(), scenario.getLines()::toString);
        Assertions.assertNull(scenario.getRuntime().getImageReplacements().findImageGraphics(REPLACED_IMAGE_TYPE));
    }
}
