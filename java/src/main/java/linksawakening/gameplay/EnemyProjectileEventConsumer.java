package linksawakening.gameplay;

import linksawakening.state.PlayerState;
import linksawakening.world.EntityProjectileEvent;

import java.util.List;
import java.util.Objects;

/** Applies one frame of ROM enemy-projectile events at the gameplay boundary. */
public final class EnemyProjectileEventConsumer {
    private static final int SHIELD_TING_ID = 0x16;
    private static final int ENEMY_BUMP_ID = 0x09;
    private static final int LINK_HURT_ID = 0x03;
    private static final int LINK_INVINCIBILITY_FRAMES = 0x50;

    private EnemyProjectileEventConsumer() {
    }

    public static void consume(List<EntityProjectileEvent> events,
                               PlayerState playerState,
                               GameplaySoundSink soundSink) {
        Objects.requireNonNull(events, "events");
        Objects.requireNonNull(playerState, "playerState");
        Objects.requireNonNull(soundSink, "soundSink");

        for (EntityProjectileEvent event : events) {
            if (event == null) {
                continue;
            }
            if (event.kind() == EntityProjectileEvent.Kind.SHIELD_BLOCK) {
                if (event.soundChannel() == EntityProjectileEvent.SoundChannel.JINGLE
                    && event.soundId() == 0x07) {
                    soundSink.play(GameplaySoundEvent.SWORD_POKE);
                } else {
                    playShieldSound(event, soundSink);
                }
                continue;
            }
            if (event.kind() == EntityProjectileEvent.Kind.SWORD_HIT) {
                if (event.soundChannel() == EntityProjectileEvent.SoundChannel.JINGLE
                    && event.soundId() == ENEMY_BUMP_ID) {
                    soundSink.play(GameplaySoundEvent.ENEMY_BUMP);
                }
                continue;
            }
            if (event.kind() != EntityProjectileEvent.Kind.LINK_DAMAGE
                || event.linkDamage() <= 0
                || playerState.invincibilityCounter() != 0) {
                continue;
            }

            playHurtSound(event, soundSink);
            playerState.damage(event.linkDamage());
            playerState.setInvincibilityCounter(LINK_INVINCIBILITY_FRAMES);
        }
    }

    private static void playShieldSound(EntityProjectileEvent event,
                                        GameplaySoundSink soundSink) {
        if (event.soundChannel() == EntityProjectileEvent.SoundChannel.JINGLE
            && event.soundId() == SHIELD_TING_ID) {
            soundSink.play(GameplaySoundEvent.SHIELD_TING);
        }
    }

    private static void playHurtSound(EntityProjectileEvent event,
                                      GameplaySoundSink soundSink) {
        if (event.soundChannel() == EntityProjectileEvent.SoundChannel.WAVE
            && event.soundId() == LINK_HURT_ID) {
            soundSink.play(GameplaySoundEvent.LINK_HURT);
        }
    }
}
