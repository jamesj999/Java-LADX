package linksawakening.save;

import java.util.Arrays;
import java.util.Objects;
import linksawakening.save.SaveSlotComponents.*;
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

        int deaths = playerState.progression().deathCountBcd();
        bytes[main + SaveRamLayout.MAIN_DEATH_COUNT_OFFSET] = (byte) deaths;
        bytes[main + SaveRamLayout.MAIN_DEATH_COUNT_OFFSET + 1] = (byte) (deaths >>> 8);
        bytes[main + SaveRamLayout.MAIN_ITEM_B_OFFSET] =
                (byte) playerState.inventory().itemB();
        bytes[main + SaveRamLayout.MAIN_ITEM_A_OFFSET] =
                (byte) playerState.inventory().itemA();
        for (int index = 0; index < SaveRamLayout.SUBSCREEN_SLOT_COUNT; index++) {
            bytes[main + SaveRamLayout.MAIN_SUBSCREEN_OFFSET + index] =
                    (byte) playerState.inventory().subscreenItem(index);
        }
        bytes[main + SaveRamLayout.MAIN_FLIPPERS_OFFSET] =
                (byte) (playerState.progression().hasFlippers() ? 1 : 0);
        bytes[main + SaveRamLayout.MAIN_MEDICINE_OFFSET] =
                (byte) (playerState.progression().hasMedicine() ? 1 : 0);
        bytes[main + SaveRamLayout.MAIN_TRADE_SEQUENCE_ITEM_OFFSET] =
                (byte) playerState.progression().tradeSequenceItem();
        bytes[main + SaveRamLayout.MAIN_SEASHELLS_OFFSET] =
                (byte) encodeBcd(playerState.progression().seashells());
        bytes[main + SaveRamLayout.MAIN_MEDICINE_COUNT_OFFSET] =
                (byte) playerState.progression().medicineCount();
        bytes[main + SaveRamLayout.MAIN_TAIL_KEY_OFFSET] =
                (byte) playerState.progression().tailKeyCount();
        bytes[main + SaveRamLayout.MAIN_ANGLER_KEY_OFFSET] =
                (byte) playerState.progression().anglerKeyCount();
        bytes[main + SaveRamLayout.MAIN_FACE_KEY_OFFSET] =
                (byte) playerState.progression().faceKeyCount();
        bytes[main + SaveRamLayout.MAIN_BIRD_KEY_OFFSET] =
                (byte) playerState.progression().birdKeyCount();
        bytes[main + SaveRamLayout.MAIN_GOLDEN_LEAVES_OFFSET] =
                (byte) playerState.progression().goldenLeavesCount();
        bytes[main + SaveRamLayout.MAIN_POWER_BRACELET_OFFSET] =
                (byte) playerState.equipmentLevels().powerBraceletLevel();
        bytes[main + SaveRamLayout.MAIN_SHIELD_OFFSET] =
                (byte) playerState.equipmentLevels().shieldLevel();
        bytes[main + SaveRamLayout.MAIN_ARROWS_OFFSET] =
                (byte) encodeBcd(playerState.ammunition().arrowCount());
        bytes[main + SaveRamLayout.MAIN_MAGIC_POWDER_OFFSET] =
                (byte) encodeBcd(playerState.ammunition().magicPowderCount());
        bytes[main + SaveRamLayout.MAIN_HAS_TOADSTOOL_OFFSET] =
                (byte) (playerState.ammunition().hasToadstool() ? 1 : 0);
        bytes[main + SaveRamLayout.MAIN_BOMBS_OFFSET] =
                (byte) encodeBcd(playerState.ammunition().bombCount());
        bytes[main + SaveRamLayout.MAIN_SWORD_OFFSET] =
                (byte) playerState.equipmentLevels().swordLevel();
        bytes[main + SaveRamLayout.MAIN_HEALTH_OFFSET] = (byte) savedHealth(playerState);
        bytes[main + SaveRamLayout.MAIN_MAX_HEARTS_OFFSET] =
                (byte) playerState.vitals().maxHearts();
        bytes[main + SaveRamLayout.MAIN_HEART_PIECES_OFFSET] =
                (byte) playerState.progression().heartPieces();
        writeRupees(main, playerState.wallet().rupees());
        bytes[main + SaveRamLayout.MAIN_MAX_MAGIC_POWDER_OFFSET] =
                (byte) encodeBcd(playerState.ammunition().maxMagicPowder());
        bytes[main + SaveRamLayout.MAIN_MAX_BOMBS_OFFSET] =
                (byte) encodeBcd(playerState.ammunition().maxBombs());
        bytes[main + SaveRamLayout.MAIN_MAX_ARROWS_OFFSET] =
                (byte) encodeBcd(playerState.ammunition().maxArrows());
        bytes[main + SaveRamLayout.MAIN_OCARINA_SONG_FLAGS_OFFSET] =
                (byte) (playerState.progression().ocarinaSongFlags() & 0x07);
        bytes[main + SaveRamLayout.MAIN_SELECTED_SONG_INDEX_OFFSET] =
                (byte) Math.max(0, Math.min(2, playerState.progression().selectedSongIndex()));
        bytes[slotOffset + SaveRamLayout.dx3Offset() + SaveRamLayout.DX3_TUNIC_OFFSET] =
                (byte) playerState.equipmentLevels().tunicType();
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
        return SaveSlotState.builder(slot, nameBytes, rawSlot)
                .resources(new Resources(
                        unsigned(bytes[main + SaveRamLayout.MAIN_HEALTH_OFFSET]),
                        unsigned(bytes[main + SaveRamLayout.MAIN_MAX_HEARTS_OFFSET]),
                        unsigned(bytes[main + SaveRamLayout.MAIN_HEART_PIECES_OFFSET]),
                        decodeRupees(
                                bytes[main + SaveRamLayout.MAIN_RUPEE_HIGH_OFFSET],
                                bytes[main + SaveRamLayout.MAIN_RUPEE_LOW_OFFSET]),
                        decodeBcd(bytes[main + SaveRamLayout.MAIN_SEASHELLS_OFFSET])))
                .inventory(new Inventory(
                        unsigned(bytes[main + SaveRamLayout.MAIN_ITEM_A_OFFSET]),
                        unsigned(bytes[main + SaveRamLayout.MAIN_ITEM_B_OFFSET]),
                        subscreen,
                        unsigned(bytes[main + SaveRamLayout.MAIN_POWER_BRACELET_OFFSET]),
                        unsigned(bytes[main + SaveRamLayout.MAIN_SHIELD_OFFSET]),
                        unsigned(bytes[main + SaveRamLayout.MAIN_SWORD_OFFSET])))
                .ammunition(new Ammunition(
                        decodeBcd(bytes[main + SaveRamLayout.MAIN_ARROWS_OFFSET]),
                        decodeBcd(bytes[main + SaveRamLayout.MAIN_MAX_ARROWS_OFFSET]),
                        decodeBcd(bytes[main + SaveRamLayout.MAIN_BOMBS_OFFSET]),
                        decodeBcd(bytes[main + SaveRamLayout.MAIN_MAX_BOMBS_OFFSET]),
                        decodeBcd(bytes[main + SaveRamLayout.MAIN_MAGIC_POWDER_OFFSET]),
                        decodeBcd(bytes[main + SaveRamLayout.MAIN_MAX_MAGIC_POWDER_OFFSET])))
                .progression(new Progression(
                        new Quest(
                                unsigned(bytes[main + SaveRamLayout.MAIN_FLIPPERS_OFFSET]),
                                unsigned(bytes[main + SaveRamLayout.MAIN_MEDICINE_OFFSET]),
                                unsigned(bytes[main + SaveRamLayout.MAIN_TRADE_SEQUENCE_ITEM_OFFSET]),
                                unsigned(bytes[main + SaveRamLayout.MAIN_MEDICINE_COUNT_OFFSET]),
                                unsigned(bytes[main + SaveRamLayout.MAIN_HAS_TOADSTOOL_OFFSET]),
                                unsigned(bytes[
                                        slotOffset + SaveRamLayout.dx3Offset() + SaveRamLayout.DX3_TUNIC_OFFSET])),
                        new Keys(
                                unsigned(bytes[main + SaveRamLayout.MAIN_TAIL_KEY_OFFSET]),
                                unsigned(bytes[main + SaveRamLayout.MAIN_ANGLER_KEY_OFFSET]),
                                unsigned(bytes[main + SaveRamLayout.MAIN_FACE_KEY_OFFSET]),
                                unsigned(bytes[main + SaveRamLayout.MAIN_BIRD_KEY_OFFSET]),
                                unsigned(bytes[main + SaveRamLayout.MAIN_GOLDEN_LEAVES_OFFSET])),
                        new Songs(
                                unsigned(bytes[main + SaveRamLayout.MAIN_OCARINA_SONG_FLAGS_OFFSET]),
                                unsigned(bytes[main + SaveRamLayout.MAIN_SELECTED_SONG_INDEX_OFFSET]))))
                .spawn(new Spawn(
                        unsigned(bytes[main + SaveRamLayout.MAIN_SPAWN_INDOOR_OFFSET]),
                        unsigned(bytes[main + SaveRamLayout.MAIN_SPAWN_MAP_ID_OFFSET]),
                        unsigned(bytes[main + SaveRamLayout.MAIN_SPAWN_MAP_ROOM_OFFSET]),
                        unsigned(bytes[main + SaveRamLayout.MAIN_SPAWN_X_OFFSET]),
                        unsigned(bytes[main + SaveRamLayout.MAIN_SPAWN_Y_OFFSET]),
                        unsigned(bytes[main + SaveRamLayout.MAIN_SPAWN_INDOOR_ROOM_OFFSET])))
                .build();
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
        if (playerState.vitals().health() != 0) {
            return playerState.vitals().health();
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
                Math.min(Math.max(playerState.vitals().maxHearts(), 0), startingHealthByMaxHearts.length - 1)];
    }

    static int unsigned(byte value) {
        return value & 0xFF;
    }
}
