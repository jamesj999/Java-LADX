package linksawakening.ui;

import linksawakening.gpu.GPU;
import linksawakening.render.HardwareFrame;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomBank;
import linksawakening.rom.RomSymbols;
import linksawakening.state.Progression;

/** Quest rows shared by the instrument and dungeon inventory layouts. */
final class InventoryQuestItems {
    private static final int LEFT = 10;
    private static final int TOP = 3;
    private static final int WIDTH = 10;
    private static final int HEIGHT = 4;
    private final int[] tiles = new int[WIDTH * HEIGHT];
    private final int[] attributes = new int[tiles.length];
    private final int[] template = new int[0x33];
    private final int[] commands = new int[template.length];
    private final int[] itemOffsets = new int[9];
    private final int[] items = new int[itemOffsets.length];
    private final int[] baseAttributes;
    private final int[] dungeonAttributes;
    private final int[][] palettes = new int[8][];
    private final int[] tradePalette = new int[4];
    private final TilemapRenderer.Region region = new TilemapRenderer.Region(0, 0, 0, 0);
    private final GPU graphics = new GPU();
    private final Rom rom;
    private boolean overridesTradeColors;
    private int tradeAttribute;
    private int loadedTrade = -1;
    private boolean loadedSlimeKey;
    private boolean loadedDungeon;

    InventoryQuestItems(Rom rom) {
        this.rom = rom;
        for (int i = 0; i < template.length; i++) template[i] = read(0x5994 + i);
        for (int i = 0; i < itemOffsets.length; i++) itemOffsets[i] = read(0x59C7 + i);
        baseAttributes = readBytes(0x596A, 0x1F);
        dungeonAttributes = readBytes(0x5989, 0x0B);
    }

    void prepare(int[] baseTiles, int[] baseAttrs, int[][] basePalettes, Progression progress, byte[] dungeonItems) {
        int leaves = progress.goldenLeavesCount();
        int trade = progress.tradeSequenceItem();
        if (trade > 14) trade = 0;
        boolean dungeon = dungeonItems != null;
        boolean slimeKey = !dungeon && leaves >= 6 && leaves != 0xFF;
        if (trade != loadedTrade || slimeKey != loadedSlimeKey || dungeon != loadedDungeon) {
            graphics.loadBaseTiles(rom);
            if (trade >= 2) graphics.updateTradingItemTiles(rom, trade);
            if (dungeon) {
                RomAddress source = RomSymbols.InventoryIndoorItemsTiles.withGbcBank();
                for (int i = 0; i < 0x100; i++) {
                    graphics.writeVRAM(0xC00 + i, (byte) rom.u8(RomAddress.fromOffset(source.offset() + i)));
                }
            } else if (slimeKey) {
                graphics.replaceSlimeKeyTilesByGoldenLeaf(rom);
            }
            if (trade != loadedTrade) loadTradePalette(trade);
            loadedTrade = trade;
            loadedSlimeKey = slimeKey;
            loadedDungeon = dungeon;
        }

        items[0] = progress.hasFlippers() ? 1 : 0;
        items[1] = progress.medicineCount();
        items[2] = trade;
        items[3] = progress.seashells();
        items[4] = progress.tailKeyCount();
        items[5] = progress.anglerKeyCount();
        items[6] = progress.faceKeyCount();
        items[7] = progress.birdKeyCount();
        items[8] = leaves;
        if (dungeon) {
            for (int i = 0; i < 5; i++) items[4 + i] = Byte.toUnsignedInt(dungeonItems[i]);
        }

        for (int row = 0; row < HEIGHT; row++) {
            int start = (TOP + row) * InventoryTilemapLoader.WIDTH + LEFT;
            System.arraycopy(baseTiles, start, tiles, row * WIDTH, WIDTH);
            System.arraycopy(baseAttrs, start, attributes, row * WIDTH, WIDTH);
        }
        System.arraycopy(template, 0, commands, 0, template.length);
        for (int i = 0; i < items.length; i++) {
            if (items[i] == 0 || items[i] == 0xFF) {
                int offset = itemOffsets[i];
                commands[offset] = commands[offset + 1] = 0x7F;
                if (i == 2) commands[offset + 7] = commands[offset + 8] = 0x7F;
            }
        }
        if (items[3] != 0) {
            commands[0x2C] = 0xB0 + items[3] / 10;
            commands[0x2D] = 0xB0 + items[3] % 10;
        }
        int count = dungeon ? items[8] : leaves < 6 ? leaves : 0;
        if (count > 0 && count <= 9) commands[0x31] = 0xB0 + count;
        applyCommands(commands, tiles);
        applyCommands(baseAttributes, attributes);
        if (dungeon) {
            applyCommands(dungeonAttributes, attributes);
            if (count > 0 && count <= 9) attributes[2 * WIDTH + 8] = attributes[3 * WIDTH + 8] = 1;
        }

        System.arraycopy(basePalettes, 0, palettes, 0, palettes.length);
        if (overridesTradeColors) {
            tradePalette[0] = palettes[5][0];
            tradePalette[3] = palettes[5][3];
            palettes[5] = tradePalette;
        }
        for (int row = 0; row < 2; row++) {
            attributes[row * WIDTH + 4] = tradeAttribute;
            attributes[row * WIDTH + 5] = tradeAttribute;
        }
    }

    private void loadTradePalette(int trade) {
        tradeAttribute = read(0x5AEE + trade);
        int colorIndex = read(0x5E0B + trade);
        overridesTradeColors = colorIndex != 0;
        if (overridesTradeColors) {
            int pointer = 0x5DFB + colorIndex * 2;
            int source = read(pointer) | read(pointer + 1) << 8;
            for (int i = 1; i <= 2; i++, source += 2) {
                tradePalette[i] = RomBank.decodeRgb555(read(source) | read(source + 1) << 8);
            }
        }
    }

    void copyRowsInto(int[] map, int[] attrs) {
        for (int row = 0; row < HEIGHT; row++) {
            int start = (TOP + row) * InventoryTilemapLoader.WIDTH + LEFT;
            System.arraycopy(tiles, row * WIDTH, map, start, WIDTH);
            System.arraycopy(attributes, row * WIDTH, attrs, start, WIDTH);
        }
    }

    private int read(int address) {
        return rom.u8(new RomAddress(0x20, address));
    }

    private int[] readBytes(int address, int length) {
        int[] result = new int[length];
        for (int i = 0; i < length; i++) result[i] = read(address + i);
        return result;
    }

    private static void applyCommands(int[] data, int[] target) {
        for (int pos = 0; pos < data.length && data[pos] != 0; ) {
            int address = (data[pos++] << 8 | data[pos++]) - 0x9C00;
            int control = data[pos++];
            int step = (control & 0x80) != 0 ? WIDTH : 1;
            int count = (control & 0x3F) + 1;
            boolean repeat = (control & 0x40) != 0;
            int cell = (address / 32 - TOP) * WIDTH + address % 32 - LEFT;
            for (int i = 0; i < count; i++, cell += step) {
                target[cell] = data[pos + (repeat ? 0 : i)];
            }
            pos += repeat ? 1 : count;
        }
    }

    void render(byte[] buffer, int windowY) {
        TilemapRenderer.render(
                buffer, graphics, tiles, attributes, palettes, region.set(WIDTH, HEIGHT, LEFT * 8, windowY + TOP * 8));
    }

    void submit(HardwareFrame frame, int windowY) {
        TilemapRenderer.submit(
                frame, graphics, tiles, attributes, palettes, region.set(WIDTH, HEIGHT, LEFT * 8, windowY + TOP * 8));
    }
}
