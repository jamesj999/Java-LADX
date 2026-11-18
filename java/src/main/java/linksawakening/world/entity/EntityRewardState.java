package linksawakening.world.entity;

/** Shared rewards slot state, independent of behavior dispatch and construction order. */
final class EntityRewardState {
    private final int[] swordPickupState = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] swordPickupSequenceActive = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] powerUpPickupCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] toadstoolPickupSequenceActive = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] instrumentPickupSequenceActive = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentPickupState = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentBackgroundFade = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentObjectFade = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentHintBatches = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentWarpX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentWarpY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentPaletteCycle = new int[32];
    private final int[] instrumentWarpSprites = new int[256];
    private final int[] instrumentHintTiles = new int[20 * 18];
    private final int[] instrumentHintAttributes = new int[20 * 18];
    private final int[] instrumentWarpFadeStep = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentHintCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] instrumentHintOpened = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] instrumentPickupSparklesPending = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentParticleKind = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentParticleSpawnCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentParticleSpawnCounter = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentParticleSpeedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentParticleSpeedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentParticleXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentParticleYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];

    int readSwordPickupState(int slot) {
        return swordPickupState[slot];
    }

    int writeSwordPickupState(int slot, int value) {
        swordPickupState[slot] = value;
        return swordPickupState[slot];
    }

    boolean readSwordPickupSequenceActive(int slot) {
        return swordPickupSequenceActive[slot];
    }

    boolean writeSwordPickupSequenceActive(int slot, boolean value) {
        swordPickupSequenceActive[slot] = value;
        return swordPickupSequenceActive[slot];
    }

    boolean[] storageSwordPickupSequenceActive() {
        return swordPickupSequenceActive;
    }

    int readPowerUpPickupCountdown(int slot) {
        return powerUpPickupCountdown[slot];
    }

    int writePowerUpPickupCountdown(int slot, int value) {
        powerUpPickupCountdown[slot] = value;
        return powerUpPickupCountdown[slot];
    }

    int[] storagePowerUpPickupCountdown() {
        return powerUpPickupCountdown;
    }

    int prePowerUpPickupCountdown(int slot, int delta) {
        return powerUpPickupCountdown[slot] += delta;
    }

    boolean readToadstoolPickupSequenceActive(int slot) {
        return toadstoolPickupSequenceActive[slot];
    }

    boolean writeToadstoolPickupSequenceActive(int slot, boolean value) {
        toadstoolPickupSequenceActive[slot] = value;
        return toadstoolPickupSequenceActive[slot];
    }

    boolean readInstrumentPickupSequenceActive(int slot) {
        return instrumentPickupSequenceActive[slot];
    }

    boolean writeInstrumentPickupSequenceActive(int slot, boolean value) {
        instrumentPickupSequenceActive[slot] = value;
        return instrumentPickupSequenceActive[slot];
    }

    int readInstrumentPickupState(int slot) {
        return instrumentPickupState[slot];
    }

    int writeInstrumentPickupState(int slot, int value) {
        instrumentPickupState[slot] = value;
        return instrumentPickupState[slot];
    }

    int[] storageInstrumentPickupState() {
        return instrumentPickupState;
    }

    int readInstrumentBackgroundFade(int slot) {
        return instrumentBackgroundFade[slot];
    }

    int writeInstrumentBackgroundFade(int slot, int value) {
        instrumentBackgroundFade[slot] = value;
        return instrumentBackgroundFade[slot];
    }

    int readInstrumentObjectFade(int slot) {
        return instrumentObjectFade[slot];
    }

    int writeInstrumentObjectFade(int slot, int value) {
        instrumentObjectFade[slot] = value;
        return instrumentObjectFade[slot];
    }

    int readInstrumentHintBatches(int slot) {
        return instrumentHintBatches[slot];
    }

    int writeInstrumentHintBatches(int slot, int value) {
        instrumentHintBatches[slot] = value;
        return instrumentHintBatches[slot];
    }

    int readInstrumentWarpX(int slot) {
        return instrumentWarpX[slot];
    }

    int writeInstrumentWarpX(int slot, int value) {
        instrumentWarpX[slot] = value;
        return instrumentWarpX[slot];
    }

    int readInstrumentWarpY(int slot) {
        return instrumentWarpY[slot];
    }

    int writeInstrumentWarpY(int slot, int value) {
        instrumentWarpY[slot] = value;
        return instrumentWarpY[slot];
    }

    int readInstrumentPaletteCycle(int slot) {
        return instrumentPaletteCycle[slot];
    }

    int writeInstrumentPaletteCycle(int slot, int value) {
        instrumentPaletteCycle[slot] = value;
        return instrumentPaletteCycle[slot];
    }

    int[] storageInstrumentPaletteCycle() {
        return instrumentPaletteCycle;
    }

    int writeInstrumentWarpSprites(int slot, int value) {
        instrumentWarpSprites[slot] = value;
        return instrumentWarpSprites[slot];
    }

    int[] storageInstrumentWarpSprites() {
        return instrumentWarpSprites;
    }

    int[] storageInstrumentHintTiles() {
        return instrumentHintTiles;
    }

    int[] storageInstrumentHintAttributes() {
        return instrumentHintAttributes;
    }

    int readInstrumentWarpFadeStep(int slot) {
        return instrumentWarpFadeStep[slot];
    }

    int writeInstrumentWarpFadeStep(int slot, int value) {
        instrumentWarpFadeStep[slot] = value;
        return instrumentWarpFadeStep[slot];
    }

    int postInstrumentWarpFadeStep(int slot, int delta) {
        int previous = instrumentWarpFadeStep[slot];
        instrumentWarpFadeStep[slot] += delta;
        return previous;
    }

    int readInstrumentHintCountdown(int slot) {
        return instrumentHintCountdown[slot];
    }

    int writeInstrumentHintCountdown(int slot, int value) {
        instrumentHintCountdown[slot] = value;
        return instrumentHintCountdown[slot];
    }

    int preInstrumentHintCountdown(int slot, int delta) {
        return instrumentHintCountdown[slot] += delta;
    }

    boolean readInstrumentHintOpened(int slot) {
        return instrumentHintOpened[slot];
    }

    boolean writeInstrumentHintOpened(int slot, boolean value) {
        instrumentHintOpened[slot] = value;
        return instrumentHintOpened[slot];
    }

    boolean readInstrumentPickupSparklesPending(int slot) {
        return instrumentPickupSparklesPending[slot];
    }

    boolean writeInstrumentPickupSparklesPending(int slot, boolean value) {
        instrumentPickupSparklesPending[slot] = value;
        return instrumentPickupSparklesPending[slot];
    }

    int readInstrumentParticleKind(int slot) {
        return instrumentParticleKind[slot];
    }

    int writeInstrumentParticleKind(int slot, int value) {
        instrumentParticleKind[slot] = value;
        return instrumentParticleKind[slot];
    }

    int readInstrumentParticleSpawnCountdown(int slot) {
        return instrumentParticleSpawnCountdown[slot];
    }

    int writeInstrumentParticleSpawnCountdown(int slot, int value) {
        instrumentParticleSpawnCountdown[slot] = value;
        return instrumentParticleSpawnCountdown[slot];
    }

    int readInstrumentParticleSpawnCounter(int slot) {
        return instrumentParticleSpawnCounter[slot];
    }

    int writeInstrumentParticleSpawnCounter(int slot, int value) {
        instrumentParticleSpawnCounter[slot] = value;
        return instrumentParticleSpawnCounter[slot];
    }

    int readInstrumentParticleSpeedX(int slot) {
        return instrumentParticleSpeedX[slot];
    }

    int writeInstrumentParticleSpeedX(int slot, int value) {
        instrumentParticleSpeedX[slot] = value;
        return instrumentParticleSpeedX[slot];
    }

    int readInstrumentParticleSpeedY(int slot) {
        return instrumentParticleSpeedY[slot];
    }

    int writeInstrumentParticleSpeedY(int slot, int value) {
        instrumentParticleSpeedY[slot] = value;
        return instrumentParticleSpeedY[slot];
    }

    int writeInstrumentParticleXAccumulator(int slot, int value) {
        instrumentParticleXAccumulator[slot] = value;
        return instrumentParticleXAccumulator[slot];
    }

    int[] storageInstrumentParticleXAccumulator() {
        return instrumentParticleXAccumulator;
    }

    int writeInstrumentParticleYAccumulator(int slot, int value) {
        instrumentParticleYAccumulator[slot] = value;
        return instrumentParticleYAccumulator[slot];
    }

    int[] storageInstrumentParticleYAccumulator() {
        return instrumentParticleYAccumulator;
    }
}
