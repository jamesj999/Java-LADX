package linksawakening.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

final class InventoryItemNumbersTest {
    @Test
    void mushroomHasNoQuantityAndWitchRewardShowsTwentyInBothInventoryLocations() {
        PlayerState player = new PlayerState();
        player.playerInitialization().initializeNewGame(60, 60, 40);
        player.playerRewards().applyToadstoolReward();
        player.inventory().setItemB(PlayerState.INVENTORY_MAGIC_POWDER);
        player.inventory().setSubscreenItems(new int[] {12, 0, 0, 0, 0, 0, 0, 0, 0, 0});
        assertDisplay(player, 0x7F, 0x7F, 2, 3);
        player.inventory().setSubscreenItems(new int[10]);
        player.playerRewards().beginWitchToadstoolExchange(0);
        player.playerRewards().applyWitchMagicPowderReward();
        player.inventory().setSubscreenItems(new int[] {12, 0, 0, 0, 0, 0, 0, 0, 0, 0});
        assertDisplay(player, 0xB2, 0xB0, 3, 3);
    }

    @Test
    void consumableCountsAndUpgradedLevelsAreSharedByHudAndSubscreen() {
        PlayerState player = new PlayerState();
        player.playerInitialization().initializeNewGame(60, 60, 40);
        player.ammunition().setBombCount(9);
        player.ammunition().setArrowCount(47);
        player.equipmentLevels().setSwordLevel(2);
        player.equipmentLevels().setShieldLevel(2);
        player.equipmentLevels().setPowerBraceletLevel(2);
        for (int[] item :
                new int[][] {{2, 0xB0, 0xB9}, {5, 0xB4, 0xB7}, {1, 0xBA, 0xB2}, {4, 0xBA, 0xB2}, {3, 0xBA, 0xB2}}) {
            player.inventory().setItemB(item[0]);
            player.inventory().setSubscreenItems(new int[] {item[0], 0, 0, 0, 0, 0, 0, 0, 0, 0});
            int[] palette = InventoryItemPalettes.forItem(item[0], player);
            assertDisplay(player, item[1], item[2], palette[0], palette[1]);
        }
    }

    private static void assertDisplay(PlayerState player, int tens, int ones, int top, int bottom) {
        int[] map = new int[20 * 18];
        int[] attrs = new int[map.length];
        HudOverlay.apply(map, attrs, player);
        InventorySubscreenOverlay.apply(map, attrs, player, 0, false);
        for (int row : new int[] {0, 3}) {
            assertEquals(tens, map[(row + 1) * 20 + 2]);
            assertEquals(ones, map[(row + 1) * 20 + 3]);
            assertEquals(top, attrs[row * 20 + 1]);
            assertEquals(bottom, attrs[(row + 1) * 20 + 1]);
        }
    }
}
