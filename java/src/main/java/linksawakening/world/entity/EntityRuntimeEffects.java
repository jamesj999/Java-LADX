package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySlotValidation.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

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
import linksawakening.world.entity.motion.*;

/** Event consumption capability; state and channel ownership remain in the room outbox. */
interface EntityRuntimeEffects {
    EntityEffectConsumption entityEffects();

    default boolean consumePendingSwitchBlockAnimationRequest() {
        return entityEffects().consumePendingSwitchBlockAnimationRequest();
    }

    default int consumePendingClearedEntityMask() {
        return entityEffects().consumePendingClearedEntityMask();
    }

    default int consumePendingRoomStatusMask() {
        return entityEffects().consumePendingRoomStatusMask();
    }

    default List<EntityCombatEvent> consumePendingEntityEvents() {
        return entityEffects().consumePendingEntityEvents();
    }

    default List<ThrownBackgroundCollisionEvent> consumePendingThrownBackgroundCollisions() {
        return entityEffects().consumePendingThrownBackgroundCollisions();
    }

    default List<DungeonWarpRequest> consumePendingDungeonWarpRequests() {
        return entityEffects().consumePendingDungeonWarpRequests();
    }

    default List<WarpLinkStateRequest> consumePendingWarpLinkStateRequests() {
        return entityEffects().consumePendingWarpLinkStateRequests();
    }

    default List<LinkFinalPositionRequest> consumePendingLinkFinalPositionRequests() {
        return entityEffects().consumePendingLinkFinalPositionRequests();
    }

    default List<RoosterLinkStateRequest> consumePendingRoosterLinkStateRequests() {
        return entityEffects().consumePendingRoosterLinkStateRequests();
    }

    default List<LinkMotionBlockRequest> consumePendingLinkMotionBlockRequests() {
        return entityEffects().consumePendingLinkMotionBlockRequests();
    }

    default List<SideViewPlatformLinkRequest> consumePendingSideViewPlatformLinkRequests() {
        return entityEffects().consumePendingSideViewPlatformLinkRequests();
    }

    default List<SideViewPotLinkRequest> consumePendingSideViewPotLinkRequests() {
        return entityEffects().consumePendingSideViewPotLinkRequests();
    }

    default List<HinoxLinkEffectRequest> consumePendingHinoxLinkEffectRequests() {
        return entityEffects().consumePendingHinoxLinkEffectRequests();
    }

    default List<LinkFallenPoseRequest> consumePendingLinkFallenPoseRequests() {
        return entityEffects().consumePendingLinkFallenPoseRequests();
    }

    default List<LinkFacingRequest> consumePendingLinkFacingRequests() {
        return entityEffects().consumePendingLinkFacingRequests();
    }

    default List<LinkAttackClearRequest> consumePendingLinkAttackClearRequests() {
        return entityEffects().consumePendingLinkAttackClearRequests();
    }

    default List<LinkAttackStepRequest> consumePendingLinkAttackStepRequests() {
        return entityEffects().consumePendingLinkAttackStepRequests();
    }

    default List<RoomStatusPersistenceRequest> consumePendingRoomStatusPersistenceRequests() {
        return entityEffects().consumePendingRoomStatusPersistenceRequests();
    }

    default List<LinkHeldItemPoseRequest> consumePendingLinkHeldItemPoseRequests() {
        return entityEffects().consumePendingLinkHeldItemPoseRequests();
    }

    default List<LinkSwordSpinPoseRequest> consumePendingLinkSwordSpinPoseRequests() {
        return entityEffects().consumePendingLinkSwordSpinPoseRequests();
    }

    default List<LinkSwordFinalPoseRequest> consumePendingLinkSwordFinalPoseRequests() {
        return entityEffects().consumePendingLinkSwordFinalPoseRequests();
    }

    default List<ScreenShakeRequest> consumePendingScreenShakeRequests() {
        return entityEffects().consumePendingScreenShakeRequests();
    }

    default List<ChestRewardEvent> consumePendingChestRewardEvents() {
        return entityEffects().consumePendingChestRewardEvents();
    }

    default List<KeyRewardEvent> consumePendingKeyRewardEvents() {
        return entityEffects().consumePendingKeyRewardEvents();
    }

    default List<SlimeKeyRewardEvent> consumePendingSlimeKeyRewardEvents() {
        return entityEffects().consumePendingSlimeKeyRewardEvents();
    }

    default List<KeyQuicksandEvent> consumePendingKeyQuicksandEvents() {
        return entityEffects().consumePendingKeyQuicksandEvents();
    }

    default int consumePendingMusicTrack() {
        return entityEffects().consumePendingMusicTrack();
    }

    default int consumePendingMusicFadeOutCountdown() {
        return entityEffects().consumePendingMusicFadeOutCountdown();
    }

    default List<LikeLikeEvent> consumePendingLikeLikeEvents() {
        return entityEffects().consumePendingLikeLikeEvents();
    }

    default List<DialogRequest> consumePendingDialogRequests() {
        return entityEffects().consumePendingDialogRequests();
    }

    default List<BombExplosionEvent> consumeBombExplosionEvents() {
        return entityEffects().consumeBombExplosionEvents();
    }

    default List<HeartPiecePickupUpdate> consumeHeartPieceUpdates() {
        return entityEffects().consumeHeartPieceUpdates();
    }

    default List<HeartContainerRewardEvent> consumePendingHeartContainerRewards() {
        return entityEffects().consumePendingHeartContainerRewards();
    }

    default List<MadamMeowMeowFullHealRequest> consumePendingMadamMeowMeowFullHealRequests() {
        return entityEffects().consumePendingMadamMeowMeowFullHealRequests();
    }

    default List<SwordPickupRewardEvent> consumePendingSwordPickupRewards() {
        return entityEffects().consumePendingSwordPickupRewards();
    }

    default List<ToadstoolRewardEvent> consumePendingToadstoolRewards() {
        return entityEffects().consumePendingToadstoolRewards();
    }

    default List<InstrumentRewardEvent> consumePendingInstrumentRewards() {
        return entityEffects().consumePendingInstrumentRewards();
    }

    default List<InstrumentCompletionEvent> consumePendingInstrumentCompletions() {
        return entityEffects().consumePendingInstrumentCompletions();
    }

    default List<RichardProgressEvent> consumePendingRichardProgressEvents() {
        return entityEffects().consumePendingRichardProgressEvents();
    }

    default List<WitchExchangeEvent> consumePendingWitchExchangeEvents() {
        return entityEffects().consumePendingWitchExchangeEvents();
    }

    default List<WitchRewardEvent> consumePendingWitchRewardEvents() {
        return entityEffects().consumePendingWitchRewardEvents();
    }

    default List<OwlEventCompletion> consumePendingOwlEventCompletions() {
        return entityEffects().consumePendingOwlEventCompletions();
    }
}
