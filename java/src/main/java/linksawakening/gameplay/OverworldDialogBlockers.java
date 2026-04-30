package linksawakening.gameplay;

public record OverworldDialogBlockers(boolean inventoryBlocking,
                                      boolean scrolling,
                                      boolean transitionInputBlocked,
                                      boolean dialogActive,
                                      boolean dialogInputConsumedThisFrame) {

    public static OverworldDialogBlockers none() {
        return new OverworldDialogBlockers(false, false, false, false, false);
    }

    public boolean blocksOpening() {
        return inventoryBlocking || scrolling || transitionInputBlocked || dialogActive || dialogInputConsumedThisFrame;
    }

    public boolean pausesGameplay() {
        return dialogActive || dialogInputConsumedThisFrame;
    }
}
