package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.ROOM_STATUS_EVENT_1;
import static linksawakening.world.room.RoomSessionRules.ROOM_STATUS_EVENT_2;
import static linksawakening.world.room.RoomSessionRules.W_TILESET_NO_UPDATE;

import java.util.Arrays;
import java.util.Map;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.rom.Rom;
import linksawakening.rom.RomTables;
import linksawakening.world.entity.ColorShellWorld;
import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.entity.FollowingNpcEntitySpawner;
import linksawakening.world.entity.FollowingNpcState;
import linksawakening.world.entity.RomEnemyCombatTables;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.transition.Warp;

/** LoadRoomEntities story flags and restoration; follower and NPC presentation have distinct owners. */
final class RoomStoryState {
    private final Dependencies dependencies;
    private final RoomNpcPresentation roomNpcPresentation;
    private final RoomFollowerSynchronizer roomFollowerSynchronizer;

    RoomStoryState(Dependencies dependencies) {
        this.dependencies = dependencies;
        roomNpcPresentation = new RoomNpcPresentation(new RoomNpcPresentation.Dependencies(
                dependencies.entityBridge(), dependencies.followers(), dependencies.location()));
        roomFollowerSynchronizer = new RoomFollowerSynchronizer(new RoomFollowerSynchronizer.Dependencies(
                dependencies.chestContentsTable(),
                dependencies.colorShellWorld(),
                dependencies.enemyCombatTables(),
                dependencies.entityBackgroundInteraction(),
                dependencies.entityBridge(),
                dependencies.entityInputs(),
                dependencies.entityRandomByteSource(),
                dependencies.entitySpriteHandlerCatalog(),
                dependencies.followers(),
                dependencies.followingNpcEntitySpawner(),
                dependencies.location(),
                dependencies.owlEventDialogResolver(),
                dependencies.romData(),
                dependencies.romTables(),
                dependencies.roomCollisionQueries(),
                dependencies.roomLoadCoordinator(),
                dependencies.roomSessionViews(),
                dependencies.roomState(),
                () -> this));
    }

    boolean applyMarinWakeUpPresentation(int x, int y, int spriteVariant) {
        return roomNpcPresentation.applyMarinWakeUpPresentation(x, y, spriteVariant);
    }

    void updateHouseTarinPresentation(int frameCounter, int linkX, int linkY) {
        roomNpcPresentation.updateHouseTarinPresentation(frameCounter, linkX, linkY);
    }

    void setBirdKeyOwned(boolean owned) {
        dependencies.followers().hasBirdKey = owned;
    }

    void setTailKeyOwned(boolean owned) {
        dependencies.followers().hasTailKey = owned;
        if (dependencies.entityBridge().get().entityRuntime != null) {
            dependencies.entityBridge().get().entityRuntime.configuration().setTailKeyOwned(owned);
        }
    }

    void initializeNewGameWorldState() {
        Arrays.fill(dependencies.roomState().overworldRoomStatus, (byte) 0);
        Arrays.fill(dependencies.roomState().indoorARoomStatus, (byte) 0);
        Arrays.fill(dependencies.roomState().indoorBRoomStatus, (byte) 0);
        Arrays.fill(dependencies.roomState().colorDungeonRoomStatus, (byte) 0);
        dependencies.location().shouldGetLostInMysteriousWoods = false;
        dependencies.entityFrame().recentRoomEntityClears.reset();
        Arrays.fill(dependencies.roomState().dungeonProgressFlags, (byte) 0);
        dependencies.roomState().dungeonItemState.reset();
        dependencies.followers().followingNpcState = FollowingNpcState.none();
        dependencies.followers().followingNpcRoomNeedsSync = true;
        dependencies.followers().followingLinkPositionHistory.fill(0x08, 0x10, 0, 0);
        dependencies.followers().hasBirdKey = false;
        dependencies.followers().hasTailKey = false;
        dependencies.followers().bowWowState = 0;
        dependencies.followers().richardSpokenFlag = 0;
        dependencies.followers().tarinFlag = 0;
        dependencies.followers().playerShieldLevel = 0;
        dependencies.followers().playerSwordLevel = 0;
        dependencies.followers().playerBraceletLevel = 0;
        dependencies.followers().houseNpcTradeItem = 0;
        dependencies.entityInputs().get().entityGoldenLeavesCount = 0;
        dependencies.entityFrame().enemyDropCounters = new EnemyDropResolver.CounterState(0, 0);
        dependencies.entityFrame().boomerangSoundCounter = 0;
        dependencies.entityFrame().boomerangSoundClockProvided = false;
        dependencies.puzzles().dungeonTitleMessageCountdown = 0;
        dependencies.terrain().switchBlocksState = 0;
        dependencies.terrain().switchableObjectAnimationStage = 0;
        dependencies.puzzles().switchButtonPressed = 0;
        dependencies.location().currentOverworldTilesetId = W_TILESET_NO_UPDATE;
        dependencies.roomTransientState().get().clearTransientRoomState();
    }

    void restoreRoomStatuses(
            byte[] overworldStatus, byte[] indoorAStatus, byte[] indoorBStatus, byte[] colorDungeonStatus) {
        dependencies.roomState().restoreRoomStatuses(overworldStatus, indoorAStatus, indoorBStatus, colorDungeonStatus);
        if (dependencies.entityBridge().get().entityRuntime != null && dependencies.location().activeRoom != null) {
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setEntityRoomStatus(dependencies.roomSessionViews().get().activeRoomStatusFlags());
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setTailKeyOwned(dependencies.followers().hasTailKey);
        }
    }

    void setFollowerSpriteOverrides(Map<Integer, EntitySpriteDefinition> overrides) {
        roomNpcPresentation.setFollowerSpriteOverrides(overrides);
    }

    void setFollowingNpcState(
            FollowingNpcState state, int linkX, int linkY, int linkZ, int entityYOffset, int linkDirection) {
        if (state == null) {
            throw new IllegalArgumentException("Follower state cannot be null");
        }
        dependencies.followers().followingLinkX = linkX & 0xFF;
        dependencies.followers().followingLinkY = linkY & 0xFF;
        dependencies.followers().followingLinkZ = linkZ & 0xFF;
        dependencies.followers().followingEntityYOffset = entityYOffset & 0xFF;
        dependencies.followers().followingLinkDirection = linkDirection & 0xFF;
        if (!state.equals(dependencies.followers().followingNpcState)) {
            dependencies.followers().followingNpcRoomNeedsSync = true;
        }
        dependencies.followers().followingNpcState = state;
        dependencies.roomStoryState().get().synchronizeFollowingNpcEntitiesIfNeeded();
    }

    FollowingNpcState followingNpcState() {
        return dependencies.followers().followingNpcState;
    }

    int bowWowState() {
        return dependencies.followers().bowWowState & 0xFF;
    }

    void setBowWowState(int state) {
        if ((state & ~0xFF) != 0) {
            throw new IllegalArgumentException("BowWow state must be an unsigned byte");
        }
        dependencies.followers().bowWowState = state;
        if (dependencies.entityBridge().get().entityRuntime != null) {
            dependencies.entityBridge().get().entityRuntime.configuration().setBowWowState(state);
        }
    }

    int richardSpokenFlag() {
        return dependencies.followers().richardSpokenFlag & 0xFF;
    }

    void setRichardSpokenFlag(int flag) {
        if ((flag & ~0xFF) != 0) {
            throw new IllegalArgumentException("Richard spoken flag must be an unsigned byte");
        }
        dependencies.followers().richardSpokenFlag = flag;
    }

    int tarinFlag() {
        return dependencies.followers().tarinFlag & 0xFF;
    }

    void setTarinFlag(int flag) {
        if ((flag & ~0xFF) != 0) {
            throw new IllegalArgumentException("Tarin flag must be an unsigned byte");
        }
        dependencies.followers().tarinFlag = flag;
    }

    void setChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel) {
        if ((shieldLevel & ~0xFF) != 0 || (swordLevel & ~0xFF) != 0 || (powerBraceletLevel & ~0xFF) != 0) {
            throw new IllegalArgumentException("Player upgrade levels must be unsigned bytes");
        }
        dependencies.followers().playerShieldLevel = shieldLevel;
        dependencies.followers().playerSwordLevel = swordLevel;
        dependencies.followers().playerBraceletLevel = powerBraceletLevel;
        configureHouseNpcStoryState();
        if (dependencies.entityBridge().get().entityRuntime != null) {
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setChestPlayerLevels(shieldLevel, swordLevel, powerBraceletLevel);
            if (dependencies.location().activeRoom != null) {
                dependencies
                        .location()
                        .activeRoom
                        .replaceEntities(dependencies
                                .entityBridge()
                                .get()
                                .entityRuntime
                                .inspection()
                                .view());
            }
        }
    }

    void setHouseNpcTradeItem(int tradeItem) {
        if ((tradeItem & ~0xFF) != 0) throw new IllegalArgumentException("Trade item must be a byte");
        dependencies.followers().houseNpcTradeItem = tradeItem;
        configureHouseNpcStoryState();
    }

    void configureHouseNpcStoryState() {
        if (dependencies.entityBridge().get().entityRuntime != null) {
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setHouseNpcStoryState(
                            dependencies.followers().tarinFlag,
                            dependencies.followers().houseNpcTradeItem,
                            Byte.toUnsignedInt(dependencies.roomState().dungeonProgressFlags[2]),
                            Byte.toUnsignedInt(dependencies.roomState().overworldRoomStatus[0xFD]));
        }
    }

    void setEntityDefaultMusicTrack(int trackId) {
        if ((trackId & ~0xFF) != 0) {
            throw new IllegalArgumentException("Default music track must be an unsigned byte");
        }
        dependencies.followers().entityDefaultMusicTrack = trackId;
        dependencies.roomStoryState().get().updateWitchEnvironment();
    }

    void updateWitchEnvironment() {
        if (dependencies.entityBridge().get().entityRuntime != null) {
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setWitchEnvironment(
                            dependencies.indoorTorchPaletteEffect().effectAddress(),
                            dependencies.indoorTorchPaletteEffect().paletteDataFlags(),
                            dependencies.followers().entityDefaultMusicTrack);
        }
    }

    boolean shouldGetLostInMysteriousWoods() {
        return dependencies.location().shouldGetLostInMysteriousWoods;
    }

    void synchronizeFollowingNpcEntitiesIfNeeded() {
        roomFollowerSynchronizer.synchronizeFollowingNpcEntitiesIfNeeded();
    }

    void markHeartContainerCollected() {
        if (dependencies.location().activeRoom == null) {
            return;
        }
        byte[] activeStatus = dependencies
                .roomSessionViews()
                .get()
                .indoorStatusTableForMap(dependencies.location().activeRoom.mapId());
        activeStatus[dependencies.location().activeRoom.roomId()] |= (byte) ROOM_STATUS_EVENT_2;
        if (dependencies.location().activeRoom.mapId() == 0x06) {
            dependencies.roomState().indoorBRoomStatus[0x2E] |= (byte) ROOM_STATUS_EVENT_2;
        } else if (dependencies.location().activeRoom.mapId() == 0x03) {
            dependencies.roomState().indoorARoomStatus[0x66] |= (byte) ROOM_STATUS_EVENT_2;
        }
    }

    void markActiveRoomCompleted() {
        if (dependencies.location().activeRoom == null) {
            return;
        }
        byte[] status = dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                ? dependencies.roomState().overworldRoomStatus
                : dependencies
                        .roomSessionViews()
                        .get()
                        .indoorStatusTableForMap(
                                dependencies.location().activeRoom.mapId());
        status[dependencies.location().activeRoom.roomId()] |= (byte) ROOM_STATUS_EVENT_1;
    }

    record Dependencies(
            RoomEntityFrameState entityFrame,
            RoomFollowerState followers,
            RoomLocationState location,
            RoomPuzzleState puzzles,
            RoomTerrainState terrain,
            ChestContentsTable chestContentsTable,
            ColorShellWorld colorShellWorld,
            RomEnemyCombatTables enemyCombatTables,
            RoomEntityBackgroundInteraction entityBackgroundInteraction,
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            java.util.function.Supplier<RoomEntityInputs> entityInputs,
            RomRandomByteSource entityRandomByteSource,
            EntitySpriteHandlerCatalog entitySpriteHandlerCatalog,
            FollowingNpcEntitySpawner followingNpcEntitySpawner,
            IndoorTorchPaletteEffect indoorTorchPaletteEffect,
            OwlEventDialogResolver owlEventDialogResolver,
            Rom romData,
            RomTables romTables,
            java.util.function.Supplier<RoomCollisionQueries> roomCollisionQueries,
            java.util.function.Supplier<RoomLoadCoordinator> roomLoadCoordinator,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            RoomWorldState roomState,
            java.util.function.Supplier<RoomStoryState> roomStoryState,
            java.util.function.Supplier<RoomTransientState> roomTransientState) {
        Dependencies {
            java.util.Objects.requireNonNull(entityFrame, "entityFrame");
            java.util.Objects.requireNonNull(followers, "followers");
            java.util.Objects.requireNonNull(location, "location");
            java.util.Objects.requireNonNull(puzzles, "puzzles");
            java.util.Objects.requireNonNull(terrain, "terrain");
            java.util.Objects.requireNonNull(chestContentsTable, "chestContentsTable");
            java.util.Objects.requireNonNull(colorShellWorld, "colorShellWorld");
            java.util.Objects.requireNonNull(enemyCombatTables, "enemyCombatTables");
            java.util.Objects.requireNonNull(entityBackgroundInteraction, "entityBackgroundInteraction");
            java.util.Objects.requireNonNull(entityBridge, "entityBridge");
            java.util.Objects.requireNonNull(entityInputs, "entityInputs");
            java.util.Objects.requireNonNull(entityRandomByteSource, "entityRandomByteSource");
            java.util.Objects.requireNonNull(entitySpriteHandlerCatalog, "entitySpriteHandlerCatalog");
            java.util.Objects.requireNonNull(followingNpcEntitySpawner, "followingNpcEntitySpawner");
            java.util.Objects.requireNonNull(indoorTorchPaletteEffect, "indoorTorchPaletteEffect");
            java.util.Objects.requireNonNull(owlEventDialogResolver, "owlEventDialogResolver");
            java.util.Objects.requireNonNull(romData, "romData");
            java.util.Objects.requireNonNull(romTables, "romTables");
            java.util.Objects.requireNonNull(roomCollisionQueries, "roomCollisionQueries");
            java.util.Objects.requireNonNull(roomLoadCoordinator, "roomLoadCoordinator");
            java.util.Objects.requireNonNull(roomSessionViews, "roomSessionViews");
            java.util.Objects.requireNonNull(roomState, "roomState");
            java.util.Objects.requireNonNull(roomStoryState, "roomStoryState");
            java.util.Objects.requireNonNull(roomTransientState, "roomTransientState");
        }
    }
}
