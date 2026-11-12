package linksawakening.state;

import java.util.Arrays;
import java.util.Objects;
import linksawakening.save.SaveSlotState;
import linksawakening.world.entity.event.FloatingItemPickupEffect;
import linksawakening.world.room.ChestContentsTable;

/** Persistent player resources, progression and transient resource-update state. */
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

    private int rupees = 0;
    private int maxHearts = 3;
    private int health = 3 * HP_PER_HEART;
    private int invincibilityCounter;
    private int swordLevel = 1;
    private int shieldLevel = 1;
    private int powerBraceletLevel = 1;
    private int itemA = InventoryItem.SWORD.romValue();
    private int itemB = InventoryItem.EMPTY.romValue();
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
    private PowerUp activePowerUp = PowerUp.NONE;
    private Tunic tunicType = Tunic.GREEN;
    private boolean runningWithPegasusBoots;
    private int addHealthBuffer;
    private int subtractHealthBuffer;
    private int addRupeeBuffer;
    private int rupeeUpdateDelay;
    private final java.util.List<linksawakening.gameplay.GameplaySoundEvent> resourceSounds =
            new java.util.ArrayList<>();
    // ThresholdLowHealthTable ($02:6308), including the overlapping code bytes at 0–2.
    private static final int[] LOW_HEALTH_THRESHOLDS = {0x00, 0x22, 0xC9, 5, 5, 5, 9, 9, 9, 17, 17, 17, 25, 25, 25};
    private boolean onLowHealth;
    private boolean fullHearts;

    /** PerformOverworldAudioTasks ($14:4B0C), before world simulation. */
    public void latchFullHearts() {
        fullHearts = health == maxHearts * HP_PER_HEART;
    }

    public boolean fullHearts() {
        return fullHearts;
    }

    private int timeToNextLowHealthSound;

    public boolean isOnLowHealth() {
        return onLowHealth;
    }

    /** Resolves queued writes to hWaveSfx: the last ROM write wins. */
    public linksawakening.gameplay.GameplaySoundEvent takeResourceSound() {
        var sound = resourceSounds.isEmpty() ? null : resourceSounds.get(resourceSounds.size() - 1);
        resourceSounds.clear();
        return sound;
    }

    private int powerUpHits;
    private final int[] chestItemCounts = new int[0x22];

    public PlayerState() {
        // Stub test data so the inventory menu has something to equip until
        // the real pickup/chest flow exists. Fills slots with the inventory
        // ids BOMBS..SHOVEL in order.
        for (int i = 0; i < subscreen.length; i++) {
            subscreen[i] = InventoryItem.BOMBS.romValue() + i;
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
        itemA = InventoryItem.EMPTY.romValue();
        itemB = InventoryItem.EMPTY.romValue();
        ocarinaSongFlags = 0;
        selectedSongIndex = 0;
        Arrays.fill(subscreen, InventoryItem.EMPTY.romValue());
        arrowCount = 0;
        this.maxArrows = clamp(maxArrows, 0, 99);
        bombCount = 0;
        this.maxBombs = clamp(maxBombs, 0, 99);
        magicPowderCount = 0;
        hasToadstool = false;
        this.maxMagicPowder = clamp(maxMagicPowder, 0, 99);
        deathCount = 0;
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
        activePowerUp = PowerUp.NONE;
        tunicType = Tunic.GREEN;
        runningWithPegasusBoots = false;
        addHealthBuffer = 0;
        subtractHealthBuffer = 0;
        addRupeeBuffer = 0;
        rupeeUpdateDelay = 0;
        resourceSounds.clear();
        onLowHealth = false;
        timeToNextLowHealthSound = 0;
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
            InventoryItem.BOMBS.romValue(), InventoryItem.POWER_BRACELET.romValue(), InventoryItem.SHIELD.romValue(),
            InventoryItem.BOW.romValue(), InventoryItem.HOOKSHOT.romValue(), InventoryItem.MAGIC_ROD.romValue(),
            InventoryItem.PEGASUS_BOOTS.romValue(), InventoryItem.OCARINA.romValue(),
                    InventoryItem.ROCS_FEATHER.romValue(),
            InventoryItem.SHOVEL.romValue()
        });
        setItemA(InventoryItem.SWORD.romValue());
        setItemB(InventoryItem.BOOMERANG.romValue());
        setOcarinaSongFlags(OcarinaSong.FROGS_SONG_OF_THE_SOUL.romValue()
                | OcarinaSong.MANBO_MAMBO.romValue()
                | OcarinaSong.BALLAD_OF_THE_WIND_FISH.romValue());
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

    /** Prevent queued pickups/damage from undoing explicit debug resource edits. */
    public void clearResourceBuffersForDebug() {
        addHealthBuffer = 0;
        subtractHealthBuffer = 0;
        addRupeeBuffer = 0;
        rupeeUpdateDelay = 0;
        resourceSounds.clear();
        onLowHealth = false;
        timeToNextLowHealthSound = 0;
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

    private int deathCount;

    public int deathCount() {
        return deathCount;
    }

    public void setDeathCount(int value) {
        deathCount = clamp(value, 0, 999);
    }

    public void incrementDeathCount() {
        setDeathCount(deathCount + 1);
    }

    public int deathCountBcd() {
        return (deathCount / 100 << 8) | (deathCount / 10 % 10 << 4) | deathCount % 10;
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
        if (tunicType == Tunic.BLUE) {
            effectiveDamage >>= 1;
        } else if (activePowerUp == PowerUp.GUARDIAN_ACORN) {
            effectiveDamage = effectiveDamage == 0x04 ? 0 : effectiveDamage >> 1;
        }

        subtractHealthBuffer = (subtractHealthBuffer + effectiveDamage) & 0xFF;
        invincibilityCounter = 0x50;
        if (activePowerUp != PowerUp.NONE) {
            powerUpHits = (powerUpHits + 1) & 0xFF;
            if (powerUpHits >= 3) {
                activePowerUp = PowerUp.NONE;
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
    public void addSubtractHealthBuffer(int amount) {
        subtractHealthBuffer = (subtractHealthBuffer + amount) & 0xFF;
    }

    public int subtractHealthBuffer() {
        return subtractHealthBuffer;
    }

    /** Mirrors WarpState3Handler's direct clear of wSubtractHealthBuffer. */
    public void clearSubtractHealthBuffer() {
        subtractHealthBuffer = 0;
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
        }
        onLowHealth = health < LOW_HEALTH_THRESHOLDS[maxHearts];
        if (onLowHealth) {
            timeToNextLowHealthSound = (timeToNextLowHealthSound - 1) & 0xFF;
            if (timeToNextLowHealthSound == 0xFF) {
                resourceSounds.add(linksawakening.gameplay.GameplaySoundEvent.LOW_HEARTS);
                timeToNextLowHealthSound = 0x30;
            }
        }
        if ((frameCounter & 1) != 0) tickHealthBuffer();
    }

    /** Applies the immediate state portion of a bank3 pickable handler. */
    public void applyEntityPickup(int entityType) {
        switch (entityType & 0xFF) {
            case 0x2D -> addHealthBuffer = Math.min(0xFF, addHealthBuffer + HP_PER_HEART);
                // PickDroppableFairy ($03:64C6) adds six hearts to the byte buffer.
            case 0x2F -> addHealthBuffer = (addHealthBuffer + 0x30) & 0xFF;
            case 0x2E -> addRupeeBuffer = Math.min(0xFFFF, addRupeeBuffer + 1);
            case 0x33 -> setPowerUp(PowerUp.PIECE_OF_POWER);
            case 0x34 -> setPowerUp(PowerUp.GUARDIAN_ACORN);
            case 0x37 -> arrowCount = incrementUpTo(arrowCount, maxArrows);
            case 0x38 -> {
                giveInventoryItem(InventoryItem.BOMBS.romValue());
                bombCount = incrementUpTo(bombCount, maxBombs);
            }
            case 0x3B -> {
                giveInventoryItem(InventoryItem.MAGIC_POWDER.romValue());
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

    public void completeHeartPieceContainer() {
        heartPieces = 0;
        setMaxHearts(maxHearts + 1);
        addHealthBuffer = 0x40;
    }

    /** Applies HeartContainerEntityHandler's delayed health reward. */
    public void applyHeartContainerReward() {
        setMaxHearts(maxHearts + 1);
        addHealthBuffer = 0xFF;
    }

    /** Mirrors Madam MeowMeow's delayed wAddHealthBuffer=$FF kiss reward. */
    public void requestFullHeal() {
        addHealthBuffer = 0xFF;
    }

    /** Applies the delayed reward at the end of the beach-sword cutscene. */
    public void applyBeachSwordReward() {
        giveInventoryItem(InventoryItem.SWORD.romValue());
        setSwordLevel(1);
    }

    /** Applies SleepyToadstoolEntityHandler's final inventory and flag writes. */
    public void applyToadstoolReward() {
        giveInventoryItem(InventoryItem.MAGIC_POWDER.romValue());
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
            itemB = InventoryItem.EMPTY.romValue();
        } else if (inventorySlot == 1) {
            itemA = InventoryItem.EMPTY.romValue();
        } else {
            throw new IllegalArgumentException("Inventory slot must be B (0) or A (1): " + inventorySlot);
        }
        hasToadstool = false;
    }

    /** Applies the witch's AssignItemToSlot and BCD +$20 powder reward. */
    public void applyWitchMagicPowderReward() {
        giveInventoryItem(InventoryItem.MAGIC_POWDER.romValue());
        // The player model stores quantities as decimal values. WitchEntityHandler
        // adds BCD $20 with DAA and does not clamp against wMaxMagicPowder.
        magicPowderCount = Math.min(99, magicPowderCount + 20);
    }

    /** Applies the source-variant dispatch at FloatingItemEntityHandler. */
    public void applyFloatingItemPickup(int entityType, int sourceVariant) {
        switch (FloatingItemPickupEffect.pickupEffect(entityType, sourceVariant)) {
            case TEN_RUPEES -> addRupeeBuffer = 10;
            case MAGIC_POWDER -> {
                giveInventoryItem(InventoryItem.MAGIC_POWDER.romValue());
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
        if (chestItem >= ChestContentsTable.CHEST_RUPEES_50 && chestItem <= ChestContentsTable.CHEST_RUPEES_500)
            rupeeUpdateDelay = 0x18;
        if (chestItem < 0 || chestItem > ChestContentsTable.CHEST_ZOL) {
            throw new IllegalArgumentException("Chest item must be an unsigned chest variant: " + chestItem);
        }
        if (chestItem < chestItemCounts.length) {
            chestItemCounts[chestItem] = Math.min(0xFF, chestItemCounts[chestItem] + 1);
        }
        switch (chestItem) {
            case ChestContentsTable.CHEST_POWER_BRACELET -> {
                powerBraceletLevel = Math.min(2, powerBraceletLevel + 1);
                giveInventoryItem(InventoryItem.POWER_BRACELET.romValue());
            }
            case ChestContentsTable.CHEST_SHIELD -> {
                setShieldLevel(shieldLevel + 1);
                giveInventoryItem(InventoryItem.SHIELD.romValue());
            }
            case ChestContentsTable.CHEST_BOW -> giveInventoryItem(InventoryItem.BOW.romValue());
            case ChestContentsTable.CHEST_HOOKSHOT -> giveInventoryItem(InventoryItem.HOOKSHOT.romValue());
            case ChestContentsTable.CHEST_MAGIC_ROD -> giveInventoryItem(InventoryItem.MAGIC_ROD.romValue());
            case ChestContentsTable.CHEST_PEGASUS_BOOTS -> giveInventoryItem(InventoryItem.PEGASUS_BOOTS.romValue());
            case ChestContentsTable.CHEST_OCARINA -> giveInventoryItem(InventoryItem.OCARINA.romValue());
            case ChestContentsTable.CHEST_FEATHER -> giveInventoryItem(InventoryItem.ROCS_FEATHER.romValue());
            case ChestContentsTable.CHEST_SHOVEL -> giveInventoryItem(InventoryItem.SHOVEL.romValue());
            case ChestContentsTable.CHEST_FLIPPERS -> hasFlippers = true;
            case ChestContentsTable.CHEST_MAGIC_POWDER_BAG -> {
                giveInventoryItem(InventoryItem.MAGIC_POWDER.romValue());
                magicPowderCount = incrementUpTo(magicPowderCount, maxMagicPowder);
            }
            case ChestContentsTable.CHEST_BOMB -> {
                giveInventoryItem(InventoryItem.BOMBS.romValue());
                bombCount = incrementUpTo(bombCount, maxBombs);
            }
            case ChestContentsTable.CHEST_SWORD -> giveInventoryItem(InventoryItem.SWORD.romValue());
            case ChestContentsTable.CHEST_MEDICINE -> {
                hasMedicine = true;
                medicineCount = incrementByte(medicineCount);
            }
            case ChestContentsTable.CHEST_TAIL_KEY -> tailKeyCount = incrementByte(tailKeyCount);
            case ChestContentsTable.CHEST_ANGLER_KEY -> anglerKeyCount = incrementByte(anglerKeyCount);
            case ChestContentsTable.CHEST_FACE_KEY -> faceKeyCount = incrementByte(faceKeyCount);
            case ChestContentsTable.CHEST_BIRD_KEY -> birdKeyCount = incrementByte(birdKeyCount);
            case ChestContentsTable.CHEST_GOLD_LEAF -> goldenLeavesCount = incrementByte(goldenLeavesCount);
            case ChestContentsTable.CHEST_RUPEES_50 -> addRupeeBuffer = Math.min(0xFFFF, addRupeeBuffer + 50);
            case ChestContentsTable.CHEST_RUPEES_20 -> addRupeeBuffer = Math.min(0xFFFF, addRupeeBuffer + 20);
            case ChestContentsTable.CHEST_RUPEES_100 -> addRupeeBuffer = Math.min(0xFFFF, addRupeeBuffer + 100);
            case ChestContentsTable.CHEST_RUPEES_200 -> addRupeeBuffer = Math.min(0xFFFF, addRupeeBuffer + 200);
            case ChestContentsTable.CHEST_RUPEES_500 -> addRupeeBuffer = Math.min(0xFFFF, addRupeeBuffer + 500);
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
            if (itemA == InventoryItem.MAGIC_POWDER.romValue()) {
                itemA = InventoryItem.EMPTY.romValue();
            }
            if (itemB == InventoryItem.MAGIC_POWDER.romValue()) {
                itemB = InventoryItem.EMPTY.romValue();
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
        heartPieces = clamp(value, 0, 4);
    }

    public int seashells() {
        return seashells;
    }

    public void setSeashells(int value) {
        seashells = clamp(value, 0, 99);
    }

    public PowerUp powerUp() {
        return activePowerUp;
    }

    /** Starts a fresh power-up streak, including when selecting NONE. */
    public void setPowerUp(PowerUp value) {
        activePowerUp = Objects.requireNonNull(value, "value");
        powerUpHits = 0;
    }

    /** @deprecated Use {@link #powerUp()}. */
    @Deprecated(forRemoval = true)
    public int activePowerUp() {
        return powerUp().romValue();
    }

    /** @deprecated Use {@link #setPowerUp(PowerUp)}; this bridge retains legacy clamping. */
    @Deprecated(forRemoval = true)
    public void setActivePowerUp(int value) {
        setPowerUp(PowerUp.fromRom(clamp(value, 0, 2)));
    }

    /** Warp loads ($00:1904/$00:1952) clear only wActivePowerUp. */
    public void clearActivePowerUpForWarp() {
        activePowerUp = PowerUp.NONE;
    }

    /** Number of accepted enemy hits taken while the current power-up was active. */
    public int powerUpHits() {
        return powerUpHits;
    }

    public Tunic tunic() {
        return tunicType;
    }

    public void setTunic(Tunic value) {
        tunicType = Objects.requireNonNull(value, "value");
    }

    /** @deprecated Use {@link #tunic()}. */
    @Deprecated(forRemoval = true)
    public int tunicType() {
        return tunic().romValue();
    }

    /** @deprecated Use {@link #setTunic(Tunic)}; this bridge retains legacy clamping. */
    @Deprecated(forRemoval = true)
    public void setTunicType(int value) {
        setTunic(Tunic.fromRom(clamp(value, 0, 2)));
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
            throw new IllegalArgumentException(
                    "Expected " + SUBSCREEN_SLOT_COUNT + " subscreen items, got " + items.length);
        }
        for (int index = 0; index < items.length; index++) {
            subscreen[index] = items[index] & 0xFF;
        }
    }

    /** Applies the persistent fields currently represented by the Java model. */
    public void applySavedGame(SaveSlotState saved) {
        Objects.requireNonNull(saved, "saved");
        setMaxHearts(saved.maxHearts());
        int deaths = saved.deathCountBcd();
        setDeathCount((deaths >>> 8 & 15) * 100 + (deaths >>> 4 & 15) * 10 + (deaths & 15));
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
        // Source pickups may exceed normal capacity (floating arrows / witch powder).
        // A load copies those saved counts; it must not apply pickup-capacity clamps.
        setMaxArrows(saved.maxArrows());
        arrowCount = clamp(saved.arrowCount(), 0, 99);
        setMaxBombs(saved.maxBombs());
        bombCount = clamp(saved.bombCount(), 0, 99);
        setMaxMagicPowder(saved.maxMagicPowder());
        magicPowderCount = clamp(saved.magicPowderCount(), 0, 99);
        setHasToadstool(saved.hasToadstool() != 0);
        setOcarinaSongFlags(saved.ocarinaSongFlags());
        setSelectedSongIndex(saved.selectedSongIndex());
        setTunicType(saved.tunicType());

        invincibilityCounter = 0;
        activePowerUp = PowerUp.NONE;
        runningWithPegasusBoots = false;
        addHealthBuffer = 0;
        subtractHealthBuffer = 0;
        addRupeeBuffer = 0;
        rupeeUpdateDelay = 0;
        resourceSounds.clear();
        onLowHealth = false;
        timeToNextLowHealthSound = 0;
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
                if ((health & 7) == 6) {
                    resourceSounds.add(linksawakening.gameplay.GameplaySoundEvent.HEART_PICKED_UP);
                }
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
        // UpdateHealth.noHealth ($02:63A3): consume medicine on a lethal tick.
        if (health == 0 && hasMedicine) {
            medicineCount = Math.max(0, medicineCount - 1);
            hasMedicine = medicineCount != 0;
            health = HP_PER_HEART;
            addHealthBuffer = (addHealthBuffer + 0x80) & 0xFF;
            invincibilityCounter = 0xA0;
        }
    }

    private void tickRupeeBuffer() {
        if (rupeeUpdateDelay != 0) {
            rupeeUpdateDelay--;
            return;
        }
        if (addRupeeBuffer == 0) {
            return;
        }
        resourceSounds.add(linksawakening.gameplay.GameplaySoundEvent.RUPEE);
        int step = Math.min(9, (addRupeeBuffer >> 5) + 1);
        addRupeeBuffer -= step;
        rupees = Math.min(MAX_RUPEES, rupees + step);
        if (rupees == MAX_RUPEES) addRupeeBuffer = 0;
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
        if (itemB == InventoryItem.EMPTY.romValue()) {
            itemB = inventoryId;
            return;
        }
        if (itemA == InventoryItem.EMPTY.romValue()) {
            itemA = inventoryId;
            return;
        }
        for (int index = 0; index < subscreen.length; index++) {
            if (subscreen[index] == InventoryItem.EMPTY.romValue()) {
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
