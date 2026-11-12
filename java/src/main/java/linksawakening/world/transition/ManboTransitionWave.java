package linksawakening.world.transition;

import linksawakening.rom.Rom;
import linksawakening.rom.RomSymbols;

/** Decodes the bank-$14 scanline wave table used by RenderTransitionEffect. */
public final class ManboTransitionWave {
    private static final int WAVE_TABLE_SIZE = 0x100;

    private final int[] signedOffsets;

    public ManboTransitionWave(Rom romData) {
        if (romData == null) {
            throw new IllegalArgumentException("ROM data cannot be null");
        }
        signedOffsets = new int[WAVE_TABLE_SIZE];
        int source = RomSymbols.Data_014_4EE8.offset();
        if (source < 0 || source > romData.size() - WAVE_TABLE_SIZE) {
            throw new IllegalArgumentException("Manbo transition wave table exceeds ROM bounds");
        }
        for (int index = 0; index < signedOffsets.length; index++) {
            signedOffsets[index] = (byte) romData.u8(RomSymbols.Data_014_4EE8.plus(index));
        }
    }

    /**
     * Returns the signed BG scroll value for one visible scanline. The source
     * increments its scanline counter before the lookup, and complements the
     * table value on odd frames for both Manbo directions.
     */
    public int offsetFor(int transitionFrame, int scanline) {
        return offsetFor(transitionFrame, scanline, false);
    }

    /** Returns the corresponding reversed-phase value used by Manbo-out. */
    public int offsetFor(int transitionFrame, int scanline, boolean manboOut) {
        int frame = transitionFrame & 0xFF;
        int tableIndex = (((frame >>> 2) + scanline + 1) & 0x1F) | (frame & 0xE0);
        if (manboOut) {
            tableIndex ^= 0xE0;
        }
        int value = signedOffsets[tableIndex];
        return (frame & 0x01) == 0 ? value : -value;
    }

    public int[] offsetsForFrame(int transitionFrame, int scanlineCount) {
        return offsetsForFrame(transitionFrame, scanlineCount, false);
    }

    public int[] offsetsForFrame(int transitionFrame, int scanlineCount, boolean manboOut) {
        if (scanlineCount < 0) {
            throw new IllegalArgumentException("Scanline count cannot be negative");
        }
        int[] offsets = new int[scanlineCount];
        for (int scanline = 0; scanline < offsets.length; scanline++) {
            offsets[scanline] = offsetFor(transitionFrame, scanline, manboOut);
        }
        return offsets;
    }
}
