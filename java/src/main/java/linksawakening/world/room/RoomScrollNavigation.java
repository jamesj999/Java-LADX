package linksawakening.world.room;







import linksawakening.world.transition.ScrollController;

import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_WIDTH;

/** Resolves scroll and bomb-passage neighbors through the ROM map layout. */
final class RoomScrollNavigation {
    private static final int INDOOR_MAP_LAYOUT_SIZE = 0x40;
    private final byte[] romData;
    private final RoomWorldState roomState;

    RoomScrollNavigation(byte[] romData, RoomWorldState roomState) {
        this.romData = romData;
        this.roomState = roomState;
    }

    record IndoorRoomDestination(int roomId, int mapPosition) {}

    int adjacentOverworldRoomId(ActiveRoom activeRoom, int direction) {
        int nextRoomId = activeRoom.roomId();
        switch (direction) {
            case ScrollController.UP:
                return nextRoomId - 16;
            case ScrollController.DOWN:
                return nextRoomId + 16;
            case ScrollController.LEFT:
                return nextRoomId - 1;
            case ScrollController.RIGHT:
                return nextRoomId + 1;
            default:
                return nextRoomId;
        }
    }

    IndoorRoomDestination adjacentIndoorRoom(ActiveRoom activeRoom, int indoorMapPosition, int direction) {
        int layoutDelta = switch (direction) {
            case ScrollController.UP -> -8;
            case ScrollController.DOWN -> 8;
            case ScrollController.LEFT -> -1;
            case ScrollController.RIGHT -> 1;
            default -> 0;
        };

        // The ROM increments wIndoorRoom, which is the room's 8x8 layout
        // position, then resolves the actual hMapRoom byte through MapLayoutN
        // (room_transition.asm:$7A4C and bank14.asm:$5897). Room ids themselves
        // are not spatially ordered: Tail Cave room $17 north is room $13,
        // not $0F.
        int currentMapPosition = indoorMapPosition >= 0
            ? indoorMapPosition
            : roomState.findIndoorMapPosition(activeRoom.mapId(), activeRoom.roomId());
        int adjacentPosition = currentMapPosition + layoutDelta;
        int layoutOffset = roomState.indoorMapLayoutOffset(activeRoom.mapId());
        if (currentMapPosition >= 0 && adjacentPosition >= 0
            && adjacentPosition < INDOOR_MAP_LAYOUT_SIZE && layoutOffset >= 0) {
            return new IndoorRoomDestination(
                Byte.toUnsignedInt(romData[layoutOffset + adjacentPosition]),
                adjacentPosition);
        }

        // room_transition.asm:$7A0C skips the wIndoorRoom/MapLayout path for
        // maps >= $0B. Those maps keep hMapRoom directly and fall through to
        // OverworldRoomIncrement, whose vertical row stride is $10.
        int directRoomDelta = switch (direction) {
            case ScrollController.UP -> -0x10;
            case ScrollController.DOWN -> 0x10;
            case ScrollController.LEFT -> -1;
            case ScrollController.RIGHT -> 1;
            default -> 0;
        };
        return new IndoorRoomDestination(
            (activeRoom.roomId() + directRoomDelta) & 0xFF, -1);
    }

    int adjacentIndoorRoomIdForBombWall(ActiveRoom activeRoom, int indoorMapPosition, int wallIndex) {
        int mapPosition = indoorMapPosition >= 0
            ? indoorMapPosition
            : roomState.findIndoorMapPosition(activeRoom.mapId(), activeRoom.roomId());
        int mapPositionDelta = switch (wallIndex) {
            case 0 -> -0x08;
            case 1 -> 0x08;
            case 2 -> -0x01;
            case 3 -> 0x01;
            default -> throw new AssertionError(wallIndex);
        };
        if (mapPosition >= 0) {
            int adjacentPosition = mapPosition + mapPositionDelta;
            if (adjacentPosition >= 0 && adjacentPosition < INDOOR_MAP_LAYOUT_SIZE) {
                int layoutOffset = roomState.indoorMapLayoutOffset(activeRoom.mapId());
                if (layoutOffset >= 0 && layoutOffset + adjacentPosition < romData.length) {
                    int roomId = Byte.toUnsignedInt(romData[layoutOffset + adjacentPosition]);
                    if (roomId != 0) {
                        return roomId;
                    }
                }
            }
        }

        // Some small cave/house maps have no spatial layout table. Preserve the
        // source's byte-grid fallback for those rooms.
        return (activeRoom.roomId() + mapPositionDelta) & 0xFF;
    }

    static int scrollTarget(int direction) {
        return (direction == ScrollController.LEFT || direction == ScrollController.RIGHT)
            ? ROOM_PIXEL_WIDTH
            : ROOM_PIXEL_HEIGHT;
    }
}
