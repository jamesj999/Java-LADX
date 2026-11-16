package linksawakening.world.entity;

import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.rom.Rom;
import linksawakening.rom.RomTables;
import linksawakening.world.entity.motion.WarpMotion;
import linksawakening.world.room.ChestContentsTable;

/** Entity composition root: validates named inputs and constructs owners in dependency order. */
public final class RoomEntityRuntimeBuilder {
    // Builder input; validated or resolved to an explicit source by build().
    private final RoomEntitySnapshot initial;
    private boolean indoorRoom;
    // Builder input; validated or resolved to an explicit source by build().
    private IntSupplier randomByteSupplier;
    // Omitted builder input; build() selects the ROM-free policy.
    private EntitySpriteHandlerCatalog spriteHandlers;
    private RomEnemyCombatTables enemyCombatTables;
    // Omitted builder input; build() selects the ROM-free policy.
    private ChestContentsTable chestContentsTable;
    // Omitted builder input; build() selects the ROM-free policy.
    private RomTables romTables;
    // Omitted builder input; build() selects the ROM-free policy.
    private Rom romData;
    private boolean enemyCombatTablesSupplied;
    private boolean chestContentsTableSupplied;
    private boolean romTablesSupplied;
    private boolean romDataSupplied;

    public RoomEntityRuntimeBuilder(RoomEntitySnapshot initial) {
        this.initial = initial;
    }

    RoomEntitySnapshot initial() {
        return initial;
    }

    boolean indoorRoom() {
        return indoorRoom;
    }

    public RoomEntityRuntimeBuilder indoorRoom(boolean value) {
        indoorRoom = value;
        return this;
    }

    IntSupplier randomByteSupplier() {
        return randomByteSupplier;
    }

    public RoomEntityRuntimeBuilder randomByteSupplier(IntSupplier value) {
        randomByteSupplier = value;
        return this;
    }

    EntitySpriteViews spriteHandlers() {
        return new EntitySpriteViews(spriteHandlers);
    }

    public RoomEntityRuntimeBuilder spriteHandlers(
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ EntitySpriteHandlerCatalog
                    value) {
        spriteHandlers = value;
        return this;
    }

    RomEnemyCombatTables enemyCombatTables() {
        return java.util.Objects.requireNonNullElse(enemyCombatTables, RomEnemyCombatTables.DEFAULTS);
    }

    public RoomEntityRuntimeBuilder enemyCombatTables(RomEnemyCombatTables value) {
        enemyCombatTables = value;
        enemyCombatTablesSupplied = true;
        return this;
    }

    EntityChestContents chestContentsTable() {
        return EntityChestContents.from(chestContentsTable);
    }

    public RoomEntityRuntimeBuilder chestContentsTable(
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ ChestContentsTable
                    value) {
        chestContentsTable = value;
        chestContentsTableSupplied = true;
        return this;
    }

    RomTables romTables() {
        return romTables;
    }

    public RoomEntityRuntimeBuilder romTables(
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ RomTables value) {
        romTables = value;
        romTablesSupplied = true;
        return this;
    }

    boolean hasRom() {
        return romDataSupplied;
    }

    java.util.function.IntUnaryOperator entityOptionsLookup() {
        return romTablesSupplied ? romTables.entityPropertyTables()::entityOptions1 : ignored -> 0;
    }

    Rom rom() {
        return romData;
    }

    public RoomEntityRuntimeBuilder rom(
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ Rom value) {
        romData = value;
        romDataSupplied = true;
        return this;
    }

    public RoomEntityRuntime build() {
        if (romTablesSupplied || romDataSupplied) {
            if (initial == null
                    || enemyCombatTables == null
                    || chestContentsTable == null
                    || romTables == null
                    || (romDataSupplied && romData == null)) {
                throw new IllegalArgumentException("ROM room runtime inputs cannot be null");
            }
        } else {
            if (initial == null) throw new IllegalArgumentException("Initial entity snapshot cannot be null");
            if ((enemyCombatTablesSupplied || chestContentsTableSupplied) && enemyCombatTables == null)
                throw new IllegalArgumentException("ROM enemy combat tables cannot be null");
            if (chestContentsTableSupplied && chestContentsTable == null)
                throw new IllegalArgumentException("ROM chest contents table cannot be null");
        }
        return assemble();
    }

    private RoomEntityRuntime assemble() {
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
        final EntityPlayerCommands playerCommands;
        final EntityRoomInitialization roomInitialization;
        final EntitySlotProperties slotProperties;
        final EntityBackgroundMechanics backgroundMechanics;
        final EntityBombInteractions bombInteractions;
        final EntitySlotLifecycle lifecycle;
        final EntityEnemySpawns entityEnemySpawns;
        final EntityPlayerSpawns entityPlayerSpawns;
        final EntityEffectConsumption effects;
        final EntityRuntimeConfiguration entityRuntimeConfiguration;
        final EntityDebugConfiguration entityDebugConfiguration;
        final EntityHandlerRegistry entityHandlers;
        final EntityRewardController rewards;
        final EntityStoryController story;
        final SlimeEyeController slimeEye;
        final GenieEntityController genie;
        final EntityProjectileController projectiles;
        final EntityPickupController pickups;
        final EntityLiftThrowController liftThrow;
        final EntityCombatResolver combat;
        final EntityBossRewards bossRewards;
        final EntityRoomInspection roomInspection;
        final EntityBossInspection bossInspection;
        final EntityFlyingEnemyInspection flyingEnemyInspection;
        final EntityPickupInspection pickupInspection;
        final EntityEnemyInspection enemyInspection;
        final EntitySlots slots;
        final java.util.List<RoomEntity> slotEntities;
        final EntitySpriteTileSnapshot spriteTiles;
        final java.util.function.IntUnaryOperator entityProperties;
        final EntityFrameRandom frameRandom;
        final WarpMotion warpMotion;

        RoomEntitySnapshot initial = this.initial();
        boolean indoorRoom = this.indoorRoom();
        frameRandom = new EntityFrameRandom(this.randomByteSupplier());
        IntSupplier defaultRandomByteSupplier = frameRandom.spawnSource();
        EntitySpriteViews spriteHandlers = this.spriteHandlers();
        RomEnemyCombatTables enemyCombatTables = this.enemyCombatTables();
        EntityChestContents chestContentsTable = this.chestContentsTable();

        Rom romData = this.rom();

        slots = new EntitySlots(initial.slots());
        slotEntities = slots.view();
        entityState.presentationState.initializeSelection(initial.spriteSelection(), indoorRoom);
        spriteTiles = initial.spriteTiles();
        entityState.physicsTables.writeGroundInteractionSideScrolling(initial.sideScrolling());

        java.util.Objects.requireNonNull(spriteHandlers, "sprite source");
        java.util.Objects.requireNonNull(enemyCombatTables, "combat rules");
        java.util.Objects.requireNonNull(chestContentsTable, "chest contents");
        entityProperties = this.entityOptionsLookup();
        slotPropertiesState = new EntitySlotPresentation(spriteHandlers);
        entityDebugConfiguration = new EntityDebugConfiguration(entityState.combatTables, entityState.motions);
        effects = new EntityEffectConsumption(
                entityState.events, entityState.presentationState, entityState.roomVariables);
        flyingEnemyInspection = new EntityFlyingEnemyInspection(entityState.motions, entityState.physicsTables);
        pickupInspection =
                new EntityPickupInspection(bombInteractionsState, entityState.motions, entityState.physicsTables);
        roomInspection = new EntityRoomInspection(
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
        enemyInspection = new EntityEnemyInspection(
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
        lifecycle = new EntitySlotLifecycle(
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
        bossInspection = new EntityBossInspection(
                bossRewardsState, enemyInspection, entityState.motions, entityState.physicsTables);
        slotProperties = new EntitySlotProperties(
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
        pickups = new EntityPickupController(
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
        playerCommands = new EntityPlayerCommands(
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
        backgroundMechanics = new EntityBackgroundMechanics(
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
        bossRewards = new EntityBossRewards(
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
        entityEnemySpawns = new EntityEnemySpawns(
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
        rewards = new EntityRewardController(
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
        bombInteractions = new EntityBombInteractions(
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
        story = new EntityStoryController(
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
        slimeEye = new SlimeEyeController(
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
        entityPlayerSpawns = new EntityPlayerSpawns(
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
        combat = new EntityCombatResolver(
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
        entityRuntimeConfiguration = new EntityRuntimeConfiguration(
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
        projectiles = new EntityProjectileController(
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
        genie = new GenieEntityController(
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
        liftThrow = new EntityLiftThrowController(
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
        roomInitialization = new EntityRoomInitialization(
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

        entityHandlers = roomInitialization.createEntityHandlers();
        if (this.hasRom()) roomInitialization.initializeRomPresentation(romData);
        warpMotion = this.hasRom() ? new WarpMotion(romData) : WarpMotion.DISABLED;

        frameExecution = new EntityFrameExecution(
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

        bossRewards.state.writeInBossBattle(false);
        roomInitialization.initializeRoomSlots(initial);

        return new RoomEntityRuntime(
                new EntitySlotPipeline(frameExecution, frameRandom),
                combat,
                rewards,
                entityPlayerSpawns,
                entityEnemySpawns,
                lifecycle,
                effects,
                playerCommands,
                backgroundMechanics,
                roomInspection,
                entityRuntimeConfiguration,
                entityDebugConfiguration,
                enemyInspection,
                bossInspection,
                flyingEnemyInspection,
                pickupInspection,
                frameExecution);
    }
}
