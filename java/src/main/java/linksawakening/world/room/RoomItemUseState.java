package linksawakening.world.room;

final class RoomItemUseState {
    void finishOcarinaPlayback() {
        ocarinaPlaybackCountdown = 0;
        ocarinaAnimationCounter = 0;
        ocarinaAnimationPhase = 0;
    }

    /** Absent when no shovel drop awaits the next entity pass. */
    private PendingShovelDrop pendingShovelDrop;

    private int shovelUseState;
    private int ocarinaPlaybackCountdown;
    private int ocarinaSongFlags;
    private int selectedSongIndex;

    linksawakening.state.OcarinaSong selectedSong() {
        return selectedSongIndex >= 0 && selectedSongIndex <= 2
                ? linksawakening.state.OcarinaSong.fromSelectionIndex(selectedSongIndex)
                : null;
    }

    private int ocarinaAnimationCounter;
    private int ocarinaAnimationPhase;
    private boolean pendingManboTransition;
    private boolean pendingInstrumentTransition;

    boolean pendingManboTransition() {
        return this.pendingManboTransition;
    }

    boolean pendingManboTransition(boolean value) {
        this.pendingManboTransition = value;
        return this.pendingManboTransition;
    }

    boolean pendingInstrumentTransition() {
        return this.pendingInstrumentTransition;
    }

    boolean pendingInstrumentTransition(boolean value) {
        this.pendingInstrumentTransition = value;
        return this.pendingInstrumentTransition;
    }

    PendingShovelDrop pendingShovelDrop() {
        return this.pendingShovelDrop;
    }

    PendingShovelDrop pendingShovelDrop(PendingShovelDrop value) {
        this.pendingShovelDrop = value;
        return this.pendingShovelDrop;
    }

    int shovelUseState() {
        return this.shovelUseState;
    }

    int shovelUseState(int value) {
        this.shovelUseState = value;
        return this.shovelUseState;
    }

    int ocarinaPlaybackCountdown() {
        return this.ocarinaPlaybackCountdown;
    }

    int ocarinaPlaybackCountdown(int value) {
        this.ocarinaPlaybackCountdown = value;
        return this.ocarinaPlaybackCountdown;
    }

    int postDecrementOcarinaPlaybackCountdown() {
        return this.ocarinaPlaybackCountdown--;
    }

    int ocarinaSongFlags() {
        return this.ocarinaSongFlags;
    }

    int ocarinaSongFlags(int value) {
        this.ocarinaSongFlags = value;
        return this.ocarinaSongFlags;
    }

    int selectedSongIndex() {
        return this.selectedSongIndex;
    }

    int selectedSongIndex(int value) {
        this.selectedSongIndex = value;
        return this.selectedSongIndex;
    }

    int ocarinaAnimationCounter() {
        return this.ocarinaAnimationCounter;
    }

    int ocarinaAnimationCounter(int value) {
        this.ocarinaAnimationCounter = value;
        return this.ocarinaAnimationCounter;
    }

    int postIncrementOcarinaAnimationCounter() {
        return this.ocarinaAnimationCounter++;
    }

    int ocarinaAnimationPhase() {
        return this.ocarinaAnimationPhase;
    }

    int ocarinaAnimationPhase(int value) {
        this.ocarinaAnimationPhase = value;
        return this.ocarinaAnimationPhase;
    }

    int xorOcarinaAnimationPhase(int value) {
        this.ocarinaAnimationPhase ^= value;
        return this.ocarinaAnimationPhase;
    }
}
