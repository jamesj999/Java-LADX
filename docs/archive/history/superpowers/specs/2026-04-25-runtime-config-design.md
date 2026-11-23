# Runtime Config Design

## Goal

Add one user-facing JSON config file for runtime options: controls, title-screen startup, story/intro startup, debug functionality, start location, and item profile.

## Current Behavior To Preserve

By default the app starts on the main title screen. Pressing Enter enters the overworld at room `0x92` with the current debug-style inventory state. Debug dumping with F1 remains enabled by default until debug tooling is formalized.

## Config Shape

The new resource file is `java/src/main/resources/config/config.json`.

```json
{
  "showTitleScreen": true,
  "playIntroStory": false,
  "debugEnabled": true,
  "startLocation": {
    "type": "OVERWORLD",
    "roomId": "0x92"
  },
  "itemProfile": "DEBUG_ALL_ITEMS",
  "controls": {
    "menuOpenKey": "ENTER",
    "upKey": "UP",
    "downKey": "DOWN",
    "leftKey": "LEFT",
    "rightKey": "RIGHT",
    "aKey": "Z",
    "bKey": "X"
  }
}
```

## Setting Semantics

`showTitleScreen` controls whether the title screen is shown before gameplay. `true` keeps the current startup path. `false` skips directly into the selected gameplay startup path.

`playIntroStory` controls whether startup should use the intro/new-game story flow. The flow is not implemented yet, so `true` is accepted but falls back to direct overworld startup with a warning.

`startLocation` controls the direct spawn target independently from title and story mode. The first implementation supports only `{ "type": "OVERWORLD", "roomId": "0x92" }`-style locations.

`itemProfile` controls initial player inventory/state. `DEBUG_ALL_ITEMS` keeps today’s behavior. `NEW_GAME` is accepted now but falls back to `DEBUG_ALL_ITEMS` with a warning until normal new-game state exists.

`debugEnabled` gates F1 debug dumping and gives future debug tools one shared switch.

`controls` replaces the existing standalone `config/input.json` payload. Key-name parsing should stay compatible with the current `InputConfig` behavior and defaults.

## Architecture

Introduce a small config model that parses the new JSON resource and exposes typed values to `Main`. `InputConfig` should remain the object consumed by input code, but it should be created from the nested `controls` block instead of reading its own separate file.

Startup branching remains in `Main`: load title assets for title mode, otherwise load the direct overworld screen. Direct overworld startup should use the configured overworld room instead of the hardcoded room constant.

Player-state selection should be explicit, even while both supported profiles currently resolve to the existing debug state. This keeps the config contract stable for the future normal new-game implementation.

## Testing

Add tests for config defaults, parsing of the nested controls block, parsing hex room IDs, fallback behavior for unsupported story/profile paths, and debug gating. Keep tests focused on pure config/player-startup logic where possible; avoid requiring GLFW/OpenGL setup.
