package linksawakening.gpu;

import linksawakening.entity.EntitySpriteCatalog;
import linksawakening.entity.EntitySpriteSelection;
import linksawakening.rom.RomBank;
import linksawakening.world.EntityRoomLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GPUEntityTilesTest {

    @Test
    void loadsFourBbTtttttSheetsIntoTheFourOamTileSlots() {
        byte[] rom = syntheticRom();
        int[] sheetValues = {0x00, 0x41, 0x82, 0xC3};
        int[] banks = {0x00, 0x31, 0x2E, 0x32};

        for (int slot = 0; slot < sheetValues.length; slot++) {
            int sheet = sheetValues[slot];
            int sourceAddress = 0x4000 + (sheet & 0x3F) * 0x100;
            int sourceOffset = RomBank.romOffset(banks[slot], sourceAddress);
            rom[sourceOffset] = (byte) (0x10 + slot);
            rom[sourceOffset + 0xFF] = (byte) (0xE0 + slot);
        }

        GPU gpu = new GPU();
        gpu.loadEntitySpriteSheets(rom, sheetValues);

        for (int slot = 0; slot < 4; slot++) {
            int destination = (0x40 + slot * 0x10) * GPU.TILE_DATA_SIZE;
            assertEquals(0x10 + slot, Byte.toUnsignedInt(gpu.readVRAM(destination)));
            assertEquals(0xE0 + slot,
                Byte.toUnsignedInt(gpu.readVRAM(destination + 0xFF)));
        }
    }

    @Test
    void keepsExistingSlotBytesForKeepCurrentEntries() {
        byte[] rom = syntheticRom();
        GPU gpu = new GPU();
        int destination = 0x50 * GPU.TILE_DATA_SIZE;
        gpu.writeVRAM(destination, (byte) 0x5A);
        gpu.writeVRAM(destination + 0xFF, (byte) 0xA5);

        gpu.loadEntitySpriteSheets(rom, new int[] {0x00, 0xFF, 0x00, 0x00});

        assertEquals(0x5A, Byte.toUnsignedInt(gpu.readVRAM(destination)));
        assertEquals(0xA5, Byte.toUnsignedInt(gpu.readVRAM(destination + 0xFF)));
    }

    @Test
    void shippedRoom92UsesAdjustedGbcNpcBanksAndSixteenTileDestinations() throws Exception {
        byte[] rom = loadRom();
        EntitySpriteSelection selection = new EntitySpriteCatalog(rom)
            .load(EntityRoomLoader.RoomTable.OVERWORLD, 0x92);
        GPU gpu = new GPU();
        gpu.loadEntitySpriteSheets(rom, selection.sheetValues());

        int[] npcBanks = {0x00, 0x31, 0x2E, 0x32};
        int[] sheetValues = selection.sheetValues();
        for (int slot = 0; slot < sheetValues.length; slot++) {
            int sheet = sheetValues[slot];
            if (sheet == 0xFF) {
                continue;
            }
            int sourceOffset = RomBank.romOffset(npcBanks[sheet >>> 6],
                0x4000 + (sheet & 0x3F) * 0x100);
            int destination = (0x40 + slot * 0x10) * GPU.TILE_DATA_SIZE;
            assertEquals(Byte.toUnsignedInt(rom[sourceOffset]),
                Byte.toUnsignedInt(gpu.readVRAM(destination)));
            assertEquals(Byte.toUnsignedInt(rom[sourceOffset + 0xFF]),
                Byte.toUnsignedInt(gpu.readVRAM(destination + 0xFF)));
        }
    }

    private static byte[] syntheticRom() {
        return new byte[RomBank.romOffset(0x33, 0x4000)];
    }

    private static byte[] loadRom() throws Exception {
        try (var stream = GPUEntityTilesTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        }
    }
}
