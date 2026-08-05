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
    void mapsSecondaryBurningNoiseAfterTheEnemyHitJingle() {
        RecordingSoundSink sounds = new RecordingSoundSink();
        List<EntityCombatEvent> events = List.of(
            new EntityCombatEvent(0, 0x09, 0, true, 0, 0xFE,
                EntityCombatEvent.SoundChannel.JINGLE, 0x03,
                EntityCombatEvent.SoundChannel.NOISE, 0x12));

        EnemyCombatEventConsumer.consume(events, sounds);

        assertEquals(List.of(GameplaySoundEvent.ENEMY_HIT, GameplaySoundEvent.ENEMY_BURNING),
            sounds.events);
    }

    @Test
    void mapsBurnExpiryNoiseToTheEnemyDestroyedSound() {
        RecordingSoundSink sounds = new RecordingSoundSink();
        List<EntityCombatEvent> events = List.of(
            new EntityCombatEvent(0, 0x09, 0, false, 0, -1,
                EntityCombatEvent.SoundChannel.NOISE, 0x13));

        EnemyCombatEventConsumer.consume(events, sounds);

        assertEquals(List.of(GameplaySoundEvent.ENEMY_DESTROYED), sounds.events);
    }

    @Test
    void mapsBeamosFiringNoiseToTheLaserSound() {
        RecordingSoundSink sounds = new RecordingSoundSink();
        List<EntityCombatEvent> events = List.of(
            new EntityCombatEvent(0, 0x2A, 0, false, 0, -1,
                EntityCombatEvent.SoundChannel.NOISE, 0x08));

        EnemyCombatEventConsumer.consume(events, sounds);

        assertEquals(List.of(GameplaySoundEvent.BEAMOS_LASER), sounds.events);
    }

    @Test
    void mapsSpikeTrapWhooshNoiseToTheGameplaySound() {
        RecordingSoundSink sounds = new RecordingSoundSink();
        List<EntityCombatEvent> events = List.of(
            new EntityCombatEvent(0, 0x27, 0, false, 0, -1,
                EntityCombatEvent.SoundChannel.NOISE, 0x0A));

        EnemyCombatEventConsumer.consume(events, sounds);

        assertEquals(List.of(GameplaySoundEvent.SPIKE_TRAP_WHOOSH), sounds.events);
    }

    @Test
    void mapsEvasiveStalfosCloneWhooshThroughTheSharedRomNoiseEffect() {
        RecordingSoundSink sounds = new RecordingSoundSink();
        List<EntityCombatEvent> events = List.of(
            new EntityCombatEvent(0, 0x1E, 0, false, 0, -1,
                EntityCombatEvent.SoundChannel.NOISE, 0x0A));

        EnemyCombatEventConsumer.consume(events, sounds);

        assertEquals(List.of(GameplaySoundEvent.SPIKE_TRAP_WHOOSH), sounds.events);
    }

    @Test
    void routesRomSwordPokeSoundAndVfxRequest() {
        RecordingSoundSink sounds = new RecordingSoundSink();
        linksawakening.vfx.TransientVfxSystem vfx =
            new linksawakening.vfx.TransientVfxSystem(1);
        EntityCombatEvent event = new EntityCombatEvent(
            0, 0x27, 0, true, 0, -1,
            EntityCombatEvent.SoundChannel.JINGLE, 0x07,
            EntityCombatEvent.SoundChannel.NONE, -1,
            new EntityCombatEvent.SwordPokeVfx(0x40, 0x50));

        EnemyCombatEventConsumer.consume(List.of(event), sounds, vfx);

        assertEquals(List.of(GameplaySoundEvent.SWORD_POKE), sounds.events);
        assertEquals(List.of(new linksawakening.vfx.TransientVfxSystem.Slot(
            0, linksawakening.vfx.TransientVfxType.SWORD_POKE, 0x0F, 0x40, 0x50)),
            vfx.activeSlots());
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
