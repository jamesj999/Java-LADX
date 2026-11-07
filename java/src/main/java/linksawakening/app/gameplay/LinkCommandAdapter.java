package linksawakening.app.gameplay;

import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.LinkAttackStepRequest;
import linksawakening.world.entity.event.WarpLinkStateRequest;
import linksawakening.world.entity.event.LinkFallenPoseRequest;
import linksawakening.world.entity.event.LinkSwordSpinPoseRequest;
import linksawakening.world.entity.event.LinkFinalPositionRequest;
import linksawakening.world.entity.event.HinoxLinkEffectRequest;
import linksawakening.world.entity.event.SideViewPlatformLinkRequest;
import linksawakening.world.entity.event.LinkSwordFinalPoseRequest;
import linksawakening.world.entity.event.SideViewPotLinkRequest;
import linksawakening.world.entity.event.RoosterLinkStateRequest;
import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.LikeLikeEvent;

import linksawakening.world.transition.Warp;

import linksawakening.entity.LinkCommand;

import linksawakening.world.entity.event.EntityProjectileEvent;

/** The sole translation boundary between world requests and Link commands. */
final class LinkCommandAdapter {
    private LinkCommandAdapter() {}

    static LinkCommand from(LinkFinalPositionRequest r) {
        return new LinkCommand.FinalPosition(r.clearLinkPositionIncrement(), r.markLinkPushing(), r.resetPegasusBoots());
    }
    static LinkCommand from(SideViewPlatformLinkRequest r) {
        return new LinkCommand.PlatformContact(r.horizontalDelta(), r.positionY(), r.speedY(), r.standing());
    }
    static LinkCommand from(SideViewPotLinkRequest r) {
        return new LinkCommand.PotContact(r.restoreFinalPositionX(), r.resetPegasusBoots(), r.speedX(),
            r.ignoreCollisionCountdown(), r.snapTop(), r.positionY(), r.speedY());
    }
    static LinkCommand from(LinkMotionBlockRequest r) {
        return new LinkCommand.MotionBlock(r.positionDeltaX(), r.positionDeltaY());
    }
    static LinkCommand from(WarpLinkStateRequest r) {
        return new LinkCommand.Warp(r.positionX(), r.visualPositionY(), r.immunityCountdown());
    }
    static LinkCommand from(LinkFacingRequest r) {
        return new LinkCommand.Facing(r.romDirection(), r.preserveWalkingPhase());
    }
    static LinkCommand from(LinkFallenPoseRequest r) { return LinkCommand.Action.FALLEN_POSE; }
    static LinkCommand from(LinkAttackClearRequest r) { return LinkCommand.Action.CLEAR_ATTACK; }
    static LinkCommand from(LinkAttackStepRequest r) { return LinkCommand.Action.START_ATTACK; }
    static LinkCommand from(LinkHeldItemPoseRequest r) { return new LinkCommand.HeldItemPose(r.powerUp()); }
    static LinkCommand from(LinkSwordSpinPoseRequest r) { return new LinkCommand.SwordSpinPose(r.countdown()); }
    static LinkCommand from(LinkSwordFinalPoseRequest r) { return LinkCommand.Action.SWORD_FINAL_POSE; }
    static LinkCommand from(RoosterLinkStateRequest r) {
        return new LinkCommand.RoosterFlight(r.positionZ(), r.velocityZ(), r.speedX(), r.speedY(), r.romDirection());
    }
    static LinkCommand from(HinoxLinkEffectRequest r) {
        return new LinkCommand.HinoxGrab(r.heldX(), r.heldY(), r.heldZ(), r.speedX(), r.speedY(),
            r.velocityZ(), r.airborneState(), r.motionBlocked(), r.applyHeldLinkPose());
    }
    static LinkCommand from(LikeLikeEvent r) {
        return r.kind() == LikeLikeEvent.Kind.CAPTURE
            ? new LinkCommand.Capture(r.entityX(), r.entityY(), r.pinLinkPosition())
            : LinkCommand.Action.RELEASE_CAPTURE;
    }
    static LinkCommand from(EntityCombatEvent event) {
        var response = event.linkCollisionResponse();
        return new LinkCommand.ContactRecoil(response.speedX(), response.speedY(), response.ignoreCollisionCountdown());
    }
    static LinkCommand from(EntityProjectileEvent r) {
        return new LinkCommand.ProjectileResponse(r.linkSpeedX(), r.linkSpeedY(),
            r.linkIgnoreCollisionCountdown(), r.kind() == EntityProjectileEvent.Kind.HOOKSHOT_PULL);
    }
}
