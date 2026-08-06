# Link Switch-Block Footing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port the ROM's `$DB/$DC` Link switch-block collision and transient standing override into the live Java room path.

**Architecture:** Keep the source table and XOR rules in a pure `SwitchBlockLinkInteraction` decision unit. `OverworldCollision` owns the live room/state flags and delegates switch-block decisions, while `Link` refreshes the standing flag before its existing leading-edge movement probe. `RoomSession` synchronizes its existing switch animation state into the shared collision object at room loads and VBlank updates.

**Tech Stack:** Java 17, JUnit 5, Gradle, shipped `azle.gbc`, LADX disassembly.

---

### Task 1: Specify the ROM decision unit

**Files:**
- Create: `java/src/test/java/linksawakening/world/SwitchBlockLinkInteractionTest.java`
- Create: `java/src/main/java/linksawakening/world/SwitchBlockLinkInteraction.java`

- [ ] **Step 1: Write the failing tests**

Add tests for the exact table and branch:

```java
@Test
void mapsLoweredAndRaisedObjectsToRomStates() {
    assertEquals(0x00, SwitchBlockLinkInteraction.expectedState(0xDB));
    assertEquals(0x02, SwitchBlockLinkInteraction.expectedState(0xDC));
    assertTrue(SwitchBlockLinkInteraction.isSwitchBlock(0xDB));
    assertTrue(SwitchBlockLinkInteraction.isSwitchBlock(0xDC));
    assertFalse(SwitchBlockLinkInteraction.isSwitchBlock(0xDA));
}

@Test
void blocksOnlyWhenRomStateDoesNotMatchTheObject() {
    assertFalse(SwitchBlockLinkInteraction.blocks(0xDB, 0x04, 0x00, false));
    assertTrue(SwitchBlockLinkInteraction.blocks(0xDC, 0x04, 0x00, false));
    assertTrue(SwitchBlockLinkInteraction.blocks(0xDB, 0x04, 0x02, false));
    assertFalse(SwitchBlockLinkInteraction.blocks(0xDC, 0x04, 0x02, false));
}

@Test
void standingOverridePassesAMismatchedSwitchBlock() {
    assertFalse(SwitchBlockLinkInteraction.blocks(0xDC, 0x04, 0x00, true));
}

@Test
void groundRefreshMarksOnlyAMismatchedOceanSwitchBlock() {
    assertTrue(SwitchBlockLinkInteraction.marksStanding(0xDC, 0x04, 0x00));
    assertFalse(SwitchBlockLinkInteraction.marksStanding(0xDC, 0x04, 0x02));
    assertFalse(SwitchBlockLinkInteraction.marksStanding(0xDC, 0x01, 0x00));
}
```

Include rejection tests for an invalid state and an object ID outside `$DB/$DC`
when `expectedState` is requested.

- [ ] **Step 2: Run the focused test to verify RED**

Run:

```bash
gradle -p java test --tests linksawakening.world.SwitchBlockLinkInteractionTest
```

Expected: compilation/test failure because the decision unit does not exist.

- [ ] **Step 3: Implement the minimal decision unit**

Create `SwitchBlockLinkInteraction` with the source mapping `$DB → $00`,
`$DC → $02`, and these core predicates:

```java
static boolean blocks(int objectId, int physicsFlag, int switchBlocksState,
                      boolean standingOnSwitchBlock) {
    validateState(switchBlocksState);
    if (physicsFlag != 0x04 || !isSwitchBlock(objectId)) {
        return false;
    }
    return !standingOnSwitchBlock
        && ((switchBlocksState ^ expectedState(objectId)) != 0);
}

static boolean marksStanding(int objectId, int physicsFlag, int switchBlocksState) {
    validateState(switchBlocksState);
    return physicsFlag == 0x04
        && isSwitchBlock(objectId)
        && ((switchBlocksState ^ expectedState(objectId)) != 0);
}
```

Keep all methods byte-validating and use the source table rather than a
generic arithmetic assumption.

- [ ] **Step 4: Run the focused test to verify GREEN**

Run the same Gradle command. Expected: all decision-unit tests pass.

- [ ] **Step 5: Commit the decision unit**

```bash
git add java/src/main/java/linksawakening/world/SwitchBlockLinkInteraction.java \
  java/src/test/java/linksawakening/world/SwitchBlockLinkInteractionTest.java
git commit -m "test: specify Link switch-block footing rules"
```

### Task 2: Add stateful switch-block behavior to room collision

**Files:**
- Modify: `java/src/main/java/linksawakening/physics/OverworldCollision.java`
- Modify: `java/src/test/java/linksawakening/physics/OverworldCollisionTest.java`

- [ ] **Step 1: Write failing collision integration tests**

Add tests using a synthetic ROM physics table with physics byte `$04` for
`$DB/$DC`. Cover both state values through `pointBlocked`, then verify the
standing flag is set by the real foot sample and makes the subsequent point
probe pass. Also verify a non-`$04` physics byte does not take the special
branch.

Use the existing padded coordinate convention: place the object at
`0x11 + row * 0x10 + column`, and call `refreshLinkGroundInteraction` with
Link top-left coordinates so `pixel + (8, 12)` lands in that cell.

- [ ] **Step 2: Run the focused collision tests to verify RED**

```bash
gradle -p java test --tests linksawakening.physics.OverworldCollisionTest
```

Expected: compilation failure for the new state/refresh API, or assertion
failure showing that both switch-block kinds currently share the old generic
passability behavior.

- [ ] **Step 3: Implement the live collision state**

Add to `OverworldCollision`:

```java
private int switchBlocksState;
private boolean linkStandingOnSwitchBlock;

public void setSwitchBlocksState(int value) {
    if (value != 0x00 && value != 0x02) {
        throw new IllegalArgumentException("Switch-block state must be $00 or $02");
    }
    switchBlocksState = value;
}

public boolean linkStandingOnSwitchBlock() {
    return linkStandingOnSwitchBlock;
}

public boolean refreshLinkGroundInteraction(int linkPixelX, int linkPixelY) {
    int objectId = objectUnderLinkFeet(linkPixelX, linkPixelY);
    int physicsFlag = romTables.objectPhysicsFlag(physicsTableIndex, objectId);
    linkStandingOnSwitchBlock = SwitchBlockLinkInteraction.marksStanding(
        objectId, physicsFlag, switchBlocksState);
    return linkStandingOnSwitchBlock;
}
```

`setRoom` must clear `linkStandingOnSwitchBlock`. In `isCellBlocking`, obtain
the raw object and physics byte before the existing tree-overlay and generic
physics checks. Delegate physics `$04`/`$DB-$DC` to
`SwitchBlockLinkInteraction.blocks`; retain `idBlocks` for every other cell.
`refreshLinkGroundInteraction` must sample `objectUnderLinkFeet`, query the
selected physics table, call `marksStanding`, assign the result, and return
the current flag.

- [ ] **Step 4: Run focused tests to verify GREEN**

```bash
gradle -p java test --tests linksawakening.world.SwitchBlockLinkInteractionTest \
  --tests linksawakening.physics.OverworldCollisionTest
```

Expected: all decision and collision tests pass.

- [ ] **Step 5: Commit the collision integration**

```bash
git add java/src/main/java/linksawakening/physics/OverworldCollision.java \
  java/src/test/java/linksawakening/physics/OverworldCollisionTest.java
git commit -m "feat: apply ROM switch-block state to Link collision"
```

### Task 3: Wire Link's ground refresh and prove movement behavior

**Files:**
- Modify: `java/src/main/java/linksawakening/entity/Link.java`
- Modify: `java/src/test/java/linksawakening/entity/LinkTest.java`

- [ ] **Step 1: Write the failing Link movement tests**

Add two real-Link tests with a room object at the leading edge: a mismatched
switch block must reject movement, while a matching switch block must allow
movement. Set the synthetic ROM's selected physics table to `$04` for the
object IDs and configure the collision state before calling `link.update()`.

Add a transition test where Link starts over a mismatched block, calls
`link.update()` to establish the standing override, and then asserts the
collision probe is passable through that override.

- [ ] **Step 2: Run the focused Link tests to verify RED**

```bash
gradle -p java test --tests linksawakening.entity.LinkTest
```

Expected: the mismatched-block movement test fails because `Link` does not
refresh `wLinkStandingOnSwitchBlock` before its leading-edge probe.

- [ ] **Step 3: Add the minimal Link call**

At the start of `refreshGroundStatus`, before the existing pit check, call:

```java
if (collision != null) {
    collision.refreshLinkGroundInteraction(pixelX(), pixelY());
}
```

Do not alter the existing pit/slow-ground ordering or movement point tables.

- [ ] **Step 4: Run focused tests to verify GREEN**

```bash
gradle -p java test --tests linksawakening.entity.LinkTest
```

Expected: all Link tests pass, including the new switch-block cases.

- [ ] **Step 5: Commit Link integration**

```bash
git add java/src/main/java/linksawakening/entity/Link.java \
  java/src/test/java/linksawakening/entity/LinkTest.java
git commit -m "feat: refresh Link switch-block footing before movement"
```

### Task 4: Synchronize the live RoomSession state

**Files:**
- Modify: `java/src/main/java/linksawakening/world/RoomSession.java`
- Modify: `java/src/test/java/linksawakening/world/RoomSessionTest.java`

- [ ] **Step 1: Write the failing synchronization test**

Extend the existing VBlank switch-stage test so it installs a room object
sample containing `$DC`, sets state `$00`, verifies the shared collision is
blocked, advances the existing stage-$02 VBlank update, and verifies the same
object is passable after the state becomes `$02`. Keep the assertions on the
room-owned stage and state already present.

- [ ] **Step 2: Run the focused RoomSession test to verify RED**

```bash
gradle -p java test --tests linksawakening.world.RoomSessionTest \
  --tests linksawakening.world.SwitchBlockAnimationTest
```

Expected: the state assertion passes but the collision assertion remains at
the old state because RoomSession currently does not synchronize its state.

- [ ] **Step 3: Implement explicit synchronization**

Add a private helper in `RoomSession`:

```java
private void synchronizeSwitchBlockCollisionState() {
    overworldCollision.setSwitchBlocksState(switchBlocksState);
}
```

Call it after `loadOverworld` and `loadIndoor` install the room collision,
inside `setEntitySwitchBlocksStateForTest`, and immediately after
`tickGameplayVBlank` assigns the `SwitchBlockAnimation.Step` state. Preserve
the existing early return that pauses ordinary animated tiles during a switch
animation.

- [ ] **Step 4: Run focused tests to verify GREEN**

```bash
gradle -p java test --tests linksawakening.world.RoomSessionTest \
  --tests linksawakening.world.SwitchBlockAnimationTest \
  --tests linksawakening.physics.OverworldCollisionTest
```

Expected: all focused switch-block tests pass.

- [ ] **Step 5: Commit the RoomSession wiring**

```bash
git add java/src/main/java/linksawakening/world/RoomSession.java \
  java/src/test/java/linksawakening/world/RoomSessionTest.java
git commit -m "feat: synchronize room switch state with Link collision"
```

### Task 5: Update source-backed documentation and verify the branch

**Files:**
- Modify: `docs/reconstruction-roadmap.md`

- [ ] **Step 1: Document the verified boundary**

Add a dated roadmap section that names the bank-$02 collision branch, the
`[0x00, 0x02]` table, the live state synchronization, and the exact remaining
scope: switch-button/mobile producers, footstep sound, `wC13B` output, and
hookshot-chain behavior.

- [ ] **Step 2: Run the complete verification suite**

```bash
gradle -p java clean test
git diff --check HEAD~4..HEAD
git status --short --branch
```

Expected: Gradle reports `BUILD SUCCESSFUL`, diff check is silent, and the
feature worktree is clean apart from no files.

- [ ] **Step 3: Commit the documentation**

```bash
git add docs/reconstruction-roadmap.md
git commit -m "docs: record Link switch-block footing state"
```

- [ ] **Step 4: Re-run final verification after the documentation commit**

```bash
gradle -p java clean test
git diff --check
git status --short --branch
```

Only report completion with the fresh outputs from this final run.
