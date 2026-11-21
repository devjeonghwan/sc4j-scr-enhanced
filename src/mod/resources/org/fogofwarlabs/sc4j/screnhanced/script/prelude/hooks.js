// The hooks of the upper layer and the entry points the runtime calls. The hooks are the top-level functions of the modules with a known
// name, collected per module once it has loaded; a hook runs in every module that has it, in load order. The event objects wrap the records
// of core.events.getFrameEvents, the decision objects the records of the decide entry point and the local input objects the records of the
// localInput entry point; each object's getRecord() returns its record.
//
// The entry points: frame(frame) calls onStart at the first frame, then the due timers, the channel handlers of the frame's commands, the
// event hooks of the frame's events and onFrame; decide(kind, record) calls the decision hooks; localFrame(frame), render() and
// localInput(record) call the local hooks, and localInput returns true when a hook consumed the input. The runtime's host runs each call of
// a hook, timer or handler (host.call), lists the loaded modules (host.getModules) and learns which decisions have hooks
// (host.setDecisionHooks).
(function (core, prelude) {
    'use strict';

    var RAW_SCALE = prelude.RAW_SCALE;
    var NONE = prelude.NONE;
    var VictoryState = prelude.VictoryState;
    var execution = prelude.execution;
    var createObject = prelude.createObject;
    var isObjectOf = prelude.isObjectOf;
    var requireObject = prelude.requireObject;
    var positionPrototype = prelude.positionPrototype;
    var createPosition = prelude.createPosition;
    var UnitType = prelude.UnitType;
    var WeaponType = prelude.WeaponType;
    var TechType = prelude.TechType;
    var UpgradeType = prelude.UpgradeType;
    var Order = prelude.Order;
    var Key = prelude.Key;
    var MouseButton = prelude.MouseButton;
    var Modifier = prelude.Modifier;
    var EventKind = prelude.EventKind;
    var InputKind = prelude.InputKind;
    var DecisionKind = prelude.DecisionKind;
    var orderPrototype = prelude.orderPrototype;
    var unitPrototype = prelude.unitPrototype;
    var getPlayer = prelude.getPlayer;
    var getUnit = prelude.getUnit;
    var getCapturedUnit = prelude.getCapturedUnit;
    var requireUnitId = prelude.requireUnitId;
    var pollDueTimer = prelude.pollDueTimer;
    var hasChannels = prelude.hasChannels;
    var readChannelMessage = prelude.readChannelMessage;
    var screen = prelude.screen;

    var freeze = Object.freeze;
    var hasOwnProperty = Object.prototype.hasOwnProperty;

    var MAXIMUM_RAW_AMOUNT = 2147483647;
    var START_HOOK = 'onStart';
    var FRAME_HOOK = 'onFrame';
    var LOCAL_FRAME_HOOK = 'onLocalFrame';
    var RENDER_HOOK = 'onRender';
    var EVENT_HOOK_PREFIX = 'on';
    var INPUT_HOOK_PREFIX = 'onLocal';
    var DECISION_HOOK_PREFIX = 'onBefore';
    var TIMER_CALL = 'Timer';
    var CHANNEL_CALL = 'Channel handler';
    var BOOLEAN_FIELDS = {queued: true, missed: true};
    var UNIT_FIELDS = ['frame', 'unit', 'unitType', 'owner'];
    var BULLET_FIELDS = ['frame', 'weapon', 'player', 'source', 'target', 'targetPosition'];
    var LOCAL_KEY_FIELDS = ['key', 'modifiers', 'mousePosition'];
    var LOCAL_BUTTON_FIELDS = ['button', 'modifiers', 'mousePosition'];

    function addMethods(prototype, methods) {
        for (var name in methods) {
            prototype[name] = methods[name];
        }

        return prototype;
    }

    // Creates the prototype of an object over a record: a getter per field (is* for the boolean fields) and getRecord().
    function createPrototype(fields) {
        var prototype = {
            getRecord: function () {
                return this.record;
            }
        };

        fields.forEach(function (field) {
            var name = (BOOLEAN_FIELDS[field] ? 'is' : 'get') + field.charAt(0).toUpperCase() + field.substring(1);

            prototype[name] = function () {
                return this[field];
            };
        });

        return prototype;
    }

    function readUnitFields(record) {
        return {
            frame: record.frame,
            unit: getCapturedUnit(record.unit, record.unitGeneration),
            unitType: UnitType.get(record.unitType),
            owner: getPlayer(record.owner)
        };
    }

    // Reads the unit fields and one more captured unit.
    function readUnitFieldsWith(name) {
        return function (record) {
            var fields = readUnitFields(record);

            fields[name] = getCapturedUnit(record[name], record[name + 'Generation']);

            return fields;
        };
    }

    function readBulletFields(record) {
        return {
            frame: record.frame,
            weapon: WeaponType.get(record.weapon),
            player: getPlayer(record.player),
            source: getCapturedUnit(record.source, record.sourceGeneration),
            target: getCapturedUnit(record.target, record.targetGeneration),
            targetPosition: createPosition(record.targetX, record.targetY),
            missed: record.missed === true
        };
    }

    // The events by the names of their kinds: the fields of their objects and the reading of the fields from a record.
    var EVENTS = {
        UnitCreated: {fields: UNIT_FIELDS, read: readUnitFields},
        UnitCompleted: {fields: UNIT_FIELDS, read: readUnitFields},
        UnitDied: {
            fields: UNIT_FIELDS.concat(['killer', 'killerPlayer', 'weapon']),
            read: function (record) {
                var fields = readUnitFieldsWith('killer')(record);

                fields.killerPlayer = getPlayer(record.killerPlayer);
                fields.weapon = WeaponType.get(record.weapon);

                return fields;
            }
        },
        UnitDamaged: {
            fields: UNIT_FIELDS.concat(['source', 'sourcePlayer', 'weapon', 'amount']),
            read: function (record) {
                var fields = readUnitFieldsWith('source')(record);

                fields.sourcePlayer = getPlayer(record.sourcePlayer);
                fields.weapon = WeaponType.get(record.weapon);
                fields.amount = record.amount / RAW_SCALE;

                return fields;
            }
        },
        OrderIssued: {
            fields: UNIT_FIELDS.concat(['order', 'target', 'targetPosition', 'buildType', 'queued']),
            read: function (record) {
                var fields = readUnitFieldsWith('target')(record);

                fields.order = Order.get(record.order);
                fields.targetPosition = createPosition(record.targetX, record.targetY);
                fields.buildType = UnitType.get(record.buildType);
                fields.queued = record.queued;

                return fields;
            }
        },
        TargetAcquired: {fields: UNIT_FIELDS.concat(['target']), read: readUnitFieldsWith('target')},
        UnitTrained: {fields: UNIT_FIELDS.concat(['producer']), read: readUnitFieldsWith('producer')},
        ResearchCompleted: {
            fields: UNIT_FIELDS.concat(['tech']),
            read: function (record) {
                var fields = readUnitFields(record);

                fields.tech = TechType.get(record.tech);

                return fields;
            }
        },
        UpgradeCompleted: {
            fields: UNIT_FIELDS.concat(['upgrade', 'level']),
            read: function (record) {
                var fields = readUnitFields(record);

                fields.upgrade = UpgradeType.get(record.upgrade);
                fields.level = record.level;

                return fields;
            }
        },
        SpellCast: {
            fields: UNIT_FIELDS.concat(['tech', 'target', 'targetPosition']),
            read: function (record) {
                var fields = readUnitFieldsWith('target')(record);

                fields.tech = TechType.get(record.tech);
                fields.targetPosition = createPosition(record.targetX, record.targetY);

                return fields;
            }
        },
        OwnerChanged: {
            fields: UNIT_FIELDS.concat(['previousOwner']),
            read: function (record) {
                var fields = readUnitFields(record);

                fields.previousOwner = getPlayer(record.previousOwner);

                return fields;
            }
        },
        UnitLoaded: {fields: UNIT_FIELDS.concat(['transport']), read: readUnitFieldsWith('transport')},
        UnitUnloaded: {fields: UNIT_FIELDS.concat(['transport']), read: readUnitFieldsWith('transport')},
        BulletCreated: {fields: BULLET_FIELDS, read: readBulletFields},
        BulletHit: {fields: BULLET_FIELDS.concat(['missed']), read: readBulletFields},
        PlayerLeft: {
            fields: ['frame', 'player', 'reason'],
            read: function (record) {
                return {frame: record.frame, player: getPlayer(record.player), reason: record.reason};
            }
        },
        PlayerResult: {
            fields: ['frame', 'player', 'result'],
            read: function (record) {
                return {frame: record.frame, player: getPlayer(record.player), result: record.result};
            },
            methods: {
                isVictory: function () {
                    return this.result === VictoryState.Victory || this.result === VictoryState.Secured;
                },
                isDefeat: function () {
                    return this.result === VictoryState.Defeat;
                },
                isDraw: function () {
                    return this.result === VictoryState.Draw;
                },
                isDropped: function () {
                    return this.result === VictoryState.Dropped;
                }
            }
        }
    };

    // The local inputs: the key or button, the modifier mask and the map position under the pointer, or null while unknown; the chat line.
    // consume() keeps the press or the chat line from the game: it marks the record consumed, which the entry point returns.
    var consumeMethods = {
        consume: function () {
            this.record.consumed = true;
        },
        isConsumed: function () {
            return this.record.consumed === true;
        }
    };
    var modifierMethods = {
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

    function readInputFields(record) {
        return {
            modifiers: record.modifiers,
            mousePosition: record.positioned ? createPosition(record.x, record.y) : null
        };
    }

    function readKeyFields(record) {
        var fields = readInputFields(record);

        fields.key = Key.get(record.key);

        return fields;
    }

    function readButtonFields(record) {
        var fields = readInputFields(record);

        fields.button = MouseButton.get(record.button);

        return fields;
    }

    var INPUTS = {
        KeyDown: {fields: LOCAL_KEY_FIELDS, read: readKeyFields, methods: modifierMethods},
        KeyUp: {fields: LOCAL_KEY_FIELDS, read: readKeyFields, methods: modifierMethods},
        MouseDown: {fields: LOCAL_BUTTON_FIELDS, read: readButtonFields, methods: modifierMethods},
        MouseUp: {fields: LOCAL_BUTTON_FIELDS, read: readButtonFields, methods: modifierMethods},
        Chat: {
            fields: ['text'],
            read: function (record) {
                return {text: String(record.text)};
            }
        }
    };

    // The decisions: the read-only fields of their objects, the methods over the fields a hook may change in the record, and the fields that
    // a hook that returns hands to the next hook and to the game.
    var DECISIONS = {
        Damage: {
            fields: ['target', 'targetType', 'targetOwner', 'source', 'sourcePlayer', 'weapon'],
            read: function (record) {
                return {
                    target: getCapturedUnit(record.unit, record.unitGeneration),
                    targetType: UnitType.get(record.unitType),
                    targetOwner: getPlayer(record.owner),
                    source: getCapturedUnit(record.source, record.sourceGeneration),
                    sourcePlayer: getPlayer(record.sourcePlayer),
                    weapon: WeaponType.get(record.weapon)
                };
            },
            methods: {
                getAmount: function () {
                    return this.record.amount / RAW_SCALE;
                },
                setAmount: function (amount) {
                    var raw = Math.round(amount * RAW_SCALE);

                    if (!(raw >= 0 && raw <= MAXIMUM_RAW_AMOUNT)) {
                        throw new RangeError('The damage ' + amount + ' is not a damage amount.');
                    }

                    this.record.amount = raw;
                },
                cancel: function () {
                    this.record.cancelled = true;
                },
                isCancelled: function () {
                    return this.record.cancelled;
                }
            },
            changes: ['amount', 'cancelled']
        },
        Order: {
            fields: ['unit', 'unitType', 'owner', 'buildType', 'queued'],
            read: function (record) {
                var fields = readUnitFields(record);

                delete fields.frame;
                fields.buildType = UnitType.get(record.buildType);
                fields.queued = record.queued;

                return fields;
            },
            methods: {
                getOrder: function () {
                    return Order.get(this.record.order);
                },
                setOrder: function (order) {
                    this.record.order = requireObject(order, orderPrototype, 'An order').id;
                },
                getTarget: function () {
                    return getCapturedUnit(this.record.target, this.record.targetGeneration);
                },
                getTargetPosition: function () {
                    return createPosition(this.record.targetX, this.record.targetY);
                },
                // Sets a target unit (and its position), a position without a target unit, or no target unit for null.
                setTarget: function (target) {
                    if (target === null || target === undefined) {
                        this.record.target = NONE;
                        this.record.targetGeneration = NONE;
                    } else if (isObjectOf(target, unitPrototype)) {
                        var id = requireUnitId(target);

                        this.record.target = id;
                        this.record.targetGeneration = target.generation;
                        this.record.targetX = core.unit.getX(id);
                        this.record.targetY = core.unit.getY(id);
                    } else {
                        requireObject(target, positionPrototype, 'A unit or a position');

                        this.record.target = NONE;
                        this.record.targetGeneration = NONE;
                        this.record.targetX = target.x;
                        this.record.targetY = target.y;
                    }
                },
                reject: function () {
                    this.record.rejected = true;
                },
                isRejected: function () {
                    return this.record.rejected;
                }
            },
            changes: ['order', 'target', 'targetGeneration', 'targetX', 'targetY', 'rejected']
        },
        TargetAcquired: {
            fields: ['unit', 'unitType', 'owner'],
            read: function (record) {
                var fields = readUnitFields(record);

                delete fields.frame;

                return fields;
            },
            methods: {
                getTarget: function () {
                    return getCapturedUnit(this.record.target, this.record.targetGeneration);
                },
                // Sets the target unit, or no target for null.
                setTarget: function (target) {
                    var unit = target === null || target === undefined ? null : getUnit(requireUnitId(target));

                    this.record.target = unit === null ? NONE : unit.id;
                    this.record.targetGeneration = unit === null ? NONE : unit.generation;
                }
            },
            changes: ['target', 'targetGeneration']
        }
    };

    // Lists the kinds of a constant table by id with the hook name, the prototype, the field reading and the changed fields of each kind
    // that has a definition.
    function createKinds(table, definitions, hookPrefix, extraMethods) {
        var kinds = [];

        for (var id = 0; id < table.getCount(); id++) {
            var name = table.get(id).getName();
            var definition = definitions[name];

            if (definition === undefined) {
                continue;
            }

            var prototype = createPrototype(definition.fields);

            addMethods(prototype, definition.methods || {});
            addMethods(prototype, extraMethods);
            kinds[id] = {hookName: hookPrefix + name, prototype: freeze(prototype), read: definition.read, changes: definition.changes};
        }

        return kinds;
    }

    var eventKinds = createKinds(EventKind, EVENTS, EVENT_HOOK_PREFIX, {});
    var inputKinds = createKinds(InputKind, INPUTS, INPUT_HOOK_PREFIX, consumeMethods);
    var decisionKinds = createKinds(DecisionKind, DECISIONS, DECISION_HOOK_PREFIX, {});

    // The names of every hook, in the order the runtime knows them.
    function collectHookNames() {
        var names = [START_HOOK, FRAME_HOOK];

        [eventKinds, inputKinds].forEach(function (kinds) {
            kinds.forEach(function (kind) {
                names.push(kind.hookName);
            });
        });
        names.push(LOCAL_FRAME_HOOK, RENDER_HOOK);
        decisionKinds.forEach(function (kind) {
            names.push(kind.hookName);
        });

        return names;
    }

    var HOOK_NAMES = collectHookNames();

    // Creates the object over a record of a kind, with the record as its hidden field.
    function createKindObject(kind, record) {
        var fields = kind.read(record);

        fields.record = record;

        return freeze(createObject(kind.prototype, fields));
    }

    function createEvent(record) {
        return createKindObject(eventKinds[record.kind], record);
    }

    function createInputEvent(record) {
        return createKindObject(inputKinds[record.kind], record);
    }

    function createDecision(kind, record) {
        return createKindObject(decisionKinds[kind], record);
    }

    // Collects the hooks of a module scope: its own top-level functions with a hook name.
    function collectHooks(scope) {
        var hooks = {};

        HOOK_NAMES.forEach(function (name) {
            if (hasOwnProperty.call(scope, name) && typeof scope[name] === 'function') {
                hooks[name] = scope[name];
            }
        });

        return hooks;
    }

    function copyRecord(record) {
        var copy = {};

        for (var key in record) {
            copy[key] = record[key];
        }

        return copy;
    }

    // Creates the entry points over the runtime's host.
    function createEntryPoints(host) {
        var modules = [];
        var hooked = {};
        var eventHooked = false;
        var started = false;

        // Returns the loaded modules in load order, with their hooks; when a module has loaded since the last call, collects its hooks and
        // tells the host which decisions have hooks.
        function getModules() {
            var current = host.getModules();

            if (current === modules) {
                return modules;
            }

            modules = current;
            hooked = {};

            for (var index = 0; index < modules.length; index++) {
                var module = modules[index];

                if (module.hooks === undefined) {
                    module.hooks = collectHooks(module.scope);
                }

                for (var name in module.hooks) {
                    hooked[name] = true;
                }
            }

            eventHooked = eventKinds.some(function (kind) {
                return hooked[kind.hookName] === true;
            });
            host.setDecisionHooks(decisionKinds.map(function (kind) {
                return hooked[kind.hookName] === true;
            }));

            return modules;
        }

        // Calls a hook in every module that has it, in load order; a module that loads during the calls joins from the next call.
        function callHooks(name, argumentList) {
            var current = getModules();

            if (hooked[name] !== true) {
                return;
            }

            for (var index = 0; index < current.length; index++) {
                var hook = current[index].hooks[name];

                if (hook !== undefined) {
                    host.call(name, hook, argumentList, current[index]);
                }
            }
        }

        function frame(frameNumber) {
            execution.local = false;

            if (!started) {
                started = true;
                callHooks(START_HOOK, []);
            }

            for (var timer = pollDueTimer(frameNumber); timer !== null; timer = pollDueTimer(frameNumber)) {
                host.call(TIMER_CALL, timer, []);
            }

            if (hasChannels()) {
                var commands = core.command.getExecuted();

                for (var commandIndex = 0; commandIndex < commands.length; commandIndex++) {
                    var message = readChannelMessage(commands[commandIndex]);

                    if (message !== null) {
                        host.call(CHANNEL_CALL, message.handler, [message.player, message.value]);
                    }
                }
            }

            getModules();

            if (eventHooked) {
                var records = core.events.getFrameEvents();

                for (var eventIndex = 0; eventIndex < records.length; eventIndex++) {
                    var kind = eventKinds[records[eventIndex].kind];

                    if (kind !== undefined && hooked[kind.hookName] === true) {
                        callHooks(kind.hookName, [createEvent(records[eventIndex])]);
                    }
                }
            }

            callHooks(FRAME_HOOK, [frameNumber]);
        }

        // Calls the decision hooks in load order, each with a decision object over a copy of the record; the changes of a hook that returns
        // go into the record, which the next hook and the game read.
        function decide(kind, record) {
            execution.local = false;

            var decisionKind = decisionKinds[kind];
            var current = getModules();

            if (decisionKind === undefined || hooked[decisionKind.hookName] !== true) {
                return;
            }

            for (var index = 0; index < current.length; index++) {
                var hook = current[index].hooks[decisionKind.hookName];

                if (hook === undefined) {
                    continue;
                }

                var working = copyRecord(record);

                if (host.call(decisionKind.hookName, hook, [createDecision(kind, working)], current[index])) {
                    decisionKind.changes.forEach(function (field) {
                        record[field] = working[field];
                    });
                }
            }
        }

        function localFrame(frameNumber) {
            execution.local = true;
            callHooks(LOCAL_FRAME_HOOK, [frameNumber]);
        }

        function render() {
            execution.local = true;
            callHooks(RENDER_HOOK, [screen]);
        }

        function localInput(record) {
            execution.local = true;

            var inputKind = inputKinds[record.kind];

            getModules();

            if (inputKind !== undefined && hooked[inputKind.hookName] === true) {
                callHooks(inputKind.hookName, [createInputEvent(record)]);
            }

            return record.consumed === true;
        }

        return freeze({frame: frame, localFrame: localFrame, render: render, decide: decide, localInput: localInput});
    }

    return {
        exports: {},
        shared: {
            hookNames: freeze(HOOK_NAMES.slice()),
            createEvent: createEvent,
            createInputEvent: createInputEvent,
            createDecision: createDecision,
            createEntryPoints: createEntryPoints
        }
    };
});
