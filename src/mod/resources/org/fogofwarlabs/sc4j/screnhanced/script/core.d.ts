// The lower layer of SC:R Enhanced Script (script API version 1): the global core, ids, raw values and plain records over the game (hit points,
// shields, energy and damage in 1/256, supply in halves, DAT values as the tables hold them). The upper layer (scr-enhanced.d.ts) calls it,
// and scripts may call it as well. No function takes a callback: what happened since the previous frame is read as records of the frame.
//
// Every function has a mark: [S] works in shared code only (module top level, onStart, onFrame, the event and decision hooks, timers and
// channel handlers) and throws a TypeError in local code; [L] works in local code only (onLocalFrame, onRender and the local input hooks)
// and throws a TypeError in shared code; [A] works anywhere. While a decision runs the game is read-only: an [S] function that changes the game
// throws a TypeError as well. An id outside its range, a missing target or a bad value throws a RangeError.

// ---------------------------------------------------------------------------------------------------------------------------------------
// The records.
// ---------------------------------------------------------------------------------------------------------------------------------------

/**
 * The record of an event of the frame ({@link CoreEvents.getFrameEvents}), read-only: the kind ({@link EventKind}), the frame it happened in and
 * the values of its kind. A unit is its id with the generation of its slot under the name followed by Generation (-1 for none), players,
 * types and orders are ids (-1 for none), the amount is in 1/256.
 * UnitCreated, UnitCompleted: unit, unitType, owner. UnitDied: also killer, killerPlayer, weapon. UnitDamaged: also source, sourcePlayer,
 * weapon, amount. OrderIssued: also order, target, targetX, targetY, buildType, queued. TargetAcquired: also target. UnitTrained: also
 * producer. ResearchCompleted: also tech. UpgradeCompleted: also upgrade, level. SpellCast: also tech, target, targetX, targetY.
 * OwnerChanged: also previousOwner. UnitLoaded, UnitUnloaded: also transport. BulletCreated, BulletHit: weapon, player, source, target,
 * targetX, targetY, and missed for BulletHit. PlayerLeft: player, reason. PlayerResult: player, result.
 */
interface CoreEventRecord {
    readonly kind: number;
    readonly frame: number;
    readonly unit?: number;
    readonly unitGeneration?: number;
    readonly unitType?: number;
    readonly owner?: number;
    readonly killer?: number;
    readonly killerGeneration?: number;
    readonly killerPlayer?: number;
    readonly source?: number;
    readonly sourceGeneration?: number;
    readonly sourcePlayer?: number;
    readonly weapon?: number;
    readonly amount?: number;
    readonly order?: number;
    readonly target?: number;
    readonly targetGeneration?: number;
    readonly targetX?: number;
    readonly targetY?: number;
    readonly buildType?: number;
    readonly queued?: boolean;
    readonly producer?: number;
    readonly producerGeneration?: number;
    readonly tech?: number;
    readonly upgrade?: number;
    readonly level?: number;
    readonly previousOwner?: number;
    readonly transport?: number;
    readonly transportGeneration?: number;
    readonly player?: number;
    readonly missed?: boolean;
    readonly reason?: number;
    readonly result?: number;
}

/**
 * The record of a decision ({@link DecisionKind}) that the decision hooks receive through their decision objects ({@link DamageDecision.getRecord}).
 * Damage: unit, unitGeneration, unitType, owner, source, sourceGeneration, sourcePlayer, weapon; a hook may change amount (1/256) and
 * cancelled. Order: unit, unitGeneration, unitType, owner, buildType, queued; a hook may change order, target, targetGeneration, targetX,
 * targetY and rejected. TargetAcquired: unit, unitGeneration, unitType, owner; a hook may change target and targetGeneration. The changes of
 * a hook that returns go to the next hook and to the game.
 */
interface CoreDecisionRecord {
    readonly kind: number;
    readonly unit: number;
    readonly unitGeneration: number;
    readonly unitType: number;
    readonly owner: number;
    readonly source?: number;
    readonly sourceGeneration?: number;
    readonly sourcePlayer?: number;
    readonly weapon?: number;
    readonly buildType?: number;
    readonly queued?: boolean;
    amount?: number;
    cancelled?: boolean;
    order?: number;
    target?: number;
    targetGeneration?: number;
    targetX?: number;
    targetY?: number;
    rejected?: boolean;
}

/**
 * The record of a local input ({@link InputKind}): for a key the key code, for a mouse button the button, and for both the modifier mask
 * ({@link Modifier}) and the map position x, y when positioned; for a chat line the text; for a command card button (ButtonPressed) the
 * shown set, the button's index in the set (-1 for a button of no table entry), its position, its condition and action ids
 * ({@link CoreButtons.getConditionName}, {@link CoreButtons.getActionName}) with their values, queued (Shift held) and hotkey (a hotkey
 * pressed it), and for the target of a ScriptTarget button (ButtonTargeted) also the map position x, y and the target unit with its
 * generation (-1 for the ground); and consumed, which keeps the input from the game when a hook sets it: a consumed press runs no action, a
 * consumed target keeps the targeting mode. Read-only once the input went to the game.
 */
interface CoreInputRecord {
    readonly kind: number;
    readonly key?: number;
    readonly button?: number;
    readonly modifiers?: number;
    readonly positioned?: boolean;
    readonly x?: number;
    readonly y?: number;
    readonly text?: string;
    readonly set?: number;
    readonly position?: number;
    readonly condition?: number;
    readonly conditionValue?: number;
    readonly action?: number;
    readonly actionValue?: number;
    readonly queued?: boolean;
    readonly hotkey?: boolean;
    readonly target?: number;
    readonly targetGeneration?: number;
    consumed: boolean;
}

/**
 * The record of a script command that ran ({@link CoreCommand.getExecuted}): the sending player's id, the value, and the ids of the live units
 * the player had selected when the command ran with their generations at the same indices.
 */
interface CoreCommandRecord {
    readonly player: number;
    readonly value: any;
    readonly selection: number[];
    readonly selectionGenerations: number[];
}

/**
 * An order waiting in a unit's queue ({@link CoreUnit.getOrderQueue}): the order, the target unit with its generation (-1 for none or a unit
 * that is gone), the target position and the unit type the order builds or trains (-1 for none).
 */
interface CoreOrderRecord {
    readonly order: number;
    readonly target: number;
    readonly targetGeneration: number;
    readonly x: number;
    readonly y: number;
    readonly unitType: number;
}

// ---------------------------------------------------------------------------------------------------------------------------------------
// The DAT tables: get<Column> [A], set<Column> [S].
// ---------------------------------------------------------------------------------------------------------------------------------------

// BEGIN GENERATED CORE DAT: the DAT columns of ScriptDatTables; ScriptDatTablesTest checks this block.

/**
 * The units.dat values by unit type.
 */
interface CoreDatUnits {
    /** Graphics (below 209) */
    getFlingyType(unitType: number): number;
    setFlingyType(unitType: number, value: number): void;
    /** Subunit1 */
    getFirstSubunit(unitType: number): number;
    setFirstSubunit(unitType: number, value: number): void;
    /** Subunit2 */
    getSecondSubunit(unitType: number): number;
    setSecondSubunit(unitType: number, value: number): void;
    /** Infestation (unit types 106 to 201) */
    getInfestedType(unitType: number): number;
    setInfestedType(unitType: number, value: number): void;
    /** Construction Animation */
    getConstructionImage(unitType: number): number;
    setConstructionImage(unitType: number, value: number): void;
    /** Unit Direction */
    getInitialDirection(unitType: number): number;
    setInitialDirection(unitType: number, value: number): void;
    /** Shield Enable */
    getShieldEnabled(unitType: number): number;
    setShieldEnabled(unitType: number, value: number): void;
    /** Shield Amount */
    getShieldAmount(unitType: number): number;
    setShieldAmount(unitType: number, value: number): void;
    /** Hit Points */
    getHitPoints(unitType: number): number;
    setHitPoints(unitType: number, value: number): void;
    /** Elevation Level */
    getElevationLevel(unitType: number): number;
    setElevationLevel(unitType: number, value: number): void;
    /** Unknown */
    getUnknownByte(unitType: number): number;
    setUnknownByte(unitType: number, value: number): void;
    /** Sublabel */
    getSublabel(unitType: number): number;
    setSublabel(unitType: number, value: number): void;
    /** Comp AI Idle */
    getComputerIdleOrder(unitType: number): number;
    setComputerIdleOrder(unitType: number, value: number): void;
    /** Human AI Idle */
    getHumanIdleOrder(unitType: number): number;
    setHumanIdleOrder(unitType: number, value: number): void;
    /** Return to Idle */
    getReturnToIdleOrder(unitType: number): number;
    setReturnToIdleOrder(unitType: number, value: number): void;
    /** Attack Unit */
    getAttackUnitOrder(unitType: number): number;
    setAttackUnitOrder(unitType: number, value: number): void;
    /** Attack Move */
    getAttackMoveOrder(unitType: number): number;
    setAttackMoveOrder(unitType: number, value: number): void;
    /** Ground Weapon */
    getGroundWeapon(unitType: number): number;
    setGroundWeapon(unitType: number, value: number): void;
    /** Max Ground Hits */
    getMaximumGroundHits(unitType: number): number;
    setMaximumGroundHits(unitType: number, value: number): void;
    /** Air Weapon */
    getAirWeapon(unitType: number): number;
    setAirWeapon(unitType: number, value: number): void;
    /** Max Air Hits */
    getMaximumAirHits(unitType: number): number;
    setMaximumAirHits(unitType: number, value: number): void;
    /** AI Internal */
    getComputerInternalFlags(unitType: number): number;
    setComputerInternalFlags(unitType: number, value: number): void;
    /** Special Ability Flags */
    getFlags(unitType: number): number;
    setFlags(unitType: number, value: number): void;
    /** Target Acquisition Range */
    getTargetAcquisitionRange(unitType: number): number;
    setTargetAcquisitionRange(unitType: number, value: number): void;
    /** Sight Range */
    getSightRange(unitType: number): number;
    setSightRange(unitType: number, value: number): void;
    /** Armor Upgrade */
    getArmorUpgrade(unitType: number): number;
    setArmorUpgrade(unitType: number, value: number): void;
    /** Unit Size */
    getSizeClass(unitType: number): number;
    setSizeClass(unitType: number, value: number): void;
    /** Armor */
    getArmor(unitType: number): number;
    setArmor(unitType: number, value: number): void;
    /** Right-click Action */
    getRightClickAction(unitType: number): number;
    setRightClickAction(unitType: number, value: number): void;
    /** Ready Sound (unit types 0 to 105) */
    getReadySound(unitType: number): number;
    setReadySound(unitType: number, value: number): void;
    /** What Sound Start */
    getWhatSoundStart(unitType: number): number;
    setWhatSoundStart(unitType: number, value: number): void;
    /** What Sound End */
    getWhatSoundEnd(unitType: number): number;
    setWhatSoundEnd(unitType: number, value: number): void;
    /** Piss Sound Start (unit types 0 to 105) */
    getAnnoyedSoundStart(unitType: number): number;
    setAnnoyedSoundStart(unitType: number, value: number): void;
    /** Piss Sound End (unit types 0 to 105) */
    getAnnoyedSoundEnd(unitType: number): number;
    setAnnoyedSoundEnd(unitType: number, value: number): void;
    /** Yes Sound Start (unit types 0 to 105) */
    getYesSoundStart(unitType: number): number;
    setYesSoundStart(unitType: number, value: number): void;
    /** Yes Sound End (unit types 0 to 105) */
    getYesSoundEnd(unitType: number): number;
    setYesSoundEnd(unitType: number, value: number): void;
    /** Placement Box Width */
    getPlacementWidth(unitType: number): number;
    setPlacementWidth(unitType: number, value: number): void;
    /** Placement Box Height */
    getPlacementHeight(unitType: number): number;
    setPlacementHeight(unitType: number, value: number): void;
    /** Addon Horizontal (unit types 106 to 201) */
    getAddonHorizontalOffset(unitType: number): number;
    setAddonHorizontalOffset(unitType: number, value: number): void;
    /** Addon Vertical (unit types 106 to 201) */
    getAddonVerticalOffset(unitType: number): number;
    setAddonVerticalOffset(unitType: number, value: number): void;
    /** Unit Size Left */
    getDimensionLeft(unitType: number): number;
    setDimensionLeft(unitType: number, value: number): void;
    /** Unit Size Up */
    getDimensionUp(unitType: number): number;
    setDimensionUp(unitType: number, value: number): void;
    /** Unit Size Right */
    getDimensionRight(unitType: number): number;
    setDimensionRight(unitType: number, value: number): void;
    /** Unit Size Down */
    getDimensionDown(unitType: number): number;
    setDimensionDown(unitType: number, value: number): void;
    /** Portrait */
    getPortrait(unitType: number): number;
    setPortrait(unitType: number, value: number): void;
    /** Mineral Cost */
    getMineralCost(unitType: number): number;
    setMineralCost(unitType: number, value: number): void;
    /** Vespene Cost */
    getGasCost(unitType: number): number;
    setGasCost(unitType: number, value: number): void;
    /** Build Time */
    getBuildTime(unitType: number): number;
    setBuildTime(unitType: number, value: number): void;
    /** Requirement Index */
    getRequirementIndex(unitType: number): number;
    setRequirementIndex(unitType: number, value: number): void;
    /** StarEdit Group Flags */
    getGroupFlags(unitType: number): number;
    setGroupFlags(unitType: number, value: number): void;
    /** Supply Provided */
    getSupplyProvided(unitType: number): number;
    setSupplyProvided(unitType: number, value: number): void;
    /** Supply Required */
    getSupplyRequired(unitType: number): number;
    setSupplyRequired(unitType: number, value: number): void;
    /** Space Required */
    getSpaceRequired(unitType: number): number;
    setSpaceRequired(unitType: number, value: number): void;
    /** Space Provided */
    getSpaceProvided(unitType: number): number;
    setSpaceProvided(unitType: number, value: number): void;
    /** Build Score */
    getBuildScore(unitType: number): number;
    setBuildScore(unitType: number, value: number): void;
    /** Destroy Score */
    getDestroyScore(unitType: number): number;
    setDestroyScore(unitType: number, value: number): void;
    /** Unit Map String */
    getMapStringIndex(unitType: number): number;
    setMapStringIndex(unitType: number, value: number): void;
    /** Broodwar Unit Flag */
    getBroodWarFlag(unitType: number): number;
    setBroodWarFlag(unitType: number, value: number): void;
    /** StarEdit Availability Flags */
    getAvailabilityFlags(unitType: number): number;
    setAvailabilityFlags(unitType: number, value: number): void;
}

/**
 * The weapons.dat values by weapon type.
 */
interface CoreDatWeapons {
    /** Label */
    getLabel(weaponType: number): number;
    setLabel(weaponType: number, value: number): void;
    /** Sprite */
    getFlingyType(weaponType: number): number;
    setFlingyType(weaponType: number, value: number): void;
    /** Special Attack */
    getSpecialAttack(weaponType: number): number;
    setSpecialAttack(weaponType: number, value: number): void;
    /** Target Flags */
    getTargetFlags(weaponType: number): number;
    setTargetFlags(weaponType: number, value: number): void;
    /** Minimum Range */
    getMinimumRange(weaponType: number): number;
    setMinimumRange(weaponType: number, value: number): void;
    /** Maximum Range */
    getMaximumRange(weaponType: number): number;
    setMaximumRange(weaponType: number, value: number): void;
    /** Upgrade Group */
    getDamageUpgrade(weaponType: number): number;
    setDamageUpgrade(weaponType: number, value: number): void;
    /** Damage Type */
    getDamageType(weaponType: number): number;
    setDamageType(weaponType: number, value: number): void;
    /** Weapon Behavior */
    getBehavior(weaponType: number): number;
    setBehavior(weaponType: number, value: number): void;
    /** Remove After */
    getLifetime(weaponType: number): number;
    setLifetime(weaponType: number, value: number): void;
    /** Explosion Type */
    getExplosionType(weaponType: number): number;
    setExplosionType(weaponType: number, value: number): void;
    /** Inner Splash Radius */
    getInnerSplashRadius(weaponType: number): number;
    setInnerSplashRadius(weaponType: number, value: number): void;
    /** Medium Splash Radius */
    getMediumSplashRadius(weaponType: number): number;
    setMediumSplashRadius(weaponType: number, value: number): void;
    /** Outer Splash Radius */
    getOuterSplashRadius(weaponType: number): number;
    setOuterSplashRadius(weaponType: number, value: number): void;
    /** Damage Amount */
    getDamageAmount(weaponType: number): number;
    setDamageAmount(weaponType: number, value: number): void;
    /** Damage Bonus */
    getDamageBonus(weaponType: number): number;
    setDamageBonus(weaponType: number, value: number): void;
    /** Weapon Cooldown */
    getCooldown(weaponType: number): number;
    setCooldown(weaponType: number, value: number): void;
    /** Damage Factor */
    getDamageFactor(weaponType: number): number;
    setDamageFactor(weaponType: number, value: number): void;
    /** Attack Angle */
    getAttackAngle(weaponType: number): number;
    setAttackAngle(weaponType: number, value: number): void;
    /** Launch Spin */
    getLaunchSpin(weaponType: number): number;
    setLaunchSpin(weaponType: number, value: number): void;
    /** Forward Offset */
    getForwardOffset(weaponType: number): number;
    setForwardOffset(weaponType: number, value: number): void;
    /** Upward Offset */
    getUpwardOffset(weaponType: number): number;
    setUpwardOffset(weaponType: number, value: number): void;
    /** Target Error Message */
    getTargetErrorMessage(weaponType: number): number;
    setTargetErrorMessage(weaponType: number, value: number): void;
    /** Icon */
    getIcon(weaponType: number): number;
    setIcon(weaponType: number, value: number): void;
}

/**
 * The flingy.dat values by flingy type.
 */
interface CoreDatFlingy {
    /** Sprite */
    getSprite(flingyType: number): number;
    setSprite(flingyType: number, value: number): void;
    /** Top Speed */
    getTopSpeed(flingyType: number): number;
    setTopSpeed(flingyType: number, value: number): void;
    /** Acceleration */
    getAcceleration(flingyType: number): number;
    setAcceleration(flingyType: number, value: number): void;
    /** Halt Distance */
    getHaltDistance(flingyType: number): number;
    setHaltDistance(flingyType: number, value: number): void;
    /** Turn Rate */
    getTurnRate(flingyType: number): number;
    setTurnRate(flingyType: number, value: number): void;
    /** Unused */
    getUnused(flingyType: number): number;
    setUnused(flingyType: number, value: number): void;
    /** Movement Control */
    getMovementControl(flingyType: number): number;
    setMovementControl(flingyType: number, value: number): void;
}

/**
 * The sprites.dat values by sprite type.
 */
interface CoreDatSprites {
    /** Image (below 999) */
    getImage(spriteType: number): number;
    setImage(spriteType: number, value: number): void;
    /** Health Bar (sprite types 130 to 516, read only) */
    getHealthBarWidth(spriteType: number): number;
    /** Unused */
    getUnused(spriteType: number): number;
    setUnused(spriteType: number, value: number): void;
    /** Is Visible */
    getVisible(spriteType: number): number;
    setVisible(spriteType: number, value: number): void;
    /** Selection Circle (sprite types 130 to 516, read only) */
    getSelectionCircle(spriteType: number): number;
    /** Y Position (sprite types 130 to 516, read only) */
    getSelectionCircleOffset(spriteType: number): number;
}

/**
 * The images.dat values by image type.
 */
interface CoreDatImages {
    /** GRP File (read only) */
    getGraphicsFile(imageType: number): number;
    /** Graphic Turns */
    getGraphicTurns(imageType: number): number;
    setGraphicTurns(imageType: number, value: number): void;
    /** Clickable */
    getClickable(imageType: number): number;
    setClickable(imageType: number, value: number): void;
    /** Use Full Iscript */
    getFullIscript(imageType: number): number;
    setFullIscript(imageType: number, value: number): void;
    /** Draw If Cloaked */
    getDrawIfCloaked(imageType: number): number;
    setDrawIfCloaked(imageType: number, value: number): void;
    /** Draw Function (below 18) */
    getDrawFunction(imageType: number): number;
    setDrawFunction(imageType: number, value: number): void;
    /** Color Shift */
    getColorShift(imageType: number): number;
    setColorShift(imageType: number, value: number): void;
    /** Iscript (below 411) */
    getIscript(imageType: number): number;
    setIscript(imageType: number, value: number): void;
    /** Shield Overlay (read only) */
    getShieldOverlay(imageType: number): number;
    /** Attack Overlay (read only) */
    getAttackOverlay(imageType: number): number;
    /** Damage Overlay (read only) */
    getDamageOverlay(imageType: number): number;
    /** Special Overlay (read only) */
    getSpecialOverlay(imageType: number): number;
    /** Landing Dust Overlay (read only) */
    getLandingDustOverlay(imageType: number): number;
    /** Liftoff Dust Overlay (read only) */
    getLiftoffDustOverlay(imageType: number): number;
}

/**
 * The upgrades.dat values by upgrade type.
 */
interface CoreDatUpgrades {
    /** Ore Cost */
    getMineralCost(upgradeType: number): number;
    setMineralCost(upgradeType: number, value: number): void;
    /** Ore Cost Factor */
    getMineralCostFactor(upgradeType: number): number;
    setMineralCostFactor(upgradeType: number, value: number): void;
    /** Gas Cost */
    getGasCost(upgradeType: number): number;
    setGasCost(upgradeType: number, value: number): void;
    /** Gas Cost Factor */
    getGasCostFactor(upgradeType: number): number;
    setGasCostFactor(upgradeType: number, value: number): void;
    /** Time Cost */
    getResearchTime(upgradeType: number): number;
    setResearchTime(upgradeType: number, value: number): void;
    /** Time Cost Factor */
    getResearchTimeFactor(upgradeType: number): number;
    setResearchTimeFactor(upgradeType: number, value: number): void;
    /** DatReqs Index */
    getRequirementIndex(upgradeType: number): number;
    setRequirementIndex(upgradeType: number, value: number): void;
    /** Icon */
    getIcon(upgradeType: number): number;
    setIcon(upgradeType: number, value: number): void;
    /** Label */
    getLabel(upgradeType: number): number;
    setLabel(upgradeType: number, value: number): void;
    /** Race */
    getRace(upgradeType: number): number;
    setRace(upgradeType: number, value: number): void;
    /** Max Repeats */
    getMaximumLevel(upgradeType: number): number;
    setMaximumLevel(upgradeType: number, value: number): void;
    /** BroodWar */
    getBroodWarFlag(upgradeType: number): number;
    setBroodWarFlag(upgradeType: number, value: number): void;
}

/**
 * The techdata.dat values by tech type.
 */
interface CoreDatTechdata {
    /** Ore Cost */
    getMineralCost(techType: number): number;
    setMineralCost(techType: number, value: number): void;
    /** Gas Cost */
    getGasCost(techType: number): number;
    setGasCost(techType: number, value: number): void;
    /** Time Cost */
    getResearchTime(techType: number): number;
    setResearchTime(techType: number, value: number): void;
    /** Energy */
    getEnergyCost(techType: number): number;
    setEnergyCost(techType: number, value: number): void;
    /** DatReqs Index */
    getResearchRequirementIndex(techType: number): number;
    setResearchRequirementIndex(techType: number, value: number): void;
    /** UseReqs Index */
    getUseRequirementIndex(techType: number): number;
    setUseRequirementIndex(techType: number, value: number): void;
    /** Icon */
    getIcon(techType: number): number;
    setIcon(techType: number, value: number): void;
    /** Label */
    getLabel(techType: number): number;
    setLabel(techType: number, value: number): void;
    /** Race */
    getRace(techType: number): number;
    setRace(techType: number, value: number): void;
    /** Researched */
    getResearchedFlag(techType: number): number;
    setResearchedFlag(techType: number, value: number): void;
    /** BroodWar */
    getBroodWarFlag(techType: number): number;
    setBroodWarFlag(techType: number, value: number): void;
}

/**
 * The orders.dat values by order.
 */
interface CoreDatOrders {
    /** Label */
    getLabel(order: number): number;
    setLabel(order: number, value: number): void;
    /** Use Weapon Targeting */
    getUseWeaponTargeting(order: number): number;
    setUseWeaponTargeting(order: number, value: number): void;
    /** Unused1 */
    getUnused1(order: number): number;
    setUnused1(order: number, value: number): void;
    /** Unused2 */
    getUnused2(order: number): number;
    setUnused2(order: number, value: number): void;
    /** Subunit Inheritance */
    getSubunitInheritance(order: number): number;
    setSubunitInheritance(order: number, value: number): void;
    /** Unused3 */
    getUnused3(order: number): number;
    setUnused3(order: number, value: number): void;
    /** Can Be Interrupted */
    getCanBeInterrupted(order: number): number;
    setCanBeInterrupted(order: number, value: number): void;
    /** Can Be Lifted */
    getCanBeLifted(order: number): number;
    setCanBeLifted(order: number, value: number): void;
    /** Can Be Queued */
    getCanBeQueued(order: number): number;
    setCanBeQueued(order: number, value: number): void;
    /** Disabling Keeps Target */
    getDisablingKeepsTarget(order: number): number;
    setDisablingKeepsTarget(order: number, value: number): void;
    /** Can Be Obstructed */
    getCanBeObstructed(order: number): number;
    setCanBeObstructed(order: number, value: number): void;
    /** Fleeable */
    getFleeable(order: number): number;
    setFleeable(order: number, value: number): void;
    /** Unused4 */
    getUnused4(order: number): number;
    setUnused4(order: number, value: number): void;
    /** Weapon */
    getWeapon(order: number): number;
    setWeapon(order: number, value: number): void;
    /** Energy */
    getTechnology(order: number): number;
    setTechnology(order: number, value: number): void;
    /** Animation */
    getAnimation(order: number): number;
    setAnimation(order: number, value: number): void;
    /** Highlight */
    getHighlight(order: number): number;
    setHighlight(order: number, value: number): void;
    /** DatReqs Index */
    getRequirementIndex(order: number): number;
    setRequirementIndex(order: number, value: number): void;
    /** Obscured */
    getObscuredOrder(order: number): number;
    setObscuredOrder(order: number, value: number): void;
}

/**
 * The DAT tables of the current game; a write changes the game as an EUD write of the value does.
 */
interface CoreDat {
    readonly units: CoreDatUnits;
    readonly weapons: CoreDatWeapons;
    readonly flingy: CoreDatFlingy;
    readonly sprites: CoreDatSprites;
    readonly images: CoreDatImages;
    readonly upgrades: CoreDatUpgrades;
    readonly techdata: CoreDatTechdata;
    readonly orders: CoreDatOrders;
}
// END GENERATED CORE DAT

// ---------------------------------------------------------------------------------------------------------------------------------------
// The namespaces.
// ---------------------------------------------------------------------------------------------------------------------------------------

interface CoreGame {
    /** [A] */
    getFrame(): number;
    /** [A] */
    getMapTileWidth(): number;
    /** [A] */
    getMapTileHeight(): number;
    /** [A] */
    getTileset(): number;
    /**
     * [A]
     * @returns the milliseconds of a frame at the fastest speed
     */
    getFrameDuration(): number;
    /** [S] */
    print(text: string): void;
    /** [A] */
    isReadOnly(): boolean;
    /** [A] */
    isSwitchSet(index: number): boolean;
    /** [S] */
    setSwitch(index: number, set: boolean): void;
    /** [S] */
    toggleSwitch(index: number): void;
    /** [A] */
    getCountdownTimer(): number;
    /** [S] */
    setCountdownTimer(seconds: number): void;
    /** [S] */
    addCountdownTimer(seconds: number): void;
    /** [S] */
    subtractCountdownTimer(seconds: number): void;
    /** [A] */
    isCountdownTimerPaused(): boolean;
    /** [S] */
    setCountdownTimerPaused(paused: boolean): void;
    /** [A] */
    getElapsedTime(): number;
    /**
     * [A]
     * @returns the speed, 0 slowest to 6 fastest
     */
    getSpeed(): number;
    /**
     * [A]
     * @returns the milliseconds of a frame at a speed in the frame duration table
     */
    getSpeedDuration(speed: number): number;
    /**
     * [S] Stores an entry of the frame duration table as an EUD write does: the frame duration takes it at once unless the game name
     * overrides the speed; the turn timeouts of a network game keep the durations of their last fill.
     */
    setSpeedDuration(speed: number, milliseconds: number): void;
    /** [A] */
    isMultiplayer(): boolean;
    /**
     * [A]
     * @returns the game type WORD of the game header ({@link GameMode})
     */
    getGameType(): number;
    /** [A] */
    getGameSubtype(): number;
    /** [A] */
    getVictoryCondition(): number;
    /** [A] */
    getResourceType(): number;
    /** [A] */
    getStartingMinerals(): number;
    /** [A] */
    getStartingUnits(): number;
    /** [A] */
    getTournamentMode(): number;
    /**
     * [A]
     * @returns whether the game options permit the cheat command
     */
    areCheatsEnabled(): boolean;
    /**
     * [A]
     * @returns the expansion byte, 0 for StarCraft
     */
    getExpansion(): number;
    /** [S] */
    setExpansion(value: number): void;
    /**
     * [A]
     * @returns the cheat flags ({@link Cheat}), one bit per cheat
     */
    getCheatFlags(): number;
    /**
     * [S] Stores the cheat flags as an EUD write does, without the reveal update and the one-shot grants of the cheat routine.
     */
    setCheatFlags(flags: number): void;
    /**
     * [S] Runs the cheat routine with the flags: nothing when the game options forbid cheats or more than one human plays a network game.
     */
    applyCheats(flags: number): void;
}

interface CorePlayer {
    /** [A] */
    getCount(): number;
    /** [A] */
    getName(player: number): string;
    /** [A] */
    getRace(player: number): number;
    /** [A] */
    getType(player: number): number;
    /** [A] */
    isHuman(player: number): boolean;
    /** [A] */
    getMinerals(player: number): number;
    /** [S] */
    setMinerals(player: number, amount: number): void;
    /** [S] */
    addMinerals(player: number, amount: number): void;
    /** [S] */
    subtractMinerals(player: number, amount: number): void;
    /** [A] */
    getGas(player: number): number;
    /** [S] */
    setGas(player: number, amount: number): void;
    /** [S] */
    addGas(player: number, amount: number): void;
    /** [S] */
    subtractGas(player: number, amount: number): void;
    /**
     * [A]
     * @param resourceType 0 minerals, 1 gas, 2 minerals and gas
     */
    getResources(player: number, resourceType: number): number;
    /** [A] */
    getGatheredMinerals(player: number): number;
    /** [S] */
    setGatheredMinerals(player: number, amount: number): void;
    /** [A] */
    getGatheredGas(player: number): number;
    /** [S] */
    setGatheredGas(player: number, amount: number): void;
    /** [A] */
    getSupplyUsed(player: number, race: number): number;
    /** [A] */
    getSupplyProvided(player: number, race: number): number;
    /** [A] */
    getSupplyMax(player: number, race: number): number;
    /**
     * [A]
     * @returns 0 enemy, 1 allied, 2 allied victory
     */
    getAlliance(player: number, other: number): number;
    /** [S] */
    setAlliance(player: number, other: number, alliance: number): void;
    /** [S] */
    endGame(player: number, result: number): boolean;
    /** [A] */
    getDeaths(player: number, unitType: number): number;
    /** [S] */
    setDeaths(player: number, unitType: number, amount: number): void;
    /** [S] */
    addDeaths(player: number, unitType: number, amount: number): void;
    /** [S] */
    subtractDeaths(player: number, unitType: number, amount: number): void;
    /** [A] */
    getKills(player: number, unitType: number): number;
    /** [A] */
    getScore(player: number, scoreType: number): number;
    /** [S] */
    setScore(player: number, scoreType: number, amount: number): void;
    /** [S] */
    addScore(player: number, scoreType: number, amount: number): void;
    /** [S] */
    subtractScore(player: number, scoreType: number, amount: number): void;
    /** [A] */
    countUnits(player: number, unitType: number, completed: boolean): number;
    /** [A] */
    countUnitsAt(player: number, unitType: number, location: number, completed: boolean): number;
    /** [A] */
    countOpponents(player: number): number;
    /**
     * [S]
     * @param location the location, -1 for everywhere
     * @param count    the most units, 0 for all
     */
    killUnits(player: number, unitType: number, location: number, count: number): void;
    /** [S] */
    removeUnits(player: number, unitType: number, location: number, count: number): void;
    /** [S] */
    runAiScript(player: number, script: number): boolean;
    /** [S] */
    runAiScriptAt(player: number, script: number, location: number): boolean;
    /** [S] */
    setMissionObjectives(player: number, text: string): void;
    /** [S] */
    print(player: number, text: string, forced: boolean): void;
    /** [S] */
    pingMinimap(player: number, x: number, y: number): void;
    /** [S] */
    playSound(player: number, path: string): void;
    /** [S] */
    showTalkingPortrait(player: number, unitType: number, milliseconds: number): void;
    /** [S] */
    setUnitSpeechMuted(player: number, muted: boolean): void;
    /** [S] */
    centerView(player: number, x: number, y: number): void;
    /** [A] */
    getUpgradeLevel(player: number, upgradeType: number): number;
    /** [S] */
    setUpgradeLevel(player: number, upgradeType: number, level: number): void;
    /** [A] */
    getMaxUpgradeLevel(player: number, upgradeType: number): number;
    /** [S] */
    setMaxUpgradeLevel(player: number, upgradeType: number, level: number): void;
    /** [A] */
    isUpgradeInProgress(player: number, upgradeType: number): boolean;
    /** [A] */
    isTechResearched(player: number, techType: number): boolean;
    /** [S] */
    setTechResearched(player: number, techType: number, researched: boolean): void;
    /** [A] */
    isTechAvailable(player: number, techType: number): boolean;
    /** [S] */
    setTechAvailable(player: number, techType: number, available: boolean): void;
    /** [A] */
    isTechInProgress(player: number, techType: number): boolean;
    /** [A] */
    isUnitAvailable(player: number, unitType: number): boolean;
    /** [S] */
    setUnitAvailable(player: number, unitType: number, available: boolean): void;
    /**
     * [A]
     * @returns the ids of the live units of the player's selection, empty for players 8 to 11
     */
    getSelection(player: number): number[];
    /**
     * [S] Replaces the player's selection as a selection command of the player does ({@link Player.setSelection}).
     *
     * @param units at most 12 ids of live units
     * @returns the number of selected units
     */
    setSelection(player: number, units: number[]): number;
    /**
     * [A]
     * @returns the players that see what the player's units see, one bit per player, the player's own bit included
     */
    getSharedVision(player: number): number;
    /**
     * [S] Sets the players that see what the player's units see, as the vision command does but without its permission test.
     */
    setSharedVision(player: number, mask: number): void;
    /**
     * [A]
     * @param player a player from 0 to 7
     */
    getPausesLeft(player: number): number;
    /** [S] */
    setPausesLeft(player: number, pauses: number): void;
    /**
     * [A]
     * @returns the x of the player's start location, -1 for a player without one
     */
    getStartLocationX(player: number): number;
    /**
     * [A]
     * @returns the y of the player's start location, -1 for a player without one
     */
    getStartLocationY(player: number): number;
    /**
     * [S]
     * @returns false for a player without a start location
     */
    setStartLocation(player: number, x: number, y: number): boolean;
}

interface CoreUnit {
    /** [A] */
    getCapacity(): number;
    /** [A] */
    getIds(): number[];
    /** [A] */
    exists(unit: number): boolean;
    /** [A] */
    getGeneration(unit: number): number;
    /** [A] */
    getType(unit: number): number;
    /** [A] */
    getOwner(unit: number): number;
    /** [A] */
    getX(unit: number): number;
    /** [A] */
    getY(unit: number): number;
    /** [A] */
    getHp(unit: number): number;
    /** [A] */
    getShields(unit: number): number;
    /** [A] */
    getEnergy(unit: number): number;
    /**
     * [A]
     * @returns the maximum energy after the energy upgrades of the unit's owner, 0 for a unit without energy
     */
    getMaxEnergy(unit: number): number;
    /** [A] */
    getOrder(unit: number): number;
    /**
     * [A]
     * @returns the target unit, -1 for none
     */
    getTarget(unit: number): number;
    /** [A] */
    getTargetX(unit: number): number;
    /** [A] */
    getTargetY(unit: number): number;
    /** [A] */
    getStatus(unit: number): number;
    /** [A] */
    getImageType(unit: number): number;
    /** [A] */
    getResources(unit: number): number;
    /** [A] */
    getHangarCount(unit: number): number;
    /** [A] */
    isCompleted(unit: number): boolean;
    /** [A] */
    isCloaked(unit: number): boolean;
    /** [A] */
    isBurrowed(unit: number): boolean;
    /** [A] */
    isInvincible(unit: number): boolean;
    /** [A] */
    isDisabled(unit: number): boolean;
    /** [S] */
    setHp(unit: number, hp: number): void;
    /** [S] */
    setShields(unit: number, shields: number): boolean;
    /** [S] */
    setEnergy(unit: number, energy: number): boolean;
    /** [S] */
    setHpPercent(unit: number, percent: number): void;
    /** [S] */
    setShieldsPercent(unit: number, percent: number): boolean;
    /** [S] */
    setEnergyPercent(unit: number, percent: number): boolean;
    /** [S] */
    setInvincible(unit: number, invincible: boolean): void;
    /** [S] */
    enableDoodad(unit: number): void;
    /** [S] */
    disableDoodad(unit: number): void;
    /** [S] */
    setResources(unit: number, amount: number): boolean;
    /** [S] */
    addHangarCount(unit: number, count: number): boolean;
    /**
     * [S]
     * @returns the new unit, -1 for none
     */
    create(unitType: number, owner: number, x: number, y: number): number;
    /** [S] */
    kill(unit: number): boolean;
    /** [S] */
    remove(unit: number): boolean;
    /** [S] */
    move(unit: number, x: number, y: number): boolean;
    /** [S] */
    give(unit: number, player: number): boolean;
    /**
     * [S] Issues an order: not queued to a position as the Order action does; queued or to a target unit through the order system after the
     * order requirement test, which appends a queued order behind the queue when the order takes one.
     *
     * @param target the target unit, -1 for the position
     */
    issueOrder(unit: number, order: number, target: number, x: number, y: number, queued: boolean): void;
    /**
     * [A]
     * @returns the orders waiting behind the current one, first to last
     */
    getOrderQueue(unit: number): CoreOrderRecord[];
    /** [A] */
    getSecondaryOrder(unit: number): number;
    /**
     * [A]
     * @returns the WORD of the unit's command card button set ({@link CoreButtons})
     */
    getButtonSet(unit: number): number;
    /** [S] Sets the button set WORD; the native routines of morphs, completion and burrowing set it again. */
    setButtonSet(unit: number, set: number): void;
    /** [A] */
    getSecondaryOrderState(unit: number): number;
    /**
     * [S] Sets the secondary order as the native routine does: a changed order clears its state and the unit in production.
     */
    setSecondaryOrder(unit: number, order: number): void;
    /**
     * [A]
     * @returns the hit points in 1/256 a unit under construction gains each frame
     */
    getHpGain(unit: number): number;
    /** [S] */
    setHpGain(unit: number, hp: number): void;
    /**
     * [A]
     * @returns the shields in 1/256 a unit under construction gains each frame
     */
    getShieldGain(unit: number): number;
    /** [S] */
    setShieldGain(unit: number, shields: number): void;
    /**
     * [A]
     * @returns the whole hit points the unit had before it morphed
     */
    getPreviousHp(unit: number): number;
    /**
     * [A]
     * @returns the players whose sight covers the unit, one bit per player
     */
    getVisibilityFlags(unit: number): number;
    /**
     * [A]
     * @returns the players that detect the unit, one bit per player
     */
    getDetectionFlags(unit: number): number;
    /** [A] */
    isVisibleTo(unit: number, player: number): boolean;
    /** [A] */
    isDetectedBy(unit: number, player: number): boolean;
    /** [A] */
    getAirStrength(unit: number): number;
    /** [A] */
    getGroundStrength(unit: number): number;
    /**
     * [S] The unit finder's search, which uses the finder's working state.
     *
     * @returns the live units whose bounds meet the rectangle, in the finder's order
     */
    findInRectangle(left: number, top: number, right: number, bottom: number): number[];
    /**
     * [S]
     * @returns the live units whose bounds hold the point, in the finder's order
     */
    findAtPoint(x: number, y: number): number[];
    /**
     * [A] The status timers in status steps of eight frames, with the native apply and end routines as {@link UnitTimers.setLockdown} has them.
     */
    getStimTimer(unit: number): number;
    /** [S] */
    setStimTimer(unit: number, timer: number): void;
    /** [A] */
    getEnsnareTimer(unit: number): number;
    /** [S] */
    setEnsnareTimer(unit: number, timer: number): void;
    /** [A] */
    getLockdownTimer(unit: number): number;
    /** [S] */
    setLockdownTimer(unit: number, timer: number): void;
    /** [A] */
    getStasisTimer(unit: number): number;
    /** [S] */
    setStasisTimer(unit: number, timer: number): void;
    /** [A] */
    getMaelstromTimer(unit: number): number;
    /** [S] */
    setMaelstromTimer(unit: number, timer: number): void;
    /** [A] */
    getPlagueTimer(unit: number): number;
    /** [S] */
    setPlagueTimer(unit: number, timer: number): void;
    /** [A] */
    getIrradiateTimer(unit: number): number;
    /** [S] */
    setIrradiateTimer(unit: number, timer: number): void;
    /** [A] */
    getStormTimer(unit: number): number;
    /** [S] */
    setStormTimer(unit: number, timer: number): void;
    /** [A] */
    getDefensiveMatrixTimer(unit: number): number;
    /** [S] */
    setDefensiveMatrixTimer(unit: number, timer: number): void;
    /** [A] */
    getDefensiveMatrixHp(unit: number): number;
    /** [S] */
    setDefensiveMatrixHp(unit: number, hp: number): void;
    /** [A] */
    getAcidSporeCount(unit: number): number;
    /** [S] */
    setAcidSporeCount(unit: number, count: number): void;
    /** [A] */
    getRemoveTimer(unit: number): number;
    /** [S] */
    setRemoveTimer(unit: number, frames: number): void;
    /**
     * [A]
     * @returns the players whose Parasite is on the unit, one bit per player
     */
    getParasiteFlags(unit: number): number;
    /** [S] */
    setParasiteFlags(unit: number, flags: number): void;
    /**
     * [A]
     * @returns the players whose Optical Flare blinded the unit, one bit per player
     */
    getBlindFlags(unit: number): number;
    /** [S] */
    setBlindFlags(unit: number, flags: number): void;
    /** [A] */
    getGroundCooldown(unit: number): number;
    /** [S] */
    setGroundCooldown(unit: number, frames: number): void;
    /** [A] */
    getAirCooldown(unit: number): number;
    /** [S] */
    setAirCooldown(unit: number, frames: number): void;
    /** [A] */
    getSpellCooldown(unit: number): number;
    /** [S] */
    setSpellCooldown(unit: number, frames: number): void;
    /** [A] */
    getKillCount(unit: number): number;
    /** [S] */
    setKillCount(unit: number, kills: number): void;
    /** [A] */
    getBuildQueue(unit: number): number[];
    /** [A] */
    getRemainingBuildTime(unit: number): number;
    /** [S] */
    setRemainingBuildTime(unit: number, frames: number): void;
    /** [A] */
    getRemainingResearchTime(unit: number): number;
    /** [S] */
    setRemainingResearchTime(unit: number, frames: number): void;
    /**
     * [A]
     * @returns the technology in progress, -1 for none
     */
    getResearch(unit: number): number;
    /**
     * [A]
     * @returns the upgrade in progress, -1 for none
     */
    getUpgrade(unit: number): number;
    /**
     * [A]
     * @returns the add-on, -1 for none
     */
    getAddon(unit: number): number;
    /** [A] */
    getRallyX(unit: number): number;
    /** [A] */
    getRallyY(unit: number): number;
    /**
     * [A]
     * @returns the rally unit, -1 for none
     */
    getRallyUnit(unit: number): number;
    /** [S] */
    setRallyPosition(unit: number, x: number, y: number): boolean;
    /**
     * [S]
     * @param target the rally unit, -1 for none
     */
    setRallyUnit(unit: number, target: number): boolean;
    /** [A] */
    getDirection(unit: number): number;
    /** [S] */
    setDirection(unit: number, direction: number): void;
    /** [A] */
    getCurrentSpeed(unit: number): number;
    /** [A] */
    getTopSpeed(unit: number): number;
    /** [A] */
    getMoveTargetX(unit: number): number;
    /** [A] */
    getMoveTargetY(unit: number): number;
    /** [S] */
    setMoveTarget(unit: number, x: number, y: number): void;
    /** [S] */
    setType(unit: number, unitType: number): boolean;
}

interface CoreLocation {
    /** [A] */
    getCount(): number;
    /** [A] */
    getName(location: number): string | null;
    /**
     * [A]
     * @returns the first location with the name, -1 for none
     */
    findByName(name: string): number;
    /** [A] */
    getLeft(location: number): number;
    /** [A] */
    getTop(location: number): number;
    /** [A] */
    getRight(location: number): number;
    /** [A] */
    getBottom(location: number): number;
    /** [A] */
    getCenterX(location: number): number;
    /** [A] */
    getCenterY(location: number): number;
    /** [S] */
    setBounds(location: number, left: number, top: number, right: number, bottom: number): void;
    /** [S] */
    setCenter(location: number, x: number, y: number): void;
    /**
     * [A]
     * @returns the elevation flags ({@link LocationFlag}): a set bit leaves the units at its elevation out of the location
     */
    getFlags(location: number): number;
    /** [S] */
    setFlags(location: number, flags: number): void;
}

/**
 * The tiles of the map as MTXM values, group << 4 | index, by tile coordinates.
 */
interface CoreTerrain {
    /** [A] */
    getTile(tileX: number, tileY: number): number;
    /**
     * [A]
     * @returns the flag DWORD the game holds for the tile: sight, walkability, creep, elevation and occupation
     */
    getTileFlags(tileX: number, tileY: number): number;
    /**
     * [S] Queues the change, which applies at the next trigger step, after the frame start hooks and before the triggers.
     */
    setTile(tileX: number, tileY: number, value: number): void;
    /** [A] */
    getTileGroupCount(): number;
    /** [A] */
    isValidTile(value: number): boolean;
    /** [A] */
    isTileWalkable(value: number): boolean;
    /** [A] */
    isTilePartiallyWalkable(value: number): boolean;
    /** [A] */
    isTileBuildable(value: number): boolean;
    /** [A] */
    hasTileCreep(value: number): boolean;
    /** [A] */
    getTileElevation(value: number): number;
    /** [A] */
    isTileBlockingSight(value: number): boolean;
}

/**
 * The sight of the players on the map.
 */
interface CoreVision {
    /**
     * [A]
     * @returns bit p set while player p does not see the tile, bit 8 + p while player p has not explored it
     */
    getTileVisibility(tileX: number, tileY: number): number;
    /** [A] */
    isVisible(player: number, x: number, y: number): boolean;
    /** [A] */
    isExplored(player: number, x: number, y: number): boolean;
    /**
     * [S] Runs a unit's sight routine for the players of the mask (0 to 0xFF) with a sight range from 0 to 11; the tiles stay seen until the
     * game marks them unseen again.
     */
    reveal(players: number, x: number, y: number, sightRange: number, air: boolean): void;
}

/**
 * The combat tables that EUD writes change.
 */
interface CoreCombat {
    /**
     * [A]
     * @param damageType 0 independent, 1 explosive, 2 concussive, 3 normal, 4 ignore armor
     * @param size       0 independent, 1 small, 2 medium, 3 large
     * @returns the factor in 1/256
     */
    getDamageMultiplier(damageType: number, size: number): number;
    /** [S] */
    setDamageMultiplier(damageType: number, size: number, value: number): void;
    /** [A] */
    getFleeDistanceCount(): number;
    /** [A] */
    getFleeDistance(index: number): number;
    /** [S] */
    setFleeDistance(index: number, pixels: number): void;
}

/**
 * The requirement scripts and the native requirement tests. The scripts by kind: 0 orders, 1 technology use, 2 research, 3 upgrades, 4 unit
 * production, each an array of WORDs (0xFF01 or, 0xFF02 the unit is, ... 0xFFFF end). A script keeps its length. The DAT requirement index
 * columns (core.dat) keep the start positions their first test built; a column entry set to 0xFFFF makes the next test of the kind build
 * the column again.
 */
interface CoreRequirements {
    /** [A] */
    getScriptLength(kind: number): number;
    /** [A] */
    getScriptWord(kind: number, position: number): number;
    /** [S] */
    setScriptWord(kind: number, position: number, word: number): void;
    /**
     * [A] Runs the native order test for a live unit and a player.
     * @returns 0 when the requirements hold, else the failure code in the low byte, with 0x10000 set when the command card shows the button
     *          disabled rather than hidden
     */
    checkOrder(unit: number, order: number, player: number): number;
    /** [A] As {@link checkOrder}. */
    checkTechnologyUse(unit: number, tech: number, player: number): number;
    /** [A] As {@link checkOrder}. */
    checkResearch(unit: number, tech: number, player: number): number;
    /** [A] As {@link checkOrder}. */
    checkUpgrade(unit: number, upgrade: number, player: number): number;
    /** [A] As {@link checkOrder}. */
    checkBuild(unit: number, unitType: number, player: number): number;
}

/**
 * The state of the computer players.
 */
interface CoreAi {
    /**
     * [A]
     * @returns the strength of the unit type against air or ground targets
     */
    getUnitStrength(unitType: number, air: boolean): number;
    /** [S] */
    setUnitStrength(unitType: number, air: boolean, value: number): void;
}

interface CoreLeaderboard {
    /** [S] */
    show(type: number, operand: number, goal: number, label: string): void;
    /** [S] */
    showAt(type: number, location: number, unitType: number, goal: number, label: string): void;
    /** [S] */
    showGreed(goal: number): void;
    /** [A] */
    areComputersIncluded(): boolean;
    /** [S] */
    setComputersIncluded(included: boolean): void;
}

interface CoreTypes {
    /** [A] */
    getCount(): number;
    /** [A] */
    getName(id: number): string;
}

interface CoreImageTypes extends CoreTypes {
    /**
     * [A]
     * @returns the image type whose HD anim the image type draws (images.rel), the image type itself for one with its own anim
     */
    getAnimImageType(imageType: number): number;
    /**
     * [A]
     * @returns the damage overlay byte: twice the flame positions of the image's damage overlay
     */
    getDamageOverlayCount(imageType: number): number;
    /** [S] */
    setDamageOverlayCount(imageType: number, count: number): void;
}

interface CoreResources {
    /** [A] */
    has(name: string): boolean;
    /**
     * [A]
     * @returns the resource handle
     */
    getGrp(name: string): number;
    /** [A] */
    getAnim(name: string): number;
    /** [A] */
    getImage(name: string): number;
    /** [A] */
    getImageWidth(image: number): number;
    /** [A] */
    getImageHeight(image: number): number;
}

interface CoreGraphics {
    /**
     * [A]
     * @param grp     the GRP handle, -1 for none
     * @param hdAnim  the HD anim handle, -1 for none
     * @param hd2Anim the HD2 anim handle, -1 for none
     */
    replaceImage(imageType: number, grp: number, hdAnim: number, hd2Anim: number): void;
    /** [A] */
    restoreImage(imageType: number): void;
    /** [A] */
    replaceUnitImage(unit: number, imageType: number, grp: number, hdAnim: number, hd2Anim: number): void;
    /** [A] */
    restoreUnitImage(unit: number, imageType: number): void;
}

/**
 * What happened in the simulation, delivered at the frame start after it happened.
 */
interface CoreEvents {
    /**
     * [S]
     * @returns a new array of the records of the frame's events, in the order they happened; every call of a frame returns the same records
     */
    getFrameEvents(): CoreEventRecord[];
    /**
     * [A]
     * @returns the number of event kinds
     */
    getKindCount(): number;
    /**
     * [A]
     * @returns the name of an event kind, that of {@link EventKind}
     */
    getKindName(kind: number): string;
}

/**
 * The script commands: mod commands that carry one value from a peer to the scripts of every peer.
 */
interface CoreCommand {
    /**
     * [A] Submits a value in the local player's next outgoing turn that has room: null, a boolean, a number, a string, an array or a plain object
     * of these; anything else throws a TypeError. A replay and an observer submit nothing; shared code submits once from every human peer.
     */
    submit(value: any): void;
    /**
     * [S]
     * @returns new records of the script commands that ran since the previous frame start, in the order they ran, each with a new copy of
     *          its value
     */
    getExecuted(): CoreCommandRecord[];
}

/**
 * The local information ({@link Local}), readable only in local code.
 */
interface CoreLocal {
    /**
     * [L]
     * @returns the local player's id, -1 in a replay and for an observer
     */
    getPlayer(): number;
    /** [L] */
    isReplay(): boolean;
    /** [L] */
    isChatOpen(): boolean;
    /**
     * [L]
     * @returns true once the client supplied the screen and pointer positions
     */
    isViewKnown(): boolean;
    /** [L] */
    getScreenX(): number;
    /** [L] */
    getScreenY(): number;
    /**
     * [L]
     * @returns the map X under the pointer
     */
    getMouseX(): number;
    /** [L] */
    getMouseY(): number;
    /**
     * [L]
     * @returns false while the screen is unknown
     */
    setScreenPosition(x: number, y: number): boolean;
    /** [L] */
    getSelection(): number[];
    /**
     * [L]
     * @returns a new array of the records of the local inputs that reached the game before the current local frame
     */
    getInputEvents(): CoreInputRecord[];
    /**
     * [L]
     * @returns the local state the Script button condition gives for a condition value (0 to 255): 1 enabled, 0 hidden, -1 disabled; every
     *          state starts -1
     */
    getButtonState(conditionValue: number): number;
    /** [L] */
    setButtonState(conditionValue: number, state: number): void;
    /**
     * [L]
     * @returns the button set the local command card shows, 0xE4 for none
     */
    getDisplayedButtonSet(): number;
    /** [L] Shows a button set at the card's next refresh, as the Change Buttons action does, until the selection changes. */
    changeButtons(set: number): void;
}

/**
 * The command card's button set table, sets 0 to 249 (the unit types' sets are their ids), each an array of buttons in the order the card tries
 * them for each position, and a connected set. Every function is [A]: the table is presentation, so a change in shared code changes every
 * peer's card and a change in local code only the local card. Sets may share one array (getArray names it by its first set); a change of a
 * button or of the array changes every set that shares it. An EUD write of the table rebuilds every set and so replaces earlier changes.
 * Condition and action ids follow SC:R's lists (getConditionName, getActionName); the last condition, Script, gives the local state of its
 * condition value ({@link CoreLocal.setButtonState}), and the last actions, Script and ScriptTarget, send no command: the press, or the
 * target of the targeting mode, reaches the local input hooks only. A text replaces a string: the enabled text in the native button string
 * format (hotkey character, button kind character, then the text) and the plain disabled text, both with line breaks and the text color
 * control characters allowed; null shows the string.
 */
interface CoreButtons {
    /** [A] */
    getSetCount(): number;
    /** [A] */
    getButtonCount(set: number): number;
    /**
     * [A]
     * @returns the connected set WORD, 0xFFFF for none
     */
    getConnectedSet(set: number): number;
    /** [A] */
    setConnectedSet(set: number, connected: number): void;
    /**
     * [A]
     * @returns the first set that holds the set's array
     */
    getArray(set: number): number;
    /** [A] The set takes the array of fromSet. */
    share(set: number, fromSet: number): void;
    /** [A] The set takes its own copy of its array. */
    unshare(set: number): void;
    /** [A] The set takes its installed array and connected set again. */
    restore(set: number): void;
    /** [A] The installed table again, also after EUD writes. */
    restoreAll(): void;
    /**
     * [A]
     * @returns the position from 1 to 9
     */
    getPosition(set: number, index: number): number;
    /** [A] */
    setPosition(set: number, index: number, position: number): void;
    /**
     * [A]
     * @returns the cmdicons frame
     */
    getIcon(set: number, index: number): number;
    /** [A] */
    setIcon(set: number, index: number, icon: number): void;
    /**
     * [A]
     * @returns the condition id, -1 for an unknown function
     */
    getCondition(set: number, index: number): number;
    /** [A] */
    setCondition(set: number, index: number, condition: number): void;
    /** [A] */
    getConditionValue(set: number, index: number): number;
    /** [A] The value BYTE. */
    setConditionValue(set: number, index: number, value: number): void;
    /**
     * [A]
     * @returns the action id, -1 for an unknown function
     */
    getAction(set: number, index: number): number;
    /** [A] */
    setAction(set: number, index: number, action: number): void;
    /** [A] */
    getActionValue(set: number, index: number): number;
    /** [A] The value WORD. */
    setActionValue(set: number, index: number, value: number): void;
    /**
     * [A]
     * @returns the one-based stat_txt id of the enabled string, 0 for none
     */
    getEnabledString(set: number, index: number): number;
    /** [A] */
    setEnabledString(set: number, index: number, stringId: number): void;
    /** [A] */
    getDisabledString(set: number, index: number): number;
    /** [A] */
    setDisabledString(set: number, index: number, stringId: number): void;
    /**
     * [A]
     * @returns the enabled text, null while the button shows its enabled string
     */
    getEnabledText(set: number, index: number): string | null;
    /** [A] */
    getDisabledText(set: number, index: number): string | null;
    /** [A] Sets both texts; null shows the string. */
    setText(set: number, index: number, enabled: string | null, disabled: string | null): void;
    /** [A] Inserts a button before the index, or at the end for the button count. */
    insert(set: number, index: number, position: number, icon: number, condition: number, conditionValue: number, action: number,
           actionValue: number, enabledString: number, disabledString: number): void;
    /** [A] */
    remove(set: number, index: number): void;
    /** [A] */
    getConditionCount(): number;
    /** [A] */
    getConditionName(condition: number): string;
    /** [A] */
    getActionCount(): number;
    /** [A] */
    getActionName(action: number): string;
}

/**
 * The drawing of one space ({@link Drawing}) with numbers: colours 0xAARRGGBB, fonts and alignments by id, images by handle.
 */
interface CoreDrawing {
    /** [L] */
    fillRectangle(x: number, y: number, width: number, height: number, argb: number): void;
    /** [L] */
    drawRectangle(x: number, y: number, width: number, height: number, argb: number, thickness: number): void;
    /** [L] */
    fillCircle(x: number, y: number, radius: number, argb: number): void;
    /** [L] */
    drawCircle(x: number, y: number, radius: number, argb: number, thickness: number): void;
    /** [L] */
    drawLine(x1: number, y1: number, x2: number, y2: number, argb: number, thickness: number): void;
    /** [L] */
    drawPoint(x: number, y: number, argb: number): void;
    /** [L] */
    drawText(x: number, y: number, text: string, font: number, argb: number, align: number): void;
    /** [L] */
    drawImage(image: number, x: number, y: number, width: number, height: number, opacity: number): void;
    /** [L] */
    measureText(text: string, font: number): number;
}

/**
 * The game screen ({@link Screen}) with numbers, and the classic 640 by 480 palette screen that EUD maps write: its pixels, written from any
 * code, stay shown.
 */
interface CoreScreen extends CoreDrawing {
    /** [L] */
    getWidth(): number;
    /** [L] */
    getHeight(): number;
    /** [L] */
    getScale(): number;
    /**
     * [L]
     * @returns 0 SD, 1 HD, 2 HD2
     */
    getGraphics(): number;
    /** [A] */
    setPixel(x: number, y: number, paletteIndex: number): void;
    /** [A] */
    fillPixels(x: number, y: number, width: number, height: number, paletteIndex: number): void;
    /** [A] */
    getPaletteColor(index: number): number;
}

/**
 * The console ({@link Console}) with numbers: the dialogs by {@link ConsoleControl} id, the console images by {@link ConsoleImage} id, the resource
 * icons by frame (0 minerals, 1 to 3 gas and 4 to 6 supply of the Zerg, Terran and Protoss), images by handle, -1 for none.
 */
interface CoreConsole {
    /** [A] */
    setVisible(visible: boolean): void;
    /** [A] */
    isVisible(): boolean;
    /** [A] */
    setDialogVisible(dialog: number, visible: boolean): void;
    /** [A] */
    isDialogVisible(dialog: number): boolean;
    /** [A] */
    setDialogPosition(dialog: number, x: number, y: number): void;
    /** [A] */
    replaceImage(consoleImage: number, image: number): void;
    /** [A] */
    setResourceIcon(icon: number, image: number): void;
}

interface CoreStrings {
    /** [A] */
    setStatText(id: number, text: string | null): void;
}

interface CoreMessages {
    /** [A] */
    clear(): void;
}

interface Core {
    readonly game: CoreGame;
    readonly player: CorePlayer;
    readonly unit: CoreUnit;
    readonly location: CoreLocation;
    readonly terrain: CoreTerrain;
    readonly vision: CoreVision;
    readonly leaderboard: CoreLeaderboard;
    readonly combat: CoreCombat;
    readonly ai: CoreAi;
    readonly requirements: CoreRequirements;
    readonly unitType: CoreTypes;
    readonly weaponType: CoreTypes;
    readonly flingyType: CoreTypes;
    readonly spriteType: CoreTypes;
    readonly imageType: CoreImageTypes;
    readonly upgradeType: CoreTypes;
    readonly techType: CoreTypes;
    readonly orderType: CoreTypes;
    readonly dat: CoreDat;
    readonly events: CoreEvents;
    readonly command: CoreCommand;
    readonly resources: CoreResources;
    readonly graphics: CoreGraphics;
    readonly local: CoreLocal;
    readonly screen: CoreScreen;
    readonly world: CoreDrawing;
    readonly minimap: CoreDrawing;
    readonly console: CoreConsole;
    readonly strings: CoreStrings;
    readonly messages: CoreMessages;
    readonly buttons: CoreButtons;
}

declare const core: Core;
