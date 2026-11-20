package org.fogofwarlabs.sc4j.screnhanced.script.runtime;

import org.fogofwarlabs.sc4j.screnhanced.script.ScriptPackage;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tests the sandbox, the globals, the hooks, the instruction budget and the error handling of {@link ScriptRuntime}.
 */
final class ScriptRuntimeTest {
    private static final @NotNull String BLOCKED = "blocked";

    /**
     * Creates a script that logs, for each expression, {@value #BLOCKED} and the error name when evaluating it throws, or the value.
     */
    private static @NotNull String createAttemptScript(@NotNull List<@NotNull String> expressions) {
        StringBuilder script = new StringBuilder();

        for (String expression : expressions) {
            script.append("try { log.info('reached ' + String(").append(expression).append(")); } catch (e) { log.info('").append(BLOCKED)
                  .append(" ' + e.name); }\n");
        }

        return script.toString();
    }

    @Test
    void globals_expose_no_java_access_clock_or_code_loading() throws IOException {
        List<String> names = List.of("java", "javax", "Packages", "getClass", "importPackage", "importClass", "JavaAdapter", "JavaImporter",
                                     "JavaException", "load", "loadClass", "print", "readFile", "runCommand", "spawn", "sync", "quit",
                                     "Date", "setTimeout", "setInterval", "eval", "Script", "Continuation", "With", "Call", "XML", "XMLList",
                                     "Namespace", "QName", "isXMLName", "__GeneratorFunction");
        StringBuilder script = new StringBuilder("var own = Object.getOwnPropertyNames(globalThis);\n");

        for (String name : names) {
            script.append("log.info('").append(name).append(" ' + typeof ").append(name).append(" + ' ' + own.includes('").append(name)
                  .append("'));\n");
        }

        ScriptRuntimeScenario scenario = ScriptRuntimeScenario.start(script.toString());
        List<String> expected = new ArrayList<>();

        for (String name : names) {
            expected.add(name + " undefined false");
        }

        Assertions.assertEquals(expected, scenario.getInfoMessages(), scenario.getLines()::toString);
    }

    @Test
    void script_errors_carry_no_java_objects() throws IOException {
        ScriptRuntimeScenario scenario = ScriptRuntimeScenario.start("""
                try { null.x; } catch (e) { log.info(e.name, typeof e.rhinoException, typeof e.javaException); }
                try { missing(); } catch (e) { log.info(e.name, typeof e.rhinoException, typeof e.javaException); }
                try { require(); } catch (e) { log.info(e.name, typeof e.rhinoException, typeof e.javaException); }
                log.info(typeof ''.getClass, typeof log.getClass, typeof Math.getClass, typeof random.getClass);
                """);

        Assertions.assertEquals(List.of("TypeError undefined undefined", "ReferenceError undefined undefined", "TypeError undefined undefined",
                                        "undefined undefined undefined undefined"), scenario.getInfoMessages(), scenario.getLines()::toString);
    }

    @Test
    void code_from_strings_cannot_run() throws IOException {
        List<String> attempts = List.of("Function('return 1')()", "new Function('return 1')()", "(function () {}).constructor('return 1')()",
                                        "(() => 1).constructor('return 1')()", "(function* () {}).constructor('yield 1')().next().value",
                                        "Object.getPrototypeOf(function* () {}).constructor('yield 1')", "[].constructor.constructor('return 1')()",
                                        "log.info.constructor('return 1')()", "require.constructor('return 1')()", "Math.sin.constructor('return 1')()",
                                        "Reflect.construct(Function, ['return 1'])()", "Function.prototype.constructor('return 1')()",
                                        "eval('1')", "globalThis.eval('1')", "new Script('1')", "this.constructor.constructor('return 1')()");

        ScriptRuntimeScenario scenario = ScriptRuntimeScenario.start(createAttemptScript(attempts));

        for (String message : scenario.getInfoMessages()) {
            Assertions.assertTrue(message.startsWith(BLOCKED), message);
        }

        Assertions.assertEquals(attempts.size(), scenario.getInfoMessages().size());
    }

    @Test
    void functions_keep_their_prototype_without_the_string_constructor() throws IOException {
        ScriptRuntimeScenario scenario = ScriptRuntimeScenario.start("""
                function add(first, second) { return first + second; }
                function* count() { yield 1; yield 2; }
                log.info((function () {}) instanceof Function, (() => 1) instanceof Function, add.call(null, 1, 2), add.apply(null, [3, 4]),
                         add.bind(null, 5)(6), Array.from(count()).join('+'), typeof Function.prototype.toString);
                """);

        Assertions.assertEquals(List.of("true true 3 7 11 1+2 function"), scenario.getInfoMessages(), scenario.getLines()::toString);
    }

    @Test
    void the_generator_constructor_stays_disabled_in_later_modules() throws IOException {
        ScriptPackage scriptPackage = ScriptRuntimeScenario.createPackage(
                ScriptRuntimeScenario.MAIN_NAME, """
                        log.info(delete globalThis.GeneratorFunction, typeof GeneratorFunction);
                        GeneratorFunction = null;
                        delete globalThis.Function;
                        require('./later.js');
                        """,
                "later.js", createAttemptScript(List.of("typeof __GeneratorFunction", "(function* () {}).constructor('yield 1')().next().value",
                                                        "GeneratorFunction('yield 1')"))
        );

        List<String> lines = ScriptRuntimeScenario.run(scriptPackage, ScriptRuntimeScenario.SEED, 0);

        Assertions.assertEquals(List.of("INFO main.js: false function", "INFO later.js: reached undefined", "INFO later.js: blocked TypeError",
                                        "INFO later.js: blocked TypeError"), lines);
    }

    @Test
    void promise_jobs_run_within_the_call() throws IOException {
        ScriptRuntimeScenario scenario = ScriptRuntimeScenario.start("""
                function onStart() {
                    Promise.resolve('job').then(value => log.info(value));
                    log.info('start');
                }
                function onFrame(frame) {
                    Promise.resolve(frame).then(value => log.info('job ' + value));
                    log.info('frame ' + frame);
                }
                """);

        scenario.getRuntime().runFrame(1);

        Assertions.assertEquals(List.of("start", "job", "frame 1", "job 1"), scenario.getInfoMessages(), scenario.getLines()::toString);
    }

    @Test
    void the_same_package_and_seed_give_the_same_results() throws IOException {
        ScriptPackage scriptPackage = ScriptRuntimeScenario.createPackage(ScriptRuntimeScenario.MAIN_NAME, """
                const values = [];
                function onStart() {
                    for (let index = 0; index < 4; index++) {
                        values.push(random.next(), random.nextInt(1000), random.chance(0.5), Math.random());
                    }
                    log.info(values.join(','));
                    log.info(JSON.stringify({b: 1, a: [3, 1, 2].sort(), c: Math.sin(1) / 3, d: (0.1 + 0.2).toFixed(20)}));
                    log.info(Object.keys({z: 1, 10: 2, y: 3, 2: 4}).join());
                    log.info((1234.5678).toString(16), (1e21).toString(), (123.456).toPrecision(10), 'abc'.localeCompare('abd'),
                             'a1b22'.replace(/[0-9]+/g, 'x'), /^([a-z]+)-([0-9])$/.exec('unit-7')[2]);
                }
                function onFrame(frame) {
                    log.info(frame, random.nextInt(6), Math.atan2(frame, 3), Math.pow(1.5, frame));
                }
                """);

        List<String> first = ScriptRuntimeScenario.run(scriptPackage, ScriptRuntimeScenario.SEED, 10);
        List<String> second = ScriptRuntimeScenario.run(scriptPackage, ScriptRuntimeScenario.SEED, 10);
        List<String> other = ScriptRuntimeScenario.run(scriptPackage, ScriptRuntimeScenario.SEED + 1, 10);

        Assertions.assertEquals(14, first.size(), first::toString);
        Assertions.assertTrue(first.get(3).endsWith(" -1 axbx 7"), first::toString);
        Assertions.assertEquals(first, second);
        Assertions.assertNotEquals(first.getFirst(), other.getFirst());
    }

    @Test
    void math_random_and_random_next_draw_from_the_same_stream() throws IOException {
        ScriptPackage mathFirst = ScriptRuntimeScenario.createPackage(ScriptRuntimeScenario.MAIN_NAME, "log.info(Math.random(), random.next());");
        ScriptPackage randomFirst = ScriptRuntimeScenario.createPackage(ScriptRuntimeScenario.MAIN_NAME, "log.info(random.next(), Math.random());");

        Assertions.assertEquals(ScriptRuntimeScenario.run(mathFirst, ScriptRuntimeScenario.SEED, 0),
                                ScriptRuntimeScenario.run(randomFirst, ScriptRuntimeScenario.SEED, 0));
    }

    @Test
    void random_gives_numbers_in_range_and_refuses_bad_bounds() throws IOException {
        ScriptRuntimeScenario scenario = ScriptRuntimeScenario.start("""
                let inRange = true;
                const seen = new Set();
                for (let index = 0; index < 1000; index++) {
                    let next = random.next();
                    let die = random.nextInt(6);
                    inRange = inRange && next >= 0 && next < 1 && Number.isInteger(die) && die >= 0 && die < 6;
                    seen.add(die);
                }
                log.info(inRange, seen.size, random.chance(0), random.chance(1), random.nextInt(1), random.nextInt(2 ** 53) < 2 ** 53);
                for (let bound of [0, -1, 1.5, NaN, Infinity, 2 ** 53 + 2, undefined]) {
                    try { random.nextInt(bound); log.info('accepted', bound); } catch (e) { log.info(e.name); }
                }
                """);

        Assertions.assertEquals(List.of("true 6 false true 0 true", "RangeError", "RangeError", "RangeError", "RangeError", "RangeError",
                                        "RangeError", "RangeError"), scenario.getInfoMessages(), scenario.getLines()::toString);
    }

    @Test
    void hooks_run_per_module_in_load_order() throws IOException {
        ScriptPackage scriptPackage = ScriptRuntimeScenario.createPackage(
                ScriptRuntimeScenario.MAIN_NAME, """
                        require('./first.js');
                        require('./second.js');
                        function onStart() { log.info('start'); }
                        function onFrame(frame) { log.info('frame', frame); }
                        """,
                "first.js", """
                        function onFrame(frame) { log.info('frame', frame); }
                        """,
                "second.js", """
                        function onStart() { log.info('start'); }
                        var onFrame = function (frame) { log.info('frame', frame); };
                        """
        );

        List<String> lines = ScriptRuntimeScenario.run(scriptPackage, ScriptRuntimeScenario.SEED, 2);

        Assertions.assertEquals(List.of("INFO main.js: start", "INFO second.js: start",
                                        "INFO main.js: frame 0", "INFO first.js: frame 0", "INFO second.js: frame 0",
                                        "INFO main.js: frame 1", "INFO first.js: frame 1", "INFO second.js: frame 1"), lines);
    }

    @Test
    void unknown_hook_names_log_one_warning_each() throws IOException {
        ScriptRuntimeScenario scenario = ScriptRuntimeScenario.start("""
                function onUnitDied() { log.info('called'); }
                function onstart() { log.info('called'); }
                function on() { log.info('called'); }
                var onCount = 3;
                function onStart() { log.info('start'); }
                """);

        scenario.getRuntime().runFrame(0);
        scenario.getRuntime().runFrame(1);

        Assertions.assertEquals(List.of("WARN main.js: onUnitDied is not a known hook and is not called.", "INFO main.js: start"),
                                scenario.getLines());
        Assertions.assertTrue(scenario.getRuntime().hasHook(ScriptHook.START));
        Assertions.assertFalse(scenario.getRuntime().hasHook(ScriptHook.FRAME));
    }

    @Test
    void an_exceeded_budget_stops_the_script_for_the_rest_of_the_game() throws IOException {
        ScriptPackage scriptPackage = ScriptRuntimeScenario.createPackage(
                ScriptRuntimeScenario.MAIN_NAME, """
                        require('./other.js');
                        function onFrame(frame) {
                            log.info('main', frame);
                            if (frame === 2) {
                                try { while (true) {} } catch (e) { log.info('caught'); } finally { log.info('finally'); }
                            }
                        }
                        """,
                "other.js", """
                        function onFrame(frame) { log.info('other', frame); }
                        """
        );
        ScriptRuntimeScenario scenario = new ScriptRuntimeScenario(scriptPackage, ScriptRuntimeScenario.SEED, 100_000);

        scenario.getRuntime().start();

        for (int frame = 0; frame < 5; frame++) {
            scenario.getRuntime().runFrame(frame);
        }

        Assertions.assertEquals(List.of("INFO main.js: main 0", "INFO other.js: other 0", "INFO main.js: main 1", "INFO other.js: other 1",
                                        "INFO main.js: main 2",
                                        "ERROR main.js: onFrame ran more than the budget of instructions; the script stopped."),
                                scenario.getLines());
        Assertions.assertTrue(scenario.getRuntime().isStopped());
        Assertions.assertFalse(scenario.getRuntime().hasHook(ScriptHook.FRAME));
    }

    @Test
    void the_budget_counts_each_call_on_its_own() throws IOException {
        ScriptPackage scriptPackage = ScriptRuntimeScenario.createPackage(ScriptRuntimeScenario.MAIN_NAME, """
                let total = 0;
                function onFrame(frame) {
                    for (let index = 0; index < 1000; index++) { total += index; }
                }
                """);
        ScriptRuntimeScenario scenario = new ScriptRuntimeScenario(scriptPackage, ScriptRuntimeScenario.SEED, 100_000);

        scenario.getRuntime().start();

        for (int frame = 0; frame < 200; frame++) {
            scenario.getRuntime().runFrame(frame);
        }

        Assertions.assertFalse(scenario.getRuntime().isStopped());
        Assertions.assertEquals(List.of(), scenario.getLines());
    }

    @Test
    void the_budget_covers_loading_and_on_start() throws IOException {
        ScriptPackage scriptPackage = ScriptRuntimeScenario.createPackage(ScriptRuntimeScenario.MAIN_NAME, """
                for (;;) {}
                """);
        ScriptRuntimeScenario scenario = new ScriptRuntimeScenario(scriptPackage, ScriptRuntimeScenario.SEED, 100_000);

        scenario.getRuntime().start();

        Assertions.assertTrue(scenario.getRuntime().isStopped());
        Assertions.assertEquals(List.of("ERROR main.js: Loading ran more than the budget of instructions; the script stopped."),
                                scenario.getLines());
    }

    @Test
    void an_uncaught_error_ends_only_its_hook_call() throws IOException {
        ScriptPackage scriptPackage = ScriptRuntimeScenario.createPackage(
                ScriptRuntimeScenario.MAIN_NAME, """
                        require('./other.js');
                        function onStart() {
                            throw new Error('start failed');
                        }
                        function onFrame(frame) {
                            if (frame === 1) {
                                null.x;
                            }
                            log.info('main', frame);
                        }
                        """,
                "other.js", """
                        function onStart() { log.info('other start'); }
                        function onFrame(frame) { log.info('other', frame); }
                        """
        );

        List<String> lines = ScriptRuntimeScenario.run(scriptPackage, ScriptRuntimeScenario.SEED, 3);

        Assertions.assertEquals(List.of("ERROR main.js: onStart failed: Error: start failed (main.js:3)", "INFO other.js: other start",
                                        "INFO main.js: main 0", "INFO other.js: other 0",
                                        "ERROR main.js: onFrame failed: TypeError: Cannot read property \"x\" from null (main.js:7)",
                                        "INFO other.js: other 1", "INFO main.js: main 2", "INFO other.js: other 2"), lines);
    }

    @Test
    void a_syntax_error_ends_the_loading_of_its_script() throws IOException {
        ScriptRuntimeScenario scenario = ScriptRuntimeScenario.start("""
                function onFrame(frame) {
                    log.info('frame', frame;
                }
                """);

        scenario.getRuntime().runFrame(0);

        Assertions.assertEquals(1, scenario.getLines().size(), scenario.getLines()::toString);
        Assertions.assertTrue(scenario.getLines().getFirst().startsWith("ERROR main.js: Loading failed: "), scenario.getLines()::toString);
        Assertions.assertTrue(scenario.getLines().getFirst().endsWith("(main.js:2)"), scenario.getLines()::toString);
        Assertions.assertFalse(scenario.getRuntime().isStopped());
    }

    @Test
    void deep_script_recursion_is_a_script_error() throws IOException {
        ScriptRuntimeScenario scenario = ScriptRuntimeScenario.start("""
                function down(depth) { return depth === 0 ? 0 : 1 + down(depth - 1); }
                log.info(down(5000));
                try { down(100000); } catch (e) { log.info(e.name); }
                """);

        Assertions.assertEquals(List.of("5000", "InternalError"), scenario.getInfoMessages(), scenario.getLines()::toString);
        Assertions.assertFalse(scenario.getRuntime().isStopped());
    }

    @Test
    void a_java_stack_overflow_stops_the_script() throws IOException {
        ScriptRuntimeScenario scenario = ScriptRuntimeScenario.start("""
                function down(depth) { return [depth].map(value => down(value + 1))[0]; }
                function onStart() { down(0); }
                """);

        Assertions.assertTrue(scenario.getRuntime().isStopped());
        Assertions.assertEquals(List.of("ERROR main.js: onStart overflowed the stack; the script stopped."), scenario.getLines());
    }

    @Test
    void start_and_run_frame_should_fail_out_of_order() throws IOException {
        ScriptPackage scriptPackage = ScriptRuntimeScenario.createPackage(ScriptRuntimeScenario.MAIN_NAME, "function onFrame() {}");
        ScriptRuntimeScenario scenario = new ScriptRuntimeScenario(scriptPackage, ScriptRuntimeScenario.SEED, ScriptRuntime.INSTRUCTION_BUDGET);
        ScriptRuntime runtime = scenario.getRuntime();

        Assertions.assertThrows(IllegalStateException.class, () -> runtime.runFrame(0));

        runtime.start();

        Assertions.assertThrows(IllegalStateException.class, runtime::start);

        runtime.runFrame(0);
        runtime.dispose();

        Assertions.assertThrows(IllegalStateException.class, () -> runtime.runFrame(1));
        Assertions.assertTrue(runtime.isStopped());
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> ScriptRuntime.create(scriptPackage, ScriptRuntimeScenario.SEED, 0, scenario));
    }

    @Test
    void runtimes_do_not_share_globals() throws IOException {
        ScriptPackage scriptPackage = ScriptRuntimeScenario.createPackage(ScriptRuntimeScenario.MAIN_NAME, """
                log.info(typeof globalThis.shared, Array.prototype.marked);
                globalThis.shared = 1;
                Array.prototype.marked = true;
                Math.sin = null;
                """);
        List<String> lines = new ArrayList<>(ScriptRuntimeScenario.run(scriptPackage, ScriptRuntimeScenario.SEED, 0));

        lines.addAll(ScriptRuntimeScenario.run(scriptPackage, ScriptRuntimeScenario.SEED, 0));

        Assertions.assertEquals(Collections.nCopies(2, "INFO main.js: undefined undefined"), lines);
    }
}
