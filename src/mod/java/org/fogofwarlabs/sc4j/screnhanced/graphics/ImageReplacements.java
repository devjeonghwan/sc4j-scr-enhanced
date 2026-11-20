package org.fogofwarlabs.sc4j.screnhanced.graphics;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.scr.content.ScrImages;
import org.fogofwarlabs.sc4j.scr.game.sprite.Sprite;
import org.fogofwarlabs.sc4j.scr.game.sprite.SpriteImage;
import org.fogofwarlabs.sc4j.scr.game.unit.Unit;
import org.fogofwarlabs.sc4j.scr.game.unit.UnitPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * The image graphics a game's simulation replaces ({@link ImageGraphics}): per image type, for every image of the type, and per unit and image
 * type, for the images of that type that belong to one unit. An image belongs to a unit when it is an image of the unit's sprite or of the
 * sprite of the unit's subunit; it is resolved when the image is drawn, so the images a unit gets later, as when it sieges, burrows or shows an
 * overlay, draw the unit's graphics too. A unit's graphics win over those of the image type.
 * <p>
 * A unit's graphics last while its unit pool slot holds the same unit (the slot is allocated and keeps its generation), its death animation
 * included; {@link #removeFreedUnits()} forgets those of freed slots. The replacements change only what is drawn, never the simulation.
 * <p>
 * The GRPs and anims are read from byte ranges that are not copied ({@link #readGrp}, {@link #readAnim}); the parsed anims of the game are kept
 * within {@code 256} MiB, as those of the classic image graphics.
 */
public final class ImageReplacements {
    /**
     * The graphics of one unit's images of one image type.
     */
    private static final class UnitReplacement {
        private final @NotNull Unit unit;
        private final int generation;
        private final int imageTypeId;

        private @NotNull ImageGraphics graphics;

        private UnitReplacement(@NotNull Unit unit, int generation, int imageTypeId, @NotNull ImageGraphics graphics) {
            this.unit        = unit;
            this.generation  = generation;
            this.imageTypeId = imageTypeId;

            this.graphics = graphics;
        }
    }


    /**
     * Checks whether a replacement's unit is still in its slot.
     *
     * @return {@code true} if the slot is allocated and keeps the generation of the replacement's unit, {@code false} otherwise
     */
    private static boolean isHeld(@NotNull UnitPool pool, @NotNull UnitReplacement replacement) {
        int index = replacement.unit.getIndex();

        return pool.isAllocated(index) && pool.getGeneration(index) == replacement.generation;
    }

    /**
     * Checks whether a sprite is the sprite of a unit or of its subunit.
     */
    private static boolean isUnitSprite(@NotNull Unit unit, @NotNull Sprite sprite) {
        if (unit.getSpriteOrNull() == sprite) {
            return true;
        }

        Unit subunit = unit.getSubunit();

        return subunit != null && subunit.getSpriteOrNull() == sprite;
    }

    private static int requireImageTypeId(int imageTypeId) {
        if (imageTypeId < 0 || imageTypeId >= ScrImages.COUNT) {
            throw Exceptions.newIllegalArgumentException("The image type {0} is not from 0 to {1}.", imageTypeId, ScrImages.COUNT - 1);
        }

        return imageTypeId;
    }


    private final @NotNull Supplier<@Nullable UnitPool> units;
    private final EmbeddedAnim.@NotNull Cache animations;

    /**
     * The graphics per image type, {@code null} for the installed graphics.
     */
    private final @Nullable ImageGraphics @NotNull [] typeGraphics;
    private final @NotNull List<UnitReplacement> unitReplacements;

    /**
     * Creates an {@link ImageReplacements} instance without replacements.
     *
     * @param units the game's unit pool, {@code null} before the game has one
     */
    public ImageReplacements(@NotNull Supplier<@Nullable UnitPool> units) {
        this.units      = units;
        this.animations = new EmbeddedAnim.Cache(ClassicImageGraphics.ANIMATION_BUDGET);

        this.typeGraphics     = new ImageGraphics[ScrImages.COUNT];
        this.unitReplacements = new ArrayList<>();
    }

    /**
     * Reads a GRP file from a byte range ({@link ImageGrp#read}).
     *
     * @param data   the bytes, not copied and not changed afterwards
     * @param offset the first byte of the GRP file
     * @param length the size of the GRP file
     * @param name   the name of the GRP for messages
     * @return the read {@link ImageGrp} instance
     * @throws IOException               if the bytes are no GRP header with a readable frame table or a frame cannot be decoded
     * @throws IndexOutOfBoundsException if the range lies outside the bytes
     */
    public @NotNull ImageGrp readGrp(byte @NotNull [] data, int offset, int length, @NotNull String name) throws IOException {
        return ImageGrp.read(data, offset, length, name);
    }

    /**
     * Reads an anim from a byte range after checking that it is a single-entry anim of asset scale {@code 4} or {@code 2} with a canvas and a
     * block-compressed {@code diffuse} layer whose textures parse; the texture sheet drawn is parsed again when the anim is first drawn.
     *
     * @param data   the bytes, not copied and not changed afterwards
     * @param offset the first byte of the anim
     * @param length the size of the anim
     * @param name   the name of the anim for messages
     * @return the read {@link ImageAnim} instance
     * @throws IOException               if the bytes are no such anim
     * @throws IndexOutOfBoundsException if the range lies outside the bytes
     */
    public @NotNull ImageAnim readAnim(byte @NotNull [] data, int offset, int length, @NotNull String name) throws IOException {
        int scale;

        try {
            scale = EmbeddedAnim.check(Arrays.copyOfRange(data, offset, Math.addExact(offset, length)));
        } catch (IOException | RuntimeException exception) {
            throw Exceptions.newIOException(exception, "The anim {0} is no single-entry HD or HD2 anim with frames on a canvas and a diffuse layer.",
                                            name);
        }

        return new ImageAnim(name, scale, new EmbeddedAnim(EmbeddedMemory.of(data, offset, length), 0, this.animations));
    }

    /**
     * Finds the graphics of an image type.
     *
     * @param imageTypeId the image type, an {@code images.dat} entry
     * @return the graphics, or {@code null} for the installed graphics
     * @throws IllegalArgumentException if the image type is outside {@code images.dat}
     */
    public @Nullable ImageGraphics findImageGraphics(int imageTypeId) {
        return this.typeGraphics[requireImageTypeId(imageTypeId)];
    }

    /**
     * Finds the graphics of one unit's images of an image type.
     *
     * @param unit        the unit
     * @param imageTypeId the image type, an {@code images.dat} entry
     * @return the graphics, or {@code null} if the unit's slot holds another unit or none, or the unit has no graphics of the image type
     * @throws IllegalArgumentException if the image type is outside {@code images.dat}
     */
    public @Nullable ImageGraphics findUnitImageGraphics(@NotNull Unit unit, int imageTypeId) {
        requireImageTypeId(imageTypeId);

        UnitReplacement replacement = this.findUnitReplacement(unit, imageTypeId);

        return replacement == null ? null : replacement.graphics;
    }

    /**
     * @return the number of units' image types with graphics, those of freed slots included until {@link #removeFreedUnits()}
     */
    public int getUnitReplacementCount() {
        return this.unitReplacements.size();
    }


    /**
     * Replaces the graphics of every image of an image type.
     *
     * @param imageTypeId the image type, an {@code images.dat} entry
     * @param graphics    the graphics
     * @throws IllegalArgumentException if the image type is outside {@code images.dat}
     */
    public void replaceImage(int imageTypeId, @NotNull ImageGraphics graphics) {
        this.typeGraphics[requireImageTypeId(imageTypeId)] = graphics;
    }

    /**
     * Restores the installed graphics of the images of an image type, except for the units with graphics of their own.
     *
     * @param imageTypeId the image type, an {@code images.dat} entry
     * @throws IllegalArgumentException if the image type is outside {@code images.dat}
     */
    public void restoreImage(int imageTypeId) {
        this.typeGraphics[requireImageTypeId(imageTypeId)] = null;
    }

    /**
     * Replaces the graphics of a unit's images of an image type, for as long as the unit's slot holds the unit.
     *
     * @param unit        the unit, in an allocated slot of the game's unit pool
     * @param imageTypeId the image type, an {@code images.dat} entry
     * @param graphics    the graphics
     * @throws IllegalArgumentException if the image type is outside {@code images.dat} or the unit's slot is not allocated
     * @throws IllegalStateException    if the game has no unit pool
     */
    public void replaceUnitImage(@NotNull Unit unit, int imageTypeId, @NotNull ImageGraphics graphics) {
        requireImageTypeId(imageTypeId);

        UnitPool pool = this.requireUnitPool();

        if (!pool.isAllocated(unit.getIndex())) {
            throw Exceptions.newIllegalArgumentException("The unit slot {0} is not allocated.", unit.getIndex());
        }

        UnitReplacement replacement = this.findUnitReplacement(unit, imageTypeId);

        if (replacement != null) {
            replacement.graphics = graphics;
        } else {
            this.unitReplacements.add(new UnitReplacement(unit, pool.getGeneration(unit.getIndex()), imageTypeId, graphics));
        }
    }

    /**
     * Restores the graphics of a unit's images of an image type: those of the image type, else the installed graphics.
     *
     * @param unit        the unit
     * @param imageTypeId the image type, an {@code images.dat} entry
     * @throws IllegalArgumentException if the image type is outside {@code images.dat}
     */
    public void restoreUnitImage(@NotNull Unit unit, int imageTypeId) {
        requireImageTypeId(imageTypeId);

        UnitReplacement replacement = this.findUnitReplacement(unit, imageTypeId);

        if (replacement != null) {
            this.unitReplacements.remove(replacement);
        }
    }

    /**
     * Forgets the graphics of the units whose slot was freed or holds another unit.
     */
    public void removeFreedUnits() {
        if (this.unitReplacements.isEmpty()) {
            return;
        }

        UnitPool pool = this.units.get();

        this.unitReplacements.removeIf(replacement -> pool == null || !isHeld(pool, replacement));
    }

    /**
     * Finds the graphics a sprite image draws instead of its installed graphics: those of the unit it belongs to and its image type, else those
     * of its image type.
     *
     * @param image an image of a drawn sprite
     * @return the graphics, or {@code null} for the installed graphics
     */
    public @Nullable ImageGraphics findGraphics(@NotNull SpriteImage image) {
        int imageTypeId = image.getImageTypeId();

        if (imageTypeId < 0 || imageTypeId >= ScrImages.COUNT) {
            return null;
        }

        if (!this.unitReplacements.isEmpty()) {
            Sprite sprite = image.getSprite();
            UnitPool pool = this.units.get();

            if (sprite != null && pool != null) {
                for (UnitReplacement replacement : this.unitReplacements) {
                    if (replacement.imageTypeId == imageTypeId && isHeld(pool, replacement) && isUnitSprite(replacement.unit, sprite)) {
                        return replacement.graphics;
                    }
                }
            }
        }

        return this.typeGraphics[imageTypeId];
    }

    private @Nullable UnitReplacement findUnitReplacement(@NotNull Unit unit, int imageTypeId) {
        UnitPool pool = this.units.get();

        if (pool == null) {
            return null;
        }

        for (UnitReplacement replacement : this.unitReplacements) {
            if (replacement.unit == unit && replacement.imageTypeId == imageTypeId && isHeld(pool, replacement)) {
                return replacement;
            }
        }

        return null;
    }

    private @NotNull UnitPool requireUnitPool() {
        UnitPool pool = this.units.get();

        if (pool == null) {
            throw Exceptions.newIllegalStateException("The game has no unit pool.");
        }

        return pool;
    }
}
