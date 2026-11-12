package linksawakening.gameplay;

/** Immutable values for five independent dialog gates; canonical combinations are shared across frames. */
public record OverworldDialogBlockers(
        boolean inventoryBlocking,
        boolean scrolling,
        boolean transitionInputBlocked,
        boolean dialogActive,
        boolean dialogInputConsumedThisFrame) {

    private static final OverworldDialogBlockers[] VALUES = values();

    private static OverworldDialogBlockers[] values() {
        var values = new OverworldDialogBlockers[32];
        for (int mask = 0; mask < values.length; mask++) {
            values[mask] = new OverworldDialogBlockers(
                    (mask & 1) != 0, (mask & 2) != 0, (mask & 4) != 0, (mask & 8) != 0, (mask & 16) != 0);
        }
        return values;
    }

    /** Reuses immutable values for the five independent frame gates. */
    public static OverworldDialogBlockers of(
            boolean inventory, boolean scrolling, boolean transition, boolean dialog, boolean consumed) {
        return VALUES[
                (inventory ? 1 : 0)
                        | (scrolling ? 2 : 0)
                        | (transition ? 4 : 0)
                        | (dialog ? 8 : 0)
                        | (consumed ? 16 : 0)];
    }

    public static OverworldDialogBlockers none() {
        return VALUES[0];
    }

    public boolean blocksOpening() {
        return inventoryBlocking || scrolling || transitionInputBlocked || dialogActive || dialogInputConsumedThisFrame;
    }

    public boolean pausesGameplay() {
        return dialogActive || dialogInputConsumedThisFrame;
    }
}
