package linksawakening.rom;

/** A CPU-visible ROM address paired with its ROM bank. */
public record RomAddress(int bank, int address) {
    public RomAddress {
        if (bank < 0
                || bank > Integer.MAX_VALUE / RomBank.BANK_SIZE
                || address < (bank == 0 ? 0 : 0x4000)
                || address >= (bank == 0 ? 0x4000 : 0x8000)) {
            throw new IllegalArgumentException("Invalid ROM bank/address");
        }
    }

    public static RomAddress fromOffset(int offset) {
        if (offset < 0) throw new IllegalArgumentException("Negative ROM offset");
        return offset < 0x4000 ? new RomAddress(0, offset) : new RomAddress(offset / 0x4000, 0x4000 + offset % 0x4000);
    }

    /** Advance within this CPU bank window; use fromOffset for explicit file-order traversal. */
    public RomAddress plus(int bytes) {
        int next = Math.addExact(address, bytes);
        return new RomAddress(bank, next);
    }

    /** Mirrors AdjustBankNumberForGBC; callers opt in only on adjusted load paths. */
    public RomAddress withGbcBank() {
        return inBank(GbcBanks.adjust(bank));
    }

    public int offset() {
        return RomBank.romOffset(bank, address);
    }

    /** Explicit bank override for AdjustBankNumberForGBC and similar paths. */
    public RomAddress inBank(int replacement) {
        return new RomAddress(replacement, address);
    }
}
