# Opening Order Playtest Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the canonical new-game house and beach sequence the immediately runnable development playtest path.

**Architecture:** Route configured `NEW_GAME` startup through the existing `Main.startNewGame()` bootstrap. Keep source gameplay behavior in the existing Marin, Tarin, room-session, owl, and sword components; add only startup selection and cross-component verification.

**Tech Stack:** Java 17, JUnit 5, Gradle, JSON resource configuration.

---

### Task 1: Select the canonical new-game bootstrap

**Files:**
- Modify: `java/src/main/java/linksawakening/startup/StartupCoordinator.java`
- Modify: `java/src/main/java/linksawakening/Main.java`
- Test: `java/src/test/java/linksawakening/startup/StartupCoordinatorTest.java`
- Test: `java/src/test/java/linksawakening/MainFileMenuFlowTest.java`

- [ ] Add a failing test that `ItemProfile.NEW_GAME` selects dedicated new-game gameplay while `DEBUG_ALL_ITEMS` does not.
- [ ] Add `StartupCoordinator.shouldStartNewGameGameplay(AppConfig)` as the single startup decision.
- [ ] Route `startConfiguredGameplay()` through `startNewGame()` when that decision is true.
- [ ] Run `gradle test --tests linksawakening.startup.StartupCoordinatorTest --tests linksawakening.MainFileMenuFlowTest` and require `BUILD SUCCESSFUL`.

### Task 2: Make opening gameplay the checked-in playtest default

**Files:**
- Modify: `java/src/main/resources/config/config.json`
- Test: `java/src/test/java/linksawakening/config/AppConfigTest.java`

- [ ] Add a resource-config test asserting title off, intro story off, and `NEW_GAME` profile.
- [ ] Change the checked-in config to those values while retaining debug controls and the existing key map.
- [ ] Run `gradle test --tests linksawakening.config.AppConfigTest` and require `BUILD SUCCESSFUL`.

### Task 3: Verify opening order through live boundaries

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomSessionTest.java`
- Modify: `java/src/test/java/linksawakening/state/PlayerStateTest.java`

- [ ] Extend the live beach test to apply the sword reward to `PlayerState` and assert sword level and inventory ownership.
- [ ] Assert the owl and sword set independent `$20` and `$10` room-status bits before room reload removes both entities.
- [ ] Run the focused room-session and player-state tests and require `BUILD SUCCESSFUL`.

### Task 4: Verification and commit

**Files:**
- Review all files changed above against the existing new-game bootstrap and `LADX-Disassembly` entity sources.

- [ ] Run `gradle test` and require `BUILD SUCCESSFUL`.
- [ ] Run `git diff --check` and inspect the complete diff.
- [ ] Commit with `git commit -m "feat: make opening sequence directly playtestable"`.

