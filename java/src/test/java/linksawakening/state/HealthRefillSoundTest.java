package linksawakening.state;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** UpdateHealth.increaseHealth/incrementHealth, $02:6342-6382. */
final class HealthRefillSoundTest {
    @Test
    void heartPickupChimesAtSixEighthsOnOddFramesOnly() {
        var state = new PlayerState();
        state.vitals().setHealth(8);
        state.playerRewards().applyEntityPickup(0x2D);
        assertEquals(List.of(11), refillFrames(state, 32));
        assertEquals(16, state.vitals().health());
        assertEquals(0, state.vitals().addHealthBuffer());
    }

    @Test
    void fairyChimesOncePerHeartDuringSixHeartRefill() {
        var state = new PlayerState();
        state.vitals().setMaxHearts(10);
        state.vitals().setHealth(8);
        state.playerRewards().applyEntityPickup(0x2F);
        assertEquals(List.of(11, 27, 43, 59, 75, 91), refillFrames(state, 100));
        assertEquals(56, state.vitals().health());
    }

    @Test
    void medicineUsesTheSameRefillCadenceAndStopsAtCapacity() {
        var state = new PlayerState();
        state.vitals().setHealth(1);
        state.progression().setMedicineCount(1);
        state.progression().setHasMedicine(true);
        state.vitals().applyRomEnemyDamage(1);
        state.playerResources().tickResourceBuffers(1);
        state.playerSounds().takeResourceSound();
        assertEquals(8, state.vitals().health());
        var frames = new ArrayList<Integer>();
        for (int frame = 2; frame < 100; frame++) {
            state.playerResources().tickResourceBuffers(frame);
            if (state.playerSounds().takeResourceSound() == ResourceSound.HEART_PICKED_UP) frames.add(frame);
        }
        assertEquals(List.of(13, 29), frames);
        assertEquals(24, state.vitals().health());
        assertEquals(0, state.vitals().addHealthBuffer());
    }

    @Test
    void refillAtFullHealthAndHealthSubtractionDoNotChime() {
        var state = new PlayerState();
        state.vitals().setHealth(24);
        state.playerRewards().applyEntityPickup(0x2D);
        assertEquals(List.of(), refillFrames(state, 20));
        state.vitals().applyRomEnemyDamage(8);
        assertEquals(List.of(), refillFrames(state, 20));
        assertEquals(16, state.vitals().health());
    }

    private static List<Integer> refillFrames(PlayerState state, int count) {
        var frames = new ArrayList<Integer>();
        for (int frame = 0; frame < count; frame++) {
            state.playerResources().tickResourceBuffers(frame);
            if (state.playerSounds().takeResourceSound() == ResourceSound.HEART_PICKED_UP) frames.add(frame);
        }
        return frames;
    }
}
