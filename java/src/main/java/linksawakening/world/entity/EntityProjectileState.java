package linksawakening.world.entity;

/** Shared projectile spawn latches and handler counters; owned by the room, independent of collision dispatch. */
final class EntityProjectileState {
    private final int[] powderSpent = new int[EntityRoomLoader.MAX_ENTITIES];
    private int latestShotArrowEntityIndex = -1;
    private boolean lastArrowShotPlayedWhoosh;
    private final int[] magicPowderState = new int[EntityRoomLoader.MAX_ENTITIES];
    private int torchPaletteTargetAddress;
    private boolean torchPaletteTransitionActive;
    private final int[] musicalNoteInertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] musicalNoteSpeedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] musicalNoteSpeedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private int boomerangSfxCounter;
    private boolean boomerangSoundClockProvided;

    int readPowderSpent(int slot) {
        return powderSpent[slot];
    }

    int writePowderSpent(int slot, int value) {
        powderSpent[slot] = value;
        return powderSpent[slot];
    }

    int readLatestShotArrowEntityIndex() {
        return latestShotArrowEntityIndex;
    }

    int writeLatestShotArrowEntityIndex(int value) {
        latestShotArrowEntityIndex = value;
        return latestShotArrowEntityIndex;
    }

    boolean readLastArrowShotPlayedWhoosh() {
        return lastArrowShotPlayedWhoosh;
    }

    boolean writeLastArrowShotPlayedWhoosh(boolean value) {
        lastArrowShotPlayedWhoosh = value;
        return lastArrowShotPlayedWhoosh;
    }

    int readMagicPowderState(int slot) {
        return magicPowderState[slot];
    }

    int writeMagicPowderState(int slot, int value) {
        magicPowderState[slot] = value;
        return magicPowderState[slot];
    }

    int readTorchPaletteTargetAddress() {
        return torchPaletteTargetAddress;
    }

    int writeTorchPaletteTargetAddress(int value) {
        torchPaletteTargetAddress = value;
        return torchPaletteTargetAddress;
    }

    boolean readTorchPaletteTransitionActive() {
        return torchPaletteTransitionActive;
    }

    boolean writeTorchPaletteTransitionActive(boolean value) {
        torchPaletteTransitionActive = value;
        return torchPaletteTransitionActive;
    }

    int readMusicalNoteInertia(int slot) {
        return musicalNoteInertia[slot];
    }

    int writeMusicalNoteInertia(int slot, int value) {
        musicalNoteInertia[slot] = value;
        return musicalNoteInertia[slot];
    }

    int readMusicalNoteSpeedX(int slot) {
        return musicalNoteSpeedX[slot];
    }

    int writeMusicalNoteSpeedX(int slot, int value) {
        musicalNoteSpeedX[slot] = value;
        return musicalNoteSpeedX[slot];
    }

    int readMusicalNoteSpeedY(int slot) {
        return musicalNoteSpeedY[slot];
    }

    int writeMusicalNoteSpeedY(int slot, int value) {
        musicalNoteSpeedY[slot] = value;
        return musicalNoteSpeedY[slot];
    }

    int readBoomerangSfxCounter() {
        return boomerangSfxCounter;
    }

    int writeBoomerangSfxCounter(int value) {
        boomerangSfxCounter = value;
        return boomerangSfxCounter;
    }

    boolean readBoomerangSoundClockProvided() {
        return boomerangSoundClockProvided;
    }

    boolean writeBoomerangSoundClockProvided(boolean value) {
        boomerangSoundClockProvided = value;
        return boomerangSoundClockProvided;
    }
}
