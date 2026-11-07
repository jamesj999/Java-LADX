package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import static linksawakening.world.EntityMotionMath.*;

/** Shared immutable slot snapshot transformations. */
public final class EntitySnapshots {
    private EntitySnapshots() {}

    public static RoomEntity withStatus(RoomEntity entity, EntityStatus status) {
        return preserveDeathMetadata(entity, new RoomEntity(
            entity.slot(), entity.sourceLoadOrder(), entity.type(), entity.x(), entity.y(),
            status, entity.spriteDefinition(), entity.spriteVariant(), entity.entityFlipAttribute(),
            entity.spriteTileOffset(), entity.z(), -1, false));
    }

    static RoomEntity withType(RoomEntity entity, int type,
                                       EntitySpriteDefinition definition) {
        int variant = definition.supported() ? definition.initialVariant() : -1;
        return preserveDeathMetadata(entity, new RoomEntity(
            entity.slot(), entity.sourceLoadOrder(), type, entity.x(), entity.y(),
            entity.status(), definition, variant, entity.entityFlipAttribute(),
            entity.spriteTileOffset(), entity.z(), -1, false));
    }

    public static RoomEntity withVariant(RoomEntity entity, int variant) {
        return preserveDeathMetadata(entity, new RoomEntity(
            entity.slot(), entity.sourceLoadOrder(), entity.type(), entity.x(), entity.y(),
            entity.status(), entity.spriteDefinition(), variant, entity.entityFlipAttribute(),
            entity.spriteTileOffset(), entity.z(), -1, false));
    }

    public static RoomEntity withFlipAttribute(RoomEntity entity, int flipAttribute) {
        return preserveDeathMetadata(entity, new RoomEntity(
            entity.slot(), entity.sourceLoadOrder(), entity.type(), entity.x(), entity.y(),
            entity.status(), entity.spriteDefinition(), entity.spriteVariant(), flipAttribute,
            entity.spriteTileOffset(), entity.z(), -1, false));
    }

    public static RoomEntity withZ(RoomEntity entity, int z) {
        return preserveDeathMetadata(entity, new RoomEntity(
            entity.slot(), entity.sourceLoadOrder(), entity.type(), entity.x(), entity.y(),
            entity.status(), entity.spriteDefinition(), entity.spriteVariant(),
            entity.entityFlipAttribute(), entity.spriteTileOffset(), z, -1, false));
    }

    public static RoomEntity withDefinition(RoomEntity entity,
                                              EntitySpriteDefinition definition, int variant) {
        int selectedVariant = definition.supported()
            ? Math.min(Math.max(variant, 0), definition.variantCount() - 1) : -1;
        return preserveDeathMetadata(entity, new RoomEntity(
            entity.slot(), entity.sourceLoadOrder(), entity.type(), entity.x(), entity.y(),
            entity.status(), definition, selectedVariant, entity.entityFlipAttribute(),
            entity.spriteTileOffset(), entity.z(), -1, false));
    }

    static RoomEntity preserveDeathMetadata(RoomEntity source, RoomEntity rebuilt) {
        if (source.status() != EntityStatus.DYING || rebuilt.status() != EntityStatus.DYING) {
            return rebuilt;
        }
        return new RoomEntity(rebuilt.slot(), rebuilt.sourceLoadOrder(), rebuilt.type(),
            rebuilt.x(), rebuilt.y(), rebuilt.status(), rebuilt.spriteDefinition(),
            rebuilt.spriteVariant(), rebuilt.entityFlipAttribute(), rebuilt.spriteTileOffset(),
            rebuilt.z(), source.deathSpriteVariant(), source.powerRecoilDeath());
    }

    static RoomEntity withDeathPresentation(RoomEntity entity, int variant,
                                                     boolean powerRecoil) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            entity.x(), entity.y(), entity.status(), entity.spriteDefinition(),
            entity.spriteVariant(), entity.entityFlipAttribute(), entity.spriteTileOffset(),
            entity.z(), variant, powerRecoil);
    }

    public static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y,
                                                       int variant) {
        return preserveDeathMetadata(entity, new RoomEntity(
            entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z(), -1, false));
    }

    static RoomEntity withSourceLoadOrder(RoomEntity entity, int sourceLoadOrder) {
        return preserveDeathMetadata(entity, new RoomEntity(
            entity.slot(), sourceLoadOrder, entity.type(), entity.x(), entity.y(),
            entity.status(), entity.spriteDefinition(), entity.spriteVariant(),
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z(), -1, false));
    }
}
