package org.fogofwarlabs.sc4j.screnhanced.script;

import org.fogofwarlabs.sc4j.scr.data.format.chk.ChkFile;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Tests the pack and unpack commands of {@link ScriptTool} on map files and script folders.
 */
final class ScriptToolTest {
    private static final byte @NotNull [] VERSION_PAYLOAD = {(byte) 0xCD, 0x00};
    private static final @NotNull Map<String, String> SCRIPT_FOLDER = Map.of(
            ScriptManifest.FILE_NAME, "{\n  \"name\": \"Round Trip\",\n  \"main\": \"main.js\",\n  \"api\": \"1\"\n}\n",
            "main.js", "const util = require('lib/util.js');\nfunction onStart() {\n    util.greet();\n}\n",
            "lib/util.js", "exports.greet = function () {\n    game.print('안녕');\n};\n",
            "resources/images/marine.grp", "GRP data",
            "resources/sounds/hit.wav", "RIFF data"
    );

    private static int runTool(@NotNull String @NotNull ... arguments) {
        return ScriptTool.run(arguments, new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8),
                              new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
    }

    private static @NotNull Path writeMap(@NotNull Path folder) throws IOException {
        Path map = folder.resolve("map.scx");

        Files.write(map, ScriptMapScenario.createMap(ScriptMapScenario.createSection(ChkFile.VERSION_SECTION, VERSION_PAYLOAD)));

        return map;
    }

    @Test
    void pack_then_unpack_gives_the_same_folder(@TempDir @NotNull Path folder) throws IOException {
        Path map = writeMap(folder);
        Path scriptFolder = folder.resolve("script");
        Path packedMap = folder.resolve("packed.scx");
        Path unpackedFolder = folder.resolve("unpacked");

        ScriptMapScenario.writeFolder(scriptFolder, SCRIPT_FOLDER);

        Assertions.assertEquals(0, runTool(ScriptTool.PACK_COMMAND, map.toString(), ScriptTool.SCRIPT_OPTION, scriptFolder.toString(),
                                           ScriptTool.OUTPUT_OPTION, packedMap.toString()));
        Assertions.assertEquals(0, runTool(ScriptTool.UNPACK_COMMAND, packedMap.toString(), ScriptTool.OUTPUT_OPTION, unpackedFolder.toString()));
        Assertions.assertEquals(ScriptMapScenario.readFolder(scriptFolder), ScriptMapScenario.readFolder(unpackedFolder));
    }

    @Test
    void unpack_should_fail_when_the_map_has_no_script_package(@TempDir @NotNull Path folder) throws IOException {
        Path map = writeMap(folder);

        Assertions.assertEquals(1, runTool(ScriptTool.UNPACK_COMMAND, map.toString(), ScriptTool.OUTPUT_OPTION, folder.resolve("out").toString()));
        Assertions.assertFalse(Files.exists(folder.resolve("out")));
    }

    @Test
    void unpack_should_fail_when_the_folder_is_not_empty(@TempDir @NotNull Path folder) throws IOException {
        Path map = writeMap(folder);
        Path scriptFolder = folder.resolve("script");
        Path packedMap = folder.resolve("packed.scx");

        ScriptMapScenario.writeFolder(scriptFolder, SCRIPT_FOLDER);

        Assertions.assertEquals(0, runTool(ScriptTool.PACK_COMMAND, map.toString(), ScriptTool.SCRIPT_OPTION, scriptFolder.toString(),
                                           ScriptTool.OUTPUT_OPTION, packedMap.toString()));
        Assertions.assertEquals(1, runTool(ScriptTool.UNPACK_COMMAND, packedMap.toString(), ScriptTool.OUTPUT_OPTION, scriptFolder.toString()));
    }

    @Test
    void run_should_fail_when_an_option_is_missing_or_unknown(@TempDir @NotNull Path folder) throws IOException {
        Path map = writeMap(folder);

        Assertions.assertEquals(1, runTool(ScriptTool.PACK_COMMAND, map.toString(), ScriptTool.SCRIPT_OPTION, folder.toString()));
        Assertions.assertEquals(1, runTool(ScriptTool.UNPACK_COMMAND, map.toString(), "--output", folder.toString()));
        Assertions.assertEquals(1, runTool("verify", map.toString()));
        Assertions.assertEquals(1, runTool(ScriptTool.RUN_COMMAND, map.toString(), ScriptTool.WATCH_OPTION));
        Assertions.assertEquals(1, runTool(ScriptTool.RUN_COMMAND, map.toString(), ScriptTool.SCRIPT_OPTION, folder.toString(),
                                           ScriptTool.MELEE_OPTION, ScriptTool.MELEE_OPTION));
        Assertions.assertEquals(1, runTool(ScriptTool.PACK_COMMAND));
    }
}
