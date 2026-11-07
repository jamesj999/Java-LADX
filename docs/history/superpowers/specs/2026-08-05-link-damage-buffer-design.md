# Link damage modifiers and health-buffer design

## Scope

Complete the shared ROM damage path for enemy contact and enemy projectile
events that already reach the gameplay boundary. The slice adds the
equipment/power-up modifiers from `ApplyLinkCollisionWithEnemy` and the
delayed health reduction performed by `UpdateHealth`. It does not invent
entity-specific movement, knockback, medicine, or low-health sound behavior.

## ROM behavior

The nominal damage value is already supplied by the entity or projectile
event. The generic collision routine in
`LADX-Disassembly/src/code/entities/bank3.asm` at bank-$03 `$6D73-$6DDF`
handles the accepted hit as follows:

1. Blue Tunic damage is shifted right once.
2. Otherwise, an active Guardian Acorn makes nominal damage `$04` become
   zero and shifts every other nominal value right once.
3. The effective value is added to `wSubtractHealthBuffer` as an unsigned
   byte, without changing health immediately.
4. `wInvincibilityCounter` becomes `$50`.
5. Any active power-up counts the hit; `wActivePowerUp` is cleared on the
   third hit. Power-up pickup resets `wPowerUpHits` to zero.

`UpdateHealth` in `src/code/bank2.asm` at `$6317-$63A2` runs its resource
work on odd frames. It consumes one pending healing point first. If healing
was pending but health was already full, it clears the healing buffer and
falls through to consume one pending damage point during the same tick. A
damage point decrements the damage buffer and health when health is nonzero.

## Java design

`PlayerState` owns the ROM-facing state and exposes:

- `applyRomEnemyDamage(int nominalDamage)`, returning the effective damage
  while updating the subtract-health buffer, invincibility counter, and power-
  up hit count;
- `subtractHealthBuffer()` and `powerUpHits()` read accessors;
- `setActivePowerUp(int)` to model a pickup/reset boundary.

The existing `Main` entity-contact loop and
`EnemyProjectileEventConsumer` keep their acceptance gates and hurt-sound
behavior, but delegate accepted damage to `applyRomEnemyDamage`. The health
buffer is drained from the existing alternating `tickResourceBuffers` path.
The separate immediate `damage(int)` method remains for non-generic paths such
as pit damage until those paths are source-mapped.

## Testing and boundaries

`PlayerStateTest` will cover green/blue/Guardian modifiers, unsigned buffer
accumulation, three-hit power-up expiry, delayed odd-frame draining, healing
precedence, and full-health fall-through. The projectile consumer test will
assert that a hit leaves health unchanged until a health tick while retaining
sound and invincibility behavior. Existing entity-event tests continue to
assert nominal damage: modifier application belongs at the gameplay state
boundary, matching the ROM call graph.

No renderer, ROM loader, or emulator layer changes are part of this slice.
