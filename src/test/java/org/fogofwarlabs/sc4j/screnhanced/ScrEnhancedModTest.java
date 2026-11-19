package org.fogofwarlabs.sc4j.screnhanced;

import org.fogofwarlabs.sc4j.base.log.Logging;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentity;
import org.fogofwarlabs.sc4j.manifest.ManifestException;
import org.fogofwarlabs.sc4j.manifest.ModManifestReader;
import org.fogofwarlabs.sc4j.manifest.install.InstallLayout;
import org.fogofwarlabs.sc4j.manifest.settings.value.SettingValuesFile;
import org.fogofwarlabs.sc4j.runtime.cli.ExitCode;
import org.fogofwarlabs.sc4j.runtime.session.RuntimeSession;
import org.fogofwarlabs.sc4j.scr.core.test.ExternalInput;
import org.fogofwarlabs.sc4j.scr.core.test.RequiresInstallation;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Tests SC:R Enhanced with the SC:R installation: the runtime loads its jar headless through the whole mod sequence (manifest, settings, the
 * installation check, registration) to the mod identity it logs, which is the patch's own and not the SC:R mod's; both mods use the one
 * saved StarCraft: Remastered folder of the install.
 */
@RequiresInstallation
final class ScrEnhancedModTest {
    private static final @NotNull String ENHANCED_ID = "scr-enhanced";
    private static final @NotNull String SCR_ID = "scr";
    private static final @NotNull String SHARED_GAME_FOLDER = "starcraft-remastered";

    /**
     * Runs the runtime headless with a mod jar of the install. The SC:R mod's client needs a window, so the run ends after the mod identity
     * with the runtime's refusal of a headless run.
     *
     * @return the runtime's log of the run
     */
    private static @NotNull String load(@NotNull InstallLayout install, @NotNull String modId, @NotNull Path jar) throws IOException,
                                                                                                                         ManifestException {
        Path installed = install.getModsDirectory().resolve(modId + ".jar");

        Assertions.assertEquals(modId, ModManifestReader.read(jar).getManifest().getId());
        Files.copy(jar, installed);

        ExitCode exitCode = RuntimeSession.run(new String[]{"--install", install.getRoot().toString(), "--mod", installed.toString(), "--headless"});
        String log = Files.readString(install.getLogDirectory(modId).resolve(Logging.FILE_NAME), StandardCharsets.UTF_8);

        Assertions.assertEquals(ExitCode.MOD_FAILURE, exitCode, log);
        Assertions.assertTrue(log.contains("cannot run headless"), log);

        return log;
    }

    private static @NotNull InstallLayout createInstall(@NotNull Path root) throws IOException {
        InstallLayout install = InstallLayout.of(root);

        Files.createDirectories(install.getModsDirectory());
        new SettingValuesFile(install.getSharedSettingsFile()).write(Map.of(SHARED_GAME_FOLDER, ExternalInput.INSTALLATION.getPath().toString()));

        return install;
    }


    @Test
    void the_runtime_loads_the_patch_headless_with_its_own_identity(@TempDir @NotNull Path directory) throws IOException, ManifestException {
        InstallLayout install = createInstall(directory);
        String enhancedLog = load(install, ENHANCED_ID, ScrEnhancedTestResources.getModJar());
        String scrLog = load(install, SCR_ID, ScrEnhancedTestResources.getScrModJar());
        ModIdentity enhanced = ScrEnhancedJarTest.createIdentity(ScrEnhancedTestResources.getModJar());
        ModIdentity scr = ScrEnhancedJarTest.createIdentity(ScrEnhancedTestResources.getScrModJar());

        String gameLine = "SC:R INSTALLATION: " + ExternalInput.INSTALLATION.getPath() + " (build";

        Assertions.assertTrue(enhancedLog.contains(gameLine), enhancedLog);
        Assertions.assertTrue(scrLog.contains(gameLine), scrLog);
        Assertions.assertTrue(enhancedLog.contains("IDENTITY: " + enhanced.describe()), enhancedLog);
        Assertions.assertTrue(scrLog.contains("IDENTITY: " + scr.describe()), scrLog);
        Assertions.assertNotEquals(scr.describe(), enhanced.describe());
        Assertions.assertTrue(Files.isDirectory(install.getStorageDirectory(ENHANCED_ID)));
    }
}
