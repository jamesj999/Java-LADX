package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BombMotion;

/** Source-ordered allocation and initialization for this spawn family. */
final class PlayerBombSpawns {
    static int spawnBomb(
            EntityPlayerSpawns runtime, int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        EntitySlotValidation.validateRomDirection(romDirection);
        runtime.bombInteractions.state.writeLastBombPlacementPlayedBump(false);
        runtime.bombInteractions.finalizePendingBombPresentations();
        if (runtime.inspection.bombActive()) {
            return -1;
        }
        int freeSlot = EntitySlotAllocator.findFreeSlot(runtime.slots, runtime.combatTables, runtime.slots.length - 1);
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_BOMB);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity bomb = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_BOMB,
                linkEntityX & 0xFF,
                linkEntityY & 0xFF,
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                (linkEntityZ + 1) & 0xFF,
                -1,
                false);
        runtime.slots[freeSlot] = bomb;
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        runtime.bombInteractions.state.writeBombDirection(freeSlot, romDirection);
        runtime.motions.get(ThrownEntityMotion.class, ThrownEntityMotion::new).startPlacedBomb(freeSlot, romDirection);
        runtime.bombInteractions.state.writePlacedBombMotionInitialized(freeSlot, true);
        runtime.bombInteractions.state.writePlayerArrowBombArrow(freeSlot, false);
        runtime.bombInteractions.state.setBombFinalPresentationPending(freeSlot, false);
        runtime.bombInteractions.state.writeBombPrivateState4(freeSlot, 0);
        runtime.bombInteractions.state.writeBombPrivateCountdown1(freeSlot, 0x10);
        runtime.bombInteractions.state.writeBombPrivateCountdown3(freeSlot, 0);
        runtime.combatTables.writeEnemyHitboxFlags(freeSlot, 0);
        runtime.physicsTables.writeEnemyTransitionCountdown(freeSlot, BombMotion.INITIAL_COUNTDOWN);
        runtime.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyHealth(freeSlot, 0);
        runtime.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        runtime.physicsTables.writeEnemyPhysicsFlags(freeSlot, EntityRomConstants.BOMB_INITIAL_PHYSICS_FLAGS);
        runtime.physicsTables.writeEntityGroundStatus(freeSlot, 0);
        runtime.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables.writeEntityOptions1Override(freeSlot, EntityRomConstants.BOMB_OPTIONS1);
        if (runtime.bombInteractions.state.readBombArrowCooldown() != 0) {
            runtime.bombInteractions.state.writeBombArrowCooldown(0);
            int arrowSlot = runtime.projectiles.readLatestShotArrowEntityIndex();
            if (runtime.roomInspection.isLoadedEntityOfType(arrowSlot, EntityRomConstants.ENTITY_ARROW)) {
                runtime.bombInteractions.markArrowAsBombArrow(arrowSlot);
            }
        } else {
            runtime.bombInteractions.state.writeBombArrowCooldown(EntityRomConstants.BOMB_ARROW_COOLDOWN);
            runtime.bombInteractions.state.writeLatestDroppedBombEntityIndex(freeSlot);
            runtime.bombInteractions.applyInitialBombArrowCandidateState(freeSlot, romDirection);
        }
        return freeSlot;
    }

    static int spawnBombFromBombArrow(EntityPlayerSpawns runtime, RoomEntity arrow) {
        int freeSlot = EntitySlotAllocator.findFreeSlot(runtime.slots, runtime.combatTables, runtime.slots.length - 1);
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_BOMB);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity bomb = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_BOMB,
                arrow.x(),
                arrow.y(),
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                arrow.z(),
                -1,
                false);
        runtime.slots[freeSlot] = bomb;
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        runtime.bombInteractions.state.writeBombDirection(
                freeSlot,
                runtime.motions
                        .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                        .direction(arrow.slot()));
        runtime.bombInteractions.state.setBombFinalPresentationPending(freeSlot, false);
        runtime.bombInteractions.state.writeBombPrivateState4(freeSlot, 0);
        runtime.bombInteractions.state.writeBombPrivateCountdown1(freeSlot, 0);
        runtime.bombInteractions.state.writeBombPrivateCountdown3(freeSlot, 0);
        runtime.combatTables.writeEnemyHitboxFlags(freeSlot, 0);
        runtime.physicsTables.writeEnemyTransitionCountdown(
                freeSlot, EntityRomConstants.BOMB_ARROW_EXPLOSION_COUNTDOWN);
        runtime.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyHealth(freeSlot, 0);
        runtime.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        runtime.physicsTables.writeEnemyPhysicsFlags(freeSlot, EntityRomConstants.BOMB_INITIAL_PHYSICS_FLAGS);
        runtime.physicsTables.writeEntityGroundStatus(freeSlot, 0);
        runtime.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables.writeEntityOptions1Override(freeSlot, EntityRomConstants.BOMB_OPTIONS1);
        runtime.bombInteractions.state.writePlacedBombMotionInitialized(freeSlot, true);
        runtime.motions.get(ThrownEntityMotion.class, ThrownEntityMotion::new).clear(freeSlot);
        runtime.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return freeSlot;
    }
}
