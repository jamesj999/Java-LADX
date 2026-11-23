# Bottle Grotto Genie Entry Plan

**Goal:** Traverse from room `$25` to `$2B` and port the Genie intro plus the
initial jar-break transition without implementing the full boss fight.

### Task 1: Extend the physical route to room `$2B`

- [x] Add failing ordered-route assertions for `$25 → $3B → $3A → $2D → $2B`.
- [x] Preserve room `$27`'s Nightmare Key state through the `$2D` boss door.
- [x] Assert room `$2A` is not used as a pre-boss instrument shortcut.
- [x] Assert Genie `$5C` at ROM `$48/$30` in room `$2B`.

### Task 2: Port Genie intro and jar state

- [x] Add failing `GenieMotionTest` and runtime integration tests.
- [x] Reuse `BossIntroMotion` for map `$01` music `$19` and dialog `$B4`.
- [x] Implement private state 0's threshold boundary, jar threshold `$03`, body spawn, smash, and
      noise `$29` from `04_genie.asm`.
- [x] Exclude Genie from generic enemy teardown while this custom lifecycle is
      active.

The active jar substates that make the threshold reachable through lifting and
throwing are intentionally deferred with the later Genie fight states. Runtime
integration tests inject private state 4 at the threshold boundary.

### Task 3: Review and verify

- [x] Run focused route/Genie tests.
- [x] Compare all implemented state transitions and spawn fields against the disassembly.
- [x] Run `gradle cleanTest test` and `git diff --check`.
- [ ] Commit the bounded milestone; leave later Genie states explicitly pending.
