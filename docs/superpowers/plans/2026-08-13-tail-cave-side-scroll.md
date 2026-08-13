# Tail Cave Staircase and Side-Scroll Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Tail Cave room `$03`'s revealed staircase enter side-scroll room `$19` and make its vertical edge return through ROM warp 0.

**Architecture:** Have `RoomObjectParser` preserve the last staircase location encountered in source stream order, carry it through the loaded-room model, and keep its live state in `RoomSession`. Add a distinct side-scroll vertical-warp boundary decision, then let `RoomTransitionCoordinator` apply the first ROM warp for both mechanisms.

**Tech Stack:** Java 17, JUnit 5, Gradle, ROM-backed room/object/warp data.

---

### Task 1: Preserve and trigger staircase state

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomObjectParserTest.java`
- Modify: `java/src/test/java/linksawakening/world/RoomSessionTest.java`
- Modify: `java/src/main/java/linksawakening/world/RoomObjectParseResult.java`
- Modify: `java/src/main/java/linksawakening/world/RoomObjectParser.java`
- Modify: `java/src/main/java/linksawakening/world/LoadedRoom.java`
- Modify: `java/src/main/java/linksawakening/world/ActiveRoom.java`
- Modify: `java/src/main/java/linksawakening/world/RoomLoader.java`
- Modify: `java/src/main/java/linksawakening/world/RoomSession.java`

- [ ] **Step 1: Write failing room-session tests**

Add parser tests proving a visible `$BE`, `$BF`, `$CB`, `$C5`, or `$C6` records its object location, a concealed `$BF` records `-1`, and later staircase objects overwrite earlier ones in stream order. Add session tests that load indoor room `$03` with status bit `$10`, assert its `$BF` staircase starts inactive at ROM center `(0x88,0x20)`, and exercise a query shaped like:

```java
assertNull(session.pollStaircaseWarp(0x88, 0x20, 0, false));
assertNull(session.pollStaircaseWarp(0x98, 0x20, 0, false));
assertEquals(session.activeRoom().firstWarp(),
    session.pollStaircaseWarp(0x88, 0x20, 0, false));
```

Also prove nonzero Z and carrying state suppress an otherwise active trigger, and that the room `$03` reveal path initializes the same inactive state when `$BE` is written.

- [ ] **Step 2: Run the focused tests and verify RED**

Run:

```bash
./gradlew test --tests linksawakening.world.RoomSessionTest
```

Expected: compilation failure because `pollStaircaseWarp` does not exist.

- [ ] **Step 3: Implement source-shaped staircase state**

Add `staircaseLocation` (default `-1`) to `RoomObjectParseResult`, `LoadedRoom`, and `ActiveRoom`. In `RoomObjectParser`, record each staircase object's actual location while processing ordinary objects, strips, and template/macro writes; because parsing is sequential, the final write mirrors the source's final HRAM configuration. Do not record concealed `$BF`.

Carry `parsed.staircaseLocation()` through `RoomLoader`. Add constants for `STAIRCASE_NONE`, `STAIRCASE_INACTIVE`, and `STAIRCASE_ACTIVE`; add state/center fields in `RoomSession`; clear them with transient room state; and configure them from `ActiveRoom.staircaseLocation()` after room activation. Convert location `YX` to the source centers:

```java
staircaseY = (location & 0xF0) + 0x10;
staircaseX = ((location & 0x0F) << 4) + 0x08;
staircaseState = STAIRCASE_INACTIVE;
```

Implement unsigned-byte range checks matching `bank2.asm`:

```java
private static boolean withinUnsignedByteRange(int value, int center, int bias, int size) {
    return ((value - center + bias) & 0xFF) < size;
}
```

`pollStaircaseWarp` must arm when either coordinate leaves the 12-pixel box, trigger only inside the 10-pixel box with `linkZ == 0` and `!carrying`, clear the state on trigger, and return `activeRoom.firstWarp()` only when one exists. Configure `(0x88,0x20)` in `tickRoomEventStairReveal()` when `$BE` is written.

- [ ] **Step 4: Run focused tests and verify GREEN**

Run the same `RoomSessionTest` command. Expected: PASS.

- [ ] **Step 5: Commit the staircase state**

```bash
git add java/src/main/java/linksawakening/world/RoomObjectParseResult.java java/src/main/java/linksawakening/world/RoomObjectParser.java java/src/main/java/linksawakening/world/LoadedRoom.java java/src/main/java/linksawakening/world/ActiveRoom.java java/src/main/java/linksawakening/world/RoomLoader.java java/src/main/java/linksawakening/world/RoomSession.java java/src/test/java/linksawakening/world/RoomObjectParserTest.java java/src/test/java/linksawakening/world/RoomSessionTest.java
git commit -m "feat: implement indoor staircase transitions"
```

### Task 2: Route side-scroll vertical edges through warp 0

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomBoundaryControllerTest.java`
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`
- Modify: `java/src/main/java/linksawakening/world/RoomBoundaryDecision.java`
- Modify: `java/src/main/java/linksawakening/world/RoomBoundaryController.java`
- Modify: `java/src/main/java/linksawakening/world/RoomTransitionCoordinator.java`

- [ ] **Step 1: Write failing boundary tests**

Add `SIDE_SCROLL_VERTICAL_WARP` to the expected API in tests and prove category 2 returns that decision at top and bottom edges while an interior position returns `NONE`. Preserve existing indoor behavior assertions.

- [ ] **Step 2: Run boundary tests and verify RED**

```bash
./gradlew test --tests linksawakening.world.RoomBoundaryControllerTest
```

Expected: compilation failure because the new decision type/factory does not exist.

- [ ] **Step 3: Implement the boundary decision**

Add the enum value and factory:

```java
public static RoomBoundaryDecision sideScrollVerticalWarp() {
    return new RoomBoundaryDecision(Type.SIDE_SCROLL_VERTICAL_WARP,
        ScrollController.NONE, 0, 0);
}
```

In `decideIndoor`, return it for category 2 when `offTop || offBottom`, before ordinary indoor scroll decisions. Leave horizontal handling unchanged.

- [ ] **Step 4: Add a failing coordinator test for room `$19`**

Load category-2 map `$00`, room `$19`, place Link beyond the top edge, call `handleWarpAndIndoorBoundaries`, and assert a fade starts while `ScrollController` remains inactive. Tick the fade and assert category 1 room `$03` with landing `(0x80,0x10)`.

- [ ] **Step 5: Implement coordinator routing**

Handle `SIDE_SCROLL_VERTICAL_WARP` beside the front-door decision, guard with `room.hasWarps()`, and call `transitionController.startFadeOut(() -> applyWarp(room.firstWarp(), link))`.

Before ordinary tile-warp matching, call `roomSession.pollStaircaseWarp(link.romEntityX(), link.romEntityY(), link.romEntityZ(), link.isCarryingLiftedObject())`; when non-null, start the same fade/apply path and return.

- [ ] **Step 6: Run focused tests and verify GREEN**

```bash
./gradlew test --tests linksawakening.world.RoomBoundaryControllerTest --tests linksawakening.world.RoomTransitionCoordinatorTest
```

Expected: PASS.

- [ ] **Step 7: Commit transition routing**

```bash
git add java/src/main/java/linksawakening/world/RoomBoundaryDecision.java java/src/main/java/linksawakening/world/RoomBoundaryController.java java/src/main/java/linksawakening/world/RoomTransitionCoordinator.java java/src/test/java/linksawakening/world/RoomBoundaryControllerTest.java java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java
git commit -m "feat: route side-scroll exits through room warps"
```

### Task 3: Extend the ordered Tail Cave regression

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`

- [ ] **Step 1: Extend the fresh-game sequence**

After the existing room `$03` reload assertion, place Link at the staircase center and prove no immediate transition; move to ROM X `$98` to arm it; return to `(0x88,0x20)` and assert fade. Tick to room `$19`, assert category 2 and Java landing `(0x70,0x00)`, then place Link at the top edge and assert the next fade returns to category 1 room `$03` at `(0x80,0x10)`.

- [ ] **Step 2: Run the ordered test**

```bash
./gradlew test --tests 'linksawakening.world.RoomTransitionCoordinatorTest.freshGameRuntimeSequencePersistsOpeningProgressThroughTailCaveEntry'
```

Expected: PASS using the production staircase and category-2 transition paths.

- [ ] **Step 3: Commit ordered coverage**

```bash
git add java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java
git commit -m "test: continue Tail Cave route through side-scroll room"
```

### Task 4: Verify and review

**Files:** No intended production changes.

- [ ] **Step 1: Run full verification**

```bash
./gradlew clean test
```

Expected: `BUILD SUCCESSFUL` with all tests passing.

- [ ] **Step 2: Inspect scope**

```bash
git status --short
git diff HEAD~3 --check
git diff HEAD~3 --stat
```

Expected: only staircase/side-scroll source, tests, and these documents; no whitespace errors.

- [ ] **Step 3: Review against source**

Compare the final implementation with `bank0.asm:.configureStairs`, `bank2.asm:renderTranscientVFXs`, and `bank2.asm:CheckPositionForMapTransition`. Correct any mismatch and rerun focused plus full tests before claiming completion.
