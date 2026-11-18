# Bush Destroy Animation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the ROM-equivalent bush destroy cut-leaves animation through a reusable transient-VFX runtime.

**Architecture:** Introduce a small fixed-slot transient-VFX system in the gameplay runtime, then route bush cutting through that system. Render the first effect type, bush cut-leaves, from ROM-backed sprite rect data and countdown-driven frame selection matching the liftable-rock smash path.

**Tech Stack:** Java 21, LWJGL renderer, JUnit 5

---

### File Map

- Create: `java/src/main/java/linksawakening/vfx/TransientVfxSystem.java`
- Create: `java/src/main/java/linksawakening/vfx/TransientVfxType.java`
- Create: `java/src/main/java/linksawakening/vfx/TransientVfxSpriteSheet.java`
- Create: `java/src/main/java/linksawakening/vfx/CutLeavesEffectRenderer.java`
- Create: `java/src/test/java/linksawakening/vfx/TransientVfxSystemTest.java`
- Modify: `java/src/main/java/linksawakening/Main.java`
- Modify: `java/src/test/java/linksawakening/world/OverworldBushInteractionTest.java`

### Task 1: Write Transient VFX Lifecycle Tests

**Files:**
- Create: `java/src/test/java/linksawakening/vfx/TransientVfxSystemTest.java`

- [ ] **Step 1: Write the failing tests**

Add tests that cover:
- spawning one effect populates a slot with type, countdown, and position
- ticking decrements countdown and expires the slot at zero
- after expiry, the slot becomes reusable
- bush-leaves frame selection maps countdown bands to ROM-equivalent frame indices

- [ ] **Step 2: Run test to verify it fails**

Run: `gradle test --tests linksawakening.vfx.TransientVfxSystemTest`
Expected: FAIL because the transient VFX classes do not exist yet.

- [ ] **Step 3: Write minimal implementation**

Create the transient VFX classes with just enough lifecycle and frame-selection logic to satisfy the tests.

- [ ] **Step 4: Run test to verify it passes**

Run: `gradle test --tests linksawakening.vfx.TransientVfxSystemTest`
Expected: PASS

### Task 2: Add Bush Integration Test

**Files:**
- Modify: `java/src/test/java/linksawakening/world/OverworldBushInteractionTest.java`
- Test: `java/src/test/java/linksawakening/world/OverworldBushInteractionTest.java`

- [ ] **Step 1: Write the failing test**

Add a test that cuts a bush, then asserts the transient VFX system receives exactly one `BUSH_LEAVES` spawn at the expected world position or room-cell-derived anchor.

- [ ] **Step 2: Run test to verify it fails**

Run: `gradle test --tests linksawakening.world.OverworldBushInteractionTest`
Expected: FAIL because the bush path does not yet enqueue the effect.

- [ ] **Step 3: Write minimal implementation**

Thread the transient VFX spawn through the bush-cut integration point in `Main.java`.

- [ ] **Step 4: Run test to verify it passes**

Run: `gradle test --tests linksawakening.world.OverworldBushInteractionTest`
Expected: PASS

### Task 3: Render Bush Leaves From ROM Data

**Files:**
- Create: `java/src/main/java/linksawakening/vfx/TransientVfxSpriteSheet.java`
- Create: `java/src/main/java/linksawakening/vfx/CutLeavesEffectRenderer.java`
- Modify: `java/src/main/java/linksawakening/Main.java`
- Test: `java/src/test/java/linksawakening/vfx/TransientVfxSystemTest.java`

- [ ] **Step 1: Write the failing render-oriented test**

Add a test that verifies a representative bush-leaves frame uses the expected ROM tile ID / rect offsets / flip flags mapping for a chosen countdown value.

- [ ] **Step 2: Run test to verify it fails**

Run: `gradle test --tests linksawakening.vfx.TransientVfxSystemTest`
Expected: FAIL because no ROM-backed leaves renderer exists yet.

- [ ] **Step 3: Write minimal implementation**

Decode the needed OAM tiles into a dedicated sprite sheet and render the cut-leaves frames in the overworld sprite pass.

- [ ] **Step 4: Run test to verify it passes**

Run: `gradle test --tests linksawakening.vfx.TransientVfxSystemTest`
Expected: PASS

### Task 4: Final Verification

**Files:**
- Modify: `java/src/main/java/linksawakening/Main.java`
- Modify: `java/src/main/java/linksawakening/vfx/*.java`
- Test: `java/src/test/java/linksawakening/vfx/TransientVfxSystemTest.java`
- Test: `java/src/test/java/linksawakening/world/OverworldBushInteractionTest.java`
- Test: `java/src/test/java/linksawakening/entity/LinkTest.java`
- Test: `java/src/test/java/linksawakening/equipment/SwordTest.java`

- [ ] **Step 1: Run focused tests**

Run:
- `gradle test --tests linksawakening.vfx.TransientVfxSystemTest`
- `gradle test --tests linksawakening.world.OverworldBushInteractionTest`
- `gradle test --tests linksawakening.entity.LinkTest`
- `gradle test --tests linksawakening.equipment.SwordTest`

Expected: PASS

- [ ] **Step 2: Run full suite**

Run: `gradle test`
Expected: PASS
