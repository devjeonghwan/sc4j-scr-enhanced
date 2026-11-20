package org.fogofwarlabs.sc4j.screnhanced.script.game;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.scr.content.ScrOrderIds;
import org.fogofwarlabs.sc4j.scr.content.ScrUnitIds;
import org.fogofwarlabs.sc4j.scr.data.type.ScrUnitTypeView;
import org.fogofwarlabs.sc4j.scr.data.type.constant.UnitTypeFlags;
import org.fogofwarlabs.sc4j.scr.game.ability.UnitHallucination;
import org.fogofwarlabs.sc4j.scr.game.player.PlayerTable;
import org.fogofwarlabs.sc4j.scr.game.state.Game;
import org.fogofwarlabs.sc4j.scr.game.unit.Unit;
import org.fogofwarlabs.sc4j.scr.game.unit.UnitPool;
import org.fogofwarlabs.sc4j.scr.game.unit.UnitQueries;
import org.fogofwarlabs.sc4j.scr.game.unit.UnitStatusFlags;
import org.fogofwarlabs.sc4j.scr.game.unit.UnitTypeTraitsUtils;
import org.jetbrains.annotations.NotNull;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Defines {@code core.unit}: the units by the index of their unit pool slot, from {@code 1} to {@code getCapacity()}.
 * <p>
 * A slot holds a live unit while it is allocated, has a sprite and does not run the Die order; {@code getGeneration(id)} tells apart the
 * units that one slot holds in turn. {@code getIds()} lists the live units in slot order. Every other function needs a live unit and throws a
 * {@code RangeError} for another id.
 * <p>
 * Reads: {@code getType}, {@code getOwner}, {@code getX}, {@code getY}, {@code getHp}, {@code getMaxHp}, {@code getShields},
 * {@code getMaxShields}, {@code getEnergy}, {@code getMaxEnergy} (raw {@code 1/256} units), {@code getOrder}, {@code getTarget} ({@code -1}
 * without a target unit), {@code getTargetX}, {@code getTargetY}, {@code getStatus} (the status flags), {@code isCompleted},
 * {@code isCloaked}, {@code isBurrowed} and {@code isInvincible}.
 * <p>
 * Writes, through the routines of the trigger actions and the order system: {@code setHp} (Modify Unit Hit Points), {@code setShields}
 * (Modify Unit Shield Points), {@code setEnergy} (Modify Unit Energy), {@code setInvincible} (Set Invincibility), {@code create(type, owner,
 * x, y)} (Create Unit; the new id or {@code -1}), {@code kill} and {@code remove} (Kill Unit and Remove Unit), {@code move(id, x, y)} (Move
 * Unit), {@code give(id, player)} (Give Units to Player) and {@code issueOrder(id, order, target, x, y)} (the order system after the order
 * requirement test, {@code target} {@code -1} for a position). A write that the routine refuses returns {@code false}.
 */
final class ScriptUnitCore {
    /**
     * The fixed-point shift of hit points, shields and energy.
     */
    private static final int FIXED_POINT_SHIFT = 8;

    private final @NotNull ScriptGame scriptGame;

    /**
     * Creates a {@link ScriptUnitCore} instance.
     *
     * @param scriptGame the game binding
     */
    ScriptUnitCore(@NotNull ScriptGame scriptGame) {
        this.scriptGame = scriptGame;
    }

    /**
     * Checks whether a slot holds a live unit.
     *
     * @param pool the unit pool
     * @param id   the slot index
     * @return {@code true} if the slot is allocated, its unit has a sprite and does not run the Die order, {@code false} otherwise
     */
    private static boolean isLive(@NotNull UnitPool pool, int id) {
        if (id < 1 || id > pool.getCapacity() || !pool.isAllocated(id)) {
            return false;
        }

        Unit unit = pool.getUnit(id);

        return unit.getSpriteOrNull() != null && unit.getPrimaryOrder() != ScrOrderIds.DIE;
    }

    /**
     * Defines the functions of {@code core.unit}.
     *
     * @param namespace the namespace object
     * @param global    the global scope
     */
    void define(@NotNull Scriptable namespace, @NotNull Scriptable global) {
        ScriptCoreUtils.defineFunction(global, namespace, "getCapacity", 0,
                                       (context, scope, thisObject, arguments) -> (double) this.getPool().getCapacity());
        ScriptCoreUtils.defineFunction(global, namespace, "getIds", 0, (context, scope, thisObject, arguments) -> {
            UnitPool pool = this.getPool();
            List<Object> ids = new ArrayList<>();

            for (int id = 1; id <= pool.getCapacity(); id++) {
                if (isLive(pool, id)) {
                    ids.add((double) id);
                }
            }

            return context.newArray(global, ids.toArray());
        });
        ScriptCoreUtils.defineFunction(global, namespace, "exists", 1, (context, scope, thisObject, arguments) -> {
            int id = ScriptCoreUtils.readInteger(arguments, 0);

            return isLive(this.getPool(), id);
        });
        ScriptCoreUtils.defineFunction(global, namespace, "getGeneration", 1, (context, scope, thisObject, arguments) -> {
            UnitPool pool = this.getPool();
            int id = ScriptCoreUtils.readInteger(arguments, 0);

            if (id < 1 || id > pool.getCapacity()) {
                throw ScriptRuntime.rangeError(Exceptions.format("The unit slot {0} is not from 1 to {1}.", id, pool.getCapacity()));
            }

            return (double) pool.getGeneration(id);
        });

        this.defineReads(namespace, global);
        this.defineWrites(namespace, global);
    }

    private void defineReads(@NotNull Scriptable namespace, @NotNull Scriptable global) {
        this.defineRead(namespace, global, "getType", Unit::getUnitId);
        this.defineRead(namespace, global, "getOwner", Unit::getOwnerIndex);
        this.defineRead(namespace, global, "getX", Unit::getPositionX);
        this.defineRead(namespace, global, "getY", Unit::getPositionY);
        this.defineRead(namespace, global, "getHp", Unit::getHitPoints);
        this.defineRead(namespace, global, "getMaxHp", unit -> this.getUnitTypes().getHitPoints(unit.getUnitId()));
        this.defineRead(namespace, global, "getShields", Unit::getShieldPoints);
        this.defineRead(namespace, global, "getMaxShields", this::getMaximumShields);
        this.defineRead(namespace, global, "getEnergy", Unit::getEnergy);
        this.defineRead(namespace, global, "getMaxEnergy", this::getMaximumEnergy);
        this.defineRead(namespace, global, "getOrder", Unit::getPrimaryOrder);
        this.defineRead(namespace, global, "getTarget", unit -> {
            Unit target = unit.getOrderTargetUnit();

            return target != null && isLive(this.getPool(), target.getIndex()) ? target.getIndex() : ScriptCoreUtils.NO_ID;
        });
        this.defineRead(namespace, global, "getTargetX", Unit::getOrderTargetX);
        this.defineRead(namespace, global, "getTargetY", Unit::getOrderTargetY);
        this.defineRead(namespace, global, "getStatus", Unit::getStatus);

        this.defineStatusRead(namespace, global, "isCompleted", UnitStatusFlags.COMPLETED);
        this.defineStatusRead(namespace, global, "isCloaked", UnitStatusFlags.CLOAKED | UnitStatusFlags.PASSIVELY_CLOAKED);
        this.defineStatusRead(namespace, global, "isBurrowed", UnitStatusFlags.BURROWED);
        this.defineStatusRead(namespace, global, "isInvincible", UnitStatusFlags.INVINCIBLE);
    }

    private void defineWrites(@NotNull Scriptable namespace, @NotNull Scriptable global) {
        ScriptCoreUtils.defineFunction(global, namespace, "setHp", 2, (context, scope, thisObject, arguments) -> {
            this.scriptGame.checkWritable();

            Unit unit = this.readUnit(arguments, 0);

            this.setHitPoints(unit, this.readAmount(arguments, 1));

            return Context.getUndefinedValue();
        });
        ScriptCoreUtils.defineFunction(global, namespace, "setShields", 2, (context, scope, thisObject, arguments) -> {
            this.scriptGame.checkWritable();

            Unit unit = this.readUnit(arguments, 0);
            int shields = this.readAmount(arguments, 1);

            if (this.getUnitTypes().getShieldEnabled(unit.getUnitId()) == 0) {
                return false;
            }

            unit.setShieldPoints(Math.min(shields, this.getMaximumShields(unit)));

            return true;
        });
        ScriptCoreUtils.defineFunction(global, namespace, "setEnergy", 2, (context, scope, thisObject, arguments) -> {
            this.scriptGame.checkWritable();

            Unit unit = this.readUnit(arguments, 0);
            int energy = this.readAmount(arguments, 1);

            if (!UnitTypeTraitsUtils.isSpellcaster(this.getUnitTypes(), unit)) {
                return false;
            }

            unit.setEnergy(Math.min(energy, this.getMaximumEnergy(unit)));

            return true;
        });
        ScriptCoreUtils.defineFunction(global, namespace, "setInvincible", 2, (context, scope, thisObject, arguments) -> {
            this.scriptGame.checkWritable();

            Unit unit = this.readUnit(arguments, 0);
            boolean invincible = ScriptCoreUtils.readBoolean(arguments, 1);

            unit.setStatus(invincible ? unit.getStatus() | UnitStatusFlags.INVINCIBLE : unit.getStatus() & ~UnitStatusFlags.INVINCIBLE);

            return Context.getUndefinedValue();
        });

        ScriptCoreUtils.defineFunction(global, namespace, "create", 4, (context, scope, thisObject, arguments) -> {
            this.scriptGame.checkWritable();

            return (double) this.createUnit(arguments);
        });
        ScriptCoreUtils.defineFunction(global, namespace, "kill", 1, (context, scope, thisObject, arguments) -> {
            this.scriptGame.checkWritable();

            return this.removeUnit(this.readUnit(arguments, 0), false);
        });
        ScriptCoreUtils.defineFunction(global, namespace, "remove", 1, (context, scope, thisObject, arguments) -> {
            this.scriptGame.checkWritable();

            return this.removeUnit(this.readUnit(arguments, 0), true);
        });
        ScriptCoreUtils.defineFunction(global, namespace, "move", 3, (context, scope, thisObject, arguments) -> {
            this.scriptGame.checkWritable();

            Unit unit = this.readUnit(arguments, 0);

            return this.moveUnit(unit, ScriptCoreUtils.readInteger(arguments, 1), ScriptCoreUtils.readInteger(arguments, 2));
        });
        ScriptCoreUtils.defineFunction(global, namespace, "give", 2, (context, scope, thisObject, arguments) -> {
            this.scriptGame.checkWritable();

            Unit unit = this.readUnit(arguments, 0);
            int owner = ScriptCoreUtils.requireId("player", ScriptCoreUtils.readInteger(arguments, 1), PlayerTable.PLAYER_SLOTS);

            return this.getGame().getUnitOwnership().giveUnit(unit, owner, this.scriptGame.getInstructionLimit());
        });
        ScriptCoreUtils.defineFunction(global, namespace, "issueOrder", 5, (context, scope, thisObject, arguments) -> {
            this.scriptGame.checkWritable();

            return this.issueOrder(arguments);
        });
    }

    /**
     * Defines a read of a live unit that returns an integer.
     */
    private void defineRead(@NotNull Scriptable namespace, @NotNull Scriptable global, @NotNull String name,
                            @NotNull ToIntFunction<Unit> read) {
        ScriptCoreUtils.defineFunction(global, namespace, name, 1,
                                       (context, scope, thisObject, arguments) -> (double) read.applyAsInt(this.readUnit(arguments, 0)));
    }

    /**
     * Defines a read of a live unit that tests status flags.
     */
    private void defineStatusRead(@NotNull Scriptable namespace, @NotNull Scriptable global, @NotNull String name, int flags) {
        ScriptCoreUtils.defineFunction(global, namespace, name, 1,
                                       (context, scope, thisObject, arguments) -> (this.readUnit(arguments, 0).getStatus() & flags) != 0);
    }

    private @NotNull Game getGame() {
        return this.scriptGame.getGame();
    }

    private @NotNull UnitPool getPool() {
        return this.getGame().getUnitSystem().requireUnitPool();
    }

    private @NotNull ScrUnitTypeView getUnitTypes() {
        return this.getGame().requireGameData().getUnitTypes();
    }

    /**
     * Reads a unit argument.
     *
     * @throws EcmaError if the argument is not an integer or names no live unit
     */
    private @NotNull Unit readUnit(@NotNull Object @NotNull [] arguments, int index) {
        int id = ScriptCoreUtils.readInteger(arguments, index);
        UnitPool pool = this.getPool();

        if (!isLive(pool, id)) {
            throw ScriptRuntime.rangeError(Exceptions.format("The unit {0} does not exist.", id));
        }

        return pool.getUnit(id);
    }

    /**
     * Reads a raw amount of hit points, shields or energy.
     *
     * @throws EcmaError if the argument is not an integer or is negative
     */
    private int readAmount(@NotNull Object @NotNull [] arguments, int index) {
        int amount = ScriptCoreUtils.readInteger(arguments, index);

        if (amount < 0) {
            throw ScriptRuntime.rangeError(Exceptions.format("The amount {0} is negative.", amount));
        }

        return amount;
    }

    private int getMaximumShields(@NotNull Unit unit) {
        ScrUnitTypeView units = this.getUnitTypes();

        return units.getShieldEnabled(unit.getUnitId()) != 0 ? units.getShieldAmount(unit.getUnitId()) << FIXED_POINT_SHIFT : 0;
    }

    private int getMaximumEnergy(@NotNull Unit unit) {
        ScrUnitTypeView units = this.getUnitTypes();

        return UnitTypeTraitsUtils.isSpellcaster(units, unit) ? this.getGame().getUnitInitializer().getMaximumEnergy(unit, units) : 0;
    }

    /**
     * Sets hit points as Modify Unit Hit Points does: at least the smallest raw amount, at most the maximum, and a hallucination or Broodling
     * receives its removal timer again.
     */
    private void setHitPoints(@NotNull Unit unit, int hitPoints) {
        this.getGame().getUnitHealth().setHitPoints(unit, hitPoints == 0 ? 1 : hitPoints, this.scriptGame.getInstructionLimit());

        boolean hallucination = (unit.getStatus() & UnitStatusFlags.HALLUCINATION) != 0;

        if (hallucination || this.getUnitTypes().getBaseType(unit.getUnitId()) == ScrUnitIds.ZERG_BROODLING) {
            UnitHallucination.setRemovalTimer(unit, hallucination
                                                    ? UnitHallucination.HALLUCINATION_REMOVAL_TIMER
                                                    : UnitHallucination.BROODLING_REMOVAL_TIMER);
        }
    }

    /**
     * Creates a unit as Create Unit does for one player: a mineral field or a Vespene Geyser belongs to the neutral player.
     *
     * @return the id of the created unit, or {@code -1} if the routine creates none
     */
    private int createUnit(@NotNull Object @NotNull [] arguments) {
        int type = ScriptCoreUtils.requireId("unit type", ScriptCoreUtils.readInteger(arguments, 0), ScrUnitIds.NONE);
        int owner = ScriptCoreUtils.requireId("player", ScriptCoreUtils.readInteger(arguments, 1), PlayerTable.PLAYER_SLOTS);
        int positionX = ScriptCoreUtils.readInteger(arguments, 2);
        int positionY = ScriptCoreUtils.readInteger(arguments, 3);

        if (this.getUnitTypes().isMineralField(type) || type == ScrUnitIds.RESOURCE_VESPENE_GEYSER) {
            owner = PlayerTable.NEUTRAL_PLAYER_ID;
        }

        Unit unit = this.getGame().getTriggerUnitActions().createUnit(type, positionX, positionY, owner, this.scriptGame.getInstructionLimit());

        return unit != null ? unit.getIndex() : ScriptCoreUtils.NO_ID;
    }

    /**
     * Kills or removes a unit as Kill Unit and Remove Unit do without a location: a subunit stays; a powerup carried by a worker is released;
     * a removed unit is hidden before it is killed.
     *
     * @return {@code true} if the routine took the unit, {@code false} otherwise
     */
    private boolean removeUnit(@NotNull Unit unit, boolean remove) {
        ScrUnitTypeView units = this.getUnitTypes();
        int flags = units.getFlags(unit.getUnitId());

        if ((flags & UnitTypeFlags.SUBUNIT) != 0) {
            return false;
        }

        Unit carrier = unit.getPowerupCarrier();

        if ((flags & UnitTypeFlags.POWERUP) != 0 && carrier != null) {
            this.getGame().getUnitPowerups().release(carrier, Integer.MAX_VALUE);

            return true;
        }

        if (remove) {
            this.getGame().getUnitLifecycle().hide(unit, true);
        }

        this.getGame().getUnitLifecycle().kill(unit, Integer.MAX_VALUE);

        return true;
    }

    /**
     * Moves a unit as Move Unit does: a building landing or lifting off, a Nuclear Missile, Dark Swarm, a resource and a trap stay; a grounded
     * building moves as a building, another unit is relocated and, when completed, finishes its order.
     *
     * @return {@code true} if Move Unit takes the unit, {@code false} otherwise
     */
    private boolean moveUnit(@NotNull Unit unit, int positionX, int positionY) {
        ScrUnitTypeView units = this.getUnitTypes();
        int type = unit.getUnitId();

        if (UnitQueries.isBuilding(units, type) &&
            (unit.getPrimaryOrder() == ScrOrderIds.BUILDING_LAND || unit.getPrimaryOrder() == ScrOrderIds.BUILDING_LIFTOFF)) {
            return false;
        }

        if (units.getBaseType(type) == ScrUnitIds.TERRAN_NUCLEAR_MISSILE || type == ScrUnitIds.SPELL_DARK_SWARM ||
            units.isMineralField(type) || type == ScrUnitIds.RESOURCE_VESPENE_GEYSER ||
            (units.getFlags(type) & UnitTypeFlags.RESOURCE_CONTAINER) != 0 ||
            type >= ScrUnitIds.SPECIAL_FLOOR_MISSILE_TRAP && type <= ScrUnitIds.SPECIAL_RIGHT_WALL_FLAME_TRAP) {
            return false;
        }

        int instructionLimit = this.scriptGame.getInstructionLimit();

        if (UnitQueries.isGroundedBuilding(unit)) {
            this.getGame().getUnitBuildingMove().move(unit, positionX, positionY, instructionLimit);

            return true;
        }

        boolean moved = this.getGame().getUnitRelocation().relocate(unit, positionX, positionY);

        if (moved && (unit.getStatus() & UnitStatusFlags.COMPLETED) != 0) {
            this.getGame().getOrders().finishOrder(unit, instructionLimit);
        }

        return true;
    }

    /**
     * Issues an order through the order system after the order requirement test for the unit's owner.
     *
     * @return {@code true} if the unit received the order, {@code false} if the requirement test refused it
     */
    private boolean issueOrder(@NotNull Object @NotNull [] arguments) {
        Unit unit = this.readUnit(arguments, 0);
        int order = ScriptCoreUtils.requireId("order", ScriptCoreUtils.readInteger(arguments, 1), ScrOrderIds.NONE);
        Unit target = ScriptCoreUtils.readInteger(arguments, 2) == ScriptCoreUtils.NO_ID ? null : this.readUnit(arguments, 2);
        int positionX = ScriptCoreUtils.readInteger(arguments, 3);
        int positionY = ScriptCoreUtils.readInteger(arguments, 4);

        if (order == ScrOrderIds.CLOAK) {
            throw ScriptRuntime.rangeError(Exceptions.format("The order {0} cannot be issued.", order));
        }

        if (!this.getGame().getUnitOrderEligibility().canReceiveOrder(unit, order, unit.getOwnerIndex())) {
            return false;
        }

        int instructionLimit = this.scriptGame.getInstructionLimit();

        this.getGame().getOrders().issue(unit, order, target, positionX, positionY, ScrUnitIds.NONE, false, instructionLimit);

        return true;
    }
}
