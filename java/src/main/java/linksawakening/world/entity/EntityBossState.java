package linksawakening.world.entity;

import linksawakening.world.entity.motion.EnemyDropMotion;
import linksawakening.world.entity.motion.FairyMotion;
import linksawakening.world.entity.motion.HookshotChainMotion;

/** Shared bossRewards slot state, independent of behavior dispatch and construction order. */
final class EntityBossState {
    private int bossAgonySfxCountdown;
    private int bossAgonySourceSlot;
    private final int[] droppedItemBySlot = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] dropPrivateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] dropPrivateCountdown3 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] droppablePrivateState3 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] droppablePrivateState4 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] enemyDropActive = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] bossDeathProducerState = new int[EntityRoomLoader.MAX_ENTITIES];
    // Optional configured drop tables; absence preserves the no-drop path.
    private EnemyDropResolver enemyDropResolver = EnemyDropResolver.NONE;
    private EnemyDropResolver.CounterState enemyDropCounters = new EnemyDropResolver.CounterState(0, 0);
    private int enemyDropMaxHearts = 3;
    private int enemyDropHealth = 6;
    // Optional override; null retains the derived/default value.
    private Boolean enemyDropLowHealth;
    private boolean enemyDropActivePowerUp;
    private boolean inBossBattle;
    private int killCount;
    private final int[] killOrder = new int[0x100];

    int readBossAgonySfxCountdown() {
        return bossAgonySfxCountdown;
    }

    int writeBossAgonySfxCountdown(int value) {
        bossAgonySfxCountdown = value;
        return bossAgonySfxCountdown;
    }

    int postBossAgonySfxCountdown(int delta) {
        int previous = bossAgonySfxCountdown;
        bossAgonySfxCountdown += delta;
        return previous;
    }

    int readBossAgonySourceSlot() {
        return bossAgonySourceSlot;
    }

    int writeBossAgonySourceSlot(int value) {
        bossAgonySourceSlot = value;
        return bossAgonySourceSlot;
    }

    int readDroppedItemBySlot(int slot) {
        return droppedItemBySlot[slot];
    }

    int writeDroppedItemBySlot(int slot, int value) {
        droppedItemBySlot[slot] = value;
        return droppedItemBySlot[slot];
    }

    int[] storageDroppedItemBySlot() {
        return droppedItemBySlot;
    }

    int readDropPrivateCountdown1(int slot) {
        return dropPrivateCountdown1[slot];
    }

    int writeDropPrivateCountdown1(int slot, int value) {
        dropPrivateCountdown1[slot] = value;
        return dropPrivateCountdown1[slot];
    }

    int postDropPrivateCountdown1(int slot, int delta) {
        int previous = dropPrivateCountdown1[slot];
        dropPrivateCountdown1[slot] += delta;
        return previous;
    }

    int readDropPrivateCountdown3(int slot) {
        return dropPrivateCountdown3[slot];
    }

    int writeDropPrivateCountdown3(int slot, int value) {
        dropPrivateCountdown3[slot] = value;
        return dropPrivateCountdown3[slot];
    }

    int postDropPrivateCountdown3(int slot, int delta) {
        int previous = dropPrivateCountdown3[slot];
        dropPrivateCountdown3[slot] += delta;
        return previous;
    }

    int readDroppablePrivateState3(int slot) {
        return droppablePrivateState3[slot];
    }

    int writeDroppablePrivateState3(int slot, int value) {
        droppablePrivateState3[slot] = value;
        return droppablePrivateState3[slot];
    }

    int readDroppablePrivateState4(int slot) {
        return droppablePrivateState4[slot];
    }

    int writeDroppablePrivateState4(int slot, int value) {
        droppablePrivateState4[slot] = value;
        return droppablePrivateState4[slot];
    }

    boolean readEnemyDropActive(int slot) {
        return enemyDropActive[slot];
    }

    boolean writeEnemyDropActive(int slot, boolean value) {
        enemyDropActive[slot] = value;
        return enemyDropActive[slot];
    }

    int readBossDeathProducerState(int slot) {
        return bossDeathProducerState[slot];
    }

    int writeBossDeathProducerState(int slot, int value) {
        bossDeathProducerState[slot] = value;
        return bossDeathProducerState[slot];
    }

    EnemyDropResolver readEnemyDropResolver() {
        return enemyDropResolver;
    }

    EnemyDropResolver writeEnemyDropResolver(EnemyDropResolver value) {
        enemyDropResolver = java.util.Objects.requireNonNullElse(value, EnemyDropResolver.NONE);
        return enemyDropResolver;
    }

    EnemyDropResolver.CounterState readEnemyDropCounters() {
        return enemyDropCounters;
    }

    EnemyDropResolver.CounterState writeEnemyDropCounters(EnemyDropResolver.CounterState value) {
        enemyDropCounters = value;
        return enemyDropCounters;
    }

    int readEnemyDropMaxHearts() {
        return enemyDropMaxHearts;
    }

    int writeEnemyDropMaxHearts(int value) {
        enemyDropMaxHearts = value;
        return enemyDropMaxHearts;
    }

    int readEnemyDropHealth() {
        return enemyDropHealth;
    }

    int writeEnemyDropHealth(int value) {
        enemyDropHealth = value;
        return enemyDropHealth;
    }

    Boolean readEnemyDropLowHealth() {
        return enemyDropLowHealth;
    }

    Boolean writeEnemyDropLowHealth(Boolean value) {
        enemyDropLowHealth = value;
        return enemyDropLowHealth;
    }

    boolean readEnemyDropActivePowerUp() {
        return enemyDropActivePowerUp;
    }

    boolean writeEnemyDropActivePowerUp(boolean value) {
        enemyDropActivePowerUp = value;
        return enemyDropActivePowerUp;
    }

    boolean readInBossBattle() {
        return inBossBattle;
    }

    boolean writeInBossBattle(boolean value) {
        inBossBattle = value;
        return inBossBattle;
    }

    int readKillCount() {
        return killCount;
    }

    int writeKillCount(int value) {
        killCount = value;
        return killCount;
    }

    int readKillOrder(int slot) {
        return killOrder[slot];
    }

    int writeKillOrder(int slot, int value) {
        killOrder[slot] = value;
        return killOrder[slot];
    }

    int[] storageKillOrder() {
        return killOrder;
    }

    void resetEnemyDropState(int slot, EntityMotionRegistry motions) {
        this.writeDroppedItemBySlot(slot, 0);
        this.writeDropPrivateCountdown1(slot, 0);
        this.writeDropPrivateCountdown3(slot, 0);
        this.writeDroppablePrivateState3(slot, 0);
        this.writeDroppablePrivateState4(slot, 0);
        this.writeEnemyDropActive(slot, false);
        motions.get(EnemyDropMotion.class, EnemyDropMotion::new).clear(slot);
        motions.get(FairyMotion.class, FairyMotion::new).clear(slot);
        motions.get(HookshotChainMotion.class, HookshotChainMotion::new).clear(slot);
    }
}
