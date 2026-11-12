package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

public interface RoomDungeon {
    /** Same-thread read-only inventory data, valid until the next tick or world mutation. */
    interface View {
        int progressFlagCount();

        int progressFlag(int index);

        int currentItemFlagCount();

        int currentItemFlag(int index);

        int minimapSize();

        int minimapStatus(int index);

        /** Copies into caller-retained buffers without allocating; the caller owns those copies. */
        default void copyTo(byte[] progress, byte[] items, byte[] minimap) {
            if (progress.length != progressFlagCount()
                    || items.length != currentItemFlagCount()
                    || minimap.length != minimapSize()) throw new IllegalArgumentException("Dungeon buffer sizes");
            for (int i = 0; i < progress.length; i++) progress[i] = (byte) progressFlag(i);
            for (int i = 0; i < items.length; i++) items[i] = (byte) currentItemFlag(i);
            for (int i = 0; i < minimap.length; i++) minimap[i] = (byte) minimapStatus(i);
        }
    }

    /** Cached view; snapshot methods below still return owned copies. */
    View view();

    void setDungeonItemForDebug(int mapId, int index, int value);

    byte[] dungeonMinimapRoomStatusSnapshot();

    byte[] overworldRoomStatusSnapshot();

    byte[] indoorARoomStatusSnapshot();

    byte[] indoorBRoomStatusSnapshot();

    byte[] colorDungeonRoomStatusSnapshot();

    byte[] dungeonItemFlagsSnapshot();

    byte[] colorDungeonItemFlagsSnapshot();

    byte[] currentDungeonItemFlagsSnapshot();

    void restoreDungeonItemFlags(byte[] dungeonFlags, byte[] colorFlags);

    byte[] dungeonProgressFlagsSnapshot();

    void restoreDungeonProgressFlags(byte[] flags);
}
