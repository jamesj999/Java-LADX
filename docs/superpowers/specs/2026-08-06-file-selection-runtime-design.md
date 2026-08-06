# ROM-Backed File Selection and New Game Design

**Date:** 2026-08-06  
**Status:** Approved for implementation as a bounded reconstruction slice

## Goal

Replace the Java engine's direct title-to-configured-room shortcut with the
shipped ROM's file-selection screen and its new-file name-entry boundary. The
screen must use the disassembly's encoded backgrounds, CGB palettes, menu/font
tiles, cursor positions, and input wrapping rules without implementing a CPU or
PPU emulator.

This slice deliberately stops after creating a named in-process new-game slot
and returning a `START_NEW_GAME` action to the existing gameplay bootstrap.
Copy, erase, load, save, SRAM persistence, and their transition effects remain
separate slices.

## Source of truth

The implementation follows:

- `LADX-Disassembly/src/code/file_menus.asm`
  - `FileSelectionPrepare6` at bank `$01:$48B3`
  - `FileSelectionInteractiveHandler` at `$01:$48E8`
  - `FileSelectionExecuteChoice` at `$01:$4995`
  - `FileCreationInit2Handler`/`FileCreationInteractiveHandler` at
    `$01:$4A24-$4CBF`
  - `NameEntryCharacterTable` at `$01:$4BB5`
- `LADX-Disassembly/src/data/backgrounds/{tilemaps,attrmaps}_pointers.asm`
  and the corresponding encoded menu background files
- `LADX-Disassembly/src/code/palettes.asm`
  - `Data_021_74F0` palette selector table
  - `Data_021_7536` menu palette block
- `LADX-Disassembly/src/code/bank0.asm`
  - `LoadMenuTiles` at `$00:$2C03`
  - `LoadBaseTiles` at `$00:$2BCF`
- `LADX-Disassembly/src/data/codepoint_to_tile.asm` for save-name glyph tile
  mapping.

ROM bank offsets are resolved through `RomBank.romOffset`; no generated PNG
or Java copy of the ROM tables is authoritative.

## Architecture

`FileMenuRomData` owns fixed ROM reads and validation for the name-entry table.
`BackgroundSceneCatalog`/`BackgroundSceneLoader` own the encoded tilemap,
attrmap, and palette block. `FileMenuController` owns only menu state and
source-derived input transitions. It publishes an immutable
`FileMenuFrameSnapshot` containing full 32x32 maps, palettes, selected slot,
name-entry cursor, and OAM-like cursor sprites.

`GameFrameState` carries the snapshot alongside the existing intro snapshot.
`GameFrameSceneBuilder` renders it through the existing background and sprite
layers. `GPU.loadMenuTiles` mirrors `LoadMenuTiles`, including the base bulk
copy, menu tiles, font tiles, and the two base cursor tiles at VRAM tile
`$E0`.

`Main` enters the file-menu screen when the title receives Enter. Its update
loop consumes the controller's action; a committed new name invokes the
existing configured gameplay bootstrap. No room renderer or entity runtime is
changed by this slice.

## State and input behavior

Selection starts at slot `0`. Slots `0..2` are the three save files. Slot `3`
is the copy/erase row and becomes reachable only when the initialized-file
bitfield is nonzero. Up/down wraps over the active range. Left/right on slot 3
toggles the source's copy/erase arrow state. Selecting an empty slot enters
creation; selecting an initialized slot publishes a separate load action for
the future persistence slice.

Creation starts with five blank name bytes and character index `0`. Up/down
move by sixteen entries through the ROM character table; left/right move by
one, all modulo 64. A stores the selected ROM name byte at the current
position and advances, B moves the position back, and Start commits the five
bytes. The rendered name uses the ROM's `CodepointToTileMap` behavior and the
source map destinations (`$984A`, `$982A`).

The controller uses edge events from `InputState`; repeated-held-key timing is
left to the existing input layer and is not fabricated inside the renderer.

## Error handling and compatibility

ROM adapters reject null or truncated ROM data with descriptive exceptions.
Snapshots defensively copy arrays and palettes. Existing no-argument intro and
test constructors remain valid. The controller exposes an explicit `LOAD_GAME`
action instead of silently treating an initialized slot as a new game.

## Verification

- Synthetic ROM adapter tests prove bank math, name-table extraction, and
  defensive copies.
- Controller tests prove empty-slot selection, command-row availability,
  cursor wrapping, character-table movement, name writing, and commit action.
- Shipped-ROM loader tests prove the menu specs, palette block, decoded map
  dimensions, and expected map/palette bytes.
- GPU tests prove menu/font/cursor tile destinations.
- A framebuffer regression proves the file-selection frame differs from the
  title frame and that name/cursor state changes alter only the expected
  dynamic overlay state.
- `gradle clean test` must pass before this slice is reported.

