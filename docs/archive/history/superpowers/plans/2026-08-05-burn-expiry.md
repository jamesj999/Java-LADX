# Burn-expiry side-effect implementation plan

## 1. Lock the ROM contract with tests

- Add a gameplay sound-consumer test mapping raw noise `$13` to
  `ENEMY_DESTROYED`.
- Extend burn-expiry runtime tests to assert one status event, exact physics
  flags `$04`, and no event/noise for the Gibdo-to-Stalfos conversion.

## 2. Add pending status-event plumbing

- Add a per-tick pending `EntityCombatEvent` list to `RoomEntityRuntime`.
- Clear it at tick start and expose a consume-once accessor.
- Let `RoomSession` expose the runtime's pending status events after its entity
  tick; keep existing projectile return values unchanged.

## 3. Implement and route the source behavior

- Set non-Gibdo burn expiry physics flags to `$04` and enqueue raw noise
  `$13` alongside the existing DYING `$1F` transition.
- Add the explicit gameplay sound enum/map entry and route it through
  `EnemyCombatEventConsumer`.
- Consume status events in `Main` immediately after the entity tick, next to
  the existing projectile event boundary.

## 4. Verify and document

- Run focused burn/audio tests, `git diff --check`, and `gradle clean test`.
- Update the roadmap to mark noise/physics complete while leaving poof and
  broader death/drop behavior deferred.
