// The upper layer of the SC:R Enhanced Script game API over the lower layer core: the globals game, Time, Position, UnitType, WeaponType,
// UpgradeType, TechType and Order, and the Unit, Player and Location objects they return. One unit, player or location is always the same
// object; a unit object stays with its unit, and exists() becomes false once the unit is gone. Hit points, shields and energy are the shown
// values (core's raw values divided by 256; no shields or energy for a unit without them), supply the shown supply (core's half units
// divided by 2). The event and decision objects of the hooks have getters for their captured values; a decision object also has the
// methods that decide. The prelude returns the functions that make these objects to the game.
(function (global, core) {
    'use strict';

    var RAW_SCALE = 256;
    var SUPPLY_SCALE = 2;
    var NONE = -1;
    var TILE_SIZE = 32;
    var MILLISECONDS_PER_SECOND = 1000;
    var SECONDS_PER_MINUTE = 60;
    var MAXIMUM_RAW_AMOUNT = 2147483647;
    // The results of core.player.endGame and of the result events: the victory states.
    var RESULT_DROPPED = 1;
    var RESULT_DEFEAT = 2;
    var RESULT_VICTORY = 3;
    var RESULT_SECURED = 4;
    var RESULT_DRAW = 5;

    var round = Math.round;
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
    var upgradeTypePrototype = create(typePrototype);
    var techTypePrototype = create(typePrototype);
    var orderPrototype = create(typePrototype);

    unitTypePrototype.getMaxHp = function () {
        return core.unitType.getMaxHp(this.id) / RAW_SCALE;
    };
    unitTypePrototype.setMaxHp = function (hp) {
        core.unitType.setMaxHp(this.id, round(hp * RAW_SCALE));
    };
    unitTypePrototype.getMaxShields = function () {
        return core.unitType.getMaxShields(this.id);
    };
    unitTypePrototype.setMaxShields = function (shields) {
        core.unitType.setMaxShields(this.id, shields);
    };
    unitTypePrototype.getArmor = function () {
        return core.unitType.getArmor(this.id);
    };
    unitTypePrototype.setArmor = function (armor) {
        core.unitType.setArmor(this.id, armor);
    };
    unitTypePrototype.getGroundWeapon = function () {
        return WeaponType.get(core.unitType.getGroundWeapon(this.id));
    };
    unitTypePrototype.getAirWeapon = function () {
        return WeaponType.get(core.unitType.getAirWeapon(this.id));
    };

    weaponTypePrototype.getDamage = function () {
        return core.weaponType.getDamage(this.id);
    };
    weaponTypePrototype.setDamage = function (damage) {
        core.weaponType.setDamage(this.id, damage);
    };
    weaponTypePrototype.getDamageBonus = function () {
        return core.weaponType.getDamageBonus(this.id);
    };
    weaponTypePrototype.setDamageBonus = function (bonus) {
        core.weaponType.setDamageBonus(this.id, bonus);
    };

    var UnitType = createTypeTable(core.unitType, unitTypePrototype);
    var WeaponType = createTypeTable(core.weaponType, weaponTypePrototype);
    var UpgradeType = createTypeTable(core.upgradeType, upgradeTypePrototype);
    var TechType = createTypeTable(core.techType, techTypePrototype);
    var Order = createTypeTable(core.orderType, orderPrototype);

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

    var Time = freeze({
        seconds: function (seconds) {
            return round(seconds * MILLISECONDS_PER_SECOND / core.game.getFrameDuration());
        },
        minutes: function (minutes) {
            return Time.seconds(minutes * SECONDS_PER_MINUTE);
        }
    });

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
        setInvincible: function (invincible) {
            core.unit.setInvincible(requireUnitId(this), invincible);
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
        toString: function () {
            return 'Unit ' + this.id;
        }
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
        getUnits: function () {
            return game.findUnits({owner: this});
        },
        print: function (text) {
            core.player.print(this.id, String(text));
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

    // The functions the game calls, which the scripts do not see.
    var gameFunctions = freeze({
        createEvent: function (hookName, values) {
            var kind = eventKinds[hookName];

            return freeze(createObject(kind.prototype, kind.read(values)));
        },
        createDecision: function (hookName, values) {
            var kind = decisionKinds[hookName];

            return freeze(createObject(kind.prototype, kind.read(values)));
        }
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
        }
    });

    freeze(typePrototype);
    freeze(unitTypePrototype);
    freeze(weaponTypePrototype);
    freeze(upgradeTypePrototype);
    freeze(techTypePrototype);
    freeze(orderPrototype);
    freeze(positionPrototype);
    freeze(unitPrototype);
    freeze(playerPrototype);
    freeze(locationPrototype);
    freeze(timerPrototype);

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
    defineGlobal('WeaponType', WeaponType);
    defineGlobal('UpgradeType', UpgradeType);
    defineGlobal('TechType', TechType);
    defineGlobal('Order', Order);

    return gameFunctions;
});
