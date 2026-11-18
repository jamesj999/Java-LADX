package linksawakening.audio.sfx;

import linksawakening.audio.apu.GameBoyApu;

/** Frame-driven SFX playback on a supplied APU. */
public final class LiveSoundEffects {
    private static final int FRAMES_PER_SECOND = 60;
    private static final short[] SILENCE = new short[0];

    private final GameBoyApu apu;
    private final RomSoundEffectEngine engine;
    private int sampleRemainder;

    public LiveSoundEffects(GameBoyApu apu, byte[] romData) {
        this.apu = apu;
        engine = new RomSoundEffectEngine(romData, apu);
        engine.initializeLive();
    }

    /** Sets the request byte for the next frame. Zero clears the pending request. */
    public void request(SoundEffectNamespace namespace, int id) {
        engine.request(namespace, id);
    }

    /** Applies the music bank's cancellation rules to ongoing effects at a track change. */
    public void changeMusicTrack(int bank, boolean stopping) {
        engine.changeMusicTrack(bank, stopping);
    }

    /** Runs the SFX handlers and returns their music request, or zero. */
    public int tick() {
        return engine.advanceLiveFrame();
    }

    /** True when this frame's effect reset the active score and the APU registers. */
    public boolean silencedMusic() {
        return engine.silencedMusic();
    }

    /** Returns music channel locks after the latest tick, with bits 0–3 for channels 1–4. */
    public int musicSuppressionMask() {
        return engine.musicSuppressionMask();
    }

    /** Advances one 60 Hz frame and returns interleaved stereo PCM, or empty PCM for silence. */
    public short[] renderFrame() {
        tick();
        int samples = apu.sampleRate() / FRAMES_PER_SECOND;
        sampleRemainder += apu.sampleRate() % FRAMES_PER_SECOND;
        if (sampleRemainder >= FRAMES_PER_SECOND) {
            samples++;
            sampleRemainder -= FRAMES_PER_SECOND;
        }

        short[] pcm = apu.render(samples);
        for (short sample : pcm) {
            if (sample != 0) {
                return pcm;
            }
        }
        return SILENCE;
    }
}
