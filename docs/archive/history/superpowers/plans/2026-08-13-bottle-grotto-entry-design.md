# Bottle Grotto Entry Ordered-Play Design

## Scope

Continue the same fresh-game ordered regression from BowWow's rescue through
the room `$36` owl event, Goponga Swamp, and the source-authored entrance to
Bottle Grotto at map `$01`, room `$36`.

## Source behavior

- Leaving the Moblin Hideout reverses the direct-room route
  `$E2 -> $E1 -> $E0 -> $F0`; room `$F0`'s ROM warp returns to overworld room
  `$35`.
- Overworld room `$36` contains one `ENTITY_OWL_EVENT` at grid `(4,5)`.
  `OwlEventEntityHandler` keeps it only while BowWow is following, Tail Cave's
  instrument completion bit is set, and Bottle Grotto's instrument flags are
  still zero. Like room `$D2`, it enters state 2 directly and waits for an
  interactive Link plus transition-sequence counter `$04`.
- `label_2A07` resolves room `$36` through the ROM tables to `Dialog0C3`. Owl
  state 3 sets `OW_ROOM_STATUS_FLAG_OWL_TALKED` (`$20`) before departure.
- The collision-valid route into and through the swamp is
  `$36 -> $35 -> $25 -> $35 -> $34 -> $33 -> $23 -> $22 -> $23 -> $24`.
  Entering `$24` from `$25` lands in the wrong collision component; the west
  approach through `$23` is the source-valid route to the dungeon entrance.
- The route exercises all source-authored swamp obstructions: giant `$7C` and
  small `$7E` flowers in `$33`, a giant in `$23`, two small flowers in `$22`,
  and five small flowers in `$24`. Both types are enabled by the ROM
  `BowWowEatableEntitiesTable`. BowWow removes them through its normal target
  and `DidKillEnemy` path. Their clears use the source's transient recent-room
  entity-clear state, not permanent overworld room-status bits.
- Overworld room `$24` contains warp `$E1,$01,$36,$50,$7C`, which enters map
  `$01` (Bottle Grotto), room `$36`, at `(x=$50,y=$7C)`.
- Bottle Grotto room `$36` is the layout-backed entrance at
  `MapLayout1[7][2]`; its source room includes the return warp to overworld
  room `$24`, a dungeon warp entity, and four droppable hearts.

## Architecture

Extend the existing single fresh-game regression so BowWow's state, Tail Cave
instrument flag, owl persistence, swamp entity clears, and dungeon entry all
flow from prior live play. Exercise `RoomTransitionCoordinator`, ROM collision,
`RoomEntityRuntime`, and ROM warp data directly. Add focused regressions only
for a production defect exposed by that ordered route, and make the smallest
source-shaped correction required.

## Verification

The slice is complete when the uninterrupted fresh session completes room
`$36`'s owl dialog `$0C3`, persists its `$20` flag, traverses every required
swamp room through collision-valid boundaries, has BowWow consume every giant
and small flower through live entity ticks with immediate transient-room
persistence, and enters Bottle Grotto map `$01`, room `$36` through the ROM
warp.

`wEntitiesClearedRooms` is paired with the source's six-slot
`wRecentRooms` ring. Visiting a sixth other distinct room evicts the oldest
room and clears its entity mask, allowing ordinary enemies and swamp flowers
to respawn exactly as in `UpdateRecentRoomsList`.
