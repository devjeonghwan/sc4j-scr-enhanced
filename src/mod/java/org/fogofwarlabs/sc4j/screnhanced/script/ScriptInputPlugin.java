package org.fogofwarlabs.sc4j.screnhanced.script;

import org.fogofwarlabs.sc4j.scr.game.extension.ScrGamePlugin;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrKeyInput;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrModCommandSubmitter;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrMouseInput;
import org.fogofwarlabs.sc4j.scr.game.trigger.eud.region.LocalView;
import org.fogofwarlabs.sc4j.screnhanced.script.input.ScriptLocalInput;
import org.fogofwarlabs.sc4j.screnhanced.script.runtime.ScriptRuntime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Hands the local player's keys, mouse buttons and chat lines to the local hooks of the game's script ({@link ScriptRuntime#callLocalHook}),
 * and each frame the local machine plays to {@code onLocalFrame} ({@link ScriptRuntime#callLocalFrame}), then the channel commands that wait
 * to the local player's outgoing turn ({@link ScriptRuntime#submitCommands}).
 * <p>
 * A key event has the map position under the pointer while the local view is known; a mouse button event has the map position under the
 * pointer. An input that a hook consumes reaches neither the later plugins nor the game; the consumption of a release has no effect.
 * <p>
 * A game without a submitter (a replay, or a local observer) calls no input hook and consumes nothing, but calls {@code onLocalFrame}, whose
 * channel commands are dropped. A game without a script calls no local hook.
 */
public final class ScriptInputPlugin implements ScrGamePlugin {
    private final @NotNull Supplier<@Nullable ScriptRuntime> runtime;
    private final @NotNull Supplier<@Nullable ScrModCommandSubmitter> submitter;
    private final @NotNull LocalView localView;

    /**
     * Creates a {@link ScriptInputPlugin} instance.
     *
     * @param runtime   the runtime of the game's script, {@code null} while the game has no script
     * @param submitter the mod command submitter of the game, {@code null} while the game has none
     * @param localView the local view of the game, whose screen and mouse positions give the map position under the pointer
     */
    public ScriptInputPlugin(@NotNull Supplier<@Nullable ScriptRuntime> runtime, @NotNull Supplier<@Nullable ScrModCommandSubmitter> submitter,
                             @NotNull LocalView localView) {
        this.runtime   = runtime;
        this.submitter = submitter;
        this.localView = localView;
    }


    /**
     * Called when the local player presses or releases a key in a running game, live or replayed, before the game's own keys see the press: the
     * command card hotkeys, the control group and screen position keys, the speed keys and Escape. The plugins see an input in extension order
     * until one consumes it. The client delivers a press only while the game takes play keys: not while the chat box is open, a game menu or
     * dialog is open, or the triggers pause the game. It does not deliver the keys that open the chat box (Enter) and the game menu (F10), the
     * presses that run an interface accelerator (the menus and their dialogs, the sound, music and graphics toggles, the chat history), nor the
     * repeats of a held key.
     * A release is delivered when its press was, also if the chat box opened in between; a release reaches no game key, so its consumption has no
     * effect.
     * <p>
     * The key goes to {@code onLocalKeyDown} or {@code onLocalKeyUp}, with the map position under the pointer while the local view is known.
     *
     * @param input the key input
     * @return {@code true} if a local hook consumed the key, {@code false} otherwise
     */
    @Override
    public boolean onKeyInput(@NotNull ScrKeyInput input) {
        boolean positioned = this.localView.isKnown();
        int x = positioned ? this.localView.getScreenX() + this.localView.getMouseX() : 0;
        int y = positioned ? this.localView.getScreenY() + this.localView.getMouseY() : 0;

        return this.callLocalHook(ScriptLocalInput.ofKey(input.getVirtualKey(), input.isPressed(), input.getModifiers(), positioned, x, y));
    }

    /**
     * Called when the local player presses or releases a mouse button over the map of a running game, live or replayed, before the game's own
     * handling: selection, orders, targeting and placement. The plugins see an input in extension order until one consumes it. The client delivers
     * a press over the map view only: not over the console, its minimap and dialogs, the replay controls or a game menu. A release is delivered when
     * its press was, wherever the pointer is; it reaches the game when its press did, whatever the plugin returns for it.
     * <p>
     * The button goes to {@code onLocalMouseDown} or {@code onLocalMouseUp} with the map position under the pointer.
     *
     * @param input the mouse input
     * @return {@code true} if a local hook consumed the button, {@code false} otherwise
     */
    @Override
    public boolean onMouseInput(@NotNull ScrMouseInput input) {
        return this.callLocalHook(ScriptLocalInput.ofMouse(input.getButton().ordinal(), input.isPressed(), input.getModifiers(), input.getMapX(),
                                                           input.getMapY()));
    }

    /**
     * Called when the local player submits a chat line in a running live game, before the game's own handling of the line: the cheat codes, then
     * the chat to the chosen recipients. The plugins see a line in extension order until one consumes it. The client does not deliver the empty
     * lines nor the lines of its own commands ({@code /} lines).
     * <p>
     * The line goes to {@code onLocalChat}.
     *
     * @param text the typed line
     * @return {@code true} if a local hook consumed the line, which is then neither sent nor shown, {@code false} otherwise
     */
    @Override
    public boolean onChatInput(@NotNull String text) {
        return this.callLocalHook(ScriptLocalInput.ofChat(text));
    }

    /**
     * Called on the local machine after each game frame the client plays, live or replayed, at every playback speed and for a single step of a
     * paused replay; not for the frames a replay seek runs.
     * <p>
     * The frame number goes to {@code onLocalFrame} when the game has a script, also in a replay and for an observer; the channel commands that
     * wait then go to the submitter, or are dropped without one.
     *
     * @param frame the number of the frame that ran, the game's current frame
     */
    @Override
    public void onFramePlayed(int frame) {
        ScriptRuntime scriptRuntime = this.runtime.get();

        if (scriptRuntime == null) {
            return;
        }

        scriptRuntime.callLocalFrame(frame);
        scriptRuntime.submitCommands(this.submitter.get());
    }

    /**
     * Calls the local hook of an input when the game has a script and a submitter, then submits the channel commands that wait.
     *
     * @return {@code true} if a hook consumed the input, {@code false} otherwise
     */
    private boolean callLocalHook(@NotNull ScriptLocalInput input) {
        ScriptRuntime scriptRuntime = this.runtime.get();
        ScrModCommandSubmitter commands = this.submitter.get();

        if (scriptRuntime == null || commands == null) {
            return false;
        }

        boolean consumed = scriptRuntime.callLocalHook(input);

        scriptRuntime.submitCommands(commands);

        return consumed;
    }
}
