package linksawakening.save;

/**
 * ROM-derived offsets for the three save slots in the SRAM image.
 *
 * <p>The image starts at the first byte backed by the source's SRAM section
 * after the unused {@code $100} byte prefix.  It is intentionally not a CPU
 * address-space model: offsets here are file offsets.</p>
 */
public final class SaveRamLayout {

    public static final int SRAM_IMAGE_OFFSET = 0x100;
    public static final int SLOT_COUNT = 3;
    public static final int NAME_LENGTH = 5;
    public static final int SUBSCREEN_SLOT_COUNT = 10;

    public static final int PREFIX_SIZE = 0x05;
    public static final int MAIN_SIZE = 0x0380;
    public static final int DX1_SIZE = 0x0005;
    public static final int DX2_SIZE = 0x0020;
    public static final int DX3_SIZE = 0x0003;
    public static final int SLOT_SIZE = PREFIX_SIZE + MAIN_SIZE + DX1_SIZE + DX2_SIZE + DX3_SIZE;
    public static final int IMAGE_SIZE = SRAM_IMAGE_OFFSET + SLOT_COUNT * SLOT_SIZE;

    // Offsets within the main WRAM save block, derived from wOverworldRoomStatus=$D800.
    public static final int MAIN_ITEM_B_OFFSET = 0x300;
    public static final int MAIN_ITEM_A_OFFSET = 0x301;
    public static final int MAIN_SUBSCREEN_OFFSET = 0x302;
    public static final int MAIN_SEASHELLS_OFFSET = 0x30F;
    public static final int MAIN_SHIELD_OFFSET = 0x344;
    public static final int MAIN_ARROWS_OFFSET = 0x345;
    public static final int MAIN_MAGIC_POWDER_OFFSET = 0x34C;
    public static final int MAIN_BOMBS_OFFSET = 0x34D;
    public static final int MAIN_SWORD_OFFSET = 0x34E;
    public static final int MAIN_NAME_OFFSET = 0x34F;
    public static final int MAIN_DEATH_COUNT_OFFSET = 0x357;
    public static final int MAIN_HEALTH_OFFSET = 0x35A;
    public static final int MAIN_MAX_HEARTS_OFFSET = 0x35B;
    public static final int MAIN_HEART_PIECES_OFFSET = 0x35C;
    public static final int MAIN_RUPEE_HIGH_OFFSET = 0x35D;
    public static final int MAIN_RUPEE_LOW_OFFSET = 0x35E;
    public static final int MAIN_SPAWN_INDOOR_OFFSET = 0x35F;
    public static final int MAIN_SPAWN_MAP_ID_OFFSET = 0x360;
    public static final int MAIN_SPAWN_MAP_ROOM_OFFSET = 0x361;
    public static final int MAIN_SPAWN_X_OFFSET = 0x362;
    public static final int MAIN_SPAWN_Y_OFFSET = 0x363;
    public static final int MAIN_SPAWN_INDOOR_ROOM_OFFSET = 0x364;
    public static final int MAIN_MAX_MAGIC_POWDER_OFFSET = 0x376;
    public static final int MAIN_MAX_BOMBS_OFFSET = 0x377;
    public static final int MAIN_MAX_ARROWS_OFFSET = 0x378;

    // DX3's first byte is wTunicType in the current Java model.
    public static final int DX3_TUNIC_OFFSET = 0x00;

    private static final byte[] VALID_PREFIX = {1, 3, 5, 7, 9};

    private SaveRamLayout() {
    }

    public static int slotOffset(int slot) {
        checkSlot(slot);
        return SRAM_IMAGE_OFFSET + slot * SLOT_SIZE;
    }

    public static int mainOffset() {
        return PREFIX_SIZE;
    }

    public static int dx1Offset() {
        return PREFIX_SIZE + MAIN_SIZE;
    }

    public static int dx2Offset() {
        return dx1Offset() + DX1_SIZE;
    }

    public static int dx3Offset() {
        return dx2Offset() + DX2_SIZE;
    }

    static boolean hasValidPrefix(byte[] image, int slotOffset) {
        for (int index = 0; index < VALID_PREFIX.length; index++) {
            if (image[slotOffset + index] != VALID_PREFIX[index]) {
                return false;
            }
        }
        return true;
    }

    static void writeValidPrefix(byte[] image, int slotOffset) {
        System.arraycopy(VALID_PREFIX, 0, image, slotOffset, VALID_PREFIX.length);
    }

    static void checkSlot(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            throw new IllegalArgumentException("Save slot must be 0..2: " + slot);
        }
    }
}
