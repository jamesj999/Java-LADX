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
import linksawakening.world.entity.motion.ColorShellMotion;
import linksawakening.world.entity.motion.HookshotChainMotion;
import linksawakening.world.entity.motion.RoamingEnemyMotion;

/** Read-only views over ROM slot state for gameplay and diagnostics. */
public final class EntityRoomInspection {
    private final RoomEntityRuntime runtime;

    EntityRoomInspection(RoomEntityRuntime runtime) {
        this.runtime = runtime;
    }

    List<RoamingEnemyMotion.LaunchRequest> projectileLaunchRequests() {
        return List.copyOf(runtime.entityState.events.projectileLaunchRequests);
    }

    public LiftedEntityState liftedEntityState() {
        if (runtime.entityState.liftedEntitySlot < 0 || runtime.entityState.liftedEntitySlot >= runtime.slots.length) {
            return LiftedEntityState.none();
        }
        RoomEntity entity = runtime.slots[runtime.entityState.liftedEntitySlot];
        if (!entity.loaded() || entity.status() != EntityStatus.LIFTED) {
            return LiftedEntityState.none();
        }
        return new LiftedEntityState(
                runtime.entityState.liftedEntitySlot,
                entity.type(),
                runtime.entityState.liftedPhase[runtime.entityState.liftedEntitySlot],
                runtime.entityState.enemyTransitionCountdown[runtime.entityState.liftedEntitySlot],
                runtime.entityState.liftedCarryState,
                runtime.entityState.liftedSourceDirection[runtime.entityState.liftedEntitySlot],
                runtime.entityState.liftedEffectiveDirection);
    }

    int initialHealth(int type) {
        return runtime.enemyCombatTables == null
                ? RoomEntityCombatRules.initialHealth(type)
                : runtime.enemyCombatTables.initialHealth(type);
    }

    int contactDamage(int slot) {
        return runtime.enemyCombatTables == null
                ? RoomEntityCombatRules.contactDamage(runtime.slots[slot].type())
                : runtime.enemyCombatTables.contactDamageForGroup(runtime.entityState.enemyHealthGroup[slot]);
    }

    int activePlayerArrowCount() {
        int count = 0;
        for (RoomEntity entity : runtime.slots) {
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_ARROW) {
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
        for (RoomEntity entity : runtime.slots) {
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_BOOMERANG) {
                return true;
            }
        }
        return false;
    }

    int activeBoomerangSlot() {
        for (RoomEntity entity : runtime.slots) {
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_BOOMERANG) {
                return entity.slot();
            }
        }
        return -1;
    }

    int boomerangDirection(int slot) {
        return runtime.entityState.boomerangMotion.direction(slot);
    }

    int boomerangSpeedX(int slot) {
        return runtime.entityState.boomerangMotion.speedX(slot);
    }

    int boomerangSpeedY(int slot) {
        return runtime.entityState.boomerangMotion.speedY(slot);
    }

    int boomerangTransitionCountdown(int slot) {
        return runtime.entityState.boomerangMotion.transitionCountdown(slot);
    }

    int boomerangState(int slot) {
        return runtime.entityState.boomerangMotion.state(slot);
    }

    int swordBeamState(int slot) {
        return runtime.entityState.swordBeamMotion.state(slot);
    }

    int magicRodFireballPrivateCountdown1(int slot) {
        return runtime.entityState.magicRodFireballMotion.privateCountdown1(slot);
    }

    int magicPowderTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.enemyTransitionCountdown[slot];
    }

    int magicPowderState(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.magicPowderState[slot];
    }

    int magicPowderSlowTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.slowTransitionCountdown[slot];
    }

    int swordBeamSpeedX(int slot) {
        return runtime.entityState.swordBeamMotion.speedX(slot);
    }

    int swordBeamSpeedY(int slot) {
        return runtime.entityState.swordBeamMotion.speedY(slot);
    }

    int playerArrowDirection(int slot) {
        return runtime.entityState.playerArrowMotion.direction(slot);
    }

    int playerArrowSpeedX(int slot) {
        return runtime.entityState.playerArrowMotion.speedX(slot);
    }

    int playerArrowSpeedY(int slot) {
        return runtime.entityState.playerArrowMotion.speedY(slot);
    }

    int playerArrowTransitionCountdown(int slot) {
        return runtime.entityState.playerArrowMotion.transitionCountdown(slot);
    }

    int bombArrowCooldownForTest() {
        return runtime.entityState.bombArrowCooldown;
    }

    boolean isBombArrowForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.playerArrowBombArrow[slot];
    }

    public boolean lastArrowShotPlayedWhoosh() {
        return runtime.entityState.lastArrowShotPlayedWhoosh;
    }

    public boolean lastBombPlacementPlayedBump() {
        return runtime.entityState.lastBombPlacementPlayedBump;
    }

    public boolean tarinTransformationHandlerInteractive() {
        return runtime.entityState.transitionSequenceCounter == 0x04
                && !runtime.entityState.linkState.dialogActive
                && !runtime.entityState.linkState.inventoryAppearing;
    }

    boolean tarinTransformationTimersInteractive() {
        return tarinTransformationHandlerInteractive()
                && runtime.entityState.linkState.linkPlayingOcarinaCountdown == 0;
    }

    public boolean genieState0Interactive(boolean creditsGameplay) {
        return !creditsGameplay
                && runtime.entityState.gameplayWorld
                && runtime.entityState.transitionSequenceCounter == 0x04
                && !runtime.entityState.linkState.dialogActive
                && !runtime.entityState.linkState.inventoryAppearing
                && !witchGotItemPresentationActive()
                && !runtime.entityState.roomTransitionActive;
    }

    boolean gameplayEntityTimersInteractive(boolean creditsGameplay) {
        return (creditsGameplay
                        || (runtime.entityState.gameplayWorld && runtime.entityState.transitionSequenceCounter == 0x04))
                && !runtime.entityState.roomTransitionActive
                && runtime.backgroundMechanics.entityTimersInteractive();
    }

    boolean isLoadedEntityOfType(int slot, int type) {
        return slot >= 0
                && slot < runtime.slots.length
                && runtime.slots[slot].loaded()
                && runtime.slots[slot].type() == type;
    }

    public boolean bombActive() {
        for (RoomEntity entity : runtime.slots) {
            if (entity.loaded()
                    && entity.type() == EntityRomConstants.ENTITY_BOMB
                    && !runtime.bombFinalPresentationPending[entity.slot()]) {
                return true;
            }
        }
        return false;
    }

    public int bombDirection(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.bombDirection[slot] & 0xFF;
    }

    int bombPrivateCountdown1ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.bombPrivateCountdown1[slot] & 0xFF;
    }

    public boolean hookshotActive() {
        return hookshotSlot() >= 0;
    }

    int hookshotSlot() {
        for (int slot = 0; slot < runtime.slots.length; slot++) {
            if (runtime.entityState.hookshotChainMotion.active(slot)) {
                return slot;
            }
        }
        return -1;
    }

    int hookshotTransitionCountdown(int slot) {
        return runtime.entityState.hookshotChainMotion.transitionCountdown(slot);
    }

    int bowWowTargetSlot(int slot) {
        return runtime.entityState.bowWowMotion.targetSlot(slot);
    }

    int bowWowPrivateCountdown2(int slot) {
        return runtime.entityState.bowWowMotion.privateCountdown2(slot);
    }

    int bowWowTransitionCountdown(int slot) {
        return runtime.entityState.bowWowMotion.transitionCountdown(slot);
    }

    int entityInertia(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.entityInertia[slot];
    }

    int pokeyPrivateState1(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.pokeyMotion.privateState1(slot);
    }

    int pokeySpeedX(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.pokeyMotion.speedX(slot);
    }

    int pokeySpeedY(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.pokeyMotion.speedY(slot);
    }

    int pokeyTransitionCountdown(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.enemyTransitionCountdown[slot];
    }

    int hookshotEntityState(int slot) {
        HookshotChainMotion.State state = runtime.entityState.hookshotChainMotion.state(slot);
        return state == null ? 0 : state.entityState();
    }

    boolean hookshotWallCollisionPending(int slot) {
        HookshotChainMotion.State state = runtime.entityState.hookshotChainMotion.state(slot);
        return state != null && state.wallCollisionPending();
    }

    /** MarinEntityHandler $05:4E6A and MarinEntityHandler_Indoor $05:51CE. */
    public boolean indoorMarinAbsent() {
        if (runtime.entityState.followingNpcState.marinFollowing()) return true;
        if (runtime.entityState.houseTradeItem >= 7) return (runtime.entityState.houseWalrusStatus & 0x30) != 0;
        return runtime.entityState.chestPlayerLevelsKnown && runtime.entityState.chestSwordLevel != 0;
    }

    /** TarinIndoorsHandler $05:4BF1-4C11, before any sprite rendering. */
    public boolean indoorTarinAbsent() {
        if (!runtime.entityState.followingNpcState.marinFollowing()
                && ((runtime.entityState.houseInstrument3Flags & 2) != 0 || runtime.entityState.houseTradeItem >= 4))
            return true;
        return runtime.entityState.houseTarinFlag == 0
                && runtime.entityState.chestPlayerLevelsKnown
                && runtime.entityState.chestSwordLevel != 0;
    }

    public RoomEntitySnapshot snapshot() {
        // These handlers test story state before drawing ($05:4E78,
        // $05:51CE, $05:4BF1). Apply the same gates to pre-tick snapshots,
        // including room transitions which pause entity advancement.
        var presentedSlots = Arrays.stream(runtime.slots)
                .map(entity -> {
                    boolean hidden = entity.type() == 0x3E
                            && (runtime.indoorRoom
                                    // The beach-letter branch has a separate sprite pair, not
                                    // Marin's person sprites. That later presentation is not yet
                                    // ported; retain its previous hidden presentation here.
                                    ? indoorMarinAbsent() || runtime.entityState.houseTradeItem >= 7
                                    : runtime.entityState.chestPlayerLevelsKnown
                                            && runtime.entityState.chestSwordLevel == 0);
                    hidden |= entity.type() == 0x3F && runtime.indoorRoom && indoorTarinAbsent();
                    return hidden ? EntitySnapshots.withVariant(entity, -1) : entity;
                })
                .toList();
        return new RoomEntitySnapshot(
                presentedSlots,
                runtime.entityState.spriteSelection,
                runtime.spriteTiles,
                runtime.entityState.groundInteractionSideScrolling,
                runtime.entityState.hookshotChainOam,
                runtime.entityState.pincerBodyOam,
                runtime.entityState.wingedOctorokOam,
                runtime.entityState.fallingVisualYOffset,
                runtime.entityState.bowWowChainOam);
    }

    List<WingedOctorokOam.Entry> wingedOctorokOam() {
        return runtime.entityState.wingedOctorokOam;
    }

    int wingedOctorokState(int slot) {
        return runtime.entityState.wingedOctorokMotion.state(slot);
    }

    int wingedOctorokSpeedX(int slot) {
        return runtime.entityState.wingedOctorokMotion.speedX(slot);
    }

    int wingedOctorokSpeedY(int slot) {
        return runtime.entityState.wingedOctorokMotion.speedY(slot);
    }

    int wingedOctorokSpeedZ(int slot) {
        return runtime.entityState.wingedOctorokMotion.speedZ(slot);
    }

    int bushCrawlerState(int slot) {
        return runtime.entityState.bushCrawlerMotion.state(slot);
    }

    int bushCrawlerPrivateState1(int slot) {
        return runtime.entityState.bushCrawlerMotion.privateState1(slot);
    }

    int bushCrawlerPrivateState4(int slot) {
        return runtime.entityState.bushCrawlerMotion.privateState4(slot);
    }

    int bushCrawlerSpeedX(int slot) {
        return runtime.entityState.bushCrawlerMotion.speedX(slot);
    }

    int bushCrawlerSpeedY(int slot) {
        return runtime.entityState.bushCrawlerMotion.speedY(slot);
    }

    public int groundStatus(int slot) {
        if (slot < 0 || slot >= runtime.entityState.entityGroundStatus.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.entityState.entityGroundStatus[slot];
    }

    public int bowWowState() {
        return runtime.entityState.bowWowState & 0xFF;
    }

    int moblinKingState(int slot) {
        return runtime.entityState.moblinKingMotion.state(slot);
    }

    public boolean shouldGetLostInMysteriousWoods() {
        return runtime.entityState.shouldGetLostInMysteriousWoods;
    }

    int swordPickupStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.swordPickupState[slot];
    }

    int slowTransitionCountdownForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.slowTransitionCountdown[slot];
    }

    int threeOfAKindStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.threeOfAKindMotion.state(slot);
    }

    int transitionCountdownForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.enemyTransitionCountdown[slot];
    }

    int hinoxStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.hinoxMotion.state(slot);
    }

    int hinoxSpeedXForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.hinoxMotion.speedX(slot);
    }

    int hinoxSpeedYForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.hinoxMotion.speedY(slot);
    }

    int enemyHealthForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.enemyHealth[slot];
    }

    int tarinRaccoonStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.tarinRaccoonMotion.state(slot);
    }

    int bombPrivateState4ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.bombPrivateState4[slot] & 0xFF;
    }

    int entityRoomStatusForTest() {
        return runtime.entityState.entityRoomStatus & 0xFF;
    }

    public int warpTransitionCountdownForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.warpTransitionCountdown[slot] & 0xFF;
    }

    int owlEventStateForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.owlEventState[slot];
    }

    int owlPrivateCountdown1ForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.owlPrivateCountdown1[slot];
    }

    int sideViewPlatformSpeedYForTest(int slot) {
        return runtime.entityState.sideViewPlatformMotion.speedY(slot);
    }

    int sideViewPlatformSpeedXForTest(int slot) {
        return runtime.entityState.sideViewPlatformMotion.speedX(slot);
    }

    int sideViewPlatformPrivateState2ForTest(int slot) {
        return runtime.entityState.sideViewPlatformMotion.privateState2(slot);
    }

    int sideViewPlatformPrivateState4ForTest(int slot) {
        return runtime.entityState.sideViewPlatformMotion.privateState4(slot);
    }

    public boolean witchGotItemPresentationActive() {
        for (int countdown : runtime.entityState.witchGotItemCountdown) {
            if (countdown != 0) {
                return true;
            }
        }
        return false;
    }

    public boolean powerUpPickupSequenceActive() {
        for (int countdown : runtime.entityState.powerUpPickupCountdown) if (countdown != 0) return true;
        return false;
    }

    public boolean swordPickupSequenceActive() {
        for (boolean active : runtime.entityState.swordPickupSequenceActive) {
            if (active) {
                return true;
            }
        }
        return false;
    }

    int likeLikeState(int slot) {
        return runtime.entityState.likeLikeMotion.state(slot);
    }

    int likeLikePrivateState1(int slot) {
        return runtime.entityState.likeLikeMotion.privateState1(slot);
    }

    int likeLikeInertia(int slot) {
        return runtime.entityState.likeLikeMotion.inertia(slot);
    }

    int likeLikeWalkState(int slot) {
        return runtime.entityState.likeLikeMotion.walkState(slot);
    }

    int likeLikeWalkSpeedY(int slot) {
        return runtime.entityState.likeLikeMotion.walkSpeedY(slot);
    }

    public List<HookshotBridgeUpdate> hookshotBridgeUpdates() {
        return List.copyOf(runtime.entityState.events.hookshotBridgeUpdates);
    }

    public RoomEntity firstLoadedEntityOfType(int type) {
        for (RoomEntity candidate : runtime.slots) {
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
        return runtime.entityState.groundInteractionSideScrolling
                ? (linkVerticalVelocity & 0x80) == 0
                : (linkVerticalVelocity & 0x80) != 0;
    }

    boolean hasNoGroundInteractionOverride(int slot) {
        return runtime.entityState.entityOptions1Override[slot] >= 0
                && (runtime.entityState.entityOptions1Override[slot]
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
        return runtime.entityState.linkState.actionButtonAHeld
                && ((yDistance + 0x14) & 0xFF) < 0x28
                && ((xDistance + 0x10) & 0xFF) < 0x20
                && (runtime.entityState.linkState.lastRomLinkDirection ^ 0x01) == entityDirection
                && !runtime.entityState.linkState.dialogActive
                && !runtime.entityState.linkState.inventoryAppearing
                && !linkAirborne
                && runtime.entityState.linkState.dialogCooldown == 0
                && runtime.entityState.linkState.windowY == 0x80;
    }

    public boolean heartPiecePickupActive() {
        for (var motion : runtime.entityState.heartPiecePickup) if (motion != null && motion.active()) return true;
        return false;
    }

    public HeartPieceVisual heartPieceVisual() {
        for (var motion : runtime.entityState.heartPiecePickup)
            if (motion != null && motion.showMeter())
                return new HeartPieceVisual(
                        runtime.entityState.heartPiecesCount, runtime.entityState.heartPieceMeterPairs);
        return null;
    }

    public String instrumentTrace() {
        StringBuilder trace = new StringBuilder().append(runtime.entityState.activeMusic ? 1 : 0);
        for (RoomEntity entity : runtime.slots) {
            if (entity.loaded()
                    && entity.type() == 0x39
                    && runtime.entityState.instrumentParticleKind[entity.slot()] == 0) {
                int slot = entity.slot();
                trace.append(';')
                        .append(slot)
                        .append(',')
                        .append(runtime.entityState.instrumentPickupState[slot])
                        .append(',')
                        .append(runtime.entityState.enemyTransitionCountdown[slot])
                        .append(',')
                        .append(runtime.entityState.slowTransitionCountdown[slot]);
            }
        }
        return trace.toString();
    }

    public String moblinKingTrace() {
        for (RoomEntity entity : runtime.slots)
            if (entity.loaded() && entity.type() == EntityRomConstants.ENTITY_MOBLIN_KING) {
                int slot = entity.slot();
                return "%d,%d,%d,%d,%d,%d,%d"
                        .formatted(
                                runtime.entityState.moblinKingMotion.state(slot),
                                entity.x(),
                                entity.y(),
                                entity.z(),
                                runtime.entityState.enemyHealth[slot],
                                runtime.entityState.enemyTransitionCountdown[slot],
                                runtime.entityState.slowTransitionCountdown[slot]);
            }
        return "-1";
    }

    public InstrumentWarpVisual instrumentWarpVisual() {
        for (int slot = 0; slot < runtime.entityState.instrumentPickupState.length; slot++) {
            // The ROM leaves the white BG/palettes in VRAM after the controller
            // finishes. Keep them until the destination room replaces this runtime.
            if (runtime.entityState.instrumentPickupState[slot] == 3
                    || runtime.entityState.instrumentPickupState[slot] == 4) {
                return new InstrumentWarpVisual(
                        runtime.entityState.instrumentBackgroundFade[slot],
                        runtime.entityState.instrumentObjectFade[slot],
                        runtime.entityState.instrumentHintBatches[slot],
                        runtime.entityState.instrumentHintTiles,
                        runtime.entityState.instrumentHintAttributes,
                        runtime.entityState.instrumentWarpX[slot],
                        runtime.entityState.instrumentWarpY[slot],
                        runtime.entityState.instrumentWarpSprites);
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

    int enemyDropHealthGroup(int slot) {
        if (runtime.enemyCombatTables == null) {
            throw new IllegalStateException("ROM enemy combat tables are required for enemy drop resolution");
        }
        return runtime.entityState.enemyHealthGroup[slot];
    }

    EntitySpriteDefinition spriteDefinitionFor(int entityType) {
        if (runtime.spriteHandlers == null) {
            return EntitySpriteDefinition.unsupported(entityType);
        }
        EntityRoomLoader.RoomTable table = runtime.entityState.spriteSelection == null
                ? (runtime.indoorRoom ? EntityRoomLoader.RoomTable.INDOORS_A : EntityRoomLoader.RoomTable.OVERWORLD)
                : runtime.entityState.spriteSelection.roomTable();
        int mapId = runtime.entityState.spriteSelection == null ? -1 : runtime.entityState.spriteSelection.roomId();
        return runtime.spriteHandlers.forEntityType(entityType, table, mapId);
    }

    EntitySpriteDefinition spriteDefinitionForBushCrawlerState(int privateState1) {
        if (runtime.spriteHandlers == null) {
            return EntitySpriteDefinition.unsupported(EntityRomConstants.ENTITY_BUSH_CRAWLER);
        }
        return runtime.spriteHandlers.forBushCrawlerState(privateState1, spriteRoomTable());
    }

    EntitySpriteDefinition spriteDefinitionForPokeySegment() {
        return runtime.spriteHandlers == null
                ? EntitySpriteDefinition.unsupported(EntityRomConstants.ENTITY_POKEY)
                : runtime.spriteHandlers.forPokeySegment();
    }

    EntitySpriteDefinition spriteDefinitionForBushCrawlerCrawlState(int privateState4) {
        if (runtime.spriteHandlers == null) {
            return EntitySpriteDefinition.unsupported(EntityRomConstants.ENTITY_BUSH_CRAWLER);
        }
        return runtime.spriteHandlers.forBushCrawlerCrawlState(spriteRoomTable(), privateState4);
    }

    EntityRoomLoader.RoomTable spriteRoomTable() {
        return runtime.entityState.spriteSelection == null
                ? (runtime.indoorRoom ? EntityRoomLoader.RoomTable.INDOORS_A : EntityRoomLoader.RoomTable.OVERWORLD)
                : runtime.entityState.spriteSelection.roomTable();
    }

    EntitySpriteDefinition spriteDefinitionForEvasiveState(EntitySpriteDefinition fallback, int privateState1) {
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
                entity.type(), runtime.entityState.colorShellMotion.state(entity.slot()), status);
        return withDefinition(entity, definition, runtime.entityState.colorShellMotion.spriteVariant(entity.slot()));
    }

    int slowTransitionCountdown(int slot) {
        if (slot < 0 || slot >= runtime.slots.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return runtime.entityState.slowTransitionCountdown[slot];
    }

    int droppedItemForTest(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.droppedItemBySlot[slot];
    }

    int killCount() {
        return runtime.entityState.killCount;
    }

    public int killOrderAt(int index) {
        if (index < 0 || index >= runtime.entityState.killOrder.length) {
            throw new IllegalArgumentException("Kill-order index out of range: " + index);
        }
        return runtime.entityState.killOrder[index];
    }

    public EnemyDropResolver.CounterState enemyDropCounters() {
        return runtime.entityState.enemyDropCounters;
    }

    int dropPrivateCountdown1(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.dropPrivateCountdown1[slot];
    }

    int dropPrivateCountdown3(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.dropPrivateCountdown3[slot];
    }

    int droppablePrivateState3(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.droppablePrivateState3[slot];
    }

    int droppablePrivateState4(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.droppablePrivateState4[slot];
    }

    int dropSpeedY(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.enemyDropMotion.speedY(slot);
    }

    public int dropSpeedX(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.enemyDropMotion.speedX(slot);
    }

    public int dropSpeedZ(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.enemyDropMotion.speedZ(slot);
    }

    int fairySpeedX(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.fairyMotion.speedX(slot);
    }

    int fairySpeedY(int slot) {
        EntitySlotValidation.validateEntitySlot(slot);
        return runtime.entityState.fairyMotion.speedY(slot);
    }

    public int entityMapId() {
        return runtime.entityState.entityMapId;
    }

    public int entityRoomId() {
        return runtime.entityState.entityRoomId;
    }

    public int entityRoomStatus() {
        return runtime.entityState.entityRoomStatus;
    }

    public boolean indoorRoom() {
        return runtime.indoorRoom;
    }

    public boolean roomTransitionActive() {
        return runtime.entityState.roomTransitionActive;
    }

    public boolean gameplayWorld() {
        return runtime.entityState.gameplayWorld;
    }

    public boolean freeMovementMode() {
        return runtime.entityState.freeMovementMode;
    }

    public int chestSwordLevel() {
        return runtime.entityState.chestSwordLevel;
    }

    public boolean chestPlayerLevelsKnown() {
        return runtime.entityState.chestPlayerLevelsKnown;
    }

    public boolean inBossBattle() {
        return runtime.entityState.inBossBattle;
    }

    public boolean dungeonMinibossReady() {
        return runtime.entityState.dungeonMinibossReady;
    }

    public int defaultMusicTrack() {
        return runtime.entityState.defaultMusicTrack;
    }

    public ColorShellWorld colorShellWorld() {
        return runtime.entityState.colorShellWorld;
    }
}
