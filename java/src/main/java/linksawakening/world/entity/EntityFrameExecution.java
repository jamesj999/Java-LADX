package linksawakening.world.entity;

import java.util.List;
import java.util.function.IntSupplier;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
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

/** Reusable ExecuteEntities locals; invocation scratch is nested and room slot tables stay shared. */
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

    // Absent between acquisitions; release deliberately drops the previous frame input.
    private EntityFrameContext context;
    private boolean inUse;

    public EntityFrameContext context() {
        return context;
    }

    EntityFrameExecution(RoomEntityRuntime runtime) {
        this.runtime = java.util.Objects.requireNonNull(runtime, "runtime");
    }

    public EntitySpriteViews spriteViews() {
        return runtime.slotProperties.spriteViews();
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

    public WarpMotion warpMotion() {
        return runtime.warpMotion;
    }

    public void requestMusic(int track) {
        runtime.entityState.presentationState.writePendingMusicTrack(track);
    }

    public void emit(linksawakening.world.entity.event.HookshotBridgeUpdate update) {
        runtime.entityState.events.hookshotBridgeUpdates.add(update);
    }

    public void emit(RoamingEnemyMotion.LaunchRequest request) {
        runtime.entityState.events.projectileLaunchRequests.add(request);
    }

    public <T> T motion(Class<T> type, java.util.function.Supplier<? extends T> factory) {
        return runtime.entityState.motions.get(type, factory);
    }

    EntityCombatTables combatTables() {
        return runtime.entityState.combatTables;
    }

    EntityPhysicsTables physicsTables() {
        return runtime.entityState.physicsTables;
    }

    EntityRoomVariables roomVariables() {
        return runtime.entityState.roomVariables;
    }

    EntityLinkState linkState() {
        return runtime.entityState.linkState;
    }

    EntityEventChannels events() {
        return runtime.entityState.events;
    }

    EntityHandlerRegistry handlers() {
        return runtime.entityHandlers;
    }

    EntityFrameExecution acquire(EntityFrameContext next) {
        if (inUse) return new EntityFrameExecution(runtime).acquire(next);
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

    public <T> T scratch(Class<T> type, java.util.function.Function<EntityFrameExecution, ? extends T> factory) {
        return scratch.get(type, this, factory);
    }

    private final EntityFrameCollision collision = new EntityFrameCollision(this);

    EntityFrameCollision collisionAdapters() {
        return collision;
    }

    public static final class EntitySlotExecution {
        // Absent before initialization or while this reusable execution scratch is inactive.
        private RoomEntity lifted;
        private int liftedRomDirection;
        private boolean carryingEntity;
        private int index;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private RoomEntity entity;
        private boolean moldormIntroOpenedThisFrame;
        private boolean rollingBonesHitThisFrame;
        private boolean kanaletLeafProducer;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private RoomEntity originalEntity;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private EntityStatus initialStatus;
        private boolean wasInitializing;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private EntityStatus status;
        private boolean keyDropTransitionActive;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private RoomEntityGroundInteraction.Result preAppliedGroundResult;
        private boolean applyGenericGroundInteraction;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private RoomEntity updated;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private EntityLinkCollisionRules.LinkPushPolicy genericLinkPushPolicy;
        private int variant;
        private int renderFlipAttribute;

        public RoomEntity lifted() {
            return lifted;
        }

        public RoomEntity lifted(RoomEntity value) {
            return lifted = value;
        }

        public int liftedRomDirection() {
            return liftedRomDirection;
        }

        public int liftedRomDirection(int value) {
            return liftedRomDirection = value;
        }

        public boolean carryingEntity() {
            return carryingEntity;
        }

        public boolean carryingEntity(boolean value) {
            return carryingEntity = value;
        }

        public int index() {
            return index;
        }

        public int index(int value) {
            return index = value;
        }

        public int postIndex(int delta) {
            int previous = index;
            index += delta;
            return previous;
        }

        public RoomEntity entity() {
            return entity;
        }

        public RoomEntity entity(RoomEntity value) {
            return entity = value;
        }

        public boolean moldormIntroOpenedThisFrame() {
            return moldormIntroOpenedThisFrame;
        }

        public boolean moldormIntroOpenedThisFrame(boolean value) {
            return moldormIntroOpenedThisFrame = value;
        }

        public boolean rollingBonesHitThisFrame() {
            return rollingBonesHitThisFrame;
        }

        public boolean rollingBonesHitThisFrame(boolean value) {
            return rollingBonesHitThisFrame = value;
        }

        public boolean kanaletLeafProducer() {
            return kanaletLeafProducer;
        }

        public boolean kanaletLeafProducer(boolean value) {
            return kanaletLeafProducer = value;
        }

        public RoomEntity originalEntity() {
            return originalEntity;
        }

        public RoomEntity originalEntity(RoomEntity value) {
            return originalEntity = value;
        }

        public EntityStatus initialStatus() {
            return initialStatus;
        }

        public EntityStatus initialStatus(EntityStatus value) {
            return initialStatus = value;
        }

        public boolean wasInitializing() {
            return wasInitializing;
        }

        public boolean wasInitializing(boolean value) {
            return wasInitializing = value;
        }

        public EntityStatus status() {
            return status;
        }

        public EntityStatus status(EntityStatus value) {
            return status = value;
        }

        public boolean keyDropTransitionActive() {
            return keyDropTransitionActive;
        }

        public boolean keyDropTransitionActive(boolean value) {
            return keyDropTransitionActive = value;
        }

        public RoomEntityGroundInteraction.Result preAppliedGroundResult() {
            return preAppliedGroundResult;
        }

        public RoomEntityGroundInteraction.Result preAppliedGroundResult(RoomEntityGroundInteraction.Result value) {
            return preAppliedGroundResult = value;
        }

        public boolean applyGenericGroundInteraction() {
            return applyGenericGroundInteraction;
        }

        public boolean applyGenericGroundInteraction(boolean value) {
            return applyGenericGroundInteraction = value;
        }

        public RoomEntity updated() {
            return updated;
        }

        public RoomEntity updated(RoomEntity value) {
            return updated = value;
        }

        public EntityLinkCollisionRules.LinkPushPolicy genericLinkPushPolicy() {
            return genericLinkPushPolicy;
        }

        public EntityLinkCollisionRules.LinkPushPolicy genericLinkPushPolicy(
                EntityLinkCollisionRules.LinkPushPolicy value) {
            return genericLinkPushPolicy = value;
        }

        public int variant() {
            return variant;
        }

        public int variant(int value) {
            return variant = value;
        }

        public int renderFlipAttribute() {
            return renderFlipAttribute;
        }

        public int renderFlipAttribute(int value) {
            return renderFlipAttribute = value;
        }

        void reset() {
            lifted = null;
            liftedRomDirection = 0;
            carryingEntity = false;
            index = 0;
            entity = null;
            moldormIntroOpenedThisFrame = false;
            rollingBonesHitThisFrame = false;
            kanaletLeafProducer = false;
            originalEntity = null;
            initialStatus = null;
            wasInitializing = false;
            status = null;
            keyDropTransitionActive = false;
            preAppliedGroundResult = null;
            applyGenericGroundInteraction = false;
            updated = null;
            genericLinkPushPolicy = null;
            variant = 0;
            renderFlipAttribute = 0;
        }
    }

    public static final class EntityFrameInputs {
        private int frameCounter;
        private int linkEntityX;
        private int linkEntityY;
        private int collisionType;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private IntSupplier randomByteSupplier;
        // Optional room geometry/history; null preserves the path without that room input.
        private RoomEntityBackgroundCollision backgroundCollision;
        // Optional room geometry/history; null preserves the path without that room input.
        private RoomEntityObjectCollision objectCollision;
        // Optional room geometry/history; null preserves the path without that room input.
        private LinkPositionHistory linkPositionHistory;
        private int linkZ;
        private boolean linkAirborne;
        private int linkDirection;
        private int entityYOffset;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private EnemyProjectileCollision.LinkState projectileLinkState;
        private boolean creditsGameplay;
        private boolean swordCollisionActive;
        private int swordX;
        private int swordWidth;
        private int swordY;
        private int swordHeight;
        private int frame;
        private int romCollisionType;
        // Optional room geometry/history; null preserves the path without that room input.
        private RoomEntityBackgroundInteraction hinoxBackgroundInteraction;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private List<EntityProjectileEvent> projectileEvents;
        private int romLinkDirection;

        public int frameCounter() {
            return frameCounter;
        }

        public int frameCounter(int value) {
            return frameCounter = value;
        }

        public int linkEntityX() {
            return linkEntityX;
        }

        public int linkEntityX(int value) {
            return linkEntityX = value;
        }

        public int linkEntityY() {
            return linkEntityY;
        }

        public int linkEntityY(int value) {
            return linkEntityY = value;
        }

        public int collisionType() {
            return collisionType;
        }

        public int collisionType(int value) {
            return collisionType = value;
        }

        public IntSupplier randomByteSupplier() {
            return randomByteSupplier;
        }

        public IntSupplier randomByteSupplier(IntSupplier value) {
            return randomByteSupplier = value;
        }

        public RoomEntityBackgroundCollision backgroundCollision() {
            return backgroundCollision;
        }

        public RoomEntityBackgroundCollision backgroundCollision(
                /* Optional room geometry/history; null preserves the path without that room input. */ RoomEntityBackgroundCollision
                        value) {
            return backgroundCollision = value;
        }

        public RoomEntityObjectCollision objectCollision() {
            return objectCollision;
        }

        public RoomEntityObjectCollision objectCollision(
                /* Optional room geometry/history; null preserves the path without that room input. */ RoomEntityObjectCollision
                        value) {
            return objectCollision = value;
        }

        public LinkPositionHistory linkPositionHistory() {
            return linkPositionHistory;
        }

        public LinkPositionHistory linkPositionHistory(
                /* Optional room geometry/history; null preserves the path without that room input. */ LinkPositionHistory
                        value) {
            return linkPositionHistory = value;
        }

        public int linkZ() {
            return linkZ;
        }

        public int linkZ(int value) {
            return linkZ = value;
        }

        public boolean linkAirborne() {
            return linkAirborne;
        }

        public boolean linkAirborne(boolean value) {
            return linkAirborne = value;
        }

        public int linkDirection() {
            return linkDirection;
        }

        public int linkDirection(int value) {
            return linkDirection = value;
        }

        public int entityYOffset() {
            return entityYOffset;
        }

        public int entityYOffset(int value) {
            return entityYOffset = value;
        }

        public EnemyProjectileCollision.LinkState projectileLinkState() {
            return projectileLinkState;
        }

        public EnemyProjectileCollision.LinkState projectileLinkState(EnemyProjectileCollision.LinkState value) {
            return projectileLinkState = value;
        }

        public boolean creditsGameplay() {
            return creditsGameplay;
        }

        public boolean creditsGameplay(boolean value) {
            return creditsGameplay = value;
        }

        public boolean swordCollisionActive() {
            return swordCollisionActive;
        }

        public boolean swordCollisionActive(boolean value) {
            return swordCollisionActive = value;
        }

        public int swordX() {
            return swordX;
        }

        public int swordX(int value) {
            return swordX = value;
        }

        public int swordWidth() {
            return swordWidth;
        }

        public int swordWidth(int value) {
            return swordWidth = value;
        }

        public int swordY() {
            return swordY;
        }

        public int swordY(int value) {
            return swordY = value;
        }

        public int swordHeight() {
            return swordHeight;
        }

        public int swordHeight(int value) {
            return swordHeight = value;
        }

        public int frame() {
            return frame;
        }

        public int frame(int value) {
            return frame = value;
        }

        public int romCollisionType() {
            return romCollisionType;
        }

        public int romCollisionType(int value) {
            return romCollisionType = value;
        }

        public RoomEntityBackgroundInteraction hinoxBackgroundInteraction() {
            return hinoxBackgroundInteraction;
        }

        public RoomEntityBackgroundInteraction hinoxBackgroundInteraction(
                /* Optional room geometry/history; null preserves the path without that room input. */ RoomEntityBackgroundInteraction
                        value) {
            return hinoxBackgroundInteraction = value;
        }

        public List<EntityProjectileEvent> projectileEvents() {
            return projectileEvents;
        }

        public List<EntityProjectileEvent> projectileEvents(List<EntityProjectileEvent> value) {
            return projectileEvents = value;
        }

        public int romLinkDirection() {
            return romLinkDirection;
        }

        public int romLinkDirection(int value) {
            return romLinkDirection = value;
        }

        void reset() {
            frameCounter = 0;
            linkEntityX = 0;
            linkEntityY = 0;
            collisionType = 0;
            randomByteSupplier = null;
            backgroundCollision = null;
            objectCollision = null;
            linkPositionHistory = null;
            linkZ = 0;
            linkAirborne = false;
            linkDirection = 0;
            entityYOffset = 0;
            projectileLinkState = null;
            creditsGameplay = false;
            swordCollisionActive = false;
            swordX = 0;
            swordWidth = 0;
            swordY = 0;
            swordHeight = 0;
            frame = 0;
            romCollisionType = 0;
            hinoxBackgroundInteraction = null;
            projectileEvents = null;
            romLinkDirection = 0;
        }
    }

    public static final class EntityTimerGates {
        private boolean handlerLinkCollisionEnabled;
        private boolean countdownBlocksInteraction;
        private boolean threeOfAKindInteractive;
        private boolean handlerInteractive;
        private boolean bowWowHandlerInteractive;
        private boolean projectileTimersInteractive;
        private boolean sideViewPotInteractive;
        private boolean sideViewPotLinkCollisionInteractive;
        private boolean freezeTarinTimers;
        private boolean freezeThreeOfAKindTimers;
        private boolean freezeHinoxTimers;
        private boolean freezeGenieTimers;
        private boolean freezeWitchTimers;
        private boolean freezePowderTimers;
        private boolean freezeBooBuddyTimers;
        private boolean freezeBowWowTimers;
        private boolean freezeMoblinKingTimers;
        private boolean ignoreHitsDecrementedBeforeHandler;

        public boolean handlerLinkCollisionEnabled() {
            return handlerLinkCollisionEnabled;
        }

        public boolean handlerLinkCollisionEnabled(boolean value) {
            return handlerLinkCollisionEnabled = value;
        }

        public boolean countdownBlocksInteraction() {
            return countdownBlocksInteraction;
        }

        public boolean countdownBlocksInteraction(boolean value) {
            return countdownBlocksInteraction = value;
        }

        public boolean threeOfAKindInteractive() {
            return threeOfAKindInteractive;
        }

        public boolean threeOfAKindInteractive(boolean value) {
            return threeOfAKindInteractive = value;
        }

        public boolean handlerInteractive() {
            return handlerInteractive;
        }

        public boolean handlerInteractive(boolean value) {
            return handlerInteractive = value;
        }

        public boolean bowWowHandlerInteractive() {
            return bowWowHandlerInteractive;
        }

        public boolean bowWowHandlerInteractive(boolean value) {
            return bowWowHandlerInteractive = value;
        }

        public boolean projectileTimersInteractive() {
            return projectileTimersInteractive;
        }

        public boolean projectileTimersInteractive(boolean value) {
            return projectileTimersInteractive = value;
        }

        public boolean sideViewPotInteractive() {
            return sideViewPotInteractive;
        }

        public boolean sideViewPotInteractive(boolean value) {
            return sideViewPotInteractive = value;
        }

        public boolean sideViewPotLinkCollisionInteractive() {
            return sideViewPotLinkCollisionInteractive;
        }

        public boolean sideViewPotLinkCollisionInteractive(boolean value) {
            return sideViewPotLinkCollisionInteractive = value;
        }

        public boolean freezeTarinTimers() {
            return freezeTarinTimers;
        }

        public boolean freezeTarinTimers(boolean value) {
            return freezeTarinTimers = value;
        }

        public boolean freezeThreeOfAKindTimers() {
            return freezeThreeOfAKindTimers;
        }

        public boolean freezeThreeOfAKindTimers(boolean value) {
            return freezeThreeOfAKindTimers = value;
        }

        public boolean freezeHinoxTimers() {
            return freezeHinoxTimers;
        }

        public boolean freezeHinoxTimers(boolean value) {
            return freezeHinoxTimers = value;
        }

        public boolean freezeGenieTimers() {
            return freezeGenieTimers;
        }

        public boolean freezeGenieTimers(boolean value) {
            return freezeGenieTimers = value;
        }

        public boolean freezeWitchTimers() {
            return freezeWitchTimers;
        }

        public boolean freezeWitchTimers(boolean value) {
            return freezeWitchTimers = value;
        }

        public boolean freezePowderTimers() {
            return freezePowderTimers;
        }

        public boolean freezePowderTimers(boolean value) {
            return freezePowderTimers = value;
        }

        public boolean freezeBooBuddyTimers() {
            return freezeBooBuddyTimers;
        }

        public boolean freezeBooBuddyTimers(boolean value) {
            return freezeBooBuddyTimers = value;
        }

        public boolean freezeBowWowTimers() {
            return freezeBowWowTimers;
        }

        public boolean freezeBowWowTimers(boolean value) {
            return freezeBowWowTimers = value;
        }

        public boolean freezeMoblinKingTimers() {
            return freezeMoblinKingTimers;
        }

        public boolean freezeMoblinKingTimers(boolean value) {
            return freezeMoblinKingTimers = value;
        }

        public boolean ignoreHitsDecrementedBeforeHandler() {
            return ignoreHitsDecrementedBeforeHandler;
        }

        public boolean ignoreHitsDecrementedBeforeHandler(boolean value) {
            return ignoreHitsDecrementedBeforeHandler = value;
        }

        void reset() {
            handlerLinkCollisionEnabled = false;
            countdownBlocksInteraction = false;
            threeOfAKindInteractive = false;
            handlerInteractive = false;
            bowWowHandlerInteractive = false;
            projectileTimersInteractive = false;
            sideViewPotInteractive = false;
            sideViewPotLinkCollisionInteractive = false;
            freezeTarinTimers = false;
            freezeThreeOfAKindTimers = false;
            freezeHinoxTimers = false;
            freezeGenieTimers = false;
            freezeWitchTimers = false;
            freezePowderTimers = false;
            freezeBooBuddyTimers = false;
            freezeBowWowTimers = false;
            freezeMoblinKingTimers = false;
            ignoreHitsDecrementedBeforeHandler = false;
        }
    }

    public static final class EntityPresentationFlags {
        private enum Kind {
            GHINI,
            MAD_BOMBER,
            BOMBITE,
            IRON_MASK,
            SNAKE,
            STAR,
            BLOOPER,
            WINGED_OCTOROK,
            PINCER,
            BUSH_CRAWLER,
            MIMIC,
            MASKED_MIMIC,
            MINI_MOLDORM,
            MOLDORM,
            CUCCO,
            GOPONGA_FLOWER,
            GIANT_GOPONGA,
            GOPONGA_PROJECTILE,
            POKEY,
            PIRANHA,
            ZORA,
            ZOMBIE,
            BUZZ_BLOB,
            SAND_CRAB,
            DOG,
            URCHIN,
            WITCH_RAT,
            WITCH,
            ROOSTER,
            WIZROBE,
            WIZROBE_PROJECTILE,
            POLS_VOICE,
            SPIKED_BEETLE,
            ARMOS_KNIGHT,
            HINOX,
            ROLLING_BONES,
            THREE_OF_A_KIND,
            MOBLIN_KING,
            SECRET_SEASHELL,
            DROPPABLE,
            TARIN
        }

        private final java.util.EnumSet<Kind> preserved = java.util.EnumSet.noneOf(Kind.class);

        public boolean preserveGhiniPresentation() {
            return preserved.contains(Kind.GHINI);
        }

        public boolean preserveGhiniPresentation(boolean value) {
            return set(Kind.GHINI, value);
        }

        public boolean preserveMadBomberPresentation() {
            return preserved.contains(Kind.MAD_BOMBER);
        }

        public boolean preserveMadBomberPresentation(boolean value) {
            return set(Kind.MAD_BOMBER, value);
        }

        public boolean preserveBombitePresentation() {
            return preserved.contains(Kind.BOMBITE);
        }

        public boolean preserveBombitePresentation(boolean value) {
            return set(Kind.BOMBITE, value);
        }

        public boolean preserveIronMaskPresentation() {
            return preserved.contains(Kind.IRON_MASK);
        }

        public boolean preserveIronMaskPresentation(boolean value) {
            return set(Kind.IRON_MASK, value);
        }

        public boolean preserveSnakePresentation() {
            return preserved.contains(Kind.SNAKE);
        }

        public boolean preserveSnakePresentation(boolean value) {
            return set(Kind.SNAKE, value);
        }

        public boolean preserveStarPresentation() {
            return preserved.contains(Kind.STAR);
        }

        public boolean preserveStarPresentation(boolean value) {
            return set(Kind.STAR, value);
        }

        public boolean preserveBlooperPresentation() {
            return preserved.contains(Kind.BLOOPER);
        }

        public boolean preserveBlooperPresentation(boolean value) {
            return set(Kind.BLOOPER, value);
        }

        public boolean preserveWingedOctorokPresentation() {
            return preserved.contains(Kind.WINGED_OCTOROK);
        }

        public boolean preserveWingedOctorokPresentation(boolean value) {
            return set(Kind.WINGED_OCTOROK, value);
        }

        public boolean preservePincerPresentation() {
            return preserved.contains(Kind.PINCER);
        }

        public boolean preservePincerPresentation(boolean value) {
            return set(Kind.PINCER, value);
        }

        public boolean preserveBushCrawlerPresentation() {
            return preserved.contains(Kind.BUSH_CRAWLER);
        }

        public boolean preserveBushCrawlerPresentation(boolean value) {
            return set(Kind.BUSH_CRAWLER, value);
        }

        public boolean preserveMimicPresentation() {
            return preserved.contains(Kind.MIMIC);
        }

        public boolean preserveMimicPresentation(boolean value) {
            return set(Kind.MIMIC, value);
        }

        public boolean preserveMaskedMimicPresentation() {
            return preserved.contains(Kind.MASKED_MIMIC);
        }

        public boolean preserveMaskedMimicPresentation(boolean value) {
            return set(Kind.MASKED_MIMIC, value);
        }

        public boolean preserveMiniMoldormPresentation() {
            return preserved.contains(Kind.MINI_MOLDORM);
        }

        public boolean preserveMiniMoldormPresentation(boolean value) {
            return set(Kind.MINI_MOLDORM, value);
        }

        public boolean preserveMoldormPresentation() {
            return preserved.contains(Kind.MOLDORM);
        }

        public boolean preserveMoldormPresentation(boolean value) {
            return set(Kind.MOLDORM, value);
        }

        public boolean preserveCuccoPresentation() {
            return preserved.contains(Kind.CUCCO);
        }

        public boolean preserveCuccoPresentation(boolean value) {
            return set(Kind.CUCCO, value);
        }

        public boolean preserveGopongaFlowerPresentation() {
            return preserved.contains(Kind.GOPONGA_FLOWER);
        }

        public boolean preserveGopongaFlowerPresentation(boolean value) {
            return set(Kind.GOPONGA_FLOWER, value);
        }

        public boolean preserveGiantGopongaPresentation() {
            return preserved.contains(Kind.GIANT_GOPONGA);
        }

        public boolean preserveGiantGopongaPresentation(boolean value) {
            return set(Kind.GIANT_GOPONGA, value);
        }

        public boolean preserveGopongaProjectilePresentation() {
            return preserved.contains(Kind.GOPONGA_PROJECTILE);
        }

        public boolean preserveGopongaProjectilePresentation(boolean value) {
            return set(Kind.GOPONGA_PROJECTILE, value);
        }

        public boolean preservePokeyPresentation() {
            return preserved.contains(Kind.POKEY);
        }

        public boolean preservePokeyPresentation(boolean value) {
            return set(Kind.POKEY, value);
        }

        public boolean preservePiranhaPresentation() {
            return preserved.contains(Kind.PIRANHA);
        }

        public boolean preservePiranhaPresentation(boolean value) {
            return set(Kind.PIRANHA, value);
        }

        public boolean preserveZoraPresentation() {
            return preserved.contains(Kind.ZORA);
        }

        public boolean preserveZoraPresentation(boolean value) {
            return set(Kind.ZORA, value);
        }

        public boolean preserveZombiePresentation() {
            return preserved.contains(Kind.ZOMBIE);
        }

        public boolean preserveZombiePresentation(boolean value) {
            return set(Kind.ZOMBIE, value);
        }

        public boolean preserveBuzzBlobPresentation() {
            return preserved.contains(Kind.BUZZ_BLOB);
        }

        public boolean preserveBuzzBlobPresentation(boolean value) {
            return set(Kind.BUZZ_BLOB, value);
        }

        public boolean preserveSandCrabPresentation() {
            return preserved.contains(Kind.SAND_CRAB);
        }

        public boolean preserveSandCrabPresentation(boolean value) {
            return set(Kind.SAND_CRAB, value);
        }

        public boolean preserveDogPresentation() {
            return preserved.contains(Kind.DOG);
        }

        public boolean preserveDogPresentation(boolean value) {
            return set(Kind.DOG, value);
        }

        public boolean preserveUrchinPresentation() {
            return preserved.contains(Kind.URCHIN);
        }

        public boolean preserveUrchinPresentation(boolean value) {
            return set(Kind.URCHIN, value);
        }

        public boolean preserveWitchRatPresentation() {
            return preserved.contains(Kind.WITCH_RAT);
        }

        public boolean preserveWitchRatPresentation(boolean value) {
            return set(Kind.WITCH_RAT, value);
        }

        public boolean preserveWitchPresentation() {
            return preserved.contains(Kind.WITCH);
        }

        public boolean preserveWitchPresentation(boolean value) {
            return set(Kind.WITCH, value);
        }

        public boolean preserveRoosterPresentation() {
            return preserved.contains(Kind.ROOSTER);
        }

        public boolean preserveRoosterPresentation(boolean value) {
            return set(Kind.ROOSTER, value);
        }

        public boolean preserveWizrobePresentation() {
            return preserved.contains(Kind.WIZROBE);
        }

        public boolean preserveWizrobePresentation(boolean value) {
            return set(Kind.WIZROBE, value);
        }

        public boolean preserveWizrobeProjectilePresentation() {
            return preserved.contains(Kind.WIZROBE_PROJECTILE);
        }

        public boolean preserveWizrobeProjectilePresentation(boolean value) {
            return set(Kind.WIZROBE_PROJECTILE, value);
        }

        public boolean preservePolsVoicePresentation() {
            return preserved.contains(Kind.POLS_VOICE);
        }

        public boolean preservePolsVoicePresentation(boolean value) {
            return set(Kind.POLS_VOICE, value);
        }

        public boolean preserveSpikedBeetlePresentation() {
            return preserved.contains(Kind.SPIKED_BEETLE);
        }

        public boolean preserveSpikedBeetlePresentation(boolean value) {
            return set(Kind.SPIKED_BEETLE, value);
        }

        public boolean preserveArmosKnightPresentation() {
            return preserved.contains(Kind.ARMOS_KNIGHT);
        }

        public boolean preserveArmosKnightPresentation(boolean value) {
            return set(Kind.ARMOS_KNIGHT, value);
        }

        public boolean preserveHinoxPresentation() {
            return preserved.contains(Kind.HINOX);
        }

        public boolean preserveHinoxPresentation(boolean value) {
            return set(Kind.HINOX, value);
        }

        public boolean preserveRollingBonesPresentation() {
            return preserved.contains(Kind.ROLLING_BONES);
        }

        public boolean preserveRollingBonesPresentation(boolean value) {
            return set(Kind.ROLLING_BONES, value);
        }

        public boolean preserveThreeOfAKindPresentation() {
            return preserved.contains(Kind.THREE_OF_A_KIND);
        }

        public boolean preserveThreeOfAKindPresentation(boolean value) {
            return set(Kind.THREE_OF_A_KIND, value);
        }

        public boolean preserveMoblinKingPresentation() {
            return preserved.contains(Kind.MOBLIN_KING);
        }

        public boolean preserveMoblinKingPresentation(boolean value) {
            return set(Kind.MOBLIN_KING, value);
        }

        public boolean preserveSecretSeashellPresentation() {
            return preserved.contains(Kind.SECRET_SEASHELL);
        }

        public boolean preserveSecretSeashellPresentation(boolean value) {
            return set(Kind.SECRET_SEASHELL, value);
        }

        public boolean preserveDroppablePresentation() {
            return preserved.contains(Kind.DROPPABLE);
        }

        public boolean preserveDroppablePresentation(boolean value) {
            return set(Kind.DROPPABLE, value);
        }

        public boolean preserveTarinPresentation() {
            return preserved.contains(Kind.TARIN);
        }

        public boolean preserveTarinPresentation(boolean value) {
            return set(Kind.TARIN, value);
        }

        boolean preservesHandlerPresentation() {
            return !preserved.isEmpty();
        }

        private boolean set(Kind kind, boolean value) {
            if (value) preserved.add(kind);
            else preserved.remove(kind);
            return value;
        }

        void reset() {
            preserved.clear();
        }
    }
}
