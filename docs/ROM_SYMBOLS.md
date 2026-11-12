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
For GBC bank adjustment call `linksawakening.rom.GbcBanks.adjust(bank)` (it mirrors
`AdjustBankNumberForGBC` in bank 0) at the call site and document the original
loader. Never silently adjust all symbols.

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

## Final owned-world follow-up

RoomTilemapBuilder and OverworldBushInteraction now derive all 27 named ROM
location constants (23 under the original suffix metric) from exact symbols.
All byte reads use Rom; overlay and attribute selection remain unchanged.
Verified IndoorObjectsTilemapCGB 08:43B0, ColorDungeonObjectsTilemap 08:4760,
ColorDungeonBGAttributes 23:6000, BGAttributesPointers_IndoorsA/B
1A:6076/6276, OverworldObjectsTilemapCGB 1A:6B1D,
OverworldBGAttributesBanks 1A:6476, BGAttributesPointers_Overworld 1A:5E76,
IndoorsA/BObjectsAttrmaps 23/24:4000 and RoomGBCOverlaysA/B 26/27:4000.
No symbol discrepancies. World original-metric remaining count is 15.

Integration note (now resolved: `rom.GbcBanks` is public): GbcBanks.adjust was
package-private in gpu at the time. Existing OR-$20 attribute-bank adjustment remains unchanged here:
a RomAddress-based replacement rejected zero-pointer synthetic fixtures before
file-offset translation. Typed reads preserve the prior offset semantics.

BackgroundSceneCatalog now uses symbols for all 30 location pairs across ten
scene specifications (23 distinct exact azle.sym labels; no discrepancies).
The guard scans physics, save, state excluding PlayerState, scene, startup and
config, including a check against literal BackgroundSceneSpec constructors.
These packages have zero remaining literal ROM locations from this audit.

### Former deferred declarations — completed

All 15 declarations formerly assigned here are migrated to RomSymbols and Rom
reads. Exact symbol matches and preserved offsets are recorded in the entity
runtime follow-up below; no discrepancies were found. Map-layout reads now
live in RoomWorldState. There are no open declarations in this assignment.

## Entity runtime follow-up

Migrated the remaining 15 original-metric declarations in WarpMotion,
EntityRoomLoader, RoomSession, EnemyDropResolver and RomEnemyCombatTables.
The room-state extraction moved map-layout access into RoomWorldState; both
files are covered by the guard. Paired address constants and the combat table
locations were migrated too. Exact local azle.sym matches:

- 19:4201 DungeonWarps.
- 16:4000/4200/4400/4600 OverworldEntitiesPointersTable,
  IndoorsAEntitiesPointersTable, IndoorsBEntitiesPointersTable,
  ColorDungeonEntitiesPointersTable.
- 03:6751 BombedCaveDoorTilesIndexesGBC (no bank adjustment in this path).
- 14:4220/44E0/4520 MapLayout0, MapLayout11, MapLayout12.
- 03:4826 DestroyedEntityHealthGroupOffsetTable; 03:559D DropTableByIndex;
  03:55AB RandomDropChanceTable; 03:55B9 RandomDropChanceTableLowHealth;
  03:55C7 DropTableRandom; 02:6308 ThresholdLowHealthTable.
- 14:5218 BowWowEatableEntitiesTable.
- 03:41F6 HealthGroupForEntity; 03:43EC Data_003_43EC;
  03:473C Data_003_473C; 03:47BC InitialHealthForGroup;
  03:47F1 EntityDamagesForGroup.

No discrepancies. Reads use Rom, preserving table lengths, ROM truncation checks,
stream termination, map selection and caller-owned/copy ownership. Existing
unrelated inline ROM locations in RoomSession remain outside this batch.
## Rendering

See [RENDERING.md](RENDERING.md) for compositor rules, parity gates, and the
future `RomOamAllocator` work.

Entity runtime integration checked the public rom.GbcBanks.adjust API after
merging develop. None of the 15 assigned declarations uses an adjusted bank;
BombedCaveDoorTilesIndexesGBC is a fixed bank-03 tile-index table. No new bank
adjustment was introduced.

## Final src/main constant audit

Migrated the last 26 suffix-metric declarations in EntitySpriteCatalog (8),
LinkSpriteSheet (5), DialogTextLoader (4), OwlEventDialogResolver (3),
TransientVfxSpriteSheet (3), SignpostDialogTable (2), SwordSpriteSheet (1).
All match local azle.sym; no address discrepancies. Group pages retain offsets
$100/$200 from RoomSpritesheetGroupsTable (bank20.asm, table local labels
.indoors_a/.indoors_b). Other verified labels: OverworldEntitySpritesheetsTable
20:73F3, IndoorEntitySpritesheetsTable 20:763B, ObjectPalettes 21:5518,
LinkCharacterTiles 0C:4000, CharacterVfxTiles 0C:4200, LinkCharacter2Tiles
0C:5800, LinkAnimationStateTable 20:5319, data_020_5407 20:5407,
DialogPointerTable 1C:4001, DialogBankTable 1C:4741, SignpostDialogTable
14:5118, MapSpecialLocationNamesLookupTable 01:5909,
MapSpecialLocationNamesTable 01:5959 and OverworldMusicTracks 02:4000.
LinkAnimationsLists 02:4910 also replaces an inline location.
Link/sword retain explicit GbcBanks adjustment; transient VFX retains bank 0C.
Typed reads retain range checks and dialog copy ownership. No routes or fixtures
changed. WorldRomConstantGuardTest now scans all Java sources under src/main,
excluding generated RomSymbols, with a documented empty non-ROM allow-list.
The suffix-metric literal declaration count is zero across src/main.
Validation: `./gradlew test romFreeTest --rerun-tasks` passed: 2,438 full tests
and 936 ROM-free tests, zero failures/errors/skips. Generated symbols pass
`--check`. Scene parity reports zero mismatches within its tested scope; no new
emulator replay comparison was performed for this address-preserving refactor.

## ROM access refactor baseline

The starting API exposes four public Rom members (constructor and three reads).
RomTables has 744 lines and 43 public methods. RomUtil/RomBank expose six
byte-array helper overloads (two/four); external main sources contain 156
`byte[] rom*` declarations. Counts are lexical declarations, not unique objects.
