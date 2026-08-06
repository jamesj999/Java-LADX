# Link Switch-Block Footing Design

## Goal

Make Link's top-down room collision follow the LADX switch-block rules while a
room's `wSwitchBlocksState` changes, including the transient
`wLinkStandingOnSwitchBlock` escape state used during a block transition.

This slice is intentionally narrower than the whole switchable-object system:
it does not add new switch-button or mobile-block event producers, footstep
audio, or the `wC13B` visual-offset output. Those callers need separate source
tracing.

## ROM contract

The source of truth is:

- `LADX-Disassembly/src/code/bank2.asm:723D-725D`,
  `ApplyCollisionWithOceanOrSwitchBlock`.
- `LADX-Disassembly/src/code/bank2.asm:77B2-77F7`,
  `ApplyLinkGroundPhysics_Default`'s switch-block branch.
- `LADX-Disassembly/src/code/bank2.asm:786F-787C`,
  `Data_002_786F` and `SwitchBlocksStateTable`.
- `LADX-Disassembly/src/constants/gfx.asm`, where the object IDs are
  `OBJECT_LOWERED_BLOCK = $DB` and `OBJECT_RAISED_BLOCK = $DC`.

The source table is `[0x00, 0x02]`, indexed by object ID minus `$DB`.
For a switch-block object, Link's collision path first checks
`wLinkStandingOnSwitchBlock`; a nonzero value makes the collision pass. When
that override is clear, the block is passable exactly when
`wSwitchBlocksState XOR SwitchBlocksStateTable[index] == 0`.

The ground path marks the override when Link's foot object is `$DB` or `$DC`,
its physics byte is `$04`, and the same XOR is nonzero. On a later ground
refresh where that condition is no longer true, the source clears the flag.

## Design

### Decision unit

Add `linksawakening.world.SwitchBlockLinkInteraction` as a pure, ROM-shaped
decision unit. It will expose:

- recognition of the two switch-block object IDs;
- the expected state for each object ID;
- whether a collision should block for a state and standing override;
- whether a ground refresh should mark Link as standing on a switch block.

It will validate unsigned byte inputs and reject object IDs outside `$DB/$DC`
where an indexed state is required. It will not infer behavior from the
rendered tilemap or from a generic physics allow-list.

### Room collision integration

Extend `OverworldCollision` with the live unsigned
`wSwitchBlocksState` and `wLinkStandingOnSwitchBlock` equivalents. Its
point-based Link collision will use the decision unit for physics byte `$04`
and object IDs `$DB/$DC`, while all other objects retain the existing ROM
physics behavior. A ground-refresh method will sample the existing
`objectUnderLinkFeet` coordinate, update the standing override, and leave the
existing pit/slow-ground queries unchanged.

`setRoom` clears the transient standing override because room changes do not
carry Link's old footing state into a new room. State updates remain explicit;
there is no hidden read of `RoomSession` from the collision object.

### Link and RoomSession wiring

`Link.refreshGroundStatus()` will ask `OverworldCollision` to refresh the
switch-block standing state before its existing pit check. This places the
override before the leading-edge movement probe in the same frame.

`RoomSession` will synchronize its room-owned `switchBlocksState` into
`OverworldCollision` after room loads, after test state changes, and after a
switch animation VBlank updates the state. The existing crystal-switch state
machine remains the only producer in this slice; no new event source is
invented.

## Failure and compatibility behavior

- A missing room remains collision-free, as it is today.
- A physics byte other than `$04` never takes the switch-block branch, even if
  a synthetic room contains `$DB` or `$DC`.
- Invalid switch states are rejected by the new decision unit rather than
  silently selecting a guessed table entry.
- Existing compatibility constructors and non-room Link tests retain their
  current behavior when no collision room is installed.

## Tests

Tests will be written first and will cover:

1. The ROM table mapping and pass/block results for both objects in states
   `$00` and `$02`.
2. The standing override making a mismatched block pass, and the ground
   sample setting/clearing that override only for physics `$04`.
3. The live `OverworldCollision` point path, including its selected physics
   table and room-buffer foot coordinate.
4. Link movement into a mismatched block versus movement over a matching
   block, using the real `Link` and `OverworldCollision` classes.
5. Room-session synchronization when the existing switch animation toggles
   `wSwitchBlocksState`.

The focused tests and `gradle -p java clean test` must pass before the slice is
reported complete.
