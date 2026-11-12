# Room/entity seam consolidation — round 4

Branch: `t3/refactor-room-entity-facade`. Integrated `develop` at `d292938f` before
implementation. All eight steps are complete, with one commit per step. No
external package source/test, build configuration, ROM/asset or replay fixture
was changed. The two architecture-test utility files are the explicitly
permitted boundary-test changes.

## Step status and metrics

| Step | Result | Before → after |
| --- | --- | --- |
| 1. Entity tests → room roles | Removed all final room bridges, positional session/combat/pickup adapters and private reflection alias | RoomSession: 333 → 96 lines; 39 → 13 public methods; 26 → 0 deprecated methods; 2 → 0 deprecated constructor/field members |
| 2. Room → entity boundary | Migrated 30 calls; removed 19 obsolete entity adapters | Entity production `ForTest` shims: 3 → 0; entity raw-ROM delegates: 5 → 0; four event constructors retained only for external tests |
| 3. Typed room ROM | Internal fields, dependency records, reads and operations use `Rom` | Raw ROM parameter/field declarations in world main: 48 → 6; all six are listed deprecated external entry points, zero raw ROM fields |
| 4. Enum adoption | Direction and LinkMotionState in closed domains; typed inventory/power/tunic/song entry points; original unknown ROM/saved values preserved | Deprecated Link/PlayerState constant references in room/transition/object main: 21 → 0; remaining world main removal warnings: 6, all deliberate legacy ROM adaptation |
| 5. Allocation | Immediate reads use live views; public history remains owned; immutable inputs retained; empty drains/merges allocate nothing | Idle: 3,979 → 3,312 B/frame; active: 11,080 → 7,457; entity mix: 874.43 → 874.43 B/tick; empty drains/merge: 0 B/iteration |
| 6. Public boundary | 28 implementation types made package-private; public-use and all-nonworld-client checks added | Public top-level world types: 341 → 313; app allow-list: 58 → 56; seven explicit stable sprite/OAM/physics contracts |
| 7. Readability | Audited twelve files by size/branch complexity; removed test setup from runtime views; consolidated publication/drains; named ROM selectors | RoomSessionViews: 399 → 216 lines; 30 → 0 test-only helpers; 340 test call sites migrated; files over 400 lines: 0 → 0; longest room/object/transition method: 78 → 78 lines |
| 8. Documentation | Architecture describes the cleaned seam, ROM/enum ownership and stricter tests; completed migration inventories replaced with done notes | Room facade and entity migration guides no longer prescribe removed compatibility APIs |

Public method counts exclude constructors and inherited `Object` methods. The
48 raw declarations are the step-3 baseline, after step 2 removed the entity
byte-array adapters. RoomSessionContext field reads outside wiring remain zero.
`RoomSession` has no superclass and no deprecated member of any kind. No
RoomSession bridge remains for an off-limits caller. The six raw boundaries are
the only raw ROM parameter declarations in world main; RAM byte arrays and the
three explicit catalog handoffs are different domains. All world main files are
under 400 lines (largest 398). In room/object/transition, the only explicit
signatures above eight parameters are immutable `LinkGroundFrame` value schema
constructors, including its required external int-input adapter. There are no
wide operation overloads left.

The ordinary ROM-configured `./gradlew spotlessApply test` gate passed before
every commit: **2,825 tests, zero failures/errors, one existing skip**, up from
2,812 at integration. No old test was deleted or weakened. Thirteen tests were
added: six enum/unknown-byte tests, three live/owned/empty-drain tests, two
immutable builder/presentation tests, and two compiled boundary guards.

## Remaining deprecated boundaries

There are exactly **22** deprecated world members, all `forRemoval = true` and
all needed by the external callers below: six ROM entry points, twelve enum
role/value adapters, and four event constructors. No owned caller still uses
one, except the projectile 9-argument compatibility constructor delegates to
the retained 11-argument constructor. Paths are relative to `java/src/`;
signatures use unqualified Java type names.

| Member | Replacement | Off-limits/stable callers |
| --- | --- | --- |
| `entity.event.EntityCombatEvent(int, int, int, boolean, int, int, SoundChannel, int, SoundChannel, int)` | `EntityCombatEvent.builder(...)` | `test/java/linksawakening/gameplay/EnemyCombatEventConsumerTest.java` |
| `entity.event.EntityCombatEvent(int, int, int, boolean, int, int, SoundChannel, int, SoundChannel, int, SwordPokeVfx)` | `EntityCombatEvent.builder(...)` | `test/java/linksawakening/gameplay/EnemyCombatEventConsumerTest.java` |
| `entity.event.EntityProjectileEvent(int, int, Kind, int, int, SoundChannel, int, boolean, boolean)` | `EntityProjectileEvent.builder(...)` | `test/java/linksawakening/audio/music/GameplayMusicControllerTest.java`; `test/java/linksawakening/gameplay/EnemyProjectileEventConsumerTest.java` |
| `entity.event.EntityProjectileEvent(int, int, Kind, int, int, SoundChannel, int, boolean, boolean, int, int)` | `EntityProjectileEvent.builder(...)` | `test/java/linksawakening/gameplay/EnemyProjectileEventConsumerTest.java` |
| `object.OverworldBushInteraction(byte[], RomTables)` | `OverworldBushInteraction(Rom, RomTables)` | `main/java/linksawakening/app/SessionBootstrap.java` |
| `room.EntityPickupInputs.Link(int, int, boolean, boolean, int, int)` | `EntityPickupInputs.Link(..., Direction, ...)` | `main/java/linksawakening/app/gameplay/LinkEquipmentPhase.java` |
| `room.LinkGroundFrame(int, int, boolean, boolean, boolean, boolean, int, int, int)` | `LinkGroundFrame(..., LinkMotionState, ...)` | `main/java/linksawakening/app/gameplay/LinkEquipmentPhase.java` |
| `room.LinkMotion(boolean, int, boolean, int, int, int)` | `LinkMotion(..., LinkMotionState, ...)` | `main/java/linksawakening/app/gameplay/EntityFrameInputsFactory.java` |
| `room.LinkPosition(int, int, int, int, int)` | `LinkPosition(..., Direction, ...)` | `main/java/linksawakening/app/gameplay/EntityFrameInputsFactory.java` |
| `room.OverworldTilesetTable(byte[])` | `OverworldTilesetTable(Rom)` | `main/java/linksawakening/app/SessionBootstrap.java`; `test/java/linksawakening/app/MusicDirectorTest.java`; `test/java/linksawakening/render/DialogEmulatorCapture.java`; `test/java/linksawakening/render/HouseEmulatorCapture.java` |
| `room.RoomDoors.tryInteractWithIndoorBlock(int, int, int, int)` | `doors().tryInteractWithIndoorBlock(..., Direction, ...)` | `main/java/linksawakening/app/gameplay/LinkEquipmentPhase.java` |
| `room.RoomDoors.tryLiftObject(int, int, int, boolean, int, boolean, int, boolean)` | `doors().tryLiftObject(..., Direction, ...)` | `main/java/linksawakening/app/gameplay/LinkEquipmentPhase.java` |
| `room.RoomDoors.tryOpenChest(int, int, int, boolean, int)` | `doors().tryOpenChest(..., Direction, ...)` | `main/java/linksawakening/app/gameplay/DialogInteractions.java` |
| `room.RoomDoors.tryUnlockIndoorKeyDoor(int, int, int, int)` | `doors().tryUnlockIndoorKeyDoor(..., Direction, ...)` | `main/java/linksawakening/app/gameplay/LinkEquipmentPhase.java` |
| `room.RoomDoors.tryUnlockKeyCavernKeyhole(int, int, int, int, int)` | `doors().tryUnlockKeyCavernKeyhole(..., Direction, ...)` | `main/java/linksawakening/app/gameplay/LinkEquipmentPhase.java` |
| `room.RoomDoors.tryUnlockTailCaveKeyhole(int, int, int, int, boolean)` | `doors().tryUnlockTailCaveKeyhole(..., Direction, ...)` | `main/java/linksawakening/app/gameplay/LinkEquipmentPhase.java` |
| `room.RoomLoader(byte[])` | `RoomLoader(Rom)` | `main/java/linksawakening/app/SessionBootstrap.java`; `test/java/linksawakening/app/MabeCollisionSweepTest.java`; `test/java/linksawakening/app/MusicDirectorTest.java`; `test/java/linksawakening/render/DialogEmulatorCapture.java`; `test/java/linksawakening/render/HouseEmulatorCapture.java` |
| `room.RoomPaletteLoader(byte[])` | `RoomPaletteLoader(Rom)` | `main/java/linksawakening/entity/EntitySpriteCatalog.java` |
| `room.RoomPlayerItems.shovelAnimationState(int, int)` | `playerItems().shovelAnimationState(Direction, ...)` | `main/java/linksawakening/app/SessionBootstrap.java` |
| `room.RoomPlayerItems.throwLiftedEntity(int)` | `playerItems().throwLiftedEntity(Direction, ...)` | `main/java/linksawakening/app/gameplay/LinkEquipmentPhase.java` |
| `room.RoomSessionConfig(byte[], GPU, RoomLoader, OverworldTilesetTable, OverworldCollision, Presentation, RomRandomByteSource)` | `RoomSessionConfig(Rom, ...)` | `main/java/linksawakening/app/SessionBootstrap.java`; `test/java/linksawakening/app/MusicDirectorTest.java`; `test/java/linksawakening/render/DialogEmulatorCapture.java`; `test/java/linksawakening/render/HouseEmulatorCapture.java` |
| `transition.TransitionController.setRomData(byte[])` | `setRomData(Rom)` | `main/java/linksawakening/app/SessionBootstrap.java` |

The temporary `-Xlint:deprecation,removal` compile produces six world-main
`[removal]` diagnostics, all at `new Rom(byte[])` inside the listed byte-array
entry points. `Rom.of(bytes)` would validate partial buffers earlier and change
the old API's behavior. Each bridge preserves its previous null handling and
shared storage; no suppression or build-argument change was committed.

Three `unsafeBytes()` catalog handoffs remain because their typed replacements
are in stable/off-limits `entity` sources: `EntityRoomLoader` and
`RoomSessionContext` construct `EntitySpriteHandlerCatalog`, and `RoomLoader`
constructs `EntitySpriteCatalog`. No internal world reader uses those arrays.

Enum adapters preserve arbitrary raw values rather than narrowing validation.
Known facings and motion states have enum classifications; unknown raw motion
states retain numeric gates. Inventory saved bytes, flag-only tunic/power inputs,
ocarina song flags/unknown selections and entity-specific ground codes remain
raw where their domains are not the corresponding closed Java enum. Entity
water `$02` must not be interpreted as Link `GroundStatus` by numeric equality.

## Allocation measurements and remaining sources

The baseline and final figures use the unchanged `app/SessionAllocationTest`
update-only probes, with ROM and assertions active, including world and audio
but excluding framebuffer composition. The active run varies slightly with
JIT compilation: step 5 measured 7,445, the latest full gate measured 7,457.
Idle measured 3,312 in both runs. The ROM-free six-entity
`EntityTickAllocationTest` remains 874.43 B/tick. Its 1,100-byte budget is unchanged;
app budgets remain unchanged because that test directory is off-limits.

Immediate `world.room` consumers now use cached inspection views. Public
`ActiveRoom.entities()` and retained `RoomRenderSnapshot.entities()` still own
immutable data, verified by retained-value and existing identity assertions.
An eight-entry presentation cache handles recurring poses; Marin's immutable
pose cache compares all source fields before reuse. Frame builders are reused,
but `EntityFrameInputs` and emitted `EntityFrameContext` stay immutable so
consumers can retain earlier inputs safely. Room-owned lists copy before clear
when ownership is requested. Empty drain/addAll/merge guards and indexed
consumption avoid temporary arrays and iterators. The new warmed outbox/merge
probe asserts exactly zero allocated bytes for the no-event path.

The two initial world idle sources, owned inspection snapshots and repeated
Marin entity replacements, disappeared from the steady idle hot path. A sampled
JFR first located them. To attribute the smaller remaining sources, a separate
headless diagnostic warmed 2,000 frames and recorded 10,000 idle frames with
`jdk.ObjectAllocationOutsideTLAB`, `-XX:-UseTLAB` and stack depth 128. The diagnostic
measured 3,317.79 B/frame by ThreadMXBean. These diagnostic rates have different
JIT/escape-analysis conditions from the normal 3,312-byte full-suite number;
they are attribution, not a second budget.

| Rank | Exact allocating file:method (line) | Allocation | Diagnostic B/frame | Owner |
| --- | --- | --- | ---: | --- |
| 1 | `audio/music/FrameAudioPlayer.java:update` (61) | PCM `short[]` | 3,216.00 | Stable audio, off-limits |
| 2 | `app/screen/GameplayScreen.java:tick` (53) | immutable-list iterator | 32.00 | Off-limits app |
| 3 | `gameplay/DialogSoundRouter.java:routePending` (14) | immutable-list iterator | 32.00 | Off-limits gameplay |
| 4 | `startup/MarinWakeUpMotion.java:tick` (54) | `Update` record | 24.60 | Off-limits startup |
| 5 | `startup/MarinWakeUpMotion.java:tickMarinPresentation` (74) | `MarinPresentation` record | 12.85 | Off-limits startup |

The two startup records are partially eliminated as JIT compilation settles.
All top five remaining sites are outside world and were left untouched. Rare
JVM linkage/deoptimization strings/byte arrays were not counted as steady
per-frame world sources. JFR files and diagnostic runners stayed in `/tmp`.
Changed entities, active motion results, immutable frame values and actual
emitted events can still allocate; this does not claim zero whole-frame cost.

## Boundary and visibility enforcement

`WorldDependencyTest` retains its world-to-app, handler/motion, service-field
and event-value rules. It now checks every nonworld main caller against the
explicit app/world surface plus seven stable contracts: `EntityRoomLoader`
(sprite room table), `BowWowChain`, `HookshotChainOam`, `PincerBodyOam`,
`WingedOctorokOam` (render values), `SwitchBlockLinkInteraction` (physics), and
`RoomPaletteLoader` (sprite catalog). The app allow-list drops unused
`EntityTickInputs` and `LinkWrite` entries; these remain explicit public value
contracts for facade signatures and caller implementations.

The shared `ClassFileReferences` reader accepts main and test roots. A second
new guard requires every public top-level world type to have a compiled
cross-package client or an explicit public-contract entry. Twenty-eight types
became package-private. The remaining entity services used across
`world.entity`, `.handler`, `.motion` and `.event` need Java public visibility;
that does not make those packages application entry points. No shelling out
to bytecode tools or method-name substring matching was introduced.

## Readability audit

Selected by file length and branch complexity within room/object/transition;
reviewed source ordering, naming, guards, responsibilities and ROM references.
No room/object/transition method exceeds 78 lines after the review.

| File | Lines before → after | Review/change |
| --- | --- | --- |
| `OverworldBushInteraction.java` | 395 → 395 | Existing CheckStaticSwordCollision / RevealObjectUnderObject documentation, rule dispatch, bounds checks and tile refresh separation were already clear; retained unchanged. |
| `RoomSessionViews.java` | 399 → 216 | Moved 30 test-only methods into RoomDebugFixture; migrated 340 calls; removed five unused dependencies; kept live queries and owned save/render projections. |
| `RoomTransitionController.java` | 307 → 307 | Names GameplayWorldLoadRoomHandler; retained category guards and observable load/reset/scroll order. |
| `RoomLiftInteractions.java` | 310 → 307 | Extracted presentation publication; renamed misleading sword-area lift probes; documented statue contact source; retained pull timing and power gates. |
| `RoomPlayerItemActions.java` | 320 → 268 | Extracted eight repeated publication blocks at the exact original sites; documented PlaceBomb/ShootArrow responsibilities. |
| `RoomEntityOutbox.java` | 313 → 289 | Centralized eleven identical owned-list drains, preserving empty guards and copy-before-clear; kept combat/dialog ordering special cases separate. |
| `RoomIndoorDoorInteractions.java` | 265 → 270 | Named boss-door physics/object constants; documented EntityInitPushedBlock; retained short-circuit probes and key-consumption gates. |
| `DroppableRupeeSystem.java` | 262 → 261 | Removed a redundant stairs-object clause already covered by the non-bush guard; retained RNG read order, timers and hitbox constants. |
| `RoomObjectMacroDecoder.java` | 212 → 209 | Named table groups, terminator and directional status bits; clarified macro index; retained bounds, byte wrapping and loader persistence rules. |
| `RoomTransitionCoordinator.java` | 262 → 263 | Names ApplyCollisionWithDoor / LinkMotionMapFadeOutHandler; retained the required pit/indoor/stair/door/fallback guard order. |
| `RoomLoadCoordinator.java` | 300 → 300 | Names LoadRoom and documents load/initialization/collision responsibilities; retained side-view and transition selector behavior. |
| `RoomPaletteLoader.java` | 256 → 266 | Named row sizes/counts and selector flag masks; distinguished entering-room selector 4 from Direction; retained first-match and invalid-selector termination. |

## Deliberately retained

Only the 22 listed source-compatibility signatures, three catalog handoffs and
public Java cross-package contracts remain. Inputs and public historical
snapshots stay immutable. All off-limits main/test packages and stable packages
were preserved. No runtime/window or screenshot tooling was used. Nothing was
pushed. The final commit SHA is reported by the coordinator handoff, avoiding a
self-referential SHA in this tracked file.
