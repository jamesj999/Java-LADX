package linksawakening.world.room;

import linksawakening.entity.Link;
import linksawakening.world.transition.Warp;

/** Boundary-operation port. It dispatches a loaded generation; it never exposes an optional current runtime. */
interface RoomBoundaryOperations {
    void handleWarpAndIndoorBoundaries(RoomTransitionCoordinator coordinator, Link link);

    void handleOverworldBoundary(RoomTransitionCoordinator coordinator, Link link);

    boolean handlePendingManboTransition(RoomTransitionCoordinator coordinator, Link link);

    boolean handlePendingInstrumentTransition(RoomTransitionCoordinator coordinator, Link link);

    void applyWarp(Warp warp, Link link);
}
