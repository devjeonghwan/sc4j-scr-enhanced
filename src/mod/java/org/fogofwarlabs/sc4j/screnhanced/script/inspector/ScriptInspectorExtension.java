package org.fogofwarlabs.sc4j.screnhanced.script.inspector;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.client.console.ConsoleCommand;
import org.fogofwarlabs.sc4j.engine.client.console.ConsoleCommandRegistry;
import org.fogofwarlabs.sc4j.scr.client.extension.ScrClientExtension;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Registers the client command {@code /inspect}, which switches the DevTools console of the client's {@link ScriptInspector} on and off.
 */
public final class ScriptInspectorExtension implements ScrClientExtension {
    private static final int LARGEST_PORT = 0xFFFF;

    /**
     * Switches the console on at a port.
     *
     * @param inspector the inspector
     * @param port      the port
     * @return the reply
     */
    private static @NotNull String start(@NotNull ScriptInspector inspector, int port) {
        try {
            return Exceptions.format("DevTools console on {0}:{1}.", ScriptInspector.HOST, Integer.toString(inspector.start(port)));
        } catch (IOException exception) {
            return Exceptions.format("The DevTools console cannot use port {0}: {1}", Integer.toString(port), exception.getMessage());
        }
    }

    private static @NotNull String stop(@NotNull ScriptInspector inspector) {
        inspector.stop();

        return "DevTools console off.";
    }


    /**
     * Registers the extension's commands in the client's command registry, once, when the client starts, after the client's own commands. The
     * commands run on the client's thread, between game frames, and {@code /help} lists them with the client's own.
     * <p>
     * The extension registers {@code /inspect}.
     *
     * @param registry the client's command registry
     */
    @Override
    public void registerCommands(@NotNull ConsoleCommandRegistry registry) {
        ScriptInspector inspector = ScriptInspector.getShared();
        String defaultPort = Integer.toString(ScriptInspector.DEFAULT_PORT);

        registry.register(ConsoleCommand.of("/inspect", "[on|off|<port>]", "the DevTools console of the map script")
                                        .describe("Chrome or Edge DevTools run script code in the game through a server on 127.0.0.1. Open " +
                                                  "chrome://inspect, add 127.0.0.1 and the port to its targets, and inspect the target.")
                                        .describe("A single-player game runs shared code, which may change the game, or local code. A " +
                                                  "multiplayer game or a replay runs local code only.")
                                        .whenEmpty("switch the console on at port " + defaultPort + ", or off",
                                                   arguments -> inspector.isRunning() ? stop(inspector) : start(inspector, ScriptInspector.DEFAULT_PORT))
                                        .subcommand("on", "", "switch the console on at port " + defaultPort,
                                                    arguments -> start(inspector, ScriptInspector.DEFAULT_PORT))
                                        .subcommand("off", "", "switch the console off", arguments -> stop(inspector))
                                        .otherwise("<port>", "switch the console on at that port", arguments -> {
                                            int port;

                                            try {
                                                port = Integer.parseInt(arguments[0]);
                                            } catch (NumberFormatException exception) {
                                                port = -1;
                                            }

                                            return port > 0 && port <= LARGEST_PORT ? start(inspector, port) : "Use /inspect [on|off|<port>].";
                                        })
                                        .availability("everywhere; the console attaches to the next game with a map script.")
                                        .example("/inspect")
                                        .example("/inspect 9340"));
    }
}
