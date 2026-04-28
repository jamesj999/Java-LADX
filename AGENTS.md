# Project Agents: Link's Awakening Java Reconstruction

## Overview
This project aims to create a pixel-perfect Java recreation of *The Legend of Zelda: Link's Awakening DX* using the `LADX-Disassembly` repository and the shipped ROM as the source of truth.

## Roles and Responsibilities

### 🛠️ Implementation Agent (Main)
**Primary Task**: Execute the implementation plan, manage the file system, and coordinate with sub-agents.
- **Responsibilities**:
    - Orchestr/Execute the build process.
    - Coordinate data extraction from assembly files.
    - Develop the Java/LWJGL rendering engine and game logic.
    - Run tests and verification processes.

### 🔍 Analysis Agent (Explore)
**Primary Task**: Analyze the `LADX-Disassembly` repository to understand the game engine, data formats, and logic.
- **Responsibilities**:
    - Map out the directory structure of the disassembly.
    - Identify key files containing tile/sprite data and map definitions.
    - Extract constants, IDs, and memory offsets for use in the new implementation.
    - **New Learnings (v0.1)**: 
        - Found pre-rendered PNGs for characters (Link, NPCs), items, and world tilesets (e.g., `ow_mabe_village.cgb.png`) in `src/gfx/`.
        - Identified key physics variables (`hLinkPositionX`, `hLinkPositionY`) in `src/code/` modules like `bank0.asm` and `world_map.asm`.
        - Discovered encoded tilemaps (`.tilemap.encoded`) referenced in `src/data/backgrounds/tilemaps_list.asm`.
    - **New Learnings (v0.2)**:
        - ROMX labels in the disassembly map to file offsets with `bank * 0x4000 + (address - 0x4000)`. Using `bank * 0x4000 + address` reads the wrong data.
        - Title screen background tiles are loaded in stages from ROM: `Intro3Tiles` to `vTiles0`, `TitleDXTilesCGB` to `vTiles0 + 0x400`, and `TitleLogoTiles` to `vTiles1`.
        - Background tile IDs are raw indices. CGB palette selection and flip flags come from the attrmap in VRAM bank 1, not from bits inside the tile ID.
        - Background tilemaps and attrmaps are encoded draw-command streams in ROM and must be decoded before use.
    - **New Learnings (v0.3)**:
        - Overworld rooms are not prebuilt encoded background tilemaps. They are assembled at runtime from room object streams.
        - The overworld room source is `LoadRoom` in `src/code/bank0.asm`, backed by `OverworldRoomPointers` and the room data in `src/data/maps/overworld.asm`.
        - Overworld room parsing is: header byte 0 = animated tiles group, header byte 1 = floor object, then room objects until `ROOM_END` (`0xFE`).
        - Overworld objects expand into 2x2 BG tiles through `src/data/objects_tilemaps/overworld.cgb.asm` on GBC, not through the encoded background loader used by title/menu screens.
        - On GBC, overworld object palette/flip attributes come from the room object attribute tables (`LoadRoomObjectsAttributes`, `GetBGAttributesAddressForObject`), not from the title-screen attrmap path.
        - Overworld palettes are selected per room through `OverworldPaletteMap[hMapRoom] -> OverworldPalettes`, not by reusing the title palette table.
        - Overworld room-specific BG tiles come from the room tileset selected by `OverworldTilesetsTable[hMapRoom / 4]`, layered on top of the shared overworld loads.
    - **New Learnings (v0.4)**:
        - Ordinary overworld room objects are not encoded as `[type, position]`. The byte order is `[position, type]`.
        - Exact object stream formats:
          - 2-byte object: `[locationYX, type]`
          - 3-byte strip object: `[directionAndLength, locationYX, type]`
          - warp entry: first byte where `(byte & 0xFC) == 0xE0`, then 4 more bytes
        - For 3-byte strip objects, the first byte is not an object id. The low nibble is the repeat count and bit `6` selects vertical placement.
        - Overworld macros are detected from the actual object type byte (`type >= 0xF5`), then expanded through `ExpandOverworldObjectMacro`; they must not be skipped.
        - Object id `0x00` is valid in `overworld.cgb.asm`; do not treat `0` as empty.
        - `OverworldPaletteMap` starts at `21:42EF`. `OverworldPalettes` starts at `21:42B1`. Confusing those two tables produces wrong colors immediately.
        - Concrete sanity check: room `Overworld00` at ROM offset `0x24200` begins `0B E5 8A 00 00 8A 10 00 ...`, which means animated group `0x0B`, floor object `0xE5`, then strip objects driven by first bytes `0x8A`, `0x8A`, `0x8A`, not ordinary objects with type `0x8A`.
        - Do not invent an overworld VRAM load from a guessed graphics label. Follow the gameplay overworld load path in `LoadBaseOverworldTiles`, `func_2D50`, and `LoadRoomSpecificTiles`, including the correct VRAM destinations and GBC-adjusted banks.
    - **New Learnings (v0.5)**:
        - The 3-byte strip-object test is performed on the first byte, not the type byte: it is a strip only when `bit7(firstByte) == 1` and `bit4(firstByte) == 0`.
        - For strips, the repeat count comes from `firstByte & 0x0F`, the vertical flag comes from `firstByte & 0x40`, and the actual object type is the third byte unchanged.
        - Concrete parser sanity check for `Overworld00`: the first entries are `strip(0x8A, loc=0x00, type=0x00)`, `strip(0x8A, loc=0x10, type=0x00)`, `strip(0x8A, loc=0x20, type=0xEF)`, then ordinary objects `(0x10,0x7C)`, `(0x11,0x7D)`, `(0x19,0x7C)`.
        - **SUPERSEDED BY v0.13**: The individual VRAM destinations listed here were wrong. See v0.13 for the correct single-bulk-copy approach.
          ~~`OverworldLandscapeTiles` -> `vTiles2 + $200` (tile index `0x120`)~~
          ~~`InventoryOverworldItemsTiles` -> `vTiles1 + $400` (tile index `0x140`)~~
          ~~`InventoryEquipmentItemsTiles` -> `vTiles1` (tile index `0x080`)~~
          ~~`LinkCharacterTiles + $200` -> `vTiles0 + $200` (tile index `0x020`)~~
        - Room-specific overworld BG tiles come from `Overworld2Tiles` via `LoadRoomSpecificTiles` and are copied to `vTiles2` (tile index `0x100`) using `OverworldTilesetsTable[hMapRoom / 4]`.
    - **New Learnings (v0.6)**:
        - Do not hand-wave macro expansion. `ExpandOverworldObjectMacro` is data-driven and expands to real object ids such as trees (`0x25..0x2A`), houses (`0x52`, `0x55..0x5B`, `0xE2`), palace doors (`0xA4..0xA8`, `0xE3`), palm trees (`0xB6`, `0xB7`, `0xCD`, `0xCE`), and walled pits (`0x2B`, `0x2C`, `0x2D`, `0x37`, `0x38`, `0xE8`, `0x0A`, `0x33`, `0x2F`, `0x34`).
        - A fake macro implementation that mostly writes placeholders like `0x5A`, `0x09`, or `0x06` is wrong even if the parser reaches the macro byte correctly.
        - `OverworldTilesetsTable` has 64 entries, one per `hMapRoom / 4`. A tiny hardcoded offsets array is not a substitute.
        - `W_TILESET_KEEP` is `0x0F`, which means "do not overwrite the room-specific BG region". Treating it like an actual tileset blob is wrong.
    - **New Learnings (v0.7)**:
        - `TreeMacroHandler` is not a fixed 2x2 object stamp. It checks the existing room-object grid and can emit overlap variants (`0x29`, `0x2A`) or bush variants (`0x82`, `0x83`) depending on what is already underneath the tree.
        - Tree macros can also represent a strip of trees. The handler steps horizontally or vertically according to the strip metadata instead of always painting exactly one tree.
        - `StonePigHeadMacroHandler` is stateful: it uses intact ids (`0xBB..0xBE`) unless room status says the pig head is blasted, in which case it writes `0x09`.
        - Macro expansion is offset-table driven. `CopyOutdoorsMacroObjectsToRoom` uses explicit object offsets from the disassembly; do not ignore those offsets and re-place ids as a simple rectangle from `(startX,startY)`.
        - `OverworldTilesetsTable` is defined in `src/code/bank20.asm`. If you hardcode a Java literal array or read it from bank `0x21`, you are no longer following the disassembly as the source of truth.
    - **New Learnings (v0.8)**:
        - Overworld room objects do not live in a flat `10x8` buffer while loading. The game decodes them into `wRoomObjectsArea`, a padded `0x10`-stride buffer with the active room starting at offset `0x11`.
        - `FillRoomMapWithObject` fills exactly the active columns inside that padded buffer. Trees and macro offsets rely on the surrounding `0xFF` border being present.
        - For ordinary 3-byte strip objects, the low nibble of the first byte is the exact object count, not `count + 1`.
        - For tree strips, each tree is `2x2`, so the handler advances by two columns or two rows per tree, not by one cell.
    - **New Learnings (v0.9)**:
        - Overworld object tilemap bytes are BG tile numbers for the gameplay `0x8800` tile data mode, not direct VRAM tile indices.
        - That means overworld tile ids `0x00..0x7F` resolve to tile slots `0x100..0x17F`, while `0x80..0xFF` map directly.
        - If the room layout looks plausible but every overworld tile graphic is obviously from the wrong set, the usual bug is treating `overworld.cgb.asm` tile ids as unsigned direct indices.
    - **New Learnings (v0.10)**:
        - `vTiles1 + $400` is tile index `0x0C0`, not `0x140`. `vTiles1` begins at VRAM address `$8800`, so adding `$400` bytes advances by `0x40` tiles, not `0xC0`.
        - For gameplay overworld loads, `InventoryOverworldItemsTiles` must go to tile slots `0x0C0..0x0FF`. Loading it at `0x140` corrupts the shared BG tile region and makes room tiles appear in the wrong order even when the room parser is correct.
    - **New Learnings (v0.11)**:
        - `InventoryOverworldItemsTiles` is still part of the GBC-adjusted gameplay load path. On GBC it must be read from bank `0x2C`, not DMG bank `0x0C`.
        - If overworld structures are recognizable but their tile patterns are still scrambled inside the right shapes, check for a shared BG block still being loaded from an unadjusted DMG bank.
    - **New Learnings (v0.12)**:
        - The first room-header byte (`hAnimatedTilesGroup`) is not optional metadata. Gameplay uses it to overwrite four BG tiles at `vTiles2 + $6C0` before rendering animated overworld features.
        - Room `0x00` uses animated group `0x0B` (slow waterfall). Ignoring that header byte leaves the waterfall/shared terrain tile slots showing the wrong art even when the static tileset loads are otherwise correct.
    - **New Learnings (v0.13)**:
        - **VRAM destinations in v0.5 were wrong.** The game's `LoadBaseTiles` (`bank0.asm:$2BCF`) performs a SINGLE contiguous copy of `0x100` tiles (`$1000` bytes) from `InventoryEquipmentItemsTiles` (`$4800` in bank `$0C`/`$2C`) to `vTiles1` (`$8800` = VRAM tile `0x080`). This one bulk copy loads equipment items, inventory items, instruments, AND landscape tiles into VRAM tiles `0x080-0x17F` at the correct offsets. In particular, `OverworldLandscapeTiles` (at ROM `$5400`) lands at VRAM tile `0x140` — NOT `0x120`. Loading landscape tiles to `0x120` shifts every tile reference by 0x20 slots and produces garbled/mirrored graphics.
        - Do not load landscape tiles, inventory items, and equipment as separate `loadTilesFromROM` calls with hand-picked VRAM destinations. Replicate the game's single bulk copy to get the correct interleaved layout.
        - On GBC, the tile bank is adjusted via `AdjustBankNumberForGBC` (`bank | $20`), so `$0C` becomes `$2C`. The CGB tile data has the same file layout and offsets as the DMG bank, so using DMG addresses with the CGB bank number is correct.
    - **New Learnings (v0.14)**:
        - On GBC, overworld BG tile IDs and attributes are NOT driven by the room object types directly. They come from a per-room **GBC overlay** table.
        - `LoadRoomObjectsAttributes` (`bank20.asm:$6DAF`) copies 80 bytes (10 cols x 8 rows) from the GBC overlay ROM data into WRAM bank 2 at the `wRoomObjects` addresses.
        - `WriteOverworldObjectToBG` (`bank0.asm:$300E`) reads the overlay value from WRAM bank 2 (not bank 0) and uses `overlayValue * 4` to index BOTH `OverworldObjectsTilemapCGB` for tile IDs AND the per-object attribute table for BG attributes (palette + flip flags).
        - GBC overlay data is stored in ROM bank `$26` (rooms `$00-$CB`) and bank `$27` (rooms `$CC-$FF` plus alternate overlays for some rooms). Each room is `$50` bytes at address `$4000 + roomIndex * $50`.
        - For a fresh room with no state changes, overlay values often match room object IDs — but the overlay is the authoritative source for GBC rendering.
        - The BG attribute byte format: bits 0-2 = palette, bit 3 = VRAM bank select, bit 5 = flipX, bit 6 = flipY, bit 7 = priority.
        - FlipX and flipY flags in the rendering code (`attributes & 0x20`, `attributes & 0x40`) are correct. The fix is ensuring the attribute LOOKUP uses the overlay value, not the object ID.
    - **New Learnings (v0.15)**:
        - Animated BG tiles are pure tile-data swapping, not palette animation. The game reserves 4 VRAM tile slots at index `0x16C` (`vTiles2 + $6C0`) and copies the next 0x40-byte frame in at a group-specific cadence.
        - `AnimatedTiles` (bank `0x0C` / CGB `0x2C`, offset `$6A00`) is a flat blob of 18 groups × `$100` bytes. Each group holds 4 frames × 4 tiles × 16 bytes. Group number does NOT equal blob index: see the `HIGH(AnimatedTiles) + N` loads in `home/animated_tiles.asm` (e.g., `0x0B` slow waterfall → blob offset `0x000`, `0x02` tide → `0x100`, `0x10` weather vane → `0xD00`).
        - Timing masks: slow (& `$0F`, every 16 frames) for groups `$02, $03, $05, $0B, $0C`; medium (& `$07`, every 8) for `$08, $0E, $0F, $10`; fast (& `$03`, every 4) for `$09, $0A, $0D`. Groups `$04` and `$06` use (& `$07`) timing but pick offsets from the ping-pong table `[0, $40, $80, $C0, $C0, $C0, $80, $40]` indexed by `(frameCount >> 3) & 7`. Groups `$01`, `$07`, `$11` are special-case and not handled by the generic path.
        - `hAnimatedTilesFrameCount` and `hAnimatedTilesDataOffset` are NOT reset on room change; only the group ID is updated. The next animation tick naturally re-populates VRAM `0x16C` with the new group's tiles at the carried-over offset.
        - Animation ticking is gated in the original: skipped while `wRoomTransitionState != 0` (room transitions) or when the inventory window overlaps the screen. Only `DrawLinkSprite` runs during those gates.

### 🎨 Asset Pipeline Agent (General)
**Primary Task**: Transform raw assembly/binary data into web-ready assets.
- **Responsibilities**:
    - Build tools to convert `.asm`/`.gb` tiles into PNG spritesheets.
    - Create parsers for map data and item configurations.
    - Generate metadata for the Java renderer while keeping ROM-driven loading paths authoritative.

## Project Guidelines
- **Accuracy**: The reconstruction must be pixel-perfect regarding tile placement, sprite animation, and palette usage.
- **Performance**: Use hardware-accelerated rendering where practical, but keep the decoded data model faithful to the Game Boy hardware.
- **Source of Truth**: Always refer back to `LADX-Disassembly` and the ROM for any logic or data discrepancies.
- **ROM-Only Runtime Data**: Final runtime code should load tiles, tilemaps, attrmaps, and palettes from the ROM rather than from premade PNG exports.
- **Overworld Rendering Rule**: Do not treat overworld screens like title/menu/background screens. Overworld screens must be built from room object streams, object-to-2x2 tile tables, room object attributes, and per-room palette/tileset selection.
- **Overworld Parser Rule**: If an implementation reads an ordinary overworld object as `[type, position]`, skips macros, ignores warp records, or treats object `0` as blank, the implementation is wrong.
- **Overworld Macro Rule**: If an implementation replaces `ExpandOverworldObjectMacro` with a hand-written `switch` that paints guessed shapes, especially for trees, it is wrong.
- **Overworld Tileset Rule**: If an implementation replaces `OverworldTilesetsTable` with a Java literal array instead of loading or mirroring the real 64-entry table from `bank20.asm`, it is wrong.
- **Overworld Buffer Rule**: If an implementation loads overworld objects directly into a flat `10x8` grid instead of the padded `wRoomObjectsArea` layout, tree macros and offset-based macro objects will be wrong.
- **Overworld BG Addressing Rule**: If an implementation renders overworld object tile ids from `overworld.cgb.asm` as direct unsigned tile indices instead of `0x8800` signed BG indices, the graphics will be wrong even when the room parser is correct.
- **Overworld VRAM Math Rule**: Convert VRAM byte addresses to tile slots from the actual destination base. Do not treat `vTiles1 + $400` as tile index `0x140`; it is `0x0C0`.
- **Overworld Bank Rule**: The shared gameplay overworld blocks loaded through `SwitchAdjustedBank` stay on the adjusted GBC bank for subsequent copies. Do not quietly load `InventoryOverworldItemsTiles` from DMG bank `0x0C` on GBC.
- **Overworld Animated-Tiles Rule**: If a room header supplies `hAnimatedTilesGroup`, load the corresponding animated BG tiles into `vTiles2 + $6C0`. Do not ignore that byte just because you are rendering a static screenshot.
- **Overworld Base Tiles Rule**: Do not load landscape tiles, inventory items, and equipment as separate calls with hand-picked VRAM destinations. The game's `LoadBaseTiles` performs a single contiguous copy of `0x100` tiles from `InventoryEquipmentItemsTiles` (`$4800`) to `vTiles1` (`$8800` = VRAM tile `0x080`). This bulk copy places landscape tiles at VRAM `0x140`, not `0x120`. Splitting it into separate loads with wrong destinations shifts every tile reference and produces garbled output.
- **Overworld GBC Overlay Rule**: On GBC, overworld tile and attribute lookups must use the per-room GBC overlay value, not the room object ID. The overlay is a separate 80-byte table per room (banks `$26`/`$27`) that `WriteOverworldObjectToBG` reads from WRAM bank 2. If tile graphics look correct individually but appear mirrored or in wrong positions, the overlay lookup is likely missing or using the wrong index.

## Project Skills
- Reusable project-local skill for ROM graphics debugging lives at `skills/ladx-rom-tileset-debug/`.
- Use it when tile placement is correct but the graphics, palette, VRAM destination, or tile addressing mode appear wrong.
