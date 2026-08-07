package linksawakening.equipment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BoomerangTest {

    @Test
    void constructorRejectsNullLaunchTarget() {
        assertThrows(NullPointerException.class, () -> new Boomerang(null));
    }

    @Test
    void pressRequestsOneLaunchAndUsesTheRuntimeMotionLock() {
        RecordingTarget target = new RecordingTarget();
        Boomerang boomerang = new Boomerang(target);

        boomerang.onPress();

        assertEquals(1, target.launchRequests);
        assertFalse(boomerang.blocksMotion());
        assertFalse(boomerang.locksFacing());

        target.active = true;

        assertTrue(boomerang.blocksMotion());
        assertTrue(boomerang.locksFacing());
    }

    @Test
    void itemUseGateRejectsThePressBeforeTheRoomBridge() {
        RecordingTarget target = new RecordingTarget();
        Boomerang boomerang = new Boomerang(target, () -> false);

        boomerang.onPress();

        assertEquals(0, target.launchRequests);
    }

    private static final class RecordingTarget implements Boomerang.LaunchTarget {
        private int launchRequests;
        private boolean active;

        @Override
        public boolean fireBoomerang() {
            launchRequests++;
            return true;
        }

        @Override
        public boolean boomerangActive() {
            return active;
        }
    }
}
