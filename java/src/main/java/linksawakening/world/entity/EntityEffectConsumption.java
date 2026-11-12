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
    private final RoomEntityRuntime runtime;

    EntityEffectConsumption(RoomEntityRuntime runtime) {
        this.runtime = runtime;
    }

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
        List<EntityCombatEvent> pending = new ArrayList<>(runtime.events.pendingPreTickEntityEvents);
        pending.addAll(runtime.events.pendingEntityEvents);
        runtime.events.pendingPreTickEntityEvents.clear();
        runtime.events.pendingEntityEvents.clear();
        return List.copyOf(pending);
    }

    List<ThrownBackgroundCollisionEvent> consumePendingThrownBackgroundCollisions() {
        return runtime.events.outbox.drain(ThrownBackgroundCollisionEvent.class, "pendingThrownBackgroundCollisions");
    }

    List<DungeonWarpRequest> consumePendingDungeonWarpRequests() {
        return runtime.events.outbox.drain(DungeonWarpRequest.class, "pendingDungeonWarpRequests");
    }

    List<WarpLinkStateRequest> consumePendingWarpLinkStateRequests() {
        return runtime.events.outbox.drain(WarpLinkStateRequest.class, "pendingWarpLinkStateRequests");
    }

    List<LinkFinalPositionRequest> consumePendingLinkFinalPositionRequests() {
        return runtime.events.outbox.drain(LinkFinalPositionRequest.class, "pendingLinkFinalPositionRequests");
    }

    List<RoosterLinkStateRequest> consumePendingRoosterLinkStateRequests() {
        return runtime.events.outbox.drain(RoosterLinkStateRequest.class, "pendingRoosterLinkStateRequests");
    }

    List<LinkMotionBlockRequest> consumePendingLinkMotionBlockRequests() {
        return runtime.events.outbox.drain(LinkMotionBlockRequest.class, "pendingLinkMotionBlockRequests");
    }

    List<SideViewPlatformLinkRequest> consumePendingSideViewPlatformLinkRequests() {
        return runtime.events.outbox.drain(SideViewPlatformLinkRequest.class, "pendingSideViewPlatformLinkRequests");
    }

    List<SideViewPotLinkRequest> consumePendingSideViewPotLinkRequests() {
        return runtime.events.outbox.drain(SideViewPotLinkRequest.class, "pendingSideViewPotLinkRequests");
    }

    List<HinoxLinkEffectRequest> consumePendingHinoxLinkEffectRequests() {
        return runtime.events.outbox.drain(HinoxLinkEffectRequest.class, "pendingHinoxLinkEffectRequests");
    }

    List<LinkFallenPoseRequest> consumePendingLinkFallenPoseRequests() {
        return runtime.events.outbox.drain(LinkFallenPoseRequest.class, "pendingLinkFallenPoseRequests");
    }

    List<LinkFacingRequest> consumePendingLinkFacingRequests() {
        return runtime.events.outbox.drain(LinkFacingRequest.class, "pendingLinkFacingRequests");
    }

    List<LinkAttackClearRequest> consumePendingLinkAttackClearRequests() {
        return runtime.events.outbox.drain(LinkAttackClearRequest.class, "pendingLinkAttackClearRequests");
    }

    List<LinkAttackStepRequest> consumePendingLinkAttackStepRequests() {
        List<LinkAttackStepRequest> pending = new ArrayList<>(runtime.events.pendingPreTickLinkAttackStepRequests);
        pending.addAll(runtime.events.pendingLinkAttackStepRequests);
        runtime.events.pendingPreTickLinkAttackStepRequests.clear();
        runtime.events.pendingLinkAttackStepRequests.clear();
        return List.copyOf(pending);
    }

    List<RoomStatusPersistenceRequest> consumePendingRoomStatusPersistenceRequests() {
        return runtime.events.outbox.drain(RoomStatusPersistenceRequest.class, "pendingRoomStatusPersistenceRequests");
    }

    List<LinkHeldItemPoseRequest> consumePendingLinkHeldItemPoseRequests() {
        return runtime.events.outbox.drain(LinkHeldItemPoseRequest.class, "pendingLinkHeldItemPoseRequests");
    }

    List<LinkSwordSpinPoseRequest> consumePendingLinkSwordSpinPoseRequests() {
        return runtime.events.outbox.drain(LinkSwordSpinPoseRequest.class, "pendingLinkSwordSpinPoseRequests");
    }

    List<LinkSwordFinalPoseRequest> consumePendingLinkSwordFinalPoseRequests() {
        return runtime.events.outbox.drain(LinkSwordFinalPoseRequest.class, "pendingLinkSwordFinalPoseRequests");
    }

    List<ScreenShakeRequest> consumePendingScreenShakeRequests() {
        return runtime.events.outbox.drain(ScreenShakeRequest.class, "pendingScreenShakeRequests");
    }

    List<ChestRewardEvent> consumePendingChestRewardEvents() {
        return runtime.events.outbox.drain(ChestRewardEvent.class, "pendingChestRewardEvents");
    }

    List<KeyRewardEvent> consumePendingKeyRewardEvents() {
        return runtime.events.outbox.drain(KeyRewardEvent.class, "pendingKeyRewardEvents");
    }

    List<SlimeKeyRewardEvent> consumePendingSlimeKeyRewardEvents() {
        return runtime.events.outbox.drain(SlimeKeyRewardEvent.class, "pendingSlimeKeyRewardEvents");
    }

    List<KeyQuicksandEvent> consumePendingKeyQuicksandEvents() {
        return runtime.events.outbox.drain(KeyQuicksandEvent.class, "pendingKeyQuicksandEvents");
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
        return runtime.events.outbox.drain(LikeLikeEvent.class, "pendingLikeLikeEvents");
    }

    List<DialogRequest> consumePendingDialogRequests() {
        return runtime.events.outbox.drain(DialogRequest.class, "pendingDialogRequests");
    }

    List<BombExplosionEvent> consumeBombExplosionEvents() {
        return runtime.events.outbox.drain(BombExplosionEvent.class, "pendingBombExplosionEvents");
    }

    List<HeartPiecePickupUpdate> consumeHeartPieceUpdates() {
        var result = List.copyOf(runtime.events.heartPieceUpdates);
        runtime.events.heartPieceUpdates.clear();
        return result;
    }

    List<HeartContainerRewardEvent> consumePendingHeartContainerRewards() {
        List<HeartContainerRewardEvent> rewards = List.copyOf(runtime.events.pendingHeartContainerRewards);
        runtime.events.pendingHeartContainerRewards.clear();
        return rewards;
    }

    List<MadamMeowMeowFullHealRequest> consumePendingMadamMeowMeowFullHealRequests() {
        List<MadamMeowMeowFullHealRequest> requests = List.copyOf(runtime.events.pendingMadamMeowMeowFullHealRequests);
        runtime.events.pendingMadamMeowMeowFullHealRequests.clear();
        return requests;
    }

    List<SwordPickupRewardEvent> consumePendingSwordPickupRewards() {
        List<SwordPickupRewardEvent> rewards = List.copyOf(runtime.events.pendingSwordPickupRewards);
        runtime.events.pendingSwordPickupRewards.clear();
        return rewards;
    }

    List<ToadstoolRewardEvent> consumePendingToadstoolRewards() {
        List<ToadstoolRewardEvent> rewards = List.copyOf(runtime.events.pendingToadstoolRewards);
        runtime.events.pendingToadstoolRewards.clear();
        return rewards;
    }

    List<InstrumentRewardEvent> consumePendingInstrumentRewards() {
        List<InstrumentRewardEvent> rewards = List.copyOf(runtime.events.pendingInstrumentRewards);
        runtime.events.pendingInstrumentRewards.clear();
        return rewards;
    }

    List<InstrumentCompletionEvent> consumePendingInstrumentCompletions() {
        List<InstrumentCompletionEvent> completions = List.copyOf(runtime.events.pendingInstrumentCompletions);
        runtime.events.pendingInstrumentCompletions.clear();
        return completions;
    }

    List<RichardProgressEvent> consumePendingRichardProgressEvents() {
        List<RichardProgressEvent> events = List.copyOf(runtime.events.pendingRichardProgressEvents);
        runtime.events.pendingRichardProgressEvents.clear();
        return events;
    }

    List<WitchExchangeEvent> consumePendingWitchExchangeEvents() {
        List<WitchExchangeEvent> events = List.copyOf(runtime.events.pendingWitchExchangeEvents);
        runtime.events.pendingWitchExchangeEvents.clear();
        return events;
    }

    List<WitchRewardEvent> consumePendingWitchRewardEvents() {
        List<WitchRewardEvent> events = List.copyOf(runtime.events.pendingWitchRewardEvents);
        runtime.events.pendingWitchRewardEvents.clear();
        return events;
    }

    List<OwlEventCompletion> consumePendingOwlEventCompletions() {
        List<OwlEventCompletion> completions = List.copyOf(runtime.events.pendingOwlEventCompletions);
        runtime.events.pendingOwlEventCompletions.clear();
        return completions;
    }
}
