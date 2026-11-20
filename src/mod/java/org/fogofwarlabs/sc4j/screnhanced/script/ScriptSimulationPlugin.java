package org.fogofwarlabs.sc4j.screnhanced.script;

import org.fogofwarlabs.sc4j.base.log.LogLevel;
import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.engine.map.MapModel;
import org.fogofwarlabs.sc4j.scr.data.format.chk.ChkFile;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrSimulationPlugin;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.SimulationDecision;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.SimulationEvent;
import org.fogofwarlabs.sc4j.scr.game.state.Game;
import org.fogofwarlabs.sc4j.screnhanced.script.runtime.ScriptRuntime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the script package of one game's scenario in the game's simulation.
 * <p>
 * At the first frame start the plugin reads the scenario's script package ({@link ScriptStorageUtils#readScripts}) from the game's map model.
 * A scenario without one, or with one that cannot be read, leaves the plugin idle for the rest of the game. Otherwise the plugin creates the
 * {@link ScriptRuntime} with the game API and a seed from the game's setup seed, the same on every peer and in every replay, starts it and
 * runs its first frame.
 * <p>
 * The events of the game go into the runtime's queue with their frame, the events from before the first frame (the units the map and the
 * game start create) as well, and the runtime delivers them at the next frame start, before the triggers. A frame start whose frame number
 * did not advance, as while the game is paused, runs no script. Decisions go to the runtime at once. Mod commands are not used.
 */
public final class ScriptSimulationPlugin implements ScrSimulationPlugin {
    private static final @NotNull Logger LOGGER = Logger.of(ScriptSimulationPlugin.class);
    private static final @NotNull String SCENARIO_DESCRIPTION = "the game's scenario";

    /**
     * An event from before the first frame start, kept until the runtime exists.
     */
    private static final class StartEvent {
        private final @NotNull SimulationEvent event;
        private final int frame;

        private StartEvent(@NotNull SimulationEvent event, int frame) {
            this.event = event;
            this.frame = frame;
        }
    }


    private final @NotNull Game game;

    private final @NotNull List<StartEvent> startEvents;
    private @Nullable ScriptRuntime runtime;
    private boolean started;
    private int lastFrame;

    /**
     * Creates a {@link ScriptSimulationPlugin} instance that has not read the scenario.
     *
     * @param game the game
     */
    public ScriptSimulationPlugin(@NotNull Game game) {
        this.game = game;

        this.startEvents = new ArrayList<>();
        this.runtime     = null;
        this.started     = false;
        this.lastFrame   = 0;
    }

    /**
     * @return the script runtime of the game's script package, or {@code null} before the first frame start and for a scenario without
     * a script package
     */
    public @Nullable ScriptRuntime getRuntime() {
        return this.runtime;
    }


    /**
     * Called once per frame when the frame's trigger step runs, before the triggers, with the random generator enabled as for the triggers; the
     * trigger step also runs while the game is paused.
     * <p>
     * The first call starts the script; every call whose frame number advanced runs the script's frame.
     */
    @Override
    public void onFrameStarted() {
        int frame = this.game.getCurrentFrame();
        boolean first = !this.started;

        if (first) {
            this.startScript();
        }

        ScriptRuntime scriptRuntime = this.runtime;

        if (scriptRuntime == null || !first && frame == this.lastFrame) {
            return;
        }

        this.lastFrame = frame;

        scriptRuntime.runFrame(frame);
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
        ScriptRuntime scriptRuntime = this.runtime;

        if (scriptRuntime != null) {
            scriptRuntime.queueEvent(event, this.game.getCurrentFrame());
        } else if (!this.started) {
            this.startEvents.add(new StartEvent(event, this.game.getCurrentFrame()));
        }
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
        ScriptRuntime scriptRuntime = this.runtime;

        if (scriptRuntime != null) {
            scriptRuntime.decide(decision);
        }
    }

    /**
     * Reads the scenario's script package and, when there is one, creates the runtime, queues the events from before the first frame and
     * starts the script.
     */
    private void startScript() {
        this.started = true;

        ScriptPackage scriptPackage = this.findScriptPackage();

        if (scriptPackage == null) {
            this.startEvents.clear();

            return;
        }

        ScriptRuntime scriptRuntime = ScriptRuntime.create(scriptPackage, this.game, Integer.toUnsignedLong(this.game.getSetup().getSeed()));

        for (StartEvent startEvent : this.startEvents) {
            scriptRuntime.queueEvent(startEvent.event, startEvent.frame);
        }

        this.startEvents.clear();

        this.runtime = scriptRuntime;

        scriptRuntime.start();
    }

    /**
     * Reads the script package of the game's scenario, the map data of the game's map model.
     *
     * @return the script package, or {@code null} if the game has no scenario, the scenario has no script package or it cannot be read
     */
    private @Nullable ScriptPackage findScriptPackage() {
        MapModel mapModel = this.game.getMapModel();

        if (mapModel == null || !(mapModel.getModData() instanceof ChkFile scenario)) {
            return null;
        }

        try {
            return ScriptStorageUtils.readScripts(scenario, SCENARIO_DESCRIPTION);
        } catch (IOException exception) {
            LOGGER.exception(LogLevel.ERROR, exception, "The script package of {0} cannot be read; the game runs without it.", SCENARIO_DESCRIPTION);

            return null;
        }
    }
}
