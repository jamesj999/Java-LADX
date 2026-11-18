package linksawakening.gpu;

/** CGB map and inventory fades alternate one complete BG or OBJ palette upload per frame. */
public final class CgbPaletteFade {
    private CgbPaletteFade() {}

    public static int color(int rgb, int fadeCalls, boolean object, boolean out) {
        int calls = (fadeCalls + (object ? 0 : 1)) / 2;
        int result = 0;
        for (int shift = 16; shift >= 0; shift -= 8) {
            int target = (rgb >> shift & 255) >> 3;
            int value = out ? Math.min(31, target + calls * 4) : Math.max(target, 31 - calls * 4);
            result |= ((value << 3) | (value >> 2)) << shift;
        }
        return result;
    }
}
