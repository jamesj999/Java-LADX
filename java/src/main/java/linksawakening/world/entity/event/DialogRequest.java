package linksawakening.world.entity.event;

/** A ROM dialog-table entry requested by an entity handler this frame. */
public record DialogRequest(int tableId, int dialogLowId, boolean forceTop) implements EntityRequest {
    public DialogRequest(int tableId, int dialogLowId) {
        this(tableId, dialogLowId, false);
    }

    public DialogRequest {
        if (tableId < 0 || tableId > 2) {
            throw new IllegalArgumentException("Dialog table id must be 0, 1, or 2");
        }
        if ((dialogLowId & ~0xFF) != 0) {
            throw new IllegalArgumentException("Dialog id must be an unsigned byte: " + dialogLowId);
        }
    }

    public int globalDialogId() {
        return (tableId << 8) | dialogLowId;
    }
}
