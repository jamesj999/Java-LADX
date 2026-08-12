package linksawakening.ui;

import java.util.Objects;

public record FileMenuAction(Type type, int selectedSlot, int targetSlot, int[] nameBytes) {

    public enum Type {
        NONE,
        START_NEW_GAME,
        LOAD_GAME,
        ERASE_SLOT,
        COPY_SLOT
    }

    public FileMenuAction(Type type, int selectedSlot, int[] nameBytes) {
        this(type, selectedSlot, -1, nameBytes);
    }

    public FileMenuAction {
        type = Objects.requireNonNull(type, "type");
        nameBytes = nameBytes == null ? new int[0] : nameBytes.clone();
    }

    public static FileMenuAction none(int selectedSlot) {
        return new FileMenuAction(Type.NONE, selectedSlot, -1, new int[0]);
    }

    @Override
    public int[] nameBytes() {
        return nameBytes.clone();
    }
}
