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
 * Mutable per-room entity state for the handler work that can be expressed by
 * the current room snapshot model. ROM display lists remain immutable; this
 * class only advances handler-owned status and sprite-variant fields.
 */
public final class RoomEntityRuntime
        implements EntityRuntimeEffects, EntityRuntimeCombat, EntityRuntimeSpawns, EntityRuntimePlayerCommands {
    final EntityWarpController warpState = new EntityWarpController();
    final EntityRuntimeState entityState = new EntityRuntimeState();
    final EntityFrameExecution frameExecution = new EntityFrameExecution(this, null);
    /** Compatibility for world.room.RoomEntityBridge. */
    @Deprecated
    public static final int BOOMERANG_SFX_COUNTER_PERIOD = EntityRomConstants.BOOMERANG_SFX_COUNTER_PERIOD;

    private final EntityPlayerCommands playerCommands =
            new EntityPlayerCommands(EntityPlayerCommandsServices.bind(this));

    private void disableEntityWithoutPersistence(int slot) {
        lifecycle.disableEntityWithoutPersistence(slot);
    }

    final EntityRoomInitialization roomInitialization =
            new EntityRoomInitialization(EntityRoomInitializationServices.bind(this));
    final EntitySlotProperties slotProperties = new EntitySlotProperties(EntitySlotPropertiesServices.bind(this));
    final EntityBackgroundMechanics backgroundMechanics =
            new EntityBackgroundMechanics(EntityBackgroundMechanicsServices.bind(this));
    final EntityBombInteractions bombInteractions =
            new EntityBombInteractions(EntityBombInteractionsServices.bind(this));
    final EntitySlotLifecycle lifecycle = new EntitySlotLifecycle(EntitySlotLifecycleServices.bind(this));
    final EntityEnemySpawns entityEnemySpawns = new EntityEnemySpawns(EntityEnemySpawnsServices.bind(this));
    final EntityPlayerSpawns entityPlayerSpawns = new EntityPlayerSpawns(EntityPlayerSpawnsServices.bind(this));
    private final EntityEffectConsumption effects =
            new EntityEffectConsumption(EntityEffectConsumptionServices.bind(this));
    private final EntityRuntimeConfiguration entityRuntimeConfiguration =
            new EntityRuntimeConfiguration(EntityRuntimeConfigurationServices.bind(this));
    final EntityDebugConfiguration entityDebugConfiguration =
            new EntityDebugConfiguration(EntityDebugConfigurationServices.bind(this));
    final EntityHandlerRegistry entityHandlers = roomInitialization.createEntityHandlers();
    final EntityRewardController rewards = new EntityRewardController(EntityRewardControllerServices.bind(this));
    final EntityStoryController story = new EntityStoryController(EntityStoryControllerServices.bind(this));
    final SlimeEyeController slimeEye = new SlimeEyeController(SlimeEyeControllerServices.bind(this));
    final GenieEntityController genie = new GenieEntityController(GenieEntityControllerServices.bind(this));
    final EntityProjectileController projectiles =
            new EntityProjectileController(EntityProjectileControllerServices.bind(this));
    final EntityPickupController pickups = new EntityPickupController(EntityPickupControllerServices.bind(this));
    final EntityLiftThrowController liftThrow =
            new EntityLiftThrowController(EntityLiftThrowControllerServices.bind(this));
    private final EntityCombatResolver combat = new EntityCombatResolver(EntityCombatResolverServices.bind(this));
    final EntityBossRewards bossRewards = new EntityBossRewards(EntityBossRewardsServices.bind(this));
    final EntityRoomInspection roomInspection = new EntityRoomInspection(EntityRoomInspectionServices.bind(this));
    final EntityBossInspection bossInspection = new EntityBossInspection(EntityBossInspectionServices.bind(this));
    final EntityFlyingEnemyInspection flyingEnemyInspection =
            new EntityFlyingEnemyInspection(EntityFlyingEnemyInspectionServices.bind(this));
    final EntityPickupInspection pickupInspection =
            new EntityPickupInspection(EntityPickupInspectionServices.bind(this));
    final EntityEnemyInspection enemyInspection = new EntityEnemyInspection(EntityEnemyInspectionServices.bind(this));

    final RoomEntity[] slots;
    final java.util.List<RoomEntity> slotEntities;

    final EntitySpriteTileSnapshot spriteTiles;
    final boolean indoorRoom;
    private final EntityPropertyTables entityProperties;
    final IntSupplier defaultRandomByteSupplier;
    private final RomRandomByteSource fallbackRomRandomByteSource;
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

    /** Object-side effect emitted by MagicPowderSprinkleEntityHandler. */
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
        this.fallbackRomRandomByteSource = defaultRandomByteSupplier == null ? new RomRandomByteSource() : null;
        this.spriteHandlers = spriteHandlers;
        this.enemyCombatTables = enemyCombatTables;
        this.chestContentsTable = chestContentsTable;
        this.entityProperties = romTables == null ? null : romTables.entityPropertyTables();
        slimeEye.loadRom(romData);
        roomInitialization.initializeRomPresentation(romData);
        this.warpMotion = romData == null ? null : new WarpMotion(romData);
        resetRoomVariables();
        roomInitialization.initializeRoomSlots(initial);
    }

    public static RoomEntityRuntimeBuilder builder(RoomEntitySnapshot initial) {
        return new RoomEntityRuntimeBuilder(initial);
    }

    /** Compatibility for world.room.RoomSessionTest; use builder for new callers. */
    @Deprecated
    public static RoomEntityRuntime from(RoomEntitySnapshot initial) {
        return builder(initial).build();
    }

    /** Compatibility for world.room.RoomSessionTest and RoomTransitionCoordinatorTest; use builder for new callers. */
    @Deprecated
    public static RoomEntityRuntime from(RoomEntitySnapshot initial, boolean indoorRoom) {
        return builder(initial).indoorRoom(indoorRoom).build();
    }

    /** Compatibility for world.room.RoomSessionTest; use builder for new callers. */
    @Deprecated
    public static RoomEntityRuntime from(
            RoomEntitySnapshot initial,
            boolean indoorRoom,
            IntSupplier randomByteSupplier,
            EntitySpriteHandlerCatalog spriteHandlers,
            RomEnemyCombatTables enemyCombatTables) {
        return builder(initial)
                .indoorRoom(indoorRoom)
                .randomByteSupplier(randomByteSupplier)
                .spriteHandlers(spriteHandlers)
                .enemyCombatTables(enemyCombatTables)
                .build();
    }

    /** Compatibility for world.room.RoomLoadCoordinator and RoomStoryState; use builder for new callers. */
    @Deprecated(forRemoval = true)
    public static RoomEntityRuntime from(
            RoomEntitySnapshot initial,
            boolean indoorRoom,
            IntSupplier randomByteSupplier,
            EntitySpriteHandlerCatalog spriteHandlers,
            RomEnemyCombatTables enemyCombatTables,
            ChestContentsTable chestContentsTable,
            RomTables romTables,
            byte[] romData) {
        return builder(initial)
                .indoorRoom(indoorRoom)
                .randomByteSupplier(randomByteSupplier)
                .spriteHandlers(spriteHandlers)
                .enemyCombatTables(enemyCombatTables)
                .chestContentsTable(chestContentsTable)
                .romTables(romTables)
                .rom(romData == null ? null : new Rom(romData))
                .build();
    }

    /** Frame-only inputs using the runtime RNG, including its carried state. */
    EntityFrameContext frameContext(int frameCounter) {
        IntSupplier randomByteSupplier = defaultRandomByteSupplier;
        if (randomByteSupplier == null) {
            fallbackRomRandomByteSource.beginFrame(frameCounter & 0xFF, 0);
            randomByteSupplier = fallbackRomRandomByteSource;
        }
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
        bossRewards.writeInBossBattle(false);
    }

    /** EntityInitHandler $03:48B5-4910. No boss-handler writes are outstanding:
     * this is the sole set site; $20:4CC6 is the sole clear site.
     * Indoor/wD478/mini-boss checks at $03:48F0-4908 only select intro setup.
     * Defeated bosses (including Master Stalfos prerequisite failures) must be
     * unloaded before calling this initialization hook.
     */
    boolean initializeBossBattle(RoomEntity entity) {
        int options = entityState.physicsTables.readEntityOptions1Override(entity.slot()) >= 0
                ? entityState.physicsTables.readEntityOptions1Override(entity.slot())
                : entityProperties == null ? 0 : entityProperties.entityOptions1(entity.type());
        return initializeBossBattle(entity, options);
    }

    private boolean initializeBossBattle(RoomEntity entity, int options) {
        if ((options & 0x80) == 0) return true;
        // $03:48BE-48C4 rejects an already defeated room boss.
        if ((entityState.roomVariables.readEntityRoomStatus() & 0x30) != 0) {
            lifecycle.disableEntityWithoutPersistence(entity.slot());
            return false;
        }
        bossRewards.writeInBossBattle(true);
        return true;
    }

    void initializeHealthGroup(int slot) {
        // ConfigureEntityHealth, $03:489B-489F, before setting initial HP.
        // Active spawns bypass the INIT dispatch; use their fresh ROM options,
        // not an override remaining from the previous occupant of this slot.
        if (slots[slot].status() == EntityStatus.ACTIVE) {
            initializeBossBattle(
                    slots[slot], entityProperties == null ? 0 : entityProperties.entityOptions1(slots[slot].type()));
        }
        entityState.combatTables.writeEnemyHealthGroup(
                slot, enemyCombatTables == null ? 0 : enemyCombatTables.healthGroup(slots[slot].type()));
    }

    int contactDamage(int slot) {
        return this.slotProperties.contactDamage(slot);
    }

    /** Unloads a slot and returns the persistent first-eight load-order bit. */
    public int clearEntity(int slot) {
        return this.lifecycle.clearEntity(slot);
    }

    public void setHeartPiecesCount(int count) {
        pickups.writeHeartPiecesCount(count);
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
        return liftThrow.readThrownDirection(slot) & 0xFF;
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

    /** Mirrors UpdateEntityTimers' shared dialog/inventory/ocarina early return. */
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
}
