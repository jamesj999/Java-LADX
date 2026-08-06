package linksawakening.ui;

import java.util.Objects;

public record FileMenuAction(Type type, int selectedSlot, int[] nameBytes) {

    public enum Type {
        NONE,
        START_NEW_GAME,
        LOAD_GAME
    }

    public FileMenuAction {
        type = Objects.requireNonNull(type, "type");
        nameBytes = nameBytes == null ? new int[0] : nameBytes.clone();
    }

    public static FileMenuAction none(int selectedSlot) {
        return new FileMenuAction(Type.NONE, selectedSlot, new int[0]);
    }

    @Override
    public int[] nameBytes() {
        return nameBytes.clone();
    }
}
