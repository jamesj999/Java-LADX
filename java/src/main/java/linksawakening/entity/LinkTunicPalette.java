package linksawakening.entity;

import linksawakening.state.PlayerState;

import java.util.Arrays;

/** ROM object palettes used by Link's green, red, and blue tunics. */
public final class LinkTunicPalette {
    private static final int GREEN_OBJECT_PALETTE = 0;
    private static final int RED_OBJECT_PALETTE = 2;
    private static final int BLUE_OBJECT_PALETTE = 3;
    private static final int[] PALETTE_ROW_FOR_TUNIC = {
        GREEN_OBJECT_PALETTE, RED_OBJECT_PALETTE, BLUE_OBJECT_PALETTE
    };

    private final int[][] objectPalettes;

    private LinkTunicPalette(int[][] objectPalettes) {
        if (objectPalettes == null || objectPalettes.length <= BLUE_OBJECT_PALETTE) {
            throw new IllegalArgumentException("ROM object palette rows are incomplete");
        }
        this.objectPalettes = clonePalettes(objectPalettes);
        for (int[] palette : this.objectPalettes) {
            if (palette == null || palette.length != 4) {
                throw new IllegalArgumentException("ROM object palettes must contain four colors");
            }
        }
    }

    /** Loads {@code ObjectPalettes} through the existing ROM palette decoder. */
    public static LinkTunicPalette loadFromRom(byte[] romData) {
        return new LinkTunicPalette(new EntitySpriteCatalog(romData).loadObjectPalettes());
    }

    /** Returns the exact four RGB colors selected by {@code wTunicType}. */
    public int[] forTunic(int tunicType) {
        if (tunicType < PlayerState.TUNIC_GREEN || tunicType > PlayerState.TUNIC_BLUE) {
            throw new IllegalArgumentException("Tunic type must be 0..2: " + tunicType);
        }
        return objectPalettes[PALETTE_ROW_FOR_TUNIC[tunicType]].clone();
    }

    /** Compatibility source for isolated Link fixtures without a ROM. */
    static LinkTunicPalette greenCompatibility() {
        return new LinkTunicPalette(new int[][] {
            {0x00000000, 0x00000000, 0x0010A840, 0x00F8B888},
            {0, 0, 0, 0},
            {0, 0, 0, 0},
            {0, 0, 0, 0},
            {0, 0, 0, 0},
            {0, 0, 0, 0}
        });
    }

    private static int[][] clonePalettes(int[][] source) {
        int[][] copy = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i] == null ? null : Arrays.copyOf(source[i], source[i].length);
        }
        return copy;
    }
}
