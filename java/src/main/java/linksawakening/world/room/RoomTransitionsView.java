package linksawakening.world.room;

import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.Warp;

final class RoomTransitionsView implements RoomTransitions {
    private final RoomTransitionController delegate;
    private final RoomSession session;

    RoomTransitionsView(RoomTransitionController delegate, RoomSession session) {
        this.delegate = java.util.Objects.requireNonNull(delegate);
        this.session = java.util.Objects.requireNonNull(session);
    }

    @Override
    public void loadWarpDestination(Warp warp) {
        session.loadWarpDestination(warp);
    }

    @Override
    public int tickDungeonTitleMessage(
            boolean dialogActive, boolean inventoryClosed, int linkMotionState, boolean freeMovement) {
        return session.tickDungeonTitleMessage(dialogActive, inventoryClosed, linkMotionState, freeMovement);
    }

    @Override
    public void startAdjacentOverworldScroll(
            ScrollController scrollController, int direction, int linkScreenX, int linkScreenY) {
        session.startAdjacentOverworldScroll(scrollController, direction, linkScreenX, linkScreenY);
    }

    @Override
    public void startAdjacentIndoorScroll(
            ScrollController scrollController, int direction, int linkScreenX, int linkScreenY) {
        session.startAdjacentIndoorScroll(scrollController, direction, linkScreenX, linkScreenY);
    }

    @Override
    public boolean startOcarina(int countdown, int songFlags, int selectedSong) {
        return session.startOcarina(countdown, songFlags, selectedSong);
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
        return session.consumeManboPondTransitionRequest();
    }

    @Override
    public Warp consumeInstrumentTransitionRequest() {
        return session.consumeInstrumentTransitionRequest();
    }

    @Override
    public Warp pollStaircaseWarp(int linkEntityX, int linkEntityY, int linkZ, boolean carryingObject) {
        return session.pollStaircaseWarp(linkEntityX, linkEntityY, linkZ, carryingObject);
    }
}
