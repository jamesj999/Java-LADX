package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.EnemyProjectileMotion;
import linksawakening.world.entity.motion.GiantGopongaMotion;
import linksawakening.world.entity.motion.GopongaProjectileMotion;
import linksawakening.world.entity.motion.RoamingEnemyMotion;
import linksawakening.world.entity.motion.WizrobeMotion;
import linksawakening.world.entity.motion.ZoraMotion;

/** Source-ordered allocation and initialization for this spawn family. */
final class EnemyProjectileSpawns {
    static void spawnPairoddProjectile(
            EntityEnemySpawnsServices runtime, RoomEntity source, int linkEntityX, int linkEntityY) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition projectileDefinition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_PAIRODD_PROJECTILE);
        int projectileVariant = projectileDefinition.supported() ? projectileDefinition.initialVariant() : -1;
        RoomEntity projectile = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_PAIRODD_PROJECTILE,
                source.x(),
                source.y(),
                EntityStatus.ACTIVE,
                projectileDefinition,
                projectileVariant,
                0,
                0,
                source.z(),
                -1,
                false);
        runtime.slots()[freeSlot] = projectile;
        runtime.initializeHealthGroup(freeSlot);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables()
                .writeEnemyHealth(freeSlot, runtime.initialHealth(EntityRomConstants.ENTITY_PAIRODD_PROJECTILE));
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(freeSlot);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        runtime.motions()
                .get(PairoddProjectileMotion.class, PairoddProjectileMotion::new)
                .initializeSpawn(freeSlot, source, linkEntityX, linkEntityY);
    }

    static void spawnGopongaProjectile(
            EntityEnemySpawnsServices runtime, RoomEntity source, GiantGopongaMotion.ProjectileSpawn spawn) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition projectileDefinition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_GOPONGA_FLOWER_PROJECTILE);
        int projectileVariant = projectileDefinition.supported() ? projectileDefinition.initialVariant() : -1;
        RoomEntity projectile = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_GOPONGA_FLOWER_PROJECTILE,
                spawn.x(),
                spawn.y(),
                EntityStatus.ACTIVE,
                projectileDefinition,
                projectileVariant,
                0,
                0,
                source.z(),
                -1,
                false);
        runtime.slots()[freeSlot] = projectile;
        runtime.initializeHealthGroup(freeSlot);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables().writeEntityOptions1Override(freeSlot, GopongaProjectileMotion.OPTIONS1);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        runtime.physicsTables().writeEnemyPhysicsFlags(freeSlot, GopongaProjectileMotion.INITIAL_PHYSICS_FLAGS);
        runtime.combatTables().writeEnemyHealth(freeSlot, GopongaProjectileMotion.HEALTH_OVERRIDE);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables()
                .writeEnemyIgnoreHitsCountdown(freeSlot, GopongaProjectileMotion.INITIAL_IGNORE_HITS_COUNTDOWN);
        runtime.motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(freeSlot);
        runtime.motions()
                .get(GopongaProjectileMotion.class, GopongaProjectileMotion::new)
                .initializeSpawn(freeSlot, spawn.speedX(), spawn.speedY());
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
    }

    static void spawnZoraProjectile(EntityEnemySpawnsServices runtime, ZoraMotion.ProjectileSpawn spawn) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition projectileDefinition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_GOPONGA_FLOWER_PROJECTILE);
        int projectileVariant = projectileDefinition.supported() ? projectileDefinition.initialVariant() : -1;
        RoomEntity projectile = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_GOPONGA_FLOWER_PROJECTILE,
                spawn.x(),
                spawn.y(),
                EntityStatus.ACTIVE,
                projectileDefinition,
                projectileVariant,
                0,
                0,
                0,
                -1,
                false);
        runtime.slots()[freeSlot] = projectile;
        runtime.initializeHealthGroup(freeSlot);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables().writeEntityOptions1Override(freeSlot, GopongaProjectileMotion.OPTIONS1);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        runtime.physicsTables().writeEnemyPhysicsFlags(freeSlot, GopongaProjectileMotion.INITIAL_PHYSICS_FLAGS);
        runtime.combatTables().writeEnemyHealth(freeSlot, GopongaProjectileMotion.HEALTH_OVERRIDE);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables()
                .writeEnemyIgnoreHitsCountdown(freeSlot, GopongaProjectileMotion.INITIAL_IGNORE_HITS_COUNTDOWN);
        runtime.motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(freeSlot);
        runtime.motions()
                .get(GopongaProjectileMotion.class, GopongaProjectileMotion::new)
                .initializeSpawn(freeSlot, spawn.speedX(), spawn.speedY(), true);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
    }

    static void spawnWizrobeProjectile(
            EntityEnemySpawnsServices runtime, RoomEntity source, WizrobeMotion.ProjectileSpawn spawn) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition projectileDefinition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_WIZROBE_PROJECTILE);
        int projectileVariant = projectileDefinition.supported() ? spawn.direction() : -1;
        RoomEntity projectile = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_WIZROBE_PROJECTILE,
                spawn.x(),
                spawn.y(),
                EntityStatus.ACTIVE,
                projectileDefinition,
                projectileVariant,
                0,
                0,
                source.z(),
                -1,
                false);
        runtime.slots()[freeSlot] = projectile;
        runtime.initializeHealthGroup(freeSlot);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables().writeEntityOptions1Override(freeSlot, EntityRomConstants.WIZROBE_PROJECTILE_OPTIONS1);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        runtime.physicsTables().writeEnemyPhysicsFlags(freeSlot, 0x42);
        runtime.combatTables()
                .writeEnemyHealth(freeSlot, runtime.initialHealth(EntityRomConstants.ENTITY_WIZROBE_PROJECTILE));
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(freeSlot);
        runtime.motions()
                .get(WizrobeProjectileMotion.class, WizrobeProjectileMotion::new)
                .initializeSpawn(freeSlot, spawn.direction());
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
    }

    static void spawnEnemyProjectile(
            EntityEnemySpawnsServices runtime, RoomEntity source, RoamingEnemyMotion.LaunchRequest request) {
        EnemyProjectileSpawns.spawnEnemyProjectile(
                runtime,
                source,
                request,
                runtime.motions()
                        .get(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                        .direction(source.slot()));
    }

    static void spawnEnemyProjectile(
            EntityEnemySpawnsServices runtime,
            RoomEntity source,
            RoamingEnemyMotion.LaunchRequest request,
            int direction) {
        // Dynamic projectiles must carry the ROM-decoded display definition;
        // a behavior-only runtime without a catalog can still expose the
        // launch request, but must not insert an unrenderable placeholder.
        if (runtime.spriteHandlers() == null) {
            return;
        }
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        EnemyProjectileMotion.SpawnData spawn = EnemyProjectileMotion.spawnData(request.projectileType(), direction);
        EntitySpriteDefinition projectileDefinition =
                runtime.slotProperties().spriteDefinitionFor(request.projectileType());
        int projectileVariant = projectileDefinition.supported() ? spawn.initialVariant() : -1;
        RoomEntity projectile = new RoomEntity(
                freeSlot,
                -1,
                request.projectileType(),
                byteValue(source.x() + signedByte(spawn.offsetX())),
                byteValue(source.y() + signedByte(spawn.offsetY())),
                EntityStatus.ACTIVE,
                projectileDefinition,
                projectileVariant,
                0,
                0,
                source.z(),
                -1,
                false);
        runtime.slots()[freeSlot] = projectile;
        runtime.initializeHealthGroup(freeSlot);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyHealth(freeSlot, runtime.initialHealth(request.projectileType()));
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(freeSlot);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        runtime.motions()
                .get(EnemyProjectileMotion.class, EnemyProjectileMotion::new)
                .initializeSpawn(freeSlot, request.projectileType(), direction);
        runtime.physicsTables().writeEnemyProjectileSpawnedThisFrame(freeSlot, true);
    }

    static void spawnWingedOctorokRock(EntityEnemySpawnsServices runtime, RoomEntity source, int direction) {
        // SpawnNewEntity returns with carry when no room slot is available.
        // The ROM handler simply abandons the launch in that case.
        if (runtime.spriteHandlers() == null) {
            return;
        }
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        EnemyProjectileMotion.SpawnData spawn =
                EnemyProjectileMotion.spawnData(EntityRomConstants.ENTITY_OCTOROK_ROCK, direction);
        EntitySpriteDefinition projectileDefinition =
                runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_OCTOROK_ROCK);
        int projectileVariant = projectileDefinition.supported() ? 1 : -1;
        RoomEntity projectile = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_OCTOROK_ROCK,
                byteValue(source.x() + signedByte(spawn.offsetX())),
                byteValue(source.y() + signedByte(spawn.offsetY())),
                EntityStatus.ACTIVE,
                projectileDefinition,
                projectileVariant,
                0,
                0,
                source.z(),
                -1,
                false);
        runtime.slots()[freeSlot] = projectile;
        runtime.initializeHealthGroup(freeSlot);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables()
                .writeEnemyHealth(freeSlot, runtime.initialHealth(EntityRomConstants.ENTITY_OCTOROK_ROCK));
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.motions().get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).clear(freeSlot);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        runtime.motions()
                .get(EnemyProjectileMotion.class, EnemyProjectileMotion::new)
                .initializeSpawn(freeSlot, EntityRomConstants.ENTITY_OCTOROK_ROCK, direction);
        runtime.physicsTables().writeEnemyProjectileSpawnedThisFrame(freeSlot, true);
    }
}
