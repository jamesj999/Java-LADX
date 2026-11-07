# Bottle Grotto Room `$22` Small Key Ordered-Play Design

## Goal

Continue the uninterrupted fresh-game Bottle Grotto session after the Power
Bracelet pickup in room `$20`, traverse the ROM-authored open route through
room `$21`, and collect and persist room `$22`'s Small Key.

## Source route and carried switch state

`MapLayout1` places `$20`, `$21`, and `$22` consecutively from west to east.
The already-open paired key door returns Link from `$20` to `$21`. Although
room `$21`'s stream includes open-right macro `$F7`, later objects `$C8/$03`
overwrite its two cells. The collision-valid exit is the northeast passage:
Link lifts pots `$26/$16/$17/$18/$28`, reaches the passable upper edge, and
uses the ordinary indoor scroll path into `$22`.

The live ordered session carries `wSwitchBlocksState == $00` from the earlier
room `$38` switch solution. The state is dungeon-global and persists across
ordinary room transitions. In state `$00`, room `$22`'s `$DB` rows are
lowered/passable and its `$DC` rows are raised/solid. The route must preserve
that state rather than overwrite it for a preferred puzzle presentation.

Room `$21` still blocks its west-to-east path with source liftable-pot objects
`$20`. After obtaining the Bracelet, Link must face a reachable pot, hold the
equipped Bracelet button, and pull away from it for the source eight-frame
counter. The shared background-object interaction replaces an ordinary pot
with `$0D` (`$8E` reveals switch `$AA`), spawns entity `$05` at the cell center, and enters
the existing status-7 carried-object lifecycle. Removing at least the pot that
seals the reachable corridor makes the east boundary collision-reachable.

From the upper entry, Link reaches the crystal with `$DB` lowered, hits it with
the Sword, and changes the dungeon-global state to `$02`. The animation raises
the `$DB` cells under Link while lowering `$DC`; the ROM's
`wLinkStandingOnSwitchBlock` rule lets Link move off the newly raised platform
and reach the chest. No cross-room carried-pot or thrown-object path is needed.

## Room `$22` source state

`IndoorsA22` contains event `$00`, closed chest `$A0` at `$27`, crystal switch
entity `$66` at source position `$24`, four droppable hearts at `$53..$56`, and
four liftable pots `$20` at room locations `$53..$56`. Its switch-block layout
contains `$DB` strips at `$11..$18`, `$21..$28`, and `$31..$38`; `$DC` appears
at `$34`, `$51..$58`, and `$63..$66`.

The test must assert this source state, perform the live Sword/crystal
interaction, and prove the chest approach through switch-aware collision. It
must not teleport across the barrier, directly clear pots, rewrite switch
state, or invent a room event.

## Small Key lifecycle and persistence

`RoomChestsTable[$22]` selects `CHEST_SMALL_KEY`. Link opens the chest through
the ordinary upward interaction, then the chest entity emits the reward/dialog
and completes its bounded presentation lifecycle. Applying the reward raises
Bottle Grotto's live Small Key count from `$00` to `$01` and sets room `$22`
status bit `$10`.

Leaving west to `$21` and returning east to `$22` must restore open chest `$A1`,
retain switch state `$02`, preserve the Small Key count, and emit no duplicate
reward. Advancing gameplay ticks after reload proves event `$00` remains inert.

## Architecture and verification

Extend the existing ordered `RoomTransitionCoordinatorTest` session so its
`RoomSession`, `Link`, `PlayerState`, collision model, dungeon item state, and
frame counter remain continuous. Add the shared static-pot interaction at the
session boundary and a runtime spawn method that hands entity `$05` to the
existing lift state; wire `Main` from live equipped-item and directional input.
The focused regression must fail before this production behavior is added.

All searches and waits are bounded. Verify the ordered test, focused room/chest
and transition suites, then `gradle clean test --rerun-tasks`. Obtain source
fidelity review before quality review, update the roadmap with exact evidence,
and commit on the current branch.
