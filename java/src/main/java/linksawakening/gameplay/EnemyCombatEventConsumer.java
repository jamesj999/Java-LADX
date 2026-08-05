package linksawakening.gameplay;

import linksawakening.world.EntityCombatEvent;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;

import java.util.List;
import java.util.Objects;

/** Applies raw enemy-combat sound writes at the gameplay/audio boundary. */
public final class EnemyCombatEventConsumer {
    private static final int JINGLE_BUMP_ID = 0x09;
    private static final int JINGLE_ENEMY_HIT_ID = 0x03;
    private static final int JINGLE_SWORD_POKE_ID = 0x07;
    private static final int NOISE_ENEMY_BURNING_ID = 0x12;
    private static final int NOISE_ENEMY_DESTROYED_ID = 0x13;
    private static final int NOISE_BEAMOS_LASER_ID = 0x08;

    private EnemyCombatEventConsumer() {
    }

    public static void consume(List<EntityCombatEvent> events, GameplaySoundSink soundSink) {
        consume(events, soundSink, null);
    }

    public static void consume(List<EntityCombatEvent> events, GameplaySoundSink soundSink,
                               TransientVfxSystem transientVfxSystem) {
        Objects.requireNonNull(events, "events");
        Objects.requireNonNull(soundSink, "soundSink");

        for (EntityCombatEvent event : events) {
            if (event == null) {
                continue;
            }
            consumeSoundWrite(event.soundChannel(), event.soundId(), soundSink);
            consumeSoundWrite(event.secondarySoundChannel(), event.secondarySoundId(), soundSink);
            if (transientVfxSystem != null && event.swordPokeVfx() != null) {
                transientVfxSystem.spawn(TransientVfxType.SWORD_POKE,
                    event.swordPokeVfx().worldX(), event.swordPokeVfx().worldY());
            }
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
                case JINGLE_SWORD_POKE_ID -> soundSink.play(GameplaySoundEvent.SWORD_POKE);
                default -> {
                    // Unknown ROM sound writes must not be guessed or routed
                    // to an unrelated gameplay effect.
                }
            }
        } else if (channel == EntityCombatEvent.SoundChannel.NOISE) {
            switch (id) {
                case NOISE_ENEMY_BURNING_ID -> soundSink.play(GameplaySoundEvent.ENEMY_BURNING);
                case NOISE_ENEMY_DESTROYED_ID -> soundSink.play(GameplaySoundEvent.ENEMY_DESTROYED);
                case NOISE_BEAMOS_LASER_ID -> soundSink.play(GameplaySoundEvent.BEAMOS_LASER);
                default -> {
                    // Unknown ROM sound writes must not be guessed or routed
                    // to an unrelated gameplay effect.
                }
            }
        }
    }
}
