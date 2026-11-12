package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;

import linksawakening.entity.EntitySpriteDefinition;

/** Shared immutable slot snapshot transformations. */
public final class EntitySnapshots {
    private EntitySnapshots() {}

    public static RoomEntity withStatus(RoomEntity entity, EntityStatus status) {
        if (entity.status() == status) return entity;
        return preserveDeathMetadata(
                entity,
                new RoomEntity(
                        entity.slot(),
                        entity.sourceLoadOrder(),
                        entity.type(),
                        entity.x(),
                        entity.y(),
                        status,
                        entity.spriteDefinition(),
                        entity.spriteVariant(),
                        entity.entityFlipAttribute(),
                        entity.spriteTileOffset(),
                        entity.z(),
                        -1,
                        false));
    }

    static RoomEntity withType(RoomEntity entity, int type, EntitySpriteDefinition definition) {
        int variant = definition.supported() ? definition.initialVariant() : -1;
        if (entity.type() == type && entity.spriteDefinition() == definition && entity.spriteVariant() == variant)
            return entity;
        return preserveDeathMetadata(
                entity,
                new RoomEntity(
                        entity.slot(),
                        entity.sourceLoadOrder(),
                        type,
                        entity.x(),
                        entity.y(),
                        entity.status(),
                        definition,
                        variant,
                        entity.entityFlipAttribute(),
                        entity.spriteTileOffset(),
                        entity.z(),
                        -1,
                        false));
    }

    public static RoomEntity withVariant(RoomEntity entity, int variant) {
        if (entity.spriteVariant() == variant) return entity;
        return preserveDeathMetadata(
                entity,
                new RoomEntity(
                        entity.slot(),
                        entity.sourceLoadOrder(),
                        entity.type(),
                        entity.x(),
                        entity.y(),
                        entity.status(),
                        entity.spriteDefinition(),
                        variant,
                        entity.entityFlipAttribute(),
                        entity.spriteTileOffset(),
                        entity.z(),
                        -1,
                        false));
    }

    public static RoomEntity withFlipAttribute(RoomEntity entity, int flipAttribute) {
        return preserveDeathMetadata(
                entity,
                new RoomEntity(
                        entity.slot(),
                        entity.sourceLoadOrder(),
                        entity.type(),
                        entity.x(),
                        entity.y(),
                        entity.status(),
                        entity.spriteDefinition(),
                        entity.spriteVariant(),
                        flipAttribute,
                        entity.spriteTileOffset(),
                        entity.z(),
                        -1,
                        false));
    }

    public static RoomEntity withZ(RoomEntity entity, int z) {
        if (entity.z() == z) return entity;
        return preserveDeathMetadata(
                entity,
                new RoomEntity(
                        entity.slot(),
                        entity.sourceLoadOrder(),
                        entity.type(),
                        entity.x(),
                        entity.y(),
                        entity.status(),
                        entity.spriteDefinition(),
                        entity.spriteVariant(),
                        entity.entityFlipAttribute(),
                        entity.spriteTileOffset(),
                        z,
                        -1,
                        false));
    }

    public static RoomEntity withDefinition(RoomEntity entity, EntitySpriteDefinition definition, int variant) {
        int selectedVariant =
                definition.supported() ? Math.min(Math.max(variant, 0), definition.variantCount() - 1) : -1;
        if (entity.spriteDefinition() == definition && entity.spriteVariant() == selectedVariant) return entity;
        return preserveDeathMetadata(
                entity,
                new RoomEntity(
                        entity.slot(),
                        entity.sourceLoadOrder(),
                        entity.type(),
                        entity.x(),
                        entity.y(),
                        entity.status(),
                        definition,
                        selectedVariant,
                        entity.entityFlipAttribute(),
                        entity.spriteTileOffset(),
                        entity.z(),
                        -1,
                        false));
    }

    static RoomEntity preserveDeathMetadata(RoomEntity source, RoomEntity rebuilt) {
        if (source.status() != EntityStatus.DYING || rebuilt.status() != EntityStatus.DYING) {
            return rebuilt;
        }
        if (source.deathSpriteVariant() == rebuilt.deathSpriteVariant()
                && source.powerRecoilDeath() == rebuilt.powerRecoilDeath()) return rebuilt;
        return new RoomEntity(
                rebuilt.slot(),
                rebuilt.sourceLoadOrder(),
                rebuilt.type(),
                rebuilt.x(),
                rebuilt.y(),
                rebuilt.status(),
                rebuilt.spriteDefinition(),
                rebuilt.spriteVariant(),
                rebuilt.entityFlipAttribute(),
                rebuilt.spriteTileOffset(),
                rebuilt.z(),
                source.deathSpriteVariant(),
                source.powerRecoilDeath());
    }

    static RoomEntity withDeathPresentation(RoomEntity entity, int variant, boolean powerRecoil) {
        if (entity.deathSpriteVariant() == variant && entity.powerRecoilDeath() == powerRecoil) return entity;
        return new RoomEntity(
                entity.slot(),
                entity.sourceLoadOrder(),
                entity.type(),
                entity.x(),
                entity.y(),
                entity.status(),
                entity.spriteDefinition(),
                entity.spriteVariant(),
                entity.entityFlipAttribute(),
                entity.spriteTileOffset(),
                entity.z(),
                variant,
                powerRecoil);
    }

    public static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y, int variant) {
        if (entity.x() == (x & 0xFF) && entity.y() == (y & 0xFF) && entity.spriteVariant() == variant) return entity;
        return preserveDeathMetadata(
                entity,
                new RoomEntity(
                        entity.slot(),
                        entity.sourceLoadOrder(),
                        entity.type(),
                        x & 0xFF,
                        y & 0xFF,
                        entity.status(),
                        entity.spriteDefinition(),
                        variant,
                        entity.entityFlipAttribute(),
                        entity.spriteTileOffset(),
                        entity.z(),
                        -1,
                        false));
    }

    static RoomEntity withSourceLoadOrder(RoomEntity entity, int sourceLoadOrder) {
        if (entity.sourceLoadOrder() == sourceLoadOrder) return entity;
        return preserveDeathMetadata(
                entity,
                new RoomEntity(
                        entity.slot(),
                        sourceLoadOrder,
                        entity.type(),
                        entity.x(),
                        entity.y(),
                        entity.status(),
                        entity.spriteDefinition(),
                        entity.spriteVariant(),
                        entity.entityFlipAttribute(),
                        entity.spriteTileOffset(),
                        entity.z(),
                        -1,
                        false));
    }

    /** Copies a motion result verbatim, including its explicit death presentation. */
    public static RoomEntity withMotion(
            RoomEntity entity,
            int x,
            int y,
            int variant,
            int flipAttribute,
            int z,
            int deathVariant,
            boolean powerRecoilDeath) {
        if (entity.x() == x
                && entity.y() == y
                && entity.spriteVariant() == variant
                && entity.entityFlipAttribute() == flipAttribute
                && entity.z() == z
                && entity.deathSpriteVariant() == deathVariant
                && entity.powerRecoilDeath() == powerRecoilDeath) return entity;
        return new RoomEntity(
                entity.slot(),
                entity.sourceLoadOrder(),
                entity.type(),
                x,
                y,
                entity.status(),
                entity.spriteDefinition(),
                variant,
                flipAttribute,
                entity.spriteTileOffset(),
                z,
                deathVariant,
                powerRecoilDeath);
    }

    static RoomEntity withPresentation(RoomEntity entity, EntityStatus status, int variant, int flip) {
        if (entity.status() == status
                && entity.spriteVariant() == variant
                && entity.entityFlipAttribute() == flip
                && entity.deathSpriteVariant() == -1
                && !entity.powerRecoilDeath()) return entity;
        return new RoomEntity(
                entity.slot(),
                entity.sourceLoadOrder(),
                entity.type(),
                entity.x(),
                entity.y(),
                status,
                entity.spriteDefinition(),
                variant,
                flip,
                entity.spriteTileOffset(),
                entity.z(),
                -1,
                false);
    }
}
