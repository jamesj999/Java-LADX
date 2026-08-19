package linksawakening.world;

import linksawakening.rom.RomBank;

import java.util.Arrays;

/**
 * The indoor branch of bank-$19's {@code WarpEntityHandler} (entity $61).
 * The handler is deliberately independent of a particular dungeon: its room
 * pair and destination room are read from the ROM's {@code DungeonWarps}
 * table, exactly as {@code WarpState3Handler} does.
 */
final class WarpMotion {
    static final int ENTITY_TYPE = 0x61;
    static final int STATE_IDLE = 0;
    static final int STATE_APPROACH = 1;
    static final int STATE_ARMED = 2;
    static final int STATE_TRANSITION = 3;
    static final int DUNGEON_WARPS_BANK = 0x19;
    static final int DUNGEON_WARPS_ADDRESS = 0x4201;
    static final int DUNGEON_WARP_PAIR_COUNT = 8;

    private final byte[] romData;

    WarpMotion(byte[] romData) {
        if (romData == null) {
            throw new IllegalArgumentException("ROM data cannot be null");
        }
        int tableEnd = RomBank.romOffset(DUNGEON_WARPS_BANK, DUNGEON_WARPS_ADDRESS)
            + DUNGEON_WARP_PAIR_COUNT * 2;
        if (tableEnd > romData.length) {
            throw new IllegalArgumentException("ROM is missing DungeonWarps table");
        }
        this.romData = Arrays.copyOf(romData, romData.length);
    }

    Update advance(RoomEntity entity, int state, int countdown, int mapId, int roomId,
                   boolean minibossReady, int linkX, int linkY, int linkZ) {
        if (entity == null || entity.type() != ENTITY_TYPE) {
            throw new IllegalArgumentException("WarpMotion requires entity-$61");
        }
        if (state < STATE_IDLE || state > STATE_TRANSITION
            || (countdown & ~0xFF) != 0 || (mapId & ~0xFF) != 0
            || (roomId & ~0xFF) != 0 || (linkX & ~0xFF) != 0
            || (linkY & ~0xFF) != 0 || (linkZ & ~0xFF) != 0) {
            throw new IllegalArgumentException("Invalid WarpMotion byte state");
        }
        if (!minibossReady) {
            return idle(entity, state, countdown);
        }
        return switch (state) {
            case STATE_IDLE -> new Update(entity, STATE_APPROACH, 0, false,
                0x1B, -1, 0, false, null);
            case STATE_APPROACH -> withinUnsignedWindow(linkX, entity.x(), 0x04, 0x08)
                    && withinUnsignedWindow(linkY, entity.y(), 0x04, 0x08)
                ? idle(entity, STATE_APPROACH, 0)
                : new Update(entity, STATE_ARMED, 0, false, -1, -1, 0, false, null);
            case STATE_ARMED -> {
                if (linkZ != 0 || !withinUnsignedWindow(linkX, entity.x(), 0x03, 0x06)
                    || !withinUnsignedWindow(linkY, entity.y(), 0x03, 0x06)) {
                    yield idle(entity, STATE_ARMED, 0);
                }
                yield new Update(entity, STATE_TRANSITION, 0x50, true,
                    -1, 0x1C, 0x20, false, null);
            }
            case STATE_TRANSITION -> advanceTransition(entity, countdown, mapId, roomId);
            default -> throw new AssertionError(state);
        };
    }

    private Update advanceTransition(RoomEntity entity, int countdown,
                                     int mapId, int roomId) {
        if (countdown > 1) {
            return new Update(entity, STATE_TRANSITION, countdown - 1, true,
                -1, -1, 0, false, null);
        }
        Destination destination = destinationFor(mapId, roomId);
        return new Update(entity, STATE_TRANSITION, 0, true, -1, -1, 0,
            true, destination);
    }

    private Destination destinationFor(int mapId, int roomId) {
        if (mapId >= DUNGEON_WARP_PAIR_COUNT) {
            return null;
        }
        int table = RomBank.romOffset(DUNGEON_WARPS_BANK, DUNGEON_WARPS_ADDRESS);
        int first = Byte.toUnsignedInt(romData[table + mapId * 2]);
        int second = Byte.toUnsignedInt(romData[table + mapId * 2 + 1]);
        int destinationRoom = roomId == first ? second : first;
        int destinationY = destinationRoom == 0x64 ? 0x28 : 0x48;
        return new Destination(mapId, destinationRoom, 0x50, destinationY);
    }

    private static Update idle(RoomEntity entity, int state, int countdown) {
        return new Update(entity, state, countdown, false, -1, -1, 0, false, null);
    }

    private static boolean withinUnsignedWindow(int value, int center, int bias, int size) {
        return ((value - center + bias) & 0xFF) < size;
    }

    record Destination(int mapId, int roomId, int x, int y) {
    }

    record Update(RoomEntity entity, int state, int countdown, boolean motionBlocked,
                 int musicTrack, int jingleId, int immunityCountdown,
                 boolean destinationReady, Destination destination) {
    }
}
