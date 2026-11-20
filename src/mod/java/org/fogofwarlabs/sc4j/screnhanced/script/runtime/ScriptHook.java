package org.fogofwarlabs.sc4j.screnhanced.script.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Lists the hooks that the script runtime calls: top-level functions of a module with a known name.
 * <p>
 * A hook name is {@value #NAME_PREFIX} followed by an upper-case letter. The runtime collects the hooks of each module when the module has
 * loaded and calls a hook in every module that has it, in load order.
 */
public enum ScriptHook {
    /**
     * Called once after the main script has loaded, without arguments.
     */
    START("onStart"),
    /**
     * Called every game frame with the frame number.
     */
    FRAME("onFrame");

    /**
     * The prefix of every hook name.
     */
    public static final @NotNull String NAME_PREFIX = "on";

    private static final @NotNull Map<String, ScriptHook> HOOKS_BY_FUNCTION_NAME = new HashMap<>();

    static {
        for (ScriptHook hook : values()) {
            HOOKS_BY_FUNCTION_NAME.put(hook.functionName, hook);
        }
    }

    /**
     * Finds the hook of a function name.
     *
     * @param functionName the function name
     * @return the hook with the function name, or {@code null} if no hook has it
     */
    public static @Nullable ScriptHook findByFunctionName(@NotNull String functionName) {
        return HOOKS_BY_FUNCTION_NAME.get(functionName);
    }

    /**
     * Checks whether a function name has the form of a hook name.
     *
     * @param functionName the function name
     * @return {@code true} if the name is {@value #NAME_PREFIX} followed by an upper-case ASCII letter and more, {@code false} otherwise
     */
    public static boolean isHookName(@NotNull String functionName) {
        if (functionName.length() <= NAME_PREFIX.length() || !functionName.startsWith(NAME_PREFIX)) {
            return false;
        }

        char first = functionName.charAt(NAME_PREFIX.length());

        return first >= 'A' && first <= 'Z';
    }


    private final @NotNull String functionName;

    ScriptHook(@NotNull String functionName) {
        this.functionName = functionName;
    }

    /**
     * @return the name of the hook function
     */
    public @NotNull String getFunctionName() {
        return this.functionName;
    }
}
