package linksawakening.gpu;

/** Mirrors bank0 AdjustBankNumberForGBC for explicitly adjusted uploads. */
final class GbcBanks {
    private GbcBanks() {}
    static int adjust(int bank) { return bank | 0x20; }
}
