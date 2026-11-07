package linksawakening.world.entity;

import linksawakening.world.room.ChestContentsTable;
import linksawakening.world.entity.event.BombExplosionEvent;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;

import linksawakening.world.entity.event.HeartPiecePickupUpdate;

import linksawakening.world.entity.motion.WarpMotion;

import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
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

import static linksawakening.world.entity.EntitySlotValidation.*;

import static linksawakening.world.entity.EntityMotionMath.*;

import static linksawakening.world.entity.EntitySnapshots.*;

import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.rom.RomTables;

import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntSupplier;

/**
 * Mutable per-room entity state for the handler work that can be expressed by
 * the current room snapshot model. ROM display lists remain immutable; this
 * class only advances handler-owned status and sprite-variant fields.
 */
public final class RoomEntityRuntime extends EntityRuntimeState {
    private final EntityPlayerCommands playerCommands = new EntityPlayerCommands(this);
    private void disableEntityWithoutPersistence(int slot) { lifecycle.disableEntityWithoutPersistence(slot); }
    final EntityRoomInitialization roomInitialization = new EntityRoomInitialization(this);
    final EntitySlotProperties slotProperties = new EntitySlotProperties(this);
    final EntityBackgroundMechanics backgroundMechanics = new EntityBackgroundMechanics(this);
    final EntityBombInteractions bombInteractions = new EntityBombInteractions(this);
    final EntitySlotLifecycle lifecycle = new EntitySlotLifecycle(this);
    final EntityEnemySpawns entityEnemySpawns = new EntityEnemySpawns(this);
    final EntityPlayerSpawns entityPlayerSpawns = new EntityPlayerSpawns(this);
    private final EntityEffectConsumption effects = new EntityEffectConsumption(this);
    private final EntityRuntimeConfiguration entityRuntimeConfiguration = new EntityRuntimeConfiguration(this);
    final EntityDebugConfiguration entityDebugConfiguration = new EntityDebugConfiguration(this);
    final EntityHandlerRegistry entityHandlers = roomInitialization.createEntityHandlers();
    final EntityRewardController rewards = new EntityRewardController(this);
    final EntityStoryController story = new EntityStoryController(this);
    final GenieEntityController genie = new GenieEntityController(this);
    final EntityProjectileController projectiles = new EntityProjectileController(this);
    final EntityPickupController pickups = new EntityPickupController(this);
    final EntityLiftThrowController liftThrow = new EntityLiftThrowController(this);
    private final EntityCombatResolver combat = new EntityCombatResolver(this);
    final EntityBossRewards bossRewards = new EntityBossRewards(this);
    final EntityRoomInspection roomInspection = new EntityRoomInspection(this);
    final EntityBossInspection bossInspection = new EntityBossInspection(this);
    final EntityFlyingEnemyInspection flyingEnemyInspection = new EntityFlyingEnemyInspection(this);
    final EntityPickupInspection pickupInspection = new EntityPickupInspection(this);
    final EntityEnemyInspection enemyInspection = new EntityEnemyInspection(this);

    final RoomEntity[] slots;
    final java.util.List<RoomEntity> slotEntities;
    
    final EntitySpriteTileSnapshot spriteTiles;
    public final boolean indoorRoom;
    private final RomTables romTables;
    final IntSupplier defaultRandomByteSupplier;
    private final RomRandomByteSource fallbackRomRandomByteSource;
    final EntitySpriteHandlerCatalog spriteHandlers;
    final RomEnemyCombatTables enemyCombatTables;
    final ChestContentsTable chestContentsTable;

    public boolean gelClingingToLink() { return gelClingingToLink; }

    final boolean[] bombFinalPresentationPending =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    public void setFreeMovementMode(boolean enabled) { freeMovementMode = enabled; }

    final WarpMotion warpMotion;

    /** The ROM-facing state needed by Link's carry animation and throw input. */

    /** Object-side effect emitted by MagicPowderSprinkleEntityHandler. */

    public EntityEnemyInspection enemies() { return enemyInspection; }

    public EntityRoomInspection inspection() { return roomInspection; }

    EntityBossInspection bosses() { return bossInspection; }

    EntityFlyingEnemyInspection flyingEnemies() { return flyingEnemyInspection; }

    EntityPickupInspection pickupState() { return pickupInspection; }

    public EntityDebugConfiguration debug() { return entityDebugConfiguration; }

    public EntityRuntimeConfiguration configuration() { return entityRuntimeConfiguration; }

    private RoomEntityRuntime(RoomEntitySnapshot initial, boolean indoorRoom,
                               IntSupplier defaultRandomByteSupplier,
                               EntitySpriteHandlerCatalog spriteHandlers,
                               RomEnemyCombatTables enemyCombatTables,
                               ChestContentsTable chestContentsTable,
                               RomTables romTables,
                               byte[] romData) {
        this.slots = initial.slots().toArray(RoomEntity[]::new);
        this.slotEntities = java.util.Arrays.asList(slots);
        this.spriteSelection = initial.spriteSelection();
        this.spriteTiles = initial.spriteTiles();
        this.groundInteractionSideScrolling = initial.sideScrolling();
        this.indoorRoom = indoorRoom;
        this.defaultRandomByteSupplier = defaultRandomByteSupplier;
        this.fallbackRomRandomByteSource = defaultRandomByteSupplier == null
            ? new RomRandomByteSource() : null;
        this.spriteHandlers = spriteHandlers;
        this.enemyCombatTables = enemyCombatTables;
        this.chestContentsTable = chestContentsTable;
        this.romTables = romTables;
        roomInitialization.initializeRomPresentation(romData);
        this.warpMotion = romData == null ? null : new WarpMotion(romData);
        roomInitialization.initializeRoomSlots(initial);
    }

    public static RoomEntityRuntime from(RoomEntitySnapshot initial) {
        return from(initial, false);
    }

    public static RoomEntityRuntime from(RoomEntitySnapshot initial, boolean indoorRoom) {
        if (initial == null) {
            throw new IllegalArgumentException("Initial entity snapshot cannot be null");
        }
        return new RoomEntityRuntime(initial, indoorRoom, null, null, null, null, null, null);
    }

    public static RoomEntityRuntime from(RoomEntitySnapshot initial, boolean indoorRoom,
                                         IntSupplier randomByteSupplier) {
        if (initial == null) {
            throw new IllegalArgumentException("Initial entity snapshot cannot be null");
        }
        return new RoomEntityRuntime(initial, indoorRoom, randomByteSupplier, null, null, null, null,
            null);
    }

    public static RoomEntityRuntime from(RoomEntitySnapshot initial, boolean indoorRoom,
                                         IntSupplier randomByteSupplier,
                                         EntitySpriteHandlerCatalog spriteHandlers) {
        if (initial == null) {
            throw new IllegalArgumentException("Initial entity snapshot cannot be null");
        }
        return new RoomEntityRuntime(initial, indoorRoom, randomByteSupplier, spriteHandlers,
            null, null, null, null);
    }

    public static RoomEntityRuntime from(RoomEntitySnapshot initial, boolean indoorRoom,
                                         IntSupplier randomByteSupplier,
                                         EntitySpriteHandlerCatalog spriteHandlers,
                                         RomEnemyCombatTables enemyCombatTables) {
        if (initial == null) {
            throw new IllegalArgumentException("Initial entity snapshot cannot be null");
        }
        if (enemyCombatTables == null) {
            throw new IllegalArgumentException("ROM enemy combat tables cannot be null");
        }
        return new RoomEntityRuntime(initial, indoorRoom, randomByteSupplier, spriteHandlers,
            enemyCombatTables, null, null, null);
    }

    public static RoomEntityRuntime from(RoomEntitySnapshot initial, boolean indoorRoom,
                                         IntSupplier randomByteSupplier,
                                         EntitySpriteHandlerCatalog spriteHandlers,
                                         RomEnemyCombatTables enemyCombatTables,
                                         ChestContentsTable chestContentsTable) {
        if (initial == null) {
            throw new IllegalArgumentException("Initial entity snapshot cannot be null");
        }
        if (enemyCombatTables == null) {
            throw new IllegalArgumentException("ROM enemy combat tables cannot be null");
        }
        if (chestContentsTable == null) {
            throw new IllegalArgumentException("ROM chest contents table cannot be null");
        }
        return new RoomEntityRuntime(initial, indoorRoom, randomByteSupplier, spriteHandlers,
            enemyCombatTables, chestContentsTable, null, null);
    }

    /** Full room runtime construction with the ROM entity hitbox table. */
    public static RoomEntityRuntime from(RoomEntitySnapshot initial, boolean indoorRoom,
                                         IntSupplier randomByteSupplier,
                                         EntitySpriteHandlerCatalog spriteHandlers,
                                         RomEnemyCombatTables enemyCombatTables,
                                         ChestContentsTable chestContentsTable,
                                         RomTables romTables) {
        if (initial == null || enemyCombatTables == null || chestContentsTable == null
            || romTables == null) {
            throw new IllegalArgumentException("ROM room runtime inputs cannot be null");
        }
        return new RoomEntityRuntime(initial, indoorRoom, randomByteSupplier, spriteHandlers,
            enemyCombatTables, chestContentsTable, romTables, null);
    }

    /** Full room runtime construction with raw ROM data for ROM-owned handlers. */
    public static RoomEntityRuntime from(RoomEntitySnapshot initial, boolean indoorRoom,
                                         IntSupplier randomByteSupplier,
                                         EntitySpriteHandlerCatalog spriteHandlers,
                                         RomEnemyCombatTables enemyCombatTables,
                                         ChestContentsTable chestContentsTable,
                                         RomTables romTables, byte[] romData) {
        if (initial == null || enemyCombatTables == null || chestContentsTable == null
            || romTables == null || romData == null) {
            throw new IllegalArgumentException("ROM room runtime inputs cannot be null");
        }
        return new RoomEntityRuntime(initial, indoorRoom, randomByteSupplier, spriteHandlers,
            enemyCombatTables, chestContentsTable, romTables, romData);
    }

    /** Frame-only inputs using the runtime RNG, including its carried state. */
    EntityFrameContext frameContext(int frameCounter) {
        IntSupplier randomByteSupplier = defaultRandomByteSupplier;
        if (randomByteSupplier == null) {
            fallbackRomRandomByteSource.beginFrame(frameCounter & 0xFF, 0);
            randomByteSupplier = fallbackRomRandomByteSource;
        }
        return new EntityFrameContext(frameCounter, 0, 0, 0, randomByteSupplier, null, null, null, 0, false, 0, 0, EnemyProjectileCollision.LinkState.nonInteractive(), false, 0, 0, 0, 0, 0, 0, false, true);
    }

    /** Advances one live frame from explicit inputs. */
    public List<EntityProjectileEvent> tick(EntityFrameContext context) {
        validateByte(context.linkSpeedX(), "Link X speed");
        validateByte(context.linkSpeedY(), "Link Y speed");
        linkState.currentLinkSpeedX = context.linkSpeedX();
        linkState.currentLinkSpeedY = context.linkSpeedY();
        try {
            return advanceFrame(context);
        } finally {
            linkState.currentLinkSpeedX = 0;
            linkState.currentLinkSpeedY = 0;
        }
    }

    /** Source-ordered frame gates and descending slot dispatch. */
    private List<EntityProjectileEvent> advanceFrame(EntityFrameContext context) {
        EntityFrameExecution state = new EntityFrameExecution(this, context);

        state.frameCounter = state.context.frameCounter();
        state.linkEntityX = state.context.linkEntityX();
        state.linkEntityY = state.context.linkEntityY();
        state.collisionType = state.context.collisionType();
        state.randomByteSupplier = state.context.randomByteSupplier();
        state.backgroundCollision = state.context.backgroundCollision();
        state.objectCollision = state.context.objectCollision();
        state.linkPositionHistory = state.context.linkPositionHistory();
        state.linkZ = state.context.linkZ();
        state.linkAirborne = state.context.linkAirborne();
        state.linkDirection = state.context.linkDirection();
        state.entityYOffset = state.context.entityYOffset();
        state.projectileLinkState = state.context.projectileLinkState();
        state.creditsGameplay = state.context.creditsGameplay();
        state.swordCollisionActive = state.context.swordCollisionActive();
        state.swordX = state.context.swordX();
        state.swordWidth = state.context.swordWidth();
        state.swordY = state.context.swordY();
        state.swordHeight = state.context.swordHeight();
        state.handlerLinkCollisionEnabled = state.context.handlerLinkCollisionEnabled();
        Objects.requireNonNull(state.randomByteSupplier, "randomByteSupplier");
        Objects.requireNonNull(state.projectileLinkState, "projectileLinkState");
        state.runtime.gelClingingToLink =
                false; // AnimateEntities clears wIsGelClingingToLink each pass.
        state.countdownBlocksInteraction = state.runtime.entityInteractionCountdown != 0;
        if (!state.runtime.linkState.dialogActive && state.runtime.entityInteractionCountdown > 0)
            state.runtime.entityInteractionCountdown--;
        state.handlerLinkCollisionEnabled =
                state.handlerLinkCollisionEnabled
                        && !state.runtime.linkState.inventoryAppearing
                        && !state.countdownBlocksInteraction;
        state.threeOfAKindInteractive =
                state.handlerLinkCollisionEnabled
                        && state.runtime.backgroundMechanics.entityTimersInteractive();
        state.handlerInteractive =
                state.runtime.backgroundMechanics.entityHandlerInteractive(state.creditsGameplay);
        state.bowWowHandlerInteractive =
                state.runtime.transitionSequenceCounter == 0x04
                        && !state.runtime.roomTransitionActive
                        && !state.runtime.linkState.dialogActive
                        && !state.runtime.linkState.inventoryAppearing
                        && !state.runtime.inspection().witchGotItemPresentationActive();
        state.projectileTimersInteractive =
                state.runtime.backgroundMechanics.gameplayEntityTimersInteractive(state.creditsGameplay);
        state.sideViewPotInteractive = state.handlerInteractive;
        state.sideViewPotLinkCollisionInteractive =
                state.sideViewPotInteractive
                        && state.handlerLinkCollisionEnabled
                        && state.projectileLinkState.motionState()
                                < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE;
        state.runtime.shouldGetLostInMysteriousWoods = false;
        state.runtime.bombInteractions.finalizePendingBombPresentations();
        state.frame = state.frameCounter & 0xFF;
        state.romCollisionType = state.collisionType & 0xFF;
        // WorldHandler runs $14:4B21 before AnimateEntities, even when no
        // boomerang exists or dialogs/room transitions suspend its handler.
        if (!state.runtime.boomerangSoundClockProvided
                && state.runtime.gameplayWorld
                && !state.creditsGameplay) {
            state.runtime.boomerangSfxCounter =
                    (state.runtime.boomerangSfxCounter + 1) % state.runtime.BOOMERANG_SFX_COUNTER_PERIOD;
        }
        state.runtime.boomerangSoundClockProvided = false;
        if (state.runtime.swordMoblinAlertingSoundCounter > 0) {
            state.runtime.swordMoblinAlertingSoundCounter--;
        }
        if (state.runtime.bombArrowCooldown > 0) {
            state.runtime.bombArrowCooldown--;
        }
        if (state.runtime.backgroundInteraction != null) {
            state.backgroundCollision =
                    (entity, direction, nextX, nextY) -> {
                        EntityBackgroundCollisionResult result =
                                state.runtime.backgroundInteraction.probe(
                                        entity,
                                        direction,
                                        nextX,
                                        nextY,
                                        state.runtime.enemyIgnoreHitsCountdown[entity.slot()],
                                        state.frame);
                        if (result.blocked()
                                && entity.status() == EntityStatus.THROWN
                                && checksThrownRoomEventTriggers(entity.type())) {
                            state.runtime.events.pendingThrownBackgroundCollisions.add(
                                    new ThrownBackgroundCollisionEvent(
                                            entity.slot(), entity.type(), direction, result.objectId()));
                        }
                        return result.blocked();
                    };
        }
        state.hinoxBackgroundInteraction = state.runtime.backgroundInteraction;
        if (state.hinoxBackgroundInteraction == null && state.backgroundCollision != null) {
            state.hinoxBackgroundInteraction =
                    RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
        }
        state.runtime.events.projectileLaunchRequests.clear();
        Arrays.fill(state.runtime.enemyProjectileSpawnedThisFrame, false);
        Arrays.fill(state.runtime.dynamicEntitySpawnedThisFrame, false);
        Arrays.fill(state.runtime.genieImpactRegisteredThisFrame, false);
        Arrays.fill(state.runtime.wingedSwordAttackThisFrame, false);
        Arrays.fill(state.runtime.wingedStateTwoThisFrame, false);
        state.runtime.events.transientVfxRequests.clear();
        state.runtime.events.pendingLinkFinalPositionRequests.clear();
        state.runtime.events.pendingRoosterLinkStateRequests.clear();
        state.runtime.events.pendingLinkMotionBlockRequests.clear();
        state.runtime.events.pendingSideViewPlatformLinkRequests.clear();
        state.runtime.events.pendingSideViewPotLinkRequests.clear();
        state.runtime.events.pendingHinoxLinkEffectRequests.clear();
        state.runtime.events.pendingLinkFallenPoseRequests.clear();
        state.runtime.events.pendingLinkFacingRequests.clear();
        state.runtime.events.pendingLinkAttackStepRequests.clear();
        state.runtime.events.pendingLinkAttackStepRequests.addAll(
                state.runtime.events.pendingPreTickLinkAttackStepRequests);
        state.runtime.events.pendingPreTickLinkAttackStepRequests.clear();
        state.runtime.events.pendingLinkAttackClearRequests.clear();
        state.runtime.events.pendingRoomStatusPersistenceRequests.clear();
        state.runtime.events.pendingLinkHeldItemPoseRequests.clear();
        state.runtime.events.pendingLinkSwordSpinPoseRequests.clear();
        state.runtime.events.pendingLinkSwordFinalPoseRequests.clear();
        state.runtime.events.pendingScreenShakeRequests.clear();
        state.runtime.events.boomerangObjectRequests.clear();
        state.runtime.events.magicRodObjectRequests.clear();
        state.runtime.events.magicPowderObjectRequests.clear();
        state.runtime.events.pendingDialogRequests.clear();
        state.runtime.events.pendingEntityEvents.clear();
        state.runtime.events.pendingThrownBackgroundCollisions.clear();
        state.runtime.events.pendingEntityEvents.addAll(state.runtime.events.pendingPreTickEntityEvents);
        state.runtime.events.pendingPreTickEntityEvents.clear();
        state.runtime.events.pendingWarpLinkStateRequests.clear();
        state.runtime.events.pendingChestRewardEvents.clear();
        state.runtime.events.pendingHeartContainerRewards.clear();
        state.runtime.events.pendingMadamMeowMeowFullHealRequests.clear();
        state.runtime.events.pendingSwordPickupRewards.clear();
        state.runtime.events.pendingToadstoolRewards.clear();
        state.runtime.events.pendingInstrumentRewards.clear();
        state.runtime.events.pendingInstrumentCompletions.clear();
        state.runtime.events.pendingRichardProgressEvents.clear();
        if (state.runtime.bossAgonySfxCountdown > 0) {
            state.runtime.bossAgonySfxCountdown--;
            if (state.runtime.bossAgonySfxCountdown == 0) {
                state.runtime.events.pendingEntityEvents.add(
                        new EntityCombatEvent(
                                state.runtime.bossAgonySourceSlot,
                                state.runtime.ENTITY_GENIE,
                                0,
                                false,
                                EntityCombatEvent.SoundChannel.WAVE,
                                0x10));
            }
        }
        state.runtime.events.pendingWitchExchangeEvents.clear();
        state.runtime.events.pendingWitchRewardEvents.clear();
        state.runtime.events.pendingOwlEventCompletions.clear();
        state.runtime.events.pendingSlimeKeyRewardEvents.clear();
        state.runtime.events.pendingKeyQuicksandEvents.clear();
        state.runtime.events.pendingLikeLikeEvents.clear();
        state.runtime.events.pendingBombExplosionEvents.clear();
        state.runtime.events.hookshotBridgeUpdates.clear();
        state.runtime.pendingSwitchBlockAnimationRequest = false;
        if (state.runtime.linkState.linkPlayingOcarinaCountdown >= 0x10
                && state.runtime.linkState.ocarinaAnimationCounter == 0x14) {
            state.runtime.entityPlayerSpawns.spawnMusicalNote(
                    state.linkEntityX, state.linkEntityY, state.runtime.linkState.ocarinaAnimationPhase);
        }
        state.projectileEvents =
                state.runtime.events.outbox.channel(EntityProjectileEvent.class, "projectileEvents");
        state.projectileEvents.clear();
        if (state.linkPositionHistory != null) {
            state.runtime.followingLinkPositionHistory = state.linkPositionHistory;
        }
        state.runtime.linkState.followingLinkZ = state.linkZ & 0xFF;
        state.runtime.linkState.followingLinkDirection = state.linkDirection & 0xFF;
        state.runtime.linkState.followingEntityYOffset = state.entityYOffset & 0xFF;
        state.carryingEntity = state.runtime.inspection().liftedEntityState().carryState() != 0;
        state.romLinkDirection = romDirectionForJavaDirection(state.linkDirection);
        state.runtime.linkState.lastRomLinkDirection = state.romLinkDirection;
        for (state.index = state.runtime.slots.length - 1; state.index >= 0; state.index--) {
            if (!EntitySlotPhase1.run(state)) continue;
            if (!EntitySlotPhase2.run(state)) continue;
            if (!EntitySlotPhase3.run(state)) continue;
            if (!EntitySlotPhase4.run(state)) continue;
            if (!EntitySlotPhase5.run(state)) continue;
        }
        if (state.threeOfAKindInteractive) {
            state.runtime.slotProperties.resolveThreeOfAKindPuzzle();
        }
        state.runtime.slotProperties.updateHookshotChainOam(
                state.linkEntityX, state.linkEntityY, state.frame);
        state.runtime.slotProperties.updatePincerBodyOam();
        state.runtime.slotProperties.updateWingedOctorokOam();
        state.runtime.slotProperties.updateBowWowChain(state.frame);
        return state.runtime.events.outbox.drain(EntityProjectileEvent.class, "projectileEvents");
    }

    /**
     * Runs the room-entity portion of PickableCollectIfNeeded. A non-null
     * event means the entity completed its pickup collision and its first-eight
     * persistence bit is ready for the room session to record.
     */
    public EntityPickupEvent collectIfNeeded(int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive) { return this.lifecycle.collectIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive); }

    /** Pickup collision with Link's Java direction available for held items. */
    public EntityPickupEvent collectIfNeeded(int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive, int linkDirection) { return this.lifecycle.collectIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, linkDirection); }

    /** Pickup collision with Link's ROM-facing Z available for floating items. */
    public EntityPickupEvent collectIfNeeded(int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive, int linkDirection, int linkZ) { return this.lifecycle.collectIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, linkDirection, linkZ); }

    /**
     * Starts EntityGetLiftedUp's shared state transition. Directions are the
     * ROM order (right, left, up, down); RoomSession performs the Java-to-ROM
     * conversion at its boundary.
     */
    public boolean beginLift(int slot, int romDirection) { return this.playerCommands.beginLift(slot, romDirection); }

    /** Hands the fully held entity to func_014_53A3's generic throw path. */
    public boolean throwLiftedEntity(int romDirection) { return this.playerCommands.throwLiftedEntity(romDirection); }

    /** Applies the two source-ordered probes made by ApplyEntityInteractionWithBackground. */

    /** Mirrors EntityThrownHandler's post-bounce func_003_75A2 collision pass. */

    /** Mirrors CuccoEntityHandler's B-slot-first Power Bracelet check. */

    /**
     * Runs the ported portion of DefaultEnemyDamageCollisionHandler for the
     * currently supported enemy families. The four sword arguments are the
     * ROM's wC140..wC143 rectangle; non-positive width/height disables sword
     * collision for this pass.
     */
    public List<EntityCombatEvent> resolveCombat(int frameCounter,
                                                 int linkEntityX,
                                                 int linkEntityY,
                                                 boolean linkAirborne,
                                                 int linkVerticalVelocity,
                                                 boolean linkInteractive,
                                                 boolean swordCollisionActive,
                                                 int swordX,
                                                 int swordWidth,
                                                 int swordY,
                                                 int swordHeight) {
        return resolveCombat(frameCounter, linkEntityX, linkEntityY, linkAirborne,
            linkVerticalVelocity, linkInteractive, swordCollisionActive, swordX, swordWidth,
            swordY, swordHeight, EnemyAttackContext.standard());
    }

    public List<EntityCombatEvent> resolveCombat(int frameCounter,
                                                 int linkEntityX,
                                                 int linkEntityY,
                                                 boolean linkAirborne,
                                                 boolean linkInteractive,
                                                 boolean swordCollisionActive,
                                                 int swordX,
                                                 int swordWidth,
                                                 int swordY,
                                                 int swordHeight) {
        return resolveCombat(frameCounter, linkEntityX, linkEntityY, linkAirborne,
            0, linkInteractive, swordCollisionActive, swordX, swordWidth, swordY, swordHeight,
            EnemyAttackContext.standard());
    }

    public List<EntityCombatEvent> resolveCombat(int frameCounter,
                                                 int linkEntityX,
                                                 int linkEntityY,
                                                 boolean linkAirborne,
                                                 boolean linkInteractive,
                                                 boolean swordCollisionActive,
                                                 int swordX,
                                                 int swordWidth,
                                                 int swordY,
                                                 int swordHeight,
                                                 EnemyAttackContext attackContext) {
        return resolveCombat(frameCounter, linkEntityX, linkEntityY, linkAirborne,
            0, linkInteractive, swordCollisionActive, swordX, swordWidth, swordY, swordHeight,
            attackContext);
    }

    public List<EntityCombatEvent> resolveCombat(int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, int linkVerticalVelocity, boolean linkInteractive, boolean swordCollisionActive, int swordX, int swordWidth, int swordY, int swordHeight, EnemyAttackContext attackContext) { return combat.resolveCombat(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkVerticalVelocity, linkInteractive, swordCollisionActive, swordX, swordWidth, swordY, swordHeight, attackContext); }

    /** Supplies the persistent world clock before this frame's entity pass. */

    /** Advances entity $01 through the ROM boomerang handler's two states. */

    /** Advances entity {@code $DF} through the ROM sword-beam handler. */

    /** Advances entity {@code $04} through MagicRodFireballEntityHandler. */

    /** Advances entity {@code $08} through MagicPowderSprinkleEntityHandler. */

    /** Shared result path of ApplySwordIntersectionWithObjects ($7CAB). */

    enum ProjectileObjectCollision {
        NONE, BLOCKED, UNLOAD
    }

    /** Common bank-$03 collision pass for entity {@code $DF}. */
    record ProjectileEntityCollision(boolean unload, boolean blocked) {}

    /** Common bank-$03 collision pass with DAMAGE_TYPE_MAGIC_ROD ($0A). */

    /**
     * Port of the common bank-$03 func_003_75A2 pass for player arrows. The
     * active arrow checks targets in descending slot order and uses the same
     * alternating frame cadence and unsigned twelve-pixel windows as the ROM.
     */

    /** Common bank-$03 collision pass for the active boomerang. */

    /**
     * Ports the BouncingBombite-specific portion of bank-$03
     * {@code func_003_75A2}. The ROM scans every target in descending slot
     * order, on alternating frames, and uses the active Bombite's transition
     * byte as the source-side explosion latch.
     */

    /**
     * Ports the Iron Mask target branch of bank-$03 {@code func_003_75A2} for
     * the active hookshot chain. Other hookshot target damage branches remain
     * owned by their entity-specific runtimes; this branch is special because
     * it changes the target's private state and creates a visible mask entity.
     */

    int initialHealth(int type) { return this.slotProperties.initialHealth(type); }

    int contactDamage(int type) { return this.slotProperties.contactDamage(type); }

    /** Unloads a slot and returns the persistent first-eight load-order bit. */
    public int clearEntity(int slot) { return this.lifecycle.clearEntity(slot); }

    /** Creates the ROM's player-projectile entity type {@code $03}. */
    public int spawnHookshotChain(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) { return this.entityPlayerSpawns.spawnHookshotChain(linkEntityX, linkEntityY, linkEntityZ, romDirection); }

    /** Spawns bank 3's temporary entity $06 at an indoor object-cell center. */
    public int spawnPushedBlock(int x, int y) { return this.entityPlayerSpawns.spawnPushedBlock(x, y); }

    /** Spawns bank 0's temporary type-$05 entity already entering lifted status. */
    public int spawnLiftedRoomObject(int x, int y, int romDirection, int spriteVariant) { return this.entityPlayerSpawns.spawnLiftedRoomObject(x, y, romDirection, spriteVariant); }

    public void movePushedBlock(int slot, int x, int y) { this.playerCommands.movePushedBlock(slot, x, y); }

    /** $03:5269: after block movement, restore Link even during a jump. */
    public void resolvePushedBlockLinkContact(int slot, int linkEntityX, int linkEntityY) { this.backgroundMechanics.resolvePushedBlockLinkContact(slot, linkEntityX, linkEntityY); }

    public void removePushedBlock(int slot) { this.playerCommands.removePushedBlock(slot); }

    /** Spawns DropKeyEffectHandler's ordinary dungeon key at ($28,$3C,$70). */
    public int spawnRoomEventKeyDrop() { return this.entityPlayerSpawns.spawnRoomEventKeyDrop(); }

    /** Creates the ROM's ordinary player-arrow entity type {@code $00}. */
    public int spawnArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) { return this.entityPlayerSpawns.spawnArrow(linkEntityX, linkEntityY, linkEntityZ, romDirection); }

    public int spawnArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) { return this.entityPlayerSpawns.spawnArrow(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower); }

    /** Creates the ROM's player-boomerang entity type {@code $01}. */
    public int spawnBoomerang(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, int pressedButtonsMask) { return this.entityPlayerSpawns.spawnBoomerang(linkEntityX, linkEntityY, linkEntityZ, romDirection, pressedButtonsMask); }

    /** Creates the ROM's level-two sword-beam entity type {@code $DF}. */
    public int spawnSwordBeam(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) { return this.entityPlayerSpawns.spawnSwordBeam(linkEntityX, linkEntityY, linkEntityZ, romDirection); }

    /** Creates the ROM's Magic Rod fireball entity type {@code $04}. */
    public int spawnMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) { return this.entityPlayerSpawns.spawnMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection); }

    public int spawnMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) { return this.entityPlayerSpawns.spawnMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower); }

    /** Creates the ROM's type-$08 Magic Powder sprinkle entity. */
    public int spawnMagicPowderSprinkle(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) { return this.entityPlayerSpawns.spawnMagicPowderSprinkle(linkEntityX, linkEntityY, linkEntityZ, romDirection); }

    /** Mirrors the player projectile handlers that feed wActiveProjectileCount. */

    /** Creates the ROM's ordinary player bomb entity type {@code $02}. */
    public int spawnBomb(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) { return this.entityPlayerSpawns.spawnBomb(linkEntityX, linkEntityY, linkEntityZ, romDirection); }

    /** Creates the source's type-$02 enemy bomb with privateState4 set to $01. */
    public int spawnEnemyBomb(int entityX, int entityY, int entityZ, int transitionCountdown) { return this.entityEnemySpawns.spawnEnemyBomb(entityX, entityY, entityZ, transitionCountdown); }

    /** Creates a Bomber-spawned type-$02 enemy bomb with its source speeds. */
    public int spawnEnemyBomb(int entityX, int entityY, int entityZ, int transitionCountdown, int speedX, int speedY, int speedZ) { return this.entityEnemySpawns.spawnEnemyBomb(entityX, entityY, entityZ, transitionCountdown, speedX, speedY, speedZ); }

    public int spawnEnemyBomb(int entityX, int entityY, int entityZ, int transitionCountdown, int speedX, int speedY, int speedZ, boolean initializeMotion, int privateState4) { return this.entityEnemySpawns.spawnEnemyBomb(entityX, entityY, entityZ, transitionCountdown, speedX, speedY, speedZ, initializeMotion, privateState4); }

    /** Mirrors the state-$00 return gate reached after Genie's BossIntro call. */

    /** Mirrors UpdateEntityTimers' gameplay and presentation gates. */

    /** Creates the temporary type-$05 entity used by bombed bushes, grass, and pots. */
    public int spawnLiftableRockSmash(int entityX, int entityY, int sourceSpriteVariant) { return this.entityPlayerSpawns.spawnLiftableRockSmash(entityX, entityY, sourceSpriteVariant); }

    /** Creates one of the four silent type-$05 rubble entities from bank-$36. */
    public int spawnLiftableRockRubble(int entityX, int entityY) { return this.entityPlayerSpawns.spawnLiftableRockRubble(entityX, entityY); }

    /** Applies the position and variant writes made by MarinEntityHandler_Indoor. */
    public boolean applyMarinWakeUpPresentation(int x, int y, int spriteVariant) { return this.playerCommands.applyMarinWakeUpPresentation(x, y, spriteVariant); }

    public void updateHouseTarinPresentation(int linkX, int linkY) { this.playerCommands.updateHouseTarinPresentation(linkX, linkY); }

    /** Supplies wDialogState's nonzero gate to handlers with dialog branches. */

    /** Supplies the nonzero wActiveMusicIndex gate used by instrument pickup. */

    /** Supplies the source-prioritized held A/B button for an equipped bomb. */

    /** Supplies the captured hLinkFinalPositionX visible to entity handlers. */

    /** Supplies the current map's wHasInstrument1 bit to entity-$61. */

    /** Supplies the three player upgrade bytes read by ChestWithItemEntityHandler. */

    /** Supplies wHasTailKey to OwlEventEntityHandler. */

    public boolean consumePendingSwitchBlockAnimationRequest() { return this.effects.consumePendingSwitchBlockAnimationRequest(); }

    public int consumePendingClearedEntityMask() { return this.effects.consumePendingClearedEntityMask(); }

    public int consumePendingRoomStatusMask() { return this.effects.consumePendingRoomStatusMask(); }

    public List<EntityCombatEvent> consumePendingEntityEvents() { return this.effects.consumePendingEntityEvents(); }

    public List<ThrownBackgroundCollisionEvent> consumePendingThrownBackgroundCollisions() { return this.effects.consumePendingThrownBackgroundCollisions(); }

    public List<DungeonWarpRequest> consumePendingDungeonWarpRequests() { return this.effects.consumePendingDungeonWarpRequests(); }

    public List<WarpLinkStateRequest> consumePendingWarpLinkStateRequests() { return this.effects.consumePendingWarpLinkStateRequests(); }

    public List<LinkFinalPositionRequest> consumePendingLinkFinalPositionRequests() { return this.effects.consumePendingLinkFinalPositionRequests(); }

    public List<RoosterLinkStateRequest> consumePendingRoosterLinkStateRequests() { return this.effects.consumePendingRoosterLinkStateRequests(); }

    public List<LinkMotionBlockRequest> consumePendingLinkMotionBlockRequests() { return this.effects.consumePendingLinkMotionBlockRequests(); }

    public List<SideViewPlatformLinkRequest> consumePendingSideViewPlatformLinkRequests() { return this.effects.consumePendingSideViewPlatformLinkRequests(); }

    public List<SideViewPotLinkRequest> consumePendingSideViewPotLinkRequests() { return this.effects.consumePendingSideViewPotLinkRequests(); }

    public List<HinoxLinkEffectRequest> consumePendingHinoxLinkEffectRequests() { return this.effects.consumePendingHinoxLinkEffectRequests(); }

    public List<LinkFallenPoseRequest> consumePendingLinkFallenPoseRequests() { return this.effects.consumePendingLinkFallenPoseRequests(); }

    public List<LinkFacingRequest> consumePendingLinkFacingRequests() { return this.effects.consumePendingLinkFacingRequests(); }

    public List<LinkAttackClearRequest> consumePendingLinkAttackClearRequests() { return this.effects.consumePendingLinkAttackClearRequests(); }

    public List<LinkAttackStepRequest> consumePendingLinkAttackStepRequests() { return this.effects.consumePendingLinkAttackStepRequests(); }

    public List<RoomStatusPersistenceRequest> consumePendingRoomStatusPersistenceRequests() { return this.effects.consumePendingRoomStatusPersistenceRequests(); }

    public List<LinkHeldItemPoseRequest> consumePendingLinkHeldItemPoseRequests() { return this.effects.consumePendingLinkHeldItemPoseRequests(); }

    public List<LinkSwordSpinPoseRequest> consumePendingLinkSwordSpinPoseRequests() { return this.effects.consumePendingLinkSwordSpinPoseRequests(); }

    public List<LinkSwordFinalPoseRequest> consumePendingLinkSwordFinalPoseRequests() { return this.effects.consumePendingLinkSwordFinalPoseRequests(); }

    public List<ScreenShakeRequest> consumePendingScreenShakeRequests() { return this.effects.consumePendingScreenShakeRequests(); }

    public List<ChestRewardEvent> consumePendingChestRewardEvents() { return this.effects.consumePendingChestRewardEvents(); }

    public List<KeyRewardEvent> consumePendingKeyRewardEvents() { return this.effects.consumePendingKeyRewardEvents(); }

    public List<SlimeKeyRewardEvent> consumePendingSlimeKeyRewardEvents() { return this.effects.consumePendingSlimeKeyRewardEvents(); }

    public List<KeyQuicksandEvent> consumePendingKeyQuicksandEvents() { return this.effects.consumePendingKeyQuicksandEvents(); }

    public int consumePendingMusicTrack() { return this.effects.consumePendingMusicTrack(); }

    public int consumePendingMusicFadeOutCountdown() { return this.effects.consumePendingMusicFadeOutCountdown(); }

    public List<LikeLikeEvent> consumePendingLikeLikeEvents() { return this.effects.consumePendingLikeLikeEvents(); }

    public List<DialogRequest> consumePendingDialogRequests() { return this.effects.consumePendingDialogRequests(); }

    public List<BombExplosionEvent> consumeBombExplosionEvents() { return this.effects.consumeBombExplosionEvents(); }

    /** Mirrors bank-$03 ApplyLinkCollisionWithEnemy's Goomba stomp branch. */

    /** Ports {@code RollingBonesEntityHandler}'s long miniboss destruction sequence. */

    /** Bank $15:7C37..7CA5: two death waits, then DidKillEnemy. */

    void startSwordPickup(int slot) { this.playerCommands.startSwordPickup(slot); }

    void startToadstoolPickup(int slot) { this.playerCommands.startToadstoolPickup(slot); }

    public void setHeartPiecesCount(int count) { heartPiecesCount = count; }

    public List<HeartPiecePickupUpdate> consumeHeartPieceUpdates() { return this.effects.consumeHeartPieceUpdates(); }

    public void presentWitchGotItem(int slot) { this.playerCommands.presentWitchGotItem(slot); }

    /** ProcessPowerUp ($03:6400) and ApplyGotItem ($00:1033). */

    void holdSwordAboveLink(RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ, boolean finalPose) { this.playerCommands.holdSwordAboveLink(entity, linkEntityX, linkEntityY, linkZ, finalPose); }

    void blockLinkForOwl(int slot) {
        events.pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
    }

    /** Mirrors Bow-Wow's label_005_4335 target-resolution branches. */

    /**
     * Ports the boss-specific death handlers that eventually create entity
     * {@code $30}. Boss entities stay on their active handler while DYING, so
     * they must not enter the ordinary EnemyDropResolver path.
     */

    /**
     * Mirrors the shovel's delayed random reward branch in
     * {@code label_002_4C92}. The first random byte is consumed even when the
     * current room is not allowed to produce a reward; the caller supplies
     * that room-specific restriction explicitly.
     */
    public int spawnShovelDrop(int objectLeft, int objectTop, int linkEntityX, int linkEntityY, boolean allowDrop) { return this.entityPlayerSpawns.spawnShovelDrop(objectLeft, objectTop, linkEntityX, linkEntityY, allowDrop); }

    /**
     * Mirrors SpawnChestWithItem. The arguments are the intersected object's
     * unaligned left/top coordinates; the source masks them to a room cell
     * and places the entity at (+8,+16).
     */
    public int spawnChestWithItem(int objectLeft, int objectTop, int itemType) { return this.entityPlayerSpawns.spawnChestWithItem(objectLeft, objectTop, itemType); }

    int findFreeEntitySlot() {
        for (int slot = slots.length - 1; slot >= 0; slot--) {
            if (!slots[slot].loaded()) {
                // Dynamic entity paths reuse this slot directly rather than
                // necessarily passing through clearEntity first.
                enemyHitboxFlags[slot] = 0;
                return slot;
            }
        }
        return -1;
    }

    int findFreeEntitySlotAtMost(int highestSlot) {
        int first = Math.min(highestSlot, slots.length - 1);
        for (int slot = first; slot >= 0; slot--) {
            if (!slots[slot].loaded()) {
                enemyHitboxFlags[slot] = 0;
                return slot;
            }
        }
        return -1;
    }

    /** Mirrors ZombieEntityHandler's SpawnNewEntityInRange call with E=$05. */

    /** Mirrors PokeyEntityHandler's SpawnNewEntity plus its detached-segment setup. */

    /**
     * Ports BushCrawlerEntityHandler's Power Bracelet branch: the source
     * slot becomes a lifted type-$05 entity and a fresh type-$BB slot starts
     * the private-state-$02 wandering handler at the same position.
     */

    /** Ports CuccoState2Handler's outdoor angry-Cucco spawn setup. */

    /** Returns true when the source handler unloaded the chest this frame. */

    /** Mirrors BombExplosionHandler's separate privateState4 enemy-bomb path. */

    /**
     * Ports the damage-table part of CheckExplosionInteractionWithEntities.
     * The ROM writes the source-relative length-$30 recoil after the generic
     * damage routine, even when the selected damage-table value is zero or a
     * source-specific special value.
     */

    /** Mirrors EntityStunnedHandler's recoil/bounce/clear-speed tail for a bomb. */

    public List<HeartContainerRewardEvent> consumePendingHeartContainerRewards() { return this.effects.consumePendingHeartContainerRewards(); }

    public List<MadamMeowMeowFullHealRequest> consumePendingMadamMeowMeowFullHealRequests() { return this.effects.consumePendingMadamMeowMeowFullHealRequests(); }

    public List<SwordPickupRewardEvent> consumePendingSwordPickupRewards() { return this.effects.consumePendingSwordPickupRewards(); }

    public List<ToadstoolRewardEvent> consumePendingToadstoolRewards() { return this.effects.consumePendingToadstoolRewards(); }

    public List<InstrumentRewardEvent> consumePendingInstrumentRewards() { return this.effects.consumePendingInstrumentRewards(); }

    public List<InstrumentCompletionEvent> consumePendingInstrumentCompletions() { return this.effects.consumePendingInstrumentCompletions(); }

    public List<RichardProgressEvent> consumePendingRichardProgressEvents() { return this.effects.consumePendingRichardProgressEvents(); }

    public List<WitchExchangeEvent> consumePendingWitchExchangeEvents() { return this.effects.consumePendingWitchExchangeEvents(); }

    public List<WitchRewardEvent> consumePendingWitchRewardEvents() { return this.effects.consumePendingWitchRewardEvents(); }

    public List<OwlEventCompletion> consumePendingOwlEventCompletions() { return this.effects.consumePendingOwlEventCompletions(); }

    public int thrownDirection(int slot) {
        validateEntitySlot(slot);
        return thrownDirection[slot] & 0xFF;
    }

    public int options1(int slot) { return this.slotProperties.options1(slot); }

    public record CommonDroppableUpdate(RoomEntity entity, boolean hidden) {
    }

    public record SlimeKeyTransitionUpdate(RoomEntity entity, int nextTransitionCountdown,
                                            boolean holdAboveLink, boolean unload,
                                            SlimeKeyRewardEvent reward) {
    }

    /**
     * The standard bank-$06 floating handler is available only where its
     * standard sprite definition was loaded. Color Dungeon's $86 handler is
     * a separate bank-$36 path and remains intentionally deferred.
     */

    /** Port of bank-$03 EntityFallHandler's pre-impact falling branch. */

    /** Shared gameplay gates in ReturnIfNonInteractive_03 and _19. */

    /** Mirrors UpdateEntityTimers' shared dialog/inventory/ocarina early return. */

}
