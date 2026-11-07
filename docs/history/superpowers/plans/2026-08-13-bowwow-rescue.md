# BowWow Rescue Implementation Plan

**Goal:** Extend fresh ordered play from Tail Cave completion through BowWow's
source-authored kidnapping warning, Moblin Hideout, and rescue.

### Task 1: Prove the post-Tail Cave overworld order

- [x] Walk from room `$D3` to `$D2` and complete the instrument-gated owl event.
- [x] Continue to Mabe room `$B0` and assert Kid 71's `Dialog220` warning.
- [x] Follow a collision-valid overworld route to the hideout entrance in room `$35`.
- [x] Enter map `$15`, room `$F0`, through the ROM warp.

### Task 2: Correct direct indoor-map transitions

- [x] Add a failing regression for map `$15`'s `$F0 -> $E0` north transition.
- [x] Mirror `RoomTransitionPrepareHandler`: layout maps use `$08` vertically;
  direct room-id maps use `$10` vertically.
- [x] Preserve the existing layout-backed dungeon behavior.

### Task 3: Defeat the Moblin King and rescue BowWow

- [x] Traverse `$E0 -> $E1` and assert the source Moblin King is active.
- [x] Defeat the king through normal sword collision and persist the room event.
- [x] Traverse to `$E2`, touch kidnapped BowWow, and assert state `$01`.
- [x] Assert BowWow reconstructs as Link's follower after leaving the rescue room.

### Task 4: Verify and commit

- [x] Run the focused ordered regression and transition tests.
- [x] Run `gradle -p java clean test` and `git diff --check`.
- [x] Request source-fidelity review and address findings.
- [ ] Commit the slice in `feature/entity-runtime`.
