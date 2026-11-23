# Plan: ROM-selected entity collision probe

## 1. Add characterization tests

- Add a ROM fixture test for `HitboxFlagsForEntity`, `EntityCollisionPointsX`,
  and `EntityCollisionPointsY`.
- Add probe tests covering normal, small, big, and Spark collision boxes in
  all four movement directions.
- Run the focused tests and confirm the new probe tests fail before the
  implementation exists.

## 2. Implement the table-backed probe

- Extend `RomTables` with the source addresses, ROM loading, and accessors.
- Add `EntityCollisionPointProbe` with the source coordinate math and safe
  normal-box fallback.
- Replace `RoomSession`'s hardcoded normal/Spark branches with the probe.

## 3. Verify and hand off

- Run focused tests, `git diff --check`, `gradle clean test`, and plain
  `gradle test`.
- Update the reconstruction roadmap with the exact scope and the deferred
  generic rollback/flag work.
- Commit the completed slice on `feature/entity-runtime`.
