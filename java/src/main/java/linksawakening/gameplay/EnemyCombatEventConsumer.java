package linksawakening.gameplay;

import linksawakening.world.EntityCombatEvent;
import linksawakening.audio.sfx.SoundEffectNamespace;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;

import java.util.List;
import java.util.Objects;

/** Applies raw enemy-combat sound writes at the gameplay/audio boundary. */
public final class EnemyCombatEventConsumer {
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
        if (channel == null || id <= 0 || id > 0xFF) return;
        switch (channel) {
            case JINGLE -> soundSink.playRomSound(SoundEffectNamespace.JINGLE, id);
            case WAVE -> soundSink.playRomSound(SoundEffectNamespace.WAVE, id);
            case NOISE -> soundSink.playRomSound(SoundEffectNamespace.NOISE, id);
            case NONE -> { }
        }
    }
}
