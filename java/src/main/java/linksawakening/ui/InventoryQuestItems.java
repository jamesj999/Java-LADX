package linksawakening.ui;

import linksawakening.gpu.GPU;
import linksawakening.rom.RomUtil;
import linksawakening.state.PlayerState;

import java.util.Arrays;

/** The quest-item rows prepared by InventoryLoad1Handler and InventoryLoad2Handler. */
final class InventoryQuestItems {
    private static final int LEFT = 10;
    private static final int TOP = 3;
    private static final int WIDTH = 10;
    private final int[] tiles = new int[WIDTH * 4];
    private final int[] attributes = new int[tiles.length];
    private final int[][] palettes;
    private final GPU graphics = new GPU();

    InventoryQuestItems(byte[] rom, PlayerState player, InventoryTilemapLoader loader, byte[] dungeonItems) {
        int leaves = player.goldenLeavesCount();
        int trade = player.tradeSequenceItem();
        graphics.loadInventoryQuestTiles(rom, trade, leaves, dungeonItems != null);
        int[] base = loader.copyBaseTilemap();
        for (int row = 0; row < 4; row++) {
            int start = (TOP + row) * InventoryTilemapLoader.WIDTH + LEFT;
            System.arraycopy(base, start, tiles, row * WIDTH, WIDTH);
            System.arraycopy(loader.attrmap(), start, attributes, row * WIDTH, WIDTH);
        }
        int source = RomUtil.romOffset(0x20, 0x5994);
        byte[] commands = Arrays.copyOfRange(rom, source, source + 0x33);
        int[] items = {player.hasFlippers() ? 1 : 0, player.hasMedicine() ? 1 : 0, trade, player.seashells(),
            player.tailKeyCount(), player.anglerKeyCount(), player.faceKeyCount(), player.birdKeyCount(), leaves};
        if (dungeonItems != null) {
            for (int i = 0; i < dungeonItems.length; i++) items[4 + i] = dungeonItems[i] & 255;
        }
        int offsets = RomUtil.romOffset(0x20, 0x59C7);
        for (int i = 0; i < items.length; i++) {
            if (items[i] == 0 || items[i] == 0xFF) {
                int offset = rom[offsets + i] & 255;
                commands[offset] = commands[offset + 1] = 0x7F;
                if (i == 2) commands[offset + 7] = commands[offset + 8] = 0x7F;
            }
        }
        if (player.seashells() != 0) {
            commands[0x2C] = (byte) (0xB0 + player.seashells() / 10);
            commands[0x2D] = (byte) (0xB0 + player.seashells() % 10);
        }
        int count = dungeonItems == null ? (leaves < 6 ? leaves : 0) : items[8];
        if (count != 0) commands[0x31] = (byte) (0xB0 + count);
        applyCommands(commands, 0, tiles);
        applyCommands(rom, RomUtil.romOffset(0x20, 0x596A), attributes);
        if (dungeonItems != null) {
            applyCommands(rom, RomUtil.romOffset(0x20, 0x5989), attributes);
            if (count != 0) attributes[2 * WIDTH + 8] = attributes[3 * WIDTH + 8] = 1;
        }
        palettes = loader.palettes().clone();
        int colorIndex = rom[RomUtil.romOffset(0x20, 0x5E0B) + trade] & 255;
        if (trade <= 14 && colorIndex != 0) {
            int pointer = RomUtil.romOffset(0x20, 0x5DFB + colorIndex * 2);
            int sourceColors = RomUtil.romOffset(0x20, (rom[pointer] & 255) | (rom[pointer + 1] & 255) << 8);
            palettes[5] = palettes[5].clone();
            for (int i = 1; i <= 2; i++, sourceColors += 2) {
                palettes[5][i] = RomUtil.decodeRgb555((rom[sourceColors] & 255) | (rom[sourceColors + 1] & 255) << 8);
            }
        }
        int palette = rom[RomUtil.romOffset(0x20, 0x5AEE) + trade] & 7;
        for (int row = 0; row < 2; row++) {
            attributes[row * WIDTH + 4] = palette;
            attributes[row * WIDTH + 5] = palette;
        }
    }

    private static void applyCommands(byte[] data, int position, int[] target) {
        while (data[position] != 0) {
            int address = (data[position++] & 255) * 256 + (data[position++] & 255) - 0x9C00;
            int control = data[position++] & 255;
            int stride = (control & 0x80) != 0 ? WIDTH : 1;
            int cell = (address / 32 - TOP) * WIDTH + address % 32 - LEFT;
            for (int i = 0; i <= (control & 63); i++, cell += stride) {
                target[cell] = data[position++] & 255;
            }
        }
    }

    void render(byte[] buffer, int windowY) {
        TilemapRenderer.render(buffer, graphics, tiles, attributes, palettes,
            WIDTH, 4, LEFT * 8, windowY + TOP * 8);
    }
}
