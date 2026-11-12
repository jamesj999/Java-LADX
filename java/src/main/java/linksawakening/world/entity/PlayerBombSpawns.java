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
            EntityPlayerSpawnsServices runtime, int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        EntitySlotValidation.validateRomDirection(romDirection);
        runtime.bombInteractions().writeLastBombPlacementPlayedBump(false);
        runtime.bombInteractions().finalizePendingBombPresentations();
        if (runtime.inspection().bombActive()) {
            return -1;
        }
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_BOMB);
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
        runtime.slots()[freeSlot] = bomb;
        runtime.initializeHealthGroup(freeSlot);
        runtime.bombInteractions().writeBombDirection(freeSlot, romDirection);
        runtime.motions()
                .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                .startPlacedBomb(freeSlot, romDirection);
        runtime.bombInteractions().writePlacedBombMotionInitialized(freeSlot, true);
        runtime.bombInteractions().writePlayerArrowBombArrow(freeSlot, false);
        runtime.setBombFinalPresentationPending(freeSlot, false);
        runtime.bombInteractions().writeBombPrivateState4(freeSlot, 0);
        runtime.bombInteractions().writeBombPrivateCountdown1(freeSlot, 0x10);
        runtime.bombInteractions().writeBombPrivateCountdown3(freeSlot, 0);
        runtime.combatTables().writeEnemyHitboxFlags(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, BombMotion.INITIAL_COUNTDOWN);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyHealth(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        runtime.physicsTables().writeEnemyPhysicsFlags(freeSlot, EntityRomConstants.BOMB_INITIAL_PHYSICS_FLAGS);
        runtime.physicsTables().writeEntityGroundStatus(freeSlot, 0);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables().writeEntityOptions1Override(freeSlot, EntityRomConstants.BOMB_OPTIONS1);
        if (runtime.bombInteractions().readBombArrowCooldown() != 0) {
            runtime.bombInteractions().writeBombArrowCooldown(0);
            int arrowSlot = runtime.projectiles().readLatestShotArrowEntityIndex();
            if (runtime.roomInspection().isLoadedEntityOfType(arrowSlot, EntityRomConstants.ENTITY_ARROW)) {
                runtime.bombInteractions().markArrowAsBombArrow(arrowSlot);
            }
        } else {
            runtime.bombInteractions().writeBombArrowCooldown(EntityRomConstants.BOMB_ARROW_COOLDOWN);
            runtime.bombInteractions().writeLatestDroppedBombEntityIndex(freeSlot);
            runtime.bombInteractions().applyInitialBombArrowCandidateState(freeSlot, romDirection);
        }
        return freeSlot;
    }

    static int spawnBombFromBombArrow(EntityPlayerSpawnsServices runtime, RoomEntity arrow) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_BOMB);
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
        runtime.slots()[freeSlot] = bomb;
        runtime.initializeHealthGroup(freeSlot);
        runtime.bombInteractions()
                .writeBombDirection(
                        freeSlot,
                        runtime.motions()
                                .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                                .direction(arrow.slot()));
        runtime.setBombFinalPresentationPending(freeSlot, false);
        runtime.bombInteractions().writeBombPrivateState4(freeSlot, 0);
        runtime.bombInteractions().writeBombPrivateCountdown1(freeSlot, 0);
        runtime.bombInteractions().writeBombPrivateCountdown3(freeSlot, 0);
        runtime.combatTables().writeEnemyHitboxFlags(freeSlot, 0);
        runtime.physicsTables()
                .writeEnemyTransitionCountdown(freeSlot, EntityRomConstants.BOMB_ARROW_EXPLOSION_COUNTDOWN);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyHealth(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        runtime.physicsTables().writeEnemyPhysicsFlags(freeSlot, EntityRomConstants.BOMB_INITIAL_PHYSICS_FLAGS);
        runtime.physicsTables().writeEntityGroundStatus(freeSlot, 0);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables().writeEntityOptions1Override(freeSlot, EntityRomConstants.BOMB_OPTIONS1);
        runtime.bombInteractions().writePlacedBombMotionInitialized(freeSlot, true);
        runtime.motions().get(ThrownEntityMotion.class, ThrownEntityMotion::new).clear(freeSlot);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return freeSlot;
    }
}
