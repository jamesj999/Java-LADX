package linksawakening.world.entity;

import java.util.List;
import java.util.Objects;
import linksawakening.world.entity.event.EntityProjectileEvent;

/** Room-facing entity execution and access to the collaborators owning each operation. */
public final class RoomEntityRuntime {
    private final EntitySlotPipeline pipeline;
    private final EntityCombatResolver combat;
    private final EntityRewardController rewards;
    private final EntityPlayerSpawns entityPlayerSpawns;
    private final EntityEnemySpawns entityEnemySpawns;
    private final EntitySlotLifecycle lifecycle;
    private final EntityEffectConsumption effects;
    private final EntityPlayerCommands playerCommands;
    private final EntityBackgroundMechanics backgroundMechanics;
    private final EntityRoomInspection roomInspection;
    private final EntityRuntimeConfiguration entityRuntimeConfiguration;
    private final EntityDebugConfiguration entityDebugConfiguration;
    private final EntityEnemyInspection enemyInspection;
    private final EntityBossInspection bossInspection;
    private final EntityFlyingEnemyInspection flyingEnemyInspection;
    private final EntityPickupInspection pickupInspection;
    private final EntityFrameExecution frameExecution;

    RoomEntityRuntime(
            EntitySlotPipeline pipeline,
            EntityCombatResolver combat,
            EntityRewardController rewards,
            EntityPlayerSpawns entityPlayerSpawns,
            EntityEnemySpawns entityEnemySpawns,
            EntitySlotLifecycle lifecycle,
            EntityEffectConsumption effects,
            EntityPlayerCommands playerCommands,
            EntityBackgroundMechanics backgroundMechanics,
            EntityRoomInspection roomInspection,
            EntityRuntimeConfiguration entityRuntimeConfiguration,
            EntityDebugConfiguration entityDebugConfiguration,
            EntityEnemyInspection enemyInspection,
            EntityBossInspection bossInspection,
            EntityFlyingEnemyInspection flyingEnemyInspection,
            EntityPickupInspection pickupInspection,
            EntityFrameExecution frameExecution) {
        this.pipeline = Objects.requireNonNull(pipeline);
        this.combat = Objects.requireNonNull(combat);
        this.rewards = Objects.requireNonNull(rewards);
        this.entityPlayerSpawns = Objects.requireNonNull(entityPlayerSpawns);
        this.entityEnemySpawns = Objects.requireNonNull(entityEnemySpawns);
        this.lifecycle = Objects.requireNonNull(lifecycle);
        this.effects = Objects.requireNonNull(effects);
        this.playerCommands = Objects.requireNonNull(playerCommands);
        this.backgroundMechanics = Objects.requireNonNull(backgroundMechanics);
        this.roomInspection = Objects.requireNonNull(roomInspection);
        this.entityRuntimeConfiguration = Objects.requireNonNull(entityRuntimeConfiguration);
        this.entityDebugConfiguration = Objects.requireNonNull(entityDebugConfiguration);
        this.enemyInspection = Objects.requireNonNull(enemyInspection);
        this.bossInspection = Objects.requireNonNull(bossInspection);
        this.flyingEnemyInspection = Objects.requireNonNull(flyingEnemyInspection);
        this.pickupInspection = Objects.requireNonNull(pickupInspection);
        this.frameExecution = Objects.requireNonNull(frameExecution);
    }

    public List<EntityProjectileEvent> tick(EntityFrameContext context) {
        return pipeline.tick(context);
    }

    EntitySlotPipeline pipeline() {
        return pipeline;
    }

    public EntityCombatResolver combat() {
        return combat;
    }

    public EntityRewardController pickups() {
        return rewards;
    }

    public EntityPlayerSpawns spawns() {
        return entityPlayerSpawns;
    }

    public EntityEnemySpawns enemySpawns() {
        return entityEnemySpawns;
    }

    public EntitySlotLifecycle lifecycle() {
        return lifecycle;
    }

    public EntityEffectConsumption outbox() {
        return effects;
    }

    public EntityPlayerCommands commands() {
        return playerCommands;
    }

    public EntityBackgroundMechanics background() {
        return backgroundMechanics;
    }

    public EntityRoomInspection inspection() {
        return roomInspection;
    }

    public EntityRuntimeConfiguration configuration() {
        return entityRuntimeConfiguration;
    }

    public EntityDebugConfiguration debug() {
        return entityDebugConfiguration;
    }

    public EntityEnemyInspection enemies() {
        return enemyInspection;
    }

    EntityBossInspection bosses() {
        return bossInspection;
    }

    EntityFlyingEnemyInspection flyingEnemies() {
        return flyingEnemyInspection;
    }

    EntityPickupInspection pickupState() {
        return pickupInspection;
    }

    EntityFrameExecution execution() {
        return frameExecution;
    }
}
