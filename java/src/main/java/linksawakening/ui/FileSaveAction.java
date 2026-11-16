package linksawakening.ui;

import java.util.Objects;

/** Immutable file save action values. */
public record FileSaveAction(Type type) {

    public FileSaveAction {
        Objects.requireNonNull(type, "type");
    }

    public static FileSaveAction none() {
        return new FileSaveAction(Type.NONE);
    }
    /** Named type values. */
    public enum Type {
        NONE,
        RETURN_TO_GAME,
        SAVE_AND_QUIT
    }
}
