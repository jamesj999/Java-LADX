package linksawakening.state;

import linksawakening.save.SaveSlotState;

/** Aggregate of inventory, resources, progression and equipment state. */
public final class PlayerState implements LinkPlayerState {
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

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public void latchFullHearts() {
        vitals.latchFullHearts();
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public boolean fullHearts() {
        return vitals.fullHearts();
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public boolean isOnLowHealth() {
        return vitals.isOnLowHealth();
    }

    /** @deprecated Use {@link #playerSounds()}. */
    @Deprecated(forRemoval = true)
    public linksawakening.gameplay.GameplaySoundEvent takeResourceSound() {
        return playerSounds.takeResourceSound();
    }

    /** @deprecated Use {@link #playerInitialization()}. */
    @Deprecated(forRemoval = true)
    public void initializeNewGame(int maxArrows, int maxBombs, int maxMagicPowder) {
        playerInitialization.initializeNewGame(maxArrows, maxBombs, maxMagicPowder);
    }

    /** @deprecated Use {@link #playerInitialization()}. */
    @Deprecated(forRemoval = true)
    public void initializeDebugState() {
        playerInitialization.initializeDebugState();
    }

    /** @deprecated Use {@link #playerResources()}. */
    @Deprecated(forRemoval = true)
    public void clearResourceBuffersForDebug() {
        playerResources.clearResourceBuffersForDebug();
    }

    /** @deprecated Use {@link #wallet()}. */
    @Deprecated(forRemoval = true)
    public int rupees() {
        return wallet.rupees();
    }

    /** @deprecated Use {@link #wallet()}. */
    @Deprecated(forRemoval = true)
    public void setRupees(int value) {
        wallet.setRupees(value);
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public int maxHearts() {
        return vitals.maxHearts();
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public void setMaxHearts(int value) {
        vitals.setMaxHearts(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int deathCount() {
        return progression.deathCount();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setDeathCount(int value) {
        progression.setDeathCount(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void incrementDeathCount() {
        progression.incrementDeathCount();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int deathCountBcd() {
        return progression.deathCountBcd();
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public int health() {
        return vitals.health();
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public void setHealth(int value) {
        vitals.setHealth(value);
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public void damage(int amount) {
        vitals.damage(amount);
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public int applyRomEnemyDamage(int nominalDamage) {
        return vitals.applyRomEnemyDamage(nominalDamage);
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public int invincibilityCounter() {
        return vitals.invincibilityCounter();
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public void setInvincibilityCounter(int value) {
        vitals.setInvincibilityCounter(value);
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public void tickInvincibility() {
        vitals.tickInvincibility();
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public int addHealthBuffer() {
        return vitals.addHealthBuffer();
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public void addSubtractHealthBuffer(int amount) {
        vitals.addSubtractHealthBuffer(amount);
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public int subtractHealthBuffer() {
        return vitals.subtractHealthBuffer();
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public void clearSubtractHealthBuffer() {
        vitals.clearSubtractHealthBuffer();
    }

    /** @deprecated Use {@link #wallet()}. */
    @Deprecated(forRemoval = true)
    public int addRupeeBuffer() {
        return wallet.addRupeeBuffer();
    }

    /** @deprecated Use {@link #playerResources()}. */
    @Deprecated(forRemoval = true)
    public void tickResourceBuffers(int frameCounter) {
        playerResources.tickResourceBuffers(frameCounter);
    }

    /** @deprecated Use {@link #playerRewards()}. */
    @Deprecated(forRemoval = true)
    public void applyEntityPickup(int entityType) {
        playerRewards.applyEntityPickup(entityType);
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public void completeHeartPieceContainer() {
        vitals.completeHeartPieceContainer();
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public void applyHeartContainerReward() {
        vitals.applyHeartContainerReward();
    }

    /** @deprecated Use {@link #vitals()}. */
    @Deprecated(forRemoval = true)
    public void requestFullHeal() {
        vitals.requestFullHeal();
    }

    /** @deprecated Use {@link #playerRewards()}. */
    @Deprecated(forRemoval = true)
    public void applyBeachSwordReward() {
        playerRewards.applyBeachSwordReward();
    }

    /** @deprecated Use {@link #playerRewards()}. */
    @Deprecated(forRemoval = true)
    public void applyToadstoolReward() {
        playerRewards.applyToadstoolReward();
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public boolean hasToadstool() {
        return ammunition.hasToadstool();
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public void setHasToadstool(boolean value) {
        ammunition.setHasToadstool(value);
    }

    /** @deprecated Use {@link #playerRewards()}. */
    @Deprecated(forRemoval = true)
    public void beginWitchToadstoolExchange(int inventorySlot) {
        playerRewards.beginWitchToadstoolExchange(inventorySlot);
    }

    /** @deprecated Use {@link #playerRewards()}. */
    @Deprecated(forRemoval = true)
    public void applyWitchMagicPowderReward() {
        playerRewards.applyWitchMagicPowderReward();
    }

    /** @deprecated Use {@link #playerRewards()}. */
    @Deprecated(forRemoval = true)
    public void applyFloatingItemPickup(int entityType, int sourceVariant) {
        playerRewards.applyFloatingItemPickup(entityType, sourceVariant);
    }

    /** @deprecated Use {@link #chestRewards()}. */
    @Deprecated(forRemoval = true)
    public void applyChestReward(int chestItem) {
        chestRewards.applyChestReward(chestItem);
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public int swordLevel() {
        return equipmentLevels.swordLevel();
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public void setSwordLevel(int value) {
        equipmentLevels.setSwordLevel(value);
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public int shieldLevel() {
        return equipmentLevels.shieldLevel();
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public void setShieldLevel(int value) {
        equipmentLevels.setShieldLevel(value);
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public int powerBraceletLevel() {
        return equipmentLevels.powerBraceletLevel();
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public void setPowerBraceletLevel(int value) {
        equipmentLevels.setPowerBraceletLevel(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public boolean hasMedicine() {
        return progression.hasMedicine();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setHasMedicine(boolean value) {
        progression.setHasMedicine(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public boolean hasFlippers() {
        return progression.hasFlippers();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setHasFlippers(boolean value) {
        progression.setHasFlippers(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int tradeSequenceItem() {
        return progression.tradeSequenceItem();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setTradeSequenceItem(int value) {
        progression.setTradeSequenceItem(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int medicineCount() {
        return progression.medicineCount();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setMedicineCount(int value) {
        progression.setMedicineCount(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int tailKeyCount() {
        return progression.tailKeyCount();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setTailKeyCount(int value) {
        progression.setTailKeyCount(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int anglerKeyCount() {
        return progression.anglerKeyCount();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setAnglerKeyCount(int value) {
        progression.setAnglerKeyCount(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int faceKeyCount() {
        return progression.faceKeyCount();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setFaceKeyCount(int value) {
        progression.setFaceKeyCount(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int birdKeyCount() {
        return progression.birdKeyCount();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setBirdKeyCount(int value) {
        progression.setBirdKeyCount(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int goldenLeavesCount() {
        return progression.goldenLeavesCount();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setGoldenLeavesCount(int value) {
        progression.setGoldenLeavesCount(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int chestItemCount(int chestItem) {
        return progression.chestItemCount(chestItem);
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public int arrowCount() {
        return ammunition.arrowCount();
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public void setArrowCount(int value) {
        ammunition.setArrowCount(value);
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public int maxArrows() {
        return ammunition.maxArrows();
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public void setMaxArrows(int value) {
        ammunition.setMaxArrows(value);
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public int bombCount() {
        return ammunition.bombCount();
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public void setBombCount(int value) {
        ammunition.setBombCount(value);
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public int maxBombs() {
        return ammunition.maxBombs();
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public void setMaxBombs(int value) {
        ammunition.setMaxBombs(value);
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public int magicPowderCount() {
        return ammunition.magicPowderCount();
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public void setMagicPowderCount(int value) {
        ammunition.setMagicPowderCount(value);
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public boolean consumeMagicPowder() {
        return ammunition.consumeMagicPowder();
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public int maxMagicPowder() {
        return ammunition.maxMagicPowder();
    }

    /** @deprecated Use {@link #ammunition()}. */
    @Deprecated(forRemoval = true)
    public void setMaxMagicPowder(int value) {
        ammunition.setMaxMagicPowder(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int heartPieces() {
        return progression.heartPieces();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setHeartPieces(int value) {
        progression.setHeartPieces(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int seashells() {
        return progression.seashells();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setSeashells(int value) {
        progression.setSeashells(value);
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public PowerUp powerUp() {
        return equipmentLevels.powerUp();
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public void setPowerUp(PowerUp value) {
        equipmentLevels.setPowerUp(value);
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public int activePowerUp() {
        return equipmentLevels.activePowerUp();
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public void setActivePowerUp(int value) {
        equipmentLevels.setActivePowerUp(value);
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public void clearActivePowerUpForWarp() {
        equipmentLevels.clearActivePowerUpForWarp();
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public int powerUpHits() {
        return equipmentLevels.powerUpHits();
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public Tunic tunic() {
        return equipmentLevels.tunic();
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public void setTunic(Tunic value) {
        equipmentLevels.setTunic(value);
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public int tunicType() {
        return equipmentLevels.tunicType();
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public void setTunicType(int value) {
        equipmentLevels.setTunicType(value);
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public boolean runningWithPegasusBoots() {
        return equipmentLevels.runningWithPegasusBoots();
    }

    /** @deprecated Use {@link #equipmentLevels()}. */
    @Deprecated(forRemoval = true)
    public void setRunningWithPegasusBoots(boolean value) {
        equipmentLevels.setRunningWithPegasusBoots(value);
    }

    /** @deprecated Use {@link #inventory()}. */
    @Deprecated(forRemoval = true)
    public int itemA() {
        return inventory.itemA();
    }

    /** @deprecated Use {@link #inventory()}. */
    @Deprecated(forRemoval = true)
    public void setItemA(int inventoryId) {
        inventory.setItemA(inventoryId);
    }

    /** @deprecated Use {@link #inventory()}. */
    @Deprecated(forRemoval = true)
    public int itemB() {
        return inventory.itemB();
    }

    /** @deprecated Use {@link #inventory()}. */
    @Deprecated(forRemoval = true)
    public void setItemB(int inventoryId) {
        inventory.setItemB(inventoryId);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int ocarinaSongFlags() {
        return progression.ocarinaSongFlags();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setOcarinaSongFlags(int value) {
        progression.setOcarinaSongFlags(value);
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public int selectedSongIndex() {
        return progression.selectedSongIndex();
    }

    /** @deprecated Use {@link #progression()}. */
    @Deprecated(forRemoval = true)
    public void setSelectedSongIndex(int value) {
        progression.setSelectedSongIndex(value);
    }

    /** @deprecated Use {@link #inventory()}. */
    @Deprecated(forRemoval = true)
    public void setSubscreenItems(int[] items) {
        inventory.setSubscreenItems(items);
    }

    /** @deprecated Use {@link #playerSaveLoader()}. */
    @Deprecated(forRemoval = true)
    public void applySavedGame(SaveSlotState saved) {
        playerSaveLoader.applySavedGame(saved);
    }

    /** @deprecated Use {@link #inventory()}. */
    @Deprecated(forRemoval = true)
    public int subscreenItem(int slotIndex) {
        return inventory.subscreenItem(slotIndex);
    }

    /** @deprecated Use {@link #inventory()}. */
    @Deprecated(forRemoval = true)
    public void swapItemAWithSubscreen(int slotIndex) {
        inventory.swapItemAWithSubscreen(slotIndex);
    }

    /** @deprecated Use {@link #inventory()}. */
    @Deprecated(forRemoval = true)
    public void swapItemBWithSubscreen(int slotIndex) {
        inventory.swapItemBWithSubscreen(slotIndex);
    }
}
