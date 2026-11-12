package linksawakening.world.entity;

import java.util.function.IntUnaryOperator;

/** Slot state owned by the owl story controller. */
final class OwlEventState {
    private final int[] owlEventState = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] owlPrivateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] owlSpeedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] owlSpeedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] owlSpeedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] owlXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] owlYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] owlZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] owlBoomerangSfxCounter = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] owlPreviousMusicTrack = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] owlCompletionEmitted = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private IntUnaryOperator owlDialogResolver = roomId -> 0x0D9;
    private int owlStatueDialogId = -1;
    private IntUnaryOperator owlDefaultMusicResolver = roomId -> 0x05;

    int readOwlEventState(int slot) {
        return owlEventState[slot];
    }

    int writeOwlEventState(int slot, int value) {
        return owlEventState[slot] = value;
    }

    int readOwlPrivateCountdown1(int slot) {
        return owlPrivateCountdown1[slot];
    }

    int writeOwlPrivateCountdown1(int slot, int value) {
        return owlPrivateCountdown1[slot] = value;
    }

    int postOwlPrivateCountdown1(int slot, int delta) {
        int previous = owlPrivateCountdown1[slot];
        owlPrivateCountdown1[slot] += delta;
        return previous;
    }

    int readOwlSpeedX(int slot) {
        return owlSpeedX[slot];
    }

    int writeOwlSpeedX(int slot, int value) {
        return owlSpeedX[slot] = value;
    }

    int readOwlSpeedY(int slot) {
        return owlSpeedY[slot];
    }

    int writeOwlSpeedY(int slot, int value) {
        return owlSpeedY[slot] = value;
    }

    int readOwlSpeedZ(int slot) {
        return owlSpeedZ[slot];
    }

    int writeOwlSpeedZ(int slot, int value) {
        return owlSpeedZ[slot] = value;
    }

    int[] storageOwlXAccumulator() {
        return owlXAccumulator;
    }

    int[] storageOwlYAccumulator() {
        return owlYAccumulator;
    }

    int[] storageOwlZAccumulator() {
        return owlZAccumulator;
    }

    int readOwlBoomerangSfxCounter(int slot) {
        return owlBoomerangSfxCounter[slot];
    }

    int writeOwlBoomerangSfxCounter(int slot, int value) {
        return owlBoomerangSfxCounter[slot] = value;
    }

    int postOwlBoomerangSfxCounter(int slot, int delta) {
        int previous = owlBoomerangSfxCounter[slot];
        owlBoomerangSfxCounter[slot] += delta;
        return previous;
    }

    int readOwlPreviousMusicTrack(int slot) {
        return owlPreviousMusicTrack[slot];
    }

    int writeOwlPreviousMusicTrack(int slot, int value) {
        return owlPreviousMusicTrack[slot] = value;
    }

    boolean readOwlCompletionEmitted(int slot) {
        return owlCompletionEmitted[slot];
    }

    boolean writeOwlCompletionEmitted(int slot, boolean value) {
        return owlCompletionEmitted[slot] = value;
    }

    IntUnaryOperator readOwlDialogResolver() {
        return owlDialogResolver;
    }

    IntUnaryOperator writeOwlDialogResolver(IntUnaryOperator value) {
        return owlDialogResolver = value;
    }

    int readOwlStatueDialogId() {
        return owlStatueDialogId;
    }

    int writeOwlStatueDialogId(int value) {
        return owlStatueDialogId = value;
    }

    IntUnaryOperator readOwlDefaultMusicResolver() {
        return owlDefaultMusicResolver;
    }

    IntUnaryOperator writeOwlDefaultMusicResolver(IntUnaryOperator value) {
        return owlDefaultMusicResolver = value;
    }
}
