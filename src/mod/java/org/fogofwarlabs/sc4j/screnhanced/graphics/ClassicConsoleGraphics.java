package org.fogofwarlabs.sc4j.screnhanced.graphics;

import org.fogofwarlabs.sc4j.base.binary.LittleEndianUtils;
import org.fogofwarlabs.sc4j.engine.resource.graphics.SpriteSheet;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrConsolePlacement;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrGamePlugin;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrScreenOverlay;
import org.fogofwarlabs.sc4j.scr.game.trigger.eud.ClassicMemoryView;
import org.fogofwarlabs.sc4j.screnhanced.memory.ClassicConsoleAreaStructure;
import org.fogofwarlabs.sc4j.screnhanced.memory.ClassicDialogStructure;
import org.fogofwarlabs.sc4j.screnhanced.memory.ClassicMemory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The classic console of one game, as a map changes it: the console dialogs and the text box moved through their rectangles, their controls moved
 * through their own rectangles or hidden through their flags, the dialog pictures a map draws, and the console transparency that decides where the
 * console and its dialogs are shown over the game screen.
 * <p>
 * The dialogs are the dialog files of {@link ClassicDialogStructure#DIALOGS} at the addresses their pointers hold. A dialog whose rectangle was
 * written is moved to its left and top; a dialog whose picture size was written shows that picture, one palette index per pixel, rows from the
 * top, at its rectangle instead of its controls. A control whose rectangle differs from its loaded one is moved to its left and top in its dialog;
 * a control without {@link ClassicDialogStructure#VISIBLE_FLAG} is hidden. The owners of the console areas
 * ({@link ClassicConsoleAreaStructure}) show their pictures the same way once their picture sizes were written. The console transparency
 * ({@link ClassicMemory#CONSOLE_TRANSPARENCY_DATA}) holds, for every screen row, pairs of BYTE counts of skipped and shown pixels and a zero pair at
 * the row's end; once written, the console, its dialogs and the pictures are shown only at its shown pixels.
 * <p>
 * The resource icons are the GRP at the address {@link ClassicMemory#RESOURCE_ICONS} holds ({@link ClassicResourceIcons}); once written, the
 * console draws its frames instead of the installed icons.
 * <p>
 * The plugin observes the writes through the game's memory view and never changes the game.
 */
public final class ClassicConsoleGraphics implements ScrGamePlugin {
    /**
     * The placement names of the dialogs of {@link ClassicDialogStructure#DIALOGS}, in their order.
     */
    public static final ScrConsolePlacement.Dialog @NotNull [] PLACED_DIALOGS = {
            ScrConsolePlacement.Dialog.COMMAND_CARD, ScrConsolePlacement.Dialog.SELECTION, ScrConsolePlacement.Dialog.RESOURCES,
            ScrConsolePlacement.Dialog.PORTRAIT, ScrConsolePlacement.Dialog.MINIMAP, ScrConsolePlacement.Dialog.MENU,
            ScrConsolePlacement.Dialog.TEXT_BOX
    };
    /**
     * The size of the picture buffer of a dialog.
     */
    public static final int PICTURE_BUFFER_SIZE = ClassicConsoleAreaStructure.PICTURE_BUFFER_SIZE;

    private static final int TOP_OFFSET = ClassicDialogStructure.RECTANGLE_OFFSET + Short.BYTES;
    private static final int HEIGHT_OFFSET = ClassicDialogStructure.PICTURE_SIZE_OFFSET + Short.BYTES;
    private static final int BYTE_SHIFT = 8;
    private static final int BYTE_MASK = 0xFF;
    private static final int TABLE_ROW_SIZE = 2;
    private static final int TRANSPARENCY_PAIR_SIZE = 2;

    /**
     * One observed dialog or console area owner: its loaded and current bytes, the bytes of its picture buffer and what the map wrote.
     */
    private static final class ObservedDialog {
        private final ScrConsolePlacement.@Nullable Dialog dialog;
        private final @Nullable ClassicDialogStructure structure;
        private final int base;
        private final byte @NotNull [] loaded;
        private final byte @NotNull [] image;
        private final int pictureBase;
        private final byte @NotNull [] picture;

        private boolean rectangleWritten;
        private boolean pictureWritten;
        private boolean controlWritten;

        ObservedDialog(ScrConsolePlacement.@Nullable Dialog dialog, @Nullable ClassicDialogStructure structure, int base, byte @NotNull [] image) {
            this.dialog      = dialog;
            this.structure   = structure;
            this.base        = base;
            this.loaded      = image.clone();
            this.image       = image;
            this.pictureBase = LittleEndianUtils.readInt32(image, ClassicDialogStructure.PICTURE_OFFSET);
            this.picture     = new byte[PICTURE_BUFFER_SIZE];

            this.rectangleWritten = false;
            this.pictureWritten   = false;
            this.controlWritten   = false;
        }

        /**
         * @return the screen column of the dialog's left edge
         */
        int getLeft() {
            return (short) LittleEndianUtils.readUnsigned16(this.image, ClassicDialogStructure.RECTANGLE_OFFSET);
        }

        /**
         * @return the screen row of the dialog's top edge
         */
        int getTop() {
            return (short) LittleEndianUtils.readUnsigned16(this.image, TOP_OFFSET);
        }
    }

    private final @NotNull List<ObservedDialog> dialogs;
    private final @NotNull ScrConsolePlacement placement;
    private final byte @NotNull [] transparency;
    private final boolean @NotNull [] mask;
    private final byte @NotNull [] pixels;
    private final boolean @NotNull [] shown;

    private boolean written;
    private boolean transparencyWritten;
    private boolean changed;

    private final byte @NotNull [] icons;
    private int iconsBase;
    private boolean iconsWritten;
    private boolean iconsChanged;
    private @Nullable SpriteSheet iconSheet;

    /**
     * Creates a {@link ClassicConsoleGraphics} instance with the installed console.
     */
    public ClassicConsoleGraphics() {
        int screenPixels = ClassicMemory.SCREEN_WIDTH * ClassicMemory.SCREEN_HEIGHT;

        this.dialogs      = new ArrayList<>();
        this.placement    = new ScrConsolePlacement(ClassicMemory.SCREEN_WIDTH, ClassicMemory.SCREEN_HEIGHT);
        this.transparency = new byte[ClassicMemory.TRANSPARENCY_DATA_SIZE];
        this.mask         = new boolean[screenPixels];
        this.pixels       = new byte[screenPixels];
        this.shown        = new boolean[screenPixels];

        this.written             = false;
        this.transparencyWritten = false;
        this.changed             = false;

        this.icons        = new byte[ClassicMemory.RESOURCE_ICONS_SIZE];
        this.iconsBase    = 0;
        this.iconsWritten = false;
        this.iconsChanged = false;
        this.iconSheet    = null;
    }

    /**
     * Hears that the game's EUD address space was created: observes the dialogs, their picture buffers, the console transparency and the resource
     * icons.
     *
     * @param memory the read-only view of the address space
     */
    @Override
    public void onAddressSpaceCreated(@NotNull ClassicMemoryView memory) {
        byte[] pointer = new byte[Integer.BYTES];

        for (int index = 0; index < ClassicDialogStructure.DIALOGS.size(); index++) {
            ClassicDialogStructure structure = ClassicDialogStructure.DIALOGS.get(index);

            if (memory.read(structure.getPointer(), pointer, 0, pointer.length) != pointer.length) {
                continue;
            }

            int base = LittleEndianUtils.readInt32(pointer, 0);
            byte[] image = new byte[structure.getSize()];

            if (base == 0 || memory.read(base, image, 0, image.length) != image.length) {
                continue;
            }

            this.observe(memory, new ObservedDialog(PLACED_DIALOGS[index], structure, base, image));
        }

        this.observeConsoleAreas(memory);

        memory.observeWrites(ClassicMemory.CONSOLE_TRANSPARENCY_DATA, ClassicMemory.TRANSPARENCY_DATA_SIZE, this::onTransparencyWrite);

        if (memory.read(ClassicMemory.RESOURCE_ICONS, pointer, 0, pointer.length) == pointer.length) {
            int base = LittleEndianUtils.readInt32(pointer, 0);

            if (base != 0 && memory.read(base, this.icons, 0, this.icons.length) == this.icons.length) {
                this.iconsBase = base;

                memory.observeWrites(base, this.icons.length, this::onIconWrite);
            }
        }
    }

    /**
     * Observes the owners of the console areas that no observed dialog is.
     *
     * @param memory the read-only view of the address space
     */
    private void observeConsoleAreas(@NotNull ClassicMemoryView memory) {
        for (int row = 0; row < ClassicConsoleAreaStructure.TABLES.length; row += TABLE_ROW_SIZE) {
            byte[] table = new byte[ClassicConsoleAreaStructure.TABLES[row + 1]];

            if (memory.read(ClassicConsoleAreaStructure.TABLES[row], table, 0, table.length) != table.length) {
                continue;
            }

            for (int offset = 0; offset + ClassicConsoleAreaStructure.ENTRY_SIZE <= table.length; offset += ClassicConsoleAreaStructure.ENTRY_SIZE) {
                int owner = LittleEndianUtils.readInt32(table, offset + ClassicConsoleAreaStructure.OWNER_OFFSET);
                byte[] image = new byte[ClassicDialogStructure.ENTRY_SIZE];

                if (owner != 0 && !this.isObserved(owner) && memory.read(owner, image, 0, image.length) == image.length) {
                    this.observe(memory, new ObservedDialog(null, null, owner, image));
                }
            }
        }
    }

    private boolean isObserved(int base) {
        for (ObservedDialog observed : this.dialogs) {
            if (observed.base == base) {
                return true;
            }
        }

        return false;
    }

    private void observe(@NotNull ClassicMemoryView memory, @NotNull ObservedDialog observed) {
        this.dialogs.add(observed);
        memory.observeWrites(observed.base, observed.image.length, (address, value) -> this.onDialogWrite(observed, address, value));

        if (observed.pictureBase != 0) {
            memory.observeWrites(observed.pictureBase, PICTURE_BUFFER_SIZE, (address, value) -> this.onPictureWrite(observed, address, value));
        }
    }

    /**
     * Finds the resource icons the map wrote.
     *
     * @return the indexed sheet of the written icons, or {@code null} before the map wrote them or while they are not a GRP
     */
    @Override
    public @Nullable SpriteSheet findResourceIcons() {
        if (!this.iconsWritten) {
            return null;
        }

        if (this.iconsChanged) {
            this.iconsChanged = false;
            this.iconSheet    = ClassicResourceIcons.parse(this.icons);
        }

        return this.iconSheet;
    }

    /**
     * Finds how the map placed the console.
     *
     * @return the placement, or {@code null} while the map wrote no dialog and no transparency
     */
    @Override
    public @Nullable ScrConsolePlacement findConsolePlacement() {
        if (!this.written) {
            return null;
        }

        if (this.changed) {
            this.changed = false;
            this.update();
        }

        return this.placement;
    }

    /**
     * Brings the placement to the written dialogs, controls, transparency and pictures.
     */
    private void update() {
        for (ObservedDialog observed : this.dialogs) {
            ScrConsolePlacement.Dialog dialog = observed.dialog;
            ClassicDialogStructure structure = observed.structure;

            if (dialog == null || structure == null) {
                continue;
            }

            if (observed.pictureWritten) {
                this.placement.hideDialog(dialog);
            } else if (observed.rectangleWritten) {
                this.placement.moveDialog(dialog, observed.getLeft(), observed.getTop());
            }

            if (observed.controlWritten) {
                this.placeControls(dialog, structure, observed);
            }
        }

        if (this.transparencyWritten) {
            decodeTransparency(this.transparency, ClassicMemory.SCREEN_WIDTH, this.mask);

            for (int offset = 0; offset < this.mask.length; offset++) {
                this.placement.setMaskPixel(offset, this.mask[offset]);
            }
        }

        Arrays.fill(this.shown, false);

        for (ObservedDialog observed : this.dialogs) {
            if (observed.pictureWritten) {
                this.drawPicture(observed);
            }
        }

        ScrScreenOverlay surfaces = this.placement.getSurfaces();

        for (int offset = 0; offset < this.shown.length; offset++) {
            if (this.shown[offset]) {
                surfaces.setPixel(offset, this.pixels[offset]);
            } else {
                surfaces.hidePixel(offset);
            }
        }
    }

    /**
     * Places the controls of a dialog whose control entries were written: a control without the visible flag is hidden, a control whose rectangle
     * differs from its loaded one or that was placed before is moved to its left and top in the dialog.
     *
     * @param dialog    the placement name of the dialog
     * @param structure the dialog file
     * @param observed  the dialog
     */
    private void placeControls(ScrConsolePlacement.@NotNull Dialog dialog, @NotNull ClassicDialogStructure structure,
                               @NotNull ObservedDialog observed) {
        for (int control = 0; control < structure.getControlCount(); control++) {
            int offset = ClassicDialogStructure.getControlOffset(control);
            int identifier = structure.getControlIdentifier(control);
            int flags = LittleEndianUtils.readInt32(observed.image, offset + ClassicDialogStructure.FLAGS_OFFSET);
            int left = (short) LittleEndianUtils.readUnsigned16(observed.image, offset + ClassicDialogStructure.RECTANGLE_OFFSET);
            int top = (short) LittleEndianUtils.readUnsigned16(observed.image, offset + TOP_OFFSET);
            boolean moved = left != (short) LittleEndianUtils.readUnsigned16(observed.loaded, offset + ClassicDialogStructure.RECTANGLE_OFFSET) ||
                            top != (short) LittleEndianUtils.readUnsigned16(observed.loaded, offset + TOP_OFFSET);

            if ((flags & ClassicDialogStructure.VISIBLE_FLAG) == 0) {
                this.placement.hideControl(dialog, identifier);
            } else if (moved || this.placement.isControlMoved(dialog, identifier) ||
                       this.placement.isControlHidden(dialog, identifier)) {
                this.placement.moveControl(dialog, identifier, observed.getLeft() + left, observed.getTop() + top);
            }
        }
    }

    /**
     * Draws the picture of a dialog at its rectangle into the composed pixels, where the console is shown and the pixel is not {@code 0}.
     *
     * @param observed the dialog
     */
    private void drawPicture(@NotNull ObservedDialog observed) {
        int left = observed.getLeft();
        int top = observed.getTop();
        int width = LittleEndianUtils.readUnsigned16(observed.image, ClassicDialogStructure.PICTURE_SIZE_OFFSET);
        int height = LittleEndianUtils.readUnsigned16(observed.image, HEIGHT_OFFSET);

        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                int source = row * width + column;
                int x = left + column;
                int y = top + row;

                if (source >= observed.picture.length) {
                    return;
                }

                if (observed.picture[source] != 0 && this.placement.isShown(x, y)) {
                    int offset = y * ClassicMemory.SCREEN_WIDTH + x;

                    this.pixels[offset] = observed.picture[source];
                    this.shown[offset]  = true;
                }
            }
        }
    }

    /**
     * Decodes a console transparency into a mask of the shown pixels.
     *
     * @param data  the transparency data
     * @param width the screen width in pixels
     * @param mask  the mask of one screen, rows from the top, set to the shown pixels
     */
    static void decodeTransparency(byte @NotNull [] data, int width, boolean @NotNull [] mask) {
        Arrays.fill(mask, false);

        int index = 0;

        for (int row = 0; row < mask.length / width; row++) {
            int column = 0;

            while (index + 1 < data.length) {
                int skipped = data[index] & BYTE_MASK;
                int copied = data[index + 1] & BYTE_MASK;

                index += TRANSPARENCY_PAIR_SIZE;

                if (skipped == 0 && copied == 0) {
                    break;
                }

                column += skipped;

                for (int pixel = 0; pixel < copied; pixel++, column++) {
                    if (column < width) {
                        mask[row * width + column] = true;
                    }
                }
            }
        }
    }

    private void onDialogWrite(@NotNull ObservedDialog observed, int address, int value) {
        int offset = address - observed.base;

        for (int index = 0; index < Integer.BYTES; index++) {
            int byteOffset = offset + index;

            if (byteOffset < 0 || byteOffset >= observed.image.length) {
                continue;
            }

            observed.image[byteOffset] = (byte) (value >>> index * BYTE_SHIFT & BYTE_MASK);

            if (byteOffset >= ClassicDialogStructure.ENTRY_SIZE) {
                observed.controlWritten = true;
            } else if (byteOffset >= ClassicDialogStructure.RECTANGLE_OFFSET && byteOffset < ClassicDialogStructure.PICTURE_SIZE_OFFSET) {
                observed.rectangleWritten = true;
            } else if (byteOffset >= ClassicDialogStructure.PICTURE_SIZE_OFFSET && byteOffset < ClassicDialogStructure.PICTURE_OFFSET) {
                observed.pictureWritten = true;
            }
        }

        this.written = true;
        this.changed = true;
    }

    private void onPictureWrite(@NotNull ObservedDialog observed, int address, int value) {
        int offset = address - observed.pictureBase;

        for (int index = 0; index < Integer.BYTES; index++) {
            if (offset + index >= 0 && offset + index < observed.picture.length) {
                observed.picture[offset + index] = (byte) (value >>> index * BYTE_SHIFT & BYTE_MASK);
            }
        }

        this.changed |= observed.pictureWritten;
    }

    private void onIconWrite(int address, int value) {
        int offset = address - this.iconsBase;

        for (int index = 0; index < Integer.BYTES; index++) {
            if (offset + index >= 0 && offset + index < this.icons.length) {
                this.icons[offset + index] = (byte) (value >>> index * BYTE_SHIFT & BYTE_MASK);
            }
        }

        this.iconsWritten = true;
        this.iconsChanged = true;
    }

    private void onTransparencyWrite(int address, int value) {
        int offset = address - ClassicMemory.CONSOLE_TRANSPARENCY_DATA;

        for (int index = 0; index < Integer.BYTES; index++) {
            if (offset + index >= 0 && offset + index < this.transparency.length) {
                this.transparency[offset + index] = (byte) (value >>> index * BYTE_SHIFT & BYTE_MASK);
            }
        }

        this.transparencyWritten = true;
        this.written             = true;
        this.changed             = true;
    }
}
