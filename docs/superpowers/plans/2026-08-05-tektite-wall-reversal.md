# Tektite wall reversal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mirror Tektite's bank-$06 horizontal and vertical wall-collision speed reversal through the live Java room runtime.

**Architecture:** Keep the movement state machine in TektiteMotion. After its fixed-point X/Y update, use the existing RoomEntityBackgroundCollision callback to restore blocked coordinates and apply the source negate-and-arithmetic-half operation to speed X once for each collision flag group, preserving the source vertical-helper quirk.

**Tech Stack:** Java, JUnit 5, Gradle, LADX disassembly.

---

### Task 1: Add failing wall-reversal tests

**Files:**
- Modify: java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java beside the existing Tektite motion tests.

- [ ] **Step 1: Add horizontal collision regression**

Add:

    @Test
    void tektiteReversesAndHalvesSpeedXAfterHorizontalWallCollision() {
        EntitySpriteDefinition definition = pairDefinition(0x0D, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x0D, 64, 64, EntityStatus.ACTIVE,
                definition, 0, 0, 0, 0x80)));
        IntSupplier randomBytes = sequence(0x00, 0x00, 0x00, 0x00);

        for (int frame = 0; frame <= 32; frame++) {
            runtime.tick(frame, 120, 120, randomBytes);
        }
        assertEquals(0, runtime.tektiteState(0));
        assertEquals(0x10, runtime.tektiteSpeedX(0));
        assertEquals(0x10, runtime.tektiteSpeedY(0));

        RoomEntityBackgroundCollision rightWall =
            (entity, direction, nextX, nextY) -> direction == 0;
        runtime.tick(33, 120, 120, randomBytes, rightWall);

        assertEquals(64, runtime.snapshot().slots().get(0).x());
        assertEquals(65, runtime.snapshot().slots().get(0).y());
        assertEquals(0xF8, runtime.tektiteSpeedX(0));
        assertEquals(0x10, runtime.tektiteSpeedY(0));
    }

- [ ] **Step 2: Add vertical collision regression**

Add:

    @Test
    void tektiteVerticalWallCollisionUsesTheRomXSpeedHelper() {
        EntitySpriteDefinition definition = pairDefinition(0x0D, 2);
        RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
            new RoomEntity(0, 0, 0x0D, 64, 64, EntityStatus.ACTIVE,
                definition, 0, 0, 0, 0x80)));
        IntSupplier randomBytes = sequence(0x00, 0x00, 0x00, 0x00);

        for (int frame = 0; frame <= 32; frame++) {
            runtime.tick(frame, 120, 120, randomBytes);
        }
        RoomEntityBackgroundCollision downWall =
            (entity, direction, nextX, nextY) -> direction == 3;
        runtime.tick(33, 120, 120, randomBytes, downWall);

        assertEquals(65, runtime.snapshot().slots().get(0).x());
        assertEquals(64, runtime.snapshot().slots().get(0).y());
        assertEquals(0xF8, runtime.tektiteSpeedX(0));
        assertEquals(0x10, runtime.tektiteSpeedY(0));
    }

- [ ] **Step 3: Run the focused tests and verify RED**

Run from java/:

    gradle test --tests linksawakening.world.RoomEntityRuntimeTest

Expected: both new tests fail because TektiteMotion currently ignores the background callback, leaving both coordinates advanced and speed X at $10. Existing tests must continue passing.

### Task 2: Implement the source collision helpers

**Files:**
- Modify: java/src/main/java/linksawakening/world/TektiteMotion.java in advance and near its private numeric helpers.

- [ ] **Step 1: Query and restore directional collisions**

Immediately after computing x and y, add:

    if (backgroundCollision != null) {
        if (x != entity.x() && backgroundCollision.blocks(entity,
                directionForX(speedX[slot]), x, y)) {
            x = entity.x();
            speedX[slot] = negateAndHalve(speedX[slot]);
        }
        if (y != entity.y() && backgroundCollision.blocks(entity,
                directionForY(speedY[slot]), x, y)) {
            y = entity.y();
            speedX[slot] = negateAndHalve(speedX[slot]);
        }
    }

Use the proposed coordinates in each callback, and do not change speed Y in the vertical branch: the source TektiteVerticalCollision helper addresses the X-speed table.

- [ ] **Step 2: Add the source arithmetic helper**

Add:

    private static int negateAndHalve(int speed) {
        return (-signedByte(speed) >> 1) & 0xFF;
    }

Use the existing signedByte method. Add direction helpers that map negative X/Y speeds to left/up (`1`/`2`) and non-negative speeds to right/down (`0`/`3`).

- [ ] **Step 3: Run the focused tests and verify GREEN**

Run from java/:

    gradle test --tests linksawakening.world.RoomEntityRuntimeTest

Expected: BUILD SUCCESSFUL; both new collision tests pass and all existing Tektite/runtime tests remain green.

### Task 3: Document and verify the increment

**Files:**
- Modify: docs/reconstruction-roadmap.md in the Tektite bullet and immediately before ## Next entity increments.

- [ ] **Step 1: Update the Tektite status**

Replace the Tektite bullet ending with wording that wall-collision reversal is verified below while recoil and full background/water interaction remain pending.

- [ ] **Step 2: Add the verified increment record**

Record that the runtime restores blocked X/Y coordinates, applies the source negate-and-half speed-X helper for horizontal and vertical collision flags in order, and leaves recoil, water/pit/conveyor behavior, and remaining damage states pending.

- [ ] **Step 3: Run final verification**

Run gradle clean test from java/, then git diff --check and git status --short --branch from the worktree root. Expected: BUILD SUCCESSFUL, no whitespace errors, and only intended files changed.

- [ ] **Step 4: Commit the checkpoint**

    git add java/src/main/java/linksawakening/world/TektiteMotion.java java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java docs/reconstruction-roadmap.md docs/superpowers/plans/2026-08-05-tektite-wall-reversal.md
    git commit -m "feat: add Tektite wall reversal"
