# Enemy Sword-Hit Response Design

## Goal

Bring the shared enemy response used by the bank-3 roaming enemies into the
Java entity runtime. A non-lethal sword collision must move the enemy away from
Link using the ROM's fixed-point recoil velocity, suppress repeat hits for the
ROM countdown, and publish the ROM feedback sounds. The implementation must
remain a host-side game runtime, not a general Game Boy emulator.

## ROM source of truth

- `LADX-Disassembly/src/code/entities/bank3.asm:6FCC`
  `ConfigureEntityRecoil` calls `GetVectorTowardsLink` with the default sword
  recoil length `$30`, negates the resulting vector, and stores X/Y recoil
  speeds before starting the ignore-hits window.
- `LADX-Disassembly/src/code/entities/bank3.asm:7F25`
  `UpdateEntityPosWithSpeed_03` updates X and Y independently using signed
  sixteen-subpixel speeds and an eight-bit accumulator.
- `LADX-Disassembly/src/code/entities/bank3.asm:7FA9`
  `ApplyRecoilIfNeeded_03` decrements the ignore-hits countdown, temporarily
  applies the recoil speed, runs background interaction, then restores the
  normal speed.
- `LADX-Disassembly/src/code/home/entities.asm:3EAF`
  `StopEntityRecoilOnCollision` clears the ignore-hits countdown when the
  entity collides in the dominant recoil axis.
- `LADX-Disassembly/src/code/entities/bank3.asm:7162-7221`
  The normal sword path uses recoil `$30`, emits `JINGLE_BUMP` `$09`, and a
  successful damage-table hit emits `JINGLE_ENEMY_HIT` `$03`.

## Design

### Runtime state

Add a dedicated `EnemyRecoilMotion` helper with one state record per entity
slot: signed recoil X/Y speeds, X/Y subpixel accumulators, and whether a recoil
is active. It exposes:

- `configure(...)`, which mirrors the ROM vector calculation from the entity's
  unsigned-byte position plus its Z-adjusted Y distance to Link;
- `advance(...)`, which performs the ROM sixteen-subpixel update and consults
  the existing `RoomEntityBackgroundCollision` callback;
- `clear(...)` for room unloads and slot reuse.

`RoomEntityRuntime` owns this helper. A successful non-lethal normal sword hit
configures recoil with `$30`. At the beginning of each active entity tick, the
runtime applies recoil before the family-specific motion handler, matching the
ordering of `AnimateRoamingEnemy` and the shared handler path. The existing
flash and ignore-hit countdowns remain authoritative for collision suppression;
the recoil helper consumes the same ignore countdown rather than creating a
second timer.

The helper receives a null-safe no-collision policy when callers use the
shorter tick overloads. When a supplied background query blocks the attempted
recoil movement, the helper stops the recoil countdown, matching the ROM's
collision stop behavior at the observable runtime boundary.

### Combat feedback

Extend `EntityCombatEvent` with one raw ROM sound request, represented as
channel and ID rather than a gameplay enum name. `EnemyCollidedWithSword`
first writes `JINGLE_BUMP` `$09`; `ApplySwordDamagesToEnemy` then overwrites the
same `hJingle` register with `JINGLE_ENEMY_HIT` `$03` when the damage table
returns a real hit. Therefore the event exposes the final sound request for
the frame—`$09` when a sword clink is all that happened, `$03` for the normal
Octorok/Moblin damage path—instead of incorrectly queuing both writes.

The existing gameplay layer maps those raw IDs to `GameplaySoundEvent` and
plays them through `GameplaySoundSink`. This keeps ROM decoding in the world
runtime and avoids coupling entity code to the audio implementation.

Death, power-sword damage variants, special enemy collision branches, and the
sword-poke transient VFX are explicitly outside this slice. The current basic
sword damage behavior remains unchanged; implementing the ROM damage matrix
and player attack-state inputs is a separate increment.

## Verification

Tests will cover:

1. The recoil vector points away from Link for horizontal, vertical, and
   diagonal positions, including the ROM's dominant-axis integer division.
2. A `$30` recoil advances with sixteen-subpixel accumulation and consumes the
   `$0A` ignore window; a blocking background query stops the countdown.
3. A non-lethal Octorok/Moblin sword hit configures recoil and returns the
   final `$03` enemy-hit raw feedback; a collision that does not apply damage
   retains `$09` bump feedback.
4. Existing collision cadence, flash/ignore gating, projectile behavior, and
   the clean full Gradle suite remain green.
