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

/** RoomEntityRewards operations for the live room session. */
final class RoomEntityRewards {
    private final RoomSession session;
    RoomEntityRewards(RoomSession session) { this.session = session; }

    void harvestKeyQuicksandEvents() {
        if (session.entityBridge.entityRuntime == null
            || session.entityBridge.entityRuntime.consumePendingKeyQuicksandEvents().isEmpty()) {
            return;
        }
        session.roomState.overworldRoomStatus[0xCE] |= (byte) session.OW_ROOM_STATUS_FLAG_CHANGED;
        session.roomState.indoorARoomStatus[0xF8] |= (byte) 0x20;
    }

    void harvestKeyRewardEvents() {
        if (session.entityBridge.entityRuntime == null) {
            return;
        }
        List<KeyRewardEvent> rewards =
            session.entityBridge.entityRuntime.consumePendingKeyRewardEvents();
        for (KeyRewardEvent reward : rewards) {
            if (reward.itemType() >= ChestContentsTable.CHEST_MAP
                && reward.itemType() <= ChestContentsTable.CHEST_SMALL_KEY) {
                session.roomState.dungeonItemState.incrementCurrentFlag(reward.itemType());
            }
            session.markActiveRoomCompleted();
        }
        session.entityBridge.pendingKeyRewardEvents.addAll(rewards);
    }

    void harvestHeartContainerRewards() {
        if (session.entityBridge.entityRuntime == null) {
            return;
        }
        List<HeartContainerRewardEvent> rewards =
            session.entityBridge.entityRuntime.consumePendingHeartContainerRewards();
        if (rewards.isEmpty()) {
            return;
        }
        session.markHeartContainerCollected();
        session.entityBridge.pendingHeartContainerRewards.addAll(rewards);
    }

    void harvestSwordPickupRewards() {
        if (session.entityBridge.entityRuntime == null) {
            return;
        }
        List<SwordPickupRewardEvent> rewards =
            session.entityBridge.entityRuntime.consumePendingSwordPickupRewards();
        if (rewards.isEmpty()) {
            return;
        }
        session.markActiveRoomCompleted();
        session.entityBridge.pendingSwordPickupRewards.addAll(rewards);
    }

    void harvestToadstoolRewards() {
        if (session.entityBridge.entityRuntime != null) {
            session.entityBridge.pendingToadstoolRewards.addAll(session.entityBridge.entityRuntime.consumePendingToadstoolRewards());
        }
    }

    void harvestInstrumentRewards() {
        if (session.entityBridge.entityRuntime == null || session.activeRoom == null
            || session.entityBridge.entityRuntime.consumePendingInstrumentRewards().isEmpty()
            || session.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
            return;
        }
        session.indoorStatusTableForMap(session.activeRoom.mapId())[session.activeRoom.roomId()]
            |= (byte) session.ROOM_STATUS_EVENT_1;
        if (session.activeRoom.mapId() >= 0 && session.activeRoom.mapId() < session.roomState.dungeonProgressFlags.length) {
            session.roomState.dungeonProgressFlags[session.activeRoom.mapId()] |= 0x02;
        }
        session.entityBridge.entityRuntime.setEntityRoomStatus(session.activeRoomStatusFlags());
    }

    void harvestInstrumentCompletions() {
        if (session.entityBridge.entityRuntime == null) {
            return;
        }
        for (InstrumentCompletionEvent completion
            : session.entityBridge.entityRuntime.consumePendingInstrumentCompletions()) {
            session.pendingInstrumentTransition = true;
            if (completion.mapId() == 0x00) {
                session.bowWowState = 0x80;
            } else if (completion.mapId() == 0x01) {
                session.tarinFlag = 0x02;
            }
        }
    }

    void harvestRichardProgressEvents() {
        if (session.entityBridge.entityRuntime == null) {
            return;
        }
        List<RichardProgressEvent> events =
            session.entityBridge.entityRuntime.consumePendingRichardProgressEvents();
        for (RichardProgressEvent event : events) {
            if (event.kind() == RichardProgressEvent.Kind.START_QUEST) {
                session.roomState.indoorBRoomStatus[0xC7] |= (byte) session.ROOM_STATUS_EVENT_1;
                session.richardSpokenFlag = Math.max(session.richardSpokenFlag, 0x02);
            } else {
                session.entityInputs.entityGoldenLeavesCount = event.goldenLeavesCount();
            }
        }
        session.entityBridge.pendingRichardProgressEvents.addAll(events);
    }

    void harvestWitchEvents() {
        if (session.entityBridge.entityRuntime != null) {
            session.entityBridge.pendingWitchExchangeEvents.addAll(
                session.entityBridge.entityRuntime.consumePendingWitchExchangeEvents());
            session.entityBridge.pendingWitchRewardEvents.addAll(
                session.entityBridge.entityRuntime.consumePendingWitchRewardEvents());
        }
    }

    void harvestOwlEventCompletions() {
        if (session.entityBridge.entityRuntime == null || session.activeRoom == null) {
            return;
        }
        if (session.entityBridge.entityRuntime.consumePendingOwlEventCompletions().isEmpty()) {
            return;
        }
        session.roomState.overworldRoomStatus[session.activeRoom.roomId()] |= (byte) session.ROOM_STATUS_EVENT_2;
    }

    void harvestSlimeKeyRewardEvents() {
        if (session.entityBridge.entityRuntime == null) {
            return;
        }
        List<SlimeKeyRewardEvent> rewards =
            session.entityBridge.entityRuntime.consumePendingSlimeKeyRewardEvents();
        for (SlimeKeyRewardEvent reward : rewards) {
            session.entityInputs.entityGoldenLeavesCount = reward.goldenLeavesCount();
            session.markActiveRoomCompleted();
        }
        session.entityBridge.pendingSlimeKeyRewardEvents.addAll(rewards);
    }
}
