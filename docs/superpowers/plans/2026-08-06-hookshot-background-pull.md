# Hookshot background interaction and pull Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the ROM `$03` hookshot chain use rich background physics, stop/poke walls, enter state `$01` on hookshotable objects, and pull Link through the existing post-entity event boundary.

**Architecture:** Keep all hookshot position/vector/state math in `HookshotChainMotion`. Let `RoomEntityRuntime` own the handler ordering and translate the rich `RoomEntityBackgroundInteraction` result into source-shaped state/events. Reuse `EntityCombatEvent` for wall audio/VFX and add one narrowly scoped `EntityProjectileEvent` kind for the forced Link speed write; defer bridge and dynamic OAM work.

**Tech Stack:** Java, JUnit 5, Gradle, shipped LADX ROM/disassembly, existing room/entity/audio/VFX event boundaries.

---

### Task 1: Extend the ROM-facing collision state and resolver exception

**Files:**
- Modify: `java/src/main/java/linksawakening/world/EntityBackgroundCollisionState.java`
- Modify: `java/src/main/java/linksawakening/world/EntityBackgroundCollisionResolver.java`
- Modify: `java/src/main/java/linksawakening/world/RoomSession.java:902-925`
- Test: `java/src/test/java/linksawakening/world/EntityBackgroundCollisionResolverTest.java`

- [x] **Step 1: Write the failing test** for a hookshot chain passing a switch-block sample while Link is standing on the block, while retaining blocked behavior when the standing flag is false.
- [x] **Step 2: Run the focused resolver test** with `gradle -p java test --tests linksawakening.world.EntityBackgroundCollisionResolverTest`; expected failure because collision state has no standing flag and the hookshot exception is absent.
- [x] **Step 3: Add the state field and source exception**. Preserve the existing four- and five-argument constructors as compatibility overloads, pass `overworldCollision.linkStandingOnSwitchBlock()` from `RoomSession`, and return a passable result only for entity `$03` plus the nonzero standing flag.
- [x] **Step 4: Run the focused resolver test** and verify it passes.

### Task 2: Add pure hookshot state, wall, and pull decisions

**Files:**
- Modify: `java/src/main/java/linksawakening/world/HookshotChainMotion.java`
- Modify: `java/src/test/java/linksawakening/world/HookshotChainMotionTest.java`

- [x] **Step 1: Write failing tests** for state `$01`, the `$26` hookshotable threshold, a deferred wall-pending state, inverse pull speeds including Z-aware Y distance, and zero pull speed when the source hitbox already overlaps Link.
- [x] **Step 2: Run the focused motion test** with `gradle -p java test --tests linksawakening.world.HookshotChainMotionTest`; expected failure because state/pull/wall APIs do not exist.
- [x] **Step 3: Implement the minimum pure state API**: add state and pending-wall fields with backward-compatible constructors, expose the `$26` threshold, create candidate outbound state transitions, expose a source-shaped pull-speed record, and provide state-copy helpers for pulling and wall-pending phases.
- [x] **Step 4: Run the focused motion test** and verify it passes without changing the existing fixed-point or return-vector assertions.

### Task 3: Add the hookshot pull event and audio mapping

**Files:**
- Modify: `java/src/main/java/linksawakening/world/EntityProjectileEvent.java`
- Modify: `java/src/main/java/linksawakening/gameplay/GameplaySoundEvent.java`
- Modify: `java/src/main/java/linksawakening/gameplay/GameplaySoundEffectMap.java`
- Modify: `java/src/main/java/linksawakening/gameplay/EnemyCombatEventConsumer.java`
- Test: `java/src/test/java/linksawakening/gameplay/EnemyCombatEventConsumerTest.java`

- [x] **Step 1: Write failing tests** for the raw hookshot pull event shape and `NOISE $0B` mapping to the hookshot gameplay sound.
- [x] **Step 2: Run the focused gameplay tests** with `gradle -p java test --tests linksawakening.gameplay.EnemyCombatEventConsumerTest`; expected failure because the event kind and sound enum/mapping do not exist.
- [x] **Step 3: Add the minimal event/audio definitions**. Keep pull speeds unsigned at the event boundary, leave collision-ignore at zero, and map the existing catalog entry `NOISE_SFX_HOOKSHOT` (`$0B`) without guessing a new ROM sound.
- [x] **Step 4: Run the focused gameplay tests** and verify they pass.

### Task 4: Route rich background results through the hookshot handler

**Files:**
- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java:429-505`
- Modify: `java/src/main/java/linksawakening/Main.java:608-623`
- Modify: `java/src/test/java/linksawakening/world/RoomEntityRuntimeHookshotTest.java`
- Test: `java/src/test/java/linksawakening/world/RoomEntityRuntimeHookshotTest.java`

- [x] **Step 1: Write failing runtime tests** for hookshotable state entry, point-blank unload at effective countdown `$26`, one-tick deferred wall poke with jingle/VFX, pulling Link event output, and the existing boolean collision fallback stopping rather than unloading the chain.
- [x] **Step 2: Run the focused runtime tests** with `gradle -p java test --tests linksawakening.world.RoomEntityRuntimeHookshotTest`; expected failures because the handler currently converts every collision to boolean and unloads immediately.
- [x] **Step 3: Implement the handler ordering**. Preserve the existing boolean adapter for other entity families, call the rich probe only for outbound hookshot state, roll back blocked positions, apply the `$60`/`$26` branch, service pending wall flags before movement, emit wall combat/VFX events, emit pull events from state `$01`, and enqueue the `$0B` noise event every fourth frame.
- [x] **Step 4: Update the main-loop event boundary** so `HOOKSHOT_PULL` writes Link's forced ROM speed without collision-ignore or sword-spin side effects.
- [x] **Step 5: Run the focused runtime and gameplay tests** and verify all pass.

### Task 5: Integrate room-session behavior and verify the whole branch

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomSessionTest.java`
- Modify: `docs/reconstruction-roadmap.md`

- [x] **Step 1: Add a room-session regression test** proving the configured rich probe reaches the hookshot handler with the real physics byte and that Link standing on a switch block makes the `$04` sample pass.
- [x] **Step 2: Run the room-session test** with `gradle -p java test --tests linksawakening.world.RoomSessionTest`; expected PASS after Tasks 1–4.
- [x] **Step 3: Record the verified slice and the bridge/dynamic-OAM deferrals** in the roadmap.
- [x] **Step 4: Run the complete suite** with `gradle -p java clean test`.
- [x] **Step 5: Run repository checks** with `git diff --check` and inspect `git diff --stat` plus the focused diff.
- [x] **Step 6: Commit** with `git add` for only the hookshot docs, Java source, and tests, then `git commit -m "feat: add ROM hookshot background pull"`.
