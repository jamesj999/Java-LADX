package linksawakening.equipment;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ShieldTest {

    @Test
    void pressedShieldPlaysTheRomDrawNoise() {
        RecordingSoundSink sounds = new RecordingSoundSink();

        new Shield(sounds).onPress();

        assertEquals(List.of(GameplaySoundEvent.SHIELD_DRAW), sounds.events);
    }

    @Test
    void blockedShieldUseDoesNotPlayTheDrawNoise() {
        RecordingSoundSink sounds = new RecordingSoundSink();

        new Shield(sounds, () -> false).onPress();

        assertEquals(List.of(), sounds.events);
    }

    private static final class RecordingSoundSink implements GameplaySoundSink {
        private final List<GameplaySoundEvent> events = new ArrayList<>();

        @Override
        public void play(GameplaySoundEvent event) {
            events.add(event);
        }
    }
}
