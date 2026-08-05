# Leever recoil Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mirror Leever's bank-$04 shared `$30` sword-recoil path through the live Java room runtime.

**Architecture:** Keep `EnemyRecoilMotion` as the ROM-shaped fixed-point recoil state. Admit only entity `$0E` to the existing shared-recoil dispatch, using the non-clearing-on-background-block policy already used by bank-$06 handlers; leave `LeeverMotion`'s separate state machine unchanged.

**Tech Stack:** Java 21, Gradle, JUnit 5, LADX disassembly, shipped ROM-backed combat tables.

---

### Task 1: Add failing Leever recoil regressions

**Files:**

- Modify: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java` beside the existing Leever tests.

- [ ] **Step 1: Add the normal recoil test.** Add:

```java
@Test
void leeverSwordHitConfiguresBankFourRecoilBeforeItsStateMovement() {
    EntitySpriteDefinition definition = pairDefinition(0x0E, 4);
    RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
        new RoomEntity(0, 0, 0x0E, 64, 64, EntityStatus.ACTIVE, definition, 0)));

    List<EntityCombatEvent> events = runtime.resolveCombat(
        0, 120, 120, false, true, true, 72, 1, 72, 1);

    assertEquals(1, events.size());
    assertEquals(EntityCombatEvent.SoundChannel.JINGLE, events.get(0).soundChannel());
    assertEquals(0x03, events.get(0).soundId());
    assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
    assertEquals(1, runtime.enemyHealth(0));
    assertEquals(0x0A, runtime.enemyIgnoreHitsCountdown(0));
    assertTrue(runtime.enemyRecoilActive(0));
    assertEquals(0xD0, runtime.enemyRecoilSpeedX(0));
    assertEquals(0xD0, runtime.enemyRecoilSpeedY(0));

    runtime.tick(1, 120, 120, sequence(0x00));

    RoomEntity afterRecoil = runtime.snapshot().slots().get(0);
    assertEquals(61, afterRecoil.x());
    assertEquals(61, afterRecoil.y());
    assertEquals(0x09, runtime.enemyIgnoreHitsCountdown(0));
}
```

- [ ] **Step 2: Add the blocked recoil test.** Add:

```java
@Test
void leeverKeepsBankFourRecoilWhenBackgroundBlocksTheStep() {
    EntitySpriteDefinition definition = pairDefinition(0x0E, 4);
    RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
        new RoomEntity(0, 0, 0x0E, 64, 64, EntityStatus.ACTIVE, definition, 0)));

    runtime.resolveCombat(0, 72, 72, false, true, true, 72, 1, 72, 1);
    RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) ->
        direction == 1 || direction == 2;

    runtime.tick(1, 120, 120, sequence(0x00), wall);

    assertEquals(64, runtime.snapshot().slots().get(0).x());
    assertEquals(64, runtime.snapshot().slots().get(0).y());
    assertTrue(runtime.enemyRecoilActive(0));
    assertEquals(0x09, runtime.enemyIgnoreHitsCountdown(0));
}
```

- [ ] **Step 3: Run focused tests and verify RED.** From `java/`, run:

```bash
gradle test --tests linksawakening.world.RoomEntityRuntimeTest
```

Expected: the two Leever tests fail because `usesSharedRecoil` currently
excludes entity `$0E`; existing runtime tests continue to pass.

### Task 2: Admit Leever to the shared ROM recoil path

**Files:**

- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java` in `usesSharedRecoil`/`usesBank6Recoil`.

- [ ] **Step 1: Add the source-specific type admission.** Add Leever to the
shared-recoil predicate without changing the bank-$03 blocked-step policy:

```java
private static boolean usesSharedRecoil(int type) {
    return type == ENTITY_LEEVER || isRoamingEnemyType(type) || usesBank6Recoil(type);
}
```

Keep `isRoamingEnemyType` unchanged. `applyEnemyRecoilIfNeeded` will therefore
pass `clearOnBlocked == false` to `EnemyRecoilMotion.advance` for Leever, while
Octorok/Moblin continue clearing recoil on a blocked step.

- [ ] **Step 2: Run focused tests and verify GREEN.** Run:

```bash
gradle test --tests linksawakening.world.RoomEntityRuntimeTest
```

Expected: both Leever regressions and all existing runtime tests pass. The
normal tick must apply recoil before `LeeverMotion.advance`; the blocked tick
must restore position while retaining the `$09` countdown and active recoil.

### Task 3: Document and checkpoint the increment

**Files:**

- Modify: `docs/reconstruction-roadmap.md` in the Leever status bullet and before `## Next entity increments`.
- Modify: `docs/superpowers/plans/2026-08-05-leever-recoil.md` to mark completed steps.

- [ ] **Step 1: Update the Leever roadmap status.** Change the Leever bullet
to state that its bank-$04 shared `$30` recoil is verified below while generic
background interaction and remaining damage behavior remain pending.

- [ ] **Step 2: Add a dated verification entry.** Insert:

```markdown
## Verified ROM Leever recoil — 2026-08-05

- Leever (`$0E`) now enters the shared ROM `$30` recoil path after a normal
  sword hit, before its bank-$04 hide/emerge/chase/burrow state movement.
- The runtime decrements the `$0A` ignore-hits countdown and preserves recoil
  after a blocked step, matching `ApplyRecoilIfNeeded_04` rather than the
  bank-$03 roaming stop-on-collision helper.
- Focused Leever regressions and the complete Java suite cover this increment.
  Generic wall/ground/water/pit/conveyor interaction, recoil smoke, and
  remaining damage-state branches remain pending.
```

- [ ] **Step 3: Run final verification.** From `java/`, run `gradle clean test`.
From the worktree root, run `git diff --check` and `git status --short --branch`.
Expected: `BUILD SUCCESSFUL`, no whitespace errors, and only the intended
source, test, roadmap, and plan files changed before commit.

- [ ] **Step 4: Commit the checkpoint.** Mark all plan checkboxes complete and
commit:

```bash
git add java/src/main/java/linksawakening/world/RoomEntityRuntime.java \
  java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java \
  docs/reconstruction-roadmap.md \
  docs/superpowers/plans/2026-08-05-leever-recoil.md \
  docs/superpowers/specs/2026-08-05-leever-recoil-design.md
git commit -m "feat: add Leever bank4 recoil"
```
