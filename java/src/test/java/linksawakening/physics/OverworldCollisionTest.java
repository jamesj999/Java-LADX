package linksawakening.physics;

import linksawakening.rom.RomBank;
import linksawakening.rom.RomTables;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OverworldCollisionTest {

    private static final int ROOM_OBJECTS_BASE = 0x11;
    private static final int ROOM_OBJECT_ROW_STRIDE = 0x10;
    private static final int SWITCH_BLOCK_CELL = ROOM_OBJECTS_BASE
        + 2 * ROOM_OBJECT_ROW_STRIDE + 2;

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

    @Test
    void groundInteractionPhysicsLookupUsesTheRomEntityCoordinateSample() {
        byte[] rom = new byte[0x100000];
        int physicsOffset = RomBank.romOffset(0x08, 0x4AD4);
        rom[physicsOffset + 0x100 + 0x42] = (byte) 0xF2;
        RomTables tables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(tables);
        collision.setPhysicsTable(RomTables.PHYSICS_TABLE_INDOORS1);

        int[] roomObjects = new int[0x100];
        Arrays.fill(roomObjects, 0xFF);
        // func_003_7E0E samples entityX - 1 and entityY - 7. At ($40,$37)
        // that selects padded row $30, column 3: $11 + $30 + 3.
        roomObjects[0x44] = 0x42;
        collision.setRoom(roomObjects);

        assertEquals(0xF2, collision.objectPhysicsFlagAtGroundInteraction(0x40, 0x37));
    }

    @Test
    void groundInteractionSampleReturnsObjectPhysicsAndAlignedCell() {
        byte[] rom = new byte[0x100000];
        int physicsOffset = RomBank.romOffset(0x08, 0x4AD4);
        rom[physicsOffset + 0x100 + 0x42] = 0x07;
        RomTables tables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(tables);
        collision.setPhysicsTable(RomTables.PHYSICS_TABLE_INDOORS1);

        int[] roomObjects = new int[0x100];
        Arrays.fill(roomObjects, 0xFF);
        roomObjects[0x44] = 0x42;
        collision.setRoom(roomObjects);

        assertEquals(new OverworldCollision.GroundInteractionSample(
            0x42, 0x07, 0x30, 0x30),
            collision.groundInteractionSample(0x40, 0x37));
    }

    @Test
    void switchBlockPointCollisionUsesTheRomStateTable() {
        byte[] rom = romWithSwitchBlockPhysics();
        RomTables tables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(tables);
        int[] roomObjects = emptyRoomObjectsArea();
        collision.setRoom(roomObjects);

        roomObjects[SWITCH_BLOCK_CELL] = 0xDB;
        collision.setSwitchBlocksState(0x00);
        assertFalse(collision.pointBlocked(0x20, 0x20));

        roomObjects[SWITCH_BLOCK_CELL] = 0xDC;
        assertTrue(collision.pointBlocked(0x20, 0x20));

        collision.setSwitchBlocksState(0x02);
        assertFalse(collision.pointBlocked(0x20, 0x20));
    }

    @Test
    void standingOnAMismatchedSwitchBlockTemporarilyPassesItsCollision() {
        byte[] rom = romWithSwitchBlockPhysics();
        RomTables tables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(tables);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[SWITCH_BLOCK_CELL] = 0xDC;
        collision.setRoom(roomObjects);
        collision.setSwitchBlocksState(0x00);

        assertTrue(collision.pointBlocked(0x20, 0x20));
        assertTrue(collision.refreshLinkGroundInteraction(0x18, 0x14));
        assertTrue(collision.linkStandingOnSwitchBlock());
        assertFalse(collision.pointBlocked(0x20, 0x20));

        collision.setSwitchBlocksState(0x02);
        assertFalse(collision.refreshLinkGroundInteraction(0x18, 0x14));
        assertFalse(collision.linkStandingOnSwitchBlock());
    }

    @Test
    void nonSwitchPhysicsDoesNotUseTheSwitchStateOverride() {
        byte[] rom = romWithSwitchBlockPhysics();
        int physicsOffset = RomBank.romOffset(0x08, 0x4AD4);
        rom[physicsOffset + 0xDC] = 0x01;
        RomTables tables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(tables);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[SWITCH_BLOCK_CELL] = 0xDC;
        collision.setRoom(roomObjects);
        collision.setSwitchBlocksState(0x02);

        assertTrue(collision.pointBlocked(0x20, 0x20));
        assertFalse(collision.refreshLinkGroundInteraction(0x18, 0x14));
    }

    @Test
    void changingRoomsClearsTheTransientStandingOverride() {
        byte[] rom = romWithSwitchBlockPhysics();
        RomTables tables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(tables);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[SWITCH_BLOCK_CELL] = 0xDC;
        collision.setRoom(roomObjects);
        collision.setSwitchBlocksState(0x00);
        assertTrue(collision.refreshLinkGroundInteraction(0x18, 0x14));

        collision.setRoom(emptyRoomObjectsArea());

        assertFalse(collision.linkStandingOnSwitchBlock());
    }

    @Test
    void gbcRenderingOverlayDoesNotChangeRoomObjectCollision() {
        byte[] rom = new byte[0x100000];
        int physicsOffset = RomBank.romOffset(0x08, 0x4AD4);
        rom[physicsOffset + 0x04] = PhysicsFlags.NONE;
        rom[physicsOffset + 0x25] = PhysicsFlags.SOLID;
        RomTables tables = RomTables.loadFromRom(rom);
        OverworldCollision collision = new OverworldCollision(tables);
        int[] roomObjects = emptyRoomObjectsArea();
        roomObjects[SWITCH_BLOCK_CELL] = 0x04;
        int[] overlay = new int[80];
        Arrays.fill(overlay, 0x04);
        overlay[2 * 10 + 2] = 0x25;
        collision.setRoom(roomObjects);
        collision.setGbcOverlay(overlay);

        assertEquals(0x04, collision.objectIdAtPoint(0x20, 0x20));
        assertFalse(collision.pointBlocked(0x20, 0x20));
    }

    private static byte[] romWithSwitchBlockPhysics() {
        byte[] rom = new byte[0x100000];
        int physicsOffset = RomBank.romOffset(0x08, 0x4AD4);
        rom[physicsOffset + 0xDB] = 0x04;
        rom[physicsOffset + 0xDC] = 0x04;
        return rom;
    }

    private static int[] emptyRoomObjectsArea() {
        int[] roomObjects = new int[0x100];
        Arrays.fill(roomObjects, 0xFF);
        return roomObjects;
    }
}
