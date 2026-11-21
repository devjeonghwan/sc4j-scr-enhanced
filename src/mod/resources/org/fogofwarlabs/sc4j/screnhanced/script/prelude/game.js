// The game of the upper layer: the globals game, tileset and terrain, and the Unit, Player, Location, Tile and Timer objects they return. One
// unit, player, location or tile is always the same object; a unit object stays with its unit, and exists() becomes false once the unit is
// gone. Hit points, shields and energy are the shown values (core's raw values divided by 256; no shields or energy for a unit without them),
// supply the shown supply (core's half units divided by 2). The timers of game.every are counted here, by frames.
(function (core, prelude) {
    'use strict';

    var RAW_SCALE = prelude.RAW_SCALE;
    var SUPPLY_SCALE = prelude.SUPPLY_SCALE;
    var NONE = prelude.NONE;
    var TILE_SIZE = prelude.TILE_SIZE;
    var VictoryState = prelude.VictoryState;
    var requireShared = prelude.requireShared;
    var createObject = prelude.createObject;
    var isObjectOf = prelude.isObjectOf;
    var requireObject = prelude.requireObject;
    var readOptions = prelude.readOptions;
    var positionPrototype = prelude.positionPrototype;
    var createPosition = prelude.createPosition;
    var UnitType = prelude.UnitType;
    var TechType = prelude.TechType;
    var UpgradeType = prelude.UpgradeType;
    var ImageType = prelude.ImageType;
    var Order = prelude.Order;
    var Alliance = prelude.Alliance;
    var unitTypePrototype = prelude.unitTypePrototype;
    var upgradeTypePrototype = prelude.upgradeTypePrototype;
    var techTypePrototype = prelude.techTypePrototype;
    var orderPrototype = prelude.orderPrototype;
    var scoreTypePrototype = prelude.scoreTypePrototype;
    var resourceTypePrototype = prelude.resourceTypePrototype;
    var alliancePrototype = prelude.alliancePrototype;
    var readUnitOperand = prelude.readUnitOperand;

    var freeze = Object.freeze;
    var round = Math.round;
    var floor = Math.floor;
    var ceil = Math.ceil;
    var min = Math.min;
    var max = Math.max;

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
    // An AI script id: four characters, the first one in the low byte.
    var AI_SCRIPT_LENGTH = 4;
    var BITS_PER_CHARACTER = 8;
    var LAST_SCRIPT_CHARACTER = 0xFF;
    var TILES_PER_GROUP = 16;
    var TILE_GROUP_SHIFT = 4;
    // The range of a timer interval: a signed 32-bit integer.
    var MINIMUM_INTEGER = -2147483648;
    var MAXIMUM_INTEGER = 2147483647;

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

    // Returns the object of a unit that a record captured: the unit's own object while its slot holds the same unit, otherwise an object of
    // the gone unit whose exists() is false.
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

    // The shown maximum shields of a unit: those of its type in units.dat when the type has shields.
    function getMaxShields(id) {
        var type = core.unit.getType(id);

        return core.dat.units.getShieldEnabled(type) !== 0 ? core.dat.units.getShieldAmount(type) : 0;
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
        // The hit points of the unit's type in units.dat.
        getMaxHp: function () {
            return core.dat.units.getHitPoints(core.unit.getType(requireUnitId(this))) / RAW_SCALE;
        },
        getShields: function () {
            var id = requireUnitId(this);

            return getMaxShields(id) === 0 ? 0 : core.unit.getShields(id) / RAW_SCALE;
        },
        setShields: function (shields) {
            return core.unit.setShields(requireUnitId(this), round(shields * RAW_SCALE));
        },
        getMaxShields: function () {
            return getMaxShields(requireUnitId(this));
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
            return core.player.endGame(this.id, VictoryState.Victory);
        },
        lose: function () {
            return core.player.endGame(this.id, VictoryState.Defeat);
        },
        draw: function () {
            return core.player.endGame(this.id, VictoryState.Draw);
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
        // Centers the screen on a position when the player is the local player; the screens of the other peers stay.
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

    // The timers of game.every: a timer runs first at the frame its interval after the game frame it was added at, then every interval; the
    // timers of a frame run in the order of their due frames, then of their adding. Only shared code adds and cancels timers.
    var timers = [];
    var nextTimerId = 1;

    var timerPrototype = {
        getId: function () {
            return this.id;
        },
        // Cancels the timer; false if it was cancelled already.
        cancel: function () {
            requireShared('Local code cannot cancel a timer.');

            for (var index = 0; index < timers.length; index++) {
                if (timers[index].id === this.id) {
                    timers.splice(index, 1);

                    return true;
                }
            }

            return false;
        }
    };

    function addTimer(frames, callback) {
        requireShared('Local code cannot add a timer.');

        if (typeof frames !== 'number' || frames !== floor(frames) || frames < MINIMUM_INTEGER || frames > MAXIMUM_INTEGER) {
            throw new TypeError('The timer interval is an integer, not ' + frames + '.');
        }

        if (typeof callback !== 'function') {
            throw new TypeError('A timer function is needed, not ' + callback + '.');
        }

        if (frames < 1) {
            throw new RangeError('The timer interval ' + frames + ' is not positive.');
        }

        var timer = {id: nextTimerId++, interval: frames, callback: callback, dueFrame: core.game.getFrame() + frames};

        timers.push(timer);

        return freeze(createObject(timerPrototype, {id: timer.id}));
    }

    // Takes the next due timer of a frame: the timer with the earliest due frame up to the frame, the earliest added on a tie. The timer
    // becomes due again its interval after the frame. Returns its function, or null when no timer is due.
    function pollDueTimer(frame) {
        var due = null;

        for (var index = 0; index < timers.length; index++) {
            var timer = timers[index];

            if (timer.dueFrame <= frame && (due === null || timer.dueFrame < due.dueFrame)) {
                due = timer;
            }
        }

        if (due === null) {
            return null;
        }

        due.dueFrame = frame + due.interval;

        return due.callback;
    }

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
        // Calls a function every given number of frames, first that many frames from now; only in shared code.
        every: function (frames, callback) {
            return addTimer(frames, callback);
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

    [unitPrototype, playerPrototype, locationPrototype, tilePrototype, timerPrototype].forEach(freeze);

    return {
        exports: {
            game: game,
            tileset: tileset,
            terrain: terrain
        },
        shared: {
            unitPrototype: unitPrototype,
            playerPrototype: playerPrototype,
            getPlayer: getPlayer,
            getUnit: getUnit,
            getCapturedUnit: getCapturedUnit,
            toUnits: toUnits,
            requireUnitId: requireUnitId,
            pollDueTimer: pollDueTimer
        }
    };
});
