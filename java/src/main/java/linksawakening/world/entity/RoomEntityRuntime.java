package linksawakening.world.entity;

import java.util.List;
import linksawakening.world.entity.event.BombExplosionEvent;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
import linksawakening.world.entity.event.HeartPiecePickupUpdate;
import linksawakening.world.entity.event.HinoxLinkEffectRequest;
import linksawakening.world.entity.event.InstrumentCompletionEvent;
import linksawakening.world.entity.event.InstrumentRewardEvent;
import linksawakening.world.entity.event.KeyQuicksandEvent;
import linksawakening.world.entity.event.KeyRewardEvent;
import linksawakening.world.entity.event.LikeLikeEvent;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.LinkAttackStepRequest;
import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.LinkFallenPoseRequest;
import linksawakening.world.entity.event.LinkFinalPositionRequest;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.LinkSwordFinalPoseRequest;
import linksawakening.world.entity.event.LinkSwordSpinPoseRequest;
import linksawakening.world.entity.event.MadamMeowMeowFullHealRequest;
import linksawakening.world.entity.event.OwlEventCompletion;
import linksawakening.world.entity.event.RichardProgressEvent;
import linksawakening.world.entity.event.RoomStatusPersistenceRequest;
import linksawakening.world.entity.event.RoosterLinkStateRequest;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.entity.event.SideViewPlatformLinkRequest;
import linksawakening.world.entity.event.SideViewPotLinkRequest;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;
import linksawakening.world.entity.event.SwordPickupRewardEvent;
import linksawakening.world.entity.event.ThrownBackgroundCollisionEvent;
import linksawakening.world.entity.event.ToadstoolRewardEvent;
import linksawakening.world.entity.event.WarpLinkStateRequest;
import linksawakening.world.entity.event.WitchExchangeEvent;
import linksawakening.world.entity.event.WitchRewardEvent;

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

    public int clearEntity(int slot) {
        return lifecycle().clearEntity(slot);
    }

    public void setHeartPiecesCount(int count) {
        configuration().setHeartPiecesCount(count);
    }

    public int thrownDirection(int slot) {
        return commands().thrownDirection(slot);
    }

    public EntityPickupEvent collectIfNeeded(EntityPickupFrameInputs inputs) {
        return pickups().collectIfNeeded(inputs);
    }

    public boolean consumePendingSwitchBlockAnimationRequest() {
        return outbox().consumePendingSwitchBlockAnimationRequest();
    }

    public int consumePendingClearedEntityMask() {
        return outbox().consumePendingClearedEntityMask();
    }

    public int consumePendingRoomStatusMask() {
        return outbox().consumePendingRoomStatusMask();
    }

    public List<EntityCombatEvent> consumePendingEntityEvents() {
        return outbox().consumePendingEntityEvents();
    }

    public List<ThrownBackgroundCollisionEvent> consumePendingThrownBackgroundCollisions() {
        return outbox().consumePendingThrownBackgroundCollisions();
    }

    public List<DungeonWarpRequest> consumePendingDungeonWarpRequests() {
        return outbox().consumePendingDungeonWarpRequests();
    }

    public List<WarpLinkStateRequest> consumePendingWarpLinkStateRequests() {
        return outbox().consumePendingWarpLinkStateRequests();
    }

    public List<LinkFinalPositionRequest> consumePendingLinkFinalPositionRequests() {
        return outbox().consumePendingLinkFinalPositionRequests();
    }

    public List<RoosterLinkStateRequest> consumePendingRoosterLinkStateRequests() {
        return outbox().consumePendingRoosterLinkStateRequests();
    }

    public List<LinkMotionBlockRequest> consumePendingLinkMotionBlockRequests() {
        return outbox().consumePendingLinkMotionBlockRequests();
    }

    public List<SideViewPlatformLinkRequest> consumePendingSideViewPlatformLinkRequests() {
        return outbox().consumePendingSideViewPlatformLinkRequests();
    }

    public List<SideViewPotLinkRequest> consumePendingSideViewPotLinkRequests() {
        return outbox().consumePendingSideViewPotLinkRequests();
    }

    public List<HinoxLinkEffectRequest> consumePendingHinoxLinkEffectRequests() {
        return outbox().consumePendingHinoxLinkEffectRequests();
    }

    public List<LinkFallenPoseRequest> consumePendingLinkFallenPoseRequests() {
        return outbox().consumePendingLinkFallenPoseRequests();
    }

    public List<LinkFacingRequest> consumePendingLinkFacingRequests() {
        return outbox().consumePendingLinkFacingRequests();
    }

    public List<LinkAttackClearRequest> consumePendingLinkAttackClearRequests() {
        return outbox().consumePendingLinkAttackClearRequests();
    }

    public List<LinkAttackStepRequest> consumePendingLinkAttackStepRequests() {
        return outbox().consumePendingLinkAttackStepRequests();
    }

    public List<RoomStatusPersistenceRequest> consumePendingRoomStatusPersistenceRequests() {
        return outbox().consumePendingRoomStatusPersistenceRequests();
    }

    public List<LinkHeldItemPoseRequest> consumePendingLinkHeldItemPoseRequests() {
        return outbox().consumePendingLinkHeldItemPoseRequests();
    }

    public List<LinkSwordSpinPoseRequest> consumePendingLinkSwordSpinPoseRequests() {
        return outbox().consumePendingLinkSwordSpinPoseRequests();
    }

    public List<LinkSwordFinalPoseRequest> consumePendingLinkSwordFinalPoseRequests() {
        return outbox().consumePendingLinkSwordFinalPoseRequests();
    }

    public List<ScreenShakeRequest> consumePendingScreenShakeRequests() {
        return outbox().consumePendingScreenShakeRequests();
    }

    public List<ChestRewardEvent> consumePendingChestRewardEvents() {
        return outbox().consumePendingChestRewardEvents();
    }

    public List<KeyRewardEvent> consumePendingKeyRewardEvents() {
        return outbox().consumePendingKeyRewardEvents();
    }

    public List<SlimeKeyRewardEvent> consumePendingSlimeKeyRewardEvents() {
        return outbox().consumePendingSlimeKeyRewardEvents();
    }

    public List<KeyQuicksandEvent> consumePendingKeyQuicksandEvents() {
        return outbox().consumePendingKeyQuicksandEvents();
    }

    public int consumePendingMusicTrack() {
        return outbox().consumePendingMusicTrack();
    }

    public int consumePendingMusicFadeOutCountdown() {
        return outbox().consumePendingMusicFadeOutCountdown();
    }

    public List<LikeLikeEvent> consumePendingLikeLikeEvents() {
        return outbox().consumePendingLikeLikeEvents();
    }

    public List<DialogRequest> consumePendingDialogRequests() {
        return outbox().consumePendingDialogRequests();
    }

    public List<BombExplosionEvent> consumeBombExplosionEvents() {
        return outbox().consumeBombExplosionEvents();
    }

    public List<HeartPiecePickupUpdate> consumeHeartPieceUpdates() {
        return outbox().consumeHeartPieceUpdates();
    }

    public List<HeartContainerRewardEvent> consumePendingHeartContainerRewards() {
        return outbox().consumePendingHeartContainerRewards();
    }

    public List<MadamMeowMeowFullHealRequest> consumePendingMadamMeowMeowFullHealRequests() {
        return outbox().consumePendingMadamMeowMeowFullHealRequests();
    }

    public List<SwordPickupRewardEvent> consumePendingSwordPickupRewards() {
        return outbox().consumePendingSwordPickupRewards();
    }

    public List<ToadstoolRewardEvent> consumePendingToadstoolRewards() {
        return outbox().consumePendingToadstoolRewards();
    }

    public List<InstrumentRewardEvent> consumePendingInstrumentRewards() {
        return outbox().consumePendingInstrumentRewards();
    }

    public List<InstrumentCompletionEvent> consumePendingInstrumentCompletions() {
        return outbox().consumePendingInstrumentCompletions();
    }

    public List<RichardProgressEvent> consumePendingRichardProgressEvents() {
        return outbox().consumePendingRichardProgressEvents();
    }

    public List<WitchExchangeEvent> consumePendingWitchExchangeEvents() {
        return outbox().consumePendingWitchExchangeEvents();
    }

    public List<WitchRewardEvent> consumePendingWitchRewardEvents() {
        return outbox().consumePendingWitchRewardEvents();
    }

    public List<OwlEventCompletion> consumePendingOwlEventCompletions() {
        return outbox().consumePendingOwlEventCompletions();
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

    public int spawnHookshotChain(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return spawns().spawnHookshotChain(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    public int spawnPushedBlock(int x, int y) {
        return spawns().spawnPushedBlock(x, y);
    }

    public int spawnLiftedRoomObject(int x, int y, int romDirection, int spriteVariant) {
        return spawns().spawnLiftedRoomObject(x, y, romDirection, spriteVariant);
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

    public int spawnRoomEventKeyDrop() {
        return spawns().spawnRoomEventKeyDrop();
    }

    public int spawnArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        return spawns().spawnArrow(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower);
    }

    public int spawnBoomerang(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, int pressedButtonsMask) {
        return spawns().spawnBoomerang(linkEntityX, linkEntityY, linkEntityZ, romDirection, pressedButtonsMask);
    }

    public int spawnSwordBeam(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return spawns().spawnSwordBeam(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    public int spawnMagicRodFireball(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        return spawns().spawnMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower);
    }

    public int spawnMagicPowderSprinkle(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return spawns().spawnMagicPowderSprinkle(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    public int spawnBomb(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return spawns().spawnBomb(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    public int spawnLiftableRockSmash(int entityX, int entityY, int sourceSpriteVariant) {
        return spawns().spawnLiftableRockSmash(entityX, entityY, sourceSpriteVariant);
    }

    public int spawnLiftableRockRubble(int entityX, int entityY) {
        return spawns().spawnLiftableRockRubble(entityX, entityY);
    }

    public int spawnShovelDrop(int objectLeft, int objectTop, int linkEntityX, int linkEntityY, boolean allowDrop) {
        return spawns().spawnShovelDrop(objectLeft, objectTop, linkEntityX, linkEntityY, allowDrop);
    }

    public int spawnChestWithItem(int objectLeft, int objectTop, int itemType) {
        return spawns().spawnChestWithItem(objectLeft, objectTop, itemType);
    }
}
