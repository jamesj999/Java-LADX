package linksawakening.cutscene;

import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class IntroRomDataTest {

    private static final int BANK = 0x01;

    @Test
    void readsIntroTablesFromBankedRomAddressesAndSignExtendsOffsets() {
        byte[] rom = syntheticRom();
        writeBytes(rom, 0x7538,
            0x00, 0x00, 0x1C, 0x02,
            0x00, 0x08, 0x1E, 0x02,
            0x10, 0xF8, 0x20, 0x02,
            0x10, 0x00, 0x22, 0x02,
            0x10, 0x08, 0x24, 0x02,
            0x10, 0x10, 0x26, 0x02);
        writeBytes(rom, 0x7550,
            0xF8, 0x04, 0x32, 0x01,
            0xE8, 0x04, 0x32, 0x01,
            0xD8, 0x04, 0x32, 0x01,
            0xC8, 0x04, 0x32, 0x01);
        writeBytes(rom, 0x7560, 2, 1, 0, 0, 0, 1, 2, 2);

        writeBytes(rom, 0x75CB, sequence(24, 0x40));
        writeBytes(rom, 0x764F, sequence(16, 0x50));
        writeBytes(rom, 0x7A27, sequence(8, 0x60));
        writeBytes(rom, 0x77BD, sequence(32, 0x70));

        IntroRomData data = new IntroRomData(rom);

        List<IntroRomData.OamEntry> ship = data.shipTiles();
        assertEquals(6, ship.size());
        assertEquals(-8, ship.get(2).xOffset());
        assertEquals(0x20, ship.get(2).tileIndex());
        assertEquals(-8, data.additionalShipTiles().get(0).yOffset());
        assertArrayEquals(new int[] { 2, 1, 0, 0, 0, 1, 2, 2 }, data.shipHeaveTable());

        assertEquals(4, data.lightningTiles().size());
        assertEquals(6, data.lightningTiles().get(0).size());
        assertEquals(0x40, data.lightningTiles().get(0).get(0).yOffset());
        assertEquals(4, data.marinVariants().size());
        assertEquals(0x50, data.marinVariants().get(0).firstTileIndex() & 0xFF);
        assertEquals(2, data.inertLinkVariants().size());
        assertEquals(8, data.sparkleVariants().size());
        assertThrows(UnsupportedOperationException.class, () -> ship.add(ship.get(0)));
    }

    @Test
    void readsPointerSelectedTitleRowsAndPostBeachMapWithoutSignedByteLoss() {
        byte[] rom = syntheticRom();
        for (int row = 0; row < 7; row++) {
            int tileAddress = 0x7400 + row * 0x20;
            int attrAddress = 0x7600 + row * 0x20;
            writeWord(rom, 0x7264 + row * 2, tileAddress);
            writeWord(rom, 0x732A + row * 2, attrAddress);
            writeBytes(rom, tileAddress, concat(
                new int[] { 0x9A, 0x16 + row * 0x20, 0x0F },
                sequence(16, row * 0x10)));
            writeBytes(rom, attrAddress, concat(
                new int[] { 0x9A, 0x16 + row * 0x20, 0x0F },
                sequence(16, 0x80 + row * 0x10)));
        }
        writeBytes(rom, 0x7AE4, sequence(20 * 19, 0x90));

        IntroRomData.TitleRow row = new IntroRomData(rom).titleRows().get(6);

        assertEquals(0x9AD6, row.tileTargetAddress());
        assertEquals(0x9AD6, row.attributeTargetAddress());
        assertArrayEquals(sequence(16, 0x60), row.tileBytes());
        assertArrayEquals(sequence(16, 0xE0), row.attributeBytes());
        assertEquals(20 * 19, new IntroRomData(rom).postBeachTilemap().length);
        assertEquals(0x90, new IntroRomData(rom).postBeachTilemap()[0]);
        assertEquals((0x90 + 379) & 0xFF, new IntroRomData(rom).postBeachTilemap()[379]);
    }

    @Test
    void decodesRgb555PaletteRowsFromRom() {
        byte[] rom = syntheticRom();
        int palette = RomBank.romOffset(BANK, 0x79A0);
        writeWordAtOffset(rom, palette, 0x001F);
        writeWordAtOffset(rom, palette + 2, 0x03E0);
        writeWordAtOffset(rom, palette + 4, 0x7C00);
        writeWordAtOffset(rom, palette + 6, 0x7FFF);

        IntroRomData.PaletteBlock block = new IntroRomData(rom).dxFadeInPalette();

        assertEquals(16, block.rows().length);
        assertArrayEquals(new int[] {
            RomBank.decodeRgb555(0x001F),
            RomBank.decodeRgb555(0x03E0),
            RomBank.decodeRgb555(0x7C00),
            RomBank.decodeRgb555(0x7FFF)
        }, block.rows()[0]);
    }

    @Test
    void readsBeachWaveCompensationFromRom() {
        byte[] rom = syntheticRom();
        writeBytes(rom, 0x7CF9, 3, 2, 1, 0, 0, 1, 2, 3);

        assertArrayEquals(new int[] { 3, 2, 1, 0, 0, 1, 2, 3 },
            new IntroRomData(rom).introVerticalOffsets());
    }

    @Test
    void rejectsTruncatedFixedTableReads() {
        byte[] rom = Arrays.copyOf(syntheticRom(), RomBank.romOffset(BANK, 0x7538) + 23);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new IntroRomData(rom).shipTiles());

        assertTrue(exception.getMessage().contains("IntroShipTiles"));
    }

    @Test
    void bankMappingUsesTheBankWindowAddressFormula() {
        assertEquals(0x24200, RomBank.romOffset(0x09, 0x4200));
        assertEquals(0x2310A, RomBank.romOffset(0x08, 0x710A));
    }

    private static byte[] syntheticRom() {
        return new byte[RomBank.romOffset(BANK, 0x7CF9) + 8];
    }

    private static int[] sequence(int length, int start) {
        int[] bytes = new int[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (start + i) & 0xFF;
        }
        return bytes;
    }

    private static int[] concat(int[] first, int[] second) {
        int[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private static void writeBytes(byte[] rom, int address, int... values) {
        int offset = RomBank.romOffset(BANK, address);
        for (int i = 0; i < values.length; i++) {
            rom[offset + i] = (byte) values[i];
        }
    }

    private static void writeWord(byte[] rom, int address, int value) {
        int offset = RomBank.romOffset(BANK, address);
        writeWordAtOffset(rom, offset, value);
    }

    private static void writeWordAtOffset(byte[] rom, int offset, int value) {
        rom[offset] = (byte) value;
        rom[offset + 1] = (byte) (value >>> 8);
    }
}
