package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.List;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.HinoxLinkEffectRequest;
import linksawakening.world.entity.event.KeyQuicksandEvent;
import linksawakening.world.entity.event.KeyRewardEvent;
import linksawakening.world.entity.event.LikeLikeEvent;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.LinkFallenPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.RoomStatusPersistenceRequest;
import linksawakening.world.entity.event.RoosterLinkStateRequest;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.entity.event.SideViewPlatformLinkRequest;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.event.WarpLinkStateRequest;
import linksawakening.world.entity.event.WitchExchangeEvent;
import linksawakening.world.entity.event.WitchRewardEvent;
import linksawakening.world.entity.motion.*;

/** Mutable execution locals for one immutable entity frame input. */
public final class EntityFrameExecution {
    private final EntityFrameInputs inputs = new EntityFrameInputs();

    public EntityFrameInputs inputs() {
        return inputs;
    }

    private final EntityTimerGates gates = new EntityTimerGates();

    public EntityTimerGates gates() {
        return gates;
    }

    private final EntitySlotExecution slotFrame = new EntitySlotExecution();

    public EntitySlotExecution slotFrame() {
        return slotFrame;
    }

    private final EntityPresentationFlags presentation = new EntityPresentationFlags();

    public EntityPresentationFlags presentation() {
        return presentation;
    }

    private final RoomEntityRuntime runtime;

    RoomEntityRuntime runtime() {
        return runtime;
    }

    private final EntityFrameContext context;

    public EntityFrameContext context() {
        return context;
    }

    EntityFrameExecution(RoomEntityRuntime runtime, EntityFrameContext context) {
        this.runtime = runtime;
        this.context = context;
    }

    public linksawakening.entity.EntitySpriteHandlerCatalog sprites() {
        return runtime.spriteHandlers;
    }

    public EntityBackgroundMechanics background() {
        return runtime.backgroundMechanics;
    }

    public EntityLiftThrowController liftThrow() {
        return runtime.liftThrow;
    }

    public EntityLinkView link() {
        return runtime.entityState.linkView;
    }

    public EntitySlotView slot() {
        return runtime.entityState.slotView;
    }

    public void replaceEntity(RoomEntity replacement) {
        runtime.slots[this.slotFrame().index()] = replacement;
    }

    public List<RoomEntity> entities() {
        return runtime.slotEntities;
    }

    public RoomEntity entityAt(int slotIndex) {
        return runtime.slots[slotIndex];
    }

    public void emit(DialogRequest event) {
        runtime.entityState.events.pendingDialogRequests.add(event);
    }

    public void emit(DungeonWarpRequest event) {
        runtime.entityState.events.pendingDungeonWarpRequests.add(event);
    }

    public void emit(EntityCombatEvent event) {
        runtime.entityState.events.pendingEntityEvents.add(event);
    }

    public void emit(HinoxLinkEffectRequest event) {
        runtime.entityState.events.pendingHinoxLinkEffectRequests.add(event);
    }

    public void emit(KeyQuicksandEvent event) {
        runtime.entityState.events.pendingKeyQuicksandEvents.add(event);
    }

    public void emit(KeyRewardEvent event) {
        runtime.entityState.events.pendingKeyRewardEvents.add(event);
    }

    public void emit(LikeLikeEvent event) {
        runtime.entityState.events.pendingLikeLikeEvents.add(event);
    }

    public void emit(LinkAttackClearRequest event) {
        runtime.entityState.events.pendingLinkAttackClearRequests.add(event);
    }

    public void emit(LinkFacingRequest event) {
        runtime.entityState.events.pendingLinkFacingRequests.add(event);
    }

    public void emit(LinkFallenPoseRequest event) {
        runtime.entityState.events.pendingLinkFallenPoseRequests.add(event);
    }

    public void emit(LinkMotionBlockRequest event) {
        runtime.entityState.events.pendingLinkMotionBlockRequests.add(event);
    }

    public void emit(RoomStatusPersistenceRequest event) {
        runtime.entityState.events.pendingRoomStatusPersistenceRequests.add(event);
    }

    public void emit(RoosterLinkStateRequest event) {
        runtime.entityState.events.pendingRoosterLinkStateRequests.add(event);
    }

    public void emit(ScreenShakeRequest event) {
        runtime.entityState.events.pendingScreenShakeRequests.add(event);
    }

    public void emit(SideViewPlatformLinkRequest event) {
        runtime.entityState.events.pendingSideViewPlatformLinkRequests.add(event);
    }

    public void emit(SlimeKeyRewardEvent event) {
        runtime.entityState.events.pendingSlimeKeyRewardEvents.add(event);
    }

    public void emit(WarpLinkStateRequest event) {
        runtime.entityState.events.pendingWarpLinkStateRequests.add(event);
    }

    public void emit(WitchExchangeEvent event) {
        runtime.entityState.events.pendingWitchExchangeEvents.add(event);
    }

    public void emit(WitchRewardEvent event) {
        runtime.entityState.events.pendingWitchRewardEvents.add(event);
    }

    public void emit(TransientVfxRequest event) {
        runtime.entityState.events.transientVfxRequests.add(event);
    }

    public EntitySlotLifecycle lifecycle() {
        return runtime.lifecycle;
    }

    public EntityEnemySpawns enemySpawns() {
        return runtime.entityEnemySpawns;
    }

    public EntityPlayerSpawns playerSpawns() {
        return runtime.entityPlayerSpawns;
    }

    public EntityProjectileController projectiles() {
        return runtime.projectiles;
    }

    public EntitySlotProperties slotProperties() {
        return runtime.slotProperties;
    }

    public EntityBombInteractions bombInteractions() {
        return runtime.bombInteractions;
    }

    public EntityRewardController rewards() {
        return runtime.rewards;
    }

    public EntityStoryController story() {
        return runtime.story;
    }

    public SlimeEyeController slimeEye() {
        return runtime.slimeEye;
    }

    public GenieEntityController genie() {
        return runtime.genie;
    }

    public EntityPickupController pickups() {
        return runtime.pickups;
    }

    public EntityBossRewards bossRewards() {
        return runtime.bossRewards;
    }

    public EntityRoomInspection roomInspection() {
        return runtime.roomInspection;
    }

    public EntityEnemyInspection enemyInspection() {
        return runtime.enemyInspection;
    }

    public linksawakening.world.entity.motion.WarpMotion warpMotion() {
        return runtime.warpMotion;
    }

    public void requestMusic(int track) {
        runtime.entityState.presentationState.writePendingMusicTrack(track);
    }

    public void emit(linksawakening.world.entity.event.HookshotBridgeUpdate update) {
        runtime.entityState.events.hookshotBridgeUpdates.add(update);
    }

    public void emit(linksawakening.world.entity.motion.RoamingEnemyMotion.LaunchRequest request) {
        runtime.entityState.events.projectileLaunchRequests.add(request);
    }

    public <T> T motion(Class<T> type, java.util.function.Supplier<? extends T> factory) {
        return runtime.entityState.motions.get(type, factory);
    }
}
