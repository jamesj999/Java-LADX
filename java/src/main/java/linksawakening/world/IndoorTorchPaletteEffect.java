package linksawakening.world;

/**
 * GBC indoor torch palette state from {@code palettes.asm:func_021_53F3} and
 * the transition handler at {@code bank14.asm:$4CB2}.
 */
final class IndoorTorchPaletteEffect {
    private static final int TRANSITION_COUNTDOWN = 0x0B;
    private static final int[] EFFECT_TWO_FACTORS =
        {0x0B, 0x0D, 0x0D, 0x0D, 0x0B, 0x0B, 0x0B, 0x0B};
    private static final int[] DEFAULT_EFFECT_FACTORS =
        {0x07, 0x09, 0x09, 0x09, 0x07, 0x07, 0x07, 0x07};
    private static final int[] EFFECT_EIGHT_FACTORS =
        {0x04, 0x05, 0x05, 0x05, 0x04, 0x04, 0x04, 0x04};

    private int[][] basePalettes = new int[0][0];
    private int[][] palettes = new int[0][0];
    private int effectAddress;
    private int targetAddress;
    private int transitionCountdown;
    private int paletteDataFlags;

    void reset(int[][] roomPalettes, int unlitTorchCount) {
        basePalettes = copy(roomPalettes);
        targetAddress = unlitTorchCount * 4 & 0xFF;
        effectAddress = targetAddress;
        transitionCountdown = 0;
        paletteDataFlags = 0;
        rebuildPalettes();
    }

    void clear() {
        reset(new int[0][0], 0);
    }

    void igniteTorch() {
        if (targetAddress == 0) {
            return;
        }
        targetAddress = targetAddress - 4 & 0xFF;
        startTransition(0x40);
    }

    void extinguishTorch() {
        targetAddress = targetAddress + 4 & 0xFF;
        startTransition(0x80);
    }

    void tick(boolean dialogActive, boolean transitionGraphicsActive) {
        paletteDataFlags = 0;
        if (transitionCountdown == 0 || dialogActive || transitionGraphicsActive) {
            return;
        }

        transitionCountdown--;
        if (transitionCountdown == 1 || transitionCountdown == 0) {
            stepTowardTarget();
            rebuildPalettes();
        }
        paletteDataFlags = 0x81;

        if (transitionCountdown == 0 && effectAddress != targetAddress) {
            transitionCountdown = TRANSITION_COUNTDOWN;
        }
    }

    int effectAddress() {
        return effectAddress;
    }

    int targetAddress() {
        return targetAddress;
    }

    int transitionCountdown() {
        return transitionCountdown;
    }

    int paletteDataFlags() {
        return paletteDataFlags;
    }

    int[][] palettes() {
        return palettes;
    }

    private void startTransition(int direction) {
        if (effectAddress == targetAddress) {
            return;
        }
        // wBGPaletteTransitionEffect records the direction in bit 6/7. The
        // host only needs its non-zero lifetime; direction follows the target.
        if (direction != 0x40 && direction != 0x80) {
            throw new IllegalArgumentException("Unsupported palette transition direction");
        }
        transitionCountdown = TRANSITION_COUNTDOWN;
    }

    private void stepTowardTarget() {
        if (effectAddress > targetAddress) {
            effectAddress = (effectAddress & 0x06) != 0
                ? effectAddress - 2 : effectAddress - 4;
        } else if (effectAddress < targetAddress) {
            effectAddress = (effectAddress & 0x04) == 0
                ? effectAddress + 2 : effectAddress + 4;
        }
        effectAddress &= 0xFF;
    }

    private void rebuildPalettes() {
        palettes = copy(basePalettes);
        if (effectAddress == 0) {
            return;
        }
        int[] factors = effectAddress == 0x02 ? EFFECT_TWO_FACTORS
            : effectAddress == 0x08 ? EFFECT_EIGHT_FACTORS
            : DEFAULT_EFFECT_FACTORS;
        for (int palette = 0; palette < palettes.length; palette++) {
            int factor = factors[Math.min(palette, factors.length - 1)];
            for (int color = 0; color < palettes[palette].length; color++) {
                palettes[palette][color] = scaleRgb555(palettes[palette][color], factor);
            }
        }
    }

    private static int scaleRgb555(int rgb, int factor) {
        int red = scaleChannel((rgb >>> 16) & 0xFF, factor);
        int green = scaleChannel((rgb >>> 8) & 0xFF, factor);
        int blue = scaleChannel(rgb & 0xFF, factor);
        return red << 16 | green << 8 | blue;
    }

    private static int scaleChannel(int decodedChannel, int factor) {
        int channel5 = (decodedChannel * 31 + 127) / 255;
        int scaled5 = channel5 * factor / 16;
        return scaled5 * 255 / 31;
    }

    private static int[][] copy(int[][] source) {
        if (source == null) {
            return new int[0][0];
        }
        int[][] result = new int[source.length][];
        for (int index = 0; index < source.length; index++) {
            result[index] = source[index] == null ? new int[0] : source[index].clone();
        }
        return result;
    }
}
