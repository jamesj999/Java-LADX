package linksawakening.world;

import java.util.ArrayList;
import java.util.List;

/**
 * The three dynamic OAM entries emitted by the ROM hookshot-chain handler.
 *
 * <p>The game stores positions in eight-bit registers before applying the two
 * arithmetic shifts. Keeping that wraparound here is important for a chain
 * crossing the screen edge.</p>
 */
public final class HookshotChainOam {
    public static final int LINK_TILE = 0x24;
    public static final int LINK_ATTRIBUTES = 0x00;
    public static final int LINK_COUNT = 3;

    private HookshotChainOam() {
    }

    /** A raw OAM entry before the renderer applies the Game Boy OAM origins. */
    public record Entry(int rawX, int rawY, int tileIndex, int attributes,
                        boolean visible) {
        public Entry {
            rawX &= 0xFF;
            rawY &= 0xFF;
            tileIndex &= 0xFF;
            attributes &= 0xFF;
        }
    }

    /**
     * Mirrors {@code RenderHookshotChain}: the first loop count is three and
     * each following link adds the original signed quarter-delta.
     */
    public static List<Entry> entries(int hookX, int hookY, int linkX, int linkY,
                                      int frameCounter) {
        int deltaX = signedEightBit(hookX - linkX) >> 2;
        int deltaY = signedEightBit(hookY - linkY) >> 2;
        int currentX = deltaX;
        int currentY = deltaY;
        int frame = frameCounter & 0xFF;
        List<Entry> entries = new ArrayList<>(LINK_COUNT);
        for (int count = LINK_COUNT; count > 0; count--) {
            boolean visible = ((count ^ frame) & 0x01) == 0;
            entries.add(new Entry(linkX + currentX + 0x04,
                linkY + currentY, LINK_TILE, LINK_ATTRIBUTES, visible));
            currentX += deltaX;
            currentY += deltaY;
        }
        return List.copyOf(entries);
    }

    private static int signedEightBit(int value) {
        int byteValue = value & 0xFF;
        return byteValue < 0x80 ? byteValue : byteValue - 0x100;
    }
}
