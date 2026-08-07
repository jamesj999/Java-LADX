package linksawakening.ui;

import java.util.Objects;

public record FileSaveAction(Type type) {

    public enum Type {
        NONE,
        RETURN_TO_GAME,
        SAVE_AND_QUIT
    }

    public FileSaveAction {
        type = Objects.requireNonNull(type, "type");
    }

    public static FileSaveAction none() {
        return new FileSaveAction(Type.NONE);
    }
}
