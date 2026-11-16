package linksawakening.world.room;

import java.util.List;
import java.util.Objects;
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
    private final RoomEntityOutputSource source;
    private final Dependencies dependencies;

    LinkOutputs(Dependencies dependencies, RoomEntityOutputSource source) {
        this.dependencies = dependencies;
        this.source = Objects.requireNonNull(source);
    }

    public void drainPositionWrites(LinkWriteSink.Positions target) {
        var platforms = source.consumeSideViewPlatformLinkRequests();
        var pots = source.consumeSideViewPotLinkRequests();
        var positions = source.consumeLinkFinalPositionRequests();
        RoomLinkWrites.visitPositions(platforms, pots, positions, target);
    }

    public void drainAttackWrites(LinkWriteSink.Attacks target) {
        var clears = source.consumeLinkAttackClearRequests();
        var steps = source.consumeLinkAttackStepRequests();
        RoomLinkWrites.visitAttacks(clears, steps, target);
    }

    public List<LinkWrite> consumePositionWrites() {
        var platforms = source.consumeSideViewPlatformLinkRequests();
        var pots = source.consumeSideViewPotLinkRequests();
        var positions = source.consumeLinkFinalPositionRequests();
        return RoomLinkWrites.positions(platforms, pots, positions);
    }

    public List<LinkMotionBlockRequest> consumeLinkMotionBlockRequests() {
        return source.consumeLinkMotionBlockRequests();
    }

    public List<WarpLinkStateRequest> consumeWarpLinkStateRequests() {
        return source.consumeWarpLinkStateRequests();
    }

    public boolean consumeWorldLinkMotionBlockRequest() {
        return dependencies.entityBridge().consumeWorldLinkMotionBlockRequest();
    }

    public List<LinkFallenPoseRequest> consumeLinkFallenPoseRequests() {
        return source.consumeLinkFallenPoseRequests();
    }

    public List<LinkFacingRequest> consumeLinkFacingRequests() {
        return source.consumeLinkFacingRequests();
    }

    public List<LinkWrite> consumeAttackWrites() {
        return RoomLinkWrites.attacks(source.consumeLinkAttackClearRequests(), source.consumeLinkAttackStepRequests());
    }

    public List<LinkHeldItemPoseRequest> consumeLinkHeldItemPoseRequests() {
        return source.consumeLinkHeldItemPoseRequests();
    }

    public List<LinkSwordSpinPoseRequest> consumeLinkSwordSpinPoseRequests() {
        return source.consumeLinkSwordSpinPoseRequests();
    }

    public List<LinkSwordFinalPoseRequest> consumeLinkSwordFinalPoseRequests() {
        return source.consumeLinkSwordFinalPoseRequests();
    }

    public List<RoosterLinkStateRequest> consumeRoosterLinkStateRequests() {
        return source.consumeRoosterLinkStateRequests();
    }

    public List<HinoxLinkEffectRequest> consumeHinoxLinkEffectRequests() {
        return source.consumeHinoxLinkEffectRequests();
    }

    record Dependencies(RoomEntityBridge entityBridge) {
        Dependencies {
            Objects.requireNonNull(entityBridge, "entityBridge");
        }
    }
}
