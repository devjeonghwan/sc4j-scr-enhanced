package org.fogofwarlabs.sc4j.screnhanced.script;

import org.fogofwarlabs.sc4j.scr.game.extension.ScrGamePlugin;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrKeyInput;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrModCommandSubmitter;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrMouseInput;
import org.fogofwarlabs.sc4j.scr.game.trigger.eud.region.LocalView;
import org.fogofwarlabs.sc4j.screnhanced.script.input.ScriptInputCommand;
import org.fogofwarlabs.sc4j.screnhanced.script.input.ScriptInputState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Sends the local input that the script of a game tracks ({@link ScriptInputState}) as mod commands of the local player
 * ({@link ScriptInputCommand}), so that every peer's script receives it at the same frame.
 * <p>
 * A press of a tracked key or button is submitted, with the map position under the pointer for a button, and for a key while the script tracks
 * the mouse; the press is consumed when the script asks for it, also if its command was not submitted. A release is submitted when its press
 * was, so the commands of a key or button alternate. A command that the outgoing turn has no room for is dropped, not submitted again: a
 * dropped press leaves the key up for the script, a dropped release leaves it down until its next release.
 * <p>
 * A game without a submitter (a replay, or a local observer) or without a script sends nothing and consumes nothing.
 */
public final class ScriptInputPlugin implements ScrGamePlugin {
    private final @NotNull Supplier<@Nullable ScriptInputState> inputState;
    private final @NotNull Supplier<@Nullable ScrModCommandSubmitter> submitter;
    private final @NotNull LocalView localView;

    private final boolean @NotNull [] submittedKeys;
    private final boolean @NotNull [] submittedButtons;

    /**
     * Creates a {@link ScriptInputPlugin} instance that has submitted no press.
     *
     * @param inputState the input state of the game's script, {@code null} while the game has no script
     * @param submitter  the mod command submitter of the game, {@code null} while the game has none
     * @param localView  the local view of the game, whose screen and mouse positions give the map position under the pointer
     */
    public ScriptInputPlugin(@NotNull Supplier<@Nullable ScriptInputState> inputState, @NotNull Supplier<@Nullable ScrModCommandSubmitter> submitter,
                             @NotNull LocalView localView) {
        this.inputState = inputState;
        this.submitter  = submitter;
        this.localView  = localView;

        this.submittedKeys    = new boolean[ScriptInputCommand.KEY_COUNT];
        this.submittedButtons = new boolean[ScriptInputCommand.BUTTON_COUNT];
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
     * A press of a tracked key is submitted, with the map position under the pointer while the mouse is tracked and the local view is known; a
     * release is submitted when its press was.
     *
     * @param input the key input
     * @return {@code true} if the key is tracked and the script consumes its presses, {@code false} otherwise
     */
    @Override
    public boolean onKeyInput(@NotNull ScrKeyInput input) {
        ScriptInputState state = this.inputState.get();
        ScrModCommandSubmitter commands = this.submitter.get();
        int virtualKey = input.getVirtualKey();

        if (state == null || commands == null) {
            return false;
        }

        if (!input.isPressed()) {
            if (this.submittedKeys[virtualKey]) {
                this.submittedKeys[virtualKey] = false;

                commands.submitModCommand(this.createKeyCommand(state, virtualKey, false, input.getModifiers()).encode());
            }

            return false;
        }

        if (!state.isKeyTracked(virtualKey)) {
            return false;
        }

        this.submittedKeys[virtualKey] = commands.submitModCommand(this.createKeyCommand(state, virtualKey, true, input.getModifiers()).encode());

        return state.isConsuming();
    }

    /**
     * Called when the local player presses or releases a mouse button over the map of a running game, live or replayed, before the game's own
     * handling: selection, orders, targeting and placement. The plugins see an input in extension order until one consumes it. The client delivers
     * a press over the map view only: not over the console, its minimap and dialogs, the replay controls or a game menu. A release is delivered when
     * its press was, wherever the pointer is; it reaches the game when its press did, whatever the plugin returns for it.
     * <p>
     * A press of a tracked button is submitted with the map position under the pointer; a release is submitted when its press was.
     *
     * @param input the mouse input
     * @return {@code true} if the button is tracked and the script consumes its presses, {@code false} otherwise
     */
    @Override
    public boolean onMouseInput(@NotNull ScrMouseInput input) {
        ScriptInputState state = this.inputState.get();
        ScrModCommandSubmitter commands = this.submitter.get();
        int button = input.getButton().ordinal();

        if (state == null || commands == null) {
            return false;
        }

        int x = Math.clamp(input.getMapX(), 0, ScriptInputCommand.MAXIMUM_COORDINATE);
        int y = Math.clamp(input.getMapY(), 0, ScriptInputCommand.MAXIMUM_COORDINATE);

        if (!input.isPressed()) {
            if (this.submittedButtons[button]) {
                this.submittedButtons[button] = false;

                commands.submitModCommand(ScriptInputCommand.ofMouse(button, false, input.getModifiers(), x, y).encode());
            }

            return false;
        }

        if (!state.isButtonTracked(button)) {
            return false;
        }

        this.submittedButtons[button] = commands.submitModCommand(ScriptInputCommand.ofMouse(button, true, input.getModifiers(), x, y).encode());

        return state.isConsuming();
    }

    /**
     * Creates the command of a key change: with the map position under the pointer while the mouse is tracked and the local view is known.
     */
    private @NotNull ScriptInputCommand createKeyCommand(@NotNull ScriptInputState state, int virtualKey, boolean pressed, int modifiers) {
        if (!state.isMouseTracked() || !this.localView.isKnown()) {
            return ScriptInputCommand.ofKey(virtualKey, pressed, modifiers);
        }

        int x = Math.clamp((long) this.localView.getScreenX() + this.localView.getMouseX(), 0, ScriptInputCommand.MAXIMUM_COORDINATE);
        int y = Math.clamp((long) this.localView.getScreenY() + this.localView.getMouseY(), 0, ScriptInputCommand.MAXIMUM_COORDINATE);

        return ScriptInputCommand.ofKey(virtualKey, pressed, modifiers, x, y);
    }
}
