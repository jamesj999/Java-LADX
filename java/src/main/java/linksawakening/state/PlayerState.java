package linksawakening.state;

/** Aggregate of inventory, resources, progression and equipment state. */
public final class PlayerState {

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

    private final Inventory inventory = new Inventory();
    private final Progression progression = new Progression();
    private final EquipmentLevels equipmentLevels = new EquipmentLevels();
    private final PlayerSounds playerSounds = new PlayerSounds();
    private final Ammunition ammunition = new Ammunition(inventory);
    private final Vitals vitals = new Vitals(equipmentLevels, playerSounds, progression);
    private final Wallet wallet = new Wallet(playerSounds);
    private final PlayerResources playerResources = new PlayerResources(playerSounds, vitals, wallet);
    private final PlayerRewards playerRewards =
            new PlayerRewards(ammunition, equipmentLevels, inventory, progression, vitals, wallet);
    private final ChestRewards chestRewards =
            new ChestRewards(ammunition, equipmentLevels, inventory, progression, wallet);
    private final PlayerInitialization playerInitialization =
            new PlayerInitialization(ammunition, equipmentLevels, inventory, playerSounds, progression, vitals, wallet);
    private final PlayerSaveLoader playerSaveLoader =
            new PlayerSaveLoader(ammunition, equipmentLevels, inventory, playerSounds, progression, vitals, wallet);

    public Inventory inventory() {
        return inventory;
    }

    public Ammunition ammunition() {
        return ammunition;
    }

    public Progression progression() {
        return progression;
    }

    public EquipmentLevels equipmentLevels() {
        return equipmentLevels;
    }

    public Vitals vitals() {
        return vitals;
    }

    public Wallet wallet() {
        return wallet;
    }

    public PlayerSounds playerSounds() {
        return playerSounds;
    }

    public PlayerResources playerResources() {
        return playerResources;
    }

    public PlayerRewards playerRewards() {
        return playerRewards;
    }

    public ChestRewards chestRewards() {
        return chestRewards;
    }

    public PlayerInitialization playerInitialization() {
        return playerInitialization;
    }

    public PlayerSaveLoader playerSaveLoader() {
        return playerSaveLoader;
    }
}
