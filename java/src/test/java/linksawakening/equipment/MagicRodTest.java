package linksawakening.equipment;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MagicRodTest {

    @Test
    void pressStartsTheRomMagicRodAttackStepButDefersTheFireball() {
        RecordingTarget target = new RecordingTarget();
        RecordingSoundSink sounds = new RecordingSoundSink();
        MagicRod rod = new MagicRod(sounds, target);

        rod.onPress();

        assertEquals(1, target.attackStepRequests);
        assertEquals(0, target.fireRequests);
        assertEquals(MagicRod.ATTACK_STEP_MAGIC_ROD, rod.attackStepCountdownForTest());
        assertTrue(rod.blocksMotion());
        assertTrue(rod.locksFacing());
        assertEquals(List.of(), sounds.events);
    }

    @Test
    void sourceFiresAtTheLowByteTwelveBoundaryAndPlaysTheMagicRodNoise() {
        RecordingTarget target = new RecordingTarget();
        RecordingSoundSink sounds = new RecordingSoundSink();
        MagicRod rod = new MagicRod(sounds, target);

        rod.onPress();
        rod.tick(false, 0);

        assertEquals(0x8D, rod.attackStepCountdownForTest());
        assertEquals(0, target.fireRequests);

        rod.tick(false, 1);

        assertEquals(1, target.fireRequests);
        assertEquals(0x8C, rod.attackStepCountdownForTest());
        assertEquals(List.of(GameplaySoundEvent.MAGIC_ROD), sounds.events);
    }

    @Test
    void failedRoomAllocationDoesNotPlayTheLaunchNoise() {
        RecordingTarget target = new RecordingTarget();
        target.fireResult = false;
        RecordingSoundSink sounds = new RecordingSoundSink();
        MagicRod rod = new MagicRod(sounds, target);

        rod.onPress();
        rod.tick(false, 0);
        rod.tick(false, 1);

        assertEquals(1, target.fireRequests);
        assertEquals(List.of(), sounds.events);
    }

    @Test
    void itemUseGateAndProjectileLimitAreCheckedBeforeArming() {
        RecordingTarget target = new RecordingTarget();
        target.activeProjectileCount = 2;
        MagicRod rod = new MagicRod(GameplaySoundSink.none(), target, () -> true);

        rod.onPress();

        assertEquals(0, target.attackStepRequests);
        assertEquals(0, target.fireRequests);

        target.activeProjectileCount = 0;
        MagicRod blocked = new MagicRod(GameplaySoundSink.none(), target, () -> false);
        blocked.onPress();
        assertEquals(0, target.attackStepRequests);
    }

    @Test
    void constructorRejectsNullDependencies() {
        RecordingTarget target = new RecordingTarget();
        assertThrows(NullPointerException.class, () -> new MagicRod(null, target));
        assertThrows(NullPointerException.class,
            () -> new MagicRod(GameplaySoundSink.none(), null));
    }

    private static final class RecordingTarget implements MagicRod.LaunchTarget {
        private int attackStepRequests;
        private int fireRequests;
        private int activeProjectileCount;
        private boolean fireResult = true;

        @Override
        public void startMagicRodAttackStep() {
            attackStepRequests++;
        }

        @Override
        public boolean fireMagicRodFireball() {
            fireRequests++;
            return fireResult;
        }

        @Override
        public int activeProjectileCount() {
            return activeProjectileCount;
        }
    }

    private static final class RecordingSoundSink implements GameplaySoundSink {
        private final List<GameplaySoundEvent> events = new ArrayList<>();

        @Override
        public void play(GameplaySoundEvent event) {
            events.add(event);
        }
    }
}
