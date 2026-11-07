package linksawakening.world;

import linksawakening.world.entity.event.BoomerangObjectRequest;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
import linksawakening.world.entity.event.HinoxLinkEffectRequest;
import linksawakening.world.entity.event.HookshotBridgeUpdate;
import linksawakening.world.entity.event.InstrumentCompletionEvent;
import linksawakening.world.entity.event.KeyRewardEvent;
import linksawakening.world.entity.event.LikeLikeEvent;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.LinkAttackStepRequest;
import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.LinkFallenPoseRequest;
import linksawakening.world.entity.event.LinkFinalPositionRequest;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.LinkSwordFinalPoseRequest;
import linksawakening.world.entity.event.LinkSwordSpinPoseRequest;
import linksawakening.world.entity.event.MadamMeowMeowFullHealRequest;
import linksawakening.world.entity.event.MagicPowderObjectRequest;
import linksawakening.world.entity.event.MagicRodObjectRequest;
import linksawakening.world.entity.event.RichardProgressEvent;
import linksawakening.world.entity.event.RoomStatusPersistenceRequest;
import linksawakening.world.entity.event.RoosterLinkStateRequest;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.entity.event.SideViewPlatformLinkRequest;
import linksawakening.world.entity.event.SideViewPotLinkRequest;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;
import linksawakening.world.entity.event.SwordPickupRewardEvent;
import linksawakening.world.entity.event.ThrownBackgroundCollisionEvent;
import linksawakening.world.entity.event.ToadstoolRewardEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.event.WarpDestination;
import linksawakening.world.entity.event.WarpLinkStateRequest;
import linksawakening.world.entity.event.WitchExchangeEvent;
import linksawakening.world.entity.event.WitchRewardEvent;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import linksawakening.rom.RomBank;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomSymbols;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_WIDTH;
import static linksawakening.world.RoomSession.*;

/** RoomStoryState operations for the live room session. */
final class RoomStoryState {
    private final RoomSession session;
    RoomStoryState(RoomSession session) { this.session = session; }

    boolean applyMarinWakeUpPresentation(int x, int y, int spriteVariant) {
        if (session.activeRoom == null || session.entityBridge.entityRuntime == null
            || !session.entityBridge.entityRuntime.applyMarinWakeUpPresentation(x, y, spriteVariant)) {
            return false;
        }
        session.activeRoom.replaceEntities(session.entityBridge.entityRuntime.snapshot());
        return true;
    }

    void updateHouseTarinPresentation(int frameCounter, int linkX, int linkY) {
        if (session.activeRoom != null && session.activeRoom.mapId() == 0x10 && session.activeRoom.roomId() == 0xA3
            && session.tarinFlag != 1 && session.tarinFlag != 2
            && session.entityBridge.entityRuntime != null && (frameCounter & 0x1F) == 0) {
            session.entityBridge.entityRuntime.updateHouseTarinPresentation(linkX, linkY);
            session.activeRoom.replaceEntities(session.entityBridge.entityRuntime.snapshot());
        }
    }

    void setBirdKeyOwned(boolean owned) {
        session.hasBirdKey = owned;
    }

    void setTailKeyOwned(boolean owned) {
        session.hasTailKey = owned;
        if (session.entityBridge.entityRuntime != null) {
            session.entityBridge.entityRuntime.setTailKeyOwned(owned);
        }
    }

    void initializeNewGameWorldState() {
        Arrays.fill(session.roomState.overworldRoomStatus, (byte) 0);
        Arrays.fill(session.roomState.indoorARoomStatus, (byte) 0);
        Arrays.fill(session.roomState.indoorBRoomStatus, (byte) 0);
        Arrays.fill(session.roomState.colorDungeonRoomStatus, (byte) 0);
        session.shouldGetLostInMysteriousWoods = false;
        session.recentRoomEntityClears.reset();
        Arrays.fill(session.roomState.dungeonProgressFlags, (byte) 0);
        session.roomState.dungeonItemState.reset();
        session.followingNpcState = FollowingNpcState.none();
        session.followingNpcRoomNeedsSync = true;
        session.followingLinkPositionHistory.fill(0x08, 0x10, 0, 0);
        session.hasBirdKey = false;
        session.hasTailKey = false;
        session.bowWowState = 0;
        session.richardSpokenFlag = 0;
        session.tarinFlag = 0;
        session.playerShieldLevel = 0;
        session.entityInputs.entityGoldenLeavesCount = 0;
        session.enemyDropCounters = new EnemyDropResolver.CounterState(0, 0);
        session.boomerangSoundCounter = 0;
        session.boomerangSoundClockProvided = false;
        session.dungeonTitleMessageCountdown = 0;
        session.switchBlocksState = 0;
        session.switchableObjectAnimationStage = 0;
        session.switchButtonPressed = 0;
        session.currentOverworldTilesetId = session.W_TILESET_NO_UPDATE;
        session.clearTransientRoomState();
    }

    void restoreRoomStatuses(byte[] overworldStatus, byte[] indoorAStatus, byte[] indoorBStatus, byte[] colorDungeonStatus) {
        session.roomState.restoreRoomStatuses(overworldStatus, indoorAStatus, indoorBStatus, colorDungeonStatus);
        if (session.entityBridge.entityRuntime != null && session.activeRoom != null) {
            session.entityBridge.entityRuntime.setEntityRoomStatus(session.activeRoomStatusFlags());
            session.entityBridge.entityRuntime.setTailKeyOwned(session.hasTailKey);
        }
    }

    void setFollowerSpriteOverrides(Map<Integer, EntitySpriteDefinition> overrides) {
        if (session.activeRoom == null || session.activeRoom.entities() == null
            || session.activeRoom.entities().spriteSelection() == null) {
            return;
        }
        Map<Integer, EntitySpriteDefinition> merged = new HashMap<>(
            session.activeRoom.entities().spriteSelection().spriteOverrides());
        if (overrides != null) {
            merged.putAll(overrides);
        }
        RoomEntitySnapshot updated = session.activeRoom.entities().withSpriteSelection(
            session.activeRoom.entities().spriteSelection().withSpriteOverrides(merged));
        session.activeRoom.replaceEntities(updated);
        if (session.entityBridge.entityRuntime != null) {
            session.entityBridge.entityRuntime.setSpriteSelection(updated.spriteSelection());
        }
    }

    void setFollowingNpcState(FollowingNpcState state, int linkX, int linkY, int linkZ, int entityYOffset, int linkDirection) {
        if (state == null) {
            throw new IllegalArgumentException("Follower state cannot be null");
        }
        session.followingLinkX = linkX & 0xFF;
        session.followingLinkY = linkY & 0xFF;
        session.followingLinkZ = linkZ & 0xFF;
        session.followingEntityYOffset = entityYOffset & 0xFF;
        session.followingLinkDirection = linkDirection & 0xFF;
        if (!state.equals(session.followingNpcState)) {
            session.followingNpcRoomNeedsSync = true;
        }
        session.followingNpcState = state;
        session.synchronizeFollowingNpcEntitiesIfNeeded();
    }

    FollowingNpcState followingNpcState() {
        return session.followingNpcState;
    }

    int bowWowState() {
        return session.bowWowState & 0xFF;
    }

    void setBowWowState(int state) {
        if ((state & ~0xFF) != 0) {
            throw new IllegalArgumentException("BowWow state must be an unsigned byte");
        }
        session.bowWowState = state;
        if (session.entityBridge.entityRuntime != null) {
            session.entityBridge.entityRuntime.setBowWowState(state);
        }
    }

    int richardSpokenFlag() {
        return session.richardSpokenFlag & 0xFF;
    }

    void setRichardSpokenFlag(int flag) {
        if ((flag & ~0xFF) != 0) {
            throw new IllegalArgumentException("Richard spoken flag must be an unsigned byte");
        }
        session.richardSpokenFlag = flag;
    }

    int tarinFlag() {
        return session.tarinFlag & 0xFF;
    }

    void setTarinFlag(int flag) {
        if ((flag & ~0xFF) != 0) {
            throw new IllegalArgumentException("Tarin flag must be an unsigned byte");
        }
        session.tarinFlag = flag;
    }

    void setChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel) {
        if ((shieldLevel & ~0xFF) != 0 || (swordLevel & ~0xFF) != 0
            || (powerBraceletLevel & ~0xFF) != 0) {
            throw new IllegalArgumentException("Player upgrade levels must be unsigned bytes");
        }
        session.playerShieldLevel = shieldLevel;
        if (session.entityBridge.entityRuntime != null) {
            session.entityBridge.entityRuntime.setChestPlayerLevels(shieldLevel, swordLevel, powerBraceletLevel);
            if (session.activeRoom != null) {
                session.activeRoom.replaceEntities(session.entityBridge.entityRuntime.snapshot());
            }
        }
    }

    void setEntityDefaultMusicTrack(int trackId) {
        if ((trackId & ~0xFF) != 0) {
            throw new IllegalArgumentException("Default music track must be an unsigned byte");
        }
        session.entityDefaultMusicTrack = trackId;
        session.updateWitchEnvironment();
    }

    void updateWitchEnvironment() {
        if (session.entityBridge.entityRuntime != null) {
            session.entityBridge.entityRuntime.setWitchEnvironment(session.indoorTorchPaletteEffect.effectAddress(),
                session.indoorTorchPaletteEffect.paletteDataFlags(), session.entityDefaultMusicTrack);
        }
    }

    boolean shouldGetLostInMysteriousWoods() {
        return session.shouldGetLostInMysteriousWoods;
    }

    void synchronizeFollowingNpcEntitiesIfNeeded() {
        if (!session.followingNpcRoomNeedsSync || session.activeRoom == null || session.entityBridge.entityRuntime == null
            || session.activeRoom.entities() == null) {
            return;
        }
        FollowingNpcRoomContext room = new FollowingNpcRoomContext(
            session.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD,
            session.activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL,
            session.activeRoom.mapId(),
            session.activeRoom.roomId());
        FollowingNpcEntitySpawner.Result result = session.followingNpcEntitySpawner.synchronize(
            session.activeRoom.entities(), room, session.followingNpcState,
            session.followingLinkX, session.followingLinkY, session.followingLinkZ,
            session.followingEntityYOffset, session.followingLinkDirection, session.followingLinkPositionHistory);
        session.followingNpcState = result.state();
        session.followingNpcRoomNeedsSync = false;
        session.activeRoom.replaceEntities(result.snapshot());
        session.entityBridge.entityRuntime = RoomEntityRuntime.from(
            result.snapshot(), session.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD,
            session.entityRandomByteSource, session.entitySpriteHandlerCatalog, session.enemyCombatTables,
            session.chestContentsTable, session.romTables, session.romData);
        session.entityBridge.entityRuntime.setColorShellWorld(session.colorShellWorld);
        session.entityBridge.entityRuntime.setFollowingNpcState(session.followingNpcState);
        session.entityBridge.entityRuntime.setEntityMapId(session.activeRoom.mapId());
        session.entityBridge.entityRuntime.setEntityRoomId(session.activeRoom.roomId());
        session.entityBridge.entityRuntime.setDungeonMinibossReady(session.activeRoom.mapId() >= 0
            && session.activeRoom.mapId() < session.roomState.dungeonProgressFlags.length
            && (session.roomState.dungeonProgressFlags[session.activeRoom.mapId()] & 0x01) != 0);
        session.entityBridge.entityRuntime.setOwlDialogResolver(session.owlEventDialogResolver::globalDialogId);
        session.entityBridge.entityRuntime.setOwlDefaultMusicResolver(session.owlEventDialogResolver::defaultMusicTrack);
        session.entityBridge.entityRuntime.setGroundInteraction(session::entityGroundInteraction);
        session.entityBridge.entityRuntime.setBackgroundInteraction(session.entityBackgroundInteraction);
        session.entityBridge.entityRuntime.setObjectQuery(session::entityObjectSample);
        session.entityBridge.entityRuntime.setObjectIntersectionQuery(session::entityObjectIntersectionSample);
        session.entityBridge.entityRuntime.setGroundInteractionSideScrolling(
            session.activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL);
        session.entityBridge.entityRuntime.setActionButtonsHeld(session.entityInputs.actionButtonAHeld, session.entityInputs.actionButtonBHeld);
        session.entityBridge.entityRuntime.setTalkState(
            session.entityInputs.entityInventoryAppearing, session.entityInputs.entityDialogCooldown, session.entityInputs.entityWindowY);
        session.entityBridge.entityRuntime.setJoypadHeld(session.entityInputs.joypadHeld);
        session.entityBridge.entityRuntime.setPressedButtonsMask(session.entityInputs.entityPressedButtonsMask);
        session.entityBridge.entityRuntime.setPowerBraceletButtonHeld(session.entityInputs.powerBraceletButtonHeld);
        session.entityBridge.entityRuntime.setBombButtonHeld(session.entityInputs.bombButtonHeld);
        session.entityBridge.entityRuntime.setLiftedLinkC13B(session.followingEntityYOffset);
        session.entityBridge.entityRuntime.setEntityRoomStatus(session.activeRoomStatusFlags());
        session.entityBridge.entityRuntime.setDungeonMinibossReady(session.activeRoom.mapId() >= 0
            && session.activeRoom.mapId() < session.roomState.dungeonProgressFlags.length
            && (session.roomState.dungeonProgressFlags[session.activeRoom.mapId()] & 0x01) != 0);
        session.entityBridge.entityRuntime.setTailKeyOwned(session.hasTailKey);
        session.entityBridge.entityRuntime.setSecretSeashellPegasusCollisionState(
            session.entityInputs.secretSeashellScreenShakeActive, session.entityInputs.secretSeashellPegasusCollisionActive,
            session.entityInputs.secretSeashellPegasusCollisionX, session.entityInputs.secretSeashellPegasusCollisionY);
        session.configureEnemyDropRuntime();
    }

    void markHeartContainerCollected() {
        if (session.activeRoom == null) {
            return;
        }
        byte[] activeStatus = session.indoorStatusTableForMap(session.activeRoom.mapId());
        activeStatus[session.activeRoom.roomId()] |= (byte) session.ROOM_STATUS_EVENT_2;
        if (session.activeRoom.mapId() == 0x06) {
            session.roomState.indoorBRoomStatus[0x2E] |= (byte) session.ROOM_STATUS_EVENT_2;
        } else if (session.activeRoom.mapId() == 0x03) {
            session.roomState.indoorARoomStatus[0x66] |= (byte) session.ROOM_STATUS_EVENT_2;
        }
    }

    void markActiveRoomCompleted() {
        if (session.activeRoom == null) {
            return;
        }
        byte[] status = session.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
            ? session.roomState.overworldRoomStatus : session.indoorStatusTableForMap(session.activeRoom.mapId());
        status[session.activeRoom.roomId()] |= (byte) session.ROOM_STATUS_EVENT_1;
    }
}
