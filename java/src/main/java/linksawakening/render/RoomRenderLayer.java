package linksawakening.render;

import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_WIDTH;

import linksawakening.world.room.RoomRenderSnapshot;
import linksawakening.world.room.RoomRenderView;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

/** Submits current and previous room backgrounds with scrolling and fade offsets. */
final class RoomRenderLayer implements HardwareRenderLayer {
    private RoomRenderView currentSnapshot;
    private final ReusableRoomInputs currentInputs = new ReusableRoomInputs();
    private int[] tileIds, tileAttrs;
    private int[][] roomPalettes;
    private ScrollController scrollController;
    private TransitionController transitionController;
    private RoomRenderSnapshot previousSnapshot;
    private int[] previousTiles, previousAttributes;
    private int[][] previousPalettes;
    private final RoomOffsets reusableOffsets = new RoomOffsets(0, 0, 0, 0);

    public RoomRenderLayer(
            RoomRenderView currentRoom, ScrollController scrollController, TransitionController transitionController) {
        currentSnapshot = currentRoom;
        this.scrollController = scrollController;
        this.transitionController = transitionController;
    }

    RoomRenderLayer() {}

    void configure(
            int[] tiles, int[] attrs, int[][] palettes, ScrollController scroll, TransitionController transition) {
        currentSnapshot = null;
        tileIds = tiles;
        tileAttrs = attrs;
        roomPalettes = palettes;
        scrollController = scroll;
        transitionController = transition;
    }

    @Override
    public void submit(HardwareFrame frame, linksawakening.gpu.GPU gpu) {
        if (currentSnapshot != null) {
            currentInputs.capture(currentSnapshot);
            tileIds = currentInputs.tiles;
            tileAttrs = currentInputs.attributes;
            roomPalettes = currentInputs.palettes;
            currentSnapshot = null;
        }
        int shakeX = -scrollController.screenShakeHorizontal();
        int shakeY = -scrollController.screenShakeVertical();
        if (!scrollController.isActive()) {
            int[][] palettes = transitionController.applyFade(roomPalettes);
            int[] waveOffsets = transitionController.manboWaveOffsets(ROOM_PIXEL_HEIGHT);
            if (waveOffsets == null) {
                HardwareBackgroundRenderer.renderRoom(frame, gpu, tileIds, tileAttrs, palettes, shakeX, shakeY);
            } else {
                HardwareBackgroundRenderer.renderRoomLineScroll(
                        frame,
                        gpu,
                        tileIds,
                        tileAttrs,
                        palettes,
                        waveOffsets,
                        scrollController.screenShakeHorizontal(),
                        scrollController.screenShakeVertical());
            }
            return;
        }

        RoomOffsets offsets = roomOffsets();
        RoomRenderSnapshot previousRoom = scrollController.previousRoom();
        if (previousRoom != null) {
            if (previousRoom != previousSnapshot) {
                previousSnapshot = previousRoom;
                previousTiles = previousRoom.tileIds();
                previousAttributes = previousRoom.tileAttrs();
                previousPalettes = previousRoom.palettes();
            }
            HardwareBackgroundRenderer.renderRoom(
                    frame,
                    gpu,
                    previousTiles,
                    previousAttributes,
                    previousPalettes,
                    offsets.previousX() + shakeX,
                    offsets.previousY() + shakeY);
        }
        HardwareBackgroundRenderer.renderRoom(
                frame, gpu, tileIds, tileAttrs, roomPalettes, offsets.currentX() + shakeX, offsets.currentY() + shakeY);
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
        return reusableOffsets.set(previousX, previousY, currentX, currentY);
    }

    private static final class RoomOffsets {
        private int px, py, cx, cy;

        RoomOffsets(int px, int py, int cx, int cy) {
            set(px, py, cx, cy);
        }

        RoomOffsets set(int px, int py, int cx, int cy) {
            this.px = px;
            this.py = py;
            this.cx = cx;
            this.cy = cy;
            return this;
        }

        int previousX() {
            return px;
        }

        int previousY() {
            return py;
        }

        int currentX() {
            return cx;
        }

        int currentY() {
            return cy;
        }
    }
}
