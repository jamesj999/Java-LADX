package linksawakening.save;

import java.util.Arrays;
import java.util.Objects;
import linksawakening.state.PlayerState;

/** Encodes modeled player fields and decodes source-shaped SRAM slots. */
final class SaveSlotCodec {
    private final byte[] bytes;

    SaveSlotCodec(byte[] bytes) {
        this.bytes = bytes;
    }

    public void writePlayerState(int slot, PlayerState playerState) {
        SaveRamLayout.checkSlot(slot);
        Objects.requireNonNull(playerState, "playerState");
        int slotOffset = SaveRamLayout.slotOffset(slot);
        int main = slotOffset + SaveRamLayout.mainOffset();

        int deaths = playerState.deathCountBcd();
        bytes[main + SaveRamLayout.MAIN_DEATH_COUNT_OFFSET] = (byte) deaths;
        bytes[main + SaveRamLayout.MAIN_DEATH_COUNT_OFFSET + 1] = (byte) (deaths >>> 8);
        bytes[main + SaveRamLayout.MAIN_ITEM_B_OFFSET] = (byte) playerState.itemB();
        bytes[main + SaveRamLayout.MAIN_ITEM_A_OFFSET] = (byte) playerState.itemA();
        for (int index = 0; index < SaveRamLayout.SUBSCREEN_SLOT_COUNT; index++) {
            bytes[main + SaveRamLayout.MAIN_SUBSCREEN_OFFSET + index] = (byte) playerState.subscreenItem(index);
        }
        bytes[main + SaveRamLayout.MAIN_FLIPPERS_OFFSET] = (byte) (playerState.hasFlippers() ? 1 : 0);
        bytes[main + SaveRamLayout.MAIN_MEDICINE_OFFSET] = (byte) (playerState.hasMedicine() ? 1 : 0);
        bytes[main + SaveRamLayout.MAIN_TRADE_SEQUENCE_ITEM_OFFSET] = (byte) playerState.tradeSequenceItem();
        bytes[main + SaveRamLayout.MAIN_SEASHELLS_OFFSET] = (byte) encodeBcd(playerState.seashells());
        bytes[main + SaveRamLayout.MAIN_MEDICINE_COUNT_OFFSET] = (byte) playerState.medicineCount();
        bytes[main + SaveRamLayout.MAIN_TAIL_KEY_OFFSET] = (byte) playerState.tailKeyCount();
        bytes[main + SaveRamLayout.MAIN_ANGLER_KEY_OFFSET] = (byte) playerState.anglerKeyCount();
        bytes[main + SaveRamLayout.MAIN_FACE_KEY_OFFSET] = (byte) playerState.faceKeyCount();
        bytes[main + SaveRamLayout.MAIN_BIRD_KEY_OFFSET] = (byte) playerState.birdKeyCount();
        bytes[main + SaveRamLayout.MAIN_GOLDEN_LEAVES_OFFSET] = (byte) playerState.goldenLeavesCount();
        bytes[main + SaveRamLayout.MAIN_POWER_BRACELET_OFFSET] = (byte) playerState.powerBraceletLevel();
        bytes[main + SaveRamLayout.MAIN_SHIELD_OFFSET] = (byte) playerState.shieldLevel();
        bytes[main + SaveRamLayout.MAIN_ARROWS_OFFSET] = (byte) encodeBcd(playerState.arrowCount());
        bytes[main + SaveRamLayout.MAIN_MAGIC_POWDER_OFFSET] = (byte) encodeBcd(playerState.magicPowderCount());
        bytes[main + SaveRamLayout.MAIN_HAS_TOADSTOOL_OFFSET] = (byte) (playerState.hasToadstool() ? 1 : 0);
        bytes[main + SaveRamLayout.MAIN_BOMBS_OFFSET] = (byte) encodeBcd(playerState.bombCount());
        bytes[main + SaveRamLayout.MAIN_SWORD_OFFSET] = (byte) playerState.swordLevel();
        bytes[main + SaveRamLayout.MAIN_HEALTH_OFFSET] = (byte) savedHealth(playerState);
        bytes[main + SaveRamLayout.MAIN_MAX_HEARTS_OFFSET] = (byte) playerState.maxHearts();
        bytes[main + SaveRamLayout.MAIN_HEART_PIECES_OFFSET] = (byte) playerState.heartPieces();
        writeRupees(main, playerState.rupees());
        bytes[main + SaveRamLayout.MAIN_MAX_MAGIC_POWDER_OFFSET] = (byte) encodeBcd(playerState.maxMagicPowder());
        bytes[main + SaveRamLayout.MAIN_MAX_BOMBS_OFFSET] = (byte) encodeBcd(playerState.maxBombs());
        bytes[main + SaveRamLayout.MAIN_MAX_ARROWS_OFFSET] = (byte) encodeBcd(playerState.maxArrows());
        bytes[main + SaveRamLayout.MAIN_OCARINA_SONG_FLAGS_OFFSET] = (byte) (playerState.ocarinaSongFlags() & 0x07);
        bytes[main + SaveRamLayout.MAIN_SELECTED_SONG_INDEX_OFFSET] =
                (byte) Math.max(0, Math.min(2, playerState.selectedSongIndex()));
        bytes[slotOffset + SaveRamLayout.dx3Offset() + SaveRamLayout.DX3_TUNIC_OFFSET] = (byte) playerState.tunicType();
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
                decodeRupees(
                        bytes[main + SaveRamLayout.MAIN_RUPEE_HIGH_OFFSET],
                        bytes[main + SaveRamLayout.MAIN_RUPEE_LOW_OFFSET]),
                unsigned(bytes[main + SaveRamLayout.MAIN_ITEM_A_OFFSET]),
                unsigned(bytes[main + SaveRamLayout.MAIN_ITEM_B_OFFSET]),
                subscreen,
                decodeBcd(bytes[main + SaveRamLayout.MAIN_SEASHELLS_OFFSET]),
                unsigned(bytes[main + SaveRamLayout.MAIN_FLIPPERS_OFFSET]),
                unsigned(bytes[main + SaveRamLayout.MAIN_MEDICINE_OFFSET]),
                unsigned(bytes[main + SaveRamLayout.MAIN_TRADE_SEQUENCE_ITEM_OFFSET]),
                unsigned(bytes[main + SaveRamLayout.MAIN_MEDICINE_COUNT_OFFSET]),
                unsigned(bytes[main + SaveRamLayout.MAIN_TAIL_KEY_OFFSET]),
                unsigned(bytes[main + SaveRamLayout.MAIN_ANGLER_KEY_OFFSET]),
                unsigned(bytes[main + SaveRamLayout.MAIN_FACE_KEY_OFFSET]),
                unsigned(bytes[main + SaveRamLayout.MAIN_BIRD_KEY_OFFSET]),
                unsigned(bytes[main + SaveRamLayout.MAIN_GOLDEN_LEAVES_OFFSET]),
                unsigned(bytes[main + SaveRamLayout.MAIN_POWER_BRACELET_OFFSET]),
                unsigned(bytes[main + SaveRamLayout.MAIN_SHIELD_OFFSET]),
                unsigned(bytes[main + SaveRamLayout.MAIN_SWORD_OFFSET]),
                decodeBcd(bytes[main + SaveRamLayout.MAIN_ARROWS_OFFSET]),
                decodeBcd(bytes[main + SaveRamLayout.MAIN_MAX_ARROWS_OFFSET]),
                decodeBcd(bytes[main + SaveRamLayout.MAIN_BOMBS_OFFSET]),
                decodeBcd(bytes[main + SaveRamLayout.MAIN_MAX_BOMBS_OFFSET]),
                decodeBcd(bytes[main + SaveRamLayout.MAIN_MAGIC_POWDER_OFFSET]),
                decodeBcd(bytes[main + SaveRamLayout.MAIN_MAX_MAGIC_POWDER_OFFSET]),
                unsigned(bytes[main + SaveRamLayout.MAIN_OCARINA_SONG_FLAGS_OFFSET]),
                unsigned(bytes[main + SaveRamLayout.MAIN_SELECTED_SONG_INDEX_OFFSET]),
                unsigned(bytes[main + SaveRamLayout.MAIN_HAS_TOADSTOOL_OFFSET]),
                unsigned(bytes[slotOffset + SaveRamLayout.dx3Offset() + SaveRamLayout.DX3_TUNIC_OFFSET]),
                unsigned(bytes[main + SaveRamLayout.MAIN_SPAWN_INDOOR_OFFSET]),
                unsigned(bytes[main + SaveRamLayout.MAIN_SPAWN_MAP_ID_OFFSET]),
                unsigned(bytes[main + SaveRamLayout.MAIN_SPAWN_MAP_ROOM_OFFSET]),
                unsigned(bytes[main + SaveRamLayout.MAIN_SPAWN_X_OFFSET]),
                unsigned(bytes[main + SaveRamLayout.MAIN_SPAWN_Y_OFFSET]),
                unsigned(bytes[main + SaveRamLayout.MAIN_SPAWN_INDOOR_ROOM_OFFSET]),
                rawSlot);
    }

    private static int decodeBcd(byte value) {
        return (unsigned(value) >>> 4) * 10 + (unsigned(value) & 0x0F);
    }

    private static int encodeBcd(int value) {
        return ((value / 10) << 4) | (value % 10);
    }

    private static int decodeRupees(byte high, byte low) {
        int hundreds = unsigned(high) & 0x0F;
        int tens = (unsigned(low) >>> 4) & 0x0F;
        int ones = unsigned(low) & 0x0F;
        return Math.min(999, hundreds * 100 + tens * 10 + ones);
    }

    private void writeRupees(int main, int rupees) {
        int value = Math.max(0, Math.min(PlayerState.MAX_RUPEES, rupees));
        bytes[main + SaveRamLayout.MAIN_RUPEE_HIGH_OFFSET] = (byte) (value / 100);
        bytes[main + SaveRamLayout.MAIN_RUPEE_LOW_OFFSET] = (byte) ((((value / 10) % 10) << 4) | (value % 10));
    }

    private static int savedHealth(PlayerState playerState) {
        if (playerState.health() != 0) {
            return playerState.health();
        }
        int[] startingHealthByMaxHearts = {
            3 * PlayerState.HP_PER_HEART,
            3 * PlayerState.HP_PER_HEART,
            3 * PlayerState.HP_PER_HEART,
            3 * PlayerState.HP_PER_HEART,
            3 * PlayerState.HP_PER_HEART,
            3 * PlayerState.HP_PER_HEART,
            5 * PlayerState.HP_PER_HEART,
            5 * PlayerState.HP_PER_HEART,
            5 * PlayerState.HP_PER_HEART,
            5 * PlayerState.HP_PER_HEART,
            7 * PlayerState.HP_PER_HEART,
            7 * PlayerState.HP_PER_HEART,
            7 * PlayerState.HP_PER_HEART,
            7 * PlayerState.HP_PER_HEART,
            10 * PlayerState.HP_PER_HEART
        };
        return startingHealthByMaxHearts[
                Math.min(Math.max(playerState.maxHearts(), 0), startingHealthByMaxHearts.length - 1)];
    }

    static int unsigned(byte value) {
        return value & 0xFF;
    }
}
