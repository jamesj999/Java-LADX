package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.HeartPieceVisual;
import linksawakening.world.entity.event.HookshotBridgeUpdate;
import linksawakening.world.entity.event.InstrumentWarpVisual;
import linksawakening.world.entity.event.LiftedEntityState;
import linksawakening.world.entity.motion.*;

/** Queries over ExecuteEntities slot state; snapshot() owns data, view() follows the live room. */
public final class EntityRoomInspection {
    private final EntityRoomInspectionServices runtime;
    private final RoomEntitySnapshot liveView;

    EntityRoomInspection(EntityRoomInspectionServices runtime) {
        this.runtime = runtime;
        this.liveView = RoomEntitySnapshot.liveView(runtime);
    }

    public LiftedEntityState liftedEntityState() {
        return EntityRoomQueries.liftedEntityState(runtime);
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

    public int activeProjectileCount() {
        return EntityRoomQueries.activeProjectileCount(runtime);
    }

    public boolean boomerangActive() {
        for (RoomEntity entity : runtime.slots()) {
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_BOOMERANG) {
                return true;
            }
        }
        return false;
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

    /** MarinEntityHandler $05:4E6A and MarinEntityHandler_Indoor $05:51CE. */
    public boolean indoorMarinAbsent() {
        return EntityRoomQueries.indoorMarinAbsent(runtime);
    }

    /** TarinIndoorsHandler $05:4BF1-4C11, before any sprite rendering. */
    public boolean indoorTarinAbsent() {
        return EntityRoomQueries.indoorTarinAbsent(runtime);
    }

    public RoomEntitySnapshot snapshot() {
        return EntityRoomQueries.snapshot(runtime);
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

    public boolean shouldGetLostInMysteriousWoods() {
        return runtime.story().readShouldGetLostInMysteriousWoods();
    }

    /** Compatibility for world.room.RoomSessionViews. */
    @Deprecated
    public int warpTransitionCountdownForTest(int slot) {
        return warpTransitionCountdown(slot);
    }

    public int warpTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.warpState().readWarpTransitionCountdown(slot) & 0xFF;
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
        return EntityRoomQueries.canGoombaStomp(
                runtime,
                entity,
                linkEntityX,
                linkEntityY,
                linkAirborne,
                linkVerticalVelocity,
                linkInteractive,
                frameCounter);
    }

    boolean hasNoGroundInteractionOverride(int slot) {
        return EntityRoomQueries.hasNoGroundInteractionOverride(runtime, slot);
    }

    boolean hasNoGroundInteraction(RoomEntity entity) {
        return EntityRoomQueries.hasNoGroundInteraction(runtime, entity);
    }

    boolean sourceNpcInteractionAllowed(int xDistance, int yDistance, boolean linkAirborne, int entityDirection) {
        return EntityRoomQueries.sourceNpcInteractionAllowed(
                runtime, xDistance, yDistance, linkAirborne, entityDirection);
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
        return EntityRoomQueries.instrumentTrace(runtime);
    }

    public String moblinKingTrace() {
        return EntityRoomQueries.moblinKingTrace(runtime);
    }

    public InstrumentWarpVisual instrumentWarpVisual() {
        return EntityRoomQueries.instrumentWarpVisual(runtime);
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
        return EntityRoomQueries.spriteDefinitionFor(runtime, entityType);
    }

    EntitySpriteDefinition spriteDefinitionForBushCrawlerState(int disguiseState) {
        return runtime.spriteHandlers() == null
                ? EntitySpriteDefinition.unsupported(EntityRomConstants.ENTITY_BUSH_CRAWLER)
                : runtime.spriteHandlers().forBushCrawlerState(disguiseState, spriteRoomTable());
    }

    EntitySpriteDefinition spriteDefinitionForPokeySegment() {
        return runtime.spriteHandlers() == null
                ? EntitySpriteDefinition.unsupported(EntityRomConstants.ENTITY_POKEY)
                : runtime.spriteHandlers().forPokeySegment();
    }

    EntitySpriteDefinition spriteDefinitionForBushCrawlerCrawlState(int crawlState) {
        return runtime.spriteHandlers() == null
                ? EntitySpriteDefinition.unsupported(EntityRomConstants.ENTITY_BUSH_CRAWLER)
                : runtime.spriteHandlers().forBushCrawlerCrawlState(spriteRoomTable(), crawlState);
    }

    EntityRoomLoader.RoomTable spriteRoomTable() {
        return EntityRoomQueries.spriteRoomTable(runtime);
    }

    EntitySpriteDefinition spriteDefinitionForEvasiveState(EntitySpriteDefinition fallback, int evadeState) {
        return runtime.spriteHandlers() == null
                ? fallback
                : runtime.spriteHandlers().forStalfosEvasiveState(evadeState);
    }

    RoomEntity refreshColorShellDisplay(RoomEntity entity, EntityStatus status) {
        return EntityRoomQueries.refreshColorShellDisplay(runtime, entity, status);
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

    EntityRoomInspectionServices inspectionServices() {
        return runtime;
    }
    /** Reusable read-only view for immediate consumption; use snapshot() to retain owned frame data. */
    public RoomEntitySnapshot view() {
        return liveView;
    }
}
