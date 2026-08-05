# Enemy Sword-Hit Response Implementation Plan

> Execute each task test-first: add the failing test, run the focused test and
> record the RED result, implement the smallest ROM-faithful change, rerun for
> GREEN, then run the relevant regression tests before the next task.

**Goal:** Add the shared bank-3 Octorok/Moblin sword-hit response—vector-away
recoil, fixed-point movement, countdown interaction, and final ROM jingle
feedback—to the Java room-entity runtime without adding emulator code or
invented VFX.

**Design:** `EnemyRecoilMotion` owns the ROM's per-slot recoil velocities and
subpixel accumulators. `RoomEntityRuntime` configures it at the existing sword
collision point and applies it before the roaming handler. `EntityCombatEvent`
returns the final raw sound write, and a gameplay consumer maps the two known
jingle IDs to the existing sound sink.

**Required references:**

- `docs/superpowers/specs/2026-08-05-enemy-sword-response-design.md`
- `LADX-Disassembly/src/code/entities/bank3.asm`
- `LADX-Disassembly/src/code/home/entities.asm`
- `LADX-Disassembly/src/data/entities/`

## Task 1: Add the pure ROM recoil state machine

**Files:**

- Create: `java/src/test/java/linksawakening/world/EnemyRecoilMotionTest.java`
- Create: `java/src/main/java/linksawakening/world/EnemyRecoilMotion.java`

1. Add failing tests for `ConfigureEntityRecoil`'s vector in horizontal,
   vertical, diagonal, equal-axis, zero-distance, and Z-adjusted-Y cases.
   Assert the signed recoil speeds point away from Link and use the ROM's
   infinity-norm division behavior.
2. Add failing fixed-point tests for speed `$30` and negative speeds. Verify
   the first movement, fractional accumulation, unsigned position wrapping,
   X-before-Y ordering, and null background collision behavior.
3. Add a failing wall test with a deterministic blocking callback. Verify the
   blocked coordinate is restored, the active recoil is stopped, and the
   dominant recoil-axis direction is passed to the callback.
4. Run:

   ```bash
   gradle test --tests linksawakening.world.EnemyRecoilMotionTest
   ```

   Confirm RED because the class does not exist.
5. Implement only the helper. Use the exact ROM signed-byte/fractional math;
   do not use floating point or normalized vectors. Make `clear` idempotent.
6. Rerun the focused test for GREEN and commit:

   ```bash
   git add java/src/main/java/linksawakening/world/EnemyRecoilMotion.java \
     java/src/test/java/linksawakening/world/EnemyRecoilMotionTest.java
   git commit -m "feat: add ROM enemy recoil motion"
   ```

## Task 2: Configure recoil and raw sound at the runtime combat boundary

**Files:**

- Modify: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`
- Modify: `java/src/main/java/linksawakening/world/EntityCombatEvent.java`
- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`

1. Add failing Octorok and Moblin tests that hit a two-health Moblin with the
   sword rectangle. Assert the first hit sets `$18` flash, `$0A` ignore,
   configures a nonzero vector-away recoil, leaves health at one, and returns
   the raw JINGLE `$03` request. Assert a lethal hit still returns the final
   enemy-hit request and enters DYING.
2. Add a failing test for a sword collision that does not apply damage (using
   a direct event/constructor or a supported zero-damage branch if available)
   and assert the raw JINGLE `$09` bump request. Preserve the current public
   event accessors with a no-sound default where appropriate.
3. Run the focused runtime tests and confirm RED.
4. Add `EnemyRecoilMotion` state to `RoomEntityRuntime`. At the existing sword
   hit point, configure `$30` recoil for the shared roaming enemy path before
   applying health changes, matching the order in `EnemyCollidedWithSword`.
   Keep the existing flash/ignore gates and Zol/special-family behavior
   unchanged.
5. Apply recoil in the active non-initialized tick before the roaming motion
   receives its entity. Reuse the existing ignore countdown: entities with
   active recoil decrement it as `ApplyRecoilIfNeeded_03` does; other entities
   retain the existing generic countdown behavior. Clear recoil on all existing
   slot cleanup, split, and projectile replacement paths.
6. Extend `EntityCombatEvent` with a validated raw sound channel/ID and make
   the combat path emit JINGLE `$03` after a basic sword damage, otherwise
   JINGLE `$09` for a standard sword collision.
7. Run the focused runtime tests for GREEN and commit:

   ```bash
   git add java/src/main/java/linksawakening/world/EntityCombatEvent.java \
     java/src/main/java/linksawakening/world/RoomEntityRuntime.java \
     java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java
   git commit -m "feat: apply ROM enemy sword recoil"
   ```

## Task 3: Route raw combat jingles through gameplay audio

**Files:**

- Create: `java/src/main/java/linksawakening/gameplay/EnemyCombatEventConsumer.java`
- Create: `java/src/test/java/linksawakening/gameplay/EnemyCombatEventConsumerTest.java`
- Modify: `java/src/main/java/linksawakening/gameplay/GameplaySoundEvent.java`
- Modify: `java/src/main/java/linksawakening/gameplay/GameplaySoundEffectMap.java`
- Modify: `java/src/main/java/linksawakening/Main.java`
- Modify: `java/src/test/java/linksawakening/gameplay/SfxGameplaySoundSinkTest.java`

1. Add failing consumer tests for JINGLE `$09`, JINGLE `$03`, unknown sound,
   and no-sound events. Assert only the two known gameplay sounds are sent.
2. Add failing sound-map assertions for `JINGLE_BUMP` and
   `JINGLE_ENEMY_HIT` using the shipped ROM catalog.
3. Run the focused gameplay tests and confirm RED.
4. Implement the consumer and map entries, then invoke it immediately after
   `RoomSession.resolveEntityCombat` in the existing gameplay update boundary.
   Keep Link damage handling unchanged and do not let unknown raw sound IDs
   reach the sink.
5. Run the focused tests for GREEN and commit:

   ```bash
   git add java/src/main/java/linksawakening/gameplay \
     java/src/main/java/linksawakening/Main.java \
     java/src/test/java/linksawakening/gameplay
   git commit -m "feat: route enemy sword hit sounds"
   ```

## Task 4: Regression and evidence checkpoint

1. Add or update tests proving recoil state is cleared by `clearEntity`, room
   unload, DYING cleanup, and dynamic slot reuse.
2. Run the focused suites:

   ```bash
   gradle test --tests linksawakening.world.EnemyRecoilMotionTest \
     --tests linksawakening.world.RoomEntityRuntimeTest \
     --tests linksawakening.gameplay.EnemyCombatEventConsumerTest \
     --tests linksawakening.gameplay.SfxGameplaySoundSinkTest
   ```

3. Run the full clean suite:

   ```bash
   gradle clean test
   ```

4. Inspect `git diff --check`, confirm the worktree contains only the design,
   plan, implementation, and tests for this increment, update
   `docs/reconstruction-roadmap.md` with exact source anchors and known
   non-goals, and request an independent code review before claiming the slice
   complete.
