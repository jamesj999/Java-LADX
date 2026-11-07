package linksawakening.render;

import linksawakening.world.ScrollController;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_WIDTH;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_HEIGHT;

/** Immutable render-input snapshots and screen offsets, shared by entity submission paths. */
final class EntityRenderInputs {
    private EntityRenderInputs() {}
    record Offset(int x, int y) {}
    static Offset currentRoomOffset(ScrollController scrollController) {
        int shakeX = -scrollController.screenShakeHorizontal();
        if (!scrollController.isActive()) {
            return new Offset(shakeX, 0);
        }
        int offset = scrollController.offset();
        return switch (scrollController.direction()) {
            case ScrollController.LEFT -> new Offset(-ROOM_PIXEL_WIDTH + offset + shakeX, 0);
            case ScrollController.RIGHT -> new Offset(ROOM_PIXEL_WIDTH - offset + shakeX, 0);
            case ScrollController.UP -> new Offset(shakeX, -ROOM_PIXEL_HEIGHT + offset);
            case ScrollController.DOWN -> new Offset(shakeX, ROOM_PIXEL_HEIGHT - offset);
            default -> new Offset(shakeX, 0);
        };
    }

    static Offset previousRoomOffset(ScrollController scrollController) {
        int shakeX = -scrollController.screenShakeHorizontal();
        if (!scrollController.isActive()) {
            return new Offset(shakeX, 0);
        }
        int offset = scrollController.offset();
        return switch (scrollController.direction()) {
            case ScrollController.LEFT -> new Offset(offset + shakeX, 0);
            case ScrollController.RIGHT -> new Offset(-offset + shakeX, 0);
            case ScrollController.UP -> new Offset(shakeX, offset);
            case ScrollController.DOWN -> new Offset(shakeX, -offset);
            default -> new Offset(shakeX, 0);
        };
    }

    static int[][] clonePalettes(int[][] source) {
        int[][] copy = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }
        return copy;
    }

    static int[] resolvePalette(int[][] palettes, int paletteIndex) {
        if (paletteIndex < 0 || paletteIndex >= palettes.length) {
            throw new IllegalArgumentException("Object palette index is unavailable: "
                + paletteIndex);
        }
        return palettes[paletteIndex];
    }
}
