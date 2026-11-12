package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import java.util.List;
import linksawakening.world.entity.EntityRoomLoader;

/** Pure signed-offset and OAM composition algorithms used by source display-list handlers. */
final class EntitySpriteGeometry {
    private EntitySpriteGeometry() {}

    static void appendMoblinSwordBody(
            List<EntitySpriteDefinition.DynamicSprite> sprites, EntitySpriteDefinition.Variant body) {
        sprites.add(new EntitySpriteDefinition.DynamicSprite(
                0, 0, body.first(), EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, true));
        if (body.second() != null) {
            sprites.add(new EntitySpriteDefinition.DynamicSprite(
                    0, 0x08, body.second(), EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, true));
        }
    }

    static void appendRectangleSprites(
            List<EntitySpriteDefinition.DynamicSprite> destination,
            List<EntitySpriteDefinition.RectangleSprite> source) {
        for (EntitySpriteDefinition.RectangleSprite sprite : source) {
            destination.add(new EntitySpriteDefinition.DynamicSprite(
                    sprite.yOffset(),
                    sprite.xOffset(),
                    sprite.oam(),
                    EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
                    false));
        }
    }

    static int signedByte(byte value) {
        return value;
    }

    static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    static boolean isIndoorRoomTable(EntityRoomLoader.RoomTable roomTable) {
        return roomTable == EntityRoomLoader.RoomTable.INDOORS_A || roomTable == EntityRoomLoader.RoomTable.INDOORS_B;
    }

    static void appendDynamicPair(
            List<EntitySpriteDefinition.DynamicSprite> destination,
            EntitySpriteDefinition.Variant pair,
            EntitySpriteDefinition.DynamicSprite.TileSource tileSource) {
        destination.add(new EntitySpriteDefinition.DynamicSprite(0, 0, pair.first(), tileSource, true));
        if (pair.second() != null) {
            destination.add(new EntitySpriteDefinition.DynamicSprite(0, 0x08, pair.second(), tileSource, true));
        }
    }

    static void appendDynamicPairAtOffset(
            List<EntitySpriteDefinition.DynamicSprite> destination,
            EntitySpriteDefinition.Variant pair,
            int yOffset,
            int xOffset) {
        destination.add(new EntitySpriteDefinition.DynamicSprite(
                signedByte(yOffset),
                signedByte(xOffset),
                pair.first(),
                EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
                true));
        if (pair.second() != null) {
            destination.add(new EntitySpriteDefinition.DynamicSprite(
                    signedByte(yOffset),
                    signedByte(xOffset + 0x08),
                    pair.second(),
                    EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
                    true));
        }
    }

    static void appendMoldormTailPair(
            List<EntitySpriteDefinition.DynamicSprite> destination,
            EntitySpriteDefinition.Variant pair,
            int yOffset,
            int xOffset,
            int paletteXor) {
        destination.add(new EntitySpriteDefinition.DynamicSprite(
                signedByte(yOffset),
                signedByte(xOffset),
                xorAttributes(pair.first(), paletteXor),
                EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
                true));
        if (pair.second() != null) {
            destination.add(new EntitySpriteDefinition.DynamicSprite(
                    signedByte(yOffset),
                    signedByte(xOffset + 0x08),
                    xorAttributes(pair.second(), paletteXor),
                    EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
                    true));
        }
    }

    static EntitySpriteDefinition.OamAttribute xorAttributes(EntitySpriteDefinition.OamAttribute oam, int attributes) {
        return new EntitySpriteDefinition.OamAttribute(oam.tile(), oam.attributes() ^ attributes);
    }

    static boolean isColorShellType(int entityType) {
        return entityType >= ENTITY_COLOR_SHELL_RED && entityType <= ENTITY_COLOR_SHELL_BLUE;
    }
}
