package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

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
        var platforms = dependencies.entityBridge().get().outbox.consumeSideViewPlatformLinkRequests();
        var pots = dependencies.entityBridge().get().outbox.consumeSideViewPotLinkRequests();
        var positions = dependencies.entityBridge().get().outbox.consumeLinkFinalPositionRequests();
        RoomLinkWrites.visitPositions(platforms, pots, positions, target);
    }

    public void drainAttackWrites(LinkWriteSink.Attacks target) {
        var clears = dependencies.entityBridge().get().outbox.consumeLinkAttackClearRequests();
        var steps = dependencies.entityBridge().get().outbox.consumeLinkAttackStepRequests();
        RoomLinkWrites.visitAttacks(clears, steps, target);
    }

    public List<LinkWrite> consumePositionWrites() {
        var platforms = dependencies.entityBridge().get().outbox.consumeSideViewPlatformLinkRequests();
        var pots = dependencies.entityBridge().get().outbox.consumeSideViewPotLinkRequests();
        var positions = dependencies.entityBridge().get().outbox.consumeLinkFinalPositionRequests();
        return RoomLinkWrites.positions(platforms, pots, positions);
    }

    public List<LinkMotionBlockRequest> consumeLinkMotionBlockRequests() {
        return dependencies.entityBridge().get().outbox.consumeLinkMotionBlockRequests();
    }

    public List<WarpLinkStateRequest> consumeWarpLinkStateRequests() {
        return dependencies.entityBridge().get().consumeWarpLinkStateRequests();
    }

    public boolean consumeWorldLinkMotionBlockRequest() {
        return dependencies.entityBridge().get().consumeWorldLinkMotionBlockRequest();
    }

    public List<LinkFallenPoseRequest> consumeLinkFallenPoseRequests() {
        return dependencies.entityBridge().get().outbox.consumeLinkFallenPoseRequests();
    }

    public List<LinkFacingRequest> consumeLinkFacingRequests() {
        return dependencies.entityBridge().get().outbox.consumeLinkFacingRequests();
    }

    public List<LinkWrite> consumeAttackWrites() {
        return RoomLinkWrites.attacks(
                dependencies.entityBridge().get().outbox.consumeLinkAttackClearRequests(),
                dependencies.entityBridge().get().outbox.consumeLinkAttackStepRequests());
    }

    public List<LinkHeldItemPoseRequest> consumeLinkHeldItemPoseRequests() {
        return dependencies.entityBridge().get().outbox.consumeLinkHeldItemPoseRequests();
    }

    public List<LinkSwordSpinPoseRequest> consumeLinkSwordSpinPoseRequests() {
        return dependencies.entityBridge().get().outbox.consumeLinkSwordSpinPoseRequests();
    }

    public List<LinkSwordFinalPoseRequest> consumeLinkSwordFinalPoseRequests() {
        return dependencies.entityBridge().get().outbox.consumeLinkSwordFinalPoseRequests();
    }

    public List<RoosterLinkStateRequest> consumeRoosterLinkStateRequests() {
        return dependencies.entityBridge().get().outbox.consumeRoosterLinkStateRequests();
    }

    public List<HinoxLinkEffectRequest> consumeHinoxLinkEffectRequests() {
        return dependencies.entityBridge().get().outbox.consumeHinoxLinkEffectRequests();
    }

    record Dependencies(java.util.function.Supplier<RoomEntityBridge> entityBridge) {}
}
