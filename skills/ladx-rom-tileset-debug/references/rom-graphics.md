# ROM Graphics Reference

## Key Rules

### ROM offsets

- ROM0: `offset = address`
- ROMX: `offset = bank * 0x4000 + (address - 0x4000)`

### GBC adjusted banks

- `AdjustBankNumberForGBC` in `src/code/bank0.asm` ORs the bank with `0x20` on GBC.
- `SwitchAdjustedBank` uses that adjusted value.
- If graphics are correct on DMG assumptions but wrong on GBC, verify the effective bank first.

### BG tile addressing

- `0x8000` mode: tile IDs are direct.
- `0x8800` mode: tile IDs are signed.
- In signed mode:
  - `0x80..0xFF` -> tile slots `0x80..0xFF`
  - `0x00..0x7F` -> tile slots `0x100..0x17F`

### Attrmap bits

- Bits `0-2`: BG palette index
- Bit `5`: X flip
- Bit `6`: Y flip
- Attrmaps live in VRAM bank 1 on GBC.
- Do not treat background tile IDs as if they carried these flags.

### Palette decode

- CGB colors are little-endian RGB555.
- Decode as:
  - red = bits `0-4`
  - green = bits `5-9`
  - blue = bits `10-14`

## Useful Files

- `LADX-Disassembly/src/code/bank0.asm`
  - `AdjustBankNumberForGBC`
  - screen and tileset load handlers
  - `LoadRoom`
  - `LoadRoomTilemap`
  - `WriteOverworldObjectToBG`
- `LADX-Disassembly/src/code/bank20.asm`
  - `TilemapsPointersTable`
  - tileset handler table
  - `LoadRoomObjectsAttributes`
  - `OverworldTilesetsTable`
- `LADX-Disassembly/src/code/background_colors.asm`
  - attrmap loading path
  - `AttrmapsPointersTable`
- `LADX-Disassembly/src/code/object_attributes.asm`
  - `GetBGAttributesAddressForObject`
  - overworld object attrmap bank/pointer selection
- `LADX-Disassembly/src/code/palettes.asm`
  - palette tables such as `Title_BG_Palettes`
  - `LoadRoomPalettes`
  - `OverworldPaletteMap`
  - `OverworldPalettes`
- `LADX-Disassembly/src/code/intro.asm`
  - intro/title handlers
  - overlay tilemaps and attrmap writes
- `LADX-Disassembly/src/code/home/animated_tiles.asm`
  - frame-by-frame tile uploads
- `LADX-Disassembly/src/main.asm`
  - bank placement of tile data via `incbin`
- `LADX-Disassembly/src/data/maps/overworld.asm`
  - overworld room pointer table
- `LADX-Disassembly/src/data/objects_tilemaps/overworld.cgb.asm`
  - CGB 2x2 object-to-tile lookup table

## Title Screen Example

This is the pattern that exposed several easy-to-miss bugs:

1. `LoadIntroSequenceTiles` loads intro graphics first.
2. `LoadTitleScreenTiles` overlays title-specific graphics afterward.
3. On GBC, title/logo banks loaded through `SwitchAdjustedBank` come from adjusted banks such as `0x2F` and `0x30`.
4. The title screen tilemap and attrmap come from encoded background data, not the inline post-beach overlay tilemap.
5. The title background uses signed BG tile IDs, so low tile numbers point into `vTiles1`.
6. Intro/title logic may also copy tile chunks every frame through `wD006..wD009`.

## Overworld Room Path

The overworld is a different pipeline from title/menu backgrounds:

1. `LoadRoom` in `src/code/bank0.asm` selects the room pointer from `OverworldRoomPointers`.
2. The room header is parsed as:
   - byte 0: animated tiles group
   - byte 1: floor object
   - remaining bytes: room objects until `ROOM_END` (`0xFE`)
3. Room objects are not all the same shape:
   - 2-byte objects: position + type
   - 3-byte objects: direction/length + position + type
   - macros: overworld object types `>= 0xF5`
   - warps: records where `(firstByte & 0xFC) == 0xE0`, followed by 4 more bytes
4. `FillRoomMapWithObject` seeds the 10x8 object grid with the floor object.
5. `LoadRoomTilemap` walks that 10x8 object grid and expands each object into a 2x2 BG block.
6. On GBC, `WriteOverworldObjectToBG` looks up the 4 tile IDs for each object in `src/data/objects_tilemaps/overworld.cgb.asm`.
7. Object attributes are a separate lookup:
   - `LoadRoomObjectsAttributes` prepares per-room object attribute data
   - `GetBGAttributesAddressForObject` picks the 4 attr bytes for each object
8. Room palettes are per-room, not global:
   - `LoadRoomPalettes` reads `OverworldPaletteMap[hMapRoom]`
   - that selects one entry from `OverworldPalettes`
9. Room-specific BG tiles are also per-room:
   - `OverworldTilesetsTable[hMapRoom / 4]` selects the current room tileset group
   - `LoadBaseOverworldTiles` and `LoadRoomSpecificTiles` copy the shared and room-specific graphics into VRAM

If you try to render the overworld by decoding an encoded background stream, reusing the title attrmap path, or filling a 20x18 array directly with guessed tile IDs, the result will be structurally wrong even if some tiles appear in roughly the right place.

## Overworld Object Stream Rules

These are the parser rules that are easiest to get wrong:

1. Ordinary object byte order is `[locationYX, type]`, not `[type, locationYX]`.
2. For a 3-byte strip object, the first byte is `directionAndLength`, not an object id.
   It is a strip only when `bit7(firstByte) == 1` and `bit4(firstByte) == 0`.
3. For a strip object:
   - low nibble = repeat count
   - bit `6` set = vertical strip
   - bit `6` clear = horizontal strip
   - actual object type = third byte unchanged
4. Overworld macros are detected from the actual object type byte after reading the object structure:
   - `type >= 0xF5`
   - dispatch through `ExpandOverworldObjectMacro`
   - macro expansion is data-driven; do not replace it with guessed placeholder ids
5. Warp records are not room objects and must not be fed through object expansion:
   - if `(firstByte & 0xFC) == 0xE0`, consume 5 warp bytes
6. Object id `0x00` is valid. Do not treat it as blank or “empty.”

Concrete example from the ROM:

- `Overworld00` starts at ROM offset `0x24200`
- first bytes:
  - `0B` = animated tiles group
  - `E5` = floor object
  - `8A 00 00` = strip object, location `0x00`, type `0x00`
  - `8A 10 00` = strip object, location `0x10`, type `0x00`
  - `8A 20 EF` = strip object, location `0x20`, type `0xEF`
  - `10 7C` = ordinary object at location `0x10`, type `0x7C`
  - `11 7D` = ordinary object at location `0x11`, type `0x7D`

If your parser prints room-00 entries like `loc=00 type=00`, then `loc=8A type=10`, then a strip derived from `type=0x8A`, your strip detection is using the wrong byte and the stream is desynchronized.

Also note that strip length is exact:

- for ordinary strip objects, write exactly `firstByte & 0x0F` objects
- do not write `count + 1`

## Overworld Macro Expansion

Do not hand-code a fake macro system that writes one or two placeholder object ids.

`ExpandOverworldObjectMacro` expands into real room-object ids through handler-specific tables. Examples from `src/code/overworld_macros.asm`:

- Two-doors house:
  - offsets: `00 01 02 03 04 / 10 11 12 13 14 / 20 21 22 23 24`
  - ids: `55 5A 5A 5A 56 / 57 59 59 59 58 / 5B E2 5B E2 5B`
- Large house:
  - ids: `55 5A 56 / 57 59 58 / 5B E2 5B`
- Palace door:
  - ids: `A4 A5 A6 / A7 E3 A8`
- Palm tree:
  - ids: `B6 B7 / CD CE`
- Walled pit:
  - ids: `2B 2C 2D / 37 E8 38 / 0A 33 2F 34 0A / 0A 0A 0A`

If a macro implementation mostly writes `0x5A`, `0x09`, `0x06`, or another tiny guessed set of ids, it is not implementing the real macro system.

Do not flatten every macro into a fixed rectangle:

- `TreeMacroHandler` is conditional.
  It reads the existing room-object grid before writing each quadrant.
  Top tiles can become overlap variants `0x29` and `0x2A`.
  Bottom tiles can become bush variants `0x82` and `0x83`.
- Tree macros also support strips.
  The handler loops horizontally or vertically based on the strip metadata instead of always placing exactly one tree.
- `StonePigHeadMacroHandler` is stateful.
  It uses `0xBB 0xBC / 0xBD 0xBE` when intact, but switches to `0x09` tiles when the room-status bit indicates the pig head is blasted.
- The offset tables are part of the behavior.
  `CopyOutdoorsMacroObjectsToRoom` walks explicit offsets like `00 01 02 / 10 11 12` or `1F 20 21 22 23`.
  If your helper reads the ids but ignores the offsets and just writes a rectangle from `(startX,startY)`, non-rectangular macros are still wrong.
- These offsets are relative to `wRoomObjects`, not a flat `10x8` array.
  The active room lives inside the padded `wRoomObjectsArea` buffer with a `0x10` row stride and a `0x11` base offset.

## Overworld Palette Selection

Use the two overworld palette tables in the right order:

1. `OverworldPaletteMap` at `21:42EF` gives the palette-set index for `hMapRoom`.
2. `OverworldPalettes` at `21:42B1` is the pointer table from palette-set index to actual RGB555 palette data.

Do not read `21:42B1` as if it were the room-indexed palette map.

## Overworld VRAM Loads

Do not guess the gameplay overworld tiles from asset names alone.

Use these handlers as the source of truth:

1. `LoadBaseTiles` (`bank0.asm:$2BCF`)
   The game copies `0x100` tiles (`$1000` bytes) as a SINGLE contiguous block from `InventoryEquipmentItemsTiles` (`$4800`) to `vTiles1` (`$8800` = tile `0x080`).
   This bulk copy covers all of: equipment items, inventory items, instruments, AND landscape tiles.
   The resulting VRAM layout within this block:
   - ROM `$4800` (items_2) -> VRAM tile `0x080`
   - ROM `$5000` (inventory_overworld_items) -> VRAM tile `0x100`
   - ROM `$5200` (instruments) -> VRAM tile `0x120`
   - ROM `$5400` (overworld_landscape) -> VRAM tile `0x140`
   On GBC, the bank is adjusted to `$2C` via `SwitchAdjustedBank`.
   **Do not split this into separate loads with hand-picked destinations.** Loading landscape tiles to `0x120` instead of `0x140` shifts every tile reference by 32 slots and produces garbled/mirrored graphics.
   `LoadBaseTiles` also copies Link character tiles (`$4000`) to `vTiles0` (`$8000` = tile `0x000`), `0x40` tiles.
2. `LoadRoomSpecificTiles` / `LoadOverworldBGTiles` (VBlank)
   - room-specific overworld BG tiles selected from `OverworldTilesetsTable[hMapRoom / 4]`
   - copied from `Overworld2Tiles` (bank `$0F`/`$2F`) to `vTiles2` (`$9000` = tile `0x100`)
   - address: `($40 + tilesetId) * $100`
   - 0x20 tiles

If a model only loads one hand-picked block such as `Overworld1Tiles` into `vTiles0`, it has not implemented the gameplay overworld graphics path.

`OverworldTilesetsTable` is the real selector. It lives in `src/code/bank20.asm` and has 64 entries, one for each `hMapRoom / 4`.
Do not replace it with a guessed 16-entry offsets array or a hand-copied Java literal table.
Also respect `W_TILESET_KEEP` (`0x0F`): it means "do not update the room-specific BG region".

## GBC Room Overlay System

On GBC, overworld tile and attribute lookups use a per-room overlay, not the room object types directly.

1. `LoadRoomObjectsAttributes` (`bank20.asm:$6DAF`) copies 80 bytes (10 cols x 8 rows) from ROM into WRAM bank 2 at `wRoomObjects` addresses.
2. Overlay ROM locations: bank `$26` for rooms `$00-$CB`, bank `$27` for rooms `$CC-$FF`. Each room is `$50` bytes at `$4000 + roomIndex * $50`.
3. `WriteOverworldObjectToBG` (`bank0.asm:$300E`) switches to WRAM bank 2, reads the overlay value, then uses `overlayValue * 4` to index BOTH `OverworldObjectsTilemapCGB` and the per-object attribute table.
4. The BG attribute byte: bits 0-2 = palette, bit 3 = VRAM bank select, bit 5 = flipX, bit 6 = flipY.
5. For fresh rooms, overlay values often match room object IDs. But the overlay is the authoritative source for GBC rendering.

## Overworld BG Tile Addressing

Do not assume the tile ids in `src/data/objects_tilemaps/overworld.cgb.asm` are direct renderer tile indices.

Gameplay overworld BG rendering uses the `0x8800` tile-data mode:

- ids `0x80..0xFF` map directly to tile slots `0x80..0xFF`
- ids `0x00..0x7F` map to tile slots `0x100..0x17F`

This matches the gameplay VRAM loads that populate `vTiles2` with the shared overworld and room-specific BG graphics.

If the room layout looks correct but the tile artwork is clearly from the wrong graphics set, check this before rewriting the room parser again.

Also double-check the VRAM math itself:

- `vTiles1` starts at `$8800`, which is tile slot `0x080`
- therefore `vTiles1 + $400` is tile slot `0x0C0`, not `0x140`

Miscomputing that destination causes the correct tile ids to point at the wrong graphics even when the ROM loads and room-object expansion are otherwise correct.

Loading the right destination from the wrong bank causes a different failure mode:

- the room/object layout will look roughly correct
- but the art inside those shapes will still look shuffled or drawn from the wrong set

For gameplay overworld on GBC, `InventoryOverworldItemsTiles` must come from adjusted bank `0x2C`, not DMG bank `0x0C`.

## Overworld Animated Tiles

The first room-header byte is the animated-tiles group.

The gameplay engine stores it in `hAnimatedTilesGroup`, and the animated-tiles path later overwrites four tiles at:

- `vTiles2 + $6C0` (tile slots `0x16C..0x16F`)

Relevant overworld groups include:

- `0x02` tide
- `0x03` village
- `0x09` water currents
- `0x0A` waterfall
- `0x0B` slow waterfall
- `0x10` weather vane

Room `0x00` uses group `0x0B`.

If these tiles are not loaded, the room layout can still look correct while the visible tile art looks like mismatched quarters from nearby terrain tiles.

## Background Decoder Shape

Encoded BG data is a sequence of commands:

- byte 0: destination high byte
- byte 1: destination low byte
- byte 2:
  - bits `0-5`: length minus 1
  - bit `6`: repeat one byte
  - bit `7`: vertical copy
- remaining bytes: one repeated byte or a byte sequence
- `0x00` ends the stream

Decode to BG-map addresses first, then clamp each decoded 32-byte BG row to the visible width you need, commonly 20 tiles.

## Debugging Checklist

1. Find the exact handler for the screen or tileset.
2. Enumerate every VRAM copy in order.
3. Compute ROM offsets with ROMX math.
4. Adjust banks for GBC if the path uses adjusted bank helpers.
5. Decode the tilemap and attrmap from ROM.
6. Confirm the BG tile addressing mode.
7. Decode the correct palette table from ROM.
8. Check for later animated or overlay writes.

For overworld rooms, replace steps 5 and 6 with this room-specific checklist:

1. Decode the room object stream instead of a title-style encoded background stream.
2. Fill the 10x8 object grid with the room floor object from header byte 1.
3. Parse ordinary objects as `[locationYX, type]`.
4. Parse strip objects as `[directionAndLength, locationYX, type]`.
5. Detect and skip warp records correctly.
6. Expand macros through `ExpandOverworldObjectMacro`; do not skip them.
7. On GBC, load the per-room GBC overlay (banks `$26`/`$27`) and use overlay values for tile and attribute lookups.
8. Expand each final object/overlay value through `overworld.cgb.asm` into a 2x2 BG block.
9. Use the overlay-indexed attribute table entries for palette/flip data on GBC.
10. Select palettes via `OverworldPaletteMap[hMapRoom]`, then `OverworldPalettes`.
10. Select room-specific tiles via `OverworldTilesetsTable[hMapRoom / 4]`.
11. Reconstruct the gameplay overworld VRAM loads from `LoadBaseOverworldTiles`, `func_2D50`, and `LoadRoomSpecificTiles`.
12. Expand macros through the real macro object tables; do not stub them with placeholders.
13. Do not call title-specific helpers such as signed `0x8800` title tile resolution unless the active overworld path explicitly uses them.
