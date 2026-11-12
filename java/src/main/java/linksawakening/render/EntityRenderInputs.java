package linksawakening.render;

import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_WIDTH;

import linksawakening.world.transition.ScrollController;

/** Palette ownership and reusable screen offsets shared by entity submission paths. */
final class EntityRenderInputs {
    private EntityRenderInputs() {}

    static final class Offset {
        private int x, y;

        Offset(int x, int y) {
            this.x = x;
            this.y = y;
        }

        int x() {
            return x;
        }

        int y() {
            return y;
        }

        Offset set(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }
    }

    static Offset currentRoomOffset(ScrollController scrollController) {
        return currentRoomOffset(scrollController, new Offset(0, 0));
    }

    static Offset currentRoomOffset(ScrollController scrollController, Offset out) {
        int shakeX = -scrollController.screenShakeHorizontal();
        if (!scrollController.isActive()) {
            return out.set(shakeX, 0);
        }
        int offset = scrollController.offset();
        return switch (scrollController.direction()) {
            case ScrollController.LEFT -> out.set(-ROOM_PIXEL_WIDTH + offset + shakeX, 0);
            case ScrollController.RIGHT -> out.set(ROOM_PIXEL_WIDTH - offset + shakeX, 0);
            case ScrollController.UP -> out.set(shakeX, -ROOM_PIXEL_HEIGHT + offset);
            case ScrollController.DOWN -> out.set(shakeX, ROOM_PIXEL_HEIGHT - offset);
            default -> out.set(shakeX, 0);
        };
    }

    static Offset previousRoomOffset(ScrollController scrollController) {
        return previousRoomOffset(scrollController, new Offset(0, 0));
    }

    static Offset previousRoomOffset(ScrollController scrollController, Offset out) {
        int shakeX = -scrollController.screenShakeHorizontal();
        if (!scrollController.isActive()) {
            return out.set(shakeX, 0);
        }
        int offset = scrollController.offset();
        return switch (scrollController.direction()) {
            case ScrollController.LEFT -> out.set(offset + shakeX, 0);
            case ScrollController.RIGHT -> out.set(-offset + shakeX, 0);
            case ScrollController.UP -> out.set(shakeX, offset);
            case ScrollController.DOWN -> out.set(shakeX, -offset);
            default -> out.set(shakeX, 0);
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
            throw new IllegalArgumentException("Object palette index is unavailable: " + paletteIndex);
        }
        return palettes[paletteIndex];
    }
}
