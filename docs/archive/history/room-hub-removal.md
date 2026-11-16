# Room facade dependency report

Role ownership and lifecycle contracts are described in [ARCHITECTURE.md](../../ARCHITECTURE.md#rooms-and-world).
There is no facade operation hub or replacement core. The construction inventory
is referenced only by its own declarations and RoomSessionContext. Within main
world.room, only RoomSession itself references the RoomSession type; lifecycle
publication/notification use typed callbacks, not behavioral forwarding.

Declared public facade methods:

- doors(), transitions(), playerItems(), story(), entities(), entityState()
- rooms(), dungeon(), progress(), terrain(), entityFrameResults(), hasRoom()
- tickEntities(EntityFrameValues), tickEntities(EntityTickValues)

Package lifecycle methods: installRoom(RoomRuntime), clearRoom(), roomLoaded(RoomRuntime).
The constructor is RoomSession(RoomSessionConfig). No production inspection getter remains.

No behavior had to remain on the facade beyond those lifecycle responsibilities.
No main or test files under world/entity were edited. Test-only owner inspection
replaces the old components field without introducing a production wiring bag.

All class-file RoomSession references in main code (including nested generated
adapter names) are listed below. External app/gameplay clients consume the public
role/frame boundary; they are not room implementation dependencies.

- linksawakening.app.HeadlessGameDriver$Diagnostics
- linksawakening.app.audio.MusicDirector
- linksawakening.app.audio.RoomMusicSelection
- linksawakening.app.debug.DebugMenu
- linksawakening.app.debug.DebugMenuPages
- linksawakening.app.debug.DebugTeleporter
- linksawakening.app.debug.DebugTools
- linksawakening.app.flow.SaveCoordinator
- linksawakening.app.flow.ScreenRouter
- linksawakening.app.gameplay.ActiveLinkPhase
- linksawakening.app.gameplay.AnimatedTilesPhase
- linksawakening.app.gameplay.CombatPhase
- linksawakening.app.gameplay.DialogStoryPhase
- linksawakening.app.gameplay.FrameInputCache
- linksawakening.app.gameplay.GameplaySimulation
- linksawakening.app.gameplay.LinkEquipmentPhase
- linksawakening.app.gameplay.LinkGroundPhase
- linksawakening.app.gameplay.entity.EntityPhase
- linksawakening.app.gameplay.entity.EntityPhase$1
- linksawakening.app.gameplay.entity.EntityRewardApplier
- linksawakening.app.gameplay.entity.GameplayCombatEvents
- linksawakening.app.gameplay.entity.LinkWriteApplier
- linksawakening.app.gameplay.interaction.BushInteraction
- linksawakening.app.gameplay.interaction.DialogInteractions
- linksawakening.app.gameplay.interaction.LiftInteraction
- linksawakening.app.gameplay.recovery.GameOverPresentation
- linksawakening.app.gameplay.recovery.PassOutController
- linksawakening.app.headless.ReplayCheckpoint
- linksawakening.app.headless.SessionDiagnosticSnapshot
- linksawakening.app.headless.SessionDiagnostics
- linksawakening.app.input.InputRouter
- linksawakening.app.input.SessionInput
- linksawakening.app.presentation.RenderStateAssembler
- linksawakening.app.services.WorldServices
- linksawakening.app.story.OpeningStory
- linksawakening.app.wiring.EquipmentWiring
- linksawakening.app.wiring.EquipmentWiring$1
- linksawakening.app.wiring.EquipmentWiring$2
- linksawakening.app.wiring.EquipmentWiring$3
- linksawakening.app.wiring.EquipmentWiring$4
- linksawakening.app.wiring.EquipmentWiring$5
- linksawakening.app.wiring.EquipmentWiring$6
- linksawakening.app.wiring.EquipmentWiring$7
- linksawakening.app.wiring.EquipmentWiring$8
- linksawakening.app.wiring.EquipmentWiring$EquipmentInputs
- linksawakening.app.wiring.GameplayWiring
- linksawakening.app.wiring.PlayerWiring
- linksawakening.app.wiring.PlayerWiring$PlayerWorld
- linksawakening.app.wiring.SessionWiring
- linksawakening.app.wiring.SubsystemWiring
- linksawakening.app.wiring.WorldWiring
- linksawakening.gameplay.BeachSwordRewardConsumer
- linksawakening.world.room.RoomSession

Validation: the ROM-enabled spotlessApply check gate passes 2,992 tests, with
zero failures/errors and one skipped. All EA-off allocation guards pass; whole
walking remains about 140 B/frame, idle/inventory zero, active simulation 137.
Allocation budgets and canonical replay fixtures were unchanged.
