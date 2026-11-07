package linksawakening.world;

import linksawakening.world.entity.motion.HeartPiecePickupMotion;
import linksawakening.world.entity.motion.RoamingEnemyMotion;
import linksawakening.world.entity.motion.WarpMotion;

import linksawakening.world.entity.event.BoomerangObjectRequest;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
import linksawakening.world.entity.event.HinoxLinkEffectRequest;
import linksawakening.world.entity.event.HookshotBridgeUpdate;
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
import linksawakening.world.entity.event.MagicPowderObjectRequest;
import linksawakening.world.entity.event.MagicRodObjectRequest;
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
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.event.WarpLinkStateRequest;
import linksawakening.world.entity.event.WitchExchangeEvent;
import linksawakening.world.entity.event.WitchRewardEvent;

import static linksawakening.world.EntitySlotValidation.*;

import static linksawakening.world.EntityMotionMath.*;

import static linksawakening.world.EntitySnapshots.*;

import static linksawakening.world.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteSelection;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;

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
    
    final EntitySpriteTileSnapshot spriteTiles;
    final boolean indoorRoom;
    private final RomTables romTables;
    final IntSupplier defaultRandomByteSupplier;
    private final RomRandomByteSource fallbackRomRandomByteSource;
    final EntitySpriteHandlerCatalog spriteHandlers;
    final RomEnemyCombatTables enemyCombatTables;
    final ChestContentsTable chestContentsTable;

    public boolean gelClingingToLink() { return gelClingingToLink; }

    final boolean[] bombFinalPresentationPending =
        new boolean[EntityRoomLoader.MAX_ENTITIES];

    void setFreeMovementMode(boolean enabled) { freeMovementMode = enabled; }

    final WarpMotion warpMotion;

    /** The ROM-facing state needed by Link's carry animation and throw input. */
    public record LiftedEntityState(int slot, int type, int phase,
                                   int transitionCountdown, int carryState,
                                   int sourceRomDirection, int effectiveRomDirection) {
        static LiftedEntityState none() {
            return new LiftedEntityState(-1, 0xFF, 0, 0, 0, 0, 0);
        }

        public boolean active() {
            return slot >= 0;
        }
    }

    /** Object-side effect emitted by MagicPowderSprinkleEntityHandler. */


    private RoomEntityRuntime(RoomEntitySnapshot initial, boolean indoorRoom,
                               IntSupplier defaultRandomByteSupplier,
                               EntitySpriteHandlerCatalog spriteHandlers,
                               RomEnemyCombatTables enemyCombatTables,
                               ChestContentsTable chestContentsTable,
                               RomTables romTables,
                               byte[] romData) {
        this.slots = initial.slots().toArray(RoomEntity[]::new);
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
    List<EntityProjectileEvent> tick(EntityFrameContext context) {
        validateByte(context.linkSpeedX(), "Link X speed");
        validateByte(context.linkSpeedY(), "Link Y speed");
        currentLinkSpeedX = context.linkSpeedX();
        currentLinkSpeedY = context.linkSpeedY();
        try {
            return advanceFrame(context);
        } finally {
            currentLinkSpeedX = 0;
            currentLinkSpeedY = 0;
        }
    }

    private List<EntityProjectileEvent> advanceFrame(EntityFrameContext context) { return new EntityFrameExecution(this, context).advance(); }

    List<RoamingEnemyMotion.LaunchRequest> projectileLaunchRequests() { return this.roomInspection.projectileLaunchRequests(); }

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
    boolean beginLift(int slot, int romDirection) { return this.playerCommands.beginLift(slot, romDirection); }

    /** Hands the fully held entity to func_014_53A3's generic throw path. */
    boolean throwLiftedEntity(int romDirection) { return this.playerCommands.throwLiftedEntity(romDirection); }

    LiftedEntityState liftedEntityState() { return this.roomInspection.liftedEntityState(); }

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
    void setBoomerangSoundCounterForFrame(int counter) { this.entityRuntimeConfiguration.setBoomerangSoundCounterForFrame(counter); }

    /** Advances entity $01 through the ROM boomerang handler's two states. */

    /** Advances entity {@code $DF} through the ROM sword-beam handler. */

    /** Advances entity {@code $04} through MagicRodFireballEntityHandler. */

    /** Advances entity {@code $08} through MagicPowderSprinkleEntityHandler. */

    void setTorchPaletteTransitionState(int targetAddress, boolean active) { this.entityRuntimeConfiguration.setTorchPaletteTransitionState(targetAddress, active); }

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
    int spawnHookshotChain(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) { return this.entityPlayerSpawns.spawnHookshotChain(linkEntityX, linkEntityY, linkEntityZ, romDirection); }

    /** Spawns bank 3's temporary entity $06 at an indoor object-cell center. */
    int spawnPushedBlock(int x, int y) { return this.entityPlayerSpawns.spawnPushedBlock(x, y); }

    /** Spawns bank 0's temporary type-$05 entity already entering lifted status. */
    int spawnLiftedRoomObject(int x, int y, int romDirection, int spriteVariant) { return this.entityPlayerSpawns.spawnLiftedRoomObject(x, y, romDirection, spriteVariant); }

    void movePushedBlock(int slot, int x, int y) { this.playerCommands.movePushedBlock(slot, x, y); }

    /** $03:5269: after block movement, restore Link even during a jump. */
    void resolvePushedBlockLinkContact(int slot, int linkEntityX, int linkEntityY) { this.backgroundMechanics.resolvePushedBlockLinkContact(slot, linkEntityX, linkEntityY); }

    void removePushedBlock(int slot) { this.playerCommands.removePushedBlock(slot); }

    /** Spawns DropKeyEffectHandler's ordinary dungeon key at ($28,$3C,$70). */
    int spawnRoomEventKeyDrop() { return this.entityPlayerSpawns.spawnRoomEventKeyDrop(); }

    /** Creates the ROM's ordinary player-arrow entity type {@code $00}. */
    int spawnArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) { return this.entityPlayerSpawns.spawnArrow(linkEntityX, linkEntityY, linkEntityZ, romDirection); }

    int spawnArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) { return this.entityPlayerSpawns.spawnArrow(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower); }

    /** Creates the ROM's player-boomerang entity type {@code $01}. */
    int spawnBoomerang(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, int pressedButtonsMask) { return this.entityPlayerSpawns.spawnBoomerang(linkEntityX, linkEntityY, linkEntityZ, romDirection, pressedButtonsMask); }

    /** Creates the ROM's level-two sword-beam entity type {@code $DF}. */
    int spawnSwordBeam(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) { return this.entityPlayerSpawns.spawnSwordBeam(linkEntityX, linkEntityY, linkEntityZ, romDirection); }

    /** Creates the ROM's Magic Rod fireball entity type {@code $04}. */
    int spawnMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) { return this.entityPlayerSpawns.spawnMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection); }

    int spawnMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) { return this.entityPlayerSpawns.spawnMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower); }

    /** Creates the ROM's type-$08 Magic Powder sprinkle entity. */
    int spawnMagicPowderSprinkle(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) { return this.entityPlayerSpawns.spawnMagicPowderSprinkle(linkEntityX, linkEntityY, linkEntityZ, romDirection); }

    int activePlayerArrowCount() { return this.roomInspection.activePlayerArrowCount(); }

    /** Mirrors the player projectile handlers that feed wActiveProjectileCount. */
    int activeProjectileCount() { return this.roomInspection.activeProjectileCount(); }

    boolean boomerangActive() { return this.roomInspection.boomerangActive(); }

    int boomerangSpeedX(int slot) { return this.roomInspection.boomerangSpeedX(slot); }

    int boomerangSpeedY(int slot) { return this.roomInspection.boomerangSpeedY(slot); }

    int boomerangTransitionCountdown(int slot) { return this.roomInspection.boomerangTransitionCountdown(slot); }

    int boomerangState(int slot) { return this.roomInspection.boomerangState(slot); }

    int swordBeamState(int slot) { return this.roomInspection.swordBeamState(slot); }

    int magicRodFireballPrivateCountdown1(int slot) { return this.roomInspection.magicRodFireballPrivateCountdown1(slot); }

    int magicPowderTransitionCountdown(int slot) { return this.roomInspection.magicPowderTransitionCountdown(slot); }

    int magicPowderState(int slot) { return this.roomInspection.magicPowderState(slot); }

    int magicPowderSlowTransitionCountdown(int slot) { return this.roomInspection.magicPowderSlowTransitionCountdown(slot); }

    void setMagicPowderTransitionCountdownForTest(int slot, int value) { this.entityDebugConfiguration.setMagicPowderTransitionCountdownForTest(slot, value); }

    void setMagicPowderSlowTransitionCountdownForTest(int slot, int value) { this.entityDebugConfiguration.setMagicPowderSlowTransitionCountdownForTest(slot, value); }

    int swordBeamSpeedX(int slot) { return this.roomInspection.swordBeamSpeedX(slot); }

    int swordBeamSpeedY(int slot) { return this.roomInspection.swordBeamSpeedY(slot); }

    int playerArrowDirection(int slot) { return this.roomInspection.playerArrowDirection(slot); }

    int playerArrowSpeedX(int slot) { return this.roomInspection.playerArrowSpeedX(slot); }

    int playerArrowSpeedY(int slot) { return this.roomInspection.playerArrowSpeedY(slot); }

    int playerArrowTransitionCountdown(int slot) { return this.roomInspection.playerArrowTransitionCountdown(slot); }

    int bombArrowCooldownForTest() { return this.roomInspection.bombArrowCooldownForTest(); }

    boolean isBombArrowForTest(int slot) { return this.roomInspection.isBombArrowForTest(slot); }

    boolean lastArrowShotPlayedWhoosh() { return this.roomInspection.lastArrowShotPlayedWhoosh(); }

    boolean lastBombPlacementPlayedBump() { return this.roomInspection.lastBombPlacementPlayedBump(); }

    /** Creates the ROM's ordinary player bomb entity type {@code $02}. */
    int spawnBomb(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) { return this.entityPlayerSpawns.spawnBomb(linkEntityX, linkEntityY, linkEntityZ, romDirection); }

    /** Creates the source's type-$02 enemy bomb with privateState4 set to $01. */
    int spawnEnemyBomb(int entityX, int entityY, int entityZ, int transitionCountdown) { return this.entityEnemySpawns.spawnEnemyBomb(entityX, entityY, entityZ, transitionCountdown); }

    /** Creates a Bomber-spawned type-$02 enemy bomb with its source speeds. */
    int spawnEnemyBomb(int entityX, int entityY, int entityZ, int transitionCountdown, int speedX, int speedY, int speedZ) { return this.entityEnemySpawns.spawnEnemyBomb(entityX, entityY, entityZ, transitionCountdown, speedX, speedY, speedZ); }

    int spawnEnemyBomb(int entityX, int entityY, int entityZ, int transitionCountdown, int speedX, int speedY, int speedZ, boolean initializeMotion, int privateState4) { return this.entityEnemySpawns.spawnEnemyBomb(entityX, entityY, entityZ, transitionCountdown, speedX, speedY, speedZ, initializeMotion, privateState4); }

    /** Mirrors the state-$00 return gate reached after Genie's BossIntro call. */

    /** Mirrors UpdateEntityTimers' gameplay and presentation gates. */

    /** Creates the temporary type-$05 entity used by bombed bushes, grass, and pots. */
    int spawnLiftableRockSmash(int entityX, int entityY, int sourceSpriteVariant) { return this.entityPlayerSpawns.spawnLiftableRockSmash(entityX, entityY, sourceSpriteVariant); }

    /** Creates one of the four silent type-$05 rubble entities from bank-$36. */
    int spawnLiftableRockRubble(int entityX, int entityY) { return this.entityPlayerSpawns.spawnLiftableRockRubble(entityX, entityY); }

    boolean bombActive() { return this.roomInspection.bombActive(); }

    int bombDirection(int slot) { return this.roomInspection.bombDirection(slot); }

    int bombPrivateCountdown1ForTest(int slot) { return this.roomInspection.bombPrivateCountdown1ForTest(slot); }

    void setBombTransitionCountdownForTest(int slot, int value) { this.entityDebugConfiguration.setBombTransitionCountdownForTest(slot, value); }

    boolean hookshotActive() { return this.roomInspection.hookshotActive(); }

    int hookshotSlot() { return this.roomInspection.hookshotSlot(); }

    int hookshotTransitionCountdown(int slot) { return this.roomInspection.hookshotTransitionCountdown(slot); }

    int bowWowTargetSlot(int slot) { return this.roomInspection.bowWowTargetSlot(slot); }

    int bowWowPrivateCountdown2(int slot) { return this.roomInspection.bowWowPrivateCountdown2(slot); }

    int bowWowTransitionCountdown(int slot) { return this.roomInspection.bowWowTransitionCountdown(slot); }

    void setBowWowTransitionCountdownForTest(int slot, int value) { this.entityDebugConfiguration.setBowWowTransitionCountdownForTest(slot, value); }

    int entityInertia(int slot) { return this.roomInspection.entityInertia(slot); }

    void setEntityInertiaForTest(int slot, int value) { this.entityDebugConfiguration.setEntityInertiaForTest(slot, value); }

    int pokeyPrivateState1(int slot) { return this.roomInspection.pokeyPrivateState1(slot); }

    int pokeyTransitionCountdown(int slot) { return this.roomInspection.pokeyTransitionCountdown(slot); }

    int hookshotEntityState(int slot) { return this.roomInspection.hookshotEntityState(slot); }

    boolean hookshotWallCollisionPending(int slot) { return this.roomInspection.hookshotWallCollisionPending(slot); }

    public RoomEntitySnapshot snapshot() { return this.roomInspection.snapshot(); }

    /** Applies the position and variant writes made by MarinEntityHandler_Indoor. */
    public boolean applyMarinWakeUpPresentation(int x, int y, int spriteVariant) { return this.playerCommands.applyMarinWakeUpPresentation(x, y, spriteVariant); }

    void updateHouseTarinPresentation(int linkX, int linkY) { this.playerCommands.updateHouseTarinPresentation(linkX, linkY); }

    void setLinkVelocityZ(int velocity) { this.entityRuntimeConfiguration.setLinkVelocityZ(velocity); }

    List<WingedOctorokOam.Entry> wingedOctorokOam() { return this.roomInspection.wingedOctorokOam(); }

    int wingedOctorokState(int slot) { return this.roomInspection.wingedOctorokState(slot); }

    int wingedOctorokSpeedX(int slot) { return this.roomInspection.wingedOctorokSpeedX(slot); }

    int bushCrawlerState(int slot) { return this.roomInspection.bushCrawlerState(slot); }

    int bushCrawlerSpeedY(int slot) { return this.roomInspection.bushCrawlerSpeedY(slot); }

    void setSpriteSelection(EntitySpriteSelection selection) { this.entityRuntimeConfiguration.setSpriteSelection(selection); }

    void setGroundInteraction(RoomEntityGroundInteraction groundInteraction) { this.entityRuntimeConfiguration.setGroundInteraction(groundInteraction); }

    void setBackgroundInteraction(RoomEntityBackgroundInteraction backgroundInteraction) { this.entityRuntimeConfiguration.setBackgroundInteraction(backgroundInteraction); }

    void setObjectQuery(RoomEntityObjectQuery objectQuery) { this.entityRuntimeConfiguration.setObjectQuery(objectQuery); }

    void setObjectIntersectionQuery(RoomEntityObjectQuery objectIntersectionQuery) { this.entityRuntimeConfiguration.setObjectIntersectionQuery(objectIntersectionQuery); }

    int groundStatus(int slot) { return this.roomInspection.groundStatus(slot); }

    void setActionButtonsHeld(boolean actionButtonsHeld) { this.entityRuntimeConfiguration.setActionButtonsHeld(actionButtonsHeld); }

    void setActionButtonsHeld(boolean actionButtonAHeld, boolean actionButtonBHeld) { this.entityRuntimeConfiguration.setActionButtonsHeld(actionButtonAHeld, actionButtonBHeld); }

    void setJoypadHeld(boolean joypadHeld) { this.entityRuntimeConfiguration.setJoypadHeld(joypadHeld); }

    void setPressedButtonsMask(int pressedButtonsMask) { this.entityRuntimeConfiguration.setPressedButtonsMask(pressedButtonsMask); }

    /** Supplies wDialogState's nonzero gate to handlers with dialog branches. */
    void setDialogActive(boolean dialogActive) { this.entityRuntimeConfiguration.setDialogActive(dialogActive); }

    void setTalkState(boolean inventoryAppearing, int dialogCooldown, int windowY) { this.entityRuntimeConfiguration.setTalkState(inventoryAppearing, dialogCooldown, windowY); }

    void setDialogAskSelectionIndex(int selectionIndex) { this.entityRuntimeConfiguration.setDialogAskSelectionIndex(selectionIndex); }

    /** Supplies the nonzero wActiveMusicIndex gate used by instrument pickup. */
    void setActiveMusic(boolean activeMusic) { this.entityRuntimeConfiguration.setActiveMusic(activeMusic); }

    void setBowWowState(int bowWowState) { this.entityRuntimeConfiguration.setBowWowState(bowWowState); }

    int bowWowState() { return this.roomInspection.bowWowState(); }

    int moblinKingState(int slot) { return this.roomInspection.moblinKingState(slot); }

    void setMoblinKingStateForTest(int slot, int state, int direction, int speedX, int speedY, int speedZ, int transitionCountdown) { this.entityDebugConfiguration.setMoblinKingStateForTest(slot, state, direction, speedX, speedY, speedZ, transitionCountdown); }

    void setEntityPrivateState4ForTest(int slot, int value) { this.entityDebugConfiguration.setEntityPrivateState4ForTest(slot, value); }

    void setLikeLikeLinkInventory(int itemA, int itemB) { this.entityRuntimeConfiguration.setLikeLikeLinkInventory(itemA, itemB); }

    void setLikeLikeLinkInventoryForTest(int itemA, int itemB) { this.entityDebugConfiguration.setLikeLikeLinkInventoryForTest(itemA, itemB); }

    void setPowerBraceletButtonHeld(boolean powerBraceletButtonHeld) { this.entityRuntimeConfiguration.setPowerBraceletButtonHeld(powerBraceletButtonHeld); }

    /** Supplies the source-prioritized held A/B button for an equipped bomb. */
    void setBombButtonHeld(boolean bombButtonHeld) { this.entityRuntimeConfiguration.setBombButtonHeld(bombButtonHeld); }

    void setLinkAttackStepAnimationCountdown(int countdown) { this.entityRuntimeConfiguration.setLinkAttackStepAnimationCountdown(countdown); }

    /** Supplies the captured hLinkFinalPositionX visible to entity handlers. */
    void setLinkFinalPositionX(int positionX) { this.entityRuntimeConfiguration.setLinkFinalPositionX(positionX); }

    boolean shouldGetLostInMysteriousWoods() { return this.roomInspection.shouldGetLostInMysteriousWoods(); }

    void setLiftedLinkC13B(int linkC13B) { this.entityRuntimeConfiguration.setLiftedLinkC13B(linkC13B); }

    void setGroundInteractionSideScrolling(boolean sideScrolling) { this.entityRuntimeConfiguration.setGroundInteractionSideScrolling(sideScrolling); }

    void setEnemyDropResolver(EnemyDropResolver resolver) { this.entityRuntimeConfiguration.setEnemyDropResolver(resolver); }

    void setEnemyDropCounters(EnemyDropResolver.CounterState counters) { this.entityRuntimeConfiguration.setEnemyDropCounters(counters); }

    void setEnemyDropPlayerState(int maxHearts, int health, boolean activePowerUp) { this.entityRuntimeConfiguration.setEnemyDropPlayerState(maxHearts, health, activePowerUp); }

    void setEnemyDropBossBattle(boolean bossBattle) { this.entityRuntimeConfiguration.setEnemyDropBossBattle(bossBattle); }

    void setDroppedItemForTest(int slot, int itemType) { this.entityDebugConfiguration.setDroppedItemForTest(slot, itemType); }

    void setEntityMapId(int mapId) { this.entityRuntimeConfiguration.setEntityMapId(mapId); }

    void setEntityMapIdForTest(int mapId) { this.entityDebugConfiguration.setEntityMapIdForTest(mapId); }

    void setEntityRoomId(int roomId) { this.entityRuntimeConfiguration.setEntityRoomId(roomId); }

    void setEntityRoomIdForTest(int roomId) { this.entityDebugConfiguration.setEntityRoomIdForTest(roomId); }

    void setEntityRoomStatus(int roomStatus) { this.entityRuntimeConfiguration.setEntityRoomStatus(roomStatus); }

    /** Supplies the current map's wHasInstrument1 bit to entity-$61. */
    void setDungeonMinibossReady(boolean ready) { this.entityRuntimeConfiguration.setDungeonMinibossReady(ready); }

    void setEntityRoomStatusForTest(int roomStatus) { this.entityDebugConfiguration.setEntityRoomStatusForTest(roomStatus); }

    void setSecretSeashellPegasusCollisionForTest(boolean screenShakeActive, boolean collisionActive, int collisionX, int collisionY) { this.entityDebugConfiguration.setSecretSeashellPegasusCollisionForTest(screenShakeActive, collisionActive, collisionX, collisionY); }

    void setDroppablePegasusCollisionForTest(boolean screenShakeActive, boolean collisionActive, int collisionX, int collisionY) { this.entityDebugConfiguration.setDroppablePegasusCollisionForTest(screenShakeActive, collisionActive, collisionX, collisionY); }

    void setDroppableRevealedForTest(int slot, int slowCountdown) { this.entityDebugConfiguration.setDroppableRevealedForTest(slot, slowCountdown); }

    void setSecretSeashellPegasusCollisionState(boolean screenShakeActive, boolean collisionActive, int collisionX, int collisionY) { this.entityRuntimeConfiguration.setSecretSeashellPegasusCollisionState(screenShakeActive, collisionActive, collisionX, collisionY); }

    /** Supplies the three player upgrade bytes read by ChestWithItemEntityHandler. */
    void setChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel) { this.entityRuntimeConfiguration.setChestPlayerLevels(shieldLevel, swordLevel, powerBraceletLevel); }

    void setToadstoolPlayerState(boolean hasToadstool, int magicPowderCount) { this.entityRuntimeConfiguration.setToadstoolPlayerState(hasToadstool, magicPowderCount); }

    /** Supplies wHasTailKey to OwlEventEntityHandler. */
    void setTailKeyOwned(boolean owned) { this.entityRuntimeConfiguration.setTailKeyOwned(owned); }

    int swordPickupStateForTest(int slot) { return this.roomInspection.swordPickupStateForTest(slot); }

    int slowTransitionCountdownForTest(int slot) { return this.roomInspection.slowTransitionCountdownForTest(slot); }

    void setSlowTransitionCountdownForTest(int slot, int countdown) { this.entityDebugConfiguration.setSlowTransitionCountdownForTest(slot, countdown); }

    void setTransitionCountdownForTest(int slot, int countdown) { this.entityDebugConfiguration.setTransitionCountdownForTest(slot, countdown); }

    void setThreeOfAKindSettledForTest(int slot, int face) { this.entityDebugConfiguration.setThreeOfAKindSettledForTest(slot, face); }

    int threeOfAKindStateForTest(int slot) { return this.roomInspection.threeOfAKindStateForTest(slot); }

    int transitionCountdownForTest(int slot) { return this.roomInspection.transitionCountdownForTest(slot); }

    int hinoxStateForTest(int slot) { return this.roomInspection.hinoxStateForTest(slot); }

    void setHinoxStateForTest(int slot, int state) { this.entityDebugConfiguration.setHinoxStateForTest(slot, state); }

    void setHinoxSpeedForTest(int slot, int speedX, int speedY) { this.entityDebugConfiguration.setHinoxSpeedForTest(slot, speedX, speedY); }

    int enemyHealthForTest(int slot) { return this.roomInspection.enemyHealthForTest(slot); }

    void setTarinRaccoonStateForTest(int slot, int state, int speedX, int speedY, int speedZ, int privateState2, int privateState3, boolean nearLinkLatch) { this.entityDebugConfiguration.setTarinRaccoonStateForTest(slot, state, speedX, speedY, speedZ, privateState2, privateState3, nearLinkLatch); }

    int tarinRaccoonStateForTest(int slot) { return this.roomInspection.tarinRaccoonStateForTest(slot); }

    int bombPrivateState4ForTest(int slot) { return this.roomInspection.bombPrivateState4ForTest(slot); }

    int entityRoomStatusForTest() { return this.roomInspection.entityRoomStatusForTest(); }

    int warpTransitionCountdownForTest(int slot) { return this.roomInspection.warpTransitionCountdownForTest(slot); }

    int owlEventStateForTest(int slot) { return this.roomInspection.owlEventStateForTest(slot); }

    int owlPrivateCountdown1ForTest(int slot) { return this.roomInspection.owlPrivateCountdown1ForTest(slot); }

    void setTransitionSequenceCounterForTest(int counter) { this.entityDebugConfiguration.setTransitionSequenceCounterForTest(counter); }

    void setGameplayWorldForTest(boolean gameplayWorld) { this.entityDebugConfiguration.setGameplayWorldForTest(gameplayWorld); }

    void setRoomTransitionStateForTest(boolean active) { this.entityDebugConfiguration.setRoomTransitionStateForTest(active); }

    void setWitchGotItemPresentationActiveForTest(boolean active) { this.entityDebugConfiguration.setWitchGotItemPresentationActiveForTest(active); }

    void setSideViewPotSpeedForTest(int slot, int speedX, int speedY) { this.entityDebugConfiguration.setSideViewPotSpeedForTest(slot, speedX, speedY); }

    void setFollowingNpcState(FollowingNpcState state) { this.entityRuntimeConfiguration.setFollowingNpcState(state); }

    void setColorShellWorld(ColorShellWorld world) { this.entityRuntimeConfiguration.setColorShellWorld(world); }

    void setSwitchBlockAnimationActive(boolean active) { this.entityRuntimeConfiguration.setSwitchBlockAnimationActive(active); }

    void setSwitchBlockAnimationActiveForTest(boolean active) { this.entityDebugConfiguration.setSwitchBlockAnimationActiveForTest(active); }

    boolean consumePendingSwitchBlockAnimationRequest() { return this.effects.consumePendingSwitchBlockAnimationRequest(); }

    int consumePendingClearedEntityMask() { return this.effects.consumePendingClearedEntityMask(); }

    int consumePendingRoomStatusMask() { return this.effects.consumePendingRoomStatusMask(); }

    List<EntityCombatEvent> consumePendingEntityEvents() { return this.effects.consumePendingEntityEvents(); }

    List<ThrownBackgroundCollisionEvent> consumePendingThrownBackgroundCollisions() { return this.effects.consumePendingThrownBackgroundCollisions(); }

    List<DungeonWarpRequest> consumePendingDungeonWarpRequests() { return this.effects.consumePendingDungeonWarpRequests(); }

    List<WarpLinkStateRequest> consumePendingWarpLinkStateRequests() { return this.effects.consumePendingWarpLinkStateRequests(); }

    List<LinkFinalPositionRequest> consumePendingLinkFinalPositionRequests() { return this.effects.consumePendingLinkFinalPositionRequests(); }

    List<RoosterLinkStateRequest> consumePendingRoosterLinkStateRequests() { return this.effects.consumePendingRoosterLinkStateRequests(); }

    List<LinkMotionBlockRequest> consumePendingLinkMotionBlockRequests() { return this.effects.consumePendingLinkMotionBlockRequests(); }

    List<SideViewPlatformLinkRequest> consumePendingSideViewPlatformLinkRequests() { return this.effects.consumePendingSideViewPlatformLinkRequests(); }

    List<SideViewPotLinkRequest> consumePendingSideViewPotLinkRequests() { return this.effects.consumePendingSideViewPotLinkRequests(); }

    int sideViewPlatformSpeedYForTest(int slot) { return this.roomInspection.sideViewPlatformSpeedYForTest(slot); }

    int sideViewPlatformSpeedXForTest(int slot) { return this.roomInspection.sideViewPlatformSpeedXForTest(slot); }

    void setSideViewPlatformSpeedYForTest(int slot, int speed) { this.entityDebugConfiguration.setSideViewPlatformSpeedYForTest(slot, speed); }

    void setSideViewPlatformSpeedXForTest(int slot, int speed) { this.entityDebugConfiguration.setSideViewPlatformSpeedXForTest(slot, speed); }

    int sideViewPlatformPrivateState2ForTest(int slot) { return this.roomInspection.sideViewPlatformPrivateState2ForTest(slot); }

    int sideViewPlatformPrivateState4ForTest(int slot) { return this.roomInspection.sideViewPlatformPrivateState4ForTest(slot); }

    List<HinoxLinkEffectRequest> consumePendingHinoxLinkEffectRequests() { return this.effects.consumePendingHinoxLinkEffectRequests(); }

    List<LinkFallenPoseRequest> consumePendingLinkFallenPoseRequests() { return this.effects.consumePendingLinkFallenPoseRequests(); }

    List<LinkFacingRequest> consumePendingLinkFacingRequests() { return this.effects.consumePendingLinkFacingRequests(); }

    List<LinkAttackClearRequest> consumePendingLinkAttackClearRequests() { return this.effects.consumePendingLinkAttackClearRequests(); }

    List<LinkAttackStepRequest> consumePendingLinkAttackStepRequests() { return this.effects.consumePendingLinkAttackStepRequests(); }

    List<RoomStatusPersistenceRequest> consumePendingRoomStatusPersistenceRequests() { return this.effects.consumePendingRoomStatusPersistenceRequests(); }

    List<LinkHeldItemPoseRequest> consumePendingLinkHeldItemPoseRequests() { return this.effects.consumePendingLinkHeldItemPoseRequests(); }

    boolean witchGotItemPresentationActive() { return this.roomInspection.witchGotItemPresentationActive(); }

    boolean powerUpPickupSequenceActive() { return this.roomInspection.powerUpPickupSequenceActive(); }

    boolean swordPickupSequenceActive() { return this.roomInspection.swordPickupSequenceActive(); }

    List<LinkSwordSpinPoseRequest> consumePendingLinkSwordSpinPoseRequests() { return this.effects.consumePendingLinkSwordSpinPoseRequests(); }

    List<LinkSwordFinalPoseRequest> consumePendingLinkSwordFinalPoseRequests() { return this.effects.consumePendingLinkSwordFinalPoseRequests(); }

    List<ScreenShakeRequest> consumePendingScreenShakeRequests() { return this.effects.consumePendingScreenShakeRequests(); }

    List<ChestRewardEvent> consumePendingChestRewardEvents() { return this.effects.consumePendingChestRewardEvents(); }

    List<KeyRewardEvent> consumePendingKeyRewardEvents() { return this.effects.consumePendingKeyRewardEvents(); }

    List<SlimeKeyRewardEvent> consumePendingSlimeKeyRewardEvents() { return this.effects.consumePendingSlimeKeyRewardEvents(); }

    List<KeyQuicksandEvent> consumePendingKeyQuicksandEvents() { return this.effects.consumePendingKeyQuicksandEvents(); }

    int consumePendingMusicTrack() { return this.effects.consumePendingMusicTrack(); }

    int consumePendingMusicFadeOutCountdown() { return this.effects.consumePendingMusicFadeOutCountdown(); }

    List<LikeLikeEvent> consumePendingLikeLikeEvents() { return this.effects.consumePendingLikeLikeEvents(); }

    int likeLikeState(int slot) { return this.roomInspection.likeLikeState(slot); }

    int likeLikePrivateState1(int slot) { return this.roomInspection.likeLikePrivateState1(slot); }

    int likeLikeInertia(int slot) { return this.roomInspection.likeLikeInertia(slot); }

    int likeLikeWalkState(int slot) { return this.roomInspection.likeLikeWalkState(slot); }

    int likeLikeWalkSpeedY(int slot) { return this.roomInspection.likeLikeWalkSpeedY(slot); }

    List<DialogRequest> consumePendingDialogRequests() { return this.effects.consumePendingDialogRequests(); }

    List<BombExplosionEvent> consumeBombExplosionEvents() { return this.effects.consumeBombExplosionEvents(); }

    List<HookshotBridgeUpdate> hookshotBridgeUpdates() { return this.roomInspection.hookshotBridgeUpdates(); }

    /** Mirrors bank-$03 ApplyLinkCollisionWithEnemy's Goomba stomp branch. */

    /** Ports {@code RollingBonesEntityHandler}'s long miniboss destruction sequence. */

    /** Bank $15:7C37..7CA5: two death waits, then DidKillEnemy. */

    void startSwordPickup(int slot) { this.playerCommands.startSwordPickup(slot); }

    void startToadstoolPickup(int slot) { this.playerCommands.startToadstoolPickup(slot); }

    void setOwlStatueDialogId(int globalDialogId) { this.entityRuntimeConfiguration.setOwlStatueDialogId(globalDialogId); }

    public boolean heartPiecePickupActive() { return this.roomInspection.heartPiecePickupActive(); }

    public HeartPieceVisual heartPieceVisual() { return this.roomInspection.heartPieceVisual(); }

    public void setHeartPiecesCount(int count) { heartPiecesCount = count; }

    public List<HeartPiecePickupMotion.Update> consumeHeartPieceUpdates() { return this.effects.consumeHeartPieceUpdates(); }

    public String instrumentTrace() { return this.roomInspection.instrumentTrace(); }

    public String moblinKingTrace() { return this.roomInspection.moblinKingTrace(); }

    public InstrumentWarpVisual instrumentWarpVisual() { return this.roomInspection.instrumentWarpVisual(); }

    void presentWitchGotItem(int slot) { this.playerCommands.presentWitchGotItem(slot); }

    /** ProcessPowerUp ($03:6400) and ApplyGotItem ($00:1033). */

    void holdSwordAboveLink(RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ, boolean finalPose) { this.playerCommands.holdSwordAboveLink(entity, linkEntityX, linkEntityY, linkZ, finalPose); }

    void setOwlDialogResolver(IntUnaryOperator resolver) { this.entityRuntimeConfiguration.setOwlDialogResolver(resolver); }

    void setOwlInstrumentFlags(int instrument1Flags, int instrument2Flags) { this.entityRuntimeConfiguration.setOwlInstrumentFlags(instrument1Flags, instrument2Flags); }

    void setOwlDefaultMusicResolver(IntUnaryOperator resolver) { this.entityRuntimeConfiguration.setOwlDefaultMusicResolver(resolver); }

    void blockLinkForOwl(int slot) {
        pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
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
    int spawnShovelDrop(int objectLeft, int objectTop, int linkEntityX, int linkEntityY, boolean allowDrop) { return this.entityPlayerSpawns.spawnShovelDrop(objectLeft, objectTop, linkEntityX, linkEntityY, allowDrop); }

    /**
     * Mirrors SpawnChestWithItem. The arguments are the intersected object's
     * unaligned left/top coordinates; the source masks them to a room cell
     * and places the entity at (+8,+16).
     */
    int spawnChestWithItem(int objectLeft, int objectTop, int itemType) { return this.entityPlayerSpawns.spawnChestWithItem(objectLeft, objectTop, itemType); }

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

    int slowTransitionCountdown(int slot) { return this.roomInspection.slowTransitionCountdown(slot); }

    int droppedItemForTest(int slot) { return this.roomInspection.droppedItemForTest(slot); }

    int killCount() { return this.roomInspection.killCount(); }

    int killOrderAt(int index) { return this.roomInspection.killOrderAt(index); }

    EnemyDropResolver.CounterState enemyDropCounters() { return this.roomInspection.enemyDropCounters(); }

    int dropPrivateCountdown1(int slot) { return this.roomInspection.dropPrivateCountdown1(slot); }

    int dropPrivateCountdown3(int slot) { return this.roomInspection.dropPrivateCountdown3(slot); }

    int droppablePrivateState3(int slot) { return this.roomInspection.droppablePrivateState3(slot); }

    int droppablePrivateState4(int slot) { return this.roomInspection.droppablePrivateState4(slot); }

    int dropSpeedY(int slot) { return this.roomInspection.dropSpeedY(slot); }

    int dropSpeedX(int slot) { return this.roomInspection.dropSpeedX(slot); }

    int dropSpeedZ(int slot) { return this.roomInspection.dropSpeedZ(slot); }

    int fairySpeedX(int slot) { return this.roomInspection.fairySpeedX(slot); }

    int fairySpeedY(int slot) { return this.roomInspection.fairySpeedY(slot); }

    int keeseState(int slot) { return this.flyingEnemyInspection.keeseState(slot); }

    int keeseTransitionCountdown(int slot) { return this.flyingEnemyInspection.keeseTransitionCountdown(slot); }

    int keeseAngle(int slot) { return this.flyingEnemyInspection.keeseAngle(slot); }

    int keeseSpeedX(int slot) { return this.flyingEnemyInspection.keeseSpeedX(slot); }

    int keeseSpeedY(int slot) { return this.flyingEnemyInspection.keeseSpeedY(slot); }

    int octorokState(int slot) { return this.enemyInspection.octorokState(slot); }

    int octorokTransitionCountdown(int slot) { return this.enemyInspection.octorokTransitionCountdown(slot); }

    int octorokDirection(int slot) { return this.enemyInspection.octorokDirection(slot); }

    int octorokSpeedY(int slot) { return this.enemyInspection.octorokSpeedY(slot); }

    int ironMaskState(int slot) { return this.enemyInspection.ironMaskState(slot); }

    int ironMaskTransitionCountdown(int slot) { return this.enemyInspection.ironMaskTransitionCountdown(slot); }

    int ironMaskDirection(int slot) { return this.enemyInspection.ironMaskDirection(slot); }

    int ironMaskSpeedY(int slot) { return this.enemyInspection.ironMaskSpeedY(slot); }

    int ironMaskPrivateState2(int slot) { return this.enemyInspection.ironMaskPrivateState2(slot); }

    int moblinSwordState(int slot) { return this.enemyInspection.moblinSwordState(slot); }

    int moblinSwordTransitionCountdown(int slot) { return this.enemyInspection.moblinSwordTransitionCountdown(slot); }

    int moblinSwordPrivateCountdown1(int slot) { return this.enemyInspection.moblinSwordPrivateCountdown1(slot); }

    int moblinSwordPrivateState3(int slot) { return this.enemyInspection.moblinSwordPrivateState3(slot); }

    int moblinSwordDirection(int slot) { return this.enemyInspection.moblinSwordDirection(slot); }

    int moblinSwordSpeedX(int slot) { return this.enemyInspection.moblinSwordSpeedX(slot); }

    int swordMoblinAlertingSoundCounter() { return this.enemyInspection.swordMoblinAlertingSoundCounter(); }

    int tektiteState(int slot) { return this.enemyInspection.tektiteState(slot); }

    int tektiteTransitionCountdown(int slot) { return this.enemyInspection.tektiteTransitionCountdown(slot); }

    int tektiteSpeedX(int slot) { return this.enemyInspection.tektiteSpeedX(slot); }

    int tektiteSpeedY(int slot) { return this.enemyInspection.tektiteSpeedY(slot); }

    int pairoddState(int slot) { return this.flyingEnemyInspection.pairoddState(slot); }

    int pairoddTransitionCountdown(int slot) { return this.flyingEnemyInspection.pairoddTransitionCountdown(slot); }

    int pairoddDirection(int slot) { return this.flyingEnemyInspection.pairoddDirection(slot); }

    int pairoddProjectileSpeedX(int slot) { return this.flyingEnemyInspection.pairoddProjectileSpeedX(slot); }

    int pairoddProjectileSpeedY(int slot) { return this.flyingEnemyInspection.pairoddProjectileSpeedY(slot); }

    int pairoddProjectileDirection(int slot) { return this.flyingEnemyInspection.pairoddProjectileDirection(slot); }

    int enemyProjectileSpeedX(int slot) { return this.enemyInspection.enemyProjectileSpeedX(slot); }

    int enemyProjectileSpeedY(int slot) { return this.enemyInspection.enemyProjectileSpeedY(slot); }

    int enemyProjectileTransitionCountdown(int slot) { return this.enemyInspection.enemyProjectileTransitionCountdown(slot); }

    int laserDirection(int slot) { return this.enemyInspection.laserDirection(slot); }

    int laserParentTransitionCountdown(int slot) { return this.enemyInspection.laserParentTransitionCountdown(slot); }

    int laserSpeedX(int slot) { return this.enemyInspection.laserSpeedX(slot); }

    int laserSpeedY(int slot) { return this.enemyInspection.laserSpeedY(slot); }

    void setLaserParentForTest(int slot, int countdown, int newSpeedX, int newSpeedY) { this.entityDebugConfiguration.setLaserParentForTest(slot, countdown, newSpeedX, newSpeedY); }

    void setPairoddProjectileForTest(int slot, int newSpeedX, int newSpeedY, int direction) { this.entityDebugConfiguration.setPairoddProjectileForTest(slot, newSpeedX, newSpeedY, direction); }

    void setLaserBeamForTest(int slot, int newSpeedX, int newSpeedY, int direction) { this.entityDebugConfiguration.setLaserBeamForTest(slot, newSpeedX, newSpeedY, direction); }

    List<TransientVfxRequest> transientVfxRequests() { return this.enemyInspection.transientVfxRequests(); }

    List<HeartContainerRewardEvent> consumePendingHeartContainerRewards() { return this.effects.consumePendingHeartContainerRewards(); }

    List<MadamMeowMeowFullHealRequest> consumePendingMadamMeowMeowFullHealRequests() { return this.effects.consumePendingMadamMeowMeowFullHealRequests(); }

    List<SwordPickupRewardEvent> consumePendingSwordPickupRewards() { return this.effects.consumePendingSwordPickupRewards(); }

    List<ToadstoolRewardEvent> consumePendingToadstoolRewards() { return this.effects.consumePendingToadstoolRewards(); }

    List<InstrumentRewardEvent> consumePendingInstrumentRewards() { return this.effects.consumePendingInstrumentRewards(); }

    List<InstrumentCompletionEvent> consumePendingInstrumentCompletions() { return this.effects.consumePendingInstrumentCompletions(); }

    List<RichardProgressEvent> consumePendingRichardProgressEvents() { return this.effects.consumePendingRichardProgressEvents(); }

    List<WitchExchangeEvent> consumePendingWitchExchangeEvents() { return this.effects.consumePendingWitchExchangeEvents(); }

    List<WitchRewardEvent> consumePendingWitchRewardEvents() { return this.effects.consumePendingWitchRewardEvents(); }

    List<OwlEventCompletion> consumePendingOwlEventCompletions() { return this.effects.consumePendingOwlEventCompletions(); }

    List<BoomerangObjectRequest> boomerangObjectRequests() { return this.enemyInspection.boomerangObjectRequests(); }

    List<MagicRodObjectRequest> magicRodObjectRequests() { return this.enemyInspection.magicRodObjectRequests(); }

    List<MagicPowderObjectRequest> magicPowderObjectRequests() { return this.enemyInspection.magicPowderObjectRequests(); }

    int physicsFlags(int slot) { return this.enemyInspection.physicsFlags(slot); }

    int tektiteSpeedZ(int slot) { return this.enemyInspection.tektiteSpeedZ(slot); }

    int leeverState(int slot) { return this.enemyInspection.leeverState(slot); }

    int leeverTransitionCountdown(int slot) { return this.enemyInspection.leeverTransitionCountdown(slot); }

    int leeverSpeedX(int slot) { return this.enemyInspection.leeverSpeedX(slot); }

    int leeverSpeedY(int slot) { return this.enemyInspection.leeverSpeedY(slot); }

    int antiFairySpeedX(int slot) { return this.enemyInspection.antiFairySpeedX(slot); }

    int antiFairySpeedY(int slot) { return this.enemyInspection.antiFairySpeedY(slot); }

    int starSpeedX(int slot) { return this.enemyInspection.starSpeedX(slot); }

    int starSpeedY(int slot) { return this.enemyInspection.starSpeedY(slot); }

    int blooperState(int slot) { return this.enemyInspection.blooperState(slot); }

    int blooperTransitionCountdown(int slot) { return this.enemyInspection.blooperTransitionCountdown(slot); }

    int blooperDirection(int slot) { return this.enemyInspection.blooperDirection(slot); }

    int blooperSpeedX(int slot) { return this.enemyInspection.blooperSpeedX(slot); }

    int blooperSpeedY(int slot) { return this.enemyInspection.blooperSpeedY(slot); }

    int pincerState(int slot) { return this.enemyInspection.pincerState(slot); }

    int pincerSpeedX(int slot) { return this.enemyInspection.pincerSpeedX(slot); }

    int pincerSpeedY(int slot) { return this.enemyInspection.pincerSpeedY(slot); }

    List<PincerBodyOam.Entry> pincerBodyOam() { return this.enemyInspection.pincerBodyOam(); }

    int sparkPrivateState1(int slot) { return this.enemyInspection.sparkPrivateState1(slot); }

    int sparkPrivateState2(int slot) { return this.enemyInspection.sparkPrivateState2(slot); }

    int sparkTransitionCountdown(int slot) { return this.enemyInspection.sparkTransitionCountdown(slot); }

    int sparkSpeedX(int slot) { return this.enemyInspection.sparkSpeedX(slot); }

    int sparkSpeedY(int slot) { return this.enemyInspection.sparkSpeedY(slot); }

    int zolState(int slot) { return this.enemyInspection.zolState(slot); }

    int zolTransitionCountdown(int slot) { return this.enemyInspection.zolTransitionCountdown(slot); }

    int zolSpeedX(int slot) { return this.enemyInspection.zolSpeedX(slot); }

    int zolSpeedY(int slot) { return this.enemyInspection.zolSpeedY(slot); }

    int zolSpeedZ(int slot) { return this.enemyInspection.zolSpeedZ(slot); }

    int zolPrivateCountdown1(int slot) { return this.enemyInspection.zolPrivateCountdown1(slot); }

    int hidingZolState(int slot) { return this.enemyInspection.hidingZolState(slot); }

    int hidingZolTransitionCountdown(int slot) { return this.enemyInspection.hidingZolTransitionCountdown(slot); }

    int spikeTrapState(int slot) { return this.enemyInspection.spikeTrapState(slot); }

    int spikeTrapTransitionCountdown(int slot) { return this.enemyInspection.spikeTrapTransitionCountdown(slot); }

    int spikeTrapDirection(int slot) { return this.enemyInspection.spikeTrapDirection(slot); }

    int spikeTrapSpeedX(int slot) { return this.enemyInspection.spikeTrapSpeedX(slot); }

    int spikeTrapSpeedY(int slot) { return this.enemyInspection.spikeTrapSpeedY(slot); }

    int spikeTrapPrivateState1(int slot) { return this.enemyInspection.spikeTrapPrivateState1(slot); }

    int spikeTrapPrivateState2(int slot) { return this.enemyInspection.spikeTrapPrivateState2(slot); }

    int waterTektiteState(int slot) { return this.enemyInspection.waterTektiteState(slot); }

    int waterTektiteTransitionCountdown(int slot) { return this.enemyInspection.waterTektiteTransitionCountdown(slot); }

    int waterTektiteSpeedX(int slot) { return this.enemyInspection.waterTektiteSpeedX(slot); }

    int waterTektiteSpeedY(int slot) { return this.enemyInspection.waterTektiteSpeedY(slot); }

    int waterTektitePrivateState1(int slot) { return this.enemyInspection.waterTektitePrivateState1(slot); }

    int waterTektitePrivateState2(int slot) { return this.enemyInspection.waterTektitePrivateState2(slot); }

    int stalfosState(int slot) { return this.enemyInspection.stalfosState(slot); }

    int stalfosTransitionCountdown(int slot) { return this.enemyInspection.stalfosTransitionCountdown(slot); }

    int stalfosSpeedX(int slot) { return this.enemyInspection.stalfosSpeedX(slot); }

    int stalfosSpeedY(int slot) { return this.enemyInspection.stalfosSpeedY(slot); }

    int stalfosSpeedZ(int slot) { return this.enemyInspection.stalfosSpeedZ(slot); }

    int evasivePrivateState1(int slot) { return this.enemyInspection.evasivePrivateState1(slot); }

    int evasivePrivateCountdown1(int slot) { return this.enemyInspection.evasivePrivateCountdown1(slot); }

    int evasiveInertia(int slot) { return this.enemyInspection.evasiveInertia(slot); }

    int evasiveSpeedX(int slot) { return this.enemyInspection.evasiveSpeedX(slot); }

    int evasiveSpeedY(int slot) { return this.enemyInspection.evasiveSpeedY(slot); }

    int evasiveSpeedZ(int slot) { return this.enemyInspection.evasiveSpeedZ(slot); }

    void setEvasivePrivateCountdown1ForTest(int slot, int value) { this.entityDebugConfiguration.setEvasivePrivateCountdown1ForTest(slot, value); }

    void setEvasiveFleeingForTest(int slot, int newSpeedX, int newSpeedY) { this.entityDebugConfiguration.setEvasiveFleeingForTest(slot, newSpeedX, newSpeedY); }

    int gibdoState(int slot) { return this.enemyInspection.gibdoState(slot); }

    int gibdoSpeedX(int slot) { return this.enemyInspection.gibdoSpeedX(slot); }

    int gibdoSpeedY(int slot) { return this.enemyInspection.gibdoSpeedY(slot); }

    int goombaState(int slot) { return this.enemyInspection.goombaState(slot); }

    int goombaTransitionCountdown(int slot) { return this.enemyInspection.goombaTransitionCountdown(slot); }

    int goombaSpeedX(int slot) { return this.enemyInspection.goombaSpeedX(slot); }

    int goombaSpeedY(int slot) { return this.enemyInspection.goombaSpeedY(slot); }

    int snakeState(int slot) { return this.enemyInspection.snakeState(slot); }

    int snakeTransitionCountdown(int slot) { return this.enemyInspection.snakeTransitionCountdown(slot); }

    int snakePrivateCountdown1(int slot) { return this.enemyInspection.snakePrivateCountdown1(slot); }

    int snakeSpeedX(int slot) { return this.enemyInspection.snakeSpeedX(slot); }

    int snakeSpeedY(int slot) { return this.enemyInspection.snakeSpeedY(slot); }

    int polsVoiceState(int slot) { return this.enemyInspection.polsVoiceState(slot); }

    int polsVoiceTransitionCountdown(int slot) { return this.enemyInspection.polsVoiceTransitionCountdown(slot); }

    int polsVoiceSpeedX(int slot) { return this.enemyInspection.polsVoiceSpeedX(slot); }

    int polsVoiceSpeedY(int slot) { return this.enemyInspection.polsVoiceSpeedY(slot); }

    int polsVoiceSpeedZ(int slot) { return this.enemyInspection.polsVoiceSpeedZ(slot); }

    int spikedBeetleState(int slot) { return this.enemyInspection.spikedBeetleState(slot); }

    int spikedBeetleTransitionCountdown(int slot) { return this.enemyInspection.spikedBeetleTransitionCountdown(slot); }

    int spikedBeetleSpeedX(int slot) { return this.enemyInspection.spikedBeetleSpeedX(slot); }

    int spikedBeetleSpeedY(int slot) { return this.enemyInspection.spikedBeetleSpeedY(slot); }

    int wizrobeState(int slot) { return this.enemyInspection.wizrobeState(slot); }

    int wizrobeDirection(int slot) { return this.enemyInspection.wizrobeDirection(slot); }

    int wizrobePrivateState1(int slot) { return this.enemyInspection.wizrobePrivateState1(slot); }

    int wizrobePrivateCountdown1(int slot) { return this.enemyInspection.wizrobePrivateCountdown1(slot); }

    int wizrobeNextSpriteVariant(int slot) { return this.enemyInspection.wizrobeNextSpriteVariant(slot); }

    void setWizrobeStateForTest(int slot, int state, int transitionCountdown, int privateState1, int privateCountdown1, int direction) { this.entityDebugConfiguration.setWizrobeStateForTest(slot, state, transitionCountdown, privateState1, privateCountdown1, direction); }

    int wizrobeProjectileSpeedX(int slot) { return this.enemyInspection.wizrobeProjectileSpeedX(slot); }

    int wizrobeProjectileSpeedY(int slot) { return this.enemyInspection.wizrobeProjectileSpeedY(slot); }

    void setWizrobeProjectileForTest(int slot, int newSpeedX, int newSpeedY, int direction) { this.entityDebugConfiguration.setWizrobeProjectileForTest(slot, newSpeedX, newSpeedY, direction); }

    int peaHatState(int slot) { return this.enemyInspection.peaHatState(slot); }

    int peaHatSlowTransitionCountdown(int slot) { return this.enemyInspection.peaHatSlowTransitionCountdown(slot); }

    int peaHatPrivateState1(int slot) { return this.enemyInspection.peaHatPrivateState1(slot); }

    int peaHatPrivateState4(int slot) { return this.enemyInspection.peaHatPrivateState4(slot); }

    int peaHatSpeedX(int slot) { return this.enemyInspection.peaHatSpeedX(slot); }

    int peaHatSpeedY(int slot) { return this.enemyInspection.peaHatSpeedY(slot); }

    int armosState(int slot) { return this.enemyInspection.armosState(slot); }

    int armosTransitionCountdown(int slot) { return this.enemyInspection.armosTransitionCountdown(slot); }

    int armosSpeedX(int slot) { return this.enemyInspection.armosSpeedX(slot); }

    int armosSpeedY(int slot) { return this.enemyInspection.armosSpeedY(slot); }

    int armosKnightState(int slot) { return this.bossInspection.armosKnightState(slot); }

    int armosKnightTransitionCountdown(int slot) { return this.bossInspection.armosKnightTransitionCountdown(slot); }

    int armosKnightSpeedZ(int slot) { return this.bossInspection.armosKnightSpeedZ(slot); }

    int armosKnightPrivateCountdown1(int slot) { return this.bossInspection.armosKnightPrivateCountdown1(slot); }

    int armosKnightInertia(int slot) { return this.bossInspection.armosKnightInertia(slot); }

    int ghiniTransitionCountdown(int slot) { return this.enemyInspection.ghiniTransitionCountdown(slot); }

    int ghiniPrivateCountdown1(int slot) { return this.enemyInspection.ghiniPrivateCountdown1(slot); }

    int ghiniTargetXDirection(int slot) { return this.enemyInspection.ghiniTargetXDirection(slot); }

    int ghiniTargetYDirection(int slot) { return this.enemyInspection.ghiniTargetYDirection(slot); }

    int ghiniSpeedX(int slot) { return this.enemyInspection.ghiniSpeedX(slot); }

    int ghiniSpeedY(int slot) { return this.enemyInspection.ghiniSpeedY(slot); }

    int hardHatSpeedX(int slot) { return this.enemyInspection.hardHatSpeedX(slot); }

    int hardHatSpeedY(int slot) { return this.enemyInspection.hardHatSpeedY(slot); }

    int butterflyPrivateStateX(int slot) { return this.enemyInspection.butterflyPrivateStateX(slot); }

    int butterflyPrivateStateY(int slot) { return this.enemyInspection.butterflyPrivateStateY(slot); }

    int transitionCountdown(int slot) { return this.enemyInspection.transitionCountdown(slot); }

    int dogState(int slot) { return this.enemyInspection.dogState(slot); }

    int witchRatState(int slot) { return this.enemyInspection.witchRatState(slot); }

    int witchRatTransitionCountdown(int slot) { return this.enemyInspection.witchRatTransitionCountdown(slot); }

    int witchRatSpeedZ(int slot) { return this.enemyInspection.witchRatSpeedZ(slot); }

    int stunnedCountdown(int slot) { return this.enemyInspection.stunnedCountdown(slot); }

    int dyingCountdown(int slot) { return this.enemyInspection.dyingCountdown(slot); }

    boolean powerRecoilDeath(int slot) { return this.enemyInspection.powerRecoilDeath(slot); }

    int enemyHealth(int slot) { return this.enemyInspection.enemyHealth(slot); }

    int enemyFlashCountdown(int slot) { return this.enemyInspection.enemyFlashCountdown(slot); }

    int enemyIgnoreHitsCountdown(int slot) { return this.enemyInspection.enemyIgnoreHitsCountdown(slot); }

    int enemyRecoilSpeedXForTest(int slot) { return this.enemyInspection.enemyRecoilSpeedXForTest(slot); }

    int enemyRecoilSpeedYForTest(int slot) { return this.enemyInspection.enemyRecoilSpeedYForTest(slot); }

    int thrownDirection(int slot) {
        validateEntitySlot(slot);
        return thrownDirection[slot] & 0xFF;
    }

    int ledgeTransitionTimer(int slot) { return this.enemyInspection.ledgeTransitionTimer(slot); }

    int fallingTargetX(int slot) { return this.enemyInspection.fallingTargetX(slot); }

    int fallingTargetY(int slot) { return this.enemyInspection.fallingTargetY(slot); }

    int fallingVisualYOffset(int slot) { return this.enemyInspection.fallingVisualYOffset(slot); }

    void setEnemyIgnoreHitsCountdownForTest(int slot, int value) { this.entityDebugConfiguration.setEnemyIgnoreHitsCountdownForTest(slot, value); }

    void setOcarinaPlayback(int countdown, int songFlags, int selectedSong) { this.entityRuntimeConfiguration.setOcarinaPlayback(countdown, songFlags, selectedSong); }

    void setOcarinaPlayback(int countdown, int songFlags, int selectedSong, int animationCounter, int animationPhase) { this.entityRuntimeConfiguration.setOcarinaPlayback(countdown, songFlags, selectedSong, animationCounter, animationPhase); }

    void setOcarinaPlaybackForTest(int countdown, int songFlags, int selectedSong) { this.entityDebugConfiguration.setOcarinaPlaybackForTest(countdown, songFlags, selectedSong); }

    void setOcarinaAnimationForTest(int animationCounter, int animationPhase) { this.entityDebugConfiguration.setOcarinaAnimationForTest(animationCounter, animationPhase); }

    int musicalNoteInertiaForTest(int slot) { return this.enemyInspection.musicalNoteInertiaForTest(slot); }

    void setHitboxFlagsForTest(int slot, int value) { this.entityDebugConfiguration.setHitboxFlagsForTest(slot, value); }

    int hitboxFlagsForTest(int slot) { return this.enemyInspection.hitboxFlagsForTest(slot); }

    void setEnemyFlashCountdownForTest(int slot, int value) { this.entityDebugConfiguration.setEnemyFlashCountdownForTest(slot, value); }

    void setEnemyHealthForTest(int slot, int value) { this.entityDebugConfiguration.setEnemyHealthForTest(slot, value); }

    int geniePrivateState1ForTest(int slot) { return this.bossInspection.geniePrivateState1ForTest(slot); }

    void setGeniePrivateState1ForTest(int slot, int value) { this.entityDebugConfiguration.setGeniePrivateState1ForTest(slot, value); }

    int genieActiveStateForTest(int slot) { return this.bossInspection.genieActiveStateForTest(slot); }

    void setGenieActiveStateForTest(int slot, int value) { this.entityDebugConfiguration.setGenieActiveStateForTest(slot, value); }

    int geniePrivateState3ForTest(int slot) { return this.bossInspection.geniePrivateState3ForTest(slot); }

    void setGeniePrivateState3ForTest(int slot, int value) { this.entityDebugConfiguration.setGeniePrivateState3ForTest(slot, value); }

    int geniePrivateCountdown1ForTest(int slot) { return this.bossInspection.geniePrivateCountdown1ForTest(slot); }

    void setGeniePrivateCountdown1ForTest(int slot, int value) { this.entityDebugConfiguration.setGeniePrivateCountdown1ForTest(slot, value); }

    int geniePrivateCountdown2ForTest(int slot) { return this.bossInspection.geniePrivateCountdown2ForTest(slot); }

    int genieSpeedXForTest(int slot) { return this.bossInspection.genieSpeedXForTest(slot); }

    int genieSpeedZForTest(int slot) { return this.bossInspection.genieSpeedZForTest(slot); }

    void setGeniePrivateState4ForTest(int slot, int value) { this.entityDebugConfiguration.setGeniePrivateState4ForTest(slot, value); }

    int geniePrivateState4ForTest(int slot) { return this.bossInspection.geniePrivateState4ForTest(slot); }

    int rollingBonesBarStateForTest(int slot) { return this.bossInspection.rollingBonesBarStateForTest(slot); }

    int rollingBonesBarSpeedXForTest(int slot) { return this.bossInspection.rollingBonesBarSpeedXForTest(slot); }

    void setPhysicsFlagsForTest(int slot, int value) { this.entityDebugConfiguration.setPhysicsFlagsForTest(slot, value); }

    void setSpriteVariantForTest(int slot, int variant) { this.entityDebugConfiguration.setSpriteVariantForTest(slot, variant); }

    void setThrownDirection(int slot, int value) { this.entityRuntimeConfiguration.setThrownDirection(slot, value); }

    void setLedgeTransitionTimer(int slot, int value) { this.entityRuntimeConfiguration.setLedgeTransitionTimer(slot, value); }

    void setThrownDirectionForTest(int slot, int value) { this.entityDebugConfiguration.setThrownDirectionForTest(slot, value); }

    void setLedgeTransitionTimerForTest(int slot, int value) { this.entityDebugConfiguration.setLedgeTransitionTimerForTest(slot, value); }

    int keyDropTransitionCountdown(int slot) { return this.pickupInspection.keyDropTransitionCountdown(slot); }

    void setKeyDropTransitionCountdownForTest(int slot, int value) { this.entityDebugConfiguration.setKeyDropTransitionCountdownForTest(slot, value); }

    void setHidingSlimeKeyTransitionCountdownForTest(int slot, int value) { this.entityDebugConfiguration.setHidingSlimeKeyTransitionCountdownForTest(slot, value); }

    void setGoldenLeavesCountForTest(int value) { this.entityDebugConfiguration.setGoldenLeavesCountForTest(value); }

    void setGoldenLeavesCount(int value) { this.entityRuntimeConfiguration.setGoldenLeavesCount(value); }

    int options1(int slot) { return this.slotProperties.options1(slot); }

    int colorShellState(int slot) { return this.pickupInspection.colorShellState(slot); }

    int bombiteState(int slot) { return this.pickupInspection.bombiteState(slot); }

    int moldormHeadSpriteVariant(int slot) { return this.bossInspection.moldormHeadSpriteVariant(slot); }

    int moldormTransitionCountdown(int slot) { return this.bossInspection.moldormTransitionCountdown(slot); }

    int moldormPrivateCountdown2(int slot) { return this.bossInspection.moldormPrivateCountdown2(slot); }

    int moldormDestructionState(int slot) { return this.bossInspection.moldormDestructionState(slot); }

    int moldormInertia(int slot) { return this.bossInspection.moldormInertia(slot); }

    int bombitePrivateCountdown1(int slot) { return this.pickupInspection.bombitePrivateCountdown1(slot); }

    int bombiteSpeedX(int slot) { return this.pickupInspection.bombiteSpeedX(slot); }

    int bombiteSpeedY(int slot) { return this.pickupInspection.bombiteSpeedY(slot); }

    int colorShellPhysicsFlags(int slot) { return this.pickupInspection.colorShellPhysicsFlags(slot); }

    int secretSeashellPrivateState3(int slot) { return this.pickupInspection.secretSeashellPrivateState3(slot); }

    int secretSeashellPrivateState4(int slot) { return this.pickupInspection.secretSeashellPrivateState4(slot); }

    int secretSeashellPrivateCountdown1(int slot) { return this.pickupInspection.secretSeashellPrivateCountdown1(slot); }

    int secretSeashellSlowTransitionCountdown(int slot) { return this.pickupInspection.secretSeashellSlowTransitionCountdown(slot); }

    int secretSeashellSpeedX(int slot) { return this.pickupInspection.secretSeashellSpeedX(slot); }

    int secretSeashellSpeedY(int slot) { return this.pickupInspection.secretSeashellSpeedY(slot); }

    int secretSeashellSpeedZ(int slot) { return this.pickupInspection.secretSeashellSpeedZ(slot); }

    int fishState(int slot) { return this.flyingEnemyInspection.fishState(slot); }

    int fishSpeedX(int slot) { return this.flyingEnemyInspection.fishSpeedX(slot); }

    int fishTransitionCountdown(int slot) { return this.flyingEnemyInspection.fishTransitionCountdown(slot); }

    int crowState(int slot) { return this.flyingEnemyInspection.crowState(slot); }

    int crowTransitionCountdown(int slot) { return this.flyingEnemyInspection.crowTransitionCountdown(slot); }

    int booBuddyState(int slot) { return this.flyingEnemyInspection.booBuddyState(slot); }

    int booBuddySpeedX(int slot) { return this.flyingEnemyInspection.booBuddySpeedX(slot); }

    int booBuddyTransitionCountdown(int slot) { return this.flyingEnemyInspection.booBuddyTransitionCountdown(slot); }

    void setBooBuddyTriggerCount(int value) { this.entityRuntimeConfiguration.setBooBuddyTriggerCount(value); }

    void setWitchEnvironment(int bgPaletteEffectAddress, int paletteDataFlags, int defaultMusicTrack) { this.entityRuntimeConfiguration.setWitchEnvironment(bgPaletteEffectAddress, paletteDataFlags, defaultMusicTrack); }

    void setBooBuddyTriggerCountForTest(int value) { this.entityDebugConfiguration.setBooBuddyTriggerCountForTest(value); }

    void setColorShellStateForTest(int slot, int state, int transitionCountdown, int direction, int speedX, int speedY) { this.entityDebugConfiguration.setColorShellStateForTest(slot, state, transitionCountdown, direction, speedX, speedY); }

    int enemyRecoilSpeedX(int slot) { return this.enemyInspection.enemyRecoilSpeedX(slot); }

    int enemyRecoilSpeedY(int slot) { return this.enemyInspection.enemyRecoilSpeedY(slot); }

    void configureEnemyRecoilForTest(int slot, int linkEntityX, int linkEntityY, int length) { this.entityDebugConfiguration.configureEnemyRecoilForTest(slot, linkEntityX, linkEntityY, length); }

    boolean enemyRecoilActive(int slot) { return this.enemyInspection.enemyRecoilActive(slot); }

    record CommonDroppableUpdate(RoomEntity entity, boolean hidden) {
    }

    record SlimeKeyTransitionUpdate(RoomEntity entity, int nextTransitionCountdown,
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
