package org.fogofwarlabs.sc4j.screnhanced.script.input;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrInputModifiers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One key or mouse button change of a player that SC:R Enhanced Script sends as a mod command, and its encoding.
 * <p>
 * The data is three bytes, or seven with a map position: the kind tag ({@link Kind#getTag()}), the code (the Windows virtual-key code of a
 * key, or the button: {@code 0} left, {@code 1} right, {@code 2} middle), the flags (bit {@code 0} pressed, bits {@code 1} to {@code 3} the
 * {@link ScrInputModifiers} bits, bit {@code 4} a position follows), then the map X and Y in pixels as unsigned little-endian 16-bit numbers.
 * A mouse command always has a position; a key command has one when the mouse is tracked, the map position under the pointer at the change.
 */
public final class ScriptInputCommand {
    /**
     * The number of Windows virtual-key codes.
     */
    public static final int KEY_COUNT = 0x100;
    /**
     * The number of mouse buttons: left, right and middle.
     */
    public static final int BUTTON_COUNT = 3;
    /**
     * The modifier bits a command keeps: {@link ScrInputModifiers#SHIFT}, {@link ScrInputModifiers#CONTROL} and {@link ScrInputModifiers#ALT}.
     */
    public static final int MODIFIER_MASK = ScrInputModifiers.SHIFT | ScrInputModifiers.CONTROL | ScrInputModifiers.ALT;
    /**
     * The largest map coordinate a command holds.
     */
    public static final int MAXIMUM_COORDINATE = 0xFFFF;

    private static final int BYTE_MASK = 0xFF;
    private static final int PRESSED_FLAG = 1;
    private static final int MODIFIER_SHIFT = 1;
    private static final int POSITION_FLAG = 1 << 4;
    private static final int FLAG_MASK = PRESSED_FLAG | MODIFIER_MASK << MODIFIER_SHIFT | POSITION_FLAG;
    private static final int KIND_OFFSET = 0;
    private static final int CODE_OFFSET = 1;
    private static final int FLAGS_OFFSET = 2;
    private static final int X_OFFSET = 3;
    private static final int Y_OFFSET = 5;
    private static final int LENGTH = 3;
    private static final int POSITIONED_LENGTH = 7;

    /**
     * The kind of an input command.
     */
    public enum Kind {
        /**
         * A key change; the code is the Windows virtual-key code.
         */
        KEY(0xE1),
        /**
         * A mouse button change; the code is the button.
         */
        MOUSE(0xE2);

        private final int tag;

        Kind(int tag) {
            this.tag = tag;
        }

        /**
         * @return the first byte of the kind's commands
         */
        public int getTag() {
            return this.tag;
        }
    }


    /**
     * Creates a {@link ScriptInputCommand} instance of a key change without a position.
     *
     * @param virtualKey the Windows virtual-key code
     * @param pressed    {@code true} if the key went down, {@code false} if it went up
     * @param modifiers  the held modifier keys ({@link ScrInputModifiers} bits; other bits are dropped)
     * @return the created {@link ScriptInputCommand} instance
     * @throws IllegalArgumentException if the virtual-key code is outside {@code 0..0xFF}
     */
    public static @NotNull ScriptInputCommand ofKey(int virtualKey, boolean pressed, int modifiers) {
        return create(Kind.KEY, virtualKey, KEY_COUNT, pressed, modifiers, false, 0, 0);
    }

    /**
     * Creates a {@link ScriptInputCommand} instance of a key change with the map position under the pointer.
     *
     * @param virtualKey the Windows virtual-key code
     * @param pressed    {@code true} if the key went down, {@code false} if it went up
     * @param modifiers  the held modifier keys ({@link ScrInputModifiers} bits; other bits are dropped)
     * @param x          the map X in pixels
     * @param y          the map Y in pixels
     * @return the created {@link ScriptInputCommand} instance
     * @throws IllegalArgumentException if the virtual-key code is outside {@code 0..0xFF} or a coordinate outside
     *                                  {@code 0..}{@value #MAXIMUM_COORDINATE}
     */
    public static @NotNull ScriptInputCommand ofKey(int virtualKey, boolean pressed, int modifiers, int x, int y) {
        return create(Kind.KEY, virtualKey, KEY_COUNT, pressed, modifiers, true, x, y);
    }

    /**
     * Creates a {@link ScriptInputCommand} instance of a mouse button change.
     *
     * @param button    the button: {@code 0} left, {@code 1} right, {@code 2} middle
     * @param pressed   {@code true} if the button went down, {@code false} if it went up
     * @param modifiers the held modifier keys ({@link ScrInputModifiers} bits; other bits are dropped)
     * @param x         the map X under the pointer in pixels
     * @param y         the map Y under the pointer in pixels
     * @return the created {@link ScriptInputCommand} instance
     * @throws IllegalArgumentException if the button is not a button or a coordinate is outside {@code 0..}{@value #MAXIMUM_COORDINATE}
     */
    public static @NotNull ScriptInputCommand ofMouse(int button, boolean pressed, int modifiers, int x, int y) {
        return create(Kind.MOUSE, button, BUTTON_COUNT, pressed, modifiers, true, x, y);
    }

    /**
     * Decodes the data of a mod command.
     *
     * @param data the mod command data
     * @return the decoded command, or {@code null} if the data is not a well-formed input command
     */
    public static @Nullable ScriptInputCommand decode(byte @NotNull [] data) {
        if (data.length != LENGTH && data.length != POSITIONED_LENGTH) {
            return null;
        }

        int tag = data[KIND_OFFSET] & BYTE_MASK;
        Kind kind = tag == Kind.KEY.getTag() ? Kind.KEY : tag == Kind.MOUSE.getTag() ? Kind.MOUSE : null;
        int code = data[CODE_OFFSET] & BYTE_MASK;
        int flags = data[FLAGS_OFFSET] & BYTE_MASK;
        boolean positioned = (flags & POSITION_FLAG) != 0;

        if (kind == null || (flags & ~FLAG_MASK) != 0 || positioned != (data.length == POSITIONED_LENGTH)) {
            return null;
        }

        if (kind == Kind.MOUSE && (!positioned || code >= BUTTON_COUNT)) {
            return null;
        }

        int x = positioned ? readUnsignedShort(data, X_OFFSET) : 0;
        int y = positioned ? readUnsignedShort(data, Y_OFFSET) : 0;

        return new ScriptInputCommand(kind, code, (flags & PRESSED_FLAG) != 0, flags >> MODIFIER_SHIFT & MODIFIER_MASK, positioned, x, y);
    }

    private static @NotNull ScriptInputCommand create(@NotNull Kind kind, int code, int codeCount, boolean pressed, int modifiers,
                                                      boolean positioned, int x, int y) {
        if (code < 0 || code >= codeCount) {
            throw Exceptions.newIllegalArgumentException("The input code {0} is not from 0 to {1}.", code, codeCount - 1);
        }

        if (x < 0 || x > MAXIMUM_COORDINATE || y < 0 || y > MAXIMUM_COORDINATE) {
            throw Exceptions.newIllegalArgumentException("The input position ({0}, {1}) is not inside 0 to {2}.", x, y, MAXIMUM_COORDINATE);
        }

        return new ScriptInputCommand(kind, code, pressed, modifiers & MODIFIER_MASK, positioned, x, y);
    }

    private static int readUnsignedShort(byte @NotNull [] data, int offset) {
        return data[offset] & BYTE_MASK | (data[offset + 1] & BYTE_MASK) << Byte.SIZE;
    }

    private static void writeShort(byte @NotNull [] data, int offset, int value) {
        data[offset]     = (byte) value;
        data[offset + 1] = (byte) (value >> Byte.SIZE);
    }


    private final @NotNull Kind kind;
    private final int code;
    private final boolean pressed;
    private final int modifiers;

    private final boolean positioned;
    private final int x;
    private final int y;

    private ScriptInputCommand(@NotNull Kind kind, int code, boolean pressed, int modifiers, boolean positioned, int x, int y) {
        this.kind      = kind;
        this.code      = code;
        this.pressed   = pressed;
        this.modifiers = modifiers;

        this.positioned = positioned;
        this.x          = x;
        this.y          = y;
    }

    /**
     * @return the kind
     */
    public @NotNull Kind getKind() {
        return this.kind;
    }

    /**
     * @return the Windows virtual-key code of a key command, the button of a mouse command
     */
    public int getCode() {
        return this.code;
    }

    /**
     * @return {@code true} if the key or button went down, {@code false} if it went up
     */
    public boolean isPressed() {
        return this.pressed;
    }

    /**
     * @return the held modifier keys ({@link ScrInputModifiers} bits)
     */
    public int getModifiers() {
        return this.modifiers;
    }

    /**
     * @return {@code true} if the command has a map position, {@code false} otherwise
     */
    public boolean isPositioned() {
        return this.positioned;
    }

    /**
     * @return the map X in pixels, {@code 0} without a position
     */
    public int getX() {
        return this.x;
    }

    /**
     * @return the map Y in pixels, {@code 0} without a position
     */
    public int getY() {
        return this.y;
    }


    /**
     * Encodes the command as mod command data.
     *
     * @return the encoded data, three bytes, or seven with a position
     */
    public byte @NotNull [] encode() {
        byte[] data = new byte[this.positioned ? POSITIONED_LENGTH : LENGTH];
        int flags = (this.pressed ? PRESSED_FLAG : 0) | this.modifiers << MODIFIER_SHIFT | (this.positioned ? POSITION_FLAG : 0);

        data[KIND_OFFSET]  = (byte) this.kind.getTag();
        data[CODE_OFFSET]  = (byte) this.code;
        data[FLAGS_OFFSET] = (byte) flags;

        if (this.positioned) {
            writeShort(data, X_OFFSET, this.x);
            writeShort(data, Y_OFFSET, this.y);
        }

        return data;
    }
}
