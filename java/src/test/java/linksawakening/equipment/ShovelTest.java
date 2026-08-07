package linksawakening.equipment;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ShovelTest {

    @Test
    void validUsePlaysDigNoiseAndKeepsLinkLockedForTheRomWindow() {
        RecordingTarget target = new RecordingTarget();
        target.startResult = new Shovel.StartResult(true, false);
        RecordingSoundSink sounds = new RecordingSoundSink();
        Shovel shovel = new Shovel(sounds, target);

        shovel.onPress();

        assertEquals(List.of(GameplaySoundEvent.SHOVEL_DIG), sounds.events);
        assertTrue(shovel.blocksMotion());
        assertTrue(shovel.locksFacing());
        assertEquals(0, shovel.timerForTest());
    }

    @Test
    void invalidUseStillStartsTheShovelButUsesTheSwordPokeJingle() {
        RecordingTarget target = new RecordingTarget();
        target.startResult = new Shovel.StartResult(true, true);
        RecordingSoundSink sounds = new RecordingSoundSink();
        Shovel shovel = new Shovel(sounds, target);

        shovel.onPress();

        assertEquals(List.of(GameplaySoundEvent.SWORD_POKE), sounds.events);
        assertTrue(shovel.blocksMotion());
    }

    @Test
    void failedStartDoesNotPlaySoundOrArmTheWindow() {
        RecordingTarget target = new RecordingTarget();
        target.startResult = new Shovel.StartResult(false, true);
        RecordingSoundSink sounds = new RecordingSoundSink();
        Shovel shovel = new Shovel(sounds, target);

        shovel.onPress();

        assertEquals(List.of(), sounds.events);
        assertFalse(shovel.blocksMotion());
        assertEquals(0, target.advanceTimers.size());
    }

    @Test
    void tickingCallsTheTargetAtEveryRomTimerAndEndsAtTwentyFour() {
        RecordingTarget target = new RecordingTarget();
        target.startResult = new Shovel.StartResult(true, false);
        Shovel shovel = new Shovel(GameplaySoundSink.none(), target);

        shovel.onPress();
        for (int i = 0; i < 24; i++) {
            shovel.tick(false, i);
        }

        assertEquals(24, target.advanceTimers.size());
        assertEquals(0x10, target.advanceTimers.get(15));
        assertEquals(0x18, target.advanceTimers.get(23));
        assertFalse(shovel.blocksMotion());
        assertEquals(0x18, shovel.timerForTest());
    }

    @Test
    void poseOverrideComesFromTheRomTargetWhileActive() {
        RecordingTarget target = new RecordingTarget();
        target.startResult = new Shovel.StartResult(true, false);
        target.animationState = 0x6D;
        Shovel shovel = new Shovel(GameplaySoundSink.none(), target);

        assertEquals(-1, shovel.overrideAnimationState(0, 0));
        shovel.onPress();
        assertEquals(0x6D, shovel.overrideAnimationState(2, 0));
    }

    private static final class RecordingTarget implements Shovel.DigTarget {
        private Shovel.StartResult startResult = new Shovel.StartResult(true, false);
        private final List<Integer> advanceTimers = new ArrayList<>();
        private int animationState = -1;

        @Override
        public Shovel.StartResult startShovel() {
            return startResult;
        }

        @Override
        public void advanceShovel(int timer) {
            advanceTimers.add(timer);
        }

        @Override
        public int shovelAnimationState(int javaDirection, int timer) {
            return animationState;
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
