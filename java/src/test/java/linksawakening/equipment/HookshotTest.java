package linksawakening.equipment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HookshotTest {

    @Test
    void constructorRejectsNullLaunchTarget() {
        assertThrows(NullPointerException.class, () -> new Hookshot(null));
    }

    @Test
    void pressRequestsOneLaunchAndReportsTargetMotionLock() {
        RecordingTarget target = new RecordingTarget();
        Hookshot hookshot = new Hookshot(target);

        hookshot.onPress();

        assertEquals(1, target.launchRequests);
        assertFalse(hookshot.blocksMotion());
        assertFalse(hookshot.locksFacing());

        target.active = true;

        assertTrue(hookshot.blocksMotion());
        assertTrue(hookshot.locksFacing());
    }

    private static final class RecordingTarget implements Hookshot.LaunchTarget {
        private int launchRequests;
        private boolean active;

        @Override
        public boolean fireHookshot() {
            launchRequests++;
            return true;
        }

        @Override
        public boolean hookshotActive() {
            return active;
        }
    }
}
