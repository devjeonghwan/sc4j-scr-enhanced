package org.fogofwarlabs.sc4j.screnhanced.script.core;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.scr.game.presentation.MessageLines;
import org.fogofwarlabs.sc4j.scr.game.presentation.StatTextOverrides;
import org.jetbrains.annotations.NotNull;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

/**
 * Defines {@code core.strings} and {@code core.messages}: the game texts a map script shows. The functions run in shared and local code; shared
 * code changes the texts of every peer, local code only the local ones. They change no game state.
 * <p>
 * {@code core.strings.setStatText(id, text)} shows a text instead of a {@code stat_txt} string ({@link StatTextOverrides}), the one-based string
 * identifier of the game's table, from {@code 1} to {@value #MAXIMUM_STRING_ID}; {@code null} shows the game's string again. A script text wins
 * over the installed table and over the table EUD memory commits; the name string of a unit type also wins over the map's name of the type.
 * <p>
 * {@code core.messages.clear()} empties the message lines on the screen.
 */
final class ScriptTextCore {
    /**
     * The largest {@code stat_txt} string identifier: the table counts its strings in a WORD.
     */
    private static final int MAXIMUM_STRING_ID = 0xFFFF;

    private final @NotNull ScriptGame scriptGame;

    /**
     * Creates a {@link ScriptTextCore} instance.
     *
     * @param scriptGame the game binding
     */
    ScriptTextCore(@NotNull ScriptGame scriptGame) {
        this.scriptGame = scriptGame;
    }

    /**
     * Defines the functions of {@code core.strings}.
     *
     * @param namespace the namespace object
     * @param global    the global scope
     */
    void defineStrings(@NotNull Scriptable namespace, @NotNull Scriptable global) {
        ScriptCoreUtils.defineFunction(global, namespace, "setStatText", 2, (context, scope, thisObject, arguments) -> {
            int id = ScriptCoreUtils.readInteger(arguments, 0);
            Object text = arguments.length > 1 ? arguments[1] : null;

            if (id < 1 || id > MAXIMUM_STRING_ID) {
                throw ScriptRuntime.rangeError(Exceptions.format("The stat_txt string id {0} is not from 1 to {1}.", id, MAXIMUM_STRING_ID));
            }

            this.getOverrides().set(id, text == null || text instanceof Undefined ? null : ScriptCoreUtils.readString(arguments, 1));

            return Context.getUndefinedValue();
        });
    }

    /**
     * Defines the functions of {@code core.messages}.
     *
     * @param namespace the namespace object
     * @param global    the global scope
     */
    void defineMessages(@NotNull Scriptable namespace, @NotNull Scriptable global) {
        ScriptCoreUtils.defineFunction(global, namespace, "clear", 0, (context, scope, thisObject, arguments) -> {
            MessageLines lines = this.scriptGame.getGame().getInterfaceLink().getMessageLines();

            for (int line = 0; line < MessageLines.LINE_COUNT; line++) {
                lines.writeByte(line * MessageLines.LINE_SIZE, 0);
            }

            return Context.getUndefinedValue();
        });
    }

    private @NotNull StatTextOverrides getOverrides() {
        return this.scriptGame.getGame().getInterfaceLink().getStatTextOverrides();
    }
}
