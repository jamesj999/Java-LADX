package linksawakening.world.room;

import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.Warp;

public interface RoomTransitions {
    RoomTransitionCoordinator coordinator(
            linksawakening.world.transition.RoomBoundaryController boundary,
            linksawakening.world.transition.TransitionController transition,
            ScrollController scroll,
            linksawakening.gameplay.GameplaySoundSink sound);

    default RoomTransitionCoordinator coordinator(
            linksawakening.world.transition.RoomBoundaryController boundary,
            linksawakening.world.transition.TransitionController transition,
            ScrollController scroll) {
        return coordinator(boundary, transition, scroll, linksawakening.gameplay.GameplaySoundSink.none());
    }

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
