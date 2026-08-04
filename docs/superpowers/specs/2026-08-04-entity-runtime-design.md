# ROM-Driven Room Entity Runtime

## Context

The Java reconstruction currently loads room geometry, palettes, Link, effects, and a small set of interactive objects, but it does not load the room entity lists that populate NPCs, enemies, pickups, and scripted actors. This leaves ordinary gameplay rooms visually and behaviorally incomplete even when their background tilemaps are correct.

The LADX disassembly is the source of truth. The implementation must preserve its room pointer-table selection, definition order, 16-slot entity model, position conversion, sprite-sheet selection, and OAM tile addressing without introducing a CPU or Game Boy emulator.

## Goals

This increment will provide the runtime foundation for room entities:

1. Read overworld, indoors-A, indoors-B, and Color Dungeon entity pointer tables from the ROM.
2. Decode each room's two-byte entity definitions until `ENTITIES_END`, preserving source order and the original disabled-slot allocation behavior.
3. Apply the original cleared-room mask rule for the first eight load-order entries.
4. Expose entity slot state, position, type, load order, and an explicit renderability/handler status to the room session.
5. Select the room's four OAM sprite sheets from `RoomSpritesheetGroupsTable` and the corresponding entity-sheet table.
6. Copy sheet data from `NpcTilesDataStart` into the same four VRAM sprite-slot ranges used by the game, with GBC-adjusted source banks.
7. Load the always-resident object palettes from ROM and render supported pair display lists with Game Boy 8x16 sprite semantics, transparency, flips, palette selection, clipping, and room-scroll offsets.
8. Integrate entity loading and rendering into room changes and the gameplay frame scene.

Unsupported entity handlers will be represented explicitly as non-rendering slots. They will not receive guessed placeholder art or guessed behavior. This keeps the visible result honest while making the loader and sprite pipeline reusable for subsequent handler increments.

## Non-goals

This increment does not port every entity AI, collision rule, state transition, item drop, dialog trigger, scripted spawn, follower system, or boss phase. It also does not emulate the LR35902 CPU, PPU, OAM DMA, or hardware timing.

The first render adapter will cover the simple pair-display-list family needed to prove the ROM-backed pipeline (including common crow, dog, and Marin variants). More complex rectangle, single-sprite, dynamically selected, and stateful handlers will be added as separate disassembly-driven increments.

## Design

### Room entity data

`EntityRoomLoader` will use bank `$16` and the pointer-table addresses used by `LoadRoomEntities`:

| Room kind | Pointer table address |
| --- | ---: |
| Overworld | `$4000` |
| Indoors A | `$4200` |
| Indoors B | `$4400` |
| Color Dungeon | `$4600` |

The selected pointer is a little-endian CPU address in bank `$16`; the entity stream is `[location, type]` pairs terminated by `$FF`. The location byte follows the disassembly exactly: the high nibble is the vertical coordinate and the low nibble is the horizontal coordinate. The runtime pixel positions are `x = (location & $0F) * $10 + $08` and `y = (location & $F0) + $10`.

The loader creates up to `MAX_ENTITIES = $10` slots. For each definition it increments the source load order even when the definition is filtered or no slot is available. For load orders `0..7`, the corresponding one-bit mask is tested against the caller-provided cleared-room byte. Loaded slots begin in `ENTITY_STATUS_INIT`; empty slots are `ENTITY_STATUS_DISABLED`.

The room model will carry the immutable definitions/slot snapshot. Entity runtime state will be mutable so later behavior handlers can update positions, status, direction, sprite variant, Z offset, and interaction state without changing the room parser.

### Sprite-sheet catalog and VRAM

`EntitySpriteCatalog` will mirror the original room selection path:

- `RoomSpritesheetGroupsTable` in bank `$20`, base `$70D3`, with overworld, indoors-A, and indoors-B sections selected by room kind/map range.
- `OverworldEntitySpritesheetsTable` at `$73F3` for overworld rooms.
- `IndoorEntitySpritesheetsTable` at `$763B` for indoor rooms.

Each sheet byte is decoded as `bbtttttt`. The two-bit bank selector indexes the disassembly's `NpcTilesBankTable` (`$00`, `$11`, `$0E`, `$12` on DMG; `$00`, `$31`, `$2E`, `$32` on GBC). The six-bit row selector selects a `$100`-byte sheet from `NpcTilesDataStart` at CPU address `$4000`.

The GPU will expose a ROM-backed entity-sheet load operation that copies each selected 16-tile sheet into the original sprite slots:

- slot 0: VRAM tile `$40`
- slot 1: VRAM tile `$50`
- slot 2: VRAM tile `$60`
- slot 3: VRAM tile `$70`

The runtime will load all four slots on a room load. `$FF` means keep the current slot; for a fresh Java room load it leaves the slot unchanged, matching the source table semantics. Color Dungeon's special tile path remains an explicit follow-up because its hardcoded loader is separate from the standard four-sheet path.

### Display-list rendering

The first adapter will represent a pair display list as ROM-backed `[tile, attributes]` pairs grouped by variant. It will read the list from the bank/address documented at the handler's `ld de, ...SpriteVariants` instruction. The adapter will not copy these tables into Java literals.

For each pair, the renderer will reproduce `RenderActiveEntitySpritesPair`:

- both OAM entries use the entity visual Y;
- the two 8x16 columns occupy X and X+8, swapping when the entity flip bit is set;
- the tile byte is an already-resolved VRAM tile number, plus any entity tile offset;
- attributes combine the display-list byte with the entity flip attribute;
- low attribute bits select one of the eight ROM object palettes;
- bits `$20`/`$40` flip X/Y, and color-zero pixels are transparent;
- off-screen pixels are clipped to the 160x144 framebuffer.

The render layer will draw after the room and before transient effects/HUD, with Link ordering kept stable until an OAM-order comparison establishes a narrower adjustment. During room scroll, entity positions use the same interpolated screen offset as Link and the room layers.

### Behavior boundary

The runtime will distinguish `SUPPORTED_PAIR`, `UNSUPPORTED_HANDLER`, and `DISABLED` rather than making every loaded type look like a generic 16x16 placeholder. This makes missing handler coverage observable in tests and prevents fabricated art from masking incorrect ROM mapping. A later handler registry can add stateful behavior without changing room parsing, sheet loading, or renderer contracts.

## Error handling

Malformed pointer-table addresses, unterminated streams, invalid VRAM ranges, and ROM reads outside the supplied byte array will fail with descriptive `IllegalArgumentException`s. `0xFF` sheet entries are valid and are skipped. A room with no entity definitions is valid and returns sixteen disabled slots.

## Verification

Tests will cover:

- synthetic pointer tables and definition streams, including position nibbles, load-order increments, cleared masks, disabled-slot reuse, and the 16-slot limit;
- real-ROM integration for overworld room `$00` and configured room `$92`;
- table-section selection and exact sheet-byte decoding;
- GBC source-bank adjustment, sheet source offsets, destination tile slots, and VRAM byte equality;
- object-palette decoding from the ROM;
- pair display-list decoding, 8x16 tile order, flips, transparent pixels, palette selection, and clipping;
- room-session replacement on initial load, warp, and adjacent scroll;
- scene-layer inclusion without changing title/cutscene layer behavior.

The focused tests will run with `gradle test --tests ...` from `java/`, followed by the full `gradle test` suite.

## Follow-up increments

After this foundation is verified, the next increments should port handler families in disassembly order: static NPCs and pickups, simple enemies, movement/collision state, entity-cleared persistence, scripted actors/followers, rectangle/single-sprite render helpers, and finally bosses and multi-entity events.
