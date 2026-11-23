# Entity runtime seam — complete

All operation bridges and the static `RoomEntityRuntime.builder` factory are
removed. `RoomEntityRuntime` exposes exactly `tick(EntityFrameContext)` and its
twelve role accessors. `EntityRuntimeShapeTest.SEAM_BRIDGES` is empty; the guard
also requires the exact accessor names. Construction uses
`new RoomEntityRuntimeBuilder(snapshot)`, including entity-owned fixtures.

## Direct role boundary

Each accessor returns the behavior owner, with no forwarding role wrapper:

- `combat()` → `EntityCombatResolver`
- `pickups()` → `EntityRewardController`
- `spawns()` → `EntityPlayerSpawns`
- `enemySpawns()` → `EntityEnemySpawns`
- `lifecycle()` → `EntitySlotLifecycle`
- `outbox()` → `EntityEffectConsumption`
- `commands()` → `EntityPlayerCommands`
- `background()` → `EntityBackgroundMechanics`
- `inspection()` → `EntityRoomInspection`
- `configuration()` → `EntityRuntimeConfiguration`
- `debug()` → `EntityDebugConfiguration`
- `enemies()` → `EntityEnemyInspection`

The migration used attributed Java call sites, including all overloads and
builder calls in entity tests. Each role's caller migration and bridge removal
was committed with the full ROM-configured gate green. Enemy-spawn, inspection
and debug callers already used their role accessors. No compatibility member or
caller hand-off remains. No test was removed, disabled or weakened.

## Room integration decisions

`RoomRuntime` constructs `RoomEntityInteractions` once per loaded generation.
The interaction owner retains the active room and its concrete combat, pickup,
lifecycle, inspection and effect-consumption owners, plus the specific shared
room state it needs. It never receives `RoomEntityRuntime`, `RoomSession` or a
construction inventory. Room/follower replacement constructs new bindings.

The former session-scoped `RoomEntityCombatBridge` and its duplicate wiring
references are deleted. Combat resolution, persistent clearing, the Angler key
source-room flag, seashell dialog/sound/completion, key reward harvesting and
live entity-view refresh keep their original sequence. Deferred contact recoil
goes straight to the combat role. Pickup's masked follower registers are written
by `RoomFollowerState.observePickup`. A regression clears the replacement
runtime while proving the previous generation remains unchanged.

Three existing boundaries deliberately remain:

- `RoomEntityBridge` owns the room role's unloaded defaults, required-input
  evaluation, session output queues, ordered drains and room/entity integration.
  It resolves the current generation once per operation. These policies cannot
  be replaced by a cached entity role surviving room replacement.
- `RoomEntitySimulation` configures the entity frame and applies its room effects
  in source order. Torch palettes, ocarina progression, world RNG, rewards,
  terrain redraw, puzzle motion and persistence are room responsibilities. It
  calls entity roles directly; there is no extra runtime operation delegate.
- `RoomEntityInputs` owns remembered inputs before a load and the distinction
  between cached writes and immediate loaded configuration. Removing this
  boundary would change validation, no-room behavior or application timing.

## Frame execution review

The independent scratch concepts are now top-level state owners:
`EntityFrameInputLatch` (latched invocation inputs), `EntitySlotExecution`
(current descending-slot scratch), `EntityTimerGates` (interaction/countdown
decisions) and `EntityPresentationFlags` (handler-owned presentation retention).
They contain state and reset behavior, not slices forwarding through execution.
They are borrowed for the current synchronous invocation/slot and must not be
retained as snapshots. Execution still owns acquisition, reset and release;
reentrant execution allocates its own groups, while ordinary frames reuse them.
A regression verifies reentrant isolation, stable identities and reset on reuse.

Thirty-five callback implementations now take only their actual groups and
behavior owners. Registration spells out those dependencies, for example:
`frame -> phase1HeartContainer(frame.slotFrame(), frame.inputs(), frame.slot(),
frame.bossRewards())`. The noncapturing adapter is retained at registration;
it creates no per-frame wrapper. Stage/visit order and stop semantics are unchanged.

The full execution protocol remains for handlers coordinating slot replacement,
event emission, dynamic typed motion and several ordered effects. Their cached
multi-step scratch also belongs to that invocation. Creating family-specific
service bundles or forwarding “Events/Spawner” slices would hide these edges.
Initialization/advance-only families already receive the entity, immutable
frame/RNG and typed motion registry instead of execution. The family inventory
below records the actual pre-migration execution capabilities; simple callbacks
now expose the applicable subset as parameters. `slotFrame`, `inputs`, `gates`
and `presentation` refer to the four independently named groups above.

- **AntiFairyHandler**: initializer/advance inputs and the typed motion registry; no execution context.
- **ArmosKnightHandler**: `background`, `emit`, `inputs`, `lifecycle`, `motion`, `playerSpawns`, `presentation`, `requestMusic`, `roomInspection`, `slot`, `slotFrame`, `slotProperties`.
- **ArmosStatueHandler**: `background`, `gates`, `inputs`, `motion`, `slot`, `slotFrame`.
- **ArrowHandler**: `background`, `emit`, `gates`, `inputs`, `lifecycle`, `motion`, `playerSpawns`, `projectiles`, `replaceEntity`, `slotFrame`.
- **BlooperHandler**: `background`, `gates`, `inputs`, `motion`, `presentation`, `slot`, `slotFrame`.
- **BombHandler**: `bombInteractions`, `entityAt`, `inputs`, `liftThrow`, `replaceEntity`, `slotFrame`.
- **BomberHandler**: `emit`, `enemySpawns`, `inputs`, `motion`, `slot`, `slotFrame`.
- **BombiteTypeHandler**: `bombInteractions`, `emit`, `enemySpawns`, `inputs`, `lifecycle`, `link`, `motion`, `presentation`, `projectiles`, `slot`, `slotFrame`.
- **BooBuddyHandler**: `gates`, `inputs`, `lifecycle`, `motion`, `replaceEntity`, `slot`, `slotFrame`, `story`.
- **BoomerangHandler**: `gates`, `inputs`, `projectiles`, `slotFrame`.
- **BowWowHandler**: `emit`, `gates`, `inputs`, `motion`, `replaceEntity`, `requestMusic`, `slot`, `slotFrame`, `spriteViews`, `sprites`, `story`.
- **BushCrawlerHandler**: `background`, `gates`, `inputs`, `liftThrow`, `link`, `motion`, `presentation`, `slot`, `slotFrame`, `slotProperties`.
- **ButterflyHandler**: initializer/advance inputs and the typed motion registry; no execution context.
- **BuzzBlobHandler**: `gates`, `inputs`, `motion`, `presentation`, `slot`, `slotFrame`.
- **ChestWithItemHandler**: `entityAt`, `pickups`, `slotFrame`.
- **ColorShellTypeHandler**: `entities`, `inputs`, `lifecycle`, `motion`, `replaceEntity`, `roomInspection`, `slot`, `slotFrame`, `slotProperties`.
- **CommonDroppableTypeHandler**: `background`, `inputs`, `lifecycle`, `pickups`, `presentation`, `replaceEntity`, `slotFrame`.
- **CrowHandler**: `inputs`, `lifecycle`, `motion`, `projectiles`, `roomInspection`, `slot`, `slotFrame`, `slotProperties`.
- **CrystalSwitchHandler**: `background`, `emit`, `slot`, `slotFrame`.
- **CuccoHandler**: `emit`, `enemySpawns`, `entityAt`, `gates`, `inputs`, `lifecycle`, `liftThrow`, `link`, `motion`, `presentation`, `projectiles`, `replaceEntity`, `roomInspection`, `slot`, `slotFrame`, `story`.
- **DogHandler**: `emit`, `gates`, `inputs`, `link`, `motion`, `presentation`, `slot`, `slotFrame`.
- **DroppableFairyHandler**: `inputs`, `motion`, `slot`, `slotFrame`.
- **DroppableSecretSeashellHandler**: `background`, `inputs`, `lifecycle`, `motion`, `presentation`, `rewards`, `roomInspection`, `slot`, `slotFrame`.
- **EnemyProjectileTypeHandler**: `background`, `emit`, `gates`, `inputs`, `lifecycle`, `motion`, `projectiles`, `slotFrame`.
- **FishHandler**: `emit`, `inputs`, `motion`, `slot`, `slotFrame`.
- **FollowingNpcTypeHandler**: `motion`, `slotFrame`.
- **GenieHandler**: `background`, `emit`, `entityAt`, `gates`, `genie`, `inputs`, `lifecycle`, `link`, `motion`, `requestMusic`, `roomInspection`, `scratch`, `slotFrame`, `slotProperties`.
- **GhiniTypeHandler**: `inputs`, `motion`, `presentation`, `slotFrame`.
- **GiantGopongaFlowerHandler**: `background`, `enemySpawns`, `gates`, `inputs`, `motion`, `presentation`, `slot`, `slotFrame`.
- **GibdoHandler**: initializer/advance inputs and the typed motion registry; no execution context.
- **GoombaHandler**: `background`, `inputs`, `lifecycle`, `motion`, `slot`, `slotFrame`.
- **GopongaFlowerHandler**: `background`, `gates`, `inputs`, `presentation`, `slotFrame`.
- **GopongaFlowerProjectileHandler**: `gates`, `inputs`, `lifecycle`, `motion`, `presentation`, `slot`, `slotFrame`.
- **HardhatBeetleHandler**: `inputs`, `motion`, `slotFrame`.
- **HeartContainerHandler**: `bossRewards`, `inputs`, `slot`, `slotFrame`.
- **HidingSlimeKeyHandler**: `emit`, `inputs`, `lifecycle`, `pickups`, `roomInspection`, `slot`, `slotFrame`.
- **HidingZolHandler**: `emit`, `inputs`, `motion`, `slot`, `slotFrame`.
- **HinoxHandler**: `background`, `emit`, `enemySpawns`, `gates`, `inputs`, `lifecycle`, `motion`, `presentation`, `requestMusic`, `roomInspection`, `scratch`, `slot`, `slotFrame`, `slotProperties`.
- **HookshotBridgeHandler**: `background`, `slotFrame`.
- **HookshotChainHandler**: `background`, `emit`, `gates`, `inputs`, `lifecycle`, `link`, `motion`, `playerSpawns`, `projectiles`, `replaceEntity`, `roomInspection`, `scratch`, `slot`, `slotFrame`.
- **InstrumentOfTheSirensHandler**: `inputs`, `rewards`, `slotFrame`.
- **IronMaskHandler**: `inputs`, `presentation`, `slot`, `slotFrame`, `spriteViews`, `sprites`.
- **KanaletBombableWallHandler**: `enemySpawns`, `lifecycle`, `roomInspection`, `slot`, `slotFrame`.
- **KeeseHandler**: initializer/advance inputs and the typed motion registry; no execution context.
- **KeyDropPointHandler**: `background`, `emit`, `inputs`, `pickups`, `replaceEntity`, `roomInspection`, `slot`, `slotFrame`, `slotProperties`.
- **KidHandler**: `inputs`, `slotFrame`, `story`.
- **LeeverHandler**: `slotFrame`.
- **LiftableRockHandler**: `inputs`, `liftThrow`, `slotFrame`.
- **LikeLikeHandler**: `emit`, `inputs`, `link`, `motion`, `slot`, `slotFrame`.
- **MadBomberHandler**: `emit`, `enemySpawns`, `inputs`, `lifecycle`, `motion`, `presentation`, `slot`, `slotFrame`.
- **MadamMeowmeowHandler**: `inputs`, `slotFrame`, `story`.
- **MagicPowderSprinkleHandler**: `gates`, `inputs`, `projectiles`, `slotFrame`.
- **MagicRodFireballHandler**: `gates`, `inputs`, `projectiles`, `slotFrame`.
- **MarinIndoorHandler**: `lifecycle`, `roomInspection`, `slotFrame`.
- **MaskedMimicGoriyaHandler**: `inputs`, `link`, `motion`, `presentation`, `roomInspection`, `slot`, `slotFrame`.
- **MimicHandler**: `inputs`, `link`, `motion`, `presentation`, `slot`, `slotFrame`.
- **MiniMoldormHandler**: `background`, `inputs`, `motion`, `presentation`, `slot`, `slotFrame`, `sprites`.
- **MoblinHandler**: `motion`, `slotFrame`.
- **MoblinKingHandler**: `background`, `bossRewards`, `emit`, `enemySpawns`, `gates`, `inputs`, `lifecycle`, `motion`, `presentation`, `replaceEntity`, `scratch`, `slot`, `slotFrame`, `spriteViews`, `sprites`, `story`.
- **MoblinSwordHandler**: `emit`, `inputs`, `lifecycle`, `link`, `motion`, `projectiles`, `roomInspection`, `slot`, `slotFrame`.
- **MoldormHandler**: `bossRewards`, `emit`, `gates`, `inputs`, `lifecycle`, `link`, `motion`, `presentation`, `replaceEntity`, `requestMusic`, `roomInspection`, `scratch`, `slot`, `slotFrame`, `slotProperties`, `sprites`.
- **MusicalNoteHandler**: `replaceEntity`, `slotFrame`, `slotProperties`.
- **OwlEventHandler**: `gates`, `inputs`, `slotFrame`, `story`.
- **PairoddHandler**: `emit`, `enemySpawns`, `inputs`, `motion`, `slot`, `slotFrame`.
- **PairoddProjectileHandler**: `emit`, `inputs`, `lifecycle`, `motion`, `slotFrame`.
- **PeahatHandler**: `inputs`, `motion`, `slotFrame`.
- **PincerHandler**: `background`, `gates`, `inputs`, `motion`, `presentation`, `slot`, `slotFrame`.
- **PiranhaPlantHandler**: `gates`, `inputs`, `lifecycle`, `motion`, `presentation`, `slot`, `slotFrame`.
- **PokeyHandler**: `emit`, `enemySpawns`, `gates`, `inputs`, `lifecycle`, `motion`, `presentation`, `slot`, `slotFrame`, `spriteViews`, `sprites`.
- **PolsVoiceHandler**: `emit`, `enemyInspection`, `inputs`, `lifecycle`, `motion`, `presentation`, `slot`, `slotFrame`.
- **RichardHandler**: `inputs`, `slotFrame`, `story`.
- **RoamingEnemyTypeHandler**: `emit`, `enemySpawns`, `inputs`, `motion`, `slot`, `slotFrame`.
- **RollingBonesBarHandler**: `background`, `emit`, `gates`, `inputs`, `lifecycle`, `link`, `motion`, `presentation`, `replaceEntity`, `requestMusic`, `roomInspection`, `slot`, `slotFrame`, `slotProperties`, `sprites`.
- **RollingBonesHandler**: `bossRewards`, `emit`, `inputs`, `motion`, `presentation`, `roomInspection`, `slot`, `slotFrame`.
- **RoosterHandler**: `emit`, `inputs`, `liftThrow`, `link`, `presentation`, `projectiles`, `slotFrame`.
- **SandCrabHandler**: `gates`, `inputs`, `motion`, `presentation`, `slot`, `slotFrame`.
- **SideViewPlatformHandler**: `background`, `emit`, `gates`, `inputs`, `lifecycle`, `link`, `motion`, `roomInspection`, `slotFrame`.
- **SideViewPotHandler**: `background`, `emit`, `gates`, `inputs`, `lifecycle`, `liftThrow`, `link`, `motion`, `playerSpawns`, `replaceEntity`, `slotFrame`.
- **SleepyToadstoolHandler**: `inputs`, `lifecycle`, `rewards`, `slotFrame`, `story`.
- **SlimeEyeHandler**: `slimeEye`.
- **SnakeHandler**: `inputs`, `motion`, `presentation`, `slot`, `slotFrame`.
- **SparkTypeHandler**: `inputs`, `motion`, `slotFrame`.
- **SpikeTrapHandler**: `emit`, `inputs`, `motion`, `slotFrame`.
- **SpikedBeetleHandler**: `inputs`, `motion`, `presentation`, `slot`, `slotFrame`.
- **StalfosAggressiveHandler**: `inputs`, `motion`, `slotFrame`.
- **StalfosEvasiveHandler**: `emit`, `enemySpawns`, `inputs`, `lifecycle`, `link`, `motion`, `roomInspection`, `slot`, `slotFrame`, `spriteViews`, `sprites`.
- **StarHandler**: `gates`, `inputs`, `motion`, `presentation`, `slot`, `slotFrame`.
- **SwordBeamHandler**: `gates`, `inputs`, `projectiles`, `slotFrame`.
- **SwordShieldPickupHandler**: `inputs`, `replaceEntity`, `rewards`, `roomInspection`, `slotFrame`, `spriteViews`, `sprites`.
- **TarinHandler**: `background`, `emit`, `enemySpawns`, `inputs`, `lifecycle`, `link`, `motion`, `presentation`, `requestMusic`, `roomInspection`, `scratch`, `slot`, `slotFrame`, `story`.
- **TektiteHandler**: initializer/advance inputs and the typed motion registry; no execution context.
- **ThreeOfAKindHandler**: `gates`, `inputs`, `motion`, `presentation`, `slot`, `slotFrame`.
- **Type0X35Handler**: `inputs`, `lifecycle`, `pickups`, `roomInspection`, `slotFrame`.
- **Type0X39Handler**: `inputs`, `rewards`, `slotFrame`.
- **Type0X42Handler**: `inputs`, `roomInspection`, `slotFrame`, `story`.
- **UrchinHandler**: `background`, `emit`, `gates`, `inputs`, `motion`, `presentation`, `slot`, `slotFrame`, `spriteViews`, `sprites`.
- **WarpHandler**: `emit`, `inputs`, `lifecycle`, `replaceEntity`, `requestMusic`, `roomInspection`, `slotFrame`, `warpMotion`.
- **WaterTektiteHandler**: `inputs`, `motion`, `slotFrame`.
- **WingedOctorokHandler**: `emit`, `enemySpawns`, `gates`, `inputs`, `link`, `motion`, `presentation`, `projectiles`, `slot`, `slotFrame`.
- **WitchHandler**: `emit`, `inputs`, `lifecycle`, `link`, `motion`, `presentation`, `requestMusic`, `roomInspection`, `slot`, `slotFrame`, `story`.
- **WitchRatHandler**: `gates`, `inputs`, `motion`, `presentation`, `slot`, `slotFrame`.
- **WizrobeHandler**: `enemySpawns`, `inputs`, `motion`, `presentation`, `slot`, `slotFrame`.
- **WizrobeProjectileHandler**: `inputs`, `lifecycle`, `motion`, `presentation`, `slotFrame`.
- **ZolGelTypeHandler**: `gates`, `inputs`, `lifecycle`, `link`, `motion`, `slot`, `slotFrame`.
- **ZombieHandler**: `background`, `enemySpawns`, `gates`, `inputs`, `lifecycle`, `motion`, `presentation`, `slot`, `slotFrame`.
- **ZoraHandler**: `background`, `emit`, `enemySpawns`, `gates`, `inputs`, `motion`, `presentation`, `slot`, `slotFrame`.

## Wide-constructor review

Reviewed every main-source constructor in `world/entity` with more than ten
parameters, including implicit record canonical constructors and nested motion
values. A wide protocol is retained when each collaborator participates in the
same ordered operation. A data value is retained when its fields describe one
step, state or emitted outcome. No remaining width exposed a second independent
state responsibility to extract. In particular, no service bundle replaces
explicit dependencies merely to shorten a signature.

- **EntityBombInteractions (11)**: keep — one bomb countdown/explosion interaction protocol; keep damage, lift and emitted-effect targets explicit.
- **EntityBossRewards (12)**: keep — one boss-death reward/completion protocol; keep slot, background, progression and presentation participants explicit.
- **EntityCombatResolver (13)**: keep — one ordered contact/weapon damage-resolution protocol; keep collision, immunity, lifecycle and boss hooks explicit.
- **EntityFrameExecution (31)**: keep — one invocation lifecycle and dispatch context; keep actual behavior owners, expose independent scratch groups and narrow simple callbacks.
- **EntityLiftThrowController (16)**: keep — one lift/throw lifecycle, including bomb and carried-entity transitions; keep its concrete physics and command targets.
- **EntityPickupController (13)**: keep — one pickup collision/collection protocol coordinating slot removal and held-item rewards; keep concrete targets.
- **EntityPlayerSpawns (18)**: keep — one player/world-origin spawn protocol allocating and initializing shared slots; keep concrete initialization collaborators.
- **EntityProjectileController (13)**: keep — one projectile contact/update protocol coordinating shared slots, bombs and player spawns; keep distinct targets.
- **EntityRewardController (13)**: keep — one collected-item presentation/completion protocol across pickup, story and Link state; keep ordered participants.
- **EntityRoomInitialization (13)**: keep — one ordered room-entry initialization protocol; keep family participants and reset order explicit.
- **EntityRoomInspection (19)**: keep — one live/owned read model over slot and family state; keep explicit read dependencies.
- **EntityRuntimeConfiguration (12)**: keep — one configuration protocol updating the concrete state owners; keep its distinct configuration targets.
- **EntitySlotLifecycle (21)**: keep — one slot reset/unload protocol across independent family owners; keep reset ordering and explicit targets.
- **EntitySlotProperties (14)**: keep — one ROM slot-property/initialization protocol; keep the state and family hooks that apply those properties.
- **EntityStoryController (14)**: keep — one NPC/story interaction protocol coordinating motion, inventory and outputs; family state remains separately owned.
- **RoomEntityRuntime (17)**: keep — one execution entry plus stable public role references and fixture inspectors; grouping them would recreate a service inventory.
- **SlimeEyeController (12)**: keep — one Slime Eye encounter with combat and rendering phases sharing encounter state; keep its concrete encounter collaborators.

Frame, state and event constructors (each verdict is one data responsibility):

- **EnemyCombatFrame (12)**: keep — one ROM handler/contact-step input; fields describe the same synchronous decision, not independently owned services.
- **EntityAttackContactFrame (11)**: keep — one ROM handler/contact-step input; fields describe the same synchronous decision, not independently owned services.
- **EntityFrameContext (22)**: keep — one immutable slot value or invocation input contract; named builders already supply construction.
- **EntityPickupFrame (12)**: keep — one ROM handler/contact-step input; fields describe the same synchronous decision, not independently owned services.
- **EntityVerticalContactFrame (11)**: keep — one ROM handler/contact-step input; fields describe the same synchronous decision, not independently owned services.
- **RoomEntity (13)**: keep — one immutable slot value or invocation input contract; named builders already supply construction.
- **SlimeEyeController.SlimeEyeCombatFrame (13)**: keep — one encounter combat-step input and its result channel; preserve synchronous contact ordering.
- **EntityCombatEvent (13)**: keep — one emitted interaction outcome, including its associated sound/Link effects; existing builders name optional effects.
- **EntityProjectileEvent (14)**: keep — one emitted interaction outcome, including its associated sound/Link effects; existing builders name optional effects.
- **HinoxLinkEffectRequest (11)**: keep — one held/thrown-Link register update, including damage and pose flags; the fields describe one ordered write outcome.
- **BombiteMotion.BombiteFrame (11)**: keep — one ROM handler/contact-step input; fields describe the same synchronous decision, not independently owned services.
- **BushCrawlerMotion.BushCrawlerFrame (12)**: keep — one ROM handler/contact-step input; fields describe the same synchronous decision, not independently owned services.
- **CuccoMotion.CuccoFrame (15)**: keep — one ROM handler/contact-step input; fields describe the same synchronous decision, not independently owned services.
- **CuccoMotion.CuccoInteractionFrame (16)**: keep — one ROM handler/contact-step input; fields describe the same synchronous decision, not independently owned services.
- **DogMotion.DogFrame (11)**: keep — one ROM handler/contact-step input; fields describe the same synchronous decision, not independently owned services.
- **DogMotion.Update (12)**: keep — one ROM handler result or captured motion state; its values are kept together for ownership and ordered application.
- **HinoxMotion.HinoxFrame (11)**: keep — one ROM handler/contact-step input; fields describe the same synchronous decision, not independently owned services.
- **HinoxMotion.HinoxResult (20)**: keep — one ROM handler result or captured motion state; its values are kept together for ownership and ordered application.
- **HinoxMotion.Update (19)**: keep — one ROM handler result or captured motion state; its values are kept together for ownership and ordered application.
- **HookshotChainMotion.HookshotChainSnapshot (12)**: keep — one ROM handler result or captured motion state; its values are kept together for ownership and ordered application.
- **HookshotChainMotion.State (11)**: keep — one ROM handler result or captured motion state; its values are kept together for ownership and ordered application.
- **LiftedEntityMotion.LiftedEntityFrame (12)**: keep — one ROM handler/contact-step input; fields describe the same synchronous decision, not independently owned services.
- **LikeLikeMotion.LikeLikeFrame (13)**: keep — one ROM handler/contact-step input; fields describe the same synchronous decision, not independently owned services.
- **SecretSeashellMotion.SecretSeashellInteractionFrame (12)**: keep — one ROM handler/contact-step input; fields describe the same synchronous decision, not independently owned services.
- **SecretSeashellMotion.SecretSeashellWorldFrame (11)**: keep — one ROM handler/contact-step input; fields describe the same synchronous decision, not independently owned services.
- **TarinRaccoonMotion.Input (14)**: keep — one ROM handler/contact-step input; fields describe the same synchronous decision, not independently owned services.
- **TarinRaccoonMotion.Update (21)**: keep — one ROM handler result or captured motion state; its values are kept together for ownership and ordered application.
- **WingedOctorokMotion.WingedOctorokFrame (11)**: keep — one ROM handler/contact-step input; fields describe the same synchronous decision, not independently owned services.
- **WingedOctorokMotion.WingedOctorokInteractionFrame (12)**: keep — one ROM handler/contact-step input; fields describe the same synchronous decision, not independently owned services.
- **WitchMotion.Input (15)**: keep — one ROM handler/contact-step input; fields describe the same synchronous decision, not independently owned services.
- **WitchMotion.Update (11)**: keep — one ROM handler result or captured motion state; its values are kept together for ownership and ordered application.

## Validation

Required ROM-configured `./gradlew spotlessApply check`: 3,000 tests, zero
failures/errors, one existing skip (2,998 tests after merging develop). Lint is
fatal. No replay fixture or allocation budget changed.

With escape analysis disabled by a temporary local init script, all allocation
guards pass: whole-frame idle 0, inventory 0 and walking 139.53 B/frame; active
simulation 137 and idle simulation 0 B/frame; representative entity mix
834.59 B/tick; two-enemy combat 400 B/frame; live room/render views and empty
outbox drains/merges 0 B/frame. Input construction, collision and output
ownership contracts are unchanged. New room-interaction objects are built only
on load/replacement, never per frame. No native window was launched or push made.
