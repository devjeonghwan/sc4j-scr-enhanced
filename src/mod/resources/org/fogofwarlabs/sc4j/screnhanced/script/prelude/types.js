// The type and value tables of the upper layer: the globals UnitType, WeaponType, FlingyType, SpriteType, ImageType, UpgradeType, TechType,
// Order, Key, MouseButton, Modifier, UnitGroup, ScoreType, ResourceType, Alliance, EventKind, InputKind and DecisionKind. The DAT methods of
// the type objects come from the DAT tables (prelude.datTables) over core.dat, with its raw values.
(function (core, prelude) {
    'use strict';

    var NONE = prelude.NONE;
    var RAW_SCALE = prelude.RAW_SCALE;
    var createObject = prelude.createObject;
    var isObjectOf = prelude.isObjectOf;
    var requireObject = prelude.requireObject;
    var toIdentifier = prelude.toIdentifier;
    var typePrototype = prelude.typePrototype;
    var createTypeTable = prelude.createTypeTable;
    var createConstantTable = prelude.createConstantTable;

    var create = Object.create;
    var freeze = Object.freeze;

    // The unit operand of the first unit group, after the unit types and their none.
    var FIRST_UNIT_GROUP = 229;
    var UNIT_GROUP_NAMES = ['Any unit', 'Men', 'Buildings', 'Factories'];
    // The keys by Windows virtual-key code: the named codes are constants of Key, the others only Key.get(code), named "Key " and the code.
    var KEY_COUNT = 256;
    var NAMED_KEYS = [
        [0x08, 'Backspace'], [0x09, 'Tab'], [0x0D, 'Enter'], [0x10, 'Shift'], [0x11, 'Control'], [0x12, 'Alt'], [0x13, 'Pause'],
        [0x14, 'CapsLock'], [0x1B, 'Escape'], [0x20, 'Space'], [0x21, 'PageUp'], [0x22, 'PageDown'], [0x23, 'End'], [0x24, 'Home'],
        [0x25, 'Left'], [0x26, 'Up'], [0x27, 'Right'], [0x28, 'Down'], [0x2D, 'Insert'], [0x2E, 'Delete'], [0x6A, 'NumpadMultiply'],
        [0x6B, 'NumpadAdd'], [0x6D, 'NumpadSubtract'], [0x6E, 'NumpadDecimal'], [0x6F, 'NumpadDivide'], [0xBA, 'Semicolon'], [0xBB, 'Equal'],
        [0xBC, 'Comma'], [0xBD, 'Minus'], [0xBE, 'Period'], [0xBF, 'Slash'], [0xC0, 'Backquote'], [0xDB, 'BracketLeft'], [0xDC, 'Backslash'],
        [0xDD, 'BracketRight'], [0xDE, 'Quote']
    ];
    var LETTER_A = 0x41;
    var LETTER_COUNT = 26;
    var DIGIT_0 = 0x30;
    var NUMPAD_0 = 0x60;
    var DIGIT_COUNT = 10;
    var F1 = 0x70;
    var FUNCTION_KEY_COUNT = 12;
    var MOUSE_BUTTON_NAMES = ['Left', 'Right', 'Middle'];
    var SCORE_TYPE_NAMES = ['Total', 'Units', 'Buildings', 'UnitsAndBuildings', 'Kills', 'Razings', 'KillsAndRazings', 'Custom'];
    // Supply names only the supply icon of the console (display.console.setResourceIcon); the resource functions refuse it.
    var RESOURCE_TYPE_NAMES = ['Minerals', 'Gas', 'MineralsAndGas', 'Supply'];
    var ALLIANCE_NAMES = ['Enemy', 'Ally', 'AlliedVictory'];
    // The kinds of the records of core.local.getInputEvents and of the decide entry point, in the ids the runtime gives them.
    var INPUT_KIND_NAMES = ['KeyDown', 'KeyUp', 'MouseDown', 'MouseUp', 'Chat'];
    var DECISION_KIND_NAMES = ['Damage', 'Order', 'TargetAcquired'];

    var unitTypePrototype = create(typePrototype);
    var weaponTypePrototype = create(typePrototype);
    var flingyTypePrototype = create(typePrototype);
    var spriteTypePrototype = create(typePrototype);
    var imageTypePrototype = create(typePrototype);
    var upgradeTypePrototype = create(typePrototype);
    var techTypePrototype = create(typePrototype);
    var orderPrototype = create(typePrototype);

    // The image type whose HD anim this image type draws (images.rel), this image type for one with its own anim.
    imageTypePrototype.getAnimImageType = function () {
        return ImageType.get(core.imageType.getAnimImageType(this.id));
    };

    // The shown maximum hit points and shields of a unit type: units.dat stores hit points in 1/256 and shields as shown.
    unitTypePrototype.getMaxHp = function () {
        return this.getHitPoints() / RAW_SCALE;
    };
    unitTypePrototype.setMaxHp = function (hp) {
        this.setHitPoints(Math.round(Number(hp) * RAW_SCALE));
    };
    unitTypePrototype.getMaxShields = function () {
        return this.getShieldAmount();
    };
    unitTypePrototype.setMaxShields = function (shields) {
        this.setShieldAmount(shields);
    };
    // The name of a unit type in its stat_txt string, which tooltips, the selection and messages show; null shows the game's name again.
    unitTypePrototype.setName = function (text) {
        core.strings.setStatText(this.id + 1, text === null || text === undefined ? null : String(text));
    };

    var UnitType = createTypeTable(core.unitType, unitTypePrototype);
    var WeaponType = createTypeTable(core.weaponType, weaponTypePrototype);
    var FlingyType = createTypeTable(core.flingyType, flingyTypePrototype);
    var SpriteType = createTypeTable(core.spriteType, spriteTypePrototype);
    var ImageType = createTypeTable(core.imageType, imageTypePrototype);
    var UpgradeType = createTypeTable(core.upgradeType, upgradeTypePrototype);
    var TechType = createTypeTable(core.techType, techTypePrototype);
    var Order = createTypeTable(core.orderType, orderPrototype);

    // The type tables and the prototypes of their objects by the type names of the DAT tables.
    var datTypeTables = {
        UnitType: UnitType, WeaponType: WeaponType, FlingyType: FlingyType, SpriteType: SpriteType, ImageType: ImageType,
        UpgradeType: UpgradeType, TechType: TechType, Order: Order
    };
    var datTypePrototypes = {
        UnitType: unitTypePrototype, WeaponType: weaponTypePrototype, FlingyType: flingyTypePrototype, SpriteType: spriteTypePrototype,
        ImageType: imageTypePrototype, UpgradeType: upgradeTypePrototype, TechType: techTypePrototype, Order: orderPrototype
    };

    // Defines the methods of a DAT column on the type objects: get<Column>() and, for a writable column, set<Column>(value) with the raw
    // value, or the type object a reference names (null for its none id).
    function defineDatColumn(prototype, namespace, column) {
        var read = namespace['get' + column.name];
        var write = column.writable ? namespace['set' + column.name] : null;

        if (column.reference === null) {
            prototype['get' + column.name] = function () {
                return read(this.id);
            };

            if (write !== null) {
                prototype['set' + column.name] = function (value) {
                    write(this.id, value);
                };
            }

            return;
        }

        var table = datTypeTables[column.reference];
        var referencePrototype = datTypePrototypes[column.reference];

        prototype['get' + column.name] = function () {
            return table.get(read(this.id));
        };

        if (write !== null) {
            prototype['set' + column.name] = function (type) {
                write(this.id, type === null && column.none !== NONE ? column.none : requireObject(type, referencePrototype, column.reference).id);
            };
        }
    }

    // Defines is<Flag>() and, for a writable column, set<Flag>(set) for each named bit of a DAT flag column.
    function defineDatFlag(prototype, namespace, column, flag) {
        var read = namespace['get' + column.name];
        var write = column.writable ? namespace['set' + column.name] : null;

        prototype['is' + flag.name] = function () {
            return (read(this.id) & flag.mask) !== 0;
        };

        if (write !== null) {
            prototype['set' + flag.name] = function (set) {
                if (typeof set !== 'boolean') {
                    throw new TypeError('A boolean is needed, not ' + set + '.');
                }

                var value = read(this.id);

                write(this.id, (set ? value | flag.mask : value & ~flag.mask) >>> 0);
            };
        }
    }

    function defineDatMethods(tables) {
        for (var tableIndex = 0; tableIndex < tables.length; tableIndex++) {
            var table = tables[tableIndex];
            var namespace = core.dat[table.name];
            var prototype = datTypePrototypes[table.type];

            for (var columnIndex = 0; columnIndex < table.columns.length; columnIndex++) {
                var column = table.columns[columnIndex];

                if (column.flags === null) {
                    defineDatColumn(prototype, namespace, column);
                } else {
                    for (var flagIndex = 0; flagIndex < column.flags.length; flagIndex++) {
                        defineDatFlag(prototype, namespace, column, column.flags[flagIndex]);
                    }
                }
            }
        }
    }

    defineDatMethods(prelude.datTables);

    function createKeyNames() {
        var names = [];

        NAMED_KEYS.forEach(function (entry) {
            names[entry[0]] = entry[1];
        });

        for (var letter = 0; letter < LETTER_COUNT; letter++) {
            names[LETTER_A + letter] = String.fromCharCode(LETTER_A + letter);
        }

        for (var digit = 0; digit < DIGIT_COUNT; digit++) {
            names[DIGIT_0 + digit] = 'Digit' + digit;
            names[NUMPAD_0 + digit] = 'Numpad' + digit;
        }

        for (var number = 1; number <= FUNCTION_KEY_COUNT; number++) {
            names[F1 + number - 1] = 'F' + number;
        }

        return names;
    }

    var keyPrototype = create(typePrototype);
    var mouseButtonPrototype = create(typePrototype);
    var Key = createConstantTable(keyPrototype, createKeyNames(), KEY_COUNT, 'Key ');
    var MouseButton = createConstantTable(mouseButtonPrototype, MOUSE_BUTTON_NAMES, MOUSE_BUTTON_NAMES.length, 'Button ');
    // The bits of the modifier mask of a local key or mouse button event.
    var Modifier = freeze({Shift: 1, Control: 2, Alt: 4});

    // The operands of the trigger counters: the unit groups after the unit types, the score types, the resource types and the alliances, with
    // the names of the original trigger editor.
    var unitGroupPrototype = create(typePrototype);
    var scoreTypePrototype = create(typePrototype);
    var resourceTypePrototype = create(typePrototype);
    var alliancePrototype = create(typePrototype);

    function createUnitGroups() {
        var table = {};

        UNIT_GROUP_NAMES.forEach(function (name, index) {
            table[toIdentifier(name)] = freeze(createObject(unitGroupPrototype, {id: FIRST_UNIT_GROUP + index, name: name}));
        });

        return freeze(table);
    }

    var UnitGroup = createUnitGroups();
    var ScoreType = createConstantTable(scoreTypePrototype, SCORE_TYPE_NAMES, SCORE_TYPE_NAMES.length, 'Score ');
    var ResourceType = createConstantTable(resourceTypePrototype, RESOURCE_TYPE_NAMES, RESOURCE_TYPE_NAMES.length, 'Resource ');
    var Alliance = createConstantTable(alliancePrototype, ALLIANCE_NAMES, ALLIANCE_NAMES.length, 'Alliance ');

    // The kinds of the lower records: the events of core.events.getFrameEvents by their names in core.events, the local inputs and the
    // decisions.
    function createEventKindNames() {
        var names = [];

        for (var id = 0; id < core.events.getKindCount(); id++) {
            names.push(core.events.getKindName(id));
        }

        return names;
    }

    var eventKindPrototype = create(typePrototype);
    var inputKindPrototype = create(typePrototype);
    var decisionKindPrototype = create(typePrototype);
    var eventKindNames = createEventKindNames();
    var EventKind = createConstantTable(eventKindPrototype, eventKindNames, eventKindNames.length, 'Event ');
    var InputKind = createConstantTable(inputKindPrototype, INPUT_KIND_NAMES, INPUT_KIND_NAMES.length, 'Input ');
    var DecisionKind = createConstantTable(decisionKindPrototype, DECISION_KIND_NAMES, DECISION_KIND_NAMES.length, 'Decision ');

    // Returns the unit operand of a UnitType or a UnitGroup.
    function readUnitOperand(unit) {
        if (isObjectOf(unit, unitTypePrototype) || isObjectOf(unit, unitGroupPrototype)) {
            return unit.id;
        }

        throw new TypeError('A unit type or a unit group is needed, not ' + unit + '.');
    }

    [unitTypePrototype, weaponTypePrototype, flingyTypePrototype, spriteTypePrototype, imageTypePrototype, upgradeTypePrototype,
     techTypePrototype, orderPrototype, keyPrototype, mouseButtonPrototype, unitGroupPrototype, scoreTypePrototype, resourceTypePrototype,
     alliancePrototype, eventKindPrototype, inputKindPrototype, decisionKindPrototype].forEach(freeze);

    return {
        exports: {
            UnitType: UnitType,
            WeaponType: WeaponType,
            FlingyType: FlingyType,
            SpriteType: SpriteType,
            ImageType: ImageType,
            UpgradeType: UpgradeType,
            TechType: TechType,
            Order: Order,
            Key: Key,
            MouseButton: MouseButton,
            Modifier: Modifier,
            UnitGroup: UnitGroup,
            ScoreType: ScoreType,
            ResourceType: ResourceType,
            Alliance: Alliance,
            EventKind: EventKind,
            InputKind: InputKind,
            DecisionKind: DecisionKind
        },
        shared: {
            unitTypePrototype: unitTypePrototype,
            imageTypePrototype: imageTypePrototype,
            upgradeTypePrototype: upgradeTypePrototype,
            techTypePrototype: techTypePrototype,
            orderPrototype: orderPrototype,
            scoreTypePrototype: scoreTypePrototype,
            resourceTypePrototype: resourceTypePrototype,
            alliancePrototype: alliancePrototype,
            readUnitOperand: readUnitOperand
        }
    };
});
