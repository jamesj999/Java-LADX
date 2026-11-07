# ROM Laser Runtime Design

## Goal

Port the reachable Beamos laser path from the LADX disassembly into the Java
room-entity runtime. The increment must cover the visible parent, its rotating
Link sensor, the moving beam, the beam's transient visual effect, and the
mirror-shield reflection branch.

## Source of truth

- `LADX-Disassembly/src/code/entities/04_laser.asm`
  - `LaserSpriteVariants` at bank `$04:$6C2D`.
  - `LaserEntityHandler` rotates every eight frames, creates a state-1
    sensor, and spawns entity `$2B` at transition countdown `$10`.
  - `LaserLinkSensorHandler` uses the sixteen signed speed bytes, the `$20`
    parent countdown, and the `$40` vector-toward-Link operation.
- `LADX-Disassembly/src/code/entities/15_laser_beam.asm`
  - Entity `$2B` is invisible; it checks Link, moves with fixed-point speed,
    intersects room objects, and emits transient VFX `$06` at `X + $04`.
  - A collision other than mirror-shield reflection clears the beam. A
    reflection reverses the active axis, resets the spin attack, ignores Link
    collisions for `$10` frames, and writes the direction-specific Link speed.
- `LADX-Disassembly/src/code/entities/bank3.asm`
  - `CheckLinkCollisionWithProjectile` defines the `$0C` unsigned contact
    window, normal-shield behavior, and the mirror-shield direction table at
    `Data_003_6BDA`.
- `LADX-Disassembly/src/code/bank2.asm`
  - `RenderTranscientLaserBeam` writes one OAM entry using tile `$24` and an
    alternating attribute bit. The VFX countdown is `$10` because the beam
    handler overwrites the default `$0F` after spawning it.

## Design

### Entity sprite data

`EntitySpriteHandlerCatalog` will decode the parent pair directly from the ROM
display list. The beam and sensor remain render-invisible, matching the source
handlers; they do not receive guessed placeholder tiles.

### Runtime state

`LaserMotion` will own per-slot state that is not present in the immutable
`RoomEntity` record:

- parent vs. sensor vs. beam role;
- parent inertia, direction, transition countdown, and flash countdown;
- sensor parent slot and fixed-point X/Y speeds;
- beam direction, fixed-point X/Y speeds, and Link-ignore countdown.

The room runtime will allocate dynamic slots from the same high-to-low pool as
`SpawnNewEntity`. Newly spawned sensors and beams will not run until the next
entity pass, matching reverse-order entity iteration.

### Projectile event boundary

`EnemyProjectileCollision` will retain its existing rock/arrow API and add the
beam rules. `LinkState` will carry shield level, invincibility countdown, and
the existing active-use flag while retaining a compatibility constructor for
callers that only provide the original six values.

Beam events will carry an optional ROM-shaped sword-poke VFX coordinate. The
gameplay consumer will route that request to the existing transient VFX system
and preserve the existing hurt/shield sound boundary.

### VFX rendering

`TransientVfxType.LASER_BEAM` will use id `$06` and countdown `$10`.
`CutLeavesEffectRenderer` will gain the one-entry bank-$02 renderer from
`RenderTranscientLaserBeam`: tile `$24`, no position bias beyond converting
the ROM OAM origin to framebuffer top-left, and attribute bit `$10` selected
from `(frameCounter XOR slotIndex)` as in the source. The renderer will accept
the frame counter for this effect while preserving existing VFX behavior.

### Integration

`RoomSession` will pass Link shield level and invincibility state into the
entity tick. `Main` will pass the real `PlayerState` values and consume beam
VFX/sound/damage events at the existing gameplay boundary. `PlayerState` will
track the ROM shield level, defaulting to the save/debug level `$01` already
used by the project’s debug-style starting state.

## Deliberate exclusions

- Parent Laser contact damage remains on the existing generic enemy collision
  path unless a source-backed regression exposes a missing branch; this slice
  focuses on the projectile-specific laser path and presentation.
- No emulator integration, guessed beam sprites, or synthetic room entities.
- The broader enemy projectile families and dungeon-specific laser room state
  remain separate follow-up work.

## Verification strategy

Tests will first fail for:

1. ROM parent display-list decoding and invisible beam handling;
2. exact transient `$06` tile/attribute/countdown behavior;
3. sensor cadence, parent countdown, and beam spawning;
4. beam movement, normal collision removal, and mirror-shield reflection;
5. gameplay-boundary forwarding of reflected sword-poke VFX and damage.

Focused tests will run before the full Gradle suite, followed by a clean build
and `git diff --check`.
