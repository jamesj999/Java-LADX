# ROM-Backed Leever Background-Wall Design

## Goal

Mirror the wall portion of Leever's bank-$04
`ApplyEntityInteractionWithBackground` call. After Leever applies its
fixed-point X/Y movement, a blocked directional query must restore the
corresponding coordinate while leaving the ROM speed and state machine
unchanged.

## ROM source of truth

- `LADX-Disassembly/src/code/entities/04_leever.asm:16-22` calls
  `UpdateEntityPosWithSpeed_04`, then
  `ApplyEntityInteractionWithBackground_trampoline`, before dispatching the
  Leever state handler.
- `LADX-Disassembly/src/code/entities/bank3.asm:7420-7500` is the shared
  wall-application path used by bank `$04`: it checks the current X speed,
  queries the directional collision object, and restores the active X
  coordinate when blocked; it then repeats the same process for Y. It does
  not negate or clear the ordinary speed registers.
- `RoomEntityBackgroundCollision` already exposes the disassembly direction
  values: right `$00`, left `$01`, up `$02`, and down `$03`.

## Design

After `LeeverMotion` computes the fixed-point X and Y positions, query the
existing background callback once for a changed X coordinate and once for a
changed Y coordinate, in source X-before-Y order. Restore only the blocked
coordinate. Pass the post-X coordinate into the Y query, matching the shared
ROM helper's sequential position writes. Do not reverse speeds, reset the
Leever countdown, change state, or make the callback mandatory.

This increment covers wall rollback only. Ground status, deep/shallow water,
tall grass, pits, conveyors, falling transitions, object IDs, and the
entity-specific no-background option remain deferred because the current
callback intentionally abstracts only directional wall blocking.

## Tests

Use a live chase-state Leever fixture. Advance frames `0..31` to leave the
hidden state, then tick frame `32` with Link at `(80,64)` so the ROM `$08`
chase vector is selected. On frame `34`, the fixed-point X accumulator reaches
one pixel. A right-wall callback must keep X at `64`, leave speed X `$08`,
and keep the Leever in chase state. The same setup with no blocking must reach
X `65`, proving the test detects a missing rollback rather than merely
checking an unchanged position.

## Scope boundary

No Leever recoil changes are included; the preceding verified increment owns
that path. No generic object/ground interaction or visual/audio effects are
added here.
