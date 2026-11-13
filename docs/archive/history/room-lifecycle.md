# Loaded-room lifecycle

## Design

`LoadedRoom` already means decoded, owned ROM input. `RoomRuntime` instead means
one installed live generation: final non-null ActiveRoom and RoomEntityRuntime,
with collision scratch owned by that generation. Absence is not a RoomRuntime.
The facade will own the optional current generation; room-only operations will
receive the installed generation rather than ask independent room/runtime fields
whether loading has occurred. Existing public role objects must remain stable
across loads, including clients retaining them. Historical scroll data remains
an owned render snapshot.

The implementation proceeds incrementally. The first design commit introduces
the generation value and tests; it does not yet replace the old lifecycle fields.
A follower synchronization that rebuilds the entity runtime must replace the
generation as well. Runtime installation and publication must preserve the old
load/configuration/observer order, including the first load.

## No-room operation contracts to preserve

Valid arguments are assumed below; existing byte/range validation must retain
its order even when no room is loaded. This inventory is expanded as each
boundary migrates; it is not permission to replace unchecked failures with defaults.

| Public operation family | Current no-room behavior |
| --- | --- |
| `rooms().load*` | Construct/load a room; not a no-op |
| `rooms().activeRoom`, `palettes`, `renderView`, `renderSnapshot` | null |
| `rooms().hasActiveRoom` | false |
| `rooms().currentRoomId`, `mapCategory`, `mapId` | NullPointerException |
| `rooms().indoorRoomPositionForSave` | -1 |
| `rooms().heartPiecePickupActive`, `entities().inBossBattle` | false |
| `rooms().instrumentTrace` / `moblinKingTrace` | empty string / "-1" |
| `rooms().liftedEntityState` | LiftedEntityState.none() |
| `entities().resolveEntityCombat` | empty list |
| `entities().collectEntityIfNeeded` | null |
| `entityState().clearEntity` | 0 |
| `entityState().gelClingingToLink`, `deferContactRecoil` | false |
| `entityState().setEntity*` cached input setters | Validate and retain cached fields; runtime writes skipped |
| Frame inventory-slot input (internal prelude) | Validate slots; runtime write skipped (no cached slot fields) |
| `entityState().setToadstoolPlayerState` | Runtime-only write skipped |
| `entityState().setEntityScreenShakeCountdown` | Runtime-only write skipped |
| World/dungeon status restore and persistent progression setters | Retain their persistent writes before load |
| Outbox consume/drain operations | Drain queued session requests; runtime-backed empty defaults where applicable |
| World entity tick | Preserve input/follower/clock prelude, then return empty events |

No caller-facing render signature will be removed while the render engineer is
working. Explicitly named nullable forms can be added and the old forms delegate.

Baseline after merging develop: world comparison occurrences 328 (306 matching
lines); app 94 occurrences / 91 lines, gameplay 22 occurrences / 20 lines.
The final inventory will report both definitions to keep the coordinator
line-count baseline and expression-level classification comparable.

## Combat migration

The facade now publishes RoomRuntime on load and follower runtime replacement.
Combat, recoil, clear and pickup operations make their no-room decision at the
facade; RoomEntityCombatBridge receives a non-null generation and contains no
active-room/runtime absence checks. Pickup event absence remains a distinct
optional domain result. Stable role objects survive replacement. Test fixtures
that replace a runtime install a generation instead of bypassing publication.
Legacy internal room/runtime fields are transitional aliases until the remaining
collaborators migrate; they are not the final model.

## Cached entity inputs

RoomEntityInputs now owns only cached inputs, their validation and loaded-runtime
application methods receiving RoomRuntime. No runtime-presence question remains
in that collaborator. Facade operations validate/store first, then apply to a
loaded generation. Frame-only channels use the same facade policy in the same
setter order. Tests that formerly bypassed the facade now invoke that boundary;
their source-order and reward assertions are unchanged. Null comparison
occurrences in world/room+transition+object: 328 -> 311 at this slice.

## Tick and rewards

The facade performs the original immutable-input dereferences and Link register
capture before deciding whether to tick. No-room calls still clear the woods
flag and store byte-masked Link history, without beginning an entity RNG frame.
Default positional tick entry points assemble inputs at that same boundary.
RoomEntitySimulation and RoomEntityRewards receive RoomRuntime explicitly through
configuration, ticking and ordered effects; neither asks whether a room/runtime
exists. No new per-frame callback or holder is constructed by this migration.

## Player items

Loaded player-item/shovel methods receive RoomRuntime. The facade preserves
false for unloaded projectile/lifted-throw operations, zero projectile count,
ArrowShotResult(false,false), and ShovelStartResult(false,true,-1). Shovel ROM
direction validation still precedes the no-room branch; animation lookup remains
a room-independent ROM query. The owned contract test includes null throw facing
and invalid shovel direction before first load. World null occurrences: 274.

## NPC presentation

Marin/Tarin presentation and sprite-override application receive RoomRuntime.
The facade retains false/no-op defaults before first load. Missing sprite
selection and optional override maps remain visual-domain absence, not runtime
lifecycle checks. World null-comparison occurrences at this slice: 268.

## Collision generation bindings

Collision object/ground/boundary caches now live on RoomRuntime. Queries take
that generation; entity object, ground, background and color-shell callbacks
are allocated and bound once on generation creation. They no longer discover
the current runtime through a supplier. Diagnostic probes use the same explicit
argument. A marked adapter for the still-unmigrated boundary coordinator uses a
transitional generation alias; it must disappear with that caller, not remain
as another lifecycle source. No per-frame callback allocation is introduced.

## Story and follower generations

Story setters store/validate persistent state before the facade optionally
applies runtime effects. Chest levels retain house-state configuration before
chest-state publication; default music still updates the witch environment in
source order. A follower rebuild takes the installed generation, publishes its
replacement, and configures that replacement. Drop setup receives a generation;
the frame's default and explicit-low-health configuration writes stay in order.
No room/runtime absence checks remain in story or follower collaborators.

## Owned room data and transient parts

RoomRuntime now owns RoomContents (the mutable map and per-room puzzle/override
parts) as well as collision scratch. Room preparation creates fresh parts after
the original transient reset; follower runtime replacement retains those parts.
The cracked-floor and Link position registers, switch-block state, title clock,
and resume-music state retain their original across-room lifetimes. A reload
regression checks both fresh room state and retained registers. Preparation
aliases remain transitional until the door/map/load collaborators migrate.
Gate: 2965 tests, 0 failures/errors, one existing skip; 986 identical scenes.

## Output boundary

The session facade now decides unloaded output defaults; RoomEntityOutbox owns
only session queues and staging, without a runtime supplier. LinkOutputs and
PresentationOutputs consume a narrow drain-only operation port implemented by
the facade, with no current-room getter. The compatibility guard now permits
exactly that interface and an added test prohibits room/runtime access through
it; the no-superclass/no-deprecated-bridge guarantees remain. Ordered queue
draining and immutable ownership tests remain unchanged in intent.
World null occurrences: 248 -> 222. Gate: 2966 tests, zero failures/errors, one skip.

## Map mutation scope

Map-only writes/redraws receive RoomContents, including pre-runtime door setup.
Entity-producing writes receive RoomRuntime. Ground probes read the supplied
room rather than the current-room alias; their persistent ROM registers retain
the existing lifetime. Terrain role operations preserve unloaded false defaults.
RoomObjectMutations contains no room/runtime absence checks. Remaining loader
preparation aliases are temporary for the next collaborator migrations.
World null occurrences: 222 -> 204. Gate: 2966 tests, zero failures/errors, one skip.
