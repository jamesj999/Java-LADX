package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import java.util.ArrayList;
import java.util.List;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.event.BombExplosionEvent;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
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

final class RoomEntityOutbox {
    private final java.util.function.Supplier<RoomEntityRuntime> runtime;

    RoomEntityOutbox(java.util.function.Supplier<RoomEntityRuntime> runtime) {
        this.runtime = runtime;
    }

    final List<BombExplosionEvent> pendingBombExplosionEvents = new ArrayList<>();
    final List<EntityCombatEvent> pendingRoomEntityEvents = new ArrayList<>();
    final List<ChestRewardEvent> pendingChestRewardEvents = new ArrayList<>();
    final List<KeyRewardEvent> pendingKeyRewardEvents = new ArrayList<>();
    final List<SlimeKeyRewardEvent> pendingSlimeKeyRewardEvents = new ArrayList<>();
    final List<HeartContainerRewardEvent> pendingHeartContainerRewards = new ArrayList<>();
    final List<MadamMeowMeowFullHealRequest> pendingMadamMeowMeowFullHealRequests = new ArrayList<>();
    final List<RichardProgressEvent> pendingRichardProgressEvents = new ArrayList<>();
    final List<SwordPickupRewardEvent> pendingSwordPickupRewards = new ArrayList<>();
    final List<ToadstoolRewardEvent> pendingToadstoolRewards = new ArrayList<>();
    final List<WitchExchangeEvent> pendingWitchExchangeEvents = new ArrayList<>();
    final List<WitchRewardEvent> pendingWitchRewardEvents = new ArrayList<>();
    final List<DialogRequest> pendingRoomDialogRequests = new ArrayList<>();
    int pendingRoomMusicTrack = -1;

    List<EntityCombatEvent> consumeEntityEvents() {
        List<EntityCombatEvent> events = new ArrayList<>();
        if (runtime.get() != null) {
            events.addAll(runtime.get().consumePendingEntityEvents());
        }
        events.addAll(pendingRoomEntityEvents);
        pendingRoomEntityEvents.clear();
        return List.copyOf(events);
    }

    List<LinkFinalPositionRequest> consumeLinkFinalPositionRequests() {
        return runtime.get() == null ? List.of() : runtime.get().consumePendingLinkFinalPositionRequests();
    }

    List<RoosterLinkStateRequest> consumeRoosterLinkStateRequests() {
        return runtime.get() == null ? List.of() : runtime.get().consumePendingRoosterLinkStateRequests();
    }

    List<LinkMotionBlockRequest> consumeLinkMotionBlockRequests() {
        return runtime.get() == null ? List.of() : runtime.get().consumePendingLinkMotionBlockRequests();
    }

    List<SideViewPlatformLinkRequest> consumeSideViewPlatformLinkRequests() {
        return runtime.get() == null ? List.of() : runtime.get().consumePendingSideViewPlatformLinkRequests();
    }

    List<SideViewPotLinkRequest> consumeSideViewPotLinkRequests() {
        return runtime.get() == null ? List.of() : runtime.get().consumePendingSideViewPotLinkRequests();
    }

    List<HinoxLinkEffectRequest> consumeHinoxLinkEffectRequests() {
        return runtime.get() == null ? List.of() : runtime.get().consumePendingHinoxLinkEffectRequests();
    }

    List<LinkFallenPoseRequest> consumeLinkFallenPoseRequests() {
        return runtime.get() == null ? List.of() : runtime.get().consumePendingLinkFallenPoseRequests();
    }

    void setFreeMovementMode(boolean enabled) {
        if (runtime.get() != null) runtime.get().setFreeMovementMode(enabled);
    }

    List<LinkFacingRequest> consumeLinkFacingRequests() {
        return runtime.get() == null ? List.of() : runtime.get().consumePendingLinkFacingRequests();
    }

    List<LinkAttackClearRequest> consumeLinkAttackClearRequests() {
        return runtime.get() == null ? List.of() : runtime.get().consumePendingLinkAttackClearRequests();
    }

    List<LinkAttackStepRequest> consumeLinkAttackStepRequests() {
        return runtime.get() == null ? List.of() : runtime.get().consumePendingLinkAttackStepRequests();
    }

    List<LinkHeldItemPoseRequest> consumeLinkHeldItemPoseRequests() {
        return runtime.get() == null ? List.of() : runtime.get().consumePendingLinkHeldItemPoseRequests();
    }

    boolean gotItemPresentationActive() {
        return runtime.get() != null && runtime.get().inspection().witchGotItemPresentationActive();
    }

    boolean powerUpPickupSequenceActive() {
        return runtime.get() != null && runtime.get().inspection().powerUpPickupSequenceActive();
    }

    boolean swordPickupSequenceActive() {
        return runtime.get() != null && runtime.get().inspection().swordPickupSequenceActive();
    }

    List<LinkSwordSpinPoseRequest> consumeLinkSwordSpinPoseRequests() {
        return runtime.get() == null ? List.of() : runtime.get().consumePendingLinkSwordSpinPoseRequests();
    }

    List<LinkSwordFinalPoseRequest> consumeLinkSwordFinalPoseRequests() {
        return runtime.get() == null ? List.of() : runtime.get().consumePendingLinkSwordFinalPoseRequests();
    }

    List<ScreenShakeRequest> consumeScreenShakeRequests() {
        return runtime.get() == null ? List.of() : runtime.get().consumePendingScreenShakeRequests();
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
        List<SlimeKeyRewardEvent> rewards = List.copyOf(pendingSlimeKeyRewardEvents);
        pendingSlimeKeyRewardEvents.clear();
        return rewards;
    }

    List<HeartContainerRewardEvent> consumeHeartContainerRewards() {
        List<HeartContainerRewardEvent> rewards = List.copyOf(pendingHeartContainerRewards);
        pendingHeartContainerRewards.clear();
        return rewards;
    }

    List<MadamMeowMeowFullHealRequest> consumeMadamMeowMeowFullHealRequests() {
        List<MadamMeowMeowFullHealRequest> requests = List.copyOf(pendingMadamMeowMeowFullHealRequests);
        pendingMadamMeowMeowFullHealRequests.clear();
        return requests;
    }

    List<RichardProgressEvent> consumeRichardProgressEvents() {
        List<RichardProgressEvent> events = List.copyOf(pendingRichardProgressEvents);
        pendingRichardProgressEvents.clear();
        return events;
    }

    List<SwordPickupRewardEvent> consumeSwordPickupRewards() {
        List<SwordPickupRewardEvent> rewards = List.copyOf(pendingSwordPickupRewards);
        pendingSwordPickupRewards.clear();
        return rewards;
    }

    List<ToadstoolRewardEvent> consumeToadstoolRewards() {
        List<ToadstoolRewardEvent> rewards = List.copyOf(pendingToadstoolRewards);
        pendingToadstoolRewards.clear();
        return rewards;
    }

    List<WitchExchangeEvent> consumeWitchExchangeEvents() {
        List<WitchExchangeEvent> events = List.copyOf(pendingWitchExchangeEvents);
        pendingWitchExchangeEvents.clear();
        return events;
    }

    List<WitchRewardEvent> consumeWitchRewardEvents() {
        List<WitchRewardEvent> events = List.copyOf(pendingWitchRewardEvents);
        pendingWitchRewardEvents.clear();
        return events;
    }

    int consumePendingMusicTrack() {
        if (pendingRoomMusicTrack >= 0) {
            int pending = pendingRoomMusicTrack;
            pendingRoomMusicTrack = -1;
            return pending;
        }
        return runtime.get() == null ? -1 : runtime.get().consumePendingMusicTrack();
    }

    int consumePendingMusicFadeOutCountdown() {
        return runtime.get() == null ? 0 : runtime.get().consumePendingMusicFadeOutCountdown();
    }

    List<LikeLikeEvent> consumeLikeLikeEvents() {
        if (runtime.get() == null) {
            return List.of();
        }
        return runtime.get().consumePendingLikeLikeEvents();
    }

    List<DialogRequest> consumeEntityDialogRequests() {
        List<DialogRequest> requests = new ArrayList<>(pendingRoomDialogRequests);
        pendingRoomDialogRequests.clear();
        if (runtime.get() != null) {
            requests.addAll(runtime.get().consumePendingDialogRequests());
        }
        return List.copyOf(requests);
    }

    List<BombExplosionEvent> consumeBombExplosionEvents() {
        List<BombExplosionEvent> pending = List.copyOf(pendingBombExplosionEvents);
        pendingBombExplosionEvents.clear();
        return pending;
    }
}
