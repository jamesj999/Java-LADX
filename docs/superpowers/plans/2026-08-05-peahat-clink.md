# PeaHat airborne sword-clink Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans (recommended) to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mirror PeaHat's bank-$07 airborne sword-clink path through the live
Java room combat runtime.

**Architecture:** Keep `PeaHatMotion.isGrounded` as the state boundary. Allow
airborne PeaHat sword rectangles through combat while suppressing only Link
contact, and extend the existing generic sword-poke predicate to the dynamic
airborne PeaHat state. Reuse the existing clink event/VFX/countdown branch.

**Tech Stack:** Java 21, Gradle, JUnit 5, LADX disassembly.

---

### Task 1: Add the failing airborne-clink regression

**Files:**

- Modify: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`
  beside the existing PeaHat combat tests.

- [ ] **Step 1: Add the airborne sword-clink test.** Add:

```java
@Test
void peaHatUsesTheRomSwordClinkPathWhileAirborne() {
    EntitySpriteDefinition definition = pairDefinition(0xA0, 2);
    RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(
        new RoomEntity(0, 0, 0xA0, 64, 64, EntityStatus.ACTIVE, definition, 0)));

    runtime.tick(0, 120, 120, sequence(0x00));
    assertEquals(1, runtime.peaHatState(0));

    List<EntityCombatEvent> events = runtime.resolveCombat(
        1, 120, 120, false, false, true, 72, 1, 72, 1);

    assertEquals(1, events.size());
    EntityCombatEvent event = events.get(0);
    assertEquals(0xA0, event.type());
    assertTrue(event.swordHit());
    assertEquals(0, event.linkDamage());
    assertEquals(0, event.enemyDamage());
    assertEquals(EntityCombatEvent.SoundChannel.JINGLE, event.soundChannel());
    assertEquals(0x07, event.soundId());
    assertEquals(new EntityCombatEvent.SwordPokeVfx(0x40, 0x40),
        event.swordPokeVfx());
    assertEquals(1, runtime.enemyHealth(0));
    assertEquals(EntityStatus.ACTIVE, runtime.snapshot().slots().get(0).status());
    assertEquals(0x10, runtime.enemyIgnoreHitsCountdown(0));
    assertFalse(runtime.enemyRecoilActive(0));
}
```

- [ ] **Step 2: Run focused tests and verify RED.** From `java/`, run:

```bash
gradle test --tests linksawakening.world.RoomEntityRuntimeTest
```

Expected: the new test fails because the current combat loop rejects
airborne PeaHats before evaluating their sword rectangle; existing PeaHat and
spike-trap tests continue to pass.

### Task 2: Admit airborne PeaHat to the shared clink branch

**Files:**

- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java` in
  the PeaHat combat gate and clink predicate call.
- Modify: `java/src/main/java/linksawakening/world/RoomEntityCombatRules.java`
  to accept the dynamic clink state without changing static spike-trap rules.

- [ ] **Step 1: Separate airborne contact from sword eligibility.** Compute
  the PeaHat grounded state before collision tests. Skip the entity only when
  it is airborne and neither a Link-contact nor a sword test could be useful;
  more directly, gate `linkCollision` on the grounded state while allowing
  `swordHit` to evaluate for airborne PeaHat.

- [ ] **Step 2: Route dynamic clink state through the existing branch.** Extend
  the sword-poke predicate with an explicit dynamic flag, and pass
  `entity.type() == ENTITY_PEAHAT && !peaHatGrounded`. Do not add PeaHat to
  normal recoil or damage in the clink case; the existing branch owns that
  response.

- [ ] **Step 3: Run focused tests and verify GREEN.** Run:

```bash
gradle test --tests linksawakening.world.RoomEntityRuntimeTest
```

Expected: the airborne clink regression, grounded/airborne contact test,
existing spike-trap clink tests, and all other runtime tests pass.

### Task 3: Document and checkpoint the increment

**Files:**

- Modify: `docs/reconstruction-roadmap.md` in the PeaHat status bullet and
  before `## Next entity increments`.
- Modify: this plan to mark completed steps.

- [ ] **Step 1: Update the PeaHat roadmap status.** State that airborne
  hitbox/contact separation and sword-clink response are verified below while
  generic ground/water/pit/conveyor behavior remains pending.

- [ ] **Step 2: Add a dated verification entry.** Insert:

```markdown
## Verified ROM PeaHat airborne sword clink — 2026-08-05

- Airborne PeaHat now suppresses Link contact while retaining sword-rectangle
  eligibility, matching its dynamic hitbox and
  `ENTITY_OPT1_SWORD_CLINK_OFF` flags.
- A colliding sword uses the shared ROM clink branch: no enemy damage or
  recoil, jingle `$07`, sword-poke VFX, and a `$10` ignore-hits window.
- Grounded PeaHat damage/recoil remains unchanged. Ground/water/pit/conveyor
  interaction and other clink-off entity handlers remain pending.
```

- [ ] **Step 3: Run final verification.** From `java/`, run `gradle clean test`.
  From the worktree root, run `git diff --check` and
  `git status --short --branch`. Expected: `BUILD SUCCESSFUL`, no whitespace
  errors, and only the intended source, test, roadmap, and plan files changed
  before commit.

- [ ] **Step 4: Commit the checkpoint.** Mark all plan checkboxes complete and
  commit:

```bash
git add java/src/main/java/linksawakening/world/RoomEntityRuntime.java \
  java/src/main/java/linksawakening/world/RoomEntityCombatRules.java \
  java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java \
  docs/reconstruction-roadmap.md \
  docs/superpowers/plans/2026-08-05-peahat-clink.md \
  docs/superpowers/specs/2026-08-05-peahat-clink-design.md
git commit -m "feat: add PeaHat airborne sword clink"
```
