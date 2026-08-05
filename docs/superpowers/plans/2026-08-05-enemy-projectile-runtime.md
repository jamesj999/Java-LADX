# Enemy Projectile Runtime Implementation Plan

> **For agentic workers:** execute each task test-first. Write the failing
> test, run it and record the expected RED result, then add the smallest
> ROM-faithful implementation that makes it GREEN. Use focused commits after
> each coherent task and run the full suite before claiming completion.

**Goal:** Add ROM-backed Octorok rock (`$0A`) and Moblin arrow (`$0C`) spawning,
movement, collision, rendering, and lifecycle behavior to the Java room-entity
runtime without introducing emulator code or guessed sprite data.

**Design:** `RoamingEnemyMotion` emits a launch request at the exact shared
handler point. `EnemyProjectileMotion` owns per-slot projectile physics and
returns immutable entity updates plus interaction results. `RoomEntityRuntime`
owns reverse-slot allocation and dispatch. `RoomSession` and `Main` route Link
state, health, sounds, and existing VFX/event sinks at the same frame boundary
as the ROM. `EntitySpriteHandlerCatalog` remains the only display-list source.

**Tech:** Java 17, Gradle/JUnit 5, `RomBank`/shipped ROM reads, immutable
`RoomEntity` snapshots, existing indexed framebuffer and sound/VFX sinks.

**Required references:**

- `docs/superpowers/specs/2026-08-05-enemy-projectile-runtime-design.md`
- `LADX-Disassembly/src/code/entities/03_moblin.asm`
- `LADX-Disassembly/src/code/entities/bank3.asm`
- `LADX-Disassembly/src/code/entities/_handlers.asm`
- `LADX-Disassembly/src/data/entities/`

The design document is already committed as `9c77986`. The implementation
must not weaken its non-goals or replace ROM table reads with Java sprite
constants.

---

### Task 1: Decode the two projectile display lists from the ROM

**Files:**

- Modify: `java/src/test/java/linksawakening/entity/EntitySpriteHandlerCatalogTest.java`
- Modify: `java/src/main/java/linksawakening/entity/EntitySpriteHandlerCatalog.java`

**Step 1: Verify the source labels and raw bytes.**

Run:

```bash
rg -n "OctorokRockSpriteVariants|MoblinArrowEntityHandler|EntityArrowSpriteVariants|OctorokRockEntityHandler" \
  LADX-Disassembly/src/code/entities/{bank3.asm,03_arrow.asm}
```

Use the ROM bank/address mapping already used by `decodePair`; do not infer
file offsets from CPU addresses without `RomBank` conversion.

**Step 2: Add failing catalog tests.**

Assert that:

```java
catalog.forEntityType(0x0A, EntityRoomLoader.RoomTable.OVERWORLD)
    == bank 0x03, address 0x6A1E, pair, 2 variants, initial 0;
catalog.forEntityType(0x0C, EntityRoomLoader.RoomTable.OVERWORLD)
    == bank 0x03, address 0x6BC6, pair, 4 variants, initial 0;
```

Add shipped-ROM assertions for all raw tile/attribute bytes in the two
Octorok variants and four `EntityArrowSpriteVariants` variants.

**Step 3: Run the focused test and verify RED.**

```bash
gradle test --tests linksawakening.entity.EntitySpriteHandlerCatalogTest
```

The new assertions must fail because `$0A` and `$0C` are currently
unsupported.

**Step 4: Add the minimal mappings and rerun GREEN.**

Add constants and `decodePair` branches only. Preserve address validation,
shape validation, room-table behavior, and runtime ROM reads.

```bash
gradle test --tests linksawakening.entity.EntitySpriteHandlerCatalogTest
```

**Step 5: Commit.**

```bash
git add java/src/main/java/linksawakening/entity/EntitySpriteHandlerCatalog.java \
  java/src/test/java/linksawakening/entity/EntitySpriteHandlerCatalogTest.java
git commit -m "feat: decode enemy projectile ROM display lists"
```

---

### Task 2: Make roaming enemies emit the ROM launch request

**Files:**

- Modify: `java/src/test/java/linksawakening/world/RoamingEnemyMotionTest.java`
  (create if absent)
- Modify: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`
- Modify: `java/src/main/java/linksawakening/world/RoamingEnemyMotion.java`
- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`

**Step 1: Add failing state/launch tests.**

Drive a roaming Octorok or Moblin to state `$01` with transition countdown
`$0A`, private countdown 1 equal to zero, and a stored direction matching
`GetEntityDirectionToLink_03`. Assert that the update returns a launch request
without changing the parent position. Add negative tests for countdown `$09`,
nonzero private countdown, direction mismatch, Iron Mask, and the credits
gameplay type for Octorok.

Use the exact direction tie-break already present in the Java port and verify
random-byte consumption: the launch check itself must not consume randomness.

**Step 2: Run focused tests and verify RED.**

```bash
gradle test --tests linksawakening.world.RoamingEnemyMotionTest \
  --tests linksawakening.world.RoomEntityRuntimeTest
```

**Step 3: Introduce an explicit update result.**

Replace the `RoomEntity`-only roaming result with a small package-private
update record containing the updated entity and an optional launch request.
Keep state-zero walking, collision stop, state-one restart, inertia, and
background collision behavior unchanged. The launch check must occur before
the parent background-interaction return, matching `AnimateRoamingEnemy`.

Pass credits state through the runtime/session boundary as an explicit context
value rather than silently removing the ROM gate. Existing overloads should
retain their current non-credits default for unit tests and callers that do
not run the credits sequence.

**Step 4: Run the focused tests and verify GREEN.**

```bash
gradle test --tests linksawakening.world.RoamingEnemyMotionTest \
  --tests linksawakening.world.RoomEntityRuntimeTest
```

**Step 5: Commit.**

```bash
git add java/src/main/java/linksawakening/world/RoamingEnemyMotion.java \
  java/src/main/java/linksawakening/world/RoomEntityRuntime.java \
  java/src/test/java/linksawakening/world/RoamingEnemyMotionTest.java \
  java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java
git commit -m "feat: expose ROM enemy projectile launch timing"
```

---

### Task 3: Add the pure projectile physics and source-specific spawn tables

**Files:**

- Create: `java/src/test/java/linksawakening/world/EnemyProjectileMotionTest.java`
- Create: `java/src/main/java/linksawakening/world/EnemyProjectileMotion.java`

**Step 1: Write failing physics tests.**

Cover both projectile types and assert:

- Moblin offsets `$08,$F8,$04,$FC` / `$FC,$FC,$F8,$00` and speeds
  `$20,$E0,$00,$00` / `$00,$00,$E0,$20`;
- Octorok X-offset/speed labels read the adjacent bytes for directions 2/3,
  yielding the same effective four-direction values as the ROM's pointer math;
- initial variant is direction for Moblin and the ROM's default for Octorok;
- ordinary movement applies the signed fixed-point speed once per axis in the
  same order as `UpdateEntityPosWithSpeed_03`;
- wall collision writes countdown `$18`, Z speed `$10`, and negates/shifts Y
  before X exactly three arithmetic shifts;
- transition frames update position, add Z speed, decrement Z speed by `$02`,
  and unload at countdown `$01`;
- Moblin spin variants use `(countdown >> 3) & 3` and the ROM frame table, while
  Octorok remains visually unspun;
- frame and position values wrap as unsigned bytes.

Use synthetic entities and a deterministic collision callback so tests do not
depend on the OpenGL renderer or a live room.

**Step 2: Run and verify RED.**

```bash
gradle test --tests linksawakening.world.EnemyProjectileMotionTest
```

**Step 3: Implement the smallest shared motion class.**

Store per-slot direction, signed speeds, fractional accumulators, Z/speedZ,
transition countdown, and initialized flags. Keep source-specific table data
in one clearly named ROM-derived section and document the intentional
Octorok table adjacency. Return a result containing the updated `RoomEntity`,
an unload flag, and collision metadata; do not mutate `RoomEntity` in place.

**Step 4: Run and verify GREEN.**

```bash
gradle test --tests linksawakening.world.EnemyProjectileMotionTest
```

**Step 5: Commit.**

```bash
git add java/src/main/java/linksawakening/world/EnemyProjectileMotion.java \
  java/src/test/java/linksawakening/world/EnemyProjectileMotionTest.java
git commit -m "feat: add shared enemy projectile physics"
```

---

### Task 4: Implement reverse-slot projectile spawning and runtime dispatch

**Files:**

- Modify: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`
- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`

**Step 1: Add failing integration tests.**

Build snapshots with an Octorok/Moblin parent and disabled slots. Assert:

- the highest disabled slot is selected;
- spawned type is `$0A` or `$0C`, status is active, and
  `sourceLoadOrder == -1`;
- source X/Y/Z are adjusted by the exact direction table;
- direction and initial sprite variant are copied exactly as the ROM spawn
  routine writes them;
- ignore-hits is one frame;
- no persistent clear mask is generated;
- all slots occupied leaves the source and motion arrays unchanged;
- a projectile inserted into a slot that the reverse loop has not yet reached
  is not advanced a second time on its spawn frame.

Add cleanup tests for explicit clear, transition unload, and DYING cleanup.

**Step 2: Run and verify RED.**

```bash
gradle test --tests linksawakening.world.RoomEntityRuntimeTest
```

**Step 3: Wire the launch result.**

Add `spawnEnemyProjectile` beside the existing Pairodd spawn helper. Resolve
the ROM display definition through `EntitySpriteHandlerCatalog`; do not use an
unsupported placeholder when the catalog is unavailable. Initialize the
projectile motion immediately, set its dynamic source order and combat fields,
and record a per-frame `spawnedSlot` guard so reverse iteration cannot tick a
new entity twice.

Dispatch `$0A`/`$0C` after normal entity initialization and dispatch their
projectile motion only for active, non-initializing entities. Keep both types
out of `RoomEntityCombatRules` and clear their motion arrays in every existing
cleanup path.

**Step 4: Run focused and regression tests.**

```bash
gradle test --tests linksawakening.world.RoomEntityRuntimeTest
gradle test --tests linksawakening.world.RoomSessionTest
```

**Step 5: Commit.**

```bash
git add java/src/main/java/linksawakening/world/RoomEntityRuntime.java \
  java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java
git commit -m "feat: spawn and dispatch enemy projectiles"
```

---

### Task 5: Add exact Link projectile collision rules and event plumbing

**Files:**

- Create: `java/src/test/java/linksawakening/world/EnemyProjectileCollisionTest.java`
- Create: `java/src/main/java/linksawakening/world/EnemyProjectileCollision.java`
- Create: `java/src/main/java/linksawakening/world/EntityProjectileEvent.java`
- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
- Modify: `java/src/main/java/linksawakening/world/RoomSession.java`

**Step 1: Write failing pure collision tests.**

Assert the ROM's exact gates:

- motion state `>= $02` and Link Z nonzero do not collide;
- both unsigned-wrapping differences use `+6` and half-open `< $0C`;
- a shield only blocks a projectile whose direction equals the reverse of
  Link's direction;
- shield block reports collision `$FF`, shield-ting sound, and the later
  projectile transition/removal path;
- unshielded contact reports the projectile's Link-damage event;
- the projectile routine is independent of generic enemy hitbox combat.

Use a table-driven test for all four direction pairs and boundary values at
`$05`, `$06`, `$0B`, and `$0C` after wrapping.

**Step 2: Run and verify RED.**

```bash
gradle test --tests linksawakening.world.EnemyProjectileCollisionTest
```

**Step 3: Implement immutable collision context/results.**

Create a narrow context carrying Link entity X/Y, Z, motion state, direction,
shield-use flag, and shield level where the ROM needs it. Return explicit
events for shield block, Link damage, projectile removal, sound, and optional
sword-poke VFX. Do not add a second player-health store. Keep laser/mirror
shield special handling out of this increment, since neither `$0A` nor `$0C`
uses that branch.

**Step 4: Thread events through `RoomEntityRuntime` and `RoomSession`.**

Change the entity tick path to return an immutable event list while preserving
source-compatible overloads for callers that discard the result. Apply
collision before/after movement in the same order as the handlers: Octorok
checks only when transition countdown is zero; Moblin does the same, then both
enter `ArrowRenderAndMove`. Ensure a removal event clears the slot and motion
state exactly once.

**Step 5: Run focused tests and commit.**

```bash
gradle test --tests linksawakening.world.EnemyProjectileCollisionTest \
  --tests linksawakening.world.RoomEntityRuntimeTest \
  --tests linksawakening.world.RoomSessionTest
git add java/src/main/java/linksawakening/world/{EnemyProjectileCollision.java,EntityProjectileEvent.java,RoomEntityRuntime.java,RoomSession.java} \
  java/src/test/java/linksawakening/world/{EnemyProjectileCollisionTest.java,RoomEntityRuntimeTest.java,RoomSessionTest.java}
git commit -m "feat: route enemy projectile Link collisions"
```

---

### Task 6: Route damage, shield state, sound, and existing VFX at the gameplay boundary

**Files:**

- Modify: `java/src/test/java/linksawakening/entity/LinkTest.java`
- Modify: `java/src/test/java/linksawakening/gameplay/GameplaySoundEffectMapTest.java`
  (or the existing sound-map test)
- Modify: `java/src/test/java/linksawakening/MainArchitectureTest.java`
  or add a focused integration test
- Modify: `java/src/main/java/linksawakening/entity/Link.java`
- Modify: `java/src/main/java/linksawakening/Main.java`
- Modify: `java/src/main/java/linksawakening/gameplay/GameplaySoundEvent.java`
- Modify: `java/src/main/java/linksawakening/gameplay/GameplaySoundEffectMap.java`
- Modify: `java/src/main/java/linksawakening/vfx/TransientVfxType.java`
  only if the shipped VFX display list already supports sword-poke

**Step 1: Add failing boundary tests.**

Prove that a projectile damage event reduces `PlayerState` health and sets the
same invincibility countdown used by ordinary contact damage, while a shield
block does not reduce health. Prove that sound events resolve to the shipped
`JINGLE_SHIELD_TING` catalog entry and that events are consumed once.

For shield state, mirror the disassembly's item-use inputs: an equipped shield
in A/B plus the corresponding held button, with the existing gameplay/dialog
blocking gates. Do not treat merely owning a shield as raising it.

**Step 2: Run and verify RED.**

```bash
gradle test --tests linksawakening.entity.LinkTest \
  --tests linksawakening.gameplay.GameplaySoundEffectMapTest
```

**Step 3: Add the minimum integration.**

Expose the current frame's ROM-equivalent shield-use state from `Link` or a
small player interaction context. In `Main`, pass Link state and shield state
into `RoomSession.tickEntities`, then consume returned projectile events after
the entity tick. Apply damage only when the existing player invincibility gate
allows it. Route shield ting through `GameplaySoundSink`; route sword-poke
only through an already-supported ROM VFX path, otherwise retain it as an
explicit event without inventing art.

Keep the original frame order: Link movement, generic enemy combat, entity
animation/projectile tick, then event consumption for projectile contact.

**Step 4: Run focused and full gameplay tests.**

```bash
gradle test --tests linksawakening.entity.LinkTest \
  --tests linksawakening.world.RoomSessionTest \
  --tests linksawakening.gameplay.GameplaySoundEffectMapTest
```

**Step 5: Commit.**

```bash
git add java/src/main/java/linksawakening/{Main.java,entity/Link.java,gameplay/GameplaySoundEvent.java,gameplay/GameplaySoundEffectMap.java} \
  java/src/main/java/linksawakening/vfx/TransientVfxType.java \
  java/src/test/java/linksawakening/{entity/LinkTest.java,gameplay/GameplaySoundEffectMapTest.java,MainArchitectureTest.java}
git commit -m "feat: integrate enemy projectile damage and shield feedback"
```

---

### Task 7: Verify ROM-backed rendering and room-session behavior

**Files:**

- Modify: `java/src/test/java/linksawakening/render/EntityRenderLayerTest.java`
- Modify: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`
- Modify: `java/src/test/java/linksawakening/world/RoomSessionTest.java`
- Modify: `java/src/main/java/linksawakening/render/EntityRenderLayer.java`

**Step 1: Add failing renderer tests.**

Render synthetic solid tiles for every Octorok rock variant and Moblin arrow
variant and assert the expected two-column positions, 8x16 flips, palette
attributes, entity tile offsets, Z subtraction, clipping, and room-scroll
offsets. Assert that transition-phase Moblin variants rotate while Octorok
rocks retain their raw two-variant list.

**Step 2: Run and verify RED.**

```bash
gradle test --tests linksawakening.render.EntityRenderLayerTest
```

**Step 3: Implement only the necessary renderer/catalog integration.**

Reuse the existing pair helper. Do not add special-case pixel data: the
projectile type's `EntitySpriteDefinition` supplies all tile/attribute bytes.
Use the same render ordering as ordinary room entities and preserve current
unsupported-handler behavior.

**Step 4: Run focused tests and commit.**

```bash
gradle test --tests linksawakening.render.EntityRenderLayerTest \
  --tests linksawakening.world.RoomEntityRuntimeTest \
  --tests linksawakening.world.RoomSessionTest
git add java/src/main/java/linksawakening/render/EntityRenderLayer.java \
  java/src/test/java/linksawakening/render/EntityRenderLayerTest.java \
  java/src/test/java/linksawakening/world/{RoomEntityRuntimeTest.java,RoomSessionTest.java}
git commit -m "feat: render ROM enemy projectiles in rooms"
```

---

### Task 8: Update the roadmap and perform completion-level verification

**Files:**

- Modify: `docs/reconstruction-roadmap.md`

**Step 1: Add an evidence-backed progress entry.**

Record the exact ROM addresses, launch conditions, table adjacency behavior,
reverse-slot semantics, movement/bounce countdowns, Link collision window,
shield direction rule, and rendering coverage. Explicitly list any remaining
object-intersection, recoil, VFX, audio, or player-projectile gaps rather than
claiming the entire entity system is complete.

**Step 2: Run focused verification.**

```bash
gradle test --tests linksawakening.entity.EntitySpriteHandlerCatalogTest \
  --tests linksawakening.world.RoamingEnemyMotionTest \
  --tests linksawakening.world.EnemyProjectileMotionTest \
  --tests linksawakening.world.EnemyProjectileCollisionTest \
  --tests linksawakening.world.RoomEntityRuntimeTest \
  --tests linksawakening.world.RoomSessionTest \
  --tests linksawakening.render.EntityRenderLayerTest
```

**Step 3: Run the complete Java suite and repository checks.**

```bash
gradle clean test
git diff --check
git status --short --branch
```

Only report completion of this increment if all commands pass and the working
tree contains only intentionally committed documentation/code. The broader
engine objective remains open until the unsupported entity families and room
systems are implemented and independently verified.

**Step 4: Commit the roadmap update.**

```bash
git add docs/reconstruction-roadmap.md
git commit -m "docs: track enemy projectile runtime progress"
```
