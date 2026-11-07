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
        if (session.context.entityBridge.entityRuntime == null
            || session.context.entityBridge.entityRuntime.consumePendingKeyQuicksandEvents().isEmpty()) {
            return;
        }
        session.context.roomState.overworldRoomStatus[0xCE] |= (byte) session.OW_ROOM_STATUS_FLAG_CHANGED;
        session.context.roomState.indoorARoomStatus[0xF8] |= (byte) 0x20;
    }

    void harvestKeyRewardEvents() {
        if (session.context.entityBridge.entityRuntime == null) {
            return;
        }
        List<KeyRewardEvent> rewards =
            session.context.entityBridge.entityRuntime.consumePendingKeyRewardEvents();
        for (KeyRewardEvent reward : rewards) {
            if (reward.itemType() >= ChestContentsTable.CHEST_MAP
                && reward.itemType() <= ChestContentsTable.CHEST_SMALL_KEY) {
                session.context.roomState.dungeonItemState.incrementCurrentFlag(reward.itemType());
            }
            session.markActiveRoomCompleted();
        }
        session.context.entityBridge.pendingKeyRewardEvents.addAll(rewards);
    }

    void harvestHeartContainerRewards() {
        if (session.context.entityBridge.entityRuntime == null) {
            return;
        }
        List<HeartContainerRewardEvent> rewards =
            session.context.entityBridge.entityRuntime.consumePendingHeartContainerRewards();
        if (rewards.isEmpty()) {
            return;
        }
        session.markHeartContainerCollected();
        session.context.entityBridge.pendingHeartContainerRewards.addAll(rewards);
    }

    void harvestSwordPickupRewards() {
        if (session.context.entityBridge.entityRuntime == null) {
            return;
        }
        List<SwordPickupRewardEvent> rewards =
            session.context.entityBridge.entityRuntime.consumePendingSwordPickupRewards();
        if (rewards.isEmpty()) {
            return;
        }
        session.markActiveRoomCompleted();
        session.context.entityBridge.pendingSwordPickupRewards.addAll(rewards);
    }

    void harvestToadstoolRewards() {
        if (session.context.entityBridge.entityRuntime != null) {
            session.context.entityBridge.pendingToadstoolRewards.addAll(session.context.entityBridge.entityRuntime.consumePendingToadstoolRewards());
        }
    }

    void harvestInstrumentRewards() {
        if (session.context.entityBridge.entityRuntime == null || session.context.activeRoom == null
            || session.context.entityBridge.entityRuntime.consumePendingInstrumentRewards().isEmpty()
            || session.context.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
            return;
        }
        session.indoorStatusTableForMap(session.context.activeRoom.mapId())[session.context.activeRoom.roomId()]
            |= (byte) session.ROOM_STATUS_EVENT_1;
        if (session.context.activeRoom.mapId() >= 0 && session.context.activeRoom.mapId() < session.context.roomState.dungeonProgressFlags.length) {
            session.context.roomState.dungeonProgressFlags[session.context.activeRoom.mapId()] |= 0x02;
        }
        session.context.entityBridge.entityRuntime.setEntityRoomStatus(session.activeRoomStatusFlags());
    }

    void harvestInstrumentCompletions() {
        if (session.context.entityBridge.entityRuntime == null) {
            return;
        }
        for (InstrumentCompletionEvent completion
            : session.context.entityBridge.entityRuntime.consumePendingInstrumentCompletions()) {
            session.context.pendingInstrumentTransition = true;
            if (completion.mapId() == 0x00) {
                session.context.bowWowState = 0x80;
            } else if (completion.mapId() == 0x01) {
                session.context.tarinFlag = 0x02;
            }
        }
    }

    void harvestRichardProgressEvents() {
        if (session.context.entityBridge.entityRuntime == null) {
            return;
        }
        List<RichardProgressEvent> events =
            session.context.entityBridge.entityRuntime.consumePendingRichardProgressEvents();
        for (RichardProgressEvent event : events) {
            if (event.kind() == RichardProgressEvent.Kind.START_QUEST) {
                session.context.roomState.indoorBRoomStatus[0xC7] |= (byte) session.ROOM_STATUS_EVENT_1;
                session.context.richardSpokenFlag = Math.max(session.context.richardSpokenFlag, 0x02);
            } else {
                session.context.entityInputs.entityGoldenLeavesCount = event.goldenLeavesCount();
            }
        }
        session.context.entityBridge.pendingRichardProgressEvents.addAll(events);
    }

    void harvestWitchEvents() {
        if (session.context.entityBridge.entityRuntime != null) {
            session.context.entityBridge.pendingWitchExchangeEvents.addAll(
                session.context.entityBridge.entityRuntime.consumePendingWitchExchangeEvents());
            session.context.entityBridge.pendingWitchRewardEvents.addAll(
                session.context.entityBridge.entityRuntime.consumePendingWitchRewardEvents());
        }
    }

    void harvestOwlEventCompletions() {
        if (session.context.entityBridge.entityRuntime == null || session.context.activeRoom == null) {
            return;
        }
        if (session.context.entityBridge.entityRuntime.consumePendingOwlEventCompletions().isEmpty()) {
            return;
        }
        session.context.roomState.overworldRoomStatus[session.context.activeRoom.roomId()] |= (byte) session.ROOM_STATUS_EVENT_2;
    }

    void harvestSlimeKeyRewardEvents() {
        if (session.context.entityBridge.entityRuntime == null) {
            return;
        }
        List<SlimeKeyRewardEvent> rewards =
            session.context.entityBridge.entityRuntime.consumePendingSlimeKeyRewardEvents();
        for (SlimeKeyRewardEvent reward : rewards) {
            session.context.entityInputs.entityGoldenLeavesCount = reward.goldenLeavesCount();
            session.markActiveRoomCompleted();
        }
        session.context.entityBridge.pendingSlimeKeyRewardEvents.addAll(rewards);
    }
}
