package linksawakening.gameplay;

import linksawakening.world.EntityCombatEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class EnemyCombatEventConsumerTest {

    @Test
    void mapsRomBumpAndEnemyHitJinglesToGameplaySounds() {
        RecordingSoundSink sounds = new RecordingSoundSink();
        List<EntityCombatEvent> events = List.of(
            new EntityCombatEvent(0, 0x0B, 0, true,
                EntityCombatEvent.SoundChannel.JINGLE, 0x09),
            new EntityCombatEvent(1, 0x09, 0, true,
                EntityCombatEvent.SoundChannel.JINGLE, 0x03));

        EnemyCombatEventConsumer.consume(events, sounds);

        assertEquals(List.of(GameplaySoundEvent.ENEMY_BUMP, GameplaySoundEvent.ENEMY_HIT),
            sounds.events);
    }

    @Test
    void ignoresUnknownChannelsIdsAndNoSoundEvents() {
        RecordingSoundSink sounds = new RecordingSoundSink();
        List<EntityCombatEvent> events = List.of(
            new EntityCombatEvent(0, 0x0B, 0, true),
            new EntityCombatEvent(1, 0x09, 0, true,
                EntityCombatEvent.SoundChannel.JINGLE, 0x7F),
            new EntityCombatEvent(2, 0x09, 0, true,
                EntityCombatEvent.SoundChannel.WAVE, 0x03));

        EnemyCombatEventConsumer.consume(events, sounds);

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
