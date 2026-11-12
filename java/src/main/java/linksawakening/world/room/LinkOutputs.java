package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

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
    private final RoomSession session;

    LinkOutputs(RoomSession session) {
        this.session = session;
    }

    public List<LinkWrite> consumePositionWrites() {
        return session.consumePositionWrites();
    }

    public List<LinkMotionBlockRequest> consumeLinkMotionBlockRequests() {
        return session.context.entityBridge.outbox.consumeLinkMotionBlockRequests();
    }

    public List<WarpLinkStateRequest> consumeWarpLinkStateRequests() {
        return session.context.entityBridge.consumeWarpLinkStateRequests();
    }

    public boolean consumeWorldLinkMotionBlockRequest() {
        return session.context.entityBridge.consumeWorldLinkMotionBlockRequest();
    }

    public List<LinkFallenPoseRequest> consumeLinkFallenPoseRequests() {
        return session.context.entityBridge.outbox.consumeLinkFallenPoseRequests();
    }

    public List<LinkFacingRequest> consumeLinkFacingRequests() {
        return session.context.entityBridge.outbox.consumeLinkFacingRequests();
    }

    public List<LinkWrite> consumeAttackWrites() {
        return session.consumeAttackWrites();
    }

    public List<LinkHeldItemPoseRequest> consumeLinkHeldItemPoseRequests() {
        return session.context.entityBridge.outbox.consumeLinkHeldItemPoseRequests();
    }

    public List<LinkSwordSpinPoseRequest> consumeLinkSwordSpinPoseRequests() {
        return session.context.entityBridge.outbox.consumeLinkSwordSpinPoseRequests();
    }

    public List<LinkSwordFinalPoseRequest> consumeLinkSwordFinalPoseRequests() {
        return session.context.entityBridge.outbox.consumeLinkSwordFinalPoseRequests();
    }

    public List<RoosterLinkStateRequest> consumeRoosterLinkStateRequests() {
        return session.context.entityBridge.outbox.consumeRoosterLinkStateRequests();
    }

    public List<HinoxLinkEffectRequest> consumeHinoxLinkEffectRequests() {
        return session.context.entityBridge.outbox.consumeHinoxLinkEffectRequests();
    }
}
