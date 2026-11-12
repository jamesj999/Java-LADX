package linksawakening.audio.sfx;

import java.util.Arrays;
import java.util.Objects;
import linksawakening.audio.apu.GameBoyApu;

/** Renders a complete ROM-backed SFX audition into owned PCM storage. */
public final class SoundEffectPcmRenderer {
    private static final int TICKS_PER_SECOND = 60;
    private static final int MAX_SOUND_EFFECT_TICKS = TICKS_PER_SECOND * 10;

    private SoundEffectPcmRenderer() {}

    public static short[] renderOneShot(GameBoyApu apu, SoundEffectPlayer player, SoundEffect effect) {
        Objects.requireNonNull(apu, "apu");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(effect, "effect");

        apu.reset();
        try {
            player.play(effect);
            return renderActiveEffect(apu, player);
        } finally {
            player.stop();
            apu.reset();
        }
    }

    private static short[] renderActiveEffect(GameBoyApu apu, SoundEffectPlayer player) {
        int wholeFramesPerTick = apu.sampleRate() / TICKS_PER_SECOND;
        int frameRemainderPerTick = apu.sampleRate() % TICKS_PER_SECOND;
        int frameRemainder = 0;
        short[] storage = new short[Math.multiplyExact(apu.sampleRate(), 20)];
        int sampleCount = 0;

        for (int tick = 0; tick < MAX_SOUND_EFFECT_TICKS && player.isPlaying(); tick++) {
            int frames = wholeFramesPerTick;
            frameRemainder += frameRemainderPerTick;
            if (frameRemainder >= TICKS_PER_SECOND) {
                frames++;
                frameRemainder -= TICKS_PER_SECOND;
            }
            apu.renderInto(storage, sampleCount, frames);
            sampleCount += frames * 2;
            player.tick60Hz();
        }

        return Arrays.copyOf(storage, sampleCount);
    }
}
