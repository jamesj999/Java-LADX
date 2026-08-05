# Spark bank-$06 recoil Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax to track progress.

**Goal:** Make Spark entities `$16` and `$17` reach the existing ROM bank-$06 sword-recoil path.

**Architecture:** Extend only `RoomEntityRuntime.usesBank6Recoil`. The shared
`EnemyRecoilMotion`, combat event ordering, Spark movement, and non-roaming
background-block policy already model the required behavior and remain the
single source of runtime recoil state.

**Tech Stack:** Java 21, JUnit 5, Gradle, LADX assembly source.

---

### Task 1: Add the failing Spark recoil regression

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`
- Reference: `LADX-Disassembly/src/code/entities/06_spark.asm:19-33`
- Reference: `LADX-Disassembly/src/code/entities/bank6.asm:211-261`

- [x] **Step 1: Extend the bank-$06 type-boundary test**

Change the existing `bankSixNormalEnemiesConfigureTheRomSharedSwordRecoil`
fixture's type array from:

```java
int[] types = {0x19, 0x0D, 0x15, 0x1A};
```

to:

```java
int[] types = {0x19, 0x0D, 0x15, 0x1A, 0x16, 0x17};
```

Keep its existing assertions for one combat event, `$0A` ignore hits,
active recoil, and `$D0/$D0` vector components. This fixture uses the same
diagonal entity/Link positions for both Spark types and therefore checks the
shared ROM vector without changing Spark's movement tests.

- [x] **Step 2: Run the focused test and verify RED**

Run from `java/`:

```bash
gradle test --tests linksawakening.world.RoomEntityRuntimeTest
```

Expected result: the existing four bank-$06 cases pass, then the first Spark
case fails its `enemyRecoilActive(0)` assertion because the current type
predicate excludes `$16` and `$17`.

### Task 2: Add Spark to the existing bank-$06 recoil boundary

**Files:**
- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java:1137-1141`
- Test: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`

- [x] **Step 1: Extend the source-backed type predicate**

Update `usesBank6Recoil` from:

```java
private static boolean usesBank6Recoil(int type) {
    return type == ENTITY_KEESE || type == ENTITY_TEKTITE
        || type == ENTITY_ANTI_FAIRY || type == ENTITY_STALFOS_AGGRESSIVE
        || type == ENTITY_HARDHAT_BEETLE || type == ENTITY_ARMOS_STATUE;
}
```

to:

```java
private static boolean usesBank6Recoil(int type) {
    return type == ENTITY_KEESE || type == ENTITY_TEKTITE
        || type == ENTITY_ANTI_FAIRY || type == ENTITY_STALFOS_AGGRESSIVE
        || type == ENTITY_HARDHAT_BEETLE || type == ENTITY_ARMOS_STATUE
        || type == ENTITY_SPARK_COUNTER_CLOCKWISE
        || type == ENTITY_SPARK_CLOCKWISE;
}
```

Do not add a Spark-specific recoil helper, modify `SparkMotion`, or change the
roaming-enemy collision-stop branch. Spark must use the existing non-roaming
`false` argument in `applyEnemyRecoilIfNeeded`, preserving bank-$06 behavior.

- [x] **Step 2: Run the focused test and verify GREEN**

Run:

```bash
gradle test --tests linksawakening.world.RoomEntityRuntimeTest
```

Expected result: the complete `RoomEntityRuntimeTest` class passes, including
the six-type shared-recoil regression and all existing Spark movement,
collision, and combat tests.

### Task 3: Verify and record the parity increment

**Files:**
- Modify: `docs/reconstruction-roadmap.md`
- Review: `docs/superpowers/specs/2026-08-05-spark-recoil-design.md`

- [x] **Step 1: Update the roadmap boundary**

Change the existing Spark bullet so it says bank-$06 recoil is covered, while
`hActiveEntityNoBGCollision`, recoil smoke, and remaining damage-state work
remain deferred. Add a dated verified increment immediately before `Next
entity increments` stating that `$16/$17` now configure the shared `$30`
vector, consume `$0A` ignore-hit frames, run before Spark movement, and retain
the bank-$06 no-stop-on-block policy.

- [x] **Step 2: Run clean verification and repository checks**

Run from `java/`:

```bash
gradle clean test
```

Then run from the worktree root:

```bash
git diff --check
git status --short
```

Expected result: `BUILD SUCCESSFUL`, no whitespace errors, and only the plan,
roadmap, runtime, and test files changed before the commit.

- [x] **Step 3: Commit the implementation checkpoint**

```bash
git add docs/reconstruction-roadmap.md \
  java/src/main/java/linksawakening/world/RoomEntityRuntime.java \
  java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java
git commit -m "feat: add Spark bank6 recoil"
```
