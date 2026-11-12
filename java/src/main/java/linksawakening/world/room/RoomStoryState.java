package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.rom.RomTables;
import linksawakening.world.entity.ColorShellWorld;
import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.entity.FollowingNpcEntitySpawner;
import linksawakening.world.entity.FollowingNpcRoomContext;
import linksawakening.world.entity.FollowingNpcState;
import linksawakening.world.entity.RomEnemyCombatTables;
import linksawakening.world.entity.RomRandomByteSource;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.transition.Warp;

/** RoomStoryState operations for the live room session. */
final class RoomStoryState {
    private final Dependencies dependencies;

    RoomStoryState(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    boolean applyMarinWakeUpPresentation(int x, int y, int spriteVariant) {
        if (dependencies.location().activeRoom == null
                || dependencies.entityBridge().get().entityRuntime == null
                || !dependencies.entityBridge().get().entityRuntime.applyMarinWakeUpPresentation(x, y, spriteVariant)) {
            return false;
        }
        dependencies
                .location()
                .activeRoom
                .replaceEntities(dependencies
                        .entityBridge()
                        .get()
                        .entityRuntime
                        .inspection()
                        .snapshot());
        return true;
    }

    void updateHouseTarinPresentation(int frameCounter, int linkX, int linkY) {
        if (dependencies.location().activeRoom != null
                && dependencies.location().activeRoom.mapId() == 0x10
                && dependencies.location().activeRoom.roomId() == 0xA3
                && dependencies.followers().tarinFlag != 1
                && dependencies.followers().tarinFlag != 2
                && dependencies.entityBridge().get().entityRuntime != null
                && (frameCounter & 0x1F) == 0) {
            dependencies.entityBridge().get().entityRuntime.updateHouseTarinPresentation(linkX, linkY);
            dependencies
                    .location()
                    .activeRoom
                    .replaceEntities(dependencies
                            .entityBridge()
                            .get()
                            .entityRuntime
                            .inspection()
                            .snapshot());
        }
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
        if (dependencies.location().activeRoom == null
                || dependencies.location().activeRoom.entities() == null
                || dependencies.location().activeRoom.entities().spriteSelection() == null) {
            return;
        }
        Map<Integer, EntitySpriteDefinition> merged = new HashMap<>(
                dependencies.location().activeRoom.entities().spriteSelection().spriteOverrides());
        if (overrides != null) {
            merged.putAll(overrides);
        }
        RoomEntitySnapshot updated = dependencies
                .location()
                .activeRoom
                .entities()
                .withSpriteSelection(dependencies
                        .location()
                        .activeRoom
                        .entities()
                        .spriteSelection()
                        .withSpriteOverrides(merged));
        dependencies.location().activeRoom.replaceEntities(updated);
        if (dependencies.entityBridge().get().entityRuntime != null) {
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setSpriteSelection(updated.spriteSelection());
        }
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
                                .snapshot());
            }
        }
    }

    void setHouseNpcTradeItem(int tradeItem) {
        if ((tradeItem & ~0xFF) != 0) throw new IllegalArgumentException("Trade item must be a byte");
        dependencies.followers().houseNpcTradeItem = tradeItem;
        configureHouseNpcStoryState();
    }

    private void configureHouseNpcStoryState() {
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
        if (!dependencies.followers().followingNpcRoomNeedsSync
                || dependencies.location().activeRoom == null
                || dependencies.entityBridge().get().entityRuntime == null
                || dependencies.location().activeRoom.entities() == null) {
            return;
        }
        FollowingNpcRoomContext room = new FollowingNpcRoomContext(
                dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD,
                dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL,
                dependencies.location().activeRoom.mapId(),
                dependencies.location().activeRoom.roomId());
        FollowingNpcEntitySpawner.Result result = dependencies
                .followingNpcEntitySpawner()
                .synchronize(
                        dependencies.location().activeRoom.entities(),
                        room,
                        dependencies.followers().followingNpcState,
                        dependencies.followers().followingLinkX,
                        dependencies.followers().followingLinkY,
                        dependencies.followers().followingLinkZ,
                        dependencies.followers().followingEntityYOffset,
                        dependencies.followers().followingLinkDirection,
                        dependencies.followers().followingLinkPositionHistory);
        dependencies.followers().followingNpcState = result.state();
        dependencies.followers().followingNpcRoomNeedsSync = false;
        dependencies.location().activeRoom.replaceEntities(result.snapshot());
        dependencies.entityBridge().get().entityRuntime = RoomEntityRuntime.from(
                result.snapshot(),
                dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD,
                dependencies.entityRandomByteSource(),
                dependencies.entitySpriteHandlerCatalog(),
                dependencies.enemyCombatTables(),
                dependencies.chestContentsTable(),
                dependencies.romTables(),
                dependencies.romData());
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setColorShellWorld(dependencies.colorShellWorld());
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setFollowingNpcState(dependencies.followers().followingNpcState);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setEntityMapId(dependencies.location().activeRoom.mapId());
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setEntityRoomId(dependencies.location().activeRoom.roomId());
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setDungeonMinibossReady(dependencies.location().activeRoom.mapId() >= 0
                        && dependencies.location().activeRoom.mapId()
                                < dependencies.roomState().dungeonProgressFlags.length
                        && (dependencies.roomState()
                                                .dungeonProgressFlags[
                                                dependencies
                                                        .location()
                                                        .activeRoom
                                                        .mapId()]
                                        & 0x01)
                                != 0);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setOwlDialogResolver(dependencies.owlEventDialogResolver()::globalDialogId);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setOwlDefaultMusicResolver(dependencies.owlEventDialogResolver()::defaultMusicTrack);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setGroundInteraction(dependencies.roomCollisionQueries().get()::entityGroundInteraction);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setBackgroundInteraction(dependencies.entityBackgroundInteraction());
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setObjectQuery(dependencies.roomCollisionQueries().get()::entityObjectSample);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setObjectIntersectionQuery(dependencies.roomCollisionQueries().get()::entityObjectIntersectionSample);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setGroundInteractionSideScrolling(
                        dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setActionButtonsHeld(
                        dependencies.entityInputs().get().actionButtonAHeld,
                        dependencies.entityInputs().get().actionButtonBHeld);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setTalkState(
                        dependencies.entityInputs().get().entityInventoryAppearing,
                        dependencies.entityInputs().get().entityDialogCooldown,
                        dependencies.entityInputs().get().entityWindowY);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setJoypadHeld(dependencies.entityInputs().get().joypadHeld);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setPressedButtonsMask(dependencies.entityInputs().get().entityPressedButtonsMask);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setPowerBraceletButtonHeld(dependencies.entityInputs().get().powerBraceletButtonHeld);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setBombButtonHeld(dependencies.entityInputs().get().bombButtonHeld);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setLiftedLinkC13B(dependencies.followers().followingEntityYOffset);
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
                .setDungeonMinibossReady(dependencies.location().activeRoom.mapId() >= 0
                        && dependencies.location().activeRoom.mapId()
                                < dependencies.roomState().dungeonProgressFlags.length
                        && (dependencies.roomState()
                                                .dungeonProgressFlags[
                                                dependencies
                                                        .location()
                                                        .activeRoom
                                                        .mapId()]
                                        & 0x01)
                                != 0);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setTailKeyOwned(dependencies.followers().hasTailKey);
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .configuration()
                .setSecretSeashellPegasusCollisionState(
                        dependencies.entityInputs().get().secretSeashellScreenShakeActive,
                                dependencies.entityInputs().get().secretSeashellPegasusCollisionActive,
                        dependencies.entityInputs().get().secretSeashellPegasusCollisionX,
                                dependencies.entityInputs().get().secretSeashellPegasusCollisionY);
        configureHouseNpcStoryState();
        if (dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD
                && dependencies.location().activeRoom.mapId() == 0x10
                && dependencies.location().activeRoom.roomId() == 0xA3) {
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .configuration()
                    .setChestPlayerLevels(
                            dependencies.followers().playerShieldLevel,
                            dependencies.followers().playerSwordLevel,
                            dependencies.followers().playerBraceletLevel);
            dependencies
                    .location()
                    .activeRoom
                    .replaceEntities(dependencies
                            .entityBridge()
                            .get()
                            .entityRuntime
                            .inspection()
                            .snapshot());
        }
        dependencies.roomLoadCoordinator().get().configureEnemyDropRuntime();
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
            byte[] romData,
            RomTables romTables,
            java.util.function.Supplier<RoomCollisionQueries> roomCollisionQueries,
            java.util.function.Supplier<RoomLoadCoordinator> roomLoadCoordinator,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            RoomWorldState roomState,
            java.util.function.Supplier<RoomStoryState> roomStoryState,
            java.util.function.Supplier<RoomTransientState> roomTransientState) {}
}
