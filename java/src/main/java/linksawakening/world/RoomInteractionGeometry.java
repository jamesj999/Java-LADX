package linksawakening.world;

import linksawakening.world.entity.RoomEntityGroundInteraction;
import linksawakening.world.entity.RoomEntity;

import linksawakening.entity.Link;
import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_WIDTH;
import static linksawakening.world.RoomSession.*;

/** Pure coordinate, direction, and collision conversions. */
final class RoomInteractionGeometry {
    static int romDirectionForProjectileCollision(int linkDirection) {
        if (linkDirection < 0 || linkDirection > 3) {
            throw new IllegalArgumentException("Link direction out of range: " + linkDirection);
        }
        return switch (linkDirection) {
            case 0 -> 3; // Java down -> ROM down
            case 1 -> 2; // Java up -> ROM up
            case 2 -> 1; // Java left -> ROM left
            case 3 -> 0; // Java right -> ROM right
            default -> throw new AssertionError(linkDirection);
        };
    }

    static boolean alignedWithIndoorKeyDoor(int location, int direction,
                                                    int linkPixelX, int linkPixelY) {
        int offset = direction < 2
            ? linkPixelX + 8 - ((location & 15) << 4) - 0x0C
            : linkPixelY + 16 - (location & 0xF0) - 0x14;
        return (offset & 0xFF) < 8;
    }

    static int keyDoorDirectionIndex(int linkDirection) {
        return switch (linkDirection) {
            case Link.DIRECTION_UP -> 0;
            case Link.DIRECTION_DOWN -> 1;
            case Link.DIRECTION_LEFT -> 2;
            case Link.DIRECTION_RIGHT -> 3;
            default -> -1;
        };
    }

    static int keyDoorCollisionBit(int direction) {
        return switch (direction) {
            case 0 -> 0x01;
            case 1 -> 0x02;
            case 2 -> 0x04;
            case 3 -> 0x08;
            default -> 0;
        };
    }

    static int[][] keyDoorCollisionPoints(int x, int y, int direction) {
        return switch (direction) {
            case 0 -> new int[][] {{x + 0x06, y + 0x06}, {x + 0x09, y + 0x06}};
            case 1 -> new int[][] {{x + 0x06, y + 0x0F}, {x + 0x09, y + 0x0F}};
            case 2 -> new int[][] {{x + 0x04, y + 0x09}, {x + 0x04, y + 0x0C}};
            case 3 -> new int[][] {{x + 0x0B, y + 0x09}, {x + 0x0B, y + 0x0C}};
            default -> new int[0][0];
        };
    }

    static boolean withinUnsignedByteRange(int value, int center,
                                                   int bias, int size) {
        return ((value - center + bias) & 0xFF) < size;
    }

    static int roomEventChestTop(int linkEntityX, int linkEntityY) {
        boolean linkNearUpperChest = ((linkEntityY - 0x30 + 0x08) & 0xFF) < 0x10
            && ((linkEntityX - 0x88 + 0x10) & 0xFF) < 0x20;
        return linkNearUpperChest ? 0x30 : 0x20;
    }

    static void requireLength(byte[] values, int expectedLength, String label) {
        if (values == null) {
            throw new NullPointerException(label);
        }
        if (values.length != expectedLength) {
            throw new IllegalArgumentException(label + " must contain " + expectedLength
                + " bytes, got " + values.length);
        }
    }

    static int hookshotObjectAreaIndex(int objectLeft, int objectTop) {
        return RoomConstants.ROOM_OBJECTS_BASE + (objectTop & 0xF0)
            + ((objectLeft & 0xF0) >>> 4);
    }

    static int colorShellObjectIndex(RoomEntity entity) {
        int x = (entity.x() - 0x01) & 0xFF;
        int y = (entity.y() - 0x07) & 0xFF;
        return RoomConstants.ROOM_OBJECTS_BASE + (y & 0xF0) + ((x & 0xF0) >>> 4);
    }

    static RoomEntityGroundInteraction.PitTransition pitTransitionFor(
            RoomEntity entity, OverworldCollision.GroundInteractionSample sample,
            int linkMotionState) {
        boolean pit = sample.objectId() == OBJECT_WELL
            || sample.physicsFlag() == PhysicsFlags.NORMAL_PIT
            || sample.physicsFlag() == PhysicsFlags.PIT_WARP;
        if (!pit || entity.type() == ENTITY_BOW_WOW || entity.type() == ENTITY_ROOSTER
            || entity.type() == ENTITY_HEART_CONTAINER) {
            return null;
        }
        if (entity.type() == ENTITY_MARIN_AT_THE_SHORE
            && (linkMotionState != LINK_MOTION_FALLING_DOWN
                || sample.objectId() != OBJECT_WELL)) {
            return null;
        }
        return new RoomEntityGroundInteraction.PitTransition(
            sample.objectLeft() + 0x08, sample.objectTop() + 0x10);
    }

    static int groundStatusFor(OverworldCollision.GroundInteractionSample sample) {
        int physicsFlag = sample.physicsFlag();
        if (physicsFlag == PhysicsFlags.DEEP_WATER || physicsFlag == PhysicsFlags.LAVA) {
            return 0x02;
        }
        if (sample.objectId() == OBJECT_WATER_LADDER_SIDESCROLL
            || physicsFlag == PhysicsFlags.WATER_SIDESCROLL) {
            return 0x01;
        }
        if (physicsFlag == PhysicsFlags.NONE) {
            return 0x00;
        }
        if (physicsFlag == PhysicsFlags.SHALLOW_WATER) {
            return 0x02;
        }
        if (physicsFlag == PhysicsFlags.GRASS) {
            return 0x03;
        }
        return 0x01;
    }

    static boolean splashAllowed(int options, int previousGroundStatus,
                                         int currentGroundStatus, int speedZ,
                                         boolean sideScrolling) {
        if ((options & ENTITY_OPT1_SPLASH_IN_WATER) == 0
            || previousGroundStatus == currentGroundStatus
            || previousGroundStatus == 0x03
            || currentGroundStatus == 0x03) {
            return false;
        }
        if (sideScrolling) {
            // The side-scroll branch uses shared speed-X/Y tables, which are
            // not yet part of the Java entity snapshot. Keep this path
            // source-safe until that table is ported.
            return false;
        }
        int unsignedSpeedZ = speedZ & 0xFF;
        return (unsignedSpeedZ & 0x80) != 0 && unsignedSpeedZ < 0xE7;
    }

    static RoomEntity withEntityPosition(RoomEntity entity, int x, int y) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(),
            entity.spriteVariant(), entity.entityFlipAttribute(), entity.spriteTileOffset(),
            entity.z(), -1, false);
    }

    static int scrollTarget(int direction) { return RoomScrollNavigation.scrollTarget(direction); }
}
