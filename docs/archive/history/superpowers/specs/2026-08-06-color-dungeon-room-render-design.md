# Color Dungeon room-facing ROM path

## Goal

Make the Java room renderer use the dedicated Color Dungeon data selected by
the disassembly for static GBC room composition. A Color Dungeon room must not
fall through to the ordinary indoor object tilemap, ordinary indoor attribute
pointer tables, or an arbitrary indoor palette entry.

## Source of truth

The dedicated data boundaries are:

- `ColorDungeonObjectsTilemap` at bank `$08`, address `$4760`, four tile bytes
  per object;
- `ColorDungeonBGAttributes` at bank `$23`, address `$6000`, four attribute
  bytes per object;
- `Data_021_67D0` at bank `$21`, address `$67D0`, the Color Dungeon room
  palette block (eight BG palette rows are consumed by the room renderer);
- `ColorDungeonTiles` at bank `$35`, with the fixed floor/item blocks at
  `$6000`, `$6100`, and `$6200`;
- `data_020_45EA` at bank `$20`, address `$45EA`, the room-selected BG tile
  source table used by `GetColorDungeonTilesAddress`;
- `ColorDungeonWallsTilesPointers` at bank `$20`, address `$45C9`, selecting
  the Color Dungeon wall source row.

The Java renderer's existing `RoomTilemapBuilder` writes each room object as a
2x2 tile block. For map `$FF`, it will keep that geometry but select the
dedicated object tilemap and direct object-attribute table. The lookup value
remains the room object's byte, matching `WriteIndoorObjectToBG` and
`GetBGAttributesAddressForObject` for Color Dungeon rooms.

## Design

`RoomPaletteLoader.loadIndoor` will special-case map `$FF` and decode the
palette block at `$21:$67D0`, instead of indexing the ordinary indoor palette
map table with `$FF`. `RoomTilemapBuilder` will special-case map `$FF` for the
object tile and attribute bases, while leaving all other indoor maps unchanged.

`GPU.loadIndoorTiles` will dispatch map `$FF` to a dedicated static loading
path. That path will load the room-selected 16-tile BG row from the ROM table
at `$20:$45EA` into VRAM tile slot `$100`, the fixed Color Dungeon floor block
from `$35:$6000` into slot `$110`, the shared dungeon block and selected wall
row into the existing shared slots, and the Color Dungeon item block from
`$35:$6100` into slot `$0F0`. It will use the fixed Color Dungeon tile bank and
the bank byte supplied by the room-source table; switch-block state and the
separate symbol/OAM reload path remain outside this slice because the current
Java runtime does not yet own those mutable hardware states.

No new room or renderer abstraction is needed. `RoomLoader` will continue to
pass the loaded palette/tile arrays through `LoadedRoom`, `ActiveRoom`, and
`RoomRenderSnapshot`, so the live scene builder automatically consumes the
correct data.

## Verification

Tests will verify:

1. a synthetic ROM makes map `$FF` palette loading read `$21:$67D0`;
2. a synthetic room object uses the Color Dungeon tile and attribute tables,
   not the ordinary indoor tables;
3. Color Dungeon GPU loading uses the room-selected BG source, fixed floor and
   item blocks, shared dungeon tiles, and the dedicated wall pointer;
4. the existing shipped-ROM Color Dungeon room path still loads its entities
   and produces the dedicated room data;
5. the complete Gradle Java suite and whitespace checks pass.

## Non-goals

This slice does not implement Color Dungeon switch-block state changes,
symbol animation, dynamic palette effects, palette-transition timing, or the
remaining Color Dungeon event scripts. Those require the corresponding WRAM
state and VBlank ordering and remain separate follow-up work.
