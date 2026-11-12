# Architecture

## Overview

This Java 21 / LWJGL engine reconstructs Link's Awakening DX for
preservation and research. Users provide their own ROM. ROMs and extracted
original assets must never enter commits, distributions, examples or fixtures.
Runtime graphics and world data come from that ROM.

The ROM and LADX disassembly are the technical sources of truth.
The simulation advances in 60 Hz frames and reconstructs ROM state machines,
rather than running a general Game Boy CPU emulator.
Byte-sized counters, slot ordering, intentional offsets and input edges are
observable behavior. A refactor must preserve them without behavior changes.
Replay checkpoints and emulator comparisons establish bounded fidelity;
a passing test suite does not establish game-wide parity.

`Main` is the entry point; `GameSession` owns a running session.
Hosts supply native input, presentation and PCM output. Headless execution
uses the same session simulation with a different host.
See [BUILDING.md](BUILDING.md) for setup and local ROM configuration.

## Design rules

Use composition for implementation reuse. A main-source class may extend a
JDK/framework type, or participate in a sealed hierarchy used polymorphically.
Do not divide one object's implementation into a linear superclass chain.
A collaborator owns its private state, receives the dependencies it actually
uses, and has a responsibility that can be tested independently. Pass shared
mutable data through small explicit state objects; do not share protected fields
or give file-slice helpers the owner's complete state.

Cohesion takes priority over line counts. A cohesive class of roughly 500 lines
is acceptable. Merge artificial slices, and split only at a real responsibility
boundary; moving methods into another file is not itself a design improvement.
Stateless algorithms and immutable table data are valid separate responsibilities
when their inputs describe that work rather than an owning object.

`architecture.NoImplementationInheritanceTest` checks superclass and sealed
permits metadata in every main class file. Owned packages have no exceptions.
Its temporary render/gpu/audio exceptions belong to the concurrent migration
and must be removed as those classes are converted. The field inventories,
responsibilities and measured before/after sizes are recorded in
[composition-redesign.md](archive/history/composition-redesign.md).

## Package map

All names below are relative to `linksawakening` under
`java/src/main/java`. Subpackages own implementation responsibilities;
package placement does not imply that every pair is independent.

### `app`

The root contains only `GameSession` and the public `HeadlessGameDriver` entry
point. `GameSession` owns one desktop/headless frame loop and delegates to the
objects built by `app.wiring.SessionWiring`.

| Package | Top-level types | Responsibility |
| --- | ---: | --- |
| `app` | 2 | Session and replay entry points |
| `app.wiring` | 14 | Composition root, subsystem factories and equipment target adapters |
| `app.services` | 8 | `SessionServices` and its seven subsystem bundles |
| `app.state` | 11 | Mutable screen, scene, clock, story, input, debug, save and stop state |
| `app.flow` | 9 | Navigation, persistence, shutdown and the completed session graph |
| `app.input` | 7 | Host/headless keys, queued gameplay routing and input completion |
| `app.audio` | 3 | Music selection and dialog-sound routing |
| `app.presentation` | 2 | Frame-state assembly, composition and debug-overlay ordering |
| `app.headless` | 4 | Diagnostic/checkpoint contracts and their owned snapshots |
| `app.host` | 3 | Desktop/headless resources and native boundary |
| `app.screen` | 13 | Five screens, their lifecycle ports and retained screen set |
| `app.gameplay` | 14 | Ordered phases, transition/input gates and immutable input reuse |
| `app.gameplay.entity` | 9 | Entity frame inputs, ordered result application and Link command adapters |
| `app.gameplay.interaction` | 8 | Bushes, lifting, dialogs and their input gates |
| `app.gameplay.recovery` | 5 | Pass-out animation, game-over presentation and recovery |
| `app.story` | 4 | Opening-story state machines and music/restoration ports |
| `app.debug` | 6 | Lazy editor/overlay ownership, teleportation and logging |

`SessionWiring` runs once. `SubsystemWiring` builds ROM-backed services through
`RomWiring`, `AudioWiring`, `UiWiring`, `WorldWiring` and `PlayerWiring` in dependency
order. Equipment registration stays in its original order across
`EquipmentWiring`, `CombatEquipment` and `ToolEquipment`; construction-only
parameter records are nested in their factories. `GameplayWiring`, `FlowWiring`
and `ScreenWiring` connect the retained application controllers and screens.

`SessionServices` contains `RomServices`, `AudioServices`, `InputServices`,
`WorldServices`, `PlayerServices`, `UiServices` and `VisualServices`.
Collaborators receive the bundles or components they use. `SessionContext` is an
immutable composition result containing services, state, frame-loop entry points,
host and stop state. `FrameLoopServices` groups screens, presentation, key dispatch,
shutdown, controllers and save storage. `SessionControllers` exposes the retained
domain controllers for delegation and headless setup. These records own neither
behavior nor functional callback fields.

`SessionState` groups live mutable holders. `FrameClock` owns the wrapping frame
byte; `OpeningStoryState` owns wake-up/shield motions; `GameplaySounds` owns the
replaceable sound sink. `SceneState` owns background maps and palettes.
`ScreenState` owns the current screen, file-menu/save controllers and save slot.
`DebugState` owns no-clip and the optional menu/overlay; `DebugTools` creates the
latter lazily, at their original input/presentation gates. `GameplayInputState`
tracks consumed input, `SessionOptions` holds live configuration, `SaveState`
owns replaceable storage, and `RunState` handles stop requests. Headless setup
uses typed accessors rather than reflection into private session fields.

The enforceable construction rule is: only `app.wiring` constructs collaborators
**across app domains**, where a domain is the first subpackage under `app`.
Domain-local helpers may construct each other; state/service values, entry types
and diagnostic snapshots are exempt. For example, gameplay phases own their
local adapters and recovery presentations, while wiring connects gameplay to
input, screens and audio. `app.gameplay`, `app.screen` and `app.story` cannot
reference wiring. Only `app.host` may reference LWJGL. Top-level `gameplay` cannot
depend on any `app` type. `architecture.AppPackageTest` checks these rules through
the shared class-file reader, including constructor method references.

Consumer-owned ports express cross-domain dependencies: `ScreenNavigator`,
`SaveActions`, `SessionLifecycle`, `InputCompletion`, `GameplayScreenDriver`,
`MusicRequests`, `DialogSoundRouting`, `DialogBlockers`, `GameOverActions` and
`DebugScroll`. `SessionNavigator` is the sole two-phase attachment: create it
before ROM/UI/world wiring, then attach screens, routes and room-music selection
once before publishing the session. `OpeningRestoration` remains for ordered
save-restoration fakes. `GameplayFrame.Transitions`/`Simulation`,
`SimulationStages`, `EntityFrameStages`, `EntityDialogRouting` and
`LinkMotionDriver` retain recording fakes for ordering checks. `Host` and `Screen`
have multiple implementations. The former local `DebugActions` and
`TeleportTarget` interfaces had no cycle or fake and were removed.
Legacy UI/host callback adaptation creates no session back-call fields; debug
editor bindings still use functional reads/writes for editable values.

`GameOverFlow`, `DialogBlockerPolicy`, `DebugTeleporter`, `SessionInput`,
`FramePresentation` and `SessionShutdown` own their respective behavior.
Required service references are validated at construction. Inactive story
motions, closed menus and absent active swords remain nullable state.

`HeadlessGameDriver` retains its public input/stepping API. Its nested `State`
and `Diagnostics` implement the `HeadlessState` and `SessionDiagnostics`
contracts and compose `ReplayCheckpoint` and `SessionDiagnosticSnapshot`.
The checkpoint owns its values, equality, hashing and text format. Diagnostics
retain live views with the captured frame number. These are composed public
entry names, with no implementation superclass or separate value-method slice.
Three **test-source** forwarding helpers preserve old static names used by tests
outside the app ownership boundary; they add no production entry types.

App/gameplay classes document their role and lifetime and follow the composition
and cohesion rules above. Per-frame orchestration retains its collaborators;
headless diagnostic snapshots are constructed only when requested.

### `app.screen`

`Screen` implementations own title, cutscene, file-menu, file-save and gameplay
lifecycle. `app.input.GameplayScreenAdapter` connects gameplay to input
interception, the retained frame runner and recovery presentation.
`GameplayScreen` retains primitive key buffers: queued edges are detached before
dispatch, new edges wait for the next frame, and save interception discards the
pending batch. Input completion still happens only at its original frame gate.

### `app.gameplay`

`GameplaySimulation`, its phase instances, `GameplayFrame` and `TransitionPhase`
are built once. Phases read live state through simulation accessors.
`GameplayFrame.tick()` runs transitions, simulation and input completion;
`SimulationStages` preserves story/recovery early exits. `EntityFrameStages`
ticks the world and then applies results in their original order, passing the
same sword box and projectile events through consumers without dispatch-list
or closure allocation.

In `app.gameplay.entity`, `EntityFrameInputsFactory` captures a new immutable
frame-number record and reuses unchanged immutable leaves through
`EntityInputCache`. `FrameInputCache` similarly reuses ground, pickup and combat
values; neither cache mutates a value already returned to a caller.
`LinkWriteApplier` translates ordered Link requests, `EntityRewardApplier` drains
synchronous reward batches, and `GameplayCombatEvents` implements damage outcomes
and the room combat drain. `LinkCommandAdapter` is the world-to-Link command
translation boundary. The world tick itself remains world-owned.

`LinkEquipmentPhase` retains `ActiveLinkPhase` for lifting, equipment, motion,
doors, pickup and transition work; `LinkGroundPhase` preserves pre/post-motion
ground sampling. Interaction policy lives in `app.gameplay.interaction`, including
`LiftButtons` for edge/B-slot priority. Pass-out animation and game-over dispatch
live in `app.gameplay.recovery`. `app.wiring.RoomBombPlacement` adapts successful
bomb placement to Link's animation write.

### `app.story`

`OpeningStory` reads live clock/motion holders from `app.state` and delegates music
through `MusicRequests`. `OpeningActions` names its dependencies;
`OpeningRestoration` names the ordered save-restoration operations.

### `audio`

ROM sequencing, deterministic synthesis, pure sample transport and host output.
Only `audio.openal` may reference LWJGL. Browser models are headless.
Key types: MusicDriver, MusicVoiceEmitter, GameBoyApu, FrameAudioMixer, OpenAlStream.

### `config`

Application configuration and launch preferences through a strict JSON reader.
Malformed JSON fails with a source position; no JSON dependency is required.
Key types: AppConfig, MiniJson.

### `cutscene`

Scripted intro orchestration and ROM-backed intro data.
Key types: CutsceneManager, IntroSequence, IntroRomData, IntroBeachActors,
IntroWater, IntroSpriteComposer and IntroFramePublisher.

IntroSequence is a final stage orchestrator. Beach actors own positions and
motion timers; water owns wave fractions and offsets. Sprite composition owns
its decoded tables, scratch lists and RNG, preserving original call order.
Frame publication owns palettes, delayed scene snapshots and its retained
builder. IntroPhaseState, IntroClock and IntroVideoMemory are small explicit
state objects for shared stage, time and hardware-map/scroll data. Palette
arithmetic and default-ROM construction have separate, stateless responsibilities.

Entry points accept Rom with deprecated byte-array compatibility delegates.
IntroRomData preserves its defensive copy and format bounds. Pre-update snapshot
timing, frame counters and immutable published copies retain their original
semantics; no collaborator receives the IntroSequence owner.

### `dialog`

Dialog state, ROM text layout, font and box rendering.
Key types: DialogController, DialogText, DialogRomLayout and DialogRenderer.

DialogController owns the cohesive reveal/scroll/choice state machine and
queued sound requests. DialogText owns name substitution and width-dependent
layout, independently of dialog timing. Both are final and own private state;
the former lifecycle/paging/formatting superclass layers are removed.
Input edges, reveal timing, scrolling, selection and sound order are preserved.
Font/layout readers use typed Rom values. Sound consumption reuses immutable
single-event lists and copies multiple-event lists to preserve retention.
ROM-free allocation guards exercise printing ticks and sound consumption.

### `entity`

Link composition, motion, commands and shared sprite definitions.
Room entity simulation lives in world.entity.
Key types: Link, LinkAssembly, LinkRoles, LinkPositionRole, LinkMotionRole,
LinkAnimationRole, LinkActionsRole, LinkStatusRole and EntitySpriteCatalog.

Link is a final aggregate with direct retained role accessors and no superclass.
Nine narrow interfaces expose position, motion, animation, actions, status,
equipment, airborne motion, side-view motion and controls. Their implementations
receive components, never Link. LinkAssembly binds cyclic component references
once before publication. Update delegates to the retained motion orchestration;
the motion command role also coordinates aggregate debug reset. The world route
fixture binds Roc's Feather to the airborne role directly; the remaining
JumpTarget adapter stays within that component owner's scope.

LinkStateMachine dispatches frame phases and composes LinkDash, LinkRoomPhysics
and LinkDoorTraversal. They own charge/rebound/shake, room defaults and door
traversal respectively. LinkMotionStatus contains explicit shared mode/recovery
gates. LinkMotion composes LinkPosition for XY subpixels, speeds and position
shadows, plus LinkCollisionState for probe/mask/door-warp state. LinkAirborne
owns jump/landing orchestration, LinkPitFall owns pit probing and fall timing,
and LinkElevation is their explicit height/speed state. These state fields are
private; no implementation superclass remains.

LinkAnimator owns pose selection and scripted presentations. LinkSpriteComposer
receives selected pose values and owns body tile composition/equipment draw
order; LinkPoseTables holds fallback data. Animation facing is Direction;
Java/ROM conversions are explicit. Raw snapshots and permissive equipment
boundaries retain their integer representation and documented fallback behavior.

EntitySpriteHandlerCatalog composes immutable registration recipes, their
context-sensitive lookup and bounded ROM decoding. Sixteen family builders
produce EntitySpriteRecipe values; EntitySpriteRegistrationData indexes them
once. EntitySpriteDecoder owns ROM access. BossSpriteComposer, EntitySpriteStates
and EntityEffectSprites own boss geometry, state-dependent selection and transient
effect composition; EntitySpriteGeometry supplies pure OAM coordinate algorithms.
First-match priority, map-byte selectors, bank bounds and palette/tile-source
choices remain unchanged. EntitySpriteCatalog passes Rom directly to the
world-owned RoomPaletteLoader. ROM-free allocation guards cover walking with
repeated sword swings.

### `equipment`

Equipped item behavior, activation and item sprite support.
Key types: EquipmentController, EquipmentItem, ItemRegistry, Sword, SwordSpin,
SwordRenderer and SwordPalette.

Sword is a final swing/charge orchestrator with private animation and movement
state. SwordSpin independently owns queued-start delay, spin frames, sector and
base direction; it returns pose values and never receives Sword or its state.
The existing renderer and palette objects handle blade presentation. Themed
SwordTables and EquipmentTables retain ROM lookup boundaries. Primitive frame
counters, held/input-edge priority and sound/RNG ordering are preserved.
Owned player and Link callers use composed APIs. Sprite/palette loaders accept
Rom; deprecated byte-array delegates retain external source and partial-buffer
compatibility.

### `gameplay`

Gameplay integration contracts, dialog interactions, sound routing and event consumers.
Key types: EnemyCombatEventConsumer, EnemyProjectileEventConsumer, PcmSoundOutput.

Owned consumers address focused PlayerState components. The sound boundary
adapts resource requests to presentation events through PlayerSounds; PlayerState
has no gameplay sound compatibility bridge. Signpost
interaction uses its seven-argument entry point and `Interaction` bundle.
`DamageConsequences` names the contact-hurt and power-up-ended effects requested
by projectile consumption. Its app implementor is retained; the older callback
parameter overloads remain for compatibility tests.

`DialogTextLoader`, `SignpostDialogTable` and ROM-backed sound construction accept
`Rom`. Dialog text retains its owned snapshot and bounds behavior; its unused
byte-array bridge is deleted. App passes `Rom` through all construction paths,
including the five former world wiring byte handoffs. Their deprecated overloads
are deleted after migration of main and test clients.

App/gameplay main and test callers use PlayerState's component APIs and Link's
composed roles, including bound method references. Link construction uses
`Link.Services`, and Roc's Feather targets the airborne role directly. No owned
caller depends on the deprecated inheritance bridges; a full temporary
`-Xlint:deprecation,removal` compile reports zero owned removal warnings.
State enum values replace deprecated inventory, tunic, power-up and direction
constants, with explicit ROM/engine index conversion at integer boundaries.

### `gpu`

VRAM, tile decoding, animation and named ROM tile loaders behind the GPU facade.
Key types: GPU, Vram, AnimatedTiles, RoomTileLoader.

### `input`

Native-free bindings and held/edge state with stable numeric engine key codes.
DesktopHost translates GLFW actions at the callback boundary; edge ordering is unchanged.
Key types: InputConfig, InputState, KeyCodes.

### `physics`

Room collision queries and physical flags.
Key types: OverworldCollision and PhysicsFlags.

OverworldCollision retains themed PhysicsTables and CollisionTables from its
existing injection boundary. Object-grid ownership, bounds, sub-pixel arithmetic
and source collision ordering are unchanged. Physics remains input-free,
enforced by StateDependencyTest.

### `render`

Frame scenes, indexed planes, OAM submission, hardware composition and GL presentation.
Key types: FrameScene, HardwareFrame, OamSink, OpenGlFramePresenter.

### `rom`

Typed ROM addresses, symbols, bank mapping and shared tables.
Key types: Rom, RomAddress, RomSymbols, GbcBanks.

Rom construction uses validated `of` or explicitly alignment-unchecked
`unchecked`; its constructor is private. Tests use `TestRoms.partial` for
synthetic buffers. RomTables exposes themed table holders only: all deprecated
aggregate methods and RomUtil delegates are removed. The five app wiring byte
handoffs are migrated, and no backing-array escape hatch remains. Shared storage,
partial-buffer validation timing and read bounds are unchanged.

### `save`

Source-shaped SRAM layout, image operations, player-field codec and host persistence.
Wrong-sized saves are copied to timestamped corrupt backups before initialization.
Persistence writes a complete temporary image then replaces the destination atomically
where supported, with a replacement-move fallback.
Key types: SaveRamStore, SaveRamLayout, SaveRamImage, SaveSlotCodec, SaveSlotState.

### `scene`

ROM-backed background scene catalog and loading.
Key types: BackgroundSceneLoader, BackgroundSceneSpec and BackgroundSceneCatalog.

BackgroundSceneLoader retains Rom and uses typed draw-command and palette
readers. Scene-specification address conversion retains the legacy file-offset
semantics, including CPU-window aliases. Partial attrmap writes preserve selection-screen state for file copy
and erase scenes. The legacy byte-array constructor remains a deprecated
compatibility delegate. Intro and menu controllers own presentation state.

### `startup`

New-game profiles and wake-up/shield motion state machines.
Key types: StartupCoordinator, NewGameStartProfile, MarinWakeUpMotion and TarinShieldMotion.

Startup calls PlayerState components and Link roles while preserving the
opening-motion timers and presentation order. NewGameStartProfile supplies a
named builder alongside its deprecated canonical constructor.

### `state`

Player resources, progression, ROM value types and save-value application.
Key types: PlayerState, Inventory, Vitals, Ammunition, Progression, Wallet,
EquipmentLevels, PlayerInitialization, PlayerSaveLoader and ResourceSound.

PlayerState is a final aggregate with no superclass or deprecated members. It
owns and directly exposes its components. The former LinkPlayerState interface
and all PlayerState compatibility layers have been deleted; Link behavior
addresses the components directly.
The external migration work list is in
[link-player-api-migration.md](archive/history/link-player-api-migration.md).

Initialization and save loading preserve write order and raw saved inventory
bytes. Reward components accept state-owned values without world dependencies.
Resource sounds are last-write-wins values mapped by the presentation consumer.
StateDependencyTest enforces the state/rom/save boundary without exceptions and
also checks that physics remains input-free. Composition guards require both
aggregates to extend Object directly; the class-file inheritance guard applies
the same composition rule to all main packages.

### `ui`

Inventory, HUD and file-menu controllers and overlays.
Key types: InventoryController, InventoryMenu, HudOverlay, FileMenuController,
FileNameEntry and TilemapRenderer.

Inventory rendering and synchronous HardwareFrame submission reuse tile and
attribute scratch buffers. Numeric and palette pairs use primitive values;
region/row arguments are retained. Published closed-window snapshots still own
independent arrays. ROM-free guards cover the fully open HUD/inventory update
and submission path, including snapshot retention checks.

UI ROM readers accept Rom. FileMenuRomData preserves its defensive copy,
including support for legacy partial synthetic buffers. FileMenuController
owns slot/copy/erase transitions and frame publication; its FileNameEntry
collaborator owns character selection, insertion position and name bytes. Both
are final with private state. Published frame snapshots remain immutable and
use retained builders. OcarinaSong supplies typed flag and selection conversions.

### `vfx`

Transient effects, source-shaped lifecycles and ROM-backed effect sprites.
Key types: TransientVfxSystem and TransientVfxSpriteSheet.

Effect iteration retains source order. Sprite loading accepts Rom with a
deprecated byte-array delegate for external callers. Published placement lists
retain independent ownership; mutable scratch storage is used only where
consumed synchronously.

### `world`

World ownership namespace partitioned into rooms, transitions, objects and entities. No direct app dependency is allowed.
Key types: room, transition, object, entity.

### `world.room`

Room facade, decoded room state, loading, palettes, tilemaps, mutations and entity integration.
Key types: RoomSession, RoomSessionContext, RoomLoadCoordinator, RoomObjectMutations.

### `world.transition`

Boundary decisions, scrolling, warps and transition progression.
Key types: RoomBoundaryController, ScrollController, TransitionController, Warp.

### `world.object`

Object-specific interactions and switch-block behavior.
Key types: OverworldBushInteraction, BombObjectInteraction, SwitchBlockAnimation.

### `world.entity`

ROM slot orchestration, owned state, scoped controller services, collision and frame requests.
Key types: RoomEntityRuntime, EntityMotionRegistry, EntityPhysicsTables, EntityCombatTables, EntityFrameExecution.

### `world.entity.handler`

Explicit type registration and ordered dispatch-site callbacks, internal to entity simulation. Handlers do not depend on world.room.
Key types: EntityHandlerFamilies, BombHandler, RoamingEnemyTypeHandler.

### `world.entity.motion`

Entity-owned slot state and state-machine calculations, acquired through the typed motion registry.
Key types: BombMotion, MoldormMotion, FollowingNpcMotion.

### `world.entity.event`

Value events and requests exchanged across the entity boundary.
Key types: EntityEvent, EntityRequest, EntityCombatEventBuilder, DialogRequest, LinkFinalPositionRequest.

### Dependency boundaries

Arrows show permitted use across the principal boundaries, not every import.
The app uses an explicit world facade allow-list and event contracts.
Handlers and motion remain internal to world.entity.

```mermaid
flowchart TD
  app --> host[app.host]
  app --> room[world.room facades]
  app --> events[world.entity.event]
  app --> render
  room --> runtime[world.entity runtime]
  runtime --> handlers[world.entity.handler]
  runtime --> motion[world.entity.motion]
  runtime --> events
  room --> transition[world.transition]
  room --> objects[world.object]
  room --> rom
  render --> gpu
  gpu --> rom
```

`ClassFileReferences` in `java/src/test/java/linksawakening/architecture`
reads symbolic constant-pool references, including descriptors, generic
signatures and field references. `WorldDependencyTest` uses this shared reader.
It rejects world-to-app references, handler-to-room references and external
use of motion/handlers, and checks the explicit app/world facade surface.
Event dependencies must be event contracts or record/enum values. The app
allow-list names the used role views, boundary records and construction/resource
facades. A second rule applies the same boundary to every non-world main client,
with seven explicit stable sprite/OAM/physics contracts. A visibility check scans
main and test constant pools: a public top-level world type needs a cross-package
client or an explicit public-contract entry. Other implementation types stay
package-private; Java subpackages still require public cross-package entity
services. The reader accepts both main and test class roots.
`AppBoundaryTest` checks entry-point/session ownership, screen lifecycle types,
native dependency restrictions and the canonical bomb-placement role API. It also
rejects session/composition references from domain collaborators and guards the
eight-parameter limit. Structural class inspection never reads private runtime state.
`GameplayFrameOrderTest`, `SimulationOrderTest`, `EntityFrameOrderTest` and
`EntityFrameSessionTest` observe lifecycle order, early exits and consumer
side effects. `RoomLinkWritesTest` checks descending merges and tie precedence;
Link command and bomb-placement tests check real player/Link effects.
`FileMenuBehaviorTest` exercises persistence, room entry and opening-story gates
in headless sessions. None shells out to `javap` or matches disassembly text.
The complete old-to-new mapping is recorded in
[the architecture-test migration](archive/history/room-architecture-test-migration.md).
`GlBoundaryTest` in `java/src/test/java/linksawakening/render` scans compiled
gpu/render classes: only presenter classes may reference LWJGL there.
These are specific guardrails, not a universal acyclic-package rule.

## Frame lifecycle

`Host.pollEvents()` delivers keys to `SessionInput`, which handles an open debug
menu before `InputRouter`. An open debug menu pauses the update, including audio
and RNG advancement. Ordinary screen input retains its original edge ownership.
`GameSession.update()` advances `FrameClock` modulo 256 and begins the
ROM random frame, then ticks `MusicDirector` before ticking the active screen.
Thus sound requests produced during simulation are sequenced after this
frame's audio advance; moving audio after entities changes timing.

For gameplay, `GameplayFrame` executes TRANSITIONS, SIMULATION and
INPUT_COMPLETION in that order. Simulation can return early at story gates;
normal completion still advances input edges. `FrameInputCompletion` ticks edges
before clearing the consumed-input flag. Exceptions do not advance them.
`GameplaySimulation` runs dialog/story, Link/equipment, combat, entities,
dialog gameplay cooldown and animated tiles in source order.
Scrolling, transitions and inventory state gate entity ticking.

The runtime iterates slots from highest index to zero. Each slot passes
`EntitySlotPhase1` through `EntitySlotPhase5`; a phase can end that slot's work.
`app.gameplay.entity.EntityPhase` builds immutable `EntityFrameInputs` from Link, player,
UI, buttons and sword-box snapshots through `EntityFrameInputsFactory`.
`LinkWriteApplier` applies ordered Link effects while `EntityPhase` orchestrates
combat and presentation and delegates rewards to `EntityRewardApplier`.
`RoomSession.tickEntities` composes `RoomFrameInputApplier`, which applies inputs
in the original setter order before advancing entity slots. Immutable component
records are reused within the tick-only input value rather than copied again.
`EntityTickInputs` is the tick-only boundary for callers that retain cached
inputs; combat and pickup boundaries use their own parameter objects.
Entity effects are consumed at the entity boundary, including projectiles,
combat, rewards, dialog and music requests. Link writes become `LinkCommand`
values through `LinkCommandAdapter` and are applied through `Link.apply`.
Competing Link writes retain descending source-slot ordering. `RoomLinkWrites`
merges position streams with pot/platform/final-position tie precedence and
attack streams with attack-step precedence on ties. These are separate
consumption stages: a platform write can load a room before later outputs
are drained. `EntityFrameResults` groups live outbox views by Link, rewards
and presentation. The facade caches these live views; it does not eagerly
snapshot every channel. Synchronous `LinkWriteSink` visitors drain every competing
stream before applying any merged write. Reward and combat `RoomFrameDrain`
callbacks receive reusable read-only batches valid only during the callback;
queue clearing still precedes consumer effects. Snapshot-returning `consume`
methods remain immutable for clients that retain results. The dialog boundary
drains all requests before selecting the first one to open.

After simulation, `FramePresentation` asks `RenderStateAssembler` for scene
composition and then overlays the optional debug menu. The assembler refills
one `ReusableGameFrameState` for scene construction, without intermediate copies. Indexed layers and OAM entries feed hardware composition.
`FrameScene` composes RGBA, and the host presents the resulting pixels.
Desktop pacing is owned by `FramePacer`, after update, composition and presentation.
It carries the remainder of one billion nanoseconds divided by 60, parks until
100 microseconds before the deadline, then uses a short spin. Each wakeup rereads
the monotonic clock. Overruns retain pending deadlines, so elapsed simulation
steps are caught up rather than discarded. Interruption exits pacing without
clearing the interrupt. GLFW swap interval is zero: refresh-rate blocking must
not establish a second cadence. Fake-clock tests cover drift, oversleep,
spurious wakeups, catch-up, interruption and nanosecond-clock wraparound. Native
presentation was not opened for this refactor. OpenAL retains its three-frame
prefill, underrun recovery and queue-pressure rebuffering policy.

Headless stepping is unpaced and composes the same framebuffer without a native
window. `headlessFrame()` still returns an owned capture; the retained-output
`headlessFrame(byte[])` overload runs the same full path and copies pixels into
caller-owned storage. `WholeFrameAllocationTest` measures that full overload,
including simulation, assembly, scene build and composition, after reaching
opening-dialog idle, house walking with entities, and open inventory by ordinary
input. This avoids counting the separate allocation required for a new owned
capture buffer. Pixel tests compare both capture APIs and compare the running
borrowed renderer with owned reference scenes through inventory transitions.

`RenderStateAssembler` retains one `ReusableGameFrameState`, refills it from the
same live collaborators and screen overrides, then uses the reusable scene
builder. Its room is `rooms().renderView()`; indexed room planes are copied into
retained renderer buffers without allocating. The common render contract is
`RoomRenderView`, implemented by immutable `RoomRenderSnapshot` and live views.
`currentGameFrameState()` remains the explicit owned room/capture path. The
world's `dungeon().view().copyTo(...)` fills retained eight/five/64-byte inventory
buffers; UI setters retain their original copying contracts. Historical scroll
rooms continue to use owned snapshots.

With escape analysis disabled, whole-frame allocation fell from approximately
27,320 / 28,315 / 20,344 to 1,072 / 1,029 / 720 B/frame (idle / walking / inventory).
Idle and inventory residuals are entirely in off-limits dialog/UI/startup
producers; owned code contributes zero steady-state allocation in those samples.
Walking includes changed immutable coordinate/tick inputs, preserving ownership,
and is below the 2 KB whole-frame target. Reusable assembly and scene construction
measure zero B/frame. `SessionAllocationTest` separately measures update-only
work at 64 idle and approximately 673 active B/frame EA off (0 / 641 EA on),
guarded at 80 and 842.
The [round-5 report](archive/history/room-integration-round-5.md) records both EA
modes, attribution, scope and budgets. The merged recording feature adds zero
steady bytes/frame with recording disabled on the default reusable PCM host path;
its capture lifecycle and tests are preserved. See [TESTING.md](TESTING.md#performance-budget)
for every allocation guard. Stage guards are not whole-session parity claims.

```mermaid
sequenceDiagram
  participant H as Host
  participant S as GameSession
  participant A as MusicDirector
  participant G as GameplayFrame
  participant E as RoomEntityRuntime
  participant L as Link
  participant R as RenderStateAssembler / FrameScene
  H->>S: poll events / route input
  S->>A: tick audio
  S->>G: tick active gameplay screen
  G->>G: transitions
  G->>L: dialog/story then Link/equipment
  G->>G: combat
  G->>E: entity frame inputs
  loop descending slots
    E->>E: phases 1 through 5 / handler stages
  end
  E-->>G: outbox events and requests
  G->>L: adapt and apply LinkCommand
  G->>G: consume rewards/dialog/music; cooldown; tiles
  G->>G: complete input edges
  S->>R: assemble and compose hardware frame
  R-->>H: RGBA pixels for presentation
```

## Entity system

`RoomEntityRuntime` is the source-compatible room facade. It contains an
`EntityRuntimeState`; neither class inherits fields or constants. The state
composition has nine references, including four cohesive stores:
`EntityPhysicsTables`, `EntityCombatTables`, `EntityRoomVariables` and
`EntityPresentationState`. Their fields are private and accessed through slot
or room methods. `EntityRomConstants` is a final, non-instantiable holder; callers
use it directly instead of facade aliases.

Per-type state belongs to its motion or controller: for example Iron Mask,
Moldorm, Rolling Bones and Moblin King retain their own slot latches, Genie
owns its jar/body/projectile coordination, and the story controller owns owl
state. `EntityMotionRegistry` lazily supplies one typed motion instance per
room through `get(MotionClass.class, MotionClass::new)`. Adding a motion does
not add a field to central runtime state. Controllers receive narrowly scoped
service interfaces; bindings connect those capabilities to the room facade.
Stateless behavior helpers operate on their owner's state. Longer operations
use private, reusable scratch and named state handlers. Each scratch holder
resets invocation values before use and releases input references afterward;
a nested call uses a separate holder so callbacks cannot overwrite the outer
invocation. Persistent slot state remains in the motion/controller owner.

`EntitySlotOrchestrator` runs the five slot phases in descending slot order.
`RoomEntity` remains an immutable record whose slot and load order are ROM
identity. Snapshot helpers combine related field writes into one replacement
and reuse unchanged values where fresh identity is not required. Lifecycle and
spawn helpers preserve slot allocation, reset and early-return sites. Do not compact slots, sort by type, add resets on slot reuse or
move RNG reads and byte writes across their original execution points.

`EntityHandlerRegistry` has 256 type bindings for initialization, generic
advance and stage hooks. Handlers register explicit type IDs and named static
callbacks. Their initializer and advance registrations live beside their tick
hooks. `EntityHandlerFamilies.INSTALLERS` is the ordered installation list;
duplicate registrations fail and unsupported bindings remain explicit.

`EntityTickStage` describes 27 dispatch sites between the phase operations.
Each site retains ordered visit positions, preserving all 141 original visits.
Dispatch reads the current slot type at each visit, so an earlier hook that
replaces or transforms an entity affects subsequent selection exactly as
before. A false callback stops the slot immediately. Site names and visit
positions describe source order; enum order and numeric entity IDs do not
establish the schedule.

`EntityFrameContext` is immutable frame input. Use its builder or `toBuilder`
when changing Link state, collision services, RNG or interaction gates; service
references retain their identity. `EntityFrameExecution` exposes operations,
not public mutable fields. Its private scratch is grouped into frame inputs,
timer gates, slot execution and presentation flags. The runtime owns one
reusable execution object; acquisition resets the frame inputs and gates,
and each descending slot visit resets slot scratch and presentation flags.
The orchestrator releases the frame in a finally block. Reentrant frame calls
use separate execution state. Frame setup and commit remain separate from
individual handler behavior.

`EntityRoomInspection.snapshot()` captures an owned immutable copy for tests,
retained state and consumers that require ownership. `view()` returns a cached
read-only live `RoomEntitySnapshot` for immediate same-thread consumption.
Its slot lists and presentation metadata follow the runtime; it must not be
used as a historical frame or shared across threads. A `withX` operation on a
live view materializes owned data before transforming it. Moldorm tail
presentation similarly reuses immutable segment lists only while their
coordinates are unchanged.

`EntityTickAllocationTest` measures a ROM-free six-entity mix: roaming enemies,
a projectile, active Moldorm, a droppable and an NPC, with three inspection
reads per tick. It retains frame inputs and repositions the same slots without
allocation, while motion state and timers continue advancing. Its 1,100-byte
limit provides roughly 25% headroom over the optimized steady-state result.
Measure with escape analysis both enabled and disabled using temporary local
JVM arguments; do not commit test-runner configuration changes. Immutable
changed entities, motion results and emitted events still allocate.

Typed events and requests accumulate in the entity outbox rather than calling
app orchestration from handlers. Room/app consumers apply Link, progression,
dialog, music and transient effects. Source slots preserve the order of
multiple Link writes in a frame. Outbox channels are cached, index-based views;
size, clear and append operations do not create iterators, predicates or channel
wrappers per tick. Draining events still returns an owned result.
`EntityCombatEvent.builder` and `EntityProjectileEvent.builder` name optional
effects without positional argument lists.

Entity ROM readers accept `linksawakening.rom.Rom` and use typed addresses and
reads. Collision and property queries use `CollisionTables` and
`EntityPropertyTables`. All entity byte-array delegates and aggregate-table
adapters have been removed. Room, entity and sprite catalogs share the typed
session Rom directly. Room-status arrays are mutable RAM state, not ROM buffers.

Use `RoomEntityRuntime.builder(snapshot)` for runtime construction. Test-side
`EntityRuntimeFixture`, inspection fixtures and `RoomEntityBuilder` provide
setup and queries without production test hooks. Runtime tests are split by
entity family and behavior. Room consumers now use runtime/frame builders,
`EntityPickupFrame`, `EnemyCombatFrame`, `EntitySwordContactFrame` and
`FollowingNpcSpawn`; the old `from`, `ForTest`, frame and wide operation adapters
are deleted. The room simulation reuses its builder but emits an immutable
`EntityFrameContext`, so retained frame inputs cannot change. Its default
handler collision gate is derived from the projectile Link motion state.
Combat/projectile consumer tests use named event builders; the four former
constructor adapters are deleted. Immutable record schemas retain their canonical constructors
and validation; value schemas are the exception to the normal eight-parameter
method limit. See [Entity API migration](archive/history/entity-api-migration.md).

To add an entity type:

1. Add its motion/state owner in `world.entity.motion`, using the disassembly's
   exact initialization, timers, byte arithmetic and RNG ordering.
2. Add its handler in `world.entity.handler`. Register initialization, advance
   and the required site/visit hooks there, acquire its motion through the
   typed registry, and emit typed events for effects outside the entity tree.
3. Add one `NewEntityHandler::install` entry to
   `EntityHandlerFamilies.INSTALLERS`.

Add focused behavior coverage using the test fixtures and ROM-backed tables;
never add extracted assets. For this refactor the required headless oracle is
`LA_ROM=/path/to/user/rom ./gradlew spotlessApply test` from `java/`, including
its default session replays. Explicitly tagged `known-gap` routes are excluded
from that suite and are not refactor validation targets. Preserve existing
consumer and sprite contracts when implementing a handler within the current
entity boundary.

## Rooms and world

`world.room.RoomSession` is the public room API. It delegates to room-scoped
collaborators instead of exposing entity handlers to application code.
The `doors()`, `transitions()`, `playerItems()`, `story()`, `entities()`,
`entityState()`, `rooms()`, `dungeon()`, `progress()` and `terrain()` views expose
focused, cached role interfaces. `RoomSession` has no superclass or compatibility
interfaces; roles and frame adapters are composed. Boundary records, including
`LinkGroundFrame`, `ShovelStartResult`, `ArrowShotResult` and `ChestOpenResult`,
are top-level types. Construction takes `RoomSessionConfig`.

All 26 former entity-test bridges, the legacy constructor and the private
reflection alias have been deleted. `RoomSession` has no deprecated members.
Tests use role views and test-side `RoomDebugFixture` / `RoomFrameTestAccess`
for internal setup and queries. All deprecated members in world/render/gpu/audio/rom/app/gameplay have been
removed after their callers migrated. Room wiring accepts `Rom` directly; no
`unsafeBytes()` accessor or call remains. Bounded `RomSlice` reads let audio
retain shared read-only storage without exposing the backing array or allocating
an address per byte. Unaligned synthetic/legacy input uses the supported unchecked
factory; validation, bounds and backing storage are preserved.
View implementations receive only their specific collaborators and state.
`RoomSessionContext` is private construction-time wiring. Runtime collaborators
receive their own dependency records and typed state: location, terrain,
followers, entity-frame state, item use and puzzles. No runtime collaborator
reads through the composition root. Typed service suppliers defer cyclic
room references until the room is wired.
`RoomWorldState`, `RoomTransientState` and `RoomStoryState` separate persistent
world concerns, transient state and story progression.
`ActiveRoom` and `RoomRenderSnapshot` provide room information to consumers.
Immediate room integration uses `EntityRoomInspection.view()` and the live
`ActiveRoom.entityView()`. Per-frame rendering uses `RoomRenderView` and the
cached room/dungeon views, with indexed read-only access. They are valid only
until the next simulation tick, room change or world mutation on the same thread;
never retain them as history or share them across threads. Public `ActiveRoom.entities()` and render snapshots
retain owned immutable entity state; a bounded eight-presentation cache avoids
copying recurring unchanged poses while preserving historical snapshots.

`EntityFrameInputs` composes Link, player, UI, button and sword values. Link
facings and motion dispatch use `Direction` and `LinkMotionState`. Raw values
are retained alongside classifications where ROM gates accept unknown bytes.
Inventory construction accepts `InventoryItem`, `Tunic` and `PowerUp`, retaining
saved raw item bytes and the original flag-only inputs. Known ocarina selectors
use `OcarinaSong`; unknown selections and song flags remain bytes. Entity ground
codes are a separate ROM domain from Link's `GroundStatus` (entity water is
`$02`), so they must not be converted by matching integer values.

`RoomLoadCoordinator` coordinates room loading; `RoomLoader` reads room streams,
`RoomObjectParser` decodes objects, and `RoomTilemapBuilder` constructs tilemaps.
`RoomPaletteLoader` owns palette decoding. Pointer tables and tileset tables
retain bank/page selection rather than flattening ROM addressing. All internal
ROM parameters, fields and reads use `Rom` and typed addresses. The five former app wiring byte entry points and the unused palette byte
constructor are deleted; callers share the typed session `Rom`.
See [ROM_FORMAT_NOTES.md](ROM_FORMAT_NOTES.md) for stream and macro details.

`RoomTransitionCoordinator` and room transition collaborators connect room
changes to scrolling and transition state. `world.transition` owns boundary
and warp mechanics. Keep load timing, input blocking and transition progress
intact: a visually plausible destination alone does not verify a transition.

`RoomObjectMutations` and room redraw helpers coordinate object changes with
room representation. Door, puzzle and player-item collaborators implement
specific interactions; `world.object` contains object-specific behavior.
Entity integration passes through room bridges, inputs and reward handling.
`RoomEntityBridge` coordinates ticking; `RoomEntityFrameSetup` configures
the runtime, `RoomEntityAfterTick` applies ordered room effects,
`RoomEntityCombatBridge` handles combat/pickup integration and
`RoomEntityOutbox` owns queued outputs. The outbox uses a runtime supplier
because loading a room replaces the runtime. Results expose grouped Link,
reward and presentation consumers. Link writes merge by descending source slot
with the established cross-stream tie precedence; the application applies the
ordered writes. Empty outbox drains and merges allocate zero bytes, guarded by
`RoomLiveBoundaryTest`. Nonempty owned-list drains copy before clearing; reusable
consumer drains retain their existing staged-clear and reentrancy behavior.

At the end of round 4, the headless session allocation probe measured 3,312 idle and approximately
7,457 active bytes/frame after this seam refactor, down from 3,979 and 11,080.
`EntityTickAllocationTest` remains 874.43 bytes/tick for its six-entity mix.
Immutable inputs and changed entities/events still allocate. A warmed JFR
allocation-site diagnostic attributes the five largest remaining idle sites to
PCM audio arrays, screen/dialog iterators and startup Marin result records,
all outside `world`. The [round-4 report](archive/history/room-seam-round-4.md)
records scope, exact sites and the separate diagnostic rates.
The integrated round-5 measurements and current attribution appear in the frame
lifecycle section above; the round-4 figures describe its historical scope.
Changing a visual tile alone is insufficient when collision or persistent room
status also changes.

## Rendering

[RENDERING.md](RENDERING.md) is the detailed compositor contract.
Software layers submit indexed BG/window pixels and OBJ entries.
`GPU` is a facade over VRAM, animation and ROM tile loaders.
`OpenGlFramePresenter` owns GL presentation; gameplay does not issue GL calls.
Hardware-only `FrameScene` composition produces RGBA once. Optional completed-frame
observers attach explicitly through the draw overload or session-local builder;
production performs no service lookup. RGBA callback scenes live in tests.

CGB OBJ-to-background/window composition uses color-zero transparency,
both priority flags and LCDC bit 0. OBJ-to-OBJ ordering currently retains the
emulator-validated legacy painter order. It does not yet implement the ROM's
dynamic OAM allocator. An 8x16 entry retains its original Y for both halves.
Dialog and inventory hiding operate on whole entries using ROM visibility rules.

The parity harness compares hardware output to a test-only RGBA reference.
Strict parity rejects unexplained differences; expected fixes require narrow
source and pixel evidence. Independent emulator fixtures remain necessary
because two engine implementations can share a mistake.
`RenderPipelineParityTest` and `SceneParityExtension` produce local reports in
`java/build/scene-parity`; `-PsceneParityProbe=true` records mismatch groups.
ROM-derived screenshots and reports containing assets remain local.

## Audio

See [audio-engine-guide.md](audio-engine-guide.md) for sequencing and synthesis.
Music and SFX drivers consume ROM data and write APU registers.
`GameBoyApu` synthesizes deterministic stereo PCM; output owns host playback.
The `audio.music`, `audio.sfx` and `audio.apu` subpackages keep these duties apart.
`audio.openal` provides native playback and `audio.browser` the Swing audition UI.

The session uses `FrameAudioPlayer` with host-created `PcmSoundOutput`.
`MusicDirector.tick()` advances it once per game frame. Sample remainders
preserve long-term sample totals. Dialog and gameplay effect routing share the
ROM-backed SFX driver/APU path with music. Headless output discards PCM.
The session closes its output resources.

The independent browser retains `OpenAlMusicPlayer` and sample-driven buffering.
Its transport is not the gameplay frame clock. Native audio is unnecessary for
pure driver/APU tests. The guide's early music-first scope summary predates
session gameplay integration described in its final section.

## ROM access

`RomSymbols` provides generated exact-label addresses. Regeneration uses a
locally built RGBDS symbol file; normal builds do not require the disassembly.
`Rom` is a read-only view of caller-owned bytes: callers preserve the backing
array, while `slice` returns a copy. Prefer typed `u8`, `u16le` and slice reads.
`RomAddress` centralizes bank-window/file-offset conversion and advancement.

`GbcBanks.adjust` mirrors the ROM's bank adjustment explicitly at loader call
sites. Do not apply a global adjustment to every symbol.
Retain bounds checks at format boundaries and exact-label verification.
[ROM_SYMBOLS.md](ROM_SYMBOLS.md) records migrations and address evidence.
Its historical declaration counts are checkpoints, not a current package audit.

`WorldRomConstantGuardTest` in `java/src/test/java/linksawakening/rom` guards
owned sources against raw ROM location patterns; read its covered surface
before extending it. `RomAddressTest`, `RomBankTest` and `RomTablesTest` cover
address/table behavior. Guard success does not prove semantic loader fidelity.

## Testing and verification

Run commands from `java/` with the checked-in Gradle wrapper and JDK 21.
`./gradlew romFreeTest` keeps development possible without original game data.
Unit tests use synthetic data for focused behavior and deterministic boundaries.
Compiled architecture guards check dependencies and selected ordering contracts.

ROM integration classes use `@RequiresRom` and `TestRom.open()`.
The annotation tags and skips integration tests when the ROM is absent;
TestRom enforces annotation use. Never add ROM fixtures to make CI pass.
`./gradlew test` includes integration tests when a local ROM is available.

Opening-route replay and comparison tools live under `tools/opening`;
intro, menu, house and dialog tools cover their corresponding slices.
Follow [BUILDING.md](BUILDING.md) for dependencies and CLI help.
Capture route hashes, checkpoints, comparison regions and remaining mismatches.
Use render parity alongside emulator comparison, not as a substitute for it.
Dated evidence is indexed in [README.md](README.md).

`.github/workflows/ci.yml` runs on pull requests targeting `main` only.
Its Ubuntu/macOS matrix uses Java 21 and runs
`./gradlew spotlessCheck classes romFreeTest distZip -PallPlatforms`.
Wrapper validation and ROM-free Python lint/unit checks accompany the Java jobs.
Failure artifacts include test reports and results. CI does not supply a ROM
or establish gameplay parity.

## Known limitations and future work

A future `RomOamAllocator` must reconstruct dynamic ROM allocation before
switching to actual CGB lower-OAM-index OBJ priority. Static Link/sword slots
cannot establish equivalent ordering.

The browser's sample-driven audio transport remains separate from gameplay.
Audio driver reconstruction is not CPU-level execution of all original routines,
and analog filtering/speaker coloration are not a claim of this architecture.

Read the Unverified notes in [ROM_FORMAT_NOTES.md](ROM_FORMAT_NOTES.md) and
[reconstruction-roadmap.md](reconstruction-roadmap.md) before extending scope.
Verified routes and compositor regions establish only their documented slice.
Historical plans are evidence of intent, not requirements already implemented.
