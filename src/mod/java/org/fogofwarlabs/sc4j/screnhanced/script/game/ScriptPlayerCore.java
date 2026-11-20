package org.fogofwarlabs.sc4j.screnhanced.script.game;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.scr.content.ScrRaceIds;
import org.fogofwarlabs.sc4j.scr.data.format.replay.ReplayHeaderBuilder;
import org.fogofwarlabs.sc4j.scr.game.player.Player;
import org.fogofwarlabs.sc4j.scr.game.player.PlayerTable;
import org.fogofwarlabs.sc4j.scr.game.player.PlayerTypes;
import org.fogofwarlabs.sc4j.scr.game.player.UnitAccounting;
import org.fogofwarlabs.sc4j.scr.game.trigger.TriggerModifiers;
import org.fogofwarlabs.sc4j.scr.game.trigger.TriggerPlayerResults;
import org.fogofwarlabs.sc4j.scr.game.trigger.TriggerScoreActionsUtils;
import org.fogofwarlabs.sc4j.scr.game.trigger.eud.region.LocalView;
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
 * {@code getMinerals(player)}, {@code setMinerals(player, amount)}, {@code addMinerals(player, amount)},
 * {@code subtractMinerals(player, amount)}, the same four for gas, {@code getSupplyUsed(player, race)}, {@code getSupplyProvided(player, race)}
 * and {@code getSupplyMax(player, race)} (half supply units of a race from {@code 0} Zerg to {@code 2} Protoss), {@code getAlliance(player,
 * other)} ({@code 0} enemy, {@code 1} allied, {@code 2} allied victory), {@code print(player, text)} and {@code endGame(player, result)}. The
 * resource writes run the Set Resources routine: setting sets the gathered amount too, adding adds to it, subtracting leaves {@code 0}
 * when the amount is not below the current one as unsigned numbers, and players from {@value PlayerTable#PLAYABLE_PLAYER_SLOTS} keep their
 * resources. A printed text shows when the player is the local player. {@code centerView(player, x, y)} centers the local screen on a map
 * position when the player is the local player, at once, as the multiplayer Center View action moves it ({@link LocalView#centerScreen}); it
 * returns nothing, so the simulation does not depend on the local screen, and does nothing while the local view or its display geometry is
 * unknown.
 * {@code endGame} ends the game for a player from {@code 0} to {@value PlayerTable#PLAYABLE_PLAYER_SLOTS} minus one as the Victory
 * ({@value TriggerPlayerResults#VICTORY}), Defeat ({@value TriggerPlayerResults#DEFEAT}) and Draw ({@value TriggerPlayerResults#DRAW})
 * actions do and returns {@code false} when a defeat or a draw meets a victory.
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
        this.defineResourceWrite(namespace, global, "setMinerals", TriggerModifiers.SET_TO, TriggerScoreActionsUtils.RESOURCE_MINERALS);
        this.defineResourceWrite(namespace, global, "addMinerals", TriggerModifiers.ADD, TriggerScoreActionsUtils.RESOURCE_MINERALS);
        this.defineResourceWrite(namespace, global, "subtractMinerals", TriggerModifiers.SUBTRACT, TriggerScoreActionsUtils.RESOURCE_MINERALS);
        ScriptCoreUtils.defineFunction(global, namespace, "getGas", 1,
                                       (context, scope, thisObject, arguments) -> (double) this.readPlayer(arguments, 0).getGas());
        this.defineResourceWrite(namespace, global, "setGas", TriggerModifiers.SET_TO, TriggerScoreActionsUtils.RESOURCE_GAS);
        this.defineResourceWrite(namespace, global, "addGas", TriggerModifiers.ADD, TriggerScoreActionsUtils.RESOURCE_GAS);
        this.defineResourceWrite(namespace, global, "subtractGas", TriggerModifiers.SUBTRACT, TriggerScoreActionsUtils.RESOURCE_GAS);

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
        ScriptCoreUtils.defineFunction(global, namespace, "centerView", 3, (context, scope, thisObject, arguments) -> {
            int player = this.readPlayerId(arguments, 0);
            int x = ScriptCoreUtils.readInteger(arguments, 1);
            int y = ScriptCoreUtils.readInteger(arguments, 2);

            if (player == this.scriptGame.requireTriggerContext().getLocalPlayer()) {
                this.scriptGame.getGame().getInterfaceLink().getLocalView().centerScreen(x, y);
            }

            return Context.getUndefinedValue();
        });
        ScriptCoreUtils.defineFunction(global, namespace, "endGame", 2, (context, scope, thisObject, arguments) -> {
            this.scriptGame.checkWritable();

            int player = ScriptCoreUtils.requireId("player", ScriptCoreUtils.readInteger(arguments, 0), PlayerTable.PLAYABLE_PLAYER_SLOTS);
            int result = ScriptCoreUtils.readInteger(arguments, 1);

            if (result != TriggerPlayerResults.VICTORY && result != TriggerPlayerResults.DEFEAT && result != TriggerPlayerResults.DRAW) {
                throw ScriptRuntime.rangeError(Exceptions.format("The result {0} is not a victory, a defeat or a draw.", result));
            }

            return this.scriptGame.requireTriggerContext().endGame(player, result);
        });
    }

    /**
     * Defines a write of a player's minerals or gas that runs the Set Resources routine with a modifier; players from
     * {@value PlayerTable#PLAYABLE_PLAYER_SLOTS} keep their resources, as Set Resources leaves them.
     */
    private void defineResourceWrite(@NotNull Scriptable namespace, @NotNull Scriptable global, @NotNull String name, int modifier, int resourceType) {
        ScriptCoreUtils.defineFunction(global, namespace, name, 2, (context, scope, thisObject, arguments) -> {
            this.scriptGame.checkWritable();

            int id = this.readPlayerId(arguments, 0);
            int amount = ScriptCoreUtils.readInteger(arguments, 1);
            Player player = this.requirePlayer(id);

            if (id < PlayerTable.PLAYABLE_PLAYER_SLOTS) {
                TriggerScoreActionsUtils.setResources(player, modifier, resourceType, amount);
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
}
