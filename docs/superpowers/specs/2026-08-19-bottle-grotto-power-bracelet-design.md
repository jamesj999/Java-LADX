# Bottle Grotto Power Bracelet Ordered-Play Design

## Goal

Continue the uninterrupted fresh-game Bottle Grotto session from the post-Hinox
warp destination in room `$36`, return through room `$21`, and obtain the Power
Bracelet from room `$20` through ROM-authored movement,
door, enemy, event, chest, inventory, and persistence paths.

## Source route

The cleared miniboss warp is bidirectional. From its room `$36` endpoint,
entity `$61` selects the other `DungeonWarps` entry and returns Link to `$28`.
`MapLayout1` then defines the ordered route as `$28 -> $29 -> $26 -> $21 ->
$20`. Before the Hinox passage, the ordered route collects room `$39`'s
step-button chest Small Key; room `$35` consumes the preceding key, leaving
that room `$39` key available after the warp. Room `$21`'s west key door
consumes it through the ordinary animation before Link crosses left into
room `$20`.

The route reuses already-resolved state rather than manufacturing shortcuts:
room `$28` retains its miniboss-clear status, active warp, and open east door.
Every room boundary after the return warp must be reached through current
collision data. The solid north wall in room `$2E` is not traversable and must
not be bypassed with Feather logic. Room `$36`'s separate overworld front-door
macro defect is not on this forward route and remains a later dungeon-exit
milestone.

## Room `$21` route checkpoint

`IndoorsA21` contains visible chest object `$A0` at location `$27` and event
`$00`. `RoomChestsTable[$21]` selects `CHEST_RUPEES_20`. This chest is optional
and is asserted as source state rather than collected during the Bracelet
milestone.

Room `$22` contains a later Small Key and crystal switch, but the raised switch-block
barrier prevents the direct ordinary/Feather path from `$21`. That branch is not
a prerequisite for entering room `$20` and is deferred.

## Key door and room `$20`

Link enters room `$20` after unlocking room `$21`'s west key door through the
current collision, key-door animation, and indoor-boundary paths.

`DungeonEventsTable[$20]` is `$61`: kill all enemies, then reveal a chest.
`IndoorsA20Entities` contains two Boo Buddies `$50` at locations `$24` and
`$45`, rendered at ROM coordinates `$48/$30` and `$58/$50`. Room objects `$AB`
at `$22` and `$65` are unlit torches. Lighting them through the Magic Powder
sprinkle lifecycle increments `wC1A2`/the room trigger count; the Boo handler
then enters state 1, overrides health to one, accepts live sword collision, and
flees from Link. Both enemies must die through that handler and shared death
machinery. No test may write enemy health, clear slots directly, or substitute
generic enemy removal.

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
must show the collected chest state without replaying the reward. The source
event byte `$61` reloads with the room, but status bit `$10` makes it inert;
the persisted state is not represented by rewriting the table event to `$00`.

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
