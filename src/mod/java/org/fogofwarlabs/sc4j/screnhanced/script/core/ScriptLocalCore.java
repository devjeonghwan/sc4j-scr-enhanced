package org.fogofwarlabs.sc4j.screnhanced.script.core;

import org.fogofwarlabs.sc4j.scr.game.presentation.InterfaceLink;
import org.fogofwarlabs.sc4j.scr.game.presentation.LocalSelection;
import org.fogofwarlabs.sc4j.scr.game.trigger.eud.region.LocalView;
import org.fogofwarlabs.sc4j.screnhanced.script.input.ScriptInputKind;
import org.fogofwarlabs.sc4j.screnhanced.script.input.ScriptLocalInput;
import org.jetbrains.annotations.NotNull;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines {@code core.local}: what only the local machine knows, which differs from peer to peer, so that every function throws a
 * {@code TypeError} outside local code ({@link ScriptGame#checkLocal()}).
 * <p>
 * {@code getPlayer()} (the slot id of the local player, {@code -1} in a replay and for an observer), {@code isReplay()}, {@code isChatOpen()}
 * (the chat box is open), {@code isViewKnown()} (the client supplied the screen and pointer positions), {@code getScreenX()} and
 * {@code getScreenY()} (the map position of the screen's top left), {@code getMouseX()} and {@code getMouseY()} (the map position under the
 * pointer: the screen position plus the pointer's screen position), and {@code getSelection()} (the ids of the live units of the local
 * interface's selection, up to its first empty slot). {@code setScreenPosition(x, y)} moves the local screen's top left at once, as the camera
 * move of Center View does ({@link LocalView#moveScreen}), and returns {@code false} while the view or its display geometry is unknown; it
 * changes no game state.
 * <p>
 * {@code getInputEvents()} returns a new array of the records of the local inputs that reached the {@code localInput} entry point before the
 * current local frame ({@link #startLocalFrame()}), read-only after that call ({@link #createInputRecord}).
 */
final class ScriptLocalCore {
    /**
     * Creates the record of a local input: its {@code kind} (the ordinal of {@link ScriptInputKind}); for a key {@code key}, for a mouse
     * button {@code button}, and for both {@code modifiers} and {@code positioned} with the map position {@code x} and {@code y}; for a chat
     * line {@code text}; and {@code consumed}, {@code false} until a hook consumes the input.
     *
     * @param context the current context
     * @param global  the global scope
     * @param input   the local input
     * @return the record
     */
    static @NotNull ScriptableObject createInputRecord(@NotNull Context context, @NotNull Scriptable global, @NotNull ScriptLocalInput input) {
        ScriptableObject record = (ScriptableObject) context.newObject(global);
        ScriptInputKind kind = input.getKind();

        ScriptableObject.putProperty(record, "kind", (double) kind.ordinal());

        if (kind == ScriptInputKind.CHAT) {
            ScriptableObject.putProperty(record, "text", input.getText());
        } else {
            boolean mouse = kind == ScriptInputKind.MOUSE_DOWN || kind == ScriptInputKind.MOUSE_UP;

            ScriptableObject.putProperty(record, mouse ? "button" : "key", (double) input.getCode());
            ScriptableObject.putProperty(record, "modifiers", (double) input.getModifiers());
            ScriptableObject.putProperty(record, "positioned", input.isPositioned());
            ScriptableObject.putProperty(record, "x", (double) input.getX());
            ScriptableObject.putProperty(record, "y", (double) input.getY());
        }

        ScriptableObject.putProperty(record, "consumed", false);

        return record;
    }


    private final @NotNull ScriptGame scriptGame;

    private @NotNull List<ScriptableObject> waitingInputs;
    private @NotNull List<ScriptableObject> frameInputs;

    /**
     * Creates a {@link ScriptLocalCore} instance without inputs.
     *
     * @param scriptGame the game binding
     */
    ScriptLocalCore(@NotNull ScriptGame scriptGame) {
        this.scriptGame = scriptGame;

        this.waitingInputs = new ArrayList<>();
        this.frameInputs   = new ArrayList<>();
    }

    /**
     * Adds the record of a local input that went through the {@code localInput} entry point; it becomes read-only.
     *
     * @param record the record
     */
    void addInput(@NotNull ScriptableObject record) {
        this.waitingInputs.add(ScriptCoreUtils.freeze(record));
    }

    /**
     * Makes the inputs that went through the {@code localInput} entry point since the previous local frame the current local frame's inputs.
     */
    void startLocalFrame() {
        List<ScriptableObject> inputs = this.frameInputs;

        inputs.clear();

        this.frameInputs   = this.waitingInputs;
        this.waitingInputs = inputs;
    }

    /**
     * Forgets every input.
     */
    void clear() {
        this.waitingInputs.clear();
        this.frameInputs.clear();
    }

    /**
     * Defines the functions of {@code core.local}.
     *
     * @param namespace the namespace object
     * @param global    the global scope
     */
    void define(@NotNull Scriptable namespace, @NotNull Scriptable global) {
        ScriptCoreUtils.defineFunction(global, namespace, "getPlayer", 0, (context, scope, thisObject, arguments) -> {
            this.scriptGame.checkLocal();

            return (double) this.scriptGame.findLocalPlayer();
        });
        ScriptCoreUtils.defineFunction(global, namespace, "isReplay", 0, (context, scope, thisObject, arguments) -> {
            this.scriptGame.checkLocal();

            return this.scriptGame.getGame().isReplayPlayback();
        });
        ScriptCoreUtils.defineFunction(global, namespace, "isChatOpen", 0, (context, scope, thisObject, arguments) -> {
            this.scriptGame.checkLocal();

            return this.getInterfaceLink().getLocalInterface().isChatOpen();
        });
        ScriptCoreUtils.defineFunction(global, namespace, "isViewKnown", 0, (context, scope, thisObject, arguments) -> {
            this.scriptGame.checkLocal();

            return this.getInterfaceLink().getLocalView().isKnown();
        });
        ScriptCoreUtils.defineFunction(global, namespace, "getScreenX", 0, (context, scope, thisObject, arguments) -> {
            this.scriptGame.checkLocal();

            return (double) this.getInterfaceLink().getLocalView().getScreenX();
        });
        ScriptCoreUtils.defineFunction(global, namespace, "getScreenY", 0, (context, scope, thisObject, arguments) -> {
            this.scriptGame.checkLocal();

            return (double) this.getInterfaceLink().getLocalView().getScreenY();
        });
        ScriptCoreUtils.defineFunction(global, namespace, "getMouseX", 0, (context, scope, thisObject, arguments) -> {
            this.scriptGame.checkLocal();

            LocalView view = this.getInterfaceLink().getLocalView();

            return (double) (view.getScreenX() + view.getMouseX());
        });
        ScriptCoreUtils.defineFunction(global, namespace, "getMouseY", 0, (context, scope, thisObject, arguments) -> {
            this.scriptGame.checkLocal();

            LocalView view = this.getInterfaceLink().getLocalView();

            return (double) (view.getScreenY() + view.getMouseY());
        });
        ScriptCoreUtils.defineFunction(global, namespace, "setScreenPosition", 2, (context, scope, thisObject, arguments) -> {
            this.scriptGame.checkLocal();

            int x = ScriptCoreUtils.readInteger(arguments, 0);
            int y = ScriptCoreUtils.readInteger(arguments, 1);

            return this.getInterfaceLink().getLocalView().moveScreen(x, y);
        });
        ScriptCoreUtils.defineFunction(global, namespace, "getSelection", 0, (context, scope, thisObject, arguments) -> {
            this.scriptGame.checkLocal();

            LocalSelection selection = this.getInterfaceLink().getLocalSelection();

            return ScriptUnitCore.createSelectionIds(context, global, this.scriptGame.getGame().getUnitSystem().requireUnitPool(),
                                                     selection::getSelectedPoolIndex);
        });
        ScriptCoreUtils.defineFunction(global, namespace, "getInputEvents", 0, (context, scope, thisObject, arguments) -> {
            this.scriptGame.checkLocal();

            return context.newArray(global, this.frameInputs.toArray());
        });
    }

    private @NotNull InterfaceLink getInterfaceLink() {
        return this.scriptGame.getGame().getInterfaceLink();
    }
}
