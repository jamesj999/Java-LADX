# PeaHat recoil Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mirror PeaHat's bank-$07 shared `$30` sword-recoil path through the
live Java room runtime.

**Architecture:** Keep `EnemyRecoilMotion` as the ROM-shaped fixed-point
recoil state. Admit only entity `$A0` to the existing shared-recoil dispatch,
using the non-clearing-on-background-block policy already used by non-roaming
handlers. Leave `PeaHatMotion`'s rest/takeoff/flying state machine unchanged.

**Tech Stack:** Java 21, Gradle, JUnit 5, LADX disassembly, ROM-backed combat
tables.

---

### Task 1: Add failing PeaHat recoil regressions

**Files:**

- Modify: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`
  beside the existing PeaHat tests.

- [ ] **Step 1: Add the normal recoil test.** Use a grounded `$A0` fixture and
  a level-zero `EnemyAttackContext` so the one-health test entity remains
  active long enough for the handler tick to consume recoil:

```java
@Test
void peaHatSwordHitConfiguresBankSevenRecoilBeforeItsStateMovement() {
    EntitySpriteDefinition definition = pairDefinition(0xA0, 2);
    EnemyAttackContext nonDamaging =
        new EnemyAttackContext(0, false, false, false, false);
    RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
        new RoomEntity(0, 0, 0xA0, 64, 64, EntityStatus.ACTIVE, definition, 0)),
        false, sequence(0x01), null, new RomEnemyCombatTables(loadRom()));

    List<EntityCombatEvent> events = runtime.resolveCombat(
        0, 120, 120, false, false, true, 72, 1, 72, 1, nonDamaging);

    assertEquals(1, events.size());
    assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
    assertEquals(0x0A, runtime.enemyIgnoreHitsCountdown(0));
    assertTrue(runtime.enemyRecoilActive(0));
    assertEquals(0xD0, runtime.enemyRecoilSpeedX(0));
    assertEquals(0xD0, runtime.enemyRecoilSpeedY(0));

    runtime.tick(1, 72, 72, sequence(0x01));

    RoomEntity afterRecoil = runtime.snapshot().slots().get(0);
    assertEquals(61, afterRecoil.x());
    assertEquals(61, afterRecoil.y());
    assertEquals(0x09, runtime.enemyIgnoreHitsCountdown(0));
}
```

- [ ] **Step 2: Add the blocked recoil test.** Use the same fixture and
  callback as the existing non-roaming bank-$06 recoil regressions:

```java
@Test
void peaHatKeepsBankSevenRecoilWhenBackgroundBlocksTheStep() {
    EntitySpriteDefinition definition = pairDefinition(0xA0, 2);
    EnemyAttackContext nonDamaging =
        new EnemyAttackContext(0, false, false, false, false);
    RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
        new RoomEntity(0, 0, 0xA0, 64, 64, EntityStatus.ACTIVE, definition, 0)),
        false, sequence(0x01), null, new RomEnemyCombatTables(loadRom()));

    runtime.resolveCombat(0, 120, 120, false, false,
        true, 72, 1, 72, 1, nonDamaging);
    RoomEntityBackgroundCollision wall = (entity, direction, nextX, nextY) ->
        direction == 1 || direction == 2;

    runtime.tick(1, 72, 72, sequence(0x01), wall);

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

Expected: the two new tests fail because `usesSharedRecoil` currently
excludes entity `$A0`; the existing PeaHat state and grounded/airborne tests
continue to pass.

### Task 2: Admit PeaHat to the shared ROM recoil path

**Files:**

- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java` in
  `usesSharedRecoil`.

- [ ] **Step 1: Add the source-specific type admission.** Add PeaHat without
  changing `usesBank6Recoil`, `isRoamingEnemyType`, or the blocked-step policy:

```java
private static boolean usesSharedRecoil(int type) {
    return type == ENTITY_LEEVER || type == ENTITY_PEAHAT
        || isRoamingEnemyType(type) || usesBank6Recoil(type);
}
```

`applyEnemyRecoilIfNeeded` will therefore pass `clearOnBlocked == false` for
PeaHat, matching `ApplyRecoilIfNeeded_07`; Octorok and Moblin retain their
bank-$03 stop-on-collision behavior.

- [ ] **Step 2: Run focused tests and verify GREEN.** Run:

```bash
gradle test --tests linksawakening.world.RoomEntityRuntimeTest
```

Expected: both PeaHat regressions and all existing runtime tests pass. The
normal tick must apply recoil before `PeaHatMotion.advance`; the blocked tick
must restore position while retaining recoil and the `$09` countdown.

### Task 3: Document and checkpoint the increment

**Files:**

- Modify: `docs/reconstruction-roadmap.md` in the PeaHat status bullet and
  before `## Next entity increments`.
- Modify: this plan to mark completed steps.

- [ ] **Step 1: Update the PeaHat roadmap status.** State that grounded
  sword-recoil through the shared bank-$07 path is verified below while
  hitbox/clink, background, and water behavior remain pending.

- [ ] **Step 2: Add a dated verification entry.** Insert:

```markdown
## Verified ROM PeaHat recoil — 2026-08-05

- PeaHat (`$A0`) now enters the shared ROM `$30` recoil path after a grounded
  sword hit, before its bank-$07 rest/takeoff/flying state movement.
- The runtime decrements the `$0A` ignore-hits countdown and preserves recoil
  after a blocked step, matching `ApplyRecoilIfNeeded_07` rather than the
  bank-$03 roaming stop-on-collision helper.
- Focused PeaHat regressions and the complete Java suite cover this increment.
  Hitbox-flag and sword-clink plumbing, generic background/water behavior,
  recoil smoke, and remaining damage-state branches remain pending.
```

- [ ] **Step 3: Run final verification.** From `java/`, run `gradle clean test`.
  From the worktree root, run `git diff --check` and
  `git status --short --branch`. Expected: `BUILD SUCCESSFUL`, no whitespace
  errors, and only the intended test, runtime, roadmap, and plan files changed
  before commit.

- [ ] **Step 4: Commit the checkpoint.** Mark all plan checkboxes complete and
  commit:

```bash
git add java/src/main/java/linksawakening/world/RoomEntityRuntime.java \
  java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java \
  docs/reconstruction-roadmap.md \
  docs/superpowers/plans/2026-08-05-peahat-recoil.md \
  docs/superpowers/specs/2026-08-05-peahat-recoil-design.md
git commit -m "feat: add PeaHat bank7 recoil"
```
