package linksawakening.audio.apu;

/** Shared saturating 4-bit hardware volume step for pulse and noise channels. */
final class EnvelopeStep {
    private EnvelopeStep() {}

    static int next(int volume, int register) {
        return (register & 0x08) != 0 ? Math.min(15, volume + 1) : Math.max(0, volume - 1);
    }
}
