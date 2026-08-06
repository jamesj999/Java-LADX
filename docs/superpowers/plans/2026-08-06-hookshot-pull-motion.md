# Hookshot Pull Motion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply the ROM hookshot handler's immediate inverse vector to Link after the entity pass, so Link visibly moves toward a hookshotable target while normal input remains locked.

**Architecture:** Keep the existing `EntityProjectileEvent.Kind.HOOKSHOT_PULL` event as the room-to-player boundary. Add a dedicated `Link.applyRomFinalPosition` operation that applies vertical then horizontal signed fixed-point speed directly to Link's stored sub-pixel position without ordinary input collision checks. Dispatch only hookshot events through that immediate path; preserve queued `applyRomSpeed` behavior for other responses.

**Tech Stack:** Java 21, JUnit 5, Gradle, LWJGL host renderer, shipped LADX ROM/disassembly.

---

### Task 1: Specify the immediate Link motion contract with failing tests

**Files:**
- Modify: `java/src/test/java/linksawakening/entity/LinkTest.java`
- Modify: `java/src/test/java/linksawakening/MainArchitectureTest.java`

- [ ] **Step 1: Add the Link behavior test before production code**

Add this test next to `consumesRomResponseSpeedOnTheNextMotionUpdate` in
`LinkTest.java`:

```java
@Test
void appliesRomFinalPositionImmediatelyWhileMotionIsBlocked() {
    InputState inputState = new InputState();
    InputConfig inputConfig = new InputConfig(1, 2, 3, 4, 5, 6, 7);
    PlayerState playerState = new PlayerState();
    ItemRegistry itemRegistry = new ItemRegistry();
    itemRegistry.register(playerState.itemA(), new BlockingItem());
    Link link = new Link(inputState, inputConfig, null, null, null,
        playerState, itemRegistry);
    link.setPixelPosition(0x40, 0x50);

    link.applyRomFinalPosition(0x30, 0xE8);
    link.update();

    assertEquals(0x43, link.pixelX());
    assertEquals(0x4E, link.pixelY());
}
```

The blocking item proves the method does not depend on `Link.update()`
consuming a queued speed. The `$30`/`$E8` pair also proves signed byte
interpretation in both axes.

- [ ] **Step 2: Update the source-wiring assertion before implementation**

Replace the existing body of
`mainAppliesHookshotPullEventsWithoutProjectileIgnoreSideEffects` in
`MainArchitectureTest.java` with:

```java
assertTrue(source.contains("EntityProjectileEvent.Kind.HOOKSHOT_PULL"));
assertTrue(source.contains("link.applyRomFinalPosition(event.linkSpeedX(), event.linkSpeedY())"));
assertTrue(source.contains("link.applyRomSpeed(event.linkSpeedX(), event.linkSpeedY())"));
```

This keeps both the immediate hookshot path and the queued non-hookshot path
visible in the architectural boundary.

- [ ] **Step 3: Run the focused tests and confirm the intended red state**

Run:

```sh
gradle -p java test --tests linksawakening.entity.LinkTest.appliesRomFinalPositionImmediatelyWhileMotionIsBlocked --tests linksawakening.MainArchitectureTest.mainAppliesHookshotPullEventsWithoutProjectileIgnoreSideEffects
```

Expected: compilation/test failure because `Link.applyRomFinalPosition` does
not yet exist and `Main` does not yet contain the new call.

### Task 2: Implement source-shaped immediate Link motion

**Files:**
- Modify: `java/src/main/java/linksawakening/entity/Link.java`
- Test: `java/src/test/java/linksawakening/entity/LinkTest.java`

- [ ] **Step 1: Add the minimal public operation**

Add this method immediately after `applyRomSpeed`:

```java
/**
 * Applies the speed bytes written by a handler that immediately calls
 * {@code UpdateFinalLinkPosition}, bypassing input and collision probes.
 */
public void applyRomFinalPosition(int speedX, int speedY) {
    if ((speedX & ~0xFF) != 0 || (speedY & ~0xFF) != 0) {
        throw new IllegalArgumentException("Link final-position speeds must be unsigned bytes");
    }
    subY += (byte) speedY;
    subX += (byte) speedX;
}
```

The existing sub-pixel representation stores the source's low fractional
position bits directly, so adding the signed speed byte is the same fixed
point operation already used by the normal movement path. The method applies
Y before X, as `UpdateFinalLinkPosition` does.

- [ ] **Step 2: Run the focused tests and confirm green**

Run:

```sh
gradle -p java test --tests linksawakening.entity.LinkTest.appliesRomFinalPositionImmediatelyWhileMotionIsBlocked
```

Expected: PASS, with the existing `LinkTest` cases still compiling and
passing.

### Task 3: Dispatch only hookshot pulls through the immediate path

**Files:**
- Modify: `java/src/main/java/linksawakening/Main.java:610-625`
- Test: `java/src/test/java/linksawakening/MainArchitectureTest.java`

- [ ] **Step 1: Change the event dispatch order**

Replace the event-body speed write with this exact branch:

```java
if (hookshotPull) {
    link.applyRomFinalPosition(event.linkSpeedX(), event.linkSpeedY());
    continue;
}
link.applyRomSpeed(event.linkSpeedX(), event.linkSpeedY());
link.setCollisionIgnoreFrames(event.linkIgnoreCollisionCountdown());
Sword reflectedSword = equipmentController.activeSword();
if (reflectedSword != null) {
    reflectedSword.resetSpinAttack();
}
```

Keep the surrounding condition that skips events without a Link response and
the existing `link == null` guard. This makes hookshot pulls immediate while
leaving laser/reflection countdown semantics unchanged.

- [ ] **Step 2: Run the boundary and hookshot regression tests**

Run:

```sh
gradle -p java test --tests linksawakening.MainArchitectureTest --tests linksawakening.world.HookshotChainMotionTest --tests linksawakening.world.RoomEntityRuntimeHookshotTest
```

Expected: PASS, including the existing event-vector and chain-state tests.

### Task 4: Verify the slice, record evidence, and commit

**Files:**
- Modify: `docs/reconstruction-roadmap.md`

- [ ] **Step 1: Run the complete test suite**

Run:

```sh
gradle -p java test
```

Expected: `BUILD SUCCESSFUL` with zero failed tests.

- [ ] **Step 2: Inspect repository hygiene**

Run:

```sh
git diff --check
git status --short --branch
git diff --stat HEAD~1
```

Expected: no whitespace errors; only the planned Link/Main tests,
implementation, and roadmap entry are changed after the spec/plan commits.

- [ ] **Step 3: Append the verified roadmap entry**

Add a dated section stating that hookshot `$01` now applies the inverse `$30`
vector immediately after the entity pass, that the active Hookshot remains
input-locking, and that non-hookshot response speeds remain queued. Include
the complete-suite command as the verification evidence.

- [ ] **Step 4: Commit the implementation**

```sh
git add java/src/main/java/linksawakening/entity/Link.java \
  java/src/main/java/linksawakening/Main.java \
  java/src/test/java/linksawakening/entity/LinkTest.java \
  java/src/test/java/linksawakening/MainArchitectureTest.java \
  docs/reconstruction-roadmap.md
git commit -m "feat: apply immediate hookshot pull motion"
```
