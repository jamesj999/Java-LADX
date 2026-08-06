# Entity switch-block collision state design

**Status:** Proposed for implementation on `feature/entity-runtime`

## Goal

Replace the Java entity collision resolver's conservative "all `$04` physics
is solid" behavior with the ROM's stateful ocean/switch-block branch while
keeping the existing ROM-backed object and physics lookup boundary intact.

## Source contract

`ApplyEntityCollisionWithObject` in
`LADX-Disassembly/src/code/entities/bank3.asm` handles
`OBJ_PHYSICS_OCEAN_SWITCH_BLOCK` (`$04`) as follows:

- Bomb (`$02`) and wrecking ball (`$A8`) pass without collision.
- Object IDs below `$DB` or at/above `$DD` are treated as solid ocean.
- Object `$DB` and `$DC` select entries `0` and `2` from the source
  switch-block state table. The object is passable when that expected byte
  equals `wSwitchBlocksState`; otherwise it collides.
- `SwitchBlocksStateTable` in bank `$02` and
  `SwitchBlockLoweredStatePerObject` in bank `$03` both contain the same ROM
  bytes `[0, 2]`. The Java implementation uses the bank-$03 branch's exact
  `[0, 2]` values, documented as a ROM table rather than an inferred rule.
- `wSwitchBlocksState` is an unsigned WRAM byte whose live game toggle and
  VBlank tile animation are outside this increment. The collision boundary
  must nevertheless carry its current value so later crystal-switch/event
  code can update one authoritative state.

The hookshot-chain-specific standing-on-switch-block exception and the
hookshotable-object transition countdown remain separate work; this slice
does not silently implement those entity-state side effects.

## Architecture

Extend `EntityBackgroundCollisionState` with a masked
`switchBlocksState` byte, retaining a four-argument compatibility constructor
that defaults it to zero. `EntityBackgroundCollisionResolver` receives the
sampled object ID in its decision helper and implements the branch above;
non-switch physics and the existing ledge timer result are unchanged.

`RoomSession` owns the WRAM-equivalent byte for the active gameplay session,
initializes it to the source reset value zero, supplies it to each rich
collision probe, and exposes only a package-private test setter for this
increment. It does not fabricate a toggle source. Room loads preserve the
session byte, matching the fact that the source WRAM variable is global to
gameplay rather than part of an individual room object stream.

## Testing

- Resolver tests cover state zero with lowered/raised objects, state two with
  the reversed result, invalid object IDs remaining solid, and bomb/wrecking
  ball pass-through in both states.
- A shipped-ROM `RoomSession` regression writes `$DB`/`$DC` into the padded
  active-room object buffer, selects both state values through the test seam,
  and verifies the live rich probe returns the ROM result and physics byte.
- Existing stateless resolver tests continue to pass, proving an object ID
  outside `$DB..$DC` retains the former conservative behavior.
- Focused tests are followed by `gradle -p java clean test` and
  `git diff --check`.

## Scope boundary

This increment ports collision-state consumption only. It does not add the
crystal-switch handler, `UpdateSwitchBlockTiles` animation stages, the
`wLinkStandingOnSwitchBlock` lifecycle, hookshot-chain transition state, or
the broader room-event scripting system. Those are explicit follow-up slices
that will consume the same session state instead of introducing a second
switch-state model.
