package linksawakening.world.entity;

/** Warp slot state exists even without ROM-backed warp lookup tables. */
final class EntityWarpController {
    private final int[] warpState = new int[EntityRoomLoader.MAX_ENTITIES];

    int readWarpState(int slot) {
        return warpState[slot];
    }

    int writeWarpState(int slot, int value) {
        return warpState[slot] = value;
    }

    private final int[] warpTransitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];

    int readWarpTransitionCountdown(int slot) {
        return warpTransitionCountdown[slot];
    }

    int writeWarpTransitionCountdown(int slot, int value) {
        return warpTransitionCountdown[slot] = value;
    }
}
