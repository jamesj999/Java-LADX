package linksawakening.state;

/** ROM Link motion dispatch indices from constants/gameplay.asm. */
public enum LinkMotionState {
    DEFAULT(0),
    SWIMMING(1),
    UNSTUCKING(2),
    MAP_FADE_OUT(3),
    MAP_FADE_IN(4),
    REVOLVING_DOOR(5),
    FALLING_DOWN(6),
    PASS_OUT(7),
    RECOVER(8),
    TELEPORT(9),
    UNKNOWN_0A(10);

    private final int romValue;

    LinkMotionState(int romValue) {
        this.romValue = romValue;
    }

    public int romValue() {
        return romValue;
    }

    public static LinkMotionState fromRom(int value) {
        return switch (value) {
            case 0 -> DEFAULT;
            case 1 -> SWIMMING;
            case 2 -> UNSTUCKING;
            case 3 -> MAP_FADE_OUT;
            case 4 -> MAP_FADE_IN;
            case 5 -> REVOLVING_DOOR;
            case 6 -> FALLING_DOWN;
            case 7 -> PASS_OUT;
            case 8 -> RECOVER;
            case 9 -> TELEPORT;
            case 10 -> UNKNOWN_0A;
            default -> throw new IllegalArgumentException("Unknown LinkMotionState ROM value: " + value);
        };
    }
}
