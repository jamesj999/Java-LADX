package linksawakening.world;

import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

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
