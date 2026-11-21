// The presentation of the upper layer: the globals resources, graphics, ConsoleControl, ConsoleImage, console, strings and messages, and the
// Grp, Anim, Image and console control objects they return. Shared code changes the presentation on every peer, local code only on the local
// machine.
(function (core, prelude) {
    'use strict';

    var NONE = prelude.NONE;
    var createObject = prelude.createObject;
    var requireObject = prelude.requireObject;
    var typePrototype = prelude.typePrototype;
    var createConstantTable = prelude.createConstantTable;
    var imageTypePrototype = prelude.imageTypePrototype;
    var resourceTypePrototype = prelude.resourceTypePrototype;
    var ResourceType = prelude.ResourceType;
    var requireUnitId = prelude.requireUnitId;

    var create = Object.create;
    var freeze = Object.freeze;

    // The resource icon frames of the console: minerals, then gas and supply for the Zerg, Terran and Protoss consoles.
    var MINERAL_ICONS = [0];
    var GAS_ICONS = [1, 2, 3];
    var SUPPLY_ICONS = [4, 5, 6];

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

    var consoleControlPrototype = create(typePrototype);
    var consoleImagePrototype = create(typePrototype);
    var ConsoleControl = createConstantTable(consoleControlPrototype,
                                             ['CommandCard', 'Selection', 'Resources', 'Portrait', 'Minimap', 'Menu', 'TextBox'], 7, 'Control ');
    // The console art files of the shown race console: SD frame, HD/HD2 pieces, time and APM plates, portrait overlay.
    var ConsoleImage = createConstantTable(consoleImagePrototype,
                                           ['Console', 'ConsoleLeft', 'ConsoleCenter', 'ConsoleRight', 'ConsoleTime', 'ConsoleApm', 'Conover'], 7,
                                           'Image ');
    var consoleControlObjects = [];

    // A console control: one of the console dialogs, placed by its top left in the classic 640 by 480 screen.
    var consoleControlObjectPrototype = {
        getControl: function () {
            return this.control;
        },
        setPosition: function (x, y) {
            core.console.setDialogPosition(this.control.id, Math.round(x), Math.round(y));
        },
        setVisible: function (visible) {
            core.console.setDialogVisible(this.control.id, visible === true);
        },
        isVisible: function () {
            return core.console.isDialogVisible(this.control.id);
        }
    };

    // The resource icon frames of a resource type.
    function readResourceIcons(resourceType) {
        var id = requireObject(resourceType, resourceTypePrototype, 'A resource type').id;

        if (id === ResourceType.Minerals.id) {
            return MINERAL_ICONS;
        }

        if (id === ResourceType.Gas.id) {
            return GAS_ICONS;
        }

        if (id === ResourceType.Supply.id) {
            return SUPPLY_ICONS;
        }

        throw new TypeError('The resource type ' + resourceType + ' has no console icon.');
    }

    // The console. A setting wins over what EUD memory gives for the same item; EUD memory still drives the items the script never set.
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

    [resourcePrototype, grpPrototype, animPrototype, imagePrototype, consoleControlPrototype, consoleImagePrototype,
     consoleControlObjectPrototype].forEach(freeze);

    return {
        exports: {
            resources: resources,
            graphics: graphics,
            ConsoleControl: ConsoleControl,
            ConsoleImage: ConsoleImage,
            console: gameConsole,
            strings: strings,
            messages: messages
        },
        shared: {
            imagePrototype: imagePrototype
        }
    };
});
