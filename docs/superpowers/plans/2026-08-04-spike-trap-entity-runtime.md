# ROM-Driven Spike Trap Entity Runtime — Implementation Plan

## Goal

Port the disassembly-backed Spike Trap (`ENTITY_SPIKE_TRAP`, `$27`) entity slice into the existing Java room runtime. The slice will decode its bank-$06 pair display list, apply its random-direction initialization, reproduce its four-state launch/return loop and fixed-point movement, expose the state for verification, and use the ROM health/damage group in the existing combat pass.

The work is intentionally bounded. The disassembly's shared background routine has broader effects (ground status, pits, water, conveyors, object side effects) that are not yet represented by the Java room model. This increment will preserve the Spike Trap's `hActiveEntityNoBGCollision` behavior as a collision signal and leave those shared effects for a separate subsystem increment.

## Source-of-truth mapping

- `LADX-Disassembly/src/code/entities/06_spike_trap.asm`
  - display list: bank `$06`, address `$74FA`, one pair variant;
  - initialization: `EntityInitWithRandomDirection`;
  - state tables: `$74FE`, `$7502`, `$7506`, `$750A`;
  - state transitions: state `0` captures X/visual-Y, state `1` waits for Link alignment and launches, state `2` advances for `$18`/`$10` frames, state `3` returns to the saved coordinates.
- `LADX-Disassembly/src/data/entities/hitbox_flags.asm`: `$27` is a big enemy collision box with `HITFLAGS_IGNORE_HITS`.
- `LADX-Disassembly/src/data/entities/health.asm`: health group `$09` gives 4 initial HP.
- `LADX-Disassembly/src/data/entities/damages.asm`: health group `$09` gives 8 Link-contact damage.
- `LADX-Disassembly/src/code/entities/bank6.asm`: signed byte distances, direction values `right=0`, `left=1`, `up=2`, `down=3`, and the 16-frame fixed-point position update.

## Design

Add a package-private `SpikeTrapMotion` parallel to the existing handler motion classes. It owns one state array per entity slot for state, transition countdown, direction, X/Y speeds, fractional accumulators, and the two private saved coordinates. The runtime initializes the direction exactly once on the INIT pass, then dispatches the active handler only on subsequent ticks, matching the current runtime convention used by other ROM handlers.

The motion implementation will:

- use unsigned-byte wrapping for positions, timers, speeds, and accumulators;
- use signed-byte Link-to-entity distances and the source's `distance + $12 < $24` window;
- use the source tables without replacing them with direction guesses;
- preserve the state-3 comparison quirk: private state 2 stores visual Y, while the source compares it with raw entity Y;
- update X then Y with the bank-$06 fixed-point algorithm;
- call the existing background query as a collision gate, without rolling back movement or adding unsupported shared-ground side effects, because the ROM handler sets `hActiveEntityNoBGCollision`;
- leave audio (`NOISE_SFX_WHOOSH`, `JINGLE_SWORD_POKING`), drops, recoil, and death effects to their existing/future shared systems.

Add `$27` to `EntitySpriteHandlerCatalog` and decode the pair list from the ROM. Add `$27` to the combat rule tables with the big-enemy hitbox, 4 HP, and 8 contact damage. Wire initialization, advancement, clearing, and package-level accessors through `RoomEntityRuntime`.

## TDD execution steps

### 1. Establish failing display and combat contracts

1. Extend `EntitySpriteHandlerCatalogTest` with the `$27` descriptor: bank `$06`, address `$74FA`, pair shape, one variant, and the shipped-ROM bytes `$50/$02` and `$50/$22`.
2. Add runtime tests that create a `$27` entity and assert:
   - INIT consumes the random direction byte and enters active state 0;
   - state 0 captures X/visual-Y and advances to state 1;
   - horizontal alignment selects the direction-indexed X speed and `$18` countdown;
   - vertical fallback selects the direction-indexed Y speed and `$10` countdown;
   - state 2 uses the ROM fixed-point movement and enters state 3 after countdown expiry;
   - state 3 returns to the saved coordinate and re-enters state 1 with `$20` countdown;
   - a background collision signal enters the same return phase without fabricating movement rollback;
   - `$27` contact damage is 8, initial health is 4, and sword damage decrements that health.
3. Run the focused tests before adding production support and confirm the failure is the missing handler/motion contract.

### 2. Implement the smallest ROM-backed production slice

1. Add the `$27` display-list mapping.
2. Add `SpikeTrapMotion` with exact source tables, state transitions, countdown timing, direction distance semantics, and fixed-point helper.
3. Wire the motion into `RoomEntityRuntime` initialization, active dispatch, clearing, and test accessors.
4. Add `$27` to `RoomEntityCombatRules`.
5. Rerun the focused catalog/runtime tests until green.

### 3. Refactor and verify

1. Compare the motion code against the handler source line-by-line, especially countdown decrement ordering, the state-1 proximity branches, state-3 saved-Y comparison, and collision-gate behavior.
2. Run the entity/world test packages and then the complete Java suite from `java/`.
3. Run `git diff --check` and inspect the diff for guessed art, generated entity data, emulator code, or unrelated edits.
4. Update `docs/reconstruction-roadmap.md` with the verified Spike Trap coverage and the remaining shared-background/audio/recoil/death gaps.

## Completion criteria

- The Spike Trap definition and display bytes come from the shipped ROM through the existing catalog decoder.
- The four-state movement and fixed-point behavior are covered by focused tests and use no emulator layer.
- Combat constants match the disassembly's health/damage tables.
- Focused, relevant-package, and full Gradle tests pass.
- The roadmap reports this as a completed focused handler increment, not full game parity.
