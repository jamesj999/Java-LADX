package linksawakening.state;

/** Individual flags in wOcarinaSongFlags; combinations remain a bit mask. */
public enum OcarinaSong {
    FROGS_SONG_OF_THE_SOUL(1),
    MANBO_MAMBO(2),
    BALLAD_OF_THE_WIND_FISH(4);

    private final int romValue;

    OcarinaSong(int romValue) {
        this.romValue = romValue;
    }

    public int romValue() {
        return romValue;
    }

    public static OcarinaSong fromRom(int value) {
        return switch (value) {
            case 1 -> FROGS_SONG_OF_THE_SOUL;
            case 2 -> MANBO_MAMBO;
            case 4 -> BALLAD_OF_THE_WIND_FISH;
            default -> throw new IllegalArgumentException("Unknown OcarinaSong ROM value: " + value);
        };
    }

    /** Tests this song bit without altering other flag bits. */
    public boolean isSet(int flags) {
        return (flags & romValue) != 0;
    }
}
