package linksawakening.entity;

/** wDialogGotItem / wDialogGotItemCountdown, ApplyGotItem ($00:103C-1070).
 * Keeps the submitted presentation separate from the live item byte: the
 * Witch can clear that byte after HandleGotItemB has already emitted OAM.
 * Item-specific dialog, pose and sound dispatch remains with the caller.
 */
final class DialogGotItemSequence {
    private int item;
    private int countdown;
    private int presentedItem;

    void start(int value, int frames) {
        if (value <= 0 || value > 0xFF || (frames & ~0xFF) != 0)
            throw new IllegalArgumentException("Got-item state must contain byte values");
        item = value;
        countdown = frames;
    }

    /** Returns the item whose dialog opens now, or zero. Dialog freezes the timer. */
    int advance(boolean dialogActive) {
        presentedItem = 0;
        if (item == 0) return 0;
        int openForItem = 0;
        if (!dialogActive) {
            countdown = (countdown - 1) & 0xFF;
            if (countdown == 2) openForItem = item;
            if (countdown == 0) {
                clear();
                return 0;
            }
        }
        presentedItem = item;
        return openForItem;
    }

    void clear() {
        item = 0;
    }

    void reset() {
        item = countdown = presentedItem = 0;
    }

    int item() {
        return item;
    }

    int countdown() {
        return countdown;
    }

    int presentedItem() {
        return presentedItem;
    }
}
