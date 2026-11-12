package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import java.util.Arrays;
import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.entity.FollowingNpcState;
import linksawakening.world.transition.Warp;

final class RoomStoryRestoration {
    private final Dependencies dependencies;

    RoomStoryRestoration(Dependencies dependencies) {
        this.dependencies = dependencies;
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

    boolean shouldGetLostInMysteriousWoods() {
        return dependencies.location().shouldGetLostInMysteriousWoods;
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
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            RoomEntityFrameState entityFrame,
            java.util.function.Supplier<RoomEntityInputs> entityInputs,
            RoomFollowerState followers,
            RoomLocationState location,
            RoomPuzzleState puzzles,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            RoomWorldState roomState,
            java.util.function.Supplier<RoomStoryState> roomStoryState,
            java.util.function.Supplier<RoomTransientState> roomTransientState,
            RoomTerrainState terrain) {}
}
