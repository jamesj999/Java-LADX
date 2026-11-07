# ROM-Backed Entity Conveyor Interaction Design

## Goal

Restore the generic conveyor part of bank-$03's
`ApplyEntityInteractionWithBackground`. Active entities that are on a
conveyor must receive the ROM's one-pixel X/Y nudge every fourth frame, while
entities whose ROM `Options1ForEntity` has
`ENTITY_OPT1_NO_GROUND_INTERACTION` must not receive that nudge.

## ROM source of truth

- `LADX-Disassembly/src/code/entities/bank3.asm:7377-7384` defines the
  movement tables:
  `X = [0, 0, -1, +1, +1, -1, +1, -1]` and
  `Y = [+1, -1, 0, 0, +1, +1, -1, -1]`.
- `LADX-Disassembly/src/code/entities/bank3.asm:7386-7578` calls
  `func_003_7E0E` before ground interaction. That helper samples the padded
  `wRoomObjects` area using `entityX - 1` and `entityY - 7`, then stores the
  selected object's physics byte in `hMultiPurpose3`.
- `LADX-Disassembly/src/code/entities/bank3.asm:7573-7585` applies the
  conveyor movement only when `hFrameCounter & $03 == 0`, with the table
  index `physics - OBJ_PHYSICS_CONVEYOR` for the valid `$F0-$F7` range.
- `LADX-Disassembly/src/data/entities/options1.asm` is loaded by bank-$03 at
  ROM bank `$03`, address `$42F1`. Bit `$10` is
  `ENTITY_OPT1_NO_GROUND_INTERACTION`; the Java runtime will read this table
  from the ROM rather than duplicate an entity-type switch.

## Design

Add a source-shaped `RoomEntityGroundInteraction` callback to the entity
runtime. The runtime invokes it once after an active, non-initializing entity
handler has produced its frame update. `RoomSession` supplies the callback;
direct runtime tests can inject a deterministic callback without needing a
room loader.

The session callback:

1. Skips entities with ROM option bit `$10`.
2. Skips airborne entities with a positive non-negative Z value (`$01-$7F`),
   matching the early ground-interaction return in bank `$03`.
3. Runs only on frames where `frame & 3 == 0`.
4. Reads the active room's selected physics table at the exact padded
   `entityX - 1`, `entityY - 7` coordinate.
5. For physics `$F0-$F7`, applies the corresponding signed movement and wraps
   the entity coordinates to one byte. Other physics values leave the entity
   unchanged.

This increment intentionally stops at conveyor movement. Ground-status
updates, water splashes, pit transitions, and wall/fine-collision behavior
remain separate source-backed slices.

## Tests

- Verify the exact ground-interaction coordinate reads the selected ROM
  physics table from the padded room buffer.
- Verify the runtime exposes an active-entity ground-interaction boundary and
  preserves the entity update ordering.
- Load an overworld room containing an Octorok, compare a passable floor with
  an all-`$CF` conveyor floor, and assert the ROM `$F4` diagonal nudge appears
  on the fourth active frame.
- Load an indoor room containing a Spark, compare the same passable floor with
  an all-`$CF` conveyor floor, and assert the ROM no-ground-interaction option
  keeps both runs identical.

## Scope boundary

This increment does not claim complete `ApplyEntityInteractionWithBackground`.
Ground status, splash VFX/audio, pit handling, conveyor-adjacent wall state,
and entity-specific airborne behavior remain pending.
