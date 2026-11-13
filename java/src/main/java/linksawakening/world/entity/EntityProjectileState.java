package linksawakening.world.entity;

/** Shared projectile spawn latches and handler counters; owned by the room, independent of collision dispatch. */
final class EntityProjectileState {
    private final int[] magicPowderPrivateState4 = new int[EntityRoomLoader.MAX_ENTITIES];
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

    int readMagicPowderPrivateState4(int slot) {
        return magicPowderPrivateState4[slot];
    }

    int writeMagicPowderPrivateState4(int slot, int value) {
        return magicPowderPrivateState4[slot] = value;
    }

    int readLatestShotArrowEntityIndex() {
        return latestShotArrowEntityIndex;
    }

    int writeLatestShotArrowEntityIndex(int value) {
        return latestShotArrowEntityIndex = value;
    }

    boolean readLastArrowShotPlayedWhoosh() {
        return lastArrowShotPlayedWhoosh;
    }

    boolean writeLastArrowShotPlayedWhoosh(boolean value) {
        return lastArrowShotPlayedWhoosh = value;
    }

    int readMagicPowderState(int slot) {
        return magicPowderState[slot];
    }

    int writeMagicPowderState(int slot, int value) {
        return magicPowderState[slot] = value;
    }

    int readTorchPaletteTargetAddress() {
        return torchPaletteTargetAddress;
    }

    int writeTorchPaletteTargetAddress(int value) {
        return torchPaletteTargetAddress = value;
    }

    boolean readTorchPaletteTransitionActive() {
        return torchPaletteTransitionActive;
    }

    boolean writeTorchPaletteTransitionActive(boolean value) {
        return torchPaletteTransitionActive = value;
    }

    int readMusicalNoteInertia(int slot) {
        return musicalNoteInertia[slot];
    }

    int writeMusicalNoteInertia(int slot, int value) {
        return musicalNoteInertia[slot] = value;
    }

    int readMusicalNoteSpeedX(int slot) {
        return musicalNoteSpeedX[slot];
    }

    int writeMusicalNoteSpeedX(int slot, int value) {
        return musicalNoteSpeedX[slot] = value;
    }

    int readMusicalNoteSpeedY(int slot) {
        return musicalNoteSpeedY[slot];
    }

    int writeMusicalNoteSpeedY(int slot, int value) {
        return musicalNoteSpeedY[slot] = value;
    }

    int readBoomerangSfxCounter() {
        return boomerangSfxCounter;
    }

    int writeBoomerangSfxCounter(int value) {
        return boomerangSfxCounter = value;
    }

    boolean readBoomerangSoundClockProvided() {
        return boomerangSoundClockProvided;
    }

    boolean writeBoomerangSoundClockProvided(boolean value) {
        return boomerangSoundClockProvided = value;
    }
}
