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
one sequence. Load initialization belongs to RoomLoadCoordinator, tick defaults
to RoomEntityBridge, and immediate tile redraw to RoomObjectMutations. The
former helpers had no independently owned mutable state.

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

The remaining non-owned construction sites are the four app methods above.
Both old enum-only and enum+raw forms remain source-compatible; their adapters
can be removed once the app owner completes the migration. Owned callers use raw
constructors. Tests check unknown bytes and the absence of duplicate stored enums.

## Tests

Numbered doors/rewards/state classes are regrouped as
RoomDoorAndObjectInteractionTest, RoomRewardLifecycleTest and
RoomStoryPersistenceTest. The dungeon route is KeyCavernEntranceRouteTest.
Navigation support is one RoomNavigationFixture. All twelve route fragments
are methods of GameplayRouteScenario, named for their gameplay phase and called
in their original sequence using the same GameplayRouteState. No assertions or
tests were removed. Test files 82 -> 66; lines 18300 -> 17970; numbered names 8 -> 0.

## Null audit

Dependency records now reject absent required collaborators at construction;
optional presentation services remain absent in domain-only/headless fixtures.
Null comparisons remain 328 main / 140 test: these are retained to preserve
unloaded-room, absent-runtime, optional-service and absent-event behavior, rather
than replacing domain absence merely to lower a metric. Intentional optionals:

- active room and entity runtime before load; cached renderView() returns null
  before the first room load (the public role contract now states this explicitly);
- pending warp/respawn/shovel requests and optional visual/event metadata;
- optional VFX, rupee presentation and load observer services;
- an unset low-health override (derive the result from hearts instead);
- unknown ocarina selection or motion classification outside the ROM table;
- cache misses, collision misses, optional ROM stream entries and absent overrides.

The initial null audit exposed lazy construction cycles. These have now been
removed: status completion lives with persistent room state, staircase setup
with puzzle state, enemy-drop configuration with entity state, and redraws with
object mutations. Required collaborators are constructed eagerly in dependency
order. The only remaining Suppliers project genuinely changing active-room or
entity-runtime values; none defer collaborator initialization.

## Readability and verification

RoomEntitySessionState names its retained lifetime explicitly: room runtime,
output queues and drop counters survive individual frames. No frame holder is
reused where consumers can retain an immutable boundary value.

The twelve reviewed owners are RoomObjectParser, RoomObjectMutations,
RoomDoorInteractions, RoomPuzzleController, RoomEntitySimulation, RoomStoryState,
RoomTransitionCoordinator, RoomLoadCoordinator, RoomWorldState,
RoomCollisionQueries, RoomSessionViews and RoomPlayerItemActions. Class comments
name the corresponding source routine or ROM responsibility. Parser scratch
state now belongs to the parser rather than a separate shared state bag.
Entity methods name configuration and effect application. Guard/return ordering,
room-event gates, byte masks and source offsets remain intact.

After the null audit, runtime-only consumers were migrated from lazy bridge
lookup to explicit retained entity state. Nine self-dependencies were removed.
The remaining service cycles were then broken by relocating state operations
and merging redraw with map mutation. Constructor dependencies are direct;
RoomSessionContext has no Supplier or deferred lookup. Live projections/outbox
still use changing-domain-value suppliers, not initialization indirection.

EA-off allocation guards, after warm-up:

- Whole frame: idle 0, walking 580.33, inventory 0 B/frame (incoming walking ~597).
- Update-only session: idle 0, active 434 B/frame.
- Entity mix: 834.43 B/tick.
- Room/inventory live projections: 0 B/frame; owned capture: 10456 B/frame.
- Empty outbox drains/merges: exact-zero guard passed.

The same retained-output full-frame path, ordinary inputs and existing budgets
were used. No guard, fixture or test was weakened. Main/test source edits are
limited to room, transition and object; the app constructor migration is a
hand-off, not an edit to concurrently owned files. The lint follow-up remains
postponed as requested. No window, manual recording, push or ROM asset commit.

## Final metrics

| Metric | Main before | Main after | Tests before | Tests after |
| --- | ---: | ---: | ---: | ---: |
| Files | 155 | 133 | 81 | 66 |
| Lines | 16,361 | 15,220 | 18,327 | 18,097 |
| Wildcard imports | 95 | 0 | 84 | 0 |
| Null comparisons | 328 | 328 | 140 | 140 |
| Types under 40 code lines | 67 | 62 | 19 | 20 |
| Numbered test/support names | 0 | 0 | 8 | 0 |

Remaining small types are chiefly public role contracts, immutable boundary
values and retained state owners; they are not cut to a line limit. The extra
small test type covers unknown-byte and representation invariants. All twelve
numbered route stage files are gone as well. RoomSession remains 94 lines.
Deferred collaborator suppliers: zero. Domain projection/outbox suppliers remain
only for values that legitimately change on room replacement.

Final full gate: **2,956 tests, zero failures/errors, one existing skip; all 986
scene captures identical**. Two boundary tests were added; no original test was
lost or disabled. All eight simplification steps are complete. Build-hardening
steps 2–6 remain postponed, independently of this completed simplification.
