package linksawakening.world;

import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class RoomLoaderTest {

    @Test
    void overworldLoaderReadsPointerHeaderObjectsTilemapAndPalette() {
        byte[] rom = new byte[0xA0000];
        int pointer = RomBank.romOffset(0x09, 0x4000);
        rom[pointer] = 0x00;
        rom[pointer + 1] = 0x42;
        int room = RomBank.romOffset(0x09, 0x4200);
        rom[room] = 0x0B;
        rom[room + 1] = (byte) 0xE5;
        rom[room + 2] = (byte) 0xFE;
        int entityPointer = RomBank.romOffset(0x16, 0x4000);
        rom[entityPointer] = 0x00;
        rom[entityPointer + 1] = 0x50;
        int entityStream = RomBank.romOffset(0x16, 0x5000);
        rom[entityStream] = (byte) 0xFF;

        LoadedRoom loaded = new RoomLoader(rom).loadOverworld(0);

        assertEquals(0, loaded.roomId());
        assertEquals(Warp.CATEGORY_OVERWORLD, loaded.mapCategory());
        assertEquals(0x0B, loaded.animatedTilesGroup());
        assertEquals(0xE5, loaded.roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE]);
        assertEquals(RoomConstants.ROOM_TILE_WIDTH * RoomConstants.ROOM_TILE_HEIGHT, loaded.tileIds().length);
    }

    @Test
    void indoorLoaderCanPreserveSideScrollingMapCategory() {
        LoadedRoom loaded = new RoomLoader(loadRom()).loadIndoor(
            0x00, 0x00, null, Warp.CATEGORY_SIDESCROLL);

        assertEquals(Warp.CATEGORY_SIDESCROLL, loaded.mapCategory());
    }

    @Test
    void overworldLoaderAttachesRomBackedEntitySnapshotAndSpriteSelection() {
        LoadedRoom loaded = new RoomLoader(loadRom()).loadOverworld(0x92);

        assertEquals(7, loaded.entities().loadedEntities().size());
        assertEquals(0x73, loaded.entities().loadedEntities().get(0).type());
        assertEquals(0x43, loaded.entities().spriteSelection().groupIndex());
        assertArrayEquals(new int[] { 0xA4, 0xE5, 0xE6, 0xDC },
            loaded.entities().spriteSelection().sheetValues());
    }

    private static byte[] loadRom() {
        try (var stream = RoomLoaderTest.class.getClassLoader().getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load ROM", e);
        }
    }
}
