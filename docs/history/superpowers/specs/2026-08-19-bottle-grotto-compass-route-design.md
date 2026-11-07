# Bottle Grotto Compass Route Ordered-Play Design

## Scope

Extend the uninterrupted fresh-game regression from room `$34`'s second Small
Key along the collision-valid source route back through rooms `$33` and `$32`,
unlock `$32`'s bottom key door, enter room `$37`, defeat its Masked Mimic, and
collect the revealed Compass chest.

## Source behavior

- Room `$34`'s vertical `$A6` partition separates the event-key alcove from the
  east corridor, so room `$35` is not the next reachable room. `MapLayout1`
  instead supports backtracking west through `$33` to `$32`.
- `IndoorsA32` contains bottom key door macro `$ED` at location `$74`. Opening
  it consumes one Small Key, runs the ordinary eight-frame door animation, and
  synchronizes room `$32`'s down-door status with room `$37`'s up-door status.
- `MapLayout1` places room `$37` directly south of `$32`.
- `DungeonEventsTable[$37]` is `$61`:
  `TRIGGER_KILL_ALL_ENEMIES | EFFECT_REVEAL_CHEST`.
- `IndoorsA37Entities` contains one Masked Mimic (`$8F`) at source location
  `$54` and two droppable rupees. The rupees are excluded from kill-all.
- The Masked Mimic uses the already verified bank-$19 mirrored-input and
  dynamic `$48/$08` combat path. Two vulnerable sword hits complete its normal
  death path.
- The shared room-event effect reveals a chest; the ROM-backed indoor-A chest
  table entry for room `$37` is `CHEST_COMPASS` (`$17`). Opening and presenting
  it must set Bottle Grotto's Compass dungeon-item flag through the existing
  chest runtime.

## Architecture

Continue the same `RoomSession`, `Link`, `PlayerState`, collision map, frame
counter, and dungeon inventory. Use `walkToAndCrossIndoorBoundary` for both west
backtracks and the final south transition. Use `tryUnlockIndoorKeyDoor` at the
loaded bottom-door collision probe, tick its ordinary animation, and verify the
directional status bits before crossing.

In room `$37`, drive the Mimic's live direction/options with source-shaped Link
coordinates and input, then apply combat through `resolveEntityCombat`. Do not
clear its slot, rewrite health, or resolve event `$61` directly. Let the shared
death/event path reveal the chest, open it through `tryOpenChest`, and advance
the spawned chest entity through its ordinary presentation/award lifecycle.

## Invariants

- No Link teleport or seeded corridor coordinate may bypass room collision.
- Both westward backtracks and the southward key-door crossing must begin from
  the current live Link position and use reachable-boundary search.
- The key count changes from two to one only when the bottom door accepts the
  correct collision/direction pair.
- Event `$61` remains active until the Masked Mimic dies; the two rupees do not
  block completion.
- Every death, reveal, or chest-presentation wait is bounded and followed by an
  explicit assertion.

## Verification

The slice is complete when uninterrupted ordered play backtracks to `$32`,
spends one key on the bottom door, persists both door sides, enters `$37`,
resolves event `$61` through the real Mimic death path despite the rupees,
reveals and opens the ROM-authored Compass chest, and records the Compass flag.
Focused transition, key-door, Mimic, chest, room-event, and full Java suites
must remain green.
