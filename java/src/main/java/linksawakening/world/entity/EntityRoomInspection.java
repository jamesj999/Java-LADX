package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.Arrays;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.HeartPieceVisual;
import linksawakening.world.entity.event.HookshotBridgeUpdate;
import linksawakening.world.entity.event.InstrumentWarpVisual;
import linksawakening.world.entity.event.LiftedEntityState;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.ColorShellMotion;
import linksawakening.world.entity.motion.HookshotChainMotion;
import linksawakening.world.entity.motion.RoamingEnemyMotion;

/** Read-only views over ROM slot state for gameplay and diagnostics. */
public final class EntityRoomInspection {
    private final EntityRoomInspectionServices runtime;

    EntityRoomInspection(EntityRoomInspectionServices runtime) {
        this.runtime = runtime;
    }

    List<RoamingEnemyMotion.LaunchRequest> projectileLaunchRequests() {
        return List.copyOf(runtime.events().projectileLaunchRequests);
    }

    public LiftedEntityState liftedEntityState() {
        if (runtime.liftThrow().readLiftedEntitySlot() < 0
                || runtime.liftThrow().readLiftedEntitySlot() >= runtime.slots().length) {
            return LiftedEntityState.none();
        }
        RoomEntity entity = runtime.slots()[runtime.liftThrow().readLiftedEntitySlot()];
        if (!entity.loaded() || entity.status() != EntityStatus.LIFTED) {
            return LiftedEntityState.none();
        }
        return new LiftedEntityState(
                runtime.liftThrow().readLiftedEntitySlot(),
                entity.type(),
                runtime.liftThrow().readLiftedPhase(runtime.liftThrow().readLiftedEntitySlot()),
                runtime.physicsTables()
                        .readEnemyTransitionCountdown(runtime.liftThrow().readLiftedEntitySlot()),
                runtime.liftThrow().readLiftedCarryState(),
                runtime.liftThrow()
                        .readLiftedSourceDirection(runtime.liftThrow().readLiftedEntitySlot()),
                runtime.liftThrow().readLiftedEffectiveDirection());
    }

    int initialHealth(int type) {
        return runtime.enemyCombatTables() == null
                ? RoomEntityCombatRules.initialHealth(type)
                : runtime.enemyCombatTables().initialHealth(type);
    }

    int contactDamage(int slot) {
        return runtime.enemyCombatTables() == null
                ? RoomEntityCombatRules.contactDamage(runtime.slots()[slot].type())
                : runtime.enemyCombatTables()
                        .contactDamageForGroup(runtime.combatTables().readEnemyHealthGroup(slot));
    }

    int activePlayerArrowCount() {
        int count = 0;
        for (RoomEntity entity : runtime.slots()) {
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_ARROW) {
                count++;
            }
        }
        return count;
    }

    public int activeProjectileCount() {
        int count = 0;
        for (RoomEntity entity : runtime.slots()) {
            if (!entity.loaded()) {
                continue;
            }
            int type = entity.type() & 0xFF;
            if (type == EntityRomConstants.ENTITY_ARROW
                    || type == EntityRomConstants.ENTITY_BOOMERANG
                    || type == EntityRomConstants.ENTITY_MAGIC_ROD_FIREBALL
                    || type == EntityRomConstants.ENTITY_SWORD_BEAM) {
                count++;
            }
        }
        return count;
    }

    public boolean boomerangActive() {
        for (RoomEntity entity : runtime.slots()) {
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_BOOMERANG) {
                return true;
            }
        }
        return false;
    }

    int activeBoomerangSlot() {
        for (RoomEntity entity : runtime.slots()) {
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_BOOMERANG) {
                return entity.slot();
            }
        }
        return -1;
    }

    int boomerangDirection(int slot) {
        return runtime.motions()
                .get(BoomerangMotion.class, BoomerangMotion::new)
                .direction(slot);
    }

    int boomerangSpeedX(int slot) {
        return runtime.motions()
                .get(BoomerangMotion.class, BoomerangMotion::new)
                .speedX(slot);
    }

    int boomerangSpeedY(int slot) {
        return runtime.motions()
                .get(BoomerangMotion.class, BoomerangMotion::new)
                .speedY(slot);
    }

    int boomerangTransitionCountdown(int slot) {
        return runtime.motions()
                .get(BoomerangMotion.class, BoomerangMotion::new)
                .transitionCountdown(slot);
    }

    int boomerangState(int slot) {
        return runtime.motions()
                .get(BoomerangMotion.class, BoomerangMotion::new)
                .state(slot);
    }

    int swordBeamState(int slot) {
        return runtime.motions()
                .get(SwordBeamMotion.class, SwordBeamMotion::new)
                .state(slot);
    }

    int magicRodFireballPrivateCountdown1(int slot) {
        return runtime.motions()
                .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                .privateCountdown1(slot);
    }

    int magicPowderTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.physicsTables().readEnemyTransitionCountdown(slot);
    }

    int magicPowderState(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.projectiles().readMagicPowderState(slot);
    }

    int magicPowderSlowTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.physicsTables().readSlowTransitionCountdown(slot);
    }

    int swordBeamSpeedX(int slot) {
        return runtime.motions()
                .get(SwordBeamMotion.class, SwordBeamMotion::new)
                .speedX(slot);
    }

    int swordBeamSpeedY(int slot) {
        return runtime.motions()
                .get(SwordBeamMotion.class, SwordBeamMotion::new)
                .speedY(slot);
    }

    int playerArrowDirection(int slot) {
        return runtime.motions()
                .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                .direction(slot);
    }

    int playerArrowSpeedX(int slot) {
        return runtime.motions()
                .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                .speedX(slot);
    }

    int playerArrowSpeedY(int slot) {
        return runtime.motions()
                .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                .speedY(slot);
    }

    int playerArrowTransitionCountdown(int slot) {
        return runtime.motions()
                .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                .transitionCountdown(slot);
    }

    int bombArrowCooldownForTest() {
        return runtime.bombInteractions().readBombArrowCooldown();
    }

    boolean isBombArrowForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.bombInteractions().readPlayerArrowBombArrow(slot);
    }

    public boolean lastArrowShotPlayedWhoosh() {
        return runtime.projectiles().readLastArrowShotPlayedWhoosh();
    }

    public boolean lastBombPlacementPlayedBump() {
        return runtime.bombInteractions().readLastBombPlacementPlayedBump();
    }

    public boolean tarinTransformationHandlerInteractive() {
        return runtime.roomVariables().readTransitionSequenceCounter() == 0x04
                && !runtime.linkState().dialogActive
                && !runtime.linkState().inventoryAppearing;
    }

    boolean tarinTransformationTimersInteractive() {
        return tarinTransformationHandlerInteractive() && runtime.linkState().linkPlayingOcarinaCountdown == 0;
    }

    public boolean genieState0Interactive(boolean creditsGameplay) {
        return !creditsGameplay
                && runtime.roomVariables().readGameplayWorld()
                && runtime.roomVariables().readTransitionSequenceCounter() == 0x04
                && !runtime.linkState().dialogActive
                && !runtime.linkState().inventoryAppearing
                && !witchGotItemPresentationActive()
                && !runtime.roomVariables().readRoomTransitionActive();
    }

    boolean gameplayEntityTimersInteractive(boolean creditsGameplay) {
        return (creditsGameplay
                        || (runtime.roomVariables().readGameplayWorld()
                                && runtime.roomVariables().readTransitionSequenceCounter() == 0x04))
                && !runtime.roomVariables().readRoomTransitionActive()
                && runtime.backgroundMechanics().entityTimersInteractive();
    }

    boolean isLoadedEntityOfType(int slot, int type) {
        return slot >= 0
                && slot < runtime.slots().length
                && runtime.slots()[slot].loaded()
                && runtime.slots()[slot].type() == type;
    }

    public boolean bombActive() {
        for (RoomEntity entity : runtime.slots()) {
            if (entity.loaded()
                    && entity.type() == EntityRomConstants.ENTITY_BOMB
                    && !runtime.bombFinalPresentationPending(entity.slot())) {
                return true;
            }
        }
        return false;
    }

    public int bombDirection(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.bombInteractions().readBombDirection(slot) & 0xFF;
    }

    int bombPrivateCountdown1ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.bombInteractions().readBombPrivateCountdown1(slot) & 0xFF;
    }

    public boolean hookshotActive() {
        return hookshotSlot() >= 0;
    }

    int hookshotSlot() {
        for (int slot = 0; slot < runtime.slots().length; slot++) {
            if (runtime.motions()
                    .get(HookshotChainMotion.class, HookshotChainMotion::new)
                    .active(slot)) {
                return slot;
            }
        }
        return -1;
    }

    int hookshotTransitionCountdown(int slot) {
        return runtime.motions()
                .get(HookshotChainMotion.class, HookshotChainMotion::new)
                .transitionCountdown(slot);
    }

    int bowWowTargetSlot(int slot) {
        return runtime.motions().get(BowWowMotion.class, BowWowMotion::new).targetSlot(slot);
    }

    int bowWowPrivateCountdown2(int slot) {
        return runtime.motions().get(BowWowMotion.class, BowWowMotion::new).privateCountdown2(slot);
    }

    int bowWowTransitionCountdown(int slot) {
        return runtime.motions().get(BowWowMotion.class, BowWowMotion::new).transitionCountdown(slot);
    }

    int entityInertia(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.physicsTables().readEntityInertia(slot);
    }

    int pokeyPrivateState1(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motions().get(PokeyMotion.class, PokeyMotion::new).privateState1(slot);
    }

    int pokeySpeedX(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motions().get(PokeyMotion.class, PokeyMotion::new).speedX(slot);
    }

    int pokeySpeedY(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motions().get(PokeyMotion.class, PokeyMotion::new).speedY(slot);
    }

    int pokeyTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.physicsTables().readEnemyTransitionCountdown(slot);
    }

    int hookshotEntityState(int slot) {
        HookshotChainMotion.State state = runtime.motions()
                .get(HookshotChainMotion.class, HookshotChainMotion::new)
                .state(slot);
        return state == null ? 0 : state.entityState();
    }

    boolean hookshotWallCollisionPending(int slot) {
        HookshotChainMotion.State state = runtime.motions()
                .get(HookshotChainMotion.class, HookshotChainMotion::new)
                .state(slot);
        return state != null && state.wallCollisionPending();
    }

    /** MarinEntityHandler $05:4E6A and MarinEntityHandler_Indoor $05:51CE. */
    public boolean indoorMarinAbsent() {
        if (runtime.motions()
                .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                .readFollowingNpcState()
                .marinFollowing()) return true;
        if (runtime.story().readHouseTradeItem() >= 7) return (runtime.story().readHouseWalrusStatus() & 0x30) != 0;
        return runtime.pickups().readChestPlayerLevelsKnown()
                && runtime.pickups().readChestSwordLevel() != 0;
    }

    /** TarinIndoorsHandler $05:4BF1-4C11, before any sprite rendering. */
    public boolean indoorTarinAbsent() {
        if (!runtime.motions()
                        .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                        .readFollowingNpcState()
                        .marinFollowing()
                && ((runtime.story().readHouseInstrument3Flags() & 2) != 0
                        || runtime.story().readHouseTradeItem() >= 4)) return true;
        return runtime.story().readHouseTarinFlag() == 0
                && runtime.pickups().readChestPlayerLevelsKnown()
                && runtime.pickups().readChestSwordLevel() != 0;
    }

    public RoomEntitySnapshot snapshot() {
        // These handlers test story state before drawing ($05:4E78,
        // $05:51CE, $05:4BF1). Apply the same gates to pre-tick snapshots,
        // including room transitions which pause entity advancement.
        var presentedSlots = Arrays.stream(runtime.slots())
                .map(entity -> {
                    boolean hidden = entity.type() == 0x3E
                            && (runtime.indoorRoom()
                                    // The beach-letter branch has a separate sprite pair, not
                                    // Marin's person sprites. That later presentation is not yet
                                    // ported; retain its previous hidden presentation here.
                                    ? indoorMarinAbsent() || runtime.story().readHouseTradeItem() >= 7
                                    : runtime.pickups().readChestPlayerLevelsKnown()
                                            && runtime.pickups().readChestSwordLevel() == 0);
                    hidden |= entity.type() == 0x3F && runtime.indoorRoom() && indoorTarinAbsent();
                    return hidden ? EntitySnapshots.withVariant(entity, -1) : entity;
                })
                .toList();
        return new RoomEntitySnapshot(
                presentedSlots,
                runtime.presentationState().readSpriteSelection(),
                runtime.spriteTiles(),
                runtime.physicsTables().readGroundInteractionSideScrolling(),
                runtime.slotProperties().readHookshotChainOam(),
                runtime.slotProperties().readPincerBodyOam(),
                runtime.slotProperties().readWingedOctorokOam(),
                runtime.physicsTables().copyFallingVisualYOffset(),
                runtime.slotProperties().readBowWowChainOam());
    }

    List<WingedOctorokOam.Entry> wingedOctorokOam() {
        return runtime.slotProperties().readWingedOctorokOam();
    }

    int wingedOctorokState(int slot) {
        return runtime.motions()
                .get(WingedOctorokMotion.class, WingedOctorokMotion::new)
                .state(slot);
    }

    int wingedOctorokSpeedX(int slot) {
        return runtime.motions()
                .get(WingedOctorokMotion.class, WingedOctorokMotion::new)
                .speedX(slot);
    }

    int wingedOctorokSpeedY(int slot) {
        return runtime.motions()
                .get(WingedOctorokMotion.class, WingedOctorokMotion::new)
                .speedY(slot);
    }

    int wingedOctorokSpeedZ(int slot) {
        return runtime.motions()
                .get(WingedOctorokMotion.class, WingedOctorokMotion::new)
                .speedZ(slot);
    }

    int bushCrawlerState(int slot) {
        return runtime.motions()
                .get(BushCrawlerMotion.class, BushCrawlerMotion::new)
                .state(slot);
    }

    int bushCrawlerPrivateState1(int slot) {
        return runtime.motions()
                .get(BushCrawlerMotion.class, BushCrawlerMotion::new)
                .privateState1(slot);
    }

    int bushCrawlerPrivateState4(int slot) {
        return runtime.motions()
                .get(BushCrawlerMotion.class, BushCrawlerMotion::new)
                .privateState4(slot);
    }

    int bushCrawlerSpeedX(int slot) {
        return runtime.motions()
                .get(BushCrawlerMotion.class, BushCrawlerMotion::new)
                .speedX(slot);
    }

    int bushCrawlerSpeedY(int slot) {
        return runtime.motions()
                .get(BushCrawlerMotion.class, BushCrawlerMotion::new)
                .speedY(slot);
    }

    public int groundStatus(int slot) {
        if (slot < 0 || slot >= runtime.physicsTables().slotCount()) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.physicsTables().readEntityGroundStatus(slot);
    }

    public int bowWowState() {
        return runtime.motions().get(BowWowMotion.class, BowWowMotion::new).readBowWowState() & 0xFF;
    }

    int moblinKingState(int slot) {
        return runtime.motions()
                .get(MoblinKingMotion.class, MoblinKingMotion::new)
                .state(slot);
    }

    public boolean shouldGetLostInMysteriousWoods() {
        return runtime.story().readShouldGetLostInMysteriousWoods();
    }

    int swordPickupStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.rewards().readSwordPickupState(slot);
    }

    int slowTransitionCountdownForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.physicsTables().readSlowTransitionCountdown(slot);
    }

    int threeOfAKindStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motions()
                .get(ThreeOfAKindMotion.class, ThreeOfAKindMotion::new)
                .state(slot);
    }

    int transitionCountdownForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.physicsTables().readEnemyTransitionCountdown(slot);
    }

    int hinoxStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motions().get(HinoxMotion.class, HinoxMotion::new).state(slot);
    }

    int hinoxSpeedXForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motions().get(HinoxMotion.class, HinoxMotion::new).speedX(slot);
    }

    int hinoxSpeedYForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motions().get(HinoxMotion.class, HinoxMotion::new).speedY(slot);
    }

    int enemyHealthForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.combatTables().readEnemyHealth(slot);
    }

    int tarinRaccoonStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motions()
                .get(TarinRaccoonMotion.class, TarinRaccoonMotion::new)
                .state(slot);
    }

    int bombPrivateState4ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.bombInteractions().readBombPrivateState4(slot) & 0xFF;
    }

    int entityRoomStatusForTest() {
        return runtime.roomVariables().readEntityRoomStatus() & 0xFF;
    }

    public int warpTransitionCountdownForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.warpState().readWarpTransitionCountdown(slot) & 0xFF;
    }

    int owlEventStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.story().readOwlEventState(slot);
    }

    int owlPrivateCountdown1ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.story().readOwlPrivateCountdown1(slot);
    }

    int sideViewPlatformSpeedYForTest(int slot) {
        return runtime.motions()
                .get(SideViewPlatformMotion.class, SideViewPlatformMotion::new)
                .speedY(slot);
    }

    int sideViewPlatformSpeedXForTest(int slot) {
        return runtime.motions()
                .get(SideViewPlatformMotion.class, SideViewPlatformMotion::new)
                .speedX(slot);
    }

    int sideViewPlatformPrivateState2ForTest(int slot) {
        return runtime.motions()
                .get(SideViewPlatformMotion.class, SideViewPlatformMotion::new)
                .privateState2(slot);
    }

    int sideViewPlatformPrivateState4ForTest(int slot) {
        return runtime.motions()
                .get(SideViewPlatformMotion.class, SideViewPlatformMotion::new)
                .privateState4(slot);
    }

    public boolean witchGotItemPresentationActive() {
        for (int countdown :
                runtime.motions().get(WitchMotion.class, WitchMotion::new).storageWitchGotItemCountdown()) {
            if (countdown != 0) {
                return true;
            }
        }
        return false;
    }

    public boolean powerUpPickupSequenceActive() {
        for (int countdown : runtime.rewards().storagePowerUpPickupCountdown()) if (countdown != 0) return true;
        return false;
    }

    public boolean swordPickupSequenceActive() {
        for (boolean active : runtime.rewards().storageSwordPickupSequenceActive()) {
            if (active) {
                return true;
            }
        }
        return false;
    }

    int likeLikeState(int slot) {
        return runtime.motions().get(LikeLikeMotion.class, LikeLikeMotion::new).state(slot);
    }

    int likeLikePrivateState1(int slot) {
        return runtime.motions().get(LikeLikeMotion.class, LikeLikeMotion::new).privateState1(slot);
    }

    int likeLikeInertia(int slot) {
        return runtime.motions().get(LikeLikeMotion.class, LikeLikeMotion::new).inertia(slot);
    }

    int likeLikeWalkState(int slot) {
        return runtime.motions().get(LikeLikeMotion.class, LikeLikeMotion::new).walkState(slot);
    }

    int likeLikeWalkSpeedY(int slot) {
        return runtime.motions().get(LikeLikeMotion.class, LikeLikeMotion::new).walkSpeedY(slot);
    }

    public List<HookshotBridgeUpdate> hookshotBridgeUpdates() {
        return List.copyOf(runtime.events().hookshotBridgeUpdates);
    }

    public RoomEntity firstLoadedEntityOfType(int type) {
        for (RoomEntity candidate : runtime.slots()) {
            if (candidate.loaded() && candidate.type() == type) {
                return candidate;
            }
        }
        return null;
    }

    boolean canGoombaStomp(
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            int linkVerticalVelocity,
            boolean linkInteractive,
            int frameCounter) {
        if (entity.type() != EntityRomConstants.ENTITY_GOOMBA
                || !linkAirborne
                || !linkInteractive
                || !RoomEntityCombatRules.collisionCadenceMatches(frameCounter, entity.slot())
                || !RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY)) {
            return false;
        }

        // Top-down hLinkVelocityZ is negative while descending. The side-view
        // branch instead tests hLinkSpeedY, where a non-negative value means
        // Link is moving downward onto the Goomba.
        return runtime.physicsTables().readGroundInteractionSideScrolling()
                ? (linkVerticalVelocity & 0x80) == 0
                : (linkVerticalVelocity & 0x80) != 0;
    }

    boolean hasNoGroundInteractionOverride(int slot) {
        return runtime.physicsTables().readEntityOptions1Override(slot) >= 0
                && (runtime.physicsTables().readEntityOptions1Override(slot)
                                & EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION)
                        != 0;
    }

    boolean hasNoGroundInteraction(RoomEntity entity) {
        return isGhiniType(entity.type())
                || entity.type() == EntityRomConstants.ENTITY_MAD_BOMBER
                || entity.type() == EntityRomConstants.ENTITY_WIZROBE_PROJECTILE
                || entity.type() == EntityRomConstants.ENTITY_GIANT_GOPONGA_FLOWER
                || entity.type() == EntityRomConstants.ENTITY_GOPONGA_FLOWER_PROJECTILE
                || entity.type() == EntityRomConstants.ENTITY_BOO_BUDDY
                || entity.type() == EntityRomConstants.ENTITY_DROPPABLE_FAIRY
                || entity.type() == EntityRomConstants.ENTITY_PINCER
                || entity.type() == EntityRomConstants.ENTITY_MOLDORM
                || hasNoGroundInteractionOverride(entity.slot());
    }

    boolean sourceNpcInteractionAllowed(int xDistance, int yDistance, boolean linkAirborne, int entityDirection) {
        return runtime.linkState().actionButtonAHeld
                && ((yDistance + 0x14) & 0xFF) < 0x28
                && ((xDistance + 0x10) & 0xFF) < 0x20
                && (runtime.linkState().lastRomLinkDirection ^ 0x01) == entityDirection
                && !runtime.linkState().dialogActive
                && !runtime.linkState().inventoryAppearing
                && !linkAirborne
                && runtime.linkState().dialogCooldown == 0
                && runtime.linkState().windowY == 0x80;
    }

    public boolean heartPiecePickupActive() {
        for (var motion : runtime.pickups().storageHeartPiecePickup())
            if (motion != null && motion.active()) return true;
        return false;
    }

    public HeartPieceVisual heartPieceVisual() {
        for (var motion : runtime.pickups().storageHeartPiecePickup())
            if (motion != null && motion.showMeter())
                return new HeartPieceVisual(
                        runtime.pickups().readHeartPiecesCount(),
                        runtime.pickups().storageHeartPieceMeterPairs());
        return null;
    }

    public String instrumentTrace() {
        StringBuilder trace =
                new StringBuilder().append(runtime.presentationState().readActiveMusic() ? 1 : 0);
        for (RoomEntity entity : runtime.slots()) {
            if (entity.loaded()
                    && entity.type() == 0x39
                    && runtime.rewards().readInstrumentParticleKind(entity.slot()) == 0) {
                int slot = entity.slot();
                trace.append(';')
                        .append(slot)
                        .append(',')
                        .append(runtime.rewards().readInstrumentPickupState(slot))
                        .append(',')
                        .append(runtime.physicsTables().readEnemyTransitionCountdown(slot))
                        .append(',')
                        .append(runtime.physicsTables().readSlowTransitionCountdown(slot));
            }
        }
        return trace.toString();
    }

    public String moblinKingTrace() {
        for (RoomEntity entity : runtime.slots())
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_MOBLIN_KING) {
                int slot = entity.slot();
                return "%d,%d,%d,%d,%d,%d,%d"
                        .formatted(
                                runtime.motions()
                                        .get(MoblinKingMotion.class, MoblinKingMotion::new)
                                        .state(slot),
                                entity.x(),
                                entity.y(),
                                entity.z(),
                                runtime.combatTables().readEnemyHealth(slot),
                                runtime.physicsTables().readEnemyTransitionCountdown(slot),
                                runtime.physicsTables().readSlowTransitionCountdown(slot));
            }
        return "-1";
    }

    public InstrumentWarpVisual instrumentWarpVisual() {
        for (int slot = 0; slot < runtime.rewards().storageInstrumentPickupState().length; slot++) {
            // The ROM leaves the white BG/palettes in VRAM after the controller
            // finishes. Keep them until the destination room replaces this runtime.
            if (runtime.rewards().readInstrumentPickupState(slot) == 3
                    || runtime.rewards().readInstrumentPickupState(slot) == 4) {
                return new InstrumentWarpVisual(
                        runtime.rewards().readInstrumentBackgroundFade(slot),
                        runtime.rewards().readInstrumentObjectFade(slot),
                        runtime.rewards().readInstrumentHintBatches(slot),
                        runtime.rewards().storageInstrumentHintTiles(),
                        runtime.rewards().storageInstrumentHintAttributes(),
                        runtime.rewards().readInstrumentWarpX(slot),
                        runtime.rewards().readInstrumentWarpY(slot),
                        runtime.rewards().storageInstrumentWarpSprites());
            }
        }
        return null;
    }

    RoomEntity owlPresentation(RoomEntity entity, int frameCounter) {
        if (runtime.spriteHandlers() == null) {
            return entity;
        }
        boolean flying = ((frameCounter >>> 2) & 0x02) != 0;
        return withDefinition(entity, runtime.spriteHandlers().forOwlEvent(flying), 0);
    }

    int enemyDropHealthGroup(int slot) {
        if (runtime.enemyCombatTables() == null) {
            throw new IllegalStateException("ROM enemy combat tables are required for enemy drop resolution");
        }
        return runtime.combatTables().readEnemyHealthGroup(slot);
    }

    EntitySpriteDefinition spriteDefinitionFor(int entityType) {
        if (runtime.spriteHandlers() == null) {
            return EntitySpriteDefinition.unsupported(entityType);
        }
        EntityRoomLoader.RoomTable table = runtime.presentationState().readSpriteSelection() == null
                ? (runtime.indoorRoom() ? EntityRoomLoader.RoomTable.INDOORS_A : EntityRoomLoader.RoomTable.OVERWORLD)
                : runtime.presentationState().readSpriteSelection().roomTable();
        int mapId = runtime.presentationState().readSpriteSelection() == null
                ? -1
                : runtime.presentationState().readSpriteSelection().roomId();
        return runtime.spriteHandlers().forEntityType(entityType, table, mapId);
    }

    EntitySpriteDefinition spriteDefinitionForBushCrawlerState(int privateState1) {
        if (runtime.spriteHandlers() == null) {
            return EntitySpriteDefinition.unsupported(EntityRomConstants.ENTITY_BUSH_CRAWLER);
        }
        return runtime.spriteHandlers().forBushCrawlerState(privateState1, spriteRoomTable());
    }

    EntitySpriteDefinition spriteDefinitionForPokeySegment() {
        return runtime.spriteHandlers() == null
                ? EntitySpriteDefinition.unsupported(EntityRomConstants.ENTITY_POKEY)
                : runtime.spriteHandlers().forPokeySegment();
    }

    EntitySpriteDefinition spriteDefinitionForBushCrawlerCrawlState(int privateState4) {
        if (runtime.spriteHandlers() == null) {
            return EntitySpriteDefinition.unsupported(EntityRomConstants.ENTITY_BUSH_CRAWLER);
        }
        return runtime.spriteHandlers().forBushCrawlerCrawlState(spriteRoomTable(), privateState4);
    }

    EntityRoomLoader.RoomTable spriteRoomTable() {
        return runtime.presentationState().readSpriteSelection() == null
                ? (runtime.indoorRoom() ? EntityRoomLoader.RoomTable.INDOORS_A : EntityRoomLoader.RoomTable.OVERWORLD)
                : runtime.presentationState().readSpriteSelection().roomTable();
    }

    EntitySpriteDefinition spriteDefinitionForEvasiveState(EntitySpriteDefinition fallback, int privateState1) {
        if (runtime.spriteHandlers() == null) {
            return fallback;
        }
        return runtime.spriteHandlers().forStalfosEvasiveState(privateState1);
    }

    RoomEntity refreshColorShellDisplay(RoomEntity entity, EntityStatus status) {
        if (!ColorShellMotion.isColorShellType(entity.type()) || runtime.spriteHandlers() == null) {
            return entity;
        }
        EntitySpriteDefinition definition = runtime.spriteHandlers()
                .forColorShellState(
                        entity.type(),
                        runtime.motions()
                                .get(ColorShellMotion.class, ColorShellMotion::new)
                                .state(entity.slot()),
                        status);
        return withDefinition(
                entity,
                definition,
                runtime.motions()
                        .get(ColorShellMotion.class, ColorShellMotion::new)
                        .spriteVariant(entity.slot()));
    }

    int slowTransitionCountdown(int slot) {
        if (slot < 0 || slot >= runtime.slots().length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.physicsTables().readSlowTransitionCountdown(slot);
    }

    int droppedItemForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.bossRewards().readDroppedItemBySlot(slot);
    }

    int killCount() {
        return runtime.bossRewards().readKillCount();
    }

    public int killOrderAt(int index) {
        if (index < 0 || index >= runtime.bossRewards().storageKillOrder().length) {
            throw new IllegalArgumentException("Kill-order index out of range: " + index);
        }
        return runtime.bossRewards().readKillOrder(index);
    }

    public EnemyDropResolver.CounterState enemyDropCounters() {
        return runtime.bossRewards().readEnemyDropCounters();
    }

    int dropPrivateCountdown1(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.bossRewards().readDropPrivateCountdown1(slot);
    }

    int dropPrivateCountdown3(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.bossRewards().readDropPrivateCountdown3(slot);
    }

    int droppablePrivateState3(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.bossRewards().readDroppablePrivateState3(slot);
    }

    int droppablePrivateState4(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.bossRewards().readDroppablePrivateState4(slot);
    }

    int dropSpeedY(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motions()
                .get(EnemyDropMotion.class, EnemyDropMotion::new)
                .speedY(slot);
    }

    public int dropSpeedX(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motions()
                .get(EnemyDropMotion.class, EnemyDropMotion::new)
                .speedX(slot);
    }

    public int dropSpeedZ(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motions()
                .get(EnemyDropMotion.class, EnemyDropMotion::new)
                .speedZ(slot);
    }

    int fairySpeedX(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motions().get(FairyMotion.class, FairyMotion::new).speedX(slot);
    }

    int fairySpeedY(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.motions().get(FairyMotion.class, FairyMotion::new).speedY(slot);
    }

    public int entityMapId() {
        return runtime.roomVariables().readEntityMapId();
    }

    public int entityRoomId() {
        return runtime.roomVariables().readEntityRoomId();
    }

    public int entityRoomStatus() {
        return runtime.roomVariables().readEntityRoomStatus();
    }

    public boolean indoorRoom() {
        return runtime.indoorRoom();
    }

    public boolean roomTransitionActive() {
        return runtime.roomVariables().readRoomTransitionActive();
    }

    public boolean gameplayWorld() {
        return runtime.roomVariables().readGameplayWorld();
    }

    public boolean freeMovementMode() {
        return runtime.roomVariables().readFreeMovementMode();
    }

    public int chestSwordLevel() {
        return runtime.pickups().readChestSwordLevel();
    }

    public boolean chestPlayerLevelsKnown() {
        return runtime.pickups().readChestPlayerLevelsKnown();
    }

    public boolean inBossBattle() {
        return runtime.bossRewards().readInBossBattle();
    }

    public boolean dungeonMinibossReady() {
        return runtime.roomVariables().readDungeonMinibossReady();
    }

    public int defaultMusicTrack() {
        return runtime.presentationState().readDefaultMusicTrack();
    }

    public ColorShellWorld colorShellWorld() {
        return runtime.motions()
                .get(ColorShellMotion.class, ColorShellMotion::new)
                .readColorShellWorld();
    }
}
