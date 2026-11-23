# ROM Laser Runtime Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `executing-plans` to implement this plan task-by-task with review checkpoints.

**Goal:** Port the ROM's Beamos parent/sensor/beam path and transient laser VFX into the live Java room runtime.

**Architecture:** Decode the parent display list from ROM, keep sensor/beam handler state in a dedicated `LaserMotion`, extend the existing projectile event boundary for mirror-shield behavior, and render transient VFX `$06` through the existing VFX layer.

**Tech Stack:** Java, JUnit 5, Gradle, LWJGL/OpenGL host renderer, shipped `azle.gbc` ROM, LADX assembly source.

---

### Task 1: Add source-backed regression tests first

**Files:**
- Modify: `java/src/test/java/linksawakening/entity/EntitySpriteHandlerCatalogTest.java`
- Modify: `java/src/test/java/linksawakening/vfx/TransientVfxSystemTest.java`
- Modify: `java/src/test/java/linksawakening/world/EnemyProjectileCollisionTest.java`
- Modify: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`

**Steps:**

1. Add tests for the `$04:$6C2D` parent display list and unsupported/invisible `$2B` beam definition.
2. Add tests for VFX `$06` countdown and exact tile/attribute/position output.
3. Add tests for the beam's `$0C` contact window, shield-level gate, direction gate, and reflection event.
4. Add runtime tests proving eight-frame parent rotation, sensor-triggered `$20` countdown, `$10` beam spawn timing, VFX emission, and beam removal/reflection.
5. Run focused tests and confirm they fail for missing behavior.

### Task 2: Implement ROM laser presentation primitives

**Files:**
- Modify: `java/src/main/java/linksawakening/entity/EntitySpriteHandlerCatalog.java`
- Modify: `java/src/main/java/linksawakening/vfx/TransientVfxType.java`
- Modify: `java/src/main/java/linksawakening/vfx/CutLeavesEffectRenderer.java`
- Modify: `java/src/main/java/linksawakening/render/TransientVfxRenderLayer.java`

**Steps:**

1. Decode the parent pair from bank `$04`, address `$6C2D`, eight variants, initial variant zero.
2. Add VFX id `$06` with a `$10` countdown.
3. Implement the one-entry tile `$24` renderer and the ROM attribute-bit cadence.
4. Route the new enum value through the transient render layer.
5. Run the focused presentation tests.

### Task 3: Implement the sensor/beam state machine

**Files:**
- Add: `java/src/main/java/linksawakening/world/LaserMotion.java`
- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
- Modify: `java/src/main/java/linksawakening/world/RoomEntity.java` only if an immutable field is proven necessary

**Steps:**

1. Port the sixteen sensor speeds, eight-frame parent rotation, sensor Link window, and vector-toward-Link speed calculation.
2. Allocate invisible state-1 sensors and invisible `$2B` beams from the high-to-low free-slot search.
3. Port beam fixed-point movement, background intersection, VFX emission, and removal.
4. Preserve reverse entity-pass timing by skipping newly spawned dynamic slots until the next frame.
5. Run focused runtime tests and inspect the diff against the source labels.

### Task 4: Extend mirror-shield projectile events and live integration

**Files:**
- Modify: `java/src/main/java/linksawakening/world/EntityProjectileEvent.java`
- Modify: `java/src/main/java/linksawakening/world/EnemyProjectileCollision.java`
- Modify: `java/src/main/java/linksawakening/world/RoomSession.java`
- Modify: `java/src/main/java/linksawakening/state/PlayerState.java`
- Modify: `java/src/main/java/linksawakening/gameplay/EnemyProjectileEventConsumer.java`
- Modify: `java/src/main/java/linksawakening/Main.java`

**Steps:**

1. Add shield level and invincibility inputs with compatibility constructors/overloads.
2. Emit source-shaped reflection, sword-poke, normal shield, and Link damage events.
3. Forward reflected VFX and existing sound/damage effects at the gameplay boundary.
4. Run focused integration tests.

### Task 5: Verify and document

**Files:**
- Modify: `docs/reconstruction-roadmap.md`

**Steps:**

1. Run the focused laser test classes.
2. Run `git diff --check`.
3. Run `gradle clean test`.
4. Update the roadmap with exact source labels, the visible parent/beam checkpoint, and deferred branches.
5. Commit the implementation only after verification is green.
