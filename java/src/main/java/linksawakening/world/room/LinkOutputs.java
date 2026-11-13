package linksawakening.world.room;

import java.util.List;
import linksawakening.world.entity.event.HinoxLinkEffectRequest;
import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.LinkFallenPoseRequest;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.LinkSwordFinalPoseRequest;
import linksawakening.world.entity.event.LinkSwordSpinPoseRequest;
import linksawakening.world.entity.event.RoosterLinkStateRequest;
import linksawakening.world.entity.event.WarpLinkStateRequest;

public final class LinkOutputs {
    private final RoomEntityOutputSource session;
    private final Dependencies dependencies;

    LinkOutputs(Dependencies dependencies, RoomEntityOutputSource session) {
        this.dependencies = dependencies;
        this.session = java.util.Objects.requireNonNull(session);
    }

    public void drainPositionWrites(LinkWriteSink.Positions target) {
        var platforms = session.consumeSideViewPlatformLinkRequests();
        var pots = session.consumeSideViewPotLinkRequests();
        var positions = session.consumeLinkFinalPositionRequests();
        RoomLinkWrites.visitPositions(platforms, pots, positions, target);
    }

    public void drainAttackWrites(LinkWriteSink.Attacks target) {
        var clears = session.consumeLinkAttackClearRequests();
        var steps = session.consumeLinkAttackStepRequests();
        RoomLinkWrites.visitAttacks(clears, steps, target);
    }

    public List<LinkWrite> consumePositionWrites() {
        var platforms = session.consumeSideViewPlatformLinkRequests();
        var pots = session.consumeSideViewPotLinkRequests();
        var positions = session.consumeLinkFinalPositionRequests();
        return RoomLinkWrites.positions(platforms, pots, positions);
    }

    public List<LinkMotionBlockRequest> consumeLinkMotionBlockRequests() {
        return session.consumeLinkMotionBlockRequests();
    }

    public List<WarpLinkStateRequest> consumeWarpLinkStateRequests() {
        return session.consumeWarpLinkStateRequests();
    }

    public boolean consumeWorldLinkMotionBlockRequest() {
        return dependencies.entityBridge().consumeWorldLinkMotionBlockRequest();
    }

    public List<LinkFallenPoseRequest> consumeLinkFallenPoseRequests() {
        return session.consumeLinkFallenPoseRequests();
    }

    public List<LinkFacingRequest> consumeLinkFacingRequests() {
        return session.consumeLinkFacingRequests();
    }

    public List<LinkWrite> consumeAttackWrites() {
        return RoomLinkWrites.attacks(
                session.consumeLinkAttackClearRequests(), session.consumeLinkAttackStepRequests());
    }

    public List<LinkHeldItemPoseRequest> consumeLinkHeldItemPoseRequests() {
        return session.consumeLinkHeldItemPoseRequests();
    }

    public List<LinkSwordSpinPoseRequest> consumeLinkSwordSpinPoseRequests() {
        return session.consumeLinkSwordSpinPoseRequests();
    }

    public List<LinkSwordFinalPoseRequest> consumeLinkSwordFinalPoseRequests() {
        return session.consumeLinkSwordFinalPoseRequests();
    }

    public List<RoosterLinkStateRequest> consumeRoosterLinkStateRequests() {
        return session.consumeRoosterLinkStateRequests();
    }

    public List<HinoxLinkEffectRequest> consumeHinoxLinkEffectRequests() {
        return session.consumeHinoxLinkEffectRequests();
    }

    record Dependencies(RoomEntityBridge entityBridge) {
        Dependencies {
            java.util.Objects.requireNonNull(entityBridge, "entityBridge");
        }
    }
}
