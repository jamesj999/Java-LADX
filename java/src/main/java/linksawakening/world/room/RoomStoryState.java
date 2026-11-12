package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.EnemyDropResolver;
import linksawakening.world.entity.FollowingNpcEntitySpawner;
import linksawakening.world.entity.FollowingNpcRoomContext;
import linksawakening.world.entity.FollowingNpcState;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.transition.Warp;

/** RoomStoryState operations for the live room session. */
final class RoomStoryState {
    private final RoomSession session;

    RoomStoryState(RoomSession session) {
        this.session = session;
    }

    boolean applyMarinWakeUpPresentation(int x, int y, int spriteVariant) {
        if (session.context.activeRoom == null
                || session.context.entityBridge.entityRuntime == null
                || !session.context.entityBridge.entityRuntime.applyMarinWakeUpPresentation(x, y, spriteVariant)) {
            return false;
        }
        session.context.activeRoom.replaceEntities(
                session.context.entityBridge.entityRuntime.inspection().snapshot());
        return true;
    }

    void updateHouseTarinPresentation(int frameCounter, int linkX, int linkY) {
        if (session.context.activeRoom != null
                && session.context.activeRoom.mapId() == 0x10
                && session.context.activeRoom.roomId() == 0xA3
                && session.context.tarinFlag != 1
                && session.context.tarinFlag != 2
                && session.context.entityBridge.entityRuntime != null
                && (frameCounter & 0x1F) == 0) {
            session.context.entityBridge.entityRuntime.updateHouseTarinPresentation(linkX, linkY);
            session.context.activeRoom.replaceEntities(
                    session.context.entityBridge.entityRuntime.inspection().snapshot());
        }
    }

    void setBirdKeyOwned(boolean owned) {
        session.context.hasBirdKey = owned;
    }

    void setTailKeyOwned(boolean owned) {
        session.context.hasTailKey = owned;
        if (session.context.entityBridge.entityRuntime != null) {
            session.context.entityBridge.entityRuntime.configuration().setTailKeyOwned(owned);
        }
    }

    void initializeNewGameWorldState() {
        Arrays.fill(session.context.roomState.overworldRoomStatus, (byte) 0);
        Arrays.fill(session.context.roomState.indoorARoomStatus, (byte) 0);
        Arrays.fill(session.context.roomState.indoorBRoomStatus, (byte) 0);
        Arrays.fill(session.context.roomState.colorDungeonRoomStatus, (byte) 0);
        session.context.shouldGetLostInMysteriousWoods = false;
        session.context.recentRoomEntityClears.reset();
        Arrays.fill(session.context.roomState.dungeonProgressFlags, (byte) 0);
        session.context.roomState.dungeonItemState.reset();
        session.context.followingNpcState = FollowingNpcState.none();
        session.context.followingNpcRoomNeedsSync = true;
        session.context.followingLinkPositionHistory.fill(0x08, 0x10, 0, 0);
        session.context.hasBirdKey = false;
        session.context.hasTailKey = false;
        session.context.bowWowState = 0;
        session.context.richardSpokenFlag = 0;
        session.context.tarinFlag = 0;
        session.context.playerShieldLevel = 0;
        session.context.playerSwordLevel = 0;
        session.context.playerBraceletLevel = 0;
        session.context.houseNpcTradeItem = 0;
        session.context.entityInputs.entityGoldenLeavesCount = 0;
        session.context.enemyDropCounters = new EnemyDropResolver.CounterState(0, 0);
        session.context.boomerangSoundCounter = 0;
        session.context.boomerangSoundClockProvided = false;
        session.context.dungeonTitleMessageCountdown = 0;
        session.context.switchBlocksState = 0;
        session.context.switchableObjectAnimationStage = 0;
        session.context.switchButtonPressed = 0;
        session.context.currentOverworldTilesetId = session.W_TILESET_NO_UPDATE;
        session.context.roomTransientState.clearTransientRoomState();
    }

    void restoreRoomStatuses(
            byte[] overworldStatus, byte[] indoorAStatus, byte[] indoorBStatus, byte[] colorDungeonStatus) {
        session.context.roomState.restoreRoomStatuses(
                overworldStatus, indoorAStatus, indoorBStatus, colorDungeonStatus);
        if (session.context.entityBridge.entityRuntime != null && session.context.activeRoom != null) {
            session.context
                    .entityBridge
                    .entityRuntime
                    .configuration()
                    .setEntityRoomStatus(session.context.roomSessionViews.activeRoomStatusFlags());
            session.context.entityBridge.entityRuntime.configuration().setTailKeyOwned(session.context.hasTailKey);
        }
    }

    void setFollowerSpriteOverrides(Map<Integer, EntitySpriteDefinition> overrides) {
        if (session.context.activeRoom == null
                || session.context.activeRoom.entities() == null
                || session.context.activeRoom.entities().spriteSelection() == null) {
            return;
        }
        Map<Integer, EntitySpriteDefinition> merged = new HashMap<>(
                session.context.activeRoom.entities().spriteSelection().spriteOverrides());
        if (overrides != null) {
            merged.putAll(overrides);
        }
        RoomEntitySnapshot updated = session.context
                .activeRoom
                .entities()
                .withSpriteSelection(
                        session.context.activeRoom.entities().spriteSelection().withSpriteOverrides(merged));
        session.context.activeRoom.replaceEntities(updated);
        if (session.context.entityBridge.entityRuntime != null) {
            session.context.entityBridge.entityRuntime.configuration().setSpriteSelection(updated.spriteSelection());
        }
    }

    void setFollowingNpcState(
            FollowingNpcState state, int linkX, int linkY, int linkZ, int entityYOffset, int linkDirection) {
        if (state == null) {
            throw new IllegalArgumentException("Follower state cannot be null");
        }
        session.context.followingLinkX = linkX & 0xFF;
        session.context.followingLinkY = linkY & 0xFF;
        session.context.followingLinkZ = linkZ & 0xFF;
        session.context.followingEntityYOffset = entityYOffset & 0xFF;
        session.context.followingLinkDirection = linkDirection & 0xFF;
        if (!state.equals(session.context.followingNpcState)) {
            session.context.followingNpcRoomNeedsSync = true;
        }
        session.context.followingNpcState = state;
        session.context.roomStoryState.synchronizeFollowingNpcEntitiesIfNeeded();
    }

    FollowingNpcState followingNpcState() {
        return session.context.followingNpcState;
    }

    int bowWowState() {
        return session.context.bowWowState & 0xFF;
    }

    void setBowWowState(int state) {
        if ((state & ~0xFF) != 0) {
            throw new IllegalArgumentException("BowWow state must be an unsigned byte");
        }
        session.context.bowWowState = state;
        if (session.context.entityBridge.entityRuntime != null) {
            session.context.entityBridge.entityRuntime.configuration().setBowWowState(state);
        }
    }

    int richardSpokenFlag() {
        return session.context.richardSpokenFlag & 0xFF;
    }

    void setRichardSpokenFlag(int flag) {
        if ((flag & ~0xFF) != 0) {
            throw new IllegalArgumentException("Richard spoken flag must be an unsigned byte");
        }
        session.context.richardSpokenFlag = flag;
    }

    int tarinFlag() {
        return session.context.tarinFlag & 0xFF;
    }

    void setTarinFlag(int flag) {
        if ((flag & ~0xFF) != 0) {
            throw new IllegalArgumentException("Tarin flag must be an unsigned byte");
        }
        session.context.tarinFlag = flag;
    }

    void setChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel) {
        if ((shieldLevel & ~0xFF) != 0 || (swordLevel & ~0xFF) != 0 || (powerBraceletLevel & ~0xFF) != 0) {
            throw new IllegalArgumentException("Player upgrade levels must be unsigned bytes");
        }
        session.context.playerShieldLevel = shieldLevel;
        session.context.playerSwordLevel = swordLevel;
        session.context.playerBraceletLevel = powerBraceletLevel;
        configureHouseNpcStoryState();
        if (session.context.entityBridge.entityRuntime != null) {
            session.context
                    .entityBridge
                    .entityRuntime
                    .configuration()
                    .setChestPlayerLevels(shieldLevel, swordLevel, powerBraceletLevel);
            if (session.context.activeRoom != null) {
                session.context.activeRoom.replaceEntities(
                        session.context.entityBridge.entityRuntime.inspection().snapshot());
            }
        }
    }

    void setHouseNpcTradeItem(int tradeItem) {
        if ((tradeItem & ~0xFF) != 0) throw new IllegalArgumentException("Trade item must be a byte");
        session.context.houseNpcTradeItem = tradeItem;
        configureHouseNpcStoryState();
    }

    private void configureHouseNpcStoryState() {
        if (session.context.entityBridge.entityRuntime != null) {
            session.context
                    .entityBridge
                    .entityRuntime
                    .configuration()
                    .setHouseNpcStoryState(
                            session.context.tarinFlag,
                            session.context.houseNpcTradeItem,
                            Byte.toUnsignedInt(session.context.roomState.dungeonProgressFlags[2]),
                            Byte.toUnsignedInt(session.context.roomState.overworldRoomStatus[0xFD]));
        }
    }

    void setEntityDefaultMusicTrack(int trackId) {
        if ((trackId & ~0xFF) != 0) {
            throw new IllegalArgumentException("Default music track must be an unsigned byte");
        }
        session.context.entityDefaultMusicTrack = trackId;
        session.context.roomStoryState.updateWitchEnvironment();
    }

    void updateWitchEnvironment() {
        if (session.context.entityBridge.entityRuntime != null) {
            session.context
                    .entityBridge
                    .entityRuntime
                    .configuration()
                    .setWitchEnvironment(
                            session.context.indoorTorchPaletteEffect.effectAddress(),
                            session.context.indoorTorchPaletteEffect.paletteDataFlags(),
                            session.context.entityDefaultMusicTrack);
        }
    }

    boolean shouldGetLostInMysteriousWoods() {
        return session.context.shouldGetLostInMysteriousWoods;
    }

    void synchronizeFollowingNpcEntitiesIfNeeded() {
        if (!session.context.followingNpcRoomNeedsSync
                || session.context.activeRoom == null
                || session.context.entityBridge.entityRuntime == null
                || session.context.activeRoom.entities() == null) {
            return;
        }
        FollowingNpcRoomContext room = new FollowingNpcRoomContext(
                session.context.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD,
                session.context.activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL,
                session.context.activeRoom.mapId(),
                session.context.activeRoom.roomId());
        FollowingNpcEntitySpawner.Result result = session.context.followingNpcEntitySpawner.synchronize(
                session.context.activeRoom.entities(),
                room,
                session.context.followingNpcState,
                session.context.followingLinkX,
                session.context.followingLinkY,
                session.context.followingLinkZ,
                session.context.followingEntityYOffset,
                session.context.followingLinkDirection,
                session.context.followingLinkPositionHistory);
        session.context.followingNpcState = result.state();
        session.context.followingNpcRoomNeedsSync = false;
        session.context.activeRoom.replaceEntities(result.snapshot());
        session.context.entityBridge.entityRuntime = RoomEntityRuntime.from(
                result.snapshot(),
                session.context.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD,
                session.context.entityRandomByteSource,
                session.context.entitySpriteHandlerCatalog,
                session.context.enemyCombatTables,
                session.context.chestContentsTable,
                session.context.romTables,
                session.context.romData);
        session.context.entityBridge.entityRuntime.configuration().setColorShellWorld(session.context.colorShellWorld);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setFollowingNpcState(session.context.followingNpcState);
        session.context.entityBridge.entityRuntime.configuration().setEntityMapId(session.context.activeRoom.mapId());
        session.context.entityBridge.entityRuntime.configuration().setEntityRoomId(session.context.activeRoom.roomId());
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setDungeonMinibossReady(session.context.activeRoom.mapId() >= 0
                        && session.context.activeRoom.mapId() < session.context.roomState.dungeonProgressFlags.length
                        && (session.context.roomState.dungeonProgressFlags[session.context.activeRoom.mapId()] & 0x01)
                                != 0);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setOwlDialogResolver(session.context.owlEventDialogResolver::globalDialogId);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setOwlDefaultMusicResolver(session.context.owlEventDialogResolver::defaultMusicTrack);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setGroundInteraction(session.context.roomCollisionQueries::entityGroundInteraction);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setBackgroundInteraction(session.context.entityBackgroundInteraction);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setObjectQuery(session.context.roomCollisionQueries::entityObjectSample);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setObjectIntersectionQuery(session.context.roomCollisionQueries::entityObjectIntersectionSample);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setGroundInteractionSideScrolling(
                        session.context.activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setActionButtonsHeld(
                        session.context.entityInputs.actionButtonAHeld, session.context.entityInputs.actionButtonBHeld);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setTalkState(
                        session.context.entityInputs.entityInventoryAppearing,
                        session.context.entityInputs.entityDialogCooldown,
                        session.context.entityInputs.entityWindowY);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setJoypadHeld(session.context.entityInputs.joypadHeld);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setPressedButtonsMask(session.context.entityInputs.entityPressedButtonsMask);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setPowerBraceletButtonHeld(session.context.entityInputs.powerBraceletButtonHeld);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setBombButtonHeld(session.context.entityInputs.bombButtonHeld);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setLiftedLinkC13B(session.context.followingEntityYOffset);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setEntityRoomStatus(session.context.roomSessionViews.activeRoomStatusFlags());
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setDungeonMinibossReady(session.context.activeRoom.mapId() >= 0
                        && session.context.activeRoom.mapId() < session.context.roomState.dungeonProgressFlags.length
                        && (session.context.roomState.dungeonProgressFlags[session.context.activeRoom.mapId()] & 0x01)
                                != 0);
        session.context.entityBridge.entityRuntime.configuration().setTailKeyOwned(session.context.hasTailKey);
        session.context
                .entityBridge
                .entityRuntime
                .configuration()
                .setSecretSeashellPegasusCollisionState(
                        session.context.entityInputs.secretSeashellScreenShakeActive,
                                session.context.entityInputs.secretSeashellPegasusCollisionActive,
                        session.context.entityInputs.secretSeashellPegasusCollisionX,
                                session.context.entityInputs.secretSeashellPegasusCollisionY);
        configureHouseNpcStoryState();
        if (session.context.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD
                && session.context.activeRoom.mapId() == 0x10
                && session.context.activeRoom.roomId() == 0xA3) {
            session.context
                    .entityBridge
                    .entityRuntime
                    .configuration()
                    .setChestPlayerLevels(
                            session.context.playerShieldLevel,
                            session.context.playerSwordLevel,
                            session.context.playerBraceletLevel);
            session.context.activeRoom.replaceEntities(
                    session.context.entityBridge.entityRuntime.inspection().snapshot());
        }
        session.context.roomLoadCoordinator.configureEnemyDropRuntime();
    }

    void markHeartContainerCollected() {
        if (session.context.activeRoom == null) {
            return;
        }
        byte[] activeStatus =
                session.context.roomSessionViews.indoorStatusTableForMap(session.context.activeRoom.mapId());
        activeStatus[session.context.activeRoom.roomId()] |= (byte) session.ROOM_STATUS_EVENT_2;
        if (session.context.activeRoom.mapId() == 0x06) {
            session.context.roomState.indoorBRoomStatus[0x2E] |= (byte) session.ROOM_STATUS_EVENT_2;
        } else if (session.context.activeRoom.mapId() == 0x03) {
            session.context.roomState.indoorARoomStatus[0x66] |= (byte) session.ROOM_STATUS_EVENT_2;
        }
    }

    void markActiveRoomCompleted() {
        if (session.context.activeRoom == null) {
            return;
        }
        byte[] status = session.context.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                ? session.context.roomState.overworldRoomStatus
                : session.context.roomSessionViews.indoorStatusTableForMap(session.context.activeRoom.mapId());
        status[session.context.activeRoom.roomId()] |= (byte) session.ROOM_STATUS_EVENT_1;
    }
}
