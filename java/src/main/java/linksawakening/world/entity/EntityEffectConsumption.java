package linksawakening.world.entity;

import linksawakening.world.entity.event.BombExplosionEvent;
import linksawakening.world.entity.event.EntityCombatEvent;

import linksawakening.world.entity.event.HeartPiecePickupUpdate;

import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
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

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;
import java.util.ArrayList;
import java.util.List;
import static linksawakening.world.entity.RoomEntityRuntime.*;

/** Typed outbox consumption preserving pre-tick staging and immutable snapshots. */
final class EntityEffectConsumption {
    private final RoomEntityRuntime runtime;
    EntityEffectConsumption(RoomEntityRuntime runtime) { this.runtime = runtime; }

    boolean consumePendingSwitchBlockAnimationRequest() {
        boolean pending = runtime.pendingSwitchBlockAnimationRequest;
        runtime.pendingSwitchBlockAnimationRequest = false;
        return pending;
    }

    int consumePendingClearedEntityMask() {
        int pending = runtime.pendingClearedEntityMask;
        runtime.pendingClearedEntityMask = 0;
        return pending;
    }

    int consumePendingRoomStatusMask() {
        int pending = runtime.pendingRoomStatusMask;
        runtime.pendingRoomStatusMask = 0;
        return pending;
    }

    List<EntityCombatEvent> consumePendingEntityEvents() {
        List<EntityCombatEvent> pending = new ArrayList<>(runtime.pendingPreTickEntityEvents);
        pending.addAll(runtime.pendingEntityEvents);
        runtime.pendingPreTickEntityEvents.clear();
        runtime.pendingEntityEvents.clear();
        return List.copyOf(pending);
    }

    List<ThrownBackgroundCollisionEvent> consumePendingThrownBackgroundCollisions() {
        return runtime.outbox.drain(ThrownBackgroundCollisionEvent.class, "pendingThrownBackgroundCollisions");
    }

    List<DungeonWarpRequest> consumePendingDungeonWarpRequests() {
        return runtime.outbox.drain(DungeonWarpRequest.class, "pendingDungeonWarpRequests");
    }

    List<WarpLinkStateRequest> consumePendingWarpLinkStateRequests() {
        return runtime.outbox.drain(WarpLinkStateRequest.class, "pendingWarpLinkStateRequests");
    }

    List<LinkFinalPositionRequest> consumePendingLinkFinalPositionRequests() {
        return runtime.outbox.drain(LinkFinalPositionRequest.class, "pendingLinkFinalPositionRequests");
    }

    List<RoosterLinkStateRequest> consumePendingRoosterLinkStateRequests() {
        return runtime.outbox.drain(RoosterLinkStateRequest.class, "pendingRoosterLinkStateRequests");
    }

    List<LinkMotionBlockRequest> consumePendingLinkMotionBlockRequests() {
        return runtime.outbox.drain(LinkMotionBlockRequest.class, "pendingLinkMotionBlockRequests");
    }

    List<SideViewPlatformLinkRequest> consumePendingSideViewPlatformLinkRequests() {
        return runtime.outbox.drain(SideViewPlatformLinkRequest.class, "pendingSideViewPlatformLinkRequests");
    }

    List<SideViewPotLinkRequest> consumePendingSideViewPotLinkRequests() {
        return runtime.outbox.drain(SideViewPotLinkRequest.class, "pendingSideViewPotLinkRequests");
    }

    List<HinoxLinkEffectRequest> consumePendingHinoxLinkEffectRequests() {
        return runtime.outbox.drain(HinoxLinkEffectRequest.class, "pendingHinoxLinkEffectRequests");
    }

    List<LinkFallenPoseRequest> consumePendingLinkFallenPoseRequests() {
        return runtime.outbox.drain(LinkFallenPoseRequest.class, "pendingLinkFallenPoseRequests");
    }

    List<LinkFacingRequest> consumePendingLinkFacingRequests() {
        return runtime.outbox.drain(LinkFacingRequest.class, "pendingLinkFacingRequests");
    }

    List<LinkAttackClearRequest> consumePendingLinkAttackClearRequests() {
        return runtime.outbox.drain(LinkAttackClearRequest.class, "pendingLinkAttackClearRequests");
    }

    List<LinkAttackStepRequest> consumePendingLinkAttackStepRequests() {
        List<LinkAttackStepRequest> pending = new ArrayList<>(
            runtime.pendingPreTickLinkAttackStepRequests);
        pending.addAll(runtime.pendingLinkAttackStepRequests);
        runtime.pendingPreTickLinkAttackStepRequests.clear();
        runtime.pendingLinkAttackStepRequests.clear();
        return List.copyOf(pending);
    }

    List<RoomStatusPersistenceRequest> consumePendingRoomStatusPersistenceRequests() {
        return runtime.outbox.drain(RoomStatusPersistenceRequest.class, "pendingRoomStatusPersistenceRequests");
    }

    List<LinkHeldItemPoseRequest> consumePendingLinkHeldItemPoseRequests() {
        return runtime.outbox.drain(LinkHeldItemPoseRequest.class, "pendingLinkHeldItemPoseRequests");
    }

    List<LinkSwordSpinPoseRequest> consumePendingLinkSwordSpinPoseRequests() {
        return runtime.outbox.drain(LinkSwordSpinPoseRequest.class, "pendingLinkSwordSpinPoseRequests");
    }

    List<LinkSwordFinalPoseRequest> consumePendingLinkSwordFinalPoseRequests() {
        return runtime.outbox.drain(LinkSwordFinalPoseRequest.class, "pendingLinkSwordFinalPoseRequests");
    }

    List<ScreenShakeRequest> consumePendingScreenShakeRequests() {
        return runtime.outbox.drain(ScreenShakeRequest.class, "pendingScreenShakeRequests");
    }

    List<ChestRewardEvent> consumePendingChestRewardEvents() {
        return runtime.outbox.drain(ChestRewardEvent.class, "pendingChestRewardEvents");
    }

    List<KeyRewardEvent> consumePendingKeyRewardEvents() {
        return runtime.outbox.drain(KeyRewardEvent.class, "pendingKeyRewardEvents");
    }

    List<SlimeKeyRewardEvent> consumePendingSlimeKeyRewardEvents() {
        return runtime.outbox.drain(SlimeKeyRewardEvent.class, "pendingSlimeKeyRewardEvents");
    }

    List<KeyQuicksandEvent> consumePendingKeyQuicksandEvents() {
        return runtime.outbox.drain(KeyQuicksandEvent.class, "pendingKeyQuicksandEvents");
    }

    int consumePendingMusicTrack() {
        int pending = runtime.pendingMusicTrack;
        runtime.pendingMusicTrack = -1;
        return pending;
    }

    int consumePendingMusicFadeOutCountdown() {
        int pending = runtime.pendingMusicFadeOutCountdown;
        runtime.pendingMusicFadeOutCountdown = 0;
        return pending;
    }

    List<LikeLikeEvent> consumePendingLikeLikeEvents() {
        return runtime.outbox.drain(LikeLikeEvent.class, "pendingLikeLikeEvents");
    }

    List<DialogRequest> consumePendingDialogRequests() {
        return runtime.outbox.drain(DialogRequest.class, "pendingDialogRequests");
    }

    List<BombExplosionEvent> consumeBombExplosionEvents() {
        return runtime.outbox.drain(BombExplosionEvent.class, "pendingBombExplosionEvents");
    }

    List<HeartPiecePickupUpdate> consumeHeartPieceUpdates() {
        var result = List.copyOf(runtime.heartPieceUpdates);
        runtime.heartPieceUpdates.clear();
        return result;
    }

    List<HeartContainerRewardEvent> consumePendingHeartContainerRewards() {
        List<HeartContainerRewardEvent> rewards = List.copyOf(runtime.pendingHeartContainerRewards);
        runtime.pendingHeartContainerRewards.clear();
        return rewards;
    }

    List<MadamMeowMeowFullHealRequest> consumePendingMadamMeowMeowFullHealRequests() {
        List<MadamMeowMeowFullHealRequest> requests =
            List.copyOf(runtime.pendingMadamMeowMeowFullHealRequests);
        runtime.pendingMadamMeowMeowFullHealRequests.clear();
        return requests;
    }

    List<SwordPickupRewardEvent> consumePendingSwordPickupRewards() {
        List<SwordPickupRewardEvent> rewards = List.copyOf(runtime.pendingSwordPickupRewards);
        runtime.pendingSwordPickupRewards.clear();
        return rewards;
    }

    List<ToadstoolRewardEvent> consumePendingToadstoolRewards() {
        List<ToadstoolRewardEvent> rewards = List.copyOf(runtime.pendingToadstoolRewards);
        runtime.pendingToadstoolRewards.clear();
        return rewards;
    }

    List<InstrumentRewardEvent> consumePendingInstrumentRewards() {
        List<InstrumentRewardEvent> rewards = List.copyOf(runtime.pendingInstrumentRewards);
        runtime.pendingInstrumentRewards.clear();
        return rewards;
    }

    List<InstrumentCompletionEvent> consumePendingInstrumentCompletions() {
        List<InstrumentCompletionEvent> completions =
            List.copyOf(runtime.pendingInstrumentCompletions);
        runtime.pendingInstrumentCompletions.clear();
        return completions;
    }

    List<RichardProgressEvent> consumePendingRichardProgressEvents() {
        List<RichardProgressEvent> events = List.copyOf(runtime.pendingRichardProgressEvents);
        runtime.pendingRichardProgressEvents.clear();
        return events;
    }

    List<WitchExchangeEvent> consumePendingWitchExchangeEvents() {
        List<WitchExchangeEvent> events = List.copyOf(runtime.pendingWitchExchangeEvents);
        runtime.pendingWitchExchangeEvents.clear();
        return events;
    }

    List<WitchRewardEvent> consumePendingWitchRewardEvents() {
        List<WitchRewardEvent> events = List.copyOf(runtime.pendingWitchRewardEvents);
        runtime.pendingWitchRewardEvents.clear();
        return events;
    }

    List<OwlEventCompletion> consumePendingOwlEventCompletions() {
        List<OwlEventCompletion> completions = List.copyOf(runtime.pendingOwlEventCompletions);
        runtime.pendingOwlEventCompletions.clear();
        return completions;
    }
}
