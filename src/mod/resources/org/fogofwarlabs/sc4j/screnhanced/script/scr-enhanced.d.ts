// The declarations of SC:R Enhanced Script (script API version 1): everything a map script sees but the lower layer core (core.d.ts).
// They stand alone: use them with "noLib": true, so that no browser or Node.js global is offered (jsconfig.json of `init`).
// The scripts run in Rhino's interpreter: no class, async/await, spread (...) or for (const ... of ...); a const in a loop keeps its first
// value and the closures of one for (let ...) loop share one binding. No eval, Function from strings, Date, timer or Java access.

// ---------------------------------------------------------------------------------------------------------------------------------------
// The standard objects the scripts have.
// ---------------------------------------------------------------------------------------------------------------------------------------

interface Object {
    constructor: Function;

    toString(): string;

    toLocaleString(): string;

    valueOf(): Object;

    hasOwnProperty(key: PropertyKey): boolean;

    isPrototypeOf(value: Object): boolean;

    propertyIsEnumerable(key: PropertyKey): boolean;
}

interface PropertyDescriptor {
    configurable?: boolean;
    enumerable?: boolean;
    value?: any;
    writable?: boolean;

    get?(): any;

    set?(value: any): void;
}

interface PropertyDescriptorMap {
    [key: string]: PropertyDescriptor;
}

interface ObjectConstructor {
    new(value?: any): Object;

    (value?: any): any;

    readonly prototype: Object;

    getPrototypeOf(value: any): any;

    setPrototypeOf(value: any, prototype: object | null): any;

    getOwnPropertyNames(value: any): string[];

    getOwnPropertyDescriptor(value: any, key: PropertyKey): PropertyDescriptor | undefined;

    defineProperty<T>(value: T, key: PropertyKey, attributes: PropertyDescriptor): T;

    defineProperties<T>(value: T, properties: PropertyDescriptorMap): T;

    create(prototype: object | null, properties?: PropertyDescriptorMap): any;

    keys(value: object): string[];

    values<T>(value: { [key: string]: T }): T[];

    entries<T>(value: { [key: string]: T }): [string, T][];

    fromEntries<T>(entries: Iterable<readonly [PropertyKey, T]>): { [key: string]: T };

    assign<T, U>(target: T, source: U): T & U;

    assign(target: object, ...sources: any[]): any;

    freeze<T>(value: T): Readonly<T>;

    isFrozen(value: any): boolean;

    seal<T>(value: T): T;

    isSealed(value: any): boolean;

    preventExtensions<T>(value: T): T;

    isExtensible(value: any): boolean;

    is(first: any, second: any): boolean;

    hasOwn(value: object, key: PropertyKey): boolean;
}

declare var Object: ObjectConstructor;

interface Function {
    apply(this: Function, thisArgument: any, argumentArray?: any): any;

    call(this: Function, thisArgument: any, ...argumentList: any[]): any;

    bind(this: Function, thisArgument: any, ...argumentList: any[]): any;

    toString(): string;

    readonly length: number;
    readonly name: string;
}

interface FunctionConstructor {
    readonly prototype: Function;
}

/**
 * The function constructor throws a {@link TypeError}: scripts cannot create functions from strings.
 */
declare var Function: FunctionConstructor;

interface CallableFunction extends Function {
}

interface NewableFunction extends Function {
}

interface IArguments {
    [index: number]: any;

    length: number;
}

interface String {
    readonly length: number;

    readonly [index: number]: string;

    toString(): string;

    valueOf(): string;

    charAt(position: number): string;

    charCodeAt(index: number): number;

    codePointAt(position: number): number | undefined;

    at(index: number): string | undefined;

    concat(...strings: string[]): string;

    includes(searchString: string, position?: number): boolean;

    startsWith(searchString: string, position?: number): boolean;

    endsWith(searchString: string, endPosition?: number): boolean;

    indexOf(searchString: string, position?: number): number;

    lastIndexOf(searchString: string, position?: number): number;

    localeCompare(that: string): number;

    match(pattern: string | RegExp): RegExpMatchArray | null;

    replace(pattern: string | RegExp, replacement: string | ((substring: string, ...rest: any[]) => string)): string;

    replaceAll(pattern: string | RegExp, replacement: string | ((substring: string, ...rest: any[]) => string)): string;

    search(pattern: string | RegExp): number;

    slice(start?: number, end?: number): string;

    split(separator: string | RegExp, limit?: number): string[];

    substring(start: number, end?: number): string;

    toLowerCase(): string;

    toUpperCase(): string;

    trim(): string;

    trimStart(): string;

    trimEnd(): string;

    padStart(maximumLength: number, fillString?: string): string;

    padEnd(maximumLength: number, fillString?: string): string;

    repeat(count: number): string;

    normalize(form?: string): string;

    [Symbol.iterator](): IterableIterator<string>;
}

interface StringConstructor {
    new(value?: any): String;

    (value?: any): string;

    readonly prototype: String;

    fromCharCode(...codes: number[]): string;

    fromCodePoint(...codePoints: number[]): string;
}

declare var String: StringConstructor;

interface Boolean {
    valueOf(): boolean;
}

interface BooleanConstructor {
    new(value?: any): Boolean;

    <T>(value?: T): boolean;

    readonly prototype: Boolean;
}

declare var Boolean: BooleanConstructor;

interface Number {
    toString(radix?: number): string;

    toFixed(fractionDigits?: number): string;

    toExponential(fractionDigits?: number): string;

    toPrecision(precision?: number): string;

    valueOf(): number;
}

interface NumberConstructor {
    new(value?: any): Number;

    (value?: any): number;

    readonly prototype: Number;
    readonly MAX_VALUE: number;
    readonly MIN_VALUE: number;
    readonly NaN: number;
    readonly NEGATIVE_INFINITY: number;
    readonly POSITIVE_INFINITY: number;
    readonly MAX_SAFE_INTEGER: number;
    readonly MIN_SAFE_INTEGER: number;
    readonly EPSILON: number;

    isFinite(value: unknown): boolean;

    isInteger(value: unknown): boolean;

    isNaN(value: unknown): boolean;

    isSafeInteger(value: unknown): boolean;

    parseFloat(text: string): number;

    parseInt(text: string, radix?: number): number;
}

declare var Number: NumberConstructor;

interface RegExpMatchArray extends Array<string> {
    index?: number;
    input?: string;
}

interface RegExpExecArray extends Array<string> {
    index: number;
    input: string;
}

interface RegExp {
    exec(text: string): RegExpExecArray | null;

    test(text: string): boolean;

    readonly source: string;
    readonly flags: string;
    readonly global: boolean;
    readonly ignoreCase: boolean;
    readonly multiline: boolean;
    lastIndex: number;
}

interface RegExpConstructor {
    new(pattern: RegExp | string, flags?: string): RegExp;

    (pattern: RegExp | string, flags?: string): RegExp;

    readonly prototype: RegExp;
}

declare var RegExp: RegExpConstructor;

interface ConcatArray<T> {
    readonly length: number;

    readonly [index: number]: T;

    join(separator?: string): string;

    slice(start?: number, end?: number): T[];
}

interface ReadonlyArray<T> {
    readonly length: number;

    readonly [index: number]: T;

    indexOf(searchElement: T, fromIndex?: number): number;

    includes(searchElement: T, fromIndex?: number): boolean;

    slice(start?: number, end?: number): T[];

    join(separator?: string): string;

    forEach(callback: (value: T, index: number, array: readonly T[]) => void, thisArgument?: any): void;

    map<U>(callback: (value: T, index: number, array: readonly T[]) => U, thisArgument?: any): U[];

    filter(predicate: (value: T, index: number, array: readonly T[]) => unknown, thisArgument?: any): T[];

    [Symbol.iterator](): IterableIterator<T>;
}

interface Array<T> {
    length: number;

    [index: number]: T;

    toString(): string;

    at(index: number): T | undefined;

    concat(...items: (T | ConcatArray<T>)[]): T[];

    copyWithin(target: number, start: number, end?: number): this;

    entries(): IterableIterator<[number, T]>;

    every(predicate: (value: T, index: number, array: T[]) => unknown, thisArgument?: any): boolean;

    fill(value: T, start?: number, end?: number): this;

    filter(predicate: (value: T, index: number, array: T[]) => unknown, thisArgument?: any): T[];

    find(predicate: (value: T, index: number, array: T[]) => unknown, thisArgument?: any): T | undefined;

    findIndex(predicate: (value: T, index: number, array: T[]) => unknown, thisArgument?: any): number;

    findLast(predicate: (value: T, index: number, array: T[]) => unknown, thisArgument?: any): T | undefined;

    findLastIndex(predicate: (value: T, index: number, array: T[]) => unknown, thisArgument?: any): number;

    flat(depth?: number): any[];

    flatMap<U>(callback: (value: T, index: number, array: T[]) => U | readonly U[], thisArgument?: any): U[];

    forEach(callback: (value: T, index: number, array: T[]) => void, thisArgument?: any): void;

    includes(searchElement: T, fromIndex?: number): boolean;

    indexOf(searchElement: T, fromIndex?: number): number;

    join(separator?: string): string;

    keys(): IterableIterator<number>;

    lastIndexOf(searchElement: T, fromIndex?: number): number;

    map<U>(callback: (value: T, index: number, array: T[]) => U, thisArgument?: any): U[];

    pop(): T | undefined;

    push(...items: T[]): number;

    reduce(callback: (previous: T, current: T, index: number, array: T[]) => T): T;

    reduce<U>(callback: (previous: U, current: T, index: number, array: T[]) => U, initialValue: U): U;

    reduceRight(callback: (previous: T, current: T, index: number, array: T[]) => T): T;

    reduceRight<U>(callback: (previous: U, current: T, index: number, array: T[]) => U, initialValue: U): U;

    reverse(): T[];

    shift(): T | undefined;

    slice(start?: number, end?: number): T[];

    some(predicate: (value: T, index: number, array: T[]) => unknown, thisArgument?: any): boolean;

    sort(compare?: (first: T, second: T) => number): this;

    splice(start: number, deleteCount?: number, ...items: T[]): T[];

    toReversed(): T[];

    toSorted(compare?: (first: T, second: T) => number): T[];

    toSpliced(start: number, deleteCount?: number, ...items: T[]): T[];

    unshift(...items: T[]): number;

    values(): IterableIterator<T>;

    with(index: number, value: T): T[];

    [Symbol.iterator](): IterableIterator<T>;
}

interface ArrayConstructor {
    new<T>(...items: T[]): T[];

    <T>(...items: T[]): T[];

    readonly prototype: any[];

    isArray(value: any): value is any[];

    from<T>(items: Iterable<T> | ArrayLike<T>): T[];

    from<T, U>(items: Iterable<T> | ArrayLike<T>, mapping: (value: T, index: number) => U): U[];

    of<T>(...items: T[]): T[];
}

declare var Array: ArrayConstructor;

interface ArrayLike<T> {
    readonly length: number;

    readonly [index: number]: T;
}

type PropertyKey = string | number | symbol;

type Partial<T> = { [P in keyof T]?: T[P] };

type Readonly<T> = { readonly [P in keyof T]: T[P] };

type Record<K extends keyof any, T> = { [P in K]: T };

interface TemplateStringsArray extends ReadonlyArray<string> {
    readonly raw: readonly string[];
}

interface Symbol {
    toString(): string;

    readonly description: string | undefined;
}

interface SymbolConstructor {
    (description?: string | number): symbol;

    readonly prototype: Symbol;
    readonly iterator: unique symbol;
}

declare var Symbol: SymbolConstructor;

interface IteratorResult<T> {
    done?: boolean;
    value: T;
}

interface Iterator<T> {
    next(): IteratorResult<T>;
}

interface Iterable<T> {
    [Symbol.iterator](): Iterator<T>;
}

interface IterableIterator<T> extends Iterator<T> {
    [Symbol.iterator](): IterableIterator<T>;
}

interface Map<K, V> {
    readonly size: number;

    clear(): void;

    delete(key: K): boolean;

    entries(): IterableIterator<[K, V]>;

    forEach(callback: (value: V, key: K, map: Map<K, V>) => void, thisArgument?: any): void;

    get(key: K): V | undefined;

    has(key: K): boolean;

    keys(): IterableIterator<K>;

    set(key: K, value: V): this;

    values(): IterableIterator<V>;

    [Symbol.iterator](): IterableIterator<[K, V]>;
}

interface MapConstructor {
    new<K, V>(entries?: Iterable<readonly [K, V]> | null): Map<K, V>;

    readonly prototype: Map<any, any>;
}

declare var Map: MapConstructor;

interface Set<T> {
    readonly size: number;

    add(value: T): this;

    clear(): void;

    delete(value: T): boolean;

    entries(): IterableIterator<[T, T]>;

    forEach(callback: (value: T, key: T, set: Set<T>) => void, thisArgument?: any): void;

    has(value: T): boolean;

    keys(): IterableIterator<T>;

    values(): IterableIterator<T>;

    [Symbol.iterator](): IterableIterator<T>;
}

interface SetConstructor {
    new<T>(values?: Iterable<T> | null): Set<T>;

    readonly prototype: Set<any>;
}

declare var Set: SetConstructor;

interface WeakMap<K extends object, V> {
    delete(key: K): boolean;

    get(key: K): V | undefined;

    has(key: K): boolean;

    set(key: K, value: V): this;
}

interface WeakMapConstructor {
    new<K extends object, V>(entries?: Iterable<readonly [K, V]> | null): WeakMap<K, V>;

    readonly prototype: WeakMap<object, any>;
}

declare var WeakMap: WeakMapConstructor;

interface WeakSet<T extends object> {
    add(value: T): this;

    delete(value: T): boolean;

    has(value: T): boolean;
}

interface WeakSetConstructor {
    new<T extends object>(values?: Iterable<T> | null): WeakSet<T>;

    readonly prototype: WeakSet<object>;
}

declare var WeakSet: WeakSetConstructor;

interface JSON {
    parse(text: string, reviver?: (key: string, value: any) => any): any;

    stringify(value: any, replacer?: ((key: string, value: any) => any) | (number | string)[] | null, space?: string | number): string;
}

declare var JSON: JSON;

interface Error {
    name: string;
    message: string;
}

interface ErrorConstructor {
    new(message?: string): Error;

    (message?: string): Error;

    readonly prototype: Error;
}

declare var Error: ErrorConstructor;
declare var EvalError: ErrorConstructor;
declare var RangeError: ErrorConstructor;
declare var ReferenceError: ErrorConstructor;
declare var SyntaxError: ErrorConstructor;
declare var TypeError: ErrorConstructor;
declare var URIError: ErrorConstructor;

declare var NaN: number;
declare var Infinity: number;

declare function parseInt(text: string, radix?: number): number;

declare function parseFloat(text: string): number;

declare function isNaN(value: number): boolean;

declare function isFinite(value: number): boolean;

declare function encodeURI(uri: string): string;

declare function encodeURIComponent(component: string | number | boolean): string;

declare function decodeURI(encodedUri: string): string;

declare function decodeURIComponent(encodedComponent: string): string;

/**
 * Deterministic math: every function gives the same result on every machine, and {@link Math.random} draws from the script RNG
 * ({@link random}).
 */
interface Math {
    readonly E: number;
    readonly LN10: number;
    readonly LN2: number;
    readonly LOG10E: number;
    readonly LOG2E: number;
    readonly PI: number;
    readonly SQRT1_2: number;
    readonly SQRT2: number;

    abs(value: number): number;

    acos(value: number): number;

    acosh(value: number): number;

    asin(value: number): number;

    asinh(value: number): number;

    atan(value: number): number;

    atan2(y: number, x: number): number;

    atanh(value: number): number;

    cbrt(value: number): number;

    ceil(value: number): number;

    clz32(value: number): number;

    cos(value: number): number;

    cosh(value: number): number;

    exp(value: number): number;

    expm1(value: number): number;

    floor(value: number): number;

    fround(value: number): number;

    hypot(...values: number[]): number;

    imul(first: number, second: number): number;

    log(value: number): number;

    log10(value: number): number;

    log1p(value: number): number;

    log2(value: number): number;

    max(...values: number[]): number;

    min(...values: number[]): number;

    pow(base: number, exponent: number): number;

    /**
     * @returns a number from the script RNG, at least 0 and below 1
     */
    random(): number;

    round(value: number): number;

    sign(value: number): number;

    sin(value: number): number;

    sinh(value: number): number;

    sqrt(value: number): number;

    tan(value: number): number;

    tanh(value: number): number;

    trunc(value: number): number;

    toSource(): string;
}

declare var Math: Math;

// ---------------------------------------------------------------------------------------------------------------------------------------
// The script runtime: modules, log and the script RNG.
// ---------------------------------------------------------------------------------------------------------------------------------------

/**
 * Writes to the SC:R Enhanced log with the module name: the values converted to strings and joined with spaces.
 */
interface Log {
    info(...values: any[]): void;

    warn(...values: any[]): void;

    error(...values: any[]): void;
}

/**
 * The log of the module.
 */
declare const log: Log;

/**
 * The random numbers of the script, seeded from the game seed, the same on every peer and separate from the game's own random numbers.
 */
interface Random {
    /**
     * @returns a number at least 0 and below 1
     */
    next(): number;

    /**
     * @param bound a positive integer up to 2^53
     * @returns an integer at least 0 and below the bound
     */
    nextInt(bound: number): number;

    /**
     * @returns true with the probability
     */
    chance(probability: number): boolean;
}

declare const random: Random;

// ---------------------------------------------------------------------------------------------------------------------------------------
// The hooks: top-level functions of a script with these names. Every script (module) that has a hook gets the call, in load order.
// ---------------------------------------------------------------------------------------------------------------------------------------

/**
 * The hooks a script may define, as top-level functions: {@code function onUnitDied(event) { ... }}. The prelude collects them per module
 * once the module has loaded and calls them from the game's entry points. A frame calls onStart (the first frame only), the due timers, the
 * channel handlers of the frame's commands, the event hooks ({@code on*}) of the frame's events ({@link CoreEvents.getFrameEvents}) and
 * onFrame, in this order, at the frame start before the triggers. Decision hooks ({@code onBefore*}) run inside the game routine and may only
 * change the decision. Give the parameter its type with JSDoc: {@code /** @param {UnitDiedEvent} event *\/}.
 *
 * Shared code (module top level, onStart, onFrame, the event and decision hooks, channel handlers, timers) runs the same on every peer and may
 * change the game. Local hooks ({@code onLocal*}, onRender) run on the local machine only, at once; the input hooks not in replays and not
 * for observers, onLocalFrame and onRender also there. They may read the game and the local information ({@link local}), move the local
 * screen and send ({@link Channel.send}, {@link PlayerValue.set}, {@link CoreCommand.submit}; nothing is sent in a replay or by an
 * observer), but changing the game, adding or cancelling a timer, creating a channel, drawing a random number or loading a new module throws.
 * They must not change variables that shared code reads.
 */
interface Hooks {
    /**
     * Called once at the first frame, before the timers and the other hooks of the frame.
     */
    onStart?(): void;

    /**
     * Called every game frame, before the triggers, after the timers, the channel handlers and the event hooks of the frame.
     */
    onFrame?(frame: number): void;

    onUnitCreated?(event: UnitCreatedEvent): void;

    onUnitCompleted?(event: UnitCompletedEvent): void;

    /**
     * Called for a unit that started to die, with its killer and the weapon when known.
     */
    onUnitDied?(event: UnitDiedEvent): void;

    /**
     * Called for hit point damage to a unit.
     */
    onUnitDamaged?(event: UnitDamagedEvent): void;

    onOrderIssued?(event: OrderIssuedEvent): void;

    /**
     * Called for a target that an automatic target search found.
     */
    onTargetAcquired?(event: TargetAcquiredEvent): void;

    onUnitTrained?(event: UnitTrainedEvent): void;

    onResearchCompleted?(event: ResearchCompletedEvent): void;

    onUpgradeCompleted?(event: UpgradeCompletedEvent): void;

    onSpellCast?(event: SpellCastEvent): void;

    onBulletCreated?(event: BulletCreatedEvent): void;

    onBulletHit?(event: BulletHitEvent): void;

    onOwnerChanged?(event: OwnerChangedEvent): void;

    /**
     * Called for a unit that entered a transport or a Bunker.
     */
    onUnitLoaded?(event: UnitLoadedEvent): void;

    /**
     * Called for a unit that left a transport or a Bunker.
     */
    onUnitUnloaded?(event: UnitUnloadedEvent): void;

    /**
     * Called for a player who left the game or was dropped.
     */
    onPlayerLeft?(event: PlayerLeftEvent): void;

    /**
     * Called for a player who received a result: the triggers ended the player's game, or the player left.
     */
    onPlayerResult?(event: PlayerResultEvent): void;

    /**
     * Local: called for a chat line the local player submitted, before it is sent.
     */
    onLocalChat?(event: LocalChatEvent): void;

    /**
     * Local: called for a key the local player pressed while the game takes play keys, before the game's own keys.
     */
    onLocalKeyDown?(event: LocalKeyEvent): void;

    /**
     * Local: called for a key the local player released.
     */
    onLocalKeyUp?(event: LocalKeyEvent): void;

    /**
     * Local: called for a mouse button the local player pressed over the map, before the game's own handling.
     */
    onLocalMouseDown?(event: LocalMouseButtonEvent): void;

    /**
     * Local: called for a mouse button the local player released.
     */
    onLocalMouseUp?(event: LocalMouseButtonEvent): void;

    /**
     * Local: called for an enabled command card button the local player pressed, by a click or a hotkey, before its action runs; consuming the
     * press runs no action. Called in a live game only.
     */
    onLocalButtonPressed?(event: LocalButtonPressedEvent): void;

    /**
     * Local: called for the unit or map point the local player targeted with a ScriptTarget button; consuming the target keeps the targeting
     * mode, as a refused target of an order does.
     */
    onLocalButtonTargeted?(event: LocalButtonTargetedEvent): void;

    /**
     * Local: called after each game frame this machine plays, live or in a replay, with the frame number that onFrame received.
     */
    onLocalFrame?(frame: number): void;

    /**
     * Local: called after onLocalFrame to draw; what it draws stays on screen until the next call. It has its own instruction budget: a call
     * over it draws nothing; a call that throws keeps what it drew.
     */
    onRender?(screen: Screen): void;

    /**
     * Decides the damage of a weapon hit.
     */
    onBeforeDamage?(decision: DamageDecision): void;

    /**
     * Decides an order issued to a unit.
     */
    onBeforeOrder?(decision: OrderDecision): void;

    /**
     * Decides the target an automatic target search found.
     */
    onBeforeTargetAcquired?(decision: TargetAcquisitionDecision): void;
}

// ---------------------------------------------------------------------------------------------------------------------------------------
// The game API: the upper layer.
// ---------------------------------------------------------------------------------------------------------------------------------------

/**
 * A type of the original data or a constant: the same object for the same id.
 */
interface GameType {
    /**
     * @returns the id, which the lower layer {@link core} takes
     */
    getId(): number;

    /**
     * @returns the original data name ("Terran Marine") or the constant's name
     */
    getName(): string;
}

/**
 * The constants of a table by id.
 */
interface ConstantTable<T> {
    /**
     * @returns the constant of the id, or null for an id outside the table
     */
    get(id: number): T | null;

    getCount(): number;
}

// BEGIN GENERATED DAT: the DAT columns of ScriptDatTables; ScriptDatTablesTest checks this block.

/**
 * The units.dat values of the unit types: raw values of the current game.
 */
interface UnitTypeData {
    /** Graphics (below 209) */
    getFlingyType(): FlingyType;

    setFlingyType(flingyType: FlingyType): void;

    /** Subunit1 */
    getFirstSubunit(): UnitType | null;

    setFirstSubunit(unitType: UnitType | null): void;

    /** Subunit2 */
    getSecondSubunit(): UnitType | null;

    setSecondSubunit(unitType: UnitType | null): void;

    /** Infestation (unit types 106 to 201) */
    getInfestedType(): UnitType | null;

    setInfestedType(unitType: UnitType | null): void;

    /** Construction Animation */
    getConstructionImage(): ImageType;

    setConstructionImage(imageType: ImageType): void;

    /** Unit Direction */
    getInitialDirection(): number;

    setInitialDirection(value: number): void;

    /** Shield Enable */
    getShieldEnabled(): number;

    setShieldEnabled(value: number): void;

    /** Shield Amount */
    getShieldAmount(): number;

    setShieldAmount(value: number): void;

    /** Hit Points */
    getHitPoints(): number;

    setHitPoints(value: number): void;

    /** Elevation Level */
    getElevationLevel(): number;

    setElevationLevel(value: number): void;

    /** Unknown */
    getUnknownByte(): number;

    setUnknownByte(value: number): void;

    /** Sublabel */
    getSublabel(): number;

    setSublabel(value: number): void;

    /** Comp AI Idle */
    getComputerIdleOrder(): Order | null;

    setComputerIdleOrder(order: Order | null): void;

    /** Human AI Idle */
    getHumanIdleOrder(): Order | null;

    setHumanIdleOrder(order: Order | null): void;

    /** Return to Idle */
    getReturnToIdleOrder(): Order | null;

    setReturnToIdleOrder(order: Order | null): void;

    /** Attack Unit */
    getAttackUnitOrder(): Order | null;

    setAttackUnitOrder(order: Order | null): void;

    /** Attack Move */
    getAttackMoveOrder(): Order | null;

    setAttackMoveOrder(order: Order | null): void;

    /** Ground Weapon */
    getGroundWeapon(): WeaponType | null;

    setGroundWeapon(weaponType: WeaponType | null): void;

    /** Max Ground Hits */
    getMaximumGroundHits(): number;

    setMaximumGroundHits(value: number): void;

    /** Air Weapon */
    getAirWeapon(): WeaponType | null;

    setAirWeapon(weaponType: WeaponType | null): void;

    /** Max Air Hits */
    getMaximumAirHits(): number;

    setMaximumAirHits(value: number): void;

    /** AI Internal 0x1 */
    isComputerInternalAttackGroupIgnored(): boolean;

    setComputerInternalAttackGroupIgnored(set: boolean): void;

    /** AI Internal 0x2 */
    isComputerInternalTownUnit(): boolean;

    setComputerInternalTownUnit(set: boolean): void;

    /** Special Ability Flags 0x1 */
    isBuilding(): boolean;

    setBuilding(set: boolean): void;

    /** Special Ability Flags 0x2 */
    isAddon(): boolean;

    setAddon(set: boolean): void;

    /** Special Ability Flags 0x4 */
    isFlyer(): boolean;

    setFlyer(set: boolean): void;

    /** Special Ability Flags 0x8 */
    isWorker(): boolean;

    setWorker(set: boolean): void;

    /** Special Ability Flags 0x10 */
    isSubunit(): boolean;

    setSubunit(set: boolean): void;

    /** Special Ability Flags 0x20 */
    isFlyingBuilding(): boolean;

    setFlyingBuilding(set: boolean): void;

    /** Special Ability Flags 0x40 */
    isHero(): boolean;

    setHero(set: boolean): void;

    /** Special Ability Flags 0x80 */
    isRegeneratesHitPoints(): boolean;

    setRegeneratesHitPoints(set: boolean): void;

    /** Special Ability Flags 0x200 */
    isCloakable(): boolean;

    setCloakable(set: boolean): void;

    /** Special Ability Flags 0x400 */
    isTwoUnitsInOneEgg(): boolean;

    setTwoUnitsInOneEgg(set: boolean): void;

    /** Special Ability Flags 0x800 */
    isPowerup(): boolean;

    setPowerup(set: boolean): void;

    /** Special Ability Flags 0x1000 */
    isResourceDepot(): boolean;

    setResourceDepot(set: boolean): void;

    /** Special Ability Flags 0x2000 */
    isResourceContainer(): boolean;

    setResourceContainer(set: boolean): void;

    /** Special Ability Flags 0x4000 */
    isRobotic(): boolean;

    setRobotic(set: boolean): void;

    /** Special Ability Flags 0x8000 */
    isDetector(): boolean;

    setDetector(set: boolean): void;

    /** Special Ability Flags 0x10000 */
    isOrganic(): boolean;

    setOrganic(set: boolean): void;

    /** Special Ability Flags 0x20000 */
    isRequiresCreep(): boolean;

    setRequiresCreep(set: boolean): void;

    /** Special Ability Flags 0x80000 */
    isRequiresPsionicMatrix(): boolean;

    setRequiresPsionicMatrix(set: boolean): void;

    /** Special Ability Flags 0x100000 */
    isBurrowable(): boolean;

    setBurrowable(set: boolean): void;

    /** Special Ability Flags 0x200000 */
    isSpellcaster(): boolean;

    setSpellcaster(set: boolean): void;

    /** Special Ability Flags 0x400000 */
    isPermanentCloak(): boolean;

    setPermanentCloak(set: boolean): void;

    /** Special Ability Flags 0x1000000 */
    isIgnoreSupplyCheck(): boolean;

    setIgnoreSupplyCheck(set: boolean): void;

    /** Special Ability Flags 0x2000000 */
    isMediumOverlays(): boolean;

    setMediumOverlays(set: boolean): void;

    /** Special Ability Flags 0x4000000 */
    isLargeOverlays(): boolean;

    setLargeOverlays(set: boolean): void;

    /** Special Ability Flags 0x8000000 */
    isCanMove(): boolean;

    setCanMove(set: boolean): void;

    /** Special Ability Flags 0x10000000 */
    isCanTurn(): boolean;

    setCanTurn(set: boolean): void;

    /** Special Ability Flags 0x20000000 */
    isInvincible(): boolean;

    setInvincible(set: boolean): void;

    /** Special Ability Flags 0x40000000 */
    isMechanical(): boolean;

    setMechanical(set: boolean): void;

    /** Target Acquisition Range */
    getTargetAcquisitionRange(): number;

    setTargetAcquisitionRange(value: number): void;

    /** Sight Range */
    getSightRange(): number;

    setSightRange(value: number): void;

    /** Armor Upgrade */
    getArmorUpgrade(): UpgradeType | null;

    setArmorUpgrade(upgradeType: UpgradeType | null): void;

    /** Unit Size */
    getSizeClass(): number;

    setSizeClass(value: number): void;

    /** Armor */
    getArmor(): number;

    setArmor(value: number): void;

    /** Right-click Action */
    getRightClickAction(): number;

    setRightClickAction(value: number): void;

    /** Ready Sound (unit types 0 to 105) */
    getReadySound(): number;

    setReadySound(value: number): void;

    /** What Sound Start */
    getWhatSoundStart(): number;

    setWhatSoundStart(value: number): void;

    /** What Sound End */
    getWhatSoundEnd(): number;

    setWhatSoundEnd(value: number): void;

    /** Piss Sound Start (unit types 0 to 105) */
    getAnnoyedSoundStart(): number;

    setAnnoyedSoundStart(value: number): void;

    /** Piss Sound End (unit types 0 to 105) */
    getAnnoyedSoundEnd(): number;

    setAnnoyedSoundEnd(value: number): void;

    /** Yes Sound Start (unit types 0 to 105) */
    getYesSoundStart(): number;

    setYesSoundStart(value: number): void;

    /** Yes Sound End (unit types 0 to 105) */
    getYesSoundEnd(): number;

    setYesSoundEnd(value: number): void;

    /** Placement Box Width */
    getPlacementWidth(): number;

    setPlacementWidth(value: number): void;

    /** Placement Box Height */
    getPlacementHeight(): number;

    setPlacementHeight(value: number): void;

    /** Addon Horizontal (unit types 106 to 201) */
    getAddonHorizontalOffset(): number;

    setAddonHorizontalOffset(value: number): void;

    /** Addon Vertical (unit types 106 to 201) */
    getAddonVerticalOffset(): number;

    setAddonVerticalOffset(value: number): void;

    /** Unit Size Left */
    getDimensionLeft(): number;

    setDimensionLeft(value: number): void;

    /** Unit Size Up */
    getDimensionUp(): number;

    setDimensionUp(value: number): void;

    /** Unit Size Right */
    getDimensionRight(): number;

    setDimensionRight(value: number): void;

    /** Unit Size Down */
    getDimensionDown(): number;

    setDimensionDown(value: number): void;

    /** Portrait */
    getPortrait(): number;

    setPortrait(value: number): void;

    /** Mineral Cost */
    getMineralCost(): number;

    setMineralCost(value: number): void;

    /** Vespene Cost */
    getGasCost(): number;

    setGasCost(value: number): void;

    /** Build Time */
    getBuildTime(): number;

    setBuildTime(value: number): void;

    /** Requirement Index */
    getRequirementIndex(): number;

    setRequirementIndex(value: number): void;

    /** StarEdit Group Flags 0x1 */
    isGroupZerg(): boolean;

    setGroupZerg(set: boolean): void;

    /** StarEdit Group Flags 0x2 */
    isGroupTerran(): boolean;

    setGroupTerran(set: boolean): void;

    /** StarEdit Group Flags 0x4 */
    isGroupProtoss(): boolean;

    setGroupProtoss(set: boolean): void;

    /** StarEdit Group Flags 0x8 */
    isGroupMen(): boolean;

    setGroupMen(set: boolean): void;

    /** StarEdit Group Flags 0x10 */
    isGroupBuilding(): boolean;

    setGroupBuilding(set: boolean): void;

    /** StarEdit Group Flags 0x20 */
    isGroupFactory(): boolean;

    setGroupFactory(set: boolean): void;

    /** StarEdit Group Flags 0x40 */
    isGroupIndependent(): boolean;

    setGroupIndependent(set: boolean): void;

    /** StarEdit Group Flags 0x80 */
    isGroupNeutral(): boolean;

    setGroupNeutral(set: boolean): void;

    /** Supply Provided */
    getSupplyProvided(): number;

    setSupplyProvided(value: number): void;

    /** Supply Required */
    getSupplyRequired(): number;

    setSupplyRequired(value: number): void;

    /** Space Required */
    getSpaceRequired(): number;

    setSpaceRequired(value: number): void;

    /** Space Provided */
    getSpaceProvided(): number;

    setSpaceProvided(value: number): void;

    /** Build Score */
    getBuildScore(): number;

    setBuildScore(value: number): void;

    /** Destroy Score */
    getDestroyScore(): number;

    setDestroyScore(value: number): void;

    /** Unit Map String */
    getMapStringIndex(): number;

    setMapStringIndex(value: number): void;

    /** Broodwar Unit Flag */
    getBroodWarFlag(): number;

    setBroodWarFlag(value: number): void;

    /** StarEdit Availability Flags 0x1 */
    isAvailabilityNonNeutral(): boolean;

    setAvailabilityNonNeutral(set: boolean): void;

    /** StarEdit Availability Flags 0x2 */
    isAvailabilityUnitListing(): boolean;

    setAvailabilityUnitListing(set: boolean): void;

    /** StarEdit Availability Flags 0x200 */
    isAvailabilityExpansionOnly(): boolean;

    setAvailabilityExpansionOnly(set: boolean): void;
}

/**
 * The weapons.dat values of the weapon types: raw values of the current game.
 */
interface WeaponTypeData {
    /** Label */
    getLabel(): number;

    setLabel(value: number): void;

    /** Sprite */
    getFlingyType(): FlingyType;

    setFlingyType(flingyType: FlingyType): void;

    /** Special Attack */
    getSpecialAttack(): number;

    setSpecialAttack(value: number): void;

    /** Target Flags 0x1 */
    isTargetAir(): boolean;

    setTargetAir(set: boolean): void;

    /** Target Flags 0x2 */
    isTargetGround(): boolean;

    setTargetGround(set: boolean): void;

    /** Target Flags 0x4 */
    isTargetMechanical(): boolean;

    setTargetMechanical(set: boolean): void;

    /** Target Flags 0x8 */
    isTargetOrganic(): boolean;

    setTargetOrganic(set: boolean): void;

    /** Target Flags 0x10 */
    isTargetNonBuilding(): boolean;

    setTargetNonBuilding(set: boolean): void;

    /** Target Flags 0x20 */
    isTargetNonRobotic(): boolean;

    setTargetNonRobotic(set: boolean): void;

    /** Target Flags 0x40 */
    isTargetTerrain(): boolean;

    setTargetTerrain(set: boolean): void;

    /** Target Flags 0x80 */
    isTargetOrganicOrMechanical(): boolean;

    setTargetOrganicOrMechanical(set: boolean): void;

    /** Target Flags 0x100 */
    isTargetOwn(): boolean;

    setTargetOwn(set: boolean): void;

    /** Minimum Range */
    getMinimumRange(): number;

    setMinimumRange(value: number): void;

    /** Maximum Range */
    getMaximumRange(): number;

    setMaximumRange(value: number): void;

    /** Upgrade Group */
    getDamageUpgrade(): UpgradeType | null;

    setDamageUpgrade(upgradeType: UpgradeType | null): void;

    /** Damage Type */
    getDamageType(): number;

    setDamageType(value: number): void;

    /** Weapon Behavior */
    getBehavior(): number;

    setBehavior(value: number): void;

    /** Remove After */
    getLifetime(): number;

    setLifetime(value: number): void;

    /** Explosion Type */
    getExplosionType(): number;

    setExplosionType(value: number): void;

    /** Inner Splash Radius */
    getInnerSplashRadius(): number;

    setInnerSplashRadius(value: number): void;

    /** Medium Splash Radius */
    getMediumSplashRadius(): number;

    setMediumSplashRadius(value: number): void;

    /** Outer Splash Radius */
    getOuterSplashRadius(): number;

    setOuterSplashRadius(value: number): void;

    /** Damage Amount */
    getDamageAmount(): number;

    setDamageAmount(value: number): void;

    /** Damage Bonus */
    getDamageBonus(): number;

    setDamageBonus(value: number): void;

    /** Weapon Cooldown */
    getCooldown(): number;

    setCooldown(value: number): void;

    /** Damage Factor */
    getDamageFactor(): number;

    setDamageFactor(value: number): void;

    /** Attack Angle */
    getAttackAngle(): number;

    setAttackAngle(value: number): void;

    /** Launch Spin */
    getLaunchSpin(): number;

    setLaunchSpin(value: number): void;

    /** Forward Offset */
    getForwardOffset(): number;

    setForwardOffset(value: number): void;

    /** Upward Offset */
    getUpwardOffset(): number;

    setUpwardOffset(value: number): void;

    /** Target Error Message */
    getTargetErrorMessage(): number;

    setTargetErrorMessage(value: number): void;

    /** Icon */
    getIcon(): number;

    setIcon(value: number): void;
}

/**
 * The flingy.dat values of the flingy types: raw values of the current game.
 */
interface FlingyTypeData {
    /** Sprite */
    getSprite(): SpriteType;

    setSprite(spriteType: SpriteType): void;

    /** Top Speed */
    getTopSpeed(): number;

    setTopSpeed(value: number): void;

    /** Acceleration */
    getAcceleration(): number;

    setAcceleration(value: number): void;

    /** Halt Distance */
    getHaltDistance(): number;

    setHaltDistance(value: number): void;

    /** Turn Rate */
    getTurnRate(): number;

    setTurnRate(value: number): void;

    /** Unused */
    getUnused(): number;

    setUnused(value: number): void;

    /** Movement Control */
    getMovementControl(): number;

    setMovementControl(value: number): void;
}

/**
 * The sprites.dat values of the sprite types: raw values of the current game.
 */
interface SpriteTypeData {
    /** Image (below 999) */
    getImage(): ImageType;

    setImage(imageType: ImageType): void;

    /** Health Bar (sprite types 130 to 516, read only) */
    getHealthBarWidth(): number;

    /** Unused */
    getUnused(): number;

    setUnused(value: number): void;

    /** Is Visible */
    getVisible(): number;

    setVisible(value: number): void;

    /** Selection Circle (sprite types 130 to 516, read only) */
    getSelectionCircle(): number;

    /** Y Position (sprite types 130 to 516, read only) */
    getSelectionCircleOffset(): number;
}

/**
 * The images.dat values of the image types: raw values of the current game.
 */
interface ImageTypeData {
    /** GRP File (read only) */
    getGraphicsFile(): number;

    /** Graphic Turns */
    getGraphicTurns(): number;

    setGraphicTurns(value: number): void;

    /** Clickable */
    getClickable(): number;

    setClickable(value: number): void;

    /** Use Full Iscript */
    getFullIscript(): number;

    setFullIscript(value: number): void;

    /** Draw If Cloaked */
    getDrawIfCloaked(): number;

    setDrawIfCloaked(value: number): void;

    /** Draw Function (below 18) */
    getDrawFunction(): number;

    setDrawFunction(value: number): void;

    /** Color Shift */
    getColorShift(): number;

    setColorShift(value: number): void;

    /** Iscript (below 411) */
    getIscript(): number;

    setIscript(value: number): void;

    /** Shield Overlay (read only) */
    getShieldOverlay(): number;

    /** Attack Overlay (read only) */
    getAttackOverlay(): number;

    /** Damage Overlay (read only) */
    getDamageOverlay(): number;

    /** Special Overlay (read only) */
    getSpecialOverlay(): number;

    /** Landing Dust Overlay (read only) */
    getLandingDustOverlay(): number;

    /** Liftoff Dust Overlay (read only) */
    getLiftoffDustOverlay(): number;
}

/**
 * The upgrades.dat values of the upgrade types: raw values of the current game.
 */
interface UpgradeTypeData {
    /** Ore Cost */
    getMineralCost(): number;

    setMineralCost(value: number): void;

    /** Ore Cost Factor */
    getMineralCostFactor(): number;

    setMineralCostFactor(value: number): void;

    /** Gas Cost */
    getGasCost(): number;

    setGasCost(value: number): void;

    /** Gas Cost Factor */
    getGasCostFactor(): number;

    setGasCostFactor(value: number): void;

    /** Time Cost */
    getResearchTime(): number;

    setResearchTime(value: number): void;

    /** Time Cost Factor */
    getResearchTimeFactor(): number;

    setResearchTimeFactor(value: number): void;

    /** DatReqs Index */
    getRequirementIndex(): number;

    setRequirementIndex(value: number): void;

    /** Icon */
    getIcon(): number;

    setIcon(value: number): void;

    /** Label */
    getLabel(): number;

    setLabel(value: number): void;

    /** Race */
    getRace(): number;

    setRace(value: number): void;

    /** Max Repeats */
    getMaximumLevel(): number;

    setMaximumLevel(value: number): void;

    /** BroodWar */
    getBroodWarFlag(): number;

    setBroodWarFlag(value: number): void;
}

/**
 * The techdata.dat values of the tech types: raw values of the current game.
 */
interface TechTypeData {
    /** Ore Cost */
    getMineralCost(): number;

    setMineralCost(value: number): void;

    /** Gas Cost */
    getGasCost(): number;

    setGasCost(value: number): void;

    /** Time Cost */
    getResearchTime(): number;

    setResearchTime(value: number): void;

    /** Energy */
    getEnergyCost(): number;

    setEnergyCost(value: number): void;

    /** DatReqs Index */
    getResearchRequirementIndex(): number;

    setResearchRequirementIndex(value: number): void;

    /** UseReqs Index */
    getUseRequirementIndex(): number;

    setUseRequirementIndex(value: number): void;

    /** Icon */
    getIcon(): number;

    setIcon(value: number): void;

    /** Label */
    getLabel(): number;

    setLabel(value: number): void;

    /** Race */
    getRace(): number;

    setRace(value: number): void;

    /** Researched */
    getResearchedFlag(): number;

    setResearchedFlag(value: number): void;

    /** BroodWar */
    getBroodWarFlag(): number;

    setBroodWarFlag(value: number): void;
}

/**
 * The orders.dat values of the orders: raw values of the current game.
 */
interface OrderData {
    /** Label */
    getLabel(): number;

    setLabel(value: number): void;

    /** Use Weapon Targeting */
    getUseWeaponTargeting(): number;

    setUseWeaponTargeting(value: number): void;

    /** Unused1 */
    getUnused1(): number;

    setUnused1(value: number): void;

    /** Unused2 */
    getUnused2(): number;

    setUnused2(value: number): void;

    /** Subunit Inheritance */
    getSubunitInheritance(): number;

    setSubunitInheritance(value: number): void;

    /** Unused3 */
    getUnused3(): number;

    setUnused3(value: number): void;

    /** Can Be Interrupted */
    getCanBeInterrupted(): number;

    setCanBeInterrupted(value: number): void;

    /** Can Be Lifted */
    getCanBeLifted(): number;

    setCanBeLifted(value: number): void;

    /** Can Be Queued */
    getCanBeQueued(): number;

    setCanBeQueued(value: number): void;

    /** Disabling Keeps Target */
    getDisablingKeepsTarget(): number;

    setDisablingKeepsTarget(value: number): void;

    /** Can Be Obstructed */
    getCanBeObstructed(): number;

    setCanBeObstructed(value: number): void;

    /** Fleeable */
    getFleeable(): number;

    setFleeable(value: number): void;

    /** Unused4 */
    getUnused4(): number;

    setUnused4(value: number): void;

    /** Weapon */
    getWeapon(): WeaponType | null;

    setWeapon(weaponType: WeaponType | null): void;

    /** Energy */
    getTechnology(): TechType | null;

    setTechnology(techType: TechType | null): void;

    /** Animation */
    getAnimation(): number;

    setAnimation(value: number): void;

    /** Highlight */
    getHighlight(): number;

    setHighlight(value: number): void;

    /** DatReqs Index */
    getRequirementIndex(): number;

    setRequirementIndex(value: number): void;

    /** Obscured */
    getObscuredOrder(): Order | null;

    setObscuredOrder(order: Order | null): void;
}

// END GENERATED DAT

/**
 * A unit type (units.dat); its DAT values are those of {@link UnitTypeData}.
 */
interface UnitType extends GameType, UnitTypeData {
    /**
     * @returns the shown maximum hit points: {@link UnitTypeData.getHitPoints} / 256
     */
    getMaxHp(): number;

    /**
     * Sets the shown maximum hit points: {@link UnitTypeData.setHitPoints} with the value times 256, rounded.
     */
    setMaxHp(hp: number): void;

    /**
     * @returns the maximum shields, the same as {@link UnitTypeData.getShieldAmount}
     */
    getMaxShields(): number;

    /**
     * Sets the maximum shields, the same as {@link UnitTypeData.setShieldAmount}.
     */
    setMaxShields(shields: number): void;

    /**
     * Shows a name instead of the type's stat_txt name (tooltips, the selection, messages), also over the map's name of the type; null shows
     * the game's name again. Shared code sets it on every peer, local code only on this machine.
     */
    setName(text: string | null): void;

    /**
     * @returns the strength of the unit type against air or ground targets for the computer players
     */
    getAiStrength(air: boolean): number;

    setAiStrength(air: boolean, strength: number): void;
}

/**
 * A weapon type (weapons.dat).
 */
interface WeaponType extends GameType, WeaponTypeData {
}

/**
 * A flingy type (flingy.dat), named after the sprite type of its sprite.
 */
interface FlingyType extends GameType, FlingyTypeData {
}

/**
 * A sprite type (sprites.dat), named after the image type of its image.
 */
interface SpriteType extends GameType, SpriteTypeData {
}

/**
 * An upgrade type (upgrades.dat).
 */
interface UpgradeType extends GameType, UpgradeTypeData {
}

/**
 * A technology (techdata.dat).
 */
interface TechType extends GameType, TechTypeData {
}

/**
 * An order (orders.dat).
 */
interface Order extends GameType, OrderData {
}

/**
 * An image type (images.dat), named after its GRP path in images.tbl ("unit\terran\marine.grp" is UnitTerranMarine).
 */
interface ImageType extends GameType, ImageTypeData {
    /**
     * @returns the image type whose HD anim this image type draws (images.rel), this image type for one with its own anim
     */
    getAnimImageType(): ImageType;

    /**
     * @returns the damage overlay byte: twice the flame positions of the image's damage overlay, a unit's damage states
     */
    getDamageOverlayCount(): number;

    setDamageOverlayCount(count: number): void;
}

/**
 * A key by its Windows virtual-key code.
 */
interface Key extends GameType {
}

interface MouseButton extends GameType {
}

/**
 * A unit group of the trigger conditions and actions, which counts where a unit type does.
 */
interface UnitGroup extends GameType {
}

interface ScoreType extends GameType {
}

interface ResourceType extends GameType {
}

interface Alliance extends GameType {
}

/**
 * A game speed, which names an entry of the frame duration table.
 */
interface GameSpeed extends GameType {
    /**
     * @returns the milliseconds of a frame at the speed
     */
    getFrameDuration(): number;

    /**
     * Sets the milliseconds of a frame at the speed, as an EUD write does: the game takes it at once unless the game name overrides the
     * speed; the turn timeouts of a network game keep their last durations.
     */
    setFrameDuration(milliseconds: number): void;
}

/**
 * A cheat; its id is its bit of the cheat flags.
 */
interface Cheat extends GameType {
}

/**
 * A game type of the game header.
 */
interface GameMode extends GameType {
}

/**
 * A unit size of units.dat.
 */
interface UnitSize extends GameType {
}

/**
 * A damage type of weapons.dat.
 */
interface DamageType extends GameType {
    /**
     * @returns the factor that scales the damage against the size, 1 for full damage
     */
    getMultiplier(size: UnitSize): number;

    /**
     * Sets the factor, stored in 1/256.
     */
    setMultiplier(size: UnitSize, factor: number): void;
}

/**
 * An elevation flag of a location; its id is its bit.
 */
interface LocationFlag extends GameType {
}

/**
 * A map position in pixels.
 */
interface Position {
    getX(): number;

    getY(): number;
}

interface PositionStatic {
    of(x: number, y: number): Position;
}

declare const Position: PositionStatic;

/**
 * Converts durations to frames, rounded to the nearest frame (halves up).
 */
interface TimeStatic {
    /**
     * @returns the frames of seconds of the trigger clock (16 frames a second), the unit of the countdown timer and the elapsed time
     */
    fromSeconds(seconds: number): number;

    /**
     * @returns the frames of minutes of the trigger clock
     */
    fromMinutes(minutes: number): number;

    /**
     * @returns the frames of real milliseconds at the fastest speed (42 ms a frame)
     */
    fromMilliseconds(milliseconds: number): number;
}

declare const Time: TimeStatic;

/**
 * A unit filter: a function of the unit, or a unit type and an owner, each optional.
 */
type UnitFilter = ((unit: Unit) => boolean) | { type?: UnitType; owner?: Player };

/**
 * A unit: the same object while the unit lives. Every method but {@link Unit.exists} and {@link Unit.getId} throws once the unit is gone.
 * Hit points, shields and energy are the shown values.
 */
interface Unit {
    /**
     * @returns the unit's slot index, which the lower layer {@link core} takes
     */
    getId(): number;

    exists(): boolean;

    getType(): UnitType;

    getOwner(): Player;

    /**
     * Gives the unit to a player as Give Units to Player does.
     */
    setOwner(player: Player): boolean;

    getX(): number;

    getY(): number;

    getPosition(): Position;

    /**
     * Moves the unit as Move Unit does.
     */
    setPosition(position: Position): boolean;

    getHp(): number;

    setHp(hp: number): void;

    /**
     * @returns the shown maximum hit points of the unit's type in units.dat
     */
    getMaxHp(): number;

    getShields(): number;

    /**
     * @returns false for a unit without shields
     */
    setShields(shields: number): boolean;

    /**
     * @returns the maximum shields of the unit's type in units.dat, 0 for a type without shields
     */
    getMaxShields(): number;

    getEnergy(): number;

    /**
     * @returns false for a unit without energy
     */
    setEnergy(energy: number): boolean;

    getMaxEnergy(): number;

    /**
     * Sets the hit points to a percentage of the maximum, as Modify Unit Hit Points does.
     */
    setHpPercent(percent: number): void;

    setShieldsPercent(percent: number): boolean;

    setEnergyPercent(percent: number): boolean;

    getOrder(): Order;

    getTarget(): Unit | null;

    getTargetPosition(): Position;

    isCompleted(): boolean;

    isCloaked(): boolean;

    isBurrowed(): boolean;

    isInvincible(): boolean;

    /**
     * @returns the image type of the unit's main image
     */
    getImageType(): ImageType;

    setInvincible(invincible: boolean): void;

    /**
     * @returns the disabled status of a doodad, or of a unit under Lockdown, Stasis Field or Maelstrom
     */
    isDisabled(): boolean;

    enableDoodad(): void;

    disableDoodad(): void;

    /**
     * @returns the resource amount of a mineral field or geyser
     */
    getResources(): number;

    /**
     * @returns false for a unit that holds no resources
     */
    setResources(amount: number): boolean;

    /**
     * @returns the Interceptors or Scarabs of a Carrier or Reaver
     */
    getHangarCount(): number;

    /**
     * Builds up to count Interceptors or Scarabs while the hangar takes another one.
     *
     * @returns false for a unit without a hangar
     */
    addHangarCount(count: number): boolean;

    kill(): boolean;

    remove(): boolean;

    /**
     * Issues an order to a target unit or a position; without a target the unit's own position is the target. With {queued: true} the order
     * goes behind the unit's queue when the order takes one, as a queued command does.
     */
    issueOrder(order: Order, target?: Unit | Position | null, options?: IssueOrderOptions): void;

    /**
     * @returns the orders waiting behind the current one, first to last
     */
    getOrderQueue(): QueuedOrder[];

    getSecondaryOrder(): Order;

    getSecondaryOrderState(): number;

    /**
     * Sets the secondary order as the native routine does: a changed order starts from its first state.
     */
    setSecondaryOrder(order: Order): void;

    /**
     * @returns the unit's command card button set, null for a set outside the table
     */
    getButtonSet(): ButtonSet | null;

    /**
     * Sets the unit's button set; the native routines of morphs, completion and burrowing set it again.
     */
    setButtonSet(set: ButtonSet | UnitType | number): void;

    /**
     * @returns the shown hit points a unit under construction gains each frame
     */
    getHpGain(): number;

    setHpGain(hp: number): void;

    /**
     * @returns the shown shields a unit under construction gains each frame
     */
    getShieldGain(): number;

    setShieldGain(shields: number): void;

    /**
     * @returns the shown hit points the unit had before it morphed, which a cancelled Zerg building gives back
     */
    getPreviousHp(): number;

    /**
     * @returns true when the player sees the unit: in the player's sight and, when cloaked or burrowed, detected
     */
    isVisibleTo(player: Player): boolean;

    isDetectedBy(player: Player): boolean;

    /**
     * @returns the players whose sight covers the unit
     */
    getVisibleTo(): Player[];

    /**
     * @returns the unit's strength against air or ground targets for the computer players
     */
    getAiStrength(air: boolean): number;

    /**
     * The unit's status timers and its remove timer; one unit always has the same object.
     */
    readonly timers: UnitTimers;

    getDefensiveMatrixHp(): number;

    setDefensiveMatrixHp(hp: number): void;

    /**
     * Setting a count shows its overlay; 0 removes the spores.
     */
    getAcidSporeCount(): number;

    setAcidSporeCount(count: number): void;

    isParasitedBy(player: Player): boolean;

    setParasitedBy(player: Player, parasited: boolean): void;

    isBlind(): boolean;

    isBlindedBy(player: Player): boolean;

    setBlindedBy(player: Player, blinded: boolean): void;

    /**
     * The weapon and spell cooldowns in frames.
     */
    getGroundCooldown(): number;

    setGroundCooldown(frames: number): void;

    getAirCooldown(): number;

    setAirCooldown(frames: number): void;

    getSpellCooldown(): number;

    setSpellCooldown(frames: number): void;

    getKillCount(): number;

    setKillCount(kills: number): void;

    /**
     * @returns the unit types in the build queue from the current one
     */
    getBuildQueue(): UnitType[];

    /**
     * The frames left of the current construction or training.
     */
    getRemainingBuildTime(): number;

    setRemainingBuildTime(frames: number): void;

    /**
     * The frames left of the current research or upgrade.
     */
    getRemainingResearchTime(): number;

    setRemainingResearchTime(frames: number): void;

    getResearch(): TechType | null;

    getUpgrade(): UpgradeType | null;

    getAddon(): Unit | null;

    getRallyPosition(): Position;

    /**
     * @returns false for a unit type without a rally point
     */
    setRallyPosition(position: Position): boolean;

    getRallyUnit(): Unit | null;

    /**
     * @returns false for a unit type without a rally point
     */
    setRallyUnit(unit: Unit | null): boolean;

    /**
     * The direction from 0 to 255; setting it turns the unit at once.
     */
    getDirection(): number;

    setDirection(direction: number): void;

    /**
     * @returns the speed in 1/256 pixels per frame
     */
    getCurrentSpeed(): number;

    getTopSpeed(): number;

    getMoveTarget(): Position;

    setMoveTarget(position: Position): void;

    /**
     * Changes the unit's type through the morph routine, which keeps the unit, scales its hit points and rebuilds its sprite.
     *
     * @returns false for a subunit, a subunit type, or a type whose turret does not match the unit's
     */
    setType(type: UnitType): boolean;

    /**
     * @returns the unit's sprite, null for none
     */
    getSprite(): Sprite | null;

    /**
     * Runs the use-weapon routine of the unit scripts: the bullets of the weapon toward the unit's order target, two for a weapon of damage
     * factor 2, none for a weapon without a flingy; only in shared code.
     */
    useWeapon(weaponType: WeaponType): void;
}

/**
 * The options of {@link Unit.issueOrder}.
 */
interface IssueOrderOptions {
    queued?: boolean;
}

/**
 * An order waiting in a unit's queue ({@link Unit.getOrderQueue}).
 */
interface QueuedOrder {
    readonly order: Order;
    readonly target: Unit | null;
    readonly position: Position;
    /**
     * The unit type the order builds or trains, null for none.
     */
    readonly unitType: UnitType | null;
}

/**
 * The timers of a unit ({@link Unit.timers}). The status timers count status steps of eight frames: a timer that starts runs the effect's apply
 * routine (overlay, disabling, speed) before it takes the value; 0 runs the effect's end routine. Every method throws once the unit is gone.
 */
interface UnitTimers {
    /**
     * @returns the Stim Packs timer in status steps
     */
    getStim(): number;

    /**
     * Sets the Stim Packs timer in status steps.
     */
    setStim(timer: number): void;

    /**
     * @returns the Ensnare timer in status steps
     */
    getEnsnare(): number;

    /**
     * Sets the Ensnare timer in status steps.
     */
    setEnsnare(timer: number): void;

    /**
     * @returns the Lockdown timer in status steps
     */
    getLockdown(): number;

    /**
     * Sets the Lockdown timer in status steps.
     */
    setLockdown(timer: number): void;

    /**
     * @returns the Stasis Field timer in status steps
     */
    getStasis(): number;

    /**
     * Sets the Stasis Field timer in status steps.
     */
    setStasis(timer: number): void;

    /**
     * @returns the Maelstrom timer in status steps
     */
    getMaelstrom(): number;

    /**
     * Sets the Maelstrom timer in status steps.
     */
    setMaelstrom(timer: number): void;

    /**
     * @returns the Plague timer in status steps
     */
    getPlague(): number;

    /**
     * Sets the Plague timer in status steps.
     */
    setPlague(timer: number): void;

    /**
     * @returns the Irradiate timer in status steps
     */
    getIrradiate(): number;

    /**
     * Sets the Irradiate timer in status steps.
     */
    setIrradiate(timer: number): void;

    /**
     * @returns the Psionic Storm timer in status steps
     */
    getStorm(): number;

    /**
     * Sets the Psionic Storm timer in status steps.
     */
    setStorm(timer: number): void;

    /**
     * @returns the Defensive Matrix timer in status steps
     */
    getDefensiveMatrix(): number;

    /**
     * Sets the Defensive Matrix timer in status steps.
     */
    setDefensiveMatrix(timer: number): void;

    /**
     * @returns the frames until a timed unit (a hallucination, a Broodling) is removed, 0 for an untimed unit
     */
    getRemove(): number;

    /**
     * Sets the frames until the unit is removed.
     */
    setRemove(frames: number): void;
}

/**
 * The options of a unit count: only the units at a location, only completed units.
 */
interface UnitCountOptions {
    location?: Location | null;
    completed?: boolean;
}

/**
 * The options of a unit removal: only the units at a location, at most count units (all without one).
 */
interface UnitRemovalOptions {
    location?: Location | null;
    count?: number;
}

/**
 * A player by slot, from 0 (the original player 1).
 */
interface Player {
    getId(): number;

    getName(): string;

    /**
     * @returns the race from 0 (Zerg) to 2 (Protoss)
     */
    getRace(): number;

    isHuman(): boolean;

    getMinerals(): number;

    setMinerals(minerals: number): void;

    addMinerals(minerals: number): void;

    subtractMinerals(minerals: number): void;

    getGas(): number;

    setGas(gas: number): void;

    addGas(gas: number): void;

    subtractGas(gas: number): void;

    /**
     * Ends the game for the player as Victory does.
     */
    win(): boolean;

    /**
     * Ends the game for the player as Defeat does; false after a victory.
     */
    lose(): boolean;

    /**
     * Ends the game for the player as Draw does; false after a victory.
     */
    draw(): boolean;

    /**
     * @param race the race from 0 (Zerg) to 2 (Protoss), the player's race without one
     */
    getSupplyUsed(race?: number): number;

    getSupplyProvided(race?: number): number;

    getSupplyMax(race?: number): number;

    isAlliedWith(player: Player): boolean;

    isEnemyOf(player: Player): boolean;

    getAlliance(player: Player): Alliance;

    /**
     * Sets the alliance toward another player as Set Alliance Status does.
     */
    setAlliance(player: Player, alliance: Alliance): void;

    /**
     * @returns minerals, gas, or minerals and gas, as the Accumulate condition reads them
     */
    getResources(resourceType: ResourceType): number;

    /**
     * @returns the opponents the Opponents condition counts
     */
    countOpponents(): number;

    /**
     * Starts an AI script by its four-character name ("EnBk"), at a location when one is given.
     *
     * @returns false when the player may not run it
     */
    runAiScript(name: string, location?: Location | null): boolean;

    setMissionObjectives(text: string): void;

    /**
     * Shows a text on the player's screen; with {forced: false} only when the subtitle option is on.
     */
    print(text: string, options?: { forced?: boolean }): void;

    pingMinimap(position: Position): void;

    /**
     * Plays a sound file of the map archive by its path.
     */
    playSound(path: string): void;

    /**
     * Shows the talking portrait of a unit type for a number of frames.
     */
    showTalkingPortrait(unitType: UnitType, frames: number): void;

    setUnitSpeechMuted(muted: boolean): void;

    /**
     * Centers the screen on a position when the player is the local player; the screens of the other peers stay.
     */
    centerView(position: Position): void;

    /**
     * @returns the player's selection, which the selection commands change and every peer holds: the live units in selection order, empty
     *          for players 8 to 11
     */
    getSelection(): Unit[];

    /**
     * Replaces the player's selection as a selection command of the player does, on every peer at the same frame, and shows it as the local
     * selection when the player is the local player. Shared code only. The command's rules may leave units out: a hidden unit, and in a
     * selection of more than one unit a building, a unit of another player or one that cannot join a group. An empty array clears the
     * selection.
     *
     * @param units at most 12 units that exist; more throw a RangeError
     * @returns true if every unit was selected, false if the rules left some out
     */
    setSelection(units: Unit[]): boolean;

    /**
     * The player's upgrade levels by upgrade type; one player always has the same object.
     */
    readonly upgrades: PlayerUpgrades;
    /**
     * The player's technologies by tech type.
     */
    readonly techs: PlayerTechs;
    /**
     * The player's units: listing, counting, killing and removing them, and their production availability.
     */
    readonly units: PlayerUnits;
    /**
     * The player's death counters of the Deaths condition and the Set Deaths action.
     */
    readonly deaths: PlayerDeaths;
    /**
     * The player's kill counters of the Kill condition.
     */
    readonly kills: PlayerKills;
    /**
     * The player's scores of the Score condition and the Set Score action.
     */
    readonly score: PlayerScore;
    /**
     * The player's shared vision and the tiles the player sees.
     */
    readonly vision: PlayerVision;

    /**
     * @returns the minerals of the gathered total of the score
     */
    getGatheredMinerals(): number;

    setGatheredMinerals(minerals: number): void;

    getGatheredGas(): number;

    setGatheredGas(gas: number): void;

    /**
     * @returns the pauses a player from 0 to 7 has left in a network game
     */
    getPausesLeft(): number;

    setPausesLeft(pauses: number): void;

    /**
     * @returns the start location, which the computer players and the start screen read, or null for a player without one
     */
    getStartLocation(): Position | null;

    /**
     * @returns false for a player without a start location
     */
    setStartLocation(position: Position): boolean;

    /**
     * @returns the palette indices of the player's unit color row, the eight remapped colors of a unit frame; the first one is also the HD team
     *          color
     */
    getColor(): number[];

    /**
     * Sets the eight palette indices (0 to 255) of the player's unit color row, as an EUD write does; the next drawn frame shows them. Shared code
     * sets them on every peer, local code only on this machine. A player shown in a custom color of the game keeps it as its HD team color.
     */
    setColor(paletteIndices: number[]): void;

    /**
     * @returns the palette index of the player's minimap dots
     */
    getMinimapColor(): number;

    /**
     * Sets the palette index of the player's minimap dots; a player shown in a custom color of the game keeps it on the minimap.
     */
    setMinimapColor(paletteIndex: number): void;
}

/**
 * The vision of a player ({@link Player.vision}).
 */
interface PlayerVision {
    /**
     * @returns the players that see what this player's units see, this player included
     */
    getSharedWith(): Player[];

    /**
     * Sets the players that see what this player's units see, without the vision command's permission test; the player sees its own units'
     * sight only while it is among them.
     */
    setSharedWith(players: Player[]): void;

    isSharedWith(player: Player): boolean;

    shareWith(player: Player, shared: boolean): void;

    /**
     * @returns true while the player sees the position
     */
    canSee(position: Position): boolean;

    hasExplored(position: Position): boolean;

    /**
     * Marks the tile at a position explored by the player, still unseen until a unit of the player sees it, or unexplored and unseen; for a
     * player from 0 to 7, only in shared code.
     */
    setExplored(position: Position, explored: boolean): void;
}

/**
 * The upgrade levels of a player ({@link Player.upgrades}).
 */
interface PlayerUpgrades {
    /**
     * @returns the upgrade's current level
     */
    getLevel(upgrade: UpgradeType): number;

    /**
     * Sets the level and refreshes the upgrade flags of the player's units, as a finished upgrade does.
     */
    setLevel(upgrade: UpgradeType, level: number): void;

    /**
     * @returns the highest level the player may research
     */
    getMaxLevel(upgrade: UpgradeType): number;

    /**
     * Sets the highest level the player may research.
     */
    setMaxLevel(upgrade: UpgradeType, level: number): void;

    /**
     * @returns true while one of the player's buildings researches the upgrade
     */
    isInProgress(upgrade: UpgradeType): boolean;
}

/**
 * The technologies of a player ({@link Player.techs}).
 */
interface PlayerTechs {
    /**
     * @returns true when the player has researched the technology
     */
    isResearched(tech: TechType): boolean;

    /**
     * Marks the technology researched or not researched.
     */
    setResearched(tech: TechType, researched: boolean): void;

    /**
     * @returns true when the player may research the technology
     */
    isAvailable(tech: TechType): boolean;

    /**
     * Allows or forbids researching the technology.
     */
    setAvailable(tech: TechType, available: boolean): void;

    /**
     * @returns true while one of the player's buildings researches the technology
     */
    isInProgress(tech: TechType): boolean;
}

/**
 * The units of a player ({@link Player.units}).
 */
interface PlayerUnits {
    /**
     * @returns the player's live units
     */
    getAll(): Unit[];

    /**
     * Counts the player's units as the Command and Bring conditions do.
     */
    count(unit: UnitType | UnitGroup, options?: UnitCountOptions): number;

    /**
     * Kills the player's units as Kill Unit does.
     */
    kill(unit: UnitType | UnitGroup, options?: UnitRemovalOptions): void;

    /**
     * Removes the player's units as Remove Unit does.
     */
    remove(unit: UnitType | UnitGroup, options?: UnitRemovalOptions): void;

    /**
     * @returns the production availability of the map settings; a game without map settings allows every unit type
     */
    isAvailable(unitType: UnitType): boolean;

    /**
     * Sets the production availability of the map settings; a game without map settings throws when a type is made unavailable.
     */
    setAvailable(unitType: UnitType, available: boolean): void;
}

/**
 * The death counters of a player by unit type or unit group ({@link Player.deaths}).
 */
interface PlayerDeaths {
    /**
     * @returns the deaths the Deaths condition compares
     */
    get(unit: UnitType | UnitGroup): number;

    /**
     * Sets the deaths as Set Deaths does.
     */
    set(unit: UnitType | UnitGroup, amount: number): void;

    /**
     * Adds to the deaths as Set Deaths does.
     */
    add(unit: UnitType | UnitGroup, amount: number): void;

    /**
     * Subtracts from the deaths as Set Deaths does.
     */
    subtract(unit: UnitType | UnitGroup, amount: number): void;
}

/**
 * The kill counters of a player by unit type or unit group ({@link Player.kills}).
 */
interface PlayerKills {
    /**
     * @returns the kills the Kill condition compares
     */
    get(unit: UnitType | UnitGroup): number;
}

/**
 * The scores of a player by score type ({@link Player.score}); the writes take players 0 to 7.
 */
interface PlayerScore {
    /**
     * @returns the score the Score condition compares
     */
    get(scoreType: ScoreType): number;

    /**
     * Sets the score as Set Score does.
     */
    set(scoreType: ScoreType, amount: number): void;

    /**
     * Adds to the score as Set Score does.
     */
    add(scoreType: ScoreType, amount: number): void;

    /**
     * Subtracts from the score as Set Score does.
     */
    subtract(scoreType: ScoreType, amount: number): void;
}

/**
 * A map location; the bounds are map pixels.
 */
interface Location {
    getId(): number;

    getName(): string | null;

    getLeft(): number;

    getTop(): number;

    getRight(): number;

    getBottom(): number;

    getCenter(): Position;

    setBounds(left: number, top: number, right: number, bottom: number): void;

    /**
     * Moves the location so the position is its center, kept inside the map as Move Location keeps it.
     */
    setCenter(position: Position): void;

    /**
     * @returns true when the flag is set, which leaves the units at its elevation out of the location
     */
    hasFlag(flag: LocationFlag): boolean;

    setFlag(flag: LocationFlag, set: boolean): void;
}

interface Timer {
    getId(): number;

    /**
     * Cancels the timer; only in shared code, local code throws a TypeError.
     *
     * @returns false when the timer was already cancelled
     */
    cancel(): boolean;
}

/**
 * The options of a leaderboard: the goal form with a goal.
 */
interface LeaderboardOptions {
    goal?: number;
}

/**
 * The options of a control leaderboard: the goal form with a goal, Leader Board Control At Location with a location.
 */
interface ControlLeaderboardOptions extends LeaderboardOptions {
    location?: Location | null;
}

/**
 * The shared game: the same on every peer. Its groups are {@link map}, {@link players}, {@link units}, {@link switches}, {@link countdown} and
 * {@link leaderboard}.
 */
interface Game {
    /**
     * @returns the number of the current game frame
     */
    getFrame(): number;

    /**
     * @returns the seconds of the trigger clock (16 frames a second) that the Elapsed Time condition compares
     */
    getElapsedTime(): number;

    /**
     * @returns true inside a decision hook and in local code, where the game state cannot change
     */
    isReadOnly(): boolean;

    /**
     * Shows a text to all players.
     */
    print(text: string): void;

    /**
     * @returns true while the game is paused, by {@link Game.pause}, Pause Game or the pause of the game menu
     */
    isPaused(): boolean;

    /**
     * Pauses the game as the Pause Game action does, at this frame's trigger step: nothing in a multiplayer session or while it is paused so
     * already. The paused game runs no frame, but onFrame keeps running at every trigger step with the frame number unchanged, so that it can
     * call {@link Game.resume}. Only in shared code.
     */
    pause(): void;

    /**
     * Resumes the game that {@link Game.pause} or Pause Game paused, as the Unpause Game action does, at this frame's trigger step. Only in shared
     * code.
     */
    resume(): void;

    /**
     * Calls a function every given number of frames, first that many frames from now ({@link Time}), at the frame start before the hooks
     * of the frame; only in shared code, local code throws a TypeError.
     */
    every(frames: number, callback: () => void): Timer;

    /**
     * Calls a function once, the given number of frames from now ({@link Time}), at the frame start before the hooks of the frame; only in
     * shared code, local code throws a TypeError.
     */
    after(frames: number, callback: () => void): Timer;

    getSpeed(): GameSpeed;

    isCheatEnabled(cheat: Cheat): boolean;

    /**
     * Turns a cheat on or off and keeps it so: sets or clears its bit of the cheat flags ({@link core.game.setCheatFlags}), as an EUD write of
     * the flags does, and writes the bit again at the start of every frame, as the always-running trigger of an EUD map does. It works in a
     * network game and where the game options forbid cheats. A reveal cheat (BlackSheepWall, WarAintWhatItUsedToBe) reveals nothing at once:
     * while its bit is set the game no longer marks the tiles unseen every 100 frames, so what the players saw stays seen; in a network game
     * that tile update clears the reveal bits first and the next frame sets them again. Only in shared code. The native cheat routine, which
     * reveals the map at once, is {@link core.game.applyCheats}.
     */
    setCheatEnabled(cheat: Cheat, enabled: boolean): void;

    /**
     * Creates a sprite that lives as long as its image scripts run, as the game creates the sprite of a spell or of dust, with its elevation and
     * its visibility from the tile below it; only in shared code.
     *
     * @returns the sprite, or null when the pools are full
     */
    createEffect(spriteType: SpriteType, position: Position, options?: EffectOptions): Sprite | null;

    /**
     * @returns the active bullets, in the order of the active list
     */
    getBullets(): Bullet[];

    /**
     * Creates a bullet of a weapon at a position for a unit, aimed at the unit's order target unit, else its order target position, as the game
     * creates one; only in shared code.
     *
     * @returns the bullet, or null when the game creates none (no free entry, no sprite, an attack the unit cannot make)
     */
    createBullet(weaponType: WeaponType, source: Unit, position: Position): Bullet | null;

    /**
     * The map: its size, tileset, terrain, locations and resource files.
     */
    readonly map: GameMap;
    /**
     * The players by slot.
     */
    readonly players: Players;
    /**
     * The units of the game: finding, counting and creating them.
     */
    readonly units: Units;
    /**
     * The switches of the trigger state.
     */
    readonly switches: Switches;
    /**
     * The countdown timer of the trigger state.
     */
    readonly countdown: Countdown;
    /**
     * The leaderboards of the leaderboard actions.
     */
    readonly leaderboard: Leaderboard;
    /**
     * The settings the game started with.
     */
    readonly settings: GameSettings;
    /**
     * The sight of the players.
     */
    readonly vision: GameVision;
    /**
     * The combat tables.
     */
    readonly combat: GameCombat;
}

/**
 * The options of {@link Game.createEffect}.
 */
interface EffectOptions {
    /**
     * The owner of the sprite, by default the player in slot 0.
     */
    owner?: Player;
    /**
     * The elevation from 0 to 255, by default 4, the elevation a new sprite starts with.
     */
    elevation?: number;
}

/**
 * A sprite of the sprite pool. The object stays with its sprite: once the sprite is gone and its slot holds another one, exists() is false and
 * every other method throws an Error. The pools of sprites and images also hold the buildings the local player remembers in the fog of war,
 * so the slots can differ from peer to peer in a network game: shared code may show them but should not let them decide the game.
 */
interface Sprite {
    /**
     * @returns the slot in the sprite pool ({@link CoreSprite})
     */
    getId(): number;

    exists(): boolean;

    getType(): SpriteType;

    getPosition(): Position;

    getOwner(): Player | null;

    getElevation(): number;

    /**
     * @returns the images of the sprite, from the one drawn on top; the local selection circles and health bars are not among them
     */
    getImages(): SpriteImage[];

    getMainImage(): SpriteImage | null;

    isVisibleTo(player: Player): boolean;
}

/**
 * An image of the image pool ({@link CoreImage}). The object stays with its image: once the image is gone and its slot holds another one,
 * exists() is false and every other method throws an Error. Its frame, draw function and graphics change only what is drawn: shared code
 * changes them on every peer, local code on the local machine only.
 */
interface SpriteImage {
    getId(): number;

    exists(): boolean;

    getType(): ImageType;

    getSprite(): Sprite | null;

    /**
     * @returns the drawn frame
     */
    getFrame(): number;

    /**
     * Draws a frame (0 to 65535) until the image's own frame changes, as an EUD write of the frame does; null draws the image's own frame
     * again.
     */
    setFrame(frame: number | null): void;

    /**
     * @returns the frame offset of the image's direction inside its frame group
     */
    getDirection(): number;

    getDrawFunction(): DrawFunction;

    /**
     * Draws with a draw function until the image's own draw function changes, as an EUD write of the draw function does; null draws with
     * the image's own again.
     */
    setDrawFunction(drawFunction: DrawFunction | null): void;

    getOffsetX(): number;

    getOffsetY(): number;

    /**
     * Replaces the graphics of this image while it lives; they win over those of its unit and of its image type.
     */
    replaceGraphics(images: ImageGraphics): void;

    restoreGraphics(): void;
}

/**
 * A bullet of the bullet table, from its creation until it is released after dying. The object stays with its bullet: once the bullet is gone
 * and its entry holds another one, exists() is false and every other method throws an Error.
 */
interface Bullet {
    /**
     * @returns the entry in the bullet table ({@link CoreBullet})
     */
    getId(): number;

    exists(): boolean;

    getWeapon(): WeaponType;

    getFlingyType(): FlingyType;

    getPosition(): Position;

    getPlayer(): Player | null;

    /**
     * @returns the unit that fired the bullet, null once it is gone
     */
    getSource(): Unit | null;

    getTarget(): Unit | null;

    /**
     * @returns the center of the hit
     */
    getTargetPosition(): Position;

    /**
     * Sets the target unit (null for none) or the target position, as an EUD write of the bullet does: a following, bouncing or near-target
     * bullet turns to a new target unit from the next frame, a moving bullet keeps its course; the hit damages the target unit and is centered
     * on the target position. Only in shared code.
     */
    setTarget(target: Unit | Position | null): void;

    /**
     * @returns the frames a moving bullet still flies, or a bullet that persists on its target position still hits
     */
    getRemainingTime(): number;

    /**
     * Sets the remaining frames, 0 to 255; only in shared code.
     */
    setRemainingTime(frames: number): void;

    getRemainingBounces(): number;

    getState(): BulletState;

    /**
     * @returns the direction from 0 to 255
     */
    getDirection(): number;

    /**
     * @returns the sprite, null once its last image ended
     */
    getSprite(): Sprite | null;
}

/**
 * The settings the game started with ({@link Game.settings}).
 */
interface GameSettings {
    getMode(): GameMode;

    getSubtype(): number;

    isMultiplayer(): boolean;

    /**
     * @returns the raw victory condition of the game options
     */
    getVictoryCondition(): number;

    /**
     * @returns the raw resource setting of the game options
     */
    getResourceType(): number;

    getStartingMinerals(): number;

    /**
     * @returns the raw starting units setting of the game options
     */
    getStartingUnits(): number;

    getTournamentMode(): number;

    /**
     * @returns true when the game options permit the cheat command
     */
    areCheatsAllowed(): boolean;

    /**
     * @returns true when Brood War content is on
     */
    isExpansion(): boolean;

    setExpansion(expansion: boolean): void;
}

/**
 * The options of {@link GameVision.reveal}.
 */
interface RevealOptions {
    air?: boolean;
}

/**
 * The sight of the players ({@link Game.vision}).
 */
interface GameVision {
    /**
     * Reveals the tiles around a position to players from 0 to 7 as a unit's sight with a sight range from 0 to 11 does, until the game marks
     * the tiles unseen again; {air: true} sees over higher ground.
     */
    reveal(players: Player | Player[], position: Position, sightRange: number, options?: RevealOptions): void;

    /**
     * @returns the fog brightness of visible tiles (0 to 255, default 31): the world's fog draws visible tiles at this level, explored tiles at
     *          half of it and unexplored tiles black; the minimap does not use it
     */
    getFogBrightness(): number;

    /**
     * Sets the fog brightness of visible tiles; shared code sets it on every peer, local code only on this machine.
     */
    setFogBrightness(level: number): void;
}

/**
 * The combat tables ({@link Game.combat}); the damage multipliers are on {@link DamageType}.
 */
interface GameCombat {
    /**
     * @returns the distances in pixels a fleeing unit tries, in order
     */
    getFleeDistances(): number[];

    setFleeDistances(distances: number[]): void;
}

declare const game: Game;

/**
 * The map of the game ({@link Game.map}); positions and sizes are map pixels, tile positions and sizes tiles of 32 pixels.
 */
interface GameMap {
    /**
     * @returns the map width in pixels
     */
    getWidth(): number;

    /**
     * @returns the map height in pixels
     */
    getHeight(): number;

    /**
     * @returns the map width in tiles
     */
    getTileWidth(): number;

    /**
     * @returns the map height in tiles
     */
    getTileHeight(): number;

    /**
     * The tileset of the map and its tiles.
     */
    readonly tileset: Tileset;
    /**
     * The tiles of the map.
     */
    readonly terrain: Terrain;
    /**
     * The locations of the map.
     */
    readonly locations: Locations;
    /**
     * The files of the map's resource folder.
     */
    readonly resources: Resources;
}

/**
 * The locations of the map ({@link GameMap.locations}).
 */
interface Locations {
    /**
     * @returns the location of an id, or null for an id outside the map's locations
     */
    get(id: number): Location | null;

    /**
     * @returns the first location with the name, or null when no location has it
     */
    findByName(name: string): Location | null;
}

/**
 * The players by slot ({@link Game.players}), from 0 (the original player 1) to 11; one player is always the same object.
 */
interface Players {
    /**
     * @returns the player of a slot, or null for a slot outside 0 to 11
     */
    get(id: number): Player | null;

    /**
     * @returns the players of every slot, from 0
     */
    getAll(): Player[];

    /**
     * @returns the human players
     */
    getHumans(): Player[];
}

/**
 * The units of the game ({@link Game.units}); one live unit is always the same object.
 */
interface Units {
    /**
     * @returns the unit in a slot, or null for an empty slot
     */
    get(id: number): Unit | null;

    /**
     * @returns every live unit
     */
    getAll(): Unit[];

    /**
     * @returns the live units that pass the filter, every live unit without one
     */
    find(filter?: UnitFilter): Unit[];

    /**
     * @returns the first live unit that passes the filter, or null
     */
    findOne(filter?: UnitFilter): Unit | null;

    /**
     * @returns the number of live units that pass the filter
     */
    count(filter?: UnitFilter): number;

    /**
     * Searches the unit finder; only in shared code, local code throws a TypeError.
     *
     * @returns the live units whose bounds meet the rectangle of the two corners, in the finder's order, that pass the filter
     */
    findInRectangle(corner: Position, otherCorner: Position, filter?: UnitFilter): Unit[];

    /**
     * Searches the unit finder; only in shared code, local code throws a TypeError.
     *
     * @returns the live units near the position whose position lies within the radius, in the finder's order, that pass the filter
     */
    findInRadius(position: Position, radius: number, filter?: UnitFilter): Unit[];

    /**
     * Creates a unit as Create Unit does.
     *
     * @returns the unit, or null when it could not be placed
     */
    create(type: UnitType, player: Player, position: Position): Unit | null;
}

/**
 * The switches 0 to 255 of the trigger state ({@link Game.switches}).
 */
interface Switches {
    /**
     * @param index a switch from 0 to 255
     * @returns true when the switch is set
     */
    isSet(index: number): boolean;

    /**
     * Sets or clears a switch as Set Switch does.
     */
    set(index: number, set: boolean): void;

    /**
     * Toggles a switch as Set Switch does.
     */
    toggle(index: number): void;
}

/**
 * The countdown timer of the trigger state ({@link Game.countdown}), in seconds of the trigger clock (16 frames a second).
 */
interface Countdown {
    /**
     * @returns the countdown timer in seconds of the trigger clock
     */
    get(): number;

    /**
     * Sets the countdown timer as Set Countdown Timer does.
     */
    set(seconds: number): void;

    /**
     * Adds seconds to the countdown timer.
     */
    add(seconds: number): void;

    /**
     * Subtracts seconds from the countdown timer.
     */
    subtract(seconds: number): void;

    /**
     * @returns true while the countdown timer is paused
     */
    isPaused(): boolean;

    /**
     * Pauses the countdown timer as Pause Timer does.
     */
    pause(): void;

    /**
     * Resumes the countdown timer as Unpause Timer does.
     */
    resume(): void;
}

/**
 * The leaderboards of the leaderboard actions with a text label ({@link Game.leaderboard}).
 */
interface Leaderboard {
    /**
     * Shows the Control leaderboard of a unit type or group: the goal form with a goal, Control At Location with a location.
     */
    showControl(label: string, unit: UnitType | UnitGroup, options?: ControlLeaderboardOptions): void;

    /**
     * Shows the Resources leaderboard of a resource type.
     */
    showResources(label: string, resourceType: ResourceType, options?: LeaderboardOptions): void;

    /**
     * Shows the Kills leaderboard of a unit type or group.
     */
    showKills(label: string, unit: UnitType | UnitGroup, options?: LeaderboardOptions): void;

    /**
     * Shows the Points leaderboard of a score type.
     */
    showPoints(label: string, scoreType: ScoreType, options?: LeaderboardOptions): void;

    /**
     * Shows the Greed leaderboard of the minerals and gas of every player, toward a goal.
     */
    showGreed(goal: number): void;

    /**
     * @returns true when the leaderboards include computer players
     */
    areComputersIncluded(): boolean;

    /**
     * Includes or leaves out computer players, as Leaderboard Computer Players does.
     */
    setComputersIncluded(included: boolean): void;
}

/**
 * A resource of the map's resource folder by its name relative to resources/, with / separators.
 */
interface Resource {
    getId(): number;

    getName(): string;
}

interface Grp extends Resource {
}

interface Anim extends Resource {
}

/**
 * A PNG or DDS picture to draw ({@link Drawing.drawImage}) or to show on the console.
 */
interface Image extends Resource {
    /**
     * @returns the width in pixels
     */
    getWidth(): number;

    /**
     * @returns the height in pixels
     */
    getHeight(): number;
}

/**
 * The files of the map's resource folder ({@link GameMap.resources}).
 */
interface Resources {
    /**
     * @returns true when the resource folder has a file of the name
     */
    has(name: string): boolean;

    /**
     * Reads a GRP; a missing or invalid one throws.
     */
    getGrp(name: string): Grp;

    /**
     * Reads a single-entry HD or HD2 anim; a missing or invalid one throws.
     */
    getAnim(name: string): Anim;

    /**
     * Reads a PNG or DDS picture once; a missing or invalid one throws.
     */
    getImage(name: string): Image;
}

/**
 * The graphics of an image: an SD GRP, an HD anim and an HD2 anim, at least one.
 */
interface ImageGraphics {
    sd?: Grp | null;
    hd?: Anim | null;
    hd2?: Anim | null;
}

/**
 * A tile of the map's tileset: a CV5 group and an index in it from 0 to 15. One tile is always the same object.
 */
interface Tile {
    /**
     * @returns the MTXM value, group << 4 | index, the value core.terrain takes
     */
    getId(): number;

    getGroup(): number;

    getIndex(): number;

    /**
     * @returns true when more than 12 of its 16 mini-tiles are walkable
     */
    isWalkable(): boolean;

    /**
     * @returns true when some but not all of its mini-tiles are walkable
     */
    isPartiallyWalkable(): boolean;

    isBuildable(): boolean;

    hasCreep(): boolean;

    /**
     * @returns 0 low, 1 middle or 2 high ground
     */
    getElevation(): number;

    /**
     * @returns true when ground units see past it from no elevation
     */
    isBlockingSight(): boolean;
}

/**
 * The tileset of the map ({@link GameMap.tileset}).
 */
interface Tileset {
    /**
     * @returns the tileset id from 0 (Badlands) to 7 (Ice)
     */
    getId(): number;

    /**
     * @returns the number of CV5 tile groups
     */
    getGroupCount(): number;

    /**
     * @returns the tile of a CV5 group and an index from 0 to 15; a RangeError for a tile the tileset does not have
     */
    getTile(group: number, index: number): Tile;
}

/**
 * The tiles of the map ({@link GameMap.terrain}), by tile coordinates (32 pixels). The changes apply together at the next trigger step, after the frame start hooks
 * (onFrame, the events, the timers) and before the triggers, as on a map that had the tiles from the start: walkability, pathing regions,
 * buildability, sight and the minimap follow, and ground units on tiles that became unwalkable move to walkable ground nearby. Reads see the
 * changes made so far.
 */
interface Terrain {
    /**
     * @returns the tile, the one below creep for a tile with creep
     */
    getTile(tileX: number, tileY: number): Tile;

    /**
     * Sets a tile.
     */
    setTile(tileX: number, tileY: number, tile: Tile): void;

    /**
     * Sets every tile the location covers.
     */
    fillTiles(location: Location, tile: Tile): void;

    /**
     * Copies the tiles one location covers onto the tiles of another location; both cover the same number of tiles across and down.
     */
    copyTiles(fromLocation: Location, toLocation: Location): void;

    /**
     * The creep of the map.
     */
    readonly creep: Creep;
}

/**
 * The creep of the map, by tile coordinates ({@link Terrain.creep}). Creep these functions add has no source: it stays until it is removed or
 * recedes. The changes work in shared code only.
 */
interface Creep {
    /**
     * @returns whether the tile has creep, by the creep flag the rules of the game read
     */
    has(tileX: number, tileY: number): boolean;

    /**
     * Covers the footprint of a building of the unit type at the position with creep and grows it until no tile is added, as the game does
     * for a building that needs creep.
     */
    fill(unitType: UnitType, position: Position): void;

    /**
     * Runs one spread step of a creep source of the unit type at the position; a type that spreads creep keeps to its ellipse.
     *
     * @returns true when it added a tile
     */
    spread(unitType: UnitType, position: Position): boolean;

    /**
     * Removes the creep of the tile at once and gives back the terrain below it.
     */
    remove(tileX: number, tileY: number): void;

    /**
     * Makes the creep tile recede later, at the pace of the recession, as the creep of a removed building does.
     *
     * @returns false for a tile that is not creep, already recedes, lies under a building or that a Zerg building sustains, and when the
     *          recession is full
     */
    recede(tileX: number, tileY: number): boolean;
}

/**
 * A channel: a value one peer sends reaches the channel's handler on every peer, the sender's included, and in every replay, at the frame the
 * game runs it, in the order the game runs them. It sends the script command {c: id, v: value} ({@link CoreCommand.submit}).
 */
interface Channel<T> {
    /**
     * @returns the channel's id, the number of channels created before it
     */
    getId(): number;

    /**
     * Sends a value: null, a boolean, a number, a string, an array or a plain object of these; anything else throws a TypeError. The handler
     * receives a copy. Shared code and local code may send; shared code sends once from every human peer, so the handler runs once per such
     * player.
     */
    send(value: T): void;
}

/**
 * A value per player, on a channel of its own: a change reaches every peer at the frame the game runs it.
 */
interface PlayerValue<T> {
    /**
     * @returns the player's value, the initial value until one was set
     */
    get(player: Player): T;

    /**
     * Sets the value of the sending player.
     */
    set(value: T): void;

    /**
     * Sets the value of a player.
     */
    set(player: Player, value: T): void;
}

/**
 * Creates the channels. Create them in shared code, which runs the same on every peer, so that every peer gives them the same ids: local code
 * throws a TypeError.
 */
interface Sync {
    /**
     * Creates a channel whose handler runs as shared code with the sending player, the value, and the live units the player had selected
     * when the command ran.
     */
    createChannel<T>(handler: (player: Player, value: T, units: Unit[]) => void): Channel<T>;

    /**
     * Creates a value per player over a new channel.
     */
    createPlayerValue<T>(initial: T): PlayerValue<T>;
}

declare const sync: Sync;

/**
 * What only this machine knows, which differs from peer to peer. Readable only in local code (the onLocal* hooks and onRender); anywhere else
 * every function throws a TypeError.
 */
interface Local {
    /**
     * @returns the local player, null in a replay and for an observer
     */
    getPlayer(): Player | null;

    isReplay(): boolean;

    /**
     * @returns true while the chat box is open
     */
    isChatOpen(): boolean;

    /**
     * @returns the map position of the local screen's top left, null while the client has not supplied it
     */
    getScreenPosition(): Position | null;

    /**
     * Moves only the local screen's top left to a map position at once, kept inside the map.
     *
     * @returns false while the screen is unknown
     */
    setScreenPosition(position: Position): boolean;

    /**
     * @returns the map position under the pointer, null while the client has not supplied it
     */
    getMousePosition(): Position | null;

    /**
     * @returns the units of the local interface's selection; it may differ from the local player's {@link Player.getSelection} until the
     *          selection command runs
     */
    getSelection(): Unit[];

    /**
     * @returns the button set the local command card shows, null for none
     */
    getDisplayedButtonSet(): ButtonSet | null;

    /**
     * Shows a button set on the local command card at its next refresh, as the Change Buttons action does, until the selection changes.
     */
    changeButtons(set: ButtonSet | UnitType | number): void;

    /**
     * @returns the local state of a condition value (0 to 255) of the Script button condition; every state starts Hidden, the native zero value
     */
    getButtonState(conditionValue: number): ButtonState;

    setButtonState(conditionValue: number, state: ButtonState): void;

    /**
     * Pauses the game as the game menu does: the local player's Pause command runs in a later turn on every peer, and a network pause takes one
     * of the player's pauses left ({@link Player.getPausesLeft}). Nothing in a replay or while the game is paused.
     */
    requestPause(): void;

    /**
     * Resumes the game as the game menu does, with the local player's Resume command. Nothing in a replay or while the game runs.
     */
    requestResume(): void;
}

declare const local: Local;

// ---------------------------------------------------------------------------------------------------------------------------------------
// Presentation: drawing in onRender, the console and the texts.
// ---------------------------------------------------------------------------------------------------------------------------------------

/**
 * A colour: a number packed 0xAARRGGBB with straight alpha.
 */
type Color = number;

interface ColorStatic {
    readonly Transparent: Color;
    readonly Black: Color;
    readonly White: Color;
    readonly Gray: Color;
    readonly Red: Color;
    readonly Green: Color;
    readonly Blue: Color;
    readonly Yellow: Color;
    readonly Cyan: Color;
    readonly Magenta: Color;
    readonly Orange: Color;

    /**
     * @returns the opaque colour; a channel is an integer from 0 to 255, else it throws a RangeError
     */
    fromRgb(red: number, green: number, blue: number): Color;

    /**
     * @returns the colour; red, green and blue are integers from 0 to 255 and alpha is the opacity from 0 to 1, else it throws a RangeError
     */
    fromRgba(red: number, green: number, blue: number, alpha: number): Color;

    /**
     * @returns the opaque colour of a palette index of the map's tileset palette
     */
    fromPalette(index: number): Color;
}

declare const Color: ColorStatic;

/**
 * A font of the game interface.
 */
interface Font extends GameType {
}

interface FontTable extends ConstantTable<Font> {
    /**
     * font8
     */
    readonly Small: Font;
    /**
     * font10, the default
     */
    readonly Medium: Font;
    /**
     * font16
     */
    readonly Large: Font;
    /**
     * font16x
     */
    readonly Huge: Font;
}

declare const Font: FontTable;

/**
 * How the lines of a text stand from its x.
 */
interface Align extends GameType {
}

interface AlignTable extends ConstantTable<Align> {
    readonly Left: Align;
    readonly Center: Align;
    readonly Right: Align;
}

declare const Align: AlignTable;

/**
 * The graphics this machine draws.
 */
interface GraphicsKind extends GameType {
}

interface GraphicsTable extends ConstantTable<GraphicsKind> {
    readonly SD: GraphicsKind;
    readonly HD: GraphicsKind;
    readonly HD2: GraphicsKind;
}

declare const Graphics: GraphicsTable;

interface LineOptions {
    /**
     * Color.White by default.
     */
    color?: Color;
    /**
     * 1 by default.
     */
    thickness?: number;
}

interface TextOptions {
    /**
     * Font.Medium by default.
     */
    font?: Font;
    /**
     * Color.White by default; colour codes in the text switch it.
     */
    color?: Color;
    /**
     * Align.Left by default.
     */
    align?: Align;
}

interface ImageOptions {
    /**
     * The image's pixel width in the drawing's units by default.
     */
    width?: number;
    height?: number;
    /**
     * From 0 to 1, 1 by default.
     */
    opacity?: number;
}

/**
 * Draws in one space, only in onRender (anywhere else every drawing function throws a TypeError). Coordinates are numbers with fractions,
 * drawn at the real resolution; what a call draws is drawn in call order and stays until the next onRender call.
 */
interface Drawing {
    fillRectangle(x: number, y: number, width: number, height: number, color?: Color): void;

    /**
     * The outline lies inside the rectangle.
     */
    drawRectangle(x: number, y: number, width: number, height: number, options?: LineOptions): void;

    fillCircle(x: number, y: number, radius: number, color?: Color): void;

    /**
     * The outline is centered on the circle.
     */
    drawCircle(x: number, y: number, radius: number, options?: LineOptions): void;

    drawLine(x1: number, y1: number, x2: number, y2: number, options?: LineOptions): void;

    /**
     * A square of one unit of line thickness with its top left at the position.
     */
    drawPoint(x: number, y: number, color?: Color): void;

    /**
     * Draws the lines, broken at \n, from the top at y, each aligned on x.
     */
    drawText(text: string, x: number, y: number, options?: TextOptions): void;

    drawImage(image: Image, x: number, y: number, options?: ImageOptions): void;

    /**
     * @returns the width of the widest line in interface units in this machine's fonts, 0 before the client drew a frame; only in local code
     */
    measureText(text: string, font?: Font): number;
}

/**
 * The game screen of onRender in interface units: 480 high, as wide as the interface (640, 853, ...), drawn above the console and below the
 * game menus. Sizes, line thicknesses and texts are in interface units. {@link world} draws in map pixels following the camera, above the units
 * and below the console (thicknesses and texts in map pixels, so they follow the zoom); {@link minimap} in map pixels on the minimap (thicknesses,
 * points and texts in interface units). The screen's size, scale and graphics are readable only in local code.
 */
interface Screen extends Drawing {
    getWidth(): number;

    getHeight(): number;

    /**
     * @returns the real pixels per interface unit
     */
    getScale(): number;

    getGraphics(): GraphicsKind;

    /**
     * @returns the interface coordinate rounded to the real pixel grid
     */
    snap(value: number): number;

    readonly world: Drawing;
    readonly minimap: Drawing;
}

/**
 * A console dialog.
 */
interface ConsoleControl extends GameType {
}

interface ConsoleControlTable extends ConstantTable<ConsoleControl> {
    readonly CommandCard: ConsoleControl;
    readonly Selection: ConsoleControl;
    readonly Resources: ConsoleControl;
    readonly Portrait: ConsoleControl;
    readonly Minimap: ConsoleControl;
    readonly Menu: ConsoleControl;
    readonly TextBox: ConsoleControl;
}

declare const ConsoleControl: ConsoleControlTable;

/**
 * An art file of the shown race console that an image can replace, named after the installed file. The image is stretched over the file's place
 * and size in every graphics set that draws the file. The console is hit where a frame image (Console with the SD graphics, the three pieces
 * with HD and HD2) is not transparent; a replaced frame image shows the whole frame even where EUD console transparency hides the console art.
 */
interface ConsoleImage extends GameType {
}

interface ConsoleImageTable extends ConstantTable<ConsoleImage> {
    /**
     * The SD console frame (game/tconsole.DDS and the Zerg and Protoss files).
     */
    readonly Console: ConsoleImage;
    /**
     * The left HD and HD2 console piece with the minimap frame (game/consoles/race/console_left.DDS).
     */
    readonly ConsoleLeft: ConsoleImage;
    /**
     * The center HD and HD2 console piece with the selection frame (console_center.DDS).
     */
    readonly ConsoleCenter: ConsoleImage;
    /**
     * The right HD and HD2 console piece with the portrait and command card frames (console_right.DDS).
     */
    readonly ConsoleRight: ConsoleImage;
    /**
     * The plate under the game time (console_TIME.DDS), every graphics set.
     */
    readonly ConsoleTime: ConsoleImage;
    /**
     * The plate under the APM number (console_APM.DDS), every graphics set.
     */
    readonly ConsoleApm: ConsoleImage;
    /**
     * The overlay over the portrait (conover.DDS), every graphics set.
     */
    readonly Conover: ConsoleImage;
}

declare const ConsoleImage: ConsoleImageTable;

/**
 * One console dialog, placed by its top left in the classic 640 by 480 screen.
 */
interface ConsoleControlHandle {
    getControl(): ConsoleControl;

    setPosition(x: number, y: number): void;

    setVisible(visible: boolean): void;

    isVisible(): boolean;

    /**
     * Shows or hides a control of the dialog by its identifier in the dialog's definition; it wins over EUD memory until
     * {@link ConsoleControlHandle.restoreChild}.
     */
    setChildVisible(id: number, visible: boolean): void;

    /**
     * Places a control of the dialog by its top left in the classic 640 by 480 screen; it wins over EUD memory until
     * {@link ConsoleControlHandle.restoreChild}.
     */
    setChildPosition(id: number, x: number, y: number): void;

    /**
     * Forgets what was set on a control of the dialog, so that EUD memory drives it again.
     */
    restoreChild(id: number): void;
}

/**
 * The console ({@link Display.console}). Shared code sets it on every peer, local code only on this machine; it changes no game state. A setting
 * wins over what EUD memory gives for the same item (the console, a dialog's position, a dialog's visibility, a console art file, a resource
 * icon); EUD memory still drives the items the script never set.
 */
interface Console {
    /**
     * Shows or hides the whole console.
     */
    setVisible(visible: boolean): void;

    /**
     * @returns false while the script hides the console
     */
    isVisible(): boolean;

    /**
     * @returns the object of a console dialog, the same for the same dialog
     */
    getControl(control: ConsoleControl): ConsoleControlHandle;

    /**
     * Draws an image instead of a console art file of the shown race console; null for the installed file.
     */
    replaceImage(consoleImage: ConsoleImage, image: Image | null): void;

    /**
     * Draws an image for the console icon of Minerals, Gas or Supply (every race's icon), fitted into the icon's box; null for the icon of EUD
     * memory or the installed one.
     */
    setResourceIcon(resourceType: ResourceType, image: Image | null): void;
}

/**
 * The stat_txt strings by their one-based ids ({@link Display.strings}). Shared code sets them on every peer, local code only on this machine.
 */
interface Strings {
    /**
     * Shows a text instead of a string; null shows the game's string again. It wins over the installed table and the table EUD memory commits.
     */
    setStatText(id: number, text: string | null): void;
}

/**
 * The message lines on the screen ({@link Display.messages}), 0 to 12: lines 0 to 10 take the messages in turn from the cursor, line 12 the
 * status texts. A written line keeps the expiry time of the message it held, as an EUD write does, so a line whose message expired is cleared at
 * the next drawn frame.
 */
interface Messages {
    /**
     * Empties the message lines on the screen.
     */
    clear(): void;

    getLine(line: number): string;

    /**
     * Stores a text in a line, at most 217 UTF-8 bytes.
     */
    setLine(line: number, text: string): void;

    /**
     * @returns the line the next message starts at, a byte; 11 or more starts at line 0
     */
    getCursor(): number;

    setCursor(line: number): void;
}

/**
 * The game sounds ({@link Display.sound}). Shared code plays them on every peer, local code only on this machine.
 */
interface DisplaySound {
    /**
     * Plays a sound of sfxdata.dat by its id (0 to 1143) at a position, louder the nearer it is to the screen, whether the players see the
     * position or not. A sound of local code plays with the sounds of the next frame start.
     */
    playAt(soundId: number, position: Position): void;
}

/**
 * The graphics of the game's images ({@link Display.images}). Shared code replaces them on every peer, local code only on this machine.
 */
interface DisplayImages {
    /**
     * Replaces the graphics of every image of an image type.
     */
    replace(imageType: ImageType, images: ImageGraphics): void;

    /**
     * Restores the installed graphics of an image type.
     */
    restore(imageType: ImageType): void;

    /**
     * Replaces the graphics of a unit's images of an image type, by default that of its main image now, while the unit lives.
     */
    replaceUnit(unit: Unit, images: ImageGraphics, imageType?: ImageType): void;

    /**
     * Restores the graphics of a unit's images of an image type, by default that of its main image now.
     */
    restoreUnit(unit: Unit, imageType?: ImageType): void;
}

/**
 * The presentation that stays until it is changed: the console, the texts, the messages and the image graphics. Shared code changes it on every
 * peer, local code only on this machine; it changes no game state. Drawing is the {@link Screen} of {@link Hooks.onRender}.
 */
interface Display {
    /**
     * The console and its dialogs.
     */
    readonly console: Console;
    /**
     * The stat_txt strings.
     */
    readonly strings: Strings;
    /**
     * The message lines on the screen.
     */
    readonly messages: Messages;
    /**
     * The game sounds.
     */
    readonly sound: DisplaySound;
    /**
     * The graphics of the game's images.
     */
    readonly images: DisplayImages;
    /**
     * The command card's button sets and the script buttons.
     */
    readonly commandCard: CommandCard;
}

declare const display: Display;

/**
 * The options of a button: its position from 1 to 9, its cmdicons frame, its condition and action with their values (0 by default), and its
 * texts: the text and disabled text replace the stat_txt strings and may hold line breaks and the text color control characters; the hotkey
 * (one character) and the kind lead the text as in the native button strings.
 */
interface ButtonOptions {
    position: number;
    icon?: number;
    condition: ButtonCondition;
    conditionValue?: number;
    action: ButtonAction;
    actionValue?: number;
    text?: string;
    hotkey?: string;
    kind?: ButtonKind;
    disabledText?: string;
    enabledString?: number;
    disabledString?: number;
}

/**
 * The target of a script target button: the targeted map position and the unit there, null for the ground or a unit that is gone.
 */
interface ButtonTarget {
    readonly position: Position;
    readonly unit: Unit | null;
}

/**
 * The options of a script button ({@link CommandCard.createButton}).
 */
interface ScriptButtonOptions {
    /**
     * The unit type whose button set gets the button, or the button set itself.
     */
    unitType?: UnitType;
    buttonSet?: ButtonSet | number;
    position: number;
    icon: number;
    text?: string;
    hotkey?: string;
    kind?: ButtonKind;
    /**
     * The text of the gray button.
     */
    disabledText?: string;
    /**
     * Whether a press enters a targeting mode whose target, a unit or the ground, goes to onPressed.
     */
    target?: boolean;

    /**
     * Tells for a unit whether the button acts for it: true enabled, false disabled (gray), null hidden. Each local frame the button is
     * enabled when it returns true for a unit of the local selection, else disabled when it returns false for one, else hidden; and when a
     * press runs, every peer calls it again for each unit the sender had selected and passes only those for which it returned true. It must
     * read the game state only, never local information. Without it the button is enabled and every selected unit passes.
     */
    isEnabled?(unit: Unit): boolean | null;

    /**
     * Local: called for the press, or the target of a target button, on the pressing machine; consuming it sends nothing, and keeps the
     * targeting mode of a target button.
     */
    onLocalPressed?(event: LocalButtonPressedEvent | LocalButtonTargetedEvent): void;

    /**
     * Shared: called on every peer and in every replay when the press runs, with the player, the selected units that isEnabled passed and the
     * target (null for a button without target).
     */
    onPressed?(player: Player, units: Unit[], target: ButtonTarget | null): void;
}

/**
 * A script button: its id, from 0 to 255, is its condition value and action value.
 */
interface ScriptButton {
    getId(): number;

    getButtonSet(): ButtonSet;

    /**
     * @returns the button in its set's table, null once it was removed
     */
    getButton(): Button | null;
}

/**
 * The command card ({@link Display.commandCard}).
 */
interface CommandCard {
    /**
     * @returns the button set of a unit type or an id, null for an id outside the table
     */
    getButtonSet(set: UnitType | number): ButtonSet | null;

    /**
     * Creates a script button in shared code (local code throws a TypeError), at most 256 per game. The button goes after the set's buttons
     * up to its position, with the condition Script and the action Script, or ScriptTarget for a target button.
     */
    createButton(options: ScriptButtonOptions): ScriptButton;
}

/**
 * A button set of the command card's table: the same object for the same id. Its buttons are in the order the card tries them for each
 * position. Shared code changes the table on every peer, local code only on this machine; sets may share one array of buttons, and a change
 * changes every set that shares it. An EUD write of the table replaces the changes.
 */
interface ButtonSet {
    getId(): number;

    getButtonCount(): number;

    getButtons(): Button[];

    /**
     * @returns the button at an index, null outside the set
     */
    getButton(index: number): Button | null;

    /**
     * Inserts a button before an index, or at the end for the button count.
     */
    insertButton(index: number, options: ButtonOptions): Button;

    removeButton(index: number): void;

    /**
     * Takes the array of another set.
     */
    share(other: ButtonSet | UnitType | number): void;

    /**
     * Takes an own copy of the array.
     */
    unshare(): void;

    /**
     * @returns the first set that holds this set's array
     */
    getArray(): ButtonSet;

    /**
     * Takes the installed array and connected set again.
     */
    restore(): void;

    getConnectedSet(): ButtonSet | null;

    setConnectedSet(set: ButtonSet | UnitType | number | null): void;
}

/**
 * A button of a set by its index: after a button before it is inserted or removed, the object names another button.
 */
interface Button {
    getButtonSet(): ButtonSet;

    getIndex(): number;

    getPosition(): number;

    setPosition(position: number): void;

    getIcon(): number;

    setIcon(icon: number): void;

    /**
     * @returns the condition, null for an unknown function
     */
    getCondition(): ButtonCondition | null;

    setCondition(condition: ButtonCondition): void;

    getConditionValue(): number;

    setConditionValue(value: number): void;

    /**
     * @returns the action, null for an unknown function
     */
    getAction(): ButtonAction | null;

    setAction(action: ButtonAction): void;

    getActionValue(): number;

    setActionValue(value: number): void;

    /**
     * @returns the one-based stat_txt id of the enabled string
     */
    getEnabledString(): number;

    getDisabledString(): number;

    /**
     * @returns the text shown instead of the enabled string, without hotkey and kind; null while the button shows its string
     */
    getText(): string | null;

    getHotkey(): string | null;

    getKind(): ButtonKind | null;

    /**
     * Sets the text, keeping the current hotkey and kind unless given; null shows the enabled string again.
     */
    setText(text: string | null, options?: { hotkey?: string | null; kind?: ButtonKind }): void;

    getDisabledText(): string | null;

    setDisabledText(text: string | null): void;
}

// ---------------------------------------------------------------------------------------------------------------------------------------
// The events and decisions of the hooks.
// ---------------------------------------------------------------------------------------------------------------------------------------

/**
 * An event of a unit, with the values of the moment it happened; the unit may be gone ({@link Unit.exists}).
 */
interface UnitEvent {
    /**
     * @returns the event's record of {@link CoreEvents.getFrameEvents}
     */
    getRecord(): CoreEventRecord;

    getFrame(): number;

    getUnit(): Unit;

    getUnitType(): UnitType;

    getOwner(): Player;
}

interface UnitCreatedEvent extends UnitEvent {
}

interface UnitCompletedEvent extends UnitEvent {
}

interface UnitDiedEvent extends UnitEvent {
    getKiller(): Unit | null;

    getKillerPlayer(): Player | null;

    getWeapon(): WeaponType | null;
}

interface UnitDamagedEvent extends UnitEvent {
    getSource(): Unit | null;

    getSourcePlayer(): Player | null;

    getWeapon(): WeaponType | null;

    getAmount(): number;
}

interface OrderIssuedEvent extends UnitEvent {
    getOrder(): Order;

    getTarget(): Unit | null;

    getTargetPosition(): Position;

    getBuildType(): UnitType | null;

    isQueued(): boolean;
}

interface TargetAcquiredEvent extends UnitEvent {
    getTarget(): Unit | null;
}

interface UnitTrainedEvent extends UnitEvent {
    getProducer(): Unit | null;
}

interface ResearchCompletedEvent extends UnitEvent {
    getTech(): TechType;
}

interface UpgradeCompletedEvent extends UnitEvent {
    getUpgrade(): UpgradeType;

    getLevel(): number;
}

interface SpellCastEvent extends UnitEvent {
    getTech(): TechType;

    getTarget(): Unit | null;

    getTargetPosition(): Position;
}

interface OwnerChangedEvent extends UnitEvent {
    getPreviousOwner(): Player;
}

interface UnitLoadedEvent extends UnitEvent {
    getTransport(): Unit | null;
}

interface UnitUnloadedEvent extends UnitEvent {
    getTransport(): Unit | null;
}

interface BulletEvent {
    getRecord(): CoreEventRecord;

    getFrame(): number;

    /**
     * @returns the bullet; once it is gone its exists() is false
     */
    getBullet(): Bullet;

    getWeapon(): WeaponType;

    getPlayer(): Player | null;

    getSource(): Unit | null;

    getTarget(): Unit | null;

    getTargetPosition(): Position;
}

interface BulletCreatedEvent extends BulletEvent {
}

interface BulletHitEvent extends BulletEvent {
    isMissed(): boolean;
}

/**
 * A player who left the game or was dropped.
 */
interface PlayerLeftEvent {
    getRecord(): CoreEventRecord;

    getFrame(): number;

    getPlayer(): Player;

    /**
     * @returns the signed departure reason of the player's Leave Game command
     */
    getReason(): number;
}

/**
 * The result a player received: the triggers ended the player's game, or the player left.
 */
interface PlayerResultEvent {
    getRecord(): CoreEventRecord;

    getFrame(): number;

    getPlayer(): Player;

    /**
     * @returns the raw result: 1 dropped, 2 defeat, 3 victory, 4 secured victory, 5 draw
     */
    getResult(): number;

    /**
     * @returns whether the result is a victory, secured or not
     */
    isVictory(): boolean;

    isDefeat(): boolean;

    isDraw(): boolean;

    isDropped(): boolean;
}

/**
 * A local event of the local player's input, seen on the local machine only. Consuming it keeps it from the game.
 */
interface LocalEvent {
    /**
     * @returns the input's record of {@link CoreLocal.getInputEvents}, whose consumed this event's {@link consume} sets
     */
    getRecord(): CoreInputRecord;

    /**
     * Keeps a pressed key or button, or a chat line, from the game's own handling; it has no effect on a release.
     */
    consume(): void;

    isConsumed(): boolean;
}

/**
 * A local key or mouse button event: the modifier mask ({@link Modifier}) and the map position under the pointer, or null while unknown.
 */
interface LocalInputEvent extends LocalEvent {
    getModifiers(): number;

    getMousePosition(): Position | null;

    isShift(): boolean;

    isControl(): boolean;

    isAlt(): boolean;
}

interface LocalKeyEvent extends LocalInputEvent {
    getKey(): Key;
}

interface LocalMouseButtonEvent extends LocalInputEvent {
    getButton(): MouseButton;
}

/**
 * A chat line of the local player; a consumed line is neither sent nor shown.
 */
interface LocalChatEvent extends LocalEvent {
    getText(): string;
}

/**
 * A command card button the local player pressed: the shown set, the button of the set's table (null for a button of none), its position,
 * condition and action (null for unknown functions) with their values. A consumed press runs no action.
 */
interface LocalButtonPressedEvent extends LocalEvent {
    getButtonSet(): ButtonSet;

    getButton(): Button | null;

    getPosition(): number;

    getCondition(): ButtonCondition | null;

    getConditionValue(): number;

    getAction(): ButtonAction | null;

    getActionValue(): number;

    /**
     * @returns true while Shift was held
     */
    isQueued(): boolean;

    /**
     * @returns true for a hotkey press, false for a click
     */
    isHotkey(): boolean;
}

/**
 * The target of a ScriptTarget button: the unit (null for the ground or a unit that is gone) and the map position. A consumed target keeps the
 * targeting mode.
 */
interface LocalButtonTargetedEvent extends LocalButtonPressedEvent {
    getTarget(): Unit | null;

    getTargetPosition(): Position;
}

/**
 * The damage of a weapon hit; the amount is in shown hit points.
 */
interface DamageDecision {
    /**
     * @returns the decision's record, whose amount and cancelled the setters change; a hook may change them there too
     */
    getRecord(): CoreDecisionRecord;

    getTarget(): Unit;

    getTargetType(): UnitType;

    getTargetOwner(): Player;

    getSource(): Unit | null;

    getSourcePlayer(): Player | null;

    getWeapon(): WeaponType | null;

    getAmount(): number;

    setAmount(amount: number): void;

    cancel(): void;

    isCancelled(): boolean;
}

interface OrderDecision {
    /**
     * @returns the decision's record, whose order, target, targetGeneration, targetX, targetY and rejected the setters change
     */
    getRecord(): CoreDecisionRecord;

    getUnit(): Unit;

    getUnitType(): UnitType;

    getOwner(): Player;

    getBuildType(): UnitType | null;

    isQueued(): boolean;

    getOrder(): Order;

    setOrder(order: Order): void;

    getTarget(): Unit | null;

    getTargetPosition(): Position;

    /**
     * Sets a target unit (and its position), a position without a target unit, or no target unit for null.
     */
    setTarget(target: Unit | Position | null): void;

    reject(): void;

    isRejected(): boolean;
}

interface TargetAcquisitionDecision {
    /**
     * @returns the decision's record, whose target and targetGeneration the setter changes
     */
    getRecord(): CoreDecisionRecord;

    getUnit(): Unit;

    getUnitType(): UnitType;

    getOwner(): Player;

    getTarget(): Unit | null;

    /**
     * Sets the target unit, or no target for null.
     */
    setTarget(target: Unit | null): void;
}

// ---------------------------------------------------------------------------------------------------------------------------------------
// The constants.
// ---------------------------------------------------------------------------------------------------------------------------------------

/**
 * An event kind of the records of {@link CoreEvents.getFrameEvents}: the hook of a kind is on followed by its name.
 */
interface EventKind extends GameType {
}

interface EventKindTable extends ConstantTable<EventKind> {
    readonly UnitCreated: EventKind;
    readonly UnitCompleted: EventKind;
    readonly UnitDied: EventKind;
    readonly UnitDamaged: EventKind;
    readonly OrderIssued: EventKind;
    readonly TargetAcquired: EventKind;
    readonly UnitTrained: EventKind;
    readonly ResearchCompleted: EventKind;
    readonly UpgradeCompleted: EventKind;
    readonly SpellCast: EventKind;
    readonly OwnerChanged: EventKind;
    readonly UnitLoaded: EventKind;
    readonly UnitUnloaded: EventKind;
    readonly BulletCreated: EventKind;
    readonly BulletHit: EventKind;
    readonly PlayerLeft: EventKind;
    readonly PlayerResult: EventKind;
}

declare const EventKind: EventKindTable;

/**
 * An input kind of the records of {@link CoreLocal.getInputEvents}: the hook of a kind is onLocal followed by its name.
 */
interface InputKind extends GameType {
}

interface InputKindTable extends ConstantTable<InputKind> {
    readonly KeyDown: InputKind;
    readonly KeyUp: InputKind;
    readonly MouseDown: InputKind;
    readonly MouseUp: InputKind;
    readonly Chat: InputKind;
    readonly ButtonPressed: InputKind;
    readonly ButtonTargeted: InputKind;
}

declare const InputKind: InputKindTable;

/**
 * A button condition of SC:R's list, whose id {@link CoreButtons} takes; Script gives the local state of its condition value
 * ({@link Local.setButtonState}).
 */
interface ButtonCondition extends GameType {
}

interface ButtonConditionTable extends ConstantTable<ButtonCondition> {
    readonly Always: ButtonCondition;
    readonly ReplaySlowDown: ButtonCondition;
    readonly ReplaySpeedUp: ButtonCondition;
    readonly ReplayPlayPause: ButtonCondition;
    readonly UnitRequirement: ButtonCondition;
    readonly HangarUnitRequirement: ButtonCondition;
    readonly AnyCanMove: ButtonCondition;
    readonly AnyHasRightClick: ButtonCondition;
    readonly AnyCanAttack: ButtonCondition;
    readonly AnyEnabledCanAttack: ButtonCondition;
    readonly TrainingUnit: ButtonCondition;
    readonly BuildingAddon: ButtonCondition;
    readonly IsHatchery: ButtonCondition;
    readonly RallyOrder: ButtonCondition;
    readonly HatcheryRallyOrder: ButtonCondition;
    readonly Incomplete: ButtonCondition;
    readonly Upgrading: ButtonCondition;
    readonly Researching: ButtonCondition;
    readonly AnyNotCarrying: ButtonCondition;
    readonly AnyCarrying: ButtonCondition;
    readonly AnyCanLoad: ButtonCondition;
    readonly AnyHasLoaded: ButtonCondition;
    readonly ResearchRequirement: ButtonCondition;
    readonly TechnologyUseRequirement: ButtonCondition;
    readonly AnyTechnologyUseRequirement: ButtonCondition;
    readonly UpgradeRequirement: ButtonCondition;
    readonly CanCloak: ButtonCondition;
    readonly CanDecloak: ButtonCondition;
    readonly CanCloakOwnTechnology: ButtonCondition;
    readonly CanDecloakOwnTechnology: ButtonCondition;
    readonly CanSiege: ButtonCondition;
    readonly CanUnsiege: ButtonCondition;
    readonly NoneSieged: ButtonCondition;
    readonly NuclearSiloReady: ButtonCondition;
    readonly NotDisabled: ButtonCondition;
    readonly NotGrounded: ButtonCondition;
    readonly CanLand: ButtonCondition;
    readonly CanLiftOff: ButtonCondition;
    readonly CanBurrow: ButtonCondition;
    readonly CanUnburrow: ButtonCondition;
    readonly LurkerAspect: ButtonCondition;
    readonly NoneBurrowed: ButtonCondition;
    readonly NoneBurrowedCanAttack: ButtonCondition;
    readonly NoneBurrowedNotCarrying: ButtonCondition;
    readonly NoneBurrowedCarrying: ButtonCondition;
    readonly SameBurrowState: ButtonCondition;
    readonly NoNydusExit: ButtonCondition;
    readonly ZergBasicStructures: ButtonCondition;
    readonly ZergAdvancedStructures: ButtonCondition;
    readonly CarrierHasInterceptors: ButtonCondition;
    readonly ReaverHasScarabs: ButtonCondition;
    readonly TechnologyUseDisabled: ButtonCondition;
    readonly ArchonWarp: ButtonCondition;
    readonly DarkArchonMeld: ButtonCondition;
    readonly ProtossBasicStructures: ButtonCondition;
    readonly ProtossAdvancedStructures: ButtonCondition;
    readonly AllConstructing: ButtonCondition;
    readonly AnyNotConstructing: ButtonCondition;
    readonly ScvCanGather: ButtonCondition;
    readonly ScvCanReturnCargo: ButtonCondition;
    readonly TerranBasicStructures: ButtonCondition;
    readonly TerranAdvancedStructures: ButtonCondition;
    readonly SingleUnitRequirement: ButtonCondition;
    readonly Script: ButtonCondition;
}

declare const ButtonCondition: ButtonConditionTable;

/**
 * A button action of SC:R's list, whose id {@link CoreButtons} takes; Script and ScriptTarget send no command.
 */
interface ButtonAction extends GameType {
}

interface ButtonActionTable extends ConstantTable<ButtonAction> {
    readonly Move: ButtonAction;
    readonly Stop: ButtonAction;
    readonly Attack: ButtonAction;
    readonly Patrol: ButtonAction;
    readonly HoldPosition: ButtonAction;
    readonly StimPacks: ButtonAction;
    readonly Cloak: ButtonAction;
    readonly Decloak: ButtonAction;
    readonly UseTechnology: ButtonAction;
    readonly NuclearStrike: ButtonAction;
    readonly Siege: ButtonAction;
    readonly Unsiege: ButtonAction;
    readonly Repair: ButtonAction;
    readonly Gather: ButtonAction;
    readonly ReturnCargo: ButtonAction;
    readonly ChangeButtons: ButtonAction;
    readonly Load: ButtonAction;
    readonly UnloadAll: ButtonAction;
    readonly Heal: ButtonAction;
    readonly UnitMorph: ButtonAction;
    readonly CancelMorph: ButtonAction;
    readonly Burrow: ButtonAction;
    readonly Unburrow: ButtonAction;
    readonly InfestedTerranAttack: ButtonAction;
    readonly DarkArchonMeld: ButtonAction;
    readonly ArchonWarp: ButtonAction;
    readonly CarrierMove: ButtonAction;
    readonly CarrierStop: ButtonAction;
    readonly CarrierAttack: ButtonAction;
    readonly TrainHangarUnit: ButtonAction;
    readonly CancelLast: ButtonAction;
    readonly ReaverStop: ButtonAction;
    readonly ReaverAttack: ButtonAction;
    readonly Train: ButtonAction;
    readonly SetRallyPoint: ButtonAction;
    readonly BuildAddon: ButtonAction;
    readonly Land: ButtonAction;
    readonly LiftOff: ButtonAction;
    readonly CancelAddon: ButtonAction;
    readonly Upgrade: ButtonAction;
    readonly Research: ButtonAction;
    readonly CancelUpgrade: ButtonAction;
    readonly CancelResearch: ButtonAction;
    readonly BuildingAttack: ButtonAction;
    readonly SelectLarva: ButtonAction;
    readonly BuildingMorph: ButtonAction;
    readonly PlaceNydusExit: ButtonAction;
    readonly RechargeShields: ButtonAction;
    readonly PlaceBeacon: ButtonAction;
    readonly CancelTargeting: ButtonAction;
    readonly CancelPlacement: ButtonAction;
    readonly CancelConstruction: ButtonAction;
    readonly CancelInfestation: ButtonAction;
    readonly CancelNuclearStrike: ButtonAction;
    readonly ZergBuild: ButtonAction;
    readonly TerranBuild: ButtonAction;
    readonly ProtossBuild: ButtonAction;
    readonly ReplayPlayPause: ButtonAction;
    readonly ReplaySlowDown: ButtonAction;
    readonly ReplaySpeedUp: ButtonAction;
    readonly Script: ButtonAction;
    readonly ScriptTarget: ButtonAction;
}

declare const ButtonAction: ButtonActionTable;

/**
 * A state of the Script button condition, its id the native condition result: Enabled 1, Hidden 0, Disabled -1.
 */
interface ButtonState extends GameType {
}

interface ButtonStateTable {
    readonly Enabled: ButtonState;
    readonly Hidden: ButtonState;
    readonly Disabled: ButtonState;

    /**
     * @returns the state of a condition result, null for another value
     */
    get(id: number): ButtonState | null;
}

declare const ButtonState: ButtonStateTable;

/**
 * The kind of a button's text, the second character of the native button string, which chooses the costs the tooltip shows for the action
 * value.
 */
interface ButtonKind extends GameType {
}

interface ButtonKindTable extends ConstantTable<ButtonKind> {
    readonly None: ButtonKind;
    readonly UnitProduction: ButtonKind;
    readonly Upgrade: ButtonKind;
    readonly TechnologyUse: ButtonKind;
    readonly Research: ButtonKind;
    readonly Morph: ButtonKind;
}

declare const ButtonKind: ButtonKindTable;

/**
 * A decision kind of the decision records ({@link CoreDecisionRecord}): the hook of a kind is onBefore followed by its name.
 */
interface DecisionKind extends GameType {
}

interface DecisionKindTable extends ConstantTable<DecisionKind> {
    readonly Damage: DecisionKind;
    readonly Order: DecisionKind;
    readonly TargetAcquired: DecisionKind;
}

declare const DecisionKind: DecisionKindTable;

/**
 * The bits of the modifier mask of a local key or mouse button event.
 */
interface ModifierTable {
    readonly Shift: number;
    readonly Control: number;
    readonly Alt: number;
}

declare const Modifier: ModifierTable;

interface UnitGroupTable {
    readonly AnyUnit: UnitGroup;
    readonly Men: UnitGroup;
    readonly Buildings: UnitGroup;
    readonly Factories: UnitGroup;
}

declare const UnitGroup: UnitGroupTable;

interface ScoreTypeTable extends ConstantTable<ScoreType> {
    readonly Total: ScoreType;
    readonly Units: ScoreType;
    readonly Buildings: ScoreType;
    readonly UnitsAndBuildings: ScoreType;
    readonly Kills: ScoreType;
    readonly Razings: ScoreType;
    readonly KillsAndRazings: ScoreType;
    readonly Custom: ScoreType;
}

declare const ScoreType: ScoreTypeTable;

interface ResourceTypeTable extends ConstantTable<ResourceType> {
    readonly Minerals: ResourceType;
    readonly Gas: ResourceType;
    readonly MineralsAndGas: ResourceType;
    /**
     * Only for the console icon ({@link Console.setResourceIcon}); the resource functions refuse it.
     */
    readonly Supply: ResourceType;
}

declare const ResourceType: ResourceTypeTable;

interface AllianceTable extends ConstantTable<Alliance> {
    readonly Enemy: Alliance;
    readonly Ally: Alliance;
    readonly AlliedVictory: Alliance;
}

declare const Alliance: AllianceTable;

interface GameSpeedTable extends ConstantTable<GameSpeed> {
    readonly Slowest: GameSpeed;
    readonly Slower: GameSpeed;
    readonly Slow: GameSpeed;
    readonly Normal: GameSpeed;
    readonly Fast: GameSpeed;
    readonly Faster: GameSpeed;
    readonly Fastest: GameSpeed;
}

declare const GameSpeed: GameSpeedTable;

/**
 * The cheats by their bit of the cheat flags.
 */
interface CheatTable {
    readonly BlackSheepWall: Cheat;
    readonly OperationCwal: Cheat;
    readonly PowerOverwhelming: Cheat;
    readonly SomethingForNothing: Cheat;
    readonly ShowMeTheMoney: Cheat;
    readonly GameOverMan: Cheat;
    readonly ThereIsNoCowLevel: Cheat;
    readonly StayingAlive: Cheat;
    readonly Ophelia: Cheat;
    readonly TheGathering: Cheat;
    readonly MedievalMan: Cheat;
    readonly ModifyThePhaseVariance: Cheat;
    readonly WarAintWhatItUsedToBe: Cheat;
    readonly FoodForThought: Cheat;
    readonly WhatsMineIsMine: Cheat;
    readonly BreatheDeep: Cheat;
    readonly Noglues: Cheat;
}

declare const Cheat: CheatTable;

/**
 * The game types of the game header, ids 0 to 15; the ids without a type are named "Game mode" and the id.
 */
interface GameModeTable extends ConstantTable<GameMode> {
    readonly Melee: GameMode;
    readonly FreeForAll: GameMode;
    readonly OneOnOne: GameMode;
    readonly CaptureTheFlag: GameMode;
    readonly Greed: GameMode;
    readonly Slaughter: GameMode;
    readonly SuddenDeath: GameMode;
    readonly Ladder: GameMode;
    readonly UseMapSettings: GameMode;
    readonly TeamMelee: GameMode;
    readonly TeamFreeForAll: GameMode;
    readonly TeamCaptureTheFlag: GameMode;
    readonly TopVsBottom: GameMode;
}

declare const GameMode: GameModeTable;

interface UnitSizeTable extends ConstantTable<UnitSize> {
    readonly Independent: UnitSize;
    readonly Small: UnitSize;
    readonly Medium: UnitSize;
    readonly Large: UnitSize;
}

declare const UnitSize: UnitSizeTable;

interface DamageTypeTable extends ConstantTable<DamageType> {
    readonly Independent: DamageType;
    readonly Explosive: DamageType;
    readonly Concussive: DamageType;
    readonly Normal: DamageType;
    readonly IgnoreArmor: DamageType;
}

declare const DamageType: DamageTypeTable;

/**
 * The elevation flags of a location: a set flag leaves the units at its elevation out of the location.
 */
interface LocationFlagTable {
    readonly LowGround: LocationFlag;
    readonly MiddleGround: LocationFlag;
    readonly HighGround: LocationFlag;
    readonly LowAir: LocationFlag;
    readonly MiddleAir: LocationFlag;
    readonly HighAir: LocationFlag;
}

declare const LocationFlag: LocationFlagTable;

/**
 * A draw function of images.dat, as SC:R's renderer names it; its id is the images.dat value.
 */
interface DrawFunction extends GameType {
}

interface DrawFunctionTable extends ConstantTable<DrawFunction> {
    readonly Normal: DrawFunction;
    readonly NormalUnchecked: DrawFunction;
    readonly UnseenCloaking: DrawFunction;
    readonly UnseenCloaked: DrawFunction;
    readonly UnseenDecloaking: DrawFunction;
    readonly Cloaking: DrawFunction;
    readonly Cloaked: DrawFunction;
    readonly Decloaking: DrawFunction;
    readonly Distortion: DrawFunction;
    readonly Remapping: DrawFunction;
    readonly Shadow: DrawFunction;
    readonly HealthBar: DrawFunction;
    readonly WarpTexture: DrawFunction;
    readonly SelectionCircle: DrawFunction;
    readonly PlayerColor: DrawFunction;
    readonly UpdateRectangle: DrawFunction;
    readonly Hallucination: DrawFunction;
    readonly WarpFlash: DrawFunction;
}

declare const DrawFunction: DrawFunctionTable;

/**
 * An order state of a bullet ({@link Bullet.getState}).
 */
interface BulletState extends GameType {
}

interface BulletStateTable extends ConstantTable<BulletState> {
    readonly Init: BulletState;
    readonly Move: BulletState;
    readonly Follow: BulletState;
    readonly Bounce: BulletState;
    readonly DamageOverTime: BulletState;
    readonly Dying: BulletState;
    readonly HitNearTarget: BulletState;
}

declare const BulletState: BulletStateTable;

interface MouseButtonTable extends ConstantTable<MouseButton> {
    readonly Left: MouseButton;
    readonly Right: MouseButton;
    readonly Middle: MouseButton;
}

declare const MouseButton: MouseButtonTable;

/**
 * The keys by Windows virtual-key code: the named ones, every other code by {@link ConstantTable.get}.
 */
interface KeyTable extends ConstantTable<Key> {
    // GENERATED Key
    /** 8 Backspace */ readonly Backspace: Key;
    /** 9 Tab */ readonly Tab: Key;
    /** 13 Enter */ readonly Enter: Key;
    /** 16 Shift */ readonly Shift: Key;
    /** 17 Control */ readonly Control: Key;
    /** 18 Alt */ readonly Alt: Key;
    /** 19 Pause */ readonly Pause: Key;
    /** 20 CapsLock */ readonly CapsLock: Key;
    /** 27 Escape */ readonly Escape: Key;
    /** 32 Space */ readonly Space: Key;
    /** 33 PageUp */ readonly PageUp: Key;
    /** 34 PageDown */ readonly PageDown: Key;
    /** 35 End */ readonly End: Key;
    /** 36 Home */ readonly Home: Key;
    /** 37 Left */ readonly Left: Key;
    /** 38 Up */ readonly Up: Key;
    /** 39 Right */ readonly Right: Key;
    /** 40 Down */ readonly Down: Key;
    /** 45 Insert */ readonly Insert: Key;
    /** 46 Delete */ readonly Delete: Key;
    /** 48 Digit0 */ readonly Digit0: Key;
    /** 49 Digit1 */ readonly Digit1: Key;
    /** 50 Digit2 */ readonly Digit2: Key;
    /** 51 Digit3 */ readonly Digit3: Key;
    /** 52 Digit4 */ readonly Digit4: Key;
    /** 53 Digit5 */ readonly Digit5: Key;
    /** 54 Digit6 */ readonly Digit6: Key;
    /** 55 Digit7 */ readonly Digit7: Key;
    /** 56 Digit8 */ readonly Digit8: Key;
    /** 57 Digit9 */ readonly Digit9: Key;
    /** 65 A */ readonly A: Key;
    /** 66 B */ readonly B: Key;
    /** 67 C */ readonly C: Key;
    /** 68 D */ readonly D: Key;
    /** 69 E */ readonly E: Key;
    /** 70 F */ readonly F: Key;
    /** 71 G */ readonly G: Key;
    /** 72 H */ readonly H: Key;
    /** 73 I */ readonly I: Key;
    /** 74 J */ readonly J: Key;
    /** 75 K */ readonly K: Key;
    /** 76 L */ readonly L: Key;
    /** 77 M */ readonly M: Key;
    /** 78 N */ readonly N: Key;
    /** 79 O */ readonly O: Key;
    /** 80 P */ readonly P: Key;
    /** 81 Q */ readonly Q: Key;
    /** 82 R */ readonly R: Key;
    /** 83 S */ readonly S: Key;
    /** 84 T */ readonly T: Key;
    /** 85 U */ readonly U: Key;
    /** 86 V */ readonly V: Key;
    /** 87 W */ readonly W: Key;
    /** 88 X */ readonly X: Key;
    /** 89 Y */ readonly Y: Key;
    /** 90 Z */ readonly Z: Key;
    /** 96 Numpad0 */ readonly Numpad0: Key;
    /** 97 Numpad1 */ readonly Numpad1: Key;
    /** 98 Numpad2 */ readonly Numpad2: Key;
    /** 99 Numpad3 */ readonly Numpad3: Key;
    /** 100 Numpad4 */ readonly Numpad4: Key;
    /** 101 Numpad5 */ readonly Numpad5: Key;
    /** 102 Numpad6 */ readonly Numpad6: Key;
    /** 103 Numpad7 */ readonly Numpad7: Key;
    /** 104 Numpad8 */ readonly Numpad8: Key;
    /** 105 Numpad9 */ readonly Numpad9: Key;
    /** 106 NumpadMultiply */ readonly NumpadMultiply: Key;
    /** 107 NumpadAdd */ readonly NumpadAdd: Key;
    /** 109 NumpadSubtract */ readonly NumpadSubtract: Key;
    /** 110 NumpadDecimal */ readonly NumpadDecimal: Key;
    /** 111 NumpadDivide */ readonly NumpadDivide: Key;
    /** 112 F1 */ readonly F1: Key;
    /** 113 F2 */ readonly F2: Key;
    /** 114 F3 */ readonly F3: Key;
    /** 115 F4 */ readonly F4: Key;
    /** 116 F5 */ readonly F5: Key;
    /** 117 F6 */ readonly F6: Key;
    /** 118 F7 */ readonly F7: Key;
    /** 119 F8 */ readonly F8: Key;
    /** 120 F9 */ readonly F9: Key;
    /** 121 F10 */ readonly F10: Key;
    /** 122 F11 */ readonly F11: Key;
    /** 123 F12 */ readonly F12: Key;
    /** 186 Semicolon */ readonly Semicolon: Key;
    /** 187 Equal */ readonly Equal: Key;
    /** 188 Comma */ readonly Comma: Key;
    /** 189 Minus */ readonly Minus: Key;
    /** 190 Period */ readonly Period: Key;
    /** 191 Slash */ readonly Slash: Key;
    /** 192 Backquote */ readonly Backquote: Key;
    /** 219 BracketLeft */ readonly BracketLeft: Key;
    /** 220 Backslash */ readonly Backslash: Key;
    /** 221 BracketRight */ readonly BracketRight: Key;
    /** 222 Quote */ readonly Quote: Key;
}

declare const Key: KeyTable;

/**
 * The image types by their names from the GRP paths of the installation; the names are known when the game starts.
 */
interface ImageTypeNames {
    readonly [name: string]: ImageType;
}

declare const ImageType: ConstantTable<ImageType> & ImageTypeNames;

/**
 * The sprite types by the names of their image types; the names are known when the game starts.
 */
interface SpriteTypeNames {
    readonly [name: string]: SpriteType;
}

declare const SpriteType: ConstantTable<SpriteType> & SpriteTypeNames;

/**
 * The flingy types by the names of their sprite types; the names are known when the game starts.
 */
interface FlingyTypeNames {
    readonly [name: string]: FlingyType;
}

declare const FlingyType: ConstantTable<FlingyType> & FlingyTypeNames;

interface UnitTypeTable extends ConstantTable<UnitType> {
    // GENERATED UnitType
    /** 0 Terran Marine */ readonly TerranMarine: UnitType;
    /** 1 Terran Ghost */ readonly TerranGhost: UnitType;
    /** 2 Terran Vulture */ readonly TerranVulture: UnitType;
    /** 3 Terran Goliath */ readonly TerranGoliath: UnitType;
    /** 4 Terran Goliath Turret */ readonly TerranGoliathTurret: UnitType;
    /** 5 Terran Siege Tank Tank Mode */ readonly TerranSiegeTankTankMode: UnitType;
    /** 6 Terran Siege Tank Tank Mode Turret */ readonly TerranSiegeTankTankModeTurret: UnitType;
    /** 7 Terran SCV */ readonly TerranSCV: UnitType;
    /** 8 Terran Wraith */ readonly TerranWraith: UnitType;
    /** 9 Terran Science Vessel */ readonly TerranScienceVessel: UnitType;
    /** 10 Hero Gui Montag */ readonly HeroGuiMontag: UnitType;
    /** 11 Terran Dropship */ readonly TerranDropship: UnitType;
    /** 12 Terran Battlecruiser */ readonly TerranBattlecruiser: UnitType;
    /** 13 Terran Vulture Spider Mine */ readonly TerranVultureSpiderMine: UnitType;
    /** 14 Terran Nuclear Missile */ readonly TerranNuclearMissile: UnitType;
    /** 15 Terran Civilian */ readonly TerranCivilian: UnitType;
    /** 16 Hero Sarah Kerrigan */ readonly HeroSarahKerrigan: UnitType;
    /** 17 Hero Alan Schezar */ readonly HeroAlanSchezar: UnitType;
    /** 18 Hero Alan Schezar Turret */ readonly HeroAlanSchezarTurret: UnitType;
    /** 19 Hero Jim Raynor Vulture */ readonly HeroJimRaynorVulture: UnitType;
    /** 20 Hero Jim Raynor Marine */ readonly HeroJimRaynorMarine: UnitType;
    /** 21 Hero Tom Kazansky */ readonly HeroTomKazansky: UnitType;
    /** 22 Hero Magellan */ readonly HeroMagellan: UnitType;
    /** 23 Hero Edmund Duke Tank Mode */ readonly HeroEdmundDukeTankMode: UnitType;
    /** 24 Hero Edmund Duke Tank Mode Turret */ readonly HeroEdmundDukeTankModeTurret: UnitType;
    /** 25 Hero Edmund Duke Siege Mode */ readonly HeroEdmundDukeSiegeMode: UnitType;
    /** 26 Hero Edmund Duke Siege Mode Turret */ readonly HeroEdmundDukeSiegeModeTurret: UnitType;
    /** 27 Hero Arcturus Mengsk */ readonly HeroArcturusMengsk: UnitType;
    /** 28 Hero Hyperion */ readonly HeroHyperion: UnitType;
    /** 29 Hero Norad II */ readonly HeroNoradII: UnitType;
    /** 30 Terran Siege Tank Siege Mode */ readonly TerranSiegeTankSiegeMode: UnitType;
    /** 31 Terran Siege Tank Siege Mode Turret */ readonly TerranSiegeTankSiegeModeTurret: UnitType;
    /** 32 Terran Firebat */ readonly TerranFirebat: UnitType;
    /** 33 Spell Scanner Sweep */ readonly SpellScannerSweep: UnitType;
    /** 34 Terran Medic */ readonly TerranMedic: UnitType;
    /** 35 Zerg Larva */ readonly ZergLarva: UnitType;
    /** 36 Zerg Egg */ readonly ZergEgg: UnitType;
    /** 37 Zerg Zergling */ readonly ZergZergling: UnitType;
    /** 38 Zerg Hydralisk */ readonly ZergHydralisk: UnitType;
    /** 39 Zerg Ultralisk */ readonly ZergUltralisk: UnitType;
    /** 40 Zerg Broodling */ readonly ZergBroodling: UnitType;
    /** 41 Zerg Drone */ readonly ZergDrone: UnitType;
    /** 42 Zerg Overlord */ readonly ZergOverlord: UnitType;
    /** 43 Zerg Mutalisk */ readonly ZergMutalisk: UnitType;
    /** 44 Zerg Guardian */ readonly ZergGuardian: UnitType;
    /** 45 Zerg Queen */ readonly ZergQueen: UnitType;
    /** 46 Zerg Defiler */ readonly ZergDefiler: UnitType;
    /** 47 Zerg Scourge */ readonly ZergScourge: UnitType;
    /** 48 Hero Torrasque */ readonly HeroTorrasque: UnitType;
    /** 49 Hero Matriarch */ readonly HeroMatriarch: UnitType;
    /** 50 Zerg Infested Terran */ readonly ZergInfestedTerran: UnitType;
    /** 51 Hero Infested Kerrigan */ readonly HeroInfestedKerrigan: UnitType;
    /** 52 Hero Unclean One */ readonly HeroUncleanOne: UnitType;
    /** 53 Hero Hunter Killer */ readonly HeroHunterKiller: UnitType;
    /** 54 Hero Devouring One */ readonly HeroDevouringOne: UnitType;
    /** 55 Hero Kukulza Mutalisk */ readonly HeroKukulzaMutalisk: UnitType;
    /** 56 Hero Kukulza Guardian */ readonly HeroKukulzaGuardian: UnitType;
    /** 57 Hero Yggdrasill */ readonly HeroYggdrasill: UnitType;
    /** 58 Terran Valkyrie */ readonly TerranValkyrie: UnitType;
    /** 59 Zerg Cocoon */ readonly ZergCocoon: UnitType;
    /** 60 Protoss Corsair */ readonly ProtossCorsair: UnitType;
    /** 61 Protoss Dark Templar */ readonly ProtossDarkTemplar: UnitType;
    /** 62 Zerg Devourer */ readonly ZergDevourer: UnitType;
    /** 63 Protoss Dark Archon */ readonly ProtossDarkArchon: UnitType;
    /** 64 Protoss Probe */ readonly ProtossProbe: UnitType;
    /** 65 Protoss Zealot */ readonly ProtossZealot: UnitType;
    /** 66 Protoss Dragoon */ readonly ProtossDragoon: UnitType;
    /** 67 Protoss High Templar */ readonly ProtossHighTemplar: UnitType;
    /** 68 Protoss Archon */ readonly ProtossArchon: UnitType;
    /** 69 Protoss Shuttle */ readonly ProtossShuttle: UnitType;
    /** 70 Protoss Scout */ readonly ProtossScout: UnitType;
    /** 71 Protoss Arbiter */ readonly ProtossArbiter: UnitType;
    /** 72 Protoss Carrier */ readonly ProtossCarrier: UnitType;
    /** 73 Protoss Interceptor */ readonly ProtossInterceptor: UnitType;
    /** 74 Hero Dark Templar */ readonly HeroDarkTemplar: UnitType;
    /** 75 Hero Zeratul */ readonly HeroZeratul: UnitType;
    /** 76 Hero Tassadar Zeratul Archon */ readonly HeroTassadarZeratulArchon: UnitType;
    /** 77 Hero Fenix Zealot */ readonly HeroFenixZealot: UnitType;
    /** 78 Hero Fenix Dragoon */ readonly HeroFenixDragoon: UnitType;
    /** 79 Hero Tassadar */ readonly HeroTassadar: UnitType;
    /** 80 Hero Mojo */ readonly HeroMojo: UnitType;
    /** 81 Hero Warbringer */ readonly HeroWarbringer: UnitType;
    /** 82 Hero Gantrithor */ readonly HeroGantrithor: UnitType;
    /** 83 Protoss Reaver */ readonly ProtossReaver: UnitType;
    /** 84 Protoss Observer */ readonly ProtossObserver: UnitType;
    /** 85 Protoss Scarab */ readonly ProtossScarab: UnitType;
    /** 86 Hero Danimoth */ readonly HeroDanimoth: UnitType;
    /** 87 Hero Aldaris */ readonly HeroAldaris: UnitType;
    /** 88 Hero Artanis */ readonly HeroArtanis: UnitType;
    /** 89 Critter Rhynadon */ readonly CritterRhynadon: UnitType;
    /** 90 Critter Bengalaas */ readonly CritterBengalaas: UnitType;
    /** 91 Special Cargo Ship */ readonly SpecialCargoShip: UnitType;
    /** 92 Special Mercenary Gunship */ readonly SpecialMercenaryGunship: UnitType;
    /** 93 Critter Scantid */ readonly CritterScantid: UnitType;
    /** 94 Critter Kakaru */ readonly CritterKakaru: UnitType;
    /** 95 Critter Ragnasaur */ readonly CritterRagnasaur: UnitType;
    /** 96 Critter Ursadon */ readonly CritterUrsadon: UnitType;
    /** 97 Zerg Lurker Egg */ readonly ZergLurkerEgg: UnitType;
    /** 98 Hero Raszagal */ readonly HeroRaszagal: UnitType;
    /** 99 Hero Samir Duran */ readonly HeroSamirDuran: UnitType;
    /** 100 Hero Alexei Stukov */ readonly HeroAlexeiStukov: UnitType;
    /** 101 Special Map Revealer */ readonly SpecialMapRevealer: UnitType;
    /** 102 Hero Gerard DuGalle */ readonly HeroGerardDuGalle: UnitType;
    /** 103 Zerg Lurker */ readonly ZergLurker: UnitType;
    /** 104 Hero Infested Duran */ readonly HeroInfestedDuran: UnitType;
    /** 105 Spell Disruption Web */ readonly SpellDisruptionWeb: UnitType;
    /** 106 Terran Command Center */ readonly TerranCommandCenter: UnitType;
    /** 107 Terran Comsat Station */ readonly TerranComsatStation: UnitType;
    /** 108 Terran Nuclear Silo */ readonly TerranNuclearSilo: UnitType;
    /** 109 Terran Supply Depot */ readonly TerranSupplyDepot: UnitType;
    /** 110 Terran Refinery */ readonly TerranRefinery: UnitType;
    /** 111 Terran Barracks */ readonly TerranBarracks: UnitType;
    /** 112 Terran Academy */ readonly TerranAcademy: UnitType;
    /** 113 Terran Factory */ readonly TerranFactory: UnitType;
    /** 114 Terran Starport */ readonly TerranStarport: UnitType;
    /** 115 Terran Control Tower */ readonly TerranControlTower: UnitType;
    /** 116 Terran Science Facility */ readonly TerranScienceFacility: UnitType;
    /** 117 Terran Covert Ops */ readonly TerranCovertOps: UnitType;
    /** 118 Terran Physics Lab */ readonly TerranPhysicsLab: UnitType;
    /** 119 Unused Terran1 */ readonly UnusedTerran1: UnitType;
    /** 120 Terran Machine Shop */ readonly TerranMachineShop: UnitType;
    /** 121 Unused Terran2 */ readonly UnusedTerran2: UnitType;
    /** 122 Terran Engineering Bay */ readonly TerranEngineeringBay: UnitType;
    /** 123 Terran Armory */ readonly TerranArmory: UnitType;
    /** 124 Terran Missile Turret */ readonly TerranMissileTurret: UnitType;
    /** 125 Terran Bunker */ readonly TerranBunker: UnitType;
    /** 126 Special Crashed Norad II */ readonly SpecialCrashedNoradII: UnitType;
    /** 127 Special Ion Cannon */ readonly SpecialIonCannon: UnitType;
    /** 128 Powerup Uraj Crystal */ readonly PowerupUrajCrystal: UnitType;
    /** 129 Powerup Khalis Crystal */ readonly PowerupKhalisCrystal: UnitType;
    /** 130 Zerg Infested Command Center */ readonly ZergInfestedCommandCenter: UnitType;
    /** 131 Zerg Hatchery */ readonly ZergHatchery: UnitType;
    /** 132 Zerg Lair */ readonly ZergLair: UnitType;
    /** 133 Zerg Hive */ readonly ZergHive: UnitType;
    /** 134 Zerg Nydus Canal */ readonly ZergNydusCanal: UnitType;
    /** 135 Zerg Hydralisk Den */ readonly ZergHydraliskDen: UnitType;
    /** 136 Zerg Defiler Mound */ readonly ZergDefilerMound: UnitType;
    /** 137 Zerg Greater Spire */ readonly ZergGreaterSpire: UnitType;
    /** 138 Zerg Queens Nest */ readonly ZergQueensNest: UnitType;
    /** 139 Zerg Evolution Chamber */ readonly ZergEvolutionChamber: UnitType;
    /** 140 Zerg Ultralisk Cavern */ readonly ZergUltraliskCavern: UnitType;
    /** 141 Zerg Spire */ readonly ZergSpire: UnitType;
    /** 142 Zerg Spawning Pool */ readonly ZergSpawningPool: UnitType;
    /** 143 Zerg Creep Colony */ readonly ZergCreepColony: UnitType;
    /** 144 Zerg Spore Colony */ readonly ZergSporeColony: UnitType;
    /** 145 Unused Zerg1 */ readonly UnusedZerg1: UnitType;
    /** 146 Zerg Sunken Colony */ readonly ZergSunkenColony: UnitType;
    /** 147 Special Overmind With Shell */ readonly SpecialOvermindWithShell: UnitType;
    /** 148 Special Overmind */ readonly SpecialOvermind: UnitType;
    /** 149 Zerg Extractor */ readonly ZergExtractor: UnitType;
    /** 150 Special Mature Chrysalis */ readonly SpecialMatureChrysalis: UnitType;
    /** 151 Special Cerebrate */ readonly SpecialCerebrate: UnitType;
    /** 152 Special Cerebrate Daggoth */ readonly SpecialCerebrateDaggoth: UnitType;
    /** 153 Unused Zerg2 */ readonly UnusedZerg2: UnitType;
    /** 154 Protoss Nexus */ readonly ProtossNexus: UnitType;
    /** 155 Protoss Robotics Facility */ readonly ProtossRoboticsFacility: UnitType;
    /** 156 Protoss Pylon */ readonly ProtossPylon: UnitType;
    /** 157 Protoss Assimilator */ readonly ProtossAssimilator: UnitType;
    /** 158 Unused Protoss1 */ readonly UnusedProtoss1: UnitType;
    /** 159 Protoss Observatory */ readonly ProtossObservatory: UnitType;
    /** 160 Protoss Gateway */ readonly ProtossGateway: UnitType;
    /** 161 Unused Protoss2 */ readonly UnusedProtoss2: UnitType;
    /** 162 Protoss Photon Cannon */ readonly ProtossPhotonCannon: UnitType;
    /** 163 Protoss Citadel of Adun */ readonly ProtossCitadelOfAdun: UnitType;
    /** 164 Protoss Cybernetics Core */ readonly ProtossCyberneticsCore: UnitType;
    /** 165 Protoss Templar Archives */ readonly ProtossTemplarArchives: UnitType;
    /** 166 Protoss Forge */ readonly ProtossForge: UnitType;
    /** 167 Protoss Stargate */ readonly ProtossStargate: UnitType;
    /** 168 Special Stasis Cell Prison */ readonly SpecialStasisCellPrison: UnitType;
    /** 169 Protoss Fleet Beacon */ readonly ProtossFleetBeacon: UnitType;
    /** 170 Protoss Arbiter Tribunal */ readonly ProtossArbiterTribunal: UnitType;
    /** 171 Protoss Robotics Support Bay */ readonly ProtossRoboticsSupportBay: UnitType;
    /** 172 Protoss Shield Battery */ readonly ProtossShieldBattery: UnitType;
    /** 173 Special Khaydarin Crystal Form */ readonly SpecialKhaydarinCrystalForm: UnitType;
    /** 174 Special Protoss Temple */ readonly SpecialProtossTemple: UnitType;
    /** 175 Special XelNaga Temple */ readonly SpecialXelNagaTemple: UnitType;
    /** 176 Resource Mineral Field */ readonly ResourceMineralField: UnitType;
    /** 177 Resource Mineral Field Type 2 */ readonly ResourceMineralFieldType2: UnitType;
    /** 178 Resource Mineral Field Type 3 */ readonly ResourceMineralFieldType3: UnitType;
    /** 179 Unused Cave */ readonly UnusedCave: UnitType;
    /** 180 Unused Cave In */ readonly UnusedCaveIn: UnitType;
    /** 181 Unused Cantina */ readonly UnusedCantina: UnitType;
    /** 182 Unused Mining Platform */ readonly UnusedMiningPlatform: UnitType;
    /** 183 Unused Independant Command Center */ readonly UnusedIndependantCommandCenter: UnitType;
    /** 184 Special Independant Starport */ readonly SpecialIndependantStarport: UnitType;
    /** 185 Unused Independant Jump Gate */ readonly UnusedIndependantJumpGate: UnitType;
    /** 186 Unused Ruins */ readonly UnusedRuins: UnitType;
    /** 187 Unused Khaydarin Crystal Formation */ readonly UnusedKhaydarinCrystalFormation: UnitType;
    /** 188 Resource Vespene Geyser */ readonly ResourceVespeneGeyser: UnitType;
    /** 189 Special Warp Gate */ readonly SpecialWarpGate: UnitType;
    /** 190 Special Psi Disrupter */ readonly SpecialPsiDisrupter: UnitType;
    /** 191 Unused Zerg Marker */ readonly UnusedZergMarker: UnitType;
    /** 192 Unused Terran Marker */ readonly UnusedTerranMarker: UnitType;
    /** 193 Unused Protoss Marker */ readonly UnusedProtossMarker: UnitType;
    /** 194 Special Zerg Beacon */ readonly SpecialZergBeacon: UnitType;
    /** 195 Special Terran Beacon */ readonly SpecialTerranBeacon: UnitType;
    /** 196 Special Protoss Beacon */ readonly SpecialProtossBeacon: UnitType;
    /** 197 Special Zerg Flag Beacon */ readonly SpecialZergFlagBeacon: UnitType;
    /** 198 Special Terran Flag Beacon */ readonly SpecialTerranFlagBeacon: UnitType;
    /** 199 Special Protoss Flag Beacon */ readonly SpecialProtossFlagBeacon: UnitType;
    /** 200 Special Power Generator */ readonly SpecialPowerGenerator: UnitType;
    /** 201 Special Overmind Cocoon */ readonly SpecialOvermindCocoon: UnitType;
    /** 202 Spell Dark Swarm */ readonly SpellDarkSwarm: UnitType;
    /** 203 Special Floor Missile Trap */ readonly SpecialFloorMissileTrap: UnitType;
    /** 204 Special Floor Hatch */ readonly SpecialFloorHatch: UnitType;
    /** 205 Special Upper Level Door */ readonly SpecialUpperLevelDoor: UnitType;
    /** 206 Special Right Upper Level Door */ readonly SpecialRightUpperLevelDoor: UnitType;
    /** 207 Special Pit Door */ readonly SpecialPitDoor: UnitType;
    /** 208 Special Right Pit Door */ readonly SpecialRightPitDoor: UnitType;
    /** 209 Special Floor Gun Trap */ readonly SpecialFloorGunTrap: UnitType;
    /** 210 Special Wall Missile Trap */ readonly SpecialWallMissileTrap: UnitType;
    /** 211 Special Wall Flame Trap */ readonly SpecialWallFlameTrap: UnitType;
    /** 212 Special Right Wall Missile Trap */ readonly SpecialRightWallMissileTrap: UnitType;
    /** 213 Special Right Wall Flame Trap */ readonly SpecialRightWallFlameTrap: UnitType;
    /** 214 Special Start Location */ readonly SpecialStartLocation: UnitType;
    /** 215 Powerup Flag */ readonly PowerupFlag: UnitType;
    /** 216 Powerup Young Chrysalis */ readonly PowerupYoungChrysalis: UnitType;
    /** 217 Powerup Psi Emitter */ readonly PowerupPsiEmitter: UnitType;
    /** 218 Powerup Data Disk */ readonly PowerupDataDisk: UnitType;
    /** 219 Powerup Khaydarin Crystal */ readonly PowerupKhaydarinCrystal: UnitType;
    /** 220 Powerup Mineral Cluster Type 1 */ readonly PowerupMineralClusterType1: UnitType;
    /** 221 Powerup Mineral Cluster Type 2 */ readonly PowerupMineralClusterType2: UnitType;
    /** 222 Powerup Protoss Gas Orb Type 1 */ readonly PowerupProtossGasOrbType1: UnitType;
    /** 223 Powerup Protoss Gas Orb Type 2 */ readonly PowerupProtossGasOrbType2: UnitType;
    /** 224 Powerup Zerg Gas Sac Type 1 */ readonly PowerupZergGasSacType1: UnitType;
    /** 225 Powerup Zerg Gas Sac Type 2 */ readonly PowerupZergGasSacType2: UnitType;
    /** 226 Powerup Terran Gas Tank Type 1 */ readonly PowerupTerranGasTankType1: UnitType;
    /** 227 Powerup Terran Gas Tank Type 2 */ readonly PowerupTerranGasTankType2: UnitType;
}

declare const UnitType: UnitTypeTable;

interface WeaponTypeTable extends ConstantTable<WeaponType> {
    // GENERATED WeaponType
    /** 0 Gauss Rifle */ readonly GaussRifle: WeaponType;
    /** 1 Gauss Rifle Jim Raynor */ readonly GaussRifleJimRaynor: WeaponType;
    /** 2 C 10 Canister Rifle */ readonly C10CanisterRifle: WeaponType;
    /** 3 C 10 Canister Rifle Sarah Kerrigan */ readonly C10CanisterRifleSarahKerrigan: WeaponType;
    /** 4 Fragmentation Grenade */ readonly FragmentationGrenade: WeaponType;
    /** 5 Fragmentation Grenade Jim Raynor */ readonly FragmentationGrenadeJimRaynor: WeaponType;
    /** 6 Spider Mines */ readonly SpiderMines: WeaponType;
    /** 7 Twin Autocannons */ readonly TwinAutocannons: WeaponType;
    /** 8 Hellfire Missile Pack */ readonly HellfireMissilePack: WeaponType;
    /** 9 Twin Autocannons Alan Schezar */ readonly TwinAutocannonsAlanSchezar: WeaponType;
    /** 10 Hellfire Missile Pack Alan Schezar */ readonly HellfireMissilePackAlanSchezar: WeaponType;
    /** 11 Arclite Cannon */ readonly ArcliteCannon: WeaponType;
    /** 12 Arclite Cannon Edmund Duke */ readonly ArcliteCannonEdmundDuke: WeaponType;
    /** 13 Fusion Cutter */ readonly FusionCutter: WeaponType;
    /** 14 unknown entry 14 */ readonly UnknownEntry14: WeaponType;
    /** 15 Gemini Missiles */ readonly GeminiMissiles: WeaponType;
    /** 16 Burst Lasers */ readonly BurstLasers: WeaponType;
    /** 17 Gemini Missiles Tom Kazansky */ readonly GeminiMissilesTomKazansky: WeaponType;
    /** 18 Burst Lasers Tom Kazansky */ readonly BurstLasersTomKazansky: WeaponType;
    /** 19 ATS Laser Battery */ readonly ATSLaserBattery: WeaponType;
    /** 20 ATA Laser Battery */ readonly ATALaserBattery: WeaponType;
    /** 21 ATS Laser Battery Hero */ readonly ATSLaserBatteryHero: WeaponType;
    /** 22 ATA Laser Battery Hero */ readonly ATALaserBatteryHero: WeaponType;
    /** 23 ATS Laser Battery Hyperion */ readonly ATSLaserBatteryHyperion: WeaponType;
    /** 24 ATA Laser Battery Hyperion */ readonly ATALaserBatteryHyperion: WeaponType;
    /** 25 Flame Thrower */ readonly FlameThrower: WeaponType;
    /** 26 Flame Thrower Gui Montag */ readonly FlameThrowerGuiMontag: WeaponType;
    /** 27 Arclite Shock Cannon */ readonly ArcliteShockCannon: WeaponType;
    /** 28 Arclite Shock Cannon Edmund Duke */ readonly ArcliteShockCannonEdmundDuke: WeaponType;
    /** 29 Longbolt Missile */ readonly LongboltMissile: WeaponType;
    /** 30 Yamato Gun */ readonly YamatoGun: WeaponType;
    /** 31 Nuclear Strike */ readonly NuclearStrike: WeaponType;
    /** 32 Lockdown */ readonly Lockdown: WeaponType;
    /** 33 EMP Shockwave */ readonly EMPShockwave: WeaponType;
    /** 34 Irradiate */ readonly Irradiate: WeaponType;
    /** 35 Claws */ readonly Claws: WeaponType;
    /** 36 Claws Devouring One */ readonly ClawsDevouringOne: WeaponType;
    /** 37 Claws Infested Kerrigan */ readonly ClawsInfestedKerrigan: WeaponType;
    /** 38 Needle Spines */ readonly NeedleSpines: WeaponType;
    /** 39 Needle Spines Hunter Killer */ readonly NeedleSpinesHunterKiller: WeaponType;
    /** 40 Kaiser Blades */ readonly KaiserBlades: WeaponType;
    /** 41 Kaiser Blades Torrasque */ readonly KaiserBladesTorrasque: WeaponType;
    /** 42 Toxic Spores */ readonly ToxicSpores: WeaponType;
    /** 43 Spines */ readonly Spines: WeaponType;
    /** 44 unknown entry 44 */ readonly UnknownEntry44: WeaponType;
    /** 45 unknown entry 45 */ readonly UnknownEntry45: WeaponType;
    /** 46 Acid Spore */ readonly AcidSpore: WeaponType;
    /** 47 Acid Spore Kukulza */ readonly AcidSporeKukulza: WeaponType;
    /** 48 Glave Wurm */ readonly GlaveWurm: WeaponType;
    /** 49 Glave Wurm Kukulza */ readonly GlaveWurmKukulza: WeaponType;
    /** 50 unknown entry 50 */ readonly UnknownEntry50: WeaponType;
    /** 51 unknown entry 51 */ readonly UnknownEntry51: WeaponType;
    /** 52 Seeker Spores */ readonly SeekerSpores: WeaponType;
    /** 53 Subterranean Tentacle */ readonly SubterraneanTentacle: WeaponType;
    /** 54 Suicide Infested Terran */ readonly SuicideInfestedTerran: WeaponType;
    /** 55 Suicide Scourge */ readonly SuicideScourge: WeaponType;
    /** 56 Parasite */ readonly Parasite: WeaponType;
    /** 57 Spawn Broodlings */ readonly SpawnBroodlings: WeaponType;
    /** 58 Ensnare */ readonly Ensnare: WeaponType;
    /** 59 Dark Swarm */ readonly DarkSwarm: WeaponType;
    /** 60 Plague */ readonly Plague: WeaponType;
    /** 61 Consume */ readonly Consume: WeaponType;
    /** 62 Particle Beam */ readonly ParticleBeam: WeaponType;
    /** 63 unknown entry 63 */ readonly UnknownEntry63: WeaponType;
    /** 64 Psi Blades */ readonly PsiBlades: WeaponType;
    /** 65 Psi Blades Fenix */ readonly PsiBladesFenix: WeaponType;
    /** 66 Phase Disruptor */ readonly PhaseDisruptor: WeaponType;
    /** 67 Phase Disruptor Fenix */ readonly PhaseDisruptorFenix: WeaponType;
    /** 68 unknown entry 68 */ readonly UnknownEntry68: WeaponType;
    /** 69 Psi Assault */ readonly PsiAssault: WeaponType;
    /** 70 Psionic Shockwave */ readonly PsionicShockwave: WeaponType;
    /** 71 Psionic Shockwave TZ Archon */ readonly PsionicShockwaveTZArchon: WeaponType;
    /** 72 unknown entry 72 */ readonly UnknownEntry72: WeaponType;
    /** 73 Dual Photon Blasters */ readonly DualPhotonBlasters: WeaponType;
    /** 74 Anti Matter Missiles */ readonly AntiMatterMissiles: WeaponType;
    /** 75 Dual Photon Blasters Mojo */ readonly DualPhotonBlastersMojo: WeaponType;
    /** 76 Anti Matter Missiles Mojo */ readonly AntiMatterMissilesMojo: WeaponType;
    /** 77 Phase Disruptor Cannon */ readonly PhaseDisruptorCannon: WeaponType;
    /** 78 Phase Disruptor Cannon Danimoth */ readonly PhaseDisruptorCannonDanimoth: WeaponType;
    /** 79 Pulse Cannon */ readonly PulseCannon: WeaponType;
    /** 80 STS Photon Cannon */ readonly STSPhotonCannon: WeaponType;
    /** 81 STA Photon Cannon */ readonly STAPhotonCannon: WeaponType;
    /** 82 Scarab */ readonly Scarab: WeaponType;
    /** 83 Stasis Field */ readonly StasisField: WeaponType;
    /** 84 Psionic Storm */ readonly PsionicStorm: WeaponType;
    /** 85 Warp Blades Zeratul */ readonly WarpBladesZeratul: WeaponType;
    /** 86 Warp Blades Hero */ readonly WarpBladesHero: WeaponType;
    /** 87 unknown entry 87 */ readonly UnknownEntry87: WeaponType;
    /** 88 unknown entry 88 */ readonly UnknownEntry88: WeaponType;
    /** 89 unknown entry 89 */ readonly UnknownEntry89: WeaponType;
    /** 90 unknown entry 90 */ readonly UnknownEntry90: WeaponType;
    /** 91 unknown entry 91 */ readonly UnknownEntry91: WeaponType;
    /** 92 Platform Laser Battery */ readonly PlatformLaserBattery: WeaponType;
    /** 93 Independant Laser Battery */ readonly IndependantLaserBattery: WeaponType;
    /** 94 unknown entry 94 */ readonly UnknownEntry94: WeaponType;
    /** 95 unknown entry 95 */ readonly UnknownEntry95: WeaponType;
    /** 96 Twin Autocannons Floor Trap */ readonly TwinAutocannonsFloorTrap: WeaponType;
    /** 97 Hellfire Missile Pack Wall Trap */ readonly HellfireMissilePackWallTrap: WeaponType;
    /** 98 Flame Thrower Wall Trap */ readonly FlameThrowerWallTrap: WeaponType;
    /** 99 Hellfire Missile Pack Floor Trap */ readonly HellfireMissilePackFloorTrap: WeaponType;
    /** 100 Neutron Flare */ readonly NeutronFlare: WeaponType;
    /** 101 Disruption Web */ readonly DisruptionWeb: WeaponType;
    /** 102 Restoration */ readonly Restoration: WeaponType;
    /** 103 Halo Rockets */ readonly HaloRockets: WeaponType;
    /** 104 Corrosive Acid */ readonly CorrosiveAcid: WeaponType;
    /** 105 Mind Control */ readonly MindControl: WeaponType;
    /** 106 Feedback */ readonly Feedback: WeaponType;
    /** 107 Optical Flare */ readonly OpticalFlare: WeaponType;
    /** 108 Maelstrom */ readonly Maelstrom: WeaponType;
    /** 109 Subterranean Spines */ readonly SubterraneanSpines: WeaponType;
    /** 110 Warp Blades */ readonly WarpBlades: WeaponType;
    /** 111 C 10 Canister Rifle Samir Duran */ readonly C10CanisterRifleSamirDuran: WeaponType;
    /** 112 C 10 Canister Rifle Infested Duran */ readonly C10CanisterRifleInfestedDuran: WeaponType;
    /** 113 Dual Photon Blasters Artanis */ readonly DualPhotonBlastersArtanis: WeaponType;
    /** 114 Anti Matter Missiles Artanis */ readonly AntiMatterMissilesArtanis: WeaponType;
    /** 115 C 10 Canister Rifle Alexei Stukov */ readonly C10CanisterRifleAlexeiStukov: WeaponType;
    /** 116 unknown entry 116 */ readonly UnknownEntry116: WeaponType;
    /** 117 unknown entry 117 */ readonly UnknownEntry117: WeaponType;
    /** 118 unknown entry 118 */ readonly UnknownEntry118: WeaponType;
    /** 119 unknown entry 119 */ readonly UnknownEntry119: WeaponType;
    /** 120 unknown entry 120 */ readonly UnknownEntry120: WeaponType;
    /** 121 unknown entry 121 */ readonly UnknownEntry121: WeaponType;
    /** 122 unknown entry 122 */ readonly UnknownEntry122: WeaponType;
    /** 123 unknown entry 123 */ readonly UnknownEntry123: WeaponType;
    /** 124 unknown entry 124 */ readonly UnknownEntry124: WeaponType;
    /** 125 unknown entry 125 */ readonly UnknownEntry125: WeaponType;
    /** 126 unknown entry 126 */ readonly UnknownEntry126: WeaponType;
    /** 127 unknown entry 127 */ readonly UnknownEntry127: WeaponType;
    /** 128 unknown entry 128 */ readonly UnknownEntry128: WeaponType;
    /** 129 unknown entry 129 */ readonly UnknownEntry129: WeaponType;
}

declare const WeaponType: WeaponTypeTable;

interface UpgradeTypeTable extends ConstantTable<UpgradeType> {
    // GENERATED UpgradeType
    /** 0 Terran Infantry Armor */ readonly TerranInfantryArmor: UpgradeType;
    /** 1 Terran Vehicle Plating */ readonly TerranVehiclePlating: UpgradeType;
    /** 2 Terran Ship Plating */ readonly TerranShipPlating: UpgradeType;
    /** 3 Zerg Carapace */ readonly ZergCarapace: UpgradeType;
    /** 4 Zerg Flyer Carapace */ readonly ZergFlyerCarapace: UpgradeType;
    /** 5 Protoss Ground Armor */ readonly ProtossGroundArmor: UpgradeType;
    /** 6 Protoss Air Armor */ readonly ProtossAirArmor: UpgradeType;
    /** 7 Terran Infantry Weapons */ readonly TerranInfantryWeapons: UpgradeType;
    /** 8 Terran Vehicle Weapons */ readonly TerranVehicleWeapons: UpgradeType;
    /** 9 Terran Ship Weapons */ readonly TerranShipWeapons: UpgradeType;
    /** 10 Zerg Melee Attacks */ readonly ZergMeleeAttacks: UpgradeType;
    /** 11 Zerg Missile Attacks */ readonly ZergMissileAttacks: UpgradeType;
    /** 12 Zerg Flyer Attacks */ readonly ZergFlyerAttacks: UpgradeType;
    /** 13 Protoss Ground Weapons */ readonly ProtossGroundWeapons: UpgradeType;
    /** 14 Protoss Air Weapons */ readonly ProtossAirWeapons: UpgradeType;
    /** 15 Protoss Plasma Shields */ readonly ProtossPlasmaShields: UpgradeType;
    /** 16 U 238 Shells */ readonly U238Shells: UpgradeType;
    /** 17 Ion Thrusters */ readonly IonThrusters: UpgradeType;
    /** 18 unknown entry 18 */ readonly UnknownEntry18: UpgradeType;
    /** 19 Titan Reactor */ readonly TitanReactor: UpgradeType;
    /** 20 Ocular Implants */ readonly OcularImplants: UpgradeType;
    /** 21 Moebius Reactor */ readonly MoebiusReactor: UpgradeType;
    /** 22 Apollo Reactor */ readonly ApolloReactor: UpgradeType;
    /** 23 Colossus Reactor */ readonly ColossusReactor: UpgradeType;
    /** 24 Ventral Sacs */ readonly VentralSacs: UpgradeType;
    /** 25 Antennae */ readonly Antennae: UpgradeType;
    /** 26 Pneumatized Carapace */ readonly PneumatizedCarapace: UpgradeType;
    /** 27 Metabolic Boost */ readonly MetabolicBoost: UpgradeType;
    /** 28 Adrenal Glands */ readonly AdrenalGlands: UpgradeType;
    /** 29 Muscular Augments */ readonly MuscularAugments: UpgradeType;
    /** 30 Grooved Spines */ readonly GroovedSpines: UpgradeType;
    /** 31 Gamete Meiosis */ readonly GameteMeiosis: UpgradeType;
    /** 32 Metasynaptic Node */ readonly MetasynapticNode: UpgradeType;
    /** 33 Singularity Charge */ readonly SingularityCharge: UpgradeType;
    /** 34 Leg Enhancements */ readonly LegEnhancements: UpgradeType;
    /** 35 Scarab Damage */ readonly ScarabDamage: UpgradeType;
    /** 36 Reaver Capacity */ readonly ReaverCapacity: UpgradeType;
    /** 37 Gravitic Drive */ readonly GraviticDrive: UpgradeType;
    /** 38 Sensor Array */ readonly SensorArray: UpgradeType;
    /** 39 Gravitic Boosters */ readonly GraviticBoosters: UpgradeType;
    /** 40 Khaydarin Amulet */ readonly KhaydarinAmulet: UpgradeType;
    /** 41 Apial Sensors */ readonly ApialSensors: UpgradeType;
    /** 42 Gravitic Thrusters */ readonly GraviticThrusters: UpgradeType;
    /** 43 Carrier Capacity */ readonly CarrierCapacity: UpgradeType;
    /** 44 Khaydarin Core */ readonly KhaydarinCore: UpgradeType;
    /** 45 unknown entry 45 */ readonly UnknownEntry45: UpgradeType;
    /** 46 unknown entry 46 */ readonly UnknownEntry46: UpgradeType;
    /** 47 Argus Jewel */ readonly ArgusJewel: UpgradeType;
    /** 48 unknown entry 48 */ readonly UnknownEntry48: UpgradeType;
    /** 49 Argus Talisman */ readonly ArgusTalisman: UpgradeType;
    /** 50 unknown entry 50 */ readonly UnknownEntry50: UpgradeType;
    /** 51 Caduceus Reactor */ readonly CaduceusReactor: UpgradeType;
    /** 52 Chitinous Plating */ readonly ChitinousPlating: UpgradeType;
    /** 53 Anabolic Synthesis */ readonly AnabolicSynthesis: UpgradeType;
    /** 54 Charon Boosters */ readonly CharonBoosters: UpgradeType;
    /** 55 Unused 55 */ readonly Unused55: UpgradeType;
    /** 56 Unused 56 */ readonly Unused56: UpgradeType;
    /** 57 Unused 57 */ readonly Unused57: UpgradeType;
    /** 58 Unused 58 */ readonly Unused58: UpgradeType;
    /** 59 Unused 59 */ readonly Unused59: UpgradeType;
    /** 60 Unused 60 */ readonly Unused60: UpgradeType;
}

declare const UpgradeType: UpgradeTypeTable;

interface TechTypeTable extends ConstantTable<TechType> {
    // GENERATED TechType
    /** 0 Stim Packs */ readonly StimPacks: TechType;
    /** 1 Lockdown */ readonly Lockdown: TechType;
    /** 2 EMP Shockwave */ readonly EMPShockwave: TechType;
    /** 3 Spider Mines */ readonly SpiderMines: TechType;
    /** 4 Scanner Sweep */ readonly ScannerSweep: TechType;
    /** 5 Tank Siege Mode */ readonly TankSiegeMode: TechType;
    /** 6 Defensive Matrix */ readonly DefensiveMatrix: TechType;
    /** 7 Irradiate */ readonly Irradiate: TechType;
    /** 8 Yamato Gun */ readonly YamatoGun: TechType;
    /** 9 Cloaking Field */ readonly CloakingField: TechType;
    /** 10 Personnel Cloaking */ readonly PersonnelCloaking: TechType;
    /** 11 Burrowing */ readonly Burrowing: TechType;
    /** 12 Infestation */ readonly Infestation: TechType;
    /** 13 Spawn Broodlings */ readonly SpawnBroodlings: TechType;
    /** 14 Dark Swarm */ readonly DarkSwarm: TechType;
    /** 15 Plague */ readonly Plague: TechType;
    /** 16 Consume */ readonly Consume: TechType;
    /** 17 Ensnare */ readonly Ensnare: TechType;
    /** 18 Parasite */ readonly Parasite: TechType;
    /** 19 Psionic Storm */ readonly PsionicStorm: TechType;
    /** 20 Hallucination */ readonly Hallucination: TechType;
    /** 21 Recall */ readonly Recall: TechType;
    /** 22 Stasis Field */ readonly StasisField: TechType;
    /** 23 Archon Warp */ readonly ArchonWarp: TechType;
    /** 24 Restoration */ readonly Restoration: TechType;
    /** 25 Disruption Web */ readonly DisruptionWeb: TechType;
    /** 26 Unused 26 */ readonly Unused26: TechType;
    /** 27 Mind Control */ readonly MindControl: TechType;
    /** 28 Dark Archon Meld */ readonly DarkArchonMeld: TechType;
    /** 29 Feedback */ readonly Feedback: TechType;
    /** 30 Optical Flare */ readonly OpticalFlare: TechType;
    /** 31 Maelstrom */ readonly Maelstrom: TechType;
    /** 32 Lurker Aspect */ readonly LurkerAspect: TechType;
    /** 33 Unused 33 */ readonly Unused33: TechType;
    /** 34 Healing */ readonly Healing: TechType;
    /** 35 Unused 35 */ readonly Unused35: TechType;
    /** 36 Unused 36 */ readonly Unused36: TechType;
    /** 37 Unused 37 */ readonly Unused37: TechType;
    /** 38 Unused 38 */ readonly Unused38: TechType;
    /** 39 Unused 39 */ readonly Unused39: TechType;
    /** 40 Unused 40 */ readonly Unused40: TechType;
    /** 41 Unused 41 */ readonly Unused41: TechType;
    /** 42 Unused 42 */ readonly Unused42: TechType;
    /** 43 Unused 43 */ readonly Unused43: TechType;
}

declare const TechType: TechTypeTable;

interface OrderTable extends ConstantTable<Order> {
    // GENERATED Order
    /** 0 Die */ readonly Die: Order;
    /** 1 Stop */ readonly Stop: Order;
    /** 2 Guard */ readonly Guard: Order;
    /** 3 PlayerGuard */ readonly PlayerGuard: Order;
    /** 4 TurretGuard */ readonly TurretGuard: Order;
    /** 5 BunkerGuard */ readonly BunkerGuard: Order;
    /** 6 Move */ readonly Move: Order;
    /** 7 ReaverStop */ readonly ReaverStop: Order;
    /** 8 AttackDefault */ readonly AttackDefault: Order;
    /** 9 MoveToAttack */ readonly MoveToAttack: Order;
    /** 10 AttackUnit */ readonly AttackUnit: Order;
    /** 11 AttackFixedRange */ readonly AttackFixedRange: Order;
    /** 12 AttackTile */ readonly AttackTile: Order;
    /** 13 Hover */ readonly Hover: Order;
    /** 14 AttackMove */ readonly AttackMove: Order;
    /** 15 InfestedCommandCenter */ readonly InfestedCommandCenter: Order;
    /** 16 UnusedNothing */ readonly UnusedNothing: Order;
    /** 17 UnusedPowerup */ readonly UnusedPowerup: Order;
    /** 18 TowerGuard */ readonly TowerGuard: Order;
    /** 19 TowerAttack */ readonly TowerAttack: Order;
    /** 20 SpiderMine */ readonly SpiderMine: Order;
    /** 21 StayInRange */ readonly StayInRange: Order;
    /** 22 TurretAttack */ readonly TurretAttack: Order;
    /** 23 Nothing */ readonly Nothing: Order;
    /** 24 NothingWait */ readonly NothingWait: Order;
    /** 25 DroneStartBuild */ readonly DroneStartBuild: Order;
    /** 26 DroneBuild */ readonly DroneBuild: Order;
    /** 27 CastInfestation */ readonly CastInfestation: Order;
    /** 28 MoveToInfest */ readonly MoveToInfest: Order;
    /** 29 InfestingCommandCenter */ readonly InfestingCommandCenter: Order;
    /** 30 PlaceBuilding */ readonly PlaceBuilding: Order;
    /** 31 PlaceProtossBuilding */ readonly PlaceProtossBuilding: Order;
    /** 32 CreateProtossBuilding */ readonly CreateProtossBuilding: Order;
    /** 33 ConstructingBuilding */ readonly ConstructingBuilding: Order;
    /** 34 Repair */ readonly Repair: Order;
    /** 35 MoveToRepair */ readonly MoveToRepair: Order;
    /** 36 PlaceAddon */ readonly PlaceAddon: Order;
    /** 37 BuildAddon */ readonly BuildAddon: Order;
    /** 38 Train */ readonly Train: Order;
    /** 39 RallyPointUnit */ readonly RallyPointUnit: Order;
    /** 40 RallyPointTile */ readonly RallyPointTile: Order;
    /** 41 ZergBirth */ readonly ZergBirth: Order;
    /** 42 ZergUnitMorph */ readonly ZergUnitMorph: Order;
    /** 43 ZergBuildingMorph */ readonly ZergBuildingMorph: Order;
    /** 44 IncompleteBuilding */ readonly IncompleteBuilding: Order;
    /** 45 IncompleteMorphing */ readonly IncompleteMorphing: Order;
    /** 46 BuildNydusExit */ readonly BuildNydusExit: Order;
    /** 47 EnterNydusCanal */ readonly EnterNydusCanal: Order;
    /** 48 IncompleteWarping */ readonly IncompleteWarping: Order;
    /** 49 Follow */ readonly Follow: Order;
    /** 50 Carrier */ readonly Carrier: Order;
    /** 51 ReaverCarrierMove */ readonly ReaverCarrierMove: Order;
    /** 52 CarrierStop */ readonly CarrierStop: Order;
    /** 53 CarrierAttack */ readonly CarrierAttack: Order;
    /** 54 CarrierMoveToAttack */ readonly CarrierMoveToAttack: Order;
    /** 55 CarrierIgnore2 */ readonly CarrierIgnore2: Order;
    /** 56 CarrierFight */ readonly CarrierFight: Order;
    /** 57 CarrierHoldPosition */ readonly CarrierHoldPosition: Order;
    /** 58 Reaver */ readonly Reaver: Order;
    /** 59 ReaverAttack */ readonly ReaverAttack: Order;
    /** 60 ReaverMoveToAttack */ readonly ReaverMoveToAttack: Order;
    /** 61 ReaverFight */ readonly ReaverFight: Order;
    /** 62 ReaverHoldPosition */ readonly ReaverHoldPosition: Order;
    /** 63 TrainFighter */ readonly TrainFighter: Order;
    /** 64 InterceptorAttack */ readonly InterceptorAttack: Order;
    /** 65 ScarabAttack */ readonly ScarabAttack: Order;
    /** 66 RechargeShieldsUnit */ readonly RechargeShieldsUnit: Order;
    /** 67 RechargeShieldsBattery */ readonly RechargeShieldsBattery: Order;
    /** 68 ShieldBattery */ readonly ShieldBattery: Order;
    /** 69 InterceptorReturn */ readonly InterceptorReturn: Order;
    /** 70 DroneLand */ readonly DroneLand: Order;
    /** 71 BuildingLand */ readonly BuildingLand: Order;
    /** 72 BuildingLiftoff */ readonly BuildingLiftoff: Order;
    /** 73 DroneLiftOff */ readonly DroneLiftOff: Order;
    /** 74 LiftingOff */ readonly LiftingOff: Order;
    /** 75 ResearchTech */ readonly ResearchTech: Order;
    /** 76 Upgrade */ readonly Upgrade: Order;
    /** 77 Larva */ readonly Larva: Order;
    /** 78 SpawningLarva */ readonly SpawningLarva: Order;
    /** 79 Harvest1 */ readonly Harvest1: Order;
    /** 80 Harvest2 */ readonly Harvest2: Order;
    /** 81 MoveToGas */ readonly MoveToGas: Order;
    /** 82 WaitForGas */ readonly WaitForGas: Order;
    /** 83 HarvestGas */ readonly HarvestGas: Order;
    /** 84 ReturnGas */ readonly ReturnGas: Order;
    /** 85 MoveToMinerals */ readonly MoveToMinerals: Order;
    /** 86 WaitForMinerals */ readonly WaitForMinerals: Order;
    /** 87 MiningMinerals */ readonly MiningMinerals: Order;
    /** 88 GatheringInterrupted */ readonly GatheringInterrupted: Order;
    /** 89 GatherWaitInterrupted */ readonly GatherWaitInterrupted: Order;
    /** 90 ReturnMinerals */ readonly ReturnMinerals: Order;
    /** 91 RechargeShieldsUnitRemoveOverlay */ readonly RechargeShieldsUnitRemoveOverlay: Order;
    /** 92 EnterTransport */ readonly EnterTransport: Order;
    /** 93 PickupIdle */ readonly PickupIdle: Order;
    /** 94 PickupTransport */ readonly PickupTransport: Order;
    /** 95 PickupBunker */ readonly PickupBunker: Order;
    /** 96 Pickup4 */ readonly Pickup4: Order;
    /** 97 PowerupIdle */ readonly PowerupIdle: Order;
    /** 98 Sieging */ readonly Sieging: Order;
    /** 99 Unsieging */ readonly Unsieging: Order;
    /** 100 WatchTarget */ readonly WatchTarget: Order;
    /** 101 InitCreepGrowth */ readonly InitCreepGrowth: Order;
    /** 102 SpreadCreep */ readonly SpreadCreep: Order;
    /** 103 StoppingCreepGrowth */ readonly StoppingCreepGrowth: Order;
    /** 104 GuardianAspect */ readonly GuardianAspect: Order;
    /** 105 ArchonWarp */ readonly ArchonWarp: Order;
    /** 106 CompletingArchonSummon */ readonly CompletingArchonSummon: Order;
    /** 107 HoldPosition */ readonly HoldPosition: Order;
    /** 108 QueenHoldPosition */ readonly QueenHoldPosition: Order;
    /** 109 Cloak */ readonly Cloak: Order;
    /** 110 Decloak */ readonly Decloak: Order;
    /** 111 Unload */ readonly Unload: Order;
    /** 112 MoveUnload */ readonly MoveUnload: Order;
    /** 113 FireYamatoGun */ readonly FireYamatoGun: Order;
    /** 114 MoveToFireYamatoGun */ readonly MoveToFireYamatoGun: Order;
    /** 115 CastLockdown */ readonly CastLockdown: Order;
    /** 116 Burrowing */ readonly Burrowing: Order;
    /** 117 Burrowed */ readonly Burrowed: Order;
    /** 118 Unburrowing */ readonly Unburrowing: Order;
    /** 119 CastDarkSwarm */ readonly CastDarkSwarm: Order;
    /** 120 CastParasite */ readonly CastParasite: Order;
    /** 121 CastSpawnBroodlings */ readonly CastSpawnBroodlings: Order;
    /** 122 CastEMPShockwave */ readonly CastEMPShockwave: Order;
    /** 123 NukeWait */ readonly NukeWait: Order;
    /** 124 NukeTrain */ readonly NukeTrain: Order;
    /** 125 NukeLaunch */ readonly NukeLaunch: Order;
    /** 126 NukePaint */ readonly NukePaint: Order;
    /** 127 NukeUnit */ readonly NukeUnit: Order;
    /** 128 CastNuclearStrike */ readonly CastNuclearStrike: Order;
    /** 129 NukeTrack */ readonly NukeTrack: Order;
    /** 130 InitializeArbiter */ readonly InitializeArbiter: Order;
    /** 131 CloakNearbyUnits */ readonly CloakNearbyUnits: Order;
    /** 132 PlaceMine */ readonly PlaceMine: Order;
    /** 133 RightClickAction */ readonly RightClickAction: Order;
    /** 134 SuicideUnit */ readonly SuicideUnit: Order;
    /** 135 SuicideLocation */ readonly SuicideLocation: Order;
    /** 136 SuicideHoldPosition */ readonly SuicideHoldPosition: Order;
    /** 137 CastRecall */ readonly CastRecall: Order;
    /** 138 Teleport */ readonly Teleport: Order;
    /** 139 CastScannerSweep */ readonly CastScannerSweep: Order;
    /** 140 Scanner */ readonly Scanner: Order;
    /** 141 CastDefensiveMatrix */ readonly CastDefensiveMatrix: Order;
    /** 142 CastPsionicStorm */ readonly CastPsionicStorm: Order;
    /** 143 CastIrradiate */ readonly CastIrradiate: Order;
    /** 144 CastPlague */ readonly CastPlague: Order;
    /** 145 CastConsume */ readonly CastConsume: Order;
    /** 146 CastEnsnare */ readonly CastEnsnare: Order;
    /** 147 CastStasisField */ readonly CastStasisField: Order;
    /** 148 CastHallucination */ readonly CastHallucination: Order;
    /** 149 Hallucination2 */ readonly Hallucination2: Order;
    /** 150 ResetCollision */ readonly ResetCollision: Order;
    /** 151 ResetHarvestCollision */ readonly ResetHarvestCollision: Order;
    /** 152 Patrol */ readonly Patrol: Order;
    /** 153 CTFCOPInit */ readonly CTFCOPInit: Order;
    /** 154 CTFCOPStarted */ readonly CTFCOPStarted: Order;
    /** 155 CTFCOP2 */ readonly CTFCOP2: Order;
    /** 156 ComputerAI */ readonly ComputerAI: Order;
    /** 157 AtkMoveEP */ readonly AtkMoveEP: Order;
    /** 158 HarassMove */ readonly HarassMove: Order;
    /** 159 AIPatrol */ readonly AIPatrol: Order;
    /** 160 GuardPost */ readonly GuardPost: Order;
    /** 161 RescuePassive */ readonly RescuePassive: Order;
    /** 162 Neutral */ readonly Neutral: Order;
    /** 163 ComputerReturn */ readonly ComputerReturn: Order;
    /** 164 InitializePsiProvider */ readonly InitializePsiProvider: Order;
    /** 165 SelfDestructing */ readonly SelfDestructing: Order;
    /** 166 Critter */ readonly Critter: Order;
    /** 167 HiddenGun */ readonly HiddenGun: Order;
    /** 168 OpenDoor */ readonly OpenDoor: Order;
    /** 169 CloseDoor */ readonly CloseDoor: Order;
    /** 170 HideTrap */ readonly HideTrap: Order;
    /** 171 RevealTrap */ readonly RevealTrap: Order;
    /** 172 EnableDoodad */ readonly EnableDoodad: Order;
    /** 173 DisableDoodad */ readonly DisableDoodad: Order;
    /** 174 WarpIn */ readonly WarpIn: Order;
    /** 175 MedicIdle */ readonly MedicIdle: Order;
    /** 176 MedicHeal */ readonly MedicHeal: Order;
    /** 177 HealMove */ readonly HealMove: Order;
    /** 178 MedicHoldPosition */ readonly MedicHoldPosition: Order;
    /** 179 MedicHealToIdle */ readonly MedicHealToIdle: Order;
    /** 180 CastRestoration */ readonly CastRestoration: Order;
    /** 181 CastDisruptionWeb */ readonly CastDisruptionWeb: Order;
    /** 182 CastMindControl */ readonly CastMindControl: Order;
    /** 183 DarkArchonMeld */ readonly DarkArchonMeld: Order;
    /** 184 CastFeedback */ readonly CastFeedback: Order;
    /** 185 CastOpticalFlare */ readonly CastOpticalFlare: Order;
    /** 186 CastMaelstrom */ readonly CastMaelstrom: Order;
    /** 187 JunkYardDog */ readonly JunkYardDog: Order;
    /** 188 Fatal */ readonly Fatal: Order;
}

declare const Order: OrderTable;
