package linksawakening.ui;

import linksawakening.save.SaveSlotState;

/** CGB file selection, blank LCD transfers and the first world palette fade. */
public final class FileLoadTransition {
    private final SaveSlotState slot;
    private final int worldStart;
    private int frame;
    private byte[] spriteBuffer;

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

    public byte[] clearSpriteBuffer(int size) {
        if (spriteBuffer == null || spriteBuffer.length != size) spriteBuffer = new byte[size];
        else java.util.Arrays.fill(spriteBuffer, (byte) 0);
        return spriteBuffer;
    }

    public void compositeSprites(byte[] room) {
        // func_020_6C7A uploads BG on even ticks and OBJ on the following odd tick.
        MenuTransition.applyPaletteFade(spriteBuffer, Math.max(0, (frame - worldStart - 1) / 2), false);
        for (int i = 0; i < room.length; i += 4) {
            if (spriteBuffer[i + 3] != 0) System.arraycopy(spriteBuffer, i, room, i, 4);
        }
    }

    public void fadeRoom(byte[] buffer) {
        MenuTransition.applyPaletteFade(buffer, (frame - worldStart) / 2, false);
    }
}
