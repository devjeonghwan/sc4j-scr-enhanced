package org.fogofwarlabs.sc4j.screnhanced.script;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.scr.data.extension.ScrDataExtensionUtils;
import org.fogofwarlabs.sc4j.scr.data.format.chk.ChkFile;
import org.fogofwarlabs.sc4j.scr.data.format.chk.ChkSection;
import org.fogofwarlabs.sc4j.scr.data.format.mpq.MpqArchive;
import org.fogofwarlabs.sc4j.scr.data.format.mpq.MpqArchiveEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads and writes the script package of a map archive.
 * <p>
 * The manifest and the scripts are the payload of the section {@value #SECTION_NAME} of {@code staredit\scenario.chk}: {@code version}
 * ({@code u16}, {@value #FORMAT_VERSION}), {@code flags} ({@code u16}, {@code 0}), {@code manifestSize} ({@code u32}), the manifest,
 * {@code fileCount} ({@code u32}) and per script {@code nameSize} ({@code u16}), the UTF-8 name, {@code size} ({@code u32}) and the
 * content, all little endian. The resources are the MPQ file {@value #RESOURCE_FILE_NAME} in the same layout without {@code manifestSize}
 * and the manifest; a package without resources has no such file.
 * <p>
 * Reading takes the last {@value #SECTION_NAME} section. Writing needs a plain CHK file, where each section starts where the previous one
 * ends, no size is negative and the last section ends at the end of the file; it removes every {@value #SECTION_NAME} section, appends
 * the new one and keeps every other section and every other MPQ file as it is.
 */
public final class ScriptStorageUtils {
    /**
     * The tag of the script section in {@code staredit\scenario.chk}.
     */
    public static final @NotNull String SECTION_NAME = "SCES";
    /**
     * The name of the resource file in the map archive.
     */
    public static final @NotNull String RESOURCE_FILE_NAME = "sces\\resources";
    /**
     * The tag of the replay section that holds the resource file in the replays of SC:R Enhanced.
     */
    public static final @NotNull String RESOURCE_REPLAY_SECTION = "SCER";
    /**
     * The format version of the script section and the resource file.
     */
    public static final int FORMAT_VERSION = 1;

    private static final int FORMAT_FLAGS = 0;
    private static final int FORMAT_HEADER_SIZE = Short.BYTES + Short.BYTES;
    private static final int ENTRY_HEADER_SIZE = Short.BYTES + Integer.BYTES;
    private static final int MAXIMUM_NAME_SIZE = 0xFFFF;
    /**
     * The size of a CHK section header: the four tag bytes and the signed size DWORD.
     */
    private static final int SECTION_HEADER_SIZE = 8;

    /**
     * Reads the script package of a map archive.
     *
     * @param archiveData the whole map archive content
     * @param description the map name used in messages
     * @return the script package, or {@code null} if the scenario has no {@value #SECTION_NAME} section
     * @throws IOException if the archive or its scenario cannot be read, or the script section, the resource file or the package is not
     *                     valid
     */
    public static @Nullable ScriptPackage readPackage(byte @NotNull [] archiveData, @NotNull String description) throws IOException {
        MpqArchive archive = MpqArchive.open(archiveData, description, false);
        ScriptPackage scripts = readScripts(ChkFile.parseTolerant(archive.readFile(ChkFile.ARCHIVE_ENTRY)), description);

        if (scripts == null || archive.findEntry(RESOURCE_FILE_NAME) == null) {
            return scripts;
        }

        ScriptResources resourceFile = readResources(archive.readFile(RESOURCE_FILE_NAME), RESOURCE_FILE_NAME + " of " + description);
        Map<String, byte[]> resources = new LinkedHashMap<>();

        for (ScriptResources.Entry entry : resourceFile.getEntries()) {
            resources.put(entry.getName(), Arrays.copyOfRange(resourceFile.getData(), entry.getOffset(), entry.getOffset() + entry.getSize()));
        }

        return ScriptPackage.create(scripts.getManifest(), scripts.getScripts(), resources);
    }

    /**
     * Reads the resource file of a map archive ({@value #RESOURCE_FILE_NAME}) without copying its resources: the resources stay in the file's
     * bytes, which the result keeps.
     *
     * @param resourceFile the content of the resource file, kept and not to be changed
     * @param description  the resource file name used in messages
     * @return the resources of the file
     * @throws IOException if the content is not a resource file
     */
    public static @NotNull ScriptResources readResources(byte @NotNull [] resourceFile, @NotNull String description) throws IOException {
        ByteBuffer payload = ByteBuffer.wrap(resourceFile).order(ByteOrder.LITTLE_ENDIAN);

        readFormatHeader(payload, description);

        Map<String, ScriptResources.Entry> entries = readEntryIndex(payload, description);

        requireEnd(payload, description);

        return new ScriptResources(resourceFile, entries);
    }

    /**
     * Reads the manifest and the scripts of a scenario's last {@value #SECTION_NAME} section, without the resources of the map archive.
     *
     * @param scenario    the parsed scenario
     * @param description the map name used in messages
     * @return the script package without resources, or {@code null} if the scenario has no {@value #SECTION_NAME} section
     * @throws IOException if the script section or the package is not valid
     */
    public static @Nullable ScriptPackage readScripts(@NotNull ChkFile scenario, @NotNull String description) throws IOException {
        List<ChkSection> sections = scenario.getSections(SECTION_NAME);

        if (sections.isEmpty()) {
            return null;
        }

        ChkSection section = sections.getLast();

        if (!section.hasCompletePayload()) {
            throw Exceptions.newIOException("The {0} section of {1} ends after the end of the scenario.", SECTION_NAME, description);
        }

        ByteBuffer sectionPayload = ByteBuffer.wrap(section.getPayload()).order(ByteOrder.LITTLE_ENDIAN);
        String sectionDescription = SECTION_NAME + " section of " + description;

        readFormatHeader(sectionPayload, sectionDescription);

        byte[] manifest = readBytes(sectionPayload, readUnsigned(sectionPayload, Integer.BYTES, sectionDescription), sectionDescription);
        Map<String, byte[]> scripts = readEntries(sectionPayload, sectionDescription);

        requireEnd(sectionPayload, sectionDescription);

        return ScriptPackage.create(ScriptManifest.parse(manifest), scripts, Map.of());
    }

    /**
     * Writes a script package into a map archive: the scenario gets the new {@value #SECTION_NAME} section and the resource file is put,
     * or removed when the package has no resources.
     *
     * @param archiveData   the whole map archive content, not modified
     * @param description   the map name used in messages
     * @param scriptPackage the script package
     * @return the new map archive content
     * @throws IOException if the archive cannot be read or edited, its scenario is not a plain CHK file, a name exceeds {@code 65535}
     *                     UTF-8 bytes, or the new scenario exceeds {@link ScrDataExtensionUtils#getMaximumScenarioSize()}
     */
    public static byte @NotNull [] writePackage(byte @NotNull [] archiveData, @NotNull String description,
                                                @NotNull ScriptPackage scriptPackage) throws IOException {
        byte[] scenario = MpqArchive.open(archiveData, description, false).readFile(ChkFile.ARCHIVE_ENTRY);
        byte[] section = createPayload(scriptPackage.getManifest().getContent(), scriptPackage.getScripts());
        MpqArchiveEditor editor = MpqArchiveEditor.open(archiveData, description);

        editor.putFile(ChkFile.ARCHIVE_ENTRY, replaceSection(scenario, section, description));

        if (scriptPackage.getResources().isEmpty()) {
            editor.removeFile(RESOURCE_FILE_NAME);
        } else {
            editor.putFile(RESOURCE_FILE_NAME, createPayload(null, scriptPackage.getResources()));
        }

        return editor.writeArchive();
    }

    /**
     * Replaces the {@value #SECTION_NAME} sections of a plain CHK file with one section after the last complete section.
     * <p>
     * The tail of the file, a last section that ends after the end of the file or bytes too short for a section header, stays after the new
     * section.
     *
     * @param scenario    the CHK file content
     * @param payload     the payload of the new section
     * @param description the map name used in messages
     * @return the new CHK file content
     * @throws IOException if the CHK file is not plain or the new one exceeds {@link ScrDataExtensionUtils#getMaximumScenarioSize()}
     */
    private static byte @NotNull [] replaceSection(byte @NotNull [] scenario, byte @NotNull [] payload, @NotNull String description) throws IOException {
        List<ChkSection> keptSections = new ArrayList<>();
        long keptSize = 0;
        long sectionEnd = 0;

        for (ChkSection section : ChkFile.parseTolerant(scenario).getSections()) {
            if (section.getDeclaredSize() < 0) {
                throw Exceptions.newIOException("The scenario of {0} is not a plain CHK file: the section {1} at offset {2} has the negative size {3}.",
                                                description, section.getName(), section.getHeaderOffset(), section.getDeclaredSize());
            }

            if (section.getHeaderOffset() != sectionEnd) {
                throw Exceptions.newIOException("The scenario of {0} is not a plain CHK file: the section {1} at offset {2} does not follow the " +
                                                "previous section.",
                                                description, section.getName(), section.getHeaderOffset());
            }

            // Only the last section can end after the end of the file; it is part of the tail.
            if (!section.hasCompletePayload()) {
                break;
            }

            sectionEnd = section.getPayloadOffset() + (long) section.getDeclaredSize();

            if (!section.getName().equals(SECTION_NAME)) {
                keptSections.add(section);
                keptSize += sectionEnd - section.getHeaderOffset();
            }
        }

        int tailOffset = (int) sectionEnd;
        int tailSize = scenario.length - tailOffset;
        long scenarioSize = keptSize + SECTION_HEADER_SIZE + payload.length + tailSize;

        if (scenarioSize > ScrDataExtensionUtils.getMaximumScenarioSize()) {
            throw Exceptions.newIOException("The scenario of {0} with the script section has {1} bytes, more than the {2} bytes a map may hold.",
                                            description, scenarioSize, ScrDataExtensionUtils.getMaximumScenarioSize());
        }

        ByteBuffer newScenario = ByteBuffer.allocate((int) scenarioSize).order(ByteOrder.LITTLE_ENDIAN);

        for (ChkSection section : keptSections) {
            newScenario.put(scenario, section.getHeaderOffset(), SECTION_HEADER_SIZE + section.getDeclaredSize());
        }

        newScenario.put(SECTION_NAME.getBytes(StandardCharsets.ISO_8859_1)).putInt(payload.length).put(payload);
        newScenario.put(scenario, tailOffset, tailSize);

        return newScenario.array();
    }

    /**
     * Computes the size of the script section with its header, as {@link #writePackage} writes it into the scenario.
     *
     * @param scriptPackage the script package
     * @return the size in bytes
     */
    static long getSectionSize(@NotNull ScriptPackage scriptPackage) {
        return SECTION_HEADER_SIZE + getPayloadSize(scriptPackage.getManifest().getContent(), scriptPackage.getScripts());
    }

    /**
     * Computes the size of the resource file, as {@link #writePackage} writes it into the map archive.
     *
     * @param scriptPackage the script package
     * @return the size in bytes, {@code 0} for a package without resources
     */
    static long getResourceFileSize(@NotNull ScriptPackage scriptPackage) {
        return scriptPackage.getResources().isEmpty() ? 0 : getPayloadSize(null, scriptPackage.getResources());
    }

    /**
     * Computes the size of the payload of the script section or the resource file.
     *
     * @param manifest the manifest of the script section, or {@code null} for the resource file
     * @param files    the file contents by name
     * @return the size in bytes
     */
    private static long getPayloadSize(byte @Nullable [] manifest, @NotNull Map<String, byte[]> files) {
        long size = FORMAT_HEADER_SIZE + (manifest == null ? 0 : Integer.BYTES + (long) manifest.length) + Integer.BYTES;

        for (Map.Entry<String, byte[]> file : files.entrySet()) {
            size += ENTRY_HEADER_SIZE + file.getKey().getBytes(StandardCharsets.UTF_8).length + (long) file.getValue().length;
        }

        return size;
    }

    /**
     * Creates the payload of the script section or the resource file.
     *
     * @param manifest the manifest of the script section, or {@code null} for the resource file
     * @param files    the file contents by name
     * @return the payload
     * @throws IOException if a name exceeds {@code 65535} UTF-8 bytes or the payload exceeds {@link MpqArchive#MAXIMUM_FILE_SIZE}
     */
    private static byte @NotNull [] createPayload(byte @Nullable [] manifest, @NotNull Map<String, byte[]> files) throws IOException {
        List<byte[]> names = new ArrayList<>(files.size());
        long size = getPayloadSize(manifest, files);

        for (Map.Entry<String, byte[]> file : files.entrySet()) {
            byte[] name = file.getKey().getBytes(StandardCharsets.UTF_8);

            if (name.length > MAXIMUM_NAME_SIZE) {
                throw Exceptions.newIOException("The file name {0} exceeds {1} UTF-8 bytes.", file.getKey(), MAXIMUM_NAME_SIZE);
            }

            names.add(name);
        }

        if (size > MpqArchive.MAXIMUM_FILE_SIZE) {
            throw Exceptions.newIOException("The script data of {0} bytes exceeds {1} bytes.", size, MpqArchive.MAXIMUM_FILE_SIZE);
        }

        ByteBuffer payload = ByteBuffer.allocate((int) size).order(ByteOrder.LITTLE_ENDIAN);

        payload.putShort((short) FORMAT_VERSION).putShort((short) FORMAT_FLAGS);

        if (manifest != null) {
            payload.putInt(manifest.length).put(manifest);
        }

        payload.putInt(files.size());

        int nameIndex = 0;

        for (byte[] content : files.values()) {
            byte[] name = names.get(nameIndex++);

            payload.putShort((short) name.length).put(name).putInt(content.length).put(content);
        }

        return payload.array();
    }

    private static void readFormatHeader(@NotNull ByteBuffer payload, @NotNull String description) throws IOException {
        int version = readUnsigned(payload, Short.BYTES, description);
        int flags = readUnsigned(payload, Short.BYTES, description);

        if (version != FORMAT_VERSION || flags != FORMAT_FLAGS) {
            throw Exceptions.newIOException("The {0} has the version {1} and the flags {2}; SC:R Enhanced reads the version {3} and the flags {4}.",
                                            description, version, flags, FORMAT_VERSION, FORMAT_FLAGS);
        }
    }

    private static @NotNull Map<String, byte[]> readEntries(@NotNull ByteBuffer payload, @NotNull String description) throws IOException {
        Map<String, byte[]> files = new LinkedHashMap<>();

        for (ScriptResources.Entry entry : readEntryIndex(payload, description).values()) {
            files.put(entry.getName(), Arrays.copyOfRange(payload.array(), entry.getOffset(), entry.getOffset() + entry.getSize()));
        }

        return files;
    }

    /**
     * Reads the file count and the files that follow it, without copying their contents.
     *
     * @param payload     the payload at the file count, backed by an array from its first byte
     * @param description the payload name used in messages
     * @return the files by name in the stored order, with the range of each content in the payload's array
     * @throws IOException if the payload ends inside a file, a name is not UTF-8 or a name repeats
     */
    private static @NotNull Map<String, ScriptResources.Entry> readEntryIndex(@NotNull ByteBuffer payload, @NotNull String description) throws IOException {
        int fileCount = readUnsigned(payload, Integer.BYTES, description);
        Map<String, ScriptResources.Entry> files = new LinkedHashMap<>();

        for (int fileIndex = 0; fileIndex < fileCount; fileIndex++) {
            byte[] nameBytes = readBytes(payload, readUnsigned(payload, Short.BYTES, description), description);
            String name;

            try {
                name = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(nameBytes)).toString();
            } catch (CharacterCodingException exception) {
                throw Exceptions.newIOException(exception, "The {0} holds a file name that is not UTF-8 at offset {1}.", description,
                                                payload.position() - nameBytes.length);
            }

            int size = readUnsigned(payload, Integer.BYTES, description);
            int offset = payload.position();

            requireRemaining(payload, size, description);
            payload.position(offset + size);

            if (files.put(name, new ScriptResources.Entry(name, offset, size)) != null) {
                throw Exceptions.newIOException("The {0} holds the file name {1} twice.", description, name);
            }
        }

        return files;
    }

    /**
     * Reads an unsigned {@code u16} or {@code u32} field.
     *
     * @param payload     the payload at the field
     * @param fieldSize   the field size in bytes, {@link Short#BYTES} or {@link Integer#BYTES}
     * @param description the payload name used in messages
     * @return the field value
     * @throws IOException if the payload ends before the field or the value exceeds {@link Integer#MAX_VALUE}
     */
    private static int readUnsigned(@NotNull ByteBuffer payload, int fieldSize, @NotNull String description) throws IOException {
        if (payload.remaining() < fieldSize) {
            throw Exceptions.newIOException("The {0} ends at offset {1}, inside a field.", description, payload.limit());
        }

        long value = fieldSize == Short.BYTES ? Short.toUnsignedInt(payload.getShort()) : Integer.toUnsignedLong(payload.getInt());

        if (value > Integer.MAX_VALUE) {
            throw Exceptions.newIOException("The {0} holds the size {1} at offset {2}, more than an array holds.", description, value,
                                            payload.position() - fieldSize);
        }

        return (int) value;
    }

    private static byte @NotNull [] readBytes(@NotNull ByteBuffer payload, int size, @NotNull String description) throws IOException {
        requireRemaining(payload, size, description);

        byte[] data = new byte[size];

        payload.get(data);

        return data;
    }

    private static void requireRemaining(@NotNull ByteBuffer payload, int size, @NotNull String description) throws IOException {
        if (payload.remaining() < size) {
            throw Exceptions.newIOException("The {0} ends {1} bytes before the end of the data at offset {2}.", description,
                                            size - payload.remaining(), payload.position());
        }
    }

    private static void requireEnd(@NotNull ByteBuffer payload, @NotNull String description) throws IOException {
        if (payload.hasRemaining()) {
            throw Exceptions.newIOException("The {0} holds {1} bytes after its last file.", description, payload.remaining());
        }
    }

    private ScriptStorageUtils() {
    }
}
