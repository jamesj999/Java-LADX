# Bottle Grotto Genie Entry Plan

**Goal:** Traverse from room `$25` to `$2B` and port the Genie intro plus the
initial jar-break transition without implementing the full boss fight.

### Task 1: Extend the physical route to room `$2B`

- [ ] Add failing ordered-route assertions for `$25 → $3B → $3A → $2D → $2B`.
- [ ] Preserve room `$27`'s Nightmare Key state until the `$2D` boss door.
- [ ] Assert room `$2A` is not used as a pre-boss instrument shortcut.
- [ ] Assert Genie `$5C` at ROM `$48/$30` in room `$2B`.

### Task 2: Port Genie intro and jar state

- [ ] Add failing `GenieMotionTest` and runtime integration tests.
- [ ] Reuse `BossIntroMotion` for map `$01` music `$19` and dialog `$B4`.
- [ ] Implement private state 0, jar threshold `$03`, body spawn, smash, and
      noise `$29` from `04_genie.asm`.
- [ ] Exclude Genie from generic enemy teardown while this custom lifecycle is
      active.

### Task 3: Review and verify

- [ ] Run focused route/Genie tests.
- [ ] Compare all state transitions and spawn fields against the disassembly.
- [ ] Run `gradle cleanTest test` and `git diff --check`.
- [ ] Commit the bounded milestone; leave later Genie states explicitly pending.
