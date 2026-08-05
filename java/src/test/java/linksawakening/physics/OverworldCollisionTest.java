package linksawakening.physics;

import linksawakening.rom.RomBank;
import linksawakening.rom.RomTables;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class OverworldCollisionTest {

    @Test
    void objectPhysicsLookupUsesThePaddedRoomCoordinateSample() {
        byte[] rom = new byte[0x100000];
        int physicsOffset = RomBank.romOffset(0x08, 0x4AD4);
        rom[physicsOffset + 0x42] = 0x01;
        RomTables tables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(tables);

        int[] roomObjects = new int[0x100];
        Arrays.fill(roomObjects, 0xFF);
        // X=$34 -> column 3. Y=$28 minus $08 -> row $20. The active room
        // begins at padded offset $11, so this is $11 + $20 + 3.
        roomObjects[0x34] = 0x42;
        collision.setRoom(roomObjects);

        assertEquals(0x01, collision.objectPhysicsFlagAtEntityPosition(0x34, 0x28));
    }

    @Test
    void pointPhysicsLookupUsesTheSelectedTableAndTheCollisionCell() {
        byte[] rom = new byte[0x100000];
        int physicsOffset = RomBank.romOffset(0x08, 0x4AD4);
        rom[physicsOffset + 0x100 + 0x42] = 0x07;
        RomTables tables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(tables);
        collision.setPhysicsTable(RomTables.PHYSICS_TABLE_INDOORS1);

        int[] roomObjects = new int[0x100];
        Arrays.fill(roomObjects, 0xFF);
        roomObjects[0x34] = 0x42;
        collision.setRoom(roomObjects);

        assertEquals(0x07, collision.objectPhysicsFlagAtPoint(0x30, 0x20));
    }
}
