# Tail Cave Completion Implementation Plan

**Goal:** Extend fresh ordered play through Moldorm, Heart Container, Full Moon
Cello, and the source dungeon-exit warp.

### Task 1: Reach and defeat Moldorm

- [x] Cross room `$0B`'s opened north boss door into `$06`.
- [x] Tick Moldorm into its live dynamic presentation.
- [x] Strike only the final tail segment four times through normal combat.
- [x] Tick the full bank-$04 destruction sequence and assert room shutters open.

### Task 2: Collect the boss rewards

- [x] Collect the spawned Heart Container through its held-item countdown.
- [x] Assert room completion and reward publication.
- [x] Move north into instrument room `$02`.

### Task 3: Complete the instrument sequence

- [x] Collect the Full Moon Cello through normal pickup collision.
- [x] Drive award, dialog/music gates, performance, jingle, and warp countdowns.
- [x] Consume the instrument transition through the room coordinator.
- [x] Assert the resulting overworld room and persisted dungeon state.

### Task 4: Verify and commit

- [x] Run the focused ordered regression.
- [x] Run `gradle -p java clean test` and `git diff --check`.
- [x] Request source-fidelity review and address findings.
- [x] Commit the slice in `feature/entity-runtime`.
