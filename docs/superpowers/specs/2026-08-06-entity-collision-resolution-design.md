# Shared entity-background collision resolution design

**Status:** Approved under the standing pixel-perfect reconstruction objective

## Goal

Make the shared entity/background probe follow the ROM's
`ApplyEntityCollisionWithObject` decision boundary for the collision cases
that can be resolved from the current Java room state. This will correct
fine-collision shapes and entity-specific terrain behavior for the existing
rich collision path used by roaming enemies.

## Source behavior

The source of truth is `LADX-Disassembly/src/code/entities/bank3.asm`:

- `EntityCollisionPointsX/Y` at `$03:$785F/$786F` selects one pixel on the
  leading edge of an entity.
- `FineCollisionShapes` at `$03:$7A85` contains four bytes for each physics
  group `$7C` through `$8D`. The four entries represent the four 8×8
  quadrants of the sampled 16×16 object cell.
- `ApplyEntityCollisionWithObject` at `$03:$7ACD` first handles the object
  physics byte, then applies water, pit/lava, open-door/fine-shape, and broad
  physics-category rules.
- `CollisionsTableFlagPerDirection` at `$03:$787F` remains represented by
  `EntityBackgroundCollisionResult`; blocked results must carry exactly the
  direction bit selected by the probe.

The Java collision point calculation is already verified against the ROM and
must remain unchanged. `OverworldCollision` remains the Link/ground policy;
entity collision must not reuse its `pointBlocked` decision because Link and
entities intentionally disagree about water and several terrain categories.

## Architecture

Add a package-private `EntityBackgroundCollisionResolver` in
`linksawakening.world`. It will take `RomTables` in its constructor and expose
one pure resolution method receiving:

```text
RoomEntity entity
int direction
EntityCollisionPointProbe.Sample sample
int objectId
int physicsFlag
```

The method will return an `EntityBackgroundCollisionResult` containing the
original object id, physics byte, direction, and sampled coordinates. It will
not read or mutate room state. `RoomSession.entityBackgroundCollisionResult`
will continue to obtain the sample and room object through
`EntityCollisionPointProbe`/`OverworldCollision`, then delegate the decision
to this resolver.

The resolver will implement these source-backed rules:

1. Fish (`$CC`) and Water Tektite (`$99`) pass only through shallow/deep water
   and collide with every other physics value, including `NONE`.
2. Ordinary entities pass through `NONE` and through physics values below the
   blocking entity categories, including shallow/deep water. Lava, normal
   pits, and pit-warps block while the entity is grounded (`z == 0`) and pass
   while airborne (`z != 0`).
3. Physics `$7C`–`$8D` uses the ROM fine-shape table. A zero table entry is
   passable; a nonzero entry blocks. Open-door values `$7C`–`$7F` use the same
   table, while the source's Spark and boss exceptions treat them as solid.
   The source range check also admits `$8E`/`$8F`, but the disassembly defines
   no shape rows for those values; the Java resolver will treat them as
   passable rather than reading beyond the ROM table. Bomb (`$02`) and
   wrecking ball (`$A8`) pass through fine-collision objects.
4. Ledge, switch-block, hookshotable, and other broad physics categories are
   handled only where their current static meaning is unambiguous: solid
   entity categories and tractor devices block; physics `$A0`–`$FE` passes
   except the `$D0`–`$D3` ledge range, which uses a blocking fallback;
   `NO_WALL_COLLISION` entities pass after the source-specific forced-solid
   exceptions. Switch-block objects use a blocking fallback until their WRAM
   state is exposed. Stateful ledge timers, switch-block state, and
   hookshot-chain transitions are deferred until the required WRAM state is
   available at the boundary.

The resolver will use `RomTables.entityOptions1` for the ROM
`ENTITY_OPT1_NO_WALL_COLLISION` bit and the existing entity type values for
the water, bomb, wrecking-ball, Spark, and boss exceptions. No guessed object
id switch will be added to `OverworldCollision`.

## Testing

Tests will be test-first and source-shaped:

- Extend `RomTablesTest` with a synthetic ROM assertion that the fine table is
  loaded from the bank/address above and that its physics groups are exposed
  without signed-byte corruption.
- Add `EntityBackgroundCollisionResolverTest` with a synthetic table/entity
  fixture. Assert all four `(sampleX bit 3, sampleY bit 3)` quadrants, zero vs
  nonzero shape entries, broad passable/solid physics, airborne pits, water
  entity exceptions, bomb/wrecking-ball fine pass-through, and open-door
  Spark/boss solidity.
- Add a room-session regression using the shipped ROM and a live roaming enemy
  probe through the same `entityBackgroundCollisionResult` boundary, proving
  a deep-water sampled cell is no longer treated as an ordinary entity wall
  while a solid sampled cell still blocks.

Focused tests must pass before the full Java suite. Verification also
includes `git diff --check` and a fresh `gradle -p java clean test`.

## Scope boundary

This slice does not claim full entity collision parity. It intentionally does
not invent the ROM's `wEntitiesIgnoreHitsCountdownTable`, thrown-direction /
ledge timer, `wSwitchBlocksState`, or hookshot-chain transition state. Those
will be separate source-backed increments once the runtime exposes those WRAM
values to the rich collision boundary.
