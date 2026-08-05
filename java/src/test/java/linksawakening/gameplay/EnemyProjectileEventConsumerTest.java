package linksawakening.gameplay;

import linksawakening.state.PlayerState;
import linksawakening.world.EntityProjectileEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class EnemyProjectileEventConsumerTest {

    @Test
    void appliesProjectileDamageThroughTheExistingHealthAndInvincibilityState() {
        PlayerState player = new PlayerState();
        player.setHealth(16);
        RecordingSoundSink sounds = new RecordingSoundSink();

        EntityProjectileEvent event = new EntityProjectileEvent(
            0, 0x0A, EntityProjectileEvent.Kind.LINK_DAMAGE, 0xFF, 0x08,
            EntityProjectileEvent.SoundChannel.WAVE, 0x03, false, false);

        EnemyProjectileEventConsumer.consume(List.of(event), player, sounds);

        assertEquals(8, player.health());
        assertEquals(0x50, player.invincibilityCounter());
        assertEquals(List.of(GameplaySoundEvent.LINK_HURT), sounds.events);

        EnemyProjectileEventConsumer.consume(List.of(event), player, sounds);
        assertEquals(8, player.health());
        assertEquals(List.of(GameplaySoundEvent.LINK_HURT), sounds.events);
    }

    @Test
    void shieldBlockPlaysTheShieldSoundWithoutChangingHealth() {
        PlayerState player = new PlayerState();
        player.setHealth(16);
        RecordingSoundSink sounds = new RecordingSoundSink();

        EntityProjectileEvent event = new EntityProjectileEvent(
            0, 0x0A, EntityProjectileEvent.Kind.SHIELD_BLOCK, 0xFF, 0,
            EntityProjectileEvent.SoundChannel.JINGLE, 0x16, false, false);

        EnemyProjectileEventConsumer.consume(List.of(event), player, sounds);

        assertEquals(16, player.health());
        assertEquals(0, player.invincibilityCounter());
        assertEquals(List.of(GameplaySoundEvent.SHIELD_TING), sounds.events);
    }

    @Test
    void mirrorShieldLaserBlockPlaysTheRomSwordPokeSound() {
        PlayerState player = new PlayerState();
        RecordingSoundSink sounds = new RecordingSoundSink();

        EntityProjectileEvent event = new EntityProjectileEvent(
            0, 0x2B, EntityProjectileEvent.Kind.SHIELD_BLOCK, 0x02, 0,
            EntityProjectileEvent.SoundChannel.JINGLE, 0x07, false, true,
            0x40, 0x50);

        EnemyProjectileEventConsumer.consume(List.of(event), player, sounds);

        assertEquals(List.of(GameplaySoundEvent.SWORD_POKE), sounds.events);
    }

    @Test
    void pairoddSwordHitPlaysTheRomBumpSoundWithoutDamagingLink() {
        PlayerState player = new PlayerState();
        player.setHealth(16);
        RecordingSoundSink sounds = new RecordingSoundSink();

        EntityProjectileEvent event = new EntityProjectileEvent(
            0, 0x58, EntityProjectileEvent.Kind.SWORD_HIT, 0xFF, 0,
            EntityProjectileEvent.SoundChannel.JINGLE, 0x09, true, true,
            0x40, 0x50, 0, 0, 0x0C);

        EnemyProjectileEventConsumer.consume(List.of(event), player, sounds);

        assertEquals(16, player.health());
        assertEquals(List.of(GameplaySoundEvent.ENEMY_BUMP), sounds.events);
    }

    @Test
    void invincibilitySuppressesTheProjectileDamageSoundAndHealthChange() {
        PlayerState player = new PlayerState();
        player.setHealth(16);
        player.setInvincibilityCounter(3);
        RecordingSoundSink sounds = new RecordingSoundSink();

        EntityProjectileEvent event = new EntityProjectileEvent(
            0, 0x0C, EntityProjectileEvent.Kind.LINK_DAMAGE, 0, 0x08,
            EntityProjectileEvent.SoundChannel.WAVE, 0x03, true, false);

        EnemyProjectileEventConsumer.consume(List.of(event), player, sounds);

        assertEquals(16, player.health());
        assertEquals(3, player.invincibilityCounter());
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
