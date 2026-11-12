package linksawakening.app.gameplay;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.entity.Link;
import linksawakening.entity.LinkCommand;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.HinoxLinkEffectRequest;
import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.LinkFinalPositionRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.RoosterLinkStateRequest;
import linksawakening.world.entity.event.SideViewPotLinkRequest;
import org.junit.jupiter.api.Test;

final class LinkCommandAdapterTest {
    @Test
    void deferredContactCapturesThePostMovementVectorBeforeTheNextLinkStep() {
        Link link = link();
        link.applyRomSpeed(0xEC, 0xEC);
        link.setCollisionIgnoreFrames(0x10);
        var event = new EntityCombatEvent(
                2,
                0x0E,
                0,
                false,
                0,
                -1,
                EntityCombatEvent.SoundChannel.NONE,
                -1,
                EntityCombatEvent.SoundChannel.NONE,
                -1,
                null,
                EntityCombatEvent.LinkAction.NONE,
                new EntityCombatEvent.LinkCollisionResponse(0, 0xEC, 0x10));
        link.apply(LinkCommandAdapter.from(event));
        link.update();
        assertEquals(64, link.pixelX());
        assertEquals(62, link.pixelY());
        assertEquals(0, link.romSpeedX());
        assertEquals(0xEC, link.romSpeedY());
    }

    private Link link() {
        Link link = new Link(null, null, null, null, null, null, null);
        link.setPixelPosition(64, 64);
        return link;
    }

    @Test
    void motionBlockRetainsRomByteWrappingForImmediatePositionWrites() {
        Link link = link();
        link.apply(LinkCommandAdapter.from(new LinkMotionBlockRequest(0, -1, -2)));
        assertEquals(63, link.pixelX());
        assertEquals(62, link.pixelY());
        link.apply(LinkCommandAdapter.from(new LinkMotionBlockRequest(0, 16, 16)));
        assertEquals(63, link.pixelX());
        assertEquals(62, link.pixelY());
    }

    @Test
    void potWritesSpeedBeforeCapturingRecoilAndRestoresOnlyX() {
        Link link = link();
        link.captureRomFinalPosition();
        link.setPixelPosition(80, 80);
        link.apply(LinkCommandAdapter.from(new SideViewPotLinkRequest(0, false, true, 2, 0xF0, false, 0, 0)));
        assertEquals(64, link.pixelX());
        assertEquals(80, link.pixelY());
        link.update();
        assertEquals(63, link.pixelX());
        assertEquals(80, link.pixelY());
        assertEquals(0xF0, link.romSpeedX());
    }

    @Test
    void finalPositionRestoresBothAxesAndCanClearPendingSpeed() {
        Link link = link();
        link.captureRomFinalPosition();
        link.setPixelPosition(80, 80);
        link.applyRomSpeed(16, 16);
        link.apply(LinkCommandAdapter.from(new LinkFinalPositionRequest(0, true, false, true, true)));
        assertEquals(64, link.pixelX());
        assertEquals(64, link.pixelY());
        assertEquals(0, link.romSpeedX());
        assertTrue(link.isRomLinkPushing());
    }

    @Test
    void facingAndActorRequestsRetainTheirIndependentFlags() {
        assertEquals(new LinkCommand.Facing(2, true), LinkCommandAdapter.from(new LinkFacingRequest(0, 2, true)));
        assertEquals(
                new LinkCommand.HinoxGrab(1, 2, 3, 4, 5, 6, 2, true, false),
                LinkCommandAdapter.from(new HinoxLinkEffectRequest(0, 1, 2, 3, 4, 5, 6, 2, 8, true, false)));
        assertEquals(
                new LinkCommand.RoosterFlight(1, 2, 3, 4, 2),
                LinkCommandAdapter.from(new RoosterLinkStateRequest(0, 1, 2, 3, 4, 2)));
    }
}
