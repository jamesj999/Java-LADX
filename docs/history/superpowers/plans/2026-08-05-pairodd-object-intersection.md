# Pairodd projectile object-intersection implementation plan

## 1. Lock ROM predicates with tests

- Add physics-flag boundary tests for the ordinary Pairodd object-intersection
  predicate.
- Add a runtime test proving the projectile is removed after movement when the
  padded room cell contains a colliding object, and that the existing
  sword-poke request is emitted once.
- Add a passable-object test proving Link/sword projectile checks still run
  when the sampled object flag is passable.

## 2. Add the source-backed room query

- Extend `OverworldCollision` with the exact `$7CAB` coordinate/index sample
  and selected ROM physics-table lookup.
- Keep the padded `$11` base and `$10` row stride; do not flatten the room into
  an 8x10 buffer.
- Keep overlay data out of this query.

## 3. Wire the live entity boundary

- Add an optional object-collision callback to the internal entity tick path,
  preserving old overloads for tests and callers without a loaded room.
- Run it after Pairodd movement and before the existing Link and sword checks.
- On collision, remove the slot and queue the same one sword-poke VFX request
  used by the ROM's final `$5F1B-$5F28` branch.

## 4. Verify and document

- Run focused Pairodd/object tests, `git diff --check`, and `gradle clean test`.
- Update the reconstruction roadmap with the covered source range and the
  remaining special thrown-object cases.
- Commit only after the clean suite passes.
