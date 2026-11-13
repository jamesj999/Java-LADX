# Room simplification

## Baseline

Scope: world/room, world/transition, world/object main and tests. Counts use
physical lines; null checks are equality/inequality comparisons with null.
Small classes use nonblank, non-import/package lines after stripping comments
(including braces). Numbered tests match `[12]Test` or `Support2`.

```text
main {"files": 155, "lines": 16361, "wildcards": 95, "null_checks": 328, "under40": 67, "numbered": 0}
test {"files": 81, "lines": 18327, "wildcards": 84, "null_checks": 140, "under40": 19, "numbered": 8}
```

## Main consolidation

The parser now owns stream walking, macro expansion and persistent status patches;
these all mutate the same decode state. Object mutations own ground, bomb-wall,
projectile and torch writes to the same room map. Puzzle progression owns block
motion, event triggers, reveals and shutters. Door interactions own indoor key
blocks and dungeon entrances. Story state owns player flag writes and restoration.
Entity simulation owns configuration, ticking and ordered post-tick effects as
one sequence. The former helpers had no independently owned mutable state.

Kept boundaries: loader/pointer tables (ROM acquisition); tilemap/palette decoding
(renderer data); collision queries (live probes and caches); output queues and
ordered drains (ownership); follower synchronization (entity respawning/history);
NPC presentation (sprite overrides); item actions (distinct lift/chest/shovel
state machines); transitions/scrolling (timed state); dungeon/progress/story
state (different persistence); public role views (consumer capabilities); boundary
values (immutable ownership). No class inheritance was introduced.

## Single-representation boundaries

LinkPosition, LinkMotion, LinkGroundFrame and EntityPickupInputs.Link now store
only raw direction/dispatch integers. All four admit arbitrary ROM probe bytes;
normalizing unknown values would change numeric gates. Direction classification
is derived (including Direction.UNKNOWN); motion offers Optional knownState()
for explicit absence outside the ROM table. Legacy nullable classification
accessors remain derived for source compatibility. No stored enum can disagree
with the raw byte. All previous enum and enum+raw constructors remain deprecated
thin delegates; the historical raw-wins behavior is unchanged.

App hand-off (files deliberately untouched):

- app/gameplay/entity/EntityInputCache.java:linkPosition ->
  `new LinkPosition(x, y, z, direction, collisionType)`.
- app/gameplay/entity/EntityInputCache.java:linkMotion ->
  `new LinkMotion(airborne, state, shield, invincibility, speedX, speedY)`.
- app/gameplay/FrameInputCache.java:pickup ->
  `new EntityPickupInputs.Link(x, y, airborne, interactive, direction, z)`.
- app/gameplay/FrameInputCache.java:ground ->
  `new LinkGroundFrame(x, y, airborne, blocked, dialog, transition, motion, slow, usage)`.

Other non-owned clients of enum constructors remain source-compatible; these
constructors can be removed once their owners migrate. Owned callers use raw
constructors. Tests check unknown bytes and the absence of duplicate stored enums.

## Tests

Numbered doors/rewards/state classes are regrouped as
RoomDoorAndObjectInteractionTest, RoomRewardLifecycleTest and
RoomStoryPersistenceTest. The dungeon route is KeyCavernEntranceRouteTest.
Navigation support is one RoomNavigationFixture. All twelve route fragments
are methods of GameplayRouteScenario, named for their gameplay phase and called
in their original sequence using the same GameplayRouteState. No assertions or
tests were removed. Test files 82 -> 66; lines 18300 -> 17970; numbered names 8 -> 0.
