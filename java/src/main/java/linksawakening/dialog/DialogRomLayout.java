package linksawakening.dialog;

import java.io.IOException;
import java.io.UncheckedIOException;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;

/** ROM row orders for opening ($1C:4A18) and closing ($1C:4A8A). */
final class DialogRomLayout {
    private static final int[] OPENING_ROW_FRAME = loadRows(0x4A18, 5);
    private static final int[] CLOSING_ROW_FRAME = loadRows(0x4A8A, 2);

    private DialogRomLayout() {}

    static int openingFrameForRow(int row) {
        return OPENING_ROW_FRAME[row];
    }

    static int closingFrameForRow(int row) {
        return CLOSING_ROW_FRAME[row];
    }

    private static int[] loadRows(int address, int firstFrame) {
        try (var input = DialogRomLayout.class.getResourceAsStream("/rom/azle.gbc")) {
            if (input == null) throw new IllegalStateException("Missing classpath ROM");
            Rom rom = Rom.of(input.readAllBytes());
            int start = new RomAddress(0x1C, address).offset();
            int[] frames = new int[5];
            for (int i = 0; i < frames.length; i++) {
                int row = (rom.u8(RomAddress.fromOffset(start + i)) >>> 5) - 1;
                if (row < 0 || row >= frames.length || frames[row] != 0) {
                    throw new IllegalArgumentException("Invalid ROM dialogue row table");
                }
                // The draw-command pipeline precedes the first row; opening
                // additionally saves the background before writing its rows.
                frames[row] = firstFrame + i;
            }
            return frames;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
