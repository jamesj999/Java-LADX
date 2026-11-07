# Pairodd projectile sword intersection implementation plan

## 1. Lock the event contract with tests

- Add direct geometry tests for the `$58` normal hitbox against the sword
  rectangle, including visual-Y (`Y-Z`) and half-open edge behavior.
- Add runtime tests proving the check uses the post-movement position, emits
  `SWORD_HIT`, removes the slot, requests one sword-poke VFX, and leaves `$58`
  outside generic combat.
- Add consumer coverage for the ROM bump jingle without Link damage.

## 2. Thread sword context through the entity tick

- Add an overload to `RoomSession.tickEntitiesWithProjectileEvents` carrying
  the optional sword rectangle while keeping old overloads source-compatible.
- Pass that context into `RoomEntityRuntime.tickWithProjectileEvents` and its
  internal tick loop.
- Keep the inactive default for tests and callers without sword state.

## 3. Implement the bank-$04 `$58` branch

- Add a source-specific sword-overlap predicate in the projectile collision
  component using the normal hitbox `[8, 5, 8, 5]` and unsigned byte math.
- Emit `SWORD_HIT` with the bank-$03 bump and Link ignore countdown.
- Preserve an existing Link collision event when both checks hit, but enqueue
  only one final sword-poke VFX request and clear the slot after both checks.

## 4. Connect the live loop and verify

- Preserve the sword box computed for the current frame in `Main` and pass it
  to the entity tick after generic combat resolution.
- Run focused tests, `git diff --check`, and `gradle clean test`.
- Update `docs/reconstruction-roadmap.md` with the verified source range and
  remaining `$58` gaps.
