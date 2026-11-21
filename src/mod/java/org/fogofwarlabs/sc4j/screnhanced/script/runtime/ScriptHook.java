package org.fogofwarlabs.sc4j.screnhanced.script.runtime;

import org.fogofwarlabs.sc4j.scr.game.extension.simulation.BulletCreatedEvent;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.BulletHitEvent;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.OrderDecision;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.OrderIssuedEvent;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.OwnerChangedEvent;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.PlayerLeftEvent;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.PlayerResultEvent;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.ResearchCompletedEvent;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.SpellCastEvent;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.TargetAcquiredEvent;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.TargetAcquisitionDecision;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.UnitCompletedEvent;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.UnitCreatedEvent;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.UnitDamagedEvent;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.UnitDiedEvent;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.UnitLoadedEvent;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.UnitTrainedEvent;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.UnitUnloadedEvent;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.UpgradeCompletedEvent;
import org.fogofwarlabs.sc4j.scr.game.extension.simulation.WeaponDamageDecision;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Lists the hooks that the script runtime calls: top-level functions of a module with a known name.
 * <p>
 * A hook name is {@value #NAME_PREFIX} followed by an upper-case letter. The runtime collects the hooks of each module when the module has
 * loaded and calls a hook in every module that has it, in load order.
 * <p>
 * An event hook receives one event object at the start of the frame after the event; a decision hook ({@code onBefore*}) receives one
 * decision object while the game routine waits for the decision. The kind of such a hook is the class of the SC:R simulation event or
 * decision it receives. These hooks run on every peer, as does the rest of the shared code.
 * <p>
 * A local hook ({@code onLocal*} and {@link #RENDER}) has no kind: it receives one local event of the local player's input at once, the frame
 * number after a played frame ({@link #LOCAL_FRAME}), or the screen object to draw on ({@link #RENDER}), on the local machine only, with the
 * game read-only ({@link #isLocal()}).
 */
public enum ScriptHook {
    /**
     * Called once after the main script has loaded, without arguments.
     */
    START("onStart", null),
    /**
     * Called every game frame with the frame number.
     */
    FRAME("onFrame", null),

    /**
     * Called with the event of a created unit.
     */
    UNIT_CREATED("onUnitCreated", UnitCreatedEvent.class),
    /**
     * Called with the event of a completed unit.
     */
    UNIT_COMPLETED("onUnitCompleted", UnitCompletedEvent.class),
    /**
     * Called with the event of a unit that started to die, with its killer and the weapon when known.
     */
    UNIT_DIED("onUnitDied", UnitDiedEvent.class),
    /**
     * Called with the event of hit point damage to a unit.
     */
    UNIT_DAMAGED("onUnitDamaged", UnitDamagedEvent.class),
    /**
     * Called with the event of an order a unit received.
     */
    ORDER_ISSUED("onOrderIssued", OrderIssuedEvent.class),
    /**
     * Called with the event of a target an automatic target search found.
     */
    TARGET_ACQUIRED("onTargetAcquired", TargetAcquiredEvent.class),
    /**
     * Called with the event of a unit a producer trained.
     */
    UNIT_TRAINED("onUnitTrained", UnitTrainedEvent.class),
    /**
     * Called with the event of a finished research.
     */
    RESEARCH_COMPLETED("onResearchCompleted", ResearchCompletedEvent.class),
    /**
     * Called with the event of a finished upgrade.
     */
    UPGRADE_COMPLETED("onUpgradeCompleted", UpgradeCompletedEvent.class),
    /**
     * Called with the event of a cast technology.
     */
    SPELL_CAST("onSpellCast", SpellCastEvent.class),
    /**
     * Called with the event of a created bullet.
     */
    BULLET_CREATED("onBulletCreated", BulletCreatedEvent.class),
    /**
     * Called with the event of a bullet hit.
     */
    BULLET_HIT("onBulletHit", BulletHitEvent.class),
    /**
     * Called with the event of a unit that changed its owner.
     */
    OWNER_CHANGED("onOwnerChanged", OwnerChangedEvent.class),
    /**
     * Called with the event of a unit that entered a transport or a Bunker.
     */
    UNIT_LOADED("onUnitLoaded", UnitLoadedEvent.class),
    /**
     * Called with the event of a unit that left a transport or a Bunker.
     */
    UNIT_UNLOADED("onUnitUnloaded", UnitUnloadedEvent.class),
    /**
     * Called with the event of a player who left the game.
     */
    PLAYER_LEFT("onPlayerLeft", PlayerLeftEvent.class),
    /**
     * Called with the event of a player's result.
     */
    PLAYER_RESULT("onPlayerResult", PlayerResultEvent.class),

    /**
     * Called on the local machine with the event of a chat line the local player submitted.
     */
    LOCAL_CHAT("onLocalChat", null),
    /**
     * Called on the local machine with the event of a key the local player pressed.
     */
    LOCAL_KEY_DOWN("onLocalKeyDown", null),
    /**
     * Called on the local machine with the event of a key the local player released.
     */
    LOCAL_KEY_UP("onLocalKeyUp", null),
    /**
     * Called on the local machine with the event of a mouse button the local player pressed over the map.
     */
    LOCAL_MOUSE_DOWN("onLocalMouseDown", null),
    /**
     * Called on the local machine with the event of a mouse button the local player released.
     */
    LOCAL_MOUSE_UP("onLocalMouseUp", null),
    /**
     * Called on the local machine with the frame number after each game frame the machine plays, live or replayed.
     */
    LOCAL_FRAME("onLocalFrame", null),
    /**
     * Called on the local machine with the screen object after each game frame the machine plays, live or replayed, after
     * {@link #LOCAL_FRAME}; what it draws is shown until the next call.
     */
    RENDER("onRender", null, true),

    /**
     * Called with the decision on the damage of a weapon hit.
     */
    BEFORE_DAMAGE("onBeforeDamage", WeaponDamageDecision.class),
    /**
     * Called with the decision on an order issued to a unit.
     */
    BEFORE_ORDER("onBeforeOrder", OrderDecision.class),
    /**
     * Called with the decision on the target an automatic target search found.
     */
    BEFORE_TARGET_ACQUIRED("onBeforeTargetAcquired", TargetAcquisitionDecision.class);

    /**
     * The prefix of every hook name.
     */
    public static final @NotNull String NAME_PREFIX = "on";
    /**
     * The prefix of every local hook name.
     */
    public static final @NotNull String LOCAL_NAME_PREFIX = "onLocal";

    private static final @NotNull Map<String, ScriptHook> HOOKS_BY_FUNCTION_NAME = new HashMap<>();
    private static final @NotNull Map<Class<?>, ScriptHook> HOOKS_BY_KIND = new HashMap<>();

    static {
        for (ScriptHook hook : values()) {
            HOOKS_BY_FUNCTION_NAME.put(hook.functionName, hook);

            if (hook.kind != null) {
                HOOKS_BY_KIND.put(hook.kind, hook);
            }
        }
    }

    /**
     * Finds the hook of a function name.
     *
     * @param functionName the function name
     * @return the hook with the function name, or {@code null} if no hook has it
     */
    public static @Nullable ScriptHook findByFunctionName(@NotNull String functionName) {
        return HOOKS_BY_FUNCTION_NAME.get(functionName);
    }

    /**
     * Finds the hook that receives a kind of SC:R simulation event or decision.
     *
     * @param kind the class of the event or decision
     * @return the hook of the kind, or {@code null} if no hook receives it
     */
    public static @Nullable ScriptHook findByKind(@NotNull Class<?> kind) {
        return HOOKS_BY_KIND.get(kind);
    }

    /**
     * Checks whether a function name has the form of a hook name.
     *
     * @param functionName the function name
     * @return {@code true} if the name is {@value #NAME_PREFIX} followed by an upper-case ASCII letter and more, {@code false} otherwise
     */
    public static boolean isHookName(@NotNull String functionName) {
        if (functionName.length() <= NAME_PREFIX.length() || !functionName.startsWith(NAME_PREFIX)) {
            return false;
        }

        char first = functionName.charAt(NAME_PREFIX.length());

        return first >= 'A' && first <= 'Z';
    }


    private final @NotNull String functionName;
    private final @Nullable Class<?> kind;
    private final boolean local;

    ScriptHook(@NotNull String functionName, @Nullable Class<?> kind) {
        this(functionName, kind, functionName.startsWith(LOCAL_NAME_PREFIX));
    }

    ScriptHook(@NotNull String functionName, @Nullable Class<?> kind, boolean local) {
        this.functionName = functionName;
        this.kind         = kind;
        this.local        = local;
    }

    /**
     * @return the name of the hook function
     */
    public @NotNull String getFunctionName() {
        return this.functionName;
    }

    /**
     * @return {@code true} if the hook runs on the local machine only, its name starting with {@value #LOCAL_NAME_PREFIX} or {@link #RENDER},
     *         {@code false} if it runs on every peer
     */
    public boolean isLocal() {
        return this.local;
    }
}
