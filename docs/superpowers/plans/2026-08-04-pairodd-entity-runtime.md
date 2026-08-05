# Pairodd Entity Runtime Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add ROM-backed Pairodd `$57` and projectile `$58` display, motion, spawning, lifecycle, and shared combat metadata to the Java room runtime.

**Architecture:** Keep Pairodd's three handler states in a focused `PairoddMotion` class and projectile fixed-point travel in a focused `PairoddProjectileMotion` class. `RoomEntityRuntime` owns dispatch, reverse-slot dynamic spawning, and lifecycle cleanup; `EntitySpriteHandlerCatalog` remains the only display-list decoder. Add one renderer special case for the ROM's variant-three double-pair draw sequence.

**Tech Stack:** Java 17, Gradle/JUnit 5, ROM byte reads through `RomBank`, existing immutable `RoomEntity` snapshots, and the current indexed framebuffer renderer.

---

### Task 1: Record and commit the ROM design

**Files:**
- Create: `docs/superpowers/specs/2026-08-04-pairodd-entity-runtime-design.md`
- Create: `docs/superpowers/plans/2026-08-04-pairodd-entity-runtime.md`

- [ ] **Step 1: Verify the source references before editing Java**

Run:

```bash
rg -n "PairoddSpriteVariants|PairoddEntityHandler|SpawnPairoddProjectile|PairoddProjectile" LADX-Disassembly/src/code/entities/04_pairodd.asm
rg -n "\._57|\._58" LADX-Disassembly/src/code/entities/_handlers.asm LADX-Disassembly/src/data/entities
```

Expected: the handler, display-list, initializer, health, hitbox, option, and
physics entries named in the design document are present.

- [ ] **Step 2: Self-review the written design and plan**

Check that every design requirement maps to a later test or implementation
task, that no task says “implement later” without identifying the excluded
subsystem, and that `$57`/`$58` are not accidentally routed through generic
enemy combat.

- [ ] **Step 3: Commit the design artifacts**

```bash
git add docs/superpowers/specs/2026-08-04-pairodd-entity-runtime-design.md docs/superpowers/plans/2026-08-04-pairodd-entity-runtime.md
git commit -m "docs: plan ROM-driven Pairodd runtime"
```

### Task 2: Add failing ROM display-list catalog tests

**Files:**
- Modify: `java/src/test/java/linksawakening/entity/EntitySpriteHandlerCatalogTest.java`
- Modify: `java/src/main/java/linksawakening/entity/EntitySpriteHandlerCatalog.java`

- [ ] **Step 1: Write the failing catalog assertions**

Add Pairodd and projectile constants to the supported-handler test and assert:

```java
assertDefinition(catalog.forEntityType(0x57, EntityRoomLoader.RoomTable.INDOORS_A),
    0x04, 0x5DD1, EntitySpriteDefinition.Shape.PAIR, 8, 0);
assertDefinition(catalog.forEntityType(0x58, EntityRoomLoader.RoomTable.INDOORS_A),
    0x04, 0x5EF4, EntitySpriteDefinition.Shape.PAIR, 2, 0);
```

Add a shipped-ROM test that reads the two definitions and checks Pairodd's
first eight raw pairs are `$70/$72`, `$72/$70`, `$74/$74`, `$00/$00`,
`$7A/$7A`, `$FF/$FF`, `$76/$78`, `$78/$76` with the source attributes.

- [ ] **Step 2: Run the focused test and verify the expected failure**

```bash
gradle test --tests linksawakening.entity.EntitySpriteHandlerCatalogTest
```

Expected: compilation succeeds but the new Pairodd catalog assertions fail
because the catalog currently returns an unsupported definition.

- [ ] **Step 3: Add the minimal ROM-backed mappings**

In `EntitySpriteHandlerCatalog`, add constants `$57` and `$58`, then add:

```java
if (entityType == ENTITY_PAIRODD) {
    return decodePair(entityType, 0x04, 0x5DD1, 8, 0);
}
if (entityType == ENTITY_PAIRODD_PROJECTILE) {
    return decodePair(entityType, 0x04, 0x5EF4, 2, 0);
}
```

Keep the ROM address validation and decoder unchanged.

- [ ] **Step 4: Run the focused test and verify it passes**

```bash
gradle test --tests linksawakening.entity.EntitySpriteHandlerCatalogTest
```

Expected: the catalog test class passes, including the shipped-ROM byte
checks.

- [ ] **Step 5: Commit the catalog increment**

```bash
git add java/src/main/java/linksawakening/entity/EntitySpriteHandlerCatalog.java java/src/test/java/linksawakening/entity/EntitySpriteHandlerCatalogTest.java
git commit -m "feat: decode Pairodd ROM display lists"
```

### Task 3: Add failing unit tests for Pairodd's state machine

**Files:**
- Create: `java/src/test/java/linksawakening/world/PairoddMotionTest.java`
- Modify: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`

- [ ] **Step 1: Write direct motion tests for countdown and variants**

Use a supported synthetic pair definition and an `INIT` room entity. Assert
that initialization consumes exactly one random byte, frame zero and frame
sixteen select variants zero and one, and a zero countdown enters state one
with countdown `$20` only when Link is inside both signed `$20` windows.

- [ ] **Step 2: Write direct motion tests for the disappearing phase**

Set the motion to the post-resting state through public test-visible accessors
or by ticking the ROM sequence. Assert countdown values below `$18` choose
`4`, `3`, `2`; countdown zero hides the sprite, writes `$40`, enters state two,
and transforms `(x,y)` to `($A0-x,$90-y)` modulo `$100`.

- [ ] **Step 3: Write direct motion tests for reappearing and flash gating**

Assert countdown values below `$18` choose `2`, `3`, `4`, countdown zero
returns to resting with `$30`, and a nonzero flash countdown prevents the
proximity transition. Assert countdown `$18` requests a projectile spawn but
does not teleport.

- [ ] **Step 4: Run the new tests and verify the expected failure**

```bash
gradle test --tests linksawakening.world.PairoddMotionTest --tests linksawakening.world.RoomEntityRuntimeTest
```

Expected: the tests fail to compile or fail on missing Pairodd runtime state;
do not add production behavior before observing this red result.

### Task 4: Implement Pairodd main motion

**Files:**
- Create: `java/src/main/java/linksawakening/world/PairoddMotion.java`
- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`

- [ ] **Step 1: Implement slot state storage and ROM initializer consumption**

Add unsigned-byte arrays for state, transition countdown, stored direction,
and initialized flags. `initialize(slot, random)` must reset state and consume
`random.getAsInt() & 0x03`; fallback initialization for an already-active test
snapshot must use direction zero without consuming random. `clear(slot)` must
reset state and mark the slot uninitialized.

- [ ] **Step 2: Implement `advance` in ROM order**

Decrement the countdown first. In state zero, calculate signed-byte Link
distances, request a spawn at `$18`, return for values above `$18`, and start
state one only for both distances in `[-$20,$20)` and zero flash countdown. In
state one and two, use the exact thresholds, variant tables, hidden `-1`
variant, and mirrored coordinate formulas from the design. Return a small
update record containing the new entity and a boolean spawn request.

- [ ] **Step 3: Wire dispatch and cleanup**

Initialize `$57` during the existing `INIT` branch, call its update during the
active branch, and clear it in both `clearEntity` and
`disableEntityWithoutPersistence`. Apply the returned entity to the existing
snapshot replacement path and call the projectile spawn helper when requested.

- [ ] **Step 4: Run focused tests and fix only implementation defects**

```bash
gradle test --tests linksawakening.world.PairoddMotionTest --tests linksawakening.world.RoomEntityRuntimeTest
```

Expected: all Pairodd motion tests and all pre-existing room runtime tests pass.

- [ ] **Step 5: Commit the main-handler increment**

```bash
git add java/src/main/java/linksawakening/world/PairoddMotion.java java/src/main/java/linksawakening/world/RoomEntityRuntime.java java/src/test/java/linksawakening/world/PairoddMotionTest.java java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java
git commit -m "feat: add Pairodd teleport state machine"
```

### Task 5: Add failing projectile spawn and motion tests

**Files:**
- Create: `java/src/test/java/linksawakening/world/PairoddProjectileMotionTest.java`
- Modify: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`

- [ ] **Step 1: Test highest-free-slot spawn semantics**

Create a room with a Pairodd in slot zero and disabled slots one and fifteen.
Drive its countdown to `$18`, tick once with Link at a known coordinate, and
assert that slot fifteen becomes active type `$58`, has source load order `-1`,
copies the Pairodd position/Z, and uses the projectile display definition.

- [ ] **Step 2: Test the ROM vector and projectile cadence**

Choose an entity/link displacement whose infinity-norm vector of length `$18`
has distinct X and Y components. Assert the stored unsigned speeds match the
ROM divide loop, each tick uses the signed bank-$04 fixed-point accumulators,
and frame `$00`/`$08` select variants zero/one.

- [ ] **Step 3: Test slot exhaustion and cleanup**

Fill all sixteen slots and assert the `$18` Pairodd frame does not create an
extra entity. Clear a spawned projectile explicitly and through the DYING
cleanup path, then assert its motion state is reset and no persistent mask is
returned for source load order `-1`.

- [ ] **Step 4: Run the new tests and verify the expected failure**

```bash
gradle test --tests linksawakening.world.PairoddProjectileMotionTest --tests linksawakening.world.RoomEntityRuntimeTest
```

Expected: the new tests fail because `$58` spawning and motion are not yet
implemented.

### Task 6: Implement projectile motion and dynamic spawn

**Files:**
- Create: `java/src/main/java/linksawakening/world/PairoddProjectileMotion.java`
- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`

- [ ] **Step 1: Implement fixed-point projectile motion**

Store speed and fractional accumulator arrays per slot. `initializeSpawn`
must calculate the ROM infinity-norm vector toward Link with length `$18`,
write signed-byte speeds, and reset accumulators. `advance` must update X then
Y with the bank-$04 nibble accumulator and return `(frame >> 3) & 1`.

- [ ] **Step 2: Implement `spawnPairoddProjectile`**

Search slots from `$0F` down to `$00`. On success create an active dynamic
`RoomEntity` at the source Pairodd's position and Z, choose the catalog
definition for `$58`, set its supported initial variant, set ignore-hits to
`$01`, and initialize the projectile vector. On failure leave every slot and
motion array unchanged.

- [ ] **Step 3: Dispatch and clean up `$58`**

Initialize a room-loaded `$58` without consuming random in the active-snapshot
fallback, dispatch active projectile updates after the same reverse-slot loop
ordering used by the ROM, and clear projectile state in both cleanup paths.
Do not route `$58` into generic enemy combat or invent a background collision
callback for its handler.

- [ ] **Step 4: Run focused tests and full tests**

```bash
gradle test --tests linksawakening.world.PairoddProjectileMotionTest --tests linksawakening.world.RoomEntityRuntimeTest
gradle test
```

Expected: both commands pass.

- [ ] **Step 5: Commit the projectile increment**

```bash
git add java/src/main/java/linksawakening/world/PairoddProjectileMotion.java java/src/main/java/linksawakening/world/RoomEntityRuntime.java java/src/test/java/linksawakening/world/PairoddProjectileMotionTest.java java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java
git commit -m "feat: add Pairodd projectile spawning and motion"
```

### Task 7: Add failing renderer and combat metadata tests

**Files:**
- Modify: `java/src/test/java/linksawakening/render/EntityRenderLayerTest.java`
- Modify: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`
- Modify: `java/src/main/java/linksawakening/render/EntityRenderLayer.java`
- Modify: `java/src/main/java/linksawakening/world/RoomEntityCombatRules.java`

- [ ] **Step 1: Write the Pairodd composite renderer test**

Build a definition with identifiable solid tiles in variants six and seven,
render an entity at X `$28`, and assert pixels appear at the four ROM columns
`entityX - 8`, `entityX`, `entityX + 8`, and `entityX + 16`, with the gap between
the two pairs left transparent.

- [ ] **Step 2: Write the combat metadata test**

Assert `$57` uses the normal enemy collision path, reports contact damage `$04`,
starts with two health points, and remains active after one basic sword hit.
Assert the second hit destroys it after the ROM flash window. Assert `$58` is
absent from the generic enemy/sword pass.

- [ ] **Step 3: Run the tests and verify the expected failure**

```bash
gradle test --tests linksawakening.render.EntityRenderLayerTest --tests linksawakening.world.RoomEntityRuntimeTest
```

Expected: the new renderer test draws the ordinary raw variant-three pair and
the combat test reports unsupported/incorrect `$57` metadata.

- [ ] **Step 4: Implement the minimal renderer branch and combat table entries**

Add a private pair-render helper in `EntityRenderLayer`. Before ordinary pair
rendering, recognize entity type `$57` and variant `$03`, render definition
variants six and seven at the two shifted pair origins, and return. Add `$57`
to the normal enemy switch, contact-damage switch, and two-health switch in
`RoomEntityCombatRules`; leave `$58` out.

- [ ] **Step 5: Run focused and full tests**

```bash
gradle test --tests linksawakening.render.EntityRenderLayerTest --tests linksawakening.world.RoomEntityRuntimeTest
gradle test
```

Expected: both commands pass.

- [ ] **Step 6: Commit the render/combat increment**

```bash
git add java/src/main/java/linksawakening/render/EntityRenderLayer.java java/src/main/java/linksawakening/world/RoomEntityCombatRules.java java/src/test/java/linksawakening/render/EntityRenderLayerTest.java java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java
git commit -m "feat: render and damage Pairodd from ROM data"
```

### Task 8: Update the reconstruction roadmap and verify the branch

**Files:**
- Modify: `docs/reconstruction-roadmap.md`

- [ ] **Step 1: Add a Pairodd status entry**

Document the bank `$04` display lists, `$20/$40/$30` state countdowns, exact
mirrored coordinates, reverse-slot `$58` spawn, `$18` vector, fixed-point
travel, normal hitbox, and two-health/$04-contact values. Explicitly retain
projectile shield/object collision, recoil, audio/VFX, and broader damage-state
gaps as pending.

- [ ] **Step 2: Run the final verification commands**

```bash
gradle clean test
git diff --check
git status --short --branch
```

Expected: Gradle reports `BUILD SUCCESSFUL`, diff check has no output, and the
branch contains only the intentional committed state.

- [ ] **Step 3: Commit the roadmap and final slice**

```bash
git add docs/reconstruction-roadmap.md
git commit -m "docs: track Pairodd runtime coverage"
```
