package linksawakening.world;

import linksawakening.entity.Link;

public final class RoomTransitionCoordinator {
    private final RoomSession roomSession;
    private final RoomBoundaryController boundaryController;
    private final TransitionController transitionController;
    private final ScrollController scrollController;

    private int suppressedWarpTile = -1;

    public RoomTransitionCoordinator(RoomSession roomSession,
                                     RoomBoundaryController boundaryController,
                                     TransitionController transitionController,
                                     ScrollController scrollController) {
        this.roomSession = roomSession;
        this.boundaryController = boundaryController;
        this.transitionController = transitionController;
        this.scrollController = scrollController;
    }

    /**
     * Warp-transition check runs before overworld edge scroll/clamp. Indoor
     * rooms need this ordering because their south-edge front-door trigger must
     * see Link past the bottom boundary before any fallback clamp can run.
     */
    public void handleWarpAndIndoorBoundaries(Link link) {
        if (!roomSession.hasActiveRoom()
            || transitionController.isActive()) {
            return;
        }
        ActiveRoom room = roomSession.activeRoom();

        if (room.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            link.initializeRoomPhysicsForRoom(roomSession.roomLoadToken(), room.mapId(),
                room.roomId(), room.mapCategory());
            RoomBoundaryDecision boundary = boundaryController.decide(
                roomSession.boundaryState(link.pixelX(), link.pixelY(), link.isAirborne(),
                    link.romPhysicsModifier()));
            if (boundary.type() == RoomBoundaryDecision.Type.INDOOR_FRONT_DOOR_WARP) {
                Warp target = room.firstWarp();
                System.out.println("Indoor front-door exit → cat=" + target.category()
                    + " map=" + String.format("%02X", target.destMap())
                    + " room=" + String.format("%02X", target.destRoom())
                    + " at (" + target.destX() + "," + target.destY() + ")");
                transitionController.startFadeOut(() -> applyWarp(target, link));
                return;
            }
            if (boundary.type() == RoomBoundaryDecision.Type.SIDE_SCROLL_VERTICAL_WARP
                && room.hasWarps()) {
                Warp target = room.firstWarp();
                transitionController.startFadeOut(() -> applyWarp(target, link));
                return;
            }
            if (boundary.type() == RoomBoundaryDecision.Type.INDOOR_SCROLL) {
                int previousX = link.pixelX();
                int previousY = link.pixelY();
                link.setRoomEntryPixelPosition(boundary.linkTargetX(), boundary.linkTargetY());
                roomSession.startAdjacentIndoorScroll(scrollController, boundary.direction(), previousX, previousY);
                suppressedWarpTile = Warp.packTileLocation(boundary.linkTargetX(), boundary.linkTargetY());
                return;
            }
        }

        if (!room.hasWarps()) {
            return;
        }

        Warp staircaseWarp = roomSession.pollStaircaseWarp(
            link.romEntityX(), link.romEntityY(), link.romEntityZ(),
            link.isCarryingLiftedObject());
        if (staircaseWarp != null) {
            transitionController.startFadeOut(() -> applyWarp(staircaseWarp, link));
            return;
        }

        int linkTile = Warp.packTileLocation(link.pixelX(), link.pixelY());
        if (linkTile != suppressedWarpTile) {
            suppressedWarpTile = -1;
        }
        if (linkTile == suppressedWarpTile) {
            return;
        }
        for (Warp warp : room.warps()) {
            if (warp.tileLocation() == linkTile) {
                final Warp target = warp;
                System.out.println("Warp trigger: tile=" + String.format("%02X", linkTile)
                    + " → cat=" + target.category()
                    + " map=" + String.format("%02X", target.destMap())
                    + " room=" + String.format("%02X", target.destRoom())
                    + " at (" + target.destX() + "," + target.destY() + ")");
                transitionController.startFadeOut(() -> applyWarp(target, link));
                return;
            }
        }
    }

    public void handleOverworldBoundary(Link link) {
        if (!roomSession.hasActiveRoom()) {
            return;
        }

        int previousX = link.pixelX();
        int previousY = link.pixelY();
        RoomBoundaryDecision boundary = boundaryController.decide(
            roomSession.boundaryState(previousX, previousY, link.isAirborne()));
        if (boundary.type() == RoomBoundaryDecision.Type.OVERWORLD_SCROLL) {
            link.setRoomEntryPixelPosition(boundary.linkTargetX(), boundary.linkTargetY());
            if (boundary.direction() == ScrollController.UP
                && roomSession.shouldGetLostInMysteriousWoods()) {
                roomSession.startMysteriousWoodsLostScroll(
                    scrollController, previousX, previousY);
            } else {
                roomSession.startAdjacentOverworldScroll(
                    scrollController, boundary.direction(), previousX, previousY);
            }
        } else if (boundary.type() == RoomBoundaryDecision.Type.CLAMP_LINK) {
            link.setPixelPosition(boundary.linkTargetX(), boundary.linkTargetY());
        }
    }

    /** Starts a pending Ocarina Mambo transition after entity playback ends. */
    public boolean handlePendingManboTransition(Link link) {
        if (link == null || !roomSession.hasActiveRoom() || transitionController.isActive()) {
            return false;
        }
        Warp target = roomSession.consumeManboPondTransitionRequest();
        if (target == null) {
            return false;
        }
        transitionController.startManboIn(() -> applyWarp(target, link));
        return true;
    }

    /** Starts disableMovementInTransition after the Sirens Instrument effect. */
    public boolean handlePendingInstrumentTransition(Link link) {
        if (link == null || !roomSession.hasActiveRoom() || transitionController.isActive()) {
            return false;
        }
        Warp target = roomSession.consumeInstrumentTransitionRequest();
        if (target == null) {
            return false;
        }
        transitionController.startFadeOut(() -> applyWarp(target, link));
        return true;
    }

    private void applyWarp(Warp warp, Link link) {
        roomSession.loadWarpDestination(warp);
        ActiveRoom destination = roomSession.activeRoom();
        link.setRoomPhysicsForRoom(roomSession.roomLoadToken(), destination.mapId(),
            destination.roomId(), destination.mapCategory());
        int landingX = warp.javaPixelX();
        int landingY = warp.javaPixelY();
        link.setRoomEntryPixelPosition(landingX, landingY);
        suppressedWarpTile = Warp.packTileLocation(landingX, landingY);
    }
}
