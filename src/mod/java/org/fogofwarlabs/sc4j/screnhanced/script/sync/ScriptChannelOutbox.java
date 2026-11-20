package org.fogofwarlabs.sc4j.screnhanced.script.sync;

import org.fogofwarlabs.sc4j.scr.game.extension.ScrModCommandSubmitter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.List;

/**
 * Keeps the channel commands the local scripts sent until the local player's mod command submitter takes them, in sending order.
 */
public final class ScriptChannelOutbox {
    private final @NotNull ArrayDeque<byte[]> commands;

    /**
     * Creates an empty {@link ScriptChannelOutbox} instance.
     */
    public ScriptChannelOutbox() {
        this.commands = new ArrayDeque<>();
    }

    /**
     * Adds the commands of a message after the waiting ones.
     *
     * @param commands the data of the commands ({@link ScriptChannelCommands#split})
     */
    public void add(@NotNull List<byte @NotNull []> commands) {
        this.commands.addAll(commands);
    }

    /**
     * Submits the waiting commands in order until the outgoing turn has no room; the rest waits for the next call. Without a submitter, as in a
     * replay or for a local observer, the waiting commands are dropped.
     *
     * @param submitter the local player's mod command submitter, or {@code null} if the game has none
     */
    public void submit(@Nullable ScrModCommandSubmitter submitter) {
        if (submitter == null) {
            this.commands.clear();

            return;
        }

        while (!this.commands.isEmpty() && submitter.submitModCommand(this.commands.peekFirst())) {
            this.commands.removeFirst();
        }
    }

    /**
     * Drops every waiting command.
     */
    public void clear() {
        this.commands.clear();
    }
}
