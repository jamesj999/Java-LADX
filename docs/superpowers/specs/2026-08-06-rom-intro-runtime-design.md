# ROM-backed opening intro runtime

## Goal

Make the Java opening sequence and title handoff follow the shipped GBC ROM's
intro path closely enough that the default launch visibly follows the same
scene order, frame timing, scrolling, entity composition, title reveal, and
palette transitions as the disassembly. The implementation remains a
domain-specific intro renderer; it does not execute Game Boy instructions or
emulate the CPU, PPU, or interrupt system.

## Current boundary and gap

The Java startup already loads the encoded intro background maps, attrmaps,
tiles, and static palette blocks through `BackgroundSceneLoader` and `GPU`.
`CutsceneManager` also owns the opening lifecycle and input skip path. The
remaining mismatch is concentrated in `IntroSequence`: it hardcodes ship and
animation tables, synthesizes rain positions, uses an approximate lightning
layout, and compresses the ROM's multiple intro/title substates into four
coarse stages. It does not expose the ROM's per-frame palette and title-map
updates.

The implementation will preserve the existing scene IDs and rendering-layer
interfaces where possible, replacing only the approximated intro state and
adding an explicit per-frame snapshot for data that changes during the scene.

## Source of truth

All runtime graphics and animation data below are read from the shipped ROM
using the bank/address mapping already used by `RomBank`:

- `IntroSeaPaletteTable` at bank `$01:$6E19`;
- the intro stage dispatch and timers in bank `$01:$6E1D-$7465`;
- `RenderRain` and its random-consumption order at `$7466-$74B7`;
- `IntroShipTiles`, `Data_001_7550`, and `ShipHeaveTable` at `$7538`,
  `$7550`, and `$7560`;
- `IntroLightningTiles` at `$75CB` and `IntroMarinSpriteVariants` at
  `$764F`;
- Marin's state handlers and the inert-Link state handlers in
  `$7681-$77BC` and `$7A2F-$7AE3`;
- `IntroSparkleSpriteVariants` at `$77BD`;
- DX-logo OAM tables and fade palette data beginning at `$7808` and `$79A0`;
- the title reveal pointer/data tables at `TitleTileMap` and `TitleAttrMap`
  (`$7264` and `$732A`), plus `TitleScreenPostBeachTilemap` at `$7AE4`;
- the existing encoded background and palette assets selected by
  `BackgroundSceneCatalog`.

ROM constants identify stable labels and offsets, but no new premade PNGs,
Java sprite shapes, or copied palette literals will be introduced.

## Design

### ROM data adapter

Add a focused intro data adapter responsible for validating the ROM and reading
the fixed-size records used by the intro. It will expose unsigned table bytes,
signed OAM offsets, pair/rectangle sprite records, title-map rows, and RGB555
palette blocks without exposing raw offsets to the state machine. It will use
the existing `RomBank.romOffset` and RGB555 decoder, and reject truncated ROM
reads with the same argument-validation style used by the GPU loaders.

The adapter will also provide the source random-byte cadence needed by
`RenderRain` and the lightning trigger. The intro starts with the source seed
`$A2`; the Java runtime will use the existing ROM random implementation with a
private intro seed so advancing the opening does not consume gameplay random
state.

### Intro state machine

Refactor `IntroSequence` into a ROM-backed state machine that retains the
current public observations (`sceneId`, scrolling, sprites, title rows) and
adds a snapshot for dynamic tilemap/attrmap and palette state. Its state names
will correspond to the meaningful disassembly substates:

- setup and sea loading;
- ship-on-sea, including 8-frame scroll/ship movement, rain, lightning
  countdown, and the source transition thresholds;
- Link-face rain, scream timing, and lightning;
- beach setup and the Marin state sequence, including inert Link entry,
  scroll pauses, line-scroll wave offsets, and the final hold;
- title row reveal and title SFX timing;
- title copyright/DX animation and the stable title state.

The Java state machine will advance once per game frame, in the same order as
the source handler: decrement/update timers, update entities, update scrolling
and palette effects, then publish the render snapshot. `skipToTitle` will reset
the state to the source's stable title values rather than merely changing a
scene string.

### Dynamic render snapshot

Extend the cutscene boundary with immutable per-frame data containing:

- scene ID and base scroll coordinates;
- optional per-scanline X offsets and the intro vertical wave offset;
- the current 32x32 tilemap and attrmap, including title reveal rows and the
  post-beach title map;
- current BG and OBJ palette rows after lightning, beach, and DX fade updates;
- the ordered OAM sprites for rain, ship, lightning, Marin, inert Link,
  sparkles, and the DX logo.

`GameFrameSceneBuilder` will consume this snapshot while keeping the existing
room rendering path untouched. A scene transition will still load the static
encoded background from `BackgroundSceneLoader`; dynamic updates will copy or
replace only the affected rows/regions, matching the source's draw-command
boundaries.

### Integration

`Main.startIntroCutscene` will construct the intro runtime with the loaded ROM
and use its initial snapshot. `CutsceneManager` will forward the runtime
snapshot and continue to own dialog/input skip coordination. The existing
title-screen Enter path will remain the handoff to configured gameplay; this
slice does not introduce save-file or file-menu behavior.

### Error handling and fidelity rules

- Every fixed ROM table read validates its bank/address range before use.
- OAM offsets are sign-extended exactly once; palette and tile bytes are
  unsigned.
- The runtime preserves source sprite ordering and does not sort by position.
- Random calls occur only at the same handler points as `RenderRain` and
  lightning selection; no Java frame-local random source is used.
- Unsupported DMG/Japanese-only branches are not silently mixed into the
  shipped GBC path. Existing DMG scene loading remains unchanged and is
  covered by regression tests, while this slice's pixel target is the shipped
  GBC ROM.

## Verification

Tests will cover the following independently:

1. Synthetic ROM adapter tests prove the bank/address mapping, signed OAM
   offsets, table record widths, and RGB555 palette reads.
2. State-machine tests assert exact scene/substate boundaries, ship movement,
   rain count and random consumption, lightning frames, Marin transitions,
   beach scroll pauses, and title reveal order.
3. Snapshot tests assert sprite order, tile/attribute updates, line-scroll
   values, and palette rows at representative frames before and after each
   transition.
4. Shipped-ROM tests assert that the loaded records equal the disassembly
   bytes and that representative opening frames use the expected ROM tile and
   palette values rather than Java literals.
5. Framebuffer regressions verify that changing only the intro frame changes
   the expected animated pixels while preserving the decoded background and
   sprite alpha masks.
6. The complete Gradle suite and whitespace checks pass without changing
   ordinary overworld, indoor, or Color Dungeon rendering.

## Non-goals

This slice does not implement the file-select menu, save persistence, new-game
initialization, or gameplay transitions after title-screen input. It also does
not add a CPU/PPU emulator, execute assembly at runtime, or replace the
existing ROM-driven background decoder. Later work can reuse the same
snapshot/data-adapter pattern for file menus and other cutscenes.
