# Link damage modifiers and health-buffer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Route accepted enemy contact and projectile damage through the ROM's tunic/power-up modifiers and delayed health-buffer timing.

**Architecture:** Keep nominal damage in existing combat/projectile events. Make `PlayerState` the single ROM-facing boundary that converts nominal damage into effective buffered damage and records invincibility/power-up hits; drain that buffer from the existing odd-frame resource tick. Keep pit damage on the existing immediate path.

**Tech Stack:** Java 21, JUnit 5, Gradle, LADX assembly source.

---

### Task 1: Lock the PlayerState ROM contract with failing tests

**Files:**
- Modify: `java/src/test/java/linksawakening/state/PlayerStateTest.java`
- Reference: `LADX-Disassembly/src/code/entities/bank3.asm:5179-5370`
- Reference: `LADX-Disassembly/src/code/bank2.asm:5025-5165`

- [ ] **Step 1: Add modifier tests**

Write tests for the wished-for `applyRomEnemyDamage` API. Assert that Blue
Tunic changes nominal `$08` to effective `$04`, Guardian Acorn changes `$04`
to zero and `$08` to `$04`, and a raw shift turns nominal `$01` into zero.
Each accepted hit must set invincibility to `$50`; a zero-effective Guardian
Acorn hit still counts as accepted.

- [ ] **Step 2: Add buffer timing tests**

Assert that accepted damage leaves health unchanged immediately, that an even
resource tick leaves it unchanged, and that each odd tick consumes one damage
buffer point. Add tests proving pending healing is consumed first and that a
full-health healing buffer is cleared before one damage point is consumed on
the same odd tick.

- [ ] **Step 3: Add power-up hit-count tests**

Set a Piece of Power, accept two hits while clearing invincibility between
hits, and assert it remains active with two hits recorded. Accept a third hit
and assert the active power-up clears. Assert that setting a new power-up
resets the hit count.

- [ ] **Step 4: Run the focused tests and confirm RED**

Run `gradle test --tests linksawakening.state.PlayerStateTest` from `java/`.
Expected result: compilation fails because the new API does not exist yet.

### Task 2: Implement the ROM damage state in PlayerState

**Files:**
- Modify: `java/src/main/java/linksawakening/state/PlayerState.java`
- Test: `java/src/test/java/linksawakening/state/PlayerStateTest.java`

- [ ] **Step 1: Add ROM state fields and accessors**

Add byte-sized `subtractHealthBuffer` and `powerUpHits` fields, with readers
`subtractHealthBuffer()` and `powerUpHits()`. Add `setActivePowerUp(int)` using
the existing constants and reset `powerUpHits` at the pickup boundary. Route
pickup types `$33` and `$34` through that setter.

- [ ] **Step 2: Implement `applyRomEnemyDamage`**

Use the source order: Blue Tunic shifts first; otherwise Guardian Acorn makes
nominal `$04` zero and shifts all other nominal damage; add effective damage
modulo `$100`; set invincibility `$50`; then increment active-power-up hits
and clear the power-up at three. Return the effective value for tests and
leave the existing immediate `damage(int)` method unchanged.

- [ ] **Step 3: Extend the odd-frame health tick**

Keep the existing healing behavior. When healing is pending and health is
below max, consume one healing point and stop. When health is already full,
clear the healing buffer and continue to damage reduction. Then decrement the
subtract buffer and health by one when health is nonzero.

- [ ] **Step 4: Run the focused tests and confirm GREEN**

Run `gradle test --tests linksawakening.state.PlayerStateTest` from `java/`.
Expected result: all PlayerState tests pass.

### Task 3: Route both gameplay damage boundaries

**Files:**
- Modify: `java/src/main/java/linksawakening/Main.java:486-489`
- Modify: `java/src/main/java/linksawakening/gameplay/EnemyProjectileEventConsumer.java:46-55`
- Modify: `java/src/test/java/linksawakening/gameplay/EnemyProjectileEventConsumerTest.java`

- [ ] **Step 1: Change the projectile expectation first**

Update the accepted projectile test to assert health remains unchanged after
consumption, the subtract buffer contains `$08`, and hurt sound/invincibility
are unchanged. Tick frame `1` and assert health decreases to `15` and the
buffer becomes `7`. Keep the invincibility suppression assertion.

- [ ] **Step 2: Run the consumer test and confirm RED**

Run `gradle test --tests linksawakening.gameplay.EnemyProjectileEventConsumerTest`.
Expected result: the changed health assertion fails because the consumer still
calls `damage` immediately.

- [ ] **Step 3: Route accepted events through PlayerState**

Replace the direct `damage` and `setInvincibilityCounter` pair in the
projectile consumer and Main entity-contact loop with
`applyRomEnemyDamage(...)`. Preserve the existing positive-damage and
zero-invincibility gates so sound suppression and event acceptance remain
unchanged.

- [ ] **Step 4: Run focused gameplay tests**

Run `gradle test --tests linksawakening.gameplay.EnemyProjectileEventConsumerTest --tests linksawakening.state.PlayerStateTest`.
Expected result: PASS.

### Task 4: Verify, review, document, and checkpoint

**Files:**
- Modify: `docs/reconstruction-roadmap.md`
- Review: all files changed by Tasks 1-3

- [ ] **Step 1: Run complete verification**

From `java/`, run `gradle clean test`. Expected result: BUILD SUCCESSFUL.

- [ ] **Step 2: Run repository checks**

From the worktree root, run `git diff --check` and `git status --short`.
Expected result: no whitespace errors and only intended files changed.

- [ ] **Step 3: Update the roadmap**

Record that generic enemy contact/projectile damage now uses the ROM's Blue
Tunic, Guardian Acorn, power-up hit, and delayed health-buffer rules. Leave
pit damage, medicine, low-health audio, and entity-specific exceptions listed
as remaining work.

- [ ] **Step 4: Commit the checkpoint**

Commit the spec, plan, Java changes, tests, and roadmap update with:
`git commit -m "feat: apply ROM Link damage buffering"`.
