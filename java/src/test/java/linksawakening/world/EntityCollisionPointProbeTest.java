package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.rom.RomBank;
import linksawakening.rom.RomTables;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class EntityCollisionPointProbeTest {

    private static final int[] POINTS_X = {
        13, 2, 8, 8,
        10, 6, 8, 8,
        16, -1, 8, 8,
        13, 2, 8, 8
    };
    private static final int[] POINTS_Y = {
        8, 8, 2, 13,
        8, 8, 6, 10,
        8, 8, -1, 16,
        8, 8, 2, 13
    };

    @Test
    void followsTheRomCollisionBoxRowAndDirectionForEveryProbe() {
        byte[] rom = new byte[0x100000];
        int hitboxOffset = RomBank.romOffset(0x03, 0x40FB);
        for (int boxType = 0; boxType < 4; boxType++) {
            rom[hitboxOffset + 0x20 + boxType] = (byte) boxType;
        }
        int pointsXOffset = RomBank.romOffset(0x03, 0x785F);
        int pointsYOffset = RomBank.romOffset(0x03, 0x786F);
        for (int index = 0; index < POINTS_X.length; index++) {
            rom[pointsXOffset + index] = (byte) POINTS_X[index];
            rom[pointsYOffset + index] = (byte) POINTS_Y[index];
        }

        EntityCollisionPointProbe probe = new EntityCollisionPointProbe(
            RomTables.loadFromRom(rom));
        int nextX = 0x80;
        int nextY = 0x90;
        for (int boxType = 0; boxType < 4; boxType++) {
            RoomEntity entity = new RoomEntity(0, 0, 0x20 + boxType, nextX, nextY,
                EntityStatus.ACTIVE, EntitySpriteDefinition.unsupported(0x20 + boxType), -1);
            for (int direction = 0; direction < 4; direction++) {
                EntityCollisionPointProbe.Sample sample = probe.sample(
                    entity, direction, nextX, nextY);
                int tableIndex = boxType * 4 + direction;
                assertEquals(nextX - 8 + POINTS_X[tableIndex], sample.x(),
                    "x box=" + boxType + " direction=" + direction);
                assertEquals(nextY - 16 + POINTS_Y[tableIndex], sample.y(),
                    "y box=" + boxType + " direction=" + direction);
            }
        }
    }
}
