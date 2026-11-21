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
 * ({@link Modifier}) and the map position x, y when positioned; for a chat line the text; and consumed, which keeps the input from the game
 * when a hook sets it. Read-only once the input went to the game.
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
    consumed: boolean;
}

/**
 * The record of a script command that ran ({@link CoreCommand.getExecuted}): the sending player's id and the value.
 */
interface CoreCommandRecord {
    readonly player: number;
    readonly value: any;
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
     * [A]
     * @param target the target unit, -1 for the position
     */
    issueOrder(unit: number, order: number, target: number, x: number, y: number): void;
    /**
     * [A] The status timers in status steps of eight frames, with the native apply and end routines as {@link Unit.setLockdownTimer} has them.
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
}

/**
 * The tiles of the map as MTXM values, group << 4 | index, by tile coordinates.
 */
interface CoreTerrain {
    /** [A] */
    getTile(tileX: number, tileY: number): number;
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
    readonly leaderboard: CoreLeaderboard;
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
}

declare const core: Core;
