package linksawakening.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DungeonItemStateTest {

    @Test
    void resetClearsPersistentAndCurrentDungeonState() {
        DungeonItemState state = new DungeonItemState();
        byte[] dungeonFlags = new byte[DungeonItemState.DUNGEON_ITEM_FLAGS_SIZE];
        dungeonFlags[DungeonItemState.SMALL_KEYS_INDEX] = 3;
        state.restore(dungeonFlags, new byte[DungeonItemState.COLOR_DUNGEON_ITEM_FLAGS_SIZE]);
        state.loadForMap(0, true);

        state.reset();

        assertArrayEquals(new byte[DungeonItemState.DUNGEON_ITEM_FLAGS_SIZE],
            state.dungeonItemFlagsSnapshot());
        assertArrayEquals(new byte[DungeonItemState.ITEM_FLAG_SIZE],
            state.currentFlagsSnapshot());
    }

    @Test
    void loadsThePersistentFiveByteEntryForAnOrdinaryDungeon() {
        DungeonItemState state = new DungeonItemState();
        byte[] dungeonFlags = new byte[DungeonItemState.DUNGEON_ITEM_FLAGS_SIZE];
        dungeonFlags[2 * DungeonItemState.ITEM_FLAG_SIZE] = 1;
        dungeonFlags[2 * DungeonItemState.ITEM_FLAG_SIZE + 4] = 3;
        state.restore(dungeonFlags, new byte[DungeonItemState.COLOR_DUNGEON_ITEM_FLAGS_SIZE]);

        state.loadForMap(2, true);

        assertArrayEquals(new byte[] {1, 0, 0, 0, 3}, state.currentFlagsSnapshot());
    }

    @Test
    void loadsColorDungeonFlagsFromItsSeparatePersistentBlock() {
        DungeonItemState state = new DungeonItemState();
        byte[] colorFlags = {1, 1, 0, 1, 2};
        state.restore(new byte[DungeonItemState.DUNGEON_ITEM_FLAGS_SIZE], colorFlags);

        state.loadForMap(DungeonItemState.MAP_COLOR_DUNGEON, true);

        assertArrayEquals(colorFlags, state.currentFlagsSnapshot());
    }

    @Test
    void windFishEggAndNonDungeonIndoorMapsExposeZeroCurrentDungeonFlags() {
        DungeonItemState state = new DungeonItemState();
        byte[] dungeonFlags = new byte[DungeonItemState.DUNGEON_ITEM_FLAGS_SIZE];
        dungeonFlags[0] = 1;
        state.restore(dungeonFlags, new byte[DungeonItemState.COLOR_DUNGEON_ITEM_FLAGS_SIZE]);

        state.loadForMap(DungeonItemState.MAP_WINDFISHS_EGG, true);
        assertArrayEquals(new byte[DungeonItemState.ITEM_FLAG_SIZE], state.currentFlagsSnapshot());

        state.loadForMap(0x0A, true);
        assertArrayEquals(new byte[DungeonItemState.ITEM_FLAG_SIZE], state.currentFlagsSnapshot());
    }

    @Test
    void synchronizingCurrentFlagsWritesBackToTheSelectedPersistentEntry() {
        DungeonItemState state = new DungeonItemState();
        state.restore(new byte[DungeonItemState.DUNGEON_ITEM_FLAGS_SIZE],
            new byte[DungeonItemState.COLOR_DUNGEON_ITEM_FLAGS_SIZE]);
        state.loadForMap(1, true);

        state.incrementCurrentFlag(DungeonItemState.CHEST_SMALL_KEY);
        state.incrementCurrentFlag(DungeonItemState.CHEST_SMALL_KEY);

        assertEquals(2, state.currentFlag(DungeonItemState.SMALL_KEYS_INDEX));
        assertEquals(2, state.dungeonItemFlagsSnapshot()[1 * DungeonItemState.ITEM_FLAG_SIZE + 4]);
    }

    @Test
    void consumesAndSynchronizesOneSmallKeyWithoutUnderflowing() {
        DungeonItemState state = new DungeonItemState();
        byte[] dungeonFlags = new byte[DungeonItemState.DUNGEON_ITEM_FLAGS_SIZE];
        dungeonFlags[DungeonItemState.SMALL_KEYS_INDEX] = 1;
        state.restore(dungeonFlags,
            new byte[DungeonItemState.COLOR_DUNGEON_ITEM_FLAGS_SIZE]);
        state.loadForMap(0x00, true);

        assertTrue(state.consumeSmallKey());
        assertEquals(0, state.currentFlag(DungeonItemState.SMALL_KEYS_INDEX));
        assertEquals(0, state.dungeonItemFlagsSnapshot()[DungeonItemState.SMALL_KEYS_INDEX]);
        assertFalse(state.consumeSmallKey());
    }

    @Test
    void colorDungeonSynchronizationUsesTheSeparateFiveByteBlock() {
        DungeonItemState state = new DungeonItemState();
        state.restore(new byte[DungeonItemState.DUNGEON_ITEM_FLAGS_SIZE],
            new byte[DungeonItemState.COLOR_DUNGEON_ITEM_FLAGS_SIZE]);
        state.loadForMap(DungeonItemState.MAP_COLOR_DUNGEON, true);

        state.incrementCurrentFlag(DungeonItemState.CHEST_MAP);

        assertEquals(1, state.colorDungeonItemFlagsSnapshot()[DungeonItemState.MAP_INDEX]);
    }

    @Test
    void rejectsWrongPersistentFlagLengths() {
        DungeonItemState state = new DungeonItemState();

        assertThrows(IllegalArgumentException.class,
            () -> state.restore(new byte[1], new byte[DungeonItemState.COLOR_DUNGEON_ITEM_FLAGS_SIZE]));
        assertThrows(IllegalArgumentException.class,
            () -> state.restore(new byte[DungeonItemState.DUNGEON_ITEM_FLAGS_SIZE], new byte[1]));
    }
}
