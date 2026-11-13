package linksawakening.world.room;

final class RoomItemUseState {
    /** Absent when no shovel drop awaits the next entity pass. */
    PendingShovelDrop pendingShovelDrop;

    int shovelUseState;
    int ocarinaPlaybackCountdown;
    int ocarinaSongFlags;
    int selectedSongIndex;

    linksawakening.state.OcarinaSong selectedSong() {
        return selectedSongIndex >= 0 && selectedSongIndex <= 2
                ? linksawakening.state.OcarinaSong.fromSelectionIndex(selectedSongIndex)
                : null;
    }

    int ocarinaAnimationCounter;
    int ocarinaAnimationPhase;
    boolean pendingManboTransition;
    boolean pendingInstrumentTransition;
}
