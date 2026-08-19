# Bottle Grotto Power Bracelet Ordered-Play Design

## Goal

Continue the uninterrupted fresh-game Bottle Grotto session from the post-Hinox
warp destination in room `$36`, collect the required Small Key in room `$22`,
and obtain the Power Bracelet from room `$20` through ROM-authored movement,
door, enemy, event, chest, inventory, and persistence paths.

## Source route

The cleared miniboss warp is bidirectional. From its room `$36` endpoint,
entity `$61` selects the other `DungeonWarps` entry and returns Link to `$28`.
`MapLayout1` then defines the ordered route as `$28 -> $29 -> $26 -> $21 ->
$22`. After collecting the Small Key, Link returns west to `$21`, opens its
west key door, and enters `$20`.

The route reuses already-resolved state rather than manufacturing shortcuts:
room `$28` retains its miniboss-clear status, active warp, and open east door.
Every room boundary after the return warp must be reached through current
collision data. The solid north wall in room `$2E` is not traversable and must
not be bypassed with Feather logic. Room `$36`'s separate overworld front-door
macro defect is not on this forward route and remains a later dungeon-exit
milestone.

## Room `$22` Small Key

`IndoorsA22` contains visible chest object `$A0` at location `$27`, crystal
switch `$66` at `$24`, and event `$00`. `RoomChestsTable[$22]` selects
`CHEST_SMALL_KEY`. The ordered test must reach the chest through collision,
open it through `tryOpenChest`, consume the ordinary chest reward/dialog flow,
increase the dungeon Small Key count, persist the chest status, and confirm the
opened room state on reload.

The crystal switch and hearts are not progression requirements for this slice.
The test should assert their source presence without collecting or rewriting
them unless collision requires an interaction.

## Key door and room `$20`

`IndoorsA21` places the west key door at `$30,$EE`; `IndoorsA20` places its
synchronized east half at `$39,$EF`. The Small Key from `$22` is spent through
the shared directional key-door API. Door animation, motion blocking, dungeon
key decrement, room-status persistence, and the matching door state in both
rooms must follow the existing generic implementation.

`DungeonEventsTable[$20]` is `$61`: kill all enemies, then reveal a chest.
`IndoorsA20Entities` contains two Boo Buddies `$50` at locations `$24` and
`$45`. They must initialize, move, accept live sword collision, recover, and
die through their existing handler and shared death machinery. No test may
write enemy health, clear slots directly, or substitute generic enemy removal.

The room source contains `OBJECT_CHEST_OPEN` `$A1` at location `$28`.
`ConfigureRoomObjects` replaces that marker with the room floor while status
bit `$10` is clear; it is therefore a hidden-event marker, not evidence that
the reward begins collected. Fresh room status must omit an interactable chest
until the `$61` event resolves; the reveal effect writes the active chest at
`$28`, and subsequent configuration preserves the collected/open state. If
current Java behavior differs, fix the shared chest/event mechanism rather
than special-casing room `$20`.

## Power Bracelet reward

`RoomChestsTable[$20]` selects `CHEST_POWER_BRACELET`. Opening the revealed
chest must use the complete chest entity lifecycle: interaction direction,
animation, reward event, item presentation, dialog, teardown, and room-status
persistence. Applying the reward must give inventory item `$03` and raise the
fresh player's Power Bracelet level from `$00` to `$01`. Reloading room `$20`
must show the collected chest state without replaying the reward.

The milestone stops after the Bracelet is owned and persisted. Pot lifting,
room `$29`'s side-view staircase, Genie, the Nightmare Key, and the dungeon boss
belong to subsequent gameplay-order milestones.

## Architecture and error handling

Extend the existing ordered `RoomTransitionCoordinatorTest` session so keys,
door state, switch state, room events, and player inventory remain live across
the full route. Reuse `RoomSession`, `RoomBoundaryController`,
`RoomEntityRuntime`, `BooBuddyMotion`, `ChestContentsTable`, and
`PlayerState.applyChestReward`. Production changes are allowed only when a RED
ordered or focused test demonstrates a shared source mismatch.

All searches and gameplay waits must be bounded and end with explicit terminal
assertions. Missing collision paths, absent source entities, failed door
settlement, an unresolved event, or an incomplete chest lifecycle are test
failures; they must not be bypassed with arbitrary arena teleports, direct
status writes, or room-specific exceptions.

## Verification

- Run the extended ordered test RED before each production correction, then
  GREEN after the smallest source-aligned fix.
- Run focused Boo Buddy, room-event, chest, key-door, Link, and transition
  suites with `--rerun-tasks`.
- Obtain source-fidelity review before code-quality review and resolve every
  Critical or Important finding.
- Run `gradle clean test`, count JUnit XML tests/failures/errors/skips, inspect
  the full diff, and run `git diff --check` before committing the milestone.
