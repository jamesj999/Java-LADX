package linksawakening.ui;

import java.util.Arrays;

/** The island map and the save choice share the ROM's CGB fade and room reload. */
public final class MenuTransition {
    private final byte[] openingFrame;
    private final byte[] roomFrame;
    private final int entryLoadFrames;
    private int transitionFrame;
    private boolean closing;

    private static final int FADE_FRAMES = 16;
    // Tilemap and tile transfers run with the LCD blank between the two palette fades.
    private static final int RETURN_LOAD_FRAMES = 16;

    public MenuTransition(byte[] roomBuffer, int entryLoadFrames) {
        this(roomBuffer.clone(), null, entryLoadFrames);
    }

    private MenuTransition(byte[] openingFrame, byte[] returnFrame, int entryLoadFrames) {
        this.openingFrame = openingFrame;
        this.roomFrame = returnFrame == null ? openingFrame : returnFrame;
        this.entryLoadFrames = entryLoadFrames;
    }

    public MenuTransition enterAnotherMenu(byte[] visibleFrame, int entryLoadFrames) {
        return new MenuTransition(visibleFrame.clone(), roomFrame, entryLoadFrames);
    }

    public void close() {
        closing = true;
        transitionFrame = 0;
    }

    public boolean returningToRoom() {
        return closing;
    }

    public boolean swappedScreens() {
        return transitionFrame == FADE_FRAMES;
    }

    public boolean transitionFinished() {
        return transitionFrame >= 2 * FADE_FRAMES + (closing ? RETURN_LOAD_FRAMES : entryLoadFrames);
    }

    public void tick() {
        if (!transitionFinished()) transitionFrame++;
    }

    public void render(byte[] buffer, Runnable drawMenu) {
        int loadFrames = closing ? RETURN_LOAD_FRAMES : entryLoadFrames;
        if (transitionFrame >= FADE_FRAMES && transitionFrame < FADE_FRAMES + loadFrames) {
            Arrays.fill(buffer, (byte) 255);
            return;
        }
        boolean showRoom = closing ? transitionFrame >= FADE_FRAMES : transitionFrame < FADE_FRAMES;
        if (showRoom) {
            System.arraycopy(closing ? roomFrame : openingFrame, 0, buffer, 0, buffer.length);
        } else {
            drawMenu.run();
        }
        if (!transitionFinished()) {
            // Four RGB555 levels per two frames, saturating at white on the outgoing
            // screen and descending from white toward each original channel on entry.
            boolean outgoing = transitionFrame < FADE_FRAMES;
            int step = outgoing ? transitionFrame / 2 : (transitionFrame - FADE_FRAMES - loadFrames) / 2;
            applyPaletteFade(buffer, step, outgoing);
        }
    }

    static void applyPaletteFade(byte[] buffer, int step, boolean outgoing) {
        for (int i = 0; i < buffer.length; i += 4) {
            for (int channel = i; channel < i + 3; channel++) {
                int base = (buffer[channel] & 255) >> 3;
                int faded = outgoing ? Math.min(31, base + step * 4) : Math.max(base, 31 - step * 4);
                buffer[channel] = (byte) (faded * 255 / 31);
            }
        }
    }
}
