package linksawakening.world.entity;

import linksawakening.world.entity.event.HeartPieceVisual;
import linksawakening.world.entity.event.InstrumentWarpVisual;

import linksawakening.world.entity.event.LiftedEntityState;

import linksawakening.world.entity.motion.ColorShellMotion;
import linksawakening.world.entity.motion.HookshotChainMotion;
import linksawakening.world.entity.motion.RoamingEnemyMotion;

import linksawakening.world.entity.event.HookshotBridgeUpdate;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;
import linksawakening.entity.EntitySpriteDefinition;
import java.util.Arrays;
import java.util.List;
import static linksawakening.world.entity.RoomEntityRuntime.*;

/** Read-only views over ROM slot state for gameplay and diagnostics. */
public final class EntityRoomInspection {
    private final RoomEntityRuntime runtime;
    EntityRoomInspection(RoomEntityRuntime runtime) { this.runtime = runtime; }

    List<RoamingEnemyMotion.LaunchRequest> projectileLaunchRequests() {
        return List.copyOf(runtime.events.projectileLaunchRequests);
    }

    public LiftedEntityState liftedEntityState() {
        if (runtime.liftedEntitySlot < 0 || runtime.liftedEntitySlot >= runtime.slots.length) {
            return LiftedEntityState.none();
        }
        RoomEntity entity = runtime.slots[runtime.liftedEntitySlot];
        if (!entity.loaded() || entity.status() != EntityStatus.LIFTED) {
            return LiftedEntityState.none();
        }
        return new LiftedEntityState(runtime.liftedEntitySlot, entity.type(),
            runtime.liftedPhase[runtime.liftedEntitySlot], runtime.enemyTransitionCountdown[runtime.liftedEntitySlot],
            runtime.liftedCarryState, runtime.liftedSourceDirection[runtime.liftedEntitySlot],
            runtime.liftedEffectiveDirection);
    }

    int initialHealth(int type) {
        return runtime.enemyCombatTables == null
            ? RoomEntityCombatRules.initialHealth(type)
            : runtime.enemyCombatTables.initialHealth(type);
    }

    int contactDamage(int type) {
        return runtime.enemyCombatTables == null
            ? RoomEntityCombatRules.contactDamage(type)
            : runtime.enemyCombatTables.contactDamage(type);
    }

    int activePlayerArrowCount() {
        int count = 0;
        for (RoomEntity entity : runtime.slots) {
            if (entity.loaded() && entity.type() == runtime.ENTITY_ARROW) {
                count++;
            }
        }
        return count;
    }

    public int activeProjectileCount() {
        int count = 0;
        for (RoomEntity entity : runtime.slots) {
            if (!entity.loaded()) {
                continue;
            }
            int type = entity.type() & 0xFF;
            if (type == runtime.ENTITY_ARROW || type == runtime.ENTITY_BOOMERANG
                || type == runtime.ENTITY_MAGIC_ROD_FIREBALL || type == runtime.ENTITY_SWORD_BEAM) {
                count++;
            }
        }
        return count;
    }

    public boolean boomerangActive() {
        for (RoomEntity entity : runtime.slots) {
            if (entity.loaded() && entity.type() == runtime.ENTITY_BOOMERANG) {
                return true;
            }
        }
        return false;
    }

    int activeBoomerangSlot() {
        for (RoomEntity entity : runtime.slots) {
            if (entity.loaded() && entity.type() == runtime.ENTITY_BOOMERANG) {
                return entity.slot();
            }
        }
        return -1;
    }

    int boomerangDirection(int slot) {
        return runtime.boomerangMotion.direction(slot);
    }

    int boomerangSpeedX(int slot) {
        return runtime.boomerangMotion.speedX(slot);
    }

    int boomerangSpeedY(int slot) {
        return runtime.boomerangMotion.speedY(slot);
    }

    int boomerangTransitionCountdown(int slot) {
        return runtime.boomerangMotion.transitionCountdown(slot);
    }

    int boomerangState(int slot) {
        return runtime.boomerangMotion.state(slot);
    }

    int swordBeamState(int slot) {
        return runtime.swordBeamMotion.state(slot);
    }

    int magicRodFireballPrivateCountdown1(int slot) {
        return runtime.magicRodFireballMotion.privateCountdown1(slot);
    }

    int magicPowderTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.enemyTransitionCountdown[slot];
    }

    int magicPowderState(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.magicPowderState[slot];
    }

    int magicPowderSlowTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.slowTransitionCountdown[slot];
    }

    int swordBeamSpeedX(int slot) {
        return runtime.swordBeamMotion.speedX(slot);
    }

    int swordBeamSpeedY(int slot) {
        return runtime.swordBeamMotion.speedY(slot);
    }

    int playerArrowDirection(int slot) {
        return runtime.playerArrowMotion.direction(slot);
    }

    int playerArrowSpeedX(int slot) {
        return runtime.playerArrowMotion.speedX(slot);
    }

    int playerArrowSpeedY(int slot) {
        return runtime.playerArrowMotion.speedY(slot);
    }

    int playerArrowTransitionCountdown(int slot) {
        return runtime.playerArrowMotion.transitionCountdown(slot);
    }

    int bombArrowCooldownForTest() {
        return runtime.bombArrowCooldown;
    }

    boolean isBombArrowForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.playerArrowBombArrow[slot];
    }

    public boolean lastArrowShotPlayedWhoosh() {
        return runtime.lastArrowShotPlayedWhoosh;
    }

    public boolean lastBombPlacementPlayedBump() {
        return runtime.lastBombPlacementPlayedBump;
    }

    public boolean tarinTransformationHandlerInteractive() {
        return runtime.transitionSequenceCounter == 0x04 && !runtime.linkState.dialogActive && !runtime.linkState.inventoryAppearing;
    }

    boolean tarinTransformationTimersInteractive() {
        return tarinTransformationHandlerInteractive() && runtime.linkState.linkPlayingOcarinaCountdown == 0;
    }

    public boolean genieState0Interactive(boolean creditsGameplay) {
        return !creditsGameplay
            && runtime.gameplayWorld
            && runtime.transitionSequenceCounter == 0x04
            && !runtime.linkState.dialogActive
            && !runtime.linkState.inventoryAppearing
            && !witchGotItemPresentationActive()
            && !runtime.roomTransitionActive;
    }

    boolean gameplayEntityTimersInteractive(boolean creditsGameplay) {
        return (creditsGameplay || (runtime.gameplayWorld && runtime.transitionSequenceCounter == 0x04))
            && !runtime.roomTransitionActive
            && runtime.backgroundMechanics.entityTimersInteractive();
    }

    boolean isLoadedEntityOfType(int slot, int type) {
        return slot >= 0 && slot < runtime.slots.length && runtime.slots[slot].loaded()
            && runtime.slots[slot].type() == type;
    }

    public boolean bombActive() {
        for (RoomEntity entity : runtime.slots) {
            if (entity.loaded() && entity.type() == runtime.ENTITY_BOMB
                && !runtime.bombFinalPresentationPending[entity.slot()]) {
                return true;
            }
        }
        return false;
    }

    public int bombDirection(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.bombDirection[slot] & 0xFF;
    }

    int bombPrivateCountdown1ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.bombPrivateCountdown1[slot] & 0xFF;
    }

    public boolean hookshotActive() {
        return hookshotSlot() >= 0;
    }

    int hookshotSlot() {
        for (int slot = 0; slot < runtime.slots.length; slot++) {
            if (runtime.hookshotChainMotion.active(slot)) {
                return slot;
            }
        }
        return -1;
    }

    int hookshotTransitionCountdown(int slot) {
        return runtime.hookshotChainMotion.transitionCountdown(slot);
    }

    int bowWowTargetSlot(int slot) {
        return runtime.bowWowMotion.targetSlot(slot);
    }

    int bowWowPrivateCountdown2(int slot) {
        return runtime.bowWowMotion.privateCountdown2(slot);
    }

    int bowWowTransitionCountdown(int slot) {
        return runtime.bowWowMotion.transitionCountdown(slot);
    }

    int entityInertia(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityInertia[slot];
    }

    int pokeyPrivateState1(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.pokeyMotion.privateState1(slot);
    }

    int pokeySpeedX(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.pokeyMotion.speedX(slot);
    }

    int pokeySpeedY(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.pokeyMotion.speedY(slot);
    }

    int pokeyTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.enemyTransitionCountdown[slot];
    }

    int hookshotEntityState(int slot) {
        HookshotChainMotion.State state = runtime.hookshotChainMotion.state(slot);
        return state == null ? 0 : state.entityState();
    }

    boolean hookshotWallCollisionPending(int slot) {
        HookshotChainMotion.State state = runtime.hookshotChainMotion.state(slot);
        return state != null && state.wallCollisionPending();
    }

    public RoomEntitySnapshot snapshot() {
        return new RoomEntitySnapshot(Arrays.asList(runtime.slots), runtime.spriteSelection, runtime.spriteTiles,
            runtime.groundInteractionSideScrolling, runtime.hookshotChainOam, runtime.pincerBodyOam,
            runtime.wingedOctorokOam,
            runtime.fallingVisualYOffset, runtime.bowWowChainOam);
    }

    List<WingedOctorokOam.Entry> wingedOctorokOam() {
        return runtime.wingedOctorokOam;
    }

    int wingedOctorokState(int slot) {
        return runtime.wingedOctorokMotion.state(slot);
    }

    int wingedOctorokSpeedX(int slot) {
        return runtime.wingedOctorokMotion.speedX(slot);
    }

    int wingedOctorokSpeedY(int slot) {
        return runtime.wingedOctorokMotion.speedY(slot);
    }

    int wingedOctorokSpeedZ(int slot) {
        return runtime.wingedOctorokMotion.speedZ(slot);
    }

    int bushCrawlerState(int slot) {
        return runtime.bushCrawlerMotion.state(slot);
    }

    int bushCrawlerPrivateState1(int slot) {
        return runtime.bushCrawlerMotion.privateState1(slot);
    }

    int bushCrawlerPrivateState4(int slot) {
        return runtime.bushCrawlerMotion.privateState4(slot);
    }

    int bushCrawlerSpeedX(int slot) {
        return runtime.bushCrawlerMotion.speedX(slot);
    }

    int bushCrawlerSpeedY(int slot) {
        return runtime.bushCrawlerMotion.speedY(slot);
    }

    public int groundStatus(int slot) {
        if (slot < 0 || slot >= runtime.entityGroundStatus.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.entityGroundStatus[slot];
    }

    public int bowWowState() {
        return runtime.bowWowState & 0xFF;
    }

    int moblinKingState(int slot) {
        return runtime.moblinKingMotion.state(slot);
    }

    public boolean shouldGetLostInMysteriousWoods() {
        return runtime.shouldGetLostInMysteriousWoods;
    }

    int swordPickupStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.swordPickupState[slot];
    }

    int slowTransitionCountdownForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.slowTransitionCountdown[slot];
    }

    int threeOfAKindStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.threeOfAKindMotion.state(slot);
    }

    int transitionCountdownForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.enemyTransitionCountdown[slot];
    }

    int hinoxStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.hinoxMotion.state(slot);
    }

    int hinoxSpeedXForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.hinoxMotion.speedX(slot);
    }

    int hinoxSpeedYForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.hinoxMotion.speedY(slot);
    }

    int enemyHealthForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.enemyHealth[slot];
    }

    int tarinRaccoonStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.tarinRaccoonMotion.state(slot);
    }

    int bombPrivateState4ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.bombPrivateState4[slot] & 0xFF;
    }

    int entityRoomStatusForTest() {
        return runtime.entityRoomStatus & 0xFF;
    }

    public int warpTransitionCountdownForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.warpTransitionCountdown[slot] & 0xFF;
    }

    int owlEventStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.owlEventState[slot];
    }

    int owlPrivateCountdown1ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.owlPrivateCountdown1[slot];
    }

    int sideViewPlatformSpeedYForTest(int slot) {
        return runtime.sideViewPlatformMotion.speedY(slot);
    }

    int sideViewPlatformSpeedXForTest(int slot) {
        return runtime.sideViewPlatformMotion.speedX(slot);
    }

    int sideViewPlatformPrivateState2ForTest(int slot) {
        return runtime.sideViewPlatformMotion.privateState2(slot);
    }

    int sideViewPlatformPrivateState4ForTest(int slot) {
        return runtime.sideViewPlatformMotion.privateState4(slot);
    }

    public boolean witchGotItemPresentationActive() {
        for (int countdown : runtime.witchGotItemCountdown) {
            if (countdown != 0) {
                return true;
            }
        }
        return false;
    }

    public boolean powerUpPickupSequenceActive() {
        for (int countdown : runtime.powerUpPickupCountdown) if (countdown != 0) return true;
        return false;
    }

    public boolean swordPickupSequenceActive() {
        for (boolean active : runtime.swordPickupSequenceActive) {
            if (active) {
                return true;
            }
        }
        return false;
    }

    int likeLikeState(int slot) {
        return runtime.likeLikeMotion.state(slot);
    }

    int likeLikePrivateState1(int slot) {
        return runtime.likeLikeMotion.privateState1(slot);
    }

    int likeLikeInertia(int slot) {
        return runtime.likeLikeMotion.inertia(slot);
    }

    int likeLikeWalkState(int slot) {
        return runtime.likeLikeMotion.walkState(slot);
    }

    int likeLikeWalkSpeedY(int slot) {
        return runtime.likeLikeMotion.walkSpeedY(slot);
    }

    public List<HookshotBridgeUpdate> hookshotBridgeUpdates() {
        return List.copyOf(runtime.events.hookshotBridgeUpdates);
    }

    public RoomEntity firstLoadedEntityOfType(int type) {
        for (RoomEntity candidate : runtime.slots) {
            if (candidate.loaded() && candidate.type() == type) {
                return candidate;
            }
        }
        return null;
    }

    boolean canGoombaStomp(RoomEntity entity, int linkEntityX, int linkEntityY,
                                   boolean linkAirborne, int linkVerticalVelocity,
                                   boolean linkInteractive, int frameCounter) {
        if (entity.type() != runtime.ENTITY_GOOMBA || !linkAirborne || !linkInteractive
            || !RoomEntityCombatRules.collisionCadenceMatches(frameCounter, entity.slot())
            || !RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY)) {
            return false;
        }

        // Top-down hLinkVelocityZ is negative while descending. The side-view
        // branch instead tests hLinkSpeedY, where a non-negative value means
        // Link is moving downward onto the Goomba.
        return runtime.groundInteractionSideScrolling
            ? (linkVerticalVelocity & 0x80) == 0
            : (linkVerticalVelocity & 0x80) != 0;
    }

    boolean hasNoGroundInteractionOverride(int slot) {
        return runtime.entityOptions1Override[slot] >= 0
            && (runtime.entityOptions1Override[slot] & runtime.ENTITY_OPT1_NO_GROUND_INTERACTION) != 0;
    }

    boolean hasNoGroundInteraction(RoomEntity entity) {
        return isGhiniType(entity.type()) || entity.type() == runtime.ENTITY_MAD_BOMBER
            || entity.type() == runtime.ENTITY_WIZROBE_PROJECTILE
            || entity.type() == runtime.ENTITY_GIANT_GOPONGA_FLOWER
            || entity.type() == runtime.ENTITY_GOPONGA_FLOWER_PROJECTILE
            || entity.type() == runtime.ENTITY_BOO_BUDDY
            || entity.type() == runtime.ENTITY_DROPPABLE_FAIRY
            || entity.type() == runtime.ENTITY_PINCER
            || entity.type() == runtime.ENTITY_MOLDORM
            || hasNoGroundInteractionOverride(entity.slot());
    }

    boolean sourceNpcInteractionAllowed(int xDistance, int yDistance,
                                                boolean linkAirborne,
                                                int entityDirection) {
        return runtime.linkState.actionButtonAHeld
            && ((yDistance + 0x14) & 0xFF) < 0x28
            && ((xDistance + 0x10) & 0xFF) < 0x20
            && (runtime.linkState.lastRomLinkDirection ^ 0x01) == entityDirection
            && !runtime.linkState.dialogActive && !runtime.linkState.inventoryAppearing && !linkAirborne
            && runtime.linkState.dialogCooldown == 0 && runtime.linkState.windowY == 0x80;
    }

    public boolean heartPiecePickupActive() {
        for (var motion : runtime.heartPiecePickup) if (motion != null && motion.active()) return true;
        return false;
    }

    public HeartPieceVisual heartPieceVisual() {
        for (var motion : runtime.heartPiecePickup) if (motion != null && motion.showMeter())
            return new HeartPieceVisual(runtime.heartPiecesCount, runtime.heartPieceMeterPairs);
        return null;
    }

    public String instrumentTrace() {
        StringBuilder trace = new StringBuilder().append(runtime.activeMusic ? 1 : 0);
        for (RoomEntity entity : runtime.slots) {
            if (entity.loaded() && entity.type() == 0x39 && runtime.instrumentParticleKind[entity.slot()] == 0) {
                int slot = entity.slot();
                trace.append(';').append(slot).append(',').append(runtime.instrumentPickupState[slot])
                    .append(',').append(runtime.enemyTransitionCountdown[slot])
                    .append(',').append(runtime.slowTransitionCountdown[slot]);
            }
        }
        return trace.toString();
    }

    public String moblinKingTrace() {
        for (RoomEntity entity : runtime.slots) if (entity.loaded() && entity.type() == runtime.ENTITY_MOBLIN_KING) {
            int slot = entity.slot();
            return "%d,%d,%d,%d,%d,%d,%d".formatted(runtime.moblinKingMotion.state(slot),
                entity.x(), entity.y(), entity.z(), runtime.enemyHealth[slot],
                runtime.enemyTransitionCountdown[slot], runtime.slowTransitionCountdown[slot]);
        }
        return "-1";
    }

    public InstrumentWarpVisual instrumentWarpVisual() {
        for (int slot = 0; slot < runtime.instrumentPickupState.length; slot++) {
            // The ROM leaves the white BG/palettes in VRAM after the controller
            // finishes. Keep them until the destination room replaces this runtime.
            if (runtime.instrumentPickupState[slot] == 3 || runtime.instrumentPickupState[slot] == 4) {
                return new InstrumentWarpVisual(runtime.instrumentBackgroundFade[slot],
                    runtime.instrumentObjectFade[slot], runtime.instrumentHintBatches[slot],
                    runtime.instrumentHintTiles, runtime.instrumentHintAttributes,
                    runtime.instrumentWarpX[slot], runtime.instrumentWarpY[slot], runtime.instrumentWarpSprites);
            }
        }
        return null;
    }

    RoomEntity owlPresentation(RoomEntity entity, int frameCounter) {
        if (runtime.spriteHandlers == null) {
            return entity;
        }
        boolean flying = ((frameCounter >>> 2) & 0x02) != 0;
        return withDefinition(entity, runtime.spriteHandlers.forOwlEvent(flying), 0);
    }

    int enemyDropHealthGroup(int entityType) {
        if (runtime.enemyCombatTables == null) {
            throw new IllegalStateException(
                "ROM enemy combat tables are required for enemy drop resolution");
        }
        return runtime.enemyCombatTables.healthGroup(entityType);
    }

    EntitySpriteDefinition spriteDefinitionFor(int entityType) {
        if (runtime.spriteHandlers == null) {
            return EntitySpriteDefinition.unsupported(entityType);
        }
        EntityRoomLoader.RoomTable table = runtime.spriteSelection == null
            ? (runtime.indoorRoom ? EntityRoomLoader.RoomTable.INDOORS_A
                : EntityRoomLoader.RoomTable.OVERWORLD)
            : runtime.spriteSelection.roomTable();
        int mapId = runtime.spriteSelection == null ? -1 : runtime.spriteSelection.roomId();
        return runtime.spriteHandlers.forEntityType(entityType, table, mapId);
    }

    EntitySpriteDefinition spriteDefinitionForBushCrawlerState(int privateState1) {
        if (runtime.spriteHandlers == null) {
            return EntitySpriteDefinition.unsupported(runtime.ENTITY_BUSH_CRAWLER);
        }
        return runtime.spriteHandlers.forBushCrawlerState(privateState1, spriteRoomTable());
    }

    EntitySpriteDefinition spriteDefinitionForPokeySegment() {
        return runtime.spriteHandlers == null
            ? EntitySpriteDefinition.unsupported(runtime.ENTITY_POKEY)
            : runtime.spriteHandlers.forPokeySegment();
    }

    EntitySpriteDefinition spriteDefinitionForBushCrawlerCrawlState(int privateState4) {
        if (runtime.spriteHandlers == null) {
            return EntitySpriteDefinition.unsupported(runtime.ENTITY_BUSH_CRAWLER);
        }
        return runtime.spriteHandlers.forBushCrawlerCrawlState(spriteRoomTable(), privateState4);
    }

    EntityRoomLoader.RoomTable spriteRoomTable() {
        return runtime.spriteSelection == null
            ? (runtime.indoorRoom ? EntityRoomLoader.RoomTable.INDOORS_A
                : EntityRoomLoader.RoomTable.OVERWORLD)
            : runtime.spriteSelection.roomTable();
    }

    EntitySpriteDefinition spriteDefinitionForEvasiveState(
            EntitySpriteDefinition fallback, int privateState1) {
        if (runtime.spriteHandlers == null) {
            return fallback;
        }
        return runtime.spriteHandlers.forStalfosEvasiveState(privateState1);
    }

    RoomEntity refreshColorShellDisplay(RoomEntity entity, EntityStatus status) {
        if (!ColorShellMotion.isColorShellType(entity.type()) || runtime.spriteHandlers == null) {
            return entity;
        }
        EntitySpriteDefinition definition = runtime.spriteHandlers.forColorShellState(
            entity.type(), runtime.colorShellMotion.state(entity.slot()), status);
        return withDefinition(entity, definition, runtime.colorShellMotion.spriteVariant(entity.slot()));
    }

    int slowTransitionCountdown(int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.slowTransitionCountdown[slot];
    }

    int droppedItemForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.droppedItemBySlot[slot];
    }

    int killCount() {
        return runtime.killCount;
    }

    public int killOrderAt(int index) {
        if (index < 0 || index >= runtime.killOrder.length) {
            throw new IllegalArgumentException("Kill-order index out of range: " + index);
        }
        return runtime.killOrder[index];
    }

    public EnemyDropResolver.CounterState enemyDropCounters() {
        return runtime.enemyDropCounters;
    }

    int dropPrivateCountdown1(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.dropPrivateCountdown1[slot];
    }

    int dropPrivateCountdown3(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.dropPrivateCountdown3[slot];
    }

    int droppablePrivateState3(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.droppablePrivateState3[slot];
    }

    int droppablePrivateState4(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.droppablePrivateState4[slot];
    }

    int dropSpeedY(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.enemyDropMotion.speedY(slot);
    }

    public int dropSpeedX(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.enemyDropMotion.speedX(slot);
    }

    public int dropSpeedZ(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.enemyDropMotion.speedZ(slot);
    }

    int fairySpeedX(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.fairyMotion.speedX(slot);
    }

    int fairySpeedY(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.fairyMotion.speedY(slot);
    }
}
