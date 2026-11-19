package org.fogofwarlabs.sc4j.screnhanced.memory;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrGameExtension;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrGamePlugin;
import org.fogofwarlabs.sc4j.scr.game.state.Game;
import org.fogofwarlabs.sc4j.scr.game.state.GameData;
import org.fogofwarlabs.sc4j.scr.game.trigger.eud.ClassicMemorySource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Gives every game its classic memory ({@link ClassicMemory}).
 */
public final class ClassicMemoryExtension implements ScrGameExtension {
    /**
     * The installed resource icons.
     */
    private static final @NotNull String RESOURCE_ICONS_FILE = "game/icons.grp";

    /**
     * Creates the extension's plugin of a game: none, as the extension provides only memory.
     *
     * @param game the game, attached and not yet initialized
     * @return {@code null}
     */
    @Override
    public @Nullable ScrGamePlugin createPlugin(@NotNull Game game) {
        return null;
    }

    /**
     * Tells whether the extension provides EUD memory to the simulation ({@link #createMemorySource}); it answers the same for every game.
     *
     * @return {@code true}, as the extension provides classic memory to every game
     */
    @Override
    public boolean providesMemory() {
        return true;
    }

    /**
     * Creates the classic memory of a game, with the resource icons of the game's installation.
     *
     * @param game the game, attached and not yet initialized
     * @return the created {@link ClassicMemory} instance
     */
    @Override
    public @NotNull ClassicMemorySource createMemorySource(@NotNull Game game) {
        return new ClassicMemory(() -> {
            GameData data = game.getGameData();

            if (data == null) {
                throw Exceptions.newIOException("The game has no game data.");
            }

            return data.readInstalledFile(RESOURCE_ICONS_FILE);
        });
    }
}
