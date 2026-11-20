package org.fogofwarlabs.sc4j.screnhanced.script.game;

import org.fogofwarlabs.sc4j.scr.game.trigger.TriggerContext;
import org.fogofwarlabs.sc4j.scr.game.trigger.TriggerLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.util.function.ToIntFunction;

/**
 * Defines {@code core.location}: the map locations by their index in the location records, from {@code 0} to {@code getCount()} minus one.
 * <p>
 * {@code getCount()}, {@code getName(id)} (the map string of the location, {@code null} without one), {@code findByName(name)} (the first
 * location with the name, {@code -1} for none), {@code getLeft}, {@code getTop}, {@code getRight}, {@code getBottom}, {@code getCenterX},
 * {@code getCenterY}, {@code setBounds(id, left, top, right, bottom)} and {@code setCenter(id, x, y)}. The bounds are map pixels;
 * {@code setBounds} writes the four bound DWORDs of the location record through the trigger state's game-data bytes, the way an EUD write of
 * the location table does; {@code setCenter} moves the location as Move Location moves its destination, kept inside the map.
 */
final class ScriptLocationCore {
    /**
     * The offsets of the four bound DWORDs in a location record, in record order: left, top, right, bottom.
     */
    private static final int @NotNull [] BOUND_OFFSETS = {0, 4, 8, 12};

    private final @NotNull ScriptGame scriptGame;

    /**
     * Creates a {@link ScriptLocationCore} instance.
     *
     * @param scriptGame the game binding
     */
    ScriptLocationCore(@NotNull ScriptGame scriptGame) {
        this.scriptGame = scriptGame;
    }

    /**
     * Defines the functions of {@code core.location}.
     *
     * @param namespace the namespace object
     * @param global    the global scope
     */
    void define(@NotNull Scriptable namespace, @NotNull Scriptable global) {
        ScriptCoreUtils.defineFunction(global, namespace, "getCount", 0,
                                       (context, scope, thisObject, arguments) -> (double) TriggerContext.LOCATION_COUNT);
        ScriptCoreUtils.defineFunction(global, namespace, "getName", 1,
                                       (context, scope, thisObject, arguments) -> this.findName(this.readLocationId(arguments, 0)));
        ScriptCoreUtils.defineFunction(global, namespace, "findByName", 1, (context, scope, thisObject, arguments) -> {
            String name = ScriptCoreUtils.readString(arguments, 0);

            for (int id = 0; id < TriggerContext.LOCATION_COUNT; id++) {
                if (name.equals(this.findName(id))) {
                    return (double) id;
                }
            }

            return (double) ScriptCoreUtils.NO_ID;
        });

        this.defineRead(namespace, global, "getLeft", TriggerLocation::getLeft);
        this.defineRead(namespace, global, "getTop", TriggerLocation::getTop);
        this.defineRead(namespace, global, "getRight", TriggerLocation::getRight);
        this.defineRead(namespace, global, "getBottom", TriggerLocation::getBottom);
        this.defineRead(namespace, global, "getCenterX", TriggerLocation::getCenterX);
        this.defineRead(namespace, global, "getCenterY", TriggerLocation::getCenterY);

        ScriptCoreUtils.defineFunction(global, namespace, "setBounds", 5, (context, scope, thisObject, arguments) -> {
            this.scriptGame.checkWritable();

            int id = this.readLocationId(arguments, 0);
            TriggerContext triggers = this.scriptGame.requireTriggerContext();
            int record = TriggerContext.LOCATIONS_OFFSET + id * TriggerContext.LOCATION_RECORD_SIZE;

            for (int bound = 0; bound < BOUND_OFFSETS.length; bound++) {
                int value = ScriptCoreUtils.readInteger(arguments, bound + 1);

                for (int byteIndex = 0; byteIndex < Integer.BYTES; byteIndex++) {
                    triggers.writeGameDataByte(record + BOUND_OFFSETS[bound] + byteIndex, value >>> byteIndex * Byte.SIZE);
                }
            }

            return Context.getUndefinedValue();
        });
        ScriptCoreUtils.defineFunction(global, namespace, "setCenter", 3, (context, scope, thisObject, arguments) -> {
            this.scriptGame.checkWritable();

            int id = this.readLocationId(arguments, 0);
            int centerX = ScriptCoreUtils.readInteger(arguments, 1);
            int centerY = ScriptCoreUtils.readInteger(arguments, 2);
            TriggerLocation location = this.scriptGame.requireTriggerContext().getLocation(id);

            this.scriptGame.getGame().getTriggerLocations().moveLocation(location, centerX, centerY);

            return Context.getUndefinedValue();
        });
    }

    /**
     * Defines a read of a location that returns an integer.
     */
    private void defineRead(@NotNull Scriptable namespace, @NotNull Scriptable global, @NotNull String name,
                            @NotNull ToIntFunction<TriggerLocation> read) {
        ScriptCoreUtils.defineFunction(global, namespace, name, 1, (context, scope, thisObject, arguments) -> {
            int id = this.readLocationId(arguments, 0);

            return (double) read.applyAsInt(this.scriptGame.requireTriggerContext().getLocation(id));
        });
    }

    private int readLocationId(@NotNull Object @NotNull [] arguments, int index) {
        return ScriptCoreUtils.requireId("location", ScriptCoreUtils.readInteger(arguments, index), TriggerContext.LOCATION_COUNT);
    }

    /**
     * Finds the name of a location: the map string of its string index.
     *
     * @param id the location id
     * @return the name, or {@code null} if the location has no string or the map strings are unreadable
     */
    private @Nullable String findName(int id) {
        int stringIndex = this.scriptGame.requireTriggerContext().getLocation(id).getStringIndex();

        if (stringIndex == 0) {
            return null;
        }

        String name = this.scriptGame.getGame().getInterfaceLink().getMessageLines().getMapString(stringIndex);

        return name == null || name.isEmpty() ? null : name;
    }
}
