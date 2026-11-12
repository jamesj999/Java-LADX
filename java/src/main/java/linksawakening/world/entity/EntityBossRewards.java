package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.function.IntSupplier;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.*;

/** Boss destruction and source-shaped reward production. */
public final class EntityBossRewards {
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

    EnemyDropResolver.Result resolveDrop(EnemyDropResolver.Context context) {
        return this.readEnemyDropLowHealth() == null
                ? this.readEnemyDropResolver().resolve(context)
                : this.readEnemyDropResolver().resolve(context, this.readEnemyDropLowHealth());
    }

    final EntityBossRewardsServices runtime;

    EntityBossRewards(EntityBossRewardsServices runtime) {
        this.runtime = runtime;
    }

    /** ApplySwordDamagesToEnemy, $03:7361: preserve descending RNG order. */
    void cascadeGhiniDeath(RoomEntity killed, IntSupplier random) {
        if (killed.type() != 0x12) return;
        int[] drops = {0x2D, 0x2E, 0x38, 0x37};
        for (int slot = RoomEntity.SLOT_COUNT - 1; slot >= 0; slot--) {
            RoomEntity other = runtime.slots()[slot];
            if (slot == killed.slot()
                    || other == null
                    || (other.type() != 0x10 && other.type() != 0x11)
                    || runtime.motions()
                                    .get(GhiniMotion.class, GhiniMotion::new)
                                    .state(slot)
                            != 0
                    || other.status() == EntityStatus.DISABLED) continue;
            runtime.slots()[slot] = withStatus(other, EntityStatus.DYING);
            runtime.combatTables().writeDyingCountdown(slot, 0x1F);
            this.writeDroppedItemBySlot(slot, drops[random.getAsInt() & 3]);
        }
        this.writeDroppedItemBySlot(killed.slot(), 0x2E);
    }

    void handleTerminalEnemyDeath(RoomEntity entity, IntSupplier randomByteSupplier) {
        BossDeathBehavior.handleTerminalEnemyDeath(this, entity, randomByteSupplier);
    }

    public void advanceRollingBonesDestruction(RoomEntity entity, IntSupplier randomByteSupplier) {
        BossDeathBehavior.advanceRollingBonesDestruction(this, entity, randomByteSupplier);
    }

    public void advanceMoblinKingDestruction(RoomEntity source, int frame, IntSupplier random) {
        BossDeathBehavior.advanceMoblinKingDestruction(this, source, frame, random);
    }

    public void advanceMoldormDestruction(RoomEntity source, int frameCounter) {
        MoldormDeathBehavior.advanceMoldormDestruction(this, source, frameCounter);
    }

    void advanceMoldormBossExplosion(RoomEntity source) {
        MoldormDeathBehavior.advanceMoldormBossExplosion(this, source);
    }

    void spawnMoldormHeartContainer(RoomEntity source) {
        MoldormDeathBehavior.spawnMoldormHeartContainer(this, source);
    }

    void createMoldormPoof(RoomEntity source, int x, int y) {
        runtime.events().transientVfxRequests.add(new TransientVfxRequest(TransientVfxType.POOF, x, y));
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(
                        source.slot(), source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13));
    }

    public void advanceCollectedHeartContainer(RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ) {
        int slot = entity.slot();
        if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 1) {
            runtime.events().pendingHeartContainerRewards.add(new HeartContainerRewardEvent(slot));
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
            return;
        }
        runtime.events().pendingLinkHeldItemPoseRequests.add(new LinkHeldItemPoseRequest(slot));
        RoomEntity held =
                withPositionAndVariant(entity, linkEntityX, (linkEntityY - 0x0C) & 0xFF, entity.spriteVariant());
        runtime.slots()[slot] = withZ(held, linkZ & 0xFF);
    }

    void advanceBossKeyDropProducer(RoomEntity source, IntSupplier randomByteSupplier) {
        BossKeyDropBehavior.advanceBossKeyDropProducer(this, source, randomByteSupplier);
    }

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
}
