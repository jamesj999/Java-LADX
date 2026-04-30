package linksawakening.audio.sfx;

import linksawakening.audio.apu.GameBoyApu;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SoundEffectPcmRenderer {
    private static final int TICKS_PER_SECOND = 60;
    private static final int MAX_SOUND_EFFECT_TICKS = TICKS_PER_SECOND * 10;

    private SoundEffectPcmRenderer() {
    }

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
        List<short[]> chunks = new ArrayList<>();
        int sampleCount = 0;

        for (int tick = 0; tick < MAX_SOUND_EFFECT_TICKS && player.isPlaying(); tick++) {
            int frames = wholeFramesPerTick;
            frameRemainder += frameRemainderPerTick;
            if (frameRemainder >= TICKS_PER_SECOND) {
                frames++;
                frameRemainder -= TICKS_PER_SECOND;
            }
            short[] chunk = apu.render(frames);
            chunks.add(chunk);
            sampleCount += chunk.length;
            player.tick60Hz();
        }

        short[] pcm = new short[sampleCount];
        int offset = 0;
        for (short[] chunk : chunks) {
            System.arraycopy(chunk, 0, pcm, offset, chunk.length);
            offset += chunk.length;
        }
        return pcm;
    }
}
