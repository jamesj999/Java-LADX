package linksawakening.world;

import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomBank;
import linksawakening.rom.RomTables;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class OpeningHouseRegressionTest {
    @Test
    void houseLoadPreservesFurnitureInTheSwitchBlockVramSlots() throws Exception {
        byte[] rom = rom();
        GPU expected = new GPU();
        expected.loadBaseTiles(rom);
        expected.loadIndoorTiles(rom, 0x10, 0xA3);
        GPU actual = new GPU();
        actual.loadBaseTiles(rom);
        session(rom, actual).loadIndoor(0x10, 0xA3);
        for (int offset = 0x1040; offset < 0x10C0; offset++) {
            assertEquals(expected.readVRAM(offset), actual.readVRAM(offset),
                "Furniture byte $" + Integer.toHexString(offset));
        }
    }

    @Test
    void roomsContainingMobileBlocksStillLoadBothBlockGraphics() throws Exception {
        byte[] rom = rom();
        RoomLoader loader = new RoomLoader(rom);
        int blockRoom = -1;
        for (int room = 0; room < 256 && blockRoom < 0; room++) {
            for (int object : loader.loadIndoor(0, room, new int[8][4]).roomObjectsArea()) {
                if (object == 0xDB || object == 0xDC) { blockRoom = room; break; }
            }
        }
        assertTrue(blockRoom >= 0, "Shipped dungeon must contain mobile blocks");
        GPU gpu = new GPU();
        session(rom, gpu).loadIndoor(0, blockRoom);
        for (var copy : SwitchBlockAnimation.initialCopies(0)) {
            int source = RomBank.romOffset(0x2C, 0x6800 + copy.sourceOffset());
            for (int i = 0; i < 0x40; i++) {
                assertEquals(rom[source + i], gpu.readVRAM(copy.destinationTile() * 16 + i));
            }
        }
    }

    @Test
    void tarinFacesLinkOnTheThirtyTwoFrameCadenceDuringTheBedScene() throws Exception {
        byte[] rom = rom();
        RoomSession session = session(rom, new GPU());
        session.loadIndoor(0x10, 0xA3);
        var before = session.activeRoom().entities().slots().get(1);
        session.updateHouseTarinPresentation(31, 0x38, 0x34);
        assertEquals(before, session.activeRoom().entities().slots().get(1));
        session.updateHouseTarinPresentation(32, 0x38, 0x34);
        var after = session.activeRoom().entities().slots().get(1);
        assertEquals(1, after.spriteVariant());
        assertEquals(before.x(), after.x());
        assertEquals(before.y(), after.y());
    }

    private static RoomSession session(byte[] rom, GPU gpu) {
        return new RoomSession(rom, gpu, new RoomLoader(rom), new OverworldTilesetTable(rom),
            new OverworldCollision(RomTables.loadFromRom(rom)), null, null);
    }

    private static byte[] rom() throws Exception {
        try (var in = OpeningHouseRegressionTest.class.getResourceAsStream("/rom/azle.gbc")) {
            return in.readAllBytes();
        }
    }
}
