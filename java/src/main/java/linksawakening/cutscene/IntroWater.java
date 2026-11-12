package linksawakening.cutscene;

import static linksawakening.cutscene.IntroSequence.*;

/** IntroHandler water scroll fractions and animated tile buffer; receives only video write gates and the source clock. */
final class IntroWater {

    private int fraction4, fraction5, fractionD;
    private byte[] waveTiles;
    private int waveDestination = 0x900;
    private int verticalWaveOffset;
    private final int[] sectionScrollOffsets = new int[5];
    private final IntroRomData rom;
    private final IntroVideoMemory video;
    private final IntroClock clockState;
    private final int[] verticalOffsets;

    IntroWater(IntroRomData rom, IntroVideoMemory video, IntroClock clockState, int[] verticalOffsets) {
        this.rom = rom;
        this.video = video;
        this.clockState = clockState;
        this.verticalOffsets = verticalOffsets;
    }

    void updateBeachOffsets(boolean slow) {
        if ((clockState.clock() & (slow ? 15 : 7)) == 0) sectionScrollOffsets[0]++;
        this.fraction4(this.fraction4() + (slow ? 0x28 : 0x50));
        this.fraction5(this.fraction5() + (slow ? 0x2C : 0x58));
        fractionD = fractionD + (slow ? 0x58 : 0xB0);
        sectionScrollOffsets[1] += this.fraction4() >>> 8;
        this.fraction4(this.fraction4() & (255));
        sectionScrollOffsets[2] += this.fraction5() >>> 8;
        this.fraction5(this.fraction5() & (255));
        sectionScrollOffsets[3] += fractionD >>> 8;
        fractionD = fractionD & (255);
        animateWaves(true);
    }

    void animateWaves(boolean beach) {
        if ((clockState.clock() & 15) < 4 || video.drawCommandQueued()) return;
        int chunk = (clockState.clock() & 3) * 32;
        byte[] bytes = rom.tileBytes(rom.animationFrameAddress(beach, clockState.clock()) + chunk, 32);
        System.arraycopy(bytes, 0, waveTiles, chunk, 32);
    }

    void updateWaveOffsets() {
        if ((clockState.clock() & 0x07) == 0) {
            sectionScrollOffsets[0]++;
        }
        if ((clockState.clock() & 0x0F) == 0) {
            sectionScrollOffsets[1]++;
            sectionScrollOffsets[3]++;
        }
        if ((clockState.clock() & 0x1F) == 0) {
            sectionScrollOffsets[2]++;
        }
        this.fraction4(this.fraction4() + (0x28));
        sectionScrollOffsets[4] += this.fraction4() >>> 8;
        this.fraction4(this.fraction4() & (255));
        int waveIndex = ((clockState.clock() + 0xFC) >>> 4) & 0x07;
        verticalWaveOffset = -verticalOffsets[waveIndex];
    }

    int fraction4() {
        return fraction4;
    }

    int fraction4(int value) {
        fraction4 = value;
        return value;
    }

    int fraction5() {
        return fraction5;
    }

    int fraction5(int value) {
        fraction5 = value;
        return value;
    }

    int fractionD() {
        return fractionD;
    }

    int fractionD(int value) {
        fractionD = value;
        return value;
    }

    byte[] waveTiles() {
        return waveTiles;
    }

    byte[] waveTiles(byte[] value) {
        waveTiles = value;
        return value;
    }

    int waveDestination() {
        return waveDestination;
    }

    int waveDestination(int value) {
        waveDestination = value;
        return value;
    }

    int verticalWaveOffset() {
        return verticalWaveOffset;
    }

    int verticalWaveOffset(int value) {
        verticalWaveOffset = value;
        return value;
    }

    int[] sectionScrollOffsets() {
        return sectionScrollOffsets;
    }
}
