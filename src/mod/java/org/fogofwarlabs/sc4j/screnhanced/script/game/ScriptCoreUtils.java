package org.fogofwarlabs.sc4j.screnhanced.script.game;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * Defines the functions of the {@code core} global and converts their arguments.
 * <p>
 * An integer argument is a JavaScript number that is integral and inside the signed 32-bit range; any other value throws a {@code TypeError}.
 * An id outside its range throws a {@code RangeError}. The functions are read-only and permanent properties, and every namespace of
 * {@code core} is closed to new properties, so a script cannot replace them.
 */
final class ScriptCoreUtils {
    /**
     * The id a {@code core} function returns for no unit, location or type.
     */
    static final int NO_ID = -1;

    private static final int MEMBER_ATTRIBUTES = ScriptableObject.READONLY | ScriptableObject.DONTENUM | ScriptableObject.PERMANENT;

    /**
     * Creates an empty namespace object and defines it as a property of {@code core}.
     *
     * @param context the current context
     * @param global  the global scope
     * @param core    the {@code core} object
     * @param name    the namespace name
     * @return the created namespace object
     */
    static @NotNull ScriptableObject createNamespace(@NotNull Context context, @NotNull Scriptable global, @NotNull Scriptable core,
                                                     @NotNull String name) {
        ScriptableObject namespace = (ScriptableObject) context.newObject(global);

        ScriptableObject.defineProperty(core, name, namespace, MEMBER_ATTRIBUTES);

        return namespace;
    }

    /**
     * Defines a function as a read-only, non-enumerable and permanent property of a namespace.
     *
     * @param global    the global scope
     * @param namespace the namespace object
     * @param name      the property and function name
     * @param length    the function's {@code length}
     * @param target    the function body
     */
    static void defineFunction(@NotNull Scriptable global, @NotNull Scriptable namespace, @NotNull String name, int length,
                               @NotNull Callable target) {
        ScriptableObject.defineProperty(namespace, name, new LambdaFunction(global, name, length, target), MEMBER_ATTRIBUTES);
    }

    /**
     * Reads an integer argument.
     *
     * @param arguments the call arguments
     * @param index     the argument index
     * @return the integer
     * @throws EcmaError if the argument is missing or not an integral number inside the 32-bit range
     */
    static int readInteger(@NotNull Object @NotNull [] arguments, int index) {
        double value = index < arguments.length && arguments[index] instanceof Number number ? number.doubleValue() : Double.NaN;

        if (!(value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) || value != StrictMath.floor(value)) {
            throw ScriptRuntime.typeError(Exceptions.format("Argument {0} is not an integer.", index + 1));
        }

        return (int) value;
    }

    /**
     * Reads a boolean argument.
     *
     * @param arguments the call arguments
     * @param index     the argument index
     * @return the boolean
     * @throws EcmaError if the argument is missing or not a boolean
     */
    static boolean readBoolean(@NotNull Object @NotNull [] arguments, int index) {
        if (index >= arguments.length || !(arguments[index] instanceof Boolean value)) {
            throw ScriptRuntime.typeError(Exceptions.format("Argument {0} is not a boolean.", index + 1));
        }

        return value;
    }

    /**
     * Reads a string argument.
     *
     * @param arguments the call arguments
     * @param index     the argument index
     * @return the string
     * @throws EcmaError if the argument is missing or not a string
     */
    static @NotNull String readString(@NotNull Object @NotNull [] arguments, int index) {
        if (index >= arguments.length || !(arguments[index] instanceof CharSequence value)) {
            throw ScriptRuntime.typeError(Exceptions.format("Argument {0} is not a string.", index + 1));
        }

        return value.toString();
    }

    /**
     * Reads a function argument.
     *
     * @param arguments the call arguments
     * @param index     the argument index
     * @return the function
     * @throws EcmaError if the argument is missing or not a function
     */
    static @NotNull Function readFunction(@NotNull Object @NotNull [] arguments, int index) {
        if (index >= arguments.length || !(arguments[index] instanceof Function value)) {
            throw ScriptRuntime.typeError(Exceptions.format("Argument {0} is not a function.", index + 1));
        }

        return value;
    }

    /**
     * Checks that an id lies in {@code 0} to {@code count - 1}.
     *
     * @param kind  the kind of the id for the error message
     * @param id    the id
     * @param count the number of ids
     * @return the id
     * @throws EcmaError if the id is outside the range
     */
    static int requireId(@NotNull String kind, int id, int count) {
        if (id < 0 || id >= count) {
            throw ScriptRuntime.rangeError(Exceptions.format("The {0} {1} is not from 0 to {2}.", kind, id, count - 1));
        }

        return id;
    }


    private ScriptCoreUtils() {
    }
}
