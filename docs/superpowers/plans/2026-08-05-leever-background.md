# Leever background-wall rollback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore Leever's blocked X/Y coordinates after its bank-$04 fixed-point movement update.

**Architecture:** Keep the Leever state machine and speed tables unchanged. Add the existing directional background callback to `LeeverMotion`'s post-movement phase, restoring X and then Y when the ROM-shaped wall query reports a block; preserve speeds, countdowns, and state.

**Tech Stack:** Java 21, Gradle, JUnit 5, LADX disassembly.

---

### Task 1: Add failing wall-rollback tests

**Files:**

- Modify: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java` beside the existing Leever tests.

- [ ] **Step 1: Add the blocked-wall regression.** Add:

```java
@Test
void leeverRestoresItsPositionWhenTheRomBackgroundHelperBlocksRightwardMotion() {
    EntitySpriteDefinition definition = pairDefinition(0x0E, 4);
    RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
        new RoomEntity(0, 0, 0x0E, 64, 64, EntityStatus.ACTIVE, definition, 0)));

    for (int frame = 0; frame <= 31; frame++) {
        runtime.tick(frame, 120, 120, sequence(0x00));
    }
    runtime.tick(32, 80, 64, sequence(0x00));
    assertEquals(2, runtime.leeverState(0));
    assertEquals(0x08, runtime.leeverSpeedX(0));

    RoomEntityBackgroundCollision rightWall =
        (entity, direction, nextX, nextY) -> direction == 0;
    runtime.tick(33, 80, 64, sequence(0x00), rightWall);
    runtime.tick(34, 80, 64, sequence(0x00), rightWall);

    assertEquals(64, runtime.snapshot().slots().get(0).x());
    assertEquals(0x08, runtime.leeverSpeedX(0));
    assertEquals(2, runtime.leeverState(0));
}
```

- [ ] **Step 2: Add the unblocked control.** Add:

```java
@Test
void leeverAdvancesItsPositionWhenTheBackgroundDoesNotBlock() {
    EntitySpriteDefinition definition = pairDefinition(0x0E, 4);
    RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
        new RoomEntity(0, 0, 0x0E, 64, 64, EntityStatus.ACTIVE, definition, 0)));

    for (int frame = 0; frame <= 31; frame++) {
        runtime.tick(frame, 120, 120, sequence(0x00));
    }
    runtime.tick(32, 80, 64, sequence(0x00));
    runtime.tick(33, 80, 64, sequence(0x00));
    runtime.tick(34, 80, 64, sequence(0x00));

    assertEquals(65, runtime.snapshot().slots().get(0).x());
    assertEquals(0x08, runtime.leeverSpeedX(0));
}
```

- [ ] **Step 3: Run focused tests and verify RED.** From `java/`, run:

```bash
gradle test --tests linksawakening.world.RoomEntityRuntimeTest
```

Expected: the blocked test fails at X `65`, while the unblocked control and
all existing runtime tests pass because `LeeverMotion` currently ignores its
background callback.

### Task 2: Implement the ROM wall rollback

**Files:**

- Modify: `java/src/main/java/linksawakening/world/LeeverMotion.java` immediately after its fixed-point X/Y update and near its numeric helpers.

- [ ] **Step 1: Query and restore the two axes in source order.** Add after
the `x` and `y` calculations:

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

Use the post-X coordinate in the Y query. Do not change `speedX`, `speedY`,
`state`, or `transitionCountdown` in either branch.

- [ ] **Step 2: Add direction helpers.** Add:

```java
private static int directionForX(int speed) {
    return signedByte(speed) < 0 ? 1 : 0;
}

private static int directionForY(int speed) {
    return signedByte(speed) < 0 ? 2 : 3;
}
```

Reuse the existing `signedByte` helper.

- [ ] **Step 3: Run focused tests and verify GREEN.** Run:

```bash
gradle test --tests linksawakening.world.RoomEntityRuntimeTest
```

Expected: both new wall tests and all existing Leever/runtime tests pass.

### Task 3: Document and checkpoint the increment

**Files:**

- Modify: `docs/reconstruction-roadmap.md` in the Leever status bullet and before `## Next entity increments`.
- Modify: `docs/superpowers/plans/2026-08-05-leever-background.md` to mark completed steps.

- [ ] **Step 1: Update the Leever status.** State that shared bank-$04 wall
rollback is verified below, while ground/water/pit/conveyor behavior and
remaining damage states remain pending.

- [ ] **Step 2: Add a dated verification entry.** Insert:

```markdown
## Verified ROM Leever wall rollback — 2026-08-05

- Leever's bank-$04 post-movement path now queries the room background in
  right/left/up/down order and restores blocked X/Y coordinates without
  reversing its ordinary speed, matching the shared ROM helper.
- The chase-state blocked and unblocked regressions plus the complete Java
  suite cover this increment. Ground status, water/pit/conveyor effects,
  recoil smoke, and remaining damage-state branches remain pending.
```

- [ ] **Step 3: Run final verification.** Run `gradle clean test` from `java/`,
then `git diff --check` and `git status --short --branch` from the worktree
root. Expected: `BUILD SUCCESSFUL`, no whitespace errors, and only intended
files changed.

- [ ] **Step 4: Commit the checkpoint.** Mark all checkboxes complete and run:

```bash
git add java/src/main/java/linksawakening/world/LeeverMotion.java \
  java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java \
  docs/reconstruction-roadmap.md \
  docs/superpowers/plans/2026-08-05-leever-background.md \
  docs/superpowers/specs/2026-08-05-leever-background-design.md
git commit -m "feat: add Leever wall rollback"
```
