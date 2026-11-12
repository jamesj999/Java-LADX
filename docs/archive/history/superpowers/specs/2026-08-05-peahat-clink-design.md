# ROM-Backed PeaHat Airborne Sword-Clink Design

## Goal

Mirror PeaHat's dynamic sword-clink behavior while it is taking off or
flying. An airborne PeaHat must not damage Link through the normal contact
path, but a colliding sword must enter the shared ROM clink branch: no enemy
damage or recoil, jingle `$07`, sword-poke VFX, and a `$10` ignore-hits window.

## ROM source of truth

- `LADX-Disassembly/src/code/entities/07_peahat.asm:10-24` runs the default
  enemy collision handler before the PeaHat state dispatch, then sets hitbox
  flag bit 7 and `ENTITY_OPT1_B_SWORD_CLINK_OFF` for the next active frame.
- `LADX-Disassembly/src/code/entities/07_peahat.asm:31-52` clears those two
  flags only when the resting handler sees `z == 0`; taking-off and flying
  frames therefore retain the airborne collision behavior.
- `LADX-Disassembly/src/code/entities/bank3.asm:709-750` handles the generic
  `ENTITY_OPT1_SWORD_CLINK_OFF` sword result. For a non-Knight/non-Genie
  entity it calls the shared label that emits the sword-poke response, sets
  ignore-hits to `$10`, clears recoil velocities, and skips normal damage.
- `LADX-Disassembly/src/code/entities/bank3.asm:6C72-6CCB` keeps Link-contact
  handling separate from sword collision. The Java runtime must therefore
  suppress only PeaHat's airborne contact result while still evaluating the
  sword rectangle.

## Design

Replace the unconditional airborne PeaHat combat skip with two decisions:

1. A PeaHat that is not grounded contributes no Link-contact collision.
2. The same airborne PeaHat remains eligible for sword-rectangle overlap and
   routes that overlap through `swordPokeForSwordCollision`, alongside the
   existing spike-trap clink path.

Keep the existing clink branch as the single implementation of the response:
it sets the `$10` countdown, clears recoil, emits jingle `$07`, and attaches
the source-shaped sword-poke VFX. Grounded PeaHat continues using the normal
health-group `$00` damage and shared bank-$07 recoil path.

## Tests

Advance a live PeaHat once so its ROM state becomes taking-off, then submit a
sword rectangle that overlaps it with Link interaction disabled. Assert one
active event with zero enemy damage, jingle `$07`, exact sword-poke
coordinates, unchanged health, cleared recoil, and ignore-hits `$10`. The
existing grounded/airborne contact regression remains in place.

## Scope boundary

This increment does not add a general options-table model, expose raw hitbox
flag bytes, or change PeaHat ground/water/pit/conveyor handling. Other
clink-off entity handlers remain separate gaps.
