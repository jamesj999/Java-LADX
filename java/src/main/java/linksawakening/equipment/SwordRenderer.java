package linksawakening.equipment;

import linksawakening.gpu.Tile;
import linksawakening.rom.RomTables;
import linksawakening.rom.SwordTables;
import linksawakening.state.Direction;

/** Blade geometry and raster/OAM submission; attack timing remains in Sword. */
final class SwordRenderer {
    private static final int ATTR_FLIP_X = 0x20;
    private static final int ATTR_FLIP_Y = 0x40;
    private final SwordTables swordTables;
    private final SwordSpriteSheet spriteSheet;

    SwordRenderer(RomTables romTables, SwordSpriteSheet spriteSheet) {
        this.swordTables = romTables == null ? null : romTables.swordTables();
        this.spriteSheet = spriteSheet;
    }

    void render(
            byte[] displayBuffer,
            int linkPixelX,
            int linkPixelY,
            int effectiveDirection,
            int lookupState,
            int[] palette,
            int offsetX,
            int offsetY) {
        int romDir = Direction.fromJavaOrDown(effectiveDirection).romIndex();
        int swordDir = swordTables.swordDirectionFor(romDir, lookupState);
        int bladeX = linkPixelX + swordTables.swordBladeXOffset(romDir, lookupState) + offsetX;
        int bladeY = linkPixelY + swordTables.swordBladeYOffset(romDir, lookupState) + offsetY;

        // Two 8x16 stacked sprites side by side, matching func_020_4AB3.
        // Tile $FF for the left half means only the right half is drawn
        // (the vertical blade cases).
        int tile0 = swordTables.swordSpriteTile(swordDir, 0);
        int tile1 = swordTables.swordSpriteTile(swordDir, 1);
        int attr0 = swordTables.swordSpriteAttr(swordDir, 0);
        int attr1 = swordTables.swordSpriteAttr(swordDir, 1);
        if (tile0 != 0xFF) {
            draw8x16(displayBuffer, tile0, bladeX, bladeY, attr0, palette);
        }
        if (tile1 != 0xFF) {
            draw8x16(displayBuffer, tile1, bladeX + 8, bladeY, attr1, palette);
        }
    }

    void submitOam(
            linksawakening.render.OamSink sink,
            int linkPixelX,
            int linkPixelY,
            int effectiveDirection,
            int lookupState,
            int[] palette,
            int offsetX,
            int offsetY) {
        int romDir = Direction.fromJavaOrDown(effectiveDirection).romIndex();
        int swordDir = swordTables.swordDirectionFor(romDir, lookupState);
        int bladeX = linkPixelX + swordTables.swordBladeXOffset(romDir, lookupState) + offsetX;
        int bladeY = linkPixelY + swordTables.swordBladeYOffset(romDir, lookupState) + offsetY;

        // Two 8x16 stacked sprites side by side, matching func_020_4AB3.
        // Tile $FF for the left half means only the right half is drawn
        // (the vertical blade cases).
        int tile0 = swordTables.swordSpriteTile(swordDir, 0);
        int tile1 = swordTables.swordSpriteTile(swordDir, 1);
        int attr0 = swordTables.swordSpriteAttr(swordDir, 0);
        int attr1 = swordTables.swordSpriteAttr(swordDir, 1);
        if (tile0 != 0xFF) {
            submit8x16(sink, tile0, bladeX, bladeY, attr0, palette);
        }
        if (tile1 != 0xFF) {
            submit8x16(sink, tile1, bladeX + 8, bladeY, attr1, palette);
        }
    }

    private void submit8x16(linksawakening.render.OamSink sink, int tile, int x, int y, int attrs, int[] palette) {
        boolean fx = (attrs & 32) != 0, fy = (attrs & 64) != 0;
        sink.append8x16(
                spriteSheet.tile(fy ? tile + 1 : tile),
                spriteSheet.tile(fy ? tile : tile + 1),
                x,
                y,
                fx,
                fy,
                palette,
                false);
    }

    private void draw8x16(byte[] buffer, int tileIndex, int screenX, int screenY, int attrs, int[] palette) {
        boolean flipX = (attrs & ATTR_FLIP_X) != 0;
        boolean flipY = (attrs & ATTR_FLIP_Y) != 0;
        // GB 8x16 sprite mode: tile T and tile T+1 stacked. flipY swaps
        // which tile is on top and flips each vertically.
        Tile top = spriteSheet.tile(flipY ? tileIndex + 1 : tileIndex);
        Tile bottom = spriteSheet.tile(flipY ? tileIndex : tileIndex + 1);
        ItemSpriteRenderer.drawTile(buffer, top, screenX, screenY, flipX, flipY, palette);
        ItemSpriteRenderer.drawTile(buffer, bottom, screenX, screenY + 8, flipX, flipY, palette);
    }
}
