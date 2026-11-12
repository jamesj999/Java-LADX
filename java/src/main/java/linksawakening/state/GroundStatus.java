package linksawakening.state;

/** ROM Link ground status values from constants/gameplay.asm. */
public enum GroundStatus {
    NORMAL(0),
    STEPS(1),
    SLOW(3),
    PIT(7);

    private final int romValue;

    GroundStatus(int romValue) {
        this.romValue = romValue;
    }

    public int romValue() {
        return romValue;
    }

    public static GroundStatus fromRom(int value) {
        return switch (value) {
            case 0 -> NORMAL;
            case 1 -> STEPS;
            case 3 -> SLOW;
            case 7 -> PIT;
            default -> throw new IllegalArgumentException("Unknown GroundStatus ROM value: " + value);
        };
    }
}
