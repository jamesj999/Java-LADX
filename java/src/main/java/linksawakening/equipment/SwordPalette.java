package linksawakening.equipment;

import linksawakening.entity.EntitySpriteCatalog;

import java.util.Arrays;

/** ROM object palettes selected by the normal and fully charged sword. */
public final class SwordPalette {
    private static final int NORMAL_OBJECT_PALETTE = 3;
    private static final int CHARGED_OBJECT_PALETTE = 4;

    private final int[] normal;
    private final int[] charged;

    private SwordPalette(int[][] objectPalettes) {
        if (objectPalettes == null || objectPalettes.length <= CHARGED_OBJECT_PALETTE) {
            throw new IllegalArgumentException("ROM object palette rows are incomplete");
        }
        normal = copyRow(objectPalettes[NORMAL_OBJECT_PALETTE], "normal sword palette");
        charged = copyRow(objectPalettes[CHARGED_OBJECT_PALETTE], "charged sword palette");
    }

    /** Loads the normal and charged sword rows from the ROM object palette table. */
    public static SwordPalette loadFromRom(byte[] romData) {
        return new SwordPalette(new EntitySpriteCatalog(romData).loadObjectPalettes());
    }

    /** Returns the four RGB colors for the normal blade, including OBJ color zero. */
    public int[] normal() {
        return normal.clone();
    }

    /** Returns the four RGB colors for the fully charged blade, including OBJ color zero. */
    public int[] charged() {
        return charged.clone();
    }

    /** Compatibility source for isolated sword fixtures without a ROM. */
    static SwordPalette compatibility() {
        return new SwordPalette(new int[][] {
            {0, 0, 0, 0},
            {0, 0, 0, 0},
            {0, 0, 0, 0},
            {0x00000000, 0x00101010, 0x00787878, 0x00F0F0F0},
            {0x00000000, 0x00201000, 0x00A89068, 0x00F8F8B8},
            {0, 0, 0, 0}
        });
    }

    private static int[] copyRow(int[] row, String description) {
        if (row == null || row.length != 4) {
            throw new IllegalArgumentException(description + " must contain four colors");
        }
        return Arrays.copyOf(row, row.length);
    }
}
