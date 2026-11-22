package org.fogofwarlabs.sc4j.screnhanced.script.inspector;

import org.fogofwarlabs.sc4j.engine.client.console.ConsoleCommandRegistry;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 * Tests the {@code /inspect} command of {@link ScriptInspectorExtension}: it switches the client's inspector on at a port and off, refuses a
 * port that is in use or no port, and has its help.
 */
final class ScriptInspectorExtensionTest {
    private static final int PAGE_LINES = 10;

    private static @NotNull ConsoleCommandRegistry createRegistry() {
        ConsoleCommandRegistry registry = new ConsoleCommandRegistry(PAGE_LINES);

        new ScriptInspectorExtension().registerCommands(registry);

        return registry;
    }

    private static @NotNull ServerSocket bindFreePort() throws IOException {
        return new ServerSocket(0, 1, InetAddress.getByName(ScriptInspector.HOST));
    }

    @Test
    void inspect_switches_the_client_inspector_on_at_a_port_and_off() throws IOException {
        ConsoleCommandRegistry registry = createRegistry();
        ScriptInspector inspector = ScriptInspector.getShared();
        int port;

        try (ServerSocket free = bindFreePort()) {
            port = free.getLocalPort();
        }

        try {
            Assertions.assertEquals("DevTools console on 127.0.0.1:" + port + ".", registry.run("/inspect " + port));
            Assertions.assertTrue(inspector.isRunning());
            Assertions.assertEquals(port, inspector.getPort());
            Assertions.assertEquals("DevTools console off.", registry.run("/inspect"));
            Assertions.assertFalse(inspector.isRunning());
            Assertions.assertEquals("Use /inspect [on|off|<port>].", registry.run("/inspect 70000"));
            Assertions.assertEquals("Use /inspect [on|off|<port>].", registry.run("/inspect next"));
            Assertions.assertEquals("DevTools console off.", registry.run("/inspect off"));
            Assertions.assertTrue(String.join("\n", registry.getCommandHelpLines("/inspect")).contains("single-player"));
        } finally {
            inspector.stop();
        }
    }

    @Test
    void inspect_refuses_a_port_in_use() throws IOException {
        ConsoleCommandRegistry registry = createRegistry();

        try (ServerSocket used = bindFreePort()) {
            Assertions.assertTrue(registry.run("/inspect " + used.getLocalPort()).startsWith("The DevTools console cannot use port " + used.getLocalPort()));
            Assertions.assertFalse(ScriptInspector.getShared().isRunning());
        } finally {
            ScriptInspector.getShared().stop();
        }
    }
}
