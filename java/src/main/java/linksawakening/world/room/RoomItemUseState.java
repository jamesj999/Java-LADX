package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

final class RoomItemUseState {
    PendingShovelDrop pendingShovelDrop;
    int shovelUseState;
    int ocarinaPlaybackCountdown;
    int ocarinaSongFlags;
    int selectedSongIndex;
    int ocarinaAnimationCounter;
    int ocarinaAnimationPhase;
    boolean pendingManboTransition;
    boolean pendingInstrumentTransition;
}
