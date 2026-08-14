package linksawakening.render;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.world.HookshotChainOam;
import linksawakening.world.PincerBodyOam;
import linksawakening.world.RoomEntity;
import linksawakening.world.RoomEntitySnapshot;
import linksawakening.world.ScrollController;
import linksawakening.world.WingedOctorokOam;
import linksawakening.world.EntityStatus;

import static linksawakening.world.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_WIDTH;

/** Renders the ROM-backed OAM display lists for the room's loaded entities. */
public final class EntityRenderLayer implements RenderLayer {
    private static final int ENTITY_BOMB = 0x02;
    private static final int ENTITY_SWORD_SHIELD_PICKUP = 0x31;
    private static final int ENTITY_WINGED_OCTOROK = 0xAE;
    private static final int ENTITY_PINCER = 0xB0;
    private static final int BOMB_NORMAL_DEFINITION_BANK = 0x03;
    private static final int BOMB_NORMAL_DEFINITION_ADDRESS = 0x652E;
    private static final int ENTITY_PAIRODD = 0x57;
    private static final int OAM_PALETTE_MASK = 0x07;
    private static final int OAM_PALETTE_FLIP = 0x10;
    private static final int OAM_XFLIP = 0x20;
    private static final int OAM_YFLIP = 0x40;
    // EntityRoomLoader preserves hActiveEntityPosX/Y, which are the values
    // written to hardware OAM. The framebuffer stores sprite top-left pixels.
    private static final int OAM_X_SCREEN_ORIGIN = 0x08;
    private static final int OAM_Y_SCREEN_ORIGIN = 0x10;

    private final RoomEntitySnapshot snapshot;
    private final int[][] objectPalettes;
    private final ScrollController scrollController;
    private final int frameCounter;

    public EntityRenderLayer(RoomEntitySnapshot snapshot, int[][] objectPalettes,
                             ScrollController scrollController) {
        this(snapshot, objectPalettes, scrollController, 0);
    }

    public EntityRenderLayer(RoomEntitySnapshot snapshot, int[][] objectPalettes,
                             ScrollController scrollController, int frameCounter) {
        if (snapshot == null || objectPalettes == null || scrollController == null) {
            throw new IllegalArgumentException("Entity render inputs cannot be null");
        }
        if (objectPalettes.length == 0) {
            throw new IllegalArgumentException("At least one object palette is required");
        }
        for (int[] palette : objectPalettes) {
            if (palette == null || palette.length < 4) {
                throw new IllegalArgumentException("Entity palettes must contain four colors");
            }
        }
        this.snapshot = snapshot;
        this.objectPalettes = clonePalettes(objectPalettes);
        this.scrollController = scrollController;
        this.frameCounter = frameCounter & 0xFF;
    }

    @Override
    public void render(RenderContext context) {
        if (context == null || context.buffer() == null || context.gpu() == null) {
            throw new IllegalArgumentException("Entity render context cannot be null");
        }
        if (scrollController.isActive()) {
            var previousRoom = scrollController.previousRoom();
            if (previousRoom != null && previousRoom.entities() != null
                && previousRoom.entities().spriteSelection() != null) {
                renderSnapshot(context, previousRoom.entities(),
                    previousRoom.entities().spriteSelection().objectPalettes(),
                    previousRoomOffset());
            }
        }
        renderSnapshot(context, snapshot, objectPalettes, currentRoomOffset());
    }

    private void renderSnapshot(RenderContext context, RoomEntitySnapshot entities,
                                int[][] palettes, ScreenOffset offset) {
        EntitySpriteTileSnapshot tiles = entities.spriteTiles();
        for (RoomEntity entity : entities.loadedEntities()) {
            EntitySpriteDefinition definition = entity.spriteDefinition();
            boolean renderingDeath = false;
            if (entity.status() == EntityStatus.DYING && entities.spriteSelection() != null) {
                EntitySpriteDefinition death = entity.powerRecoilDeath()
                    ? entities.spriteSelection().powerRecoilDeathSpriteDefinition()
                    : entities.spriteSelection().deathSpriteDefinition();
                if (death != null && entity.deathSpriteVariant() >= 0
                    && entity.deathSpriteVariant() < death.variantCount()) {
                    definition = death;
                    renderingDeath = true;
                }
            }
            if (!renderingDeath && entities.spriteSelection() != null) {
                EntitySpriteDefinition override = entities.spriteSelection()
                    .spriteOverrideFor(entity.type());
                if (override != null) {
                    definition = override;
                }
            }
            int variant = renderingDeath ? entity.deathSpriteVariant() : entity.spriteVariant();
            if (!renderingDeath && entity.type() == ENTITY_WINGED_OCTOROK) {
                // The pair is allocated before func_007_5805's rectangle in
                // OAM. Draw the later rectangle first so the earlier pair
                // keeps Game Boy OAM priority when the two overlap.
                renderWingedOctorokOam(context, entities.wingedOctorokOam(), entity.slot(),
                    palettes, offset.x(), offset.y(), entity);
            }
            renderEntity(context, entity, definition, variant, palettes, tiles,
                offset.x(), offset.y(), entities.sideScrolling(),
                entities.visualYOffset(entity.slot()));
            if (entity.type() == ENTITY_PINCER) {
                renderPincerBodyOam(context, entities.pincerBodyOam(), entity.slot(), palettes,
                    tiles, offset.x(), offset.y());
            }
            if (!renderingDeath && definition.supported() && entities.spriteSelection() != null) {
                EntitySpriteDefinition overlay = entities.spriteSelection()
                    .spriteOverlayFor(entity.type());
                if (overlay != null) {
                    // FloatingItemEntityHandler renders the two-entry
                    // rectangle after its main display list. Its tile and
                    // attribute bytes remain in the ROM-backed definition.
                    renderEntity(context, entity, overlay, (frameCounter & 0x08) != 0 ? 1 : 0,
                        palettes, tiles, offset.x(), offset.y(), entities.sideScrolling(),
                        entities.visualYOffset(entity.slot()));
                }
            }
            if (entity.status() == EntityStatus.BURNING && entities.spriteSelection() != null) {
                EntitySpriteDefinition burning = entities.spriteSelection()
                    .burningSpriteDefinition();
                if (burning != null) {
                    renderEntity(context, entity, burning, (frameCounter >>> 3) & 0x01,
                        palettes, tiles, offset.x(), offset.y(), entities.sideScrolling(),
                        entities.visualYOffset(entity.slot()));
                }
            }
        }
        renderHookshotChainOam(context, entities, palettes, offset.x(), offset.y());
    }

    private void renderPincerBodyOam(RenderContext context,
                                     java.util.List<PincerBodyOam.Entry> bodyOam,
                                     int sourceSlot, int[][] palettes,
                                     EntitySpriteTileSnapshot tiles,
                                     int offsetX, int offsetY) {
        for (PincerBodyOam.Entry entry : bodyOam) {
            if (entry.sourceSlot() != sourceSlot) {
                continue;
            }
            renderOamSprite(context, palettes, tiles,
                new EntitySpriteDefinition.OamAttribute(entry.tileIndex(), entry.attributes()), 0,
                entry.rawX() + offsetX - OAM_X_SCREEN_ORIGIN,
                entry.rawY() + offsetY - OAM_Y_SCREEN_ORIGIN, false);
        }
    }

    private void renderWingedOctorokOam(RenderContext context,
                                        java.util.List<WingedOctorokOam.Entry> oam,
                                        int sourceSlot, int[][] palettes,
                                        int offsetX, int offsetY, RoomEntity entity) {
        for (int index = oam.size() - 1; index >= 0; index--) {
            WingedOctorokOam.Entry entry = oam.get(index);
            if (entry.sourceSlot() != sourceSlot) {
                continue;
            }
            renderOamSprite(context, palettes, null,
                new EntitySpriteDefinition.OamAttribute(entry.tileIndex(), entry.attributes()),
                entity.entityFlipAttribute(),
                entry.rawX() + offsetX - OAM_X_SCREEN_ORIGIN,
                entry.rawY() + offsetY - OAM_Y_SCREEN_ORIGIN);
        }
    }

    private void renderHookshotChainOam(RenderContext context, RoomEntitySnapshot entities,
                                        int[][] palettes, int offsetX, int offsetY) {
        for (HookshotChainOam.Entry entry : entities.hookshotChainOam()) {
            if (!entry.visible()) {
                continue;
            }
            int attributes = entry.attributes();
            int paletteIndex = attributes & OAM_PALETTE_MASK;
            if ((attributes & OAM_PALETTE_FLIP) != 0) {
                paletteIndex = 4;
            }
            int[] palette = resolvePalette(palettes, paletteIndex);
            IndexedRenderer.drawSpriteTile8x16(context.buffer(), context.gpu(),
                entry.tileIndex(), entry.rawX() + offsetX - OAM_X_SCREEN_ORIGIN,
                entry.rawY() + offsetY - OAM_Y_SCREEN_ORIGIN,
                (attributes & OAM_XFLIP) != 0, (attributes & OAM_YFLIP) != 0, palette);
        }
    }

    private void renderEntity(RenderContext context, RoomEntity entity,
                              EntitySpriteDefinition definition, int spriteVariant,
                              int[][] palettes,
                              EntitySpriteTileSnapshot tiles, int offsetX, int offsetY,
                              boolean sideScrolling, int visualYOffset) {
        if (!definition.supported() || spriteVariant < 0
            || spriteVariant >= definition.variantCount()) {
            return;
        }

        int entityX = entity.x() + offsetX - OAM_X_SCREEN_ORIGIN;
        int entityY = entity.y() + offsetY - OAM_Y_SCREEN_ORIGIN - entity.z()
            + visualYOffset;
        if (entity.type() == ENTITY_BOMB
            && definition.bank() == BOMB_NORMAL_DEFINITION_BANK
            && definition.address() == BOMB_NORMAL_DEFINITION_ADDRESS) {
            // RenderBomb increments hActiveEntityVisualPosY twice before the
            // normal single BombSprite is sent to OAM. The warning pair and
            // explosion rectangle use the unshifted active position.
            entityY += 2;
        }
        int flipAttribute = entity.entityFlipAttribute();
        if (definition.shape() == EntitySpriteDefinition.Shape.PAIR) {
            EntitySpriteTileSnapshot pairTiles = definition.pairTileSource()
                == EntitySpriteDefinition.DynamicSprite.TileSource.GPU ? null : tiles;
            EntitySpriteDefinition.Variant variant = definition.variant(spriteVariant);
            if (entity.type() == ENTITY_PAIRODD && spriteVariant == 3
                && definition.variantCount() > 7) {
                // Bank $04 temporarily moves hActiveEntityPosX by -$08,
                // renders variant $06, moves it by +$10, renders variant
                // $07, and finally restores the source position.
                renderPair(context, palettes, pairTiles, definition.variant(6), entity,
                    entityX - 8, entityY, sideScrolling);
                renderPair(context, palettes, pairTiles, definition.variant(7), entity,
                    entityX + 8, entityY, sideScrolling);
                return;
            }
            renderPair(context, palettes, pairTiles, variant, entity, entityX, entityY,
                sideScrolling);
        } else if (definition.shape() == EntitySpriteDefinition.Shape.SINGLE) {
            EntitySpriteDefinition.Variant variant = definition.variant(spriteVariant);
            // The ROM's sword/shield pickup display list uses tile $84/$86,
            // which lives in the gameplay VRAM item block loaded by
            // LoadBaseTiles, not in the four entity-sheet slots ($40-$7F).
            EntitySpriteTileSnapshot source = entity.type() == ENTITY_SWORD_SHIELD_PICKUP
                ? null : tiles;
            renderOamSprite(context, palettes, source, withTileOffset(variant.first(), entity),
                flipAttribute,
                entityX + 4, entityY - (sideScrolling ? 4 : 0));
        } else if (definition.shape() == EntitySpriteDefinition.Shape.RECTANGLE) {
            for (EntitySpriteDefinition.RectangleSprite sprite
                : definition.rectangleVariant(spriteVariant)) {
                // RenderActiveEntitySpritesRect turns a raw $FF tile into a
                // hidden OAM entry. Keep that sentinel out of the host draw
                // path instead of accidentally drawing entity-sheet tile 0.
                if (sprite.oam().tile() == 0xFF) {
                    continue;
                }
                renderOamSprite(context, palettes, tiles,
                    withTileOffset(sprite.oam(), entity), flipAttribute,
                    entityX + sprite.xOffset(), entityY + sprite.yOffset());
            }
        } else if (definition.shape() == EntitySpriteDefinition.Shape.DYNAMIC) {
            renderDynamic(context, definition.dynamicVariant(spriteVariant), entity,
                entityX, entityY, palettes, tiles);
        }
    }

    private void renderDynamic(RenderContext context,
                               java.util.List<EntitySpriteDefinition.DynamicSprite> sprites,
                               RoomEntity entity, int entityX, int entityY, int[][] palettes,
                               EntitySpriteTileSnapshot tiles) {
        // OAM entries allocated earlier have priority over later entries on
        // the Game Boy. Draw the generated list backwards so the first ROM
        // entry remains visible when the sword overlaps the body pair.
        for (int index = sprites.size() - 1; index >= 0; index--) {
            EntitySpriteDefinition.DynamicSprite sprite = sprites.get(index);
            int xOffset = sprite.xOffset();
            int entityFlipAttribute = sprite.appliesEntityFlipAttribute()
                ? entity.entityFlipAttribute() : 0;
            if (sprite.appliesEntityFlipAttribute()
                && (entityFlipAttribute & OAM_XFLIP) != 0) {
                // RenderActiveEntitySpritesPair swaps the two eight-pixel
                // OAM origins when the active entity carries XFLIP.
                xOffset = 8 - xOffset;
            }
            EntitySpriteTileSnapshot source = sprite.tileSource()
                == EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS
                ? tiles : null;
            renderOamSprite(context, palettes, source, sprite.oam(), entityFlipAttribute,
                entityX + xOffset, entityY + sprite.yOffset(),
                sprite.appliesEntityFlipAttribute());
        }
    }

    private void renderPair(RenderContext context, int[][] palettes,
                            EntitySpriteTileSnapshot tiles,
                            EntitySpriteDefinition.Variant variant, RoomEntity entity,
                            int pairOriginX, int entityY, boolean sideScrolling) {
        int flipAttribute = entity.entityFlipAttribute();
        if (variant.second() == null) {
            // Hiding Zol's bank-$07 handler selects a single-sprite list
            // for variant $01 while using the pair list for the other
            // variants. The catalog represents that mixed path with a
            // null second OAM entry.
            renderOamSprite(context, palettes, tiles, withTileOffset(variant.first(), entity),
                flipAttribute, pairOriginX + 4,
                entityY - (sideScrolling ? 4 : 0));
            return;
        }
        boolean flipX = (flipAttribute & OAM_XFLIP) != 0;
        int firstX = pairOriginX + (flipX ? 8 : 0);
        int secondX = pairOriginX + (flipX ? 0 : 8);
        renderOamSprite(context, palettes, tiles, withTileOffset(variant.first(), entity),
            flipAttribute, firstX, entityY);
        renderOamSprite(context, palettes, tiles, withTileOffset(variant.second(), entity),
            flipAttribute, secondX, entityY);
    }

    private static EntitySpriteDefinition.OamAttribute withTileOffset(
        EntitySpriteDefinition.OamAttribute oam, RoomEntity entity) {
        return new EntitySpriteDefinition.OamAttribute(
            (oam.tile() + entity.spriteTileOffset()) & 0xFF, oam.attributes());
    }

    private void renderOamSprite(RenderContext context, int[][] palettes,
                                 EntitySpriteTileSnapshot tiles,
                                 EntitySpriteDefinition.OamAttribute oam,
                                 int entityFlipAttribute, int screenX, int screenY) {
        renderOamSprite(context, palettes, tiles, oam, entityFlipAttribute, screenX, screenY,
            true);
    }

    private void renderOamSprite(RenderContext context, int[][] palettes,
                                 EntitySpriteTileSnapshot tiles,
                                 EntitySpriteDefinition.OamAttribute oam,
                                 int entityFlipAttribute, int screenX, int screenY,
                                 boolean interpretPaletteFlip) {
        // Pair rendering treats any tile whose low nibble is $F as the ROM's
        // hidden-sprite sentinel after applying hActiveEntityTilesOffset.
        if ((oam.tile() & 0x0F) == 0x0F) {
            return;
        }
        int attributes = oam.attributes() ^ entityFlipAttribute;
        int paletteIndex = attributes & OAM_PALETTE_MASK;
        if (interpretPaletteFlip && (entityFlipAttribute & OAM_PALETTE_FLIP) != 0) {
            // UpdateEntityTimers writes OAMF_PAL1 while an entity is
            // flashing; RenderActiveEntitySpritesPair converts that flag to
            // GBC object palette 4. A raw definition attribute may contain
            // OAMF_PAL1 as the DMG palette selector; it must not be converted
            // to palette 4 unless hActiveEntityFlipAttribute requested it.
            paletteIndex = 4;
        }
        int[] palette = resolvePalette(palettes, paletteIndex);
        if (tiles == null) {
            IndexedRenderer.drawSpriteTile8x16(context.buffer(), context.gpu(), oam.tile(),
                screenX, screenY, (attributes & OAM_XFLIP) != 0,
                (attributes & OAM_YFLIP) != 0, palette);
        } else {
            IndexedRenderer.drawSpriteTile8x16(context.buffer(), tiles, oam.tile(),
                screenX, screenY, (attributes & OAM_XFLIP) != 0,
                (attributes & OAM_YFLIP) != 0, palette);
        }
    }

    private static int[] resolvePalette(int[][] palettes, int paletteIndex) {
        if (paletteIndex < 0 || paletteIndex >= palettes.length) {
            throw new IllegalArgumentException("Object palette index is unavailable: "
                + paletteIndex);
        }
        return palettes[paletteIndex];
    }

    private ScreenOffset currentRoomOffset() {
        int shakeX = -scrollController.screenShakeHorizontal();
        if (!scrollController.isActive()) {
            return new ScreenOffset(shakeX, 0);
        }
        int offset = scrollController.offset();
        return switch (scrollController.direction()) {
            case ScrollController.LEFT -> new ScreenOffset(-ROOM_PIXEL_WIDTH + offset + shakeX, 0);
            case ScrollController.RIGHT -> new ScreenOffset(ROOM_PIXEL_WIDTH - offset + shakeX, 0);
            case ScrollController.UP -> new ScreenOffset(shakeX, -ROOM_PIXEL_HEIGHT + offset);
            case ScrollController.DOWN -> new ScreenOffset(shakeX, ROOM_PIXEL_HEIGHT - offset);
            default -> new ScreenOffset(shakeX, 0);
        };
    }

    private ScreenOffset previousRoomOffset() {
        int shakeX = -scrollController.screenShakeHorizontal();
        if (!scrollController.isActive()) {
            return new ScreenOffset(shakeX, 0);
        }
        int offset = scrollController.offset();
        return switch (scrollController.direction()) {
            case ScrollController.LEFT -> new ScreenOffset(offset + shakeX, 0);
            case ScrollController.RIGHT -> new ScreenOffset(-offset + shakeX, 0);
            case ScrollController.UP -> new ScreenOffset(shakeX, offset);
            case ScrollController.DOWN -> new ScreenOffset(shakeX, -offset);
            default -> new ScreenOffset(shakeX, 0);
        };
    }

    private static int[][] clonePalettes(int[][] source) {
        int[][] copy = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }
        return copy;
    }

    private record ScreenOffset(int x, int y) {
    }
}
