package linksawakening.ui;

import linksawakening.state.PlayerState;

/** The two right-hand bottom-row cells written by bank20:$5B4F-$5BB8. */
final class InventoryItemNumbers {
    private InventoryItemNumbers() {}

    static int[] tiles(int item, PlayerState player) {
        int level =
                switch (item) {
                    case PlayerState.INVENTORY_SWORD -> player.swordLevel();
                    case PlayerState.INVENTORY_SHIELD -> player.shieldLevel();
                    case PlayerState.INVENTORY_POWER_BRACELET -> player.powerBraceletLevel();
                    default -> -1;
                };
        if (level >= 0) return new int[] {0xBA, 0xB0 + level};
        int quantity =
                switch (item) {
                    case PlayerState.INVENTORY_BOMBS -> player.bombCount();
                    case PlayerState.INVENTORY_BOW -> player.arrowCount();
                    case PlayerState.INVENTORY_MAGIC_POWDER -> player.hasToadstool() ? -1 : player.magicPowderCount();
                    default -> -1;
                };
        if (quantity >= 0) return new int[] {0xB0 + quantity / 10, 0xB0 + quantity % 10};
        if (item == PlayerState.INVENTORY_OCARINA && player.ocarinaSongFlags() != 0) {
            return new int[] {0xB1 + player.selectedSongIndex(), 0x7F};
        }
        return new int[] {0x7F, 0x7F};
    }
}
