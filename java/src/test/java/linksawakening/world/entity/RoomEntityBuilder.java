package linksawakening.world.entity;

import linksawakening.entity.EntitySpriteDefinition;

/** Test entities retain ROM slot identity and explicit sprite definitions. */
public final class RoomEntityBuilder {
    private int slot = 0;
    private int sourceLoadOrder = -1;
    private int type = 0;
    private int x = 0;
    private int y = 0;
    private EntityStatus status = EntityStatus.ACTIVE;
    private EntitySpriteDefinition spriteDefinition = null;
    private int spriteVariant = 0;
    private int entityFlipAttribute = 0;
    private int spriteTileOffset = 0;
    private int z = 0;
    private int deathSpriteVariant = -1;
    private boolean powerRecoilDeath = false;

    public RoomEntityBuilder slot(int value) {
        slot = value;
        return this;
    }

    public RoomEntityBuilder sourceLoadOrder(int value) {
        sourceLoadOrder = value;
        return this;
    }

    public RoomEntityBuilder type(int value) {
        type = value;
        return this;
    }

    public RoomEntityBuilder status(EntityStatus value) {
        status = value;
        return this;
    }

    public RoomEntityBuilder spriteDefinition(EntitySpriteDefinition value) {
        spriteDefinition = value;
        return this;
    }

    public RoomEntityBuilder spriteVariant(int value) {
        spriteVariant = value;
        return this;
    }

    public RoomEntityBuilder entityFlipAttribute(int value) {
        entityFlipAttribute = value;
        return this;
    }

    public RoomEntityBuilder spriteTileOffset(int value) {
        spriteTileOffset = value;
        return this;
    }

    public RoomEntityBuilder z(int value) {
        z = value;
        return this;
    }

    public RoomEntityBuilder deathSpriteVariant(int value) {
        deathSpriteVariant = value;
        return this;
    }

    public RoomEntityBuilder powerRecoilDeath(boolean value) {
        powerRecoilDeath = value;
        return this;
    }

    public RoomEntityBuilder at(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public RoomEntity build() {
        return new RoomEntity(
                slot,
                sourceLoadOrder,
                type,
                x,
                y,
                status,
                spriteDefinition,
                spriteVariant,
                entityFlipAttribute,
                spriteTileOffset,
                z,
                deathSpriteVariant,
                powerRecoilDeath);
    }
}
