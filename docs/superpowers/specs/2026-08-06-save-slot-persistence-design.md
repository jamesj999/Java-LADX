# ROM-Shaped Save-Slot Persistence Design

**Date:** 2026-08-06  
**Status:** Approved for implementation as a bounded reconstruction slice

## Goal

Replace the file menu's hardcoded empty-save input and initialized-slot
exception with a persistent host representation of the shipped game's SRAM
save area. The Java engine must discover names and initialized slots from
source-shaped bytes, write a newly created slot at the same offsets as the
ROM, and load the modeled player/spawn state of an existing slot.

This slice does not invent a Java save format. Unknown main-block and DX bytes
remain in the raw SRAM image so later systems can decode them without a format
migration.

Copy/erase screens, exact save-on-progress triggers, death-count updates, and
unmodeled WRAM fields remain separate increments.

## Source of truth

The implementation follows:

- `LADX-Disassembly/src/constants/memory/sram.asm`
  - skipped SRAM prefix `$100`
  - `SAVE_PREFIX_SIZE=$05`, `SAVE_MAIN_SIZE=$380`,
    `SAVE_DX1_SIZE=$05`, `SAVE_DX2_SIZE=$20`, `SAVE_DX3_SIZE=$03`
  - three slots with stride `$3AD`
- `LADX-Disassembly/src/constants/memory/wram.asm`
  - the main block begins at `wOverworldRoomStatus` (`$D800`)
  - modeled field offsets through `wSaveSlotNames` (`$DB80`)
- `LADX-Disassembly/src/code/bank1.asm`
  - `InitSaveFiles`/`func_001_4794` prefix validation and initialization
  - `LoadSavedFile` at `$52A4`
  - `SaveGameToFile` at `$5DE6`
- `LADX-Disassembly/src/code/file_menus.asm`
  - `FileSelectionExecuteChoice` at `$4995`
  - `FileCreationInteractiveHandler` at `$4A9B`
  - `func_5DC0`, which derives `wSaveFilesCount` from nonzero names.

## Exact layout

The host image contains only the source-defined save area, not a fabricated
CPU address space:

| Region | Offset within slot | Size |
| --- | ---: | ---: |
| prefix | `$000` | `$05` |
| main | `$005` | `$380` |
| DX1 | `$385` | `$05` |
| DX2 | `$38A` | `$20` |
| DX3 | `$3AA` | `$03` |

Slot 0 begins at image offset `$100`; slots 1 and 2 begin at `$4AD` and
`$85A`. The image ends at `$C01`.

Main offsets used by the current Java model are derived from WRAM `$D800`:

- B/A/subscreen inventory: `$300`/`$301`/`$302..$30B`
- seashells: `$30F`; shield: `$344`; arrows: `$345`
- magic powder: `$34C`; bombs: `$34D`; sword: `$34E`
- five-byte name: `$34F..$353`
- death count: `$357..$359`
- health/max hearts/heart pieces: `$35A..$35C`
- rupees high/low: `$35D..$35E`
- spawn indoor/map/room/X/Y/indoor room: `$35F..$364`
- max powder/bombs/arrows: `$376..$378`

DX3 stores tunic/photos at offsets `$00..$02` within that region. A valid
prefix is exactly `[1,3,5,7,9]`; a slot is presented as initialized only when
its prefix is valid and at least one of its five stored name bytes is nonzero,
matching the menu's name scan after `InitSaveFiles` validation.

## Architecture

`SaveRamLayout` owns the source-derived constants and checked offset math.
`SaveRamImage` owns a defensive raw byte array, prefix initialization,
slot-mask/name discovery, new-file writes, and decoded `SaveSlotState` views.
`SaveRamStore` adds a host `Path` adapter: it loads the image if present,
initializes a missing/corrupt image like `InitSaveFiles`, and writes the exact
image back on slot creation. The default runtime path is a small application
save file under the user's home directory; tests inject a temporary path or
an in-memory image.

`SaveSlotState` is immutable and contains the modeled player fields plus
spawn/map fields and the raw slot bytes. `PlayerState.applySavedGame` applies
only fields currently represented by Java, leaving future fields in the raw
image. If a loaded save has a zero spawn X (the state produced immediately by
the ROM's new-file screen), Main uses the existing `NewGameStartProfile`
runtime initialization; otherwise it loads the saved indoor or overworld room
and uses the saved position.

`Main` opens one `SaveRamStore` during menu initialization. File selection
receives its mask and names from the image. Committing `START_NEW_GAME`
writes the selected slot's exact pre-load bytes and flushes the store before
entering the existing new-game bootstrap. `LOAD_GAME` decodes the selected
slot and enters the saved room rather than throwing. No save state is copied
into a parallel serialized DTO.

## Error handling

Malformed image lengths, invalid slot indices, out-of-range field writes, and
short names fail with descriptive exceptions in the pure image layer. Host
I/O failures are surfaced to startup/action code instead of silently turning a
real save into an empty menu. All byte arrays and decoded names are defensive
copies.

## Verification

- Layout tests prove slot stride, skipped SRAM base, all modeled offsets,
  prefix validation, and defensive copies.
- Image tests prove empty initialization, new-file bytes (`name`, health
  `$18`, max hearts `$03`, zero death count), mask/name discovery, and
  round-trip host persistence through a temporary path.
- Player-state tests prove decoded modeled fields replace state without
  changing unrelated runtime buffers.
- Main/file-menu flow tests prove startup hydrates the controller from the
  store, new-file commit writes/flushed the selected slot, and initialized
  slot selection reaches saved-room loading.
- `gradle clean test` and `git diff --check` must pass.

