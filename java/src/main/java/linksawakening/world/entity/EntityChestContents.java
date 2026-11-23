package linksawakening.world.entity;

import java.util.Objects;
import linksawakening.world.room.ChestContentsTable;

/** Chest presentation inputs, with an explicit ROM-free implementation. */
sealed interface EntityChestContents {
    int NO_DIALOG = -1;

    int presentationSoundValue(int itemType);

    int dialogLowIdFor(int itemType, int shield, int sword, int bracelet, int map, int room);

    void requireSpawnTable();

    static EntityChestContents from(ChestContentsTable table) {
        return table == null ? Empty.INSTANCE : new RomContents(table);
    }

    /** Absent-table behavior: no music/dialog and the original explicit spawn precondition. */
    enum Empty implements EntityChestContents {
        INSTANCE;

        @Override
        public int presentationSoundValue(int itemType) {
            return 0;
        }

        @Override
        public int dialogLowIdFor(int itemType, int shield, int sword, int bracelet, int map, int room) {
            return NO_DIALOG;
        }

        @Override
        public void requireSpawnTable() {
            throw new IllegalStateException("ROM chest contents table is not configured");
        }
    }

    record RomContents(ChestContentsTable table) implements EntityChestContents {
        public RomContents {
            Objects.requireNonNull(table, "chest contents");
        }

        @Override
        public int presentationSoundValue(int itemType) {
            return table.presentationSoundValue(itemType);
        }

        @Override
        public int dialogLowIdFor(int itemType, int shield, int sword, int bracelet, int map, int room) {
            return table.dialogLowIdFor(itemType, shield, sword, bracelet, map, room);
        }

        @Override
        public void requireSpawnTable() {}
    }
}
