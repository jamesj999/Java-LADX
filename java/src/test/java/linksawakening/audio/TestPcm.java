package linksawakening.audio;

import linksawakening.audio.apu.GameBoyApu;

/** Owned-array convenience for assertions/capture tools; production uses explicit renderInto storage. */
public final class TestPcm {
    private TestPcm() {}

    public static short[] renderPcm(GameBoyApu apu, int frames) {
        short[] samples = new short[Math.multiplyExact(frames, 2)];
        apu.renderInto(samples, 0, frames);
        return samples;
    }
}
