package org.fogofwarlabs.sc4j.screnhanced.script.game;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.scr.content.ScrRaceIds;
import org.fogofwarlabs.sc4j.scr.data.format.replay.ReplayHeaderBuilder;
import org.fogofwarlabs.sc4j.scr.game.player.Player;
import org.fogofwarlabs.sc4j.scr.game.player.PlayerTable;
import org.fogofwarlabs.sc4j.scr.game.player.PlayerTypes;
import org.fogofwarlabs.sc4j.scr.game.player.UnitAccounting;
import org.jetbrains.annotations.NotNull;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

import java.nio.charset.StandardCharsets;

/**
 * Defines {@code core.player}: the players by slot id from {@code 0} to {@value PlayerTable#PLAYER_SLOTS} minus one.
 * <p>
 * {@code getCount()}, {@code getName(player)}, {@code getRace(player)}, {@code getType(player)}, {@code isHuman(player)},
 * {@code getMinerals(player)}, {@code setMinerals(player, amount)}, {@code addMinerals(player, amount)}, the same three for gas,
 * {@code getSupplyUsed(player, race)}, {@code getSupplyProvided(player, race)} and {@code getSupplyMax(player, race)} (half supply units of a
 * race from {@code 0} Zerg to {@code 2} Protoss), {@code getAlliance(player, other)} ({@code 0} enemy, {@code 1} allied, {@code 2} allied
 * victory) and {@code print(player, text)}. The resource writes run the Set Resources routine: setting sets the gathered amount too, adding
 * adds to it, and players from {@value PlayerTable#PLAYABLE_PLAYER_SLOTS} keep their resources. A printed text shows when the player is the
 * local player.
 */
final class ScriptPlayerCore {
    private final @NotNull ScriptGame scriptGame;

    /**
     * Creates a {@link ScriptPlayerCore} instance.
     *
     * @param scriptGame the game binding
     */
    ScriptPlayerCore(@NotNull ScriptGame scriptGame) {
        this.scriptGame = scriptGame;
    }

    /**
     * Defines the functions of {@code core.player}.
     *
     * @param namespace the namespace object
     * @param global    the global scope
     */
    void define(@NotNull Scriptable namespace, @NotNull Scriptable global) {
        ScriptCoreUtils.defineFunction(global, namespace, "getCount", 0, (context, scope, thisObject, arguments) -> (double) PlayerTable.PLAYER_SLOTS);
        ScriptCoreUtils.defineFunction(global, namespace, "getName", 1,
                                       (context, scope, thisObject, arguments) -> this.getName(this.readPlayerId(arguments, 0)));
        ScriptCoreUtils.defineFunction(global, namespace, "getRace", 1,
                                       (context, scope, thisObject, arguments) -> (double) this.readPlayer(arguments, 0).getRace());
        ScriptCoreUtils.defineFunction(global, namespace, "getType", 1,
                                       (context, scope, thisObject, arguments) -> (double) this.readPlayer(arguments, 0).getType());
        ScriptCoreUtils.defineFunction(global, namespace, "isHuman", 1,
                                       (context, scope, thisObject, arguments) -> this.readPlayer(arguments, 0).getType() == PlayerTypes.HUMAN);

        ScriptCoreUtils.defineFunction(global, namespace, "getMinerals", 1,
                                       (context, scope, thisObject, arguments) -> (double) this.readPlayer(arguments, 0).getMinerals());
        ScriptCoreUtils.defineFunction(global, namespace, "setMinerals", 2, (context, scope, thisObject, arguments) -> {
            this.changeResources(arguments, true, false);

            return Context.getUndefinedValue();
        });
        ScriptCoreUtils.defineFunction(global, namespace, "addMinerals", 2, (context, scope, thisObject, arguments) -> {
            this.changeResources(arguments, true, true);

            return Context.getUndefinedValue();
        });
        ScriptCoreUtils.defineFunction(global, namespace, "getGas", 1,
                                       (context, scope, thisObject, arguments) -> (double) this.readPlayer(arguments, 0).getGas());
        ScriptCoreUtils.defineFunction(global, namespace, "setGas", 2, (context, scope, thisObject, arguments) -> {
            this.changeResources(arguments, false, false);

            return Context.getUndefinedValue();
        });
        ScriptCoreUtils.defineFunction(global, namespace, "addGas", 2, (context, scope, thisObject, arguments) -> {
            this.changeResources(arguments, false, true);

            return Context.getUndefinedValue();
        });

        ScriptCoreUtils.defineFunction(global, namespace, "getSupplyUsed", 2, (context, scope, thisObject, arguments) -> {
            int counter = this.readSupplyCounter(arguments);

            return (double) this.requireAccounting().getSupplyUsed(counter);
        });
        ScriptCoreUtils.defineFunction(global, namespace, "getSupplyProvided", 2, (context, scope, thisObject, arguments) -> {
            int counter = this.readSupplyCounter(arguments);

            return (double) this.requireAccounting().getSupplyProvided(counter);
        });
        ScriptCoreUtils.defineFunction(global, namespace, "getSupplyMax", 2, (context, scope, thisObject, arguments) -> {
            int counter = this.readSupplyCounter(arguments);

            return (double) this.requireAccounting().getSupplyCap(counter);
        });

        ScriptCoreUtils.defineFunction(global, namespace, "getAlliance", 2, (context, scope, thisObject, arguments) -> {
            int player = this.readPlayerId(arguments, 0);
            int other = this.readPlayerId(arguments, 1);

            return (double) this.scriptGame.getGame().getPlayerTable().requirePlayerRelations().getAlliance(player, other);
        });
        ScriptCoreUtils.defineFunction(global, namespace, "print", 2, (context, scope, thisObject, arguments) -> {
            int player = this.readPlayerId(arguments, 0);
            String text = ScriptCoreUtils.readString(arguments, 1);

            if (player == this.scriptGame.requireTriggerContext().getLocalPlayer()) {
                this.scriptGame.printText(text);
            }

            return Context.getUndefinedValue();
        });
    }

    private int readPlayerId(@NotNull Object @NotNull [] arguments, int index) {
        return ScriptCoreUtils.requireId("player", ScriptCoreUtils.readInteger(arguments, index), PlayerTable.PLAYER_SLOTS);
    }

    private @NotNull Player readPlayer(@NotNull Object @NotNull [] arguments, int index) {
        return this.requirePlayer(this.readPlayerId(arguments, index));
    }

    /**
     * Returns the player of a slot.
     *
     * @throws EcmaError if the slot has no player
     */
    private @NotNull Player requirePlayer(int id) {
        Player player = this.scriptGame.getGame().getPlayerTable().getPlayer(id);

        if (player == null) {
            throw ScriptRuntime.rangeError(Exceptions.format("The player slot {0} has no player.", id));
        }

        return player;
    }

    /**
     * Reads the player and race arguments of a supply function as the index of the supply counters: the race base plus the player.
     */
    private int readSupplyCounter(@NotNull Object @NotNull [] arguments) {
        int player = this.readPlayerId(arguments, 0);
        int race = ScriptCoreUtils.requireId("race", ScriptCoreUtils.readInteger(arguments, 1), ScrRaceIds.PLAYABLE_COUNT);

        return race * PlayerTable.PLAYER_SLOTS + player;
    }

    private @NotNull UnitAccounting requireAccounting() {
        return this.scriptGame.getGame().getPlayerTable().requireUnitAccounting();
    }

    /**
     * Reads the name of a player from the game header: the null-terminated UTF-8 name of the player's record.
     *
     * @param id the player id
     * @return the name, empty if the game has no header
     */
    private @NotNull String getName(int id) {
        byte[] header = this.scriptGame.getGame().getGameContext().getGameHeaderSection();
        int start = ReplayHeaderBuilder.PLAYERS_OFFSET + id * ReplayHeaderBuilder.PLAYER_RECORD_SIZE + ReplayHeaderBuilder.PLAYER_NAME_OFFSET;
        int limit = ReplayHeaderBuilder.PLAYERS_OFFSET + (id + 1) * ReplayHeaderBuilder.PLAYER_RECORD_SIZE;

        if (header == null || limit > header.length) {
            return "";
        }

        int end = start;

        while (end < limit && header[end] != 0) {
            end++;
        }

        return new String(header, start, end - start, StandardCharsets.UTF_8);
    }

    /**
     * Sets or adds minerals or gas of a player as Set Resources does with the Set To or the Add modifier.
     *
     * @param arguments the player and the amount
     * @param minerals  {@code true} for minerals, {@code false} for gas
     * @param add       {@code true} to add the amount, {@code false} to set it
     */
    private void changeResources(@NotNull Object @NotNull [] arguments, boolean minerals, boolean add) {
        this.scriptGame.checkWritable();

        int id = this.readPlayerId(arguments, 0);
        int amount = ScriptCoreUtils.readInteger(arguments, 1);
        Player player = this.requirePlayer(id);

        if (id >= PlayerTable.PLAYABLE_PLAYER_SLOTS) {
            return;
        }

        if (minerals) {
            player.setGatheredMinerals(add ? player.getGatheredMinerals() + amount : amount);
            player.setMinerals(add ? player.getMinerals() + amount : amount);
        } else {
            player.setGatheredGas(add ? player.getGatheredGas() + amount : amount);
            player.setGas(add ? player.getGas() + amount : amount);
        }
    }
}
