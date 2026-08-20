# Bottle Grotto Room `$22` Small Key Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Continue uninterrupted Bottle Grotto play from the collected Power Bracelet through room `$22`'s ROM-backed Small Key chest and prove persistence.

**Architecture:** Extend the existing live ordered `RoomTransitionCoordinatorTest` without resetting dungeon-global switch state or constructing a new session. Add the shared ROM background-pot-to-lifted-entity bridge demonstrated by the ordered RED, then reuse ordinary collision, indoor scrolling, chest entity, reward, dialog, and room-status APIs.

**Tech Stack:** Java 21, Gradle 9, JUnit 5, `LADX-Disassembly`, shipped `azle.gbc` test ROM.

---

### Task 1: Lift room `$21`'s blocking pot through the shared ROM path

**Files:**
- Modify: `java/src/main/java/linksawakening/world/LoadedRoom.java`
- Modify: `java/src/main/java/linksawakening/world/ActiveRoom.java`
- Modify: `java/src/main/java/linksawakening/world/RoomLoader.java`
- Modify: `java/src/main/java/linksawakening/world/RoomSession.java`
- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
- Modify: `java/src/main/java/linksawakening/Main.java`
- Test: `java/src/test/java/linksawakening/world/RoomSessionTest.java`
- Reference: `LADX-Disassembly/src/code/bank0.asm:$20CF-$21A7`
- Reference: `LADX-Disassembly/src/code/bank14.asm:$5526-$557D`

- [x] **Step 1: Write the focused failing session test**

Load map `$01`, room `$21`; position Link against a reachable object `$20`.
Call the background-lift interaction for seven valid pull frames and assert the
pot remains. On frame eight assert it becomes the room header floor `$0D`,
entity `$05` appears in status 7 at the object center, and the session exposes
an active lifted state. Also prove releasing the pull direction resets the
counter and that no lift occurs without the Bracelet button.

- [x] **Step 2: Run RED**

```bash
cd java && gradle test --tests 'linksawakening.world.RoomSessionTest.indoorPowerBraceletPullLiftsSourcePotOnEighthFrame' --rerun-tasks
```

Expected: compilation failure because the shared session API does not exist.

- [x] **Step 3: Implement the minimal source bridge**

Add `RoomEntityRuntime.spawnLiftedRoomObject` to create entity `$05` with pot
variant `$00` and enter `beginLift`. Add `RoomSession.tryLiftIndoorObject` to
validate indoor state, collision direction, Bracelet button, opposite pull
direction, object `$20/$8E`, and the eight-frame counter; reveal `$0D` or `$AA`,
refresh tile/collision state, and spawn the lifted entity at the source cell.
Wire `Main` with the actual equipped Bracelet and ROM pressed-direction mask.

- [x] **Step 4: Run focused GREEN**

```bash
cd java && gradle test \
  --tests 'linksawakening.world.RoomSessionTest.indoorPowerBraceletPullLiftsSourcePotOnEighthFrame' \
  --tests 'linksawakening.world.RoomEntityLiftThrowRuntimeTest' \
  --tests 'linksawakening.LinkTest' --rerun-tasks
```

- [ ] **Step 5: Commit the shared lift bridge**

```bash
git add java/src/main/java/linksawakening/Main.java java/src/main/java/linksawakening/world java/src/test/java/linksawakening/world/RoomSessionTest.java
git commit -m "feat: lift indoor pots with Power Bracelet"
```

### Task 2: Traverse into room `$22` and assert source state

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`
- Reference: `LADX-Disassembly/src/data/maps/layouts.asm`
- Reference: `LADX-Disassembly/src/data/rooms/indoors_a.asm`
- Reference: `LADX-Disassembly/src/data/entities/indoors_a.asm`

- [x] **Step 1: Write the ordered route assertion**

After room `$20` reload persistence, cross right to `$21`, then right to `$22`
using `walkToAndCrossIndoorBoundary`. Assert room IDs at both boundaries and
that `entitySwitchBlocksStateForTest()` remains `$00`.

- [x] **Step 2: Assert room `$22` ROM state**

Assert event `$00`, chest `$A0` at `$27`, crystal entity `$66` at ROM position
`$48/$30`, four heart entities at source locations `$53..$56`, four pot objects
`$20` at `$53..$56`, representative `$DB/$DC` objects, and ROM-selected
`CHEST_SMALL_KEY`.

- [x] **Step 3: Run the ordered test**

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

### Task 3: Collect and persist room `$22`'s Small Key

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`
- Modify only after a source-backed RED: `java/src/main/java/linksawakening/world/RoomSession.java`
- Modify only after a source-backed RED: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
- Test only after a shared defect: `java/src/test/java/linksawakening/world/RoomSessionTest.java`
- Reference: `LADX-Disassembly/src/data/chests/indoors_a.asm`
- Reference: `LADX-Disassembly/src/code/entities/bank3.asm`

- [x] **Step 1: Reach and open chest `$27` through collision**

Use the northeast room `$21` exit, hit room `$22`'s crystal with the live Sword,
preserve raised-block footing, and reach the upward chest interaction tile.
Call `tryOpenChest` facing up and assert
`CHEST_SMALL_KEY`, room `$22`, and location `$27`.

- [x] **Step 2: Complete the chest lifecycle**

Tick with a bounded `$40`-frame loop, consume each `ChestRewardEvent`, apply it
through `PlayerState.applyChestReward`, observe the ROM-selected dialog, and
assert chest teardown. Verify Small Keys `$00 -> $01`, object `$A1`, and room
status bit `$10`.

- [x] **Step 3: Reload and prove persistence**

Cross west to `$21`, return east to `$22`, advance `$10` interactive gameplay
ticks, and assert chest `$A1`, event `$00`, switch state `$02`, Small Keys `$01`,
and an empty reward queue.

- [x] **Step 4: Run ordered and focused GREEN verification**

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

### Task 4: Review, verify, and document

**Files:**
- Modify: `docs/reconstruction-roadmap.md`
- Modify: `docs/superpowers/plans/2026-08-20-bottle-grotto-room-22-key.md`

- [x] **Step 1: Obtain source-fidelity review**

Compare the diff with `MapLayout1`, `IndoorsA21`, `IndoorsA22`, their entity and
chest tables, global switch-state ownership, and chest persistence. Fix and
re-review every Critical or Important finding.

- [x] **Step 2: Obtain code-quality review**

Review collision authenticity, bounded waits, lifecycle ownership, diagnostics,
and regression scope. Fix and re-review every Critical or Important finding.

- [x] **Step 3: Run clean verification and count XML results**

```bash
cd java && gradle clean test --rerun-tasks
```

Require zero failures, errors, and skipped tests. Run `git diff --check`, inspect
the complete diff, and record the exact suite/test count.

- [ ] **Step 4: Update roadmap and commit**

Document the exact route, carried switch state, chest/reward persistence, test
totals, and the next gameplay frontier. Thrown-object damage against crystal
switches remains explicitly deferred until an ordered route requires it.

```bash
git add docs/reconstruction-roadmap.md docs/superpowers/plans/2026-08-20-bottle-grotto-room-22-key.md
git commit -m "docs: verify Bottle Grotto room 22 key"
```
