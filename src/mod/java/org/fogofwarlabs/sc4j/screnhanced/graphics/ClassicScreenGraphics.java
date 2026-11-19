package org.fogofwarlabs.sc4j.screnhanced.graphics;

import org.fogofwarlabs.sc4j.base.binary.LittleEndianUtils;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrGamePlugin;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrScreenOverlay;
import org.fogofwarlabs.sc4j.scr.game.trigger.eud.ClassicMemoryView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The classic game screen of one game, as a map changes it: the pixels written to the game screen bitmap.
 * <p>
 * The game screen bitmap is the header at {@link #GAME_SCREEN} (WORD width, WORD height, DWORD pixel address) and its pixels, one palette index
 * per pixel, rows from the top; every written pixel is shown over the game screen from then on, under the console.
 * <p>
 * The plugin observes the writes through the game's memory view and never changes the game.
 */
public final class ClassicScreenGraphics implements ScrGamePlugin {
    /**
     * The game screen bitmap header.
     */
    public static final int GAME_SCREEN = 0x6CEFF0;

    private static final int HEADER_SIZE = 8;
    private static final int HEIGHT_OFFSET = 2;
    private static final int PIXELS_OFFSET = 4;
    private static final int BYTE_SHIFT = 8;
    private static final int BYTE_MASK = 0xFF;

    private @Nullable ScrScreenOverlay overlay;
    private int pixelsBase;

    /**
     * Creates a {@link ClassicScreenGraphics} instance with no shown pixel.
     */
    public ClassicScreenGraphics() {
        this.overlay    = null;
        this.pixelsBase = 0;
    }

    /**
     * Hears that the game's EUD address space was created: observes the game screen pixels.
     *
     * @param memory the read-only view of the address space
     */
    @Override
    public void onAddressSpaceCreated(@NotNull ClassicMemoryView memory) {
        byte[] header = new byte[HEADER_SIZE];

        if (memory.read(GAME_SCREEN, header, 0, header.length) == header.length) {
            int width = LittleEndianUtils.readUnsigned16(header, 0);
            int height = LittleEndianUtils.readUnsigned16(header, HEIGHT_OFFSET);
            int pixels = LittleEndianUtils.readInt32(header, PIXELS_OFFSET);

            if (width > 0 && height > 0 && pixels != 0) {
                this.overlay    = new ScrScreenOverlay(width, height);
                this.pixelsBase = pixels;

                memory.observeWrites(pixels, width * height, this::onPixelWrite);
            }
        }
    }

    /**
     * Finds the written game screen pixels.
     *
     * @return the overlay of the written pixels, or {@code null} before the first write
     */
    @Override
    public @Nullable ScrScreenOverlay findScreenOverlay() {
        ScrScreenOverlay current = this.overlay;

        return current == null || current.getShownCount() == 0 ? null : current;
    }

    /**
     * Shows the four pixels of a written DWORD.
     *
     * @param address the written classic address
     * @param value   the stored DWORD
     */
    private void onPixelWrite(int address, int value) {
        ScrScreenOverlay current = this.overlay;

        if (current == null) {
            return;
        }

        int offset = address - this.pixelsBase;

        for (int index = 0; index < Integer.BYTES; index++) {
            if (offset + index < current.getWidth() * current.getHeight()) {
                current.setPixel(offset + index, value >>> index * BYTE_SHIFT & BYTE_MASK);
            }
        }
    }
}
