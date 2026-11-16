package linksawakening.world.entity;

import java.util.List;
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
        this.pipeline = java.util.Objects.requireNonNull(pipeline);
        this.combat = java.util.Objects.requireNonNull(combat);
        this.rewards = java.util.Objects.requireNonNull(rewards);
        this.entityPlayerSpawns = java.util.Objects.requireNonNull(entityPlayerSpawns);
        this.entityEnemySpawns = java.util.Objects.requireNonNull(entityEnemySpawns);
        this.lifecycle = java.util.Objects.requireNonNull(lifecycle);
        this.effects = java.util.Objects.requireNonNull(effects);
        this.playerCommands = java.util.Objects.requireNonNull(playerCommands);
        this.backgroundMechanics = java.util.Objects.requireNonNull(backgroundMechanics);
        this.roomInspection = java.util.Objects.requireNonNull(roomInspection);
        this.entityRuntimeConfiguration = java.util.Objects.requireNonNull(entityRuntimeConfiguration);
        this.entityDebugConfiguration = java.util.Objects.requireNonNull(entityDebugConfiguration);
        this.enemyInspection = java.util.Objects.requireNonNull(enemyInspection);
        this.bossInspection = java.util.Objects.requireNonNull(bossInspection);
        this.flyingEnemyInspection = java.util.Objects.requireNonNull(flyingEnemyInspection);
        this.pickupInspection = java.util.Objects.requireNonNull(pickupInspection);
        this.frameExecution = java.util.Objects.requireNonNull(frameExecution);
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

    // Seam bridges - removed when world/room adopts the role accessors

    public boolean gelClingingToLink() {
        return enemies().gelClingingToLink();
    }

    public void setFreeMovementMode(boolean enabled) {
        configuration().setFreeMovementMode(enabled);
    }

    public static RoomEntityRuntimeBuilder builder(RoomEntitySnapshot initial) {
        return new RoomEntityRuntimeBuilder(initial);
    }

    public void setHeartPiecesCount(int count) {
        configuration().setHeartPiecesCount(count);
    }

    public int thrownDirection(int slot) {
        return commands().thrownDirection(slot);
    }

    public boolean beginLift(int slot, int romDirection) {
        return commands().beginLift(slot, romDirection);
    }

    public boolean throwLiftedEntity(int romDirection) {
        return commands().throwLiftedEntity(romDirection);
    }

    public boolean applyMarinWakeUpPresentation(int x, int y, int spriteVariant) {
        return commands().applyMarinWakeUpPresentation(x, y, spriteVariant);
    }

    public void updateHouseTarinPresentation(int linkX, int linkY) {
        commands().updateHouseTarinPresentation(linkX, linkY);
    }

    public void movePushedBlock(int slot, int x, int y) {
        commands().movePushedBlock(slot, x, y);
    }

    public void resolvePushedBlockLinkContact(int slot, int linkEntityX, int linkEntityY) {
        background().resolvePushedBlockLinkContact(slot, linkEntityX, linkEntityY);
    }

    public void removePushedBlock(int slot) {
        commands().removePushedBlock(slot);
    }
}
