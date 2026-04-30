package linksawakening.gameplay;

public record SignpostDialogRef(int tableId, int dialogLowId) {

    public SignpostDialogRef {
        if (tableId < 0 || tableId > 2) {
            throw new IllegalArgumentException("tableId must be 0, 1, or 2");
        }
        dialogLowId &= 0xFF;
    }

    public int globalDialogId() {
        return (tableId << 8) | dialogLowId;
    }
}
