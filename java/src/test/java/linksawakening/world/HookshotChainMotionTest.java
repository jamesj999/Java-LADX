package linksawakening.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HookshotChainMotionTest {

    @Test
    void fireHookshotUsesRomDirectionSpeedTables() {
        assertEquals(0x30, HookshotChainMotion.speedXForDirection(0));
        assertEquals(0xD0, HookshotChainMotion.speedXForDirection(1));
        assertEquals(0x00, HookshotChainMotion.speedXForDirection(2));
        assertEquals(0x00, HookshotChainMotion.speedXForDirection(3));
        assertEquals(0x00, HookshotChainMotion.speedYForDirection(0));
        assertEquals(0x00, HookshotChainMotion.speedYForDirection(1));
        assertEquals(0xD0, HookshotChainMotion.speedYForDirection(2));
        assertEquals(0x30, HookshotChainMotion.speedYForDirection(3));
    }

    @Test
    void spawnCopiesLinkPositionAndAddsOneToLinkZ() {
        HookshotChainMotion.State state = HookshotChainMotion.spawn(0x40, 0x50, 0x07, 3);

        assertEquals(0x40, state.x());
        assertEquals(0x50, state.y());
        assertEquals(0x08, state.z());
        assertEquals(3, state.direction());
        assertEquals(0x30, state.speedY());
        assertEquals(0x2A, state.transitionCountdown());
    }

    @Test
    void outboundStepUsesSignedFourBitFixedPointSpeedAndDecrementsCountdown() {
        HookshotChainMotion.State state = HookshotChainMotion.spawn(0x40, 0x50, 0, 0);

        HookshotChainMotion.Step step = HookshotChainMotion.advance(
            state, 0x40, 0x50, false);

        assertEquals(0x43, step.state().x());
        assertEquals(0x50, step.state().y());
        assertEquals(0x29, step.state().transitionCountdown());
        assertFalse(step.collided());
        assertFalse(step.reachedLink());
        assertFalse(step.returning());
    }

    @Test
    void blockedOutboundStepReportsCollisionWithoutAdvancingThroughTheWall() {
        HookshotChainMotion.State state = HookshotChainMotion.spawn(0x40, 0x50, 0, 0);

        HookshotChainMotion.Step step = HookshotChainMotion.advance(
            state, 0x40, 0x50, true);

        assertEquals(0x40, step.state().x());
        assertTrue(step.collided());
        assertFalse(step.reachedLink());
    }

    @Test
    void linkCollisionUsesTheEntityVisualYAndRomSmallHitbox() {
        HookshotChainMotion.State state = new HookshotChainMotion.State(
            0x40, 0x51, 0x01, 0, 0, 0, 0, 0, 0);

        assertTrue(HookshotChainMotion.overlapsLink(state, 0x40, 0x50));
        assertFalse(HookshotChainMotion.overlapsLink(state, 0x40, 0x5A));
    }

    @Test
    void countdownBoundarySwitchesToRomReturnVector() {
        HookshotChainMotion.State state = HookshotChainMotion.spawn(0x40, 0x50, 0, 0);
        HookshotChainMotion.Step step = null;

        for (int frame = 0; frame < 0x2A; frame++) {
            step = HookshotChainMotion.advance(state, 0x40, 0x50, false);
            state = step.state();
        }

        assertTrue(step.returning());
        assertEquals(0, state.transitionCountdown());
        assertEquals(0xD0, state.speedX());
    }

    @Test
    void hookshotablePointBlankGuardUsesTheRomTwentySixBoundary() {
        assertTrue(HookshotChainMotion.shouldUnloadForHookshotable(0x26));
        assertFalse(HookshotChainMotion.shouldUnloadForHookshotable(0x25));
    }

    @Test
    void backgroundStateCanLatchPullingOrDeferAWallPoke() {
        HookshotChainMotion.State state = HookshotChainMotion.spawn(0x40, 0x50, 0, 0);

        HookshotChainMotion.State pulling = HookshotChainMotion.enterPulling(state);
        assertEquals(HookshotChainMotion.PULLING_STATE, pulling.entityState());
        assertFalse(pulling.wallCollisionPending());

        HookshotChainMotion.State pending = HookshotChainMotion.deferWallPoke(state);
        assertTrue(pending.wallCollisionPending());
        HookshotChainMotion.State poked = HookshotChainMotion.completeWallPoke(pending);
        assertEquals(0, poked.transitionCountdown());
        assertFalse(poked.wallCollisionPending());
    }

    @Test
    void pullSpeedNegatesTheZAwareVectorFromChainToLink() {
        HookshotChainMotion.State state = new HookshotChainMotion.State(
            0x50, 0x50, 0x08, 0, 0, 0, 0, 0, 0,
            HookshotChainMotion.PULLING_STATE, false);

        HookshotChainMotion.PullSpeed speed = HookshotChainMotion.pullLinkSpeed(
            state, 0x40, 0x50);

        assertEquals(0x30, speed.speedX());
        assertEquals(0xE8, speed.speedY());
    }

    @Test
    void pullSpeedIsZeroWhenTheRomPatchFindsTheChainAtLinkPosition() {
        HookshotChainMotion.State state = new HookshotChainMotion.State(
            0x40, 0x50, 0x08, 0, 0, 0, 0, 0, 0,
            HookshotChainMotion.PULLING_STATE, false);

        HookshotChainMotion.PullSpeed speed = HookshotChainMotion.pullLinkSpeed(
            state, 0x40, 0x50);

        assertEquals(0, speed.speedX());
        assertEquals(0, speed.speedY());
    }

    @Test
    void pullEventCarriesUnsignedRomSpeedBytesWithoutCollisionIgnore() {
        EntityProjectileEvent event = EntityProjectileEvent.hookshotPull(3, 0x30, 0xE8);

        assertEquals(EntityProjectileEvent.Kind.HOOKSHOT_PULL, event.kind());
        assertEquals(0x03, event.type());
        assertEquals(0x30, event.linkSpeedX());
        assertEquals(0xE8, event.linkSpeedY());
        assertEquals(0, event.linkIgnoreCollisionCountdown());
        assertEquals(EntityProjectileEvent.SoundChannel.NONE, event.soundChannel());
        assertEquals(-1, event.soundId());
    }
}
