package linksawakening.render;

import java.util.List;
import linksawakening.dialog.DialogController;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.rom.RomSymbols;
import linksawakening.world.entity.PincerBodyOam;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntitySnapshot;
import linksawakening.world.entity.WingedOctorokOam;

/** Expands ROM entity display-list shapes into ordered OAM entries. */
final class EntitySpritePainter {
    DialogController.BoxPosition dialogPosition;

    EntitySpritePainter(DialogController.BoxPosition dialogPosition) {
        this.dialogPosition = dialogPosition;
    }

    // AnimateEntity's UpdateEntityTimers ($14:4D73-$4D76) clears the body
    // tile offset before the shared death handler; $4DE4-$4DF0 retains only
    // OAMF_PAL1. Apply that view without constructing a replacement RoomEntity.
    boolean renderingDeath;

    private int flip(RoomEntity entity) {
        return renderingDeath ? entity.entityFlipAttribute() & 0x10 : entity.entityFlipAttribute();
    }

    private static final int ENTITY_BOMB = 0x02;
    private static final int BOMB_NORMAL_DEFINITION_BANK = RomSymbols.BombSprite.bank();
    private static final int BOMB_NORMAL_DEFINITION_ADDRESS = RomSymbols.BombSprite.address();
    private static final int ENTITY_PAIRODD = 0x57;
    private static final int OAM_PALETTE_MASK = 0x07;
    private static final int OAM_PALETTE_FLIP = 0x10;
    private static final int OAM_XFLIP = 0x20;
    private static final int OAM_YFLIP = 0x40;
    // EntityRoomLoader preserves hActiveEntityPosX/Y, which are the values
    // written to hardware OAM. The framebuffer stores sprite top-left pixels.
    private static final int OAM_X_SCREEN_ORIGIN = 0x08;
    private static final int OAM_Y_SCREEN_ORIGIN = 0x10;

    void renderPincerBodyOam(
            RenderContext context,
            List<PincerBodyOam.Entry> bodyOam,
            int sourceSlot,
            int[][] palettes,
            EntitySpriteTileSnapshot tiles,
            int offsetX,
            int offsetY) {
        for (int entryIndex = 0; entryIndex < bodyOam.size(); entryIndex++) {
            var entry = bodyOam.get(entryIndex);
            if (entry.sourceSlot() != sourceSlot) {
                continue;
            }
            renderPackedOamSprite(
                    context,
                    palettes,
                    tiles,
                    (entry.tileIndex() | (entry.attributes() << 8)),
                    0,
                    entry.rawX() + offsetX - OAM_X_SCREEN_ORIGIN,
                    entry.rawY() + offsetY - OAM_Y_SCREEN_ORIGIN,
                    false);
        }
    }

    void renderWingedOctorokOam(
            RenderContext context,
            List<WingedOctorokOam.Entry> oam,
            int sourceSlot,
            int[][] palettes,
            int offsetX,
            int offsetY,
            RoomEntity entity) {
        for (int index = oam.size() - 1; index >= 0; index--) {
            WingedOctorokOam.Entry entry = oam.get(index);
            if (entry.sourceSlot() != sourceSlot) {
                continue;
            }
            renderPackedOamSprite(
                    context,
                    palettes,
                    null,
                    (entry.tileIndex() | (entry.attributes() << 8)),
                    flip(entity),
                    entry.rawX() + offsetX - OAM_X_SCREEN_ORIGIN,
                    entry.rawY() + offsetY - OAM_Y_SCREEN_ORIGIN,
                    true);
        }
    }

    void renderHookshotChainOam(
            RenderContext context, RoomEntitySnapshot entities, int[][] palettes, int offsetX, int offsetY) {
        var chain = entities.hookshotChainOam();
        for (int entryIndex = 0; entryIndex < chain.size(); entryIndex++) {
            var entry = chain.get(entryIndex);
            if (!entry.visible() || hiddenUnderDialog(entry.rawY() + offsetY - OAM_Y_SCREEN_ORIGIN)) {
                continue;
            }
            int attributes = entry.attributes();
            int paletteIndex = attributes & OAM_PALETTE_MASK;
            if ((attributes & OAM_PALETTE_FLIP) != 0) {
                paletteIndex = 4;
            }
            int[] palette = EntityRenderInputs.resolvePalette(palettes, paletteIndex);
            OamDrawing.draw8x16(
                    context,
                    context.gpu(),
                    entry.tileIndex(),
                    entry.rawX() + offsetX - OAM_X_SCREEN_ORIGIN,
                    entry.rawY() + offsetY - OAM_Y_SCREEN_ORIGIN,
                    (attributes & OAM_XFLIP) != 0,
                    (attributes & OAM_YFLIP) != 0,
                    palette);
        }
    }

    void renderEntity(
            RenderContext context,
            RoomEntity entity,
            EntitySpriteDefinition definition,
            int spriteVariant,
            int[][] palettes,
            EntitySpriteTileSnapshot tiles,
            int offsetX,
            int offsetY,
            boolean sideScrolling,
            int visualYOffset) {
        if (!definition.supported() || spriteVariant < 0 || spriteVariant >= definition.variantCount()) {
            return;
        }

        int entityX = entity.x() + offsetX - OAM_X_SCREEN_ORIGIN;
        int entityY = entity.y() + offsetY - OAM_Y_SCREEN_ORIGIN - entity.z() + visualYOffset;
        if (entity.type() == ENTITY_BOMB
                && definition.bank() == BOMB_NORMAL_DEFINITION_BANK
                && definition.address() == BOMB_NORMAL_DEFINITION_ADDRESS) {
            // RenderBomb increments hActiveEntityVisualPosY twice before the
            // normal single BombSprite is sent to OAM. The warning pair and
            // explosion rectangle use the unshifted active position.
            entityY += 2;
        }
        int flipAttribute = flip(entity);
        if (definition.shape() == EntitySpriteDefinition.Shape.PAIR) {
            renderPairShape(
                    context, entity, definition, spriteVariant, palettes, tiles, entityX, entityY, sideScrolling);
        } else if (definition.shape() == EntitySpriteDefinition.Shape.SINGLE) {
            EntitySpriteDefinition.Variant variant = definition.variant(spriteVariant);
            renderPackedOamSprite(
                    context,
                    palettes,
                    tiles,
                    withTileOffset(variant.first(), entity),
                    flipAttribute,
                    entityX + 4,
                    entityY - (sideScrolling ? 4 : 0),
                    true);
        } else if (definition.shape() == EntitySpriteDefinition.Shape.RECTANGLE) {
            var rectangle = definition.rectangleVariant(spriteVariant);
            for (int i = 0; i < rectangle.size(); i++) {
                var sprite = rectangle.get(i);
                // RenderActiveEntitySpritesRect turns a raw $FF tile into a
                // hidden OAM entry. Keep that sentinel out of the host draw
                // path instead of accidentally drawing entity-sheet tile 0.
                if (sprite.oam().tile() == 0xFF) {
                    continue;
                }
                renderPackedOamSprite(
                        context,
                        palettes,
                        tiles,
                        withTileOffset(sprite.oam(), entity),
                        flipAttribute,
                        entityX + sprite.xOffset(),
                        entityY + sprite.yOffset(),
                        true);
            }
        } else if (definition.shape() == EntitySpriteDefinition.Shape.DYNAMIC) {
            renderDynamic(context, definition.dynamicVariant(spriteVariant), entity, entityX, entityY, palettes, tiles);
        }
    }

    private void renderPairShape(
            RenderContext context,
            RoomEntity entity,
            EntitySpriteDefinition definition,
            int spriteVariant,
            int[][] palettes,
            EntitySpriteTileSnapshot tiles,
            int entityX,
            int entityY,
            boolean sideScrolling) {
        EntitySpriteTileSnapshot pairTiles =
                definition.pairTileSource() == EntitySpriteDefinition.DynamicSprite.TileSource.GPU ? null : tiles;
        EntitySpriteDefinition.Variant variant = definition.variant(spriteVariant);
        if (entity.type() == ENTITY_PAIRODD && spriteVariant == 3 && definition.variantCount() > 7) {
            // Bank $04 temporarily moves hActiveEntityPosX by -$08,
            // renders variant $06, moves it by +$10, renders variant
            // $07, and finally restores the source position.
            renderPair(
                    context, palettes, pairTiles, definition.variant(6), entity, entityX - 8, entityY, sideScrolling);
            renderPair(
                    context, palettes, pairTiles, definition.variant(7), entity, entityX + 8, entityY, sideScrolling);
            return;
        }
        renderPair(context, palettes, pairTiles, variant, entity, entityX, entityY, sideScrolling);
    }

    void renderDynamic(
            RenderContext context,
            List<EntitySpriteDefinition.DynamicSprite> sprites,
            RoomEntity entity,
            int entityX,
            int entityY,
            int[][] palettes,
            EntitySpriteTileSnapshot tiles) {
        // OAM entries allocated earlier have priority over later entries on
        // the Game Boy. Draw the generated list backwards so the first ROM
        // entry remains visible when the sword overlaps the body pair.
        for (int index = sprites.size() - 1; index >= 0; index--) {
            EntitySpriteDefinition.DynamicSprite sprite = sprites.get(index);
            int xOffset = sprite.xOffset();
            // The king's generated list combines ROM rectangle calls, which
            // inherit the hit/death palette flash without swapping pair origins.
            boolean rectangleFlash = entity.type() == 0xE4;
            int entityFlipAttribute =
                    sprite.appliesEntityFlipAttribute() ? flip(entity) : rectangleFlash ? flip(entity) & 0x10 : 0;
            if (sprite.appliesEntityFlipAttribute() && (entityFlipAttribute & OAM_XFLIP) != 0) {
                // RenderActiveEntitySpritesPair swaps the two eight-pixel
                // OAM origins when the active entity carries XFLIP.
                xOffset = 8 - xOffset;
            }
            EntitySpriteTileSnapshot source =
                    sprite.tileSource() == EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS ? tiles : null;
            renderOamSprite(
                    context,
                    palettes,
                    source,
                    sprite.oam(),
                    entityFlipAttribute,
                    entityX + xOffset,
                    entityY + sprite.yOffset(),
                    sprite.appliesEntityFlipAttribute() || rectangleFlash);
        }
    }

    void renderPair(
            RenderContext context,
            int[][] palettes,
            EntitySpriteTileSnapshot tiles,
            EntitySpriteDefinition.Variant variant,
            RoomEntity entity,
            int pairOriginX,
            int entityY,
            boolean sideScrolling) {
        int flipAttribute = flip(entity);
        if (variant.second() == null) {
            // Hiding Zol's bank-$07 handler selects a single-sprite list
            // for variant $01 while using the pair list for the other
            // variants. The catalog represents that mixed path with a
            // null second OAM entry.
            renderPackedOamSprite(
                    context,
                    palettes,
                    tiles,
                    withTileOffset(variant.first(), entity),
                    flipAttribute,
                    pairOriginX + 4,
                    entityY - (sideScrolling ? 4 : 0),
                    true);
            return;
        }
        boolean flipX = (flipAttribute & OAM_XFLIP) != 0;
        int firstX = pairOriginX + (flipX ? 8 : 0);
        int secondX = pairOriginX + (flipX ? 0 : 8);
        renderPackedOamSprite(
                context,
                palettes,
                tiles,
                withTileOffset(variant.first(), entity),
                flipAttribute,
                firstX,
                entityY,
                true);
        renderPackedOamSprite(
                context,
                palettes,
                tiles,
                withTileOffset(variant.second(), entity),
                flipAttribute,
                secondX,
                entityY,
                true);
    }

    private int withTileOffset(EntitySpriteDefinition.OamAttribute oam, RoomEntity entity) {
        return ((oam.tile() + (renderingDeath ? 0 : entity.spriteTileOffset())) & 255) | (oam.attributes() << 8);
    }

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
