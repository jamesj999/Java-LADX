package linksawakening.state;

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
    private int itemA = INVENTORY_SWORD;
    private int itemB = INVENTORY_EMPTY;
    private final int[] subscreen = new int[SUBSCREEN_SLOT_COUNT];
    private int arrowCount;
    private int maxArrows;
    private int bombCount;
    private int maxBombs;
    private int magicPowderCount;
    private int maxMagicPowder;
    private int heartPieces;
    private int seashells;
    private int activePowerUp;
    private int tunicType = TUNIC_GREEN;
    private boolean runningWithPegasusBoots;
    private int addHealthBuffer;
    private int addRupeeBuffer;

    public PlayerState() {
        // Stub test data so the inventory menu has something to equip until
        // the real pickup/chest flow exists. Fills slots with the inventory
        // ids BOMBS..SHOVEL in order.
        for (int i = 0; i < subscreen.length; i++) {
            subscreen[i] = INVENTORY_BOMBS + i;
        }
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
            case 0x33 -> activePowerUp = 1;
            case 0x34 -> activePowerUp = 2;
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

    public int seashells() {
        return seashells;
    }

    public int activePowerUp() {
        return activePowerUp;
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
        if (addHealthBuffer == 0) {
            return;
        }
        int maxHealth = maxHearts * HP_PER_HEART;
        if (health >= maxHealth) {
            addHealthBuffer = 0;
            return;
        }
        health++;
        addHealthBuffer--;
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

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
