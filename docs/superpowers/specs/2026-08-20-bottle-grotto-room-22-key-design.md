# Bottle Grotto Room `$22` Small Key Ordered-Play Design

## Goal

Continue the uninterrupted fresh-game Bottle Grotto session after the Power
Bracelet pickup in room `$20`, traverse the ROM-authored open route through
room `$21`, and collect and persist room `$22`'s Small Key.

## Source route and carried switch state

`MapLayout1` places `$20`, `$21`, and `$22` consecutively from west to east.
The already-open paired key door returns Link from `$20` to `$21`; room `$21`
has source open-right door `$F7` at `$39`, and room `$22` has matching open-left
door `$F6` at `$30`. Link reaches both boundaries through current collision and
uses the ordinary indoor scroll path.

The live ordered session carries `wSwitchBlocksState == $00` from the earlier
room `$38` switch solution. The state is dungeon-global and persists across
ordinary room transitions. In state `$00`, room `$22`'s `$DB` columns are
lowered/passable and its `$DC` rows are raised/solid. The route must preserve
that state rather than overwrite it for a preferred puzzle presentation.

An alternate state `$02` would raise the `$DB` barrier and can require hitting
the crystal from the opposite side, including the familiar pot-throw solution.
That alternate solution is valid game behavior but is not required by the
current source-authored state. Static pot lifting and thrown-object damage must
be implemented when ordered play first requires them, not smuggled into this
milestone by changing global state.

## Room `$22` source state

`IndoorsA22` contains event `$00`, closed chest `$A0` at `$27`, crystal switch
entity `$66` at source position `$24`, four droppable hearts at `$53..$56`, and
four liftable pots `$20` at room locations `$53..$56`. Its switch-block layout
contains `$DB` strips at `$11..$18`, `$21..$28`, and `$31..$38`; `$DC` appears
at `$34`, `$51..$58`, and `$63..$66`.

The test must assert this source state and prove the chest approach is reachable
through collision while switch state remains `$00`. It must not teleport across
the barrier, clear pots, directly rewrite switch state, or invent a room event.

## Small Key lifecycle and persistence

`RoomChestsTable[$22]` selects `CHEST_SMALL_KEY`. Link opens the chest through
the ordinary upward interaction, then the chest entity emits the reward/dialog
and completes its bounded presentation lifecycle. Applying the reward raises
Bottle Grotto's live Small Key count from `$00` to `$01` and sets room `$22`
status bit `$10`.

Leaving west to `$21` and returning east to `$22` must restore open chest `$A1`,
retain switch state `$00`, preserve the Small Key count, and emit no duplicate
reward. Advancing gameplay ticks after reload proves event `$00` remains inert.

## Architecture and verification

Extend the existing ordered `RoomTransitionCoordinatorTest` session so its
`RoomSession`, `Link`, `PlayerState`, collision model, dungeon item state, and
frame counter remain continuous. Production changes are permitted only after a
RED ordered or focused test demonstrates a shared source mismatch.

All searches and waits are bounded. Verify the ordered test, focused room/chest
and transition suites, then `gradle clean test --rerun-tasks`. Obtain source
fidelity review before quality review, update the roadmap with exact evidence,
and commit on the current branch.
