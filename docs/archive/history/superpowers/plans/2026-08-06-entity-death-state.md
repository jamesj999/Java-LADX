# ROM Entity Death-State Presentation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** Render common non-boss entity deaths from the ROM's bank-$03 rectangle tables while preserving normal entity animation state and the existing runtime countdown boundary.

**Architecture:** Decode the two ROM tables into the existing rectangle display-list model. Carry a dedicated death-frame index and power-recoil-table flag on each RoomEntity, select definitions through EntitySpriteSelection, and render them only for DYING entities. Drops, persistence, bosses, and entity-specific death handlers remain separate increments.

**Tech Stack:** Java records and immutable snapshots, JUnit 5, existing ROM bank helpers, indexed framebuffer/GPU renderer, shipped azle.gbc ROM.

---

## File map

- Modify java/src/main/java/linksawakening/entity/EntitySpriteHandlerCatalog.java: decode normal and power death lists.
- Modify java/src/main/java/linksawakening/entity/EntitySpriteSelection.java: retain optional death definitions and builders.
- Modify java/src/main/java/linksawakening/entity/EntitySpriteCatalog.java: attach definitions to standard and Color Dungeon selections.
- Modify java/src/main/java/linksawakening/world/RoomEntity.java: add death-only snapshot fields.
- Modify java/src/main/java/linksawakening/world/RoomEntityRuntime.java: derive frames from the source countdown.
- Modify java/src/main/java/linksawakening/render/EntityRenderLayer.java: render selected death rectangles.
- Modify the four matching test files under java/src/test/java for ROM bytes, selection wiring, framebuffer output, and countdown boundaries.
- Modify docs/reconstruction-roadmap.md: record verified scope and remaining gaps.

## Task 1: Decode the ROM death display lists

**Files:** EntitySpriteHandlerCatalog.java and EntitySpriteHandlerCatalogTest.java

- [ ] **Step 1: Write the failing ROM-byte test.**

Add this test beside the burning-display-list test:

~~~java
@Test
void decodesTheRomDeathRectangleTablesAndPowerFrame() throws Exception {
    EntitySpriteHandlerCatalog catalog = new EntitySpriteHandlerCatalog(loadRom());
    EntitySpriteDefinition normal = catalog.forDeathEntity();
    assertDefinition(normal, 0x00, 0x03, 0x5488,
        EntitySpriteDefinition.Shape.RECTANGLE, 4, 0);
    assertEquals(0x3C, normal.rectangleVariant(0).get(0).oam().tile());
    assertEquals(0x01, normal.rectangleVariant(0).get(0).oam().attributes());
    assertEquals(0xFF, normal.rectangleVariant(0).get(2).oam().tile());
    assertEquals(-6, normal.rectangleVariant(2).get(0).yOffset());
    assertEquals(-6, normal.rectangleVariant(2).get(0).xOffset());

    EntitySpriteDefinition power = catalog.forPowerRecoilDeathEntity();
    assertDefinition(power, 0x00, 0x03, 0x54C8,
        EntitySpriteDefinition.Shape.RECTANGLE, 4, 0);
    assertEquals(4, power.rectangleVariant(0).size());
    assertEquals(8, power.rectangleVariant(3).size());
    assertEquals(0x10, power.rectangleVariant(3).get(4).oam().tile());
    assertEquals(0x42, power.rectangleVariant(3).get(4).oam().attributes());
}
~~~

- [ ] **Step 2: Run the test and verify the expected missing-method failure.**

Run from java/: ./gradlew test --tests linksawakening.entity.EntitySpriteHandlerCatalogTest.
Expected: compilation fails because both named methods are absent.

- [ ] **Step 3: Implement the two ROM-backed methods.**

Add after forBurningEntity():

~~~java
public EntitySpriteDefinition forDeathEntity() {
    return decodeRectangle(0x00, 0x03, 0x5488, 4, 4, 0);
}

public EntitySpriteDefinition forPowerRecoilDeathEntity() {
    EntitySpriteDefinition source = decodeRectangle(0x00, 0x03, 0x54C8, 5, 4, 0);
    List<List<EntitySpriteDefinition.RectangleSprite>> variants =
        new ArrayList<>(source.rectangleVariants().subList(0, 3));
    List<EntitySpriteDefinition.RectangleSprite> finalFrame =
        new ArrayList<>(source.rectangleVariant(3));
    finalFrame.addAll(source.rectangleVariant(4));
    variants.add(List.copyOf(finalFrame));
    return new EntitySpriteDefinition(0x00, 0x03, 0x54C8,
        EntitySpriteDefinition.Shape.RECTANGLE, 0, List.of(), variants);
}
~~~

This must continue using decodeRectangle, RomBank.romOffset, signed offset
decoding, and the existing hidden-$FF representation; do not paste art bytes
into Java.

- [ ] **Step 4: Run, inspect, and commit.**

Run ./gradlew test --tests linksawakening.entity.EntitySpriteHandlerCatalogTest
and git diff --check. Then commit:

~~~bash
git add src/main/java/linksawakening/entity/EntitySpriteHandlerCatalog.java src/test/java/linksawakening/entity/EntitySpriteHandlerCatalogTest.java
git commit -m "feat: decode ROM entity death display lists"
~~~

## Task 2: Wire definitions into room selections

**Files:** EntitySpriteSelection.java, EntitySpriteCatalog.java, and EntitySpriteCatalogTest.java

- [ ] **Step 1: Write failing selection assertions.**

After the existing burning assertions, add:

~~~java
assertEquals(0x5488, overworld.deathSpriteDefinition().address());
assertEquals(0x54C8, overworld.powerRecoilDeathSpriteDefinition().address());
assertEquals(0x5488, selection.deathSpriteDefinition().address());
assertEquals(0x54C8, selection.powerRecoilDeathSpriteDefinition().address());
~~~

Run ./gradlew test --tests linksawakening.entity.EntitySpriteCatalogTest; it
must fail to compile because the accessors do not exist.

- [ ] **Step 2: Add immutable optional fields and builders.**

Add fields deathSpriteDefinition and powerRecoilDeathSpriteDefinition to
EntitySpriteSelection. Preserve all existing constructors by forwarding null,
null after the burning field. The private constructor validates non-null
definitions with the same supported-definition rule used for burning. Add:

~~~java
public EntitySpriteDefinition deathSpriteDefinition() {
    return deathSpriteDefinition;
}

public EntitySpriteDefinition powerRecoilDeathSpriteDefinition() {
    return powerRecoilDeathSpriteDefinition;
}

public EntitySpriteSelection withDeathSpriteDefinitions(
        EntitySpriteDefinition death, EntitySpriteDefinition powerDeath) {
    validateOptionalDeathDefinition(death, "Death");
    validateOptionalDeathDefinition(powerDeath, "Power-recoil death");
    return new EntitySpriteSelection(roomTable, roomId, groupIndex, sheetValues,
        standardSheets, objectPalettes, spriteOverrides, burningSpriteDefinition,
        death, powerDeath);
}
~~~

Update withBurningSpriteDefinition, withSpriteOverrides, and withSpriteOverride
to forward both fields unchanged.

- [ ] **Step 3: Attach definitions in both EntitySpriteCatalog.load paths.**

Create:

~~~java
EntitySpriteDefinition death = entitySpriteHandlerCatalog.forDeathEntity();
EntitySpriteDefinition powerDeath =
    entitySpriteHandlerCatalog.forPowerRecoilDeathEntity();
~~~

Append withDeathSpriteDefinitions(death, powerDeath) to both the Color Dungeon
and standard EntitySpriteSelection returns. The room sheet path remains
unchanged.

- [ ] **Step 4: Run and commit.**

Run ./gradlew test --tests linksawakening.entity.EntitySpriteCatalogTest and
git diff --check, then:

~~~bash
git add src/main/java/linksawakening/entity/EntitySpriteSelection.java src/main/java/linksawakening/entity/EntitySpriteCatalog.java src/test/java/linksawakening/entity/EntitySpriteCatalogTest.java
git commit -m "feat: attach ROM death displays to room sprite selections"
~~~

## Task 3: Add death-only snapshot metadata

**Files:** RoomEntity.java and RoomEntityRuntimeTest.java

- [ ] **Step 1: Write the failing record-boundary test.**

~~~java
@Test
void deathPresentationFieldsAreIndependentFromNormalSpriteVariant() {
    EntitySpriteDefinition body = pairDefinition(0x09, 2);
    RoomEntity entity = new RoomEntity(0, 0, 0x09, 64, 64, EntityStatus.DYING,
        body, 1, 0, 0, 0, 3, true);
    assertEquals(1, entity.spriteVariant());
    assertEquals(3, entity.deathSpriteVariant());
    assertTrue(entity.powerRecoilDeath());
}
~~~

Run ./gradlew test --tests linksawakening.world.RoomEntityRuntimeTest; the
new constructor/accessors must be absent and compilation must fail.

- [ ] **Step 2: Add the components and compatibility forwarding.**

Append int deathSpriteVariant and boolean powerRecoilDeath after z in the
record. Existing convenience constructors forward -1, false; add an explicit
compatibility constructor with the old full signature ending in (spriteTileOffset,
z) that also forwards -1, false, because runtime movement code calls that
signature directly. Disabled entities do the same. Validate that the death
variant is -1..3, and require -1 whenever the status is not DYING. Do not reuse
spriteVariant, since normal definitions can contain fewer than four variants.

- [ ] **Step 3: Run and commit the model change.**

Run ./gradlew test --tests linksawakening.world.RoomEntityRuntimeTest and
git diff --check, then:

~~~bash
git add src/main/java/linksawakening/world/RoomEntity.java src/test/java/linksawakening/world/RoomEntityRuntimeTest.java
git commit -m "feat: model dedicated entity death presentation state"
~~~

## Task 4: Render selected death rectangles

**Files:** EntityRenderLayer.java and EntityRenderLayerTest.java

- [ ] **Step 1: Write the failing framebuffer test.**

Create synthetic body, normal rectangle, and power rectangle definitions,
attach them with withDeathSpriteDefinitions(normal, power), and construct:

~~~java
new RoomEntity(0, 0, 0x09, 24, 32, EntityStatus.DYING,
    body, 0, 0, 0, 0, 3, true)
~~~

Use a distinct solid tile for the power definition and assert its palette color
at the expected $08/$10-origin-adjusted pixel. Include one OAM entry with tile
$FF and assert its destination remains the pre-filled background. The test
must fail while the layer still renders the body definition.

- [ ] **Step 2: Select death definitions before ordinary overrides.**

In renderSnapshot, use:

~~~java
EntitySpriteDefinition definition = entity.spriteDefinition();
boolean renderingDeath = false;
if (entity.status() == EntityStatus.DYING && entities.spriteSelection() != null) {
    EntitySpriteDefinition death = entity.powerRecoilDeath()
        ? entities.spriteSelection().powerRecoilDeathSpriteDefinition()
        : entities.spriteSelection().deathSpriteDefinition();
    if (death != null && entity.deathSpriteVariant() >= 0
        && entity.deathSpriteVariant() < death.variantCount()) {
        definition = death;
        renderingDeath = true;
    }
}
if (!renderingDeath && entities.spriteSelection() != null) {
    EntitySpriteDefinition override = entities.spriteSelection()
        .spriteOverrideFor(entity.type());
    if (override != null) {
        definition = override;
    }
}
int variant = renderingDeath ? entity.deathSpriteVariant() : entity.spriteVariant();
~~~

Pass variant to renderEntity. Leave the current rectangle renderer responsible
for signed offsets, XOR flips, palette selection, tile offsets, and hidden-$FF
entries. Keep the fire overlay limited to BURNING.

- [ ] **Step 3: Run and commit.**

Run ./gradlew test --tests linksawakening.render.EntityRenderLayerTest and
git diff --check, then:

~~~bash
git add src/main/java/linksawakening/render/EntityRenderLayer.java src/test/java/linksawakening/render/EntityRenderLayerTest.java
git commit -m "feat: render ROM entity death rectangles"
~~~

## Task 5: Drive the frame from the source countdown

**Files:** RoomEntityRuntime.java and RoomEntityRuntimeTest.java

- [ ] **Step 1: Write countdown-boundary tests.**

Extend the existing lethal Keese test:

~~~java
assertEquals(-1, runtime.snapshot().slots().get(0).deathSpriteVariant());
assertFalse(runtime.snapshot().slots().get(0).powerRecoilDeath());
for (int frame = 1; frame <= 0x20; frame++) {
    runtime.tick(frame, 120, 120, sequence(0x00));
}
assertEquals(-1, runtime.snapshot().slots().get(0).deathSpriteVariant());
assertEquals(0x20, runtime.dyingCountdown(0));
runtime.tick(0x21, 120, 120, sequence(0x00));
assertEquals(3, runtime.snapshot().slots().get(0).deathSpriteVariant());
assertEquals(0x1F, runtime.dyingCountdown(0));
~~~

Add a lethal power-recoil assertion for powerRecoilDeath() == true, and a
burning-expiry assertion for variant 3 with the flag false. Keep the current
zero-countdown removal test.

- [ ] **Step 2: Run and verify the new state is not yet driven.**

Run ./gradlew test --tests linksawakening.world.RoomEntityRuntimeTest; the new
assertions must fail because transitions do not publish death metadata.

- [ ] **Step 3: Implement source-derived runtime state.**

Add boolean[] powerRecoilDeath beside dyingCountdown and:

~~~java
private static int deathSpriteVariantForCountdown(int countdown) {
    if (countdown >= 0x20 || countdown <= 0) {
        return -1;
    }
    return ((countdown << 1) & 0x30) >>> 4;
}
~~~

Add a copy helper preserving all ordinary fields:

~~~java
private RoomEntity withDeathPresentation(RoomEntity entity, int variant,
                                         boolean powerRecoil) {
    return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
        entity.x(), entity.y(), entity.status(), entity.spriteDefinition(),
        entity.spriteVariant(), entity.entityFlipAttribute(), entity.spriteTileOffset(),
        entity.z(), variant, powerRecoil);
}
~~~

On lethal sword transition, store attackContext.powerRecoil() and publish
variant -1 with DYING. In the DYING branch, keep zero removal and otherwise
publish the derived variant. In finishBurning, clear the flag and publish
variant 3 when assigning countdown $1F. Clear the array in
disableEntityWithoutPersistence, clearEntity, and every slot reuse path.

- [ ] **Step 4: Run focused tests and commit.**

Run ./gradlew test --tests linksawakening.world.RoomEntityRuntimeTest,
./gradlew test --tests linksawakening.render.EntityRenderLayerTest, and
git diff --check, then:

~~~bash
git add src/main/java/linksawakening/world/RoomEntityRuntime.java src/test/java/linksawakening/world/RoomEntityRuntimeTest.java
git commit -m "feat: advance ROM entity death frames from countdown"
~~~

## Task 6: Record and verify the integrated slice

**Files:** docs/reconstruction-roadmap.md

- [ ] **Step 1: Add the dated verified entry.**

Add:

~~~markdown
## Verified ROM entity death presentation — 2026-08-06

- Common non-boss DYING entities select bank-$03 Data_003_5488 or
  Data_003_54C8 from the ROM and render source signed rectangle offsets,
  attributes, hidden entries, and the power-recoil eight-entry frame.
- The runtime derives visible variants from
  (wEntitiesPrivateCountdown3 << 1) & $30, preserves normal spriteVariant,
  starts lethal deaths at $40, and starts post-burning deaths at $1F.
- DidKillEnemy, drops, persistence, bosses, and entity-specific death
  handlers remain explicit follow-up work; no CPU/emulator was added.
~~~

Update the next-increment list so drops/persistence and entity-specific death
handlers remain visible as the next common-death work.

- [ ] **Step 2: Run full verification.**

From java/, run ./gradlew clean test; expected output is BUILD SUCCESSFUL with
all tests passing.

- [ ] **Step 3: Check hygiene and commit the roadmap.**

From the worktree root run git diff --check, git status --short --branch, and
git log --oneline -8. Confirm no unrelated root-worktree files changed, then:

~~~bash
git add docs/reconstruction-roadmap.md
git commit -m "docs: record ROM entity death presentation parity"
git status --short --branch
~~~

Expected result: branch feature/entity-runtime, a clean worktree, and commits
covering decoder, selection, model, renderer, runtime, and roadmap.

## Plan self-review

- Spec coverage: ROM normal/power decoding is Task 1; selection wiring Task 2;
  separate death metadata Task 3; rendering Task 4; countdown/burning Task 5;
  roadmap and full verification Task 6.
- Placeholder scan: every implementation step names a file, a command, and the
  expected behavior; deferred behavior is named concretely as scope.
- Type consistency: deathSpriteVariant, powerRecoilDeath, and
  withDeathSpriteDefinitions are introduced before their later uses.
