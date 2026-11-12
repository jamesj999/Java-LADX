package linksawakening.rom;

/** Zero-copy read-only bounded view. The owner must not mutate the ROM backing array. */
public final class RomSlice {
    private final Rom rom;
    private final RomAddress start;
    private final int length;

    RomSlice(Rom rom, RomAddress start, int length) {
        this.rom = rom;
        this.start = start;
        this.length = length;
    }

    public int size() {
        return length;
    }

    private RomAddress checked(int index, int count) {
        if (index < 0 || count < 0 || index > length || count > length - index) {
            throw new IndexOutOfBoundsException(String.format(
                    "ROM view %02X:%04X length %d: index %d length %d",
                    start.bank(), start.address(), length, index, count));
        }
        return start.plus(index);
    }

    public int u8(int index) {
        return rom.u8(checked(index, 1));
    }

    public int s8(int index) {
        return rom.s8(checked(index, 1));
    }

    public int u16le(int index) {
        return rom.u16le(checked(index, 2));
    }

    public byte[] bytes(int index, int count) {
        return rom.bytes(checked(index, count), count);
    }

    public RomSlice view(int index, int count) {
        return rom.view(checked(index, count), count);
    }

    public RomCursor cursor() {
        return new RomCursor(this);
    }

    RomAddress address(int index) {
        return checked(index, 0);
    }
}
