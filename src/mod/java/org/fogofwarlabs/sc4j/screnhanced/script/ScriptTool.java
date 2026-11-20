package org.fogofwarlabs.sc4j.screnhanced.script;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Packs a script folder into a map and unpacks it ({@link ScriptStorageUtils}, {@link ScriptPackage}).
 * <p>
 * Commands: {@code pack <map> --script <folder> -o <output>} writes the map with the package of the script folder to the output file;
 * {@code unpack <map> -o <folder>} writes the package of the map to the folder, which must be absent or empty.
 */
public final class ScriptTool {
    /**
     * The command that packs a script folder into a map.
     */
    public static final @NotNull String PACK_COMMAND = "pack";
    /**
     * The command that unpacks the script package of a map into a folder.
     */
    public static final @NotNull String UNPACK_COMMAND = "unpack";
    /**
     * The option that names the script folder of {@value #PACK_COMMAND}.
     */
    public static final @NotNull String SCRIPT_OPTION = "--script";
    /**
     * The option that names the output file of {@value #PACK_COMMAND} and the output folder of {@value #UNPACK_COMMAND}.
     */
    public static final @NotNull String OUTPUT_OPTION = "-o";

    private static final @NotNull String USAGE = "Usage: pack <map> --script <folder> -o <output> | unpack <map> -o <folder>";
    private static final int COMMAND_ARGUMENT = 0;
    private static final int MAP_ARGUMENT = 1;
    private static final int FIRST_OPTION_ARGUMENT = 2;
    private static final int SUCCESS_EXIT = 0;
    private static final int FAILURE_EXIT = 1;

    /**
     * Runs one command and exits with {@code 0}, or prints why it cannot and exits with {@code 1}.
     *
     * @param arguments the command and its arguments
     */
    public static void main(@NotNull String @NotNull [] arguments) {
        System.exit(run(arguments, System.out, System.err));
    }

    /**
     * Runs one command.
     *
     * @param arguments the command and its arguments
     * @param output    the stream of the result line
     * @param errors    the stream of the failure message
     * @return {@code 0} if the command ran, {@code 1} otherwise
     */
    public static int run(@NotNull String @NotNull [] arguments, @NotNull PrintStream output, @NotNull PrintStream errors) {
        try {
            if (arguments.length <= MAP_ARGUMENT) {
                throw Exceptions.newIllegalArgumentException("A command and a map are needed.");
            }

            Path map = Path.of(arguments[MAP_ARGUMENT]);

            if (!Files.isRegularFile(map)) {
                throw Exceptions.newIOException("The map {0} is not a file.", map);
            }

            switch (arguments[COMMAND_ARGUMENT]) {
                case PACK_COMMAND -> {
                    Map<String, String> options = readOptions(arguments, List.of(SCRIPT_OPTION, OUTPUT_OPTION));
                    Path outputFile = Path.of(options.get(OUTPUT_OPTION));
                    ScriptPackage scriptPackage = ScriptPackage.readFolder(Path.of(options.get(SCRIPT_OPTION)));

                    Files.write(outputFile, ScriptStorageUtils.writePackage(Files.readAllBytes(map), map.toString(), scriptPackage));
                    output.println(Exceptions.format("Packed {0} scripts and {1} resources into {2}.", scriptPackage.getScripts().size(),
                                                     scriptPackage.getResources().size(), outputFile));
                }
                case UNPACK_COMMAND -> {
                    Path outputFolder = Path.of(readOptions(arguments, List.of(OUTPUT_OPTION)).get(OUTPUT_OPTION));
                    ScriptPackage scriptPackage = ScriptStorageUtils.readPackage(Files.readAllBytes(map), map.toString());

                    if (scriptPackage == null) {
                        throw Exceptions.newIOException("The map {0} has no script section.", map);
                    }

                    scriptPackage.writeFolder(outputFolder);
                    output.println(Exceptions.format("Unpacked {0} scripts and {1} resources into {2}.", scriptPackage.getScripts().size(),
                                                     scriptPackage.getResources().size(), outputFolder));
                }
                default -> throw Exceptions.newIllegalArgumentException("Unknown command {0}: expected {1} or {2}.", arguments[COMMAND_ARGUMENT],
                                                                        PACK_COMMAND, UNPACK_COMMAND);
            }

            return SUCCESS_EXIT;
        } catch (IllegalArgumentException exception) {
            errors.println(exception.getMessage());
            errors.println(USAGE);

            return FAILURE_EXIT;
        } catch (IOException exception) {
            errors.println(exception.getMessage());

            return FAILURE_EXIT;
        }
    }

    /**
     * Reads the options after the map: each option once, followed by its value.
     *
     * @param arguments the command and its arguments
     * @param options   the options of the command, each one needed
     * @return the option values by option
     * @throws IllegalArgumentException if an option is unknown, repeated, without value or missing
     */
    private static @NotNull Map<String, String> readOptions(@NotNull String @NotNull [] arguments, @NotNull List<String> options) {
        Map<String, String> values = new LinkedHashMap<>();

        for (int index = FIRST_OPTION_ARGUMENT; index < arguments.length; index += 2) {
            String option = arguments[index];

            if (!options.contains(option) || index + 1 == arguments.length) {
                throw Exceptions.newIllegalArgumentException("Unknown option or missing value: {0}", option);
            }

            if (values.putIfAbsent(option, arguments[index + 1]) != null) {
                throw Exceptions.newIllegalArgumentException("Repeated option: {0}", option);
            }
        }

        for (String option : options) {
            if (!values.containsKey(option)) {
                throw Exceptions.newIllegalArgumentException("The command {0} needs {1}.", arguments[COMMAND_ARGUMENT], option);
            }
        }

        return values;
    }

    private ScriptTool() {
    }
}
