package linksawakening.world;

import java.util.Objects;

import linksawakening.rom.RomTables;

/** The shared bank-$03 entity wall-collision sample-point calculation. */
final class EntityCollisionPointProbe {
    private final RomTables romTables;

    EntityCollisionPointProbe(RomTables romTables) {
        this.romTables = Objects.requireNonNull(romTables, "romTables");
    }

    Sample sample(RoomEntity entity, int direction, int nextX, int nextY) {
        Objects.requireNonNull(entity, "entity");
        if (direction < 0 || direction >= 4) {
            throw new IllegalArgumentException("Entity collision direction out of range: "
                + direction);
        }
        int collisionBoxType = romTables.entityCollisionBoxType(entity.type());
        int pointX = nextX - 0x08
            + romTables.entityCollisionPointX(collisionBoxType, direction);
        int pointY = nextY - 0x10
            + romTables.entityCollisionPointY(collisionBoxType, direction);
        return new Sample(pointX, pointY);
    }

    record Sample(int x, int y) {
    }
}
