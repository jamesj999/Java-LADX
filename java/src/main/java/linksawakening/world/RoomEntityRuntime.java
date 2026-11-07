package linksawakening.world;

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
    private final EntityBombInteractions bombInteractions = new EntityBombInteractions(this);
    private final EntitySlotLifecycle lifecycle = new EntitySlotLifecycle(this);
    private final EntityEnemySpawns entityEnemySpawns = new EntityEnemySpawns(this);
    private final EntityPlayerSpawns entityPlayerSpawns = new EntityPlayerSpawns(this);
    private final EntityEffectConsumption effects = new EntityEffectConsumption(this);
    private final EntityRuntimeConfiguration entityRuntimeConfiguration = new EntityRuntimeConfiguration(this);
    private final EntityDebugConfiguration entityDebugConfiguration = new EntityDebugConfiguration(this);
    final EntityHandlerRegistry entityHandlers = createEntityHandlers();
    private final EntityRewardController rewards = new EntityRewardController(this);
    private final EntityStoryController story = new EntityStoryController(this);
    private final GenieEntityController genie = new GenieEntityController(this);
    private final EntityProjectileController projectiles = new EntityProjectileController(this);
    private final EntityPickupController pickups = new EntityPickupController(this);
    private final EntityLiftThrowController liftThrow = new EntityLiftThrowController(this);
    private final EntityCombatResolver combat = new EntityCombatResolver(this);
    private final EntityBossRewards bossRewards = new EntityBossRewards(this);
    private final EntityRoomInspection roomInspection = new EntityRoomInspection(this);
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

    // The three WRAM values read by PolsVoiceEntityHandler's Ocarina preamble.
    // RoomSession supplies these from the active Ocarina item state once per
    // entity tick; the zero values preserve the ordinary no-song path.

    // LinkMotionMapFadeInHandler leaves wTransitionSequenceCounter at $04 once
    // the active room is ready for interaction. Entity ticks are gated during
    // the host transition, so this is the source value visible to gameplay.
    
    // ReturnIfNonInteractive_19's gameplay selector and room-transition flag.
    // Live room sessions default to the ordinary active world path; tests can
    // select the world-map/transition branches explicitly.

    // hLinkSpeedX/Y are written by Link before AnimateEntities. These are set
    // only around the live projectile tick so the enemy-bomb branch can mirror
    // its post-collision SLA writes without making Link state part of the
    // entity snapshot.

    /** AnimateEntities copies wC111 to wC1A8 before decrementing it. */

    void setFreeMovementMode(boolean enabled) { freeMovementMode = enabled; }
    
    /** wEntitiesPrivateCountdown1Table for the owl-event handler. */

    final WarpMotion warpMotion;
    
    // These latches represent the HRAM values read by
    // DroppableRevealOrReturnIfNeeded's tree-shell branch. RoomSession feeds
    // them from Link's live Pegasus-Boots collision record before dispatch.

    private EntityHandlerRegistry createEntityHandlers() {
        EntityHandlerRegistry registry = new EntityHandlerRegistry();
        registry.register(ENTITY_KEESE, (entity, random) -> keeseMotion.initialize(entity.slot(), random));
        registry.register(ENTITY_IRON_MASK, (entity, random) -> roamingEnemyMotion.initialize(entity.slot()));
        registry.register(ENTITY_TEKTITE, (entity, random) -> tektiteMotion.initialize(entity.slot(), random));
        registry.register(ENTITY_LEEVER, (entity, random) -> leeverMotion.initialize(entity.slot()));
        registry.register(ENTITY_ANTI_FAIRY, (entity, random) -> antiFairyMotion.initialize(entity.slot(), random));
        registry.register(ENTITY_HIDING_ZOL, (entity, random) -> hidingZolMotion.initialize(entity.slot()));
        registry.register(ENTITY_STAR, (entity, random) -> starMotion.initialize(entity.slot(), random));
        registry.register(ENTITY_BLOOPER, (entity, random) -> blooperMotion.initialize(entity.slot()));
        registry.register(ENTITY_PINCER, (entity, random) -> pincerMotion.initialize(entity.slot()));
        registry.register(ENTITY_WINGED_OCTOROK, (entity, random) -> wingedOctorokMotion.initialize(entity.slot()));
        registry.register(ENTITY_SPIKE_TRAP, (entity, random) -> spikeTrapMotion.initialize(entity.slot(), random));
        registry.register(ENTITY_PAIRODD, (entity, random) -> pairoddMotion.initialize(entity.slot(), random));
        registry.register(ENTITY_PAIRODD_PROJECTILE, (entity, random) -> pairoddProjectileMotion.initialize(entity.slot(), random));
        registry.register(ENTITY_WATER_TEKTITE, (entity, random) -> waterTektiteMotion.initialize(entity.slot()));
        registry.register(ENTITY_FISH, (entity, random) -> fishMotion.initialize(entity.slot()));
        registry.register(ENTITY_CROW, (entity, random) -> crowMotion.initialize(entity.slot()));
        registry.register(ENTITY_CUCCO, (entity, random) -> cuccoMotion.initialize(entity.slot()));
        registry.register(ENTITY_POKEY, (entity, random) -> pokeyMotion.initialize(entity.slot()));
        registry.register(ENTITY_PIRANHA_PLANT, (entity, random) -> piranhaMotion.initialize(entity.slot()));
        registry.register(ENTITY_ZORA, (entity, random) -> zoraMotion.initialize(entity.slot()));
        registry.register(ENTITY_ZOMBIE, (entity, random) -> zombieMotion.initialize(entity.slot()));
        registry.register(ENTITY_BUZZ_BLOB, (entity, random) -> buzzBlobMotion.initialize(entity.slot()));
        registry.register(ENTITY_SAND_CRAB, (entity, random) -> sandCrabMotion.initialize(entity.slot()));
        registry.register(ENTITY_DOG, (entity, random) -> dogMotion.initialize(entity.slot()));
        registry.register(ENTITY_URCHIN, (entity, random) -> urchinMotion.initialize(entity.slot()));
        registry.register(ENTITY_WITCH_RAT, (entity, random) -> witchRatMotion.initialize(entity.slot()));
        registry.register(ENTITY_BOO_BUDDY, (entity, random) -> booBuddyMotion.initialize(entity.slot()));
        registry.register(ENTITY_DROPPABLE_FAIRY, (entity, random) -> fairyMotion.initialize(entity.slot()));
        registry.register(ENTITY_STALFOS_AGGRESSIVE, (entity, random) -> stalfosAggressiveMotion.initialize(entity.slot(), random));
        registry.register(ENTITY_STALFOS_EVASIVE, (entity, random) -> stalfosEvasiveMotion.initialize(entity.slot()));
        registry.register(ENTITY_GIBDO, (entity, random) -> gibdoMotion.initialize(entity.slot()));
        registry.register(ENTITY_LIKE_LIKE, (entity, random) -> likeLikeMotion.initialize(entity.slot()));
        registry.register(ENTITY_POLS_VOICE, (entity, random) -> polsVoiceMotion.initialize(entity.slot()));
        registry.register(ENTITY_GOOMBA, (entity, random) -> goombaMotion.initialize(entity.slot()));
        registry.register(ENTITY_SNAKE, (entity, random) -> snakeMotion.initialize(entity.slot()));
        registry.register(ENTITY_PEAHAT, (entity, random) -> peaHatMotion.initialize(entity.slot()));
        registry.register(ENTITY_ARMOS_STATUE, (entity, random) -> armosMotion.initialize(entity.slot()));
        registry.register(ENTITY_ARMOS_KNIGHT, (entity, random) -> armosKnightMotion.initialize(entity.slot()));
        registry.register(ENTITY_HINOX, (entity, random) -> hinoxMotion.initialize(entity.slot()));
        registry.register(ENTITY_HARDHAT_BEETLE, (entity, random) -> hardHatMotion.initialize(entity.slot()));
        registry.register(ENTITY_SPIKED_BEETLE, (entity, random) -> spikedBeetleMotion.initialize(entity.slot()));
        registry.register(ENTITY_BOMBER, (entity, random) -> bomberMotion.initialize(entity.slot()));
        registry.register(ENTITY_MAD_BOMBER, (entity, random) -> madBomberMotion.initialize(entity.slot()));
        registry.registerAdvance(ENTITY_BUTTERFLY, (entity, context) -> butterflyMotion.advance(entity, context.frameCounter(), context.linkEntityX(), context.linkEntityY(),
                    context.randomByteSupplier()));
        registry.registerAdvance(ENTITY_KEESE, (entity, context) -> keeseMotion.advance(entity, context.frameCounter(), context.linkEntityX(), context.linkEntityY(),
                    context.randomByteSupplier()));
        registry.registerAdvance(ENTITY_TEKTITE, (entity, context) -> tektiteMotion.advance(entity, context.linkEntityX(), context.linkEntityY(),
                    context.randomByteSupplier(), context.backgroundCollision()));
        registry.registerAdvance(ENTITY_LEEVER, (entity, context) -> leeverMotion.advance(entity, context.frameCounter(), context.linkEntityX(), context.linkEntityY(),
                    context.randomByteSupplier(), context.backgroundCollision()));
        registry.registerAdvance(ENTITY_ANTI_FAIRY, (entity, context) -> antiFairyMotion.advance(entity, context.frameCounter(), context.backgroundCollision(),
                    context.randomByteSupplier()));
        registry.registerAdvance(ENTITY_GIBDO, (entity, context) -> gibdoMotion.advance(entity, context.randomByteSupplier(), context.backgroundCollision()));
        EntityPhase1Handlers.install(registry);
        EntityPhase2Handlers.install(registry);
        EntityPhase3Handlers.install(registry);
        EntityPhase4Handlers.install(registry);
        EntityPhase5Handlers.install(registry);
        return registry;
    }

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

    /** Collision payload consumed by EntityCheckThrowAtTriggers in bank $03. */
    public record ThrownBackgroundCollisionEvent(int slot, int entityType,
                                                  int direction, int objectId) implements EntityEvent {
    }

    /** A ROM transient-VFX creation requested by an entity handler this frame. */
    public record TransientVfxRequest(TransientVfxType type, int worldX, int worldY,
                                      int variant) implements EntityEvent {
        public TransientVfxRequest(TransientVfxType type, int worldX, int worldY) {
            this(type, worldX, worldY, 0);
        }
    }

    public record HeartContainerRewardEvent(int slot) implements EntityEvent {
    }

    public record MadamMeowMeowFullHealRequest(int slot) implements EntityEvent {
    }

    public record SwordPickupRewardEvent(int slot) implements EntityEvent {
    }

    public record ToadstoolRewardEvent(int slot) implements EntityEvent {
    }

    public record InstrumentRewardEvent(int slot) implements EntityEvent {
    }

    public record InstrumentCompletionEvent(int slot, int mapId) implements EntityEvent {
    }

    public record RichardProgressEvent(int slot, Kind kind, int goldenLeavesCount) implements EntityEvent {
        public enum Kind { START_QUEST, RETURN_ALL_LEAVES }
    }

    public record WitchExchangeEvent(int slot, int inventorySlot) implements EntityEvent {
    }

    public record WitchRewardEvent(int slot) implements EntityEvent {
    }

    public record OwlEventCompletion(int slot) implements EntityEvent {
    }

    /** A ROM handler request carrying the complete Link-side push response. */
    public record LinkFinalPositionRequest(int sourceSlot, boolean resetPegasusBoots,
                                           boolean resetHookshotChain,
                                           boolean markLinkPushing,
                                           boolean clearLinkPositionIncrement) implements EntityEvent {
        public LinkFinalPositionRequest(int sourceSlot) {
            this(sourceSlot, false, false, false, false);
        }

        public LinkFinalPositionRequest(int sourceSlot, boolean resetPegasusBoots,
                                        boolean resetHookshotChain,
                                        boolean markLinkPushing) {
            this(sourceSlot, resetPegasusBoots, resetHookshotChain, markLinkPushing, false);
        }

        public LinkFinalPositionRequest {
            if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES) {
                throw new IllegalArgumentException("Link final-position source slot out of range: "
                    + sourceSlot);
            }
        }
    }

    /** Link HRAM writes emitted by Rooster's active lifted handler. */
    public record RoosterLinkStateRequest(int sourceSlot, int positionZ, int velocityZ,
                                          int speedX, int speedY, int romDirection) implements EntityEvent {
        public RoosterLinkStateRequest {
            if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES) {
                throw new IllegalArgumentException("Rooster source slot out of range: "
                    + sourceSlot);
            }
            if ((positionZ & ~0xFF) != 0 || (velocityZ & ~0xFF) != 0
                || (speedX & ~0xFF) != 0 || (speedY & ~0xFF) != 0) {
                throw new IllegalArgumentException("Rooster Link state must be byte-shaped");
            }
            if (romDirection < 0 || romDirection > 3) {
                throw new IllegalArgumentException("Rooster ROM direction must be between 0 and 3");
            }
        }
    }

    /** A ROM handler request to block Link's next interactive motion frame. */
    public record LinkMotionBlockRequest(int sourceSlot, int positionDeltaX, int positionDeltaY) implements EntityEvent {
        public LinkMotionBlockRequest(int sourceSlot) { this(sourceSlot, 0, 0); }
        public LinkMotionBlockRequest {
            if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES) {
                throw new IllegalArgumentException("Link motion-block source slot out of range: "
                    + sourceSlot);
            }
        }
    }

    /** Link-side position/speed writes emitted by the side-view platform handler. */
    public record SideViewPlatformLinkRequest(int sourceSlot, int horizontalDelta,
                                              int positionY, int speedY, boolean standing,
                                              boolean activate) implements EntityEvent {
        public SideViewPlatformLinkRequest {
            if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES
                || horizontalDelta < -0x80 || horizontalDelta > 0x7F
                || (positionY & ~0xFF) != 0
                || (speedY & ~0xFF) != 0) {
                throw new IllegalArgumentException("Invalid side-view platform Link request");
            }
        }
    }

    /** Link-side writes emitted by entity-$D6's state-0 collision handler. */
    public record SideViewPotLinkRequest(int sourceSlot, boolean resetPegasusBoots,
                                         boolean restoreFinalPositionX,
                                         int ignoreCollisionCountdown, int speedX,
                                         boolean snapTop, int positionY, int speedY) implements EntityEvent {
        public SideViewPotLinkRequest {
            if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES
                || ignoreCollisionCountdown < 0 || ignoreCollisionCountdown > 0xFF
                || (speedX & ~0xFF) != 0 || (positionY & ~0xFF) != 0
                || (speedY & ~0xFF) != 0) {
                throw new IllegalArgumentException("Invalid side-view pot Link request");
            }
        }
    }

    /** A cleared entity-$61 handler request for the ROM dungeon warp table. */
    public record DungeonWarpRequest(int sourceSlot, WarpMotion.Destination destination) implements EntityEvent {
        public DungeonWarpRequest {
            if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES
                || destination == null) {
                throw new IllegalArgumentException("Invalid dungeon warp request");
            }
        }
    }

    /** Link HRAM writes emitted by entity-$61's WarpState3Handler. */
    public record WarpLinkStateRequest(int sourceSlot, int positionX, int visualPositionY,
                                       int immunityCountdown) implements EntityEvent {
        public WarpLinkStateRequest {
            if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES
                || (positionX & ~0xFF) != 0 || (visualPositionY & ~0xFF) != 0
                || (immunityCountdown & ~0xFF) != 0) {
                throw new IllegalArgumentException("Warp Link state must be byte-shaped");
            }
        }
    }

    /** Link-side HRAM effects emitted by Hinox's grab/throw handler. */
    public record HinoxLinkEffectRequest(int sourceSlot, int heldX, int heldY, int heldZ,
                                         int speedX, int speedY, int velocityZ,
                                         int airborneState, int damage,
                                         boolean motionBlocked,
                                         boolean applyHeldLinkPose) implements EntityEvent {
        public HinoxLinkEffectRequest {
            if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES) {
                throw new IllegalArgumentException("Hinox source slot out of range: " + sourceSlot);
            }
            for (int value : new int[] {heldX, heldY, heldZ, speedX, speedY, velocityZ,
                                        airborneState, damage}) {
                if ((value & ~0xFF) != 0) {
                    throw new IllegalArgumentException("Hinox Link effect must be byte-shaped");
                }
            }
        }

        public boolean throwing() {
            return speedX != 0 || speedY != 0 || velocityZ != 0 || damage != 0;
        }
    }

    /** A ROM handler request to show Link's explicit fallen animation pose. */
    public record LinkFallenPoseRequest(int sourceSlot) implements EntityEvent {
        public LinkFallenPoseRequest {
            if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES) {
                throw new IllegalArgumentException("Link fallen-pose source slot out of range: "
                    + sourceSlot);
            }
        }
    }

    public record LinkFacingRequest(int sourceSlot, int romDirection,
                                    boolean preserveWalkingPhase) implements EntityEvent {
        public LinkFacingRequest(int sourceSlot, int romDirection) {
            this(sourceSlot, romDirection, false);
        }

        public LinkFacingRequest {
            if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES
                || romDirection < 0 || romDirection > 3) {
                throw new IllegalArgumentException("Invalid Link-facing request");
            }
        }
    }

    /** A lifted-throw request applied after AnimateEntities, like the ROM HRAM write. */
    public record LinkAttackStepRequest(int sourceSlot) implements EntityEvent {
        public LinkAttackStepRequest {
            if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES) {
                throw new IllegalArgumentException(
                    "Link attack-step source slot out of range: " + sourceSlot);
            }
        }
    }

    /** A source handler's direct write clearing Link's attack animation countdown. */
    public record LinkAttackClearRequest(int sourceSlot) implements EntityEvent {
        public LinkAttackClearRequest {
            if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES) {
                throw new IllegalArgumentException("Link attack-clear source slot out of range");
            }
        }
    }

    /** Deferred room-save writes that must not alter the live handler's room status. */
    public record RoomStatusPersistenceRequest(int sourceSlot, int roomStatusMask,
                                               int tarinFlag) implements EntityEvent {
        public RoomStatusPersistenceRequest {
            if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES
                || (roomStatusMask & ~0xFF) != 0 || (tarinFlag & ~0xFF) != 0) {
                throw new IllegalArgumentException("Invalid room-status persistence request");
            }
        }
    }

    public record LinkHeldItemPoseRequest(int sourceSlot, boolean powerUp) implements EntityEvent {
        public LinkHeldItemPoseRequest(int sourceSlot) { this(sourceSlot, false); }
        public LinkHeldItemPoseRequest {
            if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES) {
                throw new IllegalArgumentException("Held-item source slot out of range: "
                    + sourceSlot);
            }
        }
    }

    public record LinkSwordSpinPoseRequest(int sourceSlot, int countdown) implements EntityEvent {
        public LinkSwordSpinPoseRequest {
            if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES) {
                throw new IllegalArgumentException("Sword-spin source slot out of range: "
                    + sourceSlot);
            }
            if (countdown < 0 || countdown > 0x20) {
                throw new IllegalArgumentException("Sword-spin countdown out of range: "
                    + countdown);
            }
        }
    }

    public record LinkSwordFinalPoseRequest(int sourceSlot) implements EntityEvent {
        public LinkSwordFinalPoseRequest {
            if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES) {
                throw new IllegalArgumentException("Sword-final source slot out of range: "
                    + sourceSlot);
            }
        }
    }

    /** A ROM handler request to start the source screen-shake timer. */
    public record ScreenShakeRequest(int sourceSlot, int countdown, int phase) implements EntityEvent {
        public ScreenShakeRequest {
            if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES) {
                throw new IllegalArgumentException("Screen-shake source slot out of range: "
                    + sourceSlot);
            }
            if (countdown < 0 || countdown > 0xFF) {
                throw new IllegalArgumentException("Screen-shake countdown must be an unsigned byte");
            }
            if (phase < 0 || phase > 0x04 || (phase & 0x01) != 0) {
                throw new IllegalArgumentException("Screen-shake phase must be 0, 2, or 4");
            }
        }
    }

    /** A boomerang request to reveal the object it intersected after movement. */
    public record BoomerangObjectRequest(int sourceSlot, int location,
                                         int objectLeft, int objectTop) implements EntityEvent {
        public BoomerangObjectRequest {
            if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES) {
                throw new IllegalArgumentException("Boomerang source slot out of range: "
                    + sourceSlot);
            }
            location &= 0xFF;
            objectLeft &= 0xF0;
            objectTop &= 0xF0;
        }
    }

    /** A Magic Rod fireball request to reveal the object it burned. */
    public record MagicRodObjectRequest(int sourceSlot, int location,
                                        int objectLeft, int objectTop) implements EntityEvent {
        public MagicRodObjectRequest {
            if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES) {
                throw new IllegalArgumentException("Magic Rod source slot out of range: "
                    + sourceSlot);
            }
            location &= 0xFF;
            objectLeft &= 0xF0;
            objectTop &= 0xF0;
        }
    }

    /** Object-side effect emitted by MagicPowderSprinkleEntityHandler. */
    public enum MagicPowderObjectAction {
        REVEAL,
        IGNITE_TORCH,
        EXTINGUISH_TORCH
    }

    /** A ROM powder-sprinkle object mutation applied by RoomSession. */
    public record MagicPowderObjectRequest(int sourceSlot, int location,
                                           int objectLeft, int objectTop,
                                           MagicPowderObjectAction action) implements EntityEvent {
        public MagicPowderObjectRequest {
            if (sourceSlot < 0 || sourceSlot >= EntityRoomLoader.MAX_ENTITIES) {
                throw new IllegalArgumentException("Magic Powder source slot out of range: "
                    + sourceSlot);
            }
            location &= 0xFF;
            objectLeft &= 0xF0;
            objectTop &= 0xF0;
            if (action == null) {
                throw new IllegalArgumentException("Magic Powder object action cannot be null");
            }
        }
    }

    /** A ROM dialog-table entry requested by an entity handler this frame. */
    public record DialogRequest(int tableId, int dialogLowId, boolean forceTop) implements EntityEvent {
        public DialogRequest(int tableId, int dialogLowId) { this(tableId, dialogLowId, false); }
        public DialogRequest {
            if (tableId < 0 || tableId > 2) {
                throw new IllegalArgumentException("Dialog table id must be 0, 1, or 2");
            }
            if ((dialogLowId & ~0xFF) != 0) {
                throw new IllegalArgumentException("Dialog id must be an unsigned byte: "
                    + dialogLowId);
            }
        }

        public int globalDialogId() {
            return (tableId << 8) | dialogLowId;
        }
    }

    /** Reward application requested by EntityInitChestWithItem. */
    public record ChestRewardEvent(int slot, int itemType) implements EntityEvent {
        public ChestRewardEvent {
            if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
                throw new IllegalArgumentException("Chest entity slot out of range: " + slot);
            }
            if ((itemType & ~0xFF) != 0) {
                throw new IllegalArgumentException("Chest item must be an unsigned byte: "
                    + itemType);
            }
        }
    }

    /** Reward application requested by PickDroppableKey. */
    public record KeyRewardEvent(int slot, int itemType) implements EntityEvent {
        public KeyRewardEvent {
            if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
                throw new IllegalArgumentException("Key entity slot out of range: " + slot);
            }
            if ((itemType & ~0xFF) != 0) {
                throw new IllegalArgumentException("Key item must be an unsigned byte: "
                    + itemType);
            }
        }
    }

    /** Reward, room-completion, and dialog state emitted by HidingSlimeKeyEntityHandler. */
    public record SlimeKeyRewardEvent(int slot, int goldenLeavesCount, int dialogLowId) implements EntityEvent {
        public SlimeKeyRewardEvent {
            if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
                throw new IllegalArgumentException("Slime Key entity slot out of range: " + slot);
            }
            if (goldenLeavesCount < 0 || goldenLeavesCount > 0x63) {
                throw new IllegalArgumentException("Golden leaves count must be between 0 and 99: "
                    + goldenLeavesCount);
            }
            if ((dialogLowId & ~0xFF) != 0) {
                throw new IllegalArgumentException("Slime Key dialog must be an unsigned byte: "
                    + dialogLowId);
            }
        }
    }

    /** Status propagation requested by CheckForEntityFallingDownQuicksandHole. */
    public record KeyQuicksandEvent(int slot) implements EntityEvent {
        public KeyQuicksandEvent {
            if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
                throw new IllegalArgumentException("Key entity slot out of range: " + slot);
            }
        }
    }

    /** Link capture/release side effects emitted by Like Like's handler. */
    public record LikeLikeEvent(int slot, Kind kind, int entityX, int entityY,
                                int stolenInventorySlot, int stolenShieldLevel,
                                boolean pinLinkPosition) implements EntityEvent {
        public enum Kind { CAPTURE, RELEASE }

        public LikeLikeEvent(int slot, Kind kind, int entityX, int entityY,
                             int stolenInventorySlot, int stolenShieldLevel) {
            this(slot, kind, entityX, entityY, stolenInventorySlot, stolenShieldLevel,
                kind == Kind.CAPTURE);
        }
    }

    /** A ROM bridge handler request to replace one padded object cell and draw its columns. */
    public record HookshotBridgeUpdate(int objectLeft, int objectTop, int direction,
                                       boolean active) implements EntityEvent {
        public HookshotBridgeUpdate(int objectLeft, int objectTop, int direction) {
            this(objectLeft, objectTop, direction, true);
        }

        public HookshotBridgeUpdate {
            objectLeft &= 0xF0;
            objectTop &= 0xF0;
            if (direction < HookshotBridgeMotion.PULL_DOWN_DIRECTION
                || direction > HookshotBridgeMotion.PULL_UP_DIRECTION) {
                throw new IllegalArgumentException("Bridge direction must be 0 or 1: " + direction);
            }
        }
    }

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
        if (romData != null) {
            for (int i = 0; i < heartPieceMeterPairs.length; i++) heartPieceMeterPairs[i] =
                romData[3 * 0x4000 + 0x1B17 + i] & 255;
            for (int i = 0; i < instrumentPaletteCycle.length; i++) {
                int offset = 0x36 * 0x4000 + 0x3096 + i * 2;
                int rgb555 = (romData[offset] & 255) | ((romData[offset + 1] & 255) << 8);
                instrumentPaletteCycle[i] = linksawakening.rom.RomUtil.decodeRgb555(rgb555);
            }
            for (int i = 0; i < instrumentWarpSprites.length; i++) {
                instrumentWarpSprites[i] = romData[6 * 0x4000 + 0x373C + i] & 255;
            }
            // Eighteen fixed repeat commands, two rows per controller frame.
            // Shipped ROM symbols: Data_007_6493=$6493, Data_007_64F6=$64DB.
            for (int row = 0; row < 18; row++) {
                int tile = romData[7 * 0x4000 + 0x2493 + row * 4 + 3] & 255;
                int attr = romData[7 * 0x4000 + 0x24DB + row * 4 + 3] & 255;
                java.util.Arrays.fill(instrumentHintTiles, row * 20, (row + 1) * 20, tile);
                java.util.Arrays.fill(instrumentHintAttributes, row * 20, (row + 1) * 20, attr);
            }
        }
        this.warpMotion = romData == null ? null : new WarpMotion(romData);
        this.hookshotChainOam = initial.hookshotChainOam();
        this.pincerBodyOam = initial.pincerBodyOam();
        this.wingedOctorokOam = initial.wingedOctorokOam();
        Arrays.fill(entityOptions1Override, -1);
        Arrays.fill(droppedItemBySlot, 0);
        Arrays.fill(bombDirection, 0xFF);
        Arrays.fill(bombFinalPresentationPending, false);
        Arrays.fill(thrownDirection, 0xFF);
        for (RoomEntity entity : slots) {
            baseEntityFlipAttribute[entity.slot()] = entity.entityFlipAttribute();
            fallingVisualYOffset[entity.slot()] = initial.visualYOffset(entity.slot());
            enemyHealth[entity.slot()] = entity.loaded() ? initialHealth(entity.type()) : 0;
            enemyPhysicsFlags[entity.slot()] = entity.loaded()
                ? initialPhysicsFlags(entity.type()) : 0;
            enemyHitboxFlags[entity.slot()] = entity.loaded()
                ? initialHitboxFlags(entity.type()) : 0;
            // Room-loaded indoor warps start at state 1 ($03:4F75).
            // State 0 is reserved for dynamically spawned warps.
            if (entity.loaded() && indoorRoom && entity.type() == WarpMotion.ENTITY_TYPE) {
                warpState[entity.slot()] = WarpMotion.STATE_APPROACH;
                slots[entity.slot()] = withPositionAndVariant(entity, entity.x(), entity.y(), -1);
            }
            if (entity.loaded() && entity.type() == ENTITY_GENIE) {
                GenieMotion.JarState jar = genieMotion.initialize(entity.slot());
                genieActiveState[entity.slot()] = 0;
                geniePrivateState1[entity.slot()] = jar.privateState1();
                geniePrivateState3[entity.slot()] = 0;
                enemyHealth[entity.slot()] = jar.health();
                enemyPhysicsFlags[entity.slot()] = jar.physicsFlags();
                enemyHitboxFlags[entity.slot()] = jar.hitboxFlags();
                entityOptions1Override[entity.slot()] = GENIE_OPTIONS1;
                if (entity.status() == EntityStatus.INIT) {
                    slots[entity.slot()] = withPositionAndVariant(
                        entity, entity.x() + 0x08, entity.y(), entity.spriteVariant());
                }
            }
            if (entity.loaded() && entity.type() == ENTITY_CHEST_WITH_ITEM) {
                chestItemBySlot[entity.slot()] = entity.spriteVariant();
            }
            if (entity.loaded() && isBombiteType(entity.type())) {
                bombiteMotion.initialize(entity.slot());
                // The bank-$04 handlers use the global slow-transition tick
                // for TimerBombite's fuse. Keep it live for either family;
                // BouncingBombite simply leaves its value at zero.
                slowTimerInitialized[entity.slot()] = true;
            }
            if (entity.loaded() && isGhiniType(entity.type())) {
                ghiniMotion.initialize(entity.slot(), entity.type());
            }
            if (entity.loaded() && entity.type() == ENTITY_LIKE_LIKE) {
                likeLikeMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == ENTITY_POLS_VOICE) {
                polsVoiceMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == ENTITY_FISH) {
                fishMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == ENTITY_CROW) {
                crowMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == ENTITY_BOO_BUDDY) {
                booBuddyMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == ENTITY_BUZZ_BLOB) {
                buzzBlobMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == ENTITY_SAND_CRAB) {
                sandCrabMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == ENTITY_DOG) {
                dogMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == ENTITY_URCHIN) {
                urchinMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == ENTITY_WITCH_RAT) {
                witchRatMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == ENTITY_DROPPABLE_FAIRY) {
                fairyMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == ENTITY_SPIKED_BEETLE) {
                spikedBeetleMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == ENTITY_PINCER) {
                pincerMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == ENTITY_BUSH_CRAWLER) {
                bushCrawlerMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == ENTITY_MIMIC) {
                mimicMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == ENTITY_MASKED_MIMIC_GORIYA) {
                maskedMimicMotion.initialize(entity.slot());
            }
            if (entity.loaded() && entity.type() == ENTITY_MINI_MOLDORM) {
                miniMoldormMotion.initialize(entity);
            }
            if (entity.loaded() && entity.type() == ENTITY_MOLDORM) {
                moldormMotion.initialize(entity);
            }
            if (entity.loaded() && entity.type() == ENTITY_ROLLING_BONES) {
                rollingBonesMotion.initializeBoss(entity.slot());
            }
            if (entity.loaded() && entity.type() == ENTITY_ROLLING_BONES_BAR) {
                rollingBonesMotion.initializeBar(entity.slot());
            }
            if (entity.loaded() && entity.type() == ENTITY_CUCCO) {
                cuccoMotion.initialize(entity.slot());
            }
            if (entity.status() == EntityStatus.DYING) {
                powerRecoilDeath[entity.slot()] = entity.powerRecoilDeath();
            }
            if (isLaserType(entity.type())) {
                laserMotion.initializeForEntity(entity);
            }
            if (isFollowingNpcType(entity.type())) {
                if (entity.type() == ENTITY_BOW_WOW) {
                    bowWowMotion.initialize(entity.slot());
                } else {
                    followingNpcMotion.initialize(entity.slot(), entity.type());
                }
            }
            if (entity.status() == EntityStatus.LIFTED) {
                liftedStateInitialized[entity.slot()] = true;
                liftedSourceDirection[entity.slot()] = LiftedEntityMotion.ROM_DIRECTION_DOWN;
                liftedEffectiveDirection = LiftedEntityMotion.ROM_DIRECTION_DOWN;
                enemyTransitionCountdown[entity.slot()] = 0x02;
                liftedEntitySlot = entity.slot();
            }
            if (entity.status() == EntityStatus.THROWN) {
                thrownDirection[entity.slot()] = ThrownEntityMotion.ROM_DIRECTION_DOWN;
            }
        }
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

    /** Advances the ROM handlers that have deterministic frame-only variants. */
    @Deprecated
    public void tick(int frameCounter) {
        IntSupplier randomByteSupplier = defaultRandomByteSupplier;
        if (randomByteSupplier == null) {
            fallbackRomRandomByteSource.beginFrame(frameCounter & 0xFF, 0);
            randomByteSupplier = fallbackRomRandomByteSource;
        }
        tick(new EntityFrameContext(frameCounter, 0, 0, 0, randomByteSupplier, null, null, null, 0, false, 0, 0, EnemyProjectileCollision.LinkState.nonInteractive(), false, 0, 0, 0, 0, 0, 0, false, true));
    }

    /**
     * Advances the active entity handlers with Link's ROM entity coordinates
     * (hLinkPositionX/Y), not Java's top-left sprite coordinates. The
     * random-byte supplier is deliberately injected so ROM handlers can be
     * tested independently from the host's entropy source.
     */
    @Deprecated
    public void tick(int frameCounter, int linkEntityX, int linkEntityY,
                     IntSupplier randomByteSupplier) {
        tick(new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, 0, randomByteSupplier, null, null, null, 0, false, 0, 0, EnemyProjectileCollision.LinkState.nonInteractive(), false, 0, 0, 0, 0, 0, 0, false, true));
    }

    /** Advances handlers with the current source-shaped Link collision byte. */
    @Deprecated
    public void tick(int frameCounter, int linkEntityX, int linkEntityY,
                     int collisionType, IntSupplier randomByteSupplier) {
        tick(new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, collisionType, randomByteSupplier, null, null, null, 0, false, 0, 0, EnemyProjectileCollision.LinkState.nonInteractive(), false, 0, 0, 0, 0, 0, 0, false, true));
    }

    @Deprecated
    public void tick(int frameCounter, int linkEntityX, int linkEntityY,
                     IntSupplier randomByteSupplier,
                     RoomEntityBackgroundCollision backgroundCollision) {
        tick(new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, 0, randomByteSupplier, backgroundCollision, null, null, 0, false, 0, 0, EnemyProjectileCollision.LinkState.nonInteractive(), false, 0, 0, 0, 0, 0, 0, false, true));
    }

    /** Background-collision compatibility path with an explicit Link byte. */
    @Deprecated
    public void tick(int frameCounter, int linkEntityX, int linkEntityY,
                     int collisionType, IntSupplier randomByteSupplier,
                     RoomEntityBackgroundCollision backgroundCollision) {
        tick(new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, collisionType, randomByteSupplier, backgroundCollision, null, null, 0, false, 0, 0, EnemyProjectileCollision.LinkState.nonInteractive(), false, 0, 0, 0, 0, 0, 0, false, true));
    }

    @Deprecated
    public void tick(int frameCounter, int linkEntityX, int linkEntityY,
                     IntSupplier randomByteSupplier, boolean creditsGameplay) {
        tick(new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, 0, randomByteSupplier, null, null, null, 0, false, 0, 0, EnemyProjectileCollision.LinkState.nonInteractive(), false, 0, 0, 0, 0, 0, 0, creditsGameplay, true));
    }

    @Deprecated
    void tick(int frameCounter, int linkEntityX, int linkEntityY,
              IntSupplier randomByteSupplier,
              RoomEntityBackgroundCollision backgroundCollision,
              LinkPositionHistory linkPositionHistory,
              int linkZ, int linkDirection, int entityYOffset) {
        tick(new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, 0, randomByteSupplier, backgroundCollision, null, linkPositionHistory, linkZ, false, linkDirection, entityYOffset, EnemyProjectileCollision.LinkState.nonInteractive(), false, 0, 0, 0, 0, 0, 0, false, true));
    }

    @Deprecated
    void tick(int frameCounter, int linkEntityX, int linkEntityY, int collisionType,
              IntSupplier randomByteSupplier,
              RoomEntityBackgroundCollision backgroundCollision,
              LinkPositionHistory linkPositionHistory,
              int linkZ, int linkDirection, int entityYOffset) {
        tick(new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, collisionType, randomByteSupplier, backgroundCollision, null, linkPositionHistory, linkZ, false, linkDirection, entityYOffset, EnemyProjectileCollision.LinkState.nonInteractive(), false, 0, 0, 0, 0, 0, 0, false, true));
    }

    @Deprecated
    List<EntityProjectileEvent> tickWithProjectileEvents(
        int frameCounter, int linkEntityX, int linkEntityY,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        EnemyProjectileCollision.LinkState projectileLinkState) {
        return tick(new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, 0, randomByteSupplier, backgroundCollision, null, null, projectileLinkState.z(), false, projectileLinkState.direction(), 0, projectileLinkState, false, 0, 0, 0, 0, currentLinkSpeedX, currentLinkSpeedY, false, projectileLinkState.motionState() < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE));
    }

    @Deprecated
    List<EntityProjectileEvent> tickWithProjectileEvents(
        int frameCounter, int linkEntityX, int linkEntityY,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        EnemyProjectileCollision.LinkState projectileLinkState,
        int linkSpeedX, int linkSpeedY) {
        validateByte(linkSpeedX, "Link X speed");
        validateByte(linkSpeedY, "Link Y speed");
        currentLinkSpeedX = linkSpeedX;
        currentLinkSpeedY = linkSpeedY;
        try {
            return tickWithProjectileEvents(frameCounter, linkEntityX, linkEntityY,
                randomByteSupplier, backgroundCollision, projectileLinkState);
        } finally {
            currentLinkSpeedX = 0;
            currentLinkSpeedY = 0;
        }
    }

    @Deprecated
    List<EntityProjectileEvent> tickWithProjectileEvents(
        int frameCounter, int linkEntityX, int linkEntityY,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        EnemyProjectileCollision.LinkState projectileLinkState,
        boolean swordCollisionActive, int swordX, int swordWidth,
        int swordY, int swordHeight) {
        return tick(new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, 0, randomByteSupplier, backgroundCollision, null, null, projectileLinkState.z(), false, projectileLinkState.direction(), 0, projectileLinkState, swordCollisionActive, swordX, swordWidth, swordY, swordHeight, currentLinkSpeedX, currentLinkSpeedY, false, projectileLinkState.motionState()
                < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE));
    }

    @Deprecated
    List<EntityProjectileEvent> tickWithProjectileEvents(
        int frameCounter, int linkEntityX, int linkEntityY,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        RoomEntityObjectCollision objectCollision,
        EnemyProjectileCollision.LinkState projectileLinkState,
        boolean swordCollisionActive, int swordX, int swordWidth,
        int swordY, int swordHeight) {
        return tick(new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, 0, randomByteSupplier, backgroundCollision, objectCollision, null, projectileLinkState.z(), false, projectileLinkState.direction(), 0, projectileLinkState, swordCollisionActive, swordX, swordWidth, swordY, swordHeight, currentLinkSpeedX, currentLinkSpeedY, false, projectileLinkState.motionState()
                < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE));
    }

    /** Full live-runtime path with a per-frame Link collision byte. */
    @Deprecated
    List<EntityProjectileEvent> tickWithProjectileEvents(
        int frameCounter, int linkEntityX, int linkEntityY, int collisionType,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        RoomEntityObjectCollision objectCollision,
        LinkPositionHistory linkPositionHistory,
        int linkZ, int linkDirection, int entityYOffset,
        EnemyProjectileCollision.LinkState projectileLinkState,
        boolean swordCollisionActive, int swordX, int swordWidth,
        int swordY, int swordHeight) {
        return tick(new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, collisionType, randomByteSupplier, backgroundCollision, objectCollision, linkPositionHistory, linkZ, false, linkDirection, entityYOffset, projectileLinkState, swordCollisionActive, swordX, swordWidth, swordY, swordHeight, currentLinkSpeedX, currentLinkSpeedY, false, projectileLinkState.motionState()
                < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE));
    }

    /** Full live-runtime path with the Link speed bytes visible to handlers. */
    @Deprecated
    List<EntityProjectileEvent> tickWithProjectileEvents(
        int frameCounter, int linkEntityX, int linkEntityY, int collisionType,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        RoomEntityObjectCollision objectCollision,
        LinkPositionHistory linkPositionHistory,
        int linkZ, int linkDirection, int entityYOffset,
        EnemyProjectileCollision.LinkState projectileLinkState,
        boolean swordCollisionActive, int swordX, int swordWidth,
        int swordY, int swordHeight, int linkSpeedX, int linkSpeedY) {
        return tickWithProjectileEvents(frameCounter, linkEntityX, linkEntityY,
            collisionType, randomByteSupplier, backgroundCollision, objectCollision,
            linkPositionHistory, linkZ, false, linkDirection, entityYOffset,
            projectileLinkState, swordCollisionActive, swordX, swordWidth,
            swordY, swordHeight, linkSpeedX, linkSpeedY);
    }

    @Deprecated
    List<EntityProjectileEvent> tickWithProjectileEvents(
        int frameCounter, int linkEntityX, int linkEntityY, int collisionType,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        RoomEntityObjectCollision objectCollision,
        LinkPositionHistory linkPositionHistory,
        int linkZ, boolean linkAirborne, int linkDirection, int entityYOffset,
        EnemyProjectileCollision.LinkState projectileLinkState,
        boolean swordCollisionActive, int swordX, int swordWidth,
        int swordY, int swordHeight, int linkSpeedX, int linkSpeedY) {
        return tick(new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, collisionType, randomByteSupplier, backgroundCollision, objectCollision, linkPositionHistory, linkZ, linkAirborne, linkDirection, entityYOffset, projectileLinkState, swordCollisionActive, swordX, swordWidth, swordY, swordHeight, linkSpeedX, linkSpeedY));
    }

    /** Advances one live frame; compatibility overloads delegate here. */
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

    @Deprecated
    List<EntityProjectileEvent> tickWithProjectileEvents(
        int frameCounter, int linkEntityX, int linkEntityY,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        LinkPositionHistory linkPositionHistory,
        int linkZ, int linkDirection, int entityYOffset,
        EnemyProjectileCollision.LinkState projectileLinkState) {
        return tick(new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, 0, randomByteSupplier, backgroundCollision, null, linkPositionHistory, linkZ, false, linkDirection, entityYOffset, projectileLinkState, false, 0, 0, 0, 0, currentLinkSpeedX, currentLinkSpeedY, false, projectileLinkState.motionState()
                < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE));
    }

    @Deprecated
    List<EntityProjectileEvent> tickWithProjectileEvents(
        int frameCounter, int linkEntityX, int linkEntityY,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        LinkPositionHistory linkPositionHistory,
        int linkZ, int linkDirection, int entityYOffset,
        EnemyProjectileCollision.LinkState projectileLinkState,
        boolean swordCollisionActive, int swordX, int swordWidth,
        int swordY, int swordHeight) {
        return tick(new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, 0, randomByteSupplier, backgroundCollision, null, linkPositionHistory, linkZ, false, linkDirection, entityYOffset, projectileLinkState, swordCollisionActive, swordX, swordWidth, swordY, swordHeight, currentLinkSpeedX, currentLinkSpeedY, false, projectileLinkState.motionState()
                < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE));
    }

    @Deprecated
    List<EntityProjectileEvent> tickWithProjectileEvents(
        int frameCounter, int linkEntityX, int linkEntityY,
        IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
        RoomEntityObjectCollision objectCollision,
        LinkPositionHistory linkPositionHistory,
        int linkZ, int linkDirection, int entityYOffset,
        EnemyProjectileCollision.LinkState projectileLinkState,
        boolean swordCollisionActive, int swordX, int swordWidth,
        int swordY, int swordHeight) {
        return tick(new EntityFrameContext(frameCounter, linkEntityX, linkEntityY, 0, randomByteSupplier, backgroundCollision, objectCollision, linkPositionHistory, linkZ, false, linkDirection, entityYOffset, projectileLinkState, swordCollisionActive, swordX, swordWidth, swordY, swordHeight, currentLinkSpeedX, currentLinkSpeedY, false, projectileLinkState.motionState()
                < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE));
    }

    private List<EntityProjectileEvent> advanceFrame(EntityFrameContext context) { return new EntityFrameExecution(this, context).advance(); }

    List<RoamingEnemyMotion.LaunchRequest> projectileLaunchRequests() { return this.roomInspection.projectileLaunchRequests(); }

    void resolveThreeOfAKindPuzzle() {
        ThreeOfAKindMotion.PuzzleResult result = threeOfAKindMotion.resolvePuzzle(
            Arrays.asList(slots), enemyTransitionCountdown);
        if (result == ThreeOfAKindMotion.PuzzleResult.NONE) {
            return;
        }
        int coordinatorSlot = Arrays.stream(slots)
            .filter(entity -> entity.status() == EntityStatus.ACTIVE
                && entity.type() == ENTITY_THREE_OF_A_KIND)
            .mapToInt(RoomEntity::slot)
            .findFirst().orElse(0);
        int jingle = result == ThreeOfAKindMotion.PuzzleResult.MATCH
            ? threeOfAKindMotion.matchedJingle(coordinatorSlot) : 0x1D;
        pendingEntityEvents.add(new EntityCombatEvent(
            coordinatorSlot, ENTITY_THREE_OF_A_KIND, 0, false,
            EntityCombatEvent.SoundChannel.JINGLE, jingle));
        if (result != ThreeOfAKindMotion.PuzzleResult.MATCH) {
            return;
        }
        for (int index = 0; index < slots.length; index++) {
            RoomEntity entity = slots[index];
            if (!entity.loaded() || entity.type() != ENTITY_THREE_OF_A_KIND) {
                continue;
            }
            int slot = entity.slot();
            droppedItemBySlot[slot] = threeOfAKindMotion.matchedDrop(slot);
            dyingCountdown[slot] = 0x1F;
            enemyHealth[slot] = 0;
            enemyPhysicsFlags[slot] = 0x04;
            slots[index] = withStatus(entity, EntityStatus.DYING);
        }
        // The source writes the shared noise register once per card in the
        // same handler pass; only the final register value is audible.
        pendingEntityEvents.add(new EntityCombatEvent(
            coordinatorSlot, ENTITY_THREE_OF_A_KIND, 0, false,
            EntityCombatEvent.SoundChannel.NOISE, 0x13));
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
    boolean beginLift(int slot, int romDirection) {
        validateEntitySlot(slot);
        validateRomDirection(romDirection);
        RoomEntity entity = slots[slot];
        if (!entity.loaded()) {
            return false;
        }
        if (entity.type() == ENTITY_BOMB && bombFinalPresentationPending[slot]) {
            return false;
        }
        if (liftedEntitySlot >= 0 && liftedEntitySlot != slot
            && slots[liftedEntitySlot].status() == EntityStatus.LIFTED) {
            return false;
        }

        liftedEntitySlot = slot;
        liftedPhase[slot] = 0;
        liftedSourceDirection[slot] = romDirection;
        liftedStateInitialized[slot] = true;
        liftedCarryState = 0;
        liftedEffectiveDirection = romDirection;
        enemyTransitionCountdown[slot] = 0x02;
        if (entity.type() == ENTITY_BOMB) {
            if (placedBombMotionInitialized[slot]) {
                thrownEntityMotion.clear(slot);
                placedBombMotionInitialized[slot] = false;
            }
            bombFinalPresentationPending[slot] = false;
            enemyFlashCountdown[slot] = 0;
            entity = withDefinition(entity, spriteDefinitionFor(ENTITY_BOMB), 0);
        }
        slots[slot] = withStatus(entity, EntityStatus.LIFTED);
        return true;
    }

    /** Hands the fully held entity to func_014_53A3's generic throw path. */
    boolean throwLiftedEntity(int romDirection) {
        validateRomDirection(romDirection);
        LiftedEntityState state = liftedEntityState();
        if (!state.active() || state.carryState() != 0x01) {
            return false;
        }

        int slot = state.slot();
        RoomEntity entity = slots[slot];
        pendingPreTickLinkAttackStepRequests.add(new LinkAttackStepRequest(slot));
        pendingPreTickEntityEvents.add(new EntityCombatEvent(slot, entity.type(), 0, false,
            EntityCombatEvent.SoundChannel.JINGLE, LIFTED_THROW_JINGLE_ID));
        if (entity.type() == ENTITY_SIDE_VIEW_POT) {
            thrownDirection[slot] = romDirection;
            sideViewPotMotion.start(slot, romDirection);
            thrownEntityMotion.clear(slot);
            thrownMotionInitialized[slot] = false;
            liftedStateInitialized[slot] = false;
            liftedPhase[slot] = 0;
            liftedCarryState = 0;
            liftedEffectiveDirection = 0;
            liftedEntitySlot = -1;
            enemyTransitionCountdown[slot] = 0;
            slots[slot] = withStatus(entity, EntityStatus.ACTIVE);
            return true;
        }
        thrownDirection[slot] = romDirection;
        thrownEntityMotion.start(slot, romDirection, entity.type(),
            groundInteractionSideScrolling);
        thrownMotionInitialized[slot] = true;
        liftedStateInitialized[slot] = false;
        liftedPhase[slot] = 0;
        liftedCarryState = 0;
        liftedEffectiveDirection = 0;
        liftedEntitySlot = -1;
        enemyTransitionCountdown[slot] = entity.type() == ENTITY_BOMB
            ? BombMotion.INITIAL_COUNTDOWN : 0;
        slots[slot] = withStatus(entity, EntityStatus.THROWN);
        return true;
    }

    LiftedEntityState liftedEntityState() { return this.roomInspection.liftedEntityState(); }

    RoomEntity advanceLiftedEntity(RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ, int romLinkDirection) { return liftThrow.advanceLiftedEntity(entity, linkEntityX, linkEntityY, linkZ, romLinkDirection); }

    RoomEntity advanceThrownEntity(RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision, boolean sideScrolling, int romLinkDirection, int frame) { return liftThrow.advanceThrownEntity(entity, backgroundCollision, sideScrolling, romLinkDirection, frame); }

    RoomEntity registerGenieJarImpact(RoomEntity jar) { return genie.registerGenieJarImpact(jar); }

    /** Applies the two source-ordered probes made by ApplyEntityInteractionWithBackground. */
    boolean sideViewPotBackgroundCollision(RoomEntity original, RoomEntity moved, RoomEntityBackgroundCollision fallback, int frame) { return liftThrow.sideViewPotBackgroundCollision(original, moved, fallback, frame); }

    /** Mirrors EntityThrownHandler's post-bounce func_003_75A2 collision pass. */
    void collideThrownEntityWithEntities(RoomEntity thrown, int frame) { liftThrow.collideThrownEntityWithEntities(thrown, frame); }

    RoomEntity advancePlacedBombEntity(RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision, int frame) { return liftThrow.advancePlacedBombEntity(entity, backgroundCollision, frame); }

    RoomEntity advanceEnemyBombEntity(RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision, int frame) { return liftThrow.advanceEnemyBombEntity(entity, backgroundCollision, frame); }

    /** Mirrors CuccoEntityHandler's B-slot-first Power Bracelet check. */
    boolean cuccoPowerBraceletHeld() { return liftThrow.cuccoPowerBraceletHeld(); }

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

    boolean moldormOrHeadOverlapsSword(RoomEntity entity,
                                                int swordX, int swordWidth,
                                                int swordY, int swordHeight) {
        if (RoomEntityCombatRules.overlapsSword(
                entity, swordX, swordWidth, swordY, swordHeight)) {
            return true;
        }
        if (entity.type() != ENTITY_MOLDORM) {
            return false;
        }
        if (enemyFlashCountdown[entity.slot()] != 0) {
            return false;
        }
        MoldormMotion.TailPosition tail = moldormMotion.vulnerableTail(entity.slot());
        RoomEntity tailCollisionEntity = withPositionAndVariant(
            entity, tail.x(), (tail.y() + entity.z()) & 0xFF, entity.spriteVariant());
        return RoomEntityCombatRules.overlapsSword(
            tailCollisionEntity, swordX, swordWidth, swordY, swordHeight);
    }

    /** Supplies the persistent world clock before this frame's entity pass. */
    void setBoomerangSoundCounterForFrame(int counter) { this.entityRuntimeConfiguration.setBoomerangSoundCounterForFrame(counter); }

    void playBoomerangSoundIfDue(RoomEntity entity) { projectiles.playBoomerangSoundIfDue(entity); }

    /** Advances entity $01 through the ROM boomerang handler's two states. */
    void advanceBoomerangEntity(int index, RoomEntity entity, int frame, int linkEntityX, int linkEntityY, int linkEntityZ, int linkMotionState, boolean handlerInteractive, boolean timersInteractive) { projectiles.advanceBoomerangEntity(index, entity, frame, linkEntityX, linkEntityY, linkEntityZ, linkMotionState, handlerInteractive, timersInteractive); }

    /** Advances entity {@code $DF} through the ROM sword-beam handler. */
    void advanceSwordBeamEntity(int index, RoomEntity entity, int frame, int linkEntityX, int linkEntityY, int romLinkDirection, boolean handlerInteractive) { projectiles.advanceSwordBeamEntity(index, entity, frame, linkEntityX, linkEntityY, romLinkDirection, handlerInteractive); }

    /** Advances entity {@code $04} through MagicRodFireballEntityHandler. */
    void advanceMagicRodFireballEntity(int index, RoomEntity entity, int frame, boolean handlerInteractive, boolean timersInteractive) { projectiles.advanceMagicRodFireballEntity(index, entity, frame, handlerInteractive, timersInteractive); }

    private boolean isMagicRodBurnableObject(RoomEntityObjectSample object) { return projectiles.isMagicRodBurnableObject(object); }

    /** Advances entity {@code $08} through MagicPowderSprinkleEntityHandler. */
    void advanceMagicPowderSprinkleEntity(int index, RoomEntity entity, int frame, int linkMotionState, boolean handlerInteractive, boolean timersInteractive) { projectiles.advanceMagicPowderSprinkleEntity(index, entity, frame, linkMotionState, handlerInteractive, timersInteractive); }

    void setTorchPaletteTransitionState(int targetAddress, boolean active) { this.entityRuntimeConfiguration.setTorchPaletteTransitionState(targetAddress, active); }

    static int magicPowderObjectLeft(RoomEntity entity) {
        return (((entity.x() + 0x07 - 0x08) & 0xFF) & 0xF0);
    }

    static int magicPowderObjectTop(RoomEntity entity) {
        return (((entity.y() + 0x07 - 0x10) & 0xFF) & 0xF0);
    }

    private boolean collideMagicPowderWithEntities(RoomEntity sprinkle, int frame) { return projectiles.collideMagicPowderWithEntities(sprinkle, frame); }

    /** Shared result path of ApplySwordIntersectionWithObjects ($7CAB). */
    private ProjectileObjectCollision projectileObjectCollision(RoomEntity entity, RoomEntityObjectSample object, int frame, int slot) { return projectiles.projectileObjectCollision(entity, object, frame, slot); }

    enum ProjectileObjectCollision {
        NONE, BLOCKED, UNLOAD
    }

    /** Common bank-$03 collision pass for entity {@code $DF}. */
    record ProjectileEntityCollision(boolean unload, boolean blocked) {}

    private ProjectileEntityCollision collideSwordBeamWithEntities(RoomEntity beam, int frame) { return projectiles.collideSwordBeamWithEntities(beam, frame); }

    /** Common bank-$03 collision pass with DAMAGE_TYPE_MAGIC_ROD ($0A). */
    private boolean collideMagicRodFireballWithEntities(RoomEntity fireball, int frame) { return projectiles.collideMagicRodFireballWithEntities(fireball, frame); }

    /**
     * Port of the common bank-$03 func_003_75A2 pass for player arrows. The
     * active arrow checks targets in descending slot order and uses the same
     * alternating frame cadence and unsigned twelve-pixel windows as the ROM.
     */
    boolean collidePlayerArrowWithEntities(RoomEntity arrow, int frame) { return projectiles.collidePlayerArrowWithEntities(arrow, frame); }

    /** Common bank-$03 collision pass for the active boomerang. */
    private boolean collideBoomerangWithEntities(RoomEntity boomerang, int frame) { return projectiles.collideBoomerangWithEntities(boomerang, frame); }

    private boolean ironMaskBlocksProjectile(RoomEntity target, int direction) { return projectiles.ironMaskBlocksProjectile(target, direction); }

    /**
     * Ports the BouncingBombite-specific portion of bank-$03
     * {@code func_003_75A2}. The ROM scans every target in descending slot
     * order, on alternating frames, and uses the active Bombite's transition
     * byte as the source-side explosion latch.
     */
    boolean collideBouncingBombiteWithEntities(RoomEntity source, int frame) { return projectiles.collideBouncingBombiteWithEntities(source, frame); }

    /**
     * Ports the Iron Mask target branch of bank-$03 {@code func_003_75A2} for
     * the active hookshot chain. Other hookshot target damage branches remain
     * owned by their entity-specific runtimes; this branch is special because
     * it changes the target's private state and creates a visible mask entity.
     */
    boolean collideHookshotWithEntities(RoomEntity hookshot, HookshotChainMotion.State hookshotState, int frame) { return projectiles.collideHookshotWithEntities(hookshot, hookshotState, frame); }

    void spawnIronMasksMask(int x, int y, int variant, int sourceHookshotSlot) { this.entityPlayerSpawns.spawnIronMasksMask(x, y, variant, sourceHookshotSlot); }

    private void applyPlayerArrowDamage(RoomEntity arrow, RoomEntity target) { projectiles.applyPlayerArrowDamage(arrow, target); }

    void applyPlayerProjectileDamage(RoomEntity target, int damageType, int speedX, int speedY) { projectiles.applyPlayerProjectileDamage(target, damageType, speedX, speedY); }

    int initialHealth(int type) { return this.roomInspection.initialHealth(type); }

    int contactDamage(int type) { return this.roomInspection.contactDamage(type); }

    void advanceHookshotBridgeEntity(int index, RoomEntity entity) {
        HookshotBridgeMotion.State bridgeState = hookshotBridgeMotion.state(entity.slot());
        if (bridgeState == null) {
            clearEntity(entity.slot());
            return;
        }

        HookshotBridgeMotion.ObjectCell cell = HookshotBridgeMotion.objectCell(bridgeState);
        HookshotBridgeMotion.Step step = hookshotBridgeMotion.advance(entity.slot());
        RoomEntity moved = withPositionAndVariant(entity, step.state().x(), step.state().y(),
            entity.spriteVariant());
        RoomEntityObjectSample object = objectQuery == null ? null : objectQuery.sample(moved);
        boolean clear = object != null && object.objectId() != OBJECT_HOOKSHOT_BRIDGE_REPLACEMENT
            && object.physicsFlag() == 0;
        hookshotBridgeUpdates.add(new HookshotBridgeUpdate(
            cell.objectLeft(), cell.objectTop(), bridgeState.direction(), !clear));
        if (clear) {
            clearEntity(entity.slot());
        } else {
            slots[index] = moved;
        }
    }

    /** Unloads a slot and returns the persistent first-eight load-order bit. */
    public int clearEntity(int slot) { return this.lifecycle.clearEntity(slot); }

    /** Creates the ROM's player-projectile entity type {@code $03}. */
    int spawnHookshotChain(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) { return this.entityPlayerSpawns.spawnHookshotChain(linkEntityX, linkEntityY, linkEntityZ, romDirection); }

    /** Spawns bank 3's temporary entity $06 at an indoor object-cell center. */
    int spawnPushedBlock(int x, int y) { return this.entityPlayerSpawns.spawnPushedBlock(x, y); }

    /** Spawns bank 0's temporary type-$05 entity already entering lifted status. */
    int spawnLiftedRoomObject(int x, int y, int romDirection, int spriteVariant) { return this.entityPlayerSpawns.spawnLiftedRoomObject(x, y, romDirection, spriteVariant); }

    void movePushedBlock(int slot, int x, int y) {
        if (slot < 0 || slot >= slots.length
            || slots[slot].type() != ENTITY_PUSHED_BLOCK
            || slots[slot].status() == EntityStatus.DISABLED) {
            return;
        }
        RoomEntity entity = slots[slot];
        slots[slot] = new RoomEntity(slot, entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(),
            entity.spriteVariant(), entity.entityFlipAttribute(), entity.spriteTileOffset(),
            entity.z(), -1, false);
    }

    /** $03:5269: after block movement, restore Link even during a jump. */
    void resolvePushedBlockLinkContact(int slot, int linkEntityX, int linkEntityY) {
        if (slot >= 0 && slot < slots.length && slots[slot].type() == ENTITY_PUSHED_BLOCK
            && slots[slot].status() == EntityStatus.ACTIVE
            && RoomEntityCombatRules.overlapsLink(slots[slot], linkEntityX, linkEntityY)) {
            requestLinkPush(slots[slot], EntityLinkCollisionRules.PUSHED_BLOCK);
        }
    }

    void removePushedBlock(int slot) {
        if (slot >= 0 && slot < slots.length && slots[slot].type() == ENTITY_PUSHED_BLOCK) {
            slots[slot] = RoomEntity.disabled(slot);
        }
    }

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

    void spawnMusicalNote(int linkEntityX, int linkEntityY, int animationPhase) { this.entityPlayerSpawns.spawnMusicalNote(linkEntityX, linkEntityY, animationPhase); }

    RoomEntity advanceMusicalNoteEntity(RoomEntity entity) {
        int slot = entity.slot();
        int inertia = (musicalNoteInertia[slot] - 1) & 0xFF;
        musicalNoteInertia[slot] = inertia;
        if (inertia == 0) {
            clearEntity(slot);
            return null;
        }

        int speedXAdjustment = (inertia & 0x10) != 0 ? -1 : 1;
        if ((inertia & 0x01) == 0) {
            musicalNoteSpeedX[slot] = (musicalNoteSpeedX[slot] + speedXAdjustment) & 0xFF;
        }
        int x = (entity.x() + signedByte(musicalNoteSpeedX[slot])) & 0xFF;
        int y = (entity.y() + signedByte(musicalNoteSpeedY[slot])) & 0xFF;
        return withPositionAndVariant(entity, x, y, entity.spriteVariant());
    }

    int activePlayerArrowCount() { return this.roomInspection.activePlayerArrowCount(); }

    /** Mirrors the player projectile handlers that feed wActiveProjectileCount. */
    int activeProjectileCount() { return this.roomInspection.activeProjectileCount(); }

    boolean boomerangActive() { return this.roomInspection.boomerangActive(); }

    int activeBoomerangSlot() { return this.roomInspection.activeBoomerangSlot(); }

    int boomerangDirection(int slot) { return this.roomInspection.boomerangDirection(slot); }

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

    int spawnTarinTransformationBomb(RoomEntity tarin) { return this.entityEnemySpawns.spawnTarinTransformationBomb(tarin); }

    boolean tarinTransformationHandlerInteractive() { return this.roomInspection.tarinTransformationHandlerInteractive(); }

    boolean tarinTransformationTimersInteractive() { return this.roomInspection.tarinTransformationTimersInteractive(); }

    /** Mirrors the state-$00 return gate reached after Genie's BossIntro call. */
    boolean genieState0Interactive(boolean creditsGameplay) { return this.roomInspection.genieState0Interactive(creditsGameplay); }

    /** Mirrors UpdateEntityTimers' gameplay and presentation gates. */
    boolean gameplayEntityTimersInteractive(boolean creditsGameplay) { return this.roomInspection.gameplayEntityTimersInteractive(creditsGameplay); }

    /** Creates the temporary type-$05 entity used by bombed bushes, grass, and pots. */
    int spawnLiftableRockSmash(int entityX, int entityY, int sourceSpriteVariant) { return this.entityPlayerSpawns.spawnLiftableRockSmash(entityX, entityY, sourceSpriteVariant); }

    /** Creates one of the four silent type-$05 rubble entities from bank-$36. */
    int spawnLiftableRockRubble(int entityX, int entityY) { return this.entityPlayerSpawns.spawnLiftableRockRubble(entityX, entityY); }

    void advanceGenieJar(RoomEntity jarEntity, int linkEntityY) { this.genie.advanceGenieJar(jarEntity, linkEntityY); }

    private RoomEntity advanceGenieJarAttract(RoomEntity jar) { return genie.advanceGenieJarAttract(jar); }

    void advanceGenieAppearance(RoomEntity appearance) { this.genie.advanceGenieAppearance(appearance); }

    void advanceGenieBody(RoomEntity body, int frameCounter, int linkEntityX, int linkEntityY, IntSupplier randomByteSupplier) { this.genie.advanceGenieBody(body, frameCounter, linkEntityX, linkEntityY, randomByteSupplier); }

    private int spawnGenieAimedFireball(RoomEntity body, int bodySlot, int linkEntityX, int linkEntityY) { return genie.spawnGenieAimedFireball(body, bodySlot, linkEntityX, linkEntityY); }

    void advanceGenieDestruction(RoomEntity genie, int frameCounter, IntSupplier randomByteSupplier) { this.genie.advanceGenieDestruction(genie, frameCounter, randomByteSupplier); }

    void advanceGenieSideProjectile(RoomEntity projectile, int frameCounter) { this.genie.advanceGenieSideProjectile(projectile, frameCounter); }

    void advanceGenieFireball(RoomEntity fireball, int frameCounter) { this.genie.advanceGenieFireball(fireball, frameCounter); }

    private int spawnGenieBody(GenieMotion.BodySpawnRequest request) { return genie.spawnGenieBody(request); }

    private int spawnGenieEntity(int x, int y, int privateState1, int transitionCountdown, int health, int physicsFlags, int hitboxFlags) { return genie.spawnGenieEntity(x, y, privateState1, transitionCountdown, health, physicsFlags, hitboxFlags); }

    int spawnLiftableRockAnimation(int entityX, int entityY, int sourceSpriteVariant, int initialCountdown, int physicsFlags, int soundId) { return this.entityPlayerSpawns.spawnLiftableRockAnimation(entityX, entityY, sourceSpriteVariant, initialCountdown, physicsFlags, soundId); }

    int spawnBombFromBombArrow(RoomEntity arrow) { return this.entityPlayerSpawns.spawnBombFromBombArrow(arrow); }

    void applyInitialBombArrowCandidateState(int slot, int romDirection) { this.bombInteractions.applyInitialBombArrowCandidateState(slot, romDirection); }

    void markArrowAsBombArrow(int slot) { this.bombInteractions.markArrowAsBombArrow(slot); }

    boolean isLoadedEntityOfType(int slot, int type) { return this.roomInspection.isLoadedEntityOfType(slot, type); }

    boolean bombActive() { return this.roomInspection.bombActive(); }

    int bombDirection(int slot) { return this.roomInspection.bombDirection(slot); }

    int bombPrivateCountdown1ForTest(int slot) { return this.roomInspection.bombPrivateCountdown1ForTest(slot); }

    void setBombTransitionCountdownForTest(int slot, int value) { this.entityDebugConfiguration.setBombTransitionCountdownForTest(slot, value); }

    boolean spawnHookshotBridge(int bridgeX, int bridgeY, int direction, int transitionCountdown) { return this.entityPlayerSpawns.spawnHookshotBridge(bridgeX, bridgeY, direction, transitionCountdown); }

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

    int pokeySpeedX(int slot) { return this.roomInspection.pokeySpeedX(slot); }

    int pokeySpeedY(int slot) { return this.roomInspection.pokeySpeedY(slot); }

    int pokeyTransitionCountdown(int slot) { return this.roomInspection.pokeyTransitionCountdown(slot); }

    int hookshotEntityState(int slot) { return this.roomInspection.hookshotEntityState(slot); }

    boolean hookshotWallCollisionPending(int slot) { return this.roomInspection.hookshotWallCollisionPending(slot); }

    public RoomEntitySnapshot snapshot() { return this.roomInspection.snapshot(); }

    /** Applies the position and variant writes made by MarinEntityHandler_Indoor. */
    public boolean applyMarinWakeUpPresentation(int x, int y, int spriteVariant) {
        for (int slot = 0; slot < slots.length; slot++) {
            RoomEntity entity = slots[slot];
            if (entity.loaded() && entity.type() == 0x3E) {
                slots[slot] = new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
                    x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(), spriteVariant,
                    entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z(),
                    entity.deathSpriteVariant(), entity.powerRecoilDeath());
                return true;
            }
        }
        return false;
    }

    void updateHouseTarinPresentation(int linkX, int linkY) {
        for (int slot = 0; slot < slots.length; slot++) {
            RoomEntity entity = slots[slot];
            if (entity.loaded() && entity.type() == ENTITY_TARIN) {
                int dx = (byte) (linkX - entity.x());
                int dy = (byte) (linkY - entity.y());
                int direction = Math.abs(dy) >= Math.abs(dx) ? (dy < 0 ? 2 : 3) : (dx < 0 ? 1 : 0);
                slots[slot] = withVariant(entity, direction);
            }
        }
    }

    void updateHookshotChainOam(int linkEntityX, int linkEntityY, int frameCounter) {
        int slot = hookshotSlot();
        if (slot < 0 || !slots[slot].loaded()) {
            hookshotChainOam = List.of();
            return;
        }
        RoomEntity chain = slots[slot];
        hookshotChainOam = HookshotChainOam.entries(
            chain.x(), chain.y(), linkEntityX, linkEntityY, frameCounter);
    }

    void setLinkVelocityZ(int velocity) { this.entityRuntimeConfiguration.setLinkVelocityZ(velocity); }

    void updateBowWowChain(int frame) {
        List<BowWowChain.Entry> entries = new ArrayList<>();
        // The handler's dialogue/inventory/transition branches still run the
        // chain. Its sequence check only returns early on the unpaused path.
        if (transitionSequenceCounter != 4 && !roomTransitionActive && !dialogActive
            && !inventoryAppearing && !witchGotItemPresentationActive()) {
            bowWowChainOam = List.of();
            return;
        }
        boolean moving = (currentLinkSpeedX | currentLinkSpeedY | currentLinkVelocityZ) != 0;
        for (int slot = 0; slot < slots.length; slot++) {
            RoomEntity entity = slots[slot];
            if (entity.type() != ENTITY_BOW_WOW || entity.status() != EntityStatus.ACTIVE) continue;
            slots[slot] = bowWowMotion.advanceChain(entity, bowWowState, moving);
            entries.addAll(bowWowMotion.chainEntries(slot, bowWowState, frame));
        }
        bowWowChainOam = List.copyOf(entries);
    }

    void updatePincerBodyOam() {
        List<PincerBodyOam.Entry> entries = new ArrayList<>();
        for (RoomEntity entity : slots) {
            if (!entity.loaded() || entity.type() != ENTITY_PINCER) {
                continue;
            }
            entries.addAll(PincerBodyOam.entries(entity.slot(),
                pincerMotion.holeX(entity.slot()), pincerMotion.holeY(entity.slot()),
                entity.x(), entity.y(), pincerMotion.state(entity.slot())));
        }
        pincerBodyOam = List.copyOf(entries);
    }

    void updateWingedOctorokOam() {
        List<WingedOctorokOam.Entry> entries = new ArrayList<>();
        for (RoomEntity entity : slots) {
            if (!entity.loaded() || entity.type() != ENTITY_WINGED_OCTOROK) {
                continue;
            }
            entries.addAll(WingedOctorokOam.entries(entity.slot(),
                wingedOctorokMotion.privateState2(entity.slot()),
                entity.x(), entity.y(), entity.z()));
        }
        wingedOctorokOam = List.copyOf(entries);
    }

    List<WingedOctorokOam.Entry> wingedOctorokOam() { return this.roomInspection.wingedOctorokOam(); }

    int wingedOctorokState(int slot) { return this.roomInspection.wingedOctorokState(slot); }

    int wingedOctorokSpeedX(int slot) { return this.roomInspection.wingedOctorokSpeedX(slot); }

    int wingedOctorokSpeedY(int slot) { return this.roomInspection.wingedOctorokSpeedY(slot); }

    int wingedOctorokSpeedZ(int slot) { return this.roomInspection.wingedOctorokSpeedZ(slot); }

    int bushCrawlerState(int slot) { return this.roomInspection.bushCrawlerState(slot); }

    int bushCrawlerPrivateState1(int slot) { return this.roomInspection.bushCrawlerPrivateState1(slot); }

    int bushCrawlerPrivateState4(int slot) { return this.roomInspection.bushCrawlerPrivateState4(slot); }

    int bushCrawlerSpeedX(int slot) { return this.roomInspection.bushCrawlerSpeedX(slot); }

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

    int hinoxSpeedXForTest(int slot) { return this.roomInspection.hinoxSpeedXForTest(slot); }

    int hinoxSpeedYForTest(int slot) { return this.roomInspection.hinoxSpeedYForTest(slot); }

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

    RoomEntity firstLoadedEntityOfType(int type) { return this.roomInspection.firstLoadedEntityOfType(type); }

    /** Mirrors bank-$03 ApplyLinkCollisionWithEnemy's Goomba stomp branch. */
    boolean canGoombaStomp(RoomEntity entity, int linkEntityX, int linkEntityY, boolean linkAirborne, int linkVerticalVelocity, boolean linkInteractive, int frameCounter) { return this.roomInspection.canGoombaStomp(entity, linkEntityX, linkEntityY, linkAirborne, linkVerticalVelocity, linkInteractive, frameCounter); }

    private boolean hasNoGroundInteractionOverride(int slot) { return this.roomInspection.hasNoGroundInteractionOverride(slot); }

    boolean hasNoGroundInteraction(RoomEntity entity) { return this.roomInspection.hasNoGroundInteraction(entity); }

    RoomEntity applyZolSplit(RoomEntity original, RoomEntity updated, ZolGelMotion.Split split) { return this.lifecycle.applyZolSplit(original, updated, split); }

    void handleTerminalEnemyDeath(RoomEntity entity, IntSupplier randomByteSupplier) { this.bossRewards.handleTerminalEnemyDeath(entity, randomByteSupplier); }

    /** Ports {@code RollingBonesEntityHandler}'s long miniboss destruction sequence. */
    void advanceRollingBonesDestruction(RoomEntity entity, IntSupplier randomByteSupplier) { this.bossRewards.advanceRollingBonesDestruction(entity, randomByteSupplier); }

    /** Bank $15:7C37..7CA5: two death waits, then DidKillEnemy. */
    void advanceMoblinKingDestruction(RoomEntity source, int frame, IntSupplier random) { this.bossRewards.advanceMoblinKingDestruction(source, frame, random); }

    void advanceMoldormDestruction(RoomEntity source, int frameCounter) { this.bossRewards.advanceMoldormDestruction(source, frameCounter); }

    private void advanceMoldormBossExplosion(RoomEntity source) { this.bossRewards.advanceMoldormBossExplosion(source); }

    void spawnMoldormHeartContainer(RoomEntity source) { this.bossRewards.spawnMoldormHeartContainer(source); }

    private void createMoldormPoof(RoomEntity source, int x, int y) { this.bossRewards.createMoldormPoof(source, x, y); }

    void advanceCollectedHeartContainer(RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ) { this.bossRewards.advanceCollectedHeartContainer(entity, linkEntityX, linkEntityY, linkZ); }

    void startSwordPickup(int slot) {
        swordPickupSequenceActive[slot] = true;
        swordPickupState[slot] = 0;
        enemyTransitionCountdown[slot] = 0xA0;
        slowTimerInitialized[slot] = true;
        pendingMusicTrack = 0x0F;
    }

    void startToadstoolPickup(int slot) {
        toadstoolPickupSequenceActive[slot] = true;
        enemyTransitionCountdown[slot] = 0x68;
        pendingMusicTrack = 0x10;
    }

    void startInstrumentPickup(int slot) { rewards.startInstrumentPickup(slot); }

    void advanceKidnapWarningKid(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY) { story.advanceKidnapWarningKid(entity, frameCounter, linkEntityX, linkEntityY); }

    void advanceMadamMeowMeow(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne) { story.advanceMadamMeowMeow(entity, frameCounter, linkEntityX, linkEntityY, linkAirborne); }

    void advanceOwlStatue(RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ, int linkMotionState, boolean linkAirborne) { story.advanceOwlStatue(entity, linkEntityX, linkEntityY, linkZ, linkMotionState, linkAirborne); }

    void setOwlStatueDialogId(int globalDialogId) { this.entityRuntimeConfiguration.setOwlStatueDialogId(globalDialogId); }

    void advanceRichard(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne) { story.advanceRichard(entity, frameCounter, linkEntityX, linkEntityY, linkAirborne); }

    boolean sourceNpcInteractionAllowed(int xDistance, int yDistance, boolean linkAirborne, int entityDirection) { return this.roomInspection.sourceNpcInteractionAllowed(xDistance, yDistance, linkAirborne, entityDirection); }

    void advanceInstrumentPickup(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY, int linkZ, IntSupplier randomByteSupplier) { rewards.advanceInstrumentPickup(entity, frameCounter, linkEntityX, linkEntityY, linkZ, randomByteSupplier); }

    public boolean heartPiecePickupActive() { return this.roomInspection.heartPiecePickupActive(); }

    public HeartPieceVisual heartPieceVisual() { return this.roomInspection.heartPieceVisual(); }

    public void setHeartPiecesCount(int count) { heartPiecesCount = count; }

    public List<HeartPiecePickupMotion.Update> consumeHeartPieceUpdates() { return this.effects.consumeHeartPieceUpdates(); }

    void advanceHeartPiecePickup(RoomEntity entity, int x, int y, int z) { pickups.advanceHeartPiecePickup(entity, x, y, z); }

    public String instrumentTrace() { return this.roomInspection.instrumentTrace(); }

    public String moblinKingTrace() { return this.roomInspection.moblinKingTrace(); }

    public InstrumentWarpVisual instrumentWarpVisual() { return this.roomInspection.instrumentWarpVisual(); }

    private void spawnInstrumentParticle(RoomEntity source, int offsetIndex, int variant) { rewards.spawnInstrumentParticle(source, offsetIndex, variant); }

    void advanceInstrumentParticle(RoomEntity entity) { rewards.advanceInstrumentParticle(entity); }

    void advanceWitchGotItemPresentation(int slot) { story.advanceWitchGotItemPresentation(slot); }

    void presentWitchGotItem(int slot) {
        pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
        pendingLinkHeldItemPoseRequests.add(new LinkHeldItemPoseRequest(slot));
    }

    /** ProcessPowerUp ($03:6400) and ApplyGotItem ($00:1033). */
    void advancePowerUpPickup(RoomEntity entity, int frame, int linkX, int linkY, int linkZ) { rewards.advancePowerUpPickup(entity, frame, linkX, linkY, linkZ); }

    void advanceToadstoolPickup(RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ) { rewards.advanceToadstoolPickup(entity, linkEntityX, linkEntityY, linkZ); }

    void advanceSwordPickup(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY, int linkZ) { rewards.advanceSwordPickup(entity, frameCounter, linkEntityX, linkEntityY, linkZ); }

    void holdSwordAboveLink(RoomEntity entity, int linkEntityX, int linkEntityY,
                                    int linkZ, boolean finalPose) {
        int slot = entity.slot();
        pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
        pendingLinkHeldItemPoseRequests.add(new LinkHeldItemPoseRequest(slot));
        int x = finalPose ? (linkEntityX - 4) & 0xFF : linkEntityX;
        RoomEntity held = withPositionAndVariant(entity, x,
            (linkEntityY - 0x0C) & 0xFF, entity.spriteVariant());
        slots[slot] = withZ(held, linkZ & 0xFF);
    }

    void advanceOwlEvent(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY, boolean handlerLinkInteractive) { story.advanceOwlEvent(entity, frameCounter, linkEntityX, linkEntityY, handlerLinkInteractive); }

    private void advanceDepartingOwl(RoomEntity entity, int slot, int frameCounter) { story.advanceDepartingOwl(entity, slot, frameCounter); }

    RoomEntity owlPresentation(RoomEntity entity, int frameCounter) { return this.roomInspection.owlPresentation(entity, frameCounter); }

    void setOwlDialogResolver(IntUnaryOperator resolver) { this.entityRuntimeConfiguration.setOwlDialogResolver(resolver); }

    void setOwlInstrumentFlags(int instrument1Flags, int instrument2Flags) { this.entityRuntimeConfiguration.setOwlInstrumentFlags(instrument1Flags, instrument2Flags); }

    void setOwlDefaultMusicResolver(IntUnaryOperator resolver) { this.entityRuntimeConfiguration.setOwlDefaultMusicResolver(resolver); }

    void blockLinkForOwl(int slot) {
        pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
    }

    /** Mirrors Bow-Wow's label_005_4335 target-resolution branches. */
    void handleBowWowTargetContact(RoomEntity source, BowWowMotion.TargetContact contact, IntSupplier randomByteSupplier) { this.lifecycle.handleBowWowTargetContact(source, contact, randomByteSupplier); }

    /**
     * Ports the boss-specific death handlers that eventually create entity
     * {@code $30}. Boss entities stay on their active handler while DYING, so
     * they must not enter the ordinary EnemyDropResolver path.
     */
    void advanceBossKeyDropProducer(RoomEntity source, IntSupplier randomByteSupplier) { this.bossRewards.advanceBossKeyDropProducer(source, randomByteSupplier); }

    void spawnBossKeyDrop(RoomEntity source, int spriteVariant, int z, int speedZ, int privateCountdown1) { pickups.spawnBossKeyDrop(source, spriteVariant, z, speedZ, privateCountdown1); }

    int spawnKeyDropAt(int itemType, int x, int y, int spriteVariant, int z, int speedZ, int privateCountdown1) { return pickups.spawnKeyDropAt(itemType, x, y, spriteVariant, z, speedZ, privateCountdown1); }

    int enemyDropHealthGroup(int entityType) { return this.roomInspection.enemyDropHealthGroup(entityType); }

    void spawnEnemyDrop(RoomEntity source, int itemType) { pickups.spawnEnemyDrop(source, itemType); }

    boolean spawnKanaletWallMoblin(RoomEntity wall) { return this.entityEnemySpawns.spawnKanaletWallMoblin(wall); }

    boolean kanaletCrowTakeoffTriggered(RoomEntity crow) {
        for (RoomEntity candidate : slots) {
            if (!candidate.loaded()) {
                continue;
            }
            if (candidate.type() == ENTITY_LIFTABLE_ROCK
                && liftableRockSmashActive[candidate.slot()]
                && liftableRockSmashSourceVariant[candidate.slot()]
                    == LIFTABLE_ROCK_SMASH_MODE_ROCK) {
                int distanceX = Math.abs(signedByte((crow.x() - candidate.x()) & 0xFF));
                int distanceY = signedByte(
                    ((crow.y() - crow.z()) - candidate.y()) & 0xFF);
                if (distanceX < 0x10 && distanceY >= -0x28 && distanceY < 0x28) {
                    return true;
                }
                continue;
            }
            if (candidate.type() != ENTITY_BOMB) {
                continue;
            }
            int countdown = enemyTransitionCountdown[candidate.slot()] & 0xFF;
            if (enemyFlashCountdown[candidate.slot()] == 0
                || countdown >= 0x22) {
                continue;
            }
            int distanceX = Math.abs(signedByte((crow.x() - candidate.x()) & 0xFF));
            int distanceY = Math.abs(signedByte(
                ((crow.y() - crow.z()) - candidate.y()) & 0xFF));
            if (distanceX < 0x20 && distanceY < 0x20) {
                return true;
            }
        }
        return false;
    }

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

    void spawnPairoddProjectile(RoomEntity source, int linkEntityX, int linkEntityY) { this.entityEnemySpawns.spawnPairoddProjectile(source, linkEntityX, linkEntityY); }

    void spawnGopongaProjectile(RoomEntity source, GiantGopongaMotion.ProjectileSpawn spawn) { this.entityEnemySpawns.spawnGopongaProjectile(source, spawn); }

    void spawnZoraProjectile(ZoraMotion.ProjectileSpawn spawn) { this.entityEnemySpawns.spawnZoraProjectile(spawn); }

    /** Mirrors ZombieEntityHandler's SpawnNewEntityInRange call with E=$05. */
    void spawnZombieChild(ZombieMotion.SpawnRequest spawn) { this.entityEnemySpawns.spawnZombieChild(spawn); }

    /** Mirrors PokeyEntityHandler's SpawnNewEntity plus its detached-segment setup. */
    void spawnPokeySegment(PokeyMotion.SegmentSpawn spawn) { this.entityEnemySpawns.spawnPokeySegment(spawn); }

    void spawnWizrobeProjectile(RoomEntity source, WizrobeMotion.ProjectileSpawn spawn) { this.entityEnemySpawns.spawnWizrobeProjectile(source, spawn); }

    boolean spawnEvasiveClone(RoomEntity source, StalfosEvasiveMotion.CloneRequest request) { return this.entityEnemySpawns.spawnEvasiveClone(source, request); }

    void spawnEnemyProjectile(RoomEntity source, RoamingEnemyMotion.LaunchRequest request) { this.entityEnemySpawns.spawnEnemyProjectile(source, request); }

    void spawnEnemyProjectile(RoomEntity source, RoamingEnemyMotion.LaunchRequest request, int direction) { this.entityEnemySpawns.spawnEnemyProjectile(source, request, direction); }

    void spawnWingedOctorokRock(RoomEntity source, int direction) { this.entityEnemySpawns.spawnWingedOctorokRock(source, direction); }

    /**
     * Ports BushCrawlerEntityHandler's Power Bracelet branch: the source
     * slot becomes a lifted type-$05 entity and a fresh type-$BB slot starts
     * the private-state-$02 wandering handler at the same position.
     */
    boolean beginBushCrawlerLift(int index, RoomEntity source, int privateState4, int romLinkDirection) { return liftThrow.beginBushCrawlerLift(index, source, privateState4, romLinkDirection); }

    /** Ports CuccoState2Handler's outdoor angry-Cucco spawn setup. */
    boolean spawnCuccoAngry(CuccoMotion.SpawnRequest request) { return this.entityEnemySpawns.spawnCuccoAngry(request); }

    void spawnLaserSensor(RoomEntity parent) { this.entityEnemySpawns.spawnLaserSensor(parent); }

    boolean spawnLaserBeam(RoomEntity parent) { return this.entityEnemySpawns.spawnLaserBeam(parent); }

    EntitySpriteDefinition spriteDefinitionFor(int entityType) { return this.roomInspection.spriteDefinitionFor(entityType); }

    EntitySpriteDefinition spriteDefinitionForBushCrawlerState(int privateState1) { return this.roomInspection.spriteDefinitionForBushCrawlerState(privateState1); }

    EntitySpriteDefinition spriteDefinitionForPokeySegment() { return this.roomInspection.spriteDefinitionForPokeySegment(); }

    EntitySpriteDefinition spriteDefinitionForBushCrawlerCrawlState(int privateState4) { return this.roomInspection.spriteDefinitionForBushCrawlerCrawlState(privateState4); }

    private EntityRoomLoader.RoomTable spriteRoomTable() { return this.roomInspection.spriteRoomTable(); }

    EntitySpriteDefinition spriteDefinitionForChest(int itemType) {
        if (itemType == ChestContentsTable.CHEST_ZOL) {
            return EntitySpriteDefinition.unsupported(ENTITY_CHEST_WITH_ITEM);
        }
        if (spriteHandlers == null) {
            return EntitySpriteDefinition.unsupported(ENTITY_CHEST_WITH_ITEM);
        }
        int roomId = spriteSelection == null ? -1 : spriteSelection.roomId();
        if (entityMapId >= 0 && roomId >= 0) {
            return spriteHandlers.forChestState(entityMapId, roomId, itemType);
        }
        return spriteHandlers.forEntityType(ENTITY_CHEST_WITH_ITEM,
            spriteSelection == null
                ? (indoorRoom ? EntityRoomLoader.RoomTable.INDOORS_A
                    : EntityRoomLoader.RoomTable.OVERWORLD)
                : spriteSelection.roomTable(), entityMapId);
    }

    void initializeChestEntity(RoomEntity entity) { pickups.initializeChestEntity(entity); }

    /** Returns true when the source handler unloaded the chest this frame. */
    boolean advanceChestEntity(int index, RoomEntity entity) { return pickups.advanceChestEntity(index, entity); }

    private void spawnChestZol(RoomEntity chest) { pickups.spawnChestZol(chest); }

    void finalizePendingBombPresentations() { this.bombInteractions.finalizePendingBombPresentations(); }

    BombMotion.Decision advanceBombEntity( int index, RoomEntity entity, List<EntityProjectileEvent> projectileEvents, int linkEntityX, int linkEntityY, EnemyProjectileCollision.LinkState projectileLinkState) { return this.bombInteractions.advanceBombEntity(index, entity, projectileEvents, linkEntityX, linkEntityY, projectileLinkState); }

    void advanceLiftableRockSmash(int index, RoomEntity entity, int frameCounter) { liftThrow.advanceLiftableRockSmash(index, entity, frameCounter); }

    static int liftableRockSmashVariant(int sourceVariant, boolean swampLeaves,
                                                int countdown) {
        if (sourceVariant == LIFTABLE_ROCK_SMASH_MODE_ROCK) {
            int frame = (countdown & 0x0C) >>> 2;
            return EntitySpriteHandlerCatalog.LIFTABLE_ROCK_SMASHED_ROCK_VARIANT_BASE + frame;
        }
        int frame = (((countdown & 0x1C) ^ 0x1C) >>> 2);
        int base = swampLeaves
            ? EntitySpriteHandlerCatalog.LIFTABLE_ROCK_CUT_LEAVES_SWAMP_VARIANT_BASE
            : EntitySpriteHandlerCatalog.LIFTABLE_ROCK_CUT_LEAVES_VARIANT_BASE;
        return base + frame;
    }

    private void queueBombExplosionInteractions( RoomEntity bomb, BombMotion.Decision decision, List<EntityProjectileEvent> projectileEvents, int linkEntityX, int linkEntityY, EnemyProjectileCollision.LinkState projectileLinkState) { this.bombInteractions.queueBombExplosionInteractions(bomb, decision, projectileEvents, linkEntityX, linkEntityY, projectileLinkState); }

    /** Mirrors BombExplosionHandler's separate privateState4 enemy-bomb path. */
    private void queueEnemyBombLinkCollision( RoomEntity bomb, List<EntityProjectileEvent> projectileEvents, int linkEntityX, int linkEntityY, EnemyProjectileCollision.LinkState projectileLinkState) { this.bombInteractions.queueEnemyBombLinkCollision(bomb, projectileEvents, linkEntityX, linkEntityY, projectileLinkState); }

    /**
     * Ports the damage-table part of CheckExplosionInteractionWithEntities.
     * The ROM writes the source-relative length-$30 recoil after the generic
     * damage routine, even when the selected damage-table value is zero or a
     * source-specific special value.
     */
    private void applyBombDamage(RoomEntity bomb, RoomEntity target) { this.bombInteractions.applyBombDamage(bomb, target); }

    RoomEntity renderLiftedEntity(RoomEntity entity, int frameCounter) { return liftThrow.renderLiftedEntity(entity, frameCounter); }

    RoomEntity renderLiftedBomb(RoomEntity entity) { return liftThrow.renderLiftedBomb(entity); }

    boolean tryLiftBombIfRequested(int index, BombMotion.Decision decision, int linkEntityX, int linkEntityY, int linkZ, int romLinkDirection) { return liftThrow.tryLiftBombIfRequested(index, decision, linkEntityX, linkEntityY, linkZ, romLinkDirection); }

    /** Mirrors EntityStunnedHandler's recoil/bounce/clear-speed tail for a bomb. */
    RoomEntity applyBombStunnedPostActiveWork( RoomEntity stunned, RoomEntityBackgroundCollision backgroundCollision, int frame) { return this.bombInteractions.applyBombStunnedPostActiveWork(stunned, backgroundCollision, frame); }

    EntitySpriteDefinition spriteDefinitionForEvasiveState( EntitySpriteDefinition fallback, int privateState1) { return this.roomInspection.spriteDefinitionForEvasiveState(fallback, privateState1); }

    RoomEntity refreshColorShellDisplay(RoomEntity entity, EntityStatus status) { return this.roomInspection.refreshColorShellDisplay(entity, status); }

    void finishBurning(RoomEntity entity) { this.lifecycle.finishBurning(entity); }

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

    int keeseState(int slot) { return this.enemyInspection.keeseState(slot); }

    int keeseTransitionCountdown(int slot) { return this.enemyInspection.keeseTransitionCountdown(slot); }

    int keeseAngle(int slot) { return this.enemyInspection.keeseAngle(slot); }

    int keeseSpeedX(int slot) { return this.enemyInspection.keeseSpeedX(slot); }

    int keeseSpeedY(int slot) { return this.enemyInspection.keeseSpeedY(slot); }

    int octorokState(int slot) { return this.enemyInspection.octorokState(slot); }

    int octorokTransitionCountdown(int slot) { return this.enemyInspection.octorokTransitionCountdown(slot); }

    int octorokDirection(int slot) { return this.enemyInspection.octorokDirection(slot); }

    int octorokSpeedX(int slot) { return this.enemyInspection.octorokSpeedX(slot); }

    int octorokSpeedY(int slot) { return this.enemyInspection.octorokSpeedY(slot); }

    int ironMaskState(int slot) { return this.enemyInspection.ironMaskState(slot); }

    int ironMaskTransitionCountdown(int slot) { return this.enemyInspection.ironMaskTransitionCountdown(slot); }

    int ironMaskDirection(int slot) { return this.enemyInspection.ironMaskDirection(slot); }

    int ironMaskSpeedX(int slot) { return this.enemyInspection.ironMaskSpeedX(slot); }

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

    int pairoddState(int slot) { return this.enemyInspection.pairoddState(slot); }

    int pairoddTransitionCountdown(int slot) { return this.enemyInspection.pairoddTransitionCountdown(slot); }

    int pairoddDirection(int slot) { return this.enemyInspection.pairoddDirection(slot); }

    int pairoddProjectileSpeedX(int slot) { return this.enemyInspection.pairoddProjectileSpeedX(slot); }

    int pairoddProjectileSpeedY(int slot) { return this.enemyInspection.pairoddProjectileSpeedY(slot); }

    int pairoddProjectileDirection(int slot) { return this.enemyInspection.pairoddProjectileDirection(slot); }

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

    int blooperPrivateCountdown3(int slot) { return this.enemyInspection.blooperPrivateCountdown3(slot); }

    int pincerState(int slot) { return this.enemyInspection.pincerState(slot); }

    int pincerHoleX(int slot) { return this.enemyInspection.pincerHoleX(slot); }

    int pincerHoleY(int slot) { return this.enemyInspection.pincerHoleY(slot); }

    int pincerLungeVariant(int slot) { return this.enemyInspection.pincerLungeVariant(slot); }

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

    void setZolPrivateCountdown1ForTest(int slot, int value) { this.entityDebugConfiguration.setZolPrivateCountdown1ForTest(slot, value); }

    int hidingZolState(int slot) { return this.enemyInspection.hidingZolState(slot); }

    int hidingZolTransitionCountdown(int slot) { return this.enemyInspection.hidingZolTransitionCountdown(slot); }

    int hidingZolPrivateState1(int slot) { return this.enemyInspection.hidingZolPrivateState1(slot); }

    int hidingZolSpeedX(int slot) { return this.enemyInspection.hidingZolSpeedX(slot); }

    int hidingZolSpeedY(int slot) { return this.enemyInspection.hidingZolSpeedY(slot); }

    int hidingZolSpeedZ(int slot) { return this.enemyInspection.hidingZolSpeedZ(slot); }

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

    int wizrobeProjectileDirection(int slot) { return this.enemyInspection.wizrobeProjectileDirection(slot); }

    void setWizrobeProjectileForTest(int slot, int newSpeedX, int newSpeedY, int direction) { this.entityDebugConfiguration.setWizrobeProjectileForTest(slot, newSpeedX, newSpeedY, direction); }

    int peaHatState(int slot) { return this.enemyInspection.peaHatState(slot); }

    int peaHatSlowTransitionCountdown(int slot) { return this.enemyInspection.peaHatSlowTransitionCountdown(slot); }

    int peaHatPrivateState1(int slot) { return this.enemyInspection.peaHatPrivateState1(slot); }

    int peaHatPrivateState2(int slot) { return this.enemyInspection.peaHatPrivateState2(slot); }

    int peaHatPrivateState3(int slot) { return this.enemyInspection.peaHatPrivateState3(slot); }

    int peaHatPrivateState4(int slot) { return this.enemyInspection.peaHatPrivateState4(slot); }

    int peaHatSpeedX(int slot) { return this.enemyInspection.peaHatSpeedX(slot); }

    int peaHatSpeedY(int slot) { return this.enemyInspection.peaHatSpeedY(slot); }

    int armosState(int slot) { return this.enemyInspection.armosState(slot); }

    int armosTransitionCountdown(int slot) { return this.enemyInspection.armosTransitionCountdown(slot); }

    int armosSpeedX(int slot) { return this.enemyInspection.armosSpeedX(slot); }

    int armosSpeedY(int slot) { return this.enemyInspection.armosSpeedY(slot); }

    int armosKnightState(int slot) { return this.enemyInspection.armosKnightState(slot); }

    int armosKnightTransitionCountdown(int slot) { return this.enemyInspection.armosKnightTransitionCountdown(slot); }

    int armosKnightSpeedX(int slot) { return this.enemyInspection.armosKnightSpeedX(slot); }

    int armosKnightSpeedY(int slot) { return this.enemyInspection.armosKnightSpeedY(slot); }

    int armosKnightSpeedZ(int slot) { return this.enemyInspection.armosKnightSpeedZ(slot); }

    int armosKnightPrivateCountdown1(int slot) { return this.enemyInspection.armosKnightPrivateCountdown1(slot); }

    int armosKnightInertia(int slot) { return this.enemyInspection.armosKnightInertia(slot); }

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

    int geniePrivateState1ForTest(int slot) { return this.enemyInspection.geniePrivateState1ForTest(slot); }

    void setGeniePrivateState1ForTest(int slot, int value) { this.entityDebugConfiguration.setGeniePrivateState1ForTest(slot, value); }

    int genieActiveStateForTest(int slot) { return this.enemyInspection.genieActiveStateForTest(slot); }

    void setGenieActiveStateForTest(int slot, int value) { this.entityDebugConfiguration.setGenieActiveStateForTest(slot, value); }

    int geniePrivateState3ForTest(int slot) { return this.enemyInspection.geniePrivateState3ForTest(slot); }

    void setGeniePrivateState3ForTest(int slot, int value) { this.entityDebugConfiguration.setGeniePrivateState3ForTest(slot, value); }

    int geniePrivateCountdown1ForTest(int slot) { return this.enemyInspection.geniePrivateCountdown1ForTest(slot); }

    void setGeniePrivateCountdown1ForTest(int slot, int value) { this.entityDebugConfiguration.setGeniePrivateCountdown1ForTest(slot, value); }

    int geniePrivateCountdown2ForTest(int slot) { return this.enemyInspection.geniePrivateCountdown2ForTest(slot); }

    int genieSpeedXForTest(int slot) { return this.enemyInspection.genieSpeedXForTest(slot); }

    int genieSpeedZForTest(int slot) { return this.enemyInspection.genieSpeedZForTest(slot); }

    void setGeniePrivateState4ForTest(int slot, int value) { this.entityDebugConfiguration.setGeniePrivateState4ForTest(slot, value); }

    int geniePrivateState4ForTest(int slot) { return this.enemyInspection.geniePrivateState4ForTest(slot); }

    int rollingBonesBarStateForTest(int slot) { return this.enemyInspection.rollingBonesBarStateForTest(slot); }

    int rollingBonesBarSpeedXForTest(int slot) { return this.enemyInspection.rollingBonesBarSpeedXForTest(slot); }

    void setPhysicsFlagsForTest(int slot, int value) { this.entityDebugConfiguration.setPhysicsFlagsForTest(slot, value); }

    void setSpriteVariantForTest(int slot, int variant) { this.entityDebugConfiguration.setSpriteVariantForTest(slot, variant); }

    void setThrownDirection(int slot, int value) { this.entityRuntimeConfiguration.setThrownDirection(slot, value); }

    void setLedgeTransitionTimer(int slot, int value) { this.entityRuntimeConfiguration.setLedgeTransitionTimer(slot, value); }

    void setThrownDirectionForTest(int slot, int value) { this.entityDebugConfiguration.setThrownDirectionForTest(slot, value); }

    void setLedgeTransitionTimerForTest(int slot, int value) { this.entityDebugConfiguration.setLedgeTransitionTimerForTest(slot, value); }

    static void validateCountdownTestValue(int slot, int value) {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException("Countdown must be an unsigned byte: " + value);
        }
    }

    int keyDropTransitionCountdown(int slot) { return this.enemyInspection.keyDropTransitionCountdown(slot); }

    void setKeyDropTransitionCountdownForTest(int slot, int value) { this.entityDebugConfiguration.setKeyDropTransitionCountdownForTest(slot, value); }

    void setHidingSlimeKeyTransitionCountdownForTest(int slot, int value) { this.entityDebugConfiguration.setHidingSlimeKeyTransitionCountdownForTest(slot, value); }

    void setGoldenLeavesCountForTest(int value) { this.entityDebugConfiguration.setGoldenLeavesCountForTest(value); }

    void setGoldenLeavesCount(int value) { this.entityRuntimeConfiguration.setGoldenLeavesCount(value); }

    int options1(int slot) {
        if (slot < 0 || slot >= slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        if (entityOptions1Override[slot] >= 0) {
            return entityOptions1Override[slot];
        }
        if (isGhiniType(slots[slot].type())) {
            return GHINI_OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_BOMB) {
            return BOMB_OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_BOMBER) {
            return BOMBER_OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_MAD_BOMBER) {
            return MAD_BOMBER_OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_WIZROBE_PROJECTILE) {
            return WIZROBE_PROJECTILE_OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_SWORD_SHIELD_PICKUP) {
            return ENTITY_OPT1_SPLASH_IN_WATER | ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        }
        if (slots[slot].type() == ENTITY_KEY_DROP_POINT) {
            return KEY_DROP_POINT_OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_ARMOS_KNIGHT) {
            return ARMOS_KNIGHT_OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_HINOX) {
            return HINOX_OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_ROLLING_BONES) {
            return 0x84;
        }
        if (slots[slot].type() == ENTITY_ROLLING_BONES_BAR) {
            return 0x40;
        }
        if (slots[slot].type() == ENTITY_CHEST_WITH_ITEM) {
            return CHEST_OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_DROPPABLE_SECRET_SEASHELL) {
            return SECRET_SEASHELL_HIDDEN_OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_FISH) {
            return ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        }
        if (slots[slot].type() == ENTITY_CROW) {
            return ENTITY_OPT1_ALLOW_OUT_OF_BOUNDS;
        }
        if (slots[slot].type() == ENTITY_CUCCO) {
            return ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL;
        }
        if (slots[slot].type() == ENTITY_GOPONGA_FLOWER) {
            return GopongaFlowerMotion.OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_GIANT_GOPONGA_FLOWER) {
            return GiantGopongaMotion.OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_POKEY) {
            return PokeyMotion.OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_ZORA) {
            return ZoraMotion.OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_ZOMBIE) {
            return ZombieMotion.OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_BUZZ_BLOB) {
            return BuzzBlobMotion.OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_SAND_CRAB) {
            return SandCrabMotion.OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_URCHIN) {
            return UrchinMotion.OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_WITCH_RAT) {
            return WitchRatMotion.OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_GOPONGA_FLOWER_PROJECTILE) {
            return GopongaProjectileMotion.OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_BOO_BUDDY) {
            return BooBuddyMotion.OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_DROPPABLE_FAIRY) {
            return FAIRY_OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_SPIKED_BEETLE) {
            // The static table starts at splash-only. The handler writes the
            // sword-clink-off bit on its first normal active pass.
            return ENTITY_OPT1_SPLASH_IN_WATER;
        }
        if (slots[slot].type() == ENTITY_POLS_VOICE) {
            return ENTITY_OPT1_SPLASH_IN_WATER;
        }
        if (slots[slot].type() == ENTITY_MINI_MOLDORM) {
            return ENTITY_OPT1_SPLASH_IN_WATER;
        }
        if (slots[slot].type() == ENTITY_MOLDORM) {
            return MoldormMotion.INITIAL_OPTIONS1;
        }
        if (slots[slot].type() == ENTITY_MIMIC) {
            return ENTITY_OPT1_SPLASH_IN_WATER;
        }
        if (isBombiteType(slots[slot].type())) {
            return BOMBITE_OPTIONS1;
        }
        return slots[slot].type() == ENTITY_STALFOS_EVASIVE
            || slots[slot].type() == ENTITY_LIKE_LIKE
            ? ENTITY_OPT1_SPLASH_IN_WATER : 0;
    }

    int colorShellState(int slot) { return this.enemyInspection.colorShellState(slot); }

    int bombiteState(int slot) { return this.enemyInspection.bombiteState(slot); }

    int moldormHeadSpriteVariant(int slot) { return this.enemyInspection.moldormHeadSpriteVariant(slot); }

    int moldormTransitionCountdown(int slot) { return this.enemyInspection.moldormTransitionCountdown(slot); }

    int moldormPrivateCountdown2(int slot) { return this.enemyInspection.moldormPrivateCountdown2(slot); }

    int moldormDestructionState(int slot) { return this.enemyInspection.moldormDestructionState(slot); }

    int moldormInertia(int slot) { return this.enemyInspection.moldormInertia(slot); }

    int bombitePrivateCountdown1(int slot) { return this.enemyInspection.bombitePrivateCountdown1(slot); }

    int bombiteSpeedX(int slot) { return this.enemyInspection.bombiteSpeedX(slot); }

    int bombiteSpeedY(int slot) { return this.enemyInspection.bombiteSpeedY(slot); }

    int colorShellPhysicsFlags(int slot) { return this.enemyInspection.colorShellPhysicsFlags(slot); }

    int secretSeashellPrivateState3(int slot) { return this.enemyInspection.secretSeashellPrivateState3(slot); }

    int secretSeashellPrivateState4(int slot) { return this.enemyInspection.secretSeashellPrivateState4(slot); }

    int secretSeashellPrivateCountdown1(int slot) { return this.enemyInspection.secretSeashellPrivateCountdown1(slot); }

    int secretSeashellSlowTransitionCountdown(int slot) { return this.enemyInspection.secretSeashellSlowTransitionCountdown(slot); }

    int secretSeashellSpeedX(int slot) { return this.enemyInspection.secretSeashellSpeedX(slot); }

    int secretSeashellSpeedY(int slot) { return this.enemyInspection.secretSeashellSpeedY(slot); }

    int secretSeashellSpeedZ(int slot) { return this.enemyInspection.secretSeashellSpeedZ(slot); }

    int fishState(int slot) { return this.enemyInspection.fishState(slot); }

    int fishSpeedX(int slot) { return this.enemyInspection.fishSpeedX(slot); }

    int fishSpeedZ(int slot) { return this.enemyInspection.fishSpeedZ(slot); }

    int fishTransitionCountdown(int slot) { return this.enemyInspection.fishTransitionCountdown(slot); }

    int crowState(int slot) { return this.enemyInspection.crowState(slot); }

    int crowSpeedX(int slot) { return this.enemyInspection.crowSpeedX(slot); }

    int crowSpeedY(int slot) { return this.enemyInspection.crowSpeedY(slot); }

    int crowSpeedZ(int slot) { return this.enemyInspection.crowSpeedZ(slot); }

    int crowTransitionCountdown(int slot) { return this.enemyInspection.crowTransitionCountdown(slot); }

    int booBuddyState(int slot) { return this.enemyInspection.booBuddyState(slot); }

    int booBuddySpeedX(int slot) { return this.enemyInspection.booBuddySpeedX(slot); }

    int booBuddySpeedY(int slot) { return this.enemyInspection.booBuddySpeedY(slot); }

    int booBuddyTransitionCountdown(int slot) { return this.enemyInspection.booBuddyTransitionCountdown(slot); }

    int booBuddyTriggerCount() { return this.enemyInspection.booBuddyTriggerCount(); }

    void setBooBuddyTriggerCount(int value) { this.entityRuntimeConfiguration.setBooBuddyTriggerCount(value); }

    void setWitchEnvironment(int bgPaletteEffectAddress, int paletteDataFlags, int defaultMusicTrack) { this.entityRuntimeConfiguration.setWitchEnvironment(bgPaletteEffectAddress, paletteDataFlags, defaultMusicTrack); }

    void setBooBuddyTriggerCountForTest(int value) { this.entityDebugConfiguration.setBooBuddyTriggerCountForTest(value); }

    void setColorShellStateForTest(int slot, int state, int transitionCountdown, int direction, int speedX, int speedY) { this.entityDebugConfiguration.setColorShellStateForTest(slot, state, transitionCountdown, direction, speedX, speedY); }

    int enemyRecoilSpeedX(int slot) { return this.enemyInspection.enemyRecoilSpeedX(slot); }

    int enemyRecoilSpeedY(int slot) { return this.enemyInspection.enemyRecoilSpeedY(slot); }

    void configureEnemyRecoilForTest(int slot, int linkEntityX, int linkEntityY, int length) { this.entityDebugConfiguration.configureEnemyRecoilForTest(slot, linkEntityX, linkEntityY, length); }

    boolean enemyRecoilActive(int slot) { return this.enemyInspection.enemyRecoilActive(slot); }

    static int variantFor(RoomEntity entity, int frameCounter) {
        if (!entity.spriteDefinition().supported() || entity.spriteDefinition().variantCount() < 2) {
            return entity.spriteVariant();
        }
        return switch (entity.type()) {
            case ENTITY_PIECE_OF_POWER -> (frameCounter >>> 3) & 0x01;
            case ENTITY_BOMBER -> (frameCounter >>> 2) & 0x03;
            case ENTITY_BUTTERFLY -> ((frameCounter + entity.slot() * 8) >>> 3) & 0x01;
            case ENTITY_ARMOS_STATUE -> (frameCounter >>> 4) & 0x01;
            case ENTITY_GHINI -> ((frameCounter >>> 4) ^ entity.slot()) & 0x01;
            case ENTITY_HARDHAT_BEETLE -> (frameCounter >>> 3) & 0x01;
            case ENTITY_SPARK_COUNTER_CLOCKWISE, ENTITY_SPARK_CLOCKWISE ->
                (frameCounter >>> 1) & 0x01;
            case 0x70, 0x73 -> (frameCounter >>> 4) & 0x01;
            default -> entity.spriteVariant();
        };
    }

    private void initializeCommonDroppable(RoomEntity entity) { pickups.initializeCommonDroppable(entity); }

    CommonDroppableUpdate advanceCommonDroppable(RoomEntity entity, int linkEntityX, int linkEntityY, RoomEntityObjectSample objectUnderEntity) { return pickups.advanceCommonDroppable(entity, linkEntityX, linkEntityY, objectUnderEntity); }

    private void revealCommonDroppable(RoomEntity entity, int linkEntityX, int linkEntityY) { pickups.revealCommonDroppable(entity, linkEntityX, linkEntityY); }

    record CommonDroppableUpdate(RoomEntity entity, boolean hidden) {
    }

    record SlimeKeyTransitionUpdate(RoomEntity entity, int nextTransitionCountdown,
                                            boolean holdAboveLink, boolean unload,
                                            SlimeKeyRewardEvent reward) {
    }

    SlimeKeyTransitionUpdate advanceHidingSlimeKey(RoomEntity entity, int slot, int linkEntityX, int linkEntityY, int linkZ) { return pickups.advanceHidingSlimeKey(entity, slot, linkEntityX, linkEntityY, linkZ); }

    void initializeEntityTimers(RoomEntity entity) { pickups.initializeEntityTimers(entity); }

    void resetEnemyDropState(int slot) { pickups.resetEnemyDropState(slot); }

    void decrementEnemyDropCountdowns(RoomEntity entity) { pickups.decrementEnemyDropCountdowns(entity); }

    boolean checkForKeyDropQuicksandHole(RoomEntity entity) { return pickups.checkForKeyDropQuicksandHole(entity); }

    boolean polsVoiceBalladOcarinaTrigger() { return this.enemyInspection.polsVoiceBalladOcarinaTrigger(); }

    void decrementSlowTransitionCountdown(int slot, int frameCounter) { this.lifecycle.decrementSlowTransitionCountdown(slot, frameCounter); }

    boolean shouldDisappear(RoomEntity entity) { return this.lifecycle.shouldDisappear(entity); }

    /**
     * The standard bank-$06 floating handler is available only where its
     * standard sprite definition was loaded. Color Dungeon's $86 handler is
     * a separate bank-$36 path and remains intentionally deferred.
     */

    boolean isDisabledFollower(int type) { return this.enemyInspection.isDisabledFollower(type); }

    static int deathSpriteVariantForCountdown(int countdown) {
        if (countdown >= 0x20 || countdown <= 0) {
            return -1;
        }
        return ((countdown << 1) & 0x30) >>> 4;
    }

    boolean beginFalling(RoomEntity entity,
                                 RoomEntityGroundInteraction.PitTransition transition,
                                 boolean ignoreHitsDecrementedBeforeHandler) {
        int slot = entity.slot();
        int ignoreHitsCountdown = enemyIgnoreHitsCountdown[slot];
        if (ignoreHitsDecrementedBeforeHandler) {
            // The Java runtime keeps a shared combat countdown pass for the
            // already-ported handlers.  In the ROM, the pit branch reads the
            // value before that branch's own decrement; reconstruct that
            // value here so INIT and ACTIVE entities both get one decrement
            // at the same source-level point.
            ignoreHitsCountdown = (ignoreHitsCountdown + 1) & 0xFF;
        }
        if (ignoreHitsCountdown == 0) {
            return false;
        }

        ignoreHitsCountdown--;
        enemyIgnoreHitsCountdown[slot] = ignoreHitsCountdown;
        enemyFlashCountdown[slot] = 0;
        fallingTargetX[slot] = transition.targetX() & 0xFF;
        fallingTargetY[slot] = transition.targetY() & 0xFF;
        fallingSpeedX[slot] = 0;
        fallingSpeedY[slot] = 0;
        fallingSpeedXAccumulator[slot] = 0;
        fallingSpeedYAccumulator[slot] = 0;
        fallingVisualYOffset[slot] = 0;

        boolean longFallingTransition = entity.type() == ENTITY_MOBLIN_SWORD
            || entity.type() == ENTITY_MOBLIN || entity.type() == ENTITY_OCTOROK;
        enemyTransitionCountdown[slot] = longFallingTransition ? 0x6F : 0x48;
        if (!longFallingTransition && ignoreHitsCountdown == 0) {
            enemyTransitionCountdown[slot] = 0x2F;
            pendingEntityEvents.add(new EntityCombatEvent(
                slot, entity.type(), 0, false,
                EntityCombatEvent.SoundChannel.JINGLE, FALLING_JINGLE_ID));
        }
        return true;
    }

    /** Port of bank-$03 EntityFallHandler's pre-impact falling branch. */
    RoomEntity advanceFallingEntity(RoomEntity entity) {
        int slot = entity.slot();
        int transition = enemyTransitionCountdown[slot] & 0xFF;
        if (transition == 0) {
            return null;
        }

        if (transition >= 0x40) {
            // EntityFallHandler calls SetEntityVariantForDirection_03 three
            // times before the family handler. ReturnIfNonInteractive then
            // stops that handler after presentation because the status is
            // still FALLING, so movement and wall collision must not run here.
            fallingVisualYOffset[slot] = 0;
            int variant = entity.spriteVariant();
            if (entity.type() == ENTITY_OCTOROK || entity.type() == ENTITY_MOBLIN) {
                variant = roamingEnemyMotion.advancePresentationVariant(slot, 3);
            } else if (entity.type() == ENTITY_MOBLIN_SWORD) {
                variant = moblinSwordMotion.advancePresentationVariant(slot, entity.x(), 3);
            }
            return withPositionAndVariant(entity, entity.x(), entity.y(), variant);
        }

        int phase = (transition >>> 4) & 0x03;
        fallingVisualYOffset[slot] = FALLING_VISUAL_Y_OFFSETS[phase];
        if (transition == 0x3F) {
            pendingEntityEvents.add(new EntityCombatEvent(
                slot, entity.type(), 0, false,
                EntityCombatEvent.SoundChannel.JINGLE, FALLING_JINGLE_ID));
        }

        FallingVector vector = vectorTowardsTarget(entity.x(), entity.y(), entity.z(),
            fallingTargetX[slot], fallingTargetY[slot], FALLING_VECTOR_LENGTHS[phase]);
        fallingSpeedX[slot] = vector.x();
        fallingSpeedY[slot] = vector.y();
        int x = addFallingSpeedToPosition(entity.x(), fallingSpeedX[slot],
            fallingSpeedXAccumulator, slot);
        int y = addFallingSpeedToPosition(entity.y(), fallingSpeedY[slot],
            fallingSpeedYAccumulator, slot);
        if (spriteHandlers != null) {
            entity = withDefinition(entity, spriteHandlers.forFallingEntity(), phase);
        }
        return withPositionAndVariant(entity, x, y, phase);
    }

    void disableEntityWithoutPersistence(int slot) { this.lifecycle.disableEntityWithoutPersistence(slot); }

    int verticalSpeedZ(RoomEntity entity) {
        int slot = entity.slot();
        if (enemyDropActive[slot]) {
            return enemyDropMotion.speedZ(slot);
        }
        if (entity.type() == ENTITY_TEKTITE) {
            return tektiteMotion.speedZ(slot);
        }
        if (entity.type() == ENTITY_FISH) {
            return fishMotion.speedZ(slot);
        }
        if (entity.type() == ENTITY_CROW) {
            return crowMotion.speedZ(slot);
        }
        if (entity.type() == ENTITY_CUCCO) {
            return cuccoMotion.speedZ(slot);
        }
        if (entity.type() == ENTITY_DROPPABLE_SECRET_SEASHELL) {
            return secretSeashellMotion.speedZ(slot);
        }
        if (entity.type() == ENTITY_WINGED_OCTOROK) {
            return wingedOctorokMotion.speedZ(slot);
        }
        if (entity.type() == ENTITY_STALFOS_AGGRESSIVE) {
            return stalfosAggressiveMotion.speedZ(slot);
        }
        if (entity.type() == ENTITY_STALFOS_EVASIVE) {
            return stalfosEvasiveMotion.speedZ(slot);
        }
        if (isZolGelType(entity.type())) {
            return zolGelMotion.speedZ(slot);
        }
        if (entity.type() == ENTITY_HIDING_ZOL) {
            return hidingZolMotion.speedZ(slot);
        }
        if (entity.type() == ENTITY_POLS_VOICE) {
            return polsVoiceMotion.speedZ(slot);
        }
        if (entity.type() == ENTITY_SPIKED_BEETLE) {
            return spikedBeetleMotion.speedZ(slot);
        }
        if (entity.type() == ENTITY_DOG) {
            return dogMotion.speedZ(slot);
        }
        if (isEnemyProjectileType(entity.type())) {
            return enemyProjectileMotion.speedZ(slot);
        }
        if (entity.type() == ENTITY_ARROW) {
            return playerArrowMotion.speedZ(slot);
        }
        if (ColorShellMotion.isColorShellType(entity.type())) {
            return colorShellMotion.speedZ(slot);
        }
        return 0;
    }

    boolean decrementEnemyCombatCountdowns(int slot, boolean decrementIgnoreHits) { return this.lifecycle.decrementEnemyCombatCountdowns(slot, decrementIgnoreHits); }

    void decrementEnemyStatusCountdowns(int slot) { this.lifecycle.decrementEnemyStatusCountdowns(slot); }

    /** Shared gameplay gates in ReturnIfNonInteractive_03 and _19. */
    boolean entityHandlerInteractive(boolean creditsGameplay) { return this.enemyInspection.entityHandlerInteractive(creditsGameplay); }

    boolean sideViewPotPowerBraceletHeld() { return liftThrow.sideViewPotPowerBraceletHeld(); }

    void pendingLinkPotContact(int sourceSlot, SideViewPotContact.Result contact) { liftThrow.pendingLinkPotContact(sourceSlot, contact); }

    /** Mirrors UpdateEntityTimers' shared dialog/inventory/ocarina early return. */
    boolean entityTimersInteractive() { return this.enemyInspection.entityTimersInteractive(); }

    void requestArmosLinkPush(RoomEntity entity, int linkEntityX, int linkEntityY,
                                      int linkMotionState) {
        if (linkMotionState >= EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE
            || !RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY)) {
            return;
        }
        requestLinkPush(entity, EntityLinkCollisionRules.STANDARD_PUSH);
    }

    void requestLinkPush(RoomEntity entity,
                                 EntityLinkCollisionRules.LinkPushPolicy policy) {
        if (entity == null || policy == null || !policy.restoreFinalPosition()) {
            return;
        }
        pendingLinkFinalPositionRequests.add(new LinkFinalPositionRequest(
            entity.slot(), policy.resetPegasusBoots(), policy.resetHookshotChain(),
            policy.markLinkPushing(), policy.clearLinkPositionIncrement()));
        if (policy.resetHookshotChain()) {
            resetHookshotChainAfterLinkPush();
        }
    }

    private void resetHookshotChainAfterLinkPush() {
        for (int slot = 0; slot < slots.length; slot++) {
            if (!slots[slot].loaded() || slots[slot].type() != ENTITY_HOOKSHOT_CHAIN
                || !hookshotChainMotion.active(slot)) {
                continue;
            }
            HookshotChainMotion.State state = hookshotChainMotion.state(slot);
            hookshotChainMotion.setState(slot, new HookshotChainMotion.State(
                state.x(), state.y(), state.z(), state.direction(), state.speedX(),
                state.speedY(), state.transitionCountdown(), state.speedXAccumulator(),
                state.speedYAccumulator(), 0, state.wallCollisionPending()));
            return;
        }
    }

    EnemyRecoilMotion.Update applyEnemyRecoilIfNeeded(
            RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        if (!enemyRecoilMotion.isActive(slot)) {
            return new EnemyRecoilMotion.Update(entity, false);
        }
        if (enemyIgnoreHitsCountdown[slot] == 0) {
            enemyRecoilMotion.clear(slot);
            return new EnemyRecoilMotion.Update(entity, false);
        }

        if (isRoamingEnemyType(entity.type())) {
            roamingEnemyMotion.beginRecoil(slot);
        }
        // ApplyRecoilIfNeeded_03/06 decrements the shared countdown
        // immediately before applying one fixed-point recoil step.
        enemyIgnoreHitsCountdown[slot]--;
        EnemyRecoilMotion.Update update = enemyRecoilMotion.advance(
            entity, backgroundCollision, isRoamingEnemyType(entity.type()));
        if (update.blocked() && isRoamingEnemyType(entity.type())) {
            // StopEntityRecoilOnCollision clears the ignore-hits countdown.
            enemyIgnoreHitsCountdown[slot] = 0;
        }
        return update;
    }

    static void validateEntitySlot(int slot) {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
    }

    static void validateEnemyDropByte(int value, String label) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(label + " must be an unsigned byte: " + value);
        }
    }

    static void validateByte(int value, String label) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(label + " must be an unsigned byte: " + value);
        }
    }

    static void validateRomDirection(int direction) {
        if (direction < LiftedEntityMotion.ROM_DIRECTION_RIGHT
            || direction > LiftedEntityMotion.ROM_DIRECTION_DOWN) {
            throw new IllegalArgumentException("ROM direction must be between 0 and 3");
        }
    }
}
