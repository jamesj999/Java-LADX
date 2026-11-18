package linksawakening.rom;

/** Forward-only stream reader. Failed reads and skips leave the position unchanged. */
public final class RomCursor {
    private final RomSlice view;
    private int index;

    RomCursor(RomSlice view) {
        this.view = view;
    }

    public RomAddress position() {
        return view.address(index);
    }

    public int u8() {
        int value = view.u8(index);
        index++;
        return value;
    }

    public int s8() {
        int value = view.s8(index);
        index++;
        return value;
    }

    public int u16le() {
        int value = view.u16le(index);
        index += 2;
        return value;
    }

    public void skip(int count) {
        if (count < 0 || count > view.size() - index)
            throw new IndexOutOfBoundsException("ROM cursor " + position() + " skip length " + count);
        index += count;
    }
}
