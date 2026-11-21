// The channels of the upper layer: the global sync and the Channel and PlayerValue objects it returns, over core.command. A channel sends a
// value as the command {c: channel id, v: value}; when the command runs, every peer and every replay calls the channel's handler with the
// sending player and the value. Commands of another shape belong to no channel and are left alone.
(function (core, prelude) {
    'use strict';

    var NONE = prelude.NONE;
    var requireShared = prelude.requireShared;
    var createObject = prelude.createObject;
    var requireObject = prelude.requireObject;
    var playerPrototype = prelude.playerPrototype;
    var getPlayer = prelude.getPlayer;

    var freeze = Object.freeze;
    var hasOwnProperty = Object.prototype.hasOwnProperty;
    var getPrototypeOf = Object.getPrototypeOf;

    var CHANNEL_KEY = 'c';
    var VALUE_KEY = 'v';
    var ENVELOPE_KEY_COUNT = 2;

    // The handlers by channel id, in the order shared code created the channels, the same on every peer.
    var handlers = [];

    var channelPrototype = {
        getId: function () {
            return this.id;
        },
        // Sends a value to the handler on every peer: the local player submits the command; a replay and an observer submit nothing.
        send: function (value) {
            var envelope = {};

            envelope[CHANNEL_KEY] = this.id;
            envelope[VALUE_KEY] = value;
            core.command.submit(envelope);
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
        // Creates a channel; only in shared code, so that every peer has the same channels.
        createChannel: function (handler) {
            if (typeof handler !== 'function') {
                throw new TypeError('A channel handler function is needed, not ' + handler + '.');
            }

            requireShared('Local code cannot create a channel.');
            handlers.push(handler);

            return freeze(createObject(channelPrototype, {id: handlers.length - 1}));
        },
        // Creates a value per player over a new channel; only in shared code, as createChannel.
        createPlayerValue: function (initial) {
            var values = [];
            var assigned = [];
            var channel = sync.createChannel(function (player, message) {
                if (!Array.isArray(message) || message.length !== 2) {
                    return;
                }

                var id = message[0] === NONE ? player.id : message[0];

                if (typeof id === 'number' && id === Math.floor(id) && getPlayer(id) !== null) {
                    values[id] = message[1];
                    assigned[id] = true;
                }
            });

            return freeze(createObject(playerValuePrototype, {channel: channel, initial: initial, values: values, assigned: assigned}));
        }
    });

    function hasChannels() {
        return handlers.length > 0;
    }

    // Reads a command of core.command.getExecuted as a channel message: {handler, player, value} for an envelope {c, v} of a created channel,
    // null for any other command.
    function readChannelMessage(command) {
        var envelope = command.value;

        if (envelope === null || typeof envelope !== 'object' || getPrototypeOf(envelope) !== Object.prototype ||
            Object.keys(envelope).length !== ENVELOPE_KEY_COUNT || !hasOwnProperty.call(envelope, CHANNEL_KEY) ||
            !hasOwnProperty.call(envelope, VALUE_KEY)) {
            return null;
        }

        var channel = envelope[CHANNEL_KEY];

        if (typeof channel !== 'number' || channel !== Math.floor(channel) || channel < 0 || channel >= handlers.length) {
            return null;
        }

        return {handler: handlers[channel], player: getPlayer(command.player), value: envelope[VALUE_KEY]};
    }

    freeze(channelPrototype);
    freeze(playerValuePrototype);

    return {
        exports: {
            sync: sync
        },
        shared: {
            hasChannels: hasChannels,
            readChannelMessage: readChannelMessage
        }
    };
});
