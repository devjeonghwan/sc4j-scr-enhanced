package org.fogofwarlabs.sc4j.screnhanced;

import org.fogofwarlabs.sc4j.base.binary.LittleEndianUtils;
import org.fogofwarlabs.sc4j.scr.data.format.chk.ChkFile;
import org.fogofwarlabs.sc4j.scr.data.format.mpq.MpqArchive;
import org.fogofwarlabs.sc4j.scr.data.format.mpq.MpqCryptoUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.zip.Deflater;

/**
 * Tests the scenario size limit of the map reader: a map whose {@code staredit\scenario.chk} is one byte larger than {@code 256} MiB is
 * refused by the reader of the SC:R mod ({@code scr.jar}) and read whole by the reader of SC:R Enhanced ({@code scr-enhanced.jar}).
 */
final class ScrEnhancedMapSizeTest {
    /**
     * The scenario size of the test map: one byte more than the scenario size the SC:R mod accepts.
     */
    private static final int SCENARIO_SIZE = 0x10000001;
    private static final @NotNull String SCENARIO_NAME = "staredit\\scenario.chk";
    private static final @NotNull String MAP_FILE_NAME = "large-scenario.scx";
    private static final @NotNull String ARCHIVE_CLASS_NAME = MpqArchive.class.getName();
    /**
     * The package prefix of the SC:R mod's classes ({@code org.fogofwarlabs.sc4j.scr}) and of SC:R Enhanced's own
     * ({@code org.fogofwarlabs.sc4j.screnhanced}).
     */
    private static final @NotNull String MOD_PACKAGE_PREFIX = "org.fogofwarlabs.sc4j.scr";
    private static final @NotNull String SERVICES_FOLDER = "META-INF/services/";

    private static final int MPQ_MAGIC = 0x1A51504D;
    private static final int HEADER_SIZE = 0x20;
    private static final int ARCHIVE_SIZE_OFFSET = 8;
    private static final int SECTOR_SHIFT_OFFSET = 14;
    private static final int HASH_TABLE_POSITION_OFFSET = 16;
    private static final int BLOCK_TABLE_POSITION_OFFSET = 20;
    private static final int HASH_TABLE_SIZE_OFFSET = 24;
    private static final int BLOCK_TABLE_SIZE_OFFSET = 28;
    /**
     * The sector size shift of the maps the map editors write: {@code 0x200 << 3} bytes per sector.
     */
    private static final int SECTOR_SHIFT = 3;
    private static final int SECTOR_SIZE = 0x200 << SECTOR_SHIFT;
    private static final int HASH_ENTRY_COUNT = 16;
    private static final int TABLE_ENTRY_VALUE_COUNT = 4;
    private static final int EMPTY_HASH_VALUE = -1;
    private static final int COMPRESSED_FILE = MpqArchive.MPQ_FILE_EXISTS | MpqArchive.MPQ_FILE_COMPRESS;
    private static final byte ZLIB_COMPRESSION = 0x02;

    private static final @NotNull String VERSION_SECTION = "VER ";
    private static final int VERSION_PAYLOAD_SIZE = 2;
    private static final int VERSION = 205;
    private static final int SECTION_HEADER_SIZE = 8;
    private static final int STRING_PAYLOAD_SIZE = SCENARIO_SIZE - 2 * SECTION_HEADER_SIZE - VERSION_PAYLOAD_SIZE;

    /**
     * Writes a map archive of one zlib-compressed {@code staredit\scenario.chk} of {@link #SCENARIO_SIZE} bytes: a {@code VER } section
     * and a {@code STR } section of zero bytes that fills the rest.
     */
    private static void writeMap(@NotNull Path file) throws IOException {
        int sectorCount = (SCENARIO_SIZE + SECTOR_SIZE - 1) / SECTOR_SIZE;
        int[] offsets = new int[sectorCount + 1];
        ByteArrayOutputStream sectors = new ByteArrayOutputStream();
        byte[] zeroSector = compressSector(new byte[SECTOR_SIZE]);
        byte[] firstSector = new byte[SECTOR_SIZE];

        writeSectionHeader(firstSector, 0, VERSION_SECTION, VERSION_PAYLOAD_SIZE);
        LittleEndianUtils.writeInt16(firstSector, SECTION_HEADER_SIZE, VERSION);
        writeSectionHeader(firstSector, SECTION_HEADER_SIZE + VERSION_PAYLOAD_SIZE, ChkFile.STRING_SECTION, STRING_PAYLOAD_SIZE);
        offsets[0] = offsets.length * Integer.BYTES;

        for (int sector = 0; sector < sectorCount; sector++) {
            int length = Math.min(SECTOR_SIZE, SCENARIO_SIZE - sector * SECTOR_SIZE);
            byte[] stored = sector == 0 ? compressSector(firstSector) : length == SECTOR_SIZE ? zeroSector : new byte[length];

            sectors.write(stored);
            offsets[sector + 1] = offsets[sector] + stored.length;
        }

        int hashTablePosition = HEADER_SIZE;
        int blockTablePosition = hashTablePosition + HASH_ENTRY_COUNT * TABLE_ENTRY_VALUE_COUNT * Integer.BYTES;
        int dataPosition = blockTablePosition + TABLE_ENTRY_VALUE_COUNT * Integer.BYTES;
        int storedSize = offsets[sectorCount];
        byte[] tables = new byte[dataPosition];
        int[] hashTable = new int[HASH_ENTRY_COUNT * TABLE_ENTRY_VALUE_COUNT];
        int slot = MpqCryptoUtils.hashTableOffset(SCENARIO_NAME) & HASH_ENTRY_COUNT - 1;

        Arrays.fill(hashTable, EMPTY_HASH_VALUE);
        System.arraycopy(new int[]{MpqCryptoUtils.hashNameA(SCENARIO_NAME), MpqCryptoUtils.hashNameB(SCENARIO_NAME), 0, 0}, 0,
                         hashTable, slot * TABLE_ENTRY_VALUE_COUNT, TABLE_ENTRY_VALUE_COUNT);
        LittleEndianUtils.writeInt32(tables, 0, MPQ_MAGIC);
        LittleEndianUtils.writeInt32(tables, Integer.BYTES, HEADER_SIZE);
        LittleEndianUtils.writeInt32(tables, ARCHIVE_SIZE_OFFSET, dataPosition + storedSize);
        LittleEndianUtils.writeInt16(tables, SECTOR_SHIFT_OFFSET, SECTOR_SHIFT);
        LittleEndianUtils.writeInt32(tables, HASH_TABLE_POSITION_OFFSET, hashTablePosition);
        LittleEndianUtils.writeInt32(tables, BLOCK_TABLE_POSITION_OFFSET, blockTablePosition);
        LittleEndianUtils.writeInt32(tables, HASH_TABLE_SIZE_OFFSET, HASH_ENTRY_COUNT);
        LittleEndianUtils.writeInt32(tables, BLOCK_TABLE_SIZE_OFFSET, 1);
        writeInts(tables, hashTablePosition, encrypt(hashTable, MpqCryptoUtils.hashKey("(hash table)")));
        writeInts(tables, blockTablePosition, encrypt(new int[]{dataPosition, storedSize, SCENARIO_SIZE, COMPRESSED_FILE},
                                                      MpqCryptoUtils.hashKey("(block table)")));

        byte[] offsetTable = new byte[offsets.length * Integer.BYTES];

        writeInts(offsetTable, 0, offsets);

        try (OutputStream output = Files.newOutputStream(file)) {
            output.write(tables);
            output.write(offsetTable);
            sectors.writeTo(output);
        }
    }

    private static void writeSectionHeader(byte @NotNull [] data, int offset, @NotNull String name, int size) {
        System.arraycopy(name.getBytes(StandardCharsets.US_ASCII), 0, data, offset, Integer.BYTES);
        LittleEndianUtils.writeInt32(data, offset + Integer.BYTES, size);
    }

    /**
     * Compresses a whole sector with zlib behind its compression mask byte.
     */
    private static byte @NotNull [] compressSector(byte @NotNull [] sector) {
        byte[] buffer = new byte[sector.length];
        int length;

        try (Deflater deflater = new Deflater()) {
            deflater.setInput(sector);
            deflater.finish();
            length = deflater.deflate(buffer, 1, buffer.length - 1);
        }

        buffer[0] = ZLIB_COMPRESSION;

        return Arrays.copyOf(buffer, length + 1);
    }

    /**
     * Encrypts table DWORDs by solving the keystream one DWORD at a time with {@link MpqCryptoUtils#decrypt}, whose keystream depends only on
     * the key and the preceding plain DWORDs.
     */
    private static int @NotNull [] encrypt(int @NotNull [] plain, int key) {
        int[] encrypted = new int[plain.length];

        for (int index = 0; index < plain.length; index++) {
            int[] probe = Arrays.copyOf(encrypted, index + 1);

            probe[index] = 0;
            MpqCryptoUtils.decrypt(probe, key);
            encrypted[index] = plain[index] ^ probe[index];
        }

        return encrypted;
    }

    private static void writeInts(byte @NotNull [] data, int offset, int @NotNull [] values) {
        for (int index = 0; index < values.length; index++) {
            LittleEndianUtils.writeInt32(data, offset + index * Integer.BYTES, values[index]);
        }
    }

    /**
     * Reads the scenario of a map with the map reader of a mod jar: the classes of the SC:R mod and of SC:R Enhanced and the service files
     * come from the jar, every other class from the tests.
     */
    private static byte @NotNull [] readScenario(@NotNull Path jar, @NotNull Path map) throws Exception {
        ClassLoader shared = new ClassLoader(ScrEnhancedMapSizeTest.class.getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith(MOD_PACKAGE_PREFIX)) {
                    throw new ClassNotFoundException(name);
                }

                return super.loadClass(name, resolve);
            }

            @Override
            public @Nullable URL getResource(String name) {
                return name.startsWith(SERVICES_FOLDER) ? null : super.getResource(name);
            }

            @Override
            public Enumeration<URL> getResources(String name) throws IOException {
                return name.startsWith(SERVICES_FOLDER) ? Collections.emptyEnumeration() : super.getResources(name);
            }
        };

        try (URLClassLoader loader = new URLClassLoader(new URL[]{jar.toUri().toURL()}, shared)) {
            Class<?> archiveClass = loader.loadClass(ARCHIVE_CLASS_NAME);
            Method open = archiveClass.getMethod("open", Path.class);
            Method readFile = archiveClass.getMethod("readFile", String.class);

            Assertions.assertSame(loader, archiveClass.getClassLoader());

            try {
                return (byte[]) readFile.invoke(open.invoke(null, map), SCENARIO_NAME);
            } catch (InvocationTargetException exception) {
                if (exception.getCause() instanceof Exception cause) {
                    throw cause;
                }

                throw exception;
            }
        }
    }

    @Test
    void scenario_over_256_mib_is_refused_by_the_scr_mod_and_read_by_scr_enhanced(@TempDir @NotNull Path folder) throws Exception {
        Path map = folder.resolve(MAP_FILE_NAME);

        writeMap(map);

        IOException refusal = Assertions.assertThrows(IOException.class, () -> readScenario(ScrEnhancedTestResources.getScrModJar(), map));

        Assertions.assertTrue(refusal.getMessage().contains("scenario size"), refusal.getMessage());

        byte[] scenario = readScenario(ScrEnhancedTestResources.getModJar(), map);

        Assertions.assertEquals(SCENARIO_SIZE, scenario.length);

        ChkFile chk = ChkFile.parseTolerant(scenario);

        Assertions.assertEquals(VERSION, LittleEndianUtils.readUnsigned16(chk.requireSection(VERSION_SECTION), 0));
        Assertions.assertEquals(STRING_PAYLOAD_SIZE, chk.requireSection(ChkFile.STRING_SECTION).length);
    }
}
