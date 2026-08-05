package linksawakening.gameplay;

import linksawakening.world.EntityCombatEvent;

import java.util.List;
import java.util.Objects;

/** Applies raw enemy-combat sound writes at the gameplay/audio boundary. */
public final class EnemyCombatEventConsumer {
    private static final int JINGLE_BUMP_ID = 0x09;
    private static final int JINGLE_ENEMY_HIT_ID = 0x03;

    private EnemyCombatEventConsumer() {
    }

    public static void consume(List<EntityCombatEvent> events, GameplaySoundSink soundSink) {
        Objects.requireNonNull(events, "events");
        Objects.requireNonNull(soundSink, "soundSink");

        for (EntityCombatEvent event : events) {
            if (event == null) {
                continue;
            }
            consumeSoundWrite(event.soundChannel(), event.soundId(), soundSink);
            consumeSoundWrite(event.secondarySoundChannel(), event.secondarySoundId(), soundSink);
        }
    }

    private static void consumeSoundWrite(EntityCombatEvent.SoundChannel channel, int id,
                                          GameplaySoundSink soundSink) {
        if (channel == null || channel == EntityCombatEvent.SoundChannel.NONE || id < 0) {
            return;
        }
        if (channel == EntityCombatEvent.SoundChannel.JINGLE) {
            switch (id) {
                case JINGLE_BUMP_ID -> soundSink.play(GameplaySoundEvent.ENEMY_BUMP);
                case JINGLE_ENEMY_HIT_ID -> soundSink.play(GameplaySoundEvent.ENEMY_HIT);
                default -> {
                    // Unknown ROM sound writes must not be guessed or routed
                    // to an unrelated gameplay effect.
                }
            }
        } else if (channel == EntityCombatEvent.SoundChannel.NOISE && id == 0x12) {
            soundSink.play(GameplaySoundEvent.ENEMY_BURNING);
        }
    }
}
