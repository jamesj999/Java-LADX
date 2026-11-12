package linksawakening.world.room;

import linksawakening.world.room.RoomSession.*;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.Warp;

final class RoomTransitionsView implements RoomTransitions {
    private final RoomTransitionController delegate;

    RoomTransitionsView(RoomTransitionController delegate) {
        this.delegate = delegate;
    }

    @Override
    public void loadWarpDestination(Warp warp) {
        delegate.loadWarpDestination(warp);
    }

    @Override
    public int tickDungeonTitleMessage(
            boolean dialogActive, boolean inventoryClosed, int linkMotionState, boolean freeMovement) {
        return delegate.tickDungeonTitleMessage(dialogActive, inventoryClosed, linkMotionState, freeMovement);
    }

    @Override
    public void startAdjacentOverworldScroll(
            ScrollController scrollController, int direction, int linkScreenX, int linkScreenY) {
        delegate.startAdjacentOverworldScroll(scrollController, direction, linkScreenX, linkScreenY);
    }

    @Override
    public void startAdjacentIndoorScroll(
            ScrollController scrollController, int direction, int linkScreenX, int linkScreenY) {
        delegate.startAdjacentIndoorScroll(scrollController, direction, linkScreenX, linkScreenY);
    }

    @Override
    public boolean startOcarina(int countdown, int songFlags, int selectedSong) {
        return delegate.startOcarina(countdown, songFlags, selectedSong);
    }

    @Override
    public boolean ocarinaPlaying() {
        return delegate.ocarinaPlaying();
    }

    @Override
    public int ocarinaAnimationState() {
        return delegate.ocarinaAnimationState();
    }

    @Override
    public Warp consumeManboPondTransitionRequest() {
        return delegate.consumeManboPondTransitionRequest();
    }

    @Override
    public Warp consumeInstrumentTransitionRequest() {
        return delegate.consumeInstrumentTransitionRequest();
    }

    @Override
    public Warp pollStaircaseWarp(int linkEntityX, int linkEntityY, int linkZ, boolean carryingObject) {
        return delegate.pollStaircaseWarp(linkEntityX, linkEntityY, linkZ, carryingObject);
    }
}
