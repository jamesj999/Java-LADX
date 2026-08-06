package linksawakening.ui;

import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

final class FileMenuRomDataTest {

    @Test
    void readsAllFileMenuTablesFromTheirDisassemblyAddresses() {
        byte[] rom = syntheticRom();
        writeBytes(rom, 0x01, 0x48E4, 0x3B, 0x53, 0x6B, 0x83);
        writeBytes(rom, 0x01, 0x4BB5, sequence(0x40, 0x10));
        writeBytes(rom, 0x01, 0x4B30, sequence(0x40, 0x20));
        writeBytes(rom, 0x01, 0x4B70, sequence(0x40, 0x60));
        writeBytes(rom, 0x01, 0x4BB0, 0x4C, 0x54, 0x5C, 0x64, 0x6C);
        writeBytes(rom, 0x1C, 0x4641, sequence(0x100, 0xA0));

        FileMenuRomData data = new FileMenuRomData(rom);

        assertArrayEquals(new int[] { 0x3B, 0x53, 0x6B, 0x83 }, data.selectionCursorYPositions());
        assertArrayEquals(sequence(0x40, 0x10), data.nameEntryCharacterTable());
        assertArrayEquals(sequence(0x40, 0x20), data.nameCursorYPositions());
        assertArrayEquals(sequence(0x40, 0x60), data.nameCursorXPositions());
        assertArrayEquals(new int[] { 0x4C, 0x54, 0x5C, 0x64, 0x6C },
            data.namePositionXPositions());
        assertArrayEquals(sequence(0x100, 0xA0), data.codepointToTileMap());
    }

    @Test
    void returnsDefensiveCopiesOfRomTables() {
        byte[] rom = syntheticRom();
        writeBytes(rom, 0x01, 0x48E4, 0x3B, 0x53, 0x6B, 0x83);
        writeBytes(rom, 0x01, 0x4BB5, sequence(0x40, 0x10));
        writeBytes(rom, 0x01, 0x4B30, sequence(0x40, 0x20));
        writeBytes(rom, 0x01, 0x4B70, sequence(0x40, 0x60));
        writeBytes(rom, 0x01, 0x4BB0, 0x4C, 0x54, 0x5C, 0x64, 0x6C);
        writeBytes(rom, 0x1C, 0x4641, sequence(0x100, 0xA0));

        FileMenuRomData data = new FileMenuRomData(rom);
        int[] first = data.selectionCursorYPositions();
        int[] second = data.selectionCursorYPositions();
        int[] chars = data.nameEntryCharacterTable();
        int[] charsAgain = data.nameEntryCharacterTable();

        first[0] = 0;
        chars[0] = 0;

        assertNotSame(first, second);
        assertNotSame(chars, charsAgain);
        assertArrayEquals(new int[] { 0x3B, 0x53, 0x6B, 0x83 }, second);
        assertArrayEquals(sequence(0x40, 0x10), charsAgain);
    }

    private static byte[] syntheticRom() {
        return new byte[RomBank.romOffset(0x1C, 0x4641) + 0x100];
    }

    private static int[] sequence(int length, int start) {
        int[] bytes = new int[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (start + i) & 0xFF;
        }
        return bytes;
    }

    private static void writeBytes(byte[] rom, int bank, int address, int... values) {
        int offset = RomBank.romOffset(bank, address);
        for (int i = 0; i < values.length; i++) {
            rom[offset + i] = (byte) values[i];
        }
    }
}
