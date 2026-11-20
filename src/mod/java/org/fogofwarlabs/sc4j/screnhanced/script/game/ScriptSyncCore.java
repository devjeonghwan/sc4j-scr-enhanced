package org.fogofwarlabs.sc4j.screnhanced.script.game;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.screnhanced.script.sync.ScriptChannelCommands;
import org.fogofwarlabs.sc4j.screnhanced.script.sync.ScriptChannelOutbox;
import org.fogofwarlabs.sc4j.screnhanced.script.sync.ScriptValueUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines {@code core.sync}: the channels, which carry values from one peer to the scripts of every peer.
 * <p>
 * {@code createChannel(handler)} creates a channel and returns its id, the number of channels created before it; it works only while the
 * modules load, the prelude first, then the modules in load order, so the ids are the same on every peer. {@code send(channel, value)}
 * encodes the value ({@link ScriptValueUtils}) and queues its commands for the local player's outgoing turn ({@link ScriptChannelOutbox});
 * when a command runs, every peer and every replay of the game calls the channel's handler with the sending player's slot id and a copy of
 * the value. Sending changes no game state, so it works in local code and decisions too.
 */
final class ScriptSyncCore {
    private final @NotNull Scriptable global;

    private final @NotNull List<Function> handlers;
    private final @NotNull ScriptChannelOutbox outbox;
    private boolean creationClosed;

    /**
     * Creates a {@link ScriptSyncCore} instance without channels, open for creation.
     *
     * @param global the global scope
     */
    ScriptSyncCore(@NotNull Scriptable global) {
        this.global = global;

        this.handlers       = new ArrayList<>();
        this.outbox         = new ScriptChannelOutbox();
        this.creationClosed = false;
    }

    /**
     * @return the commands that wait for the local player's outgoing turn, the same instance for the whole game
     */
    @NotNull ScriptChannelOutbox getOutbox() {
        return this.outbox;
    }

    /**
     * Finds the handler of a channel.
     *
     * @param channel the channel id
     * @return the handler, or {@code null} if no channel has the id
     */
    @Nullable Function findHandler(int channel) {
        return channel >= 0 && channel < this.handlers.size() ? this.handlers.get(channel) : null;
    }


    /**
     * Ends the creation of channels, once the modules have loaded.
     */
    void closeCreation() {
        this.creationClosed = true;
    }

    /**
     * Decodes the value of a channel message.
     *
     * @param context the current context
     * @param value   the encoded value
     * @return the value
     * @throws IllegalArgumentException if the bytes are not one well-formed value
     */
    @Nullable Object decodeValue(@NotNull Context context, byte @NotNull [] value) {
        return ScriptValueUtils.decode(context, this.global, value);
    }

    /**
     * Defines the functions of {@code core.sync}.
     *
     * @param namespace the namespace object
     */
    void define(@NotNull Scriptable namespace) {
        ScriptCoreUtils.defineFunction(this.global, namespace, "createChannel", 1, (context, scope, thisObject, arguments) -> {
            Function handler = ScriptCoreUtils.readFunction(arguments, 0);

            if (this.creationClosed) {
                throw ScriptRuntime.typeError("A channel is created only while the modules load, at their top level.");
            }

            if (this.handlers.size() >= ScriptChannelCommands.CHANNEL_COUNT) {
                throw ScriptRuntime.rangeError(Exceptions.format("No more than {0} channels can be created.", ScriptChannelCommands.CHANNEL_COUNT));
            }

            this.handlers.add(handler);

            return (double) (this.handlers.size() - 1);
        });
        ScriptCoreUtils.defineFunction(this.global, namespace, "send", 2, (context, scope, thisObject, arguments) -> {
            int channel = ScriptCoreUtils.requireId("channel", ScriptCoreUtils.readInteger(arguments, 0), this.handlers.size());
            byte[] value = ScriptValueUtils.encode(arguments.length > 1 ? arguments[1] : Context.getUndefinedValue(), this.global);

            this.outbox.add(ScriptChannelCommands.split(channel, value));

            return Context.getUndefinedValue();
        });
    }
}
