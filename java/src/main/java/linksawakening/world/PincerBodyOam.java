package linksawakening.world;

import java.util.ArrayList;
import java.util.List;

/** The three handler-generated body tiles emitted by RenderPincer. */
public final class PincerBodyOam {
    private static final int BODY_TILE = 0x6A;
    private static final int BODY_ATTRIBUTES = 0x02;
    private static final int BODY_COUNT = 3;

    private PincerBodyOam() {
    }

    /** A raw OAM entry before the renderer applies the Game Boy OAM origins. */
    public record Entry(int sourceSlot, int rawX, int rawY, int tileIndex, int attributes) {
        public Entry {
            rawX &= 0xFF;
            rawY &= 0xFF;
            tileIndex &= 0xFF;
            attributes &= 0xFF;
        }
    }

    /** Mirrors RenderPincer's three body entries after the head becomes visible. */
    public static List<Entry> entries(int sourceSlot, int holeX, int holeY,
                                      int entityX, int entityY, int state) {
        if (state < 3) {
            return List.of();
        }
        int deltaX = signedByte(entityX - holeX) >> 2;
        int deltaY = signedByte(entityY - holeY) >> 2;
        List<Entry> entries = new ArrayList<>(BODY_COUNT);
        for (int index = 0; index < BODY_COUNT; index++) {
            entries.add(new Entry(sourceSlot, holeX + 0x04 + deltaX * index,
                holeY + deltaY * index, BODY_TILE, BODY_ATTRIBUTES));
        }
        return List.copyOf(entries);
    }

    private static int signedByte(int value) {
        int byteValue = value & 0xFF;
        return byteValue < 0x80 ? byteValue : byteValue - 0x100;
    }
}
