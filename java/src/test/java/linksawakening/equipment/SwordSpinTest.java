package linksawakening.equipment;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.state.Direction;
import org.junit.jupiter.api.Test;

class SwordSpinTest {
    @Test
    void queuedReleaseWaitsOneFrameThenUsesTheTwentyFiveFrameCadence() {
        var spin = new SwordSpin(null);
        spin.queue(Direction.DOWN.javaIndex());
        assertTrue(spin.queued());
        assertFalse(spin.active());
        assertEquals(Sword.STATE_HOLDING, spin.advance(Sword.STATE_HOLDING));
        assertTrue(spin.queued());
        int state = Sword.STATE_HOLDING;
        int visibleFrames = 0;
        do {
            state = spin.advance(state);
            if (spin.active()) visibleFrames++;
        } while (spin.active());
        assertEquals(25, visibleFrames);
        assertEquals(Sword.STATE_NONE, state);
        assertFalse(spin.queued());
    }

    @Test
    void acquisitionValidationPrecedesMutationAndInactiveDirectionFlowsThrough() {
        var spin = new SwordSpin(null);
        assertEquals(99, spin.effectiveDirection(99));
        spin.startAcquisition(Direction.RIGHT.javaIndex());
        int direction = spin.effectiveDirection(99);
        int animation = spin.animationState();
        assertThrows(IllegalArgumentException.class, () -> spin.startAcquisition(99));
        assertTrue(spin.active());
        assertEquals(direction, spin.effectiveDirection(99));
        assertEquals(animation, spin.animationState());
    }
}
