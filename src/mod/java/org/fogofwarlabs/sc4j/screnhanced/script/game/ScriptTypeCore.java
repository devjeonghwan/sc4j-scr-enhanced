package org.fogofwarlabs.sc4j.screnhanced.script.game;

import org.fogofwarlabs.sc4j.engine.content.type.ContentType;
import org.fogofwarlabs.sc4j.scr.content.ScrImages;
import org.fogofwarlabs.sc4j.scr.content.ScrOrders;
import org.fogofwarlabs.sc4j.scr.content.ScrTechs;
import org.fogofwarlabs.sc4j.scr.content.ScrUnits;
import org.fogofwarlabs.sc4j.scr.content.ScrUpgrades;
import org.fogofwarlabs.sc4j.scr.content.ScrWeapons;
import org.fogofwarlabs.sc4j.scr.data.format.dat.DatTable;
import org.fogofwarlabs.sc4j.scr.data.format.dat.UnitsDat;
import org.fogofwarlabs.sc4j.scr.data.format.dat.WeaponsDat;
import org.fogofwarlabs.sc4j.scr.game.sprite.SpriteImage;
import org.fogofwarlabs.sc4j.scr.game.state.GameData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Defines the type namespaces of {@code core}: {@code unitType}, {@code weaponType}, {@code upgradeType}, {@code techType},
 * {@code orderType} and {@code imageType}, the entries of {@code units.dat}, {@code weapons.dat}, {@code upgrades.dat}, {@code techdata.dat},
 * {@code orders.dat} and {@code images.dat} by their index.
 * <p>
 * Each has {@code getCount()} and {@code getName(id)}, the original data name of the entry ({@code "Terran Marine"}). The {@code images.dat}
 * entries have no names, so an image type is named after its GRP path in {@code images.tbl} when the game starts: the path without the
 * {@code .grp} extension, each part between {@code /} and {@code \} with its first letter in upper case ({@code unit/terran/marine.grp} is
 * {@code UnitTerranMarine}). An image that shares its GRP path with another image and has a drawing function other than the normal one gets
 * the drawing function's name ({@code UnitProtossArbiterShadow}); a name that is still taken gets {@code 2}, {@code 3} and so on in entry
 * order ({@code UnitNeutralGeyser2}). An image without a valid GRP path, and every image of a game without game data, is named
 * {@code Image<entry>}.
 * <p>
 * {@code unitType} also has {@code getMaxHp} and {@code setMaxHp} (raw {@code 1/256} units), {@code getMaxShields}, {@code setMaxShields},
 * {@code getArmor}, {@code setArmor}, {@code getGroundWeapon} and {@code getAirWeapon} ({@code -1} for none); {@code weaponType} has
 * {@code getDamage}, {@code setDamage}, {@code getDamageBonus} and {@code setDamageBonus}. A write changes the data table of the current game, which the map
 * settings and EUD writes change too, with the field's width.
 */
final class ScriptTypeCore {
    private static final @NotNull String GRP_EXTENSION = ".grp";
    private static final @NotNull String GRP_PATH_SEPARATORS = "[/\\\\]";
    private static final @NotNull String IMAGE_NAME_PREFIX = "Image";
    /**
     * The number the second image with a taken name gets.
     */
    private static final int FIRST_NAME_NUMBER = 2;
    /**
     * The name suffixes of the drawing functions, for an image that shares its GRP path.
     */
    private static final @NotNull Map<Integer, String> DRAW_FUNCTION_SUFFIXES = Map.ofEntries(
            Map.entry(SpriteImage.MODIFIER_CLOAKING, "Cloaking"),
            Map.entry(SpriteImage.MODIFIER_CLOAKED, "Cloaked"),
            Map.entry(SpriteImage.MODIFIER_DECLOAKING, "Decloaking"),
            Map.entry(SpriteImage.MODIFIER_DETECTED_CLOAKING, "DetectedCloaking"),
            Map.entry(SpriteImage.MODIFIER_DETECTED_CLOAKED, "DetectedCloaked"),
            Map.entry(SpriteImage.MODIFIER_DETECTED_DECLOAKING, "DetectedDecloaking"),
            Map.entry(SpriteImage.MODIFIER_REMAPPING, "Remapping"),
            Map.entry(SpriteImage.MODIFIER_SHADOW, "Shadow"),
            Map.entry(SpriteImage.MODIFIER_HEALTH_BAR, "HealthBar"),
            Map.entry(SpriteImage.MODIFIER_WARP_TEXTURE, "WarpTexture"),
            Map.entry(SpriteImage.MODIFIER_SELECTION_CIRCLE, "SelectionCircle"),
            Map.entry(SpriteImage.MODIFIER_PLAYER_COLOR, "PlayerColor"),
            Map.entry(SpriteImage.MODIFIER_HALLUCINATION, "Hallucination"),
            Map.entry(SpriteImage.MODIFIER_WARP_FLASH, "WarpFlash")
    );

    /**
     * Defines the type namespaces.
     *
     * @param context    the current context
     * @param scriptGame the game binding
     * @param core       the {@code core} object
     * @param global     the global scope
     */
    static void define(@NotNull Context context, @NotNull ScriptGame scriptGame, @NotNull Scriptable core, @NotNull Scriptable global) {
        Scriptable unitTypes = defineNames(ScriptCoreUtils.createNamespace(context, global, core, "unitType"), global, "unit type",
                                           getNames(ScrUnits.getTypes()));
        Scriptable weaponTypes = defineNames(ScriptCoreUtils.createNamespace(context, global, core, "weaponType"), global, "weapon type",
                                             getNames(ScrWeapons.getTypes()));

        defineNames(ScriptCoreUtils.createNamespace(context, global, core, "upgradeType"), global, "upgrade type", getNames(ScrUpgrades.getTypes()));
        defineNames(ScriptCoreUtils.createNamespace(context, global, core, "techType"), global, "tech type", getNames(ScrTechs.getTypes()));
        defineNames(ScriptCoreUtils.createNamespace(context, global, core, "orderType"), global, "order type", getNames(ScrOrders.getTypes()));
        defineNames(ScriptCoreUtils.createNamespace(context, global, core, "imageType"), global, "image type",
                    createImageNames(scriptGame.getGame().getGameData()));

        defineField(scriptGame, unitTypes, global, "MaxHp", "unit type", ScrUnits.COUNT, GameData::getUnits, UnitsDat.HIT_POINTS);
        defineField(scriptGame, unitTypes, global, "MaxShields", "unit type", ScrUnits.COUNT, GameData::getUnits, UnitsDat.SHIELD_AMOUNT);
        defineField(scriptGame, unitTypes, global, "Armor", "unit type", ScrUnits.COUNT, GameData::getUnits, UnitsDat.ARMOR);
        defineWeapon(scriptGame, unitTypes, global, "getGroundWeapon", UnitsDat.GROUND_WEAPON);
        defineWeapon(scriptGame, unitTypes, global, "getAirWeapon", UnitsDat.AIR_WEAPON);

        defineField(scriptGame, weaponTypes, global, "Damage", "weapon type", ScrWeapons.COUNT, GameData::getWeapons, WeaponsDat.DAMAGE_AMOUNT);
        defineField(scriptGame, weaponTypes, global, "DamageBonus", "weapon type", ScrWeapons.COUNT, GameData::getWeapons, WeaponsDat.DAMAGE_BONUS);
    }

    /**
     * Defines {@code getCount()} and {@code getName(id)} of a type namespace.
     *
     * @return the namespace
     */
    private static @NotNull Scriptable defineNames(@NotNull Scriptable namespace, @NotNull Scriptable global, @NotNull String kind,
                                                   @NotNull List<@NotNull String> names) {
        ScriptCoreUtils.defineFunction(global, namespace, "getCount", 0, (context, scope, thisObject, arguments) -> (double) names.size());
        ScriptCoreUtils.defineFunction(global, namespace, "getName", 1, (context, scope, thisObject, arguments) -> {
            int id = ScriptCoreUtils.requireId(kind, ScriptCoreUtils.readInteger(arguments, 0), names.size());

            return names.get(id);
        });

        return namespace;
    }

    /**
     * @return the original data names of the types, by id
     */
    private static @NotNull List<@NotNull String> getNames(@NotNull List<? extends ContentType> types) {
        return types.stream().map(ContentType::getDebugName).toList();
    }

    /**
     * Creates the unique names of the {@code images.dat} entries from their GRP paths and drawing functions.
     *
     * @param gameData the game data, or {@code null} for a game without game data
     * @return the names, by entry
     */
    private static @NotNull List<@NotNull String> createImageNames(@Nullable GameData gameData) {
        String[] paths = new String[ScrImages.COUNT];
        Map<String, Integer> pathCounts = new HashMap<>();

        for (int id = 0; id < ScrImages.COUNT; id++) {
            String path = findGrpPath(gameData, id);

            if (path != null) {
                paths[id] = path;
                pathCounts.merge(path.toLowerCase(Locale.ROOT), 1, Integer::sum);
            }
        }

        String[] baseNames = new String[ScrImages.COUNT];
        Set<String> baseNameSet = new HashSet<>();

        for (int id = 0; id < ScrImages.COUNT; id++) {
            String path = paths[id];
            String name = path == null ? IMAGE_NAME_PREFIX + id : toImageName(path);

            if (gameData != null && path != null && pathCounts.get(path.toLowerCase(Locale.ROOT)) > 1) {
                name += DRAW_FUNCTION_SUFFIXES.getOrDefault(gameData.getImageTypes().getDrawFunction(id), "");
            }

            baseNames[id] = name;
            baseNameSet.add(name);
        }

        String[] names = new String[ScrImages.COUNT];
        Set<String> takenNames = new HashSet<>();

        for (int id = 0; id < ScrImages.COUNT; id++) {
            String name = baseNames[id];

            if (takenNames.contains(name)) {
                int number = FIRST_NAME_NUMBER;

                while (takenNames.contains(name + number) || baseNameSet.contains(name + number)) {
                    number++;
                }

                name += number;
            }

            names[id] = name;
            takenNames.add(name);
        }

        return List.of(names);
    }

    /**
     * @return the GRP path of an {@code images.dat} entry, or {@code null} if there is no game data or the entry has no valid GRP path
     */
    private static @Nullable String findGrpPath(@Nullable GameData gameData, int id) {
        if (gameData == null) {
            return null;
        }

        try {
            return gameData.getGrpPath(id);
        } catch (IOException exception) {
            return null;
        }
    }

    /**
     * Converts a GRP path to a name: the path without its extension, each part with its first letter in upper case, after {@code Image} if
     * it would not start as an identifier.
     *
     * @param path the GRP path, ending with {@code .grp} in any case
     * @return the name
     */
    private static @NotNull String toImageName(@NotNull String path) {
        StringBuilder name = new StringBuilder();

        for (String part : path.substring(0, path.length() - GRP_EXTENSION.length()).split(GRP_PATH_SEPARATORS)) {
            if (!part.isEmpty()) {
                name.append(Character.toUpperCase(part.charAt(0))).append(part, 1, part.length());
            }
        }

        if (name.isEmpty() || !Character.isLetter(name.charAt(0))) {
            name.insert(0, IMAGE_NAME_PREFIX);
        }

        return name.toString();
    }

    /**
     * Defines {@code get<property>(id)} and {@code set<property>(id, value)} of a data table field.
     */
    private static void defineField(@NotNull ScriptGame scriptGame, @NotNull Scriptable namespace, @NotNull Scriptable global,
                                    @NotNull String property, @NotNull String kind, int count,
                                    @NotNull Function<GameData, DatTable> table, @NotNull String field) {
        ScriptCoreUtils.defineFunction(global, namespace, "get" + property, 1, (context, scope, thisObject, arguments) -> {
            int id = ScriptCoreUtils.requireId(kind, ScriptCoreUtils.readInteger(arguments, 0), count);

            return (double) table.apply(scriptGame.getGame().requireGameData()).get(field, id);
        });
        ScriptCoreUtils.defineFunction(global, namespace, "set" + property, 2, (context, scope, thisObject, arguments) -> {
            scriptGame.checkWritable();

            int id = ScriptCoreUtils.requireId(kind, ScriptCoreUtils.readInteger(arguments, 0), count);
            int value = ScriptCoreUtils.readInteger(arguments, 1);

            table.apply(scriptGame.getGame().requireGameData()).set(field, id, value);

            return Context.getUndefinedValue();
        });
    }

    /**
     * Defines a read of a unit type's weapon: the weapon id, or {@code -1} for none.
     */
    private static void defineWeapon(@NotNull ScriptGame scriptGame, @NotNull Scriptable namespace, @NotNull Scriptable global,
                                     @NotNull String name, @NotNull String field) {
        ScriptCoreUtils.defineFunction(global, namespace, name, 1, (context, scope, thisObject, arguments) -> {
            int id = ScriptCoreUtils.requireId("unit type", ScriptCoreUtils.readInteger(arguments, 0), ScrUnits.COUNT);
            int weapon = scriptGame.getGame().requireGameData().getUnits().get(field, id);

            return (double) (weapon < ScrWeapons.COUNT ? weapon : ScriptCoreUtils.NO_ID);
        });
    }


    private ScriptTypeCore() {
    }
}
