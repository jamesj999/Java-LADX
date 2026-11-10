package linksawakening.state;

import linksawakening.gameplay.GameplaySoundEvent;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** UpdateHealth.increaseHealth/incrementHealth, $02:6342-6382. */
final class HealthRefillSoundTest {
    @Test
    void heartPickupChimesAtSixEighthsOnOddFramesOnly() {
        var state = new PlayerState();
        state.setHealth(8);
        state.applyEntityPickup(0x2D);
        assertEquals(List.of(11), refillFrames(state, 32));
        assertEquals(16, state.health());
        assertEquals(0, state.addHealthBuffer());
    }

    @Test
    void fairyChimesOncePerHeartDuringSixHeartRefill() {
        var state = new PlayerState();
        state.setMaxHearts(10);
        state.setHealth(8);
        state.applyEntityPickup(0x2F);
        assertEquals(List.of(11, 27, 43, 59, 75, 91), refillFrames(state, 100));
        assertEquals(56, state.health());
    }

    @Test
    void medicineUsesTheSameRefillCadenceAndStopsAtCapacity() {
        var state = new PlayerState();
        state.setHealth(1);
        state.setMedicineCount(1);
        state.setHasMedicine(true);
        state.applyRomEnemyDamage(1);
        state.tickResourceBuffers(1);
        state.takeResourceSound();
        assertEquals(8, state.health());
        var frames = new ArrayList<Integer>();
        for (int frame = 2; frame < 100; frame++) {
            state.tickResourceBuffers(frame);
            if (state.takeResourceSound() == GameplaySoundEvent.HEART_PICKED_UP) frames.add(frame);
        }
        assertEquals(List.of(13, 29), frames);
        assertEquals(24, state.health());
        assertEquals(0, state.addHealthBuffer());
    }

    @Test
    void refillAtFullHealthAndHealthSubtractionDoNotChime() {
        var state = new PlayerState();
        state.setHealth(24);
        state.applyEntityPickup(0x2D);
        assertEquals(List.of(), refillFrames(state, 20));
        state.applyRomEnemyDamage(8);
        assertEquals(List.of(), refillFrames(state, 20));
        assertEquals(16, state.health());
    }

    private static List<Integer> refillFrames(PlayerState state, int count) {
        var frames = new ArrayList<Integer>();
        for (int frame = 0; frame < count; frame++) {
            state.tickResourceBuffers(frame);
            if (state.takeResourceSound() == GameplaySoundEvent.HEART_PICKED_UP) frames.add(frame);
        }
        return frames;
    }
}
