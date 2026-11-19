package org.fogofwarlabs.sc4j.screnhanced.memory;

import org.fogofwarlabs.sc4j.scr.game.trigger.eud.ClassicAddressSpace;
import org.fogofwarlabs.sc4j.scr.game.trigger.eud.ClassicDynamicRegionsUtils;
import org.fogofwarlabs.sc4j.scr.game.trigger.eud.ClassicMemorySink;
import org.fogofwarlabs.sc4j.scr.game.trigger.eud.region.ClassicStorageRegion;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Set;

/**
 * Checks the served classic structures over the emulation storage of the SC:R mod: the resource icons pointer, the game screen bitmap, the console
 * transparency, the dialogs, the console areas and the image GRP table read as the classic game laid them out, their buffers take writes up to
 * their last byte, every game gets the same heap addresses, and the bytes enter the state hash.
 */
final class ClassicMemoryTest {
    private static final int VALUE = 0x5A5AA5A5;
    private static final int HASH_SEED = 3;
    private static final int WORD_MASK = 0xFFFF;
    private static final int TRANSPARENCY_DATA_OFFSET = 8;
    /**
     * The node SC:R serves for the resource display and the picture buffer it names.
     */
    private static final int RESOURCES_NODE = 0x0082F280;
    private static final int RESOURCES_PICTURE = 0x0082F8C0;
    private static final int RESOURCES_LEFT = 220;
    private static final int RESOURCES_RIGHT = 639;
    /**
     * The identifier of the resource display's first control, its left edge in the dialog and its width.
     */
    private static final int FIRST_CONTROL = 1;
    private static final int FIRST_CONTROL_LEFT = 352;
    private static final int FIRST_CONTROL_WIDTH = 66;
    /**
     * The control type of a left-aligned text.
     */
    private static final int LEFT_TEXT_TYPE = 9;

    /**
     * The Terran console area table and its five areas: left, top, width and height of each.
     */
    private static final int CONSOLE_AREAS = 0x515300;
    private static final int CONSOLE_AREAS_SIZE = 0x48;
    private static final int @NotNull [] AREAS = {
            0, 293, 23, 22,
            138, 315, 28, 52,
            138, 367, 358, 21,
            444, 354, 52, 13,
            466, 325, 174, 29
    };
    private static final int AREA_VALUES = 4;
    private static final int AREA_TOP = 1;
    private static final int AREA_WIDTH = 2;
    private static final int AREA_HEIGHT = 3;
    /**
     * The first bytes of the installed resource icons of the tests.
     */
    private static final byte @NotNull [] ICONS = {12, 0, 64, 0, 64, 0};

    private static @NotNull ClassicAddressSpace createSpace() {
        ClassicAddressSpace space = new ClassicAddressSpace();

        space.add(ClassicStorageRegion.createImageTableRegion());
        space.addAll(ClassicStorageRegion.createEmulatorStorageRegions());
        space.addAll(ClassicDynamicRegionsUtils.create(null));
        space.add(ClassicStorageRegion.createEmulatorStorageRegion(CONSOLE_AREAS, createConsoleAreas()));
        new ClassicMemory(ICONS::clone).provideRegions(new ClassicMemorySink(space));

        return space;
    }

    /**
     * Creates the Terran console area table as SC:R fills it: every area's owner is the resource display node, then the end entry.
     */
    private static byte @NotNull [] createConsoleAreas() {
        ByteBuffer table = ByteBuffer.allocate(CONSOLE_AREAS_SIZE).order(ByteOrder.LITTLE_ENDIAN);

        for (int area = 0; area < AREAS.length; area += AREA_VALUES) {
            table.putShort((short) AREAS[area]).putShort((short) AREAS[area + AREA_TOP]).putShort((short) AREAS[area + AREA_WIDTH])
                 .putShort((short) AREAS[area + AREA_HEIGHT]).putInt(RESOURCES_NODE);
        }

        table.putInt(-1).putInt(-1).putInt(0);

        return table.array();
    }

    /**
     * Reads a DWORD at an address that need not be aligned.
     */
    private static int readUnaligned(@NotNull ClassicAddressSpace space, int address) {
        int aligned = address & -Integer.BYTES;
        long both = Integer.toUnsignedLong(space.readDword(aligned)) | (long) space.readDword(aligned + Integer.BYTES) << Integer.SIZE;

        return (int) (both >>> (address - aligned) * Byte.SIZE);
    }

    @Test
    void the_resource_icons_pointer_names_a_writable_buffer_that_starts_with_the_installed_icons() {
        ClassicAddressSpace space = createSpace();
        int buffer = space.readDword(ClassicMemory.RESOURCE_ICONS);
        int last = buffer + ClassicMemory.RESOURCE_ICONS_SIZE - Integer.BYTES;

        Assertions.assertEquals(ClassicHeap.BASE, buffer);
        Assertions.assertEquals(ByteBuffer.wrap(ICONS).order(ByteOrder.LITTLE_ENDIAN).getInt(), space.readDword(buffer));
        Assertions.assertEquals(0, space.readDword(last));

        space.writeDword(last, VALUE);

        Assertions.assertEquals(VALUE, space.readDword(last));

        space.writeDword(ClassicMemory.RESOURCE_ICONS, VALUE);

        Assertions.assertEquals(VALUE, space.readDword(ClassicMemory.RESOURCE_ICONS), "the pointer is writable");
    }

    @Test
    void every_console_area_has_an_owner_of_its_own_with_the_area_rectangle_and_a_picture() {
        ClassicAddressSpace space = createSpace();
        int count = AREAS.length / AREA_VALUES;
        Set<Integer> owners = new HashSet<>();

        for (int area = 0; area < count; area++) {
            int owner = space.readDword(CONSOLE_AREAS + area * ClassicConsoleAreaStructure.ENTRY_SIZE + ClassicConsoleAreaStructure.OWNER_OFFSET);
            int left = AREAS[area * AREA_VALUES];
            int top = AREAS[area * AREA_VALUES + AREA_TOP];
            int right = left + AREAS[area * AREA_VALUES + AREA_WIDTH] - 1;
            int bottom = top + AREAS[area * AREA_VALUES + AREA_HEIGHT] - 1;

            Assertions.assertNotEquals(RESOURCES_NODE, owner);
            Assertions.assertTrue(owners.add(owner), "area " + area);
            Assertions.assertEquals(top << Short.SIZE | left, readUnaligned(space, owner + ClassicDialogStructure.RECTANGLE_OFFSET));
            Assertions.assertEquals(bottom << Short.SIZE | right,
                                    readUnaligned(space, owner + ClassicDialogStructure.RECTANGLE_OFFSET + Integer.BYTES));
            Assertions.assertEquals(ClassicDialogStructure.VISIBLE_FLAG, readUnaligned(space, owner + ClassicDialogStructure.FLAGS_OFFSET));
            Assertions.assertEquals(AREAS[area * AREA_VALUES + AREA_HEIGHT] << Short.SIZE | AREAS[area * AREA_VALUES + AREA_WIDTH],
                                    readUnaligned(space, owner + ClassicDialogStructure.PICTURE_SIZE_OFFSET));

            int picture = readUnaligned(space, owner + ClassicDialogStructure.PICTURE_OFFSET);

            space.writeDword(picture + ClassicConsoleAreaStructure.PICTURE_BUFFER_SIZE - Integer.BYTES, VALUE);

            Assertions.assertEquals(VALUE, space.readDword(picture + ClassicConsoleAreaStructure.PICTURE_BUFFER_SIZE - Integer.BYTES));
        }

        int owner = space.readDword(CONSOLE_AREAS + ClassicConsoleAreaStructure.OWNER_OFFSET);

        space.writeDword(owner + ClassicDialogStructure.RECTANGLE_OFFSET, VALUE);

        Assertions.assertEquals(VALUE, space.readDword(owner + ClassicDialogStructure.RECTANGLE_OFFSET));
        Assertions.assertEquals(RESOURCES_LEFT, space.readDword(RESOURCES_NODE + ClassicDialogStructure.RECTANGLE_OFFSET) & WORD_MASK,
                                "the resource display keeps its place");
    }

    @Test
    void the_game_screen_is_a_bitmap_of_one_screen() {
        ClassicAddressSpace space = createSpace();
        int size = space.readDword(ClassicMemory.GAME_SCREEN);
        int pixels = space.readDword(ClassicMemory.GAME_SCREEN + Integer.BYTES);
        int lastRow = pixels + ClassicMemory.SCREEN_WIDTH * (ClassicMemory.SCREEN_HEIGHT - 1);

        Assertions.assertEquals(ClassicMemory.SCREEN_WIDTH, size & WORD_MASK);
        Assertions.assertEquals(ClassicMemory.SCREEN_HEIGHT, size >>> Short.SIZE);
        Assertions.assertNotEquals(space.readDword(ClassicMemory.RESOURCE_ICONS), pixels);

        space.writeDword(lastRow, VALUE);

        Assertions.assertEquals(VALUE, space.readDword(lastRow));
    }

    @Test
    void the_console_transparency_data_holds_the_largest_transparency_of_one_screen() {
        ClassicAddressSpace space = createSpace();
        int data = space.readDword(space.readDword(ClassicMemory.CONSOLE_TRANSPARENCY) + TRANSPARENCY_DATA_OFFSET);
        int last = data + ClassicMemory.TRANSPARENCY_DATA_SIZE - Integer.BYTES;

        Assertions.assertEquals(ClassicMemory.CONSOLE_TRANSPARENCY_DATA, data);
        Assertions.assertEquals(0, space.readDword(last));

        space.writeDword(data, VALUE);
        space.writeDword(last, VALUE);

        Assertions.assertEquals(VALUE, space.readDword(data));
        Assertions.assertEquals(VALUE, space.readDword(last));
    }

    @Test
    void a_dialog_is_laid_out_as_its_dialog_file_over_the_node_of_sc_r() {
        ClassicAddressSpace space = createSpace();
        ClassicDialogStructure dialog = ClassicDialogStructure.RESOURCES;
        int base = space.readDword(dialog.getPointer());
        int first = base + ClassicDialogStructure.getControlOffset(0);
        int last = base + ClassicDialogStructure.getControlOffset(dialog.getControlCount() - 1);

        Assertions.assertEquals(RESOURCES_NODE, base);
        Assertions.assertEquals(RESOURCES_PICTURE, space.readDword(base + ClassicDialogStructure.PICTURE_OFFSET));
        Assertions.assertEquals(RESOURCES_LEFT, space.readDword(base + ClassicDialogStructure.RECTANGLE_OFFSET) & WORD_MASK);
        Assertions.assertEquals(RESOURCES_RIGHT, space.readDword(base + ClassicDialogStructure.RECTANGLE_OFFSET + Integer.BYTES) & WORD_MASK);
        Assertions.assertEquals(first, readUnaligned(space, base + ClassicDialogStructure.FIRST_CONTROL_OFFSET));
        Assertions.assertEquals(first + ClassicDialogStructure.ENTRY_SIZE, readUnaligned(space, first + ClassicDialogStructure.NEXT_OFFSET));
        Assertions.assertEquals(0, readUnaligned(space, last + ClassicDialogStructure.NEXT_OFFSET));
        Assertions.assertEquals(base, readUnaligned(space, first + ClassicDialogStructure.DIALOG_OFFSET));
        Assertions.assertEquals(FIRST_CONTROL, readUnaligned(space, first + ClassicDialogStructure.IDENTIFIER_OFFSET) & WORD_MASK);
        Assertions.assertEquals(LEFT_TEXT_TYPE, readUnaligned(space, first + ClassicDialogStructure.TYPE_OFFSET) & WORD_MASK);
        Assertions.assertEquals(FIRST_CONTROL_LEFT | FIRST_CONTROL_LEFT + FIRST_CONTROL_WIDTH - 1 << Short.SIZE,
                                readUnaligned(space, first + ClassicDialogStructure.RECTANGLE_OFFSET) & WORD_MASK |
                                readUnaligned(space, first + ClassicDialogStructure.RECTANGLE_OFFSET + Integer.BYTES) << Short.SIZE);
        Assertions.assertEquals(ClassicDialogStructure.VISIBLE_FLAG, readUnaligned(space, first + ClassicDialogStructure.FLAGS_OFFSET));
        Assertions.assertEquals(0, (byte) readUnaligned(space, readUnaligned(space, first + ClassicDialogStructure.TEXT_OFFSET)),
                                "a text is an empty string");

        space.writeDword(base + dialog.getSize() - Integer.BYTES, VALUE);

        Assertions.assertEquals(VALUE, space.readDword(base + dialog.getSize() - Integer.BYTES));
    }

    @Test
    void every_dialog_fits_its_dialog_file_and_is_served() {
        ClassicAddressSpace space = createSpace();

        for (ClassicDialogStructure dialog : ClassicDialogStructure.DIALOGS) {
            int base = space.readDword(dialog.getPointer());

            Assertions.assertTrue(ClassicDialogStructure.getControlOffset(dialog.getControlCount()) < dialog.getSize());
            Assertions.assertEquals(base + ClassicDialogStructure.getControlOffset(0),
                                    readUnaligned(space, base + ClassicDialogStructure.FIRST_CONTROL_OFFSET));
        }
    }

    @Test
    void every_game_gets_the_same_addresses_and_the_bytes_enter_the_state_hash() {
        ClassicAddressSpace first = createSpace();
        ClassicAddressSpace second = createSpace();

        Assertions.assertEquals(first.readDword(ClassicMemory.RESOURCE_ICONS), second.readDword(ClassicMemory.RESOURCE_ICONS));
        Assertions.assertEquals(first.readDword(ClassicMemory.GAME_SCREEN + Integer.BYTES),
                                second.readDword(ClassicMemory.GAME_SCREEN + Integer.BYTES));
        Assertions.assertEquals(first.computeProvidedStateHash(HASH_SEED), second.computeProvidedStateHash(HASH_SEED));

        first.writeDword(first.readDword(ClassicMemory.RESOURCE_ICONS), VALUE);

        Assertions.assertNotEquals(first.computeProvidedStateHash(HASH_SEED), second.computeProvidedStateHash(HASH_SEED));
    }

    @Test
    void the_grp_table_names_a_zero_block_per_image_and_takes_writes() {
        ClassicAddressSpace space = createSpace();
        Set<Integer> pointers = new HashSet<>();

        for (int image = 0; image < ClassicGrpTableStructure.GRP_TABLE_ENTRIES; image++) {
            int pointer = space.readDword(ClassicGrpTableStructure.GRP_TABLE + image * Integer.BYTES);

            Assertions.assertTrue(pointers.add(pointer), "every image has a pointer of its own");
            Assertions.assertEquals(0, space.readDword(pointer));
            Assertions.assertEquals(0, space.readDword(pointer + ClassicGrpTableStructure.INSTALLED_GRP_SIZE - Integer.BYTES));
        }

        space.writeDword(ClassicGrpTableStructure.GRP_TABLE, VALUE);

        Assertions.assertEquals(VALUE, space.readDword(ClassicGrpTableStructure.GRP_TABLE));
    }

    @Test
    void the_heap_places_aligned_buffers_in_order_and_refuses_what_does_not_fit() {
        ClassicHeap heap = new ClassicHeap();

        Assertions.assertEquals(ClassicHeap.BASE, heap.allocate(1));
        Assertions.assertEquals(ClassicHeap.BASE + ClassicHeap.ALIGNMENT, heap.allocate(ClassicHeap.ALIGNMENT));
        Assertions.assertThrows(IllegalArgumentException.class, () -> heap.allocate(0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> heap.allocate(ClassicHeap.END - ClassicHeap.BASE));
    }

    @Test
    void a_memory_block_counts_the_writes_that_change_a_byte() {
        ClassicMemoryBlock block = new ClassicMemoryBlock(ClassicHeap.BASE, Integer.BYTES);

        block.writeByte(0, 0);

        Assertions.assertEquals(0, block.getChangeCount());
        Assertions.assertEquals(-1, block.findChangeSince(0, Integer.BYTES, 0));

        block.writeByte(1, 1);

        Assertions.assertEquals(1, block.getChangeCount());
        Assertions.assertEquals(0, block.findChangeSince(0, Integer.BYTES, 0));
        Assertions.assertTrue(block.isPlainMemory());
    }
}
