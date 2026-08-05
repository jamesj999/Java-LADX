# PeaHat background-wall rollback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore PeaHat's blocked X/Y coordinates after its bank-$07
fixed-point movement update.

**Architecture:** Keep `PeaHatMotion`'s state machine and speed tables
unchanged. Add the existing directional background callback immediately after
the X/Y/Z update and before state dispatch, restoring X then Y when the
ROM-shaped wall query reports a block.

**Tech Stack:** Java 21, Gradle, JUnit 5, LADX disassembly.

---

### Task 1: Add the failing wall-rollback regression

**Files:**

- Modify: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`
  beside the existing PeaHat tests.

- [ ] **Step 1: Add the flying-state blocked/control test.** Advance identical
  runtimes through frame `280`, where the existing ROM phase-table regression
  confirms PeaHat is flying with positive X speed. Then run frames `281..304`
  with a right-wall callback on one runtime and no callback on the control:

```java
@Test
void peaHatRestoresItsPositionWhenTheRomBackgroundHelperBlocksRightwardMotion() {
    EntitySpriteDefinition definition = pairDefinition(0xA0, 2);
    RoomEntitySnapshot initial = snapshot(
        new RoomEntity(0, 0, 0xA0, 64, 64, EntityStatus.ACTIVE, definition, 0));
    RoomEntityRuntime blocked = RoomEntityRuntime.from(initial);
    RoomEntityRuntime unblocked = RoomEntityRuntime.from(initial);
    IntSupplier blockedRandom = sequence(0x00);
    IntSupplier unblockedRandom = sequence(0x00);

    for (int frame = 0; frame <= 280; frame++) {
        blocked.tick(frame, 120, 120, blockedRandom);
        unblocked.tick(frame, 120, 120, unblockedRandom);
    }
    RoomEntity phaseStart = blocked.snapshot().slots().get(0);
    assertEquals(2, blocked.peaHatState(0));
    assertEquals(0x07, blocked.peaHatSpeedX(0));

    RoomEntityBackgroundCollision rightWall =
        (entity, direction, nextX, nextY) -> direction == 0;
    for (int frame = 281; frame <= 304; frame++) {
        blocked.tick(frame, 120, 120, blockedRandom, rightWall);
        unblocked.tick(frame, 120, 120, unblockedRandom);
    }

    RoomEntity blockedEntity = blocked.snapshot().slots().get(0);
    RoomEntity unblockedEntity = unblocked.snapshot().slots().get(0);
    assertEquals(phaseStart.x(), blockedEntity.x());
    assertTrue(unblockedEntity.x() > blockedEntity.x());
    assertEquals(unblockedEntity.y(), blockedEntity.y());
    assertEquals(unblocked.peaHatState(0), blocked.peaHatState(0));
    assertEquals(unblocked.peaHatSpeedX(0), blocked.peaHatSpeedX(0));
    assertEquals(unblocked.peaHatSpeedY(0), blocked.peaHatSpeedY(0));
}
```

- [ ] **Step 2: Run focused tests and verify RED.** From `java/`, run:

```bash
gradle test --tests linksawakening.world.RoomEntityRuntimeTest
```

Expected: the new test fails because `PeaHatMotion` currently ignores the
background callback; the existing PeaHat state and recoil tests continue to
pass.

### Task 2: Implement the ROM wall rollback

**Files:**

- Modify: `java/src/main/java/linksawakening/world/PeaHatMotion.java` after
  its fixed-point X/Y calculation and before the state switch.

- [ ] **Step 1: Query and restore the two axes in source order.** Add:

```java
if (backgroundCollision != null) {
    if (x != entity.x() && backgroundCollision.blocks(entity,
            directionForX(speedX[slot]), x, y)) {
        x = entity.x();
    }
    if (y != entity.y() && backgroundCollision.blocks(entity,
            directionForY(speedY[slot]), x, y)) {
        y = entity.y();
    }
}
```

Add ROM direction helpers matching the callback contract:

```java
private static int directionForX(int speed) {
    return (speed & 0x80) != 0 ? 1 : 0;
}

private static int directionForY(int speed) {
    return (speed & 0x80) != 0 ? 2 : 3;
}
```

Do not reverse speeds, reset countdowns, or change PeaHat state on a block.

- [ ] **Step 2: Run focused tests and verify GREEN.** Run:

```bash
gradle test --tests linksawakening.world.RoomEntityRuntimeTest
```

Expected: the blocked/control regression and all existing runtime tests pass.

### Task 3: Document and checkpoint the increment

**Files:**

- Modify: `docs/reconstruction-roadmap.md` in the PeaHat status bullet and
  before `## Next entity increments`.
- Modify: this plan to mark completed steps.

- [ ] **Step 1: Update the PeaHat roadmap status.** State that wall rollback
  through the shared bank-$07 background path is verified below while generic
  ground/water/pit/conveyor interaction remains pending.

- [ ] **Step 2: Add a dated verification entry.** Insert:

```markdown
## Verified ROM PeaHat wall rollback — 2026-08-05

- PeaHat's bank-$07 post-movement path now queries the room background in
  right/left/up/down order and restores blocked X/Y coordinates without
  reversing ordinary speed or changing its rest/takeoff/flying state.
- The flying-state blocked/control regression and the complete Java suite
  cover this increment. Ground status, water/pit/conveyor effects, collision
  flags, sword-clink behavior, and remaining damage-state branches remain
  pending.
```

- [ ] **Step 3: Run final verification.** From `java/`, run `gradle clean test`.
  From the worktree root, run `git diff --check` and
  `git status --short --branch`. Expected: `BUILD SUCCESSFUL`, no whitespace
  errors, and only the intended source, test, roadmap, and plan files changed
  before commit.

- [ ] **Step 4: Commit the checkpoint.** Mark all plan checkboxes complete and
  commit:

```bash
git add java/src/main/java/linksawakening/world/PeaHatMotion.java \
  java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java \
  docs/reconstruction-roadmap.md \
  docs/superpowers/plans/2026-08-05-peahat-background.md \
  docs/superpowers/specs/2026-08-05-peahat-background-design.md
git commit -m "feat: add PeaHat wall rollback"
```
