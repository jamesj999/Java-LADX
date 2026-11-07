package linksawakening.world;

import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
import linksawakening.world.entity.event.HinoxLinkEffectRequest;
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
import linksawakening.world.entity.event.RichardProgressEvent;
import linksawakening.world.entity.event.RoosterLinkStateRequest;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.entity.event.SideViewPlatformLinkRequest;
import linksawakening.world.entity.event.SideViewPotLinkRequest;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;
import linksawakening.world.entity.event.SwordPickupRewardEvent;
import linksawakening.world.entity.event.ToadstoolRewardEvent;
import linksawakening.world.entity.event.WitchExchangeEvent;
import linksawakening.world.entity.event.WitchRewardEvent;

import java.util.ArrayList;
import java.util.List;

/** Owns room-level entity output queues and their consumption boundary. */
final class RoomEntityBridge {
    final List<BombExplosionEvent> pendingBombExplosionEvents = new ArrayList<>();
    final List<EntityCombatEvent> pendingRoomEntityEvents = new ArrayList<>();
    final List<ChestRewardEvent> pendingChestRewardEvents =
        new ArrayList<>();
    final List<KeyRewardEvent> pendingKeyRewardEvents =
        new ArrayList<>();
    final List<SlimeKeyRewardEvent> pendingSlimeKeyRewardEvents =
        new ArrayList<>();
    final List<HeartContainerRewardEvent>
        pendingHeartContainerRewards = new ArrayList<>();
    final List<MadamMeowMeowFullHealRequest>
        pendingMadamMeowMeowFullHealRequests = new ArrayList<>();
    final List<RichardProgressEvent>
        pendingRichardProgressEvents = new ArrayList<>();
    final List<SwordPickupRewardEvent>
        pendingSwordPickupRewards = new ArrayList<>();
    final List<ToadstoolRewardEvent>
        pendingToadstoolRewards = new ArrayList<>();
    final List<WitchExchangeEvent>
        pendingWitchExchangeEvents = new ArrayList<>();
    final List<WitchRewardEvent>
        pendingWitchRewardEvents = new ArrayList<>();
    final List<DialogRequest> pendingRoomDialogRequests =
        new ArrayList<>();
    RoomEntityRuntime entityRuntime;
    int pendingRoomMusicTrack = -1;

    List<EntityCombatEvent> consumeEntityEvents() {
        List<EntityCombatEvent> events = new ArrayList<>();
        if (entityRuntime != null) {
            events.addAll(entityRuntime.consumePendingEntityEvents());
        }
        events.addAll(pendingRoomEntityEvents);
        pendingRoomEntityEvents.clear();
        return List.copyOf(events);
    }

    List<LinkFinalPositionRequest>
            consumeLinkFinalPositionRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingLinkFinalPositionRequests();
    }

    List<RoosterLinkStateRequest>
            consumeRoosterLinkStateRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingRoosterLinkStateRequests();
    }

    List<LinkMotionBlockRequest>
            consumeLinkMotionBlockRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingLinkMotionBlockRequests();
    }

    List<SideViewPlatformLinkRequest>
            consumeSideViewPlatformLinkRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingSideViewPlatformLinkRequests();
    }

    List<SideViewPotLinkRequest>
            consumeSideViewPotLinkRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingSideViewPotLinkRequests();
    }

    List<HinoxLinkEffectRequest>
            consumeHinoxLinkEffectRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingHinoxLinkEffectRequests();
    }

    List<LinkFallenPoseRequest>
            consumeLinkFallenPoseRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingLinkFallenPoseRequests();
    }

    void setFreeMovementMode(boolean enabled) {
        if (entityRuntime != null) entityRuntime.setFreeMovementMode(enabled);
    }

    List<LinkFacingRequest> consumeLinkFacingRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingLinkFacingRequests();
    }

    List<LinkAttackClearRequest>
            consumeLinkAttackClearRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingLinkAttackClearRequests();
    }

    List<LinkAttackStepRequest>
            consumeLinkAttackStepRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingLinkAttackStepRequests();
    }

    List<LinkHeldItemPoseRequest>
            consumeLinkHeldItemPoseRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingLinkHeldItemPoseRequests();
    }

    boolean gotItemPresentationActive() {
        return entityRuntime != null && entityRuntime.witchGotItemPresentationActive();
    }

    boolean powerUpPickupSequenceActive() {
        return entityRuntime != null && entityRuntime.powerUpPickupSequenceActive();
    }

    boolean swordPickupSequenceActive() {
        return entityRuntime != null && entityRuntime.swordPickupSequenceActive();
    }

    List<LinkSwordSpinPoseRequest>
            consumeLinkSwordSpinPoseRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingLinkSwordSpinPoseRequests();
    }

    List<LinkSwordFinalPoseRequest>
            consumeLinkSwordFinalPoseRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingLinkSwordFinalPoseRequests();
    }

    List<ScreenShakeRequest> consumeScreenShakeRequests() {
        return entityRuntime == null
            ? List.of() : entityRuntime.consumePendingScreenShakeRequests();
    }

    List<ChestRewardEvent> consumeChestRewardEvents() {
        List<ChestRewardEvent> rewards = List.copyOf(pendingChestRewardEvents);
        pendingChestRewardEvents.clear();
        return rewards;
    }

    List<KeyRewardEvent> consumeKeyRewardEvents() {
        List<KeyRewardEvent> rewards = List.copyOf(pendingKeyRewardEvents);
        pendingKeyRewardEvents.clear();
        return rewards;
    }

    List<SlimeKeyRewardEvent> consumeSlimeKeyRewardEvents() {
        List<SlimeKeyRewardEvent> rewards =
            List.copyOf(pendingSlimeKeyRewardEvents);
        pendingSlimeKeyRewardEvents.clear();
        return rewards;
    }

    List<HeartContainerRewardEvent>
            consumeHeartContainerRewards() {
        List<HeartContainerRewardEvent> rewards =
            List.copyOf(pendingHeartContainerRewards);
        pendingHeartContainerRewards.clear();
        return rewards;
    }

    List<MadamMeowMeowFullHealRequest>
            consumeMadamMeowMeowFullHealRequests() {
        List<MadamMeowMeowFullHealRequest> requests =
            List.copyOf(pendingMadamMeowMeowFullHealRequests);
        pendingMadamMeowMeowFullHealRequests.clear();
        return requests;
    }

    List<RichardProgressEvent> consumeRichardProgressEvents() {
        List<RichardProgressEvent> events =
            List.copyOf(pendingRichardProgressEvents);
        pendingRichardProgressEvents.clear();
        return events;
    }

    List<SwordPickupRewardEvent> consumeSwordPickupRewards() {
        List<SwordPickupRewardEvent> rewards =
            List.copyOf(pendingSwordPickupRewards);
        pendingSwordPickupRewards.clear();
        return rewards;
    }

    List<ToadstoolRewardEvent> consumeToadstoolRewards() {
        List<ToadstoolRewardEvent> rewards =
            List.copyOf(pendingToadstoolRewards);
        pendingToadstoolRewards.clear();
        return rewards;
    }

    List<WitchExchangeEvent> consumeWitchExchangeEvents() {
        List<WitchExchangeEvent> events =
            List.copyOf(pendingWitchExchangeEvents);
        pendingWitchExchangeEvents.clear();
        return events;
    }

    List<WitchRewardEvent> consumeWitchRewardEvents() {
        List<WitchRewardEvent> events =
            List.copyOf(pendingWitchRewardEvents);
        pendingWitchRewardEvents.clear();
        return events;
    }

    int consumePendingMusicTrack() {
        if (pendingRoomMusicTrack >= 0) {
            int pending = pendingRoomMusicTrack;
            pendingRoomMusicTrack = -1;
            return pending;
        }
        return entityRuntime == null ? -1 : entityRuntime.consumePendingMusicTrack();
    }

    int consumePendingMusicFadeOutCountdown() {
        return entityRuntime == null ? 0
            : entityRuntime.consumePendingMusicFadeOutCountdown();
    }

    List<LikeLikeEvent> consumeLikeLikeEvents() {
        if (entityRuntime == null) {
            return List.of();
        }
        return entityRuntime.consumePendingLikeLikeEvents();
    }

    List<DialogRequest> consumeEntityDialogRequests() {
        List<DialogRequest> requests = new ArrayList<>(
            pendingRoomDialogRequests);
        pendingRoomDialogRequests.clear();
        if (entityRuntime != null) {
            requests.addAll(entityRuntime.consumePendingDialogRequests());
        }
        return List.copyOf(requests);
    }

    List<BombExplosionEvent> consumeBombExplosionEvents() {
        List<BombExplosionEvent> pending = List.copyOf(pendingBombExplosionEvents);
        pendingBombExplosionEvents.clear();
        return pending;
    }
}
