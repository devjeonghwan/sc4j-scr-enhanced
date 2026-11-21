// The upper layer of the SC:R Enhanced Script game API over the lower layer core: the globals game, Time, Position, UnitType, UnitGroup,
// WeaponType, FlingyType, SpriteType, ImageType, UpgradeType, TechType, Order, ScoreType, ResourceType, Alliance, Key, MouseButton, Modifier,
// resources, graphics, tileset, terrain, sync, local, Color, Font, Align, Graphics, ConsoleControl, ConsoleImage, console, strings and
// messages, and the Unit, Player, Location, Tile, Channel, PlayerValue, Image and console control objects they return, and the screen object
// of onRender. One unit,
// player, location or tile is always the same object; a unit object stays with its unit, and exists() becomes false once the unit is gone.
// Hit points, shields and energy are the shown values (core's raw values divided by 256; no shields or energy for a unit without them), supply
// the shown supply (core's half units divided by 2). The DAT methods of the type objects come from the DAT tables the game passes
// (datTables) over core.dat, with its raw values. The event and decision objects of the hooks have getters for their captured values; a
// decision object also has the methods that decide. The prelude returns the functions that make these objects to the game.
(function (global, core, datTables) {
    'use strict';

    var RAW_SCALE = 256;
    var SUPPLY_SCALE = 2;
    var NONE = -1;
    var TILE_SIZE = 32;
    // The frames of one second of the trigger clock.
    var FRAMES_PER_CLOCK_SECOND = 16;
    var SECONDS_PER_MINUTE = 60;
    var MAXIMUM_RAW_AMOUNT = 2147483647;
    // The results of core.player.endGame and of the result events: the victory states.
    var RESULT_DROPPED = 1;
    var RESULT_DEFEAT = 2;
    var RESULT_VICTORY = 3;
    var RESULT_SECURED = 4;
    var RESULT_DRAW = 5;
    // The leaderboard types of core.leaderboard: the action ids of the leaderboard actions and of their goal forms.
    var LEADERBOARD_CONTROL = 17;
    var LEADERBOARD_CONTROL_AT_LOCATION = 18;
    var LEADERBOARD_RESOURCES = 19;
    var LEADERBOARD_KILLS = 20;
    var LEADERBOARD_POINTS = 21;
    var LEADERBOARD_GOAL_CONTROL = 33;
    var LEADERBOARD_GOAL_CONTROL_AT_LOCATION = 34;
    var LEADERBOARD_GOAL_RESOURCES = 35;
    var LEADERBOARD_GOAL_KILLS = 36;
    var LEADERBOARD_GOAL_POINTS = 37;
    // The unit operand of the first unit group, after the unit types and their none.
    var FIRST_UNIT_GROUP = 229;
    var UNIT_GROUP_NAMES = ['Any unit', 'Men', 'Buildings', 'Factories'];
    // An AI script id: four characters, the first one in the low byte.
    var AI_SCRIPT_LENGTH = 4;
    var BITS_PER_CHARACTER = 8;
    var LAST_SCRIPT_CHARACTER = 0xFF;

    var round = Math.round;
    var floor = Math.floor;
    var ceil = Math.ceil;
    var min = Math.min;
    var max = Math.max;
    var create = Object.create;
    var freeze = Object.freeze;
    var defineProperty = Object.defineProperty;
    var getPrototypeOf = Object.getPrototypeOf;

    function defineGlobal(name, value) {
        defineProperty(global, name, {value: value, writable: false, enumerable: false, configurable: false});
    }

    // Creates an object of a prototype with hidden, read-only fields.
    function createObject(prototype, fields) {
        var object = create(prototype);

        for (var key in fields) {
            defineProperty(object, key, {value: fields[key]});
        }

        return object;
    }

    function isObjectOf(value, prototype) {
        return value !== null && typeof value === 'object' && getPrototypeOf(value) === prototype;
    }

    function requireObject(value, prototype, kind) {
        if (!isObjectOf(value, prototype)) {
            throw new TypeError(kind + ' is needed, not ' + value + '.');
        }

        return value;
    }

    // Converts an original data name to an identifier: "Hero Jim Raynor Marine" becomes "HeroJimRaynorMarine".
    function toIdentifier(name) {
        var words = name.split(' ');
        var identifier = '';

        for (var index = 0; index < words.length; index++) {
            var word = words[index];

            if (word.length > 0) {
                identifier += word.charAt(0).toUpperCase() + word.substring(1);
            }
        }

        return identifier;
    }

    // Creates the constants of a type namespace of core: one object per id under its identifier, get(id) and getCount().
    function createTypeTable(namespace, prototype) {
        var count = namespace.getCount();
        var types = [];
        var table = {};

        for (var id = 0; id < count; id++) {
            var type = freeze(createObject(prototype, {id: id, name: namespace.getName(id)}));

            types.push(type);
            table[toIdentifier(type.name)] = type;
        }

        defineProperty(table, 'get', {
            value: function (id) {
                var type = types[id];

                return type === undefined ? null : type;
            }
        });
        defineProperty(table, 'getCount', {
            value: function () {
                return count;
            }
        });

        return freeze(table);
    }

    var typePrototype = {
        getId: function () {
            return this.id;
        },
        getName: function () {
            return this.name;
        },
        toString: function () {
            return this.name;
        }
    };

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

    defineDatMethods(datTables);

    var positionPrototype = {
        getX: function () {
            return this.x;
        },
        getY: function () {
            return this.y;
        },
        toString: function () {
            return '(' + this.x + ', ' + this.y + ')';
        }
    };

    function createPosition(x, y) {
        return freeze(createObject(positionPrototype, {x: x, y: y}));
    }

    var Position = freeze({
        of: function (x, y) {
            return createPosition(x, y);
        }
    });

    // The frames of a duration, rounded to the nearest frame (halves up). Seconds and minutes are those of the trigger clock, which the
    // countdown timer and the elapsed time count: sixteen frames a second. Milliseconds are real time at the fastest speed: the frames of the
    // game's frame duration.
    var Time = freeze({
        fromSeconds: function (seconds) {
            return round(seconds * FRAMES_PER_CLOCK_SECOND);
        },
        fromMinutes: function (minutes) {
            return round(minutes * SECONDS_PER_MINUTE * FRAMES_PER_CLOCK_SECOND);
        },
        fromMilliseconds: function (milliseconds) {
            return round(milliseconds / core.game.getFrameDuration());
        }
    });

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

    // Creates the constants of ids from 0 to count - 1: one object per id, under its name when it has one, get(id) and getCount().
    function createConstantTable(prototype, names, count, unnamedPrefix) {
        var constants = [];
        var table = {};

        for (var id = 0; id < count; id++) {
            var name = names[id];
            var constant = freeze(createObject(prototype, {id: id, name: name === undefined ? unnamedPrefix + id : name}));

            constants.push(constant);

            if (name !== undefined) {
                table[name] = constant;
            }
        }

        defineProperty(table, 'get', {
            value: function (id) {
                var constant = constants[id];

                return constant === undefined ? null : constant;
            }
        });
        defineProperty(table, 'getCount', {
            value: function () {
                return count;
            }
        });

        return freeze(table);
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
    var SCORE_TYPE_NAMES = ['Total', 'Units', 'Buildings', 'UnitsAndBuildings', 'Kills', 'Razings', 'KillsAndRazings', 'Custom'];
    // Supply names only the supply icon of the console (console.setResourceIcon); the resource functions refuse it.
    var RESOURCE_TYPE_NAMES = ['Minerals', 'Gas', 'MineralsAndGas', 'Supply'];
    var ALLIANCE_NAMES = ['Enemy', 'Ally', 'AlliedVictory'];
    var ScoreType = createConstantTable(scoreTypePrototype, SCORE_TYPE_NAMES, SCORE_TYPE_NAMES.length, 'Score ');
    var ResourceType = createConstantTable(resourceTypePrototype, RESOURCE_TYPE_NAMES, RESOURCE_TYPE_NAMES.length, 'Resource ');
    var Alliance = createConstantTable(alliancePrototype, ALLIANCE_NAMES, ALLIANCE_NAMES.length, 'Alliance ');

    // Returns the unit operand of a UnitType or a UnitGroup.
    function readUnitOperand(unit) {
        if (isObjectOf(unit, unitTypePrototype) || isObjectOf(unit, unitGroupPrototype)) {
            return unit.id;
        }

        throw new TypeError('A unit type or a unit group is needed, not ' + unit + '.');
    }

    // Returns an optional options object, empty when it is missing.
    function readOptions(options) {
        if (options === undefined) {
            return {};
        }

        if (options === null || typeof options !== 'object') {
            throw new TypeError('The options are an object, not ' + options + '.');
        }

        return options;
    }

    // Returns the location id of the location option, NONE without one.
    function readLocationOption(options) {
        var location = options.location;

        return location === undefined || location === null ? NONE : requireObject(location, locationPrototype, 'A location').id;
    }

    // Returns the id of an AI script name: four characters up to 0xFF, the first one in the low byte ('EnBk', '+Vi0').
    function readAiScript(name) {
        var text = String(name);
        var script = 0;

        if (text.length !== AI_SCRIPT_LENGTH) {
            throw new RangeError('An AI script name has four characters, not ' + text + '.');
        }

        for (var index = 0; index < AI_SCRIPT_LENGTH; index++) {
            var code = text.charCodeAt(index);

            if (code > LAST_SCRIPT_CHARACTER) {
                throw new RangeError('The AI script name ' + text + ' has a character above 0xFF.');
            }

            script |= code << index * BITS_PER_CHARACTER;
        }

        return script;
    }

    // Kills or removes a player's units of a UnitType or UnitGroup: {location: a Location to take only the units there, count: the most units
    // to take, all without one}.
    function removePlayerUnits(player, unit, options, remove) {
        var operand = readUnitOperand(unit);
        var values = readOptions(options);
        var location = readLocationOption(values);
        var count = values.count === undefined ? 0 : values.count;

        if (values.count !== undefined && !(count >= 1)) {
            throw new RangeError('The unit count ' + count + ' is not positive.');
        }

        if (remove) {
            core.player.removeUnits(player.id, operand, location, count);
        } else {
            core.player.killUnits(player.id, operand, location, count);
        }
    }

    // Shows a leaderboard without a location: the goal form with the goal option.
    function showLeaderboard(type, goalType, operand, label, options) {
        var goal = readOptions(options).goal;

        if (goal === undefined) {
            core.leaderboard.show(type, operand, 0, String(label));
        } else {
            core.leaderboard.show(goalType, operand, goal, String(label));
        }
    }

    var playerCount = core.player.getCount();
    var players = [];
    var units = [];
    var locations = [];

    function getPlayer(id) {
        if (!(id >= 0 && id < playerCount)) {
            return null;
        }

        var player = players[id];

        if (player === undefined) {
            player = createObject(playerPrototype, {id: id});
            players[id] = player;
        }

        return player;
    }

    // Returns the object of the unit in a slot: the cached one while the slot holds the same unit.
    function getUnit(id) {
        if (id === NONE || !core.unit.exists(id)) {
            return null;
        }

        var generation = core.unit.getGeneration(id);
        var unit = units[id];

        if (unit === undefined || unit.generation !== generation) {
            unit = createObject(unitPrototype, {id: id, generation: generation});
            units[id] = unit;
        }

        return unit;
    }

    function getLocation(id) {
        if (!(id >= 0 && id < core.location.getCount())) {
            return null;
        }

        var location = locations[id];

        if (location === undefined) {
            location = createObject(locationPrototype, {id: id});
            locations[id] = location;
        }

        return location;
    }

    // Returns the objects of the units of ids.
    function toUnits(ids) {
        return ids.map(function (id) {
            return getUnit(id);
        });
    }

    function requireUnitId(unit) {
        if (!requireObject(unit, unitPrototype, 'A unit').exists()) {
            throw new Error('The unit ' + unit.id + ' does not exist.');
        }

        return unit.id;
    }

    // Returns the units that pass a filter: a function of the unit, or an object with an optional type (a UnitType) and owner (a Player).
    function filterUnits(filter, limit) {
        var test;

        if (typeof filter === 'function') {
            test = filter;
        } else {
            var type = filter === undefined || filter.type === undefined ? null : requireObject(filter.type, unitTypePrototype, 'A unit type');
            var owner = filter === undefined || filter.owner === undefined ? null : requireObject(filter.owner, playerPrototype, 'A player');

            test = function (unit) {
                return (type === null || core.unit.getType(unit.id) === type.id) && (owner === null || core.unit.getOwner(unit.id) === owner.id);
            };
        }

        var ids = core.unit.getIds();
        var found = [];

        for (var index = 0; index < ids.length && found.length < limit; index++) {
            var unit = getUnit(ids[index]);

            if (test(unit)) {
                found.push(unit);
            }
        }

        return found;
    }

    var unitPrototype = {
        getId: function () {
            return this.id;
        },
        exists: function () {
            return core.unit.exists(this.id) && core.unit.getGeneration(this.id) === this.generation;
        },
        getType: function () {
            return UnitType.get(core.unit.getType(requireUnitId(this)));
        },
        getOwner: function () {
            return getPlayer(core.unit.getOwner(requireUnitId(this)));
        },
        setOwner: function (player) {
            return core.unit.give(requireUnitId(this), requireObject(player, playerPrototype, 'A player').id);
        },
        getX: function () {
            return core.unit.getX(requireUnitId(this));
        },
        getY: function () {
            return core.unit.getY(requireUnitId(this));
        },
        getPosition: function () {
            var id = requireUnitId(this);

            return createPosition(core.unit.getX(id), core.unit.getY(id));
        },
        setPosition: function (position) {
            var id = requireUnitId(this);

            requireObject(position, positionPrototype, 'A position');

            return core.unit.move(id, position.x, position.y);
        },
        getHp: function () {
            return core.unit.getHp(requireUnitId(this)) / RAW_SCALE;
        },
        setHp: function (hp) {
            core.unit.setHp(requireUnitId(this), round(hp * RAW_SCALE));
        },
        getMaxHp: function () {
            return core.unit.getMaxHp(requireUnitId(this)) / RAW_SCALE;
        },
        getShields: function () {
            var id = requireUnitId(this);

            return core.unit.getMaxShields(id) === 0 ? 0 : core.unit.getShields(id) / RAW_SCALE;
        },
        setShields: function (shields) {
            return core.unit.setShields(requireUnitId(this), round(shields * RAW_SCALE));
        },
        getMaxShields: function () {
            return core.unit.getMaxShields(requireUnitId(this)) / RAW_SCALE;
        },
        getEnergy: function () {
            var id = requireUnitId(this);

            return core.unit.getMaxEnergy(id) === 0 ? 0 : core.unit.getEnergy(id) / RAW_SCALE;
        },
        setEnergy: function (energy) {
            return core.unit.setEnergy(requireUnitId(this), round(energy * RAW_SCALE));
        },
        getMaxEnergy: function () {
            return core.unit.getMaxEnergy(requireUnitId(this)) / RAW_SCALE;
        },
        // The percent writes take a percentage of the maximum, as the trigger actions do.
        setHpPercent: function (percent) {
            core.unit.setHpPercent(requireUnitId(this), percent);
        },
        setShieldsPercent: function (percent) {
            return core.unit.setShieldsPercent(requireUnitId(this), percent);
        },
        setEnergyPercent: function (percent) {
            return core.unit.setEnergyPercent(requireUnitId(this), percent);
        },
        getOrder: function () {
            return Order.get(core.unit.getOrder(requireUnitId(this)));
        },
        getTarget: function () {
            return getUnit(core.unit.getTarget(requireUnitId(this)));
        },
        getTargetPosition: function () {
            var id = requireUnitId(this);

            return createPosition(core.unit.getTargetX(id), core.unit.getTargetY(id));
        },
        isCompleted: function () {
            return core.unit.isCompleted(requireUnitId(this));
        },
        isCloaked: function () {
            return core.unit.isCloaked(requireUnitId(this));
        },
        isBurrowed: function () {
            return core.unit.isBurrowed(requireUnitId(this));
        },
        isInvincible: function () {
            return core.unit.isInvincible(requireUnitId(this));
        },
        // The image type of the unit's main image.
        getImageType: function () {
            return ImageType.get(core.unit.getImageType(requireUnitId(this)));
        },
        setInvincible: function (invincible) {
            core.unit.setInvincible(requireUnitId(this), invincible);
        },
        // The disabled status of a doodad (or a unit under Lockdown, Stasis Field or Maelstrom), which Set Doodad State changes.
        isDisabled: function () {
            return core.unit.isDisabled(requireUnitId(this));
        },
        enableDoodad: function () {
            core.unit.enableDoodad(requireUnitId(this));
        },
        disableDoodad: function () {
            core.unit.disableDoodad(requireUnitId(this));
        },
        // The resource amount of a mineral field or geyser; setting it returns false for a unit that holds no resources.
        getResources: function () {
            return core.unit.getResources(requireUnitId(this));
        },
        setResources: function (amount) {
            return core.unit.setResources(requireUnitId(this), amount);
        },
        // The Interceptors or Scarabs of a Carrier or Reaver; adding builds up to count of them while the hangar takes another one and returns
        // false for a unit without a hangar.
        getHangarCount: function () {
            return core.unit.getHangarCount(requireUnitId(this));
        },
        addHangarCount: function (count) {
            return core.unit.addHangarCount(requireUnitId(this), count);
        },
        kill: function () {
            return core.unit.kill(requireUnitId(this));
        },
        remove: function () {
            return core.unit.remove(requireUnitId(this));
        },
        // Issues an order to a target unit or a position; without a target the unit's own position is the target.
        issueOrder: function (order, target) {
            var id = requireUnitId(this);
            var orderId = requireObject(order, orderPrototype, 'An order').id;

            if (target === undefined || target === null) {
                core.unit.issueOrder(id, orderId, NONE, core.unit.getX(id), core.unit.getY(id));
            } else if (isObjectOf(target, unitPrototype)) {
                var targetId = requireUnitId(target);

                core.unit.issueOrder(id, orderId, targetId, core.unit.getX(targetId), core.unit.getY(targetId));
            } else {
                requireObject(target, positionPrototype, 'A unit or a position');
                core.unit.issueOrder(id, orderId, NONE, target.x, target.y);
            }
        },
        getDefensiveMatrixHp: function () {
            return core.unit.getDefensiveMatrixHp(requireUnitId(this)) / RAW_SCALE;
        },
        setDefensiveMatrixHp: function (hp) {
            core.unit.setDefensiveMatrixHp(requireUnitId(this), round(hp * RAW_SCALE));
        },
        isParasitedBy: function (player) {
            return (core.unit.getParasiteFlags(requireUnitId(this)) & 1 << requireObject(player, playerPrototype, 'A player').id) !== 0;
        },
        setParasitedBy: function (player, parasited) {
            var id = requireUnitId(this);
            var bit = 1 << requireObject(player, playerPrototype, 'A player').id;
            var flags = core.unit.getParasiteFlags(id);

            core.unit.setParasiteFlags(id, parasited ? flags | bit : flags & ~bit);
        },
        isBlind: function () {
            return core.unit.getBlindFlags(requireUnitId(this)) !== 0;
        },
        isBlindedBy: function (player) {
            return (core.unit.getBlindFlags(requireUnitId(this)) & 1 << requireObject(player, playerPrototype, 'A player').id) !== 0;
        },
        setBlindedBy: function (player, blinded) {
            var id = requireUnitId(this);
            var bit = 1 << requireObject(player, playerPrototype, 'A player').id;
            var flags = core.unit.getBlindFlags(id);

            core.unit.setBlindFlags(id, blinded ? flags | bit : flags & ~bit);
        },
        // The unit types in the build queue from the current one.
        getBuildQueue: function () {
            return core.unit.getBuildQueue(requireUnitId(this)).map(function (id) {
                return UnitType.get(id);
            });
        },
        getResearch: function () {
            return TechType.get(core.unit.getResearch(requireUnitId(this)));
        },
        getUpgrade: function () {
            return UpgradeType.get(core.unit.getUpgrade(requireUnitId(this)));
        },
        getAddon: function () {
            return getUnit(core.unit.getAddon(requireUnitId(this)));
        },
        getRallyPosition: function () {
            var id = requireUnitId(this);

            return createPosition(core.unit.getRallyX(id), core.unit.getRallyY(id));
        },
        // The rally writes return false for a unit type without a rally point.
        setRallyPosition: function (position) {
            var id = requireUnitId(this);

            requireObject(position, positionPrototype, 'A position');

            return core.unit.setRallyPosition(id, position.x, position.y);
        },
        getRallyUnit: function () {
            return getUnit(core.unit.getRallyUnit(requireUnitId(this)));
        },
        setRallyUnit: function (unit) {
            var id = requireUnitId(this);

            return core.unit.setRallyUnit(id, unit === null ? NONE : requireUnitId(unit));
        },
        getMoveTarget: function () {
            var id = requireUnitId(this);

            return createPosition(core.unit.getMoveTargetX(id), core.unit.getMoveTargetY(id));
        },
        setMoveTarget: function (position) {
            var id = requireUnitId(this);

            requireObject(position, positionPrototype, 'A position');
            core.unit.setMoveTarget(id, position.x, position.y);
        },
        // Changes the unit's type through the morph routine; false for a subunit, a subunit type or a type whose turret does not match.
        setType: function (type) {
            return core.unit.setType(requireUnitId(this), requireObject(type, unitTypePrototype, 'A unit type').id);
        },
        toString: function () {
            return 'Unit ' + this.id;
        }
    };

    // The raw unit fields with a get and a set: the status timers in status steps of eight frames (a timer that starts runs the effect's apply
    // routine, 0 its end routine), the cooldowns and the remaining times in frames.
    function defineUnitField(name) {
        unitPrototype['get' + name] = function () {
            return core.unit['get' + name](requireUnitId(this));
        };
        unitPrototype['set' + name] = function (value) {
            core.unit['set' + name](requireUnitId(this), value);
        };
    }

    ['StimTimer', 'EnsnareTimer', 'LockdownTimer', 'StasisTimer', 'MaelstromTimer', 'PlagueTimer', 'IrradiateTimer', 'StormTimer',
     'DefensiveMatrixTimer', 'AcidSporeCount', 'RemoveTimer', 'GroundCooldown', 'AirCooldown', 'SpellCooldown', 'KillCount',
     'RemainingBuildTime', 'RemainingResearchTime', 'Direction'].forEach(defineUnitField);

    unitPrototype.getCurrentSpeed = function () {
        return core.unit.getCurrentSpeed(requireUnitId(this));
    };
    unitPrototype.getTopSpeed = function () {
        return core.unit.getTopSpeed(requireUnitId(this));
    };

    var playerPrototype = {
        getId: function () {
            return this.id;
        },
        getName: function () {
            return core.player.getName(this.id);
        },
        getRace: function () {
            return core.player.getRace(this.id);
        },
        isHuman: function () {
            return core.player.isHuman(this.id);
        },
        getMinerals: function () {
            return core.player.getMinerals(this.id);
        },
        setMinerals: function (minerals) {
            core.player.setMinerals(this.id, minerals);
        },
        addMinerals: function (minerals) {
            core.player.addMinerals(this.id, minerals);
        },
        subtractMinerals: function (minerals) {
            core.player.subtractMinerals(this.id, minerals);
        },
        getGas: function () {
            return core.player.getGas(this.id);
        },
        setGas: function (gas) {
            core.player.setGas(this.id, gas);
        },
        addGas: function (gas) {
            core.player.addGas(this.id, gas);
        },
        subtractGas: function (gas) {
            core.player.subtractGas(this.id, gas);
        },
        // Ends the game for the player as the Victory, Defeat and Draw actions do; a defeat or a draw after a victory returns false.
        win: function () {
            return core.player.endGame(this.id, RESULT_VICTORY);
        },
        lose: function () {
            return core.player.endGame(this.id, RESULT_DEFEAT);
        },
        draw: function () {
            return core.player.endGame(this.id, RESULT_DRAW);
        },
        // The supply functions take a race from 0 (Zerg) to 2 (Protoss); without one, the player's race.
        getSupplyUsed: function (race) {
            return core.player.getSupplyUsed(this.id, race === undefined ? this.getRace() : race) / SUPPLY_SCALE;
        },
        getSupplyProvided: function (race) {
            return core.player.getSupplyProvided(this.id, race === undefined ? this.getRace() : race) / SUPPLY_SCALE;
        },
        getSupplyMax: function (race) {
            return core.player.getSupplyMax(this.id, race === undefined ? this.getRace() : race) / SUPPLY_SCALE;
        },
        isAlliedWith: function (player) {
            return core.player.getAlliance(this.id, requireObject(player, playerPrototype, 'A player').id) !== 0;
        },
        isEnemyOf: function (player) {
            return core.player.getAlliance(this.id, requireObject(player, playerPrototype, 'A player').id) === 0;
        },
        getAlliance: function (player) {
            return Alliance.get(core.player.getAlliance(this.id, requireObject(player, playerPrototype, 'A player').id));
        },
        // Sets the alliance toward another player as Set Alliance Status does; the player is one from 0 to 7.
        setAlliance: function (player, alliance) {
            var other = requireObject(player, playerPrototype, 'A player').id;

            core.player.setAlliance(this.id, other, requireObject(alliance, alliancePrototype, 'An alliance').id);
        },
        // Minerals, gas, or minerals and gas, as the Accumulate condition reads them.
        getResources: function (resourceType) {
            return core.player.getResources(this.id, requireObject(resourceType, resourceTypePrototype, 'A resource type').id);
        },
        // The counters of the trigger conditions and actions, of a UnitType or a UnitGroup: deaths (Deaths, Set Deaths), kills (Kill) and
        // scores of a ScoreType (Score, Set Score; set for players 0 to 7).
        getDeaths: function (unit) {
            return core.player.getDeaths(this.id, readUnitOperand(unit));
        },
        setDeaths: function (unit, amount) {
            core.player.setDeaths(this.id, readUnitOperand(unit), amount);
        },
        addDeaths: function (unit, amount) {
            core.player.addDeaths(this.id, readUnitOperand(unit), amount);
        },
        subtractDeaths: function (unit, amount) {
            core.player.subtractDeaths(this.id, readUnitOperand(unit), amount);
        },
        getKills: function (unit) {
            return core.player.getKills(this.id, readUnitOperand(unit));
        },
        getScore: function (scoreType) {
            return core.player.getScore(this.id, requireObject(scoreType, scoreTypePrototype, 'A score type').id);
        },
        setScore: function (scoreType, amount) {
            core.player.setScore(this.id, requireObject(scoreType, scoreTypePrototype, 'A score type').id, amount);
        },
        addScore: function (scoreType, amount) {
            core.player.addScore(this.id, requireObject(scoreType, scoreTypePrototype, 'A score type').id, amount);
        },
        subtractScore: function (scoreType, amount) {
            core.player.subtractScore(this.id, requireObject(scoreType, scoreTypePrototype, 'A score type').id, amount);
        },
        // Counts the player's units of a UnitType or a UnitGroup as the Command and Bring conditions do: {location: a Location to count only
        // the units there, completed: true to count only completed units}.
        countUnits: function (unit, options) {
            var operand = readUnitOperand(unit);
            var values = readOptions(options);
            var location = readLocationOption(values);
            var completed = values.completed === true;

            return location === NONE ? core.player.countUnits(this.id, operand, completed)
                                     : core.player.countUnitsAt(this.id, operand, location, completed);
        },
        // The opponents the Opponents condition counts.
        countOpponents: function () {
            return core.player.countOpponents(this.id);
        },
        // Kills or removes the player's units as Kill Unit and Remove Unit do: {location: a Location, count: the most units, all without one}.
        killUnits: function (unit, options) {
            removePlayerUnits(this, unit, options, false);
        },
        removeUnits: function (unit, options) {
            removePlayerUnits(this, unit, options, true);
        },
        // Starts an AI script by its four-character name, at a location when one is given; false when the player may not run it.
        runAiScript: function (name, location) {
            var script = readAiScript(name);

            if (location === undefined || location === null) {
                return core.player.runAiScript(this.id, script);
            }

            return core.player.runAiScriptAt(this.id, script, requireObject(location, locationPrototype, 'A location').id);
        },
        setMissionObjectives: function (text) {
            core.player.setMissionObjectives(this.id, String(text));
        },
        getUnits: function () {
            return game.findUnits({owner: this});
        },
        // The local display of the player's screen, as the trigger actions show it when the player is the local player. A printed text always
        // shows; with {forced: false} it shows when the subtitle option is on.
        print: function (text, options) {
            core.player.print(this.id, String(text), readOptions(options).forced !== false);
        },
        pingMinimap: function (position) {
            requireObject(position, positionPrototype, 'A position');
            core.player.pingMinimap(this.id, position.x, position.y);
        },
        // Plays a sound file of the map archive by its path.
        playSound: function (path) {
            core.player.playSound(this.id, String(path));
        },
        // Shows the talking portrait of a unit type for a number of frames.
        showTalkingPortrait: function (unitType, frames) {
            var id = requireObject(unitType, unitTypePrototype, 'A unit type').id;

            core.player.showTalkingPortrait(this.id, id, round(frames * core.game.getFrameDuration()));
        },
        setUnitSpeechMuted: function (muted) {
            core.player.setUnitSpeechMuted(this.id, muted);
        },
        // Centers the screen on a position when the player is the local player; the screens of the other peers stay.
        getUpgradeLevel: function (upgrade) {
            return core.player.getUpgradeLevel(this.id, requireObject(upgrade, upgradeTypePrototype, 'An upgrade type').id);
        },
        // Sets the level and refreshes the upgrade flags of the player's units, as a finished upgrade does.
        setUpgradeLevel: function (upgrade, level) {
            core.player.setUpgradeLevel(this.id, requireObject(upgrade, upgradeTypePrototype, 'An upgrade type').id, level);
        },
        getMaxUpgradeLevel: function (upgrade) {
            return core.player.getMaxUpgradeLevel(this.id, requireObject(upgrade, upgradeTypePrototype, 'An upgrade type').id);
        },
        setMaxUpgradeLevel: function (upgrade, level) {
            core.player.setMaxUpgradeLevel(this.id, requireObject(upgrade, upgradeTypePrototype, 'An upgrade type').id, level);
        },
        isUpgradeInProgress: function (upgrade) {
            return core.player.isUpgradeInProgress(this.id, requireObject(upgrade, upgradeTypePrototype, 'An upgrade type').id);
        },
        isTechResearched: function (tech) {
            return core.player.isTechResearched(this.id, requireObject(tech, techTypePrototype, 'A tech type').id);
        },
        setTechResearched: function (tech, researched) {
            core.player.setTechResearched(this.id, requireObject(tech, techTypePrototype, 'A tech type').id, researched);
        },
        isTechAvailable: function (tech) {
            return core.player.isTechAvailable(this.id, requireObject(tech, techTypePrototype, 'A tech type').id);
        },
        setTechAvailable: function (tech, available) {
            core.player.setTechAvailable(this.id, requireObject(tech, techTypePrototype, 'A tech type').id, available);
        },
        isTechInProgress: function (tech) {
            return core.player.isTechInProgress(this.id, requireObject(tech, techTypePrototype, 'A tech type').id);
        },
        // The map settings' production availability; a game without map settings allows every unit type.
        isUnitAvailable: function (unitType) {
            return core.player.isUnitAvailable(this.id, requireObject(unitType, unitTypePrototype, 'A unit type').id);
        },
        setUnitAvailable: function (unitType, available) {
            core.player.setUnitAvailable(this.id, requireObject(unitType, unitTypePrototype, 'A unit type').id, available);
        },
        centerView: function (position) {
            requireObject(position, positionPrototype, 'A position');
            core.player.centerView(this.id, position.x, position.y);
        },
        // The player's selection, which the selection commands change and every peer holds: the live units in selection order.
        getSelection: function () {
            return toUnits(core.player.getSelection(this.id));
        },
        // Replaces the selection as a selection command of the player does, with at most 12 units; the command's rules may leave some out (a
        // hidden unit, and in a selection of more than one unit a building, another player's unit and the like). True if every unit was
        // selected.
        setSelection: function (units) {
            if (!Array.isArray(units)) {
                throw new TypeError('An array of units is needed, not ' + units + '.');
            }

            var ids = units.map(function (unit) {
                return requireUnitId(unit);
            });

            core.player.setSelection(this.id, ids);

            var selected = core.player.getSelection(this.id);

            return ids.every(function (id) {
                return selected.indexOf(id) >= 0;
            });
        },
        toString: function () {
            return 'Player ' + this.id;
        }
    };

    var locationPrototype = {
        getId: function () {
            return this.id;
        },
        getName: function () {
            return core.location.getName(this.id);
        },
        getLeft: function () {
            return core.location.getLeft(this.id);
        },
        getTop: function () {
            return core.location.getTop(this.id);
        },
        getRight: function () {
            return core.location.getRight(this.id);
        },
        getBottom: function () {
            return core.location.getBottom(this.id);
        },
        getCenter: function () {
            return createPosition(core.location.getCenterX(this.id), core.location.getCenterY(this.id));
        },
        setBounds: function (left, top, right, bottom) {
            core.location.setBounds(this.id, left, top, right, bottom);
        },
        // Moves the location so the position is its center, kept inside the map as Move Location keeps it.
        setCenter: function (position) {
            requireObject(position, positionPrototype, 'A position');
            core.location.setCenter(this.id, position.x, position.y);
        },
        toString: function () {
            return 'Location ' + this.id;
        }
    };

    var timerPrototype = {
        getId: function () {
            return this.id;
        },
        cancel: function () {
            return core.game.cancelTimer(this.id);
        }
    };

    var resourcePrototype = {
        getId: function () {
            return this.id;
        },
        getName: function () {
            return this.name;
        },
        toString: function () {
            return this.name;
        }
    };
    var grpPrototype = create(resourcePrototype);
    var animPrototype = create(resourcePrototype);
    var imagePrototype = create(resourcePrototype);
    var resourceHandles = [];

    // The pixel size of an image.
    imagePrototype.getWidth = function () {
        return core.resources.getImageWidth(this.id);
    };
    imagePrototype.getHeight = function () {
        return core.resources.getImageHeight(this.id);
    };

    // Returns the object of a resource handle: one object per handle id.
    function getResource(id, name, prototype) {
        var resource = resourceHandles[id];

        if (resource === undefined) {
            resource = freeze(createObject(prototype, {id: id, name: name}));
            resourceHandles[id] = resource;
        }

        return resource;
    }

    function readResourceId(resource, prototype, kind) {
        return resource === undefined || resource === null ? NONE : requireObject(resource, prototype, kind).id;
    }

    // Returns the handle ids of the graphics {sd, hd, hd2}: a GRP, an HD anim and an HD2 anim, each optional.
    function readGraphicsIds(images) {
        if (images === null || typeof images !== 'object') {
            throw new TypeError('The graphics {sd, hd, hd2} are needed, not ' + images + '.');
        }

        return [readResourceId(images.sd, grpPrototype, 'A GRP'), readResourceId(images.hd, animPrototype, 'An anim'),
                readResourceId(images.hd2, animPrototype, 'An anim')];
    }

    // The image type of a unit's images: the given one, else the image type of the unit's main image.
    function readUnitImageType(unit, imageType) {
        if (imageType === undefined) {
            return core.unit.getImageType(requireUnitId(unit));
        }

        return requireObject(imageType, imageTypePrototype, 'An image type').id;
    }

    var resources = freeze({
        has: function (name) {
            return core.resources.has(String(name));
        },
        getGrp: function (name) {
            var text = String(name);

            return getResource(core.resources.getGrp(text), text, grpPrototype);
        },
        getAnim: function (name) {
            var text = String(name);

            return getResource(core.resources.getAnim(text), text, animPrototype);
        },
        // A PNG or DDS picture to draw (screen.drawImage) or to show on the console.
        getImage: function (name) {
            var text = String(name);

            return getResource(core.resources.getImage(text), text, imagePrototype);
        }
    });

    var graphics = freeze({
        // Replaces the graphics of every image of an image type with {sd: a GRP, hd: an HD anim, hd2: an HD2 anim}.
        replaceImage: function (imageType, images) {
            var imageTypeId = requireObject(imageType, imageTypePrototype, 'An image type').id;
            var ids = readGraphicsIds(images);

            core.graphics.replaceImage(imageTypeId, ids[0], ids[1], ids[2]);
        },
        restoreImage: function (imageType) {
            core.graphics.restoreImage(requireObject(imageType, imageTypePrototype, 'An image type').id);
        },
        // Replaces the graphics of a unit's images of an image type, by default that of its main image now, while the unit lives.
        replaceUnitImage: function (unit, images, imageType) {
            var id = requireUnitId(unit);
            var imageTypeId = readUnitImageType(unit, imageType);
            var ids = readGraphicsIds(images);

            core.graphics.replaceUnitImage(id, imageTypeId, ids[0], ids[1], ids[2]);
        },
        restoreUnitImage: function (unit, imageType) {
            core.graphics.restoreUnitImage(requireUnitId(unit), readUnitImageType(unit, imageType));
        }
    });

    var TILES_PER_GROUP = 16;
    var TILE_GROUP_SHIFT = 4;

    var tilePrototype = {
        getId: function () {
            return this.id;
        },
        getGroup: function () {
            return this.id >> TILE_GROUP_SHIFT;
        },
        getIndex: function () {
            return this.id & (TILES_PER_GROUP - 1);
        },
        isWalkable: function () {
            return core.terrain.isTileWalkable(this.id);
        },
        isPartiallyWalkable: function () {
            return core.terrain.isTilePartiallyWalkable(this.id);
        },
        isBuildable: function () {
            return core.terrain.isTileBuildable(this.id);
        },
        hasCreep: function () {
            return core.terrain.hasTileCreep(this.id);
        },
        getElevation: function () {
            return core.terrain.getTileElevation(this.id);
        },
        isBlockingSight: function () {
            return core.terrain.isTileBlockingSight(this.id);
        },
        toString: function () {
            return 'Tile ' + this.getGroup() + ':' + this.getIndex();
        }
    };
    var tiles = [];

    // Returns the object of a tile value: one object per value.
    function getTileObject(id) {
        var tile = tiles[id];

        if (tile === undefined) {
            tile = freeze(createObject(tilePrototype, {id: id}));
            tiles[id] = tile;
        }

        return tile;
    }

    // The tiles a location covers, clipped to the map: {left, top, right, bottom}, right and bottom exclusive.
    function readLocationTiles(location) {
        var id = requireObject(location, locationPrototype, 'A location').id;
        var left = core.location.getLeft(id);
        var top = core.location.getTop(id);
        var right = core.location.getRight(id);
        var bottom = core.location.getBottom(id);

        return {
            left: max(0, floor(min(left, right) / TILE_SIZE)),
            top: max(0, floor(min(top, bottom) / TILE_SIZE)),
            right: min(core.game.getMapTileWidth(), ceil(max(left, right) / TILE_SIZE)),
            bottom: min(core.game.getMapTileHeight(), ceil(max(top, bottom) / TILE_SIZE))
        };
    }

    var tileset = freeze({
        getId: function () {
            return core.game.getTileset();
        },
        getGroupCount: function () {
            return core.terrain.getTileGroupCount();
        },
        // Returns the tile of a CV5 group and an index in it from 0 to 15.
        getTile: function (group, index) {
            if (index !== (index | 0) || index < 0 || index >= TILES_PER_GROUP) {
                throw new RangeError('The tile index ' + index + ' is not from 0 to ' + (TILES_PER_GROUP - 1) + '.');
            }

            if (group !== (group | 0) || group < 0 || !core.terrain.isValidTile(group << TILE_GROUP_SHIFT | index)) {
                throw new RangeError('The tile ' + group + ':' + index + ' is not a tile of the tileset of the map.');
            }

            return getTileObject(group << TILE_GROUP_SHIFT | index);
        }
    });

    // The tiles of the map. The changes apply at the next trigger step, after the frame start hooks and before the triggers; reads see the
    // changes made so far.
    var terrain = freeze({
        getTile: function (tileX, tileY) {
            return getTileObject(core.terrain.getTile(tileX, tileY));
        },
        setTile: function (tileX, tileY, tile) {
            core.terrain.setTile(tileX, tileY, requireObject(tile, tilePrototype, 'A tile').id);
        },
        // Sets every tile a location covers.
        fillTiles: function (location, tile) {
            var id = requireObject(tile, tilePrototype, 'A tile').id;
            var area = readLocationTiles(location);

            for (var tileY = area.top; tileY < area.bottom; tileY++) {
                for (var tileX = area.left; tileX < area.right; tileX++) {
                    core.terrain.setTile(tileX, tileY, id);
                }
            }
        },
        // Copies the tiles one location covers onto the tiles another location of the same size in tiles covers.
        copyTiles: function (fromLocation, toLocation) {
            var source = readLocationTiles(fromLocation);
            var target = readLocationTiles(toLocation);
            var width = source.right - source.left;
            var height = source.bottom - source.top;

            if (width !== target.right - target.left || height !== target.bottom - target.top) {
                throw new RangeError('The locations cover ' + width + 'x' + height + ' and ' + (target.right - target.left) + 'x' +
                                     (target.bottom - target.top) + ' tiles, not the same size.');
            }

            var values = [];

            for (var row = 0; row < height; row++) {
                for (var column = 0; column < width; column++) {
                    values.push(core.terrain.getTile(source.left + column, source.top + row));
                }
            }

            for (var index = 0; index < values.length; index++) {
                core.terrain.setTile(target.left + index % width, target.top + floor(index / width), values[index]);
            }
        }
    });

    // Returns the object of a unit that an event or a decision captured: the unit's own object while its slot holds the same unit,
    // otherwise an object of the gone unit whose exists() is false.
    function getCapturedUnit(id, generation) {
        if (id === NONE) {
            return null;
        }

        var unit = units[id];

        if (unit !== undefined && unit.generation === generation) {
            return unit;
        }

        if (core.unit.exists(id) && core.unit.getGeneration(id) === generation) {
            return getUnit(id);
        }

        return createObject(unitPrototype, {id: id, generation: generation});
    }

    // The event and decision kinds by hook name: the prototype of their objects and the reading of their values (see
    // ScriptSimulationUtils for the values). A field gets a getter, is* for the boolean fields.
    var eventKinds = {};
    var decisionKinds = {};
    var BOOLEAN_FIELDS = {queued: true, missed: true};
    var UNIT_FIELDS = ['frame', 'unit', 'unitType', 'owner'];

    function createGetters(fields) {
        var prototype = {};

        fields.forEach(function (field) {
            var name = (BOOLEAN_FIELDS[field] ? 'is' : 'get') + field.charAt(0).toUpperCase() + field.substring(1);

            prototype[name] = function () {
                return this[field];
            };
        });

        return prototype;
    }

    function defineEvent(hookName, fields, read) {
        var prototype = createGetters(fields);

        eventKinds[hookName] = {prototype: prototype, read: read};

        return prototype;
    }

    function readUnitFields(values) {
        return {
            frame: values.frame,
            unit: getCapturedUnit(values.unit, values.unitGeneration),
            unitType: UnitType.get(values.unitType),
            owner: getPlayer(values.owner)
        };
    }

    // Reads the unit fields and one more captured unit.
    function readUnitFieldsWith(name) {
        return function (values) {
            var fields = readUnitFields(values);

            fields[name] = getCapturedUnit(values[name], values[name + 'Generation']);

            return fields;
        };
    }

    defineEvent('onUnitCreated', UNIT_FIELDS, readUnitFields);
    defineEvent('onUnitCompleted', UNIT_FIELDS, readUnitFields);
    defineEvent('onUnitDied', UNIT_FIELDS.concat(['killer', 'killerPlayer', 'weapon']), function (values) {
        var fields = readUnitFieldsWith('killer')(values);

        fields.killerPlayer = getPlayer(values.killerPlayer);
        fields.weapon = WeaponType.get(values.weapon);

        return fields;
    });
    defineEvent('onUnitDamaged', UNIT_FIELDS.concat(['source', 'sourcePlayer', 'weapon', 'amount']), function (values) {
        var fields = readUnitFieldsWith('source')(values);

        fields.sourcePlayer = getPlayer(values.sourcePlayer);
        fields.weapon = WeaponType.get(values.weapon);
        fields.amount = values.amount / RAW_SCALE;

        return fields;
    });
    defineEvent('onOrderIssued', UNIT_FIELDS.concat(['order', 'target', 'targetPosition', 'buildType', 'queued']), function (values) {
        var fields = readUnitFieldsWith('target')(values);

        fields.order = Order.get(values.order);
        fields.targetPosition = createPosition(values.targetX, values.targetY);
        fields.buildType = UnitType.get(values.buildType);
        fields.queued = values.queued;

        return fields;
    });
    defineEvent('onTargetAcquired', UNIT_FIELDS.concat(['target']), readUnitFieldsWith('target'));
    defineEvent('onUnitTrained', UNIT_FIELDS.concat(['producer']), readUnitFieldsWith('producer'));
    defineEvent('onResearchCompleted', UNIT_FIELDS.concat(['tech']), function (values) {
        var fields = readUnitFields(values);

        fields.tech = TechType.get(values.tech);

        return fields;
    });
    defineEvent('onUpgradeCompleted', UNIT_FIELDS.concat(['upgrade', 'level']), function (values) {
        var fields = readUnitFields(values);

        fields.upgrade = UpgradeType.get(values.upgrade);
        fields.level = values.level;

        return fields;
    });
    defineEvent('onSpellCast', UNIT_FIELDS.concat(['tech', 'target', 'targetPosition']), function (values) {
        var fields = readUnitFieldsWith('target')(values);

        fields.tech = TechType.get(values.tech);
        fields.targetPosition = createPosition(values.targetX, values.targetY);

        return fields;
    });

    function readBulletFields(values) {
        return {
            frame: values.frame,
            weapon: WeaponType.get(values.weapon),
            player: getPlayer(values.player),
            source: getCapturedUnit(values.source, values.sourceGeneration),
            target: getCapturedUnit(values.target, values.targetGeneration),
            targetPosition: createPosition(values.targetX, values.targetY),
            missed: values.missed === true
        };
    }

    var BULLET_FIELDS = ['frame', 'weapon', 'player', 'source', 'target', 'targetPosition'];

    defineEvent('onBulletCreated', BULLET_FIELDS, readBulletFields);
    defineEvent('onBulletHit', BULLET_FIELDS.concat(['missed']), readBulletFields);
    defineEvent('onOwnerChanged', UNIT_FIELDS.concat(['previousOwner']), function (values) {
        var fields = readUnitFields(values);

        fields.previousOwner = getPlayer(values.previousOwner);

        return fields;
    });
    defineEvent('onUnitLoaded', UNIT_FIELDS.concat(['transport']), readUnitFieldsWith('transport'));
    defineEvent('onUnitUnloaded', UNIT_FIELDS.concat(['transport']), readUnitFieldsWith('transport'));
    defineEvent('onPlayerLeft', ['frame', 'player', 'reason'], function (values) {
        return {frame: values.frame, player: getPlayer(values.player), reason: values.reason};
    });

    var playerResultPrototype = defineEvent('onPlayerResult', ['frame', 'player', 'result'], function (values) {
        return {frame: values.frame, player: getPlayer(values.player), result: values.result};
    });

    playerResultPrototype.isVictory = function () {
        return this.result === RESULT_VICTORY || this.result === RESULT_SECURED;
    };
    playerResultPrototype.isDefeat = function () {
        return this.result === RESULT_DEFEAT;
    };
    playerResultPrototype.isDraw = function () {
        return this.result === RESULT_DRAW;
    };
    playerResultPrototype.isDropped = function () {
        return this.result === RESULT_DROPPED;
    };

    // The local events of the local player's input: the key or button, the modifier mask and the map position under the pointer, or null
    // while unknown; the chat line. consume() keeps the press or the chat line from the game; the event keeps its values object, whose
    // consumed flag the game reads after the hooks.
    var LOCAL_KEY_FIELDS = ['key', 'modifiers', 'mousePosition'];
    var LOCAL_BUTTON_FIELDS = ['button', 'modifiers', 'mousePosition'];
    var consumeMethods = {
        consume: function () {
            this.values.consumed = true;
        },
        isConsumed: function () {
            return this.values.consumed === true;
        }
    };
    var modifierGetters = {
        isShift: function () {
            return (this.modifiers & Modifier.Shift) !== 0;
        },
        isControl: function () {
            return (this.modifiers & Modifier.Control) !== 0;
        },
        isAlt: function () {
            return (this.modifiers & Modifier.Alt) !== 0;
        }
    };

    function addMethods(prototype, methods) {
        for (var name in methods) {
            prototype[name] = methods[name];
        }
    }

    function readLocalInputFields(values) {
        return {
            values: values,
            modifiers: values.modifiers,
            mousePosition: values.positioned ? createPosition(values.x, values.y) : null
        };
    }

    function readLocalKeyFields(values) {
        var fields = readLocalInputFields(values);

        fields.key = Key.get(values.key);

        return fields;
    }

    function readLocalButtonFields(values) {
        var fields = readLocalInputFields(values);

        fields.button = MouseButton.get(values.button);

        return fields;
    }

    [
        defineEvent('onLocalKeyDown', LOCAL_KEY_FIELDS, readLocalKeyFields),
        defineEvent('onLocalKeyUp', LOCAL_KEY_FIELDS, readLocalKeyFields),
        defineEvent('onLocalMouseDown', LOCAL_BUTTON_FIELDS, readLocalButtonFields),
        defineEvent('onLocalMouseUp', LOCAL_BUTTON_FIELDS, readLocalButtonFields)
    ].forEach(function (prototype) {
        addMethods(prototype, modifierGetters);
        addMethods(prototype, consumeMethods);
    });
    addMethods(defineEvent('onLocalChat', ['text'], function (values) {
        return {values: values, text: String(values.text)};
    }), consumeMethods);

    // A decision object keeps the values a hook may change in its hidden state, which the game reads back after the hook.
    function defineDecision(hookName, fields, prototype, read) {
        var getters = createGetters(fields);

        for (var name in prototype) {
            getters[name] = prototype[name];
        }

        decisionKinds[hookName] = {prototype: getters, read: read};
    }

    function requireUnitOrNull(unit) {
        return unit === null || unit === undefined ? null : getUnit(requireUnitId(unit));
    }

    defineDecision('onBeforeDamage', ['target', 'targetType', 'targetOwner', 'source', 'sourcePlayer', 'weapon'], {
        getAmount: function () {
            return this.state.amount / RAW_SCALE;
        },
        setAmount: function (amount) {
            var raw = round(amount * RAW_SCALE);

            if (!(raw >= 0 && raw <= MAXIMUM_RAW_AMOUNT)) {
                throw new RangeError('The damage ' + amount + ' is not a damage amount.');
            }

            this.state.amount = raw;
        },
        cancel: function () {
            this.state.cancelled = true;
        },
        isCancelled: function () {
            return this.state.cancelled;
        }
    }, function (values) {
        return {
            target: getCapturedUnit(values.unit, values.unitGeneration),
            targetType: UnitType.get(values.unitType),
            targetOwner: getPlayer(values.owner),
            source: getCapturedUnit(values.source, values.sourceGeneration),
            sourcePlayer: getPlayer(values.sourcePlayer),
            weapon: WeaponType.get(values.weapon),
            state: {amount: values.amount, cancelled: values.cancelled}
        };
    });
    defineDecision('onBeforeOrder', ['unit', 'unitType', 'owner', 'buildType', 'queued'], {
        getOrder: function () {
            return Order.get(this.state.order);
        },
        setOrder: function (order) {
            this.state.order = requireObject(order, orderPrototype, 'An order').id;
        },
        getTarget: function () {
            return getCapturedUnit(this.state.target, this.state.targetGeneration);
        },
        getTargetPosition: function () {
            return createPosition(this.state.targetX, this.state.targetY);
        },
        // Sets a target unit (and its position), a position without a target unit, or no target unit for null.
        setTarget: function (target) {
            if (target === null || target === undefined) {
                this.state.target = NONE;
                this.state.targetGeneration = NONE;
            } else if (isObjectOf(target, unitPrototype)) {
                var id = requireUnitId(target);

                this.state.target = id;
                this.state.targetGeneration = target.generation;
                this.state.targetX = core.unit.getX(id);
                this.state.targetY = core.unit.getY(id);
            } else {
                requireObject(target, positionPrototype, 'A unit or a position');

                this.state.target = NONE;
                this.state.targetGeneration = NONE;
                this.state.targetX = target.x;
                this.state.targetY = target.y;
            }
        },
        reject: function () {
            this.state.rejected = true;
        },
        isRejected: function () {
            return this.state.rejected;
        }
    }, function (values) {
        var fields = readUnitFields(values);

        delete fields.frame;
        fields.buildType = UnitType.get(values.buildType);
        fields.queued = values.queued;
        fields.state = {
            order: values.order,
            target: values.target,
            targetGeneration: values.targetGeneration,
            targetX: values.targetX,
            targetY: values.targetY,
            rejected: values.rejected
        };

        return fields;
    });
    defineDecision('onBeforeTargetAcquired', ['unit', 'unitType', 'owner'], {
        getTarget: function () {
            return getCapturedUnit(this.state.target, this.state.targetGeneration);
        },
        // Sets the target unit, or no target for null.
        setTarget: function (target) {
            var unit = requireUnitOrNull(target);

            this.state.target = unit === null ? NONE : unit.id;
            this.state.targetGeneration = unit === null ? NONE : unit.generation;
        }
    }, function (values) {
        var fields = readUnitFields(values);

        delete fields.frame;
        fields.state = {target: values.target, targetGeneration: values.targetGeneration};

        return fields;
    });

    // A channel sends a value to its handler on every peer (core.sync); the handler receives the sending player and a copy of the value.
    var channelPrototype = {
        send: function (value) {
            core.sync.send(this.id, value);
        }
    };

    // A value per player on a channel: a message [slot, value] sets the slot, or the sending player's slot for NONE; get returns the initial
    // value until a message set the slot.
    var playerValuePrototype = {
        get: function (player) {
            var id = requireObject(player, playerPrototype, 'A player').id;

            return this.assigned[id] ? this.values[id] : this.initial;
        },
        set: function (playerOrValue, value) {
            if (arguments.length < 2) {
                this.channel.send([NONE, playerOrValue]);
            } else {
                this.channel.send([requireObject(playerOrValue, playerPrototype, 'A player').id, value]);
            }
        }
    };

    var sync = freeze({
        // Creates a channel; only while the modules load, at their top level, so that every peer has the same channels.
        createChannel: function (handler) {
            if (typeof handler !== 'function') {
                throw new TypeError('A channel handler function is needed, not ' + handler + '.');
            }

            var id = core.sync.createChannel(function (player, value) {
                handler(getPlayer(player), value);
            });

            return freeze(createObject(channelPrototype, {id: id}));
        },
        // Creates a value per player over a new channel; only while the modules load, as createChannel.
        createPlayerValue: function (initial) {
            var values = [];
            var assigned = [];
            var channel = sync.createChannel(function (player, message) {
                if (!Array.isArray(message) || message.length !== 2) {
                    return;
                }

                var id = message[0] === NONE ? player.id : message[0];

                if (typeof id === 'number' && id === floor(id) && getPlayer(id) !== null) {
                    values[id] = message[1];
                    assigned[id] = true;
                }
            });

            return freeze(createObject(playerValuePrototype, {channel: channel, initial: initial, values: values, assigned: assigned}));
        }
    });

    // What only the local machine knows, which differs from peer to peer: readable only in local code (the onLocal* hooks), elsewhere every
    // function throws a TypeError. The player is null in a replay and for an observer; the screen and pointer positions are null while the
    // client has not supplied them.
    var local = freeze({
        getPlayer: function () {
            return getPlayer(core.local.getPlayer());
        },
        isReplay: function () {
            return core.local.isReplay();
        },
        isChatOpen: function () {
            return core.local.isChatOpen();
        },
        // The map position of the local screen's top left.
        getScreenPosition: function () {
            return core.local.isViewKnown() ? createPosition(core.local.getScreenX(), core.local.getScreenY()) : null;
        },
        // Moves only the local screen's top left to a map position at once, kept inside the map; false while the screen is unknown.
        setScreenPosition: function (position) {
            requireObject(position, positionPrototype, 'A position');

            return core.local.setScreenPosition(position.x, position.y);
        },
        // The map position under the pointer.
        getMousePosition: function () {
            return core.local.isViewKnown() ? createPosition(core.local.getMouseX(), core.local.getMouseY()) : null;
        },
        // The units of the local interface's selection, which may differ from the local player's synchronized selection until its selection
        // command runs.
        getSelection: function () {
            return toUnits(core.local.getSelection());
        }
    });

    // Colours are numbers, packed 0xAARRGGBB with straight alpha.
    var MAXIMUM_CHANNEL = 255;
    var ALPHA_FACTOR = 0x1000000;
    var RED_SHIFT = 16;
    var GREEN_SHIFT = 8;

    function readChannel(value, name) {
        var channel = Number(value);

        if (channel !== floor(channel) || channel < 0 || channel > MAXIMUM_CHANNEL) {
            throw new RangeError('The ' + name + ' channel is an integer from 0 to 255, not ' + value + '.');
        }

        return channel;
    }

    var Color = freeze({
        Transparent: 0x00000000,
        Black: 0xFF000000,
        White: 0xFFFFFFFF,
        Gray: 0xFF808080,
        Red: 0xFFFF0000,
        Green: 0xFF00FF00,
        Blue: 0xFF0000FF,
        Yellow: 0xFFFFFF00,
        Cyan: 0xFF00FFFF,
        Magenta: 0xFFFF00FF,
        Orange: 0xFFFFA500,
        fromRgb: function (red, green, blue) {
            return Color.fromRgba(red, green, blue, MAXIMUM_CHANNEL);
        },
        fromRgba: function (red, green, blue, alpha) {
            return readChannel(alpha, 'alpha') * ALPHA_FACTOR +
                   (readChannel(red, 'red') << RED_SHIFT | readChannel(green, 'green') << GREEN_SHIFT | readChannel(blue, 'blue'));
        },
        // The colour of a palette index of the map's tileset palette, opaque.
        fromPalette: function (index) {
            return core.screen.getPaletteColor(index);
        }
    });

    var fontPrototype = create(typePrototype);
    var alignPrototype = create(typePrototype);
    var graphicsPrototype = create(typePrototype);
    var consoleControlPrototype = create(typePrototype);
    var consoleImagePrototype = create(typePrototype);
    // The four fonts of the game interface: font8, font10, font16 and font16x.
    var Font = createConstantTable(fontPrototype, ['Small', 'Medium', 'Large', 'Huge'], 4, 'Font ');
    var Align = createConstantTable(alignPrototype, ['Left', 'Center', 'Right'], 3, 'Align ');
    var Graphics = createConstantTable(graphicsPrototype, ['SD', 'HD', 'HD2'], 3, 'Graphics ');
    var ConsoleControl = createConstantTable(consoleControlPrototype,
                                             ['CommandCard', 'Selection', 'Resources', 'Portrait', 'Minimap', 'Menu', 'TextBox'], 7, 'Control ');
    var ConsoleImage = createConstantTable(consoleImagePrototype, ['Console'], 1, 'Image ');
    var DEFAULT_THICKNESS = 1;
    var DEFAULT_OPACITY = 1;

    function readColorOption(color) {
        return color === undefined ? Color.White : color;
    }

    function readThicknessOption(values) {
        return values.thickness === undefined ? DEFAULT_THICKNESS : values.thickness;
    }

    function readFontOption(font) {
        return font === undefined ? Font.Medium.id : requireObject(font, fontPrototype, 'A font').id;
    }

    // The drawing functions of one space over its core namespace: they work only in onRender.
    function createDrawing(namespace) {
        return {
            fillRectangle: function (x, y, width, height, color) {
                namespace.fillRectangle(x, y, width, height, readColorOption(color));
            },
            // Options {color, thickness}; the outline lies inside the rectangle.
            drawRectangle: function (x, y, width, height, options) {
                var values = readOptions(options);

                namespace.drawRectangle(x, y, width, height, readColorOption(values.color), readThicknessOption(values));
            },
            fillCircle: function (x, y, radius, color) {
                namespace.fillCircle(x, y, radius, readColorOption(color));
            },
            // Options {color, thickness}; the outline is centered on the circle.
            drawCircle: function (x, y, radius, options) {
                var values = readOptions(options);

                namespace.drawCircle(x, y, radius, readColorOption(values.color), readThicknessOption(values));
            },
            // Options {color, thickness}.
            drawLine: function (x1, y1, x2, y2, options) {
                var values = readOptions(options);

                namespace.drawLine(x1, y1, x2, y2, readColorOption(values.color), readThicknessOption(values));
            },
            drawPoint: function (x, y, color) {
                namespace.drawPoint(x, y, readColorOption(color));
            },
            // Options {font, color, align}: the lines, broken at \n, from the top at y, each aligned on x; colour codes in the text work.
            drawText: function (text, x, y, options) {
                var values = readOptions(options);
                var align = values.align === undefined ? Align.Left.id : requireObject(values.align, alignPrototype, 'An alignment').id;

                namespace.drawText(x, y, String(text), readFontOption(values.font), readColorOption(values.color), align);
            },
            // Options {width, height, opacity}; the size is the image's pixel size in the space's units by default.
            drawImage: function (image, x, y, options) {
                var values = readOptions(options);
                var id = requireObject(image, imagePrototype, 'An image').id;
                var width = values.width === undefined ? image.getWidth() : values.width;
                var height = values.height === undefined ? image.getHeight() : values.height;

                namespace.drawImage(id, x, y, width, height, values.opacity === undefined ? DEFAULT_OPACITY : values.opacity);
            },
            // The width of the widest line in interface units, in the client's fonts; only in local code.
            measureText: function (text, font) {
                return namespace.measureText(String(text), readFontOption(font));
            }
        };
    }

    // The screen object of onRender: the game screen in interface units, its world (map pixels, following the camera) and its minimap.
    var screenObject = createDrawing(core.screen);

    screenObject.getWidth = function () {
        return core.screen.getWidth();
    };
    screenObject.getHeight = function () {
        return core.screen.getHeight();
    };
    screenObject.getScale = function () {
        return core.screen.getScale();
    };
    screenObject.getGraphics = function () {
        return Graphics.get(core.screen.getGraphics());
    };
    // Rounds an interface coordinate to the real pixel grid.
    screenObject.snap = function (value) {
        var scale = core.screen.getScale();

        return round(value * scale) / scale;
    };
    screenObject.world = freeze(createDrawing(core.world));
    screenObject.minimap = freeze(createDrawing(core.minimap));
    freeze(screenObject);

    var consoleControlObjects = [];

    // A console control: one of the console dialogs, placed by its top left in the classic 640 by 480 screen.
    var consoleControlObjectPrototype = {
        getControl: function () {
            return this.control;
        },
        setPosition: function (x, y) {
            core.console.setDialogPosition(this.control.id, round(x), round(y));
        },
        setVisible: function (visible) {
            core.console.setDialogVisible(this.control.id, visible === true);
        },
        isVisible: function () {
            return core.console.isDialogVisible(this.control.id);
        }
    };

    // The resource icon frames of a resource type: minerals, then gas and supply for the Zerg, Terran and Protoss consoles.
    function readResourceIcons(resourceType) {
        var id = requireObject(resourceType, resourceTypePrototype, 'A resource type').id;

        if (id === ResourceType.Minerals.id) {
            return [0];
        }

        if (id === ResourceType.Gas.id) {
            return [1, 2, 3];
        }

        if (id === ResourceType.Supply.id) {
            return [4, 5, 6];
        }

        throw new TypeError('The resource type ' + resourceType + ' has no console icon.');
    }

    // The console: shared code sets it on every peer, local code only on the local machine. A setting wins over what EUD memory gives for the
    // same item; EUD memory still drives the items the script never set.
    var gameConsole = freeze({
        setVisible: function (visible) {
            core.console.setVisible(visible === true);
        },
        isVisible: function () {
            return core.console.isVisible();
        },
        getControl: function (control) {
            var id = requireObject(control, consoleControlPrototype, 'A console control').id;
            var object = consoleControlObjects[id];

            if (object === undefined) {
                object = freeze(createObject(consoleControlObjectPrototype, {control: control}));
                consoleControlObjects[id] = object;
            }

            return object;
        },
        // Draws an image instead of a console image, null for the installed one.
        replaceImage: function (consoleImage, image) {
            var id = requireObject(consoleImage, consoleImagePrototype, 'A console image').id;

            core.console.replaceImage(id, image === null || image === undefined ? NONE : requireObject(image, imagePrototype, 'An image').id);
        },
        // Draws an image for the icon of a resource type, null for the icon of EUD memory or the installed one.
        setResourceIcon: function (resourceType, image) {
            var icons = readResourceIcons(resourceType);
            var imageId = image === null || image === undefined ? NONE : requireObject(image, imagePrototype, 'An image').id;

            for (var index = 0; index < icons.length; index++) {
                core.console.setResourceIcon(icons[index], imageId);
            }
        }
    });

    // The stat_txt strings by their one-based ids; a text wins over the game's table, null shows the game's string again.
    var strings = freeze({
        setStatText: function (id, text) {
            core.strings.setStatText(id, text === null || text === undefined ? null : String(text));
        }
    });

    var messages = freeze({
        // Empties the message lines on the screen.
        clear: function () {
            core.messages.clear();
        }
    });

    // The name of a unit type in its stat_txt string, which tooltips, the selection and messages show; null shows the game's name again.
    unitTypePrototype.setName = function (text) {
        strings.setStatText(this.id + 1, text);
    };

    // The functions the game calls, which the scripts do not see.
    var gameFunctions = freeze({
        createEvent: function (hookName, values) {
            var kind = eventKinds[hookName];

            return freeze(createObject(kind.prototype, kind.read(values)));
        },
        createDecision: function (hookName, values) {
            var kind = decisionKinds[hookName];

            return freeze(createObject(kind.prototype, kind.read(values)));
        },
        screen: screenObject
    });

    var game = freeze({
        getFrame: function () {
            return core.game.getFrame();
        },
        getMapWidth: function () {
            return core.game.getMapTileWidth() * TILE_SIZE;
        },
        getMapHeight: function () {
            return core.game.getMapTileHeight() * TILE_SIZE;
        },
        getMapTileWidth: function () {
            return core.game.getMapTileWidth();
        },
        getMapTileHeight: function () {
            return core.game.getMapTileHeight();
        },
        getTileset: function () {
            return core.game.getTileset();
        },
        isReadOnly: function () {
            return core.game.isReadOnly();
        },
        print: function (text) {
            core.game.print(String(text));
        },
        getPlayer: function (id) {
            return getPlayer(id);
        },
        getPlayers: function () {
            var all = [];

            for (var id = 0; id < playerCount; id++) {
                all.push(getPlayer(id));
            }

            return all;
        },
        getHumanPlayers: function () {
            return game.getPlayers().filter(function (player) {
                return player.isHuman();
            });
        },
        getUnit: function (id) {
            return getUnit(id);
        },
        getUnits: function () {
            return filterUnits(undefined, Infinity);
        },
        findUnits: function (filter) {
            return filterUnits(filter, Infinity);
        },
        findUnit: function (filter) {
            var found = filterUnits(filter, 1);

            return found.length > 0 ? found[0] : null;
        },
        countUnits: function (filter) {
            return filterUnits(filter, Infinity).length;
        },
        createUnit: function (type, player, position) {
            requireObject(type, unitTypePrototype, 'A unit type');
            requireObject(player, playerPrototype, 'A player');
            requireObject(position, positionPrototype, 'A position');

            return getUnit(core.unit.create(type.id, player.id, position.x, position.y));
        },
        getLocation: function (id) {
            return getLocation(id);
        },
        findLocationByName: function (name) {
            return getLocation(core.location.findByName(String(name)));
        },
        // Calls a function every given number of frames, first that many frames from now.
        every: function (frames, callback) {
            return freeze(createObject(timerPrototype, {id: core.game.every(frames, callback)}));
        },
        // The switches 0 to 255 of the trigger state.
        isSwitchSet: function (index) {
            return core.game.isSwitchSet(index);
        },
        setSwitch: function (index, set) {
            core.game.setSwitch(index, set);
        },
        toggleSwitch: function (index) {
            core.game.toggleSwitch(index);
        },
        // The countdown timer and the elapsed time count game seconds of the trigger clock, one every sixteen frames, not frames.
        getCountdownTimer: function () {
            return core.game.getCountdownTimer();
        },
        setCountdownTimer: function (seconds) {
            core.game.setCountdownTimer(seconds);
        },
        addCountdownTimer: function (seconds) {
            core.game.addCountdownTimer(seconds);
        },
        subtractCountdownTimer: function (seconds) {
            core.game.subtractCountdownTimer(seconds);
        },
        isCountdownTimerPaused: function () {
            return core.game.isCountdownTimerPaused();
        },
        pauseCountdownTimer: function () {
            core.game.setCountdownTimerPaused(true);
        },
        resumeCountdownTimer: function () {
            core.game.setCountdownTimerPaused(false);
        },
        getElapsedTime: function () {
            return core.game.getElapsedTime();
        },
        // The leaderboards of the leaderboard actions with a text label: the goal form with {goal: amount}, Control also {location: a
        // Location}.
        showControlLeaderboard: function (label, unit, options) {
            var operand = readUnitOperand(unit);
            var values = readOptions(options);
            var location = readLocationOption(values);

            if (location === NONE) {
                showLeaderboard(LEADERBOARD_CONTROL, LEADERBOARD_GOAL_CONTROL, operand, label, values);
            } else if (values.goal === undefined) {
                core.leaderboard.showAt(LEADERBOARD_CONTROL_AT_LOCATION, location, operand, 0, String(label));
            } else {
                core.leaderboard.showAt(LEADERBOARD_GOAL_CONTROL_AT_LOCATION, location, operand, values.goal, String(label));
            }
        },
        showResourcesLeaderboard: function (label, resourceType, options) {
            var operand = requireObject(resourceType, resourceTypePrototype, 'A resource type').id;

            showLeaderboard(LEADERBOARD_RESOURCES, LEADERBOARD_GOAL_RESOURCES, operand, label, options);
        },
        showKillsLeaderboard: function (label, unit, options) {
            showLeaderboard(LEADERBOARD_KILLS, LEADERBOARD_GOAL_KILLS, readUnitOperand(unit), label, options);
        },
        showPointsLeaderboard: function (label, scoreType, options) {
            var operand = requireObject(scoreType, scoreTypePrototype, 'A score type').id;

            showLeaderboard(LEADERBOARD_POINTS, LEADERBOARD_GOAL_POINTS, operand, label, options);
        },
        // The Greed leaderboard of the ore and gas of every player, toward a goal.
        showGreedLeaderboard: function (goal) {
            core.leaderboard.showGreed(goal);
        },
        areLeaderboardComputersIncluded: function () {
            return core.leaderboard.areComputersIncluded();
        },
        setLeaderboardComputersIncluded: function (included) {
            core.leaderboard.setComputersIncluded(included);
        }
    });

    freeze(typePrototype);
    freeze(unitTypePrototype);
    freeze(weaponTypePrototype);
    freeze(upgradeTypePrototype);
    freeze(techTypePrototype);
    freeze(orderPrototype);
    freeze(imageTypePrototype);
    freeze(keyPrototype);
    freeze(mouseButtonPrototype);
    freeze(unitGroupPrototype);
    freeze(scoreTypePrototype);
    freeze(resourceTypePrototype);
    freeze(alliancePrototype);
    freeze(positionPrototype);
    freeze(unitPrototype);
    freeze(playerPrototype);
    freeze(locationPrototype);
    freeze(tilePrototype);
    freeze(timerPrototype);
    freeze(resourcePrototype);
    freeze(grpPrototype);
    freeze(animPrototype);
    freeze(imagePrototype);
    freeze(fontPrototype);
    freeze(alignPrototype);
    freeze(graphicsPrototype);
    freeze(consoleControlPrototype);
    freeze(consoleImagePrototype);
    freeze(consoleControlObjectPrototype);
    freeze(channelPrototype);
    freeze(playerValuePrototype);

    for (var eventName in eventKinds) {
        freeze(eventKinds[eventName].prototype);
    }

    for (var decisionName in decisionKinds) {
        freeze(decisionKinds[decisionName].prototype);
    }

    defineGlobal('game', game);
    defineGlobal('Time', Time);
    defineGlobal('Position', Position);
    defineGlobal('UnitType', UnitType);
    defineGlobal('UnitGroup', UnitGroup);
    defineGlobal('WeaponType', WeaponType);
    defineGlobal('FlingyType', FlingyType);
    defineGlobal('SpriteType', SpriteType);
    defineGlobal('ImageType', ImageType);
    defineGlobal('UpgradeType', UpgradeType);
    defineGlobal('TechType', TechType);
    defineGlobal('Order', Order);
    defineGlobal('ScoreType', ScoreType);
    defineGlobal('ResourceType', ResourceType);
    defineGlobal('Alliance', Alliance);
    defineGlobal('Key', Key);
    defineGlobal('MouseButton', MouseButton);
    defineGlobal('Modifier', Modifier);
    defineGlobal('resources', resources);
    defineGlobal('graphics', graphics);
    defineGlobal('tileset', tileset);
    defineGlobal('terrain', terrain);
    defineGlobal('sync', sync);
    defineGlobal('local', local);
    defineGlobal('Color', Color);
    defineGlobal('Font', Font);
    defineGlobal('Align', Align);
    defineGlobal('Graphics', Graphics);
    defineGlobal('ConsoleControl', ConsoleControl);
    defineGlobal('ConsoleImage', ConsoleImage);
    defineGlobal('console', gameConsole);
    defineGlobal('strings', strings);
    defineGlobal('messages', messages);

    return gameFunctions;
});
