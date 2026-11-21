package org.fogofwarlabs.sc4j.screnhanced.script.game;

import org.fogofwarlabs.sc4j.scr.game.presentation.InterfaceLink;
import org.fogofwarlabs.sc4j.scr.game.presentation.LocalSelection;
import org.fogofwarlabs.sc4j.scr.game.trigger.eud.region.LocalView;
import org.jetbrains.annotations.NotNull;
import org.mozilla.javascript.Scriptable;

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
 */
final class ScriptLocalCore {
    private final @NotNull ScriptGame scriptGame;

    /**
     * Creates a {@link ScriptLocalCore} instance.
     *
     * @param scriptGame the game binding
     */
    ScriptLocalCore(@NotNull ScriptGame scriptGame) {
        this.scriptGame = scriptGame;
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
    }

    private @NotNull InterfaceLink getInterfaceLink() {
        return this.scriptGame.getGame().getInterfaceLink();
    }
}
