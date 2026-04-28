---
name: ladx-rom-tileset-debug
description: Diagnose Link's Awakening DX rendering bugs when tiles, tilemaps, attrmaps, VRAM destinations, or palettes are being loaded from ROM and the on-screen layout is right but the graphics are wrong. Use when working against the LADX disassembly or the game ROM to debug tilesets, title screens, backgrounds, or other ROM-backed graphics paths in Java, WebGL, emulators, or custom engines.
---

# LADX ROM Tileset Debug

## Overview

Use this skill to recover the exact ROM-backed graphics path for a Link's Awakening DX screen or tileset and compare it against the current implementation. Treat the ROM as the runtime source of truth. Use the disassembly to discover bank numbers, labels, VRAM destinations, tilemap/attrmap pointers, palette tables, and any per-frame tile uploads.

## Workflow

1. Identify the exact screen or tileset path in the disassembly.
   Search for the gameplay handler, tileset load handler, tilemap pointer, attrmap pointer, and palette table before changing code.
   For title and intro work, start in `src/code/intro.asm`, `src/code/bank0.asm`, `src/code/bank20.asm`, `src/code/background_colors.asm`, and `src/code/palettes.asm`.
   For overworld room work, start in `src/code/bank0.asm`, `src/code/object_attributes.asm`, `src/code/palettes.asm`, `src/data/maps/overworld.asm`, and `src/data/objects_tilemaps/overworld.cgb.asm`.

2. Recover the real ROM offsets before inspecting bytes.
   ROMX labels use file offset `bank * 0x4000 + (address - 0x4000)`.
   ROM0 labels use file offset `address`.
   Do not use `bank * 0x4000 + address` for ROMX labels.

3. Check whether the tile source bank is adjusted on GBC.
   If the load path goes through `AdjustBankNumberForGBC` or `SwitchAdjustedBank`, the effective bank on GBC is usually `bank | 0x20`.
   For example, title graphics that appear to come from banks `0x0F` or `0x10` on DMG may actually come from `0x2F` or `0x30` on GBC.

4. Reconstruct the VRAM load order, not just one asset.
   Many screens are composites.
   Verify every block copied into `vTiles0`, `vTiles1`, or `vTiles2`, including overlays and later writes that replace earlier tiles.
   If a screen partly looks right, the usual bug is that one VRAM region or one later copy was omitted.

5. Decode tilemap and attrmap data separately.
   Background tilemaps and attrmaps are encoded draw-command streams, not raw 20x18 byte arrays in ROM.
   Decode both from ROM before rendering.
   Attrmaps provide palette selection and background flip flags on GBC. Background tile IDs themselves are raw IDs, not packed with flip bits.

6. Use the correct BG tile addressing mode.
   Check the LCDC BG/window tile data mode for the screen.
   If the screen uses the `0x8000` mode, tile IDs map directly.
   If the screen uses the `0x8800` mode, treat tile IDs as signed:
   IDs `0x80..0xFF` map directly to tile slots `0x80..0xFF`.
   IDs `0x00..0x7F` map to tile slots `0x100..0x17F`.
   Overworld object tilemaps in `overworld.cgb.asm` also follow this gameplay BG addressing rule.

7. Decode palettes from ROM rather than guessing RGB values.
   CGB palette tables store 4 colors per palette as little-endian RGB555 entries.
   Convert each channel from 5-bit to 8-bit when feeding a modern renderer.

8. Check for per-frame tile uploads.
   Some intro and title paths set ROM and VRAM pointers and copy tile chunks every frame.
   If static tiles still look wrong after the initial load order is fixed, inspect any animated-tile or intro-specific copy path before assuming the base tileset is wrong.
   Gameplay overworld also does this through the room-header `hAnimatedTilesGroup`, commonly into `vTiles2 + $6C0`.

9. Treat overworld rooms as object-driven, not tilemap-driven.
   Overworld gameplay screens are assembled by `LoadRoom` and `LoadRoomTilemap`, not by decoding an encoded 20x18 background stream.
   Parse the room header and room object stream, fill the room with the floor object, expand each object through the 2x2 object tilemap table, and apply object attributes separately on GBC.
   Do not reuse title-screen helpers such as encoded background decoders, title attrmaps, or title-specific signed tile-ID resolvers for overworld unless the disassembly proves that exact path is active.

10. Parse the overworld object stream in the exact byte order used by the game.
   Ordinary 2-byte objects are `[locationYX, type]`.
   3-byte strip objects are `[directionAndLength, locationYX, type]`.
   Warp records are detected when `(firstByte & 0xFC) == 0xE0` and consume 5 bytes total.
   `ROOM_END` is `0xFE`.
   Do not reinterpret the first byte of an ordinary object as the object id.
   The 3-byte/strip decision is made from the first byte:
   strip only when `bit7(firstByte) == 1` and `bit4(firstByte) == 0`.

11. Implement overworld strips and macros before blaming tilesets.
   For 3-byte strip objects, the low nibble of the first byte is the repeat count and bit `6` controls vertical placement.
   The actual object type is the third byte unchanged, and the low nibble is the exact object count, not `count + 1`.
   For macros, detect them from the actual object type byte with `type >= 0xF5`, then expand them through `ExpandOverworldObjectMacro`.
   Skipping macros, or replacing them with guessed placeholder objects, will corrupt large parts of a room even if the tile table is correct.
   Tree macros are the common trap: `TreeMacroHandler` is conditional and strip-aware, not a fixed 2x2 tile/object stamp.
   The macro offset tables also matter. Do not read the ids from `overworld_macros.asm` and then ignore the paired offsets by stamping a plain rectangle.
   Model the same padded room-object buffer the game uses (`wRoomObjectsArea` / `wRoomObjects` with `0x10` stride), not just a flat `10x8` grid.

12. Reconstruct the gameplay overworld VRAM loads from the real handlers.
   For gameplay overworld backgrounds, use `LoadBaseTiles` (`bank0.asm:$2BCF`), `LoadRoomSpecificTiles`, and the VBlank `LoadOverworldBGTiles` as the source of truth.
   The game's `LoadBaseTiles` copies `0x100` tiles (`$1000` bytes) as a single contiguous block from `InventoryEquipmentItemsTiles` (`$4800`) to `vTiles1` (`$8800` = tile `0x080`). This one copy covers equipment, inventory items, instruments, AND landscape tiles in the correct VRAM positions. Landscape tiles (`OverworldLandscapeTiles` at `$5400`) land at VRAM tile `0x140`.
   Do not split this into separate loads with guessed destinations — the offsets will be wrong.
   Be careful converting VRAM destinations to tile indices:
   `vTiles1 + $400` is tile index `0x0C0`, not `0x140`.
   Also keep the active ROM bank semantics intact: if the game used `SwitchAdjustedBank` before a sequence of overworld copies, later labels in that sequence are still coming from the adjusted GBC bank.

12b. Use the GBC overlay for overworld tile and attribute lookups.
   On GBC, `WriteOverworldObjectToBG` reads the overlay value from WRAM bank 2 (not the room object type from bank 0) and uses `overlayValue * 4` to index both `OverworldObjectsTilemapCGB` and the per-object attribute table.
   The overlay data is stored per room in banks `$26` (rooms `$00-$CB`) and `$27` (rooms `$CC-$FF`), 80 bytes per room at `$4000 + roomIndex * $50`.
   `LoadRoomObjectsAttributes` (`bank20.asm:$6DAF`) copies the overlay to WRAM bank 2 before rendering.
   For a fresh room, overlay values often match room object IDs — but the overlay is the authoritative source and must be used for correctness.

13. Do not replace `OverworldTilesetsTable` with a guessed or copied convenience table.
   The real table lives in `src/code/bank20.asm`, has 64 entries, and is indexed by `hMapRoom / 4`.
   Prefer loading it from ROM using the disassembly label-derived offset instead of retyping it as a Java array.
   Respect `W_TILESET_KEEP` (`0x0F`): it means "leave the room-specific BG region unchanged", not "load tileset 0x0F".

## Quick Checks

- Right tile placement, wrong graphics:
  Usually wrong ROM offset, wrong effective bank, wrong tile addressing mode, or missing VRAM copy.

- Correct logo or foreground, corrupted background:
  Usually one background tile block is missing or the screen uses signed BG tile IDs.

- Correct graphics, wrong colors:
  Usually guessed palette, wrong palette table, or attrmap palette bits ignored.

- Text fixed but the rest is still wrong:
  Usually one overlay copy is correct while the base background tileset is still loaded from the wrong bank or mode.

- Overworld room layout is plausible but every tile looks like noise or repeated filler:
  Usually the implementation skipped room-object parsing and is drawing a test tile, a decoded background stream, or the wrong object-to-tiles table.

- Overworld parser seems to produce lots of objects with ids like `0x8A`, `0x82`, `0x83` from the start of the stream:
  Usually the parser swapped the object byte order and is reading location bytes as object ids.

- Overworld parser prints room-00 entries like `loc=00 type=00`, `loc=8A type=10`, `loc=EF type=10`:
  Usually it is deciding strip-vs-normal from the wrong byte and is desynchronizing the whole stream.

- Overworld colors are wrong even though object expansion looks roughly plausible:
  Usually `OverworldPalettes` and `OverworldPaletteMap` were confused, or object attribute bytes were not loaded from the per-room attr table.

- Overworld large structures are still nonsense but ordinary small objects look closer:
  Usually macros were expanded with guessed placeholder ids instead of the real macro object tables.

- Overworld trees look especially wrong while houses and small objects look closer:
  Usually `TreeMacroHandler` was simplified into a fixed 2x2 placement and its overlap/bush rules were lost.

- Overworld room-specific graphics look wrong only in some regions/rooms:
  Usually `OverworldTilesetsTable` was replaced by a guessed offsets array or Java literal table, or `W_TILESET_KEEP` was treated as a real tileset load.

- Overworld room geometry looks roughly correct but the actual tile artwork is obviously from the wrong bank or looks scrambled:
  Usually the `overworld.cgb.asm` tile ids were rendered as direct unsigned tile indices instead of `0x8800` signed BG indices.

- Overworld geometry is close but tiles still appear shuffled within the correct structures:
  Usually one gameplay VRAM copy landed in the wrong tile slot range, especially `InventoryOverworldItemsTiles` at `vTiles1 + $400`.

- Overworld geometry is close and the tile slot math is correct, but the visible patterns still look like the wrong art inside the right shapes:
  Usually one shared overworld block was loaded from the DMG bank instead of the GBC-adjusted bank.

- Overworld geometry is close and most static tiles look plausible, but waterfalls or shared terrain details still look like the wrong quarters of nearby tiles:
  Usually the room-header animated-tiles group was ignored, so `vTiles2 + $6C0` never got the correct overwrite.

- Overworld tiles are individually recognizable (tree bark, grass, water) but many appear horizontally or vertically mirrored, giving the whole room a "weird mirroring" look:
  Usually the base tile VRAM load was split into separate calls with hand-picked destinations instead of replicating the game's single bulk copy. The game's `LoadBaseTiles` copies `0x100` tiles contiguously from `InventoryEquipmentItemsTiles` (`$4800`) to `vTiles1` (`$8800` = tile `0x080`). This places landscape tiles at tile `0x140`, NOT `0x120`. Loading landscape separately to `0x120` shifts every tile reference by 32 slots, so the CGB tilemap points at the wrong graphics. Fix: replicate the single bulk copy.

- Overworld tile IDs and attributes trace correctly through logging but the image still looks wrong:
  Verify the actual VRAM tile content by dumping pixel data from the GPU tile array and comparing against the expected ROM bytes. If they match but the image is garbled, the VRAM destination offsets are wrong — the tile data is correct but in the wrong slots.

- Overworld flipX/flipY flags appear to be "never set" (all attribute bytes are 0x00-0x07):
  This may be correct for many rooms. On GBC, the game uses a per-room overlay (banks `$26`/`$27`) to index the attribute table, and many room positions genuinely have no flip flags. Only some rooms/positions use flip flags (e.g., room `$A0` has ~25% of positions with flipX/flipY). Check the overlay values and the corresponding attribute table entries, not just the room object IDs.

## Reference

Read [references/rom-graphics.md](references/rom-graphics.md) for the address formulas, key files, title-screen example, attrmap bits, and the debugging checklist.
