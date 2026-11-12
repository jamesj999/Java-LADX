package linksawakening.gameplay;

import static org.junit.jupiter.api.Assertions.*;

import linksawakening.rom.Rom;
import linksawakening.rom.RomSymbols;
import org.junit.jupiter.api.Test;

/** Synthetic buffers exercise ownership and bounds without any original ROM data. */
final class DialogTextLoaderOwnershipTest {
    @Test
    void typedLoaderStillOwnsItsSnapshot() {
        byte[] bytes = new byte[0x74000];
        int pointer = RomSymbols.DialogPointerTable.offset();
        bytes[pointer] = 0;
        bytes[pointer + 1] = 0x40;
        bytes[RomSymbols.DialogBankTable.offset()] = 1;
        bytes[0x4000] = 'A';
        bytes[0x4001] = (byte) 0xff;
        var loader = DialogTextLoader.loadFromRom(Rom.of(bytes));
        bytes[0x4000] = 'B';
        assertEquals("A\u00ff", loader.load(new SignpostDialogRef(0, 0)));
    }

    @Test
    void partialBuffersRetainTheTableBoundsFailure() {
        var error =
                assertThrows(IllegalArgumentException.class, () -> DialogTextLoader.loadFromRom(new Rom(new byte[7])));
        assertEquals("DialogPointerTable is outside the ROM data", error.getMessage());
    }
}
