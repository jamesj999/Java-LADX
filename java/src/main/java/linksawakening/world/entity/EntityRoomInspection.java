package linksawakening.world.entity;

import static linksawakening.world.entity.EntitySnapshots.withDefinition;
import static linksawakening.world.entity.EntityTypeRules.isGhiniType;

import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.HeartPieceVisual;
import linksawakening.world.entity.event.HookshotBridgeUpdate;
import linksawakening.world.entity.event.InstrumentWarpVisual;
import linksawakening.world.entity.event.LiftedEntityState;
import linksawakening.world.entity.motion.BowWowMotion;
import linksawakening.world.entity.motion.ColorShellMotion;
import linksawakening.world.entity.motion.EnemyDropMotion;
import linksawakening.world.entity.motion.FollowingNpcMotion;
import linksawakening.world.entity.motion.HookshotChainMotion;
import linksawakening.world.entity.motion.MoblinKingMotion;
import linksawakening.world.entity.motion.WitchMotion;

/** Queries over ExecuteEntities slot state; snapshot() owns data, view() follows the live room. */
public final class EntityRoomInspection {
    static final RoomEntity UNREAD_PRESENTATION = RoomEntity.disabled(0);
    final EntityWarpController warpState;
    final boolean indoorRoom;
    final linksawakening.world.entity.EntitySpriteViews spriteHandlers;
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
    final RomEnemyCombatTables enemyCombatTables;
    final RoomEntity[] slots;

    private final RoomEntitySnapshot liveView;

    EntityRoomInspection(
            EntityWarpController warpState,
            boolean indoorRoom,
            linksawakening.world.entity.EntitySpriteViews spriteHandlers,
            /* Optional ROM-backed data; absent in ROM-free or unconfigured presentation state. */ linksawakening.gpu
                            .EntitySpriteTileSnapshot
                    spriteTiles,
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
        this.spriteHandlers = java.util.Objects.requireNonNull(spriteHandlers, "sprite source");
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
        this.enemyCombatTables = java.util.Objects.requireNonNull(enemyCombatTables, "combat rules");
        this.slots = java.util.Objects.requireNonNull(slots, "slots");
        this.liveView = RoomEntitySnapshot.liveView(this);
    }

    public LiftedEntityState liftedEntityState() {
        if (this.liftThrow.readLiftedEntitySlot() < 0 || this.liftThrow.readLiftedEntitySlot() >= this.slots.length) {
            return LiftedEntityState.none();
        }
        RoomEntity entity = this.slots[this.liftThrow.readLiftedEntitySlot()];
        if (!entity.loaded() || entity.status() != EntityStatus.LIFTED) {
            return LiftedEntityState.none();
        }
        return new LiftedEntityState(
                this.liftThrow.readLiftedEntitySlot(),
                entity.type(),
                this.liftThrow.readLiftedPhase(this.liftThrow.readLiftedEntitySlot()),
                this.physicsTables.readEnemyTransitionCountdown(this.liftThrow.readLiftedEntitySlot()),
                this.liftThrow.readLiftedCarryState(),
                this.liftThrow.readLiftedSourceDirection(this.liftThrow.readLiftedEntitySlot()),
                this.liftThrow.readLiftedEffectiveDirection());
    }

    int initialHealth(int type) {
        return this.enemyCombatTables.initialHealth(type);
    }

    int contactDamage(int slot) {
        return this.enemyCombatTables.contactDamage(
                this.slots[slot].type(), this.combatTables.readEnemyHealthGroup(slot));
    }

    public int activeProjectileCount() {
        int count = 0;
        for (RoomEntity entity : this.slots) {
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
        return EntityRoomInspection.entityRoomQueriesIndoorMarinAbsent(this);
    }

    /** TarinIndoorsHandler $05:4BF1-4C11, before any sprite rendering. */
    public boolean indoorTarinAbsent() {
        return EntityRoomInspection.entityRoomQueriesIndoorTarinAbsent(this);
    }

    public RoomEntitySnapshot snapshot() {
        return EntityRoomInspection.entityRoomQueriesSnapshot(this);
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
        return this.physicsTables.readGroundInteractionSideScrolling()
                ? (linkVerticalVelocity & 0x80) == 0
                : (linkVerticalVelocity & 0x80) != 0;
    }

    boolean hasNoGroundInteractionOverride(int slot) {
        return EntityRoomInspection.entityRoomQueriesHasNoGroundInteractionOverride(this, slot);
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
                || entityRoomQueriesHasNoGroundInteractionOverride(this, entity.slot());
    }

    boolean sourceNpcInteractionAllowed(int xDistance, int yDistance, boolean linkAirborne, int entityDirection) {
        return this.linkState.actionButtonAHeld
                && ((yDistance + 0x14) & 0xFF) < 0x28
                && ((xDistance + 0x10) & 0xFF) < 0x20
                && (this.linkState.lastRomLinkDirection ^ 0x01) == entityDirection
                && !this.linkState.dialogActive
                && !this.linkState.inventoryAppearing
                && !linkAirborne
                && this.linkState.dialogCooldown == 0
                && this.linkState.windowY == 0x80;
    }

    public boolean heartPiecePickupActive() {
        for (var motion : this.pickups.storageHeartPiecePickup()) if (motion.active()) return true;
        return false;
    }

    public HeartPieceVisual heartPieceVisual() {
        for (var motion : this.pickups.storageHeartPiecePickup())
            if (motion.showMeter())
                return new HeartPieceVisual(
                        this.pickups.readHeartPiecesCount(), this.pickups.storageHeartPieceMeterPairs());
        return null;
    }

    public String instrumentTrace() {
        StringBuilder trace = new StringBuilder().append(this.presentationState.readActiveMusic() ? 1 : 0);
        for (RoomEntity entity : this.slots) {
            if (entity.loaded()
                    && entity.type() == 0x39
                    && this.rewards.readInstrumentParticleKind(entity.slot()) == 0) {
                int slot = entity.slot();
                trace.append(';')
                        .append(slot)
                        .append(',')
                        .append(this.rewards.readInstrumentPickupState(slot))
                        .append(',')
                        .append(this.physicsTables.readEnemyTransitionCountdown(slot))
                        .append(',')
                        .append(this.physicsTables.readSlowTransitionCountdown(slot));
            }
        }
        return trace.toString();
    }

    public String moblinKingTrace() {
        for (RoomEntity entity : this.slots)
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_MOBLIN_KING) {
                int slot = entity.slot();
                return "%d,%d,%d,%d,%d,%d,%d"
                        .formatted(
                                this.motions
                                        .get(MoblinKingMotion.class, MoblinKingMotion::new)
                                        .state(slot),
                                entity.x(),
                                entity.y(),
                                entity.z(),
                                this.combatTables.readEnemyHealth(slot),
                                this.physicsTables.readEnemyTransitionCountdown(slot),
                                this.physicsTables.readSlowTransitionCountdown(slot));
            }
        return "-1";
    }

    public InstrumentWarpVisual instrumentWarpVisual() {
        for (int slot = 0; slot < this.rewards.storageInstrumentPickupState().length; slot++) {
            // The ROM leaves the white BG/palettes in VRAM after the controller
            // finishes. Keep them until the destination room replaces this runtime.
            if (this.rewards.readInstrumentPickupState(slot) == 3
                    || this.rewards.readInstrumentPickupState(slot) == 4) {
                return new InstrumentWarpVisual(
                        this.rewards.readInstrumentBackgroundFade(slot),
                        this.rewards.readInstrumentObjectFade(slot),
                        this.rewards.readInstrumentHintBatches(slot),
                        this.rewards.storageInstrumentHintTiles(),
                        this.rewards.storageInstrumentHintAttributes(),
                        this.rewards.readInstrumentWarpX(slot),
                        this.rewards.readInstrumentWarpY(slot),
                        this.rewards.storageInstrumentWarpSprites());
            }
        }
        return null;
    }

    RoomEntity owlPresentation(RoomEntity entity, int frameCounter) {
        boolean flying = ((frameCounter >>> 2) & 0x02) != 0;
        return this.spriteHandlers.present(
                entity, this.slotProperties.spriteViews().forOwlEvent(flying), 0);
    }

    int enemyDropHealthGroup(int slot) {
        this.enemyCombatTables.requireDropTables();
        return this.combatTables.readEnemyHealthGroup(slot);
    }

    EntitySpriteDefinition spriteDefinitionFor(int entityType) {

        EntityRoomLoader.RoomTable table =
                this.presentationState.readSpriteSelection().roomTable();
        int mapId = this.presentationState.readSpriteSelection().roomId();
        return this.slotProperties.spriteViews().forEntityType(entityType, table, mapId);
    }

    EntitySpriteDefinition spriteDefinitionForBushCrawlerState(int disguiseState) {
        return this.slotProperties.spriteViews().forBushCrawlerState(disguiseState, spriteRoomTable());
    }

    EntitySpriteDefinition spriteDefinitionForPokeySegment() {
        return this.slotProperties.spriteViews().forPokeySegment();
    }

    EntitySpriteDefinition spriteDefinitionForBushCrawlerCrawlState(int crawlState) {
        return this.slotProperties.spriteViews().forBushCrawlerCrawlState(spriteRoomTable(), crawlState);
    }

    EntityRoomLoader.RoomTable spriteRoomTable() {
        return this.presentationState.readSpriteSelection().roomTable();
    }

    EntitySpriteDefinition spriteDefinitionForEvasiveState(EntitySpriteDefinition fallback, int evadeState) {
        return !this.spriteHandlers.supportsPresentation()
                ? fallback
                : this.slotProperties.spriteViews().forStalfosEvasiveState(evadeState);
    }

    RoomEntity refreshColorShellDisplay(RoomEntity entity, EntityStatus status) {
        if (!ColorShellMotion.isColorShellType(entity.type()) || !this.spriteHandlers.supportsPresentation()) {
            return entity;
        }
        EntitySpriteDefinition definition = this.slotProperties
                .spriteViews()
                .forColorShellState(
                        entity.type(),
                        this.motions
                                .get(ColorShellMotion.class, ColorShellMotion::new)
                                .state(entity.slot()),
                        status);
        return withDefinition(
                entity,
                definition,
                this.motions.get(ColorShellMotion.class, ColorShellMotion::new).spriteVariant(entity.slot()));
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

    static boolean entityRoomQueriesIndoorMarinAbsent(EntityRoomInspection runtime) {
        if (runtime.motions
                .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                .readFollowingNpcState()
                .marinFollowing()) return true;
        if (runtime.story.readHouseTradeItem() >= 7) return (runtime.story.readHouseWalrusStatus() & 0x30) != 0;
        return runtime.pickups.readChestPlayerLevelsKnown() && runtime.pickups.readChestSwordLevel() != 0;
    }

    static boolean entityRoomQueriesIndoorTarinAbsent(EntityRoomInspection runtime) {
        if (!runtime.motions
                        .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                        .readFollowingNpcState()
                        .marinFollowing()
                && ((runtime.story.readHouseInstrument3Flags() & 2) != 0 || runtime.story.readHouseTradeItem() >= 4))
            return true;
        return runtime.story.readHouseTarinFlag() == 0
                && runtime.pickups.readChestPlayerLevelsKnown()
                && runtime.pickups.readChestSwordLevel() != 0;
    }

    static RoomEntitySnapshot entityRoomQueriesSnapshot(EntityRoomInspection runtime) {
        // These handlers test story state before drawing ($05:4E78,
        // $05:51CE, $05:4BF1). Apply the same gates to pre-tick snapshots,
        // including room transitions which pause entity advancement.
        var presentedSlots = new java.util.ArrayList<RoomEntity>(runtime.slots.length);
        for (int slot = 0; slot < runtime.slots.length; slot++)
            presentedSlots.add(entityRoomQueriesPresentedEntity(runtime, slot));
        return new RoomEntitySnapshot(
                presentedSlots,
                runtime.presentationState.snapshotSelection(),
                runtime.spriteTiles,
                runtime.physicsTables.readGroundInteractionSideScrolling(),
                runtime.slotProperties.readHookshotChainOam(),
                runtime.slotProperties.readPincerBodyOam(),
                runtime.slotProperties.readWingedOctorokOam(),
                runtime.physicsTables.copyFallingVisualYOffset(),
                runtime.slotProperties.readBowWowChainOam());
    }

    static boolean entityRoomQueriesHasNoGroundInteractionOverride(EntityRoomInspection runtime, int slot) {
        return runtime.physicsTables.readEntityOptions1Override(slot) >= 0
                && (runtime.physicsTables.readEntityOptions1Override(slot)
                                & EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION)
                        != 0;
    }

    static RoomEntity entityRoomQueriesPresentedEntity(EntityRoomInspection runtime, int slot) {
        return entityRoomQueriesPresentedEntity(runtime, slot, UNREAD_PRESENTATION, UNREAD_PRESENTATION);
    }

    // Identity distinguishes cache generations or explicit sentinels; equal payloads must not coalesce.
    @SuppressWarnings("ReferenceEquality")
    static RoomEntity entityRoomQueriesPresentedEntity(
            EntityRoomInspection runtime,
            int slot, /* Source identity of the cached projection, or UNREAD_PRESENTATION. */
            RoomEntity previousSource,
            /* Immutable cached projection, seeded with UNREAD_PRESENTATION. */ RoomEntity previousPresentation) {
        RoomEntity entity = runtime.slots[slot];
        boolean hidden = entity.type() == 0x3E
                && (runtime.indoorRoom
                        ? entityRoomQueriesIndoorMarinAbsent(runtime) || runtime.story.readHouseTradeItem() >= 7
                        : runtime.pickups.readChestPlayerLevelsKnown() && runtime.pickups.readChestSwordLevel() == 0);
        hidden |= entity.type() == 0x3F && runtime.indoorRoom && entityRoomQueriesIndoorTarinAbsent(runtime);
        if (!hidden || entity.spriteVariant() == -1) return entity;
        if (entity == previousSource && previousPresentation.spriteVariant() == -1) return previousPresentation;
        return EntitySnapshots.withVariant(entity, -1);
    }
}
