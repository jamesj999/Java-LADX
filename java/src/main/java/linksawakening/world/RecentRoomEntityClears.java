package linksawakening.world;

import java.util.Arrays;

/** Mirrors wRecentRooms, wRecentRoomsIndex, and wEntitiesClearedRooms. */
final class RecentRoomEntityClears {
    private static final int RECENT_ROOM_COUNT = 6;

    private final int[] masks = new int[0x100];
    private final int[] recentRooms = new int[RECENT_ROOM_COUNT];
    private int recentRoomsIndex;

    void visit(int roomId) {
        validateRoom(roomId);
        for (int recentRoom : recentRooms) {
            if (recentRoom == roomId) {
                return;
            }
        }
        recentRoomsIndex = (recentRoomsIndex + 1) % RECENT_ROOM_COUNT;
        int evictedRoom = recentRooms[recentRoomsIndex];
        recentRooms[recentRoomsIndex] = roomId;
        masks[evictedRoom] = 0;
    }

    int mask(int roomId) {
        validateRoom(roomId);
        return masks[roomId];
    }

    void addMask(int roomId, int mask) {
        validateRoom(roomId);
        masks[roomId] |= mask;
    }

    void reset() {
        Arrays.fill(masks, 0);
        Arrays.fill(recentRooms, 0);
        recentRoomsIndex = 0;
    }

    private static void validateRoom(int roomId) {
        if ((roomId & ~0xFF) != 0) {
            throw new IllegalArgumentException("Room id must be an unsigned byte: " + roomId);
        }
    }
}
