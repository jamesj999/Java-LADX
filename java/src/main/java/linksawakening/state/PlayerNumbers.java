package linksawakening.state;

/** Byte and capacity arithmetic shared by player components. */
final class PlayerNumbers {
    private PlayerNumbers() {}

    static int incrementUpTo(int value, int maximum) {
        return value < maximum ? value + 1 : value;
    }

    static int incrementByTenUpTo(int value, int maximum) {
        return Math.min(maximum, value + 10);
    }

    static int incrementByte(int value) {
        return (value + 1) & 0xFF;
    }

    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
