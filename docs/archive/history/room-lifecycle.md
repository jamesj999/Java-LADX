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
