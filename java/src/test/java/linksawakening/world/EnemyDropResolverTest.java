package linksawakening.world;

import linksawakening.world.EnemyDropResolver.Context;
import linksawakening.world.EnemyDropResolver.CounterState;
import linksawakening.world.EnemyDropResolver.Result;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EnemyDropResolverTest {
    private static final int[] DESTROYED_HEALTH_GROUP_OFFSETS = {
        0x02, 0x06, 0x01, 0x03, 0x03, 0x03, 0x0D, 0x08, 0x0A, 0x02, 0x07, 0x0B,
        0x00, 0x04, 0x00, 0x08, 0x04, 0x0E, 0x0E, 0x0E, 0x0E, 0x0E, 0x00, 0x03,
        0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x02, 0x00, 0x00,
        0x02, 0x00, 0x00, 0x00, 0x00, 0x06, 0x06, 0x0D, 0x0E, 0x00, 0x09, 0x03,
        0x06, 0x00, 0x02, 0x0E, 0x0E
    };
    private static final int[] DROP_TABLE_BY_INDEX = {
        0x2E, 0x2E, 0x2D, 0x2D, 0x37, 0x2D, 0xFF, 0xFF, 0x2F, 0x37, 0x38, 0x2E,
        0x2F, 0x2F
    };
    private static final int[] RANDOM_DROP_CHANCE = {
        0x03, 0x01, 0x01, 0x00, 0x03, 0x03, 0x03, 0x03, 0x01, 0x00, 0x00, 0x00,
        0x03, 0x00
    };
    private static final int[] RANDOM_DROP_CHANCE_LOW_HEALTH = {
        0x01, 0x01, 0x01, 0x00, 0x01, 0x01, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00,
        0x01, 0x00
    };
    private static final int[] DROP_TABLE_RANDOM = {
        0x2E, 0x2D, 0x38, 0x2F, 0x2E, 0x2D, 0x38, 0x37
    };
    private static final int[] THRESHOLD_LOW_HEALTH = {
        0x00, 0x22, 0xC9, 0x05, 0x05, 0x05, 0x09, 0x09, 0x09, 0x11, 0x11, 0x11,
        0x19, 0x19, 0x19
    };

    @Test
    void decodesAllDropTablesFromTheShippedRom() throws IOException {
        EnemyDropResolver resolver = new EnemyDropResolver(loadRom());

        assertArrayEquals(DESTROYED_HEALTH_GROUP_OFFSETS,
            resolver.destroyedHealthGroupOffsets());
        assertArrayEquals(DROP_TABLE_BY_INDEX, resolver.dropTableByIndex());
        assertArrayEquals(RANDOM_DROP_CHANCE, resolver.randomDropChanceTable());
        assertArrayEquals(RANDOM_DROP_CHANCE_LOW_HEALTH,
            resolver.randomDropChanceTableLowHealth());
        assertArrayEquals(DROP_TABLE_RANDOM, resolver.dropTableRandom());
        assertArrayEquals(THRESHOLD_LOW_HEALTH, resolver.thresholdLowHealthTable());
    }

    @Test
    void tableAccessorsReturnDefensiveCopies() throws IOException {
        EnemyDropResolver resolver = new EnemyDropResolver(loadRom());

        int[] first = resolver.destroyedHealthGroupOffsets();
        int[] second = resolver.destroyedHealthGroupOffsets();
        first[0] = 0;

        assertNotSame(first, second);
        assertEquals(DESTROYED_HEALTH_GROUP_OFFSETS[0], second[0]);
        assertEquals(DESTROYED_HEALTH_GROUP_OFFSETS[0],
            resolver.destroyedHealthGroupOffsets()[0]);
    }

    @Test
    void noneAndDirectItemsReturnBeforeAnyRandomRead() throws IOException {
        EnemyDropResolver resolver = new EnemyDropResolver(loadRom());
        CounterState counters = new CounterState(0x03, 0x04);

        RecordingRandom noneRandom = new RecordingRandom(0x12);
        Result none = resolver.resolve(context(0x09, 0x01, EnemyDropResolver.ENTITY_NONE,
            0x08, 0x04, false, false, false, counters, noneRandom));
        assertEquals(EnemyDropResolver.ENTITY_NONE, none.itemType());
        assertEquals(counters, none.counters());
        assertEquals(0, none.randomReads());
        assertTrue(noneRandom.valuesRead().isEmpty());
        assertFalse(none.dropped());

        RecordingRandom directRandom = new RecordingRandom(0x34);
        Result direct = resolver.resolve(context(0x09, 0x01, 0x2A,
            0x08, 0x04, true, true, true, counters, directRandom));
        assertEquals(0x2A, direct.itemType());
        assertEquals(counters, direct.counters());
        assertEquals(0, direct.randomReads());
        assertTrue(directRandom.valuesRead().isEmpty());
        assertTrue(direct.dropped());
    }

    @Test
    void guardianAcornCounterIncrementsAndDropsAtTwelveOnlyWhenUngated() throws IOException {
        EnemyDropResolver resolver = new EnemyDropResolver(loadRom());

        RecordingRandom belowThresholdRandom = new RecordingRandom(0x01);
        Result belowThreshold = resolver.resolve(context(0x09, 0x02, 0,
            0x08, 0x08, false, false, false,
            new CounterState(0x0A, 0x00), belowThresholdRandom));
        assertEquals(EnemyDropResolver.ENTITY_NONE, belowThreshold.itemType());
        assertEquals(new CounterState(0x0B, 0x01), belowThreshold.counters());
        assertEquals(List.of(0x01), belowThresholdRandom.valuesRead());
        assertEquals(1, belowThreshold.randomReads());

        RecordingRandom acornRandom = new RecordingRandom(0xFF);
        Result acorn = resolver.resolve(context(0x09, 0x02, 0,
            0x08, 0x08, false, false, false,
            new CounterState(0x0B, 0x00), acornRandom));
        assertEquals(0x34, acorn.itemType());
        assertEquals(new CounterState(0x00, 0x00), acorn.counters());
        assertEquals(0, acorn.randomReads());
        assertTrue(acornRandom.valuesRead().isEmpty());

        for (Gate gate : Gate.values()) {
            RecordingRandom gatedRandom = new RecordingRandom(0x01);
            Result gated = resolver.resolve(context(0x09, 0x02, 0,
                0x08, 0x08, gate.bossBattle(), gate.activePowerUp(), gate.sideScrolling(),
                new CounterState(0x0B, 0x00), gatedRandom));
            assertEquals(EnemyDropResolver.ENTITY_NONE, gated.itemType(), gate.name());
            assertEquals(new CounterState(0x00, 0x01), gated.counters(), gate.name());
            assertEquals(1, gated.randomReads(), gate.name());
            assertEquals(List.of(0x01), gatedRandom.valuesRead(), gate.name());
        }
    }

    @Test
    void pieceOfPowerUsesThreeMaxHeartThresholdsAndResetsBeforeGates() throws IOException {
        EnemyDropResolver resolver = new EnemyDropResolver(loadRom());

        assertPieceOfPowerAtThreshold(resolver, 0x06, 0x1E);
        assertPieceOfPowerAtThreshold(resolver, 0x0A, 0x23);
        assertPieceOfPowerAtThreshold(resolver, 0x0B, 0x28);

        for (Gate gate : Gate.values()) {
            RecordingRandom gatedRandom = new RecordingRandom(0x01);
            Result gated = resolver.resolve(context(0x09, 0x02, 0,
                0x06, 0x08, gate.bossBattle(), gate.activePowerUp(), gate.sideScrolling(),
                new CounterState(0x00, 0x1D), gatedRandom));
            assertEquals(EnemyDropResolver.ENTITY_NONE, gated.itemType(), gate.name());
            assertEquals(new CounterState(0x01, 0x00), gated.counters(), gate.name());
            assertEquals(1, gated.randomReads(), gate.name());
            assertEquals(List.of(0x01), gatedRandom.valuesRead(), gate.name());
        }
    }

    @Test
    void zeroHealthGroupStopsBeforePieceCounterOrRandomness() throws IOException {
        EnemyDropResolver resolver = new EnemyDropResolver(loadRom());
        RecordingRandom random = new RecordingRandom(0x00);

        Result result = resolver.resolve(context(0x09, 0x0C, 0,
            0x08, 0x08, false, false, false,
            new CounterState(0x04, 0x1D), random));

        assertEquals(EnemyDropResolver.ENTITY_NONE, result.itemType());
        assertEquals(new CounterState(0x05, 0x1D), result.counters());
        assertEquals(0, result.randomReads());
        assertTrue(random.valuesRead().isEmpty());
    }

    @Test
    void normalAndLowHealthMasksUseTheCurrentHealthAgainstClampedThreshold() throws IOException {
        EnemyDropResolver resolver = new EnemyDropResolver(loadRom());

        assertTrue(resolver.isLowHealth(0x03, 0x04));
        assertFalse(resolver.isLowHealth(0x03, 0x05));
        assertFalse(resolver.isLowHealth(0x00, 0x00));
        assertTrue(resolver.isLowHealth(0xFF, 0x18));
        assertFalse(resolver.isLowHealth(0xFF, 0x19));

        RecordingRandom normalRandom = new RecordingRandom(0x02);
        Result normal = resolver.resolve(context(0x09, 0x02, 0,
            0x03, 0x05, false, false, false,
            new CounterState(0x00, 0x00), normalRandom));
        assertEquals(EnemyDropResolver.ENTITY_NONE, normal.itemType());
        assertEquals(new CounterState(0x01, 0x01), normal.counters());
        assertEquals(1, normal.randomReads());

        RecordingRandom lowHealthRandom = new RecordingRandom(0x02);
        Result lowHealth = resolver.resolve(context(0x09, 0x02, 0,
            0x03, 0x04, false, false, false,
            new CounterState(0x00, 0x00), lowHealthRandom));
        assertEquals(0x2E, lowHealth.itemType());
        assertEquals(new CounterState(0x01, 0x01), lowHealth.counters());
        assertEquals(1, lowHealth.randomReads());
        assertEquals(List.of(0x02), lowHealthRandom.valuesRead());
    }

    @Test
    void sentinelDropUsesChanceRandomThenFallbackRandomInOrder() throws IOException {
        EnemyDropResolver resolver = new EnemyDropResolver(loadRom());
        RecordingRandom random = new RecordingRandom(0x100, -1);

        Result result = resolver.resolve(context(0x09, 0x0A, 0,
            0x08, 0x08, false, false, false,
            new CounterState(0x00, 0x00), random));

        assertEquals(0x37, result.itemType());
        assertEquals(new CounterState(0x01, 0x01), result.counters());
        assertEquals(2, result.randomReads());
        assertEquals(List.of(0x100, -1), random.valuesRead());
    }

    @Test
    void validatesByteFieldsAndRequiredReferences() throws IOException {
        assertThrows(NullPointerException.class, () -> new EnemyDropResolver(null));

        assertThrows(IllegalArgumentException.class, () -> new CounterState(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new CounterState(0, 0x100));

        CounterState counters = new CounterState(0, 0);
        RecordingRandom random = new RecordingRandom(0);
        assertThrows(NullPointerException.class, () -> context(0, 0, 0,
            0, 0, false, false, false, null, random));
        assertThrows(NullPointerException.class, () -> context(0, 0, 0,
            0, 0, false, false, false, counters, null));

        int[][] invalidFields = {
            {-1, 0, 0, 0, 0},
            {0x100, 0, 0, 0, 0},
            {0, -1, 0, 0, 0},
            {0, 0x100, 0, 0, 0},
            {0, 0, -1, 0, 0},
            {0, 0, 0x100, 0, 0},
            {0, 0, 0, -1, 0},
            {0, 0, 0, 0x100, 0},
            {0, 0, 0, 0, -1},
            {0, 0, 0, 0, 0x100}
        };
        for (int[] fields : invalidFields) {
            assertThrows(IllegalArgumentException.class, () -> context(
                fields[0], fields[1], fields[2], fields[3], fields[4],
                false, false, false, counters, random));
        }
    }

    private static void assertPieceOfPowerAtThreshold(EnemyDropResolver resolver,
                                                       int maxHearts, int threshold) {
        RecordingRandom random = new RecordingRandom(0xFF);
        Result result = resolver.resolve(context(0x09, 0x02, 0,
            maxHearts, 0x08, false, false, false,
            new CounterState(0x00, threshold - 1), random));

        assertEquals(0x33, result.itemType());
        assertEquals(new CounterState(0x01, 0x00), result.counters());
        assertEquals(0, result.randomReads());
        assertTrue(random.valuesRead().isEmpty());
    }

    private static Context context(int entityType, int healthGroup, int droppedItem,
                                   int maxHearts, int health, boolean bossBattle,
                                   boolean activePowerUp, boolean sideScrolling,
                                   CounterState counters, IntSupplier randomByteSupplier) {
        return new Context(entityType, healthGroup, droppedItem, maxHearts, health,
            bossBattle, activePowerUp, sideScrolling, counters, randomByteSupplier);
    }

    private static byte[] loadRom() throws IOException {
        try (var stream = EnemyDropResolverTest.class.getClassLoader()
            .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        }
    }

    private enum Gate {
        BOSS(true, false, false),
        POWER_UP(false, true, false),
        SIDE_SCROLLING(false, false, true);

        private final boolean bossBattle;
        private final boolean activePowerUp;
        private final boolean sideScrolling;

        Gate(boolean bossBattle, boolean activePowerUp, boolean sideScrolling) {
            this.bossBattle = bossBattle;
            this.activePowerUp = activePowerUp;
            this.sideScrolling = sideScrolling;
        }

        boolean bossBattle() {
            return bossBattle;
        }

        boolean activePowerUp() {
            return activePowerUp;
        }

        boolean sideScrolling() {
            return sideScrolling;
        }
    }

    private static final class RecordingRandom implements IntSupplier {
        private final int[] values;
        private final List<Integer> valuesRead = new ArrayList<>();

        private RecordingRandom(int... values) {
            this.values = values;
        }

        @Override
        public int getAsInt() {
            if (valuesRead.size() >= values.length) {
                throw new AssertionError("Unexpected random read");
            }
            int value = values[valuesRead.size()];
            valuesRead.add(value);
            return value;
        }

        List<Integer> valuesRead() {
            return List.copyOf(valuesRead);
        }
    }
}
