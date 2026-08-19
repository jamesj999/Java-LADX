package linksawakening.world;

import linksawakening.entity.Link;

import static linksawakening.world.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_WIDTH;

public final class RoomBoundaryController {
    private static final int OVERWORLD_COLUMNS = 16;
    private static final int OVERWORLD_ROWS = 16;
    private static final int SIDE_VIEW_NO_FADE_PHYSICS_MODIFIER = 0x02;
    private static final int SIDE_VIEW_VERTICAL_TOP = -0x04;
    private static final int SIDE_VIEW_VERTICAL_BOTTOM = 0x74;

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
        boolean sideScrolling = state.mapCategory() == Warp.CATEGORY_SIDESCROLL;
        boolean sideScrollingWithWarp = sideScrolling && state.hasWarps();
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
        // CheckPositionForMapTransition has a small side-view margin in which
        // Link is off the visible room but the source does not start an
        // ordinary indoor scroll. The source's vertical path then applies
        // room exceptions and hLinkPhysicsModifier before its generic fade.
        if (sideScrolling) {
            boolean inVerticalMargin = (offTop && y >= SIDE_VIEW_VERTICAL_TOP)
                || (offBottom && y < SIDE_VIEW_VERTICAL_BOTTOM);
            if (inVerticalMargin) {
                return RoomBoundaryDecision.none();
            }
            if (y < SIDE_VIEW_VERTICAL_TOP || y >= SIDE_VIEW_VERTICAL_BOTTOM) {
                RoomBoundaryDecision sideViewDecision = sideViewVerticalDecision(state,
                    sideScrollingWithWarp);
                if (sideViewDecision != null) {
                    return sideViewDecision;
                }
            }
        }
        if (supportedTailCaveSideView) {
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

    private static RoomBoundaryDecision sideViewVerticalDecision(RoomBoundaryState state,
                                                                  boolean hasWarps) {
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
        if (roomId == 0xFF
            && (romY >= 0x50 || state.sideViewEntityActive())) {
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
