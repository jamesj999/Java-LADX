# Tail Cave First Key Door Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Continue the fresh-game runtime sequence from Tail Cave's first small-key chest through the first locked door and into the adjacent room using ROM layout, collision, key inventory, animation, and synchronized room status.

**Architecture:** Extend the existing ordered integration in `RoomTransitionCoordinatorTest` so no dungeon state is preloaded. Traverse indoor boundaries through `RoomTransitionCoordinator`, invoke the same collision bridge used by `Main`, and assert the source's eight-frame key-door mutation plus both rooms' status bits. Change production only if this live handoff fails for a source-backed reason.

**Tech Stack:** Java 21, JUnit 5, Gradle, LADX disassembly and shipped ROM tables.

---

### Task 1: Connect the earned key to room `$0F`

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`

- [ ] **Step 1: Write the ordered failing test extension**

After the room `$13` small-key assertion, cross right to room `$14` and up to room `$0F` through a new indoor collision-connectivity helper:

```java
walkToAndCrossIndoorBoundary(
    coordinator, scroll, collision, link, ScrollController.RIGHT);
assertEquals(0x14, session.currentRoomId());
walkToAndCrossIndoorBoundary(
    coordinator, scroll, collision, link, ScrollController.UP);
assertEquals(0x0F, session.currentRoomId());
```

- [ ] **Step 2: Run the focused test and verify the handoff**

Run:

```bash
gradle test --tests linksawakening.world.RoomTransitionCoordinatorTest.freshGameRuntimeSequencePersistsOpeningProgressThroughTailCaveEntry
```

Expected: either PASS, proving existing production already connects this handoff, or a behavior assertion failure identifying the missing source path. Compilation/setup errors must be corrected before interpreting the result.

### Task 2: Unlock, animate, synchronize, and cross the first key door

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`
- Modify only on a demonstrated red behavior: `java/src/main/java/linksawakening/world/RoomSession.java`

- [ ] **Step 1: Add the live key-door assertions**

Use the source coordinates and collision byte already established by `tailCaveFirstKeyDoorConsumesTheKeyAndSynchronizesBothRooms`:

```java
assertTrue(session.tryUnlockIndoorKeyDoor(
    0x85, 0x38, Link.DIRECTION_RIGHT, 0x08));
assertEquals(0, session.currentDungeonItemFlagsSnapshot()[
    DungeonItemState.SMALL_KEYS_INDEX]);
for (int tick = 0; tick < 8; tick++) {
    session.tickEntities(frame++, 0x85, 0x38);
    assertTrue(session.consumeWorldLinkMotionBlockRequest());
}
assertEquals(0x01, session.indoorRoomStatusForTest(0x00, 0x0F) & 0x01);
assertEquals(0x02, session.indoorRoomStatusForTest(0x00, 0x10) & 0x02);
```

Then cross right through the mutated doorway and assert room `$10`.

- [ ] **Step 2: Verify RED if production behavior is missing**

Run the focused Gradle test. If it fails, confirm the failure is the door handoff—not test setup—and trace `ApplyCollisionWithObject`/door animation in `bank2.asm` before editing production.

- [ ] **Step 3: Implement the minimal source-backed correction if required**

Keep the fix in the existing `RoomSession.tryUnlockIndoorKeyDoor` and pending door-animation path. Do not preload keys, directly mutate status in the test, or bypass collision/room transitions.

- [ ] **Step 4: Verify focused and full suites**

Run:

```bash
gradle test --tests linksawakening.world.RoomTransitionCoordinatorTest.freshGameRuntimeSequencePersistsOpeningProgressThroughTailCaveEntry
gradle clean test
git diff --check
```

Expected: focused PASS, full BUILD SUCCESSFUL, and no whitespace errors.

- [ ] **Step 5: Review and commit**

Review the diff against this plan and the disassembly, then commit only the scoped files:

```bash
git commit -m "test: connect Tail Cave first key door"
```
