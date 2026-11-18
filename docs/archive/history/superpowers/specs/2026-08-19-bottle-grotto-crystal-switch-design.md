# Bottle Grotto Crystal-Switch Ordered-Play Design

## Scope

Extend the uninterrupted fresh-game regression from Bottle Grotto room `$32`'s
first Small Key into room `$33`. Exercise the room's ROM-authored crystal switch,
the global red/blue switch-block state, and the resulting collision change through
the same gameplay boundaries used by an interactive run.

## Source behavior

- `MapLayout1` places room `$33` immediately east of `$32`.
- `IndoorsA33` supplies the left, top, and right room template, an open left door,
  switch-block object runs `$DC`, raised/lowered block objects `$C0`, and the
  ordinary dungeon floor and wall objects.
- `IndoorsA33Entities` supplies the owl statue, crystal switch (`$66`), and a
  counter-clockwise Spark.
- `CrystalSwitchEntityHandler` makes the switch hittable, clears its flash after a
  hit, starts `wSwitchableObjectAnimationStage` at `$01` only when no animation is
  already active, loads transition countdown `$18`, and requests wave SFX `$0E`.
- The gameplay VBlank path advances the eight-stage switch-block animation. Its
  final stage toggles `wSwitchBlocksState` between `$00` and `$02`; Link and entity
  collision must observe the same state as the rendered block tiles.

## Architecture

Continue using the live `RoomSession`, `Link`, collision map, transition
coordinator, inventory, and dungeon-state objects already owned by
`freshGameRuntimeSequenceCollectsBottleGrottoFirstKeyInOrder`. Walk east through
the open ROM door, locate the ROM-loaded crystal-switch entity, approach it using
the collision-aware path helper, and strike it through `resolveEntityCombat`.
Advance ordinary entity ticks and `tickGameplayVBlank` rather than mutating test
state. Assert the emitted source sound, the complete animation transition, and a
collision point whose passability changes with the global state. Then follow the
newly passable route to the next ROM-authored room boundary.

If the integration test exposes missing behavior, add the smallest source-shaped
production change at the component that owns that behavior. Do not special-case
room `$33`, hardcode a test-only switch toggle, or bypass collision.

## Verification

The slice is complete when the uninterrupted ordered-play regression enters room
`$33`, observes its ROM objects and entities, hits the switch through live combat,
receives wave SFX `$0E`, completes the VBlank animation, observes synchronized
tile/collision state `$02`, and crosses the newly available ROM route. Focused
switch, room-session, transition, and full Java suites must remain green.
