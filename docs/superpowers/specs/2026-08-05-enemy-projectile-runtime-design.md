# ROM-Driven Octorok Rock and Moblin Arrow Runtime Design

## Goal

Port the shared enemy-projectile path used by Octorok rocks (`$0A`) and Moblin
arrows (`$0C`) into the Java room-entity runtime. The increment must preserve
the disassembly's projectile launch conditions, reverse-slot allocation,
direction tables, fixed-point movement, wall response, Link collision and
shield-facing rules, display-list selection, and unload timing.

This is one gameplay increment in the larger reconstruction. It adds the
projectile behavior needed by two common overworld enemies without pretending
that every player projectile, enemy weapon, or boss projectile already shares
the same path.

## Source of truth

- `LADX-Disassembly/src/code/entities/03_moblin.asm`
  - `AnimateRoamingEnemy` at bank `$03:$583C`
  - `SpawnMoblinArrow` and its four-direction offset/speed tables at
    bank `$03:$5947`
  - `SpawnOctorokRock` and its intentionally adjacent table layout at bank
    `$03:$5998`
- `LADX-Disassembly/src/code/entities/bank3.asm`
  - `OctorokRockEntityHandler` at bank `$03:$6A26`
  - `ArrowRenderAndMove` at bank `$03:$6AD4`
  - `ArrowRockAfterHittingWall` at bank `$03:$6B4C`
  - `CheckLinkCollisionWithProjectile` at bank `$03:$6BDE`
  - `EntityBounceOffWallX/Y` at bank `$03:$6B31`
  - `OctorokRockSpriteVariants` at bank `$03:$6A1E`
  - `EntityArrowSpriteVariants` at bank `$03:$6BC6`
- `LADX-Disassembly/src/code/entities/_handlers.asm`
  - `$0A` dispatches to `OctorokRockEntityHandler` and initializes with
    `EntityInitWithRandomDirection`.
  - `$0C` dispatches to `MoblinArrowEntityHandler` and initializes with
    `EntityInitWithRandomDirection`.
- `LADX-Disassembly/src/code/entities/bank3.asm`
  - `SpawnNewEntity` defines the highest-free-slot search, copied source
    fields, and the new entity's one-frame ignore-hits countdown.
  - `UpdateEntityPosWithSpeed_03` defines the regular signed entity movement
    representation used by the shared arrow handler.
  - `func_003_6CC0` and `ApplyLinkCollisionWithEnemy` define the unshielded
    Link-damage path reached by `CheckLinkCollisionWithProjectile`.
- `LADX-Disassembly/src/data/entities/{hitbox_flags,physics_flags,options1,
  health_groups,damages}.asm`
  - These tables document the projectile collision flags and confirm that
    `$0A` and `$0C` are not ordinary targets for the generic enemy combat pass.

The ROM shipped with the project is authoritative for all display-list bytes.
No PNG-derived tile or Java-authored sprite literal will be added.

## Design decisions

### One shared motion object, source-specific launch data

`EnemyProjectileMotion` will own per-slot state for `$0A` and `$0C`:

- direction;
- signed X/Y speed;
- Z position and speed used after a wall hit;
- transition countdown;
- initialization and pending-unload state.

The motion object will expose a result that can update the immutable
`RoomEntity` record and emit projectile interaction events. It will not put
projectile state into `RoamingEnemyMotion`; the latter will only report the
ROM launch event at the point where `AnimateRoamingEnemy` would call
`SpawnOctorokRock` or `SpawnMoblinArrow`.

The two spawn paths will remain distinct data paths:

- Moblin uses four entries for offsets, speeds, direction, and initial sprite
  variant.
- Octorok uses the original labels and byte adjacency. Its X-offset and
  X-speed tables contain two declared bytes, so direction indices `$02` and
  `$03` read the following table's first two bytes exactly as the ROM does.
  The Java port must encode or derive this layout explicitly, not silently
  replace it with a guessed four-entry table.

Successful spawns search disabled slots from `$0F` down to `$00`, set
`sourceLoadOrder == -1`, copy the source position/Z/sign fields represented by
the Java model, set the projectile's initial variant where the ROM does, and
set the one-frame ignore-hits countdown. A full slot leaves the source
unchanged and consumes no projectile state.

### Launch timing and parent state

On the active Octorok/Moblin handler path, launch is eligible only when:

1. the enemy is in state `$01` (the stopped/recovering state);
2. its transition countdown is exactly `$0A`;
3. private countdown 1 is zero;
4. the ROM direction-to-Link result equals the enemy's stored direction;
5. the source is not an Iron Mask; and
6. Octorok does not launch during the credits gameplay type.

The runtime will preserve the ROM's countdown decrement ordering: the
countdown is decremented before handler dispatch, while the `$0A` comparison
is made against the resulting value. Launch must happen before the parent
background interaction returns for that frame, and a reverse-slot entity must
not be ticked again merely because it was inserted into the slot array.

`RoamingEnemyMotion` will continue to own the normal state-0 walking and
state-1 stop/restart transitions. The launch result is an explicit event so
the parent motion does not directly mutate the room slot array.

### Shared movement and wall response

For a projectile with no transition countdown, the handler order is:

1. render the active pair display list;
2. skip interaction if the entity is non-interactive;
3. update position with the signed entity speed;
4. apply the existing object/sword intersection abstraction;
5. if a collision is reported, set countdown `$18`, set Z speed `$10`, alert
   sword-related enemy state, and apply the enemy-projectile wall bounce;
6. otherwise retain the new position and speed.

Enemy-projectile wall bounce negates each speed component and arithmetic-shifts
it right three times. The Y bounce is applied before the X bounce. The
transition phase updates position and then applies the non-side-scrolling
gravity path (`Z += speedZ`, `speedZ -= $02`). At transition countdown `$01`
the entity is unloaded. Octorok rocks retain their normal two-frame display
variant during this phase; Moblin arrows select the four spinning variants by
`(countdown >> 3) & $03` through the ROM's direction-frame table.

Side-scrolling ground-status speed adjustments will be represented in the
motion boundary even if no current room calls this path; the implementation
must not hard-code an overworld-only assumption into the shared projectile
class.

### Link collision and shield behavior

The runtime will add a narrow projectile collision input containing the ROM
entity coordinates and Link state needed by the shared routine:

- Link motion state must be interactive (`< $02`);
- Link Z must be zero;
- unsigned wrapping X and visual-Y differences must both satisfy the ROM's
  `$06`/`$0C` collision window;
- shield use must be checked before unshielded damage;
- ordinary shields block only when the projectile direction is opposite Link's
  direction, using `ReversedDirectionsTable`;
- a blocked projectile writes collision `$FF`, plays the shield-ting sound,
  and is removed by the later enemy-projectile path;
- a non-blocked projectile reaches the ROM's Link-damage event path;
- projectile collision is not routed through `RoomEntityCombatRules`, which
  is reserved for entities whose handlers call the generic enemy collision
  routine.

The Java API will return explicit events for damage, shield block, projectile
removal, sword-poke VFX, and sound requests. The existing gameplay integration
will consume those events so tests can assert state changes without coupling
the motion class to the renderer or audio backend. Damage application will
respect the existing `PlayerState` invincibility/health model and will not
invent a second health store.

### ROM-backed rendering

`EntitySpriteHandlerCatalog` will decode:

- `$0A` from bank `$03:$6A1E` as two pair variants;
- `$0C` from bank `$03:$6BC6` as four pair variants.

The existing pair renderer will be reused. Octorok rocks use their own
two-variant display list, while Moblin arrows use the common four-variant list
and the handler's direction/spin variant updates. Tile bytes and attributes
are read from the ROM catalog; renderer behavior must preserve 8x16 sprite
column ordering, entity flips, object palette bits, clipping, and room scroll.

### Lifecycle and persistence

Dynamic projectiles have no persistent source load-order bit. Explicit clear,
transition unload, and DYING cleanup must clear all projectile motion state.
Room snapshots must never serialize a dynamic projectile as one of the first
eight persistent room entities. A projectile inserted into a disabled slot
must be initialized immediately, just as the existing Pairodd projectile
path does, and must not consume an extra initialization frame.

## Testing

Focused tests will prove:

- exact ROM bank/address, pair counts, and display-list bytes for `$0A` and
  `$0C`;
- `EntityInitWithRandomDirection` byte consumption and low-two-bit direction;
- all Moblin and Octorok spawn offsets/speeds, including Octorok's adjacent
  table behavior for directions `$02` and `$03`;
- launch gating at countdown `$0A`, private-countdown zero, direction match,
  credits exclusion, and full-slot behavior;
- highest-free-slot allocation, copied source fields, dynamic load order,
  initial variant, and one-frame ignore-hits behavior;
- signed movement, collision countdown `$18`, exact speed bounce, Z gravity,
  spinning arrow variants, rock non-spinning behavior, and unload at `$01`;
- Link interaction gates, exact `$06`/`$0C` collision window, shield-facing
  block/reflection, unshielded damage event, and projectile removal;
- renderer placement and ROM attribute/tile selection for both projectile
  families;
- cleanup of motion/event state on explicit clear and transition unload;
- integration through `RoomSession` without changing title, cutscene, or
  unsupported-handler behavior.

Tests will use the shipped ROM for raw-byte assertions and synthetic ROM/state
fixtures for boundary cases. Verification will run focused Gradle tests first,
then the complete `gradle test` suite from `java/`.

## Non-goals for this increment

- player arrows, bomb arrows, magic-rod fireballs, laser beams, boomerangs,
  and boss-specific projectiles;
- the entire generic enemy damage matrix or every missing parent-enemy recoil
  side effect;
- every implementation detail of `ApplySwordIntersectionWithObjects` beyond
  the collision result needed by this shared path;
- a CPU, PPU, OAM, or hardware emulator;
- guessed behavior for any unsupported entity type.
