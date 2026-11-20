package org.fogofwarlabs.sc4j.screnhanced.script;

import org.fogofwarlabs.sc4j.base.log.LogLevel;
import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.scr.data.format.chk.ChkFile;
import org.fogofwarlabs.sc4j.scr.data.format.mpq.MpqArchive;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrSimulationExtension;
import org.fogofwarlabs.sc4j.scr.game.state.Game;
import org.fogofwarlabs.sc4j.scr.game.state.GameSetup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Runs the script package of a map's scenario ({@link ScriptStorageUtils}) in the games of SC:R Enhanced: a game whose scenario has a script
 * package gets a {@link ScriptSimulationPlugin}, any other game none.
 * <p>
 * The plugin also gets the map's resource file ({@value ScriptStorageUtils#RESOURCE_FILE_NAME}): a live game started from a map file reads it
 * from the map archive and stores its bytes as the replay section {@value ScriptStorageUtils#RESOURCE_REPLAY_SECTION}, so that its replays
 * carry it; a replay played back reads it from that section. A game without either, or whose resource file cannot be read, runs the script
 * without resources.
 */
public final class ScriptSimulationExtension implements ScrSimulationExtension {
    private static final @NotNull Logger LOGGER = Logger.of(ScriptSimulationExtension.class);
    private static final @NotNull String SCENARIO_DESCRIPTION = "the game's scenario";
    private static final @NotNull String RESOURCE_DESCRIPTION = "the game's resource file";

    /**
     * Reads the script package of a game's scenario.
     *
     * @param setup the game's setup
     * @return the script package, or {@code null} if the game has no scenario, the scenario has no script package or it cannot be read
     */
    private static @Nullable ScriptPackage readScriptPackage(@NotNull GameSetup setup) {
        ChkFile scenario = setup.getScenario();

        if (scenario == null) {
            return null;
        }

        try {
            return ScriptStorageUtils.readScripts(scenario, SCENARIO_DESCRIPTION);
        } catch (IOException exception) {
            LOGGER.exception(LogLevel.ERROR, exception, "The script package of {0} cannot be read; the game runs without it.", SCENARIO_DESCRIPTION);

            return null;
        }
    }

    /**
     * Reads the resource file of a game: from the map archive of a live game, whose bytes become the game's replay section, or from the replay
     * section of a replay played back.
     *
     * @param game the game
     * @return the resources, {@link ScriptResources#EMPTY} if the game has no resource file or it cannot be read
     */
    private static @NotNull ScriptResources readResources(@NotNull Game game) {
        GameSetup setup = game.getSetup();
        MpqArchive archive = setup.getMapArchive();
        byte[] resourceFile = null;

        try {
            if (archive == null) {
                resourceFile = setup.readReplaySection(ScriptStorageUtils.RESOURCE_REPLAY_SECTION);
            } else if (archive.findEntry(ScriptStorageUtils.RESOURCE_FILE_NAME) != null) {
                resourceFile = archive.readFile(ScriptStorageUtils.RESOURCE_FILE_NAME);

                game.setReplaySection(ScriptStorageUtils.RESOURCE_REPLAY_SECTION, resourceFile);
            }

            return resourceFile == null ? ScriptResources.EMPTY : ScriptStorageUtils.readResources(resourceFile, RESOURCE_DESCRIPTION);
        } catch (IOException exception) {
            LOGGER.exception(LogLevel.ERROR, exception, "The resources of {0} cannot be read; the script runs without them.", RESOURCE_DESCRIPTION);

            return ScriptResources.EMPTY;
        }
    }


    /**
     * Creates the extension's simulation plugin of a game, once, when the game's SC:R system attaches; the plugin lives as long as the game, so
     * every new game, live, replay or loaded save, starts with a new plugin.
     * <p>
     * Only a game whose scenario has a readable script package gets a plugin, which gets the package and the map's resources.
     *
     * @param game the game, attached and not yet initialized
     * @return the created {@link ScriptSimulationPlugin} instance, or {@code null} if the game's scenario has no readable script package
     */
    @Override
    public @Nullable ScriptSimulationPlugin createPlugin(@NotNull Game game) {
        ScriptPackage scriptPackage = readScriptPackage(game.getSetup());

        if (scriptPackage == null) {
            return null;
        }

        return new ScriptSimulationPlugin(game, scriptPackage, readResources(game));
    }
}
