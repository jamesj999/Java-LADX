package linksawakening.world.entity;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import linksawakening.world.entity.event.BombExplosionEvent;
import linksawakening.world.entity.event.BoomerangObjectRequest;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
import linksawakening.world.entity.event.HeartPiecePickupUpdate;
import linksawakening.world.entity.event.HinoxLinkEffectRequest;
import linksawakening.world.entity.event.HookshotBridgeUpdate;
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
import linksawakening.world.entity.event.MagicPowderObjectRequest;
import linksawakening.world.entity.event.MagicRodObjectRequest;
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
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.event.WarpLinkStateRequest;
import linksawakening.world.entity.event.WitchExchangeEvent;
import linksawakening.world.entity.event.WitchRewardEvent;
import linksawakening.world.entity.motion.RoamingEnemyMotion;

/** One entity outbox and its stable typed channel views. */
final class EntityEventChannels {
    final EntityOutbox outbox = new EntityOutbox();
    private final List<RoamingEnemyMotion.LaunchRequest> projectileLaunchRequests =
            outbox.channel(RoamingEnemyMotion.LaunchRequest.class, "projectileLaunchRequests");
    private final List<TransientVfxRequest> transientVfxRequests =
            outbox.channel(TransientVfxRequest.class, "transientVfxRequests");
    private final List<HeartContainerRewardEvent> pendingHeartContainerRewards =
            outbox.channel(HeartContainerRewardEvent.class, "pendingHeartContainerRewards");
    private final List<MadamMeowMeowFullHealRequest> pendingMadamMeowMeowFullHealRequests =
            outbox.channel(MadamMeowMeowFullHealRequest.class, "pendingMadamMeowMeowFullHealRequests");
    private final List<SwordPickupRewardEvent> pendingSwordPickupRewards =
            outbox.channel(SwordPickupRewardEvent.class, "pendingSwordPickupRewards");
    private final List<ToadstoolRewardEvent> pendingToadstoolRewards =
            outbox.channel(ToadstoolRewardEvent.class, "pendingToadstoolRewards");
    private final List<InstrumentRewardEvent> pendingInstrumentRewards =
            outbox.channel(InstrumentRewardEvent.class, "pendingInstrumentRewards");
    private final List<InstrumentCompletionEvent> pendingInstrumentCompletions =
            outbox.channel(InstrumentCompletionEvent.class, "pendingInstrumentCompletions");
    private final List<RichardProgressEvent> pendingRichardProgressEvents =
            outbox.channel(RichardProgressEvent.class, "pendingRichardProgressEvents");
    private final List<OwlEventCompletion> pendingOwlEventCompletions =
            outbox.channel(OwlEventCompletion.class, "pendingOwlEventCompletions");
    private final List<LinkFinalPositionRequest> pendingLinkFinalPositionRequests =
            outbox.channel(LinkFinalPositionRequest.class, "pendingLinkFinalPositionRequests");
    private final List<RoosterLinkStateRequest> pendingRoosterLinkStateRequests =
            outbox.channel(RoosterLinkStateRequest.class, "pendingRoosterLinkStateRequests");
    private final List<SideViewPlatformLinkRequest> pendingSideViewPlatformLinkRequests =
            outbox.channel(SideViewPlatformLinkRequest.class, "pendingSideViewPlatformLinkRequests");
    private final List<SideViewPotLinkRequest> pendingSideViewPotLinkRequests =
            outbox.channel(SideViewPotLinkRequest.class, "pendingSideViewPotLinkRequests");
    private final List<LinkMotionBlockRequest> pendingLinkMotionBlockRequests =
            outbox.channel(LinkMotionBlockRequest.class, "pendingLinkMotionBlockRequests");
    private final List<HinoxLinkEffectRequest> pendingHinoxLinkEffectRequests =
            outbox.channel(HinoxLinkEffectRequest.class, "pendingHinoxLinkEffectRequests");
    private final List<LinkFallenPoseRequest> pendingLinkFallenPoseRequests =
            outbox.channel(LinkFallenPoseRequest.class, "pendingLinkFallenPoseRequests");
    private final List<LinkFacingRequest> pendingLinkFacingRequests =
            outbox.channel(LinkFacingRequest.class, "pendingLinkFacingRequests");
    private final List<LinkAttackStepRequest> pendingLinkAttackStepRequests =
            outbox.channel(LinkAttackStepRequest.class, "pendingLinkAttackStepRequests");
    private final List<LinkAttackStepRequest> pendingPreTickLinkAttackStepRequests =
            outbox.channel(LinkAttackStepRequest.class, "pendingPreTickLinkAttackStepRequests");
    private final List<LinkAttackClearRequest> pendingLinkAttackClearRequests =
            outbox.channel(LinkAttackClearRequest.class, "pendingLinkAttackClearRequests");
    private final List<RoomStatusPersistenceRequest> pendingRoomStatusPersistenceRequests =
            outbox.channel(RoomStatusPersistenceRequest.class, "pendingRoomStatusPersistenceRequests");
    private final List<LinkHeldItemPoseRequest> pendingLinkHeldItemPoseRequests =
            outbox.channel(LinkHeldItemPoseRequest.class, "pendingLinkHeldItemPoseRequests");
    private final List<LinkSwordSpinPoseRequest> pendingLinkSwordSpinPoseRequests =
            outbox.channel(LinkSwordSpinPoseRequest.class, "pendingLinkSwordSpinPoseRequests");
    private final List<LinkSwordFinalPoseRequest> pendingLinkSwordFinalPoseRequests =
            outbox.channel(LinkSwordFinalPoseRequest.class, "pendingLinkSwordFinalPoseRequests");
    private final List<ScreenShakeRequest> pendingScreenShakeRequests =
            outbox.channel(ScreenShakeRequest.class, "pendingScreenShakeRequests");
    private final List<DialogRequest> pendingDialogRequests =
            outbox.channel(DialogRequest.class, "pendingDialogRequests");
    private final List<EntityCombatEvent> pendingEntityEvents =
            outbox.channel(EntityCombatEvent.class, "pendingEntityEvents");
    private final List<EntityCombatEvent> pendingPreTickEntityEvents =
            outbox.channel(EntityCombatEvent.class, "pendingPreTickEntityEvents");
    private final List<ThrownBackgroundCollisionEvent> pendingThrownBackgroundCollisions =
            outbox.channel(ThrownBackgroundCollisionEvent.class, "pendingThrownBackgroundCollisions");
    private final List<DungeonWarpRequest> pendingDungeonWarpRequests =
            outbox.channel(DungeonWarpRequest.class, "pendingDungeonWarpRequests");
    private final List<WarpLinkStateRequest> pendingWarpLinkStateRequests =
            outbox.channel(WarpLinkStateRequest.class, "pendingWarpLinkStateRequests");
    private final List<ChestRewardEvent> pendingChestRewardEvents =
            outbox.channel(ChestRewardEvent.class, "pendingChestRewardEvents");
    private final List<KeyRewardEvent> pendingKeyRewardEvents =
            outbox.channel(KeyRewardEvent.class, "pendingKeyRewardEvents");
    private final List<SlimeKeyRewardEvent> pendingSlimeKeyRewardEvents =
            outbox.channel(SlimeKeyRewardEvent.class, "pendingSlimeKeyRewardEvents");
    private final List<WitchExchangeEvent> pendingWitchExchangeEvents =
            outbox.channel(WitchExchangeEvent.class, "pendingWitchExchangeEvents");
    private final List<WitchRewardEvent> pendingWitchRewardEvents =
            outbox.channel(WitchRewardEvent.class, "pendingWitchRewardEvents");
    private final List<KeyQuicksandEvent> pendingKeyQuicksandEvents =
            outbox.channel(KeyQuicksandEvent.class, "pendingKeyQuicksandEvents");
    private final List<LikeLikeEvent> pendingLikeLikeEvents =
            outbox.channel(LikeLikeEvent.class, "pendingLikeLikeEvents");
    private final List<BombExplosionEvent> pendingBombExplosionEvents =
            outbox.channel(BombExplosionEvent.class, "pendingBombExplosionEvents");
    private final List<HookshotBridgeUpdate> hookshotBridgeUpdates =
            outbox.channel(HookshotBridgeUpdate.class, "hookshotBridgeUpdates");
    private final List<HeartPiecePickupUpdate> heartPieceUpdates =
            outbox.channel(HeartPiecePickupUpdate.class, "heartPieceUpdates");
    private final List<BoomerangObjectRequest> boomerangObjectRequests =
            outbox.channel(BoomerangObjectRequest.class, "boomerangObjectRequests");
    private final List<MagicRodObjectRequest> magicRodObjectRequests =
            outbox.channel(MagicRodObjectRequest.class, "magicRodObjectRequests");
    private final List<MagicPowderObjectRequest> magicPowderObjectRequests =
            outbox.channel(MagicPowderObjectRequest.class, "magicPowderObjectRequests");

    void combat(EntityCombatEvent event) {
        pendingEntityEvents.add(event);
    }

    void combat(Collection<? extends EntityCombatEvent> events) {
        pendingEntityEvents.addAll(events);
    }

    void clearPendingEntityEvents() {
        pendingEntityEvents.clear();
    }

    private final List<EntityCombatEvent> pendingEntityEventsView = Collections.unmodifiableList(pendingEntityEvents);

    /** Read-only live channel; valid for synchronous consumption before the next drain. */
    List<EntityCombatEvent> pendingEntityEvents() {
        return pendingEntityEventsView;
    }

    void vfx(TransientVfxRequest event) {
        transientVfxRequests.add(event);
    }

    void clearTransientVfxRequests() {
        transientVfxRequests.clear();
    }

    private final List<TransientVfxRequest> transientVfxRequestsView =
            Collections.unmodifiableList(transientVfxRequests);

    /** Read-only live channel; valid for synchronous consumption before the next drain. */
    List<TransientVfxRequest> transientVfxRequests() {
        return transientVfxRequestsView;
    }

    void dialog(DialogRequest event) {
        pendingDialogRequests.add(event);
    }

    void clearPendingDialogRequests() {
        pendingDialogRequests.clear();
    }

    void hookshotBridge(HookshotBridgeUpdate event) {
        hookshotBridgeUpdates.add(event);
    }

    void clearHookshotBridgeUpdates() {
        hookshotBridgeUpdates.clear();
    }

    private final List<HookshotBridgeUpdate> hookshotBridgeUpdatesView =
            Collections.unmodifiableList(hookshotBridgeUpdates);

    /** Read-only live channel; valid for synchronous consumption before the next drain. */
    List<HookshotBridgeUpdate> hookshotBridgeUpdates() {
        return hookshotBridgeUpdatesView;
    }

    void dungeonWarp(DungeonWarpRequest event) {
        pendingDungeonWarpRequests.add(event);
    }

    void hinoxLinkEffect(HinoxLinkEffectRequest event) {
        pendingHinoxLinkEffectRequests.add(event);
    }

    void clearPendingHinoxLinkEffectRequests() {
        pendingHinoxLinkEffectRequests.clear();
    }

    void keyQuicksand(KeyQuicksandEvent event) {
        pendingKeyQuicksandEvents.add(event);
    }

    void clearPendingKeyQuicksandEvents() {
        pendingKeyQuicksandEvents.clear();
    }

    void keyReward(KeyRewardEvent event) {
        pendingKeyRewardEvents.add(event);
    }

    void likeLike(LikeLikeEvent event) {
        pendingLikeLikeEvents.add(event);
    }

    void clearPendingLikeLikeEvents() {
        pendingLikeLikeEvents.clear();
    }

    void linkAttackClear(LinkAttackClearRequest event) {
        pendingLinkAttackClearRequests.add(event);
    }

    void clearPendingLinkAttackClearRequests() {
        pendingLinkAttackClearRequests.clear();
    }

    void linkFacing(LinkFacingRequest event) {
        pendingLinkFacingRequests.add(event);
    }

    void clearPendingLinkFacingRequests() {
        pendingLinkFacingRequests.clear();
    }

    void linkFallenPose(LinkFallenPoseRequest event) {
        pendingLinkFallenPoseRequests.add(event);
    }

    void clearPendingLinkFallenPoseRequests() {
        pendingLinkFallenPoseRequests.clear();
    }

    void linkMotionBlock(LinkMotionBlockRequest event) {
        pendingLinkMotionBlockRequests.add(event);
    }

    void clearPendingLinkMotionBlockRequests() {
        pendingLinkMotionBlockRequests.clear();
    }

    void roomStatusPersistence(RoomStatusPersistenceRequest event) {
        pendingRoomStatusPersistenceRequests.add(event);
    }

    void clearPendingRoomStatusPersistenceRequests() {
        pendingRoomStatusPersistenceRequests.clear();
    }

    void roosterLinkState(RoosterLinkStateRequest event) {
        pendingRoosterLinkStateRequests.add(event);
    }

    void clearPendingRoosterLinkStateRequests() {
        pendingRoosterLinkStateRequests.clear();
    }

    void screenShake(ScreenShakeRequest event) {
        pendingScreenShakeRequests.add(event);
    }

    void clearPendingScreenShakeRequests() {
        pendingScreenShakeRequests.clear();
    }

    void sideViewPlatformLink(SideViewPlatformLinkRequest event) {
        pendingSideViewPlatformLinkRequests.add(event);
    }

    void clearPendingSideViewPlatformLinkRequests() {
        pendingSideViewPlatformLinkRequests.clear();
    }

    void slimeKeyReward(SlimeKeyRewardEvent event) {
        pendingSlimeKeyRewardEvents.add(event);
    }

    void clearPendingSlimeKeyRewardEvents() {
        pendingSlimeKeyRewardEvents.clear();
    }

    void warpLinkState(WarpLinkStateRequest event) {
        pendingWarpLinkStateRequests.add(event);
    }

    void clearPendingWarpLinkStateRequests() {
        pendingWarpLinkStateRequests.clear();
    }

    void witchExchange(WitchExchangeEvent event) {
        pendingWitchExchangeEvents.add(event);
    }

    void clearPendingWitchExchangeEvents() {
        pendingWitchExchangeEvents.clear();
    }

    private final List<WitchExchangeEvent> pendingWitchExchangeEventsView =
            Collections.unmodifiableList(pendingWitchExchangeEvents);

    /** Read-only live channel; valid for synchronous consumption before the next drain. */
    List<WitchExchangeEvent> pendingWitchExchangeEvents() {
        return pendingWitchExchangeEventsView;
    }

    void witchReward(WitchRewardEvent event) {
        pendingWitchRewardEvents.add(event);
    }

    void clearPendingWitchRewardEvents() {
        pendingWitchRewardEvents.clear();
    }

    private final List<WitchRewardEvent> pendingWitchRewardEventsView =
            Collections.unmodifiableList(pendingWitchRewardEvents);

    /** Read-only live channel; valid for synchronous consumption before the next drain. */
    List<WitchRewardEvent> pendingWitchRewardEvents() {
        return pendingWitchRewardEventsView;
    }

    void projectileLaunch(RoamingEnemyMotion.LaunchRequest event) {
        projectileLaunchRequests.add(event);
    }

    void clearProjectileLaunchRequests() {
        projectileLaunchRequests.clear();
    }

    private final List<RoamingEnemyMotion.LaunchRequest> projectileLaunchRequestsView =
            Collections.unmodifiableList(projectileLaunchRequests);

    /** Read-only live channel; valid for synchronous consumption before the next drain. */
    List<RoamingEnemyMotion.LaunchRequest> projectileLaunchRequests() {
        return projectileLaunchRequestsView;
    }

    void preTickCombat(EntityCombatEvent event) {
        pendingPreTickEntityEvents.add(event);
    }

    void clearPendingPreTickEntityEvents() {
        pendingPreTickEntityEvents.clear();
    }

    private final List<EntityCombatEvent> pendingPreTickEntityEventsView =
            Collections.unmodifiableList(pendingPreTickEntityEvents);

    /** Read-only live channel; valid for synchronous consumption before the next drain. */
    List<EntityCombatEvent> pendingPreTickEntityEvents() {
        return pendingPreTickEntityEventsView;
    }

    void preTickLinkAttackStep(LinkAttackStepRequest event) {
        pendingPreTickLinkAttackStepRequests.add(event);
    }

    void clearPendingPreTickLinkAttackStepRequests() {
        pendingPreTickLinkAttackStepRequests.clear();
    }

    private final List<LinkAttackStepRequest> pendingPreTickLinkAttackStepRequestsView =
            Collections.unmodifiableList(pendingPreTickLinkAttackStepRequests);

    /** Read-only live channel; valid for synchronous consumption before the next drain. */
    List<LinkAttackStepRequest> pendingPreTickLinkAttackStepRequests() {
        return pendingPreTickLinkAttackStepRequestsView;
    }

    void linkAttackStep(Collection<? extends LinkAttackStepRequest> events) {
        pendingLinkAttackStepRequests.addAll(events);
    }

    void clearPendingLinkAttackStepRequests() {
        pendingLinkAttackStepRequests.clear();
    }

    private final List<LinkAttackStepRequest> pendingLinkAttackStepRequestsView =
            Collections.unmodifiableList(pendingLinkAttackStepRequests);

    /** Read-only live channel; valid for synchronous consumption before the next drain. */
    List<LinkAttackStepRequest> pendingLinkAttackStepRequests() {
        return pendingLinkAttackStepRequestsView;
    }

    void heartPiece(HeartPiecePickupUpdate event) {
        heartPieceUpdates.add(event);
    }

    void clearHeartPieceUpdates() {
        heartPieceUpdates.clear();
    }

    private final List<HeartPiecePickupUpdate> heartPieceUpdatesView = Collections.unmodifiableList(heartPieceUpdates);

    /** Read-only live channel; valid for synchronous consumption before the next drain. */
    List<HeartPiecePickupUpdate> heartPieceUpdates() {
        return heartPieceUpdatesView;
    }

    void heartContainerReward(HeartContainerRewardEvent event) {
        pendingHeartContainerRewards.add(event);
    }

    void clearPendingHeartContainerRewards() {
        pendingHeartContainerRewards.clear();
    }

    private final List<HeartContainerRewardEvent> pendingHeartContainerRewardsView =
            Collections.unmodifiableList(pendingHeartContainerRewards);

    /** Read-only live channel; valid for synchronous consumption before the next drain. */
    List<HeartContainerRewardEvent> pendingHeartContainerRewards() {
        return pendingHeartContainerRewardsView;
    }

    void madamMeowMeowFullHeal(MadamMeowMeowFullHealRequest event) {
        pendingMadamMeowMeowFullHealRequests.add(event);
    }

    void clearPendingMadamMeowMeowFullHealRequests() {
        pendingMadamMeowMeowFullHealRequests.clear();
    }

    private final List<MadamMeowMeowFullHealRequest> pendingMadamMeowMeowFullHealRequestsView =
            Collections.unmodifiableList(pendingMadamMeowMeowFullHealRequests);

    /** Read-only live channel; valid for synchronous consumption before the next drain. */
    List<MadamMeowMeowFullHealRequest> pendingMadamMeowMeowFullHealRequests() {
        return pendingMadamMeowMeowFullHealRequestsView;
    }

    void swordPickupReward(SwordPickupRewardEvent event) {
        pendingSwordPickupRewards.add(event);
    }

    void clearPendingSwordPickupRewards() {
        pendingSwordPickupRewards.clear();
    }

    private final List<SwordPickupRewardEvent> pendingSwordPickupRewardsView =
            Collections.unmodifiableList(pendingSwordPickupRewards);

    /** Read-only live channel; valid for synchronous consumption before the next drain. */
    List<SwordPickupRewardEvent> pendingSwordPickupRewards() {
        return pendingSwordPickupRewardsView;
    }

    void toadstoolReward(ToadstoolRewardEvent event) {
        pendingToadstoolRewards.add(event);
    }

    void clearPendingToadstoolRewards() {
        pendingToadstoolRewards.clear();
    }

    private final List<ToadstoolRewardEvent> pendingToadstoolRewardsView =
            Collections.unmodifiableList(pendingToadstoolRewards);

    /** Read-only live channel; valid for synchronous consumption before the next drain. */
    List<ToadstoolRewardEvent> pendingToadstoolRewards() {
        return pendingToadstoolRewardsView;
    }

    void instrumentReward(InstrumentRewardEvent event) {
        pendingInstrumentRewards.add(event);
    }

    void clearPendingInstrumentRewards() {
        pendingInstrumentRewards.clear();
    }

    private final List<InstrumentRewardEvent> pendingInstrumentRewardsView =
            Collections.unmodifiableList(pendingInstrumentRewards);

    /** Read-only live channel; valid for synchronous consumption before the next drain. */
    List<InstrumentRewardEvent> pendingInstrumentRewards() {
        return pendingInstrumentRewardsView;
    }

    void instrumentCompletion(InstrumentCompletionEvent event) {
        pendingInstrumentCompletions.add(event);
    }

    void clearPendingInstrumentCompletions() {
        pendingInstrumentCompletions.clear();
    }

    private final List<InstrumentCompletionEvent> pendingInstrumentCompletionsView =
            Collections.unmodifiableList(pendingInstrumentCompletions);

    /** Read-only live channel; valid for synchronous consumption before the next drain. */
    List<InstrumentCompletionEvent> pendingInstrumentCompletions() {
        return pendingInstrumentCompletionsView;
    }

    void richardProgress(RichardProgressEvent event) {
        pendingRichardProgressEvents.add(event);
    }

    void clearPendingRichardProgressEvents() {
        pendingRichardProgressEvents.clear();
    }

    private final List<RichardProgressEvent> pendingRichardProgressEventsView =
            Collections.unmodifiableList(pendingRichardProgressEvents);

    /** Read-only live channel; valid for synchronous consumption before the next drain. */
    List<RichardProgressEvent> pendingRichardProgressEvents() {
        return pendingRichardProgressEventsView;
    }

    void owlEventCompletion(OwlEventCompletion event) {
        pendingOwlEventCompletions.add(event);
    }

    void clearPendingOwlEventCompletions() {
        pendingOwlEventCompletions.clear();
    }

    private final List<OwlEventCompletion> pendingOwlEventCompletionsView =
            Collections.unmodifiableList(pendingOwlEventCompletions);

    /** Read-only live channel; valid for synchronous consumption before the next drain. */
    List<OwlEventCompletion> pendingOwlEventCompletions() {
        return pendingOwlEventCompletionsView;
    }

    void linkHeldItemPose(LinkHeldItemPoseRequest event) {
        pendingLinkHeldItemPoseRequests.add(event);
    }

    void clearPendingLinkHeldItemPoseRequests() {
        pendingLinkHeldItemPoseRequests.clear();
    }

    void linkSwordSpinPose(LinkSwordSpinPoseRequest event) {
        pendingLinkSwordSpinPoseRequests.add(event);
    }

    void clearPendingLinkSwordSpinPoseRequests() {
        pendingLinkSwordSpinPoseRequests.clear();
    }

    void linkSwordFinalPose(LinkSwordFinalPoseRequest event) {
        pendingLinkSwordFinalPoseRequests.add(event);
    }

    void clearPendingLinkSwordFinalPoseRequests() {
        pendingLinkSwordFinalPoseRequests.clear();
    }

    void linkFinalPosition(LinkFinalPositionRequest event) {
        pendingLinkFinalPositionRequests.add(event);
    }

    void clearPendingLinkFinalPositionRequests() {
        pendingLinkFinalPositionRequests.clear();
    }

    void boomerangObject(BoomerangObjectRequest event) {
        boomerangObjectRequests.add(event);
    }

    void clearBoomerangObjectRequests() {
        boomerangObjectRequests.clear();
    }

    private final List<BoomerangObjectRequest> boomerangObjectRequestsView =
            Collections.unmodifiableList(boomerangObjectRequests);

    /** Read-only live channel; valid for synchronous consumption before the next drain. */
    List<BoomerangObjectRequest> boomerangObjectRequests() {
        return boomerangObjectRequestsView;
    }

    void magicRodObject(MagicRodObjectRequest event) {
        magicRodObjectRequests.add(event);
    }

    void clearMagicRodObjectRequests() {
        magicRodObjectRequests.clear();
    }

    private final List<MagicRodObjectRequest> magicRodObjectRequestsView =
            Collections.unmodifiableList(magicRodObjectRequests);

    /** Read-only live channel; valid for synchronous consumption before the next drain. */
    List<MagicRodObjectRequest> magicRodObjectRequests() {
        return magicRodObjectRequestsView;
    }

    void magicPowderObject(MagicPowderObjectRequest event) {
        magicPowderObjectRequests.add(event);
    }

    void clearMagicPowderObjectRequests() {
        magicPowderObjectRequests.clear();
    }

    private final List<MagicPowderObjectRequest> magicPowderObjectRequestsView =
            Collections.unmodifiableList(magicPowderObjectRequests);

    /** Read-only live channel; valid for synchronous consumption before the next drain. */
    List<MagicPowderObjectRequest> magicPowderObjectRequests() {
        return magicPowderObjectRequestsView;
    }

    void chestReward(ChestRewardEvent event) {
        pendingChestRewardEvents.add(event);
    }

    void clearPendingChestRewardEvents() {
        pendingChestRewardEvents.clear();
    }

    void thrownBackgroundCollision(ThrownBackgroundCollisionEvent event) {
        pendingThrownBackgroundCollisions.add(event);
    }

    void clearPendingThrownBackgroundCollisions() {
        pendingThrownBackgroundCollisions.clear();
    }

    void bombExplosion(BombExplosionEvent event) {
        pendingBombExplosionEvents.add(event);
    }

    void clearPendingBombExplosionEvents() {
        pendingBombExplosionEvents.clear();
    }

    void sideViewPotLink(SideViewPotLinkRequest event) {
        pendingSideViewPotLinkRequests.add(event);
    }

    void clearPendingSideViewPotLinkRequests() {
        pendingSideViewPotLinkRequests.clear();
    }

    void sound(int slot, int type, EntityCombatEvent.SoundChannel channel, int id) {
        combat(new EntityCombatEvent(slot, type, 0, false, channel, id));
    }

    void dialog(int sourceSlot, int globalId) {
        dialog(new DialogRequest(sourceSlot, globalId));
    }

    void linkFacing(int sourceSlot, int direction) {
        linkFacing(new LinkFacingRequest(sourceSlot, direction));
    }

    void linkFacing(int sourceSlot, int direction, boolean preserveWalkingPhase) {
        linkFacing(new LinkFacingRequest(sourceSlot, direction, preserveWalkingPhase));
    }
}
