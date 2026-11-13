package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.ArrayList;
import java.util.List;
import linksawakening.world.entity.event.BombExplosionEvent;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
import linksawakening.world.entity.event.HeartPiecePickupUpdate;
import linksawakening.world.entity.event.HinoxLinkEffectRequest;
import linksawakening.world.entity.event.InstrumentCompletionEvent;
import linksawakening.world.entity.event.InstrumentRewardEvent;
import linksawakening.world.entity.event.KeyQuicksandEvent;
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
import linksawakening.world.entity.event.OwlEventCompletion;
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
import linksawakening.world.entity.event.WarpLinkStateRequest;
import linksawakening.world.entity.event.WitchExchangeEvent;
import linksawakening.world.entity.event.WitchRewardEvent;

/** Typed outbox consumption preserving pre-tick staging and immutable snapshots. */
final class EntityEffectConsumption {
    private final EntityEventChannels events;
    private final EntityPresentationState presentationState;
    private final EntityRoomVariables roomVariables;

    EntityEffectConsumption(
            EntityEventChannels events, EntityPresentationState presentationState, EntityRoomVariables roomVariables) {
        this.events = java.util.Objects.requireNonNull(events, "events");
        this.presentationState = java.util.Objects.requireNonNull(presentationState, "presentationState");
        this.roomVariables = java.util.Objects.requireNonNull(roomVariables, "roomVariables");
    }

    boolean consumePendingSwitchBlockAnimationRequest() {
        boolean pending = roomVariables.readPendingSwitchBlockAnimationRequest();
        roomVariables.writePendingSwitchBlockAnimationRequest(false);
        return pending;
    }

    int consumePendingClearedEntityMask() {
        int pending = roomVariables.readPendingClearedEntityMask();
        roomVariables.writePendingClearedEntityMask(0);
        return pending;
    }

    int consumePendingRoomStatusMask() {
        int pending = roomVariables.readPendingRoomStatusMask();
        roomVariables.writePendingRoomStatusMask(0);
        return pending;
    }

    List<EntityCombatEvent> consumePendingEntityEvents() {
        var first = this.events.pendingPreTickEntityEvents;
        var second = this.events.pendingEntityEvents;
        int size = first.size() + second.size();
        if (size == 0) return List.of();
        List<EntityCombatEvent> pending = new ArrayList<>(size);
        for (int i = 0; i < first.size(); i++) pending.add(first.get(i));
        for (int i = 0; i < second.size(); i++) pending.add(second.get(i));
        first.clear();
        second.clear();
        return List.copyOf(pending);
    }

    List<ThrownBackgroundCollisionEvent> consumePendingThrownBackgroundCollisions() {
        return events.outbox.drain(ThrownBackgroundCollisionEvent.class, "pendingThrownBackgroundCollisions");
    }

    List<DungeonWarpRequest> consumePendingDungeonWarpRequests() {
        return events.outbox.drain(DungeonWarpRequest.class, "pendingDungeonWarpRequests");
    }

    List<WarpLinkStateRequest> consumePendingWarpLinkStateRequests() {
        return events.outbox.drain(WarpLinkStateRequest.class, "pendingWarpLinkStateRequests");
    }

    List<LinkFinalPositionRequest> consumePendingLinkFinalPositionRequests() {
        return events.outbox.drain(LinkFinalPositionRequest.class, "pendingLinkFinalPositionRequests");
    }

    List<RoosterLinkStateRequest> consumePendingRoosterLinkStateRequests() {
        return events.outbox.drain(RoosterLinkStateRequest.class, "pendingRoosterLinkStateRequests");
    }

    List<LinkMotionBlockRequest> consumePendingLinkMotionBlockRequests() {
        return events.outbox.drain(LinkMotionBlockRequest.class, "pendingLinkMotionBlockRequests");
    }

    List<SideViewPlatformLinkRequest> consumePendingSideViewPlatformLinkRequests() {
        return events.outbox.drain(SideViewPlatformLinkRequest.class, "pendingSideViewPlatformLinkRequests");
    }

    List<SideViewPotLinkRequest> consumePendingSideViewPotLinkRequests() {
        return events.outbox.drain(SideViewPotLinkRequest.class, "pendingSideViewPotLinkRequests");
    }

    List<HinoxLinkEffectRequest> consumePendingHinoxLinkEffectRequests() {
        return events.outbox.drain(HinoxLinkEffectRequest.class, "pendingHinoxLinkEffectRequests");
    }

    List<LinkFallenPoseRequest> consumePendingLinkFallenPoseRequests() {
        return events.outbox.drain(LinkFallenPoseRequest.class, "pendingLinkFallenPoseRequests");
    }

    List<LinkFacingRequest> consumePendingLinkFacingRequests() {
        return events.outbox.drain(LinkFacingRequest.class, "pendingLinkFacingRequests");
    }

    List<LinkAttackClearRequest> consumePendingLinkAttackClearRequests() {
        return events.outbox.drain(LinkAttackClearRequest.class, "pendingLinkAttackClearRequests");
    }

    List<LinkAttackStepRequest> consumePendingLinkAttackStepRequests() {
        var first = this.events.pendingPreTickLinkAttackStepRequests;
        var second = this.events.pendingLinkAttackStepRequests;
        int size = first.size() + second.size();
        if (size == 0) return List.of();
        List<LinkAttackStepRequest> pending = new ArrayList<>(size);
        for (int i = 0; i < first.size(); i++) pending.add(first.get(i));
        for (int i = 0; i < second.size(); i++) pending.add(second.get(i));
        first.clear();
        second.clear();
        return List.copyOf(pending);
    }

    List<RoomStatusPersistenceRequest> consumePendingRoomStatusPersistenceRequests() {
        return events.outbox.drain(RoomStatusPersistenceRequest.class, "pendingRoomStatusPersistenceRequests");
    }

    List<LinkHeldItemPoseRequest> consumePendingLinkHeldItemPoseRequests() {
        return events.outbox.drain(LinkHeldItemPoseRequest.class, "pendingLinkHeldItemPoseRequests");
    }

    List<LinkSwordSpinPoseRequest> consumePendingLinkSwordSpinPoseRequests() {
        return events.outbox.drain(LinkSwordSpinPoseRequest.class, "pendingLinkSwordSpinPoseRequests");
    }

    List<LinkSwordFinalPoseRequest> consumePendingLinkSwordFinalPoseRequests() {
        return events.outbox.drain(LinkSwordFinalPoseRequest.class, "pendingLinkSwordFinalPoseRequests");
    }

    List<ScreenShakeRequest> consumePendingScreenShakeRequests() {
        return events.outbox.drain(ScreenShakeRequest.class, "pendingScreenShakeRequests");
    }

    List<ChestRewardEvent> consumePendingChestRewardEvents() {
        return events.outbox.drain(ChestRewardEvent.class, "pendingChestRewardEvents");
    }

    List<KeyRewardEvent> consumePendingKeyRewardEvents() {
        return events.outbox.drain(KeyRewardEvent.class, "pendingKeyRewardEvents");
    }

    List<SlimeKeyRewardEvent> consumePendingSlimeKeyRewardEvents() {
        return events.outbox.drain(SlimeKeyRewardEvent.class, "pendingSlimeKeyRewardEvents");
    }

    List<KeyQuicksandEvent> consumePendingKeyQuicksandEvents() {
        return events.outbox.drain(KeyQuicksandEvent.class, "pendingKeyQuicksandEvents");
    }

    int consumePendingMusicTrack() {
        int pending = presentationState.readPendingMusicTrack();
        presentationState.writePendingMusicTrack(-1);
        return pending;
    }

    int consumePendingMusicFadeOutCountdown() {
        int pending = presentationState.readPendingMusicFadeOutCountdown();
        presentationState.writePendingMusicFadeOutCountdown(0);
        return pending;
    }

    List<LikeLikeEvent> consumePendingLikeLikeEvents() {
        return events.outbox.drain(LikeLikeEvent.class, "pendingLikeLikeEvents");
    }

    List<DialogRequest> consumePendingDialogRequests() {
        return events.outbox.drain(DialogRequest.class, "pendingDialogRequests");
    }

    List<BombExplosionEvent> consumeBombExplosionEvents() {
        return events.outbox.drain(BombExplosionEvent.class, "pendingBombExplosionEvents");
    }

    List<HeartPiecePickupUpdate> consumeHeartPieceUpdates() {
        var result = List.copyOf(events.heartPieceUpdates);
        events.heartPieceUpdates.clear();
        return result;
    }

    List<HeartContainerRewardEvent> consumePendingHeartContainerRewards() {
        List<HeartContainerRewardEvent> rewards = List.copyOf(this.events.pendingHeartContainerRewards);
        this.events.pendingHeartContainerRewards.clear();
        return rewards;
    }

    List<MadamMeowMeowFullHealRequest> consumePendingMadamMeowMeowFullHealRequests() {
        List<MadamMeowMeowFullHealRequest> requests = List.copyOf(this.events.pendingMadamMeowMeowFullHealRequests);
        this.events.pendingMadamMeowMeowFullHealRequests.clear();
        return requests;
    }

    List<SwordPickupRewardEvent> consumePendingSwordPickupRewards() {
        List<SwordPickupRewardEvent> rewards = List.copyOf(this.events.pendingSwordPickupRewards);
        this.events.pendingSwordPickupRewards.clear();
        return rewards;
    }

    List<ToadstoolRewardEvent> consumePendingToadstoolRewards() {
        List<ToadstoolRewardEvent> rewards = List.copyOf(this.events.pendingToadstoolRewards);
        this.events.pendingToadstoolRewards.clear();
        return rewards;
    }

    List<InstrumentRewardEvent> consumePendingInstrumentRewards() {
        List<InstrumentRewardEvent> rewards = List.copyOf(this.events.pendingInstrumentRewards);
        this.events.pendingInstrumentRewards.clear();
        return rewards;
    }

    List<InstrumentCompletionEvent> consumePendingInstrumentCompletions() {
        List<InstrumentCompletionEvent> completions = List.copyOf(this.events.pendingInstrumentCompletions);
        this.events.pendingInstrumentCompletions.clear();
        return completions;
    }

    List<RichardProgressEvent> consumePendingRichardProgressEvents() {
        List<RichardProgressEvent> events = List.copyOf(this.events.pendingRichardProgressEvents);
        this.events.pendingRichardProgressEvents.clear();
        return events;
    }

    List<WitchExchangeEvent> consumePendingWitchExchangeEvents() {
        List<WitchExchangeEvent> events = List.copyOf(this.events.pendingWitchExchangeEvents);
        this.events.pendingWitchExchangeEvents.clear();
        return events;
    }

    List<WitchRewardEvent> consumePendingWitchRewardEvents() {
        List<WitchRewardEvent> events = List.copyOf(this.events.pendingWitchRewardEvents);
        this.events.pendingWitchRewardEvents.clear();
        return events;
    }

    List<OwlEventCompletion> consumePendingOwlEventCompletions() {
        List<OwlEventCompletion> completions = List.copyOf(this.events.pendingOwlEventCompletions);
        this.events.pendingOwlEventCompletions.clear();
        return completions;
    }
}
