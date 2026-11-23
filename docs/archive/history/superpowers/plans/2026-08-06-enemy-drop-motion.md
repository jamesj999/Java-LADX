# Common Enemy-Drop Motion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reproduce the ROM common enemy-drop fixed-point motion, gravity, terrain status, and landing bounce in the live Java room runtime.

**Architecture:** A focused `EnemyDropMotion` owns per-slot speed and fractional state. `RoomEntityRuntime` marks common drops explicitly, advances their motion before the existing ROM-backed ground callback, and applies the side-scroll collision/top-down Z bounce afterward using the existing background-collision boundary.

**Tech Stack:** Java, JUnit 5, Gradle, shipped LADX ROM/disassembly.

---

### Task 1: Add the ROM-shaped motion test seam

**Files:**
- Create: `java/src/test/java/linksawakening/world/EnemyDropMotionTest.java`
- Modify: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`

- [ ] **Step 1: Write the failing pure-motion tests**

Create a package-local test entity with a supported pair definition and cover:

```java
@Test
void topDownLaunchUsesFixedPointZAndRomGravity() {
    EnemyDropMotion motion = new EnemyDropMotion();
    motion.initialize(0, false);
    RoomEntity entity = entity(0, 0x2E, 0x40, 0x50, 0);

    entity = motion.advance(entity, 0, 0, false);

    assertEquals(0x01, entity.z());
    assertEquals(0x16, motion.speedZ(0));
}

@Test
void topDownLandingStopsInShallowWaterAndSideScrollBouncesOnDownCollision() {
    EnemyDropMotion motion = new EnemyDropMotion();
    motion.initialize(0, false);
    RoomEntity falling = entity(0, 0x2E, 0x40, 0x50, 0x80);
    falling = motion.bounce(falling, 0x02, false, false);
    assertEquals(0, falling.z());
    assertEquals(0, motion.speedZ(0));

    motion.initialize(0, true);
    RoomEntity side = entity(0, 0x2E, 0x40, 0x63, 0);
    for (int frame = 0; frame < 18; frame++) {
        side = motion.advance(side, frame, 0, true);
    }
    assertEquals(0x10, motion.speedY(0));
    int landingY = side.y();
    side = motion.bounce(side, 0x00, true, true);
    assertEquals((landingY & 0xF0) + 0x05, side.y());
    assertEquals(0xF7, motion.speedY(0));
}
```

The helper entity factory should use `EntitySpriteDefinition.unsupported` only
if the motion constructor does not inspect sprites; otherwise use the same
two-OAM pair definition already used by `ThrownEntityMotionTest`. Keep all
coordinates and expected values unsigned bytes.

- [ ] **Step 2: Run the focused test and verify the intended red failure**

Run:

```text
gradle -p java test --tests linksawakening.world.EnemyDropMotionTest --no-build-cache --rerun-tasks
```

Expected: compilation/test failure because `EnemyDropMotion` does not yet
exist. Fix only test syntax or factory errors if the failure is unrelated.

- [ ] **Step 3: Add the runtime integration assertions before implementation**

In the existing top-down death-drop test, after `runtime.tick(1, ...)`, assert
that the spawned drop has advanced from the source Z by one pixel and that its
speed changed from `$18` to `$16`. Add a side-scroll test that passes a
down-direction `RoomEntityBackgroundCollision` callback returning `true` for
the drop and asserts that a later active tick aligns Y to `(Y & 0xF0) + 5` and
stops or reverses according to the ROM threshold. Do not alter the existing
pickup-delay assertions.

- [ ] **Step 4: Run the runtime test and verify it fails for missing motion**

Run:

```text
gradle -p java test --tests linksawakening.world.RoomEntityRuntimeTest --no-build-cache --rerun-tasks
```

Expected: the new movement assertions fail while the pre-existing drop spawn
and pickup assertions remain green. This proves the tests observe the missing
behavior rather than merely exercising setup.

### Task 2: Implement the focused motion handler

**Files:**
- Create: `java/src/main/java/linksawakening/world/EnemyDropMotion.java`
- Test: `java/src/test/java/linksawakening/world/EnemyDropMotionTest.java`

- [ ] **Step 1: Implement launch state and accessors**

Add per-slot arrays for speed Y, speed Z, and their eight-bit accumulators.
`initialize(slot, sideScrolling)` must clear both accumulators and set exactly:

```java
speedY[slot] = sideScrolling ? 0xEC : 0;
speedZ[slot] = sideScrolling ? 0 : 0x18;
```

Provide `speedY`, `speedZ`, and `clear` methods with slot validation.

- [ ] **Step 2: Implement ROM fixed-point movement and gravity**

Use the bank-$03 `AddEntitySpeedToPos_03` arithmetic for each active axis:

```java
int fractionalSum = accumulator[slot] + ((speed & 0xFF) << 4 & 0xF0);
accumulator[slot] = fractionalSum & 0xFF;
int delta = signedByte(speed) >> 4;
if (fractionalSum > 0xFF) {
    delta++;
}
return (position + delta) & 0xFF;
```

For top-down mode, update Z first and subtract `$02` from speed Z. For
side-scroll mode, update Y first, add the previous-ground-status acceleration
from `{2,1,2,2}`, and clamp against `{0x40,0x08,0x40,0x40}` when
`(speed - cap) & 0x80` is clear. Return an immutable entity with only the
changed coordinate.

- [ ] **Step 3: Implement the two ROM bounce branches**

For top-down mode, only process a negative Z byte. Set Z to zero, stop all
drop speeds for shallow-water status `$02`, otherwise compute
`cpl(sra(speedZ))`; retain it as the new speed Z only when the unsigned result
is at least `$07`, otherwise stop. For side-scroll mode, require the supplied
down-collision boolean, align Y to `(Y & 0xF0) + 5`, compute
`cpl(sra(speedY))`, retain it only when the unsigned result is below `$F8`,
otherwise stop speed Y. Keep the helper free of sound/VFX side effects.

- [ ] **Step 4: Run the focused motion tests green**

Run the `EnemyDropMotionTest` command from Task 1. Expected: all motion tests
pass, with no production changes outside the new handler.

- [ ] **Step 5: Commit the isolated motion helper**

```text
git add java/src/main/java/linksawakening/world/EnemyDropMotion.java java/src/test/java/linksawakening/world/EnemyDropMotionTest.java
git commit -m "feat: add ROM common enemy drop motion"
```

### Task 3: Wire motion into the runtime and live room collision boundary

**Files:**
- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
- Modify: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`

- [ ] **Step 1: Add explicit common-drop ownership state**

Add `boolean[] enemyDropActive` and one `EnemyDropMotion` field. Set the flag
and initialize motion only in `spawnEnemyDrop`; clear it in both
`resetEnemyDropState`/slot-disable paths. Keep dynamic projectiles and clones
out of the drop path even though they also use source load order `$FF`.

- [ ] **Step 2: Advance the drop before ground interaction**

In `tickInternal`, after the active entity-family motion dispatches and before
the existing `groundInteraction.apply` call, add:

```java
if (status == EntityStatus.ACTIVE && !wasInitializing
    && enemyDropActive[updated.slot()]) {
    updated = enemyDropMotion.advance(updated, frame,
        entityGroundStatus[updated.slot()], groundInteractionSideScrolling);
}
```

This preserves the source order: movement/gravity, terrain sample, then bounce.

- [ ] **Step 3: Pass the drop speed Z through the existing ground boundary**

Extend `verticalSpeedZ(RoomEntity)` with the explicit drop case:

```java
if (enemyDropActive[slot]) {
    return enemyDropMotion.speedZ(slot);
}
```

Do not change the callback signature or the behavior of unrelated entity
families.

- [ ] **Step 4: Apply side-scroll landing bounce after terrain sampling**

After a non-unloaded ground result, compute a down-direction collision only
for a side-scroll common drop whose current speed Y is positive:

```java
boolean dropGroundCollision = groundInteractionSideScrolling
    && enemyDropActive[updated.slot()]
    && enemyDropMotion.speedY(updated.slot()) != 0
    && (enemyDropMotion.speedY(updated.slot()) & 0x80) == 0
    && backgroundCollision != null
    && backgroundCollision.blocks(updated, EntityBackgroundCollisionResult.DOWN,
        updated.x(), updated.y());
if (enemyDropActive[updated.slot()]) {
    updated = enemyDropMotion.bounce(updated, groundResult.groundStatus(),
        groundInteractionSideScrolling, dropGroundCollision);
}
```

Keep the current unload/pit/VFX handling before the bounce call, matching the
source's early exit for entities unloaded by ground interaction. Top-down
landing uses the Z sign bit and therefore does not need a background probe.

- [ ] **Step 5: Run focused runtime tests green**

Run:

```text
gradle -p java test --tests linksawakening.world.EnemyDropMotionTest --tests linksawakening.world.RoomEntityRuntimeTest --no-build-cache --rerun-tasks
```

Expected: the new motion/bounce assertions and all existing runtime tests pass.

- [ ] **Step 6: Commit the runtime integration**

```text
git add java/src/main/java/linksawakening/world/RoomEntityRuntime.java java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java
git commit -m "feat: animate common enemy drops in room runtime"
```

### Task 4: Record the visible parity increment and verify the branch

**Files:**
- Modify: `docs/reconstruction-roadmap.md`

- [ ] **Step 1: Update the roadmap**

Add a dated entry stating that common ROM-selected enemy drops now execute
fixed-point top-down Z flight, side-scroll Y gravity, ground-status clamping,
and landing alignment/bounce through the live room session. Keep handler-
specific drops, bounce audio, and complete entity collision tables explicitly
pending.

- [ ] **Step 2: Run the full verification commands**

```text
gradle -p java test --no-build-cache --rerun-tasks
git diff --check
git status --short --branch
```

Expected: Gradle `BUILD SUCCESSFUL`, no diff-check output, and only the
intentional commits/docs in the feature worktree.

- [ ] **Step 3: Commit the roadmap entry**

```text
git add docs/reconstruction-roadmap.md
git commit -m "docs: record common enemy drop motion"
```

## Self-review checklist

- Spec coverage: Tasks 1–3 cover fixed-point launch, both gravity branches,
  ground-status use, side-scroll collision alignment, top-down landing, and
  explicit drop ownership; Task 4 records and verifies the result.
- Placeholder scan: no task depends on a TODO, guessed table, or undefined
  helper; every production method named in the plan is either new in Task 2 or
  existing in the current runtime.
- Type consistency: `EnemyDropMotion.advance` and `.bounce` both return
  `RoomEntity`; speed accessors accept a slot; the runtime uses the same
  `enemyDropMotion` field and `enemyDropActive` flag in initialization,
  ticking, ground-speed forwarding, and cleanup.
