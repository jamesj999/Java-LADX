package linksawakening.world;

import java.util.Arrays;

/** RGB555 palette uploads for room transitions and the slower four-group Manbo effect. */
public final class RoomPaletteFade {
    private RoomPaletteFade() {}

    static int[][] palettes(TransitionController transition, int[][] source, boolean objects) {
        int[][] result = new int[source.length][];
        for (int row = 0; row < source.length; row++) {
            int group = (objects ? 2 : 0) + row / 4;
            int step = uploadedStep(transition, group);
            result[row] = new int[source[row].length];
            for (int c = 0; c < source[row].length; c++) {
                int rgb = source[row][c];
                result[row][c] = component(rgb >> 16, step) << 16
                    | component(rgb >> 8, step) << 8 | component(rgb, step);
            }
        }
        return result;
    }

    /** Positive steps brighten the departing room. Negative steps set the floor during arrival. */
    static int uploadedStep(TransitionController transition, int group) {
        int frame = transition.transitionFrame();
        switch (transition.state()) {
            case IDLE: return 0;
            case ROOM_LOAD, MANBO_ROOM_LOAD: return -31;
            case FADING_OUT, FADING_IN:
                // Ordinary map fades upload all BG palettes on odd ticks, all OBJ on even ticks.
                int writes = (frame + (group < 2 ? 1 : 0)) / 2;
                int amount = Math.min(31, 4 * writes);
                return transition.state() == TransitionController.State.FADING_OUT ? amount : amount - 31;
            default: break;
        }
        int writes = Math.clamp(transition.transitionFrame() - 0x60, 0, 32);
        int lastWrite = writes <= group ? 0 : writes - (writes - group - 1) % 4;
        if (transition.state() == TransitionController.State.MANBO_IN) return Math.min(lastWrite, 31);
        return -Math.max(0, 31 - ((lastWrite + 3) / 4) * 8);
    }

    private static int component(int rgb, int step) {
        if (step == 0) return rgb & 255;
        int base = (rgb & 255) >> 3;
        int faded = step > 0 ? Math.min(31, base + step) : Math.max(base, -step);
        return faded * 255 / 31;
    }

    public static void compositeObjects(byte[] room, byte[] objects, TransitionController transition) {
        int step = uploadedStep(transition, 2);
        for (int i = 0; i < room.length; i += 4) {
            if (objects[i + 3] == 0) continue;
            for (int c = i; c < i + 3; c++) room[c] = (byte) component(objects[c], step);
            room[i + 3] = (byte) 255;
        }
        Arrays.fill(objects, (byte) 0);
    }
}
