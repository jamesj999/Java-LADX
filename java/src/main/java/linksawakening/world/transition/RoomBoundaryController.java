package linksawakening.world.transition;

import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_WIDTH;

import linksawakening.entity.Link;

public final class RoomBoundaryController {
    private static final int OVERWORLD_COLUMNS = 16;
    private static final int OVERWORLD_ROWS = 16;
    private static final int SIDE_VIEW_NO_FADE_PHYSICS_MODIFIER = 0x02;
    // CheckPositionForMapTransition ($02:6C99) uses the sprite's ROM center X and bottom Y.
    private static final int SCROLL_LEFT = 0x04 - 8;
    private static final int SCROLL_RIGHT = 0x9C - 8;
    private static final int SCROLL_TOP = 0x0C - 16;
    private static final int SCROLL_BOTTOM = 0x84 - 16;

    public RoomBoundaryDecision decide(RoomBoundaryView state) {
        if (state.mapCategory() == Warp.CATEGORY_OVERWORLD) {
            return decideOverworld(state);
        }
        return decideIndoor(state);
    }

    private static RoomBoundaryDecision decideOverworld(RoomBoundaryView state) {
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

        if (y < SCROLL_TOP && roomRow > 0) {
            return adjacentScroll(RoomBoundaryDecision.Type.OVERWORLD_SCROLL, ScrollController.UP, x, y);
        }
        if (y >= SCROLL_BOTTOM && roomRow < OVERWORLD_ROWS - 1) {
            return adjacentScroll(RoomBoundaryDecision.Type.OVERWORLD_SCROLL, ScrollController.DOWN, x, y);
        }
        if (x < SCROLL_LEFT && roomCol > 0) {
            return adjacentScroll(RoomBoundaryDecision.Type.OVERWORLD_SCROLL, ScrollController.LEFT, x, y);
        }
        if (x >= SCROLL_RIGHT && roomCol < OVERWORLD_COLUMNS - 1) {
            return adjacentScroll(RoomBoundaryDecision.Type.OVERWORLD_SCROLL, ScrollController.RIGHT, x, y);
        }

        // Interior rooms must allow the off-screen margin required above.
        // Only the outer world border clamps movement without a destination.
        int clampedX = roomCol == 0 ? Math.max(0, x) : x;
        if (roomCol == OVERWORLD_COLUMNS - 1) clampedX = Math.min(clampedX, ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE);
        int clampedY = roomRow == 0 ? Math.max(0, y) : y;
        if (roomRow == OVERWORLD_ROWS - 1) clampedY = Math.min(clampedY, ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE);
        if (clampedX != x || clampedY != y) {
            return RoomBoundaryDecision.clamp(clampedX, clampedY);
        }
        return RoomBoundaryDecision.none();
    }

    private static RoomBoundaryDecision decideIndoor(RoomBoundaryView state) {
        int x = state.linkX();
        int y = state.linkY();
        boolean offBottom = y + Link.SPRITE_SIZE > ROOM_PIXEL_HEIGHT;
        boolean offTop = y < 0;
        boolean offLeft = x < 0;
        boolean offRight = x + Link.SPRITE_SIZE > ROOM_PIXEL_WIDTH;
        boolean sideScrolling = state.mapCategory() == Warp.CATEGORY_SIDESCROLL;
        if (offBottom && (state.shutterDoorMask() & 0x02) != 0) {
            return RoomBoundaryDecision.clamp(x, ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE);
        }
        if (offTop && (state.shutterDoorMask() & 0x01) != 0) {
            return RoomBoundaryDecision.clamp(x, 0);
        }
        // Ordinary top-down rooms use the source airborne guard before room
        // transition initiation. Side-scrolling rooms take a separate source
        // path below and intentionally remain exempt from this guard.
        if (state.linkAirborne() && !sideScrolling) {
            return RoomBoundaryDecision.none();
        }
        // Side-view rooms can warp instead of scrolling at a vertical edge ($02:6CD1).
        if (sideScrolling && (y < SCROLL_TOP || y >= SCROLL_BOTTOM)) {
            RoomBoundaryDecision sideViewDecision = sideViewVerticalDecision(state, state.hasWarps());
            if (sideViewDecision != null) return sideViewDecision;
        }
        if (!sideScrolling && offBottom && state.indoorHasSouthEntrance() && state.hasWarps()) {
            return RoomBoundaryDecision.indoorFrontDoorWarp();
        }
        if (offLeft && (state.shutterDoorMask() & 0x04) != 0) {
            return RoomBoundaryDecision.clamp(0, y);
        }
        if (offRight && (state.shutterDoorMask() & 0x08) != 0) {
            return RoomBoundaryDecision.clamp(ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE, y);
        }
        if (y < SCROLL_TOP) {
            return adjacentScroll(RoomBoundaryDecision.Type.INDOOR_SCROLL, ScrollController.UP, x, y);
        }
        if (y >= SCROLL_BOTTOM) {
            return adjacentScroll(RoomBoundaryDecision.Type.INDOOR_SCROLL, ScrollController.DOWN, x, y);
        }
        if (x < SCROLL_LEFT) {
            return adjacentScroll(RoomBoundaryDecision.Type.INDOOR_SCROLL, ScrollController.LEFT, x, y);
        }
        if (x >= SCROLL_RIGHT) {
            return adjacentScroll(RoomBoundaryDecision.Type.INDOOR_SCROLL, ScrollController.RIGHT, x, y);
        }
        return RoomBoundaryDecision.none();
    }

    private static RoomBoundaryDecision adjacentScroll(RoomBoundaryDecision.Type type, int direction, int x, int y) {
        // ApplyRoomTransition integrates $3A/16 for 40 horizontal or 32 vertical steps.
        return switch (direction) {
            case ScrollController.UP -> new RoomBoundaryDecision(type, direction, x, y + 116);
            case ScrollController.DOWN -> new RoomBoundaryDecision(type, direction, x, y - 116);
            case ScrollController.LEFT -> new RoomBoundaryDecision(type, direction, x + 145, y);
            case ScrollController.RIGHT -> new RoomBoundaryDecision(type, direction, x - 145, y);
            default -> throw new IllegalArgumentException("Room scroll direction " + direction);
        };
    }

    private static RoomBoundaryDecision sideViewVerticalDecision(RoomBoundaryView state, boolean hasWarps) {
        int roomId = state.roomId() & 0xFF;
        if (roomId == 0xA3 || roomId == 0xC0 || roomId == 0xC1) {
            // The source starts ApplyMapFadeOutTransitionWithNoise here.
            // Its map-level destination/reload is not represented by the
            // room boundary API, so suppress every room fallback.
            return RoomBoundaryDecision.none();
        }
        if (roomId == 0xE8 || roomId == 0xF8 || roomId == 0xFD) {
            return RoomBoundaryDecision.none();
        }
        // Source room $FF suppresses the fallback when Link's ROM Y is at or
        // past $50, or when its first entity slot is active. Only an inactive
        // upper-room $FF position continues to the generic physics path.
        int romY = (state.linkY() + 0x10) & 0xFF;
        if (roomId == 0xFF && (romY >= 0x50 || state.sideViewEntityActive())) {
            return RoomBoundaryDecision.none();
        }
        if (!hasWarps) {
            return null;
        }
        if ((state.linkPhysicsModifier() & 0xFF) == SIDE_VIEW_NO_FADE_PHYSICS_MODIFIER) {
            // Modifier $02 skips the source generic map fade, then falls
            // through to the ordinary adjacent-room boundary path below.
            return null;
        }
        return RoomBoundaryDecision.sideScrollVerticalWarp();
    }
}
