package linksawakening.ui;

import linksawakening.state.InventoryItem;
import linksawakening.state.PlayerState;

/** The two right-hand bottom-row cells written by bank20:$5B4F-$5BB8. */
final class InventoryItemNumbers {
    private InventoryItemNumbers() {}

    static long tilePair(int item, PlayerState player) {
        int level =
                switch (InventoryItem.fromRom(item)) {
                    case SWORD -> player.equipmentLevels().swordLevel();
                    case SHIELD -> player.equipmentLevels().shieldLevel();
                    case POWER_BRACELET -> player.equipmentLevels().powerBraceletLevel();
                    default -> -1;
                };
        if (level >= 0) return pair(0xBA, 0xB0 + level);
        int quantity =
                switch (InventoryItem.fromRom(item)) {
                    case BOMBS -> player.ammunition().bombCount();
                    case BOW -> player.ammunition().arrowCount();
                    case MAGIC_POWDER -> player.ammunition().hasToadstool()
                            ? -1
                            : player.ammunition().magicPowderCount();
                    default -> -1;
                };
        if (quantity >= 0) return pair(0xB0 + quantity / 10, 0xB0 + quantity % 10);
        if (item == InventoryItem.OCARINA.romValue() && player.progression().ocarinaSongFlags() != 0) {
            return pair(0xB1 + player.progression().selectedSongIndex(), 0x7F);
        }
        return pair(0x7F, 0x7F);
    }

    private static long pair(int first, int second) {
        return ((long) first << 32) | (second & 0xFFFFFFFFL);
    }

    static int[] tiles(int item, PlayerState player) {
        long pair = tilePair(item, player);
        return new int[] {(int) (pair >> 32), (int) pair};
    }
}
