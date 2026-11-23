package linksawakening.world.entity;

/** Warp slot state exists even without ROM-backed warp lookup tables. */
final class EntityWarpController {
    private final int[] warpState = new int[EntityRoomLoader.MAX_ENTITIES];

    int readWarpState(int slot) {
        return warpState[slot];
    }

    int writeWarpState(int slot, int value) {
        warpState[slot] = value;
        return warpState[slot];
    }

    private final int[] warpTransitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];

    int readWarpTransitionCountdown(int slot) {
        return warpTransitionCountdown[slot];
    }

    int writeWarpTransitionCountdown(int slot, int value) {
        warpTransitionCountdown[slot] = value;
        return warpTransitionCountdown[slot];
    }
}
