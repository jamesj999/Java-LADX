package linksawakening.rom;

import java.util.Arrays;
import java.util.Objects;

/** Read-only view of caller-owned ROM bytes; callers must not mutate the backing array. */
public final class Rom {
    private final byte[] data;

    public Rom(byte[] data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    public int u8(RomAddress address) {
        return Byte.toUnsignedInt(data[address.offset()]);
    }

    public int u16le(RomAddress address) {
        int offset = address.offset();
        Objects.checkFromIndexSize(offset, 2, data.length);
        return Byte.toUnsignedInt(data[offset]) | (Byte.toUnsignedInt(data[offset + 1]) << 8);
    }

    public byte[] slice(RomAddress address, int length) {
        int offset = address.offset();
        Objects.checkFromIndexSize(offset, length, data.length);
        return Arrays.copyOfRange(data, offset, offset + length);
    }
}
