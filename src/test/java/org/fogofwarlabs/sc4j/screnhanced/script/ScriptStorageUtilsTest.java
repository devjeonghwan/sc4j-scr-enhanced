package org.fogofwarlabs.sc4j.screnhanced.script;

import org.fogofwarlabs.sc4j.scr.data.format.chk.ChkFile;
import org.fogofwarlabs.sc4j.scr.data.format.chk.ChkSection;
import org.fogofwarlabs.sc4j.scr.data.format.mpq.MpqArchive;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests {@link ScriptStorageUtils} with synthetic map archives; the layout test reads the section and resource bytes because their layout
 * is the map format.
 */
final class ScriptStorageUtilsTest {
    private static final @NotNull String MAP_NAME = "script-map.scx";
    private static final @NotNull String MAIN_SCRIPT = "main.js";
    private static final @NotNull String MODULE_SCRIPT = "lib/module.js";
    private static final @NotNull String RESOURCE_NAME = "images/marine.grp";
    private static final @NotNull String OTHER_RESOURCE_NAME = "sounds/hit.wav";
    private static final byte @NotNull [] VERSION_PAYLOAD = {(byte) 0xCD, 0x00};
    private static final byte @NotNull [] DIMENSIONS_PAYLOAD = {0x40, 0x00, 0x40, 0x00};
    private static final byte @NotNull [] TILESET_PAYLOAD = {0x04, 0x00};
    private static final byte @NotNull [] OWNER_PAYLOAD = {5, 5, 5, 5, 6, 6, 6, 6, 0, 0, 0, 0};
    private static final byte @NotNull [] OLD_SECTION_PAYLOAD = "old script data".getBytes(StandardCharsets.ISO_8859_1);
    /**
     * The capacity of the expected section and resource file bytes of the layout test.
     */
    private static final int LAYOUT_BUFFER_SIZE = 256;

    private static byte @NotNull [] createScenario() {
        return ScriptMapScenario.join(ScriptMapScenario.createSection(ChkFile.VERSION_SECTION, VERSION_PAYLOAD),
                                      ScriptMapScenario.createSection(ScriptStorageUtils.SECTION_NAME, OLD_SECTION_PAYLOAD),
                                      ScriptMapScenario.createSection(ChkFile.DIMENSIONS_SECTION, DIMENSIONS_PAYLOAD),
                                      ScriptMapScenario.createSection(ChkFile.TILESET_SECTION, TILESET_PAYLOAD),
                                      ScriptMapScenario.createSection(ChkFile.OWNER_SECTION, OWNER_PAYLOAD));
    }

    private static @NotNull ScriptPackage createPackage(@NotNull String name, @NotNull Map<String, String> scripts,
                                                        @NotNull Map<String, String> resources) throws IOException {
        String manifest = "{\"name\": \"" + name + "\", \"main\": \"" + MAIN_SCRIPT + "\", \"api\": \"1\"}";

        return ScriptPackage.create(ScriptManifest.parse(manifest.getBytes(StandardCharsets.UTF_8)), toContents(scripts), toContents(resources));
    }

    private static @NotNull Map<String, byte[]> toContents(@NotNull Map<String, String> texts) {
        Map<String, byte[]> contents = new LinkedHashMap<>();

        for (Map.Entry<String, String> text : texts.entrySet()) {
            contents.put(text.getKey(), text.getValue().getBytes(StandardCharsets.UTF_8));
        }

        return contents;
    }

    private static @NotNull Map<String, String> toTexts(@NotNull Map<String, byte[]> contents) {
        Map<String, String> texts = new LinkedHashMap<>();

        for (Map.Entry<String, byte[]> content : contents.entrySet()) {
            texts.put(content.getKey(), new String(content.getValue(), StandardCharsets.UTF_8));
        }

        return texts;
    }

    private static byte @NotNull [] readScenario(byte @NotNull [] map) throws IOException {
        return MpqArchive.open(map, MAP_NAME, false).readFile(ChkFile.ARCHIVE_ENTRY);
    }

    private static @NotNull List<ChkSection> findOtherSections(@NotNull ChkFile scenario) {
        List<ChkSection> sections = new ArrayList<>();

        for (ChkSection section : scenario.getSections()) {
            if (!section.getName().equals(ScriptStorageUtils.SECTION_NAME)) {
                sections.add(section);
            }
        }

        return sections;
    }

    private static void writeEntry(@NotNull ByteBuffer payload, @NotNull String name, @NotNull String content) {
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

        payload.putShort((short) nameBytes.length).put(nameBytes).putInt(contentBytes.length).put(contentBytes);
    }

    @Test
    void write_package_should_write_the_section_and_resource_file_layout() throws IOException {
        ScriptPackage scriptPackage = createPackage("Layout", Map.of(MAIN_SCRIPT, "game.print(1);"), Map.of(RESOURCE_NAME, "GRP"));
        byte[] manifest = scriptPackage.getManifest().getContent();
        ByteBuffer section = ByteBuffer.allocate(LAYOUT_BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer resourceFile = ByteBuffer.allocate(LAYOUT_BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);

        section.putShort((short) ScriptStorageUtils.FORMAT_VERSION).putShort((short) 0).putInt(manifest.length).put(manifest).putInt(1);
        writeEntry(section, MAIN_SCRIPT, "game.print(1);");
        resourceFile.putShort((short) ScriptStorageUtils.FORMAT_VERSION).putShort((short) 0).putInt(1);
        writeEntry(resourceFile, RESOURCE_NAME, "GRP");

        byte[] map = ScriptStorageUtils.writePackage(ScriptMapScenario.createMap(createScenario()), MAP_NAME, scriptPackage);
        ChkFile scenario = ChkFile.parse(readScenario(map));

        byte[] storedResourceFile = MpqArchive.open(map, MAP_NAME, false).readFile(ScriptStorageUtils.RESOURCE_FILE_NAME);

        Assertions.assertArrayEquals(Arrays.copyOf(section.array(), section.position()), scenario.getSection(ScriptStorageUtils.SECTION_NAME));
        Assertions.assertArrayEquals(Arrays.copyOf(resourceFile.array(), resourceFile.position()), storedResourceFile);
    }

    @Test
    void write_package_keeps_every_other_chk_section_and_mpq_file() throws IOException {
        byte[] originalScenario = createScenario();
        ScriptPackage scriptPackage = createPackage("Kept", Map.of(MAIN_SCRIPT, "1;", MODULE_SCRIPT, "2;"), Map.of(RESOURCE_NAME, "GRP"));
        byte[] map = ScriptStorageUtils.writePackage(ScriptMapScenario.createMap(originalScenario), MAP_NAME, scriptPackage);
        MpqArchive archive = MpqArchive.open(map, MAP_NAME, false);
        ChkFile scenario = ChkFile.parse(archive.readFile(ChkFile.ARCHIVE_ENTRY));
        List<ChkSection> originalSections = findOtherSections(ChkFile.parse(originalScenario));
        List<ChkSection> sections = findOtherSections(scenario);

        Assertions.assertEquals(List.of(ChkFile.VERSION_SECTION, ChkFile.DIMENSIONS_SECTION, ChkFile.TILESET_SECTION, ChkFile.OWNER_SECTION,
                                        ScriptStorageUtils.SECTION_NAME), scenario.getSectionNames());
        Assertions.assertEquals(ScriptStorageUtils.SECTION_NAME, scenario.getSections().getLast().getName());
        Assertions.assertEquals(originalSections.size(), sections.size());

        for (int index = 0; index < sections.size(); index++) {
            Assertions.assertEquals(originalSections.get(index).getName(), sections.get(index).getName());
            Assertions.assertArrayEquals(originalSections.get(index).getPayload(), sections.get(index).getPayload());
        }

        Assertions.assertArrayEquals(ScriptMapScenario.SOUND, archive.readFile(ScriptMapScenario.SOUND_NAME));
        Assertions.assertTrue(new String(archive.readFile("(listfile)"), StandardCharsets.ISO_8859_1).contains(ScriptStorageUtils.RESOURCE_FILE_NAME));

        ScriptPackage readPackage = ScriptStorageUtils.readPackage(map, MAP_NAME);

        Assertions.assertNotNull(readPackage);
        Assertions.assertEquals("Kept", readPackage.getManifest().getName());
        Assertions.assertEquals(toTexts(scriptPackage.getScripts()), toTexts(readPackage.getScripts()));
        Assertions.assertEquals(toTexts(scriptPackage.getResources()), toTexts(readPackage.getResources()));
    }

    @Test
    void write_package_again_replaces_the_section_and_the_resource_file() throws IOException {
        byte[] firstMap = ScriptStorageUtils.writePackage(ScriptMapScenario.createMap(createScenario()), MAP_NAME,
                                                          createPackage("First", Map.of(MAIN_SCRIPT, "1;"), Map.of(RESOURCE_NAME, "first")));
        ScriptPackage secondPackage = createPackage("Second", Map.of(MAIN_SCRIPT, "2;", MODULE_SCRIPT, "3;"), Map.of(OTHER_RESOURCE_NAME, "second"));
        byte[] secondMap = ScriptStorageUtils.writePackage(firstMap, MAP_NAME, secondPackage);
        ScriptPackage secondReadPackage = ScriptStorageUtils.readPackage(secondMap, MAP_NAME);

        Assertions.assertEquals(1, ChkFile.parse(readScenario(secondMap)).getSections(ScriptStorageUtils.SECTION_NAME).size());
        Assertions.assertNotNull(secondReadPackage);
        Assertions.assertEquals("Second", secondReadPackage.getManifest().getName());
        Assertions.assertEquals(toTexts(secondPackage.getScripts()), toTexts(secondReadPackage.getScripts()));
        Assertions.assertEquals(Map.of(OTHER_RESOURCE_NAME, "second"), toTexts(secondReadPackage.getResources()));

        byte[] thirdMap = ScriptStorageUtils.writePackage(secondMap, MAP_NAME, createPackage("Third", Map.of(MAIN_SCRIPT, "4;"), Map.of()));
        ScriptPackage thirdReadPackage = ScriptStorageUtils.readPackage(thirdMap, MAP_NAME);
        MpqArchive thirdArchive = MpqArchive.open(thirdMap, MAP_NAME, false);

        Assertions.assertEquals(1, ChkFile.parse(readScenario(thirdMap)).getSections(ScriptStorageUtils.SECTION_NAME).size());
        Assertions.assertNull(thirdArchive.findEntry(ScriptStorageUtils.RESOURCE_FILE_NAME));
        Assertions.assertArrayEquals(ScriptMapScenario.SOUND, thirdArchive.readFile(ScriptMapScenario.SOUND_NAME));
        Assertions.assertNotNull(thirdReadPackage);
        Assertions.assertEquals("Third", thirdReadPackage.getManifest().getName());
        Assertions.assertEquals(Map.of(MAIN_SCRIPT, "4;"), toTexts(thirdReadPackage.getScripts()));
        Assertions.assertTrue(thirdReadPackage.getResources().isEmpty());
    }

    @Test
    void write_package_should_refuse_a_scenario_that_is_not_plain() throws IOException {
        byte[] version = ScriptMapScenario.createSection(ChkFile.VERSION_SECTION, VERSION_PAYLOAD);
        byte[] scenario = ScriptMapScenario.join(version, ScriptMapScenario.createSection("BACK", -16, new byte[0]), version);
        byte[] map = ScriptMapScenario.createMap(scenario);
        ScriptPackage scriptPackage = createPackage("Refused", Map.of(MAIN_SCRIPT, "1;"), Map.of());
        IOException refusal = Assertions.assertThrows(IOException.class, () -> ScriptStorageUtils.writePackage(map, MAP_NAME, scriptPackage));

        Assertions.assertTrue(refusal.getMessage().contains("not a plain CHK file"), refusal.getMessage());
    }

    @Test
    void write_package_keeps_the_scenario_tail_after_the_script_section() throws IOException {
        byte[] version = ScriptMapScenario.createSection(ChkFile.VERSION_SECTION, VERSION_PAYLOAD);
        List<byte[]> tails = List.of(new byte[]{1, 2, 3}, ScriptMapScenario.createSection("LONG", 64, new byte[4]));
        ScriptPackage scriptPackage = createPackage("Tail", Map.of(MAIN_SCRIPT, "1;"), Map.of());

        for (byte[] tail : tails) {
            byte[] map = ScriptMapScenario.createMap(ScriptMapScenario.join(version, tail));
            byte[] packedMap = ScriptStorageUtils.writePackage(ScriptStorageUtils.writePackage(map, MAP_NAME, scriptPackage), MAP_NAME, scriptPackage);
            byte[] scenario = readScenario(packedMap);
            ScriptPackage readPackage = ScriptStorageUtils.readPackage(packedMap, MAP_NAME);

            Assertions.assertArrayEquals(version, Arrays.copyOf(scenario, version.length));
            Assertions.assertArrayEquals(tail, Arrays.copyOfRange(scenario, scenario.length - tail.length, scenario.length));
            Assertions.assertEquals(1, ChkFile.parseTolerant(scenario).getSections(ScriptStorageUtils.SECTION_NAME).size());
            Assertions.assertNotNull(readPackage);
            Assertions.assertEquals("Tail", readPackage.getManifest().getName());
        }
    }

    @Test
    void read_package_takes_the_last_script_section() throws IOException {
        byte[] firstMap = ScriptStorageUtils.writePackage(ScriptMapScenario.createMap(createScenario()), MAP_NAME,
                                                          createPackage("First", Map.of(MAIN_SCRIPT, "1;"), Map.of()));
        byte[] secondMap = ScriptStorageUtils.writePackage(firstMap, MAP_NAME, createPackage("Second", Map.of(MAIN_SCRIPT, "2;"), Map.of()));
        byte[] firstSection = ChkFile.parse(readScenario(firstMap)).requireSection(ScriptStorageUtils.SECTION_NAME);
        byte[] secondSection = ChkFile.parse(readScenario(secondMap)).requireSection(ScriptStorageUtils.SECTION_NAME);
        byte[] scenario = ScriptMapScenario.join(ScriptMapScenario.createSection(ChkFile.VERSION_SECTION, VERSION_PAYLOAD),
                                                 ScriptMapScenario.createSection(ScriptStorageUtils.SECTION_NAME, firstSection),
                                                 ScriptMapScenario.createSection(ChkFile.DIMENSIONS_SECTION, DIMENSIONS_PAYLOAD),
                                                 ScriptMapScenario.createSection(ScriptStorageUtils.SECTION_NAME, secondSection),
                                                 ScriptMapScenario.createSection(ChkFile.TILESET_SECTION, TILESET_PAYLOAD));
        ScriptPackage readPackage = ScriptStorageUtils.readPackage(ScriptMapScenario.createMap(scenario), MAP_NAME);

        Assertions.assertNotNull(readPackage);
        Assertions.assertEquals("Second", readPackage.getManifest().getName());
        Assertions.assertEquals(Map.of(MAIN_SCRIPT, "2;"), toTexts(readPackage.getScripts()));
    }

    @Test
    void read_package_should_return_null_when_the_scenario_has_no_script_section() throws IOException {
        byte[] scenario = ScriptMapScenario.createSection(ChkFile.VERSION_SECTION, VERSION_PAYLOAD);

        Assertions.assertNull(ScriptStorageUtils.readPackage(ScriptMapScenario.createMap(scenario), MAP_NAME));
    }

    @Test
    void read_scripts_should_read_the_scenario_section_without_the_resources() throws IOException {
        byte[] map = ScriptStorageUtils.writePackage(ScriptMapScenario.createMap(createScenario()), MAP_NAME,
                                                     createPackage("Scripts", Map.of(MAIN_SCRIPT, "main;", MODULE_SCRIPT, "module;"),
                                                                   Map.of(RESOURCE_NAME, "image")));
        ScriptPackage scripts = ScriptStorageUtils.readScripts(ChkFile.parseTolerant(readScenario(map)), MAP_NAME);
        byte[] withoutSection = ScriptMapScenario.createSection(ChkFile.VERSION_SECTION, VERSION_PAYLOAD);

        Assertions.assertNotNull(scripts);
        Assertions.assertEquals("Scripts", scripts.getManifest().getName());
        Assertions.assertEquals(Map.of(MAIN_SCRIPT, "main;", MODULE_SCRIPT, "module;"), toTexts(scripts.getScripts()));
        Assertions.assertTrue(scripts.getResources().isEmpty());
        Assertions.assertNull(ScriptStorageUtils.readScripts(ChkFile.parseTolerant(withoutSection), MAP_NAME));
    }

    @Test
    void read_resources_should_index_the_resources_in_the_file_bytes() throws IOException {
        byte[] map = ScriptStorageUtils.writePackage(ScriptMapScenario.createMap(createScenario()), MAP_NAME,
                                                     createPackage("Resources", Map.of(MAIN_SCRIPT, "main;"),
                                                                   Map.of(RESOURCE_NAME, "image", OTHER_RESOURCE_NAME, "sound")));
        byte[] resourceFile = MpqArchive.open(map, MAP_NAME, false).readFile(ScriptStorageUtils.RESOURCE_FILE_NAME);
        ScriptResources resources = ScriptStorageUtils.readResources(resourceFile, ScriptStorageUtils.RESOURCE_FILE_NAME);
        ScriptResources.Entry image = resources.findEntry(RESOURCE_NAME);

        Assertions.assertSame(resourceFile, resources.getData(), "the resources stay in the file bytes");
        Assertions.assertNotNull(image);
        Assertions.assertEquals("image", new String(resourceFile, image.getOffset(), image.getSize(), StandardCharsets.UTF_8));
        Assertions.assertEquals(2, resources.getEntries().size());
        Assertions.assertNull(resources.findEntry("images/missing.grp"));
        Assertions.assertTrue(ScriptResources.EMPTY.getEntries().isEmpty());
    }

    @Test
    void read_resources_should_reject_a_malformed_file() {
        ByteBuffer truncated = ByteBuffer.allocate(LAYOUT_BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer repeated = ByteBuffer.allocate(LAYOUT_BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        byte[] otherVersion = {2, 0, 0, 0, 0, 0, 0, 0};

        truncated.putShort((short) ScriptStorageUtils.FORMAT_VERSION).putShort((short) 0).putInt(1);
        writeEntry(truncated, RESOURCE_NAME, "image");
        repeated.putShort((short) ScriptStorageUtils.FORMAT_VERSION).putShort((short) 0).putInt(2);
        writeEntry(repeated, RESOURCE_NAME, "image");
        writeEntry(repeated, RESOURCE_NAME, "again");

        byte[] truncatedFile = Arrays.copyOf(truncated.array(), truncated.position() - 1);
        byte[] repeatedFile = Arrays.copyOf(repeated.array(), repeated.position());
        byte[] trailingFile = Arrays.copyOf(truncated.array(), truncated.position() + 1);

        Assertions.assertThrows(IOException.class, () -> ScriptStorageUtils.readResources(truncatedFile, MAP_NAME));
        Assertions.assertThrows(IOException.class, () -> ScriptStorageUtils.readResources(repeatedFile, MAP_NAME));
        Assertions.assertThrows(IOException.class, () -> ScriptStorageUtils.readResources(trailingFile, MAP_NAME));
        Assertions.assertThrows(IOException.class, () -> ScriptStorageUtils.readResources(otherVersion, MAP_NAME));
    }
}
