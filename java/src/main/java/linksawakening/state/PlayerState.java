package linksawakening.state;

import linksawakening.save.SaveSlotState;
import linksawakening.world.ChestContentsTable;
import linksawakening.world.FloatingItemMotion;

import java.util.Arrays;
import java.util.Objects;

public final class PlayerState {

    public static final int INVENTORY_EMPTY = 0x00;
    public static final int INVENTORY_SWORD = 0x01;
    public static final int INVENTORY_BOMBS = 0x02;
    public static final int INVENTORY_POWER_BRACELET = 0x03;
    public static final int INVENTORY_SHIELD = 0x04;
    public static final int INVENTORY_BOW = 0x05;
    public static final int INVENTORY_HOOKSHOT = 0x06;
    public static final int INVENTORY_MAGIC_ROD = 0x07;
    public static final int INVENTORY_PEGASUS_BOOTS = 0x08;
    public static final int INVENTORY_OCARINA = 0x09;
    public static final int INVENTORY_ROCS_FEATHER = 0x0A;
    public static final int INVENTORY_SHOVEL = 0x0B;
    public static final int INVENTORY_MAGIC_POWDER = 0x0C;
    public static final int INVENTORY_BOOMERANG = 0x0D;

    // Values stored in the ROM's wOcarinaSongFlags byte.
    public static final int FROGS_SONG_OF_THE_SOUL_FLAG = 0x01;
    public static final int MANBO_MAMBO_FLAG = 0x02;
    public static final int BALLAD_OF_THE_WIND_FISH_FLAG = 0x04;

    // Values written to the ROM's wTunicType ($DC0F).
    public static final int TUNIC_GREEN = 0x00;
    public static final int TUNIC_RED = 0x01;
    public static final int TUNIC_BLUE = 0x02;

    public static final int ACTIVE_POWER_UP_NONE = 0x00;
    public static final int ACTIVE_POWER_UP_PIECE_OF_POWER = 0x01;
    public static final int ACTIVE_POWER_UP_GUARDIAN_ACORN = 0x02;

    public static final int MAX_RUPEES = 999;
    public static final int MAX_HEARTS = 14;
    public static final int HP_PER_HEART = 8;

    // Matches the disassembly's wInventoryItems.subscreen (DB02-DB0B): 10
    // persistent inventory slots visible as a 2-col x 5-row grid under the
    // A/B status bar. When the player equips an item, it swaps between the
    // A/B slot and the selected subscreen slot (see bank20.asm:5FDB).
    public static final int SUBSCREEN_SLOT_COUNT = 10;

    private int rupees = 0;
    private int maxHearts = 3;
    private int health = 3 * HP_PER_HEART;
    private int invincibilityCounter;
    private int swordLevel = 1;
    private int shieldLevel = 1;
    private int powerBraceletLevel = 1;
    private int itemA = INVENTORY_SWORD;
    private int itemB = INVENTORY_EMPTY;
    private int ocarinaSongFlags;
    private int selectedSongIndex;
    private final int[] subscreen = new int[SUBSCREEN_SLOT_COUNT];
    private int arrowCount;
    private int maxArrows;
    private int bombCount;
    private int maxBombs;
    private int magicPowderCount;
    private boolean hasToadstool;
    private int maxMagicPowder;
    private int heartPieces;
    private int seashells;
    private boolean hasFlippers;
    private boolean hasMedicine;
    private int tradeSequenceItem;
    private int medicineCount;
    private int tailKeyCount;
    private int anglerKeyCount;
    private int faceKeyCount;
    private int birdKeyCount;
    private int goldenLeavesCount;
    private int activePowerUp;
    private int tunicType = TUNIC_GREEN;
    private boolean runningWithPegasusBoots;
    private int addHealthBuffer;
    private int subtractHealthBuffer;
    private int addRupeeBuffer;
    private int powerUpHits;
    private final int[] chestItemCounts = new int[0x22];

    public PlayerState() {
        // Stub test data so the inventory menu has something to equip until
        // the real pickup/chest flow exists. Fills slots with the inventory
        // ids BOMBS..SHOVEL in order.
        for (int i = 0; i < subscreen.length; i++) {
            subscreen[i] = INVENTORY_BOMBS + i;
        }
    }

    /**
     * Initializes the fields represented by a newly created ROM save file.
     *
     * <p>The normal constructor intentionally retains its debug inventory for
     * existing development entry points.  The real file-menu New Game path
     * calls this method after the ROM's zeroed save block has been selected.
     * The capacity arguments are the values written by
     * {@code LoadSavedFile.initNewGame}.</p>
     */
    public void initializeNewGame(int maxArrows, int maxBombs, int maxMagicPowder) {
        rupees = 0;
        maxHearts = 3;
        health = maxHearts * HP_PER_HEART;
        invincibilityCounter = 0;
        swordLevel = 0;
        shieldLevel = 0;
        powerBraceletLevel = 0;
        itemA = INVENTORY_EMPTY;
        itemB = INVENTORY_EMPTY;
        ocarinaSongFlags = 0;
        selectedSongIndex = 0;
        Arrays.fill(subscreen, INVENTORY_EMPTY);
        arrowCount = 0;
        this.maxArrows = clamp(maxArrows, 0, 99);
        bombCount = 0;
        this.maxBombs = clamp(maxBombs, 0, 99);
        magicPowderCount = 0;
        hasToadstool = false;
        this.maxMagicPowder = clamp(maxMagicPowder, 0, 99);
        heartPieces = 0;
        seashells = 0;
        hasFlippers = false;
        hasMedicine = false;
        tradeSequenceItem = 0;
        medicineCount = 0;
        tailKeyCount = 0;
        anglerKeyCount = 0;
        faceKeyCount = 0;
        birdKeyCount = 0;
        goldenLeavesCount = 0;
        activePowerUp = ACTIVE_POWER_UP_NONE;
        tunicType = TUNIC_GREEN;
        runningWithPegasusBoots = false;
        addHealthBuffer = 0;
        subtractHealthBuffer = 0;
        addRupeeBuffer = 0;
        powerUpHits = 0;
        Arrays.fill(chestItemCounts, 0);
    }

    /** Loads a generous development loadout without changing ROM gameplay rules. */
    public void initializeDebugState() {
        setMaxHearts(MAX_HEARTS);
        setHealth(MAX_HEARTS * HP_PER_HEART);
        setRupees(MAX_RUPEES);
        setSwordLevel(2);
        setShieldLevel(2);
        setPowerBraceletLevel(2);
        setMaxArrows(99);
        setArrowCount(99);
        setMaxBombs(99);
        setBombCount(99);
        setMaxMagicPowder(99);
        setMagicPowderCount(99);
        setSubscreenItems(new int[] {
            INVENTORY_BOMBS, INVENTORY_POWER_BRACELET, INVENTORY_SHIELD,
            INVENTORY_BOW, INVENTORY_HOOKSHOT, INVENTORY_MAGIC_ROD,
            INVENTORY_PEGASUS_BOOTS, INVENTORY_OCARINA, INVENTORY_ROCS_FEATHER,
            INVENTORY_SHOVEL
        });
        setItemA(INVENTORY_SWORD);
        setItemB(INVENTORY_BOOMERANG);
        setOcarinaSongFlags(FROGS_SONG_OF_THE_SOUL_FLAG | MANBO_MAMBO_FLAG
            | BALLAD_OF_THE_WIND_FISH_FLAG);
        setHasFlippers(true);
        setHasMedicine(true);
        setMedicineCount(1);
        setTailKeyCount(1);
        setAnglerKeyCount(1);
        setFaceKeyCount(1);
        setBirdKeyCount(1);
        setGoldenLeavesCount(5);
        setSeashells(99);
        setHasToadstool(true);
    }

    public int rupees() {
        return rupees;
    }

    public void setRupees(int value) {
        rupees = clamp(value, 0, MAX_RUPEES);
    }

    public int maxHearts() {
        return maxHearts;
    }

    public void setMaxHearts(int value) {
        maxHearts = clamp(value, 1, MAX_HEARTS);
        health = Math.min(health, maxHearts * HP_PER_HEART);
    }

    public int health() {
        return health;
    }

    public void setHealth(int value) {
        health = clamp(value, 0, maxHearts * HP_PER_HEART);
    }

    public void damage(int amount) {
        setHealth(health - Math.max(0, amount));
    }

    /**
     * Applies the generic enemy collision damage path from bank3.asm. The
     * nominal damage is buffered; UpdateHealth consumes that buffer on odd
     * frames instead of changing health immediately.
     */
    public int applyRomEnemyDamage(int nominalDamage) {
        int effectiveDamage = nominalDamage & 0xFF;
        if (tunicType == TUNIC_BLUE) {
            effectiveDamage >>= 1;
        } else if (activePowerUp == ACTIVE_POWER_UP_GUARDIAN_ACORN) {
            effectiveDamage = effectiveDamage == 0x04
                ? 0
                : effectiveDamage >> 1;
        }

        subtractHealthBuffer = (subtractHealthBuffer + effectiveDamage) & 0xFF;
        invincibilityCounter = 0x50;
        if (activePowerUp != ACTIVE_POWER_UP_NONE) {
            powerUpHits = (powerUpHits + 1) & 0xFF;
            if (powerUpHits >= 3) {
                activePowerUp = ACTIVE_POWER_UP_NONE;
            }
        }
        return effectiveDamage;
    }

    public int invincibilityCounter() {
        return invincibilityCounter;
    }

    public void setInvincibilityCounter(int value) {
        invincibilityCounter = clamp(value, 0, 0xFF);
    }

    public void tickInvincibility() {
        if (invincibilityCounter > 0) {
            invincibilityCounter--;
        }
    }

    /** Amount of health still waiting in the ROM's wAddHealthBuffer. */
    public int addHealthBuffer() {
        return addHealthBuffer;
    }

    /** Amount of health still waiting in the ROM's wSubtractHealthBuffer. */
    public int subtractHealthBuffer() {
        return subtractHealthBuffer;
    }

    /** Amount of rupees still waiting in the ROM's wAddRupeeBufferLow. */
    public int addRupeeBuffer() {
        return addRupeeBuffer;
    }

    /**
     * Advances the resource counters with the same frame parity as bank2's
     * UpdateRupeesCount and UpdateHealth routines.
     */
    public void tickResourceBuffers(int frameCounter) {
        if ((frameCounter & 0x01) == 0) {
            tickRupeeBuffer();
        } else {
            tickHealthBuffer();
        }
    }

    /** Applies the immediate state portion of a bank3 pickable handler. */
    public void applyEntityPickup(int entityType) {
        switch (entityType & 0xFF) {
            case 0x2D -> addHealthBuffer = Math.min(0xFF, addHealthBuffer + HP_PER_HEART);
            case 0x2E -> addRupeeBuffer = Math.min(0xFF, addRupeeBuffer + 1);
            case 0x33 -> setActivePowerUp(ACTIVE_POWER_UP_PIECE_OF_POWER);
            case 0x34 -> setActivePowerUp(ACTIVE_POWER_UP_GUARDIAN_ACORN);
            case 0x37 -> arrowCount = incrementUpTo(arrowCount, maxArrows);
            case 0x38 -> {
                giveInventoryItem(INVENTORY_BOMBS);
                bombCount = incrementUpTo(bombCount, maxBombs);
            }
            case 0x3B -> {
                giveInventoryItem(INVENTORY_MAGIC_POWDER);
                magicPowderCount = incrementUpTo(magicPowderCount, maxMagicPowder);
            }
            case 0x3D -> seashells = Math.min(99, seashells + 1);
            default -> {
                // Transition-driven pickups are advanced by their entity
                // handlers; their room event still reaches this method when
                // they need a future-specific state implementation.
            }
        }
    }

    /** Applies HeartContainerEntityHandler's delayed health reward. */
    public void applyHeartContainerReward() {
        setMaxHearts(maxHearts + 1);
        addHealthBuffer = 0xFF;
    }

    /** Applies the delayed reward at the end of the beach-sword cutscene. */
    public void applyBeachSwordReward() {
        giveInventoryItem(INVENTORY_SWORD);
        setSwordLevel(1);
    }

    /** Applies SleepyToadstoolEntityHandler's final inventory and flag writes. */
    public void applyToadstoolReward() {
        giveInventoryItem(INVENTORY_MAGIC_POWDER);
        hasToadstool = true;
    }

    public boolean hasToadstool() {
        return hasToadstool;
    }

    public void setHasToadstool(boolean value) {
        hasToadstool = value;
    }

    /** Applies WitchEntityHandler's slot clear and wHasToadstool reset. */
    public void beginWitchToadstoolExchange(int inventorySlot) {
        if (inventorySlot == 0) {
            itemB = INVENTORY_EMPTY;
        } else if (inventorySlot == 1) {
            itemA = INVENTORY_EMPTY;
        } else {
            throw new IllegalArgumentException("Inventory slot must be B (0) or A (1): "
                + inventorySlot);
        }
        hasToadstool = false;
    }

    /** Applies the witch's AssignItemToSlot and BCD +$20 powder reward. */
    public void applyWitchMagicPowderReward() {
        giveInventoryItem(INVENTORY_MAGIC_POWDER);
        // The player model stores quantities as decimal values. WitchEntityHandler
        // adds BCD $20 with DAA and does not clamp against wMaxMagicPowder.
        magicPowderCount = Math.min(99, magicPowderCount + 20);
    }

    /** Applies the source-variant dispatch at FloatingItemEntityHandler. */
    public void applyFloatingItemPickup(int entityType, int sourceVariant) {
        switch (FloatingItemMotion.pickupEffect(entityType, sourceVariant)) {
            case TEN_RUPEES -> addRupeeBuffer = 10;
            case MAGIC_POWDER -> {
                giveInventoryItem(INVENTORY_MAGIC_POWDER);
                magicPowderCount = incrementByTenUpTo(magicPowderCount, maxMagicPowder);
            }
            case TEN_BOMBS -> bombCount = incrementByTenUpTo(bombCount, maxBombs);
            case HEALTH_18 -> addHealthBuffer = Math.min(0xFF, addHealthBuffer + 0x18);
            // FloatingArrowsHandler adds BCD $10 and performs no capacity
            // check. PlayerState stores semantic decimal counts, so the
            // equivalent is decimal modulo-100 addition.
            case TEN_ARROWS -> arrowCount = (arrowCount + 10) % 100;
        }
    }

    /** Applies the immediate reward writes performed by EntityInitChestWithItem. */
    public void applyChestReward(int chestItem) {
        if (chestItem < 0 || chestItem > ChestContentsTable.CHEST_ZOL) {
            throw new IllegalArgumentException("Chest item must be an unsigned chest variant: "
                + chestItem);
        }
        if (chestItem < chestItemCounts.length) {
            chestItemCounts[chestItem] = Math.min(0xFF, chestItemCounts[chestItem] + 1);
        }
        switch (chestItem) {
            case ChestContentsTable.CHEST_POWER_BRACELET -> {
                powerBraceletLevel = Math.min(2, powerBraceletLevel + 1);
                giveInventoryItem(INVENTORY_POWER_BRACELET);
            }
            case ChestContentsTable.CHEST_SHIELD -> {
                setShieldLevel(shieldLevel + 1);
                giveInventoryItem(INVENTORY_SHIELD);
            }
            case ChestContentsTable.CHEST_BOW -> giveInventoryItem(INVENTORY_BOW);
            case ChestContentsTable.CHEST_HOOKSHOT -> giveInventoryItem(INVENTORY_HOOKSHOT);
            case ChestContentsTable.CHEST_MAGIC_ROD -> giveInventoryItem(INVENTORY_MAGIC_ROD);
            case ChestContentsTable.CHEST_PEGASUS_BOOTS ->
                giveInventoryItem(INVENTORY_PEGASUS_BOOTS);
            case ChestContentsTable.CHEST_OCARINA -> giveInventoryItem(INVENTORY_OCARINA);
            case ChestContentsTable.CHEST_FEATHER -> giveInventoryItem(INVENTORY_ROCS_FEATHER);
            case ChestContentsTable.CHEST_SHOVEL -> giveInventoryItem(INVENTORY_SHOVEL);
            case ChestContentsTable.CHEST_FLIPPERS -> hasFlippers = true;
            case ChestContentsTable.CHEST_MAGIC_POWDER_BAG -> {
                giveInventoryItem(INVENTORY_MAGIC_POWDER);
                magicPowderCount = incrementUpTo(magicPowderCount, maxMagicPowder);
            }
            case ChestContentsTable.CHEST_BOMB -> {
                giveInventoryItem(INVENTORY_BOMBS);
                bombCount = incrementUpTo(bombCount, maxBombs);
            }
            case ChestContentsTable.CHEST_SWORD -> giveInventoryItem(INVENTORY_SWORD);
            case ChestContentsTable.CHEST_MEDICINE -> {
                hasMedicine = true;
                medicineCount = incrementByte(medicineCount);
            }
            case ChestContentsTable.CHEST_TAIL_KEY -> tailKeyCount = incrementByte(tailKeyCount);
            case ChestContentsTable.CHEST_ANGLER_KEY ->
                anglerKeyCount = incrementByte(anglerKeyCount);
            case ChestContentsTable.CHEST_FACE_KEY -> faceKeyCount = incrementByte(faceKeyCount);
            case ChestContentsTable.CHEST_BIRD_KEY -> birdKeyCount = incrementByte(birdKeyCount);
            case ChestContentsTable.CHEST_GOLD_LEAF ->
                goldenLeavesCount = incrementByte(goldenLeavesCount);
            case ChestContentsTable.CHEST_RUPEES_50 -> addRupeeBuffer =
                Math.min(MAX_RUPEES, addRupeeBuffer + 50);
            case ChestContentsTable.CHEST_RUPEES_20 -> addRupeeBuffer =
                Math.min(MAX_RUPEES, addRupeeBuffer + 20);
            case ChestContentsTable.CHEST_RUPEES_100 -> addRupeeBuffer =
                Math.min(MAX_RUPEES, addRupeeBuffer + 100);
            case ChestContentsTable.CHEST_RUPEES_200 -> addRupeeBuffer =
                Math.min(MAX_RUPEES, addRupeeBuffer + 200);
            case ChestContentsTable.CHEST_RUPEES_500 -> addRupeeBuffer =
                Math.min(MAX_RUPEES, addRupeeBuffer + 500);
            case ChestContentsTable.CHEST_SEASHELL -> setSeashells(seashells + 1);
            case ChestContentsTable.CHEST_MESSAGE, ChestContentsTable.CHEST_ZOL -> {
                // The source marks these rooms complete without applying a
                // normal inventory reward.
            }
            default -> {
                // Flippers, lens, keys, leaves, and dungeon items have their
                // source-specific WRAM counters in the room/dungeon bridges;
                // chestItemCounts retains the immediate collection fact until
                // those dedicated state fields are modeled.
            }
        }
    }

    public int swordLevel() {
        return swordLevel;
    }

    public void setSwordLevel(int value) {
        swordLevel = clamp(value, 0, 2);
    }

    /** Mirrors the ROM shield level used by projectile collision handlers. */
    public int shieldLevel() {
        return shieldLevel;
    }

    public void setShieldLevel(int value) {
        shieldLevel = clamp(value, 0, 2);
    }

    public int powerBraceletLevel() {
        return powerBraceletLevel;
    }

    public void setPowerBraceletLevel(int value) {
        powerBraceletLevel = clamp(value, 0, 2);
    }

    public boolean hasMedicine() {
        return hasMedicine;
    }

    public void setHasMedicine(boolean value) {
        hasMedicine = value;
    }

    public boolean hasFlippers() {
        return hasFlippers;
    }

    public void setHasFlippers(boolean value) {
        hasFlippers = value;
    }

    public int tradeSequenceItem() {
        return tradeSequenceItem;
    }

    public void setTradeSequenceItem(int value) {
        tradeSequenceItem = clamp(value, 0, 0xFF);
    }

    public int medicineCount() {
        return medicineCount;
    }

    public void setMedicineCount(int value) {
        medicineCount = clamp(value, 0, 0xFF);
    }

    public int tailKeyCount() {
        return tailKeyCount;
    }

    public void setTailKeyCount(int value) {
        tailKeyCount = clamp(value, 0, 0xFF);
    }

    public int anglerKeyCount() {
        return anglerKeyCount;
    }

    public void setAnglerKeyCount(int value) {
        anglerKeyCount = clamp(value, 0, 0xFF);
    }

    public int faceKeyCount() {
        return faceKeyCount;
    }

    public void setFaceKeyCount(int value) {
        faceKeyCount = clamp(value, 0, 0xFF);
    }

    public int birdKeyCount() {
        return birdKeyCount;
    }

    public void setBirdKeyCount(int value) {
        birdKeyCount = clamp(value, 0, 0xFF);
    }

    public int goldenLeavesCount() {
        return goldenLeavesCount;
    }

    public void setGoldenLeavesCount(int value) {
        goldenLeavesCount = clamp(value, 0, 0xFF);
    }

    public int chestItemCount(int chestItem) {
        if (chestItem < 0 || chestItem >= chestItemCounts.length) {
            throw new IllegalArgumentException("Chest item count index out of range: " + chestItem);
        }
        return chestItemCounts[chestItem];
    }

    public int arrowCount() {
        return arrowCount;
    }

    public void setArrowCount(int value) {
        arrowCount = clamp(value, 0, maxArrows);
    }

    public int maxArrows() {
        return maxArrows;
    }

    public void setMaxArrows(int value) {
        maxArrows = clamp(value, 0, 99);
        arrowCount = Math.min(arrowCount, maxArrows);
    }

    public int bombCount() {
        return bombCount;
    }

    public void setBombCount(int value) {
        bombCount = clamp(value, 0, maxBombs);
    }

    public int maxBombs() {
        return maxBombs;
    }

    public void setMaxBombs(int value) {
        maxBombs = clamp(value, 0, 99);
        bombCount = Math.min(bombCount, maxBombs);
    }

    public int magicPowderCount() {
        return magicPowderCount;
    }

    public void setMagicPowderCount(int value) {
        magicPowderCount = clamp(value, 0, maxMagicPowder);
    }

    /**
     * Mirrors SprinkleMagicPowder's BCD decrement and empty-button cleanup.
     * Returns false when the source count was already zero.
     */
    public boolean consumeMagicPowder() {
        if (magicPowderCount == 0) {
            return false;
        }
        magicPowderCount--;
        if (magicPowderCount == 0) {
            if (itemA == INVENTORY_MAGIC_POWDER) {
                itemA = INVENTORY_EMPTY;
            }
            if (itemB == INVENTORY_MAGIC_POWDER) {
                itemB = INVENTORY_EMPTY;
            }
        }
        return true;
    }

    public int maxMagicPowder() {
        return maxMagicPowder;
    }

    public void setMaxMagicPowder(int value) {
        maxMagicPowder = clamp(value, 0, 99);
        magicPowderCount = Math.min(magicPowderCount, maxMagicPowder);
    }

    public int heartPieces() {
        return heartPieces;
    }

    public void setHeartPieces(int value) {
        heartPieces = clamp(value, 0, 3);
    }

    public int seashells() {
        return seashells;
    }

    public void setSeashells(int value) {
        seashells = clamp(value, 0, 99);
    }

    public int activePowerUp() {
        return activePowerUp;
    }

    /** Mirrors the ROM pickup boundary that starts a fresh power-up streak. */
    public void setActivePowerUp(int value) {
        activePowerUp = clamp(value, ACTIVE_POWER_UP_NONE, ACTIVE_POWER_UP_GUARDIAN_ACORN);
        powerUpHits = 0;
    }

    /** Number of accepted enemy hits taken while the current power-up was active. */
    public int powerUpHits() {
        return powerUpHits;
    }

    public int tunicType() {
        return tunicType;
    }

    public void setTunicType(int value) {
        tunicType = clamp(value, TUNIC_GREEN, TUNIC_BLUE);
    }

    /** Mirrors wIsRunningWithPegasusBoots; movement owns when this is set. */
    public boolean runningWithPegasusBoots() {
        return runningWithPegasusBoots;
    }

    public void setRunningWithPegasusBoots(boolean value) {
        runningWithPegasusBoots = value;
    }

    public int itemA() {
        return itemA;
    }

    public void setItemA(int inventoryId) {
        itemA = inventoryId & 0xFF;
    }

    public int itemB() {
        return itemB;
    }

    public void setItemB(int inventoryId) {
        itemB = inventoryId & 0xFF;
    }

    /** Mirrors the persistent wOcarinaSongFlags byte (only bits 0..2 are used). */
    public int ocarinaSongFlags() {
        return ocarinaSongFlags;
    }

    public void setOcarinaSongFlags(int value) {
        ocarinaSongFlags = value & 0x07;
    }

    /** Mirrors wSelectedSongIndex, a zero-based Ballad/Mambo/Frog selector. */
    public int selectedSongIndex() {
        return selectedSongIndex;
    }

    public void setSelectedSongIndex(int value) {
        selectedSongIndex = clamp(value, 0, 2);
    }

    public void setSubscreenItems(int[] items) {
        Objects.requireNonNull(items, "items");
        if (items.length != SUBSCREEN_SLOT_COUNT) {
            throw new IllegalArgumentException("Expected " + SUBSCREEN_SLOT_COUNT
                + " subscreen items, got " + items.length);
        }
        for (int index = 0; index < items.length; index++) {
            subscreen[index] = items[index] & 0xFF;
        }
    }

    /** Applies the persistent fields currently represented by the Java model. */
    public void applySavedGame(SaveSlotState saved) {
        Objects.requireNonNull(saved, "saved");
        setMaxHearts(saved.maxHearts());
        setHealth(saved.health());
        setRupees(saved.rupees());
        setHeartPieces(saved.heartPieces());
        setSeashells(saved.seashells());
        setHasFlippers(saved.hasFlippers() != 0);
        setHasMedicine(saved.hasMedicine() != 0);
        setTradeSequenceItem(saved.tradeSequenceItem());
        setMedicineCount(saved.medicineCount());
        setTailKeyCount(saved.tailKeyCount());
        setAnglerKeyCount(saved.anglerKeyCount());
        setFaceKeyCount(saved.faceKeyCount());
        setBirdKeyCount(saved.birdKeyCount());
        setGoldenLeavesCount(saved.goldenLeavesCount());
        setPowerBraceletLevel(saved.powerBraceletLevel());
        setSwordLevel(saved.swordLevel());
        setShieldLevel(saved.shieldLevel());
        setItemA(saved.itemA());
        setItemB(saved.itemB());
        setSubscreenItems(saved.subscreen());
        setMaxArrows(saved.maxArrows());
        setArrowCount(saved.arrowCount());
        setMaxBombs(saved.maxBombs());
        setBombCount(saved.bombCount());
        setMaxMagicPowder(saved.maxMagicPowder());
        setMagicPowderCount(saved.magicPowderCount());
        setHasToadstool(saved.hasToadstool() != 0);
        setOcarinaSongFlags(saved.ocarinaSongFlags());
        setSelectedSongIndex(saved.selectedSongIndex());
        setTunicType(saved.tunicType());

        invincibilityCounter = 0;
        activePowerUp = ACTIVE_POWER_UP_NONE;
        runningWithPegasusBoots = false;
        addHealthBuffer = 0;
        subtractHealthBuffer = 0;
        addRupeeBuffer = 0;
        powerUpHits = 0;
        Arrays.fill(chestItemCounts, 0);
    }

    public int subscreenItem(int slotIndex) {
        return subscreen[slotIndex];
    }

    /** Swap slot A with the given subscreen slot, matching bank20.asm:5FDB. */
    public void swapItemAWithSubscreen(int slotIndex) {
        int tmp = itemA;
        itemA = subscreen[slotIndex];
        subscreen[slotIndex] = tmp;
    }

    /** Swap slot B with the given subscreen slot, matching bank20.asm:5FF3. */
    public void swapItemBWithSubscreen(int slotIndex) {
        int tmp = itemB;
        itemB = subscreen[slotIndex];
        subscreen[slotIndex] = tmp;
    }

    private void tickHealthBuffer() {
        if (addHealthBuffer != 0) {
            int maxHealth = maxHearts * HP_PER_HEART;
            if (health >= maxHealth) {
                addHealthBuffer = 0;
            } else {
                health++;
                addHealthBuffer--;
                return;
            }
        }
        if (subtractHealthBuffer == 0) {
            return;
        }
        subtractHealthBuffer--;
        if (health > 0) {
            health--;
        }
    }

    private void tickRupeeBuffer() {
        if (addRupeeBuffer == 0) {
            return;
        }
        if (rupees >= MAX_RUPEES) {
            addRupeeBuffer = 0;
            return;
        }
        rupees++;
        addRupeeBuffer--;
    }

    private void giveInventoryItem(int inventoryId) {
        if (itemB == inventoryId || itemA == inventoryId) {
            return;
        }
        for (int item : subscreen) {
            if (item == inventoryId) {
                return;
            }
        }
        if (itemB == INVENTORY_EMPTY) {
            itemB = inventoryId;
            return;
        }
        if (itemA == INVENTORY_EMPTY) {
            itemA = inventoryId;
            return;
        }
        for (int index = 0; index < subscreen.length; index++) {
            if (subscreen[index] == INVENTORY_EMPTY) {
                subscreen[index] = inventoryId;
                return;
            }
        }
    }

    private static int incrementUpTo(int value, int maximum) {
        return value < maximum ? value + 1 : value;
    }

    private static int incrementByTenUpTo(int value, int maximum) {
        return Math.min(maximum, value + 10);
    }

    private static int incrementByte(int value) {
        return (value + 1) & 0xFF;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
