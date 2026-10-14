package linksawakening.world;

/** Final arrow/fireball speeds from {@code label_140F}, after generic spawning. */
final class PlayerProjectileLaunchSpeed {
    // ROM $13AD/$13B5: the branch at $1414 adds four ONLY with Piece of Power.
    private static final int[] SPEED_X = {0x30, 0xD0, 0, 0, 0x40, 0xC0, 0, 0};
    private static final int[] SPEED_Y = {0, 0, 0xD0, 0x30, 0, 0, 0xC0, 0x40};

    private PlayerProjectileLaunchSpeed() {
    }

    static int x(int direction, boolean pieceOfPower) {
        return SPEED_X[direction + (pieceOfPower ? 4 : 0)];
    }

    static int y(int direction, boolean pieceOfPower) {
        return SPEED_Y[direction + (pieceOfPower ? 4 : 0)];
    }
}
