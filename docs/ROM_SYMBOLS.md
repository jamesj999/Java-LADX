# ROM symbols

This open source engine reimplementation requires a user-supplied ROM. Never
check in ROM bytes or extracted assets.

Regenerate from a locally built RGBDS symbol file:

    python3 tools/rom/gen_symbols.py /path/to/LADX-Disassembly/azle.sym

Use `--check` to verify without writing. Normal builds need no disassembly.
`symbols.txt` is a curated allow-list of exact labels, one per line; blank lines
and full-line `#` comments are permitted. Missing and duplicate labels fail.

Use `RomSymbols.Label` with `Rom.u8`, `u16le`, or `slice`. `Rom` is a read-only
view of caller-owned bytes; callers must preserve the backing array. Slice
returns a copy. `RomAddress.offset()` uses the central bank-window calculation.
For GBC bank adjustment use an explicit `.inBank(label.bank() | 0x20)` at the
call site and document the original loader. Never silently adjust all symbols.

For world, gpu, render, and entity migration, first match the exact label and
bank against the local symbol file, retain any intentional offset, then replace
the numeric pair. Report mismatches before changing behavior. Do not substitute
an address merely because another label has a similar name.

`RomAddress.fromOffset` bridges existing file-offset cursors to typed reads;
`plus` advances in file order and `withGbcBank()` explicitly mirrors the original
GBC bank adjustment. Retain range checks at format boundaries for useful errors.

Audio clients can use `FrameAudioPlayer` with the existing `PcmSoundOutput`
interface. It advances music and SFX once per `update()` (60 Hz), renders an
integer number of samples with a carried remainder, and routes dialog PCM to
the same output. The caller owns the output lifetime. The legacy OpenAL browser
transport remains available with its existing sample-driven buffering behavior.

## Room data migration checkpoint

OverworldTilesetTable uses typed Rom reads; IndoorRoomPointerTables uses
RomSymbols. Seven literal location constants removed. The guard currently covers
these two files with an empty non-ROM allow-list. Remaining owned files still
require migration; this checkpoint does not claim a completed package audit.

Verified against azle.sym: OverworldTilesetsTable = 20:6E73,
OverworldPaletteMap = 21:42EF, OverworldPalettes = 21:42B1; indoor pointers
are 0A:4000, 0B:4000 and 0A:7B77. No discrepancies in these locations.

Remaining literal location declarations by package (not a complete ROM-access
audit; includes other owners and genuine non-ROM constants):

- entity: 6 declarations in 2 files: EntitySpriteCatalog, LinkSpriteSheet.
- equipment: 1 declarations in 1 files: SwordSpriteSheet.
- gameplay: 6 declarations in 2 files: SignpostDialogTable, DialogTextLoader.
- gpu: 13 declarations in 3 files: AnimatedTiles, EntitySheetTileLoader, RoomTileLoader.
- render: 1 declarations in 1 files: EntityRenderLayer.
- vfx: 3 declarations in 1 files: TransientVfxSpriteSheet.
- world: 86 declarations in 14 files: WarpMotion, RoomObjectParser, EntityRoomLoader, RoomSession, EnemyDropResolver, RomEnemyCombatTables, ChestContentsTable, DungeonRoomEventTable, ManboTransitionWave, RoomLoader, RoomPaletteLoader, ManboWarpResolver, OverworldBushInteraction, RoomTilemapBuilder.

Next batch: DungeonRoomEventTable, ManboTransitionWave and ManboWarpResolver
use typed reads. Seven location declarations replaced; exact azle.sym matches:
14:4000 DungeonEventsTable, 14:4200 ColorDungeonEventsTable,
14:4EE8 Data_014_4EE8 and 14:4DF1 Data_014_4DF1. No discrepancies.
Guard now also rejects _ADDRESS literals. Physics source audit found no
RomBank accesses, raw romData reads, or bank-window address literals.

ChestContentsTable: six location declarations migrated, preserving table bounds
and page selection. Verified exact matches 14:4560 RoomChestsTable,
14:4860 ColorDungeonRoomChestsTable, 07:7B99 GotItemDialog and
07:7BBB Data_007_7BBB. No discrepancies; guard expanded to this file.
Remaining original-metric world declarations: 81 (the metric omits _ADDRESS).

RoomLoader migrated three location declarations and all ROM byte reads.
Verified 09:4000 OverworldRoomPointers and 1A:4000 OverworldRoomsSecondHalf.
No discrepancies; pointer/page selection unchanged. Guard expanded.
Remaining original-metric world declarations: 79 (BANK_SECOND_HALF was never
included in that metric). This turn has migrated 16 location declarations.

RoomObjectParser migrated 23 named declarations plus inline table locations,
verified at 54 exact azle.sym labels. Template, door, macro and shutter data
retain identical addresses; no discrepancies. All byte reads are typed.
Guard covers the parser and all physics sources; both suites pass.
Remaining original-metric world declarations: 56; this turn removed 39 named
location declarations (including ADDRESS and BANK_SECOND_HALF variants).

RoomPaletteLoader migrated 18 declarations and inline palette-transition locations
at 15 exact symbol addresses; no discrepancies. Typed reads preserve decoding
and bounds. The historical palette-map/table distinction is now symbolic.

Current original-metric literal declaration totals:

- entity: 6.
- equipment: 1.
- gameplay: 6.
- gpu: 13.
- render: 1.
- vfx: 3.
- world: 38.

World remaining: RoomTilemapBuilder 17, OverworldBushInteraction 6;
entity-runtime-owned WarpMotion 1, EntityRoomLoader 1 and RoomSession 6.
EnemyDropResolver 6 and RomEnemyCombatTables 1 left untouched because their
entity/combat ownership is ambiguous. Physics has zero matching declarations.
This turn migrated 57 named declarations across seven files, plus inline tables.
