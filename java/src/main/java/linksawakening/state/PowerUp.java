package linksawakening.state;

/** Temporary ROM power-up selection. */
public enum PowerUp {
    NONE(0),
    PIECE_OF_POWER(1),
    GUARDIAN_ACORN(2);

    private final int romValue;

    PowerUp(int romValue) {
        this.romValue = romValue;
    }

    public int romValue() {
        return romValue;
    }

    public static PowerUp fromRom(int value) {
        return switch (value) {
            case 0 -> NONE;
            case 1 -> PIECE_OF_POWER;
            case 2 -> GUARDIAN_ACORN;
            default -> throw new IllegalArgumentException("Unknown PowerUp ROM value: " + value);
        };
    }
}
