# Mysterious Woods Raccoon Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reproduce the disassembly's outdoor Tarin/raccoon progression gate in overworld room `$51`, including powder transformation and persistent completion.

**Architecture:** A focused `TarinRaccoonMotion` owns source-shaped state and timing. `RoomEntityRuntime` adapts Link input, powder overlap, rendering, dialogs, sounds, and persistence; `RoomSession` writes the resulting room-status bit.

**Tech Stack:** Java 21, JUnit 5, Gradle, ROM-backed entity/OAM tables, LADX disassembly bank `$05`.

---

### Task 1: Source-shaped raccoon state machine

**Files:**
- Create: `java/src/main/java/linksawakening/world/TarinRaccoonMotion.java`
- Create: `java/src/test/java/linksawakening/world/TarinRaccoonMotionTest.java`

- [ ] **Step 1: Write failing initialization and state-0 tests**

Create tests that instantiate an active `$3F` entity at `(0x78, 0x40)` and assert:

```java
TarinRaccoonMotion motion = new TarinRaccoonMotion();
TarinRaccoonMotion.Update normal = motion.advance(
    raccoon(), new TarinRaccoonMotion.Input(0, 0x50, 0x60, false, false, false));
assertEquals(0, normal.state());
assertEquals(0, normal.spriteVariant());
assertFalse(normal.shouldGetLost());

TarinRaccoonMotion.Update blocked = motion.advance(
    normal.entity(), new TarinRaccoonMotion.Input(1, 0x50, 0x1F, false, false, false));
assertTrue(blocked.shouldGetLost());
assertEquals(2, blocked.spriteVariant());
```

- [ ] **Step 2: Run the test and verify RED**

Run: `cd java && gradle test --tests linksawakening.world.TarinRaccoonMotionTest`

Expected: compilation fails because `TarinRaccoonMotion` does not exist.

- [ ] **Step 3: Implement minimal state 0**

Define `Input(frameCounter, linkX, linkY, actionHeld, dialogActive, powderHit)` and
`Update(entity, state, spriteVariant, shouldGetLost, dialogGlobalId,
linkMotionBlocked, roomChanged, tarinFlag, soundChannel, soundId, spawnBomb)`.
Implement `05_tarin.asm:$49A3-$4A10`: variants `(frame >>> 4) & 1`, variants
`2 + ((frame >>> 3) & 1)` above Link Y `$30`, the one-shot lost flag, Dialog
`021` when Link first crosses Y `$20`, and Dialog `00D` for a valid action talk.

- [ ] **Step 4: Run tests and verify GREEN**

Run: `cd java && gradle test --tests linksawakening.world.TarinRaccoonMotionTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add java/src/main/java/linksawakening/world/TarinRaccoonMotion.java \
  java/src/test/java/linksawakening/world/TarinRaccoonMotionTest.java
git commit -m "feat: add Mysterious Woods raccoon motion"
```

### Task 2: Live outdoor Tarin behavior

**Files:**
- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
- Modify: `java/src/main/java/linksawakening/world/RoomSession.java`
- Create: `java/src/test/java/linksawakening/world/TarinRaccoonRuntimeTest.java`

- [ ] **Step 1: Write failing live-room tests**

Load room `$51` in a real `RoomSession`, set sword level 1, and assert that a
tick with Link Y `$1F` emits a lost-woods request. Then hold the action button
while facing/near the raccoon and assert Dialog `00D`. Restore room status `$10`,
reload `$51`, and assert entity `$3F` is absent.

```java
session.loadInitialOverworld(0x51);
session.setChestPlayerLevels(1, 1, 0);
session.tickEntities(0, 0x78, 0x1F);
assertTrue(session.consumeMysteriousWoodsLostRequests().size() == 1);
```

- [ ] **Step 2: Run the test and verify RED**

Run: `cd java && gradle test --tests linksawakening.world.TarinRaccoonRuntimeTest`

Expected: compilation fails because the lost-woods event API is absent.

- [ ] **Step 3: Wire state 0 into the runtime**

Add one `TarinRaccoonMotion`, a pending lost-woods request list, and an outdoor
`ENTITY_TARIN` branch in `RoomEntityRuntime.tick`. Restrict it to overworld
rooms and preserve the existing indoor friendly push path. Queue returned
dialogs through `DialogRequest`, apply variants, and expose/harvest the lost
request through `RoomSession`.

- [ ] **Step 4: Apply source status-bit unload**

In the outdoor Tarin branch, disable `$3F` before state processing when
`entityRoomStatus & 0x10` is nonzero. Do not apply this rule to indoor Tarin.

- [ ] **Step 5: Run focused tests and verify GREEN**

Run: `cd java && gradle test --tests linksawakening.world.TarinRaccoonRuntimeTest --tests linksawakening.world.RoomSessionTest.newGameHouseMarinAndTarinUseTheLiveFriendlyPushPath`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add java/src/main/java/linksawakening/world/RoomEntityRuntime.java \
  java/src/main/java/linksawakening/world/RoomSession.java \
  java/src/test/java/linksawakening/world/TarinRaccoonRuntimeTest.java
git commit -m "feat: block the Mysterious Woods path"
```

### Task 3: Powder transformation sequence

**Files:**
- Modify: `java/src/main/java/linksawakening/world/TarinRaccoonMotion.java`
- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
- Modify: `java/src/test/java/linksawakening/world/TarinRaccoonMotionTest.java`
- Modify: `java/src/test/java/linksawakening/world/TarinRaccoonRuntimeTest.java`

- [ ] **Step 1: Write a failing live powder-collision test**

Spawn powder through `RoomSession.sprinkleMagicPowder` at the raccoon's source
coordinates, tick through the final collision window with matching slot cadence,
and assert the raccoon enters state 1 even though `$3F` has NPC/ignore-hits flags.
Assert indoor room `$A3` Tarin does not transform under the same overlap.

- [ ] **Step 2: Run the test and verify RED**

Run: `cd java && gradle test --tests linksawakening.world.TarinRaccoonRuntimeTest.magicPowderStartsTheSourceTransformation`

Expected: FAIL because ordinary powder collision filters out Tarin.

- [ ] **Step 3: Add the explicit source collision branch**

In `collideMagicPowderWithEntities`, before generic enemy filtering, detect an
active outdoor `$3F` within the source `$0C` windows. Set a per-slot powder-hit
latch consumed by `TarinRaccoonMotion`; keep indoor Tarin and transformed states
excluded.

- [ ] **Step 4: Write failing state 1-3 timing tests**

Advance from `powderHit=true` and assert Link blocking, six-frame variant cycle,
bomb spawn with private state `$4C`, room-change/Tarin flag at the source
transition, landing jingle `$23`, countdown `$40`, Dialog `00A` at countdown 1,
and post-transform Dialog `00B`.

- [ ] **Step 5: Implement states 1-3 and runtime event adaptation**

Port `$4A17-$4BBA` with byte-wrapped speeds/countdowns. Use existing runtime
bomb spawning, `EntityCombatEvent`, `LinkMotionBlockRequest`, and
`EntitySpriteHandlerCatalog` APIs; add ROM definitions for transformation
variants only if the existing 11 outdoor Tarin variants do not already cover
indices 8-10.

- [ ] **Step 6: Run focused tests and verify GREEN**

Run: `cd java && gradle test --tests linksawakening.world.TarinRaccoonMotionTest --tests linksawakening.world.TarinRaccoonRuntimeTest`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add java/src/main/java/linksawakening/world/TarinRaccoonMotion.java \
  java/src/main/java/linksawakening/world/RoomEntityRuntime.java \
  java/src/test/java/linksawakening/world/TarinRaccoonMotionTest.java \
  java/src/test/java/linksawakening/world/TarinRaccoonRuntimeTest.java
git commit -m "feat: transform the Mysterious Woods raccoon"
```

### Task 4: Persistence and order-of-play verification

**Files:**
- Modify: `java/src/main/java/linksawakening/world/RoomSession.java`
- Modify: `java/src/test/java/linksawakening/world/TarinRaccoonRuntimeTest.java`
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`

- [ ] **Step 1: Write a failing persistence test**

Complete the powder sequence in room `$51`, assert
`overworldRoomStatusForTest(0x51) & 0x10 == 0x10`, reload the room, and assert no
active entity `$3F` remains.

- [ ] **Step 2: Run the test and verify RED**

Run: `cd java && gradle test --tests linksawakening.world.TarinRaccoonRuntimeTest.transformationPersistsAcrossRoomReload`

Expected: FAIL until RoomSession harvests the room-change event.

- [ ] **Step 3: Persist the transformation**

Harvest the runtime completion into `overworldRoomStatus[0x51] |= 0x10`, mirror
the returned Tarin flag in RoomSession state, and synchronize both values back
into newly created runtimes.

- [ ] **Step 4: Extend the opening route regression**

After the existing house-to-beach assertions, initialize sword level 1, load
room `$80` to prove the forest owl remains available, then load `$51` and assert
the raccoon gate is active before powder and absent after completion/reload.

- [ ] **Step 5: Run all verification**

Run:

```bash
cd java
gradle test --rerun-tasks
cd ..
git diff --check
```

Expected: `BUILD SUCCESSFUL` and no diff-check output.

- [ ] **Step 6: Commit**

```bash
git add java/src/main/java/linksawakening/world/RoomSession.java \
  java/src/test/java/linksawakening/world/TarinRaccoonRuntimeTest.java \
  java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java
git commit -m "test: verify the Mysterious Woods opening gate"
```
