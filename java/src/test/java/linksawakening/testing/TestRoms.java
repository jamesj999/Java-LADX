package linksawakening.testing;

import linksawakening.rom.Rom;

/** ROM-free synthetic buffers whose length need not fill a complete ROM bank. */
public final class TestRoms {
    private TestRoms() {}

    /** Retains the supplied bytes, including deliberately short buffers used to test read bounds. */
    public static Rom partial(byte... data) {
        return Rom.unchecked(data);
    }
}
