# Hookshot launch and outbound chain implementation plan

## 1. Specify the ROM contract

- [x] Capture source labels, direction order, guard conditions, sprite bytes,
  and fixed-point timing in the design spec.

## 2. Drive the behavior with tests

- [x] Test the pure chain-motion tables, `$30` fixed-point movement, return
  vector, and ROM hitbox boundary.
- [x] Test launch guards and Link motion lock through the equipped-item seam.
- [x] Test runtime spawn state, slot exhaustion, and duplicate-launch rejection.
- [x] Test the `$36/$36+XFLIP` sprite definition.

## 3. Implement the minimum runtime slice

- [x] Add `HookshotChainMotion`.
- [x] Add `Hookshot` and its target interface.
- [x] Add entity type `$03` sprite catalog support.
- [x] Spawn and advance the chain from `RoomEntityRuntime` and expose it
  through `RoomSession`.

## 4. Wire the normal game loop

- [x] Register the ROM hookshot item in `Main`.
- [x] Pass Link's current ROM coordinates/direction to the launch target.
- [x] Keep Link blocked until the runtime unloads the chain.

## 5. Verify and hand off

- [ ] Run focused tests, then `gradle -p java clean test`.
- [ ] Run `git diff --check` and inspect the final diff.
- [ ] Record the completed slice and the deferred pull/background work in the
  reconstruction roadmap.
- [ ] Commit the slice with a focused message.
