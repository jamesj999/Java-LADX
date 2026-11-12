package linksawakening.state;

/** ROM tunic selection. */
public enum Tunic {
    GREEN(0),
    RED(1),
    BLUE(2);

    private final int romValue;

    Tunic(int romValue) {
        this.romValue = romValue;
    }

    public int romValue() {
        return romValue;
    }

    public static Tunic fromRom(int value) {
        return switch (value) {
            case 0 -> GREEN;
            case 1 -> RED;
            case 2 -> BLUE;
            default -> throw new IllegalArgumentException("Unknown Tunic ROM value: " + value);
        };
    }
}
