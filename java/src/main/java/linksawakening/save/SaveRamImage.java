package linksawakening.save;

import java.util.Arrays;
import java.util.Objects;

/** Mutable raw image of the source-defined SRAM save area. */
public final class SaveRamImage {

    private final byte[] bytes;

    private SaveRamImage(byte[] bytes) {
        if (bytes.length != SaveRamLayout.IMAGE_SIZE) {
            throw new IllegalArgumentException("Save image must be exactly "
                + SaveRamLayout.IMAGE_SIZE + " bytes, got " + bytes.length);
        }
        this.bytes = bytes.clone();
    }

    public static SaveRamImage empty() {
        byte[] bytes = new byte[SaveRamLayout.IMAGE_SIZE];
        for (int slot = 0; slot < SaveRamLayout.SLOT_COUNT; slot++) {
            SaveRamLayout.writeValidPrefix(bytes, SaveRamLayout.slotOffset(slot));
        }
        return new SaveRamImage(bytes);
    }

    public static SaveRamImage fromBytes(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        return new SaveRamImage(bytes);
    }

    public byte[] bytes() {
        return bytes.clone();
    }

    /** Returns the file-menu mask derived from valid prefixes and nonzero names. */
    public int saveFilesMask() {
        int mask = 0;
        for (int slot = 0; slot < SaveRamLayout.SLOT_COUNT; slot++) {
            int slotOffset = SaveRamLayout.slotOffset(slot);
            if (!SaveRamLayout.hasValidPrefix(bytes, slotOffset)) {
                continue;
            }
            if (hasStoredName(slotOffset)) {
                mask |= 1 << slot;
            }
        }
        return mask;
    }

    public int[][] savedNames() {
        int[][] names = new int[SaveRamLayout.SLOT_COUNT][SaveRamLayout.NAME_LENGTH];
        for (int slot = 0; slot < SaveRamLayout.SLOT_COUNT; slot++) {
            int slotOffset = SaveRamLayout.slotOffset(slot);
            if (!SaveRamLayout.hasValidPrefix(bytes, slotOffset)) {
                continue;
            }
            int main = slotOffset + SaveRamLayout.mainOffset();
            for (int index = 0; index < SaveRamLayout.NAME_LENGTH; index++) {
                names[slot][index] = unsigned(bytes[main + SaveRamLayout.MAIN_NAME_OFFSET + index]);
            }
        }
        return names;
    }

    /** Writes the bytes emitted by the ROM's file-creation handler. */
    public void createNewGame(int slot, int[] nameBytes) {
        SaveRamLayout.checkSlot(slot);
        Objects.requireNonNull(nameBytes, "nameBytes");
        if (nameBytes.length != SaveRamLayout.NAME_LENGTH) {
            throw new IllegalArgumentException("nameBytes must contain "
                + SaveRamLayout.NAME_LENGTH + " bytes, got " + nameBytes.length);
        }
        for (int value : nameBytes) {
            if (value < 0 || value > 0xFF) {
                throw new IllegalArgumentException("Name byte outside unsigned byte range: " + value);
            }
        }

        int slotOffset = SaveRamLayout.slotOffset(slot);
        Arrays.fill(bytes, slotOffset, slotOffset + SaveRamLayout.SLOT_SIZE, (byte) 0);
        SaveRamLayout.writeValidPrefix(bytes, slotOffset);
        int main = slotOffset + SaveRamLayout.mainOffset();
        for (int index = 0; index < nameBytes.length; index++) {
            bytes[main + SaveRamLayout.MAIN_NAME_OFFSET + index] = (byte) nameBytes[index];
        }
        bytes[main + SaveRamLayout.MAIN_HEALTH_OFFSET] = 0x18;
        bytes[main + SaveRamLayout.MAIN_MAX_HEARTS_OFFSET] = 0x03;
        Arrays.fill(bytes, main + SaveRamLayout.MAIN_DEATH_COUNT_OFFSET,
            main + SaveRamLayout.MAIN_DEATH_COUNT_OFFSET + 3, (byte) 0);
    }

    public SaveSlotState readSlot(int slot) {
        SaveRamLayout.checkSlot(slot);
        int slotOffset = SaveRamLayout.slotOffset(slot);
        int main = slotOffset + SaveRamLayout.mainOffset();
        int[] nameBytes = new int[SaveRamLayout.NAME_LENGTH];
        for (int index = 0; index < nameBytes.length; index++) {
            nameBytes[index] = unsigned(bytes[main + SaveRamLayout.MAIN_NAME_OFFSET + index]);
        }
        int[] subscreen = new int[SaveRamLayout.SUBSCREEN_SLOT_COUNT];
        for (int index = 0; index < subscreen.length; index++) {
            subscreen[index] = unsigned(bytes[main + SaveRamLayout.MAIN_SUBSCREEN_OFFSET + index]);
        }
        byte[] rawSlot = Arrays.copyOfRange(bytes, slotOffset, slotOffset + SaveRamLayout.SLOT_SIZE);
        return new SaveSlotState(
            slot,
            nameBytes,
            unsigned(bytes[main + SaveRamLayout.MAIN_HEALTH_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_MAX_HEARTS_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_HEART_PIECES_OFFSET]),
            decodeRupees(bytes[main + SaveRamLayout.MAIN_RUPEE_HIGH_OFFSET],
                bytes[main + SaveRamLayout.MAIN_RUPEE_LOW_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_ITEM_A_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_ITEM_B_OFFSET]),
            subscreen,
            unsigned(bytes[main + SaveRamLayout.MAIN_SEASHELLS_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_SHIELD_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_SWORD_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_ARROWS_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_MAX_ARROWS_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_BOMBS_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_MAX_BOMBS_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_MAGIC_POWDER_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_MAX_MAGIC_POWDER_OFFSET]),
            unsigned(bytes[slotOffset + SaveRamLayout.dx3Offset() + SaveRamLayout.DX3_TUNIC_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_SPAWN_INDOOR_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_SPAWN_MAP_ID_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_SPAWN_MAP_ROOM_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_SPAWN_X_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_SPAWN_Y_OFFSET]),
            unsigned(bytes[main + SaveRamLayout.MAIN_SPAWN_INDOOR_ROOM_OFFSET]),
            rawSlot);
    }

    /** Mirrors InitSaveFiles for a host image with an invalid slot prefix. */
    void initializeInvalidSlots() {
        for (int slot = 0; slot < SaveRamLayout.SLOT_COUNT; slot++) {
            int slotOffset = SaveRamLayout.slotOffset(slot);
            if (SaveRamLayout.hasValidPrefix(bytes, slotOffset)) {
                continue;
            }
            Arrays.fill(bytes, slotOffset, slotOffset + SaveRamLayout.SLOT_SIZE, (byte) 0);
            SaveRamLayout.writeValidPrefix(bytes, slotOffset);
        }
    }

    private boolean hasStoredName(int slotOffset) {
        int main = slotOffset + SaveRamLayout.mainOffset();
        for (int index = 0; index < SaveRamLayout.NAME_LENGTH; index++) {
            if (bytes[main + SaveRamLayout.MAIN_NAME_OFFSET + index] != 0) {
                return true;
            }
        }
        return false;
    }

    private static int decodeRupees(byte high, byte low) {
        int hundreds = unsigned(high) & 0x0F;
        int tens = (unsigned(low) >>> 4) & 0x0F;
        int ones = unsigned(low) & 0x0F;
        return Math.min(999, hundreds * 100 + tens * 10 + ones);
    }

    private static int unsigned(byte value) {
        return value & 0xFF;
    }
}
