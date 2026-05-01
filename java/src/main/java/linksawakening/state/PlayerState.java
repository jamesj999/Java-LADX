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
    private int itemA = INVENTORY_SWORD;
    private int itemB = INVENTORY_EMPTY;
    private final int[] subscreen = new int[SUBSCREEN_SLOT_COUNT];

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

    public int swordLevel() {
        return swordLevel;
    }

    public void setSwordLevel(int value) {
        swordLevel = clamp(value, 0, 2);
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

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
