package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.Warp;

public interface RoomTransitions {
    void loadWarpDestination(Warp warp);

    int tickDungeonTitleMessage(
            boolean dialogActive, boolean inventoryClosed, int linkMotionState, boolean freeMovement);

    void startAdjacentOverworldScroll(
            ScrollController scrollController, int direction, int linkScreenX, int linkScreenY);

    void startAdjacentIndoorScroll(ScrollController scrollController, int direction, int linkScreenX, int linkScreenY);

    boolean startOcarina(int countdown, int songFlags, int selectedSong);

    default boolean startOcarina(int countdown, int songFlags, linksawakening.state.OcarinaSong song) {
        return startOcarina(countdown, songFlags, song.selectionIndex());
    }

    boolean ocarinaPlaying();

    int ocarinaAnimationState();

    Warp consumeManboPondTransitionRequest();

    Warp consumeInstrumentTransitionRequest();

    Warp pollStaircaseWarp(int linkEntityX, int linkEntityY, int linkZ, boolean carryingObject);
}
