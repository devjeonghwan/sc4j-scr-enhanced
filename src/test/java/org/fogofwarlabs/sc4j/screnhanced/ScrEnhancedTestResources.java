package org.fogofwarlabs.sc4j.screnhanced;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Names the build outputs and the overlay of SC:R Enhanced that its tests use.
 */
public final class ScrEnhancedTestResources {
    /**
     * The system property, set by the build for every test task, that names the built mod jar {@code scr-enhanced.jar}.
     */
    public static final @NotNull String MOD_JAR_PROPERTY = "sc4j.screnhanced.jar";

    /**
     * The system property, set by the build for every test task, that names the built SC:R mod jar {@code scr.jar}.
     */
    public static final @NotNull String SCR_MOD_JAR_PROPERTY = "sc4j.scr.jar";

    /**
     * The system property, set by the build for every test task, that names the overlay folder {@code overlay}.
     */
    public static final @NotNull String OVERLAY_PROPERTY = "sc4j.screnhanced.overlay";

    /**
     * The system property, set by the build for every test task, that names the folder of the mod's own resources {@code src/mod/resources}.
     */
    public static final @NotNull String MOD_RESOURCES_PROPERTY = "sc4j.screnhanced.resources";

    /**
     * @return the built mod jar of SC:R Enhanced
     * @throws IllegalStateException if the build did not name the jar
     */
    public static @NotNull Path getModJar() {
        return getPath(MOD_JAR_PROPERTY);
    }

    /**
     * @return the built mod jar of the SC:R mod
     * @throws IllegalStateException if the build did not name the jar
     */
    public static @NotNull Path getScrModJar() {
        return getPath(SCR_MOD_JAR_PROPERTY);
    }

    /**
     * @return the overlay folder of SC:R Enhanced
     * @throws IllegalStateException if the build did not name the folder
     */
    public static @NotNull Path getOverlay() {
        return getPath(OVERLAY_PROPERTY);
    }

    /**
     * @return the folder of the mod's own resources
     * @throws IllegalStateException if the build did not name the folder
     */
    public static @NotNull Path getModResources() {
        return getPath(MOD_RESOURCES_PROPERTY);
    }

    private static @NotNull Path getPath(@NotNull String property) {
        String path = System.getProperty(property, "");

        if (path.isBlank()) {
            throw Exceptions.newIllegalStateException("The SC:R Enhanced tests need the system property {0} of the build.", property);
        }

        return Path.of(path);
    }

    private ScrEnhancedTestResources() {
    }
}
