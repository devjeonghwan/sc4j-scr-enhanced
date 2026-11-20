package org.fogofwarlabs.sc4j.screnhanced.script;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents the resource file of a script package ({@value ScriptStorageUtils#RESOURCE_FILE_NAME}, read by
 * {@link ScriptStorageUtils#readResources}) without copying its resources: the bytes of the file and the place of each resource in them, by name
 * in the stored order.
 */
public final class ScriptResources {
    /**
     * The resources of a map without a resource file: no bytes and no resource.
     */
    public static final @NotNull ScriptResources EMPTY = new ScriptResources(new byte[0], Map.of());

    /**
     * One resource: its name and the range of its content in the bytes of the resource file.
     */
    public static final class Entry {
        private final @NotNull String name;
        private final int offset;
        private final int size;

        /**
         * Creates an {@link Entry} instance.
         *
         * @param name   the resource name
         * @param offset the first byte of the content
         * @param size   the size of the content
         */
        Entry(@NotNull String name, int offset, int size) {
            this.name   = name;
            this.offset = offset;
            this.size   = size;
        }

        /**
         * @return the resource name
         */
        public @NotNull String getName() {
            return this.name;
        }

        /**
         * @return the first byte of the content in the bytes of the resource file
         */
        public int getOffset() {
            return this.offset;
        }

        /**
         * @return the size of the content
         */
        public int getSize() {
            return this.size;
        }
    }


    private final byte @NotNull [] data;
    private final @NotNull Map<String, Entry> entries;

    /**
     * Creates a {@link ScriptResources} instance.
     *
     * @param data    the bytes of the resource file, not copied
     * @param entries the resources by name, in the stored order; the map is copied
     */
    ScriptResources(byte @NotNull [] data, @NotNull Map<String, Entry> entries) {
        this.data    = data;
        this.entries = Collections.unmodifiableMap(new LinkedHashMap<>(entries));
    }

    /**
     * @return the bytes of the resource file, shared and not to be changed
     */
    public byte @NotNull [] getData() {
        return this.data;
    }

    /**
     * @return the unmodifiable resources in the stored order
     */
    public @NotNull Collection<Entry> getEntries() {
        return this.entries.values();
    }

    /**
     * Finds a resource by its name.
     *
     * @param name the resource name, relative to the resource folder with {@code /} separators
     * @return the resource, or {@code null} if the file has no resource of the name
     */
    public @Nullable Entry findEntry(@NotNull String name) {
        return this.entries.get(name);
    }
}
