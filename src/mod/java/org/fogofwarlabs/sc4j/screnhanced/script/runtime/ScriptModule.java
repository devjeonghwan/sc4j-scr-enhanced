package org.fogofwarlabs.sc4j.screnhanced.script.runtime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.TopLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a loaded script of a script runtime: its name, its own top-level scope, its {@code module} object and the record the prelude
 * collects its hooks from.
 */
final class ScriptModule {
    private static final @NotNull String EXPORTS_PROPERTY = "exports";
    private static final @NotNull String NAME_PROPERTY = "name";
    private static final @NotNull String SCOPE_PROPERTY = "scope";

    private final @NotNull String name;
    private final @NotNull TopLevel scope;
    private final @NotNull ScriptableObject moduleObject;

    private @Nullable Scriptable record;
    private boolean loaded;

    /**
     * Creates a {@link ScriptModule} instance that is loading.
     *
     * @param name         the script name
     * @param scope        the module's top-level scope
     * @param moduleObject the module's {@code module} object
     */
    ScriptModule(@NotNull String name, @NotNull TopLevel scope, @NotNull ScriptableObject moduleObject) {
        this.name         = name;
        this.scope        = scope;
        this.moduleObject = moduleObject;

        this.record = null;
        this.loaded = false;
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
     * @return {@code true} if the module's script has run to its end, {@code false} while it loads
     */
    boolean isLoaded() {
        return this.loaded;
    }

    /**
     * Marks the module loaded: its script has run to its end.
     */
    void setLoaded() {
        this.loaded = true;
    }

    /**
     * Returns the module's record for the prelude, created once: an object with the module's {@code name} and {@code scope}, to which the
     * prelude adds the hooks it collected.
     *
     * @param context the current context
     * @param global  the global scope
     * @return the record
     */
    @NotNull Scriptable getRecord(@NotNull Context context, @NotNull Scriptable global) {
        Scriptable moduleRecord = this.record;

        if (moduleRecord == null) {
            moduleRecord = context.newObject(global);

            ScriptableObject.putProperty(moduleRecord, NAME_PROPERTY, this.name);
            ScriptableObject.putProperty(moduleRecord, SCOPE_PROPERTY, this.scope);

            this.record = moduleRecord;
        }

        return moduleRecord;
    }

    /**
     * Lists the top-level functions of the scope with the form of a hook name that no hook has.
     *
     * @return the names, in definition order
     */
    @NotNull List<@NotNull String> findUnknownHookNames() {
        List<String> unknownNames = new ArrayList<>();

        for (Object id : this.scope.getAllIds()) {
            if (id instanceof String functionName && ScriptHook.isHookName(functionName) && ScriptHook.findByFunctionName(functionName) == null &&
                this.scope.get(functionName, this.scope) instanceof Function) {
                unknownNames.add(functionName);
            }
        }

        return unknownNames;
    }
}
