package linksawakening.world.entity;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
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
import linksawakening.world.entity.motion.RoamingEnemyMotion;
import linksawakening.world.entity.motion.WarpMotion;

/** Owns acquisition/reset/release of ExecuteEntities scratch and the explicit behavior owners used by stages. */
public final class EntityFrameExecution {
    private final EntityFrameInputLatch inputs = new EntityFrameInputLatch();

    public EntityFrameInputLatch inputs() {
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

    private final EntitySlotProperties slotProperties;
    private final EntitySpriteViews spriteHandlers;
    private final EntityBackgroundMechanics backgroundMechanics;
    private final EntityLiftThrowController liftThrow;
    private final EntitySlots slots;
    private final List<RoomEntity> slotEntities;
    private final EntitySlotLifecycle lifecycle;
    private final EntityEnemySpawns entityEnemySpawns;
    private final EntityPlayerSpawns entityPlayerSpawns;
    private final EntityProjectileController projectiles;
    private final EntityBombInteractions bombInteractions;
    private final EntityRewardController rewards;
    private final EntityStoryController story;
    private final SlimeEyeController slimeEye;
    private final GenieEntityController genie;
    private final EntityPickupController pickups;
    private final EntityBossRewards bossRewards;
    private final EntityRoomInspection roomInspection;
    private final EntityEnemyInspection enemyInspection;
    private final WarpMotion warpMotion;
    private final EntityHandlerRegistry entityHandlers;
    private final EntityLinkView linkView;
    private final EntitySlotView slotView;
    private final EntityEventChannels events;
    private final EntityPresentationState presentationState;
    private final EntityMotionRegistry motions;
    private final EntityCombatTables combatTables;
    private final EntityPhysicsTables physicsTables;
    private final EntityRoomVariables roomVariables;
    private final EntityLinkState linkState;
    private final boolean indoorRoom;
    private final RomEnemyCombatTables enemyCombatTables;
    private final EntityCombatResolver combat;
    private final EntityPlayerCommands playerCommands;

    EntityFrameExecution(
            EntitySlotProperties slotProperties,
            EntitySpriteViews spriteHandlers,
            EntityBackgroundMechanics backgroundMechanics,
            EntityLiftThrowController liftThrow,
            EntitySlots slots,
            EntitySlotLifecycle lifecycle,
            EntityEnemySpawns entityEnemySpawns,
            EntityPlayerSpawns entityPlayerSpawns,
            EntityProjectileController projectiles,
            EntityBombInteractions bombInteractions,
            EntityRewardController rewards,
            EntityStoryController story,
            SlimeEyeController slimeEye,
            GenieEntityController genie,
            EntityPickupController pickups,
            EntityBossRewards bossRewards,
            EntityRoomInspection roomInspection,
            EntityEnemyInspection enemyInspection,
            WarpMotion warpMotion,
            EntityHandlerRegistry entityHandlers,
            EntityLinkView linkView,
            EntitySlotView slotView,
            EntityEventChannels events,
            EntityPresentationState presentationState,
            EntityMotionRegistry motions,
            EntityRoomVariables roomVariables,
            EntityLinkState linkState,
            boolean indoorRoom,
            RomEnemyCombatTables enemyCombatTables,
            EntityCombatResolver combat,
            EntityPlayerCommands playerCommands) {
        List<RoomEntity> slotEntities = slots.view();
        EntityCombatTables combatTables = slots.combat();
        EntityPhysicsTables physicsTables = slots.physics();
        this.slotProperties = Objects.requireNonNull(slotProperties, "slotProperties");
        this.spriteHandlers = Objects.requireNonNull(spriteHandlers, "spriteHandlers");
        this.backgroundMechanics = Objects.requireNonNull(backgroundMechanics, "backgroundMechanics");
        this.liftThrow = Objects.requireNonNull(liftThrow, "liftThrow");
        this.slots = Objects.requireNonNull(slots, "slots");
        this.slotEntities = Objects.requireNonNull(slotEntities, "slotEntities");
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        this.entityEnemySpawns = Objects.requireNonNull(entityEnemySpawns, "entityEnemySpawns");
        this.entityPlayerSpawns = Objects.requireNonNull(entityPlayerSpawns, "entityPlayerSpawns");
        this.projectiles = Objects.requireNonNull(projectiles, "projectiles");
        this.bombInteractions = Objects.requireNonNull(bombInteractions, "bombInteractions");
        this.rewards = Objects.requireNonNull(rewards, "rewards");
        this.story = Objects.requireNonNull(story, "story");
        this.slimeEye = Objects.requireNonNull(slimeEye, "slimeEye");
        this.genie = Objects.requireNonNull(genie, "genie");
        this.pickups = Objects.requireNonNull(pickups, "pickups");
        this.bossRewards = Objects.requireNonNull(bossRewards, "bossRewards");
        this.roomInspection = Objects.requireNonNull(roomInspection, "roomInspection");
        this.enemyInspection = Objects.requireNonNull(enemyInspection, "enemyInspection");
        this.warpMotion = Objects.requireNonNull(warpMotion, "warpMotion");
        this.entityHandlers = Objects.requireNonNull(entityHandlers, "entityHandlers");
        this.linkView = Objects.requireNonNull(linkView, "linkView");
        this.slotView = Objects.requireNonNull(slotView, "slotView");
        this.events = Objects.requireNonNull(events, "events");
        this.presentationState = Objects.requireNonNull(presentationState, "presentationState");
        this.motions = Objects.requireNonNull(motions, "motions");
        this.combatTables = Objects.requireNonNull(combatTables, "combatTables");
        this.physicsTables = Objects.requireNonNull(physicsTables, "physicsTables");
        this.roomVariables = Objects.requireNonNull(roomVariables, "roomVariables");
        this.linkState = Objects.requireNonNull(linkState, "linkState");
        this.indoorRoom = indoorRoom;
        this.enemyCombatTables = Objects.requireNonNull(enemyCombatTables, "enemyCombatTables");
        this.combat = Objects.requireNonNull(combat, "combat");
        this.playerCommands = Objects.requireNonNull(playerCommands, "playerCommands");
    }

    // Absent between acquisitions; release deliberately drops the previous frame input.
    private EntityFrameContext context;
    private boolean inUse;

    public EntityFrameContext context() {
        return context;
    }

    public EntitySpriteViews spriteViews() {
        return slotProperties.spriteViews();
    }

    public EntitySpriteViews sprites() {
        return spriteHandlers;
    }

    public EntityBackgroundMechanics background() {
        return backgroundMechanics;
    }

    public EntityLiftThrowController liftThrow() {
        return liftThrow;
    }

    public EntityLinkView link() {
        return linkView;
    }

    public EntitySlotView slot() {
        return slotView;
    }

    public void replaceEntity(RoomEntity replacement) {
        slots.replace(this.slotFrame().index(), replacement);
    }

    public List<RoomEntity> entities() {
        return slotEntities;
    }

    public RoomEntity entityAt(int slotIndex) {
        return slots.get(slotIndex);
    }

    public void emit(DialogRequest event) {
        events.dialog(event);
    }

    public void emit(DungeonWarpRequest event) {
        events.dungeonWarp(event);
    }

    public void emit(EntityCombatEvent event) {
        events.combat(event);
    }

    public void emit(HinoxLinkEffectRequest event) {
        events.hinoxLinkEffect(event);
    }

    public void emit(KeyQuicksandEvent event) {
        events.keyQuicksand(event);
    }

    public void emit(KeyRewardEvent event) {
        events.keyReward(event);
    }

    public void emit(LikeLikeEvent event) {
        events.likeLike(event);
    }

    public void emit(LinkAttackClearRequest event) {
        events.linkAttackClear(event);
    }

    public void emit(LinkFacingRequest event) {
        events.linkFacing(event);
    }

    public void emit(LinkFallenPoseRequest event) {
        events.linkFallenPose(event);
    }

    public void emit(LinkMotionBlockRequest event) {
        events.linkMotionBlock(event);
    }

    public void emit(RoomStatusPersistenceRequest event) {
        events.roomStatusPersistence(event);
    }

    public void emit(RoosterLinkStateRequest event) {
        events.roosterLinkState(event);
    }

    public void emit(ScreenShakeRequest event) {
        events.screenShake(event);
    }

    public void emit(SideViewPlatformLinkRequest event) {
        events.sideViewPlatformLink(event);
    }

    public void emit(SlimeKeyRewardEvent event) {
        events.slimeKeyReward(event);
    }

    public void emit(WarpLinkStateRequest event) {
        events.warpLinkState(event);
    }

    public void emit(WitchExchangeEvent event) {
        events.witchExchange(event);
    }

    public void emit(WitchRewardEvent event) {
        events.witchReward(event);
    }

    public void emit(TransientVfxRequest event) {
        events.vfx(event);
    }

    public EntitySlotLifecycle lifecycle() {
        return lifecycle;
    }

    public EntityEnemySpawns enemySpawns() {
        return entityEnemySpawns;
    }

    public EntityPlayerSpawns playerSpawns() {
        return entityPlayerSpawns;
    }

    public EntityProjectileController projectiles() {
        return projectiles;
    }

    public EntitySlotProperties slotProperties() {
        return slotProperties;
    }

    public EntityBombInteractions bombInteractions() {
        return bombInteractions;
    }

    public EntityRewardController rewards() {
        return rewards;
    }

    public EntityStoryController story() {
        return story;
    }

    public SlimeEyeController slimeEye() {
        return slimeEye;
    }

    public GenieEntityController genie() {
        return genie;
    }

    public EntityPickupController pickups() {
        return pickups;
    }

    public EntityBossRewards bossRewards() {
        return bossRewards;
    }

    public EntityRoomInspection roomInspection() {
        return roomInspection;
    }

    public EntityEnemyInspection enemyInspection() {
        return enemyInspection;
    }

    public WarpMotion warpMotion() {
        return warpMotion;
    }

    public void requestMusic(int track) {
        presentationState.writePendingMusicTrack(track);
    }

    public void emit(linksawakening.world.entity.event.HookshotBridgeUpdate update) {
        events.hookshotBridge(update);
    }

    public void emit(RoamingEnemyMotion.LaunchRequest request) {
        events.projectileLaunch(request);
    }

    public <T> T motion(Class<T> type, Supplier<? extends T> factory) {
        return motions.get(type, factory);
    }

    EntityCombatTables combatTables() {
        return combatTables;
    }

    EntityPhysicsTables physicsTables() {
        return physicsTables;
    }

    EntityRoomVariables roomVariables() {
        return roomVariables;
    }

    EntityLinkState linkState() {
        return linkState;
    }

    EntityEventChannels events() {
        return events;
    }

    EntityHandlerRegistry handlers() {
        return entityHandlers;
    }

    EntitySlots slots() {
        return slots;
    }

    boolean indoorRoom() {
        return indoorRoom;
    }

    RomEnemyCombatTables enemyCombatTables() {
        return enemyCombatTables;
    }

    EntityCombatResolver combat() {
        return combat;
    }

    EntityPlayerCommands playerCommands() {
        return playerCommands;
    }

    EntityFrameExecution acquire(EntityFrameContext next) {
        if (inUse)
            return new EntityFrameExecution(
                            slotProperties,
                            spriteHandlers,
                            backgroundMechanics,
                            liftThrow,
                            slots,
                            lifecycle,
                            entityEnemySpawns,
                            entityPlayerSpawns,
                            projectiles,
                            bombInteractions,
                            rewards,
                            story,
                            slimeEye,
                            genie,
                            pickups,
                            bossRewards,
                            roomInspection,
                            enemyInspection,
                            warpMotion,
                            entityHandlers,
                            linkView,
                            slotView,
                            events,
                            presentationState,
                            motions,
                            roomVariables,
                            linkState,
                            indoorRoom,
                            enemyCombatTables,
                            combat,
                            playerCommands)
                    .acquire(next);
        inUse = true;
        context = next;
        inputs.reset();
        gates.reset();
        slotFrame.reset();
        presentation.reset();
        return this;
    }

    void release() {
        context = null;
        inUse = false;
    }

    private final EntityMotionRegistry scratch = new EntityMotionRegistry();

    public <T> T scratch(Class<T> type, Function<EntityFrameExecution, ? extends T> factory) {
        return scratch.get(type, this, factory);
    }

    private final EntityFrameCollision collision = new EntityFrameCollision(this);

    EntityFrameCollision collisionAdapters() {
        return collision;
    }
}
