# Pairodd Projectile Collision Implementation Plan

## Goal

Implement the ROM `$58` Link/shield collision and transient sword-poke path
identified in the design document, with focused tests and a roadmap update.

### 1. Add failing collision and motion-direction tests

- Extend `EnemyProjectileCollisionTest` with `$58` shield-facing, unshielded
  damage, and visual-Y cases.
- Add a runtime test that moves a Pairodd projectile, collides it with Link,
  and asserts the slot is disabled plus a `$05` VFX request at post-move
  coordinates.
- Assert a spawned Pairodd projectile keeps the source `SpawnNewEntity`
  direction-zero behavior while room-loaded projectiles retain their motion
  direction state.
- Run the focused tests and observe the expected red failure before changing
  production code.

### 2. Extend the shared ROM collision contract

- Add `ENTITY_PAIRODD_PROJECTILE = $58` to `EnemyProjectileCollision`.
- Use the same generic shield reverse-direction table as rock/arrow.
- Emit `$FF`, removal, and sword-poke metadata for both Pairodd collision
  outcomes; emit wave `$03` and `$08` Link damage only for normal contact.
- Expose `PairoddProjectileMotion.direction(slot)` without changing its
  existing fixed-point vector behavior.

### 3. Wire the post-movement runtime branch

- Run the `$58` collision check immediately after `PairoddProjectileMotion`
  advances the entity, matching `04_pairodd.asm` handler order.
- Publish the event, enqueue the ROM sword-poke VFX at post-move X/visual Y,
  and disable the dynamic slot without persistent room-mask changes.
- Leave the projectile out of `isEnemyProjectileType` and the generic enemy
  combat pass.

### 4. Verify and document

- Run the focused collision/runtime tests, then `gradle clean test`.
- Run `git diff --check` and inspect the diff for unrelated changes.
- Add a dated roadmap entry that names the source labels and lists the
  remaining projectile sword/object/recoil/background gaps.
- Commit the implementation and documentation as one intentional increment.
