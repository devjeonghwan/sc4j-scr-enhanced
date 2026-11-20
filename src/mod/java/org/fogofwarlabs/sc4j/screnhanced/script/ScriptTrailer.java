package org.fogofwarlabs.sc4j.screnhanced.script;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a read script trailer ({@link ScriptStorageUtils}): the script package without its resources, read and checked at once, and the
 * resources, read on demand. The trailer of a replay section holds the resources as references without stored bytes.
 */
public final class ScriptTrailer {
    private final @NotNull ScriptPackage scripts;
    private final @NotNull ScriptResources resources;

    /**
     * Creates a {@link ScriptTrailer} instance.
     *
     * @param scripts   the manifest and the scripts, as a package without resources
     * @param resources the resources
     */
    ScriptTrailer(@NotNull ScriptPackage scripts, @NotNull ScriptResources resources) {
        this.scripts   = scripts;
        this.resources = resources;
    }

    /**
     * @return the manifest and the scripts, as a package without resources
     */
    public @NotNull ScriptPackage getScripts() {
        return this.scripts;
    }

    /**
     * @return the resources, not read
     */
    public @NotNull ScriptResources getResources() {
        return this.resources;
    }

    /**
     * Creates the whole script package: the manifest, the scripts and every resource, read and checked.
     *
     * @return the created {@link ScriptPackage} instance
     * @throws IOException if a resource has no stored bytes or cannot be read ({@link ScriptResources#read})
     */
    public @NotNull ScriptPackage createPackage() throws IOException {
        Map<String, byte[]> contents = new LinkedHashMap<>();

        for (ScriptResources.Entry entry : this.resources.getEntries()) {
            contents.put(entry.getName(), this.resources.read(entry));
        }

        return ScriptPackage.create(this.scripts.getManifest(), this.scripts.getScripts(), contents);
    }
}
