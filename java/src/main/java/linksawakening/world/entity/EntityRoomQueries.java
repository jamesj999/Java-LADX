package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.InstrumentWarpVisual;
import linksawakening.world.entity.event.LiftedEntityState;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.ColorShellMotion;

/** Snapshot, presentation and collision queries over the narrow room inspection services. */
final class EntityRoomQueries {
    static EntityRoomLoader.RoomTable spriteRoomTable(EntityRoomInspectionServices runtime) {
        return runtime.presentationState().readSpriteSelection() == null
                ? (runtime.indoorRoom() ? EntityRoomLoader.RoomTable.INDOORS_A : EntityRoomLoader.RoomTable.OVERWORLD)
                : runtime.presentationState().readSpriteSelection().roomTable();
    }

    static LiftedEntityState liftedEntityState(EntityRoomInspectionServices runtime) {
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

    static int activeProjectileCount(EntityRoomInspectionServices runtime) {
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

    static boolean indoorMarinAbsent(EntityRoomInspectionServices runtime) {
        if (runtime.motions()
                .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                .readFollowingNpcState()
                .marinFollowing()) return true;
        if (runtime.story().readHouseTradeItem() >= 7) return (runtime.story().readHouseWalrusStatus() & 0x30) != 0;
        return runtime.pickups().readChestPlayerLevelsKnown()
                && runtime.pickups().readChestSwordLevel() != 0;
    }

    static boolean indoorTarinAbsent(EntityRoomInspectionServices runtime) {
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

    static RoomEntitySnapshot snapshot(EntityRoomInspectionServices runtime) {
        // These handlers test story state before drawing ($05:4E78,
        // $05:51CE, $05:4BF1). Apply the same gates to pre-tick snapshots,
        // including room transitions which pause entity advancement.
        var presentedSlots = new java.util.ArrayList<RoomEntity>(runtime.slots().length);
        for (int slot = 0; slot < runtime.slots().length; slot++) presentedSlots.add(presentedEntity(runtime, slot));
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

    static boolean canGoombaStomp(
            EntityRoomInspectionServices runtime,
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

    static boolean hasNoGroundInteractionOverride(EntityRoomInspectionServices runtime, int slot) {
        return runtime.physicsTables().readEntityOptions1Override(slot) >= 0
                && (runtime.physicsTables().readEntityOptions1Override(slot)
                                & EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION)
                        != 0;
    }

    static boolean hasNoGroundInteraction(EntityRoomInspectionServices runtime, RoomEntity entity) {
        return isGhiniType(entity.type())
                || entity.type() == EntityRomConstants.ENTITY_MAD_BOMBER
                || entity.type() == EntityRomConstants.ENTITY_WIZROBE_PROJECTILE
                || entity.type() == EntityRomConstants.ENTITY_GIANT_GOPONGA_FLOWER
                || entity.type() == EntityRomConstants.ENTITY_GOPONGA_FLOWER_PROJECTILE
                || entity.type() == EntityRomConstants.ENTITY_BOO_BUDDY
                || entity.type() == EntityRomConstants.ENTITY_DROPPABLE_FAIRY
                || entity.type() == EntityRomConstants.ENTITY_PINCER
                || entity.type() == EntityRomConstants.ENTITY_MOLDORM
                || hasNoGroundInteractionOverride(runtime, entity.slot());
    }

    static boolean sourceNpcInteractionAllowed(
            EntityRoomInspectionServices runtime,
            int xDistance,
            int yDistance,
            boolean linkAirborne,
            int entityDirection) {
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

    static String instrumentTrace(EntityRoomInspectionServices runtime) {
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

    static String moblinKingTrace(EntityRoomInspectionServices runtime) {
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

    static InstrumentWarpVisual instrumentWarpVisual(EntityRoomInspectionServices runtime) {
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

    static EntitySpriteDefinition spriteDefinitionFor(EntityRoomInspectionServices runtime, int entityType) {
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

    static RoomEntity refreshColorShellDisplay(
            EntityRoomInspectionServices runtime, RoomEntity entity, EntityStatus status) {
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

    static RoomEntity presentedEntity(EntityRoomInspectionServices runtime, int slot) {
        RoomEntity entity = runtime.slots()[slot];
        boolean hidden = entity.type() == 0x3E
                && (runtime.indoorRoom()
                        ? indoorMarinAbsent(runtime) || runtime.story().readHouseTradeItem() >= 7
                        : runtime.pickups().readChestPlayerLevelsKnown()
                                && runtime.pickups().readChestSwordLevel() == 0);
        hidden |= entity.type() == 0x3F && runtime.indoorRoom() && indoorTarinAbsent(runtime);
        return hidden && entity.spriteVariant() != -1 ? EntitySnapshots.withVariant(entity, -1) : entity;
    }
}
