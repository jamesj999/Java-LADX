package linksawakening.render;

import linksawakening.dialog.DialogController;
import linksawakening.gpu.GPU;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.scene.BackgroundScene;
import linksawakening.scene.BackgroundSceneLoader;
import linksawakening.scene.BackgroundSceneSpec;

/** Select map from bank $01, including the visited-room overlay and location inspector. */
public final class WorldMapLayer implements HardwareRenderLayer {
    private final Rom rom;
    private final GPU tiles = new GPU();
    private final BackgroundScene scene;
    private final DialogRenderLayer dialog = new DialogRenderLayer(null);
    private byte[] statuses;
    private int playerRoom;
    private int cursor;
    private int frame;
    private int icon;
    private int iconRoom;
    private int iconIn;
    private int iconOut;

    public WorldMapLayer(Rom rom) {
        this.rom = rom;
        scene = new BackgroundSceneLoader(rom).load(new BackgroundSceneSpec(0x08, 0x62E9, 0x24, 0x66E1, 0x21, 0x786E));
        tiles.loadWorldMapTiles(rom);
    }

    public void open(int roomId, byte[] overworldStatuses) {
        statuses = overworldStatuses.clone();
        playerRoom = cursor = iconRoom = roomId;
        icon = iconFor(cursor);
        frame = iconIn = iconOut = 0;
        var tileTable = new RomAddress(0x20, 0x568B);
        var attrTable = new RomAddress(0x20, 0x578B);
        for (int room = 0; room < 256; room++) {
            int cell = 34 + (room >> 4) * 32 + (room & 15);
            boolean visited = (statuses[room] & 0x80) != 0;
            scene.tilemap()[cell] = visited ? rom.u8(tileTable.plus(room)) : 0x2C;
            scene.attrmap()[cell] = visited ? rom.u8(attrTable.plus(room)) : 1;
        }
    }

    public void tick() {
        frame++;
        if (iconIn > 0) iconIn--;
        if (iconOut > 0) iconOut--;
    }

    public boolean moveCursor(int dx, int dy) {
        if (!isCursorMovementReady()) return false;
        int next = (((cursor >> 4) + dy) & 15) * 16 + ((cursor + dx) & 15);
        if (statuses[next] == 0 || next == cursor) return false;
        cursor = next;
        int nextIcon = iconFor(cursor);
        if (nextIcon != 0) {
            if (icon == 0) iconIn = 16;
            iconRoom = cursor;
        } else if (icon != 0) {
            iconOut = 16;
        }
        icon = nextIcon;
        return true;
    }

    public int cursorRoom() {
        return cursor;
    }

    public boolean isCursorMovementReady() {
        return iconIn == 0 && iconOut == 0;
    }

    private int specialLocation(int room) {
        int value = mapByte(0x5959 + room);
        return value < 16 && (statuses[room] & 0x20) == 0 ? 0 : value;
    }

    private int iconFor(int room) {
        int special = specialLocation(room);
        return special == 0 ? 0 : ((special >> 4) & 7) + 1;
    }

    public int locationDialogIndex() {
        int special = specialLocation(cursor);
        if (special != 0) return mapByte(0x5909 + special);
        if (cursor == 0x24 || cursor == 0x34) return 0x76;
        return mapByte(0x58C9 + ((cursor >> 2) & 0x38) + ((cursor >> 1) & 7));
    }

    public int locationDialogTable() {
        return locationDialogIndex() == 0xA7 || cursor == 0x37 ? 1 : 0;
    }

    public void configureDialog(DialogController controller) {
        dialog.configure(controller, scene.palettes()[1], scene.objectPalettes(), frame);
    }

    @Override
    public void submit(HardwareFrame hardware, GPU ignored) {
        HardwareBackgroundRenderer.renderBackground(
                hardware, tiles, scene.tilemap(), scene.attrmap(), scene.palettes(), 32, 32, 20, 18, 0, 0);
        var context = RenderContext.indexed(tiles, hardware);
        int x = (cursor & 15) * 8 + 12;
        int y = (cursor >> 4) * 8 + 4;
        sprite(context, (playerRoom & 15) * 8 + 16, (playerRoom >> 4) * 8 + 8, 0x3E, 0);
        if ((frame & 16) == 0) {
            sprite(context, x - 10, y + 4, 0xF6, 0);
            sprite(context, x + 19, y + 4, 0xF6, 0x20);
            sprite(context, x + 4, y - 10, 0xF8, 0);
            sprite(context, x + 4, y + 11, 0xF8, 0x40);
        }
        sprite(context, x, y, 0xF0, 0);
        sprite(context, x + 8, y, 0xF0, 0x20);
        if (icon != 0 || iconOut > 0) renderIcon(context);
        dialog.submit(hardware, tiles);
    }

    private int mapByte(int address) {
        return rom.u8(new RomAddress(1, address));
    }

    private void renderIcon(RenderContext context) {
        int quadrant = (iconRoom >= 0x70 ? 2 : 0) + ((iconRoom & 15) < 8 ? 1 : 0);
        int x = mapByte(0x5C41 + quadrant) - 8;
        int y = mapByte(0x5C45 + quadrant) - 16;
        int phase = iconOut > 0 ? (16 - iconOut) / 4 : Math.max(0, iconIn - 1) / 4;
        int rect = 0x5BAD + phase * 32;
        for (int i = 7; i >= 0; i--) {
            int at = rect + i * 4;
            sprite(context, x + (byte) mapByte(at + 1), y + (byte) mapByte(at), mapByte(at + 2), mapByte(at + 3));
        }
        if (phase == 0 && icon != 0) {
            int at = 0x5C2D + (icon - 1) * 4;
            sprite(context, x, y, mapByte(at), mapByte(at + 1));
            sprite(context, x + 8, y, mapByte(at + 2), mapByte(at + 3));
        }
    }

    private void sprite(RenderContext context, int x, int y, int tile, int attr) {
        OamDrawing.draw8x16(
                context, tiles, tile, x, y, (attr & 32) != 0, (attr & 64) != 0, scene.objectPalettes()[attr & 7]);
    }
}
