package linksawakening.rom;

/** A CPU-visible ROM address paired with its ROM bank. */
public record RomAddress(int bank, int address) {
    public RomAddress {
        if (bank < 0 || address < (bank == 0 ? 0 : 0x4000)
                || address >= (bank == 0 ? 0x4000 : 0x8000)) {
            throw new IllegalArgumentException("Invalid ROM bank/address");
        }
    }

    public int offset() {
        return RomBank.romOffset(bank, address);
    }

    /** Explicit bank override for AdjustBankNumberForGBC and similar paths. */
    public RomAddress inBank(int replacement) {
        return new RomAddress(replacement, address);
    }
}
