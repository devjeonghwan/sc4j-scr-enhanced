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
import org.fogofwarlabs.sc4j.scr.game.state.GameData;
import org.jetbrains.annotations.NotNull;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.util.List;
import java.util.function.Function;

/**
 * Defines the type namespaces of {@code core}: {@code unitType}, {@code weaponType}, {@code upgradeType}, {@code techType},
 * {@code orderType} and {@code imageType}, the entries of {@code units.dat}, {@code weapons.dat}, {@code upgrades.dat}, {@code techdata.dat},
 * {@code orders.dat} and {@code images.dat} by their index.
 * <p>
 * Each has {@code getCount()} and {@code getName(id)}, the original data name of the entry ({@code "Terran Marine"}); an {@code images.dat}
 * entry without a name in the SC:R mod's image types is named {@code "image <entry>"}. {@code unitType} also
 * has {@code getMaxHp} and {@code setMaxHp} (raw {@code 1/256} units), {@code getMaxShields}, {@code setMaxShields}, {@code getArmor},
 * {@code setArmor}, {@code getGroundWeapon} and {@code getAirWeapon} ({@code -1} for none); {@code weaponType} has {@code getDamage},
 * {@code setDamage}, {@code getDamageBonus} and {@code setDamageBonus}. A write changes the data table of the current game, which the map
 * settings and EUD writes change too, with the field's width.
 */
final class ScriptTypeCore {
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
                                           ScrUnits.getTypes());
        Scriptable weaponTypes = defineNames(ScriptCoreUtils.createNamespace(context, global, core, "weaponType"), global, "weapon type",
                                             ScrWeapons.getTypes());

        defineNames(ScriptCoreUtils.createNamespace(context, global, core, "upgradeType"), global, "upgrade type", ScrUpgrades.getTypes());
        defineNames(ScriptCoreUtils.createNamespace(context, global, core, "techType"), global, "tech type", ScrTechs.getTypes());
        defineNames(ScriptCoreUtils.createNamespace(context, global, core, "orderType"), global, "order type", ScrOrders.getTypes());
        defineNames(ScriptCoreUtils.createNamespace(context, global, core, "imageType"), global, "image type", ScrImages.getTypes());

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
                                                   @NotNull List<? extends ContentType> types) {
        ScriptCoreUtils.defineFunction(global, namespace, "getCount", 0, (context, scope, thisObject, arguments) -> (double) types.size());
        ScriptCoreUtils.defineFunction(global, namespace, "getName", 1, (context, scope, thisObject, arguments) -> {
            int id = ScriptCoreUtils.requireId(kind, ScriptCoreUtils.readInteger(arguments, 0), types.size());

            return types.get(id).getDebugName();
        });

        return namespace;
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
