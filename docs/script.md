# Enhanced Script

**English** | [한국어](script.ko.md)

How to write map scripts for SC:R Enhanced. The full API reference is the declaration files [`scr-enhanced.d.ts`](../src/mod/resources/org/fogofwarlabs/sc4j/screnhanced/script/scr-enhanced.d.ts) and [`core.d.ts`](../src/mod/resources/org/fogofwarlabs/sc4j/screnhanced/script/core.d.ts); `init` copies both into every new script folder.

- [1. What it is](#1-what-it-is)
- [2. Getting started](#2-getting-started)
- [3. Shared code and local code](#3-shared-code-and-local-code)
- [4. The API at a glance](#4-the-api-at-a-glance)
- [5. Examples](#5-examples)
- [6. Map protection](#6-map-protection)
- [7. Limits](#7-limits)

## 1. What it is

A map script is JavaScript stored inside the map file. SC:R Enhanced runs it with [Rhino](https://github.com/mozilla/rhino) 1.8.1 in interpreted mode, at the newest ECMAScript level Rhino supports.

- The map file keeps its own data as it is; the script package (the manifest, the scripts and the files of `resources/`) is stored after it.
- The script runs on every player's game in lockstep: the same code, at the same frame, with the same results. Every player of a game runs SC:R Enhanced.
- A replay holds the scripts and plays them again. It reads the resources from the map: keep the map in `<install>/userdata/mods/scr-enhanced/maps/`.
- A game with a script cannot be saved.

## 2. Getting started

### 2.1 The script tool

The script tool is the tool `script` of the mod `scr-enhanced`. Run it with the SC4J install's `SC4J.bat` (Linux `SC4J`, Windows console `SC4J-cli.exe`):

```powershell
SC4J.bat tool scr-enhanced script <command> [arguments]
```

Relative paths are relative to the current folder.

| Command | What it does |
|---|---|
| `init <folder>` | writes a new script folder; the folder must be absent or empty |
| `check <folder>` | prints the errors and warnings of a script folder |
| `run <map> --script <folder> [--melee] [--watch]` | packs the folder into a copy of the map and starts SC:R Enhanced on it |
| `pack <map> --script <folder> -o <output>` | writes the map with the script package to `<output>`; an old package is replaced |
| `pack <map> --script <folder> -o <output> --protect` | the same, as a protected map (chapter 6) |
| `unpack <map> -o <folder>` | writes the script package of a map to a folder that is absent or empty |
| `trace <source map> --at <script>:<line>` | prints the original line of a line of a protected script |
| `trace <source map> --log <file>` | prints a file, such as a log, with the original lines |

`run` starts a Use Map Settings game, or with `--melee` a melee game against one computer of a random race. The copy of the map goes to `<install>/userdata/mods/scr-enhanced/script-run/`; the source map stays as it is. With `--watch` a change of the folder is packed again and loads when the map starts again. The command ends when the game ends; `--install <folder>` names another SC4J install.

In a clone of this repository the same commands run with Gradle, paths relative to the repository root:

```powershell
.\gradlew.bat runScriptTool --args="init my-map"
```

### 2.2 A script folder

```powershell
SC4J.bat tool scr-enhanced script init my-map
```

| File | Content |
|---|---|
| `manifest.json` | `{"name": "my-map", "main": "main.js", "api": "1"}`: the package name (the folder name), the script that runs first, the script API version |
| `main.js` | the main script, an example with `onStart` and `onFrame` |
| `jsconfig.json` | editor settings: only the declaration files, scripts checked, every file its own module |
| `scr-enhanced.d.ts` | declarations of the upper API, the runtime and the standard objects |
| `core.d.ts` | declarations of the lower API `core` |
| `resources/` | files the script reads: GRP, anim, PNG and DDS |

Only `manifest.json`, the `.js` files outside `resources/` and the files under `resources/` go into the map.

### 2.3 Editor completion

Open the folder in an editor with TypeScript-based JavaScript support, such as Visual Studio Code. `jsconfig.json` makes it complete and check the scripts against `scr-enhanced.d.ts` and `core.d.ts`, without browser or Node.js globals.

Hooks are plain functions, so give their parameter a type with JSDoc:

```js
/** @param {UnitDiedEvent} event */
function onUnitDied(event) {
    log.info(event.getUnitType().getName());
}
```

### 2.4 Modules

Each `.js` file is a module with its own top level. `require` loads another script once and returns its `module.exports`; `./` and `../` are relative to the requiring script, other ids relative to the folder, and `.js` may be left out.

```js
// bonus.js
exports.give = function (player, amount) {
    player.addMinerals(amount);
};
```

```js
// main.js
const bonus = require('./bonus');

function onStart() {
    game.players.getHumans().forEach((player) => bonus.give(player, 100));
}
```

Every module can define hooks; each hook runs in every module that has it, in load order. Call `require` in shared code (chapter 3): local code cannot load a new module.

### 2.5 Logs

`log.info`, `log.warn` and `log.error` write to the SC:R Enhanced log `<install>/userdata/logs/scr-enhanced/sc4j.log`, prefixed with the module name. Script errors go there too, with their location (`main.js:12`).

## 3. Shared code and local code

| | Shared code | Local code |
|---|---|---|
| Where | the module top level, `onStart`, `onFrame`, timers, event hooks, decision hooks, channel handlers, `onPressed` of a script button | `onLocalFrame`, `onRender`, the `onLocal*` input hooks, `onLocalPressed` of a script button |
| Runs | on every player's game and in every replay, the same way | on this player's machine only |
| May | change the game, add timers, create channels and buttons, draw random numbers, load modules | read the game, use `local`, draw, move the local screen, send over channels |
| May not | use `local` (throws a `TypeError`) | change the game, add or cancel timers, create channels, draw random numbers, load modules (each throws a `TypeError`) |

A member marked "Shared code only." or "Local code only." in the declarations throws a `TypeError` on the other side; "Only in onRender." throws outside `onRender`.

### 3.1 Hooks

Hooks are top-level functions with known names.

| Hook | When |
|---|---|
| `onStart()` | once, at the first frame |
| `onFrame(frame)` | every frame, before the triggers |
| `onUnitCreated`, `onUnitCompleted`, `onUnitDied`, `onUnitDamaged`, `onOrderIssued`, `onTargetAcquired`, `onUnitTrained`, `onResearchCompleted`, `onUpgradeCompleted`, `onSpellCast`, `onBulletCreated`, `onBulletHit`, `onOwnerChanged`, `onUnitLoaded`, `onUnitUnloaded`, `onPlayerLeft`, `onPlayerResult` | the events of the frame, one call per event |
| `onBeforeDamage`, `onBeforeOrder`, `onBeforeTargetAcquired` | decisions: inside the game routine, the game read-only; change only the decision |
| `onLocalKeyDown`, `onLocalKeyUp`, `onLocalMouseDown`, `onLocalMouseUp`, `onLocalChat`, `onLocalButtonPressed`, `onLocalButtonTargeted` | input of the local player, before the game handles it; not in replays or for observers |
| `onLocalFrame(frame)` | after each frame this machine plays |
| `onRender(screen)` | after `onLocalFrame`, to draw |

A frame runs, at the frame start before the triggers: `onStart` (first frame only), the due timers, the channel handlers, the event hooks, `onFrame`.

`game.every(frames, callback)` and `game.after(frames, callback)` add timers; `Time.fromSeconds`, `Time.fromMinutes` and `Time.fromMilliseconds` give the frames. Both return a `Timer` with `cancel()`.

A local input hook can call `event.consume()` to keep a key, mouse button, chat line or button press from the game.

A decision hook changes the decision object: `setAmount` or `cancel` for damage, `setOrder`, `setTarget` or `reject` for an order, `setTarget` for a target. The game is read-only meanwhile.

### 3.2 From local input to shared code

Local code cannot change the game. It sends a value over a channel, and the channel's handler runs as shared code on every player's game at the frame the game runs the command, in every replay too.

```js
const votes = sync.createChannel((player, value, units) => {
    game.print(player.getName() + ' votes ' + value);
});

/** @param {LocalChatEvent} event */
function onLocalChat(event) {
    if (event.getText() === '-easy' || event.getText() === '-hard') {
        votes.send(event.getText().substring(1));
        event.consume();
    }
}
```

- Create channels in shared code, in the same order on every game: at the module top level or in `onStart`.
- The handler gets the sending player, the value, and the live units the player had selected.
- A value is `null`, a boolean, a number, a string, or an array or plain object of these.
- `send` in shared code sends once from every human player's game.
- `sync.createPlayerValue(initial)` keeps one value per player on its own channel: `get(player)`, `set(value)` for the sending player, `set(player, value)`.

A script button (section 5.2) uses a channel of its own: `onPressed` is its handler.

### 3.3 Determinism

Shared code must give the same result on every game, or the games split.

- Use `random` (`next()`, `nextInt(bound)`, `chance(probability)`) for random numbers. It is seeded from the game seed and is the same on every game; `Math.random()` draws from it as well. Both are shared code only.
- There is no `Date` and no clock. Count frames: `game.getFrame()`, `game.getElapsedTime()`, timers.
- Local code must not change a variable that shared code reads. Pass local input to shared code through a channel.
- Sprite and image slots can differ between players: the pools also hold the buildings each player remembers in the fog of war. Shared code may show sprites and images but must not let their ids decide the game.
- `local.*` and the local selection differ between players; they are local code only.

### 3.4 Errors

- An error that a hook, timer or handler does not catch ends that call only, and is logged.
- A shared call that exceeds the instruction budget (chapter 7) stops the script for the rest of the game.
- A local call that exceeds it ends that call only; an `onRender` call that exceeds it draws nothing.

## 4. The API at a glance

The upper API is a set of globals over objects: units, players, types. The lower API `core` works on ids and raw values; the upper API is built on it, and scripts may call both.

| Global | What it holds | Example |
|---|---|---|
| `game` | the game: frame, timers, players, units, map, switches, countdown, leaderboard, settings, vision, combat | `game.units.find({type: UnitType.TerranMarine, owner: player})` |
| `local` | what only this machine knows: local player, screen, mouse, local selection, command card state. Local code only | `local.getPlayer()` |
| `display` | presentation: console, stat_txt strings, message lines, sounds, image graphics, command card | `display.messages.clear()` |
| `sync` | channels between the games | `sync.createChannel(handler)` |
| `random` | the script random numbers. Shared code only | `random.nextInt(4)` |
| `log` | the module's log | `log.info('frame', game.getFrame())` |
| `Position`, `Time` | positions in map pixels, durations in frames | `Position.of(320, 640)`, `Time.fromSeconds(10)` |
| `UnitType`, `WeaponType`, `UpgradeType`, `TechType`, `Order`, `ImageType`, `SpriteType`, `FlingyType` | the game data types, one object per id, with their DAT values | `UnitType.ProtossZealot.getMaxHp()` |
| `Color`, `Font`, `Align`, `Key`, `MouseButton`, `Modifier`, `EventKind`, `ButtonCondition`, `ButtonAction`, ... | constant tables | `Color.fromRgb(255, 128, 0)` |
| `core` | the lower API: ids, raw values, frame records | `core.player.getMinerals(0)` |

Types compare with `===`: `unit.getType() === UnitType.TerranMarine`. A table gives a type by id too: `UnitType.get(0)`.

A few objects in use:

```js
const player = game.players.get(0);
const marines = player.units.count(UnitType.TerranMarine);
const spawn = game.map.locations.findByName('Spawn');

log.info(player.getName(), 'has', marines, 'Marines');

if (spawn !== null) {
    game.units.create(UnitType.TerranMarine, player, spawn.getCenter());
}

game.switches.set(3, true);
display.strings.setStatText(1, 'Marine');
```

Positions are map pixels unless a member names tiles; player slots and type ids start at 0. An object of a unit, sprite, image or bullet throws an `Error` once that thing is gone; check `exists()` first when unsure.

For each member, its arguments and its notes, read the declaration files. Your editor shows them on completion.

## 5. Examples

### 5.1 A timer that gives minerals

```js
const bonus = game.every(Time.fromSeconds(30), () => {
    game.players.getHumans().forEach((player) => {
        player.addMinerals(50);
        player.print('+50 minerals');
    });
});

game.after(Time.fromMinutes(10), () => bonus.cancel());
```

Every 30 seconds each human player gets 50 minerals, for 10 minutes.

### 5.2 A command card button

```js
const COST = 100;

display.commandCard.createButton({
    unitType: UnitType.TerranCommandCenter,
    position: 9,
    icon: 0,
    text: 'Call a Marine (100)',
    hotkey: 'M',
    isEnabled: (unit) => unit.getOwner().getMinerals() >= COST,
    onPressed: (player, units) => {
        units.forEach((unit) => {
            if (player.getMinerals() >= COST) {
                player.subtractMinerals(COST);
                game.units.create(UnitType.TerranMarine, player, unit.getPosition());
            }
        });
    }
});
```

- `createButton` is shared code; call it at the module top level or in `onStart`. A game has at most 256 script buttons.
- `position` is the card slot 1 to 9; `icon` is a cmdicons frame.
- `isEnabled(unit)` returns `true` (enabled), `false` (gray) or `null` (hidden). It must read only the game, never `local`: every game calls it again when the press runs.
- `onPressed(player, units, target)` is shared code; `units` are the selected units for which `isEnabled` returned `true`.
- `target: true` makes a target button: the player picks a unit or a point, and `target` holds `position` and `unit`.
- `onLocalPressed(event)` runs first, on the pressing machine; `event.consume()` sends nothing.

### 5.3 Screen text and messages

Shared code shows messages:

```js
function onStart() {
    game.print('Defend the base for 10 minutes.');
    game.players.get(0).print('You are the defender.');
}
```

Local code draws in `onRender`; the drawing stays until the next call:

```js
function onRender(screen) {
    const seconds = game.getElapsedTime();
    const clock = Math.floor(seconds / 60) + ':' + String(seconds % 60).padStart(2, '0');

    screen.drawText(clock, screen.getWidth() / 2, 8, {font: Font.Large, color: Color.Yellow, align: Align.Center});

    const player = local.getPlayer();

    if (player !== null) {
        player.units.getAll().forEach((unit) => {
            if (unit.getType() === UnitType.TerranGhost) {
                screen.world.drawCircle(unit.getX(), unit.getY(), 24, {color: Color.Cyan});
            }
        });
    }
}
```

`screen` draws in interface units (480 high, `screen.getWidth()` wide), `screen.world` in map pixels above the units, `screen.minimap` in map pixels on the minimap. `drawImage` draws a picture of `resources/`: `game.map.resources.getImage('logo.png')`.

### 5.4 Reacting to a unit death

```js
/** @param {UnitDiedEvent} event */
function onUnitDied(event) {
    const killer = event.getKillerPlayer();

    if (event.getUnitType() === UnitType.ZergZergling && killer !== null) {
        killer.addMinerals(5);
    }
}
```

The event holds the values of the moment the unit died: `getUnit()`, `getUnitType()`, `getOwner()`, `getKiller()`, `getKillerPlayer()`, `getWeapon()`. The unit may be gone already.

## 6. Map protection

```powershell
SC4J.bat tool scr-enhanced script pack Map.scx --script my-map -o Protected.scx --protect
```

What `--protect` does:

- Each script loses its comments and spare whitespace, and the names declared inside functions and blocks get short names. The script runs as the original does.
- Every line of the protected script comes from one original line, so error lines can be traced back.
- The map is marked protected, and the replays of its games keep the mark. `unpack` refuses a protected map.
- The source map `Protected.scx.sourcemap.json` is written next to the output. The map does not hold it; keep it to yourself.

What it does not do:

- It does not encrypt. Names that are looked up by name stay: top-level functions and variables of a module, hooks, `module.exports`, `exports`, `require` ids, property names, the API, and the text of strings.
- The files of `resources/` are stored as they are.

To read an error of a protected map, trace its location with the source map:

```powershell
SC4J.bat tool scr-enhanced script trace Protected.scx.sourcemap.json --at main.js:3
SC4J.bat tool scr-enhanced script trace Protected.scx.sourcemap.json --log sc4j.log
```

## 7. Limits

- Language: Rhino 1.8.1 has no `class`, `async`/`await`, spread (`...`), `import`/`export` or `for (const ... of ...)`. A `const` in a loop keeps its first value, and the closures of one `for (let ...)` loop share one binding. `check` reports these.
- Globals: no `eval`, no `Function` from strings, no `Date`, no `setTimeout` or other timers, no Java access. `Math` gives the same results on every platform.
- Instruction budget: each call into the script (loading, a frame, a decision, a local input, a local frame, a render) may run 5,000,000 interpreter instructions; function calls may nest 10,000 deep. Spread long work over frames with timers.
- Script buttons: at most 256 per game.
- Channel values: `null`, booleans, numbers, strings, and arrays and plain objects of these.
- Saved games: a game with a script cannot be saved.
