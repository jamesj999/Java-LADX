# ROM Bank-$06 Enemy Recoil Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port the source bank-$06 sword-recoil response to the four already modeled normal enemy families without changing their existing movement state machines.

**Architecture:** Reuse `EnemyRecoilMotion` for the exact ROM vector and fixed-point accumulators. `RoomEntityRuntime` owns an explicit bank-$06 type predicate, configures recoil in the shared combat branch, and applies one bank-$06 step before the existing family motion; Octorok/Moblin retain bank-$03 collision-stop behavior and spike traps retain their clink-only path.

**Tech Stack:** Java 17, JUnit 5, Gradle, shipped ROM bytes, and LADX disassembly source.

---

### Task 1: Add the failing bank-$06 combat regression

**Files:**
- Test: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`

- [ ] **Step 1: Write the failing test**

Add these tests beside the existing Hard Hat combat tests:

```java
@Test
void bankSixNormalEnemiesConfigureTheRomSharedSwordRecoil() {
    int[] types = {0x19, 0x0D, 0x15, 0x1A};
    for (int type : types) {
        EntitySpriteDefinition definition = pairDefinition(type, 3);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, type, 64, 64, EntityStatus.ACTIVE, definition, 0)));

        List<EntityCombatEvent> events = runtime.resolveCombat(
            0, 72, 72, false, true, true, 72, 1, 72, 1);

        assertEquals(1, events.size(), "type=" + Integer.toHexString(type));
        assertEquals(0x18, runtime.enemyFlashCountdown(0),
            "type=" + Integer.toHexString(type));
        assertEquals(0x0A, runtime.enemyIgnoreHitsCountdown(0),
            "type=" + Integer.toHexString(type));
        assertTrue(runtime.enemyRecoilActive(0),
            "type=" + Integer.toHexString(type));
        assertEquals(0xD0, runtime.enemyRecoilSpeedX(0),
            "type=" + Integer.toHexString(type));
        assertEquals(0xD0, runtime.enemyRecoilSpeedY(0),
            "type=" + Integer.toHexString(type));
    }
}

@Test
void tektiteAppliesBankSixRecoilBeforeItsOrdinaryMotion() {
    EntitySpriteDefinition definition = pairDefinition(0x0D, 2);
    RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
        new RoomEntity(0, 0, 0x0D, 64, 64, EntityStatus.ACTIVE, definition, 0)));

    runtime.resolveCombat(0, 72, 72, false, true, true, 72, 1, 72, 1);
    runtime.tick(1, 72, 72, sequence(0x00));

    assertEquals(61, runtime.snapshot().slots().get(0).x());
    assertEquals(61, runtime.snapshot().slots().get(0).y());
    assertEquals(0x09, runtime.enemyIgnoreHitsCountdown(0));
}
```

- [ ] **Step 2: Run the focused tests to verify RED**

Run from `java/`:

```bash
gradle test --tests linksawakening.world.RoomEntityRuntimeTest.bankSixNormalEnemiesConfigureTheRomSharedSwordRecoil --tests linksawakening.world.RoomEntityRuntimeTest.tektiteAppliesBankSixRecoilBeforeItsOrdinaryMotion
```

Expected result: both tests fail because `RoomEntityRuntime` currently configures recoil only for Octorok, Moblin, and Hard Hat.

### Task 2: Add the explicit source-backed type boundary

**Files:**
- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java:890-900`

- [ ] **Step 1: Replace the combined recoil predicate**

Replace the existing `usesSharedRecoil` method next to
`isRoamingEnemyType` with these two methods:

```java
private static boolean usesBank6Recoil(int type) {
    return type == ENTITY_KEESE || type == ENTITY_TEKTITE
        || type == ENTITY_ANTI_FAIRY || type == ENTITY_STALFOS_AGGRESSIVE
        || type == ENTITY_HARDHAT_BEETLE;
}

private static boolean usesSharedRecoil(int type) {
    return isRoamingEnemyType(type) || usesBank6Recoil(type);
}
```

- [ ] **Step 2: Route the normal sword branch through the predicate**

Change the existing `if (usesSharedRecoil(entity.type()))` check in
`resolveCombat` to use the new combined predicate. Leave the earlier spike
trap branch before it, so spike traps still clear recoil without configuring
it.

### Task 3: Preserve bank-specific recoil application

**Files:**
- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java:1630-1655`
- Modify: `java/src/main/java/linksawakening/world/EnemyRecoilMotion.java:58-100`

- [ ] **Step 1: Select the bank-$06 collision-stop mode**

Pass `isRoamingEnemyType(entity.type())` as `clearOnBlocked` to
`EnemyRecoilMotion.advance`. This keeps `true` for bank-$03 Octorok/Moblin
and `false` for all bank-$06 families. Only the bank-$03 branch sets the
ignore countdown to zero after a blocked step.

- [ ] **Step 2: Keep the roaming state hook narrow**

Guard `roamingEnemyMotion.beginRecoil(slot)` with
`isRoamingEnemyType(entity.type())`. Keese, Tektite, Anti-Fairy, Stalfos, and
Hard Hat must enter their own existing motion classes after the recoil step.

### Task 4: Run focused and full verification

**Files:**
- Modify: `docs/reconstruction-roadmap.md`

- [ ] **Step 1: Run the focused regression suite**

```bash
gradle test --tests linksawakening.world.RoomEntityRuntimeTest --tests linksawakening.world.EnemyRecoilMotionTest
```

Expected result: all selected tests pass, including the prior Hard Hat,
Octorok, Moblin, and spike-trap cases.

- [ ] **Step 2: Run the clean full suite and diff check**

```bash
git diff --check
gradle clean test
```

Expected result: `BUILD SUCCESSFUL` and no whitespace errors.

- [ ] **Step 3: Record the verified increment**

Add a `Verified ROM bank-$06 enemy recoil` section to
`docs/reconstruction-roadmap.md`, naming the four source handlers, the
pre-motion ordering, the bank-$06 blocked-recoil distinction, and the explicit
Spark/Zol exclusions.

- [ ] **Step 4: Commit the increment**

```bash
git add java/src/main/java/linksawakening/world/EnemyRecoilMotion.java \
  java/src/main/java/linksawakening/world/RoomEntityRuntime.java \
  java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java \
  docs/reconstruction-roadmap.md
git commit -m "feat: extend ROM bank-06 enemy recoil"
```
