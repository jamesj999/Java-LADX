# Bottle Grotto Room `$3B` Side-View Passage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reproduce room `$3B`'s sinking platform and side-view pots, then traverse its authored exit back into Bottle Grotto room `$25`.

**Architecture:** Put ROM fixed-point movement in focused motion components while `RoomEntityRuntime` owns handler dispatch and emits typed Link-state requests. Forward those requests through `RoomSession` to `Main`, then prove the behavior in isolation and in the continuous ordered route.

**Tech Stack:** Java 21, Gradle 9, JUnit 5, `LADX-Disassembly`, shipped `azle.gbc` test ROM.

---

### Task 1: Port `$A5` platform motion

**Files:**
- Create: `java/src/main/java/linksawakening/world/SideViewPlatformMotion.java`
- Create: `java/src/test/java/linksawakening/world/SideViewPlatformMotionTest.java`
- Reference: `LADX-Disassembly/src/code/entities/07_sideviewplatform.asm`

- [x] **Step 1: Write failing tests**

Test fixed-point Y motion, frame-`&3` acceleration toward `$04`, activation
ramp to private state 4 `$04`, one-shot rumble, and immediate reset when Link
is no longer standing.

- [x] **Step 2: Run RED**

```bash
cd java && gradle test --tests linksawakening.world.SideViewPlatformMotionTest
```

Expected: compilation failure because `SideViewPlatformMotion` is absent.

- [x] **Step 3: Implement the minimal component**

Provide `advance(RoomEntity, frame, standing, activationAllowed)` returning the
updated entity, speed Y, private states 2/4, vertical delta, and rumble edge.
Use signed ROM speed bytes with an 8-bit fractional accumulator.

- [x] **Step 4: Run GREEN and commit**

```bash
cd java && gradle test --tests linksawakening.world.SideViewPlatformMotionTest
git add src/main/java/linksawakening/world/SideViewPlatformMotion.java src/test/java/linksawakening/world/SideViewPlatformMotionTest.java
git commit -m "feat: port side-view platform motion"
```

### Task 2: Integrate platform contact with live Link

**Files:**
- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
- Modify: `java/src/main/java/linksawakening/world/RoomSession.java`
- Modify: `java/src/main/java/linksawakening/Main.java`
- Create: `java/src/test/java/linksawakening/world/SideViewPlatformRuntimeTest.java`

- [x] **Step 1: Write the runtime RED**

Assert contact requires an interactive frame, nonnegative Link Y speed, the
ROM `$2C` platform hitbox, and Link above the platform. Assert room `$3B`
activates without a carried entity, another room does not, and the emitted
request contains `positionY=platformY-$10`, platform X delta, `speedY=$02`,
and standing=true.

- [x] **Step 2: Run RED**

```bash
cd java && gradle test --tests linksawakening.world.SideViewPlatformRuntimeTest
```

- [x] **Step 3: Wire runtime, session, and Main**

Add `SideViewPlatformLinkRequest`, clear/consume it with the other per-frame
requests, forward it from `RoomSession`, and apply the snap/carry/speed writes
to live Link before transition checks.

- [x] **Step 4: Run GREEN and commit**

```bash
cd java && gradle test --tests linksawakening.world.SideViewPlatformRuntimeTest --tests linksawakening.world.RoomSessionTest --tests linksawakening.LinkTest
git add src/main/java/linksawakening src/test/java/linksawakening
git commit -m "feat: carry Link on side-view platforms"
```

### Task 3: Port `$D6` motion and collision teardown

**Files:**
- Create: `java/src/main/java/linksawakening/world/SideViewPotMotion.java`
- Create: `java/src/test/java/linksawakening/world/SideViewPotMotionTest.java`
- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
- Modify: `java/src/test/java/linksawakening/world/RoomEntityLiftThrowRuntimeTest.java`
- Reference: `LADX-Disassembly/src/code/entities/19_side_view_pot.asm`

- [x] **Step 1: Write failing motion/runtime tests**

Assert Y speed increases by `$02` toward `$40`, signed fixed-point X/Y motion
matches ROM bytes, any background collision emits smash behavior and unloads,
and the already-ported pickup gates still hold.

- [x] **Step 2: Run RED**

```bash
cd java && gradle test --tests linksawakening.world.SideViewPotMotionTest --tests linksawakening.world.RoomEntityLiftThrowRuntimeTest
```

- [x] **Step 3: Implement and dispatch dedicated states 1/2**

Route `$D6` thrown/dropped states to `SideViewPotMotion`; do not call
`ThrownEntityMotion` for this type. Reuse runtime smash presentation and slot
teardown semantics.

- [x] **Step 4: Run GREEN and commit**

```bash
cd java && gradle test --tests linksawakening.world.SideViewPotMotionTest --tests linksawakening.world.RoomEntityLiftThrowRuntimeTest
git add src/main/java/linksawakening/world src/test/java/linksawakening/world
git commit -m "feat: port side-view pot motion"
```

### Task 4: Traverse room `$3B` in the ordered route

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`
- Reference: `LADX-Disassembly/src/data/rooms/indoors_a.asm`
- Reference: `LADX-Disassembly/src/data/entities/indoors_a.asm`

- [x] **Step 1: Extend the ordered test**

From the existing room `$3B` entry, use live entity ticks and Link requests to
activate and ride `$A5`, interact with `$D6`, reach the vertical warp, and
assert return to map `$01`, room `$25`, at ROM coordinates `$88/$20`.

- [x] **Step 2: Run focused GREEN**

```bash
cd java && gradle test --tests linksawakening.world.RoomTransitionCoordinatorTest.completeGameplayRouteUsesPhysicalTraversalAndRealRoomMechanics
```

- [x] **Step 3: Review and full verification**

Compare behavior against both source handlers and the room/warp tables. Then
run `gradle cleanTest test`, require zero failures/errors/skips, run
`git diff --check`, inspect the complete diff, and commit.

```bash
git add java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java docs/superpowers
git commit -m "feat: traverse Bottle Grotto side-view passage"
```
