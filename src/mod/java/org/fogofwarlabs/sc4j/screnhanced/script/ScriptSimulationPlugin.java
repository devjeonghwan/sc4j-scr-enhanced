package org.fogofwarlabs.sc4j.screnhanced.script;

import org.fogofwarlabs.sc4j.scr.game.extension.ScrSimulationPlugin;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.SimulationDecision;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.SimulationEvent;
import org.fogofwarlabs.sc4j.scr.game.player.Player;
import org.fogofwarlabs.sc4j.scr.game.player.PlayerTable;
import org.fogofwarlabs.sc4j.scr.game.presentation.InterfaceLink;
import org.fogofwarlabs.sc4j.scr.game.state.Game;
import org.fogofwarlabs.sc4j.screnhanced.graphics.ImageReplacementSource;
import org.fogofwarlabs.sc4j.screnhanced.graphics.ImageReplacements;
import org.fogofwarlabs.sc4j.screnhanced.script.input.ScriptInputCommand;
import org.fogofwarlabs.sc4j.screnhanced.script.input.ScriptInputEvent;
import org.fogofwarlabs.sc4j.screnhanced.script.input.ScriptInputState;
import org.fogofwarlabs.sc4j.screnhanced.script.runtime.ScriptRuntime;
import org.jetbrains.annotations.NotNull;

/**
 * Runs the script package of one game's scenario in the game's simulation.
 * <p>
 * The plugin creates the {@link ScriptRuntime} with the game API, the map's resources and a seed from the game's setup seed, the same on every
 * peer and in every replay, and starts it at the first frame start.
 * <p>
 * The events of the game go into the runtime's queue with their frame, the events from before the first frame (the units the map and the
 * game start create) as well, and the runtime delivers them at the next frame start, before the triggers. A frame start whose frame number
 * did not advance, as while the game is paused, runs no script. Decisions go to the runtime at once. The input commands of the players
 * ({@link ScriptInputCommand}), which the game's {@link ScriptInputPlugin} of each peer sends, change the players' input state when they run
 * and queue their events like the other events.
 * <p>
 * The classic image graphics of the game draw the image graphics the script replaces ({@link #getImageReplacements()}); every frame start first
 * forgets the replacements of units whose slot was freed. A game the plugin runs cannot be saved: a saved game would lose the script's state.
 */
public final class ScriptSimulationPlugin implements ScrSimulationPlugin, ImageReplacementSource {
    private final @NotNull Game game;
    private final @NotNull ScriptRuntime runtime;

    private boolean started;
    private int lastFrame;

    /**
     * Creates a {@link ScriptSimulationPlugin} instance whose script has not started.
     *
     * @param game          the game
     * @param scriptPackage the script package of the game's scenario
     * @param resources     the resource file of the game's map
     */
    public ScriptSimulationPlugin(@NotNull Game game, @NotNull ScriptPackage scriptPackage, @NotNull ScriptResources resources) {
        this.game    = game;
        this.runtime = ScriptRuntime.create(scriptPackage, game, resources, Integer.toUnsignedLong(game.getSetup().getSeed()));

        this.started   = false;
        this.lastFrame = 0;
    }

    /**
     * @return the script runtime of the game's script package
     */
    public @NotNull ScriptRuntime getRuntime() {
        return this.runtime;
    }

    /**
     * @return the image graphics the script replaces, the same instance for the whole game
     */
    @Override
    public @NotNull ImageReplacements getImageReplacements() {
        return this.runtime.getImageReplacements();
    }

    /**
     * @return the input the script tracks and the input state of the players, the same instance for the whole game
     */
    public @NotNull ScriptInputState getInputState() {
        return this.runtime.getInputState();
    }


    /**
     * Called once per frame when the frame's trigger step runs, before the triggers, with the random generator enabled as for the triggers; the
     * trigger step also runs while the game is paused.
     * <p>
     * Every call forgets the image replacements of units whose slot was freed; the first call starts the script; every call whose frame number
     * advanced runs the script's frame.
     */
    @Override
    public void onFrameStarted() {
        int frame = this.game.getCurrentFrame();

        this.runtime.getImageReplacements().removeFreedUnits();

        if (!this.started) {
            this.started = true;

            this.runtime.start();
        } else if (frame == this.lastFrame) {
            return;
        }

        this.lastFrame = frame;

        this.runtime.runFrame(frame);
    }

    /**
     * Called when something happens in the simulation, from inside the routine where it happens.
     * <p>
     * The event goes into the runtime's queue with the current frame.
     *
     * @param event the event; its kind is its class
     */
    @Override
    public void onEvent(@NotNull SimulationEvent event) {
        this.runtime.queueEvent(event, this.game.getCurrentFrame());
    }

    /**
     * Called at a decision point of a simulation routine, which continues with the values the plugins leave in the decision; the plugins decide
     * in extension order, each on the values the previous one left.
     * <p>
     * The script's decision hooks decide with the game read-only.
     *
     * @param decision the decision; its kind is its class
     */
    @Override
    public void decide(@NotNull SimulationDecision decision) {
        this.runtime.decide(decision);
    }

    /**
     * Called when a mod command of a player runs, at its place in the command stream of the frame. The player is the record the command's issuer
     * resolved to, the issuing player's own record also in a team game, where the other commands of every member act for the team's leader. A
     * live game queues the mod commands of the local player through the game's mod command submitter
     * ({@link InterfaceLink#getModCommandSubmitter()}).
     * <p>
     * An input command ({@link ScriptInputCommand}) changes the input state of the player's slot, and the event of a change goes into the
     * runtime's queue with the current frame; any other data is ignored.
     *
     * @param player the issuing player's record
     * @param data   the command data, which every plugin of the game receives; the plugin does not change it
     */
    @Override
    public void onModCommand(@NotNull Player player, byte @NotNull [] data) {
        ScriptInputCommand command = ScriptInputCommand.decode(data);
        int slot = this.findSlot(player);

        if (command == null || slot < 0) {
            return;
        }

        ScriptInputEvent event = this.runtime.getInputState().apply(slot, command);

        if (event != null) {
            this.runtime.queueInputEvent(event, this.game.getCurrentFrame());
        }
    }

    /**
     * Tells whether the game can be saved while this plugin runs it: never, since a saved game would lose the script's state.
     *
     * @return {@code false}
     */
    @Override
    public boolean allowsSave() {
        return false;
    }

    /**
     * Finds the slot of a player record in the game's player table.
     *
     * @return the slot id, or {@code -1} if no slot holds the record
     */
    private int findSlot(@NotNull Player player) {
        PlayerTable table = this.game.getPlayerTable();

        for (int slot = 0; slot < PlayerTable.PLAYER_SLOTS; slot++) {
            if (table.getPlayer(slot) == player) {
                return slot;
            }
        }

        return -1;
    }
}
