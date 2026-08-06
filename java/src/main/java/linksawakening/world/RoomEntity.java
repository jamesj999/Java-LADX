package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;

/** One of the sixteen entity slots populated by the room definition stream. */
public record RoomEntity(
    int slot,
    int sourceLoadOrder,
    int type,
    int x,
    int y,
    EntityStatus status,
    EntitySpriteDefinition spriteDefinition,
    int spriteVariant,
    int entityFlipAttribute,
    int spriteTileOffset,
    int z,
    int deathSpriteVariant,
    boolean powerRecoilDeath) {

    public RoomEntity(int slot, int sourceLoadOrder, int type, int x, int y,
                      EntityStatus status, EntitySpriteDefinition spriteDefinition,
                      int spriteVariant) {
        this(slot, sourceLoadOrder, type, x, y, status, spriteDefinition, spriteVariant,
            0, 0, 0, -1, false);
    }

    public RoomEntity(int slot, int sourceLoadOrder, int type, int x, int y,
                      EntityStatus status, EntitySpriteDefinition spriteDefinition,
                      int spriteVariant, int entityFlipAttribute) {
        this(slot, sourceLoadOrder, type, x, y, status, spriteDefinition, spriteVariant,
            entityFlipAttribute, 0, 0, -1, false);
    }

    public RoomEntity(int slot, int sourceLoadOrder, int type, int x, int y,
                      EntityStatus status, EntitySpriteDefinition spriteDefinition,
                      int spriteVariant, int entityFlipAttribute, int spriteTileOffset) {
        this(slot, sourceLoadOrder, type, x, y, status, spriteDefinition, spriteVariant,
            entityFlipAttribute, spriteTileOffset, 0, -1, false);
    }

    /** Compatibility constructor for callers using the pre-death-state full signature. */
    public RoomEntity(int slot, int sourceLoadOrder, int type, int x, int y,
                      EntityStatus status, EntitySpriteDefinition spriteDefinition,
                      int spriteVariant, int entityFlipAttribute, int spriteTileOffset, int z) {
        this(slot, sourceLoadOrder, type, x, y, status, spriteDefinition, spriteVariant,
            entityFlipAttribute, spriteTileOffset, z, -1, false);
    }

    public RoomEntity {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        if (sourceLoadOrder < -1) {
            throw new IllegalArgumentException("Entity source load order out of range: "
                + sourceLoadOrder);
        }
        if ((type & ~0xFF) != 0) {
            throw new IllegalArgumentException("Entity type must be an unsigned byte: " + type);
        }
        if ((entityFlipAttribute & ~0xFF) != 0) {
            throw new IllegalArgumentException("Entity flip attributes must be an unsigned byte: "
                + entityFlipAttribute);
        }
        if ((spriteTileOffset & ~0xFF) != 0) {
            throw new IllegalArgumentException("Entity tile offsets must be unsigned bytes: "
                + spriteTileOffset);
        }
        if ((z & ~0xFF) != 0) {
            throw new IllegalArgumentException("Entity Z positions must be unsigned bytes: " + z);
        }
        if (status == null) {
            throw new IllegalArgumentException("Entity status cannot be null");
        }
        if (deathSpriteVariant < -1 || deathSpriteVariant > 3) {
            throw new IllegalArgumentException("Entity death sprite variant is out of range: "
                + deathSpriteVariant);
        }
        if (status != EntityStatus.DYING && deathSpriteVariant != -1) {
            throw new IllegalArgumentException("Non-dying entities must not have a death sprite "
                + "variant: " + deathSpriteVariant);
        }
        if (status != EntityStatus.DYING && powerRecoilDeath) {
            throw new IllegalArgumentException("Non-dying entities must not use power-recoil "
                + "death presentation");
        }
        if (spriteDefinition == null) {
            throw new IllegalArgumentException("Entity sprite definition cannot be null");
        }
        if (spriteVariant < -1 || (spriteDefinition.supported()
            && spriteVariant >= spriteDefinition.variantCount())) {
            throw new IllegalArgumentException("Entity sprite variant is out of range: "
                + spriteVariant);
        }
    }

    public static RoomEntity disabled(int slot) {
        return new RoomEntity(slot, -1, 0xFF, 0, 0, EntityStatus.DISABLED,
            EntitySpriteDefinition.unsupported(0xFF), -1, 0, 0, 0, -1, false);
    }

    public boolean loaded() {
        return status != EntityStatus.DISABLED;
    }
}
