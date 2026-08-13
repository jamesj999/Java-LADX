package linksawakening.world;

import linksawakening.entity.Link;

import static linksawakening.world.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_WIDTH;

public final class RoomBoundaryController {
    private static final int OVERWORLD_COLUMNS = 16;
    private static final int OVERWORLD_ROWS = 16;

    public RoomBoundaryDecision decide(RoomBoundaryState state) {
        if (state.mapCategory() == Warp.CATEGORY_OVERWORLD) {
            return decideOverworld(state);
        }
        return decideIndoor(state);
    }

    private static RoomBoundaryDecision decideOverworld(RoomBoundaryState state) {
        int x = state.linkX();
        int y = state.linkY();
        int roomCol = state.roomId() % OVERWORLD_COLUMNS;
        int roomRow = state.roomId() / OVERWORLD_COLUMNS;
        // CheckPositionForMapTransition reaches its wIsLinkInTheAir guard
        // before initiating any ordinary top-down room transition. In
        // particular, the bottom edge must not start a scroll in the middle
        // of a ledge flip.
        if (state.linkAirborne()) {
            return RoomBoundaryDecision.none();
        }

        if (x < 0 && roomCol > 0) {
            return RoomBoundaryDecision.overworldScroll(ScrollController.LEFT,
                ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE, y);
        }
        if (x + Link.SPRITE_SIZE > ROOM_PIXEL_WIDTH && roomCol < OVERWORLD_COLUMNS - 1) {
            return RoomBoundaryDecision.overworldScroll(ScrollController.RIGHT, 0, y);
        }
        if (y < 0 && roomRow > 0) {
            return RoomBoundaryDecision.overworldScroll(ScrollController.UP, x,
                ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE);
        }
        if (y + Link.SPRITE_SIZE > ROOM_PIXEL_HEIGHT && roomRow < OVERWORLD_ROWS - 1) {
            return RoomBoundaryDecision.overworldScroll(ScrollController.DOWN, x, 0);
        }

        int clampedX = Math.max(0, Math.min(x, ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE));
        int clampedY = Math.max(0, Math.min(y, ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE));
        if (clampedX != x || clampedY != y) {
            return RoomBoundaryDecision.clamp(clampedX, clampedY);
        }
        return RoomBoundaryDecision.none();
    }

    private static RoomBoundaryDecision decideIndoor(RoomBoundaryState state) {
        int x = state.linkX();
        int y = state.linkY();
        boolean offBottom = y + Link.SPRITE_SIZE > ROOM_PIXEL_HEIGHT;
        boolean offTop = y < 0;
        boolean offLeft = x < 0;
        boolean offRight = x + Link.SPRITE_SIZE > ROOM_PIXEL_WIDTH;
        boolean supportedTailCaveSideView = state.mapCategory() == Warp.CATEGORY_SIDESCROLL
            && state.mapId() == 0x00
            && (state.roomId() == 0x18 || state.roomId() == 0x19)
            && state.hasWarps();

        if (offBottom && (state.shutterDoorMask() & 0x02) != 0) {
            return RoomBoundaryDecision.clamp(x, ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE);
        }
        if (offTop && (state.shutterDoorMask() & 0x01) != 0) {
            return RoomBoundaryDecision.clamp(x, 0);
        }
        // Ordinary top-down rooms use the source airborne guard before room
        // transition initiation. Side-scrolling rooms take a separate source
        // path below and intentionally remain exempt from this guard.
        if (state.linkAirborne() && state.mapCategory() != Warp.CATEGORY_SIDESCROLL) {
            return RoomBoundaryDecision.none();
        }
        // Tail Cave's room $19 follows the ordinary side-view fade path in
        // CheckPositionForMapTransition. Other category-2 rooms include source
        // exceptions and remain on the existing indoor path until their state
        // (physics modifier, entities, and map-specific rules) is represented.
        if (supportedTailCaveSideView) {
            if (y < -0x04 || y >= 0x74) {
                return RoomBoundaryDecision.sideScrollVerticalWarp();
            }
            if (offTop || offBottom) {
                return RoomBoundaryDecision.none();
            }
            if ((offLeft && x >= -0x04) || (offRight && x < 0x94)) {
                return RoomBoundaryDecision.none();
            }
        }
        if (offBottom && state.indoorHasSouthEntrance() && state.hasWarps()) {
            return RoomBoundaryDecision.indoorFrontDoorWarp();
        }
        if (offLeft) {
            if ((state.shutterDoorMask() & 0x04) != 0) {
                return RoomBoundaryDecision.clamp(0, y);
            }
            return RoomBoundaryDecision.indoorScroll(ScrollController.LEFT,
                ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE, y);
        }
        if (offRight) {
            if ((state.shutterDoorMask() & 0x08) != 0) {
                return RoomBoundaryDecision.clamp(ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE, y);
            }
            return RoomBoundaryDecision.indoorScroll(ScrollController.RIGHT, 0, y);
        }
        if (offTop) {
            return RoomBoundaryDecision.indoorScroll(ScrollController.UP, x,
                ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE);
        }
        if (offBottom) {
            return RoomBoundaryDecision.indoorScroll(ScrollController.DOWN, x, 0);
        }
        return RoomBoundaryDecision.none();
    }
}
