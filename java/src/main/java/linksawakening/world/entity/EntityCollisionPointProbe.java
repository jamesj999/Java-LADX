package linksawakening.world.entity;

import java.util.Objects;
import linksawakening.rom.CollisionTables;
import linksawakening.rom.EntityPropertyTables;
import linksawakening.rom.RomTables;

/** The shared bank-$03 entity wall-collision sample-point calculation. */
public final class EntityCollisionPointProbe {
    private final CollisionTables collisionTables;
    private final EntityPropertyTables entityProperties;

    /** @deprecated External caller: world.room.RoomSessionContext; pass the themed tables. */
    @Deprecated(forRemoval = true)
    public EntityCollisionPointProbe(RomTables romTables) {
        this(Objects.requireNonNull(romTables, "romTables").collisionTables(), romTables.entityPropertyTables());
    }

    public EntityCollisionPointProbe(CollisionTables collisionTables, EntityPropertyTables entityProperties) {
        this.collisionTables = Objects.requireNonNull(collisionTables, "collisionTables");
        this.entityProperties = Objects.requireNonNull(entityProperties, "entityProperties");
    }

    public Sample sample(RoomEntity entity, int direction, int nextX, int nextY) {
        Objects.requireNonNull(entity, "entity");
        if (direction < 0 || direction >= 4) {
            throw new IllegalArgumentException("Entity collision direction out of range: " + direction);
        }
        int collisionBoxType = entityProperties.entityCollisionBoxType(entity.type());
        int pointX = nextX - 0x08 + collisionTables.entityCollisionPointX(collisionBoxType, direction);
        int pointY = nextY - 0x10 + collisionTables.entityCollisionPointY(collisionBoxType, direction);
        return new Sample(pointX, pointY);
    }

    public record Sample(int x, int y) {}
}
