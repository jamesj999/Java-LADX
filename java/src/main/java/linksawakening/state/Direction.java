package linksawakening.state;

/** Cardinal facing with explicit engine and ROM table indices. */
public enum Direction {
    DOWN(0, 3),
    UP(1, 2),
    LEFT(2, 1),
    RIGHT(3, 0),
    /** Classification only; never a valid facing or ROM direction. */
    UNKNOWN(-1, -1);

    private final int javaIndex;
    private final int romIndex;

    Direction(int javaIndex, int romIndex) {
        this.javaIndex = javaIndex;
        this.romIndex = romIndex;
    }

    public int javaIndex() {
        return javaIndex;
    }

    public int romIndex() {
        return romIndex;
    }

    public int romValue() {
        return romIndex;
    }

    /** Sentinel classification for legacy switches whose default accepts arbitrary integers. */
    public static Direction fromJavaOrUnknown(int value) {
        return value >= 0 && value <= 3 ? fromJava(value) : UNKNOWN;
    }

    /** Unknown ROM indices classify as UNKNOWN without masking. */
    public static Direction fromRomOrUnknown(int value) {
        return value >= 0 && value <= 3 ? fromRom(value) : UNKNOWN;
    }

    public static Direction fromJava(int value) {
        return switch (value) {
            case 0 -> DOWN;
            case 1 -> UP;
            case 2 -> LEFT;
            case 3 -> RIGHT;
            default -> throw new IllegalArgumentException("Link direction out of range: " + value);
        };
    }

    /** Strict ROM conversion, retaining the existing Link assertion on invalid indices. */
    public static Direction fromRom(int value) {
        return switch (value) {
            case 0 -> RIGHT;
            case 1 -> LEFT;
            case 2 -> UP;
            case 3 -> DOWN;
            default -> throw new AssertionError(value);
        };
    }

    /** Legacy sword presentation treats unrecognized engine directions as down. */
    public static Direction fromJavaOrDown(int value) {
        return value >= 0 && value <= 3 ? fromJava(value) : DOWN;
    }
}
