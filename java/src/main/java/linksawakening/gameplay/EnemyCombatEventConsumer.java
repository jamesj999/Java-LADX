package linksawakening.gameplay;

import linksawakening.world.EntityCombatEvent;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;

import java.util.List;
import java.util.Objects;

/** Applies raw enemy-combat sound writes at the gameplay/audio boundary. */
public final class EnemyCombatEventConsumer {
    private static final int JINGLE_BUMP_ID = 0x09;
    private static final int JINGLE_TREASURE_FOUND_ID = 0x01;
    private static final int JINGLE_PUZZLE_SOLVED_ID = 0x02;
    private static final int JINGLE_ENEMY_HIT_ID = 0x03;
    private static final int JINGLE_SWORD_POKE_ID = 0x07;
    private static final int JINGLE_WATER_SPLASH_ID = 0x0E;
    private static final int JINGLE_ITEM_FALLING_ID = 0x18;
    private static final int JINGLE_ENEMY_BOMB_THROW_ID = 0x08;
    private static final int JINGLE_MAGIC_POWDER_ID = 0x05;
    private static final int JINGLE_MAGIC_POWDER_POOF_ID = 0x2F;
    private static final int JINGLE_SWORD_BEAM_ID = 0x3B;
    private static final int WAVE_SWITCH_BLOCK_TOGGLE_ID = 0x0E;
    private static final int NOISE_ENEMY_BURNING_ID = 0x12;
    private static final int NOISE_ENEMY_DESTROYED_ID = 0x13;
    private static final int NOISE_BEAMOS_LASER_ID = 0x08;
    private static final int NOISE_SPIKE_TRAP_WHOOSH_ID = 0x0A;
    private static final int NOISE_HOOKSHOT_ID = 0x0B;
    private static final int NOISE_BOOMERANG_ID = 0x2D;
    private static final int NOISE_MAGIC_ROD_ID = 0x0D;
    private static final int NOISE_BOMB_EXPLOSION_ID = 0x0C;
    private static final int NOISE_DOOR_UNLOCKED_ID = 0x04;

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
                case JINGLE_TREASURE_FOUND_ID -> soundSink.play(GameplaySoundEvent.TREASURE_FOUND);
                case JINGLE_PUZZLE_SOLVED_ID -> soundSink.play(GameplaySoundEvent.PUZZLE_SOLVED);
                case JINGLE_ENEMY_HIT_ID -> soundSink.play(GameplaySoundEvent.ENEMY_HIT);
                case JINGLE_SWORD_POKE_ID -> soundSink.play(GameplaySoundEvent.SWORD_POKE);
                case JINGLE_WATER_SPLASH_ID -> soundSink.play(GameplaySoundEvent.WATER_SPLASH);
                case JINGLE_ITEM_FALLING_ID -> soundSink.play(GameplaySoundEvent.ITEM_FALLING);
                case JINGLE_ENEMY_BOMB_THROW_ID ->
                    soundSink.play(GameplaySoundEvent.ENEMY_BOMB_THROW);
                case JINGLE_MAGIC_POWDER_ID -> soundSink.play(GameplaySoundEvent.MAGIC_POWDER);
                case JINGLE_MAGIC_POWDER_POOF_ID ->
                    soundSink.play(GameplaySoundEvent.MAGIC_POWDER_POOF);
                case JINGLE_SWORD_BEAM_ID -> soundSink.play(GameplaySoundEvent.SWORD_BEAM);
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
                case NOISE_SPIKE_TRAP_WHOOSH_ID ->
                    soundSink.play(GameplaySoundEvent.SPIKE_TRAP_WHOOSH);
                case NOISE_HOOKSHOT_ID -> soundSink.play(GameplaySoundEvent.HOOKSHOT);
                case NOISE_BOOMERANG_ID -> soundSink.play(GameplaySoundEvent.BOOMERANG);
                case NOISE_MAGIC_ROD_ID -> soundSink.play(GameplaySoundEvent.MAGIC_ROD);
                case NOISE_BOMB_EXPLOSION_ID ->
                    soundSink.play(GameplaySoundEvent.BOMB_EXPLOSION);
                case NOISE_DOOR_UNLOCKED_ID -> soundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
                default -> {
                    // Unknown ROM sound writes must not be guessed or routed
                    // to an unrelated gameplay effect.
                }
            }
        } else if (channel == EntityCombatEvent.SoundChannel.WAVE
            && id == WAVE_SWITCH_BLOCK_TOGGLE_ID) {
            soundSink.play(GameplaySoundEvent.SWITCH_BLOCK_TOGGLE);
        }
    }
}
