package linksawakening.state;

/** Named inventory bytes; UNKNOWN classifies arbitrary saved bytes without replacing raw storage. */
public enum InventoryItem {
    EMPTY(0),
    SWORD(1),
    BOMBS(2),
    POWER_BRACELET(3),
    SHIELD(4),
    BOW(5),
    HOOKSHOT(6),
    MAGIC_ROD(7),
    PEGASUS_BOOTS(8),
    OCARINA(9),
    ROCS_FEATHER(10),
    SHOVEL(11),
    MAGIC_POWDER(12),
    BOOMERANG(13),
    UNKNOWN(-1);

    private final int romValue;

    InventoryItem(int romValue) {
        this.romValue = romValue;
    }

    public int romValue() {
        return romValue;
    }

    /** Unknown bytes map to UNKNOWN; callers retain the original byte for persistence. */
    public static InventoryItem fromRom(int value) {
        return switch (value) {
            case 0 -> EMPTY;
            case 1 -> SWORD;
            case 2 -> BOMBS;
            case 3 -> POWER_BRACELET;
            case 4 -> SHIELD;
            case 5 -> BOW;
            case 6 -> HOOKSHOT;
            case 7 -> MAGIC_ROD;
            case 8 -> PEGASUS_BOOTS;
            case 9 -> OCARINA;
            case 10 -> ROCS_FEATHER;
            case 11 -> SHOVEL;
            case 12 -> MAGIC_POWDER;
            case 13 -> BOOMERANG;
            default -> UNKNOWN;
        };
    }
}
