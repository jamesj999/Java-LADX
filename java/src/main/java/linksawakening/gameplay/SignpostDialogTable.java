package linksawakening.gameplay;

import linksawakening.rom.RomBank;

public final class SignpostDialogTable {

    private static final int SIGNPOST_DIALOG_TABLE_BANK = 0x14;
    private static final int SIGNPOST_DIALOG_TABLE_ADDR = 0x5118;
    private static final int ROOM_COUNT = 0x100;
    private static final int DIALOG_083_LOW = 0x83;
    private static final int DIALOG_22D_LOW = 0x2D;

    private final int[] dialogLowIds;

    private SignpostDialogTable(int[] dialogLowIds) {
        if (dialogLowIds.length != ROOM_COUNT) {
            throw new IllegalArgumentException("Signpost dialog table must contain 256 entries");
        }
        this.dialogLowIds = dialogLowIds.clone();
    }

    public static SignpostDialogTable loadFromRom(byte[] romData) {
        if (romData == null) {
            throw new IllegalArgumentException("ROM data is required");
        }
        int offset = RomBank.romOffset(SIGNPOST_DIALOG_TABLE_BANK, SIGNPOST_DIALOG_TABLE_ADDR);
        if (offset < 0 || offset + ROOM_COUNT > romData.length) {
            throw new IllegalArgumentException("ROM data does not contain SignpostDialogTable");
        }
        int[] dialogLowIds = new int[ROOM_COUNT];
        for (int i = 0; i < ROOM_COUNT; i++) {
            dialogLowIds[i] = Byte.toUnsignedInt(romData[offset + i]);
        }
        return new SignpostDialogTable(dialogLowIds);
    }

    public SignpostDialogRef resolve(int roomId) {
        int dialogLowId = dialogLowIds[roomId & 0xFF];
        if (dialogLowId == DIALOG_083_LOW) {
            return new SignpostDialogRef(0, dialogLowId);
        }
        if (dialogLowId == DIALOG_22D_LOW) {
            return new SignpostDialogRef(2, dialogLowId);
        }
        return new SignpostDialogRef(1, dialogLowId);
    }
}
