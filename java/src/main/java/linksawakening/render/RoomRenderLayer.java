package linksawakening.render;

import linksawakening.world.RoomRenderSnapshot;
import linksawakening.world.ScrollController;
import linksawakening.world.TransitionController;

import static linksawakening.world.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_WIDTH;

public final class RoomRenderLayer implements RenderLayer {
    private final RoomRenderSnapshot currentRoom;
    private final ScrollController scrollController;
    private final TransitionController transitionController;

    public RoomRenderLayer(RoomRenderSnapshot currentRoom,
                           ScrollController scrollController,
                           TransitionController transitionController) {
        this.currentRoom = currentRoom;
        this.scrollController = scrollController;
        this.transitionController = transitionController;
    }

    @Override
    public void render(RenderContext context) {
        if (!scrollController.isActive()) {
            int[][] palettes = transitionController.applyFade(currentRoom.palettes());
            IndexedRenderer.renderRoom(context.buffer(), context.gpu(), currentRoom.tileIds(),
                currentRoom.tileAttrs(), palettes, 0, 0);
            return;
        }

        RoomOffsets offsets = roomOffsets();
        RoomRenderSnapshot previousRoom = scrollController.previousRoom();
        if (previousRoom != null) {
            IndexedRenderer.renderRoom(context.buffer(), context.gpu(), previousRoom.tileIds(),
                previousRoom.tileAttrs(), previousRoom.palettes(), offsets.previousX(), offsets.previousY());
        }
        IndexedRenderer.renderRoom(context.buffer(), context.gpu(), currentRoom.tileIds(),
            currentRoom.tileAttrs(), currentRoom.palettes(), offsets.currentX(), offsets.currentY());
    }

    private RoomOffsets roomOffsets() {
        int previousX = 0;
        int previousY = 0;
        int currentX = 0;
        int currentY = 0;

        switch (scrollController.direction()) {
            case ScrollController.LEFT:
                previousX = scrollController.offset();
                currentX = -ROOM_PIXEL_WIDTH + scrollController.offset();
                break;
            case ScrollController.RIGHT:
                previousX = -scrollController.offset();
                currentX = ROOM_PIXEL_WIDTH - scrollController.offset();
                break;
            case ScrollController.UP:
                previousY = scrollController.offset();
                currentY = -ROOM_PIXEL_HEIGHT + scrollController.offset();
                break;
            case ScrollController.DOWN:
                previousY = -scrollController.offset();
                currentY = ROOM_PIXEL_HEIGHT - scrollController.offset();
                break;
            default:
                break;
        }
        return new RoomOffsets(previousX, previousY, currentX, currentY);
    }

    private record RoomOffsets(int previousX, int previousY, int currentX, int currentY) {
    }
}
