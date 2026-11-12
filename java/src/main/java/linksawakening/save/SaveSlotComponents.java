package linksawakening.save;

import java.util.Objects;

/** Cohesive groups of modeled SRAM fields; raw unmodeled bytes remain in the slot image. */
public final class SaveSlotComponents {
    private SaveSlotComponents() {}
    /** Health, heart pieces, currency and seashell counters. */
    public record Resources(int health, int maxHearts, int heartPieces, int rupees, int seashells) {}
    /** Equipped items, backpack slots and equipment levels. */
    public record Inventory(
            int itemA, int itemB, int[] subscreen, int powerBraceletLevel, int shieldLevel, int swordLevel) {
        public Inventory {
            subscreen = SaveSlotState.copyExact(subscreen, SaveRamLayout.SUBSCREEN_SLOT_COUNT, "subscreen");
        }

        @Override
        public int[] subscreen() {
            return subscreen.clone();
        }
    }
    /** Current and maximum counts for arrows, bombs and powder. */
    public record Ammunition(
            int arrowCount, int maxArrows, int bombCount, int maxBombs, int magicPowderCount, int maxMagicPowder) {}
    /** Persistent ability, trade, medicine and tunic fields. */
    public record Quest(
            int hasFlippers,
            int hasMedicine,
            int tradeSequenceItem,
            int medicineCount,
            int hasToadstool,
            int tunicType) {}
    /** Overworld key and golden-leaf counters. */
    public record Keys(
            int tailKeyCount, int anglerKeyCount, int faceKeyCount, int birdKeyCount, int goldenLeavesCount) {}
    /** Ocarina song flags and selected song index. */
    public record Songs(int ocarinaSongFlags, int selectedSongIndex) {}
    /** Source-shaped spawn location bytes. */
    public record Spawn(
            int spawnIsIndoor,
            int spawnMapId,
            int spawnMapRoom,
            int spawnPositionX,
            int spawnPositionY,
            int spawnIndoorRoom) {}
    /** Quest flags, overworld keys and ocarina progression. */
    public record Progression(Quest quest, Keys keys, Songs songs) {
        public Progression {
            Objects.requireNonNull(quest, "quest");
            Objects.requireNonNull(keys, "keys");
            Objects.requireNonNull(songs, "songs");
        }
    }
}
