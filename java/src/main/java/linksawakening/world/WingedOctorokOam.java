package linksawakening.world;

import java.util.List;

/** The handler-generated two-entry GPU-tile rectangle for a Winged Octorok. */
public final class WingedOctorokOam {
    private static final int TILE = 0x22;

    private WingedOctorokOam() {
    }

    /** A raw OAM entry before the renderer applies Game Boy OAM origins. */
    public record Entry(int sourceSlot, int rawX, int rawY, int tileIndex, int attributes) {
        public Entry {
            rawX &= 0xFF;
            rawY &= 0xFF;
            tileIndex &= 0xFF;
            attributes &= 0xFF;
        }
    }

    /** Mirrors {@code Data_007_57F5} and {@code RenderActiveEntitySpritesRect}. */
    public static List<Entry> entries(int sourceSlot, int privateState2,
                                      int entityX, int entityY, int entityZ) {
        int firstAttributes = (privateState2 & 0x01) == 0 ? 0x40 : 0x00;
        int secondAttributes = firstAttributes | 0x20;
        int rawY = entityY - entityZ;
        return List.of(
            new Entry(sourceSlot, entityX - 0x04, rawY, TILE, firstAttributes),
            new Entry(sourceSlot, entityX + 0x0C, rawY, TILE, secondAttributes));
    }
}
