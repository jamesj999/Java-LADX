package linksawakening.gameplay;

import linksawakening.entity.Link;
import linksawakening.equipment.ItemRegistry;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;
import linksawakening.world.EntityProjectileEvent;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        assertEquals(16, player.health());
        assertEquals(8, player.subtractHealthBuffer());
        assertEquals(0x50, player.invincibilityCounter());
        assertEquals(List.of(GameplaySoundEvent.LINK_HURT), sounds.events);

        player.tickResourceBuffers(1);
        assertEquals(15, player.health());
        assertEquals(7, player.subtractHealthBuffer());

        EnemyProjectileEventConsumer.consume(List.of(event), player, sounds);
        assertEquals(15, player.health());
        assertEquals(List.of(GameplaySoundEvent.LINK_HURT), sounds.events);
    }

    @Test
    void appliesAcceptedProjectileRecoilToLinkExactlyOnce() throws IOException {
        PlayerState player = new PlayerState();
        player.setHealth(16);
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            loadRomTables(), null, null, player, new ItemRegistry());
        RecordingSoundSink sounds = new RecordingSoundSink();
        EntityProjectileEvent event = new EntityProjectileEvent(
            0, 0x0A, EntityProjectileEvent.Kind.LINK_DAMAGE, 0xFF, 0x08,
            EntityProjectileEvent.SoundChannel.WAVE, 0x03, false, false,
            0, 0, 0xF0, 0x14, 0x10);

        EnemyProjectileEventConsumer.consume(List.of(event), player, link, sounds);
        link.update();

        assertEquals(0xF0, link.romSpeedX());
        assertEquals(0x14, link.romSpeedY());
        assertEquals(8, player.subtractHealthBuffer());
    }

    @Test
    void appliesProtectedEnemyBombMotionWithoutApplyingAnotherHit() throws IOException {
        PlayerState player = new PlayerState();
        player.setHealth(16);
        RomTables romTables = loadRomTables();
        OverworldCollision collision = new OverworldCollision(romTables);
        collision.setRoom(emptyRoomObjectsArea());
        Link link = new Link(new InputState(), new InputConfig(1, 2, 3, 4, 5, 6, 7),
            romTables, collision, null, player, new ItemRegistry());
        RecordingSoundSink sounds = new RecordingSoundSink();
        EntityProjectileEvent event = new EntityProjectileEvent(
            0, 0x02, EntityProjectileEvent.Kind.LINK_DAMAGE, 0, 0,
            EntityProjectileEvent.SoundChannel.NONE, -1, false, false,
            0, 0, 0x20, 0xE0, 0);

        EnemyProjectileEventConsumer.consume(List.of(event), player, link, sounds);
        link.update();

        assertEquals(0x20, link.romSpeedX());
        assertEquals(0xE0, link.romSpeedY());
        assertEquals(0, player.subtractHealthBuffer());
        assertTrue(sounds.events.isEmpty());
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

    @Test
    void enemyBombCollisionResetsPegasusBootsBeforeApplyingNominalDamageModifiers() {
        PlayerState player = new PlayerState();
        player.setActivePowerUp(PlayerState.ACTIVE_POWER_UP_GUARDIAN_ACORN);
        player.setRunningWithPegasusBoots(true);
        RecordingSoundSink sounds = new RecordingSoundSink();

        EntityProjectileEvent event = new EntityProjectileEvent(
            0, 0x02, EntityProjectileEvent.Kind.LINK_DAMAGE, 0, 0x08,
            EntityProjectileEvent.SoundChannel.WAVE, 0x03, false, false);

        EnemyProjectileEventConsumer.consume(List.of(event), player, sounds);

        assertEquals(4, player.subtractHealthBuffer());
        assertEquals(0x50, player.invincibilityCounter());
        assertFalse(player.runningWithPegasusBoots());
        assertEquals(List.of(GameplaySoundEvent.LINK_HURT), sounds.events);
    }

    private static final class RecordingSoundSink implements GameplaySoundSink {
        private final List<GameplaySoundEvent> events = new ArrayList<>();

        @Override
        public void play(GameplaySoundEvent event) {
            events.add(event);
        }
    }

    private static RomTables loadRomTables() throws IOException {
        try (var stream = EnemyProjectileEventConsumerTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return RomTables.loadFromRom(stream.readAllBytes());
        }
    }

    private static int[] emptyRoomObjectsArea() {
        int[] roomObjectsArea = new int[0x100];
        java.util.Arrays.fill(roomObjectsArea, 0xFF);
        return roomObjectsArea;
    }
}
