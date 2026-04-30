package linksawakening.audio.sfx;

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
