package linksawakening.world;

import linksawakening.rom.RomBank;

import java.util.Objects;
import java.util.function.IntSupplier;

/** Decodes the ROM-backed rules in bank-$03's SpawnEnemyDrop routine. */
public final class EnemyDropResolver {
    public static final int ENTITY_NONE = 0xFF;

    private static final int DESTROYED_HEALTH_GROUP_OFFSETS_BANK = 0x03;
    private static final int DESTROYED_HEALTH_GROUP_OFFSETS_ADDRESS = 0x4826;
    // The source comment says 52 entries, but the label spans $4826-$485A and
    // the shipped ROM contains all 53 bytes listed by the disassembly.
    private static final int DESTROYED_HEALTH_GROUP_OFFSETS_LENGTH = 53;

    private static final int DROP_TABLE_BY_INDEX_BANK = 0x03;
    private static final int DROP_TABLE_BY_INDEX_ADDRESS = 0x559D;
    private static final int DROP_TABLE_BY_INDEX_LENGTH = 14;

    private static final int RANDOM_DROP_CHANCE_TABLE_BANK = 0x03;
    private static final int RANDOM_DROP_CHANCE_TABLE_ADDRESS = 0x55AB;
    private static final int RANDOM_DROP_CHANCE_TABLE_LENGTH = 14;

    private static final int RANDOM_DROP_CHANCE_TABLE_LOW_HEALTH_BANK = 0x03;
    private static final int RANDOM_DROP_CHANCE_TABLE_LOW_HEALTH_ADDRESS = 0x55B9;
    private static final int RANDOM_DROP_CHANCE_TABLE_LOW_HEALTH_LENGTH = 14;

    private static final int DROP_TABLE_RANDOM_BANK = 0x03;
    private static final int DROP_TABLE_RANDOM_ADDRESS = 0x55C7;
    private static final int DROP_TABLE_RANDOM_LENGTH = 8;

    private static final int THRESHOLD_LOW_HEALTH_TABLE_BANK = 0x02;
    private static final int THRESHOLD_LOW_HEALTH_TABLE_ADDRESS = 0x6308;
    private static final int THRESHOLD_LOW_HEALTH_TABLE_LENGTH = 15;

    private static final int GUARDIAN_ACORN_COUNTER_MAX = 0x0C;
    private static final int PIECE_OF_POWER_COUNTER_MAX_LOW_MAX_HEALTH = 0x1E;
    private static final int PIECE_OF_POWER_COUNTER_MAX_MEDIUM_MAX_HEALTH = 0x23;
    private static final int PIECE_OF_POWER_COUNTER_MAX_HIGH_MAX_HEALTH = 0x28;

    private final int[] destroyedHealthGroupOffsets;
    private final int[] dropTableByIndex;
    private final int[] randomDropChanceTable;
    private final int[] randomDropChanceTableLowHealth;
    private final int[] dropTableRandom;
    private final int[] thresholdLowHealthTable;

    public EnemyDropResolver(byte[] romData) {
        Objects.requireNonNull(romData, "ROM data cannot be null");
        destroyedHealthGroupOffsets = loadTable(romData,
            DESTROYED_HEALTH_GROUP_OFFSETS_BANK, DESTROYED_HEALTH_GROUP_OFFSETS_ADDRESS,
            DESTROYED_HEALTH_GROUP_OFFSETS_LENGTH, "DestroyedEntityHealthGroupOffsetTable");
        dropTableByIndex = loadTable(romData, DROP_TABLE_BY_INDEX_BANK,
            DROP_TABLE_BY_INDEX_ADDRESS, DROP_TABLE_BY_INDEX_LENGTH, "DropTableByIndex");
        randomDropChanceTable = loadTable(romData, RANDOM_DROP_CHANCE_TABLE_BANK,
            RANDOM_DROP_CHANCE_TABLE_ADDRESS, RANDOM_DROP_CHANCE_TABLE_LENGTH,
            "RandomDropChanceTable");
        randomDropChanceTableLowHealth = loadTable(romData,
            RANDOM_DROP_CHANCE_TABLE_LOW_HEALTH_BANK,
            RANDOM_DROP_CHANCE_TABLE_LOW_HEALTH_ADDRESS,
            RANDOM_DROP_CHANCE_TABLE_LOW_HEALTH_LENGTH,
            "RandomDropChanceTableLowHealth");
        dropTableRandom = loadTable(romData, DROP_TABLE_RANDOM_BANK,
            DROP_TABLE_RANDOM_ADDRESS, DROP_TABLE_RANDOM_LENGTH, "DropTableRandom");
        thresholdLowHealthTable = loadTable(romData, THRESHOLD_LOW_HEALTH_TABLE_BANK,
            THRESHOLD_LOW_HEALTH_TABLE_ADDRESS, THRESHOLD_LOW_HEALTH_TABLE_LENGTH,
            "ThresholdLowHealthTable");
    }

    /** The two persistent byte counters consumed by SpawnEnemyDrop. */
    public record CounterState(int guardianAcornCounter, int pieceOfPowerKillCount) {
        public CounterState {
            validateByte(guardianAcornCounter, "Guardian Acorn counter");
            validateByte(pieceOfPowerKillCount, "Piece of Power kill count");
        }
    }

    /** Inputs corresponding to the WRAM and hardware state used by SpawnEnemyDrop. */
    public record Context(int entityType, int healthGroup, int droppedItem, int maxHearts,
                          int health, boolean bossBattle, boolean activePowerUp,
                          boolean sideScrolling, CounterState counters,
                          IntSupplier randomByteSupplier) {
        public Context {
            validateByte(entityType, "Entity type");
            validateByte(healthGroup, "Health group");
            validateByte(droppedItem, "Dropped item");
            validateByte(maxHearts, "Maximum hearts");
            validateByte(health, "Health");
            Objects.requireNonNull(counters, "Counter state cannot be null");
            Objects.requireNonNull(randomByteSupplier, "Random byte supplier cannot be null");
        }
    }

    /** The item selected and the updated persistent counters after one resolution. */
    public record Result(int itemType, CounterState counters, int randomReads) {
        public Result {
            validateByte(itemType, "Item type");
            Objects.requireNonNull(counters, "Counter state cannot be null");
            if (randomReads < 0) {
                throw new IllegalArgumentException("Random read count cannot be negative");
            }
        }

        public boolean dropped() {
            return itemType != ENTITY_NONE;
        }
    }

    /** Resolves one enemy death while preserving the source routine's branch order. */
    public Result resolve(Context context) {
        Objects.requireNonNull(context, "Context cannot be null");

        int droppedItem = context.droppedItem();
        if (droppedItem == ENTITY_NONE) {
            return noItem(context.counters(), 0);
        }
        if (droppedItem != 0) {
            return new Result(droppedItem, context.counters(), 0);
        }

        CounterState counters = incrementGuardianAcornCounter(context.counters());
        if (counters.guardianAcornCounter() >= GUARDIAN_ACORN_COUNTER_MAX) {
            counters = new CounterState(0, counters.pieceOfPowerKillCount());
            if (canDropPowerUp(context)) {
                return new Result(0x34, counters, 0);
            }
        }

        int healthGroupOffset = healthGroupOffset(context.healthGroup());
        if (healthGroupOffset == 0) {
            return noItem(counters, 0);
        }
        int tableIndex = healthGroupOffset - 1;
        if (tableIndex < 0 || tableIndex >= dropTableByIndex.length) {
            throw new IllegalArgumentException("Destroyed entity health-group offset out of range: "
                + healthGroupOffset);
        }

        int pieceOfPowerThreshold = pieceOfPowerThreshold(context.maxHearts());
        int pieceOfPowerKillCount = (counters.pieceOfPowerKillCount() + 1) & 0xFF;
        boolean reachedPieceOfPowerThreshold = pieceOfPowerKillCount >= pieceOfPowerThreshold;
        if (reachedPieceOfPowerThreshold) {
            pieceOfPowerKillCount = 0;
        }
        counters = new CounterState(counters.guardianAcornCounter(), pieceOfPowerKillCount);
        if (reachedPieceOfPowerThreshold && canDropPowerUp(context)) {
            return new Result(0x33, counters, 0);
        }

        int chanceMask = (isLowHealth(context.maxHearts(), context.health())
            ? randomDropChanceTableLowHealth : randomDropChanceTable)[tableIndex];
        int randomReads = 1;
        if ((nextRandomByte(context.randomByteSupplier()) & chanceMask) != 0) {
            return noItem(counters, randomReads);
        }

        int itemType = dropTableByIndex[tableIndex];
        if (itemType != ENTITY_NONE) {
            return new Result(itemType, counters, randomReads);
        }

        int randomIndex = nextRandomByte(context.randomByteSupplier()) & 0x07;
        return new Result(dropTableRandom[randomIndex], counters, randomReads + 1);
    }

    public boolean isLowHealth(int maxHearts, int health) {
        validateByte(maxHearts, "Maximum hearts");
        validateByte(health, "Health");
        int thresholdIndex = Math.min(maxHearts, thresholdLowHealthTable.length - 1);
        return health < thresholdLowHealthTable[thresholdIndex];
    }

    public int[] destroyedHealthGroupOffsets() {
        return destroyedHealthGroupOffsets.clone();
    }

    public int[] dropTableByIndex() {
        return dropTableByIndex.clone();
    }

    public int[] randomDropChanceTable() {
        return randomDropChanceTable.clone();
    }

    public int[] randomDropChanceTableLowHealth() {
        return randomDropChanceTableLowHealth.clone();
    }

    public int[] dropTableRandom() {
        return dropTableRandom.clone();
    }

    public int[] thresholdLowHealthTable() {
        return thresholdLowHealthTable.clone();
    }

    private int healthGroupOffset(int healthGroup) {
        if (healthGroup >= destroyedHealthGroupOffsets.length) {
            throw new IllegalArgumentException("Health group is not in the destroyed-entity table: "
                + healthGroup);
        }
        return destroyedHealthGroupOffsets[healthGroup];
    }

    private static CounterState incrementGuardianAcornCounter(CounterState counters) {
        int nextCounter = (counters.guardianAcornCounter() + 1) & 0xFF;
        return new CounterState(nextCounter, counters.pieceOfPowerKillCount());
    }

    private static int pieceOfPowerThreshold(int maxHearts) {
        if (maxHearts < 0x07) {
            return PIECE_OF_POWER_COUNTER_MAX_LOW_MAX_HEALTH;
        }
        if (maxHearts < 0x0B) {
            return PIECE_OF_POWER_COUNTER_MAX_MEDIUM_MAX_HEALTH;
        }
        return PIECE_OF_POWER_COUNTER_MAX_HIGH_MAX_HEALTH;
    }

    private static boolean canDropPowerUp(Context context) {
        return !context.bossBattle() && !context.activePowerUp() && !context.sideScrolling();
    }

    private static int nextRandomByte(IntSupplier randomByteSupplier) {
        return randomByteSupplier.getAsInt() & 0xFF;
    }

    private static Result noItem(CounterState counters, int randomReads) {
        return new Result(ENTITY_NONE, counters, randomReads);
    }

    private static int[] loadTable(byte[] romData, int bank, int address, int length,
                                   String tableName) {
        int offset = RomBank.romOffset(bank, address);
        if (offset < 0 || length < 0 || offset > romData.length - length) {
            throw new IllegalArgumentException("ROM table is truncated: " + tableName);
        }
        int[] values = new int[length];
        for (int index = 0; index < length; index++) {
            values[index] = Byte.toUnsignedInt(romData[offset + index]);
        }
        return values;
    }

    private static void validateByte(int value, String name) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException(name + " must be an unsigned byte");
        }
    }
}
