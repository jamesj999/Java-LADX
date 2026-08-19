# Bottle Grotto Power Bracelet Route Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the uninterrupted fresh-game Bottle Grotto trace from room `$36` through the ROM route to room `$20`, clear the Boo Buddy event, and obtain and persist the Power Bracelet.

**Architecture:** Keep one live `RoomSession`, `Link`, `PlayerState`, collision model, dungeon item state, and frame counter across the ordered route. Exercise existing generic boundary, key-door, enemy, room-event, chest, and reward APIs; add production code only when a watched RED test proves a shared source mismatch. Any correction must follow the disassembly rather than introduce room IDs or test-only state changes.

**Tech Stack:** Java 21, Gradle 9, JUnit 5, LWJGL input constants, `LADX-Disassembly`, shipped `azle.gbc` test ROM.

---

### Task 1: Return through the warp and reach room `$21`

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`
- Reference: `LADX-Disassembly/src/data/maps/layouts.asm`
- Reference: `LADX-Disassembly/src/data/rooms/indoors_a.asm`
- Reference: `LADX-Disassembly/src/data/entities/indoors_a.asm`

- [x] **Step 1: Extend the existing ordered test after the room `$36` warp**

Continue `freshGameRuntimeSequenceCollectsBottleGrottoFirstKeyInOrder` by using
room `$36`'s active entity `$61` to return to `$28`, including the source
leave-to-arm/contact/countdown flow. Then traverse `$28 -> $29 -> $26 -> $21`
using the collision-path and indoor-boundary helpers. Assert each room
ID and retain the already-cleared miniboss/door state that makes the route
legal. Do not attempt the solid `$2E -> $2C` north wall.

- [x] **Step 2: Assert room `$21` source state**

Assert event `$00`, chest `$A0` at `$27`, and the ROM-selected
`CHEST_RUPEES_20` reward.

- [x] **Step 3: Run the ordered test and verify RED or route success**

Run:

```bash
cd java && gradle test --tests 'linksawakening.world.RoomTransitionCoordinatorTest.freshGameRuntimeSequenceCollectsBottleGrottoFirstKeyInOrder' --rerun-tasks
```

Expected: either PASS through room `$21`, or FAIL at the first precise
collision/room-state mismatch. Do not add production code if the route passes.

- [x] **Step 4: If RED, trace and fix only the demonstrated shared defect**

Read the complete matching source routine, add the smallest focused regression
that fails for the same reason, implement the generic fix, and rerun both tests.

- [x] **Step 5: Commit the ordered route checkpoint**

```bash
git add java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java
git commit -m "test: return from Bottle Grotto Hinox warp"
```

Completed as commit `027709d`.

### Task 2: Enter room `$20`

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`
- Modify only for a demonstrated shared defect: `java/src/main/java/linksawakening/world/RoomSession.java`
- Modify only for a demonstrated shared defect: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
- Test only for a demonstrated shared defect: `java/src/test/java/linksawakening/world/RoomSessionTest.java`
- Reference: `LADX-Disassembly/src/data/maps/layouts.asm`
- Reference: `LADX-Disassembly/src/data/rooms/indoors_a.asm`

- [ ] **Step 1: Cross `$21 -> $20` through collision**

Reach room `$21`'s left boundary through collision and use the ordinary indoor
scroll path. Assert room `$20`; do not spend a key or interact with room `$21`'s
optional 20-rupee chest.

- [ ] **Step 2: Assert fresh room `$20` state**

Assert event `$61`, both Boo Buddies `$50` at `$24/$45`, no interactable chest
before the event, and the hidden `$A1` marker's floor substitution.

- [ ] **Step 3: Run RED**

Run the exact ordered test with `--rerun-tasks`. Confirm any failure is caused
by a missing lifecycle behavior, not a bad interaction coordinate or timeout.

- [ ] **Step 4: Implement the minimal generic fix if required**

Preserve `ChestContentsTable`, chest entity timing, dialog, and status ownership.
No room `$22` conditional is permitted.

- [ ] **Step 5: Run focused and ordered tests GREEN**

```bash
cd java && gradle test \
  --tests 'linksawakening.world.RoomTransitionCoordinatorTest.freshGameRuntimeSequenceCollectsBottleGrottoFirstKeyInOrder' \
  --rerun-tasks
```

- [ ] **Step 6: Commit**

```bash
git add java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java java/src/main/java/linksawakening/world/RoomSession.java java/src/main/java/linksawakening/world/RoomEntityRuntime.java java/src/test/java/linksawakening/world/RoomSessionTest.java
git commit -m "test: reach Bottle Grotto room 20"
```

### Task 3: Clear Boo Buddies and reveal the Bracelet chest

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`
- Modify only for a demonstrated shared defect: `java/src/main/java/linksawakening/world/BooBuddyMotion.java`
- Modify only for a demonstrated shared defect: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
- Modify only for a demonstrated shared defect: `java/src/main/java/linksawakening/world/RoomSession.java`
- Test only for a demonstrated shared defect: `java/src/test/java/linksawakening/world/BooBuddyRuntimeTest.java`
- Reference: `LADX-Disassembly/src/code/entities/06_boo_buddy.asm`
- Reference: `LADX-Disassembly/src/code/events.asm`
- Reference: `LADX-Disassembly/src/code/bank0.asm`

- [ ] **Step 1: Drive both Boo Buddies through live combat**

Initialize them, use collision-reachable sword contact positions, face them as
required by `BooBuddyEntityHandler`, and call `resolveEntityCombat` with live
Sword collision boxes. Wait through recoil/death with bounded deadlines. Do not
write health or slots directly.

- [ ] **Step 2: Assert event `$61` resolution**

Verify the kill-all trigger fires only after both enemies are disabled, event
state clears, room status bit `$10` persists, chest-appearance VFX/timing runs,
and active chest `$A0` appears at `$28`.

- [ ] **Step 3: Run ordered RED and focused Boo Buddy coverage**

```bash
cd java && gradle test \
  --tests 'linksawakening.world.BooBuddyRuntimeTest' \
  --tests 'linksawakening.world.RoomTransitionCoordinatorTest.freshGameRuntimeSequenceCollectsBottleGrottoFirstKeyInOrder' \
  --rerun-tasks
```

- [ ] **Step 4: Implement a source-backed shared correction only if RED**

Follow `BooBuddyEntityHandler`, generic enemy death, and the room-event reveal
handler. No forced enemy clear, event assignment, or room `$20` branch.

- [ ] **Step 5: Rerun GREEN and commit**

```bash
git add java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java java/src/main/java/linksawakening/world/BooBuddyMotion.java java/src/main/java/linksawakening/world/RoomEntityRuntime.java java/src/main/java/linksawakening/world/RoomSession.java java/src/test/java/linksawakening/world/BooBuddyRuntimeTest.java
git commit -m "feat: reveal Bottle Grotto Bracelet chest"
```

### Task 4: Collect and persist the Power Bracelet

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`
- Modify only for a demonstrated shared defect: `java/src/main/java/linksawakening/state/PlayerState.java`
- Modify only for a demonstrated shared defect: `java/src/main/java/linksawakening/world/ChestContentsTable.java`
- Modify only for a demonstrated shared defect: `java/src/main/java/linksawakening/world/RoomSession.java`
- Test only for a demonstrated shared defect: `java/src/test/java/linksawakening/state/PlayerStateTest.java`

- [ ] **Step 1: Open the revealed chest through collision**

Use the ordinary upward chest interaction at `$28`, tick the chest lifecycle,
consume `CHEST_POWER_BRACELET`, and assert the expected dialog and presentation
events terminate.

- [ ] **Step 2: Apply and verify the reward**

Assert inventory item `$03`, Power Bracelet level `$00 -> $01`, equipped-item
availability, zero duplicate rewards, and room `$20` status bit `$10`.

- [ ] **Step 3: Reload and prove persistence**

Leave and re-enter `$20`; assert open chest `$A1`, no live Boo Buddies, event
`$00`, retained Bracelet state, and no replayed chest reward.

- [ ] **Step 4: Run focused and ordered tests**

```bash
cd java && gradle test \
  --tests 'linksawakening.state.PlayerStateTest' \
  --tests 'linksawakening.world.RoomEntityRuntimeChestTest' \
  --tests 'linksawakening.world.RoomSessionTest' \
  --tests 'linksawakening.world.RoomTransitionCoordinatorTest.freshGameRuntimeSequenceCollectsBottleGrottoFirstKeyInOrder' \
  --rerun-tasks
```

- [ ] **Step 5: Commit**

```bash
git add java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java java/src/main/java/linksawakening/state/PlayerState.java java/src/main/java/linksawakening/world/ChestContentsTable.java java/src/main/java/linksawakening/world/RoomSession.java java/src/test/java/linksawakening/state/PlayerStateTest.java
git commit -m "feat: collect Bottle Grotto Power Bracelet"
```

### Task 5: Review, verify, and document

**Files:**
- Modify: `docs/reconstruction-roadmap.md`
- Modify: `docs/superpowers/plans/2026-08-19-bottle-grotto-power-bracelet.md`

- [ ] **Step 1: Obtain source-fidelity review**

Compare the complete diff against `MapLayout1`, room/entity/chest/event tables,
key-door code, Boo Buddy handler, chest handler, and status persistence. Fix and
re-review every Critical or Important finding.

- [ ] **Step 2: Obtain code-quality review after source approval**

Review lifecycle ownership, bounded waits, collision authenticity, helper
reuse, genericity, and regression coverage. Fix and re-review every Critical
or Important finding.

- [ ] **Step 3: Run clean verification**

```bash
cd java && gradle clean test
```

Count every `TEST-*.xml` suite and require zero failures, errors, and skips.

- [ ] **Step 4: Inspect and validate the complete diff**

```bash
git diff --check
git status --short
git diff --stat
```

Update the roadmap with the exact route, source labels, behavior, test totals,
and next gameplay frontier.

- [ ] **Step 5: Commit the verified milestone**

```bash
git add docs/reconstruction-roadmap.md docs/superpowers/plans/2026-08-19-bottle-grotto-power-bracelet.md
git commit -m "feat: obtain Bottle Grotto Power Bracelet"
```
