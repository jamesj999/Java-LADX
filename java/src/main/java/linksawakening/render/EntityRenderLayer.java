package linksawakening.render;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.world.RoomEntity;
import linksawakening.world.RoomEntitySnapshot;
import linksawakening.world.ScrollController;

import static linksawakening.world.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_WIDTH;

/** Renders the ROM-backed OAM display lists for the room's loaded entities. */
public final class EntityRenderLayer implements RenderLayer {
    private static final int OAM_PALETTE_MASK = 0x07;
    private static final int OAM_XFLIP = 0x20;
    private static final int OAM_YFLIP = 0x40;
    // EntityRoomLoader preserves hActiveEntityPosX/Y, which are the values
    // written to hardware OAM. The framebuffer stores sprite top-left pixels.
    private static final int OAM_X_SCREEN_ORIGIN = 0x08;
    private static final int OAM_Y_SCREEN_ORIGIN = 0x10;

    private final RoomEntitySnapshot snapshot;
    private final int[][] objectPalettes;
    private final ScrollController scrollController;

    public EntityRenderLayer(RoomEntitySnapshot snapshot, int[][] objectPalettes,
                             ScrollController scrollController) {
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
            if (entities.spriteSelection() != null) {
                EntitySpriteDefinition override = entities.spriteSelection()
                    .spriteOverrideFor(entity.type());
                if (override != null) {
                    definition = override;
                }
            }
            renderEntity(context, entity, definition, palettes, tiles, offset.x(), offset.y());
        }
    }

    private void renderEntity(RenderContext context, RoomEntity entity,
                              EntitySpriteDefinition definition, int[][] palettes,
                              EntitySpriteTileSnapshot tiles, int offsetX, int offsetY) {
        if (!definition.supported() || entity.spriteVariant() < 0
            || entity.spriteVariant() >= definition.variantCount()) {
            return;
        }

        int entityX = entity.x() + offsetX - OAM_X_SCREEN_ORIGIN;
        int entityY = entity.y() + offsetY - OAM_Y_SCREEN_ORIGIN - entity.z();
        int flipAttribute = entity.entityFlipAttribute();
        if (definition.shape() == EntitySpriteDefinition.Shape.PAIR) {
            EntitySpriteDefinition.Variant variant = definition.variant(entity.spriteVariant());
            if (variant.second() == null) {
                return;
            }
            boolean flipX = (flipAttribute & OAM_XFLIP) != 0;
            int firstX = entityX + (flipX ? 8 : 0);
            int secondX = entityX + (flipX ? 0 : 8);
            renderOamSprite(context, palettes, tiles, withTileOffset(variant.first(), entity),
                flipAttribute, firstX, entityY);
            renderOamSprite(context, palettes, tiles, withTileOffset(variant.second(), entity),
                flipAttribute, secondX, entityY);
        } else if (definition.shape() == EntitySpriteDefinition.Shape.SINGLE) {
            EntitySpriteDefinition.Variant variant = definition.variant(entity.spriteVariant());
            renderOamSprite(context, palettes, tiles, withTileOffset(variant.first(), entity),
                flipAttribute,
                entityX + 4, entityY);
        } else if (definition.shape() == EntitySpriteDefinition.Shape.RECTANGLE) {
            for (EntitySpriteDefinition.RectangleSprite sprite
                : definition.rectangleVariant(entity.spriteVariant())) {
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
        }
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
        // Pair rendering treats any tile whose low nibble is $F as the ROM's
        // hidden-sprite sentinel after applying hActiveEntityTilesOffset.
        if ((oam.tile() & 0x0F) == 0x0F) {
            return;
        }
        int attributes = oam.attributes() ^ entityFlipAttribute;
        int paletteIndex = attributes & OAM_PALETTE_MASK;
        int[] palette = palettes[Math.min(paletteIndex, palettes.length - 1)];
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

    private ScreenOffset currentRoomOffset() {
        if (!scrollController.isActive()) {
            return new ScreenOffset(0, 0);
        }
        int offset = scrollController.offset();
        return switch (scrollController.direction()) {
            case ScrollController.LEFT -> new ScreenOffset(-offset, 0);
            case ScrollController.RIGHT -> new ScreenOffset(ROOM_PIXEL_WIDTH - offset, 0);
            case ScrollController.UP -> new ScreenOffset(0, -offset);
            case ScrollController.DOWN -> new ScreenOffset(0, ROOM_PIXEL_HEIGHT - offset);
            default -> new ScreenOffset(0, 0);
        };
    }

    private ScreenOffset previousRoomOffset() {
        if (!scrollController.isActive()) {
            return new ScreenOffset(0, 0);
        }
        int offset = scrollController.offset();
        return switch (scrollController.direction()) {
            case ScrollController.LEFT -> new ScreenOffset(offset, 0);
            case ScrollController.RIGHT -> new ScreenOffset(-offset, 0);
            case ScrollController.UP -> new ScreenOffset(0, offset);
            case ScrollController.DOWN -> new ScreenOffset(0, -offset);
            default -> new ScreenOffset(0, 0);
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
