# Hookshot Chain and Indoor Bridge Rendering Plan

## Goal

Implement the approved design in
`docs/superpowers/specs/2026-08-06-hookshot-chain-rendering-design.md` so the
Java runtime exposes the ROM's visible hookshot chain links and indoor bridge
rewrites without introducing non-ROM graphics data.

## Implementation steps

### 1. Add the pure chain OAM model and tests

Files:

- `java/src/main/java/linksawakening/world/HookshotChainOam.java`
- `java/src/test/java/linksawakening/world/HookshotChainOamTest.java`

Write tests first. Cover wrapped signed deltas, three-link accumulation,
raw-coordinate offsets, tile `$24`, zero attributes, and the ROM cadence for
each of the three loop counts.

Run the focused test and confirm it fails before implementation:

```sh
gradle -p java test --tests linksawakening.world.HookshotChainOamTest
```

Implement the immutable value model, then rerun the focused test.

### 2. Carry chain OAM through snapshots and render it

Files:

- `java/src/main/java/linksawakening/world/RoomEntitySnapshot.java`
- `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
- `java/src/main/java/linksawakening/render/EntityRenderLayer.java`
- focused snapshot/runtime/render tests under `java/src/test/java/`

Add a backward-compatible snapshot field and have the runtime recompute the
chain entries from the active hookshot entity and Link position after each
tick. Render visible entries with raw OAM origin conversion and the live GPU
tile `$24`. Test that regular hookshot sprite rendering remains intact and
that dynamic entries are emitted/cleared with the chain.

### 3. Add exact padded-room object lookup and bridge motion tests

Files:

- `java/src/main/java/linksawakening/world/RoomEntityObjectQuery.java`
- `java/src/main/java/linksawakening/world/RoomEntityObjectSample.java`
- `java/src/main/java/linksawakening/world/HookshotBridgeMotion.java`
- corresponding tests under `java/src/test/java/linksawakening/world/`

Write tests first for the `$11`/`$10` padded lookup contract and the bridge's
`$30`/`$D0` fixed-point movement, including carry and the no-physics clear
condition. Keep the lookup callback independent from `RoomSession` so runtime
tests can provide deterministic samples.

### 4. Integrate bridge trigger and entity `$68` into `RoomEntityRuntime`

Files:

- `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
- `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`

Add bridge state, spawn handling, post-step `$9E`/`$9F` detection, bridge
motion, and side-effect records. Verify that the chain triggers only after a
successful passable step, uses the expected direction, and does not trigger
from a blocked background collision. Verify side effects are cleared per tick
and bridge slots/snapshots remain compatible with existing entity behavior.

### 5. Apply bridge mutations in `RoomSession`

Files:

- `java/src/main/java/linksawakening/world/RoomSession.java`
- `java/src/main/java/linksawakening/world/ActiveRoom.java` (only if the
  existing replacement boundary requires it)
- room/session tests under `java/src/test/java/linksawakening/world/`

Wire the object query to the active room's exact ground/object sample. Consume
runtime bridge requests by writing `$9D` to the padded object area, rebuilding
the ROM-backed tilemap, and reapplying persistent direction-specific tile
overrides with bounds guards. Clear overrides when switching rooms. Test with
the shipped ROM and existing room setup paths.

### 6. Verify and document the completed slice

Run:

```sh
gradle -p java test --tests linksawakening.world.HookshotChainOamTest
gradle -p java test --tests linksawakening.world.HookshotBridgeMotionTest
gradle -p java test --tests linksawakening.world.RoomEntityRuntimeTest
gradle -p java test
```

Inspect the diff and worktree, update the project roadmap with the verified
hookshot rendering behavior, and record the exact test output before claiming
completion. Commit the isolated branch only after all tests pass.
