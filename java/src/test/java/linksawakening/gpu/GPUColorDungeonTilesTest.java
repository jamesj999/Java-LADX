package linksawakening.gpu;

import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GPUColorDungeonTilesTest {

    private static final int TABLE_BANK = 0x20;
    private static final int[] TABLES = {0x46AA, 0x46D6, 0x4702, 0x472E};

    @Test
    void loadsTheFourColorDungeonRowsIntoTheirFixedOamSlots() {
        byte[] rom = syntheticRom();
        int roomId = 0x01;
        int[] sourceBanks = {0x35, 0x31, 0x2E, 0x35};
        int[] sourceHighBytes = {0x51, 0x5D, 0x4F, 0x47};

        for (int slot = 0; slot < 4; slot++) {
            writeTableEntry(rom, TABLES[slot], roomId, sourceHighBytes[slot], sourceBanks[slot]);
            int sourceOffset = RomBank.romOffset(sourceBanks[slot], sourceHighBytes[slot] << 8);
            rom[sourceOffset] = (byte) (0x10 + slot);
            rom[sourceOffset + 0xFF] = (byte) (0xE0 + slot);
        }

        GPU gpu = new GPU();
        gpu.loadColorDungeonEntitySheets(rom, roomId);

        for (int slot = 0; slot < 4; slot++) {
            int destination = (0x40 + slot * 0x10) * GPU.TILE_DATA_SIZE;
            assertEquals(0x10 + slot, Byte.toUnsignedInt(gpu.readVRAM(destination)));
            assertEquals(0xE0 + slot,
                Byte.toUnsignedInt(gpu.readVRAM(destination + 0xFF)));
        }
    }

    @Test
    void zeroHighByteKeepsTheExistingColorDungeonSlot() {
        byte[] rom = syntheticRom();
        int roomId = 0x00;
        writeTableEntry(rom, TABLES[0], roomId, 0x00, 0x00);

        GPU gpu = new GPU();
        int destination = 0x40 * GPU.TILE_DATA_SIZE;
        gpu.writeVRAM(destination, (byte) 0x5A);
        gpu.writeVRAM(destination + 0xFF, (byte) 0xA5);

        gpu.loadColorDungeonEntitySheets(rom, roomId);

        assertEquals(0x5A, Byte.toUnsignedInt(gpu.readVRAM(destination)));
        assertEquals(0xA5, Byte.toUnsignedInt(gpu.readVRAM(destination + 0xFF)));
    }

    private static byte[] syntheticRom() {
        return new byte[RomBank.romOffset(0x36, 0x4000)];
    }

    private static void writeTableEntry(byte[] rom, int tableAddress, int roomId,
                                        int sourceHighByte, int sourceBank) {
        int offset = RomBank.romOffset(TABLE_BANK, tableAddress + roomId * 2);
        rom[offset] = (byte) sourceHighByte;
        rom[offset + 1] = (byte) sourceBank;
    }
}
