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
            if (event == null
                || event.soundChannel() != EntityCombatEvent.SoundChannel.JINGLE) {
                continue;
            }
            switch (event.soundId()) {
                case JINGLE_BUMP_ID -> soundSink.play(GameplaySoundEvent.ENEMY_BUMP);
                case JINGLE_ENEMY_HIT_ID -> soundSink.play(GameplaySoundEvent.ENEMY_HIT);
                default -> {
                    // Unknown ROM sound writes must not be guessed or routed
                    // to an unrelated gameplay effect.
                }
            }
        }
    }
}
