package linksawakening.world.entity;

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
    public final List<RoamingEnemyMotion.LaunchRequest> projectileLaunchRequests =
            outbox.channel(RoamingEnemyMotion.LaunchRequest.class, "projectileLaunchRequests");
    final List<TransientVfxRequest> transientVfxRequests =
            outbox.channel(TransientVfxRequest.class, "transientVfxRequests");
    final List<HeartContainerRewardEvent> pendingHeartContainerRewards =
            outbox.channel(HeartContainerRewardEvent.class, "pendingHeartContainerRewards");
    final List<MadamMeowMeowFullHealRequest> pendingMadamMeowMeowFullHealRequests =
            outbox.channel(MadamMeowMeowFullHealRequest.class, "pendingMadamMeowMeowFullHealRequests");
    final List<SwordPickupRewardEvent> pendingSwordPickupRewards =
            outbox.channel(SwordPickupRewardEvent.class, "pendingSwordPickupRewards");
    final List<ToadstoolRewardEvent> pendingToadstoolRewards =
            outbox.channel(ToadstoolRewardEvent.class, "pendingToadstoolRewards");
    final List<InstrumentRewardEvent> pendingInstrumentRewards =
            outbox.channel(InstrumentRewardEvent.class, "pendingInstrumentRewards");
    final List<InstrumentCompletionEvent> pendingInstrumentCompletions =
            outbox.channel(InstrumentCompletionEvent.class, "pendingInstrumentCompletions");
    final List<RichardProgressEvent> pendingRichardProgressEvents =
            outbox.channel(RichardProgressEvent.class, "pendingRichardProgressEvents");
    final List<OwlEventCompletion> pendingOwlEventCompletions =
            outbox.channel(OwlEventCompletion.class, "pendingOwlEventCompletions");
    final List<LinkFinalPositionRequest> pendingLinkFinalPositionRequests =
            outbox.channel(LinkFinalPositionRequest.class, "pendingLinkFinalPositionRequests");
    final List<RoosterLinkStateRequest> pendingRoosterLinkStateRequests =
            outbox.channel(RoosterLinkStateRequest.class, "pendingRoosterLinkStateRequests");
    final List<SideViewPlatformLinkRequest> pendingSideViewPlatformLinkRequests =
            outbox.channel(SideViewPlatformLinkRequest.class, "pendingSideViewPlatformLinkRequests");
    final List<SideViewPotLinkRequest> pendingSideViewPotLinkRequests =
            outbox.channel(SideViewPotLinkRequest.class, "pendingSideViewPotLinkRequests");
    final List<LinkMotionBlockRequest> pendingLinkMotionBlockRequests =
            outbox.channel(LinkMotionBlockRequest.class, "pendingLinkMotionBlockRequests");
    final List<HinoxLinkEffectRequest> pendingHinoxLinkEffectRequests =
            outbox.channel(HinoxLinkEffectRequest.class, "pendingHinoxLinkEffectRequests");
    final List<LinkFallenPoseRequest> pendingLinkFallenPoseRequests =
            outbox.channel(LinkFallenPoseRequest.class, "pendingLinkFallenPoseRequests");
    final List<LinkFacingRequest> pendingLinkFacingRequests =
            outbox.channel(LinkFacingRequest.class, "pendingLinkFacingRequests");
    final List<LinkAttackStepRequest> pendingLinkAttackStepRequests =
            outbox.channel(LinkAttackStepRequest.class, "pendingLinkAttackStepRequests");
    final List<LinkAttackStepRequest> pendingPreTickLinkAttackStepRequests =
            outbox.channel(LinkAttackStepRequest.class, "pendingPreTickLinkAttackStepRequests");
    final List<LinkAttackClearRequest> pendingLinkAttackClearRequests =
            outbox.channel(LinkAttackClearRequest.class, "pendingLinkAttackClearRequests");
    final List<RoomStatusPersistenceRequest> pendingRoomStatusPersistenceRequests =
            outbox.channel(RoomStatusPersistenceRequest.class, "pendingRoomStatusPersistenceRequests");
    final List<LinkHeldItemPoseRequest> pendingLinkHeldItemPoseRequests =
            outbox.channel(LinkHeldItemPoseRequest.class, "pendingLinkHeldItemPoseRequests");
    final List<LinkSwordSpinPoseRequest> pendingLinkSwordSpinPoseRequests =
            outbox.channel(LinkSwordSpinPoseRequest.class, "pendingLinkSwordSpinPoseRequests");
    final List<LinkSwordFinalPoseRequest> pendingLinkSwordFinalPoseRequests =
            outbox.channel(LinkSwordFinalPoseRequest.class, "pendingLinkSwordFinalPoseRequests");
    final List<ScreenShakeRequest> pendingScreenShakeRequests =
            outbox.channel(ScreenShakeRequest.class, "pendingScreenShakeRequests");
    final List<DialogRequest> pendingDialogRequests = outbox.channel(DialogRequest.class, "pendingDialogRequests");
    final List<EntityCombatEvent> pendingEntityEvents = outbox.channel(EntityCombatEvent.class, "pendingEntityEvents");
    final List<EntityCombatEvent> pendingPreTickEntityEvents =
            outbox.channel(EntityCombatEvent.class, "pendingPreTickEntityEvents");
    final List<ThrownBackgroundCollisionEvent> pendingThrownBackgroundCollisions =
            outbox.channel(ThrownBackgroundCollisionEvent.class, "pendingThrownBackgroundCollisions");
    final List<DungeonWarpRequest> pendingDungeonWarpRequests =
            outbox.channel(DungeonWarpRequest.class, "pendingDungeonWarpRequests");
    final List<WarpLinkStateRequest> pendingWarpLinkStateRequests =
            outbox.channel(WarpLinkStateRequest.class, "pendingWarpLinkStateRequests");
    final List<ChestRewardEvent> pendingChestRewardEvents =
            outbox.channel(ChestRewardEvent.class, "pendingChestRewardEvents");
    final List<KeyRewardEvent> pendingKeyRewardEvents = outbox.channel(KeyRewardEvent.class, "pendingKeyRewardEvents");
    final List<SlimeKeyRewardEvent> pendingSlimeKeyRewardEvents =
            outbox.channel(SlimeKeyRewardEvent.class, "pendingSlimeKeyRewardEvents");
    final List<WitchExchangeEvent> pendingWitchExchangeEvents =
            outbox.channel(WitchExchangeEvent.class, "pendingWitchExchangeEvents");
    final List<WitchRewardEvent> pendingWitchRewardEvents =
            outbox.channel(WitchRewardEvent.class, "pendingWitchRewardEvents");
    final List<KeyQuicksandEvent> pendingKeyQuicksandEvents =
            outbox.channel(KeyQuicksandEvent.class, "pendingKeyQuicksandEvents");
    final List<LikeLikeEvent> pendingLikeLikeEvents = outbox.channel(LikeLikeEvent.class, "pendingLikeLikeEvents");
    final List<BombExplosionEvent> pendingBombExplosionEvents =
            outbox.channel(BombExplosionEvent.class, "pendingBombExplosionEvents");
    public final List<HookshotBridgeUpdate> hookshotBridgeUpdates =
            outbox.channel(HookshotBridgeUpdate.class, "hookshotBridgeUpdates");
    final List<HeartPiecePickupUpdate> heartPieceUpdates =
            outbox.channel(HeartPiecePickupUpdate.class, "heartPieceUpdates");
    final List<BoomerangObjectRequest> boomerangObjectRequests =
            outbox.channel(BoomerangObjectRequest.class, "boomerangObjectRequests");
    final List<MagicRodObjectRequest> magicRodObjectRequests =
            outbox.channel(MagicRodObjectRequest.class, "magicRodObjectRequests");
    final List<MagicPowderObjectRequest> magicPowderObjectRequests =
            outbox.channel(MagicPowderObjectRequest.class, "magicPowderObjectRequests");
}
