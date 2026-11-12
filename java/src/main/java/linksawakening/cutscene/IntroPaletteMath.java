package linksawakening.cutscene;

import java.util.Arrays;
import linksawakening.rom.RomBank;

/** RGB555 palette copy, brighten and approach operations used by intro fades. */
final class IntroPaletteMath {
    private IntroPaletteMath() {}

    static int[][] solidPalettes(int color) {
        int[][] palettes = new int[8][4];
        for (int[] palette : palettes) Arrays.fill(palette, color);
        return palettes;
    }

    static int[][] brighten(int[][] source, int amount, int count) {
        int[][] result = copyPalettes(source);
        for (int i = 0; i < Math.min(count, result.length * 4); i++) {
            int color = result[i / 4][i % 4];
            int r = Math.min(31, (color >>> 19) + amount);
            int g = Math.min(31, ((color >>> 11) & 31) + amount);
            int b = Math.min(31, ((color >>> 3) & 31) + amount);
            result[i / 4][i % 4] = RomBank.decodeRgb555(r | g << 5 | b << 10);
        }
        return result;
    }

    static int[][] approachPalettes(int[][] source, int[][] target) {
        int[][] result = copyPalettes(source);
        for (int p = 0; p < result.length; p++)
            for (int c = 0; c < 4; c++) {
                int v = 0;
                for (int channel = 0; channel < 3; channel++) {
                    int shift = channel * 8 + 3;
                    int a = (source[p][c] >>> shift) & 31;
                    int b = (target[p][c] >>> shift) & 31;
                    v |= Math.max(b, a - 1) << (channel == 0 ? 10 : channel == 1 ? 5 : 0);
                }
                result[p][c] = RomBank.decodeRgb555(v);
            }
        return result;
    }

    static int applyRgb555Modifier(int rgb888, int modifier) {
        int red = (rgb888 >>> 19) & 31;
        int green = (rgb888 >>> 11) & 31;
        int blue = (rgb888 >>> 3) & 31;
        int color = red | (green << 5) | (blue << 10);
        return RomBank.decodeRgb555(color | modifier);
    }

    static int[][] copyPalettes(int[][] source) {
        int[][] copy = new int[source.length][];
        for (int row = 0; row < source.length; row++) {
            copy[row] = source[row].clone();
        }
        return copy;
    }
}
