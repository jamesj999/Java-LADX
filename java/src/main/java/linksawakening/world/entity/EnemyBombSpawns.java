package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.motion.*;

/** Source-ordered allocation and initialization for this spawn family. */
final class EnemyBombSpawns {
    static int spawnEnemyBomb(
            EntityEnemySpawnsServices runtime, int entityX, int entityY, int entityZ, int transitionCountdown) {
        return EnemyBombSpawns.spawnEnemyBomb(
                runtime, new EnemyBombSpawn(entityX, entityY, entityZ, transitionCountdown, 0, 0, 0, false, 0x01));
    }

    static int spawnEnemyBomb(
            EntityEnemySpawnsServices runtime,
            int entityX,
            int entityY,
            int entityZ,
            int transitionCountdown,
            int speedX,
            int speedY,
            int speedZ) {
        return EnemyBombSpawns.spawnEnemyBomb(
                runtime,
                new EnemyBombSpawn(entityX, entityY, entityZ, transitionCountdown, speedX, speedY, speedZ, true, 0x01));
    }

    static int spawnEnemyBomb(EntityEnemySpawnsServices runtime, EnemyBombSpawn inputs) {
        int entityX = inputs.entityX();
        int entityY = inputs.entityY();
        int entityZ = inputs.entityZ();
        int transitionCountdown = inputs.transitionCountdown();
        int speedX = inputs.speedX();
        int speedY = inputs.speedY();
        int speedZ = inputs.speedZ();
        boolean initializeMotion = inputs.initializeMotion();
        int privateState4 = inputs.privateState4();
        EntitySlotValidation.validateByte(entityX, "Enemy bomb X");
        EntitySlotValidation.validateByte(entityY, "Enemy bomb Y");
        EntitySlotValidation.validateByte(entityZ, "Enemy bomb Z");
        EntitySlotValidation.validateByte(transitionCountdown, "Enemy bomb transition countdown");
        EntitySlotValidation.validateByte(speedX, "Enemy bomb X speed");
        EntitySlotValidation.validateByte(speedY, "Enemy bomb Y speed");
        EntitySlotValidation.validateByte(speedZ, "Enemy bomb Z speed");
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
                entityX,
                entityY,
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                entityZ,
                -1,
                false);
        runtime.slots()[freeSlot] = bomb;
        runtime.initializeHealthGroup(freeSlot);
        runtime.bombInteractions().writeBombDirection(freeSlot, 0xFF);
        runtime.bombInteractions().writePlayerArrowBombArrow(freeSlot, false);
        runtime.setBombFinalPresentationPending(freeSlot, false);
        runtime.bombInteractions().writeBombPrivateState4(freeSlot, privateState4 & 0xFF);
        runtime.bombInteractions().writeBombPrivateCountdown1(freeSlot, 0);
        runtime.bombInteractions().writeBombPrivateCountdown3(freeSlot, 0);
        runtime.bombInteractions().writePlacedBombMotionInitialized(freeSlot, false);
        runtime.bombInteractions().writeEnemyBombMotionInitialized(freeSlot, initializeMotion);
        if (initializeMotion) {
            runtime.motions()
                    .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                    .startEnemyBomb(freeSlot, speedX, speedY, speedZ);
        } else {
            runtime.motions()
                    .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                    .clear(freeSlot);
        }
        runtime.liftThrow().writeThrownMotionInitialized(freeSlot, false);
        runtime.combatTables().writeEnemyHitboxFlags(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, transitionCountdown);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyHealth(freeSlot, runtime.initialHealth(EntityRomConstants.ENTITY_BOMB));
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, initializeMotion ? 1 : 0);
        runtime.physicsTables().writeEnemyPhysicsFlags(freeSlot, EntityRomConstants.BOMB_INITIAL_PHYSICS_FLAGS);
        runtime.physicsTables().writeEntityGroundStatus(freeSlot, 0);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables().writeEntityOptions1Override(freeSlot, EntityRomConstants.BOMB_OPTIONS1);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return freeSlot;
    }

    static int spawnTarinTransformationBomb(EntityEnemySpawnsServices runtime, RoomEntity tarin) {
        return EnemyBombSpawns.spawnEnemyBomb(
                runtime, new EnemyBombSpawn(tarin.x(), tarin.y(), tarin.z(), 0x20, 0, 0, 0, false, 0x4C));
    }

    static boolean spawnKanaletWallMoblin(EntityEnemySpawnsServices runtime, RoomEntity wall) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return false;
        }
        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_MOBLIN_SWORD);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        runtime.slots()[freeSlot] = new RoomEntity(
                freeSlot,
                0xFF,
                EntityRomConstants.ENTITY_MOBLIN_SWORD,
                wall.x(),
                (wall.y() + 0x08) & 0xFF,
                EntityStatus.INIT,
                definition,
                variant,
                0,
                0,
                wall.z(),
                -1,
                false);
        runtime.initializeHealthGroup(freeSlot);
        runtime.pickups().resetEnemyDropState(freeSlot);
        runtime.bossRewards().writeDroppedItemBySlot(freeSlot, EntityRomConstants.ENTITY_HIDING_SLIME_KEY);
        runtime.physicsTables()
                .writeEnemyPhysicsFlags(freeSlot, initialPhysicsFlags(EntityRomConstants.ENTITY_MOBLIN_SWORD));
        runtime.combatTables()
                .writeEnemyHealth(freeSlot, runtime.initialHealth(EntityRomConstants.ENTITY_MOBLIN_SWORD));
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.physicsTables().writeEntityOptions1Override(freeSlot, -1);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return true;
    }
}
