package linksawakening.world.room;

import java.util.List;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.HinoxLinkEffectRequest;
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
import linksawakening.world.entity.event.RoosterLinkStateRequest;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.entity.event.SideViewPlatformLinkRequest;
import linksawakening.world.entity.event.SideViewPotLinkRequest;

/** Entity-owner drain port; each drain preserves its unloaded default before accessing a runtime. */
interface RoomEntityOutputSource {
    List<linksawakening.world.entity.event.WarpLinkStateRequest> consumeWarpLinkStateRequests();

    List<linksawakening.world.entity.event.HeartPiecePickupUpdate> consumeHeartPieceUpdates();

    void drainEntityEvents(RoomFrameDrain<EntityCombatEvent> target);

    DialogRequest consumeFirstEntityDialogRequest();

    List<EntityCombatEvent> consumeEntityEvents();

    List<LinkFinalPositionRequest> consumeLinkFinalPositionRequests();

    List<RoosterLinkStateRequest> consumeRoosterLinkStateRequests();

    List<LinkMotionBlockRequest> consumeLinkMotionBlockRequests();

    List<SideViewPlatformLinkRequest> consumeSideViewPlatformLinkRequests();

    List<SideViewPotLinkRequest> consumeSideViewPotLinkRequests();

    List<HinoxLinkEffectRequest> consumeHinoxLinkEffectRequests();

    List<LinkFallenPoseRequest> consumeLinkFallenPoseRequests();

    List<LinkFacingRequest> consumeLinkFacingRequests();

    List<LinkAttackClearRequest> consumeLinkAttackClearRequests();

    List<LinkAttackStepRequest> consumeLinkAttackStepRequests();

    List<LinkHeldItemPoseRequest> consumeLinkHeldItemPoseRequests();

    List<LinkSwordSpinPoseRequest> consumeLinkSwordSpinPoseRequests();

    List<LinkSwordFinalPoseRequest> consumeLinkSwordFinalPoseRequests();

    List<ScreenShakeRequest> consumeScreenShakeRequests();

    int consumePendingMusicTrack();

    int consumePendingMusicFadeOutCountdown();

    List<LikeLikeEvent> consumeLikeLikeEvents();
}
