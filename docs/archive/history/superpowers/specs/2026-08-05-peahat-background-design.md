# ROM-Backed PeaHat Background-Wall Design

## Goal

Mirror the wall portion of PeaHat's bank-$07
`ApplyEntityInteractionWithBackground` call. After PeaHat applies its
fixed-point X/Y and Z movement, a blocked directional query must restore the
corresponding X or Y coordinate while leaving the state machine and ordinary
speed registers unchanged.

## ROM source of truth

- `LADX-Disassembly/src/code/entities/07_peahat.asm:10-22` calls
  `UpdateEntityPosWithSpeed_07`, `AddEntityZSpeedToPos_07`, and then
  `ApplyEntityInteractionWithBackground_trampoline` before setting PeaHat's
  airborne hitbox flags and dispatching its state handler.
- `LADX-Disassembly/src/code/entities/bank3.asm:7420-7500` is the shared wall
  path. It checks the current X speed, queries the directional collision
  object, and restores the active X coordinate when blocked; it then repeats
  the same process for Y using the post-X position. It does not reverse or
  clear the ordinary speed registers.
- `RoomEntityBackgroundCollision` exposes the ROM direction values: right
  `$00`, left `$01`, up `$02`, and down `$03`.

## Design

After `PeaHatMotion` computes the fixed-point X/Y positions and before its
rest/takeoff/flying state dispatch, query the existing background callback in
X-before-Y order. Restore only a coordinate whose directional query reports a
block, and pass the post-X coordinate into the Y query. Keep Z and all
PeaHat-private state unchanged.

The current callback intentionally abstracts only directional wall blocking.
Ground status, deep/shallow water, tall grass, pits, conveyors, falling
transitions, object IDs, collision flags, and the entity-specific
no-background option remain deferred to the broader room-interaction work.

## Tests

Use two live PeaHat runtimes with identical random inputs. Advance both to
the ROM flying state where the source phase table gives positive X speed, then
advance one through the remaining phase frames with a right-wall callback and
the other without a callback. The blocked runtime must retain its X position
from the start of that phase while the unblocked control advances. Both
runtimes must retain the same Y position, state, and speed tables.

## Scope boundary

No PeaHat recoil, hitbox-flag, sword-clink, water, pit, conveyor, or visual
effect changes are included; those are separate parity slices.
