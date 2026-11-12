package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

final class RoomItemUseState {
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
