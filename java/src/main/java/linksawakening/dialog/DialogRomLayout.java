package linksawakening.dialog;

import java.io.IOException;
import java.io.UncheckedIOException;
import linksawakening.rom.RomBank;

/** Background row order in DialogOpenAnimationEnd ($1C:4A18). */
final class DialogRomLayout {
    private static final int[] OPENING_ROW_FRAME = loadOpeningRows();

    private DialogRomLayout() {}

    static int openingFrameForRow(int row) {
        return OPENING_ROW_FRAME[row];
    }

    private static int[] loadOpeningRows() {
        try (var input = DialogRomLayout.class.getResourceAsStream("/rom/azle.gbc")) {
            if (input == null) throw new IllegalStateException("Missing classpath ROM");
            byte[] rom = input.readAllBytes();
            int start = RomBank.romOffset(0x1C, 0x4A18);
            int[] frames = new int[5];
            for (int i = 0; i < frames.length; i++) {
                int row = ((rom[start + i] & 255) >>> 5) - 1;
                if (row < 0 || row >= frames.length || frames[row] != 0) {
                    throw new IllegalArgumentException("Invalid ROM dialogue opening row table");
                }
                // BG save/setup plus the draw-command display pipeline precede
                // the first row; then one ROM-selected row appears per frame.
                frames[row] = 5 + i;
            }
            return frames;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
