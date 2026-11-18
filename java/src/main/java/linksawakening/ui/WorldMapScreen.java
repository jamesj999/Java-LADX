package linksawakening.ui;

import linksawakening.dialog.DialogController;
import linksawakening.dialog.DialogRenderer;
import linksawakening.gpu.GPU;
import linksawakening.render.IndexedRenderer;
import linksawakening.rom.RomBank;
import linksawakening.scene.BackgroundScene;
import linksawakening.scene.BackgroundSceneLoader;
import linksawakening.scene.BackgroundSceneSpec;

/** Select map from bank $01, including the visited-room overlay and location inspector. */
public final class WorldMapScreen {
    private final byte[] rom;
    private final GPU tiles = new GPU();
    private final BackgroundScene scene;
    private byte[] statuses;
    private int playerRoom;
    private int cursor;
    private int frame;
    private int icon;
    private int iconRoom;
    private int iconIn;
    private int iconOut;
    public WorldMapScreen(byte[] rom) {
        this.rom = rom;
        scene = new BackgroundSceneLoader(rom).load(
            new BackgroundSceneSpec(8, 0x62E9, 0x24, 0x66E1, 0x21, 0x786E));
        tiles.loadWorldMapTiles(rom);
    }

    public void open(int roomId, byte[] overworldStatuses) {
        statuses = overworldStatuses.clone();
        playerRoom = cursor = iconRoom = roomId;
        icon = iconFor(cursor);
        frame = iconIn = iconOut = 0;
        int tileOffset = RomBank.romOffset(0x20, 0x568B);
        int attrOffset = RomBank.romOffset(0x20, 0x578B);
        for (int room = 0; room < 256; room++) {
            int cell = 34 + (room >> 4) * 32 + (room & 15);
            boolean visited = (statuses[room] & 0x80) != 0;
            scene.tilemap()[cell] = visited ? rom[tileOffset + room] & 255 : 0x2C;
            scene.attrmap()[cell] = visited ? rom[attrOffset + room] & 255 : 1;
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
        int value = rom[0x5959 + room] & 255;
        return value < 16 && (statuses[room] & 0x20) == 0 ? 0 : value;
    }

    private int iconFor(int room) {
        int special = specialLocation(room);
        return special == 0 ? 0 : ((special >> 4) & 7) + 1;
    }

    public int locationDialogIndex() {
        int special = specialLocation(cursor);
        if (special != 0) return rom[0x5909 + special] & 255;
        if (cursor == 0x24 || cursor == 0x34) return 0x76;
        return rom[0x58C9 + ((cursor >> 2) & 0x38) + ((cursor >> 1) & 7)] & 255;
    }

    public int locationDialogTable() {
        return locationDialogIndex() == 0xA7 || cursor == 0x37 ? 1 : 0;
    }

    public void render(byte[] buffer, DialogController dialog, boolean showObjects) {
        TilemapRenderer.render(buffer, tiles, scene.tilemap(), scene.attrmap(), scene.palettes(),
            32, 18, 0, 0);
        if (!showObjects) return;
        int x = (cursor & 15) * 8 + 12;
        int y = (cursor >> 4) * 8 + 4;
        sprite(buffer, (playerRoom & 15) * 8 + 16, (playerRoom >> 4) * 8 + 8, 0x3E, 0);
        if ((frame & 16) == 0) {
            sprite(buffer, x - 10, y + 4, 0xF6, 0);
            sprite(buffer, x + 19, y + 4, 0xF6, 0x20);
            sprite(buffer, x + 4, y - 10, 0xF8, 0);
            sprite(buffer, x + 4, y + 11, 0xF8, 0x40);
        }
        sprite(buffer, x, y, 0xF0, 0);
        sprite(buffer, x + 8, y, 0xF0, 0x20);
        if (icon != 0 || iconOut > 0) renderIcon(buffer);
        if (dialog != null && dialog.isActive()) {
            DialogRenderer.render(buffer, dialog, scene.palettes()[1]);
            if (dialog.isWaitingForPageAdvance() && (frame & 16) == 0) {
                sprite(buffer, 143, dialog.boxY() + 31, 0xA2, 0x42);
            }
        }
    }

    private void renderIcon(byte[] buffer) {
        int quadrant = (iconRoom >= 0x70 ? 2 : 0) + ((iconRoom & 15) < 8 ? 1 : 0);
        int x = (rom[0x5C41 + quadrant] & 255) - 8;
        int y = (rom[0x5C45 + quadrant] & 255) - 16;
        int phase = iconOut > 0 ? (16 - iconOut) / 4 : Math.max(0, iconIn - 1) / 4;
        int rect = 0x5BAD + phase * 32;
        for (int i = 7; i >= 0; i--) {
            int at = rect + i * 4;
            sprite(buffer, x + rom[at + 1], y + rom[at], rom[at + 2] & 255, rom[at + 3] & 255);
        }
        if (phase == 0 && icon != 0) {
            int at = 0x5C2D + (icon - 1) * 4;
            sprite(buffer, x, y, rom[at] & 255, rom[at + 1] & 255);
            sprite(buffer, x + 8, y, rom[at + 2] & 255, rom[at + 3] & 255);
        }
    }

    private void sprite(byte[] buffer, int x, int y, int tile, int attr) {
        IndexedRenderer.drawSpriteTile8x16(buffer, tiles, tile, x, y, (attr & 32) != 0,
            (attr & 64) != 0, scene.objectPalettes()[attr & 7]);
    }
}
