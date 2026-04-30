package linksawakening.audio.music;

final class MusicChannelState {
    final int channel;
    int streamPointer;
    int definitionPointer;
    int programCounter;
    int loopStart;
    int loopRemaining;
    int selectedLength = 1;
    int lengthCounter;
    int envelope;
    int dutyLength;
    int waveOutputLevel;
    boolean active;
    boolean playingRest;

    MusicChannelState(int channel) {
        this.channel = channel;
    }

    void reset() {
        streamPointer = 0;
        definitionPointer = 0;
        programCounter = 0;
        loopStart = 0;
        loopRemaining = 0;
        selectedLength = 1;
        lengthCounter = 0;
        envelope = 0;
        dutyLength = 0;
        waveOutputLevel = 0;
        active = false;
        playingRest = false;
    }
}
