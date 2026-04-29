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

        if (offBottom && state.indoorHasSouthEntrance() && state.hasWarps()) {
            return RoomBoundaryDecision.indoorFrontDoorWarp();
        }
        if (offLeft) {
            return RoomBoundaryDecision.indoorScroll(ScrollController.LEFT,
                ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE, y);
        }
        if (offRight) {
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
