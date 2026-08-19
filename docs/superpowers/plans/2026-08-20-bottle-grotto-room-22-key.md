# Bottle Grotto Room `$22` Small Key Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Continue uninterrupted Bottle Grotto play from the collected Power Bracelet through room `$22`'s ROM-backed Small Key chest and prove persistence.

**Architecture:** Extend the existing live ordered `RoomTransitionCoordinatorTest` without resetting dungeon-global switch state or constructing a new session. Reuse ordinary collision, indoor scrolling, chest entity, reward, dialog, and room-status APIs. Add production behavior only if a watched RED test proves a shared source mismatch.

**Tech Stack:** Java 21, Gradle 9, JUnit 5, `LADX-Disassembly`, shipped `azle.gbc` test ROM.

---

### Task 1: Traverse into room `$22` and assert source state

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`
- Reference: `LADX-Disassembly/src/data/maps/layouts.asm`
- Reference: `LADX-Disassembly/src/data/rooms/indoors_a.asm`
- Reference: `LADX-Disassembly/src/data/entities/indoors_a.asm`

- [ ] **Step 1: Write the ordered route assertion**

After room `$20` reload persistence, cross right to `$21`, then right to `$22`
using `walkToAndCrossIndoorBoundary`. Assert room IDs at both boundaries and
that `entitySwitchBlocksStateForTest()` remains `$00`.

- [ ] **Step 2: Assert room `$22` ROM state**

Assert event `$00`, chest `$A0` at `$27`, crystal entity `$66` at ROM position
`$48/$30`, four heart entities at source locations `$53..$56`, four pot objects
`$20` at `$53..$56`, representative `$DB/$DC` objects, and ROM-selected
`CHEST_SMALL_KEY`.

- [ ] **Step 3: Run the ordered test**

```bash
cd java && gradle test --tests 'linksawakening.world.RoomTransitionCoordinatorTest.freshGameRuntimeSequenceCollectsBottleGrottoFirstKeyInOrder' --rerun-tasks
```

Expected: PASS into room `$22`, or FAIL at the first exact collision/state
mismatch. A passing result means no production correction is authorized.

- [ ] **Step 4: Commit the traversal checkpoint**

```bash
git add java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java
git commit -m "test: reach Bottle Grotto room 22"
```

### Task 2: Collect and persist room `$22`'s Small Key

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`
- Modify only after a source-backed RED: `java/src/main/java/linksawakening/world/RoomSession.java`
- Modify only after a source-backed RED: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
- Test only after a shared defect: `java/src/test/java/linksawakening/world/RoomSessionTest.java`
- Reference: `LADX-Disassembly/src/data/chests/indoors_a.asm`
- Reference: `LADX-Disassembly/src/code/entities/bank3.asm`

- [ ] **Step 1: Reach and open chest `$27` through collision**

Use `reachablePositionPath` to reach the upward chest interaction tile without
crossing blocked `$DC` cells. Call `tryOpenChest` facing up and assert
`CHEST_SMALL_KEY`, room `$22`, and location `$27`.

- [ ] **Step 2: Complete the chest lifecycle**

Tick with a bounded `$40`-frame loop, consume each `ChestRewardEvent`, apply it
through `PlayerState.applyChestReward`, observe the ROM-selected dialog, and
assert chest teardown. Verify Small Keys `$00 -> $01`, object `$A1`, and room
status bit `$10`.

- [ ] **Step 3: Reload and prove persistence**

Cross west to `$21`, return east to `$22`, advance `$10` interactive gameplay
ticks, and assert chest `$A1`, event `$00`, switch state `$00`, Small Keys `$01`,
and an empty reward queue.

- [ ] **Step 4: Run ordered and focused GREEN verification**

```bash
cd java && gradle test \
  --tests 'linksawakening.world.RoomEntityRuntimeChestTest' \
  --tests 'linksawakening.world.RoomSessionTest' \
  --tests 'linksawakening.world.RoomTransitionCoordinatorTest' \
  --rerun-tasks
```

- [ ] **Step 5: Commit the key milestone**

```bash
git add java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java
git commit -m "feat: collect Bottle Grotto room 22 key"
```

### Task 3: Review, verify, and document

**Files:**
- Modify: `docs/reconstruction-roadmap.md`
- Modify: `docs/superpowers/plans/2026-08-20-bottle-grotto-room-22-key.md`

- [ ] **Step 1: Obtain source-fidelity review**

Compare the diff with `MapLayout1`, `IndoorsA21`, `IndoorsA22`, their entity and
chest tables, global switch-state ownership, and chest persistence. Fix and
re-review every Critical or Important finding.

- [ ] **Step 2: Obtain code-quality review**

Review collision authenticity, bounded waits, lifecycle ownership, diagnostics,
and regression scope. Fix and re-review every Critical or Important finding.

- [ ] **Step 3: Run clean verification and count XML results**

```bash
cd java && gradle clean test --rerun-tasks
```

Require zero failures, errors, and skipped tests. Run `git diff --check`, inspect
the complete diff, and record the exact suite/test count.

- [ ] **Step 4: Update roadmap and commit**

Document the exact route, carried switch state, chest/reward persistence, test
totals, and the next gameplay frontier. Static pot lifting remains explicitly
deferred until an ordered route requires it.

```bash
git add docs/reconstruction-roadmap.md docs/superpowers/plans/2026-08-20-bottle-grotto-room-22-key.md
git commit -m "docs: verify Bottle Grotto room 22 key"
```
