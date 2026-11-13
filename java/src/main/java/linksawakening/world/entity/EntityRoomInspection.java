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
    final EntityWarpController warpState;
    final boolean indoorRoom;
    // Optional ROM data, absent in ROM-free runtimes.
    final linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers;
    // Optional ROM data, absent in ROM-free runtimes.
    final linksawakening.gpu.EntitySpriteTileSnapshot spriteTiles;
    final EntityBombState bombInteractions;
    final EntityBossState bossRewards;
    final EntityCombatTables combatTables;
    final EntityEventChannels events;
    final EntityLiftState liftThrow;
    final EntityLinkState linkState;
    final EntityMotionRegistry motions;
    final EntityPhysicsTables physicsTables;
    final EntityPickupState pickups;
    final EntityPresentationState presentationState;
    final EntityProjectileState projectiles;
    final EntityRewardState rewards;
    final EntityRoomVariables roomVariables;
    final EntitySlotPresentation slotProperties;
    final EntityStoryState story;
    // Optional ROM data, absent in ROM-free runtimes.
    final RomEnemyCombatTables enemyCombatTables;
    final RoomEntity[] slots;

    private final RoomEntitySnapshot liveView;

    EntityRoomInspection(
            EntityWarpController warpState,
            boolean indoorRoom,
            linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers,
            linksawakening.gpu.EntitySpriteTileSnapshot spriteTiles,
            EntityBombState bombInteractions,
            EntityBossState bossRewards,
            EntityCombatTables combatTables,
            EntityEventChannels events,
            EntityLiftState liftThrow,
            EntityLinkState linkState,
            EntityMotionRegistry motions,
            EntityPhysicsTables physicsTables,
            EntityPickupState pickups,
            EntityPresentationState presentationState,
            EntityProjectileState projectiles,
            EntityRewardState rewards,
            EntityRoomVariables roomVariables,
            EntitySlotPresentation slotProperties,
            EntityStoryState story,
            RomEnemyCombatTables enemyCombatTables,
            RoomEntity[] slots) {
        this.warpState = java.util.Objects.requireNonNull(warpState, "warpState");
        this.indoorRoom = indoorRoom;
        this.spriteHandlers = spriteHandlers;
        this.spriteTiles = spriteTiles;
        this.bombInteractions = java.util.Objects.requireNonNull(bombInteractions, "bombInteractions");
        this.bossRewards = java.util.Objects.requireNonNull(bossRewards, "bossRewards");
        this.combatTables = java.util.Objects.requireNonNull(combatTables, "combatTables");
        this.events = java.util.Objects.requireNonNull(events, "events");
        this.liftThrow = java.util.Objects.requireNonNull(liftThrow, "liftThrow");
        this.linkState = java.util.Objects.requireNonNull(linkState, "linkState");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
        this.pickups = java.util.Objects.requireNonNull(pickups, "pickups");
        this.presentationState = java.util.Objects.requireNonNull(presentationState, "presentationState");
        this.projectiles = java.util.Objects.requireNonNull(projectiles, "projectiles");
        this.rewards = java.util.Objects.requireNonNull(rewards, "rewards");
        this.roomVariables = java.util.Objects.requireNonNull(roomVariables, "roomVariables");
        this.slotProperties = java.util.Objects.requireNonNull(slotProperties, "slotProperties");
        this.story = java.util.Objects.requireNonNull(story, "story");
        this.enemyCombatTables = enemyCombatTables;
        this.slots = java.util.Objects.requireNonNull(slots, "slots");
        this.liveView = RoomEntitySnapshot.liveView(this);
    }

    public LiftedEntityState liftedEntityState() {
        return EntityRoomQueries.liftedEntityState(this);
    }

    int initialHealth(int type) {
        return this.enemyCombatTables == null
                ? RoomEntityCombatRules.initialHealth(type)
                : this.enemyCombatTables.initialHealth(type);
    }

    int contactDamage(int slot) {
        return this.enemyCombatTables == null
                ? RoomEntityCombatRules.contactDamage(this.slots[slot].type())
                : this.enemyCombatTables.contactDamageForGroup(this.combatTables.readEnemyHealthGroup(slot));
    }

    public int activeProjectileCount() {
        return EntityRoomQueries.activeProjectileCount(this);
    }

    public boolean boomerangActive() {
        for (RoomEntity entity : this.slots) {
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_BOOMERANG) {
                return true;
            }
        }
        return false;
    }

    public boolean lastArrowShotPlayedWhoosh() {
        return this.projectiles.readLastArrowShotPlayedWhoosh();
    }

    public boolean lastBombPlacementPlayedBump() {
        return this.bombInteractions.readLastBombPlacementPlayedBump();
    }

    public boolean tarinTransformationHandlerInteractive() {
        return this.roomVariables.readTransitionSequenceCounter() == 0x04
                && !this.linkState.dialogActive
                && !this.linkState.inventoryAppearing;
    }

    boolean tarinTransformationTimersInteractive() {
        return tarinTransformationHandlerInteractive() && this.linkState.linkPlayingOcarinaCountdown == 0;
    }

    public boolean genieState0Interactive(boolean creditsGameplay) {
        return !creditsGameplay
                && this.roomVariables.readGameplayWorld()
                && this.roomVariables.readTransitionSequenceCounter() == 0x04
                && !this.linkState.dialogActive
                && !this.linkState.inventoryAppearing
                && !witchGotItemPresentationActive()
                && !this.roomVariables.readRoomTransitionActive();
    }

    boolean gameplayEntityTimersInteractive(boolean creditsGameplay) {
        return (creditsGameplay
                        || (this.roomVariables.readGameplayWorld()
                                && this.roomVariables.readTransitionSequenceCounter() == 0x04))
                && !this.roomVariables.readRoomTransitionActive()
                && this.linkState.entityTimersInteractive();
    }

    boolean isLoadedEntityOfType(int slot, int type) {
        return slot >= 0 && slot < this.slots.length && this.slots[slot].loaded() && this.slots[slot].type() == type;
    }

    public boolean bombActive() {
        for (RoomEntity entity : this.slots) {
            if (entity.loaded()
                    && entity.type() == EntityRomConstants.ENTITY_BOMB
                    && !this.bombInteractions.bombFinalPresentationPending(entity.slot())) {
                return true;
            }
        }
        return false;
    }

    public int bombDirection(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return this.bombInteractions.readBombDirection(slot) & 0xFF;
    }

    public boolean hookshotActive() {
        return hookshotSlot() >= 0;
    }

    int hookshotSlot() {
        for (int slot = 0; slot < this.slots.length; slot++) {
            if (this.motions
                    .get(HookshotChainMotion.class, HookshotChainMotion::new)
                    .active(slot)) {
                return slot;
            }
        }
        return -1;
    }

    /** MarinEntityHandler $05:4E6A and MarinEntityHandler_Indoor $05:51CE. */
    public boolean indoorMarinAbsent() {
        return EntityRoomQueries.indoorMarinAbsent(this);
    }

    /** TarinIndoorsHandler $05:4BF1-4C11, before any sprite rendering. */
    public boolean indoorTarinAbsent() {
        return EntityRoomQueries.indoorTarinAbsent(this);
    }

    public RoomEntitySnapshot snapshot() {
        return EntityRoomQueries.snapshot(this);
    }

    public int groundStatus(int slot) {
        if (slot < 0 || slot >= this.physicsTables.slotCount()) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return this.physicsTables.readEntityGroundStatus(slot);
    }

    public int bowWowState() {
        return this.motions.get(BowWowMotion.class, BowWowMotion::new).readBowWowState() & 0xFF;
    }

    public boolean shouldGetLostInMysteriousWoods() {
        return this.story.readShouldGetLostInMysteriousWoods();
    }

    public int warpTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return this.warpState.readWarpTransitionCountdown(slot) & 0xFF;
    }

    public boolean witchGotItemPresentationActive() {
        for (int countdown :
                this.motions.get(WitchMotion.class, WitchMotion::new).storageWitchGotItemCountdown()) {
            if (countdown != 0) {
                return true;
            }
        }
        return false;
    }

    public boolean powerUpPickupSequenceActive() {
        for (int countdown : this.rewards.storagePowerUpPickupCountdown()) if (countdown != 0) return true;
        return false;
    }

    public boolean swordPickupSequenceActive() {
        for (boolean active : this.rewards.storageSwordPickupSequenceActive()) {
            if (active) {
                return true;
            }
        }
        return false;
    }

    public List<HookshotBridgeUpdate> hookshotBridgeUpdates() {
        return List.copyOf(this.events.hookshotBridgeUpdates);
    }

    public RoomEntity firstLoadedEntityOfType(int type) {
        for (RoomEntity candidate : this.slots) {
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
                this,
                entity,
                linkEntityX,
                linkEntityY,
                linkAirborne,
                linkVerticalVelocity,
                linkInteractive,
                frameCounter);
    }

    boolean hasNoGroundInteractionOverride(int slot) {
        return EntityRoomQueries.hasNoGroundInteractionOverride(this, slot);
    }

    boolean hasNoGroundInteraction(RoomEntity entity) {
        return EntityRoomQueries.hasNoGroundInteraction(this, entity);
    }

    boolean sourceNpcInteractionAllowed(int xDistance, int yDistance, boolean linkAirborne, int entityDirection) {
        return EntityRoomQueries.sourceNpcInteractionAllowed(this, xDistance, yDistance, linkAirborne, entityDirection);
    }

    public boolean heartPiecePickupActive() {
        for (var motion : this.pickups.storageHeartPiecePickup()) if (motion != null && motion.active()) return true;
        return false;
    }

    public HeartPieceVisual heartPieceVisual() {
        for (var motion : this.pickups.storageHeartPiecePickup())
            if (motion != null && motion.showMeter())
                return new HeartPieceVisual(
                        this.pickups.readHeartPiecesCount(), this.pickups.storageHeartPieceMeterPairs());
        return null;
    }

    public String instrumentTrace() {
        return EntityRoomQueries.instrumentTrace(this);
    }

    public String moblinKingTrace() {
        return EntityRoomQueries.moblinKingTrace(this);
    }

    public InstrumentWarpVisual instrumentWarpVisual() {
        return EntityRoomQueries.instrumentWarpVisual(this);
    }

    RoomEntity owlPresentation(RoomEntity entity, int frameCounter) {
        if (this.spriteHandlers == null) {
            return entity;
        }
        boolean flying = ((frameCounter >>> 2) & 0x02) != 0;
        return withDefinition(entity, this.slotProperties.spriteViews().forOwlEvent(flying), 0);
    }

    int enemyDropHealthGroup(int slot) {
        if (this.enemyCombatTables == null) {
            throw new IllegalStateException("ROM enemy combat tables are required for enemy drop resolution");
        }
        return this.combatTables.readEnemyHealthGroup(slot);
    }

    EntitySpriteDefinition spriteDefinitionFor(int entityType) {
        return EntityRoomQueries.spriteDefinitionFor(this, entityType);
    }

    EntitySpriteDefinition spriteDefinitionForBushCrawlerState(int disguiseState) {
        return this.spriteHandlers == null
                ? EntitySpriteDefinition.unsupported(EntityRomConstants.ENTITY_BUSH_CRAWLER)
                : this.slotProperties.spriteViews().forBushCrawlerState(disguiseState, spriteRoomTable());
    }

    EntitySpriteDefinition spriteDefinitionForPokeySegment() {
        return this.spriteHandlers == null
                ? EntitySpriteDefinition.unsupported(EntityRomConstants.ENTITY_POKEY)
                : this.slotProperties.spriteViews().forPokeySegment();
    }

    EntitySpriteDefinition spriteDefinitionForBushCrawlerCrawlState(int crawlState) {
        return this.spriteHandlers == null
                ? EntitySpriteDefinition.unsupported(EntityRomConstants.ENTITY_BUSH_CRAWLER)
                : this.slotProperties.spriteViews().forBushCrawlerCrawlState(spriteRoomTable(), crawlState);
    }

    EntityRoomLoader.RoomTable spriteRoomTable() {
        return EntityRoomQueries.spriteRoomTable(this);
    }

    EntitySpriteDefinition spriteDefinitionForEvasiveState(EntitySpriteDefinition fallback, int evadeState) {
        return this.spriteHandlers == null
                ? fallback
                : this.slotProperties.spriteViews().forStalfosEvasiveState(evadeState);
    }

    RoomEntity refreshColorShellDisplay(RoomEntity entity, EntityStatus status) {
        return EntityRoomQueries.refreshColorShellDisplay(this, entity, status);
    }

    public int killOrderAt(int index) {
        if (index < 0 || index >= this.bossRewards.storageKillOrder().length) {
            throw new IllegalArgumentException("Kill-order index out of range: " + index);
        }
        return this.bossRewards.readKillOrder(index);
    }

    public EnemyDropResolver.CounterState enemyDropCounters() {
        return this.bossRewards.readEnemyDropCounters();
    }

    public int dropSpeedX(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return this.motions.get(EnemyDropMotion.class, EnemyDropMotion::new).speedX(slot);
    }

    public int dropSpeedZ(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return this.motions.get(EnemyDropMotion.class, EnemyDropMotion::new).speedZ(slot);
    }

    public int entityMapId() {
        return this.roomVariables.readEntityMapId();
    }

    public int entityRoomId() {
        return this.roomVariables.readEntityRoomId();
    }

    public int entityRoomStatus() {
        return this.roomVariables.readEntityRoomStatus();
    }

    public boolean indoorRoom() {
        return this.indoorRoom;
    }

    public boolean roomTransitionActive() {
        return this.roomVariables.readRoomTransitionActive();
    }

    public boolean gameplayWorld() {
        return this.roomVariables.readGameplayWorld();
    }

    public boolean freeMovementMode() {
        return this.roomVariables.readFreeMovementMode();
    }

    public int chestSwordLevel() {
        return this.pickups.readChestSwordLevel();
    }

    public boolean inBossBattle() {
        return this.bossRewards.readInBossBattle();
    }

    public boolean dungeonMinibossReady() {
        return this.roomVariables.readDungeonMinibossReady();
    }

    public int defaultMusicTrack() {
        return this.presentationState.readDefaultMusicTrack();
    }

    public ColorShellWorld colorShellWorld() {
        return this.motions.get(ColorShellMotion.class, ColorShellMotion::new).readColorShellWorld();
    }

    /** Reusable read-only view for immediate consumption; use snapshot() to retain owned frame data. */
    public RoomEntitySnapshot view() {
        return liveView;
    }
}
