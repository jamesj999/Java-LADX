package linksawakening.equipment;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ArrowTest {

    @Test
    void constructorRejectsNullDependencies() {
        PlayerState player = playerWithArrows(1);
        RecordingTarget target = new RecordingTarget();
        RecordingSoundSink sounds = new RecordingSoundSink();

        assertThrows(NullPointerException.class, () -> new Arrow(null, sounds, target));
        assertThrows(NullPointerException.class, () -> new Arrow(player, null, target));
        assertThrows(NullPointerException.class, () -> new Arrow(player, sounds, null));
    }

    @Test
    void activeProjectileLimitIsCheckedBeforeArrowInventory() {
        PlayerState player = playerWithArrows(0);
        RecordingTarget target = new RecordingTarget();
        target.activeProjectileCount = 2;
        RecordingSoundSink sounds = new RecordingSoundSink();
        Arrow arrow = new Arrow(player, sounds, target);

        arrow.onPress();

        assertEquals(0, player.arrowCount());
        assertEquals(0, target.shotRequests);
        assertEquals(List.of(), sounds.events);
    }

    @Test
    void zeroArrowsPlayWrongAnswerWithoutCallingTheRoom() {
        PlayerState player = playerWithArrows(0);
        RecordingTarget target = new RecordingTarget();
        RecordingSoundSink sounds = new RecordingSoundSink();
        Arrow arrow = new Arrow(player, sounds, target);

        arrow.onPress();

        assertEquals(0, player.arrowCount());
        assertEquals(0, target.shotRequests);
        assertEquals(List.of(GameplaySoundEvent.WRONG_ANSWER), sounds.events);
    }

    @Test
    void successfulShotSpendsAnArrowStartsTheRomGateAndPlaysWhoosh() {
        PlayerState player = playerWithArrows(2);
        RecordingTarget target = new RecordingTarget(player);
        RecordingSoundSink sounds = new RecordingSoundSink();
        Arrow arrow = new Arrow(player, sounds, target);

        arrow.onPress();

        assertEquals(1, player.arrowCount());
        assertEquals(1, target.shotRequests);
        assertEquals(1, target.countObservedAtShot);
        assertEquals(0x10, arrow.shootingCountdownForTest());
        assertEquals(List.of(GameplaySoundEvent.ARROW_SHOT), sounds.events);
    }

    @Test
    void failedSpawnStillConsumesTheArrowAfterTheItemGate() {
        PlayerState player = playerWithArrows(2);
        RecordingTarget target = new RecordingTarget(player);
        target.shotSucceeds = false;
        Arrow arrow = new Arrow(player, GameplaySoundSink.none(), target);

        arrow.onPress();

        assertEquals(1, player.arrowCount());
        assertEquals(1, target.shotRequests);
        assertEquals(1, target.countObservedAtShot);
    }

    private static PlayerState playerWithArrows(int count) {
        PlayerState player = new PlayerState();
        player.setMaxArrows(99);
        player.setArrowCount(count);
        return player;
    }

    private static final class RecordingTarget implements Arrow.ShootTarget {
        private final PlayerState player;
        private int activeProjectileCount;
        private int shotRequests;
        private int countObservedAtShot = -1;
        private boolean shotSucceeds = true;

        private RecordingTarget() {
            this(null);
        }

        private RecordingTarget(PlayerState player) {
            this.player = player;
        }

        @Override
        public boolean shootArrow() {
            shotRequests++;
            countObservedAtShot = player == null ? -1 : player.arrowCount();
            return shotSucceeds;
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
