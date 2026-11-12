package linksawakening.gameplay;

import linksawakening.rom.*;
import linksawakening.rom.RomBank;

/** Dialog text loader behavior and state. */
public final class DialogTextLoader {

    private static final int POINTER_TABLE_BANK = RomSymbols.DialogPointerTable.bank();
    private static final int POINTER_TABLE_ADDR = RomSymbols.DialogPointerTable.address();
    private static final int BANK_TABLE_BANK = RomSymbols.DialogBankTable.bank();
    private static final int BANK_TABLE_ADDR = RomSymbols.DialogBankTable.address();
    private static final int DIALOG_TABLE_SIZE = 0x100;
    private static final int ROM_BANK_MASK = 0x3F;
    private static final int TERMINATOR = 0xFF;
    private static final int ASK = 0xFE;

    private final Rom romData;

    private DialogTextLoader(Rom romData) {
        this.romData = new Rom(romData.bytes(RomAddress.fromOffset(0), romData.size()));
    }

    /** @deprecated Legacy capture tools still supply partial byte buffers; use the Rom overload. */
    @Deprecated(forRemoval = true)
    public static DialogTextLoader loadFromRom(byte[] romData) {
        if (romData == null) throw new IllegalArgumentException("ROM data is required");
        return loadFromRom(new Rom(romData));
    }

    public static DialogTextLoader loadFromRom(Rom romData) {
        if (romData == null) {
            throw new IllegalArgumentException("ROM data is required");
        }
        requireRange(
                romData,
                RomBank.romOffset(POINTER_TABLE_BANK, POINTER_TABLE_ADDR),
                DIALOG_TABLE_SIZE * 3 * 2,
                "DialogPointerTable");
        requireRange(
                romData, RomBank.romOffset(BANK_TABLE_BANK, BANK_TABLE_ADDR), DIALOG_TABLE_SIZE * 3, "DialogBankTable");
        return new DialogTextLoader(romData);
    }

    public String load(SignpostDialogRef dialogRef) {
        if (dialogRef == null) {
            throw new IllegalArgumentException("Dialog reference is required");
        }
        int dialogIndex = dialogRef.globalDialogId();
        int pointerOffset = RomBank.romOffset(POINTER_TABLE_BANK, POINTER_TABLE_ADDR + dialogIndex * 2);
        int address = romData.u8(RomAddress.fromOffset(pointerOffset))
                | (romData.u8(RomAddress.fromOffset(pointerOffset + 1)) << 8);
        int bankOffset = RomBank.romOffset(BANK_TABLE_BANK, BANK_TABLE_ADDR + dialogIndex);
        int bank = romData.u8(RomAddress.fromOffset(bankOffset)) & ROM_BANK_MASK;
        int textOffset = RomBank.romOffset(bank, address);

        StringBuilder text = new StringBuilder();
        for (int offset = textOffset; offset < romData.size(); offset++) {
            int codepoint = romData.u8(RomAddress.fromOffset(offset));
            text.append(decode(codepoint));
            if (codepoint == TERMINATOR || codepoint == ASK) {
                return text.toString();
            }
        }
        throw new IllegalArgumentException(
                "Dialog 0x" + Integer.toHexString(dialogIndex) + " has no terminator before end of ROM");
    }

    private static char decode(int codepoint) {
        if (codepoint == 0x5E) {
            return '\'';
        }
        return (char) (codepoint & 0xFF);
    }

    private static void requireRange(Rom romData, int offset, int length, String label) {
        if (offset < 0 || length < 0 || offset > romData.size() || romData.size() - offset < length) {
            throw new IllegalArgumentException(label + " is outside the ROM data");
        }
    }
}
