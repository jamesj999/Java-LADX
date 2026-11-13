package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

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
    private EnemyDropResolver enemyDropResolver;
    private EnemyDropResolver.CounterState enemyDropCounters = new EnemyDropResolver.CounterState(0, 0);
    private int enemyDropMaxHearts = 3;
    private int enemyDropHealth = 6;
    private Boolean enemyDropLowHealth;
    private boolean enemyDropActivePowerUp;
    private boolean inBossBattle;
    private int killCount;
    private final int[] killOrder = new int[0x100];

    int readBossAgonySfxCountdown() {
        return bossAgonySfxCountdown;
    }

    int writeBossAgonySfxCountdown(int value) {
        return bossAgonySfxCountdown = value;
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
        return bossAgonySourceSlot = value;
    }

    int readDroppedItemBySlot(int slot) {
        return droppedItemBySlot[slot];
    }

    int writeDroppedItemBySlot(int slot, int value) {
        return droppedItemBySlot[slot] = value;
    }

    int[] storageDroppedItemBySlot() {
        return droppedItemBySlot;
    }

    int readDropPrivateCountdown1(int slot) {
        return dropPrivateCountdown1[slot];
    }

    int writeDropPrivateCountdown1(int slot, int value) {
        return dropPrivateCountdown1[slot] = value;
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
        return dropPrivateCountdown3[slot] = value;
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
        return droppablePrivateState3[slot] = value;
    }

    int readDroppablePrivateState4(int slot) {
        return droppablePrivateState4[slot];
    }

    int writeDroppablePrivateState4(int slot, int value) {
        return droppablePrivateState4[slot] = value;
    }

    boolean readEnemyDropActive(int slot) {
        return enemyDropActive[slot];
    }

    boolean writeEnemyDropActive(int slot, boolean value) {
        return enemyDropActive[slot] = value;
    }

    int readBossDeathProducerState(int slot) {
        return bossDeathProducerState[slot];
    }

    int writeBossDeathProducerState(int slot, int value) {
        return bossDeathProducerState[slot] = value;
    }

    EnemyDropResolver readEnemyDropResolver() {
        return enemyDropResolver;
    }

    EnemyDropResolver writeEnemyDropResolver(EnemyDropResolver value) {
        return enemyDropResolver = value;
    }

    EnemyDropResolver.CounterState readEnemyDropCounters() {
        return enemyDropCounters;
    }

    EnemyDropResolver.CounterState writeEnemyDropCounters(EnemyDropResolver.CounterState value) {
        return enemyDropCounters = value;
    }

    int readEnemyDropMaxHearts() {
        return enemyDropMaxHearts;
    }

    int writeEnemyDropMaxHearts(int value) {
        return enemyDropMaxHearts = value;
    }

    int readEnemyDropHealth() {
        return enemyDropHealth;
    }

    int writeEnemyDropHealth(int value) {
        return enemyDropHealth = value;
    }

    Boolean readEnemyDropLowHealth() {
        return enemyDropLowHealth;
    }

    Boolean writeEnemyDropLowHealth(Boolean value) {
        return enemyDropLowHealth = value;
    }

    boolean readEnemyDropActivePowerUp() {
        return enemyDropActivePowerUp;
    }

    boolean writeEnemyDropActivePowerUp(boolean value) {
        return enemyDropActivePowerUp = value;
    }

    boolean readInBossBattle() {
        return inBossBattle;
    }

    boolean writeInBossBattle(boolean value) {
        return inBossBattle = value;
    }

    int readKillCount() {
        return killCount;
    }

    int writeKillCount(int value) {
        return killCount = value;
    }

    int readKillOrder(int slot) {
        return killOrder[slot];
    }

    int writeKillOrder(int slot, int value) {
        return killOrder[slot] = value;
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
