package linksawakening.ui;

import java.util.Arrays;
import linksawakening.gpu.GPU;
import linksawakening.render.IndexedRenderer;
import linksawakening.rom.Rom;
import linksawakening.rom.RomBank;

/** ROM CreateMinimapTilemap, LoadMinimap and the dungeon inventory draw commands. */
final class InventoryDungeonPanel {
    private final int[] rowTiles = new int[11];
    private final int[] rowAttributes = new int[11];

    private final TilemapRenderer.Region tileRegion = new TilemapRenderer.Region(0, 0, 0, 0);

    private final Rom romData;
    private final linksawakening.rom.Rom data;
    private final GPU tiles = new GPU();

    InventoryDungeonPanel(Rom romData) {
        this.romData = romData;
        this.data = romData;
        tiles.loadBaseTiles(romData);
        int items = linksawakening.rom.RomSymbols.InventoryIndoorItemsTiles.withGbcBank()
                .offset();
        for (int i = 0; i < 0x300; i++)
            tiles.writeVRAM(0xC00 + i, (byte) data.u8(linksawakening.rom.RomAddress.fromOffset(items + i)));
        // LoadDungeonMinimapTiles: adjusted bank $32, $7E00 -> VRAM $8D00.
        int source =
                linksawakening.rom.RomSymbols.DungeonMinimapTiles.withGbcBank().offset();
        for (int i = 0; i < 0x200; i++)
            tiles.writeVRAM(0xD00 + i, (byte) data.u8(linksawakening.rom.RomAddress.fromOffset(source + i)));
    }

    private int read(int bank, int address) {
        return data.u8(linksawakening.rom.RomAddress.fromOffset(RomBank.romOffset(bank, address)));
    }

    int minimapTile(int base, int status, boolean map, boolean compass) {
        if (base == 0x7D) return base;
        if ((base == 0xED || base == 0xEE) && !compass) base = 0xEF;
        if (!map && !((base == 0xED || base == 0xEE) && compass)) return 0x7D;
        if ((status & 0x80) == 0) return base;
        if (base == 0xED && (status & 0x10) == 0 || base == 0xEE && (status & 0x20) == 0) return base;
        return map
                ? 0xD0
                        + read(
                                linksawakening.rom.RomSymbols.Data_002_66F9.bank(),
                                linksawakening.rom.RomSymbols.Data_002_66F9.address() + (status & 15))
                : 0x7D;
    }

    void apply(int[] tilemap, int[] attrs, int mapId, boolean collapsed, byte[] items, byte[] roomStatusByPosition) {
        int layout = read(
                linksawakening.rom.RomSymbols.MinimapLayoutTable.bank(),
                linksawakening.rom.RomSymbols.MinimapLayoutTable.address() + (mapId == 0xFF ? 15 : mapId));
        for (int y = 7; y < 17; y++) Arrays.fill(attrs, y * 20 + 9, y * 20 + 20, 1);
        // CreateMinimapTilemap: erase the instrument panel, then draw its ROM frame.
        commands(tilemap, bytes(1, 0x54E8, 0x29));
        int index = ((layout >>> 4) & 3) * 4;
        while (read(1, 0x53D8 + index) != 0xFF) {
            command(
                    tilemap,
                    read(1, 0x53D8 + index) * 256 + read(1, 0x53E8 + index),
                    read(1, 0x53F8 + index),
                    new int[] {read(1, 0x5408 + index)});
            index++;
        }
        index = ((layout >>> 4) & 3) * 8;
        if ((layout & 0x30) != 0) {
            if ((layout & 3) != 0) index += (layout & 0x30) == 0x30 ? (layout & 3) * 11 : 4;
            while (read(1, 0x5418 + index) != 0xFF) {
                command(tilemap, read(1, 0x5418 + index) * 256 + read(1, 0x545C + index), 0, new int[] {
                    read(1, 0x54A0 + index)
                });
                index++;
            }
        }
        int splitY = (layout & 0x20) != 0 ? 1 : 0;
        command(tilemap, read(1, 0x54E4 + splitY) * 256 + read(1, 0x54E6 + splitY), 1, new int[] {
            (mapId + 0xB1) & 255, 0x7F
        });
        int minimapAddress = mapId == 0xFF ? 0x66B9 : mapId == 6 && collapsed ? 0x6679 : 0x6479 + mapId * 64;
        for (int pos = 0; pos < 64; pos++) {
            int x = 11 + (pos & 7) + ((layout & 0x10) != 0 && (pos & 7) >= 4 ? 1 : 0);
            int y = 8 + (pos >>> 3) - (splitY != 0 && pos < 32 ? 1 : 0);
            int tile = minimapTile(
                    read(2, minimapAddress + pos),
                    Byte.toUnsignedInt(roomStatusByPosition[pos]),
                    items[0] != 0,
                    items[1] != 0);
            tilemap[y * 20 + x] = tile;
            attrs[y * 20 + x] = tile == 0xED ? 6 : 1;
        }
        int arrow = 0x6DCA + 2 * (mapId == 0xFF ? 15 : mapId);
        command(tilemap, read(1, arrow) | read(1, arrow + 1) << 8, 0, new int[] {0xA3});
        // InventoryLoad1Handler: dungeon map, compass, beak, Nightmare Key, small keys.
        int[] commands = bytes(0x20, 0x5994, 0x33);
        for (int i = 0; i < 9; i++) {
            int value = i >= 4 ? Byte.toUnsignedInt(items[i - 4]) : 0;
            if (value == 0 || value == 255) {
                int offset = read(0x20, 0x59C7 + i);
                commands[offset] = commands[offset + 1] = 0x7F;
                if (i == 2) commands[offset + 7] = commands[offset + 8] = 0x7F;
            }
        }
        if (items[4] != 0) commands[0x31] = 0xB0 + Byte.toUnsignedInt(items[4]);
        commands(tilemap, commands);
        commands(attrs, bytes(0x20, 0x596A, 0x1F));
        commands(attrs, bytes(0x20, 0x5989, 0x0B));
        if (items[4] != 0) attrs[5 * 20 + 18] = attrs[6 * 20 + 18] = 1;
    }

    void render(byte[] buffer, int[] map, int[] attrs, int[][] palettes, int offsetY, Presentation arguments) {
        int mapId = arguments.mapId();
        int position = arguments.position();
        int frame = arguments.frame();
        int[][] objectPalettes = arguments.objectPalettes();

        // Render only the right-hand panel using its own VRAM so gameplay tiles survive closing.
        for (int y = 3; y < 17; y++) {
            copyRow(map, y * 20 + 9, rowTiles);
            int[] row = rowTiles;
            copyRow(attrs, y * 20 + 9, rowAttributes);
            int[] attributes = rowAttributes;
            TilemapRenderer.render(
                    buffer, tiles, row, attributes, palettes, tileRegion.set(11, 1, 72, y * 8 + offsetY));
        }
        if (position < 0 || position >= 64) return;
        int layout = read(
                linksawakening.rom.RomSymbols.MinimapLayoutTable.bank(),
                linksawakening.rom.RomSymbols.MinimapLayoutTable.address() + (mapId == 0xFF ? 15 : mapId));
        int x = 88 + (position & 7) * 8 + ((layout & 0x10) != 0 && (position & 7) >= 4 ? 8 : 0);
        int y = 64 + (position & 0x38) - ((layout & 0x20) != 0 && position < 32 ? 8 : 0);
        IndexedRenderer.drawSpriteTile8x16(
                buffer, tiles, 0x3E, x, y + offsetY, false, false, objectPalettes[(frame & 8) == 0 ? 1 : 4]);
    }

    private int[] bytes(int bank, int address, int length) {
        int[] result = new int[length];
        for (int i = 0; i < length; i++) result[i] = read(bank, address + i);
        return result;
    }

    private void commands(int[] map, int[] stream) {
        for (int p = 0; p < stream.length && stream[p] != 0; ) {
            int address = stream[p++] * 256 + stream[p++];
            int length = stream[p++];
            int count = (length & 0x3F) + 1;
            int bytes = (length & 0x40) != 0 ? 1 : count;
            command(map, address, length, Arrays.copyOfRange(stream, p, p + bytes));
            p += bytes;
        }
    }

    private void command(int[] map, int address, int length, int[] values) {
        for (int i = 0; i <= (length & 0x3F); i++) {
            int at = address - 0x9C00 + i * ((length & 0x80) != 0 ? 32 : 1);
            int x = at & 31, y = at >>> 5;
            if (address >= 0x9C00 && x < 20 && y < 18) map[y * 20 + x] = values[(length & 0x40) != 0 ? 0 : i];
        }
    }

    void submit(
            linksawakening.render.HardwareFrame frameModel,
            int[] map,
            int[] attrs,
            int[][] palettes,
            int offsetY,
            Presentation arguments) {
        int mapId = arguments.mapId();
        int position = arguments.position();
        int frame = arguments.frame();
        int[][] objectPalettes = arguments.objectPalettes();

        // Render only the right-hand panel using its own VRAM so gameplay tiles survive closing.
        for (int y = 3; y < 17; y++) {
            copyRow(map, y * 20 + 9, rowTiles);
            int[] row = rowTiles;
            copyRow(attrs, y * 20 + 9, rowAttributes);
            int[] attributes = rowAttributes;
            TilemapRenderer.submit(
                    frameModel, tiles, row, attributes, palettes, tileRegion.set(11, 1, 72, y * 8 + offsetY));
        }
        if (position < 0 || position >= 64) return;
        int layout = read(
                linksawakening.rom.RomSymbols.MinimapLayoutTable.bank(),
                linksawakening.rom.RomSymbols.MinimapLayoutTable.address() + (mapId == 0xFF ? 15 : mapId));
        int x = 88 + (position & 7) * 8 + ((layout & 0x10) != 0 && (position & 7) >= 4 ? 8 : 0);
        int y = 64 + (position & 0x38) - ((layout & 0x20) != 0 && position < 32 ? 8 : 0);
        frameModel.sprite8x16(tiles, 0x3E, x, y + offsetY, false, false, objectPalettes[(frame & 8) == 0 ? 1 : 4]);
    }

    private static void copyRow(int[] source, int offset, int[] destination) {
        int count = Math.min(11, source.length - offset);
        System.arraycopy(source, offset, destination, 0, count);
        Arrays.fill(destination, count, 11, 0);
    }

    /** Arguments shared by render operations. */
    public static final class Presentation {
        private int mapId;
        private int position;
        private int frame;
        private int[][] objectPalettes;

        public Presentation(int mapId, int position, int frame, int[][] objectPalettes) {
            set(mapId, position, frame, objectPalettes);
        }

        Presentation set(int mapId, int position, int frame, int[][] objectPalettes) {
            this.mapId = mapId;
            this.position = position;
            this.frame = frame;
            this.objectPalettes = objectPalettes;
            return this;
        }

        public int mapId() {
            return mapId;
        }

        public int position() {
            return position;
        }

        public int frame() {
            return frame;
        }

        public int[][] objectPalettes() {
            return objectPalettes;
        }
    }
}
