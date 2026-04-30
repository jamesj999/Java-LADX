package linksawakening.gameplay;

import linksawakening.rom.RomBank;

public final class DialogTextLoader {

    private static final int POINTER_TABLE_BANK = 0x1C;
    private static final int POINTER_TABLE_ADDR = 0x4001;
    private static final int BANK_TABLE_BANK = 0x1C;
    private static final int BANK_TABLE_ADDR = 0x4741;
    private static final int DIALOG_TABLE_SIZE = 0x100;
    private static final int ROM_BANK_MASK = 0x3F;
    private static final int TERMINATOR = 0xFF;
    private static final int ASK = 0xFE;

    private final byte[] romData;

    private DialogTextLoader(byte[] romData) {
        this.romData = romData.clone();
    }

    public static DialogTextLoader loadFromRom(byte[] romData) {
        if (romData == null) {
            throw new IllegalArgumentException("ROM data is required");
        }
        requireRange(romData, RomBank.romOffset(POINTER_TABLE_BANK, POINTER_TABLE_ADDR),
            DIALOG_TABLE_SIZE * 3 * 2, "DialogPointerTable");
        requireRange(romData, RomBank.romOffset(BANK_TABLE_BANK, BANK_TABLE_ADDR),
            DIALOG_TABLE_SIZE * 3, "DialogBankTable");
        return new DialogTextLoader(romData);
    }

    public String load(SignpostDialogRef dialogRef) {
        if (dialogRef == null) {
            throw new IllegalArgumentException("Dialog reference is required");
        }
        int dialogIndex = dialogRef.globalDialogId();
        int pointerOffset = RomBank.romOffset(POINTER_TABLE_BANK, POINTER_TABLE_ADDR + dialogIndex * 2);
        int address = Byte.toUnsignedInt(romData[pointerOffset])
            | (Byte.toUnsignedInt(romData[pointerOffset + 1]) << 8);
        int bankOffset = RomBank.romOffset(BANK_TABLE_BANK, BANK_TABLE_ADDR + dialogIndex);
        int bank = Byte.toUnsignedInt(romData[bankOffset]) & ROM_BANK_MASK;
        int textOffset = RomBank.romOffset(bank, address);

        StringBuilder text = new StringBuilder();
        for (int offset = textOffset; offset < romData.length; offset++) {
            int codepoint = Byte.toUnsignedInt(romData[offset]);
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

    private static void requireRange(byte[] romData, int offset, int length, String label) {
        if (offset < 0 || length < 0 || offset > romData.length || romData.length - offset < length) {
            throw new IllegalArgumentException(label + " is outside the ROM data");
        }
    }
}
