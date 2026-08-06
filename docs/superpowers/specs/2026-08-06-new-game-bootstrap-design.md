# ROM-Backed New Game Bootstrap Design

**Date:** 2026-08-06  
**Status:** Approved for implementation as a bounded reconstruction slice

## Goal

Make the file-menu `START_NEW_GAME` boundary initialize the Java runtime like
the shipped ROM's `LoadSavedFile.initNewGame` path. A newly created file must
enter Marin's House with the source entry coordinates and fresh-game player
state, rather than falling through to the configured debug overworld room.

This slice ends at the in-process gameplay bootstrap. SRAM layout and
persistence, loading an initialized save, copy/erase commands, and the exact
file-menu fade/audio sequence remain separate slices.

## Source of truth

The implementation follows:

- `LADX-Disassembly/src/code/bank1.asm`
  - `LoadSavedFile` at bank `$01:$52A4`
  - `.initNewGame` at bank `$01:$5394`
  - `MaxHeartsToStartingHealthTable` at bank `$01:$5295`
- `LADX-Disassembly/src/code/world_handler.asm`
  - `GameplayWorldLoadRoomHandler`, which copies the map entrance position
    directly into Link's runtime position
- `LADX-Disassembly/src/constants/memory/wram.asm` for the meaning of the
  initialized player fields.

The exact new-game values are:

| Runtime value | ROM value |
| --- | ---: |
| map id (`MAP_HOUSE`) | `$10` |
| map room (`ROOM_INDOOR_B_MARIN_HOUSE`) | `$A3` |
| Link entry X | `$50` |
| Link entry Y | `$60` |
| max arrows | `$30` |
| max bombs | `$30` |
| max magic powder | `$20` |
| Link direction (ROM encoding) | down (`$03`) |
| Link animation state | standing down (`$00`) |
| wrecking-ball room | `$16` |
| wrecking-ball X/Y | `$50` / `$27` |

The new save's zeroed main block leaves rupees, counts, inventory slots,
sword/shield levels, heart pieces, seashells, and persistent flags at zero;
`LoadSavedFile` supplies three full hearts (`$18`) from the max-heart table.
The Java model keeps its existing debug-friendly no-argument constructor for
tests and calls an explicit reset method at the real New Game boundary.

## Architecture

`NewGameStartProfile` is an immutable source-derived value object containing
the map, room, entry point, resource capacities, direction encoding, and
wrecking-ball state. It exposes one operation that applies the fresh-game
player fields to a `PlayerState`; it does not own room loading or rendering.

`PlayerState.initializeNewGame()` clears all mutable player resources and
inventory slots, then applies the values represented by the profile. The
profile invokes this reset so the Java state cannot retain the constructor's
debug inventory or values from an earlier session.

`Main.startNewGame()` is a separate transition from
`startConfiguredGameplay()`. It clears the file-menu controller, selects the
overworld gameplay screen, applies the new-game profile, calls
`RoomSession.loadIndoor(0x10, 0xA3)`, and places Link at `(0x50,0x60)` using
the existing room-entry setter. Because Java's direction enum uses a different
ordering from the ROM's direction constants, the profile records the ROM
encoding while the Java Link remains explicitly facing its `DIRECTION_DOWN`
value. No emulator-style CPU/WRAM abstraction is introduced.

## State behavior

The fresh player state is:

- 3 max hearts and 24 health points;
- green tunic, no power-up, no rupees, no heart pieces, and no seashells;
- zero arrow/bomb/powder counts;
- max arrows `0x30`, max bombs `0x30`, max powder `0x20`;
- sword level 0 and shield level 0;
- empty A and B slots and ten empty subscreen slots;
- cleared damage/resource buffers, invincibility, Pegasus Boots state, and
  power-up hit counter.

The Java Link's existing default direction is down and its animation state is
the standing frame, matching the values written by `.initNewGame`; no
unnecessary new direction abstraction is added for this boundary.

## Verification

- `NewGameStartProfileTest` verifies every source-derived constant and the
  profile's fresh-player state, including clearing deliberately mutated debug
  state.
- `PlayerStateTest` verifies the reset clears all inventory/resource state
  while preserving the exact three-heart health value.
- `RoomSessionTest` verifies the shipped ROM can load map `$10`, room `$A3`
  through the indoor path and reports the expected category.
- `MainFileMenuFlowTest` source inspection verifies the file-menu new-game
  branch calls the dedicated bootstrap instead of the configured overworld
  bootstrap.
- `gradle clean test` and `git diff --check` must pass.

