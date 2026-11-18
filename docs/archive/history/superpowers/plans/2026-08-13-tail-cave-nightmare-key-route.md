# Tail Cave Nightmare Key Route Implementation Plan

**Goal:** Extend ordered play with room `$16`'s third Small Key, then continue
from Roc's Feather through room `$09`'s keyhole block, collect the Nightmare
Key, and open Tail Cave's boss door with the acquired state.

**Architecture:** Extend the existing fresh-game regression and keep production
behavior behind its current source-shaped APIs: `Link` owns Feather movement,
`RoomSession` owns room events/chests/doors/interactive blocks, and
`PlayerState` consumes rewards as `Main` does. Keep the broad path harness for
room navigation while proving the actual Feather gate with live Link physics.

### Task 1: Add strict gate connectivity

- [x] Add a dedicated path check in which normal pits are unavailable ground,
      without changing the broad fine-collision route harness.
- [x] Prove room `$04`'s south side is disconnected before the real jump.

### Task 2: Supply the source third key

- [x] Enter room `$16` from the dungeon entrance.
- [x] Knock both Hardhat Beetles into the pit through live sword recoil.
- [x] Implement event `$81`'s ROM-positioned key drop and collect it normally.
- [x] Port `EntityFallHandler`'s dedicated four-frame ROM display sequence.

### Task 3: Add the post-Feather regression

- [x] Backtrack through `$1C/$01/$18/$19/$03` using live boundary and warp
      handling.
- [x] Follow `$04/$07/$0D/$0E/$0F` into room `$09`.
- [x] Assert room `$04`'s south side cannot be reached as ordinary ground.
- [x] Cross the room-$04 pit field using held input, the equipped
      `RocsFeather` handler, and `Link.update()`.
- [x] Port bank 2's `$20`-tick keyhole-block interaction, key consumption,
      floor reveal, sound, and room-status persistence.
- [x] Continue west from `$09` into room `$08`.
- [x] Open location `$14`, tick the chest entity, consume its reward event,
      and assert the live Nightmare Key count.

### Task 4: Prove the key reaches the boss door

- [x] Continue through `$09/$0F/$10` to room `$11` after the persisted keyhole
      block removal, using live Feather physics across room `$09`'s `$B0` pit.
- [x] Defeat the live Rolling Bones entity through combat and tick its source
      destruction countdown until the miniboss event opens the shutters.
- [x] Move north to room `$0B`.
- [x] Exercise the source north-door collision branch.
- [x] Tick the door animation and assert the open objects and synchronized
      room-status bits.

### Task 5: Verify and commit

- [x] Run the focused ordered regression.
- [x] Run `gradle -p java clean test`.
- [x] Inspect the diff and worktree status.
- [x] Commit the source-shaped slice.
