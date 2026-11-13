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
    final EntityBossState state;

    EnemyDropResolver.Result resolveDrop(EnemyDropResolver.Context context) {
        return state.readEnemyDropLowHealth() == null
                ? state.readEnemyDropResolver().resolve(context)
                : state.readEnemyDropResolver().resolve(context, state.readEnemyDropLowHealth());
    }

    final EntityBossRewardsServices runtime;

    EntityBossRewards(EntityBossState state, EntityBossRewardsServices runtime) {
        this.state = java.util.Objects.requireNonNull(state, "state");
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
            state.writeDroppedItemBySlot(slot, drops[random.getAsInt() & 3]);
        }
        state.writeDroppedItemBySlot(killed.slot(), 0x2E);
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

    final BossKeyDropStep bossKeyDropStep = new BossKeyDropStep();
}
