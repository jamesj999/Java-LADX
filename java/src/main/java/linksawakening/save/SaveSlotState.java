package linksawakening.save;

import java.util.Objects;

/** Immutable view of the currently modeled fields in one raw SRAM slot. */
public record SaveSlotState(
    int slot,
    int[] nameBytes,
    int health,
    int maxHearts,
    int heartPieces,
    int rupees,
    int itemA,
    int itemB,
    int[] subscreen,
    int seashells,
    int shieldLevel,
    int swordLevel,
    int arrowCount,
    int maxArrows,
    int bombCount,
    int maxBombs,
    int magicPowderCount,
    int maxMagicPowder,
    int ocarinaSongFlags,
    int selectedSongIndex,
    int tunicType,
    int spawnIsIndoor,
    int spawnMapId,
    int spawnMapRoom,
    int spawnPositionX,
    int spawnPositionY,
    int spawnIndoorRoom,
    byte[] rawSlot
) {

    public SaveSlotState {
        SaveRamLayout.checkSlot(slot);
        nameBytes = copyExact(nameBytes, SaveRamLayout.NAME_LENGTH, "nameBytes");
        subscreen = copyExact(subscreen, SaveRamLayout.SUBSCREEN_SLOT_COUNT, "subscreen");
        rawSlot = copyExact(rawSlot, SaveRamLayout.SLOT_SIZE, "rawSlot");
    }

    @Override
    public int[] nameBytes() {
        return nameBytes.clone();
    }

    @Override
    public int[] subscreen() {
        return subscreen.clone();
    }

    @Override
    public byte[] rawSlot() {
        return rawSlot.clone();
    }

    private static int[] copyExact(int[] values, int expectedLength, String label) {
        Objects.requireNonNull(values, label);
        if (values.length != expectedLength) {
            throw new IllegalArgumentException(label + " must contain " + expectedLength
                + " bytes, got " + values.length);
        }
        return values.clone();
    }

    private static byte[] copyExact(byte[] values, int expectedLength, String label) {
        Objects.requireNonNull(values, label);
        if (values.length != expectedLength) {
            throw new IllegalArgumentException(label + " must contain " + expectedLength
                + " bytes, got " + values.length);
        }
        return values.clone();
    }
}
