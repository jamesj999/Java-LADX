package linksawakening.world;

import linksawakening.rom.RomBank;

/**
 * GBC indoor torch palette state from {@code palettes.asm:func_021_53F3} and
 * the transition handler at {@code bank14.asm:$4CB2}.
 */
final class IndoorTorchPaletteEffect {
    private static final int TRANSITION_COUNTDOWN = 0x0B;
    private final byte[] romData;

    private int[][] basePalettes = new int[0][0];
    private int[][] palettes = new int[0][0];
    private int[][] pendingPalettes = new int[0][0];
    private int effectAddress;
    private int targetAddress;
    private int transitionCountdown;
    private int transitionEffect;
    private int paletteDataFlags;

    IndoorTorchPaletteEffect(byte[] romData) {
        this.romData = romData;
    }

    void reset(int[][] roomPalettes, int unlitTorchCount) {
        basePalettes = copy(roomPalettes);
        targetAddress = unlitTorchCount * 4 & 0xFF;
        effectAddress = targetAddress;
        transitionCountdown = 0;
        transitionEffect = 0;
        paletteDataFlags = 0;
        rebuildPalettes();
        palettes = copy(pendingPalettes);
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
        extinguishTorch(true);
    }

    void extinguishTorch(boolean allowPaletteTransition) {
        // $18:7A8C caps the target before checking the GBC motion gate.
        if (targetAddress >= 0x0C) {
            return;
        }
        targetAddress = targetAddress + 4 & 0xFF;
        if (allowPaletteTransition) {
            startTransition(0x80);
        }
    }

    void tick(boolean dialogActive, boolean transitionGraphicsActive) {
        tick(dialogActive, transitionGraphicsActive, 0, 0);
    }

    void tick(boolean dialogActive, boolean transitionGraphicsActive,
              int linkMotionState, int mapId) {
        paletteDataFlags = 0;
        if ((effectAddress == targetAddress && transitionEffect == 0)
            || dialogActive || transitionGraphicsActive || linkMotionState != 0) {
            return;
        }

        transitionCountdown = (transitionCountdown - 1) & 0xFF;
        if (transitionCountdown != 1 && transitionCountdown != 0) {
            return;
        }
        if (transitionCountdown == 1) {
            stepEffect();
            rebuildPalettes();
        } else if (effectAddress == targetAddress) {
            transitionEffect = 0;
        }
        // $14:4D4A uploads 16 colors per pass: BG palettes 0-3 at one,
        // then palettes 4-7 at zero, from the same prepared palette buffer.
        int first = transitionCountdown == 1 ? 0 : 4;
        for (int palette = first; palette < Math.min(first + 4, palettes.length); palette++) {
            palettes[palette] = pendingPalettes[palette].clone();
        }
        paletteDataFlags = 0x81;

        if (transitionCountdown == 0) {
            transitionCountdown = mapId == 0x07 ? 0x0A : TRANSITION_COUNTDOWN;
        }
    }

    boolean transitionActive() {
        return transitionEffect != 0;
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
        if (direction != 0x40 && direction != 0x80) {
            throw new IllegalArgumentException("Unsupported palette transition direction");
        }
        transitionEffect = direction;
        transitionCountdown = TRANSITION_COUNTDOWN;
    }

    private void stepEffect() {
        if ((transitionEffect & 0x80) == 0) {
            effectAddress = (effectAddress & 0x06) != 0
                ? effectAddress - 2 : effectAddress - 4;
        } else {
            effectAddress = (effectAddress & 0x04) == 0
                ? effectAddress + 2 : effectAddress + 4;
        }
        effectAddress &= 0xFF;
    }

    private void rebuildPalettes() {
        pendingPalettes = copy(basePalettes);
        if ((effectAddress & 0x0E) == 0) {
            return;
        }
        // func_021_5432 selects one of the three eight-byte ROM tables.
        int factorsAddress = effectAddress == 0x02 ? 0x53DB
            : effectAddress == 0x08 ? 0x53EB : 0x53E3;
        int factorsOffset = RomBank.romOffset(0x21, factorsAddress);
        for (int palette = 0; palette < pendingPalettes.length; palette++) {
            int factor = Byte.toUnsignedInt(romData[factorsOffset + palette]);
            for (int color = 0; color < pendingPalettes[palette].length; color++) {
                pendingPalettes[palette][color] = scaleRgb555(pendingPalettes[palette][color], factor);
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
