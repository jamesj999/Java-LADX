package linksawakening.audio.sfx;

/** Independent ROM request namespaces for jingles, wave and noise effects. */
public enum SoundEffectNamespace {
    JINGLE("hJingle"),
    WAVE("hWaveSfx"),
    NOISE("hNoiseSfx");

    private final String ladxRegisterName;

    SoundEffectNamespace(String ladxRegisterName) {
        this.ladxRegisterName = ladxRegisterName;
    }

    public String ladxRegisterName() {
        return ladxRegisterName;
    }
}
