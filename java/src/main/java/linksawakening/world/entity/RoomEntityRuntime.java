package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySlotValidation.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.List;
import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.rom.EntityPropertyTables;
import linksawakening.rom.Rom;
import linksawakening.rom.RomTables;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.WarpMotion;
import linksawakening.world.room.ChestContentsTable;

/**
 * Per-room composition root for ExecuteEntities and EntityInitHandler.
 * Owns shared slot tables, handler services and reusable frame scratch;
 * entity-specific state belongs to motions. Slots remain immutable values.
 */
public final class RoomEntityRuntime
        implements EntityRuntimeEffects, EntityRuntimeCombat, EntityRuntimeSpawns, EntityRuntimePlayerCommands {
    final GenieEntityController.State genieState = new GenieEntityController.State();
    final SlimeEyeController.State slimeEyeState = new SlimeEyeController.State();
    final EntityBombState bombInteractionsState = new EntityBombState();
    final EntityStoryState storyState = new EntityStoryState();
    final EntityLiftState liftThrowState = new EntityLiftState();
    final EntityRewardState rewardsState = new EntityRewardState();
    final EntityPickupState pickupsState = new EntityPickupState();
    final EntityBossState bossRewardsState = new EntityBossState();
    final EntitySlotPresentation slotPropertiesState;
    final EntityProjectileState projectileState = new EntityProjectileState();
    final EntityWarpController warpState = new EntityWarpController();
    final EntityRuntimeState entityState = new EntityRuntimeState();
    final EntityFrameExecution frameExecution = new EntityFrameExecution(this, null);

    private final EntityPlayerCommands playerCommands;

    final EntityRoomInitialization roomInitialization;
    final EntitySlotProperties slotProperties;
    final EntityBackgroundMechanics backgroundMechanics;
    final EntityBombInteractions bombInteractions;
    final EntitySlotLifecycle lifecycle;
    final EntityEnemySpawns entityEnemySpawns;
    final EntityPlayerSpawns entityPlayerSpawns;
    private final EntityEffectConsumption effects;
    private final EntityRuntimeConfiguration entityRuntimeConfiguration;
    final EntityDebugConfiguration entityDebugConfiguration;
    final EntityHandlerRegistry entityHandlers;
    final EntityRewardController rewards;
    final EntityStoryController story;
    final SlimeEyeController slimeEye;
    final GenieEntityController genie;
    final EntityProjectileController projectiles;
    final EntityPickupController pickups;
    final EntityLiftThrowController liftThrow;
    private final EntityCombatResolver combat;
    final EntityBossRewards bossRewards;
    final EntityRoomInspection roomInspection;
    final EntityBossInspection bossInspection;
    final EntityFlyingEnemyInspection flyingEnemyInspection;
    final EntityPickupInspection pickupInspection;
    final EntityEnemyInspection enemyInspection;

    final RoomEntity[] slots;
    final java.util.List<RoomEntity> slotEntities;

    final EntitySpriteTileSnapshot spriteTiles;
    final boolean indoorRoom;
    private final EntityPropertyTables entityProperties;
    final IntSupplier defaultRandomByteSupplier;
    private final EntityFrameRandom frameRandom;
    final EntitySpriteHandlerCatalog spriteHandlers;
    final RomEnemyCombatTables enemyCombatTables;
    final ChestContentsTable chestContentsTable;

    public boolean gelClingingToLink() {
        return entityState.motions.get(ZolGelMotion.class, ZolGelMotion::new).readGelClingingToLink();
    }

    public void setFreeMovementMode(boolean enabled) {
        entityState.roomVariables.writeFreeMovementMode(enabled);
    }

    final WarpMotion warpMotion;

    public EntityEnemyInspection enemies() {
        return enemyInspection;
    }

    public EntityRoomInspection inspection() {
        return roomInspection;
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

    public EntityDebugConfiguration debug() {
        return entityDebugConfiguration;
    }

    public EntityRuntimeConfiguration configuration() {
        return entityRuntimeConfiguration;
    }

    RoomEntityRuntime(RoomEntityRuntimeBuilder builder) {

        RoomEntitySnapshot initial = builder.initial();
        boolean indoorRoom = builder.indoorRoom();
        IntSupplier defaultRandomByteSupplier = builder.randomByteSupplier();
        EntitySpriteHandlerCatalog spriteHandlers = builder.spriteHandlers();
        RomEnemyCombatTables enemyCombatTables = builder.enemyCombatTables();
        ChestContentsTable chestContentsTable = builder.chestContentsTable();
        RomTables romTables = builder.romTables();
        Rom romData = builder.rom();

        this.slots = initial.slots().toArray(RoomEntity[]::new);
        this.slotEntities = java.util.Arrays.asList(slots);
        this.entityState.presentationState.writeSpriteSelection(initial.spriteSelection());
        this.spriteTiles = initial.spriteTiles();
        this.entityState.physicsTables.writeGroundInteractionSideScrolling(initial.sideScrolling());
        this.indoorRoom = indoorRoom;
        this.defaultRandomByteSupplier = defaultRandomByteSupplier;

        this.spriteHandlers = spriteHandlers;
        this.enemyCombatTables = enemyCombatTables;
        this.chestContentsTable = chestContentsTable;
        this.entityProperties = romTables == null ? null : romTables.entityPropertyTables();
        this.slotPropertiesState = new EntitySlotPresentation(new EntitySpriteViews(spriteHandlers));
        this.entityDebugConfiguration = new EntityDebugConfiguration(entityState.combatTables, entityState.motions);
        this.effects = new EntityEffectConsumption(
                entityState.events, entityState.presentationState, entityState.roomVariables);
        this.flyingEnemyInspection = new EntityFlyingEnemyInspection(entityState.motions, entityState.physicsTables);
        this.frameRandom = new EntityFrameRandom(defaultRandomByteSupplier);
        this.pickupInspection =
                new EntityPickupInspection(bombInteractionsState, entityState.motions, entityState.physicsTables);
        this.roomInspection = new EntityRoomInspection(
                warpState,
                indoorRoom,
                spriteHandlers,
                spriteTiles,
                bombInteractionsState,
                bossRewardsState,
                entityState.combatTables,
                entityState.events,
                liftThrowState,
                entityState.linkState,
                entityState.motions,
                entityState.physicsTables,
                pickupsState,
                entityState.presentationState,
                projectileState,
                rewardsState,
                entityState.roomVariables,
                slotPropertiesState,
                storyState,
                enemyCombatTables,
                slots);
        this.enemyInspection = new EntityEnemyInspection(
                bossRewardsState,
                entityState.combatTables,
                entityState.events,
                liftThrowState,
                entityState.linkState,
                entityState.motions,
                entityState.physicsTables,
                roomInspection,
                roomInspection,
                entityState.roomVariables,
                slotPropertiesState,
                slots);
        this.lifecycle = new EntitySlotLifecycle(
                warpState,
                bombInteractionsState,
                bossRewardsState,
                entityState.combatTables,
                enemyInspection,
                entityState.events,
                liftThrowState,
                entityState.linkState,
                entityState.motions,
                entityState.physicsTables,
                pickupsState,
                entityState.presentationState,
                projectileState,
                rewardsState,
                entityState.roomVariables,
                storyState,
                genieState,
                enemyCombatTables,
                slots,
                slimeEyeState,
                entityProperties,
                roomInspection,
                slotPropertiesState);
        this.bossInspection = new EntityBossInspection(
                bossRewardsState, enemyInspection, entityState.motions, entityState.physicsTables);
        this.slotProperties = new EntitySlotProperties(
                slotPropertiesState,
                indoorRoom,
                spriteHandlers,
                bossRewardsState,
                entityState.combatTables,
                entityState.events,
                liftThrowState,
                entityState.linkState,
                entityState.motions,
                entityState.physicsTables,
                entityState.presentationState,
                projectileState,
                roomInspection,
                roomInspection,
                entityState.roomVariables,
                slots,
                lifecycle);
        this.pickups = new EntityPickupController(
                pickupsState,
                indoorRoom,
                bossRewardsState,
                entityState.combatTables,
                entityState.events,
                entityState.linkState,
                entityState.motions,
                entityState.physicsTables,
                entityState.presentationState,
                entityState.roomVariables,
                lifecycle,
                slotProperties,
                storyState,
                slots,
                chestContentsTable);
        this.playerCommands = new EntityPlayerCommands(
                bombInteractionsState,
                entityState.combatTables,
                entityState.events,
                liftThrowState,
                entityState.motions,
                entityState.physicsTables,
                entityState.presentationState,
                rewardsState,
                roomInspection,
                slotProperties,
                slots);
        this.backgroundMechanics = new EntityBackgroundMechanics(
                spriteHandlers,
                bossRewardsState,
                entityState.combatTables,
                enemyInspection,
                entityState.events,
                entityState.motions,
                entityState.physicsTables,
                roomInspection,
                entityState.roomVariables,
                slots,
                slotProperties.spriteViews(),
                lifecycle);
        this.bossRewards = new EntityBossRewards(
                bossRewardsState,
                spriteHandlers,
                backgroundMechanics,
                entityState.combatTables,
                entityState.events,
                entityState.motions,
                entityState.physicsTables,
                pickups,
                roomInspection,
                entityState.roomVariables,
                lifecycle,
                slotProperties,
                slots,
                entityState.linkState);
        this.entityEnemySpawns = new EntityEnemySpawns(
                spriteHandlers,
                bombInteractionsState,
                bossRewardsState,
                entityState.combatTables,
                entityState.events,
                liftThrowState,
                entityState.motions,
                entityState.physicsTables,
                pickups,
                slotProperties,
                slots,
                lifecycle);
        this.rewards = new EntityRewardController(
                rewardsState,
                entityState.motions,
                spriteHandlers,
                entityState.combatTables,
                entityState.events,
                entityState.linkState,
                entityState.physicsTables,
                entityState.presentationState,
                entityState.roomVariables,
                lifecycle,
                storyState,
                slots,
                slotProperties.spriteViews(),
                playerCommands,
                pickupsState,
                bossRewardsState);
        this.bombInteractions = new EntityBombInteractions(
                bombInteractionsState,
                spriteHandlers,
                backgroundMechanics,
                entityState.combatTables,
                entityState.events,
                liftThrowState,
                entityState.linkState,
                entityState.motions,
                entityState.physicsTables,
                slotProperties,
                enemyCombatTables,
                slots,
                lifecycle);
        this.story = new EntityStoryController(
                storyState,
                entityState.motions,
                spriteHandlers,
                backgroundMechanics,
                bossRewardsState,
                entityState.combatTables,
                entityState.events,
                entityState.linkState,
                entityState.physicsTables,
                pickupsState,
                entityState.presentationState,
                roomInspection,
                roomInspection,
                entityState.roomVariables,
                lifecycle,
                slots,
                slotProperties.spriteViews(),
                playerCommands);
        this.slimeEye = new SlimeEyeController(
                slimeEyeState,
                slotEntities,
                spriteHandlers,
                backgroundMechanics,
                bossRewards,
                entityState.combatTables,
                entityState.events,
                entityState.motions,
                entityState.physicsTables,
                entityState.roomVariables,
                lifecycle,
                slotProperties,
                enemyCombatTables,
                slots);
        this.entityPlayerSpawns = new EntityPlayerSpawns(
                indoorRoom,
                defaultRandomByteSupplier,
                spriteHandlers,
                bombInteractions,
                bossRewardsState,
                entityState.combatTables,
                entityState.events,
                liftThrowState,
                entityState.motions,
                entityState.physicsTables,
                pickups,
                entityState.presentationState,
                projectileState,
                roomInspection,
                roomInspection,
                entityState.roomVariables,
                lifecycle,
                slotProperties,
                slots,
                chestContentsTable,
                playerCommands);
        this.combat = new EntityCombatResolver(
                spriteHandlers,
                backgroundMechanics,
                bombInteractions,
                bossRewards,
                entityState.combatTables,
                entityState.events,
                entityState.linkState,
                entityState.motions,
                entityState.physicsTables,
                roomInspection,
                slotProperties,
                enemyCombatTables,
                slots,
                slimeEye,
                frameRandom);
        this.entityRuntimeConfiguration = new EntityRuntimeConfiguration(
                spriteHandlers,
                bossRewardsState,
                liftThrowState,
                entityState.linkState,
                entityState.motions,
                entityState.physicsTables,
                pickupsState,
                entityState.presentationState,
                projectileState,
                entityState.roomVariables,
                storyState,
                slots,
                slimeEye,
                slotProperties.spriteViews());
        this.projectiles = new EntityProjectileController(
                indoorRoom,
                spriteHandlers,
                bombInteractionsState,
                entityState.combatTables,
                entityState.events,
                liftThrowState,
                entityState.motions,
                entityState.physicsTables,
                entityPlayerSpawns,
                entityState.roomVariables,
                lifecycle,
                slotProperties,
                enemyCombatTables,
                slots,
                projectileState);
        this.genie = new GenieEntityController(
                genieState,
                bossRewards,
                entityState.combatTables,
                entityState.events,
                liftThrowState,
                entityState.motions,
                entityState.physicsTables,
                entityPlayerSpawns,
                lifecycle,
                slotProperties,
                slots);
        this.liftThrow = new EntityLiftThrowController(
                liftThrowState,
                indoorRoom,
                spriteHandlers,
                bombInteractionsState,
                entityState.combatTables,
                entityState.events,
                entityState.linkState,
                entityState.motions,
                entityState.physicsTables,
                pickupsState,
                entityState.presentationState,
                projectiles,
                slotProperties,
                genie,
                slots,
                lifecycle,
                playerCommands,
                entityPlayerSpawns);
        this.roomInitialization = new EntityRoomInitialization(
                warpState,
                indoorRoom,
                bombInteractionsState,
                bossRewardsState,
                entityState.combatTables,
                liftThrowState,
                entityState.motions,
                entityState.physicsTables,
                pickupsState,
                rewardsState,
                slotProperties,
                storyState,
                genie,
                slots,
                lifecycle);

        this.entityHandlers = roomInitialization.createEntityHandlers();
        slimeEye.loadRom(romData);
        roomInitialization.initializeRomPresentation(romData);
        this.warpMotion = romData == null ? null : new WarpMotion(romData);

        resetRoomVariables();
        roomInitialization.initializeRoomSlots(initial);
    }

    public static RoomEntityRuntimeBuilder builder(RoomEntitySnapshot initial) {
        return new RoomEntityRuntimeBuilder(initial);
    }

    /** Frame-only inputs using the runtime RNG, including its carried state. */
    EntityFrameContext frameContext(int frameCounter) {
        IntSupplier randomByteSupplier = frameRandom.forFrame(frameCounter);
        return EntityFrameContext.builder()
                .frameCounter(frameCounter)
                .randomByteSupplier(randomByteSupplier)
                .projectileLinkState(EnemyProjectileCollision.LinkState.nonInteractive())
                .handlerLinkCollisionEnabled(true)
                .build();
    }

    /** Advances one live frame from explicit inputs. */
    public List<EntityProjectileEvent> tick(EntityFrameContext context) {
        validateByte(context.linkSpeedX(), "Link X speed");
        validateByte(context.linkSpeedY(), "Link Y speed");
        entityState.linkState.currentLinkSpeedX = context.linkSpeedX();
        entityState.linkState.currentLinkSpeedY = context.linkSpeedY();
        try {
            return advanceFrame(context);
        } finally {
            combat.clearDeferredContactRecoil();
            entityState.linkState.currentLinkSpeedX = 0;
            entityState.linkState.currentLinkSpeedY = 0;
        }
    }

    /** Defers only a Leever contact already accepted by the player damage gate. */
    public boolean deferContactRecoil(int slot, int linkX, int linkY) {
        EntitySlotValidation.validateEntitySlot(slot);
        validateByte(linkX, "Link X");
        validateByte(linkY, "Link Y");
        return combat.deferContactRecoil(slot, linkX, linkY);
    }

    void finishLeeverContactRecoil(RoomEntity moved) {
        combat.finishLeeverContactRecoil(moved);
    }

    /** Source-ordered frame gates and descending slot dispatch. */
    private List<EntityProjectileEvent> advanceFrame(EntityFrameContext context) {
        return EntitySlotOrchestrator.advanceFrame(this, context);
    }

    /** Shared result path of ApplySwordIntersectionWithObjects ($7CAB). */
    enum ProjectileObjectCollision {
        NONE,
        BLOCKED,
        UNLOAD
    }

    /** Common bank-$03 collision pass for entity {@code $DF}. */
    record ProjectileEntityCollision(boolean unload, boolean blocked) {}

    /**
     * Ports the Iron Mask target branch of bank-$03 {@code func_003_75A2} for
     * the active hookshot chain. Other hookshot target damage branches remain
     * owned by their entity-specific runtimes; this branch is special because
     * it changes the target's private state and creates a visible mask entity.
     */
    int initialHealth(int type) {
        return this.slotProperties.initialHealth(type);
    }

    /** ResetRoomVariables ($20:4CC6), called by LoadRoom ($00:30FC).
     * The battle flag survives boss death, but not a room load/warp.
     */
    void resetRoomVariables() {
        bossRewards.state.writeInBossBattle(false);
    }

    /** EntityInitHandler $03:48B5-4910. No boss-handler writes are outstanding:
     * this is the sole set site; $20:4CC6 is the sole clear site.
     * Indoor/wD478/mini-boss checks at $03:48F0-4908 only select intro setup.
     * Defeated bosses (including Master Stalfos prerequisite failures) must be
     * unloaded before calling this initialization hook.
     */
    boolean initializeBossBattle(RoomEntity entity) {
        return lifecycle.initializeBossBattle(entity);
    }

    void initializeHealthGroup(int slot) {
        lifecycle.initializeHealthGroup(slot);
    }

    int contactDamage(int slot) {
        return this.slotProperties.contactDamage(slot);
    }

    /** Unloads a slot and returns the persistent first-eight load-order bit. */
    public int clearEntity(int slot) {
        return this.lifecycle.clearEntity(slot);
    }

    public void setHeartPiecesCount(int count) {
        pickups.state.writeHeartPiecesCount(count);
    }

    void blockLinkForOwl(int slot) {
        entityState.events.pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
    }

    int findFreeEntitySlot() {
        return EntitySlotAllocator.findFreeSlot(slots, entityState.combatTables, slots.length - 1);
    }

    int findFreeEntitySlotAtMost(int highestSlot) {
        return EntitySlotAllocator.findFreeSlot(slots, entityState.combatTables, highestSlot);
    }

    /** Mirrors EntityStunnedHandler's recoil/bounce/clear-speed tail for a bomb. */
    public int thrownDirection(int slot) {
        validateEntitySlot(slot);
        return liftThrow.state.readThrownDirection(slot) & 0xFF;
    }

    public int options1(int slot) {
        return this.slotProperties.options1(slot);
    }

    public record CommonDroppableUpdate(RoomEntity entity, boolean hidden) {}

    public record SlimeKeyTransitionUpdate(
            RoomEntity entity,
            int nextTransitionCountdown,
            boolean holdAboveLink,
            boolean unload,
            SlimeKeyRewardEvent reward) {}

    @Override
    public EntityEffectConsumption entityEffects() {
        return effects;
    }

    @Override
    public EntityCombatResolver entityCombat() {
        return combat;
    }

    @Override
    public EntityEnemySpawns entityEnemySpawns() {
        return entityEnemySpawns;
    }

    @Override
    public EntityPlayerSpawns entityPlayerSpawns() {
        return entityPlayerSpawns;
    }

    @Override
    public EntitySlotLifecycle entityLifecycle() {
        return lifecycle;
    }

    @Override
    public EntityPlayerCommands entityPlayerCommands() {
        return playerCommands;
    }

    @Override
    public EntityBackgroundMechanics entityBackground() {
        return backgroundMechanics;
    }

    public EntityRewardController entityRewards() {
        return rewards;
    }
}
