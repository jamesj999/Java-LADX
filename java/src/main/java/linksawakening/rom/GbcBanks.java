package linksawakening.rom;

/** Mirrors {@code AdjustBankNumberForGBC} in disassembly {@code src/code/bank0.asm}. */
public final class GbcBanks {
    private GbcBanks() {}
    public static int adjust(int bank) { return bank | 0x20; }
}
