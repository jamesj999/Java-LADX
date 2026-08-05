# Pairodd Entity Runtime Design

## Goal

Port the ordinary Pairodd enemy (`$57`) and its dynamically spawned projectile
(`$58`) into the ROM-driven Java room-entity runtime, preserving the disassembly's
display-list selection, state/countdown ordering, teleport coordinates, reverse
slot allocation, and fixed-point projectile vector.

This is one bounded entity increment in the larger reconstruction. It does not
pretend that the general enemy-damage matrix, shield reflection, or every
background interaction is complete.

## ROM source of truth

- `src/code/entities/04_pairodd.asm`
  - `PairoddSpriteVariants` at bank `$04:$5DD1`
  - `PairoddEntityHandler` at bank `$04:$5DF1`
  - `SpawnPairoddProjectile` at bank `$04:$5EC6`
  - `PairoddProjectileSpriteVariants` at bank `$04:$5EF4`
  - `PairoddProjectileEntityHandler` at bank `$04:$5EFC`
- `src/code/entities/_handlers.asm`
  - `$57` and `$58` both use `EntityInitWithRandomDirection`.
- `src/code/entities/bank3.asm`
  - `EntityInitWithRandomDirection` consumes one ROM random byte and stores
    its low two bits as direction.
  - `SpawnNewEntity` searches disabled slots from `$0F` down to `$00`, marks
    the selected slot active, copies the source entity's position, and sets
    the new entity's ignore-hits countdown to `$01`.
  - `ApplyVectorTowardsLink` writes an infinity-norm vector of the requested
    length into the selected entity's X/Y speed tables.
- `src/code/entities/bank4.asm`
  - `UpdateEntityPosWithSpeed_04` uses signed 4-bit-per-frame speeds and
    per-axis fractional accumulators.
- `src/data/entities/{health_groups,health,damages,hitbox_flags,options1,physics_flags}.asm`
  - `$57` is a normal enemy with health group `$30`, splash-in-water option,
    and two allocated sprites.
  - `$58` is a normal projectile collision box with health group `$0E`, no
    ground interaction, projectile noclip, and kill-all exclusion.

## Behavior

### Main Pairodd (`$57`)

`PairoddMotion` owns state, transition countdown, the initializer's direction
byte, and initialized flags per entity slot. The runtime decrements its
transition countdown before dispatch, matching the shared entity countdown
update.

The resting state mirrors the handler's order:

1. The runtime's existing combat pass remains responsible for the preceding
   `DefaultEnemyDamageCollisionHandler` call.
2. The display variant is the current frame counter bit 4.
3. At countdown `$18`, request a projectile spawn and return from the resting
   logic for that frame.
4. At countdown greater than `$18`, return.
5. Otherwise, require Link's signed X and Y distances to be in the half-open
   `[-$20, $20)` window and the ROM flash countdown to be zero. On success,
   write countdown `$20`, increment to disappearing state, and preserve the
   teleport jingle as a future audio integration point.

The disappearing state returns to the default damage path while its countdown
is at least `$18`. Below that threshold it selects the ROM table
`{4, 3, 2}` by countdown divided by eight. At zero it writes countdown `$40`,
advances to reappearing state, hides the sprite, and transforms the position as
the ROM arithmetic does:

```
newX = $50 - (oldX - $50) = $A0 - oldX
newY = $48 - (oldY - $48) = $90 - oldY
```

The reappearing state remains hidden while countdown is at least `$18`. Below
that threshold it selects `{2, 3, 4}` by countdown divided by eight. At zero it
writes countdown `$30`, advances once, and then stores state zero. The sprite
becomes visible again on the next resting handler dispatch, matching the
disassembly's render-before-state-update ordering.

`EntityInitWithRandomDirection` is modeled on room initialization so shared ROM
random-byte consumption remains ordered even though the Pairodd handler does
not currently read its direction after initialization. Active snapshots used
by unit tests get a deterministic zero direction without consuming randomness.

### Projectile (`$58`)

`PairoddProjectileMotion` owns X/Y speeds, fractional accumulators, and
initialized flags per slot. A successful Pairodd spawn:

- uses the highest disabled slot;
- creates an active dynamic entity with `sourceLoadOrder == -1`;
- copies Pairodd's X/Y/Z and uses the projectile display definition;
- initializes the projectile speed to the ROM `$18` vector toward Link;
- sets the new slot's ignore-hits countdown to `$01`.

Each active projectile selects animation variant `(frameCounter >> 3) & 1`
and advances both axes with the bank-$04 signed fixed-point algorithm. The
current `RoomEntityBackgroundCollision` abstraction is not called for this
handler because the ROM path uses sword/object intersection helpers rather
than `ApplyEntityInteractionWithBackground`.

The existing runtime has no shield/reflection event channel and no post-move
object-intersection result channel. Projectile Link collision, shield
reflection, sword-poke VFX, and object collision therefore remain explicit
follow-up work; this slice must not invent generic enemy collision behavior for
`$58`.

### Display lists and rendering

`EntitySpriteHandlerCatalog` decodes `$57` from bank `$04:$5DD1` as eight
pair variants and `$58` from bank `$04:$5EF4` as two pair variants. The ROM
bytes are read at runtime; no PNG or Java tile literal is introduced.

Pairodd variant `$03` is a handler-level composite: the ROM renders pair
variant `$06` at X minus eight and pair variant `$07` at X plus eight. The
renderer will recognize this one ROM-defined case and render the two pairs at
the corresponding four OAM columns, while leaving the catalog's raw eight
pair variants intact.

### Combat and lifecycle

`RoomEntityCombatRules` adds `$57` as a normal enemy with the ROM normal
hitbox, two initial health points from health group `$30`, and `$04` nominal
contact damage from that group's damage byte. `$58` is not added to the generic
sword/enemy pass. Both motion objects are cleared from `clearEntity` and
DYING cleanup paths; dynamic projectile slots must not update persistent room
clear masks.

## Testing

Focused tests will prove:

- catalog bank/address/shape/variant counts and shipped ROM display bytes;
- initializer random consumption and the resting frame animation;
- projectile spawn timing at countdown `$18`, signed Link proximity, flash
  gating, countdown thresholds, state transitions, and exact mirrored
  coordinates;
- disappearing/reappearing variant tables and hidden-frame ordering;
- highest-free-slot dynamic spawn, `sourceLoadOrder == -1`, copied position,
  `$18` vector components, fixed-point projectile movement, and frame-bit-3
  animation;
- normal Pairodd hitbox, two health points, and `$04` contact damage;
- renderer placement for the variant-three four-sprite composite;
- motion reset on both explicit clear and DYING cleanup.

## Non-goals for this increment

- shield reflection and projectile damage event plumbing;
- complete `ApplySwordIntersectionWithObjects` emulation;
- Pairodd recoil, water/pit/conveyor side effects, audio, and VFX;
- the remaining unsupported entity handlers or a generic emulator layer.
