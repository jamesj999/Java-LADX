# Bottle Grotto Room `$38` Design

## Goal

Continue the uninterrupted fresh-game Bottle Grotto regression from the
Compass chest in room `$37` into room `$38`, and prove the room's ROM-authored
Moblin Sword, crystal switch, switch blocks, and Small Key chest through normal
runtime paths.

## Source of truth

- `MapLayout1` places room `$38` immediately east of `$37` and room `$39`
  immediately east of `$38`.
- `IndoorsA38` uses `ROOM_TEMPLATE_RIGHT_BOTTOM_LEFT`, contains chest `$A0` at
  location `$43`, and contains the `$DB` switch-block arrangement.
- `IndoorsA38Entities` contains Moblin Sword `$14` at `$62` and crystal switch
  `$66` at `$45`.
- `DungeonEventsTable[$38]` is `$00`; killing the Moblin must not manufacture a
  room event or gate the chest.
- The indoor-A chest contents entry for room `$38` is `CHEST_SMALL_KEY` `$1A`.
  Room `$39` also contains a Small Key; the Power Bracelet is in room `$20`.

## Considered approaches

1. **Complete room `$38` in the ordered regression (selected).** Enter through
   the real east boundary, validate the room, defeat the Moblin through shared
   combat, collect the visible Small Key through the complete chest lifecycle,
   and exercise the crystal switch. This is the smallest slice that proves the
   whole room without misrepresenting later progression.
2. **Stop after entering `$38`.** This would prove map adjacency but leave its
   gameplay interactions unverified.
3. **Continue through `$39` toward the Power Bracelet.** This spans multiple
   rooms and risks obscuring whether failures belong to `$38`, `$39`, or the
   later route to room `$20`.

## Runtime flow

The existing ordered-play test keeps its live `RoomSession`, `Link`,
`PlayerState`, frame counter, switch state `$00`, and one Small Key after the
room `$37` Compass. It walks collision-validly across the east boundary into
room `$38` and asserts event `$00`, the exact two source entities, chest `$A0`
at `$43`, and the loaded `$DB` cells.

The Moblin is initialized and defeated through `tickEntities` and
`resolveEntityCombat`, with bounded recovery and death waits. Its removal must
leave event `$00` unchanged. The test then moves Link through room collision to
the chest's real upward interaction point, opens it through `tryOpenChest`, and
ticks the spawned chest entity until reward, dialog, and presentation teardown
complete. Applying the emitted reward raises the dungeon Small Key count from
one to two and persists the chest-open room bit.

Finally, Link reaches the live crystal switch through collision, strikes it
with the ordinary sword collision path, and advances the nine gameplay VBlanks.
The test asserts stage/SFX behavior, global state `$00 -> $02`, and synchronized
Link collision for representative `$DB` cells. Room `$39` remains the next
collision-accessible frontier; crossing it is outside this slice.

## Production changes

No production change is assumed. Existing room loading, Moblin Sword motion,
generic combat/death, chest reward, crystal-switch animation, and switch-block
collision paths should support the room. TDD applies if the ordered regression
exposes a real shared-runtime mismatch: preserve the failing assertion, confirm
the source behavior, then make the smallest general fix rather than adding a
room-specific exception.

## Verification

- Run the extended ordered-play test with `--rerun-tasks`.
- Run focused room transition, Moblin Sword, chest, switch-block, and room
  runtime suites.
- Run `gradle clean test`, count JUnit XML totals, and run `git diff --check`.
- Obtain source-fidelity review followed by code-quality review, resolving all
  Critical and Important findings before commit.
