# Plan: shared entity collision results

## Scope

Implement the design in `docs/superpowers/specs/2026-08-06-entity-collision-result-design.md`. The first implementation slice is the shared result boundary plus the bank-$03 roaming consumer; it is not a wholesale rewrite of all entity collision branches.

## Steps

1. Add result and interaction types.
   - Define the four source direction values and collision flags in one place.
   - Keep a passable result explicit and normalize all byte-valued fields.
   - Add a boolean-to-result adapter so existing tests and handlers need no signature migration.

2. Expose the sampled room object from `OverworldCollision`.
   - Reuse its padded room-buffer addressing and active ROM physics table.
   - Do not reuse Link-only `idBlocks` as a new entity policy.

3. Write failing roaming tests first.
   - Inject one rich probe that blocks one direction and returns a known object/physics result.
   - Verify the movement coordinate is retained, the exact collision bit is recorded, and the next dispatch consumes it with the source `$10..$1F` countdown.
   - Verify the existing boolean overload still behaves the same.

4. Wire the result through `RoamingEnemyMotion` and `RoomEntityRuntime`.
   - Preserve the current overloads.
   - Keep collision recording after movement and collision consumption at the next state-0 entry.
   - Install the rich callback from both `RoomSession` entity-runtime construction paths.

5. Convert `RoomSession`'s entity query to a result-producing implementation.
   - Use the ROM-selected collision-point sample.
   - Preserve the current Water Tektite pass-through exception for this slice.
   - Return raw object ID, physics byte, sample coordinates, and source direction flag.

6. Verify and document.
   - Run focused tests after the intentional red phase.
   - Run `git diff --check`, `gradle clean test`, and plain `gradle test` from `java`.
   - Add a dated roadmap entry and commit the isolated worktree.

## Follow-up slices

After this plan, migrate the exact entity collision policy into a shared resolver, then port handler-specific consumers of directional flags/object IDs. Fine collision shapes, ledges, switch blocks, pit/warp exceptions, and the generic post-handler rollback boundary must each be validated against their corresponding disassembly paths before being enabled globally.
