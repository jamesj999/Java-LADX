package linksawakening.world;

public final class RoomConstants {
    public static final int OBJECTS_PER_ROW = 10;
    public static final int OBJECTS_PER_COLUMN = 8;
    public static final int ROOM_TILE_WIDTH = 20;
    public static final int ROOM_TILE_HEIGHT = 16;
    public static final int ROOM_PIXEL_WIDTH = ROOM_TILE_WIDTH * 8;
    public static final int ROOM_PIXEL_HEIGHT = ROOM_TILE_HEIGHT * 8;
    public static final int ROOM_OBJECTS_AREA_SIZE = 0x100;
    public static final int ROOM_OBJECTS_BASE = 0x11;
    public static final int ROOM_OBJECT_ROW_STRIDE = 0x10;
    public static final int ACTIVE_ROOM_OBJECT_BYTES = 0x80;
    public static final int ROOM_END = 0xFE;

    private RoomConstants() {
    }
}
