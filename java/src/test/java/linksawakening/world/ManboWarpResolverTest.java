package linksawakening.world;

import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ManboWarpResolverTest {

    @Test
    void loadsTheIndoorManboDestinationRecordsFromBankFourteen() {
        ManboWarpResolver resolver = new ManboWarpResolver(loadRom());

        Warp destination = resolver.resolve(Warp.CATEGORY_INDOOR, 0x00);

        assertEquals(1, destination.category());
        assertEquals(0x00, destination.destMap());
        assertEquals(0x17, destination.destRoom());
        assertEquals(0x50, destination.destX());
        assertEquals(0x7C, destination.destY());
        assertEquals(-1, destination.tileLocation());
    }

    @Test
    void fallsBackToTheRomOverworldPondWarpForOutdoorAndUnsupportedMaps() {
        ManboWarpResolver resolver = new ManboWarpResolver(loadRom());

        Warp outdoor = resolver.resolve(Warp.CATEGORY_OVERWORLD, 0x92);
        Warp unsupportedIndoor = resolver.resolve(Warp.CATEGORY_INDOOR, 0x0A);

        assertEquals(new Warp(0, 0x00, 0x45, 0x38, 0x60, 0x53), outdoor);
        assertEquals(new Warp(0, 0x00, 0x45, 0x38, 0x60, 0x53), unsupportedIndoor);
    }

    @Test
    void keepsTheColorDungeonLowNibbleSelectionUsedByTeleportToManboPond() {
        byte[] rom = loadRom();
        int source = RomBank.romOffset(0x14, 0x4DF1 + 0x0F * 5);
        ManboWarpResolver resolver = new ManboWarpResolver(rom);

        Warp destination = resolver.resolve(Warp.CATEGORY_INDOOR, 0xFF);

        assertEquals(Byte.toUnsignedInt(rom[source]), destination.category());
        assertEquals(Byte.toUnsignedInt(rom[source + 1]), destination.destMap());
        assertEquals(Byte.toUnsignedInt(rom[source + 2]), destination.destRoom());
        assertEquals(Byte.toUnsignedInt(rom[source + 3]), destination.destX());
        assertEquals(Byte.toUnsignedInt(rom[source + 4]), destination.destY());
    }

    private static byte[] loadRom() {
        try (var stream = ManboWarpResolverTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load ROM", e);
        }
    }
}
