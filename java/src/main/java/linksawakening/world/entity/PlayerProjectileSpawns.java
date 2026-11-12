package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.MagicRodFireballMotion;

/** Source-ordered allocation and initialization for this spawn family. */
final class PlayerProjectileSpawns {
    static int spawnArrow(
            EntityPlayerSpawnsServices runtime, int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return PlayerProjectileSpawns.spawnArrow(runtime, linkEntityX, linkEntityY, linkEntityZ, romDirection, false);
    }

    static int spawnArrow(
            EntityPlayerSpawnsServices runtime,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int romDirection,
            boolean pieceOfPower) {
        EntitySlotValidation.validateRomDirection(romDirection);
        runtime.projectiles().writeLastArrowShotPlayedWhoosh(false);
        if (runtime.inspection().activeProjectileCount() >= 0x02) {
            return -1;
        }
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_ARROW);
        int variant = romDirection;
        RoomEntity arrow = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_ARROW,
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
        runtime.slots()[freeSlot] = arrow;
        runtime.initializeHealthGroup(freeSlot);
        runtime.motions().get(PlayerArrowMotion.class, PlayerArrowMotion::new).clear(freeSlot);
        runtime.motions()
                .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                .initializeSpawn(freeSlot, romDirection, pieceOfPower);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables()
                .writeEntityOptions1Override(
                        freeSlot,
                        EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                                | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        runtime.physicsTables()
                .writeEnemyPhysicsFlags(freeSlot, 2 | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        runtime.combatTables().writeEnemyHealth(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        runtime.projectiles().writeLatestShotArrowEntityIndex(freeSlot);
        if (runtime.bombInteractions().readBombArrowCooldown() != 0) {
            int bombSlot = runtime.bombInteractions().readLatestDroppedBombEntityIndex();
            runtime.bombInteractions().writeBombArrowCooldown(0);
            if (runtime.roomInspection().isLoadedEntityOfType(bombSlot, EntityRomConstants.ENTITY_BOMB)) {
                runtime.lifecycle().disableEntityWithoutPersistence(bombSlot);
            }
            runtime.bombInteractions().writeLatestDroppedBombEntityIndex(-1);
            runtime.bombInteractions().writePlayerArrowBombArrow(freeSlot, true);
            if (runtime.spriteHandlers() != null) {
                runtime.slots()[freeSlot] = withDefinition(
                        runtime.slots()[freeSlot], runtime.spriteHandlers().forBombArrow(), romDirection);
            }
        } else {
            runtime.bombInteractions().writeBombArrowCooldown(EntityRomConstants.BOMB_ARROW_COOLDOWN);
            runtime.projectiles().writeLastArrowShotPlayedWhoosh(true);
        }
        return freeSlot;
    }

    static int spawnBoomerang(
            EntityPlayerSpawnsServices runtime,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int romDirection,
            int pressedButtonsMask) {
        EntitySlotValidation.validateRomDirection(romDirection);
        if (runtime.inspection().activeProjectileCount() != 0) {
            return -1;
        }
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_BOOMERANG);
        RoomEntity boomerang = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_BOOMERANG,
                linkEntityX & 0xFF,
                linkEntityY & 0xFF,
                EntityStatus.ACTIVE,
                definition,
                romDirection,
                0,
                0,
                (linkEntityZ + 1) & 0xFF,
                -1,
                false);
        runtime.slots()[freeSlot] = boomerang;
        runtime.initializeHealthGroup(freeSlot);
        runtime.motions().get(BoomerangMotion.class, BoomerangMotion::new).clear(freeSlot);
        runtime.motions()
                .get(BoomerangMotion.class, BoomerangMotion::new)
                .initializeSpawn(freeSlot, romDirection, pressedButtonsMask);
        runtime.liftThrow().writeThrownDirection(freeSlot, romDirection);
        runtime.physicsTables().writeEntityUnknownJ(freeSlot, 1);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables()
                .writeEntityOptions1Override(
                        freeSlot,
                        EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                                | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        runtime.physicsTables()
                .writeEnemyPhysicsFlags(freeSlot, 2 | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        runtime.combatTables().writeEnemyHealth(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        return freeSlot;
    }

    static int spawnSwordBeam(
            EntityPlayerSpawnsServices runtime, int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        EntitySlotValidation.validateRomDirection(romDirection);
        if (runtime.inspection().activeProjectileCount() != 0) {
            return -1;
        }
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_SWORD_BEAM);
        int variant = definition.supported() ? romDirection : -1;
        RoomEntity beam = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_SWORD_BEAM,
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
        runtime.slots()[freeSlot] = beam;
        runtime.initializeHealthGroup(freeSlot);
        runtime.motions().get(SwordBeamMotion.class, SwordBeamMotion::new).clear(freeSlot);
        runtime.motions().get(SwordBeamMotion.class, SwordBeamMotion::new).initializeSpawn(freeSlot, romDirection);
        runtime.liftThrow().writeThrownDirection(freeSlot, romDirection);
        runtime.physicsTables().writeEntityUnknownJ(freeSlot, 1);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables()
                .writeEntityOptions1Override(
                        freeSlot,
                        EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                                | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        runtime.physicsTables()
                .writeEnemyPhysicsFlags(freeSlot, 2 | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        runtime.combatTables().writeEnemyHealth(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.combatTables().writeEnemyHitboxFlags(freeSlot, 0);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        return freeSlot;
    }

    static int spawnMagicRodFireball(
            EntityPlayerSpawnsServices runtime, int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return PlayerProjectileSpawns.spawnMagicRodFireball(
                runtime, linkEntityX, linkEntityY, linkEntityZ, romDirection, false);
    }

    static int spawnMagicRodFireball(
            EntityPlayerSpawnsServices runtime,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int romDirection,
            boolean pieceOfPower) {
        EntitySlotValidation.validateRomDirection(romDirection);
        if (runtime.inspection().activeProjectileCount() >= 0x02) {
            return -1;
        }
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_MAGIC_ROD_FIREBALL);
        MagicRodFireballMotion.SpawnData spawn = MagicRodFireballMotion.spawnData(romDirection, pieceOfPower);
        int variant = definition.supported() ? spawn.initialVariant() : -1;
        RoomEntity fireball = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_MAGIC_ROD_FIREBALL,
                (linkEntityX + spawn.offsetX()) & 0xFF,
                (linkEntityY + spawn.offsetY()) & 0xFF,
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                (linkEntityZ + 1) & 0xFF,
                -1,
                false);
        runtime.slots()[freeSlot] = fireball;
        runtime.initializeHealthGroup(freeSlot);
        runtime.motions()
                .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                .clear(freeSlot);
        runtime.motions()
                .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                .initializeSpawn(freeSlot, romDirection, pieceOfPower);
        runtime.liftThrow().writeThrownDirection(freeSlot, romDirection);
        runtime.physicsTables().writeEntityUnknownJ(freeSlot, 1);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables()
                .writeEntityOptions1Override(
                        freeSlot,
                        EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION
                                | EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        runtime.physicsTables()
                .writeEnemyPhysicsFlags(freeSlot, 2 | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        runtime.combatTables().writeEnemyHealth(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.combatTables().writeEnemyHitboxFlags(freeSlot, 0);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        return freeSlot;
    }

    static int spawnMagicPowderSprinkle(
            EntityPlayerSpawnsServices runtime, int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        EntitySlotValidation.validateRomDirection(romDirection);
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return -1;
        }

        int[] xOffsets = {0x0E, -0x0E, 0x00, 0x00};
        int[] yOffsets = {0x00, 0x00, -0x0C, 0x0C};
        EntitySpriteDefinition definition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_MAGIC_POWDER_SPRINKLE);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity sprinkle = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_MAGIC_POWDER_SPRINKLE,
                (linkEntityX + xOffsets[romDirection]) & 0xFF,
                (linkEntityY + yOffsets[romDirection]) & 0xFF,
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                linkEntityZ & 0xFF,
                -1,
                false);
        runtime.slots()[freeSlot] = sprinkle;
        runtime.initializeHealthGroup(freeSlot);
        runtime.projectiles().writeMagicPowderState(freeSlot, 0);
        runtime.physicsTables()
                .writeEnemyTransitionCountdown(freeSlot, EntityRomConstants.MAGIC_POWDER_TRANSITION_COUNTDOWN);
        runtime.physicsTables().writeSlowTransitionCountdown(freeSlot, 0);
        runtime.physicsTables().writeSlowTimerInitialized(freeSlot, false);
        runtime.combatTables().writeEnemyHitboxFlags(freeSlot, 0);
        runtime.physicsTables()
                .writeEnemyPhysicsFlags(
                        freeSlot,
                        0x03
                                | EntityRomConstants.ENTITY_PHYSICS_HARMLESS
                                | EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP);
        runtime.combatTables().writeEnemyHealth(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables()
                .writeEntityOptions1Override(freeSlot, EntityRomConstants.ENTITY_OPT1_EXCLUDED_FROM_KILL_ALL);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return freeSlot;
    }
}
