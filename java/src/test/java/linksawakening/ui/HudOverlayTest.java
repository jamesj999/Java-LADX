package linksawakening.ui;

import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class HudOverlayTest {

    @Test
    void equippedIconsUseTheSameColourPalettesAsMatchingInventoryIcons() throws Exception {
        byte[] rom = loadRom();
        GPU gpu = new GPU();
        gpu.loadBaseTiles(rom);

        PlayerState player = new PlayerState();
        player.setItemB(PlayerState.INVENTORY_BOW);
        player.setItemA(PlayerState.INVENTORY_HOOKSHOT);
        player.setSubscreenItems(new int[] {
            PlayerState.INVENTORY_BOW,
            PlayerState.INVENTORY_HOOKSHOT,
            PlayerState.INVENTORY_EMPTY,
            PlayerState.INVENTORY_EMPTY,
            PlayerState.INVENTORY_EMPTY,
            PlayerState.INVENTORY_EMPTY,
            PlayerState.INVENTORY_EMPTY,
            PlayerState.INVENTORY_EMPTY,
            PlayerState.INVENTORY_EMPTY,
            PlayerState.INVENTORY_EMPTY
        });

        InventoryMenu menu = new InventoryMenu(
            InventoryTilemapLoader.loadFromRom(rom), player);
        menu.requestToggle();
        for (int frame = 0; frame < 16; frame++) {
            menu.tick();
        }

        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        menu.render(buffer, gpu);

        assertRegionEquals(buffer, 8, 0, 8, 24, "B slot");
        assertRegionEquals(buffer, 48, 0, 40, 24, "A slot");
    }

    private static void assertRegionEquals(byte[] buffer, int expectedX, int expectedY,
                                            int actualX, int actualY, String label) {
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 8; x++) {
                int expectedOffset = pixelOffset(expectedX + x, expectedY + y);
                int actualOffset = pixelOffset(actualX + x, actualY + y);
                for (int channel = 0; channel < 4; channel++) {
                    assertEquals(Byte.toUnsignedInt(buffer[expectedOffset + channel]),
                        Byte.toUnsignedInt(buffer[actualOffset + channel]),
                        label + " mismatch at (" + x + "," + y + ") channel " + channel);
                }
            }
        }
    }

    private static int pixelOffset(int x, int y) {
        return (y * Framebuffer.WIDTH + x) * 4;
    }

    private static byte[] loadRom() throws Exception {
        try (InputStream stream = HudOverlayTest.class.getClassLoader()
                .getResourceAsStream("rom/azle.gbc")) {
            return stream.readAllBytes();
        }
    }
}
