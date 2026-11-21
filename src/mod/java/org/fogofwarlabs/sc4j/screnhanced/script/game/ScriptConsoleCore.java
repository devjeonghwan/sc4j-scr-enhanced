package org.fogofwarlabs.sc4j.screnhanced.script.game;

import org.fogofwarlabs.sc4j.scr.game.extension.ScrConsolePlacement;
import org.fogofwarlabs.sc4j.screnhanced.graphics.ConsoleSettings;
import org.jetbrains.annotations.NotNull;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * Defines {@code core.console}: what a map script sets on the console ({@link ConsoleSettings}), which the classic console merges with what EUD
 * memory gives, the script's setting winning for each item it set. The functions run in shared and local code; shared code sets the console of
 * every peer, local code only the local one. They change no game state.
 * <p>
 * {@code setVisible(visible)} and {@code isVisible()} (the whole console); {@code setDialogVisible(dialog, visible)},
 * {@code setDialogPosition(dialog, x, y)} (the top left in the classic {@code 640} by {@code 480} screen), {@code isDialogVisible(dialog)} for
 * the dialogs of {@link ScrConsolePlacement.Dialog} in order; {@code replaceImage(image, imageId)} ({@code 0} the console art; {@code -1} for
 * the installed art) and {@code setResourceIcon(icon, imageId)} (the resource icon frames, {@code 0} minerals, {@code 1} to {@code 3} gas and
 * {@code 4} to {@code 6} supply of the Zerg, Terran and Protoss; {@code -1} for the icon of EUD memory or the installed one).
 */
final class ScriptConsoleCore {
    /**
     * The console images {@code replaceImage} takes: the console art.
     */
    private static final int CONSOLE_IMAGE_COUNT = 1;
    private static final @NotNull ScrConsolePlacement.Dialog @NotNull [] DIALOGS = ScrConsolePlacement.Dialog.values();

    private final @NotNull ConsoleSettings settings;
    private final @NotNull ScriptGraphicsCore graphicsCore;

    /**
     * Creates a {@link ScriptConsoleCore} instance.
     *
     * @param settings     the console settings of the script
     * @param graphicsCore the resource functions, which read image handles
     */
    ScriptConsoleCore(@NotNull ConsoleSettings settings, @NotNull ScriptGraphicsCore graphicsCore) {
        this.settings     = settings;
        this.graphicsCore = graphicsCore;
    }

    /**
     * Defines the functions of {@code core.console}.
     *
     * @param namespace the namespace object
     * @param global    the global scope
     */
    void define(@NotNull Scriptable namespace, @NotNull Scriptable global) {
        ScriptCoreUtils.defineFunction(global, namespace, "setVisible", 1, (context, scope, thisObject, arguments) -> {
            this.settings.setConsoleVisible(ScriptCoreUtils.readBoolean(arguments, 0));

            return Context.getUndefinedValue();
        });
        ScriptCoreUtils.defineFunction(global, namespace, "isVisible", 0, (context, scope, thisObject, arguments) -> this.settings.isConsoleVisible());
        ScriptCoreUtils.defineFunction(global, namespace, "setDialogVisible", 2, (context, scope, thisObject, arguments) -> {
            this.settings.setDialogVisible(readDialog(arguments), ScriptCoreUtils.readBoolean(arguments, 1));

            return Context.getUndefinedValue();
        });
        ScriptCoreUtils.defineFunction(global, namespace, "isDialogVisible", 1,
                                       (context, scope, thisObject, arguments) -> this.settings.isDialogVisible(readDialog(arguments)));
        ScriptCoreUtils.defineFunction(global, namespace, "setDialogPosition", 3, (context, scope, thisObject, arguments) -> {
            this.settings.setDialogPosition(readDialog(arguments), ScriptCoreUtils.readInteger(arguments, 1), ScriptCoreUtils.readInteger(arguments, 2));

            return Context.getUndefinedValue();
        });
        ScriptCoreUtils.defineFunction(global, namespace, "replaceImage", 2, (context, scope, thisObject, arguments) -> {
            ScriptCoreUtils.requireId("console image", ScriptCoreUtils.readInteger(arguments, 0), CONSOLE_IMAGE_COUNT);
            this.settings.setArt(this.graphicsCore.readOptionalPicture(arguments, 1));

            return Context.getUndefinedValue();
        });
        ScriptCoreUtils.defineFunction(global, namespace, "setResourceIcon", 2, (context, scope, thisObject, arguments) -> {
            int icon = ScriptCoreUtils.requireId("resource icon", ScriptCoreUtils.readInteger(arguments, 0), ConsoleSettings.RESOURCE_ICON_COUNT);

            this.settings.setResourceIcon(icon, this.graphicsCore.readOptionalPicture(arguments, 1));

            return Context.getUndefinedValue();
        });
    }

    private static ScrConsolePlacement.@NotNull Dialog readDialog(@NotNull Object @NotNull [] arguments) {
        return DIALOGS[ScriptCoreUtils.requireId("console dialog", ScriptCoreUtils.readInteger(arguments, 0), DIALOGS.length)];
    }
}
