package linksawakening.render;

import linksawakening.dialog.DialogController;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.gpu.EntitySpriteTileSnapshot;

/** Resolves entity OAM attributes, tile sources, and whole-entry dialog hiding. */
class EntityOamPainter {
    DialogController.BoxPosition dialogPosition;

    EntityOamPainter(DialogController.BoxPosition dialogPosition) {
        this.dialogPosition = dialogPosition;
    }

    private static final int OAM_PALETTE_MASK = 0x07;
    private static final int OAM_PALETTE_FLIP = 0x10;
    private static final int OAM_XFLIP = 0x20;
    private static final int OAM_YFLIP = 0x40;
    // EntityRoomLoader preserves hActiveEntityPosX/Y, which are the values
    // written to hardware OAM. The framebuffer stores sprite top-left pixels.
    private static final int OAM_Y_SCREEN_ORIGIN = 0x10;

    final void renderOamSprite(
            RenderContext context,
            int[][] palettes,
            EntitySpriteTileSnapshot tiles,
            EntitySpriteDefinition.OamAttribute oam,
            int entityFlipAttribute,
            int screenX,
            int screenY) {
        renderOamSprite(context, palettes, tiles, oam, entityFlipAttribute, screenX, screenY, true);
    }

    final void renderOamSprite(
            RenderContext context,
            int[][] palettes,
            EntitySpriteTileSnapshot tiles,
            EntitySpriteDefinition.OamAttribute oam,
            int entityFlipAttribute,
            int screenX,
            int screenY,
            boolean interpretPaletteFlip) {
        renderPackedOamSprite(
                context,
                palettes,
                tiles,
                oam.tile() | (oam.attributes() << 8),
                entityFlipAttribute,
                screenX,
                screenY,
                interpretPaletteFlip);
    }

    final void renderPackedOamSprite(
            RenderContext context,
            int[][] palettes,
            EntitySpriteTileSnapshot tiles,
            int packed,
            int entityFlipAttribute,
            int screenX,
            int screenY,
            boolean interpretPaletteFlip) {
        // Pair rendering treats any tile whose low nibble is $F as the ROM's
        // hidden-sprite sentinel after applying hActiveEntityTilesOffset.
        if (((packed & 255) & 0x0F) == 0x0F || hiddenUnderDialog(screenY)) {
            return;
        }
        int attributes = (packed >>> 8) ^ entityFlipAttribute;
        int paletteIndex = attributes & OAM_PALETTE_MASK;
        if (interpretPaletteFlip && (entityFlipAttribute & OAM_PALETTE_FLIP) != 0) {
            // UpdateEntityTimers writes OAMF_PAL1 while an entity is
            // flashing; RenderActiveEntitySpritesPair converts that flag to
            // GBC object palette 4. A raw definition attribute may contain
            // OAMF_PAL1 as the DMG palette selector; it must not be converted
            // to palette 4 unless hActiveEntityFlipAttribute requested it.
            paletteIndex = 4;
        }
        int[] palette = EntityRenderInputs.resolvePalette(palettes, paletteIndex);
        // Room snapshots own only the four replaceable entity sheets,
        // $40-$7F. Common item graphics (heart pieces, rupees, etc.) and
        // Link's shared tiles remain in gameplay VRAM outside that range.
        int base = EntitySpriteTileSnapshot.BASE_TILE_INDEX;
        if (tiles == null || (packed & 255) < base || (packed & 255) >= base + EntitySpriteTileSnapshot.TILE_COUNT) {
            OamDrawing.draw8x16(
                    context,
                    context.gpu(),
                    packed & 255,
                    screenX,
                    screenY,
                    (attributes & OAM_XFLIP) != 0,
                    (attributes & OAM_YFLIP) != 0,
                    palette);
        } else {
            OamDrawing.draw8x16(
                    context,
                    tiles,
                    packed & 255,
                    screenX,
                    screenY,
                    (attributes & OAM_XFLIP) != 0,
                    (attributes & OAM_YFLIP) != 0,
                    palette);
        }
    }

    /** HideSpritesUnderDialog ($01:5F6A) removes whole OAM entries, not pixels. */
    final boolean hiddenUnderDialog(int screenY) {
        int rawY = (screenY + OAM_Y_SCREEN_ORIGIN) & 0xFF;
        return dialogPosition == DialogController.BoxPosition.TOP && rawY < 0x3E
                || dialogPosition == DialogController.BoxPosition.BOTTOM && rawY >= 0x58;
    }
}
