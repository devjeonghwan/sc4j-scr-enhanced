package org.fogofwarlabs.sc4j.screnhanced.script;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.scr.data.extension.ScrDataExtensionUtils;
import org.fogofwarlabs.sc4j.scr.data.format.mpq.MpqArchive;
import org.fogofwarlabs.sc4j.screnhanced.graphics.ImageReplacements;
import org.fogofwarlabs.sc4j.screnhanced.script.ScriptFinding.Severity;
import org.fogofwarlabs.sc4j.screnhanced.script.runtime.ScriptHook;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.ArrayLiteral;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.BreakStatement;
import org.mozilla.javascript.ast.CatchClause;
import org.mozilla.javascript.ast.ContinueStatement;
import org.mozilla.javascript.ast.ForInLoop;
import org.mozilla.javascript.ast.ForLoop;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.Loop;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.ObjectProperty;
import org.mozilla.javascript.ast.PropertyGet;
import org.mozilla.javascript.ast.VariableDeclaration;
import org.mozilla.javascript.ast.VariableInitializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Checks a script folder before it is packed: the files {@link ScriptPackage#readFolder} reads, with the rules of the script runtime.
 * <p>
 * The manifest: present and valid, the main script in the folder, the script API {@value ScriptProjectUtils#API_VERSION} (a warning
 * otherwise); the names of the scripts and resources (errors of the folder {@value #FOLDER_FILE}).
 * <p>
 * Each script, read as UTF-8 and compiled by Rhino with the settings of the runtime: the syntax errors, the first of each line, with a note
 * when the line holds syntax that Rhino 1.8.1 lacks ({@code class}, {@code async} and {@code await}, spread {@code ...},
 * {@code for (const ...)}, {@code import} and {@code export}); after a successful compile, references to the globals the runtime does not
 * have ({@code eval}, {@code Date}, {@code java}, {@code Packages} and the other Java and timer globals) and calls of the {@code Function}
 * constructor (errors) unless the script declares the name, top-level functions with the form of a hook name that no hook has (warnings, as
 * the runtime warns), a {@code const} in a loop, which keeps its first value in Rhino 1.8.1 (a warning), and functions in the body of a
 * {@code for (let ...)} loop that use its variables, which all iterations share in Rhino 1.8.1 (a warning).
 * <p>
 * Each resource named {@code *.grp} or {@code *.anim}, read as the runtime reads a GRP or an anim. The sizes: the script section against
 * the largest scenario of a map (an error) and of a StarCraft: Remastered map (a warning), the resource file against the largest file of a
 * map archive (an error).
 */
public final class ScriptCheckUtils {
    /**
     * The file of a finding of the whole script folder.
     */
    public static final @NotNull String FOLDER_FILE = ".";

    private static final @NotNull String RESOURCE_FILE_PREFIX = ScriptPackage.RESOURCE_FOLDER + "/";
    private static final @NotNull String GRP_EXTENSION = ".grp";
    private static final @NotNull String ANIM_EXTENSION = ".anim";
    private static final int FIRST_LINE = 1;
    private static final @NotNull String FUNCTION_CONSTRUCTOR_NAME = "Function";
    /**
     * The globals a script does not have: removed from Rhino's standard objects, or Java, XML and timer globals that the runtime never defines.
     */
    private static final @NotNull Set<String> UNAVAILABLE_GLOBALS = Set.of(
            "eval", "Date", "Script", "Continuation", "XML", "XMLList", "Namespace", "QName", "isXMLName",
            "java", "javax", "Packages", "JavaAdapter", "JavaImporter", "importClass", "importPackage",
            "setTimeout", "setInterval", "clearTimeout", "clearInterval"
    );
    /**
     * The notes of the syntax that Rhino 1.8.1 lacks, for a line with a syntax error that holds it.
     */
    private static final @NotNull List<SyntaxNote> SYNTAX_NOTES = List.of(
            new SyntaxNote("\\bclass\\b", "Rhino 1.8.1 has no class syntax; use functions and prototypes"),
            new SyntaxNote("\\basync\\b|\\bawait\\b", "Rhino 1.8.1 has no async and await"),
            new SyntaxNote("\\bfor\\s*\\(\\s*const\\b", "Rhino 1.8.1 has no for (const ...); use let"),
            new SyntaxNote("\\.\\.\\.", "Rhino 1.8.1 has no spread ...; only rest parameters"),
            new SyntaxNote("^\\s*(import|export)\\b", "Rhino 1.8.1 has no import and export; use require and module.exports")
    );

    /**
     * Syntax that Rhino 1.8.1 lacks: the pattern of a line that holds it and the note.
     */
    private static final class SyntaxNote {
        private final @NotNull Pattern pattern;
        private final @NotNull String note;

        private SyntaxNote(@NotNull String pattern, @NotNull String note) {
            this.pattern = Pattern.compile(pattern);
            this.note    = note;
        }
    }

    /**
     * Collects the syntax errors of one script, the first of each line.
     */
    private static final class SyntaxErrors implements ErrorReporter {
        private final @NotNull Map<Integer, ScriptFinding> findings;
        private final @NotNull String scriptName;

        private SyntaxErrors(@NotNull String scriptName) {
            this.findings   = new TreeMap<>();
            this.scriptName = scriptName;
        }

        @Override
        public void warning(@NotNull String message, @Nullable String sourceName, int line, @Nullable String lineSource, int lineOffset) {
        }

        @Override
        public void error(@NotNull String message, @Nullable String sourceName, int line, @Nullable String lineSource, int lineOffset) {
            this.add(message, line, lineSource);
        }

        @Override
        public @NotNull EvaluatorException runtimeError(@NotNull String message, @Nullable String sourceName, int line,
                                                        @Nullable String lineSource, int lineOffset) {
            return new EvaluatorException(message, sourceName, line, lineSource, lineOffset);
        }

        private void add(@NotNull String message, int line, @Nullable String lineSource) {
            int findingLine = Math.max(line, FIRST_LINE);

            if (this.findings.containsKey(findingLine)) {
                return;
            }

            String text = message;

            if (lineSource != null) {
                for (SyntaxNote note : SYNTAX_NOTES) {
                    if (note.pattern.matcher(lineSource).find()) {
                        text = message + " (" + note.note + ")";
                        break;
                    }
                }
            }

            this.findings.put(findingLine, new ScriptFinding(this.scriptName, findingLine, Severity.ERROR, text));
        }
    }

    /**
     * Checks a script folder.
     *
     * @param folder the script folder
     * @return the findings: the manifest and the folder first, then the scripts and the resources by name, each by line
     * @throws IOException if the path is not a folder or a file of it cannot be read
     */
    public static @NotNull List<@NotNull ScriptFinding> check(@NotNull Path folder) throws IOException {
        if (!Files.isDirectory(folder)) {
            throw Exceptions.newIOException("The script folder {0} is not a folder.", folder);
        }

        List<ScriptFinding> findings = new ArrayList<>();
        Map<String, byte[]> scripts = new TreeMap<>();
        Map<String, byte[]> resources = new TreeMap<>();

        ScriptPackage.readFolderFiles(folder, scripts, resources);

        ScriptManifest manifest = checkManifest(folder, scripts, findings);
        boolean namesValid = true;

        try {
            ScriptPackage.checkNames(scripts.keySet(), resources.keySet());
        } catch (IOException exception) {
            findings.add(new ScriptFinding(FOLDER_FILE, ScriptFinding.NO_LINE, Severity.ERROR, exception.getMessage()));
            namesValid = false;
        }

        if (manifest != null && namesValid && scripts.containsKey(manifest.getMain())) {
            checkSizes(ScriptPackage.create(manifest, scripts, resources), findings);
        }

        checkScripts(scripts, findings);
        checkResources(resources, findings);

        return findings;
    }

    /**
     * Counts the errors of findings.
     *
     * @param findings the findings
     * @return the number of findings with {@link Severity#ERROR}
     */
    public static int countErrors(@NotNull List<@NotNull ScriptFinding> findings) {
        int count = 0;

        for (ScriptFinding finding : findings) {
            if (finding.getSeverity() == Severity.ERROR) {
                count++;
            }
        }

        return count;
    }

    /**
     * Checks the manifest of a script folder.
     *
     * @return the manifest, or {@code null} if the folder has none or it is not valid
     */
    private static @Nullable ScriptManifest checkManifest(@NotNull Path folder, @NotNull Map<String, byte[]> scripts,
                                                          @NotNull List<ScriptFinding> findings) throws IOException {
        Path manifestFile = folder.resolve(ScriptManifest.FILE_NAME);

        if (!Files.isRegularFile(manifestFile)) {
            findings.add(new ScriptFinding(ScriptManifest.FILE_NAME, ScriptFinding.NO_LINE, Severity.ERROR,
                                           Exceptions.format("The script folder has no {0}.", ScriptManifest.FILE_NAME)));

            return null;
        }

        ScriptManifest manifest;

        try {
            manifest = ScriptManifest.parse(Files.readAllBytes(manifestFile));
        } catch (IOException exception) {
            findings.add(new ScriptFinding(ScriptManifest.FILE_NAME, ScriptFinding.NO_LINE, Severity.ERROR, exception.getMessage()));

            return null;
        }

        if (!scripts.containsKey(manifest.getMain())) {
            findings.add(new ScriptFinding(ScriptManifest.FILE_NAME, ScriptFinding.NO_LINE, Severity.ERROR,
                                           Exceptions.format("The main script {0} is not in the script folder.", manifest.getMain())));
        }

        if (!manifest.getApi().equals(ScriptProjectUtils.API_VERSION)) {
            findings.add(new ScriptFinding(ScriptManifest.FILE_NAME, ScriptFinding.NO_LINE, Severity.WARNING,
                                           Exceptions.format("The manifest names the script API {0}; SC:R Enhanced has the script API {1}.",
                                                             manifest.getApi(), ScriptProjectUtils.API_VERSION)));
        }

        return manifest;
    }

    private static void checkSizes(@NotNull ScriptPackage scriptPackage, @NotNull List<ScriptFinding> findings) {
        long sectionSize = ScriptStorageUtils.getSectionSize(scriptPackage);
        long resourceFileSize = ScriptStorageUtils.getResourceFileSize(scriptPackage);

        int maximumScenarioSize = ScrDataExtensionUtils.getMaximumScenarioSize();

        if (sectionSize > maximumScenarioSize) {
            findings.add(new ScriptFinding(FOLDER_FILE, ScriptFinding.NO_LINE, Severity.ERROR,
                                           Exceptions.format("The manifest and the scripts take {0} bytes of the scenario, more than the {1} bytes " +
                                                             "a map may hold.", sectionSize, maximumScenarioSize)));
        } else if (sectionSize > MpqArchive.NATIVE_MAXIMUM_SCENARIO_SIZE) {
            findings.add(new ScriptFinding(FOLDER_FILE, ScriptFinding.NO_LINE, Severity.WARNING,
                                           Exceptions.format("The manifest and the scripts take {0} bytes of the scenario, more than the {1} bytes " +
                                                             "StarCraft: Remastered reads.", sectionSize, MpqArchive.NATIVE_MAXIMUM_SCENARIO_SIZE)));
        }

        if (resourceFileSize > MpqArchive.MAXIMUM_FILE_SIZE) {
            findings.add(new ScriptFinding(FOLDER_FILE, ScriptFinding.NO_LINE, Severity.ERROR,
                                           Exceptions.format("The resources take {0} bytes, more than the {1} bytes of one map archive file.",
                                                             resourceFileSize, MpqArchive.MAXIMUM_FILE_SIZE)));
        }
    }

    /**
     * Checks every script with one Rhino context of the runtime's settings.
     */
    private static void checkScripts(@NotNull Map<String, byte[]> scripts, @NotNull List<ScriptFinding> findings) {
        try (Context context = new ContextFactory().enterContext()) {
            context.setInterpretedMode(true);
            context.setLanguageVersion(Context.VERSION_ECMASCRIPT);

            for (Map.Entry<String, byte[]> script : scripts.entrySet()) {
                List<ScriptFinding> scriptFindings = new ArrayList<>();

                checkScript(context, script.getKey(), new String(script.getValue(), StandardCharsets.UTF_8), scriptFindings);
                scriptFindings.sort(Comparator.comparingInt(ScriptFinding::getLine));
                findings.addAll(scriptFindings);
            }
        }
    }

    private static void checkScript(@NotNull Context context, @NotNull String name, @NotNull String source,
                                    @NotNull List<ScriptFinding> findings) {
        CompilerEnvirons environment = new CompilerEnvirons();
        SyntaxErrors errors = new SyntaxErrors(name);
        AstRoot root = null;

        environment.initFromContext(context);
        environment.setRecoverFromErrors(true);

        try {
            root = new Parser(environment, errors).parse(source, name, FIRST_LINE);
        } catch (EvaluatorException exception) {
            // The parser ends with a summary of the errors it reported, which adds nothing to them.
            if (errors.findings.isEmpty()) {
                errors.add(exception.details(), exception.lineNumber(), exception.lineSource());
            }
        }

        if (errors.findings.isEmpty()) {
            try {
                context.compileString(source, name, FIRST_LINE, null);
            } catch (EvaluatorException exception) {
                errors.add(exception.details(), exception.lineNumber(), exception.lineSource());
            }
        }

        findings.addAll(errors.findings.values());

        if (root != null && errors.findings.isEmpty()) {
            checkTree(name, root, findings);
        }
    }

    /**
     * Checks the syntax tree of a script that compiles.
     */
    private static void checkTree(@NotNull String scriptName, @NotNull AstRoot root, @NotNull List<ScriptFinding> findings) {
        Set<String> declaredNames = collectDeclaredNames(root);

        root.visitAll(node -> {
            if (node instanceof Name name && UNAVAILABLE_GLOBALS.contains(name.getIdentifier()) && isReference(name) &&
                !declaredNames.contains(name.getIdentifier())) {
                findings.add(new ScriptFinding(scriptName, name.getLineno(), Severity.ERROR,
                                               Exceptions.format("{0} is not available to scripts.", name.getIdentifier())));
            } else if (node instanceof FunctionCall call && call.getTarget() instanceof Name target &&
                       target.getIdentifier().equals(FUNCTION_CONSTRUCTOR_NAME) && !declaredNames.contains(FUNCTION_CONSTRUCTOR_NAME)) {
                findings.add(new ScriptFinding(scriptName, call.getLineno(), Severity.ERROR,
                                               "The Function constructor throws: scripts cannot create functions from strings."));
            } else if (node instanceof FunctionNode function) {
                checkHookFunction(scriptName, function, findings);
            } else if (node instanceof VariableDeclaration declaration) {
                checkDeclaration(scriptName, declaration, findings);
            } else if (node instanceof Loop loop) {
                checkLetLoop(scriptName, loop, findings);
            }

            return true;
        });
    }

    /**
     * Collects the names a script declares: variables, functions, parameters and catch variables.
     */
    private static @NotNull Set<String> collectDeclaredNames(@NotNull AstNode root) {
        Set<String> names = new HashSet<>();

        root.visit(node -> {
            if (node instanceof Name name && !isReference(name) && !(node.getParent() instanceof PropertyGet) &&
                !(node.getParent() instanceof ObjectProperty) && !(node.getParent() instanceof BreakStatement) &&
                !(node.getParent() instanceof ContinueStatement)) {
                names.add(name.getIdentifier());
            }

            return true;
        });

        return names;
    }

    /**
     * Checks whether a name is a reference to a variable: not a property name, a declaration or a label.
     */
    private static boolean isReference(@NotNull Name name) {
        AstNode parent = name.getParent();

        return switch (parent) {
            case PropertyGet get -> get.getProperty() != name;
            case ObjectProperty property -> property.getLeft() != name;
            case FunctionNode function -> function.getFunctionName() != name && !function.getParams().contains(name);
            case VariableInitializer initializer -> initializer.getTarget() != name;
            case CatchClause clause -> clause.getVarName() != name;
            case BreakStatement ignored -> false;
            case ContinueStatement ignored -> false;
            case null, default -> true;
        };
    }

    /**
     * Warns of a top-level function with the form of a hook name that no hook has, as the runtime warns when the script loads.
     */
    private static void checkHookFunction(@NotNull String scriptName, @NotNull FunctionNode function, @NotNull List<ScriptFinding> findings) {
        Name functionName = function.getFunctionName();

        if (function.getFunctionType() == FunctionNode.FUNCTION_STATEMENT && functionName != null && function.getEnclosingFunction() == null) {
            checkHookName(scriptName, functionName.getIdentifier(), function.getLineno(), findings);
        }
    }

    private static void checkHookName(@NotNull String scriptName, @NotNull String functionName, int line, @NotNull List<ScriptFinding> findings) {
        if (ScriptHook.isHookName(functionName) && ScriptHook.findByFunctionName(functionName) == null) {
            findings.add(new ScriptFinding(scriptName, line, Severity.WARNING,
                                           Exceptions.format("{0} is not a known hook and is not called.", functionName)));
        }
    }

    /**
     * Checks a variable declaration: a top-level variable that holds a function with the form of a hook name that no hook has, and a
     * {@code const} in a loop.
     */
    private static void checkDeclaration(@NotNull String scriptName, @NotNull VariableDeclaration declaration, @NotNull List<ScriptFinding> findings) {
        boolean topLevel = declaration.getEnclosingFunction() == null && (declaration.isVar() || declaration.getParent() instanceof AstRoot);

        for (VariableInitializer variable : declaration.getVariables()) {
            if (topLevel && variable.getTarget() instanceof Name name && variable.getInitializer() instanceof FunctionNode) {
                checkHookName(scriptName, name.getIdentifier(), variable.getLineno(), findings);
            }
        }

        if (declaration.isConst() && findLoopOfBody(declaration) != null) {
            findings.add(new ScriptFinding(scriptName, declaration.getLineno(), Severity.WARNING,
                                           Exceptions.format("The const {0} in a loop keeps its first value in Rhino 1.8.1; use let.",
                                                             String.join(", ", collectTargetNames(declaration)))));
        }
    }

    /**
     * Finds the loop whose body holds a node, inside the node's function.
     *
     * @return the loop, or {@code null} if the node is in no loop body of its function
     */
    private static @Nullable Loop findLoopOfBody(@NotNull AstNode node) {
        AstNode child = node;

        for (AstNode parent = node.getParent(); parent != null && !(parent instanceof FunctionNode); parent = parent.getParent()) {
            if (parent instanceof Loop loop && loop.getBody() == child) {
                return loop;
            }

            child = parent;
        }

        return null;
    }

    /**
     * Warns of the functions in the body of a {@code for (let ...)} loop that use its variables: all iterations share one binding in
     * Rhino 1.8.1, so every such function sees the last value.
     */
    private static void checkLetLoop(@NotNull String scriptName, @NotNull Loop loop, @NotNull List<ScriptFinding> findings) {
        AstNode head = switch (loop) {
            case ForLoop forLoop -> forLoop.getInitializer();
            case ForInLoop forInLoop -> forInLoop.getIterator();
            default -> null;
        };

        if (!(head instanceof VariableDeclaration declaration) || !declaration.isLet() || loop.getBody() == null) {
            return;
        }

        Set<String> loopNames = collectTargetNames(declaration);

        loop.getBody().visit(node -> {
            if (!(node instanceof FunctionNode function)) {
                return true;
            }

            Set<String> functionNames = collectDeclaredNames(function);
            Set<String> usedNames = new LinkedHashSet<>();

            function.visit(inner -> {
                if (inner instanceof Name name && loopNames.contains(name.getIdentifier()) && isReference(name) &&
                    !functionNames.contains(name.getIdentifier())) {
                    usedNames.add(name.getIdentifier());
                }

                return true;
            });

            if (!usedNames.isEmpty()) {
                findings.add(new ScriptFinding(scriptName, function.getLineno(), Severity.WARNING,
                                               Exceptions.format("The function uses {0} of the for (let ...) loop at line {1}, which all iterations " +
                                                                 "share in Rhino 1.8.1; copy it into a let inside the loop body.",
                                                                 String.join(", ", usedNames), loop.getLineno())));
            }

            return false;
        });
    }

    /**
     * Collects the variable names of a declaration, with the names of destructuring targets.
     */
    private static @NotNull Set<String> collectTargetNames(@NotNull VariableDeclaration declaration) {
        Set<String> names = new LinkedHashSet<>();

        for (VariableInitializer variable : declaration.getVariables()) {
            variable.getTarget().visit(node -> {
                if (node instanceof Name name && (node == variable.getTarget() || node.getParent() instanceof ArrayLiteral ||
                                                  node.getParent() instanceof ObjectProperty property && property.getRight() == node)) {
                    names.add(name.getIdentifier());
                }

                return true;
            });
        }

        return names;
    }

    /**
     * Reads every {@value #GRP_EXTENSION} and {@value #ANIM_EXTENSION} resource as the runtime reads a GRP or an anim.
     */
    private static void checkResources(@NotNull Map<String, byte[]> resources, @NotNull List<ScriptFinding> findings) {
        ImageReplacements reader = new ImageReplacements(() -> null);

        for (Map.Entry<String, byte[]> resource : resources.entrySet()) {
            String name = resource.getKey();
            String lowerName = name.toLowerCase(Locale.ROOT);
            byte[] content = resource.getValue();

            try {
                if (lowerName.endsWith(GRP_EXTENSION)) {
                    reader.readGrp(content, 0, content.length, name);
                } else if (lowerName.endsWith(ANIM_EXTENSION)) {
                    reader.readAnim(content, 0, content.length, name);
                }
            } catch (IOException | RuntimeException exception) {
                findings.add(new ScriptFinding(RESOURCE_FILE_PREFIX + name, ScriptFinding.NO_LINE, Severity.ERROR,
                                               Exceptions.format("The resource cannot be read: {0}", exception.getMessage())));
            }
        }
    }

    private ScriptCheckUtils() {
    }
}
