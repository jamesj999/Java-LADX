package linksawakening.world.room;

import static linksawakening.world.room.RoomConstants.ROOM_OBJECTS_BASE;
import static linksawakening.world.room.RoomConstants.ROOM_OBJECT_ROW_STRIDE;
import static linksawakening.world.room.RoomTransitionTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.function.BiPredicate;
import linksawakening.entity.Link;
import linksawakening.equipment.ItemRegistry;
import linksawakening.equipment.Sword;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomTables;
import linksawakening.state.PlayerState;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.Warp;

final class RoomTransitionTestSupport2 {
    static void jumpOverPitWithFeather(
            OverworldCollision collision,
            Link link,
            InputState inputState,
            InputConfig inputConfig,
            ItemRegistry itemRegistry,
            PlayerState playerState,
            int joypadMask) {
        setDirectionalInput(inputState, inputConfig, joypadMask, GLFW_PRESS);
        itemRegistry.lookup(playerState.itemA()).onPress();
        assertTrue(link.isAirborne());
        boolean crossedPit = false;
        int deadline = 0x40;
        while (link.isAirborne() && deadline-- > 0) {
            link.update();
            crossedPit |= collision.linkOnNormalPit(link.pixelX(), link.pixelY());
        }
        setDirectionalInput(inputState, inputConfig, joypadMask, GLFW_RELEASE);
        assertTrue(crossedPit);
        assertFalse(link.isAirborne());
        assertFalse(link.isFallingIntoPit());
        assertFalse(collision.linkOnNormalPit(link.pixelX(), link.pixelY()));
        link.update();
        assertFalse(link.isFallingIntoPit());
    }

    static List<int[]> reachableBoundaryPathWithFeather(
            OverworldCollision collision, RomTables romTables, int startX, int startY, int targetDirection) {
        return reachablePositionPathWithFeather(
                collision,
                romTables,
                startX,
                startY,
                (x, y) -> isTargetBoundary(
                        x,
                        y,
                        RoomConstants.ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE + 1,
                        RoomConstants.ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE + 1,
                        targetDirection));
    }

    static List<int[]> reachablePositionPathWithFeather(
            OverworldCollision collision,
            RomTables romTables,
            int startX,
            int startY,
            java.util.function.BiPredicate<Integer, Integer> target) {
        int width = RoomConstants.ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE + 1;
        int height = RoomConstants.ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE + 1;
        boolean[][] visited = new boolean[height][width];
        int[][] parentX = new int[height][width];
        int[][] parentY = new int[height][width];
        int[][] parentAction = new int[height][width];
        for (int row = 0; row < height; row++) {
            java.util.Arrays.fill(parentX[row], -1);
            java.util.Arrays.fill(parentY[row], -1);
            java.util.Arrays.fill(parentAction[row], -1);
        }
        int clampedX = Math.max(0, Math.min(startX, width - 1));
        int clampedY = Math.max(0, Math.min(startY, height - 1));
        visited[clampedY][clampedX] = true;
        parentX[clampedY][clampedX] = clampedX;
        parentY[clampedY][clampedX] = clampedY;
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] {clampedX, clampedY});
        int[][] moves = {
            {0, -1, Link.DIRECTION_UP},
            {0, 1, Link.DIRECTION_DOWN},
            {-1, 0, Link.DIRECTION_LEFT},
            {1, 0, Link.DIRECTION_RIGHT}
        };
        int[] jumpMasks = {0x00, 0x04, 0x08, 0x02, 0x01, 0x06, 0x05, 0x0A, 0x09};

        while (!queue.isEmpty()) {
            int[] position = queue.removeFirst();
            if (target.test(position[0], position[1])) {
                ArrayDeque<int[]> reverse = new ArrayDeque<>();
                int x = position[0];
                int y = position[1];
                while (parentX[y][x] != x || parentY[y][x] != y) {
                    reverse.addFirst(new int[] {x, y, parentAction[y][x]});
                    int nextX = parentX[y][x];
                    int nextY = parentY[y][x];
                    x = nextX;
                    y = nextY;
                }
                reverse.addFirst(new int[] {x, y, -1});
                return List.copyOf(reverse);
            }

            for (int[] move : moves) {
                int x = position[0] + move[0];
                int y = position[1] + move[1];
                if (x < 0
                        || x >= width
                        || y < 0
                        || y >= height
                        || visited[y][x]
                        || leadingEdgeBlocked(collision, x, y, move[2], true)
                        || collision.linkOnNormalPit(x, y)) {
                    continue;
                }
                visited[y][x] = true;
                parentX[y][x] = position[0];
                parentY[y][x] = position[1];
                parentAction[y][x] = move[2];
                queue.addLast(new int[] {x, y});
            }

            for (int firstMask : jumpMasks) {
                for (int secondMask : jumpMasks) {
                    for (int thirdMask : jumpMasks) {
                        int subX = position[0] << Link.SUB_PIXEL_SHIFT;
                        int subY = position[1] << Link.SUB_PIXEL_SHIFT;
                        boolean crossedPit = false;
                        boolean blockedJump = false;
                        int speedX = (byte) romTables.linkSpeedX(firstMask);
                        int speedY = (byte) romTables.linkSpeedY(firstMask);
                        for (int frame = 0; frame < 33; frame++) {
                            int joypadMask = frame < 11 ? firstMask : frame < 22 ? secondMask : thirdMask;
                            if (speedX != 0) {
                                int candidateSubX = subX + speedX;
                                int nextX = candidateSubX >> Link.SUB_PIXEL_SHIFT;
                                int y = subY >> Link.SUB_PIXEL_SHIFT;
                                int moveDirection = speedX < 0 ? Link.DIRECTION_LEFT : Link.DIRECTION_RIGHT;
                                if (nextX >= 0
                                        && nextX < width
                                        && !leadingEdgeBlocked(collision, nextX, y, moveDirection, false)) {
                                    subX = candidateSubX;
                                } else {
                                    blockedJump = true;
                                    break;
                                }
                            }
                            if (speedY != 0) {
                                int candidateSubY = subY + speedY;
                                int x = subX >> Link.SUB_PIXEL_SHIFT;
                                int nextY = candidateSubY >> Link.SUB_PIXEL_SHIFT;
                                int moveDirection = speedY < 0 ? Link.DIRECTION_UP : Link.DIRECTION_DOWN;
                                if (nextY >= 0
                                        && nextY < height
                                        && !leadingEdgeBlocked(collision, x, nextY, moveDirection, false)) {
                                    subY = candidateSubY;
                                } else {
                                    blockedJump = true;
                                    break;
                                }
                            }
                            int x = subX >> Link.SUB_PIXEL_SHIFT;
                            int y = subY >> Link.SUB_PIXEL_SHIFT;
                            crossedPit |= collision.linkOnNormalPit(x, y);
                            // ApplyLinkGroundMotion steers after XY; released axes coast.
                            if ((joypadMask & 3) != 0) {
                                speedX += Integer.compare(romTables.airSpeedX(joypadMask & 3), speedX);
                            }
                            if ((joypadMask & 12) != 0) {
                                speedY += Integer.compare(romTables.airSpeedY((joypadMask >>> 2) & 3), speedY);
                            }
                        }
                        int x = subX >> Link.SUB_PIXEL_SHIFT;
                        int y = subY >> Link.SUB_PIXEL_SHIFT;
                        if (blockedJump || !crossedPit || collision.linkOnNormalPit(x, y) || visited[y][x]) {
                            continue;
                        }
                        visited[y][x] = true;
                        parentX[y][x] = position[0];
                        parentY[y][x] = position[1];
                        parentAction[y][x] = 4 + firstMask + (secondMask << 4) + (thirdMask << 8);
                        queue.addLast(new int[] {x, y});
                    }
                }
            }
        }
        return null;
    }

    static void setDirectionalInput(InputState inputState, InputConfig inputConfig, int joypadMask, int action) {
        if ((joypadMask & 0x04) != 0) inputState.onKeyEvent(inputConfig.upKey(), action);
        if ((joypadMask & 0x08) != 0) inputState.onKeyEvent(inputConfig.downKey(), action);
        if ((joypadMask & 0x02) != 0) inputState.onKeyEvent(inputConfig.leftKey(), action);
        if ((joypadMask & 0x01) != 0) inputState.onKeyEvent(inputConfig.rightKey(), action);
    }

    static List<int[]> reachableBoundaryPath(
            OverworldCollision collision, int startX, int startY, int targetDirection) {
        return reachableBoundaryPath(collision, startX, startY, targetDirection, false);
    }

    static List<int[]> reachableEntityWindowPath(
            OverworldCollision collision, int startX, int startY, RoomEntity target) {
        return reachablePositionPath(
                collision,
                startX,
                startY,
                (x, y) -> Math.abs(signedByteDelta(target.x(), x + 8)) <= 0x28
                        && Math.abs(signedByteDelta(target.y(), y + 16)) <= 0x28);
    }

    static List<int[]> reachableFollowerLeashPath(
            OverworldCollision collision, int startX, int startY, RoomEntity bowWow) {
        return reachablePositionPath(
                collision,
                startX,
                startY,
                (x, y) -> Math.abs(signedByteDelta(bowWow.x(), x + 8)) <= 0x10
                        && Math.abs(signedByteDelta(bowWow.y(), y + 16)) <= 0x10);
    }

    static List<int[]> reachableEntityContactPath(
            OverworldCollision collision, int startX, int startY, RoomEntity entity) {
        return reachablePositionPath(
                collision,
                startX,
                startY,
                (x, y) -> Math.abs(signedByteDelta(entity.x(), x + 8)) <= 0x04
                        && Math.abs(signedByteDelta(entity.y(), y + 16)) <= 0x04);
    }

    record SwordApproach(List<int[]> path, int direction) {}

    static SwordApproach reachableSwordContactPath(
            OverworldCollision collision, int startX, int startY, RoomEntity entity, RomTables romTables) {
        Sword sword = new Sword(romTables, null);
        sword.onPress();
        for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
            sword.tick(false);
        }
        for (int direction = Link.DIRECTION_DOWN; direction <= Link.DIRECTION_RIGHT; direction++) {
            int swordDirection = direction;
            List<int[]> path = reachablePositionPath(collision, startX, startY, (x, y) -> {
                Sword.CollisionBox box = sword.enemyCollisionBox(x + 8, y + 16, swordDirection);
                return box.active()
                        && linksawakening.world.entity.EntityCombatTestAccess.overlapsSword(
                                entity, box.x(), box.width(), box.y(), box.height());
            });
            if (path != null) {
                return new SwordApproach(path, swordDirection);
            }
        }
        return null;
    }

    static SwordApproach reachableSwordContactPathWithFeather(
            OverworldCollision collision, RomTables romTables, int startX, int startY, RoomEntity entity) {
        Sword sword = new Sword(romTables, null);
        sword.onPress();
        for (int swingFrame = 0; swingFrame < 4; swingFrame++) {
            sword.tick(false);
        }
        for (int direction = Link.DIRECTION_DOWN; direction <= Link.DIRECTION_RIGHT; direction++) {
            int swordDirection = direction;
            List<int[]> path = reachablePositionPathWithFeather(collision, romTables, startX, startY, (x, y) -> {
                Sword.CollisionBox box = sword.enemyCollisionBox(x + 8, y + 16, swordDirection);
                return box.active()
                        && linksawakening.world.entity.EntityCombatTestAccess.overlapsSword(
                                entity, box.x(), box.width(), box.y(), box.height());
            });
            if (path != null) {
                return new SwordApproach(path, swordDirection);
            }
        }
        return null;
    }

    static PowderApproach reachablePowderApproach(
            OverworldCollision collision, int startX, int startY, int targetLocation) {
        int[] xOffsets = {0x0E, -0x0E, 0x00, 0x00};
        int[] yOffsets = {0x00, 0x00, -0x0C, 0x0C};
        for (int romDirection = 0; romDirection < 4; romDirection++) {
            int direction = romDirection;
            List<int[]> path = reachablePositionPath(collision, startX, startY, (x, y) -> {
                int sprinkleX = (x + 0x08 + xOffsets[direction]) & 0xFF;
                int sprinkleY = (y + 0x10 + yOffsets[direction]) & 0xFF;
                int objectLeft = ((sprinkleX - 0x01) & 0xFF) & 0xF0;
                int objectTop = ((sprinkleY - 0x09) & 0xFF) & 0xF0;
                int sampledLeft = sprinkleX & 0xF0;
                int sampledTop = ((sprinkleY - 0x08) & 0xFF) & 0xF0;
                return (objectTop | (objectLeft >>> 4)) == targetLocation
                        && (sampledTop | (sampledLeft >>> 4)) == targetLocation;
            });
            if (path != null) {
                return new PowderApproach(path, direction);
            }
        }
        return null;
    }

    record PowderApproach(List<int[]> path, int romDirection) {}

    static List<int[]> reachableWarpPath(OverworldCollision collision, int startX, int startY, int tileLocation) {
        return reachablePositionPath(collision, startX, startY, (x, y) -> Warp.packTileLocation(x, y) == tileLocation);
    }

    static List<int[]> reachablePositionPath(
            OverworldCollision collision, int startX, int startY, BiPredicate<Integer, Integer> goal) {
        int width = RoomConstants.ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE + 1;
        int height = RoomConstants.ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE + 1;
        boolean[][] visited = new boolean[height][width];
        int[][] parentX = new int[height][width];
        int[][] parentY = new int[height][width];
        int clampedX = Math.max(0, Math.min(startX, width - 1));
        int clampedY = Math.max(0, Math.min(startY, height - 1));
        visited[clampedY][clampedX] = true;
        parentX[clampedY][clampedX] = clampedX;
        parentY[clampedY][clampedX] = clampedY;
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] {clampedX, clampedY});
        int[][] moves = {
            {0, -1, Link.DIRECTION_UP},
            {0, 1, Link.DIRECTION_DOWN},
            {-1, 0, Link.DIRECTION_LEFT},
            {1, 0, Link.DIRECTION_RIGHT}
        };

        while (!queue.isEmpty()) {
            int[] position = queue.removeFirst();
            if (goal.test(position[0], position[1])) {
                ArrayDeque<int[]> reverse = new ArrayDeque<>();
                int x = position[0];
                int y = position[1];
                while (parentX[y][x] != x || parentY[y][x] != y) {
                    reverse.addFirst(new int[] {x, y});
                    int nextX = parentX[y][x];
                    int nextY = parentY[y][x];
                    x = nextX;
                    y = nextY;
                }
                reverse.addFirst(new int[] {x, y});
                return List.copyOf(reverse);
            }
            for (int[] move : moves) {
                int x = position[0] + move[0];
                int y = position[1] + move[1];
                if (x < 0
                        || x >= width
                        || y < 0
                        || y >= height
                        || visited[y][x]
                        || leadingEdgeBlocked(collision, x, y, move[2], false)) {
                    continue;
                }
                visited[y][x] = true;
                parentX[y][x] = position[0];
                parentY[y][x] = position[1];
                queue.addLast(new int[] {x, y});
            }
        }
        return null;
    }

    static List<int[]> reachableBoundaryPath(
            OverworldCollision collision, int startX, int startY, int targetDirection, boolean pitsBlock) {
        int width = RoomConstants.ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE + 1;
        int height = RoomConstants.ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE + 1;
        boolean[][] visited = new boolean[height][width];
        int[][] parentX = new int[height][width];
        int[][] parentY = new int[height][width];
        for (int row = 0; row < height; row++) {
            java.util.Arrays.fill(parentX[row], -1);
            java.util.Arrays.fill(parentY[row], -1);
        }
        int clampedX = Math.max(0, Math.min(startX, width - 1));
        int clampedY = Math.max(0, Math.min(startY, height - 1));
        visited[clampedY][clampedX] = true;
        parentX[clampedY][clampedX] = clampedX;
        parentY[clampedY][clampedX] = clampedY;
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] {clampedX, clampedY});
        int[][] moves = {
            {0, -1, Link.DIRECTION_UP},
            {0, 1, Link.DIRECTION_DOWN},
            {-1, 0, Link.DIRECTION_LEFT},
            {1, 0, Link.DIRECTION_RIGHT}
        };

        while (!queue.isEmpty()) {
            int[] position = queue.removeFirst();
            if (isTargetBoundary(position[0], position[1], width, height, targetDirection)) {
                ArrayDeque<int[]> reverse = new ArrayDeque<>();
                int x = position[0];
                int y = position[1];
                while (parentX[y][x] != x || parentY[y][x] != y) {
                    reverse.addFirst(new int[] {x, y});
                    int nextX = parentX[y][x];
                    int nextY = parentY[y][x];
                    x = nextX;
                    y = nextY;
                }
                reverse.addFirst(new int[] {x, y});
                return List.copyOf(reverse);
            }
            for (int[] move : moves) {
                int x = position[0] + move[0];
                int y = position[1] + move[1];
                if (x < 0
                        || x >= width
                        || y < 0
                        || y >= height
                        || visited[y][x]
                        || leadingEdgeBlocked(collision, x, y, move[2], pitsBlock)) {
                    continue;
                }
                visited[y][x] = true;
                parentX[y][x] = position[0];
                parentY[y][x] = position[1];
                queue.addLast(new int[] {x, y});
            }
        }
        return null;
    }

    static int[] reachableBoundaryPosition(OverworldCollision collision, int startX, int startY, int targetDirection) {
        return reachableBoundaryPosition(collision, startX, startY, targetDirection, (x, y) -> true);
    }

    static int[] reachableBoundaryPosition(
            OverworldCollision collision,
            int startX,
            int startY,
            int targetDirection,
            java.util.function.BiPredicate<Integer, Integer> landingAllowed) {
        int width = RoomConstants.ROOM_PIXEL_WIDTH - Link.SPRITE_SIZE + 1;
        int height = RoomConstants.ROOM_PIXEL_HEIGHT - Link.SPRITE_SIZE + 1;
        boolean[][] visited = new boolean[height][width];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        int clampedX = Math.max(0, Math.min(startX, width - 1));
        int clampedY = Math.max(0, Math.min(startY, height - 1));
        visited[clampedY][clampedX] = true;
        queue.add(new int[] {clampedX, clampedY});

        int[][] moves = {
            {0, -1, Link.DIRECTION_UP},
            {0, 1, Link.DIRECTION_DOWN},
            {-1, 0, Link.DIRECTION_LEFT},
            {1, 0, Link.DIRECTION_RIGHT}
        };
        while (!queue.isEmpty()) {
            int[] position = queue.removeFirst();
            if (isTargetBoundary(position[0], position[1], width, height, targetDirection)
                    && landingAllowed.test(position[0], position[1])) {
                return position;
            }
            for (int[] move : moves) {
                int x = position[0] + move[0];
                int y = position[1] + move[1];
                if (x < 0
                        || x >= width
                        || y < 0
                        || y >= height
                        || visited[y][x]
                        || leadingEdgeBlocked(collision, x, y, move[2], false)) {
                    continue;
                }
                visited[y][x] = true;
                queue.addLast(new int[] {x, y});
            }
        }
        return null;
    }

    static boolean isTargetBoundary(int x, int y, int width, int height, int direction) {
        return switch (direction) {
            case ScrollController.UP -> y == 0;
            case ScrollController.DOWN -> y == height - 1;
            case ScrollController.LEFT -> x == 0;
            case ScrollController.RIGHT -> x == width - 1;
            default -> false;
        };
    }

    static boolean leadingEdgeBlocked(OverworldCollision collision, int x, int y, int direction, boolean pitsBlock) {
        int[][] pointX = {{6, 9}, {6, 9}, {4, 4}, {11, 11}};
        int[][] pointY = {{15, 15}, {6, 6}, {9, 12}, {9, 12}};
        for (int index = 0; index < 2; index++) {
            int sampleX = x + pointX[direction][index];
            int sampleY = y + pointY[direction][index];
            int object = collision.objectIdAtPoint(sampleX, sampleY);
            // This connectivity helper models the sword-owned route after its
            // already-covered static sword collision removes a bush.
            boolean swordCuttable = object == 0x5C || object == 0xD3;
            if (pitsBlock && collision.pointNormalPit(sampleX, sampleY)) return true;
            if (!swordCuttable
                    && collision.pointBlockedForLink(sampleX, sampleY, false)
                    && (pitsBlock || !collision.pointNormalPit(sampleX, sampleY))) {
                return true;
            }
        }
        return false;
    }

    static String collisionGrid(OverworldCollision collision) {
        StringBuilder result = new StringBuilder();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 10; col++) {
                int x = col * 16 + 8;
                int y = row * 16 + 8;
                result.append(String.format(
                        "%02X%s ",
                        collision.objectIdAtPoint(x, y), collision.pointBlockedForLink(x, y, false) ? "#" : "."));
            }
            result.append('\n');
        }
        return result.toString();
    }

    static int[] roomWithPitAt(int col, int row) {
        int[] roomObjectsArea = new int[0x100];
        java.util.Arrays.fill(roomObjectsArea, 0xFF);
        roomObjectsArea[ROOM_OBJECTS_BASE + row * ROOM_OBJECT_ROW_STRIDE + col] = OBJECT_NORMAL_PIT;
        return roomObjectsArea;
    }

    static void runUntilPitFallStarts(Link link) {
        int guard = 0;
        while (!link.isFallingIntoPit() && guard++ < 128) {
            link.update();
        }
        assertTrue(link.isFallingIntoPit());
    }

    static void runUntilPitFallFinishes(Link link) {
        int guard = 0;
        while (link.isFallingIntoPit() && guard++ < 128) {
            link.update();
        }
        assertFalse(link.isFallingIntoPit());
    }

    static byte[] loadRom() throws IOException {
        try (var stream = linksawakening.testing.TestRom.open()) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        }
    }
}
