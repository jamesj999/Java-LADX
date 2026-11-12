package linksawakening.gameplay;

import java.util.List;
import java.util.Objects;
import linksawakening.entity.Link;
import linksawakening.state.PlayerState;
import linksawakening.state.PowerUp;
import linksawakening.world.entity.event.EntityProjectileEvent;

/** Applies one frame of ROM enemy-projectile events at the gameplay boundary. */
public final class EnemyProjectileEventConsumer {
    private static final int SHIELD_TING_ID = 0x16;
    private static final int ENEMY_BUMP_ID = 0x09;
    private static final int LINK_HURT_ID = 0x03;

    private EnemyProjectileEventConsumer() {}

    public static void consume(
            List<EntityProjectileEvent> events, PlayerState playerState, GameplaySoundSink soundSink) {
        consume(events, playerState, null, soundSink);
    }

    /** Applies projectile damage and the accepted hit's Link motion response. */
    public static void consume(
            List<EntityProjectileEvent> events, PlayerState playerState, Link link, GameplaySoundSink soundSink) {
        consume(events, playerState, link, soundSink, () -> {});
    }

    public static void consume(
            List<EntityProjectileEvent> events,
            PlayerState playerState,
            Link link,
            GameplaySoundSink soundSink,
            Runnable onContactHurt) {
        consume(events, playerState, link, soundSink, onContactHurt, () -> {});
    }

    public static void consume(
            List<EntityProjectileEvent> events,
            PlayerState playerState,
            Link link,
            GameplaySoundSink soundSink,
            Runnable onContactHurt,
            Runnable onPowerUpEnded) {
        Objects.requireNonNull(events, "events");
        Objects.requireNonNull(playerState, "playerState");
        Objects.requireNonNull(soundSink, "soundSink");

        for (int eventIndex = 0; eventIndex < events.size(); eventIndex++) {
            EntityProjectileEvent event = events.get(eventIndex);

            if (event == null) {
                continue;
            }
            if (event.kind() == EntityProjectileEvent.Kind.SHIELD_BLOCK) {
                if (event.soundChannel() == EntityProjectileEvent.SoundChannel.JINGLE && event.soundId() == 0x07) {
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
            if (event.kind() != EntityProjectileEvent.Kind.LINK_DAMAGE) {
                continue;
            }

            // The enemy-bomb handler doubles the existing Link speed even
            // when ApplyLinkCollisionWithEnemy rejects the new damage due to
            // invincibility. It is a motion response, not another hit.
            if (event.linkDamage() <= 0) {
                applyLinkResponse(event, link);
                continue;
            }
            if (playerState.vitals().invincibilityCounter() != 0 || (link != null && link.isCollisionDamageImmune())) {
                continue;
            }

            playHurtSound(event, soundSink);
            // ApplyLinkCollisionWithEnemy begins by ResetPegasusBoots for
            // every generic damaging source, even when a guardian acorn
            // reduces the resulting damage to zero.
            playerState.equipmentLevels().setRunningWithPegasusBoots(false);
            PowerUp previousPowerUp = playerState.equipmentLevels().powerUp();
            playerState.vitals().applyRomEnemyDamage(event.linkDamage());
            if (previousPowerUp != PowerUp.NONE && playerState.equipmentLevels().powerUp() == PowerUp.NONE) {
                onPowerUpEnded.run();
            }
            onContactHurt.run();
            applyLinkResponse(event, link);
        }
    }

    private static void applyLinkResponse(EntityProjectileEvent event, Link link) {
        if (link != null && hasLinkResponse(event)) {
            link.applyRomSpeed(event.linkSpeedX(), event.linkSpeedY());
            link.setCollisionIgnoreFrames(event.linkIgnoreCollisionCountdown());
        }
    }

    private static boolean hasLinkResponse(EntityProjectileEvent event) {
        return event.linkIgnoreCollisionCountdown() != 0 || event.linkSpeedX() != 0 || event.linkSpeedY() != 0;
    }

    private static void playShieldSound(EntityProjectileEvent event, GameplaySoundSink soundSink) {
        if (event.soundChannel() == EntityProjectileEvent.SoundChannel.JINGLE && event.soundId() == SHIELD_TING_ID) {
            soundSink.play(GameplaySoundEvent.SHIELD_TING);
        }
    }

    private static void playHurtSound(EntityProjectileEvent event, GameplaySoundSink soundSink) {
        if (event.soundChannel() == EntityProjectileEvent.SoundChannel.WAVE && event.soundId() == LINK_HURT_ID) {
            soundSink.play(GameplaySoundEvent.LINK_HURT);
        }
    }
}
