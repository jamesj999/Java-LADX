# Pairodd projectile sword intersection design

## Scope

Implement the bank-$04 `$58` Pairodd projectile branch that is still missing
from the Java runtime: collision with Link's active sword rectangle.

This slice does not add `$58` to the generic room enemy-combat pass. The ROM
keeps the projectile in its own handler and routes the hit through the shared
`DefaultEnemyDamageCollisionHandler` only after the projectile has moved.

## ROM behavior

`src/code/entities/04_pairodd.asm:PairoddProjectileEntityHandler` at
`04:5EFC` performs, in order:

1. Render and skip non-interactive entities.
2. Update the projectile position with `UpdateEntityPosWithSpeed_04`.
3. Apply sword/background-object intersection.
4. Call `CheckLinkCollisionWithProjectile`.
5. Call `label_3B70`, the trampoline to bank-$03's
   `DefaultEnemyDamageCollisionHandler`.
6. If the collision table is nonzero, clear the `$58` slot and create the
   shared sword-poke VFX at the active entity position.

Within `DefaultEnemyDamageCollisionHandler`, the sword rectangle is compared
against the active entity's normal hitbox. The `$58` branch at
`bank3.asm:6F20` calls `func_003_6F93`, then stores the shared `$FF` collision
value. `func_003_6F93` emits `JINGLE_BUMP` (`$09`), resets Pegasus Boots, and
writes `wIgnoreLinkCollisionsCountdown = $0C`.

The projectile's visual Y is `entityY - entityZ`, matching the active
entity's visual position used by both collision routines and the final VFX.

## Java contract

Extend `EntityProjectileEvent.Kind` with `SWORD_HIT`. A Pairodd sword hit
emits:

- collision value `$FF`;
- no Link damage;
- jingle `$09`;
- `remove = true`;
- Link collision-ignore countdown `$0C`;
- sword-poke VFX at `(projectile.x, projectile.y - projectile.z)`.

The runtime receives the current sword collision rectangle as optional input
to the entity tick. Existing callers retain an inactive rectangle. Only the
Pairodd branch consumes it, after `PairoddProjectileMotion.advance` and
alongside the existing projectile-vs-Link check.

If Link and the sword overlap the same frame, retain both ROM-observable
responses in event order: the projectile-vs-Link event first, then
`SWORD_HIT`. The VFX request is deduplicated because the ROM creates one VFX
after the final collision-table check.

## Non-goals

- Generic sword damage, health, recoil, or enemy flash for `$58`.
- Full `ApplySwordIntersectionWithObjects` background-object behavior.
- Reworking the existing generic projectile collision contract.
