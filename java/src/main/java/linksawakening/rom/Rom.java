package linksawakening.rom;

import java.util.Arrays;
import java.util.Objects;

/** Read-only view of caller-owned ROM bytes; callers must not mutate the backing array. */
public final class Rom {
    private final byte[] data;

    private Rom(byte[] data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    /**
     * Wraps caller-owned bytes without bank-alignment validation, for partial buffers and
     * legacy loaders. Null validation and all read bounds checks still apply; no copy is made.
     * Prefer {@link #of(byte[])} for complete ROMs.
     */
    public static Rom unchecked(byte[] data) {
        return new Rom(data);
    }

    public static Rom of(byte[] data) {
        Objects.requireNonNull(data, "data");
        if (data.length % RomBank.BANK_SIZE != 0) {
            throw new IllegalArgumentException("ROM length must be a multiple of 0x4000: " + data.length);
        }
        return new Rom(data);
    }

    public int size() {
        return data.length;
    }

    /** Temporary migration escape hatch. Returns caller-owned mutable storage; do not mutate it. */
    public byte[] unsafeBytes() {
        return data;
    }

    int checkedOffset(RomAddress address, int length) {
        Objects.requireNonNull(address, "address");
        int offset = address.offset();
        if (length < 0 || offset > data.length || length > data.length - offset) {
            throw new IndexOutOfBoundsException(String.format(
                    "ROM %02X:%04X length %d exceeds ROM length %d",
                    address.bank(), address.address(), length, data.length));
        }
        return offset;
    }

    public int u8(RomAddress address) {
        return Byte.toUnsignedInt(data[checkedOffset(address, 1)]);
    }

    public int s8(RomAddress address) {
        return data[checkedOffset(address, 1)];
    }

    public int u16le(RomAddress address) {
        int offset = checkedOffset(address, 2);
        return Byte.toUnsignedInt(data[offset]) | (Byte.toUnsignedInt(data[offset + 1]) << 8);
    }

    /** Copies a validated flat ROM span directly into caller-owned storage without allocation. */
    public void copyInto(int offset, byte[] destination, int destinationOffset, int length) {
        Objects.checkFromIndexSize(offset, length, data.length);
        Objects.checkFromIndexSize(destinationOffset, length, destination.length);
        System.arraycopy(data, offset, destination, destinationOffset, length);
    }

    public byte[] bytes(RomAddress address, int length) {
        int offset = checkedOffset(address, length);
        return Arrays.copyOfRange(data, offset, offset + length);
    }

    public RomSlice view(RomAddress address, int length) {
        checkedOffset(address, length);
        return new RomSlice(this, address, length);
    }

    public int[] u8Table(RomAddress address, int length) {
        int offset = checkedOffset(address, length);
        int[] result = new int[length];
        for (int i = 0; i < length; i++) result[i] = Byte.toUnsignedInt(data[offset + i]);
        return result;
    }

    public int[] s8Table(RomAddress address, int length) {
        int offset = checkedOffset(address, length);
        int[] result = new int[length];
        for (int i = 0; i < length; i++) result[i] = data[offset + i];
        return result;
    }
    /** Decode a CPU pointer with an explicitly selected switchable bank. ROM0 pointers stay in bank zero. */
    public RomAddress pointer(RomAddress address, int bank) {
        if (bank < 0) throw new IllegalArgumentException("Negative pointer bank: " + bank);
        int value = u16le(address);
        return new RomAddress(value < RomBank.BANK_SIZE ? 0 : bank, value);
    }

    public RomAddress[] pointerTable(RomAddress address, int count, int bank) {
        if (count < 0 || count > Integer.MAX_VALUE / 2)
            throw new IllegalArgumentException("Invalid pointer count: " + count);
        checkedOffset(address, count * 2);
        if (bank < 0) throw new IllegalArgumentException("Negative pointer bank: " + bank);
        RomAddress[] result = new RomAddress[count];
        for (int i = 0; i < count; i++) result[i] = pointer(RomAddress.fromOffset(address.offset() + i * 2), bank);
        return result;
    }

    public RomCursor cursor(RomAddress address) {
        return new RomCursor(view(address, size() - checkedOffset(address, 0)));
    }
}
