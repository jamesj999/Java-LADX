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
    private final Dependencies dependencies;

    LinkOutputs(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    public void drainPositionWrites(LinkWriteSink.Positions target) {
        var platforms = dependencies.entityBridge().outbox.consumeSideViewPlatformLinkRequests();
        var pots = dependencies.entityBridge().outbox.consumeSideViewPotLinkRequests();
        var positions = dependencies.entityBridge().outbox.consumeLinkFinalPositionRequests();
        RoomLinkWrites.visitPositions(platforms, pots, positions, target);
    }

    public void drainAttackWrites(LinkWriteSink.Attacks target) {
        var clears = dependencies.entityBridge().outbox.consumeLinkAttackClearRequests();
        var steps = dependencies.entityBridge().outbox.consumeLinkAttackStepRequests();
        RoomLinkWrites.visitAttacks(clears, steps, target);
    }

    public List<LinkWrite> consumePositionWrites() {
        var platforms = dependencies.entityBridge().outbox.consumeSideViewPlatformLinkRequests();
        var pots = dependencies.entityBridge().outbox.consumeSideViewPotLinkRequests();
        var positions = dependencies.entityBridge().outbox.consumeLinkFinalPositionRequests();
        return RoomLinkWrites.positions(platforms, pots, positions);
    }

    public List<LinkMotionBlockRequest> consumeLinkMotionBlockRequests() {
        return dependencies.entityBridge().outbox.consumeLinkMotionBlockRequests();
    }

    public List<WarpLinkStateRequest> consumeWarpLinkStateRequests() {
        return dependencies.entityBridge().consumeWarpLinkStateRequests();
    }

    public boolean consumeWorldLinkMotionBlockRequest() {
        return dependencies.entityBridge().consumeWorldLinkMotionBlockRequest();
    }

    public List<LinkFallenPoseRequest> consumeLinkFallenPoseRequests() {
        return dependencies.entityBridge().outbox.consumeLinkFallenPoseRequests();
    }

    public List<LinkFacingRequest> consumeLinkFacingRequests() {
        return dependencies.entityBridge().outbox.consumeLinkFacingRequests();
    }

    public List<LinkWrite> consumeAttackWrites() {
        return RoomLinkWrites.attacks(
                dependencies.entityBridge().outbox.consumeLinkAttackClearRequests(),
                dependencies.entityBridge().outbox.consumeLinkAttackStepRequests());
    }

    public List<LinkHeldItemPoseRequest> consumeLinkHeldItemPoseRequests() {
        return dependencies.entityBridge().outbox.consumeLinkHeldItemPoseRequests();
    }

    public List<LinkSwordSpinPoseRequest> consumeLinkSwordSpinPoseRequests() {
        return dependencies.entityBridge().outbox.consumeLinkSwordSpinPoseRequests();
    }

    public List<LinkSwordFinalPoseRequest> consumeLinkSwordFinalPoseRequests() {
        return dependencies.entityBridge().outbox.consumeLinkSwordFinalPoseRequests();
    }

    public List<RoosterLinkStateRequest> consumeRoosterLinkStateRequests() {
        return dependencies.entityBridge().outbox.consumeRoosterLinkStateRequests();
    }

    public List<HinoxLinkEffectRequest> consumeHinoxLinkEffectRequests() {
        return dependencies.entityBridge().outbox.consumeHinoxLinkEffectRequests();
    }

    record Dependencies(RoomEntityBridge entityBridge) {
        Dependencies {
            java.util.Objects.requireNonNull(entityBridge, "entityBridge");
        }
    }
}
