package linksawakening.state;

/** Aggregate of inventory, resources, progression and equipment state. */
public final class PlayerState extends PlayerLifecycleCompatibility implements LinkPlayerState {

    /** @deprecated Use {@link InventoryItem#EMPTY}. */
    @Deprecated(forRemoval = true)
    public static final int INVENTORY_EMPTY = 0x00;
    /** @deprecated Use {@link InventoryItem#SWORD}. */
    @Deprecated(forRemoval = true)
    public static final int INVENTORY_SWORD = 0x01;
    /** @deprecated Use {@link InventoryItem#BOMBS}. */
    @Deprecated(forRemoval = true)
    public static final int INVENTORY_BOMBS = 0x02;
    /** @deprecated Use {@link InventoryItem#POWER_BRACELET}. */
    @Deprecated(forRemoval = true)
    public static final int INVENTORY_POWER_BRACELET = 0x03;
    /** @deprecated Use {@link InventoryItem#SHIELD}. */
    @Deprecated(forRemoval = true)
    public static final int INVENTORY_SHIELD = 0x04;
    /** @deprecated Use {@link InventoryItem#BOW}. */
    @Deprecated(forRemoval = true)
    public static final int INVENTORY_BOW = 0x05;
    /** @deprecated Use {@link InventoryItem#HOOKSHOT}. */
    @Deprecated(forRemoval = true)
    public static final int INVENTORY_HOOKSHOT = 0x06;
    /** @deprecated Use {@link InventoryItem#MAGIC_ROD}. */
    @Deprecated(forRemoval = true)
    public static final int INVENTORY_MAGIC_ROD = 0x07;
    /** @deprecated Use {@link InventoryItem#PEGASUS_BOOTS}. */
    @Deprecated(forRemoval = true)
    public static final int INVENTORY_PEGASUS_BOOTS = 0x08;
    /** @deprecated Use {@link InventoryItem#OCARINA}. */
    @Deprecated(forRemoval = true)
    public static final int INVENTORY_OCARINA = 0x09;
    /** @deprecated Use {@link InventoryItem#ROCS_FEATHER}. */
    @Deprecated(forRemoval = true)
    public static final int INVENTORY_ROCS_FEATHER = 0x0A;
    /** @deprecated Use {@link InventoryItem#SHOVEL}. */
    @Deprecated(forRemoval = true)
    public static final int INVENTORY_SHOVEL = 0x0B;
    /** @deprecated Use {@link InventoryItem#MAGIC_POWDER}. */
    @Deprecated(forRemoval = true)
    public static final int INVENTORY_MAGIC_POWDER = 0x0C;
    /** @deprecated Use {@link InventoryItem#BOOMERANG}. */
    @Deprecated(forRemoval = true)
    public static final int INVENTORY_BOOMERANG = 0x0D;

    // Values stored in the ROM's wOcarinaSongFlags byte.
    /** @deprecated Use {@link OcarinaSong#FROGS_SONG_OF_THE_SOUL}. */
    @Deprecated(forRemoval = true)
    public static final int FROGS_SONG_OF_THE_SOUL_FLAG = 0x01;
    /** @deprecated Use {@link OcarinaSong#MANBO_MAMBO}. */
    @Deprecated(forRemoval = true)
    public static final int MANBO_MAMBO_FLAG = 0x02;
    /** @deprecated Use {@link OcarinaSong#BALLAD_OF_THE_WIND_FISH}. */
    @Deprecated(forRemoval = true)
    public static final int BALLAD_OF_THE_WIND_FISH_FLAG = 0x04;

    // Values written to the ROM's wTunicType ($DC0F).
    /** @deprecated Use {@link Tunic#GREEN}. */
    @Deprecated(forRemoval = true)
    public static final int TUNIC_GREEN = 0x00;
    /** @deprecated Use {@link Tunic#RED}. */
    @Deprecated(forRemoval = true)
    public static final int TUNIC_RED = 0x01;
    /** @deprecated Use {@link Tunic#BLUE}. */
    @Deprecated(forRemoval = true)
    public static final int TUNIC_BLUE = 0x02;

    /** @deprecated Use {@link PowerUp#NONE}. */
    @Deprecated(forRemoval = true)
    public static final int ACTIVE_POWER_UP_NONE = 0x00;
    /** @deprecated Use {@link PowerUp#PIECE_OF_POWER}. */
    @Deprecated(forRemoval = true)
    public static final int ACTIVE_POWER_UP_PIECE_OF_POWER = 0x01;
    /** @deprecated Use {@link PowerUp#GUARDIAN_ACORN}. */
    @Deprecated(forRemoval = true)
    public static final int ACTIVE_POWER_UP_GUARDIAN_ACORN = 0x02;

    public static final int MAX_RUPEES = 999;
    public static final int MAX_HEARTS = 14;
    public static final int HP_PER_HEART = 8;

    // Matches the disassembly's wInventoryItems.subscreen (DB02-DB0B): 10
    // persistent inventory slots visible as a 2-col x 5-row grid under the
    // A/B status bar. When the player equips an item, it swaps between the
    // A/B slot and the selected subscreen slot (see bank20.asm:5FDB).
    public static final int SUBSCREEN_SLOT_COUNT = 10;

    public PlayerState() {
        super();
    }
}
