package org.fogofwarlabs.sc4j.screnhanced.script;

import org.fogofwarlabs.sc4j.scr.game.extension.ScrSimulationExtension;
import org.fogofwarlabs.sc4j.scr.game.state.Game;
import org.jetbrains.annotations.NotNull;

/**
 * Runs the script package of a map's scenario ({@link ScriptStorageUtils}) in the games of SC:R Enhanced: every game gets a
 * {@link ScriptSimulationPlugin}, which finds the scenario's script package at the first frame and stays idle for a map without one.
 */
public final class ScriptSimulationExtension implements ScrSimulationExtension {
    /**
     * Creates the extension's simulation plugin of a game, once, when the game's SC:R system attaches; the plugin lives as long as the game, so
     * every new game, live, replay or loaded save, starts with a new plugin.
     * <p>
     * The game's scenario is known only after the game has loaded its map, so every game gets a plugin.
     *
     * @param game the game, attached and not yet initialized
     * @return the created {@link ScriptSimulationPlugin} instance
     */
    @Override
    public @NotNull ScriptSimulationPlugin createPlugin(@NotNull Game game) {
        return new ScriptSimulationPlugin(game);
    }
}
