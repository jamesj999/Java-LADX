package linksawakening.dialog;

public final class DialogController extends DialogTextFormatting {

    public enum SoundEvent {
        TEXT_PRINT(0x0F),
        DIALOG_BREAK(0x15),
        MOVE_SELECTION(0x0A);

        private final int code;

        SoundEvent(int code) {
            this.code = code;
        }

        public int code() {
            return code;
        }
    }

    public enum BoxPosition {
        TOP(8),
        // DialogOpenAnimationEnd writes rows $0A..$0E, above the HUD.
        BOTTOM(80);

        private final int y;

        BoxPosition(int y) {
            this.y = y;
        }

        public int y() {
            return y;
        }
    }

    public DialogController(int lineWidth) {
        super(lineWidth);
    }
}
