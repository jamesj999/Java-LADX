# Bottle Grotto Room `$3B` Side-View Passage Design

## Goal

Continue the uninterrupted Bottle Grotto route from room `$25` through
side-view room `$3B`, reproducing its `$A5` sinking platform and `$D6` pots
from the disassembly, then return through the authored warp to room `$25`.

## Source behavior

`IndoorsA3B` spawns one `ENTITY_SIDE_VIEW_PLATFORM` (`$A5`) and two
`ENTITY_SIDE_VIEW_POT` (`$D6`) entities. Entry from room `$25` uses warp
`E2 01 3B 88 10`; room `$3B` returns through `E1 01 25 88 20`.

The platform handler at `07:6432` delegates position, background interaction,
and standing detection to `func_007_639E`. On contact from above it snaps Link
to `platformY-$10`, carries Link by the platform's horizontal delta, writes
Link speed Y `$02`, sets the standing flag, and writes private state 2 `$10`.
In room `$3B`, Link alone activates the platform. Elsewhere the same `$A5`
handler requires `wC3CF != 0`, meaning Link must carry an entity. Activation
increments private state 4 to `$04`, emits rumble on the `$03->$04`
transition, and every fourth frame increments platform speed Y toward `$04`.
Loss of contact clears speed Y and private state 4.

The side-view pot handler at `19:58E0` first performs its dedicated Link
collision/push and atop-pot snap. Pickup requires an interactive frame,
grounded Link, the `$3C` hitbox collision, the exact unsigned `-$12..+$11`
position windows, no attack countdown, a held equipped Bracelet button, and
no existing carried entity. Its states 1 and 2 use direct X/Y speed motion,
increase Y speed by two toward `$40`, apply background interaction, and smash
and unload on any collision. This path must not use generic top-view thrown
gravity.

## Architecture

Create focused `SideViewPlatformMotion` and `SideViewPotMotion` components to
own their per-slot ROM speed bytes and fixed-point accumulators. Keep handler
dispatch and room-specific gates in `RoomEntityRuntime`. The platform emits a
typed Link request containing snapped Y, horizontal carry delta, Y speed, and
standing state; `RoomSession` forwards it and `Main` applies it to the live
Link before boundary handling. Pot collision emits the existing smash VFX/SFX
and unloads through normal runtime ownership.

## Verification

Use test-first focused motion and runtime tests for cadence, room `$3B`
activation, non-`$3B` carrying gates, contact loss, Link request contents,
pot acceleration, and collision teardown. Extend the ordered route test to
activate and ride the platform, use the source pots, and take the room `$3B`
exit back to room `$25`. Run the clean full suite and source-fidelity review
before committing the gameplay milestone.
