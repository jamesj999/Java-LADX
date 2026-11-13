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
| Existing `from(...)` overloads and frame/motion overloads | Named policies selected by builder or input normalization | Existing earlier-round migration shims remain source-compatible |
| Snapshot sprite selection / tiles | Nullable owned presentation metadata | Kept because absence is domain data used by external callers |
