# ROM-Driven Water Tektite Entity Runtime Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Water Tektite (`$99`) as a ROM-backed room entity with its bank-$07 display list, three-state movement loop, collision reset, and shared combat metadata.

**Architecture:** Keep Water Tektite state in a dedicated `WaterTektiteMotion` class, following the existing per-handler motion-object pattern. `RoomEntityRuntime` owns lifecycle dispatch, `EntitySpriteHandlerCatalog` decodes the display bytes from the ROM, and `RoomEntityCombatRules` supplies the data-table-derived hitbox and health behavior. The background callback remains the narrow directional collision seam already used by other motion classes.

**Tech Stack:** Java 21, Gradle, JUnit 5, the checked-in LADX disassembly, and the shipped ROM fixture.

---

### Task 1: Add failing ROM display-list coverage

**Files:**
- Modify: `java/src/test/java/linksawakening/entity/EntitySpriteHandlerCatalogTest.java`
- Reference: `LADX-Disassembly/src/code/entities/07_water_tektite.asm:1-8`

- [ ] **Step 1: Write the failing catalog test**

Add a test that asks the catalog for entity `$99` and asserts bank `$07`,
address `$752D`, pair shape, two variants, and the decoded OAM entries:

```java
@Test
void waterTektiteDecodesTheBankSevenPairVariantsFromTheRom() {
    EntitySpriteDefinition definition = catalog().forEntityType(
        0x99, EntityRoomLoader.RoomTable.INDOORS_A);

    assertEquals(0x07, definition.bank());
    assertEquals(0x752D, definition.address());
    assertEquals(EntitySpriteDefinition.Shape.PAIR, definition.shape());
    assertEquals(2, definition.variantCount());
    assertEquals(new EntitySpriteDefinition.OamAttribute(0x70, 0x00),
        definition.variant(0).first());
    assertEquals(new EntitySpriteDefinition.OamAttribute(0x70, 0x20),
        definition.variant(0).second());
    assertEquals(new EntitySpriteDefinition.OamAttribute(0x72, 0x00),
        definition.variant(1).first());
    assertEquals(new EntitySpriteDefinition.OamAttribute(0x72, 0x20),
        definition.variant(1).second());
}
```

- [ ] **Step 2: Run the focused catalog test and verify red**

Run from `java/`:

```bash
gradle test --tests linksawakening.entity.EntitySpriteHandlerCatalogTest
```

Expected result: failure because entity `$99` is not yet mapped by the catalog.

### Task 2: Add failing motion and runtime lifecycle tests

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`
- Reference: `LADX-Disassembly/src/code/entities/07_water_tektite.asm:10-128`

- [ ] **Step 1: Add a test fixture for a two-variant Water Tektite**

Use the existing `pairDefinition(0x99, 2)` helper or the catalog fixture and
create an entity at `(64,64)` with `EntityStatus.ACTIVE`. Use a deterministic
random supplier that returns `$00` for `privateState1 = -1` and `$02` for
`privateState2 = +1`.

- [ ] **Step 2: Write the failing state-machine test**

Cover the ROM order and constants:

```java
runtime.tick(0x10, 0, 0, sequence(0x00, 0x02));
assertEquals(0x01, runtime.waterTektiteState(0));
assertEquals(0x20, runtime.waterTektiteTransitionCountdown(0));
assertEquals(0xFF, runtime.waterTektitePrivateState1(0));
assertEquals(0x01, runtime.waterTektitePrivateState2(0));

runtime.tick(0x11, 0, 0, sequence());
assertEquals(0xFF, runtime.waterTektiteSpeedX(0));
assertEquals(0x01, runtime.waterTektiteSpeedY(0));
assertEquals(0x00, runtime.snapshot().slots().get(0).spriteVariant());

for (int frame = 0x12; frame < 0x31; frame++) {
    runtime.tick(frame, 0, 0, sequence());
}
assertEquals(0x02, runtime.waterTektiteState(0));
```

Also assert that the frame-bit-4 variant is 1 at frame `$10` and 0 at frame
`$00`, that state 2 changes speed only on even frames, and that the signed
fixed-point update moves X left and Y right by one pixel every 16 frames.

- [ ] **Step 3: Write collision and clear tests**

Supply a callback that blocks the moved coordinate. Assert the entity returns
to its pre-move coordinate, speeds become zero, state becomes 0, and the
transition countdown becomes `$10`. Then resolve/clear the entity and assert
that the motion accessors reset and the slot is disabled through the existing
lifecycle.

- [ ] **Step 4: Run the focused runtime test and verify red**

Run:

```bash
gradle test --tests linksawakening.world.RoomEntityRuntimeTest
```

Expected result: compilation or assertion failure because the Water Tektite
motion accessors and dispatch do not exist yet.

### Task 3: Add failing combat-data coverage

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`
- Reference: `LADX-Disassembly/src/data/entities/health_groups.asm:157`, `health.asm:4`, `damages.asm:5`, `hitbox_flags.asm:161`, `options1.asm:161`, `physics_flags.asm:179`

- [ ] **Step 1: Assert the ROM health group and normal hitbox behavior**

Create a `$99` entity at the same coordinates as Link. Assert one initial
health, four Link contact damage, and a basic sword hit that lowers health to
zero and enters the existing `DYING` state. Assert a normal-size sword/link
overlap boundary rather than the Spike Trap big-box boundary.

- [ ] **Step 2: Run the focused combat test and verify red**

Use the same `gradle test --tests linksawakening.world.RoomEntityRuntimeTest`
command and confirm the new `$99` assertions fail before implementation.

### Task 4: Implement the dedicated ROM motion class

**Files:**
- Create: `java/src/main/java/linksawakening/world/WaterTektiteMotion.java`
- Reference: `LADX-Disassembly/src/code/entities/07_water_tektite.asm:48-128`, `LADX-Disassembly/src/code/entities/bank7.asm:275-333`

- [ ] **Step 1: Add per-slot state and reset methods**

Declare arrays for `state`, `transitionCountdown`, `speedX`, `speedY`,
`speedXAccumulator`, `speedYAccumulator`, `privateState1`, `privateState2`,
and `initialized`. `initialize` and `clear` must reset every array entry to
zero; `clear` then marks the slot uninitialized.

- [ ] **Step 2: Implement the bank-$07 signed 4.4 position helper**

Use the same byte behavior as `AddEntitySpeedToPos_07`: do nothing for zero
speed; add `(speed << 4) & $F0` to the per-slot accumulator; sign-extend the
high nibble; add the carry as one pixel; and wrap the position to `$00-$FF`.

- [ ] **Step 3: Implement the three ROM states**

On each advance, decrement a nonzero transition countdown, move X then Y with
the current speed, and select `(frameCounter >>> 4) & 1`. Then implement:

- State 0: if the countdown is nonzero, keep state 0; otherwise set `$20`,
  enter state 1, and consume two random bytes as `(random & 2) - 1`.
- State 1: if the countdown is zero, enter state 2; otherwise, only when the
  countdown bit 0 is clear, add private state 1/2 to speed X/Y.
- State 2: only on even frame counters, if speed X is zero, set state 0 and
  countdown `$10`; otherwise move both signed speeds one step toward zero.

- [ ] **Step 4: Implement collision reset and accessors**

When the background callback blocks a nonzero movement, restore the coordinate
before that axis's update, clear both speeds, set state 0 and countdown `$10`,
and return the frame-selected variant. Expose package-private accessors for
state, countdown, speed X/Y, and private state 1/2 for the tests.

### Task 5: Wire catalog, runtime, and combat tables

**Files:**
- Modify: `java/src/main/java/linksawakening/entity/EntitySpriteHandlerCatalog.java`
- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
- Modify: `java/src/main/java/linksawakening/world/RoomEntityCombatRules.java`

- [ ] **Step 1: Add the ROM display mapping**

Map `$99` to `decodePair(0x99, 0x07, 0x752D, 2, 0)`; do not add tile or
attribute literals to the catalog.

- [ ] **Step 2: Add runtime lifecycle dispatch**

Declare `ENTITY_WATER_TEKTITE = 0x99`, add `WaterTektiteMotion`, initialize it
on INIT without consuming a random byte, advance it after INIT with the current
frame and background callback, and clear it in both disable paths. Preserve the
motion-selected variant when the generic `variantFor` switch runs.

- [ ] **Step 3: Add shared combat constants**

Include `$99` in `supportsEnemyCollision`, contact damage `$04`, and initial
health `$01`. Do not change the normal hitbox dimensions; the ROM hitbox table
selects `HitboxPositions._00`.

### Task 6: Document, verify, and commit

**Files:**
- Modify: `docs/reconstruction-roadmap.md`

- [ ] **Step 1: Add a roadmap entry**

Record that `$99` now uses bank `$07:$752D`, noop initialization, the frame-bit-4
pair animation, three-state acceleration/deceleration loop, collision reset,
normal enemy hitbox, and health group `$00`; explicitly note that full recoil
and ground interaction remain pending.

- [ ] **Step 2: Run focused tests**

```bash
gradle test --tests linksawakening.entity.EntitySpriteHandlerCatalogTest \
  --tests linksawakening.world.RoomEntityRuntimeTest
```

Expected result: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run clean verification**

```bash
gradle clean test
git diff --check
git status --short --branch
```

Expected result: all tests pass, no whitespace errors, and the only status
line is the intended feature branch with its intended changes.

- [ ] **Step 4: Commit the increment**

```bash
git add docs/reconstruction-roadmap.md \
  docs/superpowers/specs/2026-08-04-water-tektite-entity-runtime-design.md \
  docs/superpowers/plans/2026-08-04-water-tektite-entity-runtime.md \
  java/src/main/java/linksawakening/entity/EntitySpriteHandlerCatalog.java \
  java/src/main/java/linksawakening/world/RoomEntityCombatRules.java \
  java/src/main/java/linksawakening/world/RoomEntityRuntime.java \
  java/src/main/java/linksawakening/world/WaterTektiteMotion.java \
  java/src/test/java/linksawakening/entity/EntitySpriteHandlerCatalogTest.java \
  java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java
git commit -m "Implement ROM-driven Water Tektite runtime"
```
