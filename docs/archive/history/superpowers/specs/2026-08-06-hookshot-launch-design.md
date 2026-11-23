# Hookshot launch and outbound chain design

## Scope

Implement the first live hookshot slice: pressing the equipped hookshot must
create the ROM's `$03` hookshot-chain entity, move it with the exact outbound
speed tables, return it toward Link in an open room, and hold Link's movement
while the entity is active. The entity uses the inline hook sprite list from
`HookshotChainSpriteVariants`.

This slice deliberately stops at the open-room return boundary. Hookshotable-
object collision, the `$01` pull state, Link-vector movement while pulling,
hookshot bridges, and the three dynamic chain-link OAM entries are separate
follow-up work because they share the background collision and vector helper
contracts used by many other entities.

## Source of truth

- `src/code/bank2.asm:89-148` — `HookshotChainSpeedX/Y` and `FireHookshot`.
- `src/code/bank0.asm:2046-2086` — `SpawnPlayerProjectile`, including zero
  position offsets, Link Z plus one, and the initial direction.
- `src/code/entities/18_hookshot_chain.asm:1-36` — active handler motion lock,
  entity state, and hook sprite bytes.
- `src/code/entities/18_hookshot_chain.asm:38-88` — countdown boundary,
  return vector, and Link collision unload path.
- `src/code/entities/18__helpers_3.asm:251-297` — signed 4-bit fixed-point
  `UpdateEntityPosWithSpeed_18` behavior.
- `src/code/entities/bank3.asm:5089-5130` and
  `src/code/home/entities.asm:278-292` — entity-$03's normal hitbox and
  visual-Y Link collision predicate.

## ROM-facing behavior

1. `FireHookshot` returns without spawning when Link is pushing, airborne, or a
   hookshot entity is already active.
2. `SpawnPlayerProjectile` starts the entity at `hLinkPositionX/Y`, with
   `hLinkPositionZ + 1`, and copies the ROM direction.
3. `FireHookshot` overwrites the projectile speeds with `$30, -$30, 0, 0`
   and `0, 0, -$30, $30`, then initializes the transition countdown to `$2A`.
4. Each active handler tick sets Link's interactive-motion block and
   `wIsUsingHookshot`; while the countdown is nonzero, the chain advances with
   the signed 4-bit fixed-point position helper. Once it reaches zero, the
   open-room host applies the ROM `$30` vector toward Link until the entity's
   normal small hitbox overlaps Link's visual position.
5. The hook renders as tile `$36` twice, with the second OAM entry carrying
   `OAMF_XFLIP` (`$20`).

## Host design

- `Hookshot` is an `EquippedItem` whose target owns the room/entity state. It
  requests a launch on the button edge and reports `blocksMotion()` from the
  target's active state.
- `RoomSession` exposes the narrow launch/active API and delegates entity slot
  allocation to `RoomEntityRuntime`.
- `HookshotChainMotion` owns the per-slot fixed-point accumulators and countdown
  rather than adding another set of parallel WRAM arrays to the runtime.
- Entity type `$03` gets a catalog definition built from the source's inline
  hook OAM bytes; it is not decoded as a guessed ROM display-list address.

## Invariants

- Direction tables use ROM order right, left, up, down, even though Java Link
  directions use down, up, left, right.
- Speed bytes are unsigned at storage boundaries and interpreted as signed
  4-bit fixed-point values when advancing positions.
- A second launch is rejected while the existing `$03` entity is loaded.
- No hookshot state is synthesized when there is no free entity slot.
- The launch grace period prevents the chain's spawn-on-Link position from
  unloading during the outbound leg; the return leg uses the ROM hitbox rather
  than an exact raw-coordinate comparison.
