# Witch Toadstool Exchange Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port the ROM-faithful witch/toadstool exchange so opening progression awards Magic Powder exactly through the original state sequence.

**Architecture:** Add a focused `WitchMotion` state machine whose inputs and outputs mirror the WRAM values touched by `WitchEntityHandler`. Keep orchestration in `RoomEntityRuntime`, durable player mutations in `PlayerState`, and host-side dialog/audio/palette consumption in `RoomSession` and `Main`.

**Tech Stack:** Java 17, JUnit 5, Gradle, ROM-backed entity display-list decoding.

---

### Task 1: ROM-backed witch presentation

**Files:**
- Modify: `java/src/main/java/linksawakening/entity/EntitySpriteHandlerCatalog.java`
- Test: `java/src/test/java/linksawakening/entity/EntitySpriteHandlerCatalogTest.java`

- [ ] Add a failing test asserting entity `$40` decodes four rectangle variants from bank `$05:$4780`.
- [ ] Run `gradle test --tests linksawakening.entity.EntitySpriteHandlerCatalogTest` and confirm the witch definition is unsupported.
- [ ] Add the `$40` catalog constant and `decodeRectangle(entityType, 0x05, 0x4780, 4, 4, 0)` dispatch.
- [ ] Re-run the focused test and confirm it passes.

### Task 2: Witch state machine

**Files:**
- Create: `java/src/main/java/linksawakening/world/WitchMotion.java`
- Create: `java/src/test/java/linksawakening/world/WitchMotionTest.java`

- [ ] Add failing tests for animation, talk geometry, no-toadstool dialog, wrong equipped-button rejection, and both valid item-button pairings.
- [ ] Run `gradle test --tests linksawakening.world.WitchMotionTest` and confirm compilation fails because `WitchMotion` is absent.
- [ ] Implement per-slot states 0-7 and an immutable update record carrying dialog, block, inventory-clear, music, reward, jingle, and palette-effect outputs.
- [ ] Add failing timing tests for countdowns `$08` and `$C0`, dialog order `$009/$0FE/$17E`, and reward timing.
- [ ] Implement the minimum source-ordered transitions and re-run the focused tests.

### Task 3: Runtime and player integration

**Files:**
- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
- Modify: `java/src/main/java/linksawakening/world/RoomSession.java`
- Modify: `java/src/main/java/linksawakening/state/PlayerState.java`
- Modify: `java/src/main/java/linksawakening/Main.java`
- Test: `java/src/test/java/linksawakening/world/RoomSessionTest.java`
- Test: `java/src/test/java/linksawakening/state/PlayerStateTest.java`

- [ ] Add failing tests that load indoor-B `$A2`, drive the exchange through the session, and assert inventory removal, toadstool clearing, 20 powder uses, Link blocking, dialogs, music, jingle, and palette requests.
- [ ] Add a failing player-state test for clearing only the equipped powder slot at exchange start and assigning/replenishing powder at reward.
- [ ] Run focused tests and verify failures identify missing APIs/behavior.
- [ ] Wire live state inputs and typed outputs through runtime/session/Main, applying player mutations only at source-equivalent transitions.
- [ ] Re-run focused tests until green.

### Task 4: Verification and commit

**Files:**
- Review all files changed above against `LADX-Disassembly/src/code/entities/05_witch.asm` and `05__helpers_1.asm`.

- [ ] Run `gradle test` and require `BUILD SUCCESSFUL`.
- [ ] Run `git diff --check` and inspect the complete diff for source-order or persistence mistakes.
- [ ] Commit the milestone with `git commit -m "feat: implement witch toadstool exchange"`.
