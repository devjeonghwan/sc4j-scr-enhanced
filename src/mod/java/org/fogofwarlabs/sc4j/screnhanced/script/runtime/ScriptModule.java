package org.fogofwarlabs.sc4j.screnhanced.script.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.TopLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a loaded script of a script runtime: its name, its own top-level scope, its {@code module} object and its hooks.
 */
final class ScriptModule {
    private static final @NotNull String EXPORTS_PROPERTY = "exports";

    private final @NotNull String name;
    private final @NotNull TopLevel scope;
    private final @NotNull ScriptableObject moduleObject;

    private final @Nullable Function @NotNull [] hooks;

    /**
     * Creates a {@link ScriptModule} instance without hooks.
     *
     * @param name         the script name
     * @param scope        the module's top-level scope
     * @param moduleObject the module's {@code module} object
     */
    ScriptModule(@NotNull String name, @NotNull TopLevel scope, @NotNull ScriptableObject moduleObject) {
        this.name         = name;
        this.scope        = scope;
        this.moduleObject = moduleObject;

        this.hooks = new Function[ScriptHook.values().length];
    }

    /**
     * @return the script name
     */
    @NotNull String getName() {
        return this.name;
    }

    /**
     * @return the module's top-level scope
     */
    @NotNull TopLevel getScope() {
        return this.scope;
    }

    /**
     * @return the current value of {@code module.exports}
     */
    @Nullable Object getExports() {
        return ScriptableObject.getProperty(this.moduleObject, EXPORTS_PROPERTY);
    }

    /**
     * Finds the function of a hook in the module.
     *
     * @param hook the hook
     * @return the hook function, or {@code null} if the module has none
     */
    @Nullable Function getHook(@NotNull ScriptHook hook) {
        return this.hooks[hook.ordinal()];
    }

    /**
     * Collects the hooks: the top-level functions of the scope with a known hook name.
     *
     * @return the names of the top-level functions with the form of a hook name that no hook has, in definition order
     */
    @NotNull List<@NotNull String> collectHooks() {
        List<String> unknownNames = new ArrayList<>();

        for (Object id : this.scope.getAllIds()) {
            if (!(id instanceof String functionName) || !ScriptHook.isHookName(functionName) ||
                !(this.scope.get(functionName, this.scope) instanceof Function function)) {
                continue;
            }

            ScriptHook hook = ScriptHook.findByFunctionName(functionName);

            if (hook == null) {
                unknownNames.add(functionName);
            } else {
                this.hooks[hook.ordinal()] = function;
            }
        }

        return unknownNames;
    }
}
