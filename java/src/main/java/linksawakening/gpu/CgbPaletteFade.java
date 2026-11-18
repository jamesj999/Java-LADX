package linksawakening.gpu;

/** CGB map and inventory fades alternate one complete BG or OBJ palette upload per frame. */
public final class CgbPaletteFade {
    private CgbPaletteFade() {}

    public static int color(int rgb, int fadeCalls, boolean object, boolean out) {
        int calls = (fadeCalls + (object ? 0 : 1)) / 2;
        return shiftColor(rgb, out ? Math.min(31, calls * 4) : Math.min(0, calls * 4 - 31));
    }

    public static int shiftColor(int rgb, int step) {
        if (step == 0) return rgb;
        int result = 0;
        for (int shift = 16; shift >= 0; shift -= 8) {
            int target = (rgb >> shift & 255) >> 3;
            int value = step > 0 ? Math.min(31, target + step) : Math.max(target, -step);
            result |= ((value << 3) | (value >> 2)) << shift;
        }
        return result;
    }
}
