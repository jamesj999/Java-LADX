# Fix sword acquisition spin and beach sword rendering

## Goal

Make the first-time beach sword presentation use the same ROM-driven spin
animation as ordinary sword use, and render the sword lying on the beach with
the ROM's actual OAM palette and transparent color handling.

## Evidence and source of truth

- `LADX-Disassembly/src/code/entities/bank3.asm`: the beach sword uses
  `Data_003_5B97` (`$84, $17`), and the pickup handler sets
  `wIsUsingSpinAttack = $20`.
- `LADX-Disassembly/src/code/bank2.asm`: `LinkDirectionToSwordAnimationState`
  at `$46C9`, `LinkDirectionToAbsolute` at `$46E9`, and
  `LinkDirectionToLinkAnimationState1` at `$4636` define the spin and Link
  body pose lookups.
- `LADX-Disassembly/src/code/home/entities.asm`: on GBC, palette 4 is forced
  only when `hActiveEntityFlipAttribute` requests it; the raw `$17` definition
  attribute otherwise selects palette 7.

## Implementation steps

1. Add focused RED tests for the ROM spin lookup used by the acquisition pose,
   including a non-down starting direction, and for a beach sword with raw OAM
   attribute `$17`, distinct palette 7 colors, and a color-0 transparent pixel.
2. Load the two spin tables into `RomTables` using the existing bank/address
   ROM offset helper, and have the acquisition pose derive its sword state,
   absolute direction, and Link animation state from those tables. Preserve the
   ROM behavior for hidden body-state entries and clear the temporary state at
   the final pickup pose.
3. Keep the existing normal `Sword` animation behavior intact, while advancing
   the registered ROM `Sword` handler for every pickup pose request even before
   `GiveInventoryItem` fills an inventory slot. Render that temporary blade
   through Link during the spin, and do not add the generic `$6A` motion-block
   pose to the handler’s state-2 spin frames.
4. Correct entity palette selection to inspect the active-entity flip request,
   not bit 4 of the already-combined raw OAM attribute. Retain color index 0 as
   transparent and keep the beach sword sourced from gameplay VRAM tile `$84`.
5. Run focused tests, the complete Java test suite, and inspect the final diff
   for unrelated changes.

## Verification

- ROM table tests prove the Java lookups match the disassembly bytes.
- Rendering tests prove palette 7 is used for `$17` and transparent pixels do
  not overwrite the background.
- Existing ordinary sword-spin and entity-render tests remain green.
