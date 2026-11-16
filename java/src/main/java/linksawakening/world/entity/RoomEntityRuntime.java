package linksawakening.world.entity;

import static linksawakening.world.entity.EntitySlotValidation.validateByte;
import static linksawakening.world.entity.EntitySlotValidation.validateEntitySlot;

import java.util.List;
import java.util.function.IntSupplier;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.rom.Rom;
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
import linksawakening.world.entity.motion.WarpMotion;
import linksawakening.world.entity.motion.ZolGelMotion;

/**
 * Per-room composition root for ExecuteEntities and EntityInitHandler.
 * Constructs concrete controllers in dependency order and owns reusable frame scratch;
 * shared encounter state breaks observer/reset cycles. Slots remain immutable values.
 */
public final class RoomEntityRuntime {
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
    final EntityFrameExecution frameExecution;

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

    final EntitySlots slots;
    final java.util.List<RoomEntity> slotEntities;

    // Optional ROM-backed data; absent in ROM-free or unconfigured presentation state.
    final EntitySpriteTileSnapshot spriteTiles;
    final boolean indoorRoom;
    // Optional ROM-backed data; absent in ROM-free or unconfigured presentation state.
    private final java.util.function.IntUnaryOperator entityProperties;
    // Optional RNG override; null selects the room-owned ROM-compatible source.
    final IntSupplier defaultRandomByteSupplier;
    private final EntityFrameRandom frameRandom;
    final EntitySpriteViews spriteHandlers;
    final RomEnemyCombatTables enemyCombatTables;
    final EntityChestContents chestContentsTable;

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
        this.frameRandom = new EntityFrameRandom(builder.randomByteSupplier());
        IntSupplier defaultRandomByteSupplier = frameRandom.spawnSource();
        EntitySpriteViews spriteHandlers = builder.spriteHandlers();
        RomEnemyCombatTables enemyCombatTables = builder.enemyCombatTables();
        EntityChestContents chestContentsTable = builder.chestContentsTable();

        Rom romData = builder.rom();

        this.slots = new EntitySlots(initial.slots());
        this.slotEntities = slots.view();
        this.entityState.presentationState.initializeSelection(initial.spriteSelection(), indoorRoom);
        this.spriteTiles = initial.spriteTiles();
        this.entityState.physicsTables.writeGroundInteractionSideScrolling(initial.sideScrolling());
        this.indoorRoom = indoorRoom;
        this.defaultRandomByteSupplier = defaultRandomByteSupplier;

        this.spriteHandlers = java.util.Objects.requireNonNull(spriteHandlers, "sprite source");
        this.enemyCombatTables = java.util.Objects.requireNonNull(enemyCombatTables, "combat rules");
        this.chestContentsTable = java.util.Objects.requireNonNull(chestContentsTable, "chest contents");
        this.entityProperties = builder.entityOptionsLookup();
        this.slotPropertiesState = new EntitySlotPresentation(spriteHandlers);
        this.entityDebugConfiguration = new EntityDebugConfiguration(entityState.combatTables, entityState.motions);
        this.effects = new EntityEffectConsumption(
                entityState.events, entityState.presentationState, entityState.roomVariables);
        this.flyingEnemyInspection = new EntityFlyingEnemyInspection(entityState.motions, entityState.physicsTables);
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
                slots,
                romData);
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
        if (builder.hasRom()) roomInitialization.initializeRomPresentation(romData);
        this.warpMotion = builder.hasRom() ? new WarpMotion(romData) : WarpMotion.DISABLED;

        this.frameExecution = new EntityFrameExecution(
                slotProperties,
                spriteHandlers,
                backgroundMechanics,
                liftThrow,
                slots,
                slotEntities,
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
                entityState.linkView,
                entityState.slotView,
                entityState.events,
                entityState.presentationState,
                entityState.motions,
                entityState.combatTables,
                entityState.physicsTables,
                entityState.roomVariables,
                entityState.linkState,
                indoorRoom,
                enemyCombatTables,
                combat,
                playerCommands);

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
        return EntitySlotPipeline.advanceFrame(frameExecution, context);
    }

    /** Shared result path of ApplySwordIntersectionWithObjects ($7CAB). */

    /** Common bank-$03 collision pass for entity {@code $DF}. */

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
        return slots.findFree(entityState.combatTables, slots.size() - 1);
    }

    int findFreeEntitySlotAtMost(int highestSlot) {
        return slots.findFree(entityState.combatTables, highestSlot);
    }

    /** Mirrors EntityStunnedHandler's recoil/bounce/clear-speed tail for a bomb. */
    public int thrownDirection(int slot) {
        validateEntitySlot(slot);
        return liftThrow.state.readThrownDirection(slot) & 0xFF;
    }

    public int options1(int slot) {
        return this.slotProperties.options1(slot);
    }

    public EntityEffectConsumption entityEffects() {
        return effects;
    }

    public EntityCombatResolver entityCombat() {
        return combat;
    }

    public EntityEnemySpawns entityEnemySpawns() {
        return entityEnemySpawns;
    }

    public EntityPlayerSpawns entityPlayerSpawns() {
        return entityPlayerSpawns;
    }

    public EntitySlotLifecycle entityLifecycle() {
        return lifecycle;
    }

    public EntityPlayerCommands entityPlayerCommands() {
        return playerCommands;
    }

    public EntityBackgroundMechanics entityBackground() {
        return backgroundMechanics;
    }

    public EntityRewardController entityRewards() {
        return rewards;
    }

    /**
     * Runs the room-entity portion of PickableCollectIfNeeded. A non-null
     * event means the entity completed its pickup collision and its first-eight
     * persistence bit is ready for the room session to record.
     */
    public EntityPickupEvent collectIfNeeded(
            int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive) {
        return entityRewards().collectIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive);
    }

    /** Pickup collision with Link's Java direction available for held items. */
    public EntityPickupEvent collectIfNeeded(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            boolean linkInteractive,
            int linkDirection) {
        return entityRewards()
                .collectIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, linkDirection);
    }

    /** Pickup collision with Link's ROM-facing Z available for floating items. */
    public EntityPickupEvent collectIfNeeded(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            boolean linkInteractive,
            int linkDirection,
            int linkZ) {
        return entityRewards()
                .collectIfNeeded(
                        frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, linkDirection, linkZ);
    }

    public EntityPickupEvent collectIfNeeded(EntityPickupFrameInputs inputs) {
        return entityRewards().collectIfNeeded(inputs);
    }

    /**
     * Runs the ported portion of DefaultEnemyDamageCollisionHandler for the
     * currently supported enemy families. The four sword arguments are the
     * ROM's wC140..wC143 rectangle; non-positive width/height disables sword
     * collision for this pass.
     */
    public List<EntityCombatEvent> resolveCombat(EntityAttackContactFrame inputs) {
        int frameCounter = inputs.frameCounter();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        boolean linkAirborne = inputs.linkAirborne();
        int linkVerticalVelocity = inputs.linkVerticalVelocity();
        boolean linkInteractive = inputs.linkInteractive();
        boolean swordCollisionActive = inputs.swordCollisionActive();
        int swordX = inputs.swordX();
        int swordWidth = inputs.swordWidth();
        int swordY = inputs.swordY();
        int swordHeight = inputs.swordHeight();
        return resolveCombat(new EnemyCombatFrame(
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkAirborne,
                linkVerticalVelocity,
                linkInteractive,
                swordCollisionActive,
                swordX,
                swordWidth,
                swordY,
                swordHeight,
                EnemyAttackContext.standard()));
    }

    public List<EntityCombatEvent> resolveCombat(EntitySwordContactFrame inputs) {
        int frameCounter = inputs.frameCounter();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        boolean linkAirborne = inputs.linkAirborne();
        boolean linkInteractive = inputs.linkInteractive();
        boolean swordCollisionActive = inputs.swordCollisionActive();
        int swordX = inputs.swordX();
        int swordWidth = inputs.swordWidth();
        int swordY = inputs.swordY();
        int swordHeight = inputs.swordHeight();
        return resolveCombat(new EnemyCombatFrame(
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkAirborne,
                0,
                linkInteractive,
                swordCollisionActive,
                swordX,
                swordWidth,
                swordY,
                swordHeight,
                EnemyAttackContext.standard()));
    }

    public List<EntityCombatEvent> resolveCombat(EntityVerticalContactFrame inputs) {
        int frameCounter = inputs.frameCounter();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        boolean linkAirborne = inputs.linkAirborne();
        boolean linkInteractive = inputs.linkInteractive();
        boolean swordCollisionActive = inputs.swordCollisionActive();
        int swordX = inputs.swordX();
        int swordWidth = inputs.swordWidth();
        int swordY = inputs.swordY();
        int swordHeight = inputs.swordHeight();
        EnemyAttackContext attackContext = inputs.attackContext();
        return resolveCombat(new EnemyCombatFrame(
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkAirborne,
                0,
                linkInteractive,
                swordCollisionActive,
                swordX,
                swordWidth,
                swordY,
                swordHeight,
                attackContext));
    }

    public List<EntityCombatEvent> resolveCombat(EnemyCombatInputs inputs) {
        return entityCombat().resolveCombat(inputs);
    }

    public boolean consumePendingSwitchBlockAnimationRequest() {
        return entityEffects().consumePendingSwitchBlockAnimationRequest();
    }

    public int consumePendingClearedEntityMask() {
        return entityEffects().consumePendingClearedEntityMask();
    }

    public int consumePendingRoomStatusMask() {
        return entityEffects().consumePendingRoomStatusMask();
    }

    public List<EntityCombatEvent> consumePendingEntityEvents() {
        return entityEffects().consumePendingEntityEvents();
    }

    public List<ThrownBackgroundCollisionEvent> consumePendingThrownBackgroundCollisions() {
        return entityEffects().consumePendingThrownBackgroundCollisions();
    }

    public List<DungeonWarpRequest> consumePendingDungeonWarpRequests() {
        return entityEffects().consumePendingDungeonWarpRequests();
    }

    public List<WarpLinkStateRequest> consumePendingWarpLinkStateRequests() {
        return entityEffects().consumePendingWarpLinkStateRequests();
    }

    public List<LinkFinalPositionRequest> consumePendingLinkFinalPositionRequests() {
        return entityEffects().consumePendingLinkFinalPositionRequests();
    }

    public List<RoosterLinkStateRequest> consumePendingRoosterLinkStateRequests() {
        return entityEffects().consumePendingRoosterLinkStateRequests();
    }

    public List<LinkMotionBlockRequest> consumePendingLinkMotionBlockRequests() {
        return entityEffects().consumePendingLinkMotionBlockRequests();
    }

    public List<SideViewPlatformLinkRequest> consumePendingSideViewPlatformLinkRequests() {
        return entityEffects().consumePendingSideViewPlatformLinkRequests();
    }

    public List<SideViewPotLinkRequest> consumePendingSideViewPotLinkRequests() {
        return entityEffects().consumePendingSideViewPotLinkRequests();
    }

    public List<HinoxLinkEffectRequest> consumePendingHinoxLinkEffectRequests() {
        return entityEffects().consumePendingHinoxLinkEffectRequests();
    }

    public List<LinkFallenPoseRequest> consumePendingLinkFallenPoseRequests() {
        return entityEffects().consumePendingLinkFallenPoseRequests();
    }

    public List<LinkFacingRequest> consumePendingLinkFacingRequests() {
        return entityEffects().consumePendingLinkFacingRequests();
    }

    public List<LinkAttackClearRequest> consumePendingLinkAttackClearRequests() {
        return entityEffects().consumePendingLinkAttackClearRequests();
    }

    public List<LinkAttackStepRequest> consumePendingLinkAttackStepRequests() {
        return entityEffects().consumePendingLinkAttackStepRequests();
    }

    public List<RoomStatusPersistenceRequest> consumePendingRoomStatusPersistenceRequests() {
        return entityEffects().consumePendingRoomStatusPersistenceRequests();
    }

    public List<LinkHeldItemPoseRequest> consumePendingLinkHeldItemPoseRequests() {
        return entityEffects().consumePendingLinkHeldItemPoseRequests();
    }

    public List<LinkSwordSpinPoseRequest> consumePendingLinkSwordSpinPoseRequests() {
        return entityEffects().consumePendingLinkSwordSpinPoseRequests();
    }

    public List<LinkSwordFinalPoseRequest> consumePendingLinkSwordFinalPoseRequests() {
        return entityEffects().consumePendingLinkSwordFinalPoseRequests();
    }

    public List<ScreenShakeRequest> consumePendingScreenShakeRequests() {
        return entityEffects().consumePendingScreenShakeRequests();
    }

    public List<ChestRewardEvent> consumePendingChestRewardEvents() {
        return entityEffects().consumePendingChestRewardEvents();
    }

    public List<KeyRewardEvent> consumePendingKeyRewardEvents() {
        return entityEffects().consumePendingKeyRewardEvents();
    }

    public List<SlimeKeyRewardEvent> consumePendingSlimeKeyRewardEvents() {
        return entityEffects().consumePendingSlimeKeyRewardEvents();
    }

    public List<KeyQuicksandEvent> consumePendingKeyQuicksandEvents() {
        return entityEffects().consumePendingKeyQuicksandEvents();
    }

    public int consumePendingMusicTrack() {
        return entityEffects().consumePendingMusicTrack();
    }

    public int consumePendingMusicFadeOutCountdown() {
        return entityEffects().consumePendingMusicFadeOutCountdown();
    }

    public List<LikeLikeEvent> consumePendingLikeLikeEvents() {
        return entityEffects().consumePendingLikeLikeEvents();
    }

    public List<DialogRequest> consumePendingDialogRequests() {
        return entityEffects().consumePendingDialogRequests();
    }

    public List<BombExplosionEvent> consumeBombExplosionEvents() {
        return entityEffects().consumeBombExplosionEvents();
    }

    public List<HeartPiecePickupUpdate> consumeHeartPieceUpdates() {
        return entityEffects().consumeHeartPieceUpdates();
    }

    public List<HeartContainerRewardEvent> consumePendingHeartContainerRewards() {
        return entityEffects().consumePendingHeartContainerRewards();
    }

    public List<MadamMeowMeowFullHealRequest> consumePendingMadamMeowMeowFullHealRequests() {
        return entityEffects().consumePendingMadamMeowMeowFullHealRequests();
    }

    public List<SwordPickupRewardEvent> consumePendingSwordPickupRewards() {
        return entityEffects().consumePendingSwordPickupRewards();
    }

    public List<ToadstoolRewardEvent> consumePendingToadstoolRewards() {
        return entityEffects().consumePendingToadstoolRewards();
    }

    public List<InstrumentRewardEvent> consumePendingInstrumentRewards() {
        return entityEffects().consumePendingInstrumentRewards();
    }

    public List<InstrumentCompletionEvent> consumePendingInstrumentCompletions() {
        return entityEffects().consumePendingInstrumentCompletions();
    }

    public List<RichardProgressEvent> consumePendingRichardProgressEvents() {
        return entityEffects().consumePendingRichardProgressEvents();
    }

    public List<WitchExchangeEvent> consumePendingWitchExchangeEvents() {
        return entityEffects().consumePendingWitchExchangeEvents();
    }

    public List<WitchRewardEvent> consumePendingWitchRewardEvents() {
        return entityEffects().consumePendingWitchRewardEvents();
    }

    public List<OwlEventCompletion> consumePendingOwlEventCompletions() {
        return entityEffects().consumePendingOwlEventCompletions();
    }

    /**
     * Starts EntityGetLiftedUp's shared state transition. Directions are the
     * ROM order (right, left, up, down); RoomSession performs the Java-to-ROM
     * conversion at its boundary.
     */
    public boolean beginLift(int slot, int romDirection) {
        return entityPlayerCommands().beginLift(slot, romDirection);
    }

    /** Hands the fully held entity to func_014_53A3's generic throw path. */
    public boolean throwLiftedEntity(int romDirection) {
        return entityPlayerCommands().throwLiftedEntity(romDirection);
    }

    /** Applies the position and variant writes made by MarinEntityHandler_Indoor. */
    public boolean applyMarinWakeUpPresentation(int x, int y, int spriteVariant) {
        return entityPlayerCommands().applyMarinWakeUpPresentation(x, y, spriteVariant);
    }

    public void updateHouseTarinPresentation(int linkX, int linkY) {
        entityPlayerCommands().updateHouseTarinPresentation(linkX, linkY);
    }

    /** Bank $15:7C37..7CA5: two death waits, then DidKillEnemy. */
    public void startSwordPickup(int slot) {
        entityPlayerCommands().startSwordPickup(slot);
    }

    public void startToadstoolPickup(int slot) {
        entityPlayerCommands().startToadstoolPickup(slot);
    }

    public void presentWitchGotItem(int slot) {
        entityPlayerCommands().presentWitchGotItem(slot);
    }

    /** ProcessPowerUp ($03:6400) and ApplyGotItem ($00:1033). */
    public void holdSwordAboveLink(RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ, boolean finalPose) {
        entityPlayerCommands().holdSwordAboveLink(entity, linkEntityX, linkEntityY, linkZ, finalPose);
    }

    /** Creates the ROM's player-projectile entity type {@code $03}. */
    public int spawnHookshotChain(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return entityPlayerSpawns().spawnHookshotChain(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    /** Spawns bank 3's temporary entity $06 at an indoor object-cell center. */
    public int spawnPushedBlock(int x, int y) {
        return entityPlayerSpawns().spawnPushedBlock(x, y);
    }

    /** Spawns bank 0's temporary type-$05 entity already entering lifted status. */
    public int spawnLiftedRoomObject(int x, int y, int romDirection, int spriteVariant) {
        return entityPlayerSpawns().spawnLiftedRoomObject(x, y, romDirection, spriteVariant);
    }

    public void movePushedBlock(int slot, int x, int y) {
        entityPlayerCommands().movePushedBlock(slot, x, y);
    }

    /** $03:5269: after block movement, restore Link even during a jump. */
    public void resolvePushedBlockLinkContact(int slot, int linkEntityX, int linkEntityY) {
        entityBackground().resolvePushedBlockLinkContact(slot, linkEntityX, linkEntityY);
    }

    public void removePushedBlock(int slot) {
        entityPlayerCommands().removePushedBlock(slot);
    }

    /** Spawns DropKeyEffectHandler's ordinary dungeon key at ($28,$3C,$70). */
    public int spawnRoomEventKeyDrop() {
        return entityPlayerSpawns().spawnRoomEventKeyDrop();
    }

    /** Creates the ROM's ordinary player-arrow entity type {@code $00}. */
    public int spawnArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return entityPlayerSpawns().spawnArrow(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    public int spawnArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        return entityPlayerSpawns().spawnArrow(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower);
    }

    /** Creates the ROM's player-boomerang entity type {@code $01}. */
    public int spawnBoomerang(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, int pressedButtonsMask) {
        return entityPlayerSpawns()
                .spawnBoomerang(linkEntityX, linkEntityY, linkEntityZ, romDirection, pressedButtonsMask);
    }

    /** Creates the ROM's level-two sword-beam entity type {@code $DF}. */
    public int spawnSwordBeam(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return entityPlayerSpawns().spawnSwordBeam(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    /** Creates the ROM's Magic Rod fireball entity type {@code $04}. */
    public int spawnMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return entityPlayerSpawns().spawnMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    public int spawnMagicRodFireball(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        return entityPlayerSpawns()
                .spawnMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower);
    }

    /** Creates the ROM's type-$08 Magic Powder sprinkle entity. */
    public int spawnMagicPowderSprinkle(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return entityPlayerSpawns().spawnMagicPowderSprinkle(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    /** Creates the ROM's ordinary player bomb entity type {@code $02}. */
    public int spawnBomb(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return entityPlayerSpawns().spawnBomb(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    /** Creates the source's type-$02 enemy bomb with privateState4 set to $01. */
    public int spawnEnemyBomb(int entityX, int entityY, int entityZ, int transitionCountdown) {
        return entityEnemySpawns().spawnEnemyBomb(entityX, entityY, entityZ, transitionCountdown);
    }

    /** Creates a Bomber-spawned type-$02 enemy bomb with its source speeds. */
    public int spawnEnemyBomb(
            int entityX, int entityY, int entityZ, int transitionCountdown, int speedX, int speedY, int speedZ) {
        return entityEnemySpawns()
                .spawnEnemyBomb(entityX, entityY, entityZ, transitionCountdown, speedX, speedY, speedZ);
    }

    public int spawnEnemyBomb(EnemyBombSpawn inputs) {
        int entityX = inputs.entityX();
        int entityY = inputs.entityY();
        int entityZ = inputs.entityZ();
        int transitionCountdown = inputs.transitionCountdown();
        int speedX = inputs.speedX();
        int speedY = inputs.speedY();
        int speedZ = inputs.speedZ();
        boolean initializeMotion = inputs.initializeMotion();
        int privateState4 = inputs.privateState4();
        return entityEnemySpawns()
                .spawnEnemyBomb(new EnemyBombSpawn(
                        entityX,
                        entityY,
                        entityZ,
                        transitionCountdown,
                        speedX,
                        speedY,
                        speedZ,
                        initializeMotion,
                        privateState4));
    }

    /** Creates the temporary type-$05 entity used by bombed bushes, grass, and pots. */
    public int spawnLiftableRockSmash(int entityX, int entityY, int sourceSpriteVariant) {
        return entityPlayerSpawns().spawnLiftableRockSmash(entityX, entityY, sourceSpriteVariant);
    }

    /** Creates one of the four silent type-$05 rubble entities from bank-$36. */
    public int spawnLiftableRockRubble(int entityX, int entityY) {
        return entityPlayerSpawns().spawnLiftableRockRubble(entityX, entityY);
    }

    /**
     * Mirrors the shovel's delayed random reward branch in
     * {@code label_002_4C92}. The first random byte is consumed even when the
     * current room is not allowed to produce a reward; the caller supplies
     * that room-specific restriction explicitly.
     */
    public int spawnShovelDrop(int objectLeft, int objectTop, int linkEntityX, int linkEntityY, boolean allowDrop) {
        return entityPlayerSpawns().spawnShovelDrop(objectLeft, objectTop, linkEntityX, linkEntityY, allowDrop);
    }

    /**
     * Mirrors SpawnChestWithItem. The arguments are the intersected object's
     * unaligned left/top coordinates; the source masks them to a room cell
     * and places the entity at (+8,+16).
     */
    public int spawnChestWithItem(int objectLeft, int objectTop, int itemType) {
        return entityPlayerSpawns().spawnChestWithItem(objectLeft, objectTop, itemType);
    }
}
