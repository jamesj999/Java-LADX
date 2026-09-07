package linksawakening.world;

import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class OwlStatueDialogResolverTest {
    @Test
    void resolvesKeyCavernHintsAndBeakGate() throws Exception {
        OwlStatueDialogResolver resolver = new OwlStatueDialogResolver(loadRom());
        assertEquals(0x110, resolver.globalDialogId(2, 0x54, false));
        assertEquals(0x282, resolver.globalDialogId(2, 0x54, true));
        assertEquals(0x28B, resolver.globalDialogId(2, 0x40, true));
        assertEquals(0x28C, resolver.globalDialogId(2, 0x47, true));
        assertEquals(0x110, resolver.globalDialogId(0xFF, 0, false));
        assertEquals(0x263, resolver.globalDialogId(0xFF, 0, true));
    }

    @Test
    void followsRomPointersAndPreservesNoMatchFallthrough() throws Exception {
        byte[] rom = loadRom();
        write(rom, 0x4A18, 0x00, 0x70);
        write(rom, 0x4A40, 0x10, 0x70);
        write(rom, 0x7000, 0xA1, 0xA2, 0xA3, 0xA4);
        write(rom, 0x7010, 0x60, 0x61, 0x62);
        OwlStatueDialogResolver resolver = new OwlStatueDialogResolver(rom);
        assertEquals(0x2A1, resolver.globalDialogId(2, 0x60, true));
        assertEquals(0x2A2, resolver.globalDialogId(2, 0x61, true));
        assertEquals(0x2A3, resolver.globalDialogId(2, 0x62, true));
        assertEquals(0x2A4, resolver.globalDialogId(2, 0x54, true));
        assertThrows(IllegalArgumentException.class,
            () -> resolver.globalDialogId(0x16, 0, true));
        assertThrows(IllegalArgumentException.class,
            () -> resolver.globalDialogId(2, 256, true));
        assertThrows(IllegalArgumentException.class,
            () -> new OwlStatueDialogResolver(new byte[0x8000]));
    }

    private static void write(byte[] rom, int address, int... values) {
        int offset = RomBank.romOffset(0x36, address);
        for (int value : values) rom[offset++] = (byte) value;
    }

    private static byte[] loadRom() throws Exception {
        try (var stream = OwlStatueDialogResolverTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) throw new IllegalStateException("Missing ROM");
            return stream.readAllBytes();
        }
    }
}
