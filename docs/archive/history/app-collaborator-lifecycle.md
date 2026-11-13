# App collaborator lifecycle audit

A3 replaces nullable screen controllers with FileMenuState/FileSaveState operations, and nullable death controllers with explicit sequence states. DebugTools owns a final menu and overlay created in wiring. GameSession remains the one recording lifecycle owner; its presence predicate is checked in one method and encoding/host behavior is unchanged.

Remaining domains: immutable value-cache occupancy, optional requests/NPCs/equipped sword values, scene lookups, opening sequences and resource/error results. Remaining validation: input/public lifecycle guards, resource lookup failures and initialization checks. None of the new menu/debug/death collaborators is nullable.

The debug assertions now check the observable closed state instead of lazy construction; each test and its disabled/blocked guarantee is retained.

| Class | Baseline comparisons | After | Classification of retained sites |
|---|---:|---:|---|
| app/GameSession.java | 15 | 7 | single recording/session lifetime owner; lifecycle validation |
| app/LaunchOptions.java | 2 | 2 | domain values / boundary validation |
| app/audio/MusicDirector.java | 5 | 0 | domain values / boundary validation |
| app/debug/DebugTools.java | 3 | 0 | domain values / boundary validation |
| app/flow/SaveCoordinator.java | 2 | 2 | domain values / boundary validation |
| app/flow/ScreenRouter.java | 3 | 3 | domain values / boundary validation |
| app/flow/SessionNavigator.java | 1 | 1 | domain values / boundary validation |
| app/gameplay/ActiveLinkPhase.java | 4 | 4 | domain values / boundary validation |
| app/gameplay/CombatPhase.java | 2 | 2 | domain values / boundary validation |
| app/gameplay/FrameInputCache.java | 4 | 4 | immutable cached domain values |
| app/gameplay/GameplaySimulation.java | 1 | 1 | domain values / boundary validation |
| app/gameplay/LinkEquipmentPhase.java | 1 | 1 | domain values / boundary validation |
| app/gameplay/entity/EntityInputCache.java | 11 | 11 | immutable cached domain values |
| app/gameplay/entity/LinkWriteApplier.java | 1 | 1 | domain values / boundary validation |
| app/gameplay/interaction/BushInteraction.java | 1 | 1 | domain values / boundary validation |
| app/gameplay/interaction/DialogInteractions.java | 1 | 1 | domain values / boundary validation |
| app/gameplay/recovery/GameOverFlow.java | 1 | 1 | domain values / boundary validation |
| app/gameplay/recovery/PassOutController.java | 4 | 0 | none: explicit sequence states |
| app/headless/ReplayCheckpoint.java | 3 | 3 | domain values / boundary validation |
| app/headless/SessionDiagnosticSnapshot.java | 1 | 1 | domain values / boundary validation |
| app/host/DesktopHost.java | 3 | 3 | domain values / boundary validation |
| app/host/HostResources.java | 2 | 2 | domain values / boundary validation |
| app/input/InputRouter.java | 3 | 3 | domain values / boundary validation |
| app/input/SessionInput.java | 1 | 0 | domain values / boundary validation |
| app/presentation/RenderStateAssembler.java | 1 | 0 | domain values / boundary validation |
| app/screen/FileMenuScreen.java | 2 | 0 | domain frame/buffer values; no optional controllers |
| app/screen/FileSaveScreen.java | 2 | 0 | domain frame/buffer values; no optional controllers |
| app/screen/GameplayScreen.java | 2 | 2 | domain frame/buffer values; no optional controllers |
| app/story/OpeningStory.java | 7 | 7 | domain values / boundary validation |
| app/wiring/RomWiring.java | 1 | 1 | domain values / boundary validation |
| app/wiring/SessionWiring.java | 1 | 1 | domain values / boundary validation |

Baseline app aggregate 91 -> 65. Gameplay disables optional VFX through a zero-slot no-op system; nullable request/data validation remains at public boundaries.
