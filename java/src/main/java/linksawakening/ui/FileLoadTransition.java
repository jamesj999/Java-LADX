package linksawakening.ui;

import linksawakening.save.SaveSlotState;

/** CGB file selection, blank LCD transfers and the first world palette fade. */
public final class FileLoadTransition {
    private final SaveSlotState slot;
    private final int worldStart;
    private int frame;

    public FileLoadTransition(SaveSlotState slot) {
        this.slot = slot;
        // Measured English DX WorldLoad0..6 durations for each startup path.
        worldStart = slot.spawnPositionX() == 0 ? 40 : slot.spawnIsIndoor() == 0 ? 39 : 38;
    }

    public SaveSlotState slot() { return slot; }
    public void tick() { frame++; }
    public boolean showSelection() { return frame < 3; }
    public boolean stopFileMusic() { return frame == 13; }
    public boolean beforeWorld() { return frame < worldStart; }
    public boolean startWorld() { return frame == worldStart; }
    public boolean blocksControl() { return frame < worldStart + 16; }
    public boolean finished() { return frame >= worldStart + 17; }
    public int sequenceCounter() { return Math.clamp((frame - worldStart) / 4, 0, 4); }

    public void fadeRoom(byte[] buffer) {
        MenuTransition.applyPaletteFade(buffer, (frame - worldStart) / 2, false);
    }
}
