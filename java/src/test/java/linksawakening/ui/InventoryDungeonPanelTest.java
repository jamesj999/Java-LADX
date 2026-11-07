package linksawakening.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@linksawakening.testing.RequiresRom
final class InventoryDungeonPanelTest {
    private byte[] rom() throws Exception {
        try (var stream = linksawakening.testing.TestRom.open()) { return stream.readAllBytes(); }
    }

    @Test
    void tailCavePanelMatchesTheRealEmulatorsTilemapAndAttributes() throws Exception {
        byte[] rom = rom();
        var loader = InventoryTilemapLoader.loadFromRom(rom);
        int[] map = loader.copyBaseTilemap(), attrs = loader.copyAttrmap();
        var lines = Files.readAllLines(Path.of("src/test/resources/opening/tail-cave-inventory/panel.tsv"))
            .stream().filter(line -> !line.startsWith("#")).toList();
        String[] values = lines.getFirst().split(" ");
        byte[] status = new byte[64];
        for (int i = 0; i < 64; i++) status[i] = (byte) Integer.parseInt(values[i]);
        new InventoryDungeonPanel(rom).apply(map, attrs, 0, false,
            new byte[]{1, 1, 0, 0, 2}, status);
        for (String line : lines.subList(1, lines.size())) {
            String[] cell = line.split(" ");
            int x = Integer.parseInt(cell[0]), y = Integer.parseInt(cell[1]);
            assertEquals(Integer.parseInt(cell[2]), map[y * 20 + x], "tile " + x + "," + y);
            assertEquals(Integer.parseInt(cell[3]), attrs[y * 20 + x], "attribute " + x + "," + y);
        }
    }

    @Test
    void mapCompassAndRewardStatusControlTheRoomSymbols() throws Exception {
        var panel = new InventoryDungeonPanel(rom());
        assertEquals(0x7D, panel.minimapTile(0xEF, 0x8F, false, false));
        assertEquals(0xED, panel.minimapTile(0xED, 0x80, false, true));
        assertEquals(0x7D, panel.minimapTile(0xED, 0x90, false, true));
        assertEquals(0xEF, panel.minimapTile(0xED, 0, true, false));
        assertEquals(0xD1, panel.minimapTile(0xEF, 0x8F, true, false));
        assertEquals(0xEE, panel.minimapTile(0xEE, 0x80, true, true));
        assertEquals(0xD0, panel.minimapTile(0xEE, 0xA0, true, true));
        assertEquals(0x7D, panel.minimapTile(0x7D, 0xFF, true, true));
    }
}
