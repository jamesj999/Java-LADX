# ROM Color Shell Runtime Implementation Plan

> **Execution note:** Work through this plan in the current feature worktree,
> keeping each task RED → GREEN → refactor and running the focused tests after
> each task.

**Goal:** Make Color Shell entities `$E9-$EB` load ROM-backed rectangle art,
advance through bank-$36 states `0-$D`, and apply their room-object puzzle
side effects in the Java runtime.

**Architecture:** Add a dedicated `ColorShellMotion` helper with explicit
per-slot state, fixed-point accumulators, and a small `ColorShellWorld` effect
boundary. Extend `EntitySpriteHandlerCatalog` with six bank-$20 rectangle
families and refresh a shell's definition when its state/status changes
between active and inactive rendering groups. `RoomEntityRuntime` remains the
owner of slot status, combat countdowns, snapshots, and dispatch ordering;
`RoomSession` adapts active-room object storage and existing sound/VFX sinks to
the world boundary.

**ROM references:**

- `LADX-Disassembly/src/code/entities/bank36.asm:6130-6748`
- `LADX-Disassembly/src/code/entities/bank36.asm:7099-7180`
- `LADX-Disassembly/src/code/entities/bank36.asm:7220-7230`
- `LADX-Disassembly/src/code/bank20.asm:4526-4585`

## Task 1: Add the ROM-backed dynamic rectangle catalog

**Files:**

- Modify: `java/src/main/java/linksawakening/entity/EntitySpriteHandlerCatalog.java`
- Modify: `java/src/main/java/linksawakening/world/EntityRoomLoader.java`
- Test: `java/src/test/java/linksawakening/entity/EntitySpriteHandlerCatalogTest.java`
- Test: `java/src/test/java/linksawakening/world/EntityRoomLoaderTest.java`

1. Write failing tests for red, green, and blue Color Shell definitions:
   active state `0` selects bank `$20` addresses `$6688/$66E8/$6748` with
   eight variants and three rectangle sprites; inactive state/status selects
   `$67A8/$67D8/$6808` with four variants. Assert the first ROM tuples and a
   hidden `$FF` tile remain intact.
2. Add `forColorShellState(entityType, state, status)` with strict type/status
   validation and a single color-index table. Keep all addresses and variant
   counts in the catalog, not in the runtime.
3. Route `forEntityType` for `$E9-$EB` through the inactive definition used by
   the initial `INIT` snapshot. Add loader assertions that the three entities
   are supported rectangle definitions rather than unsupported placeholders.
4. Run the two focused test classes and commit:
   `feat: decode ROM Color Shell display lists`.

## Task 2: Add the Color Shell motion state holder and pure state tests

**Files:**

- Create: `java/src/main/java/linksawakening/world/ColorShellMotion.java`
- Test: `java/src/test/java/linksawakening/world/ColorShellMotionTest.java`

1. Write failing tests for initialization, random direction selection, the
   `$40` idle countdown, state-1 direction speed tables, `$16/$89` X and
   `$1E/$72` Y clamps, and the `$30`/`$20` Link proximity thresholds.
2. Add per-slot state, transition, direction, signed speed, fixed-point
   accumulators, Z state, sprite variant, harmless/mask flags, and initialized
   arrays. Implement clear/reset and package-visible accessors for exact test
   inspection.
3. Implement the bank-$36 fixed-point XY/Z helpers and the existing background
   collision callback contract. Preserve unsigned-byte wrapping and ROM
   signed-speed behavior.
4. Implement states `0-3`, including state-1 eight-frame variant cadence,
   state-2/3 even-frame toggle, direction remap `$02,$03,$01,$00`, and the
   transition to state 4 when ignore-hits is nonzero.
5. Run the focused motion tests and commit:
   `feat: port Color Shell movement states`.

## Task 3: Port harmless, jump, puzzle, and completion states

**Files:**

- Modify: `java/src/main/java/linksawakening/world/ColorShellMotion.java`
- Create: `java/src/main/java/linksawakening/world/ColorShellWorld.java`
- Test: `java/src/test/java/linksawakening/world/ColorShellMotionTest.java`

1. Write failing tests for states 4/5: clearing ignore hits, setting/retaining
   harmless physics, status `$06` gate, and returning to state 1 only when the
   ROM status is below stunned.
2. Add state 6/7 corner placement and the signed-Z crossing behavior.
3. Add `ColorShellWorld` methods for padded-room object read/write, raw
   jingle/noise, poof, and solved-shell unload. Supply a no-op implementation
   for pure/no-world tests.
4. Write failing tests for state 8 correct/incorrect object checks, color
   object IDs `$5E/$59/$63`, wrong-answer object writes `$5F/$5A/$64`, wrong
   jingle `$1D`, unlock noise `$04`, failed arc, state C all-shell gate,
   final writes, poof `$02`, and unload.
5. Implement states 8-D and the all-sixteen-slot color-shell scan with the
   exact active-status and state `< $0C` predicates.
6. Run focused tests and commit:
   `feat: port Color Shell puzzle states`.

## Task 4: Integrate runtime dispatch and dynamic definitions

**Files:**

- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
- Modify: `java/src/main/java/linksawakening/world/RoomSession.java`
- Modify: `java/src/main/java/linksawakening/world/ActiveRoom.java`
- Modify: `java/src/main/java/linksawakening/world/RoomTilemapBuilder.java` (only if a
  live object write needs a source-backed refresh)
- Test: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`
- Test: `java/src/test/java/linksawakening/world/RoomSessionTest.java`

1. Write failing integration tests proving a loaded Color Shell transitions
   from `INIT` to active motion, stores position/state/variant, and changes
   its rectangle definition at state/status boundaries.
2. Construct and reset `ColorShellMotion` in the runtime, initialize on the
   existing `INIT` pass, dispatch only on active frames, and apply its returned
   definition via `forColorShellState`.
3. Pass the world boundary from `RoomSession` without changing old tick
   overload behavior. Apply room-object writes to the active padded area and
   route raw sound/VFX/unload requests through existing gameplay boundaries.
4. Ensure clear, dynamic replacement, Zol split, projectile spawn, and room
   reload cannot inherit Color Shell state arrays.
5. Run focused integration tests and commit:
   `feat: integrate ROM Color Shell runtime`.

## Task 5: Verify and record the observable increment

**Files:**

- Modify: `docs/reconstruction-roadmap.md`

1. Run the focused catalog, loader, motion, runtime, and session tests.
2. Run `gradle clean test` from `java/` and inspect failures rather than
   weakening assertions or substituting guessed art/behavior.
3. Run `git diff --check` and inspect the final diff for accidental changes.
4. Add a dated roadmap entry listing the bank-$36 states and the exact ROM
   rectangle families now covered. Explicitly record any live renderer/audio
   boundary that remains deferred.
5. Commit the roadmap and verification record:
   `docs: record ROM Color Shell runtime`.

