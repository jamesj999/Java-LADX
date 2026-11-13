# Entity dependency and null-contract audit

Baseline: merged develop `e8bf6525`. Counts are Java AST equality/inequality
comparisons with null in `world/entity` main, including nested types. They do
not count text in comments or `Objects.requireNonNull` calls. The table groups
nested types under their source file. This is an audit record, not a shape target.

| Category | Baseline | After |
| --- | ---: | ---: |
| (a) Optional collaborators in execution paths | 246 | 0 |
| (b) Optional domain values | 34 | 24 |
| (c) Boundary validation / construction-time default selection | 76 | 81 |
| (d) Lazy initialization / cache null markers | 18 | 0 |
| (e) No-result protocols | 61 | 28 |
| (f) Leftovers | 0 | 0 |
| **Total** | **435** | **133** |

Category (c) includes one-time source selections (RNG, sprite source, chest
source, trace observer and Slime Eye ROM reader). Legacy nullable public
inputs are normalized at their boundary. This does not mean callers must now
supply every optional builder input. Constructor validation and existing public
motion preconditions remain; runtime controllers hold non-null services.

The remaining domain optionals describe unspawned hookshot state, an unavailable
Moldorm tail, missing parent/bar entities, follower position history, an omitted
saved-room status table, absent snapshot presentation metadata, occupancy
entries, optional combat overrides and a ground result not yet computed. Each
is documented on its owning field, component or input. No-result values remain
for optional emitted spawn/effect/reward requests where a nullable result is
already clear and avoids creating an object on every tick.

## Remaining comparisons by source class

Categories (a), (d) and (f) are zero in every class. Classes absent from this
table contain no null comparisons. Columns sum to the totals above.

| Class | (b) Domain | (c) Boundary | (e) No result | Total |
| --- | ---: | ---: | ---: | ---: |
| `EnemyProjectileCollision` | 0 | 3 | 0 | 3 |
| `EntityBackgroundCollisionResolution` | 0 | 1 | 0 | 1 |
| `EntityBackgroundMechanics` | 3 | 0 | 0 | 3 |
| `EntityBossRewards` | 1 | 0 | 0 | 1 |
| `EntityChestContents` | 0 | 1 | 0 | 1 |
| `EntityCombatResolver` | 1 | 0 | 0 | 1 |
| `EntityFrameContextBuilder` | 1 | 0 | 0 | 1 |
| `EntityFrameRandom` | 0 | 1 | 0 | 1 |
| `EntityRewardController` | 0 | 0 | 1 | 1 |
| `EntityRoomLoader` | 2 | 2 | 0 | 4 |
| `EntityRuntimeConfiguration` | 0 | 1 | 0 | 1 |
| `EntitySlotPipeline` | 2 | 0 | 2 | 4 |
| `EntitySpriteViews` | 0 | 1 | 0 | 1 |
| `FollowingNpcEntitySpawner` | 2 | 7 | 0 | 9 |
| `GenieEntityController` | 0 | 0 | 4 | 4 |
| `RomRandomByteSource` | 0 | 1 | 0 | 1 |
| `RoomEntity` | 0 | 2 | 0 | 2 |
| `RoomEntityRuntimeBuilder` | 0 | 8 | 0 | 8 |
| `RoomEntitySnapshot` | 0 | 6 | 0 | 6 |
| `SlimeEyeController` | 0 | 1 | 0 | 1 |
| `event/DungeonWarpRequest` | 0 | 1 | 0 | 1 |
| `event/EntityCombatEvent` | 0 | 3 | 0 | 3 |
| `event/EntityProjectileEvent` | 0 | 2 | 0 | 2 |
| `event/MagicPowderObjectRequest` | 0 | 1 | 0 | 1 |
| `handler/ArmosKnightHandler` | 0 | 0 | 2 | 2 |
| `handler/BomberHandler` | 0 | 0 | 1 | 1 |
| `handler/CuccoHandler` | 0 | 0 | 1 | 1 |
| `handler/GiantGopongaFlowerHandler` | 0 | 0 | 1 | 1 |
| `handler/HidingSlimeKeyHandler` | 0 | 0 | 1 | 1 |
| `handler/HinoxHandler` | 0 | 0 | 1 | 1 |
| `handler/HookshotChainHandler` | 1 | 0 | 0 | 1 |
| `handler/LikeLikeHandler` | 0 | 0 | 1 | 1 |
| `handler/MadBomberHandler` | 0 | 0 | 1 | 1 |
| `handler/MoblinKingHandler` | 0 | 0 | 1 | 1 |
| `handler/PokeyHandler` | 0 | 0 | 1 | 1 |
| `handler/RoamingEnemyTypeHandler` | 0 | 0 | 1 | 1 |
| `handler/RollingBonesHandler` | 1 | 0 | 0 | 1 |
| `handler/StalfosEvasiveHandler` | 0 | 0 | 1 | 1 |
| `handler/WingedOctorokHandler` | 0 | 0 | 1 | 1 |
| `handler/WizrobeHandler` | 0 | 0 | 1 | 1 |
| `handler/ZolGelTypeHandler` | 0 | 0 | 1 | 1 |
| `handler/ZombieHandler` | 0 | 0 | 1 | 1 |
| `handler/ZoraHandler` | 0 | 0 | 1 | 1 |
| `motion/BlooperMotion` | 1 | 0 | 0 | 1 |
| `motion/BooBuddyMotion` | 0 | 2 | 0 | 2 |
| `motion/BoomerangMotion` | 1 | 0 | 0 | 1 |
| `motion/BushCrawlerMotion` | 0 | 1 | 0 | 1 |
| `motion/BuzzBlobMotion` | 0 | 1 | 0 | 1 |
| `motion/ColorShellMotion` | 2 | 2 | 0 | 4 |
| `motion/CrowMotion` | 0 | 1 | 0 | 1 |
| `motion/CuccoMotion` | 0 | 2 | 1 | 3 |
| `motion/DogMotion` | 0 | 1 | 0 | 1 |
| `motion/FishMotion` | 0 | 1 | 0 | 1 |
| `motion/FollowingNpcMotion` | 1 | 0 | 0 | 1 |
| `motion/HinoxMotion` | 0 | 1 | 0 | 1 |
| `motion/HookshotBridgeMotion` | 1 | 4 | 0 | 5 |
| `motion/HookshotChainMotion` | 1 | 5 | 0 | 6 |
| `motion/LaserMotion` | 2 | 0 | 0 | 2 |
| `motion/MadBomberMotion` | 0 | 0 | 1 | 1 |
| `motion/MiniMoldormMotion` | 0 | 1 | 0 | 1 |
| `motion/MoldormMotion` | 1 | 1 | 0 | 2 |
| `motion/PiranhaMotion` | 0 | 1 | 0 | 1 |
| `motion/PolsVoiceMotion` | 0 | 1 | 0 | 1 |
| `motion/RoamingEnemyMotion` | 0 | 0 | 1 | 1 |
| `motion/SandCrabMotion` | 0 | 1 | 0 | 1 |
| `motion/SecretSeashellMotion` | 0 | 1 | 0 | 1 |
| `motion/SnakeMotion` | 0 | 1 | 0 | 1 |
| `motion/SpikedBeetleMotion` | 0 | 1 | 0 | 1 |
| `motion/TarinRaccoonMotion` | 0 | 1 | 0 | 1 |
| `motion/UrchinMotion` | 0 | 1 | 0 | 1 |
| `motion/WarpMotion` | 0 | 2 | 0 | 2 |
| `motion/WingedOctorokMotion` | 0 | 1 | 0 | 1 |
| `motion/WitchRatMotion` | 0 | 1 | 0 | 1 |
| `motion/WizrobeMotion` | 0 | 1 | 0 | 1 |
| `motion/WizrobeProjectileMotion` | 0 | 1 | 0 | 1 |
| `motion/ZombieMotion` | 0 | 1 | 0 | 1 |
| `motion/ZoraMotion` | 0 | 1 | 0 | 1 |

## Compatibility and room-side hand-off

No externally used signature changed and no compatibility shim was added in
this round. `RoomLoadCoordinator` and `RoomFollowerSynchronizer` already supply
real sprite, combat, chest and ROM data, plus the RNG; their code is untouched.
Room configuration setters still support replacing geometry/query policies.
They normalize an omitted legacy input once when installed, not on every use.

| Retained boundary | Internal representation | Follow-up needed |
| --- | --- | --- |
| `RoomEntityRuntimeBuilder.spriteHandlers(EntitySpriteHandlerCatalog)` | `EntitySpriteViews` ROM or unsupported source | None; the external final catalog needs no new API |
| `RoomEntityRuntimeBuilder.chestContentsTable(ChestContentsTable)` | `EntityChestContents.RomContents` or `Empty` | None; the external final table needs no new API |
| Existing frame/motion overloads | Named policies selected by builder or input normalization | No new migration required; old `from` / `ForTest` adapters were already removed before this round |
| Snapshot sprite selection / tiles | Nullable owned presentation metadata | Kept because absence is domain data used by external callers |

## Review of the 88 baseline small classes

The earlier metric counted Java files declaring a **class** with fewer than
40 real-code lines (excluding comments, imports, package and brace-only lines).
It excluded files declaring only a record, event record or interface. The list
below preserves that exact baseline inventory rather than silently changing the
metric to include those value types.

### Value / presentation projections: 3 retained

These construct immutable OAM values for distinct sprite layouts. They keep the
projection arithmetic together; standalone event/value records are also retained
but were not among the 88 classes counted by the original metric.

- `HookshotChainOam` (27 baseline code lines).
- `PincerBodyOam` (24 baseline code lines).
- `WingedOctorokOam` (16 baseline code lines).

### Real small collaborators and complete entity routines: 83 retained

The handlers below each own a complete type/family registration and its callback,
rather than a fragment of another handler. Motions own a complete ROM algorithm
or state. Slot allocation/validation, collision algorithms, seeded RNG, motion
registry, runtime state, live projections and stable probe adapters are shared
responsibilities with real consumers. `EntityDebugConfiguration` is an externally
used configuration API, including `world/room/RoomDebugFixture`. Moldorm history
remains separate because it owns the tail projection cache; screen shake owns
its countdown and phase. `EntityFrameCollision` owns stable callback adapters
whose identity avoids per-frame allocations.

- `ColorShellWorld` (22 baseline code lines).
- `EntityCollisionPointProbe` (15 baseline code lines).
- `EntityDebugConfiguration` (13 baseline code lines).
- `EntityFrameCollision` (31 baseline code lines).
- `EntityFrameRandom` (10 baseline code lines).
- `EntityLinkCollisionRules` (39 baseline code lines).
- `EntityLinkState` (35 baseline code lines).
- `EntityLiveSlotList` (27 baseline code lines).
- `EntityMotionRegistry` (10 baseline code lines).
- `EntityPresentationState` (38 baseline code lines).
- `EntityRuntimeState` (11 baseline code lines).
- `EntitySideViewGravity` (20 baseline code lines).
- `EntitySlotAllocator` (9 baseline code lines).
- `EntitySlotValidation` (19 baseline code lines).
- `EntityWarpController` (11 baseline code lines).
- `PairoddProjectileObjectCollision` (17 baseline code lines).
- `PlayerProjectileLaunchSpeed` (8 baseline code lines).
- `RomRandomByteSource` (34 baseline code lines).
- `handler/AntiFairyHandler` (9 baseline code lines).
- `handler/ArmosStatueHandler` (37 baseline code lines).
- `handler/BoomerangHandler` (22 baseline code lines).
- `handler/ButterflyHandler` (11 baseline code lines).
- `handler/BuzzBlobHandler` (33 baseline code lines).
- `handler/ChestWithItemHandler` (29 baseline code lines).
- `handler/CrowHandler` (35 baseline code lines).
- `handler/CrystalSwitchHandler` (24 baseline code lines).
- `handler/DroppableFairyHandler` (25 baseline code lines).
- `handler/FishHandler` (32 baseline code lines).
- `handler/FollowingNpcTypeHandler` (31 baseline code lines).
- `handler/GhiniTypeHandler` (38 baseline code lines).
- `handler/GiantGopongaFlowerHandler` (32 baseline code lines).
- `handler/GibdoHandler` (9 baseline code lines).
- `handler/GoombaHandler` (29 baseline code lines).
- `handler/GopongaFlowerHandler` (22 baseline code lines).
- `handler/GopongaFlowerProjectileHandler` (31 baseline code lines).
- `handler/HardhatBeetleHandler` (23 baseline code lines).
- `handler/HeartContainerHandler` (17 baseline code lines).
- `handler/HidingZolHandler` (37 baseline code lines).
- `handler/HookshotBridgeHandler` (13 baseline code lines).
- `handler/InstrumentOfTheSirensHandler` (35 baseline code lines).
- `handler/IronMaskHandler` (21 baseline code lines).
- `handler/KanaletBombableWallHandler` (19 baseline code lines).
- `handler/KeeseHandler` (14 baseline code lines).
- `handler/KidHandler` (15 baseline code lines).
- `handler/LeeverHandler` (22 baseline code lines).
- `handler/LiftableRockHandler` (15 baseline code lines).
- `handler/LikeLikeHandler` (37 baseline code lines).
- `handler/MadamMeowmeowHandler` (20 baseline code lines).
- `handler/MagicPowderSprinkleHandler` (22 baseline code lines).
- `handler/MagicRodFireballHandler` (21 baseline code lines).
- `handler/MarinIndoorHandler` (13 baseline code lines).
- `handler/MaskedMimicGoriyaHandler` (34 baseline code lines).
- `handler/MimicHandler` (26 baseline code lines).
- `handler/MoblinHandler` (9 baseline code lines).
- `handler/MusicalNoteHandler` (16 baseline code lines).
- `handler/OwlEventHandler` (18 baseline code lines).
- `handler/PairoddHandler` (31 baseline code lines).
- `handler/PeahatHandler` (17 baseline code lines).
- `handler/PincerHandler` (27 baseline code lines).
- `handler/PiranhaPlantHandler` (31 baseline code lines).
- `handler/RichardHandler` (17 baseline code lines).
- `handler/SandCrabHandler` (33 baseline code lines).
- `handler/SleepyToadstoolHandler` (26 baseline code lines).
- `handler/SlimeEyeHandler` (6 baseline code lines).
- `handler/SnakeHandler` (24 baseline code lines).
- `handler/SpikeTrapHandler` (27 baseline code lines).
- `handler/SpikedBeetleHandler` (32 baseline code lines).
- `handler/StalfosAggressiveHandler` (25 baseline code lines).
- `handler/StarHandler` (27 baseline code lines).
- `handler/SwordBeamHandler` (23 baseline code lines).
- `handler/SwordShieldPickupHandler` (27 baseline code lines).
- `handler/TektiteHandler` (14 baseline code lines).
- `handler/ThreeOfAKindHandler` (28 baseline code lines).
- `handler/Type0X35Handler` (21 baseline code lines).
- `handler/Type0X39Handler` (10 baseline code lines).
- `handler/Type0X42Handler` (20 baseline code lines).
- `handler/WaterTektiteHandler` (18 baseline code lines).
- `handler/WitchRatHandler` (32 baseline code lines).
- `motion/BombMotion` (38 baseline code lines).
- `motion/GopongaFlowerMotion` (10 baseline code lines).
- `motion/KeyDropPointMotion` (34 baseline code lines).
- `motion/MoldormTailSnapshots` (28 baseline code lines).
- `motion/ScreenShakeMotion` (27 baseline code lines).

### Remaining slices: 2 merged

- `EntitySlotCombat` (16 baseline code lines) is now the private ordered combat
  sequence in `EntityCombatResolver`; it had no independent state or policy.
- `motion/SlimeEyeHalfMotion` (18 baseline code lines) is now the half-state
  transition methods and `HalfUpdate` value in `SlimeEyeController`. Its byte
  arithmetic and integration/timer call sites are unchanged; all existing tests
  remain and now call the owning controller.

Also merged `EntityLinkContact` into `EntityCombatResolver`: it was over the
40-line threshold but still only a continuation of that same combat sequence.
None of the three removed classes had callers outside `world/entity` main/tests.

## Final verification

The required `LA_ROM`-configured `./gradlew spotlessApply test` suite contains
2,963 tests, with zero failures/errors and one existing skip (baseline: 2,960;
three default-contract tests were added). No existing test or fixture was removed
or weakened. The complete suite ran before each commit.

Allocation guards were also run with temporary Gradle init scripts setting
`-XX:+DoEscapeAnalysis` and `-XX:-DoEscapeAnalysis`; runner settings are not
committed. Measurements are steady-state bytes per tick/frame unless noted.

| Guard | EA on | EA off | Budget |
| --- | ---: | ---: | ---: |
| Representative six-entity tick, three live inspections | 834.43 | 834.50 | 1,100 |
| Two-enemy combat | 400 | 400 | 570 |
| Cached sprite presentation | 0 | 0 | 0 |
| Repeated Marin presentation | 0 | 0 | 0 |
| Idle session simulation | 0 | 0 | Existing session guard |
| Active session simulation | 433 | 433 | Existing session guard |

Cold falling-definition decoding remains 2,480 bytes per first decode; subsequent
cached reads allocate zero. Immutable changed entities, motion result records
and emitted events account for the remaining tick allocation. Their ownership
contracts remain unchanged.

Main Java files: 371 -> 369; real-code-small class files: 88 -> 84; wildcard
imports: 0 -> 0. Of the 88 reviewed classes, two were merged; others can move
out of the small-class metric as their responsibilities become explicit. Test
Java files: 178 -> 179. All edits outside the entity source/test tree are confined
to the Entity system section of `docs/ARCHITECTURE.md`.
