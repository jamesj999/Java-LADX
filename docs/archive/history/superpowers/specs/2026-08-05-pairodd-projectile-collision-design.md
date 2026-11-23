# ROM Pairodd Projectile Collision Design

## Goal

Add the missing Link-collision portion of Pairodd's projectile handler
(`$58`) without routing the projectile through the generic enemy combat pass.
The increment covers the shared bank-$03 projectile check, the bank-$04
post-collision cleanup, and the ROM sword-poke transient effect.

## Source of truth

- `src/code/entities/04_pairodd.asm:$5EFC-$5F28`
  - moves the projectile first;
  - calls `CheckLinkCollisionWithProjectile`;
  - calls the shared `DefaultEnemyDamageCollisionHandler` for the separate
    sword-intersection path;
  - clears `$58` when the collision byte is nonzero and jumps to
    `func_004_6BE1.createSwordPokeVfx`.
- `src/code/entities/bank3.asm:$6BDE-$6C5A`
  - uses the byte-wide half-open `$0C` collision windows and visual Y
    (`entity Y - entity Z`);
  - a normal shield blocks only a projectile whose direction is the reverse
    of Link's direction, producing jingle `$16` and collision `$FF`;
  - an unshielded projectile goes through `ApplyLinkCollisionWithEnemy` and
    then receives collision `$FF`.
- `src/code/entities/_handlers.asm:$383`
  - room-loaded `$58` entities use `EntityInitWithRandomDirection`.
- `src/code/entities/reset_entity.asm:$404E-$4052` and
  `src/code/entities/bank3.asm:$64CA-$6523`
  - a dynamically spawned entity is reset with direction zero and is active
    immediately; the Pairodd spawn path does not overwrite that direction.
- `src/code/entities/04_knight.asm:$6BE1-$6C2A`
  - the shared sword-poke helper uses the active projectile position and
    visual Y, and emits transient VFX `$05`.

## Java shape

`EnemyProjectileCollision` will accept `$58` in addition to the existing
rock, arrow, and laser types. Pairodd events have these ROM-defined results:

| Path | collision | sound | Link damage | remove | sword-poke |
| --- | ---: | --- | ---: | --- | --- |
| reverse-facing shield | `$FF` | jingle `$16` | `0` | yes | yes |
| normal contact | `$FF` | wave `$03` | `$08` | yes | yes |

The runtime invokes the checker on the post-movement `RoomEntity` and uses
`PairoddProjectileMotion.direction(slot)`. It records the VFX request at the
projectile's post-move X and visual Y. The gameplay event consumer continues
to own Link health/invincibility and audio sinks.

## Deliberate boundary

This increment does not emulate `ApplySwordIntersectionWithObjects`, the
projectile's own sword-hit path inside `DefaultEnemyDamageCollisionHandler`,
background/object collision, recoil, or additional Pairodd audio. Those are
separate source audits; `$58` remains excluded from generic enemy combat.

## Verification

Tests will cover the `$58` direction-filtered shield result, normal damage
result, visual-Y collision coordinate, post-move runtime removal, VFX request,
and the unchanged behavior of the existing rock/arrow/laser paths.
