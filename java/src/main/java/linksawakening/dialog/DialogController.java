package linksawakening.dialog;
/** Public controller for DialogDrawNextCharacterHandler and the dialog opening/closing state machine. */
public final class DialogController extends DialogTextFormatting {
    /** Named sound event values. */
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
    /** Named box position values. */
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
