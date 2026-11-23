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

## Doors, lifting and room events

Door/chest/bracelet interactions receive the installed generation. Puzzle/map
initialization uses RoomContents before runtime construction; runtime-dependent
room-event effects receive RoomRuntime. Stable door roles route once through
the facade: unloaded interactions return false, chest results remain
(false,-1,-1,-1), lifting clears the pull counter, and the keyhole tick returns
zero/clears its final-block flag. World null occurrences: 204 -> 180.
Gate: 2966 tests, zero failures/errors, one skip; 986 identical scenes.

## Projection boundaries

Loaded render views bind directly to final room/runtime data; standalone room
projections use an explicit no-visuals port. No room/runtime supplier remains in
render or minimap views. The stable dungeon projection receives map identity at
facade installation/reset so retained views still follow room changes. Optional
render signatures now have explicit renderViewOrNull/renderSnapshotOrNull names;
old names remain compatible. Off-limits hand-off: app/presentation/RenderStateAssembler
currentReusableGameFrameState -> rooms().renderViewOrNull(); currentGameFrameState -> rooms().renderSnapshotOrNull();
its activeRoom presence guard can use roomSession.hasRoom().
World null occurrences: 180 -> 158. Gate: 2966 tests, zero failures/errors, one skip,
986 identical scenes. Room/runtime aliases remain only for loading/transitions.

## Complete lifecycle publication

RoomSession now owns the only optional current RoomRuntime. RoomLocationState
and RoomEntitySessionState no longer contain room/runtime aliases; no room/runtime
supplier or presence comparison remains in a room-only collaborator. Loader
locals own prepared room parts before the runtime exists; RoomContents is then
owned by the generation, including follower replacement. Session queues and ROM
registers with across-room lifetimes remain separate. New-game reset explicitly
clears the installed room contents in addition to session queues.

Boundary coordinator callbacks use a narrow operation port and receive an explicit
loaded generation. Runtime-backed requests/defaults are decided by the facade.
Post-load listener notification returns the generation published after any nested
load; a regression verifies publication before notification and the resulting
room/runtime metadata and collision map. No current-runtime getter is injected.

Additional unloaded contracts: boundary handlers are no-op/false; staircase and
pending Ocarina warp polls return null; Ocarina byte validation runs before a
false no-room result; title-message tick returns -1 with the ordinary zero clock;
adjacent-scroll operations retain NullPointerException; warp destination loads
remain supported before the first room. World null expressions: 158 -> 129.
Gate: 2967 tests, zero failures/errors, one existing skip; 986 identical scenes.

## Completed boundary hand-offs

EntityInputCache.linkPosition/linkMotion and FrameInputCache.pickup/ground now
call the canonical raw-value constructors with the same original expressions.
All eight deprecated enum/dual-value constructors are deleted; the four record
types each expose only one canonical constructor. Derived enum accessors and
unknown byte behavior are unchanged. Gate: 2967 tests, zero failures/errors, one skip.

## Final no-room operation matrix

This table covers valid arguments; existing validation preceding an absence
branch still runs. Overloads share the policy shown. A room-only transient state
does not exist before load, so counter-reset defaults are observably zero/false.

| Operation | Before the first room load |
| --- | --- |
| RoomSession constructor, role accessors, entityFrameResults | Construct/return non-null cached boundaries; no fake room/runtime |
| hasRoom / rooms.hasActiveRoom | false |
| rooms.loadInitialOverworld / loadOverworld / all loadIndoor forms | Perform the load and publish a generation |
| rooms.activeRoom / activeRoomOrNull / palettes / palettesOrNull | null |
| rooms.renderView / renderViewOrNull / renderSnapshot / renderSnapshotOrNull | null |
| rooms.requireActiveRoom / currentRoomId / mapCategory / mapId | NullPointerException |
| rooms.indoorRoomPositionForSave | -1 |
| rooms.heartPiecePickupActive | false |
| rooms.instrumentTrace / moblinKingTrace | empty string / "-1" |
| rooms.liftedEntityState | LiftedEntityState.none() |
| rooms.musicSelectionIsScroll | Read retained loader selection state |
| entities.inBossBattle | false |
| entities.resolveEntityCombat | Empty list, after the original immutable input dereferences |
| entities.collectEntityIfNeeded | null, after the original immutable input dereferences |
| entities.tickEntities / tickEntitiesWithProjectileEvents / frame-input tick | Preserve validation, input/cache writes, Link byte-register/history capture and woods-flag reset; return no entity events and do not begin an entity RNG frame |
| entityState.clearEntity | 0 |
| entityState.entityHealth | NullPointerException |
| entityState.gelClingingToLink / deferContactRecoil / gotItemPresentationActive / powerUpPickupSequenceActive / swordPickupSequenceActive | false |
| entityState.setEntityDialogActive / setEntityTalkState / setEntityActionButtonsHeld / setEntityPowerBraceletButtonHeld / setEntityBombButtonHeld | Validate/cache in the original order; skip runtime writes |
| entityState.setSecretSeashellPegasusCollisionState | Validate/cache as before; skip runtime writes |
| entityState.setEntityAttackStepAnimationCountdown | Validate the unsigned countdown; skip runtime-only write (no cached countdown) |
| entityState.setToadstoolPlayerState / setEntityScreenShakeCountdown | Skip runtime-only writes, retaining the original no-room validation policy |
| entityState.resetGuardianAcornCounterOnHurt | Reset retained drop counters; skip runtime configuration |
| entityState.advanceBoomerangSoundClock / tickGameplayVBlank | Advance retained source clocks/switch animation as before |
| entityState.entityDefaultMusicTrack | Return retained default track |
| entityState.setColorShellSoundSink | Retain the sink; normalize null to GameplaySoundSink.none() |
| entityState.consumeLinkAttackClearRequests | Empty list |
| entityState.consumeSwordPickupRewards / consumeEntityDialogRequests | Drain queued session values; no runtime values |
| playerItems.throwLiftedEntity / fireHookshot / fireBoomerang / fireSwordBeam / fireMagicRodFireball / sprinkleMagicPowder / placeBomb / shootArrow | false |
| playerItems.shootArrowResult | ArrowShotResult(false,false) |
| playerItems.startShovel | Validate the 0..3 ROM direction, then ShovelStartResult(false,true,-1) |
| playerItems.advanceShovel | No-op |
| playerItems.shovelAnimationState | Independent ROM lookup and its original argument validation |
| playerItems.activeProjectileCount | 0 |
| playerItems.bombActive / hookshotActive / boomerangActive / lastBombPlacementPlayedBump | false |
| doors.tryOpenChest | ChestOpenResult(false,-1,-1,-1) |
| doors.tryUnlockIndoorKeyDoor / tryPushIndoorBlock / tryLiftObject / tryLiftIndoorObject / tryInteractWithIndoorBlock / tryUnlockIndoorKeyholeBlock / tryUnlockTailCaveKeyhole / tryUnlockKeyCavernKeyhole | false; absent room-only pull state remains zero |
| doors.tickTailCaveKeyholeSequence / tailCaveKeyholeSequenceActive | 0 / false |
| terrain.tickCrackedFloor / tickCrackedFloorOnLanding / breakIndoorSwordCrystal | false |
| transitions.loadWarpDestination | Perform the load; preserve respawn writes, map-boundary callback order, source indoor flag, palette-history timing and title clock |
| transitions.startAdjacentOverworldScroll / startAdjacentIndoorScroll | NullPointerException |
| transitions.tickDungeonTitleMessage | -1 with its ordinary zero clock; original decrement/gate and required-room exception remain for an internally seeded clock |
| transitions.startOcarina | Validate countdown/song bytes, then false |
| transitions.ocarinaPlaying / ocarinaAnimationState | false / -1 |
| transitions.consumeManboPondTransitionRequest / consumeInstrumentTransitionRequest / pollStaircaseWarp | null |
| RoomTransitionCoordinator boundary handlers | No-op for warp/overworld dispatch; false for pending Manbo/instrument dispatch |
| story.applyMarinWakeUpPresentation / updateHouseTarinPresentation / setFollowerSpriteOverrides | false / no-op / no-op |
| story.setBirdKeyOwned / setTailKeyOwned / setBowWowState / setRichardSpokenFlag / setTarinFlag / setHouseNpcTradeItem / setChestPlayerLevels / setEntityDefaultMusicTrack / setFollowingNpcState | Preserve persistent validation/cache writes; skip loaded-room effects |
| story.followingNpcState / bowWowState / richardSpokenFlag / tarinFlag / shouldGetLostInMysteriousWoods | Read retained state |
| story.initializeNewGameWorldState / restoreRoomStatuses | Reset/restore persistent/session state with the original validation; no room-only state exists to clear |
| dungeon.view | Non-null stable read-only view; empty minimap, live persistent progression/item flags |
| dungeon.dungeonMinimapRoomStatusSnapshot | New owned zeroed 64-byte array |
| All other dungeon snapshots/restores/debug item setters | Read/copy/update persistent state with original bounds/length checks |
| progress.onMapBoundaryWarp / setRespawnPoint / clearGameOverPowerCounter / resetGameOverSwitchBlocks | Update persistent/session state; runtime configuration skipped |
| progress.respawnPoint / respawnIndoorRoom | Read retained optional destination/register |
| LinkOutputs runtime-backed channels | Empty ordered lists/drains; session motion-block requests still drain |
| RewardOutputs channels | Drain queued session rewards; heart-piece runtime updates are empty |
| PresentationOutputs entity/dialog channels | Drain queued session values first/in the original merge order; no runtime events |
| PresentationOutputs music track / fade countdown | Queued session track if present, otherwise -1 / 0 |

## Null discipline and remaining render hand-off

All remaining comparisons are inventoried with source locations and reasons in
[room-null-audit.md](room-null-audit.md). World: 328 -> 115 expressions (306 -> 110
matching lines): 86 optional domain values, 29 public-boundary validations, zero
leftovers. App: 94 expressions / 91 lines (72 optional, 22 boundary); gameplay:
22 expressions / 20 lines (5 optional, 17 boundary), unchanged because their
remaining values are truly optional or null-compatible public boundary inputs.
The sole loaded-generation presence comparison is confined to RoomSession.
Session bags contain no room/runtime aliases, enforced by a structural test.
Disabled VFX/drops/listener become non-null zero-slot/no-op services at wiring;
a feature bit retains the original skip of owned entity VFX-list production.

The render engineer can make these switches in the off-limits file
`app/presentation/RenderStateAssembler.java` (old forms remain working):

| Method | Switch |
| --- | --- |
| composeFrameBuffer | activeRoom presence guard -> roomSession.hasRoom(); within the guarded branch use rooms().requireActiveRoom() for metadata |
| currentReusableGameFrameState | rooms().renderView() -> rooms().renderViewOrNull() |
| currentGameFrameState | rooms().renderSnapshot() -> rooms().renderSnapshotOrNull() |

The four app frame-cache constructor hand-offs are complete; none remain.

Required RoomSessionConfig collaborators are now validated once at the public
construction boundary; optional presentation settings retain their disabled
forms. The now-empty PresentationOutputs dependency record and its wiring slot
were removed. Null-comparison metrics remain unchanged. The gate remains 2969
tests, zero failures/errors, one existing skip, and 986 identical scenes.

## Allocation verification

Room generations, their prepared contents, callback bindings and collision scratch
are created only on room load or follower-runtime replacement. Existing guard
budgets were not relaxed. EA-off was selected only with an untracked temporary
Gradle init script; no JVM/build option was committed.

| Guard | EA on, B/frame or tick | EA off, B/frame or tick | Existing budget |
| --- | --- | --- | --- |
| Whole frame: opening-dialog idle | 0 | 0 | 0 |
| Whole frame: walking with entities | 556.8 | 580.3 | 747 |
| Whole frame: inventory open | 0 | 0 | 0 |
| Simulation: idle | 0 | 0 | 0 |
| Simulation: active | 422 | 433 | 542 |
| Room render/inventory live views | 0 | 0 | less than 256 |
| Empty room/entity outbox drains and ordered merges | 0 | 0 | 0 |
| Entity mixed tick (unchanged off-limits implementation) | about 835 | about 835 | unchanged entity guard |

Owned room/inventory history snapshots still allocate about 10.5 KB per owned
capture by design; the per-frame live path does not. All measured paths and the
EA-off whole-frame assertions pass with their existing budgets.

## Error-path reset ordering

Final review also retained ResetRoomVariables timing when acquisition fails.
A constructor-injected reset action calls the facade at the original load phase;
it passes the current contents to the ordered loaded reset, or clears session
state alone before the first load. Loaded resets preserve the source's exact
interleaving of puzzle, VFX/drop, override, queue and item-register clears.
The failed-acquisition regression checks the retained generation, cleared old
transients and advanced load serial after an invalid room-id error. The nested
load regression uses a distinct callback-time map mutation to verify collision
continuation against the replacement. Gate: 2970 tests, zero failures/errors,
one existing skip, 986 identical scenes. Null metrics remain unchanged.

## Completion

Lifecycle steps 1-6 are complete. Required wiring is validated before frames; no
per-room collaborator asks whether a runtime/active room exists. All four app
constructor hand-offs are applied and all eight obsolete constructors are gone.
Only the explicitly listed render/presentation naming hand-off remains external;
legacy forms retain their behavior. The final full gate reports 2970 tests, zero
failures/errors, one existing skip and 986 identical scenes. EA-on/off guards
pass unchanged budgets. Off-limits source/test packages and fixtures are untouched.
