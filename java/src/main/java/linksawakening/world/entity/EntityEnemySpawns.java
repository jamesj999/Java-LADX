package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;
import static linksawakening.world.entity.RoomEntityRuntime.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.CuccoMotion;
import linksawakening.world.entity.motion.EnemyProjectileMotion;
import linksawakening.world.entity.motion.GiantGopongaMotion;
import linksawakening.world.entity.motion.GopongaProjectileMotion;
import linksawakening.world.entity.motion.PokeyMotion;
import linksawakening.world.entity.motion.RoamingEnemyMotion;
import linksawakening.world.entity.motion.StalfosEvasiveMotion;
import linksawakening.world.entity.motion.WizrobeMotion;
import linksawakening.world.entity.motion.ZombieMotion;
import linksawakening.world.entity.motion.ZoraMotion;

/** ROM slot allocation and enemy child spawning. */
public final class EntityEnemySpawns {
    private final RoomEntityRuntime runtime;

    EntityEnemySpawns(RoomEntityRuntime runtime) {
        this.runtime = runtime;
    }

    public int spawnEnemyBomb(int entityX, int entityY, int entityZ, int transitionCountdown) {
        return spawnEnemyBomb(entityX, entityY, entityZ, transitionCountdown, 0, 0, 0, false, 0x01);
    }

    public int spawnEnemyBomb(
            int entityX, int entityY, int entityZ, int transitionCountdown, int speedX, int speedY, int speedZ) {
        return spawnEnemyBomb(entityX, entityY, entityZ, transitionCountdown, speedX, speedY, speedZ, true, 0x01);
    }

    public int spawnEnemyBomb(
            int entityX,
            int entityY,
            int entityZ,
            int transitionCountdown,
            int speedX,
            int speedY,
            int speedZ,
            boolean initializeMotion,
            int privateState4) {
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

        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(runtime.ENTITY_BOMB);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity bomb = new RoomEntity(
                freeSlot,
                -1,
                runtime.ENTITY_BOMB,
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
        runtime.slots[freeSlot] = bomb;
        runtime.initializeHealthGroup(freeSlot);
        runtime.bombDirection[freeSlot] = 0xFF;
        runtime.playerArrowBombArrow[freeSlot] = false;
        runtime.bombFinalPresentationPending[freeSlot] = false;
        runtime.bombPrivateState4[freeSlot] = privateState4 & 0xFF;
        runtime.bombPrivateCountdown1[freeSlot] = 0;
        runtime.bombPrivateCountdown3[freeSlot] = 0;
        runtime.placedBombMotionInitialized[freeSlot] = false;
        runtime.enemyBombMotionInitialized[freeSlot] = initializeMotion;
        if (initializeMotion) {
            runtime.thrownEntityMotion.startEnemyBomb(freeSlot, speedX, speedY, speedZ);
        } else {
            runtime.thrownEntityMotion.clear(freeSlot);
        }
        runtime.thrownMotionInitialized[freeSlot] = false;
        runtime.enemyHitboxFlags[freeSlot] = 0;
        runtime.enemyTransitionCountdown[freeSlot] = transitionCountdown;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.enemyHealth[freeSlot] = runtime.initialHealth(runtime.ENTITY_BOMB);
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = initializeMotion ? 1 : 0;
        runtime.enemyPhysicsFlags[freeSlot] = runtime.BOMB_INITIAL_PHYSICS_FLAGS;
        runtime.entityGroundStatus[freeSlot] = 0;
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityOptions1Override[freeSlot] = runtime.BOMB_OPTIONS1;
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        return freeSlot;
    }

    public int spawnTarinTransformationBomb(RoomEntity tarin) {
        return spawnEnemyBomb(tarin.x(), tarin.y(), tarin.z(), 0x20, 0, 0, 0, false, 0x4C);
    }

    public boolean spawnKanaletWallMoblin(RoomEntity wall) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return false;
        }
        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(runtime.ENTITY_MOBLIN_SWORD);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        runtime.slots[freeSlot] = new RoomEntity(
                freeSlot,
                0xFF,
                runtime.ENTITY_MOBLIN_SWORD,
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
        runtime.pickups.resetEnemyDropState(freeSlot);
        runtime.droppedItemBySlot[freeSlot] = runtime.ENTITY_HIDING_SLIME_KEY;
        runtime.enemyPhysicsFlags[freeSlot] = initialPhysicsFlags(runtime.ENTITY_MOBLIN_SWORD);
        runtime.enemyHealth[freeSlot] = runtime.initialHealth(runtime.ENTITY_MOBLIN_SWORD);
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.entityOptions1Override[freeSlot] = -1;
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        return true;
    }

    public void spawnPairoddProjectile(RoomEntity source, int linkEntityX, int linkEntityY) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition projectileDefinition =
                runtime.slotProperties.spriteDefinitionFor(runtime.ENTITY_PAIRODD_PROJECTILE);
        int projectileVariant = projectileDefinition.supported() ? projectileDefinition.initialVariant() : -1;
        RoomEntity projectile = new RoomEntity(
                freeSlot,
                -1,
                runtime.ENTITY_PAIRODD_PROJECTILE,
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
        runtime.slots[freeSlot] = projectile;
        runtime.initializeHealthGroup(freeSlot);
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.enemyTransitionCountdown[freeSlot] = 0;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.enemyHealth[freeSlot] = runtime.initialHealth(runtime.ENTITY_PAIRODD_PROJECTILE);
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.enemyRecoilMotion.clear(freeSlot);
        runtime.dyingCountdown[freeSlot] = 0;
        runtime.powerRecoilDeath[freeSlot] = false;
        runtime.pairoddProjectileMotion.initializeSpawn(freeSlot, source, linkEntityX, linkEntityY);
    }

    public void spawnGopongaProjectile(RoomEntity source, GiantGopongaMotion.ProjectileSpawn spawn) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition projectileDefinition =
                runtime.slotProperties.spriteDefinitionFor(runtime.ENTITY_GOPONGA_FLOWER_PROJECTILE);
        int projectileVariant = projectileDefinition.supported() ? projectileDefinition.initialVariant() : -1;
        RoomEntity projectile = new RoomEntity(
                freeSlot,
                -1,
                runtime.ENTITY_GOPONGA_FLOWER_PROJECTILE,
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
        runtime.slots[freeSlot] = projectile;
        runtime.initializeHealthGroup(freeSlot);
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityOptions1Override[freeSlot] = GopongaProjectileMotion.OPTIONS1;
        runtime.enemyTransitionCountdown[freeSlot] = 0;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.dyingCountdown[freeSlot] = 0;
        runtime.powerRecoilDeath[freeSlot] = false;
        runtime.enemyPhysicsFlags[freeSlot] = GopongaProjectileMotion.INITIAL_PHYSICS_FLAGS;
        runtime.enemyHealth[freeSlot] = GopongaProjectileMotion.HEALTH_OVERRIDE;
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = GopongaProjectileMotion.INITIAL_IGNORE_HITS_COUNTDOWN;
        runtime.enemyRecoilMotion.clear(freeSlot);
        runtime.gopongaProjectileMotion.initializeSpawn(freeSlot, spawn.speedX(), spawn.speedY());
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = true;
    }

    public void spawnZoraProjectile(ZoraMotion.ProjectileSpawn spawn) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition projectileDefinition =
                runtime.slotProperties.spriteDefinitionFor(runtime.ENTITY_GOPONGA_FLOWER_PROJECTILE);
        int projectileVariant = projectileDefinition.supported() ? projectileDefinition.initialVariant() : -1;
        RoomEntity projectile = new RoomEntity(
                freeSlot,
                -1,
                runtime.ENTITY_GOPONGA_FLOWER_PROJECTILE,
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
        runtime.slots[freeSlot] = projectile;
        runtime.initializeHealthGroup(freeSlot);
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityOptions1Override[freeSlot] = GopongaProjectileMotion.OPTIONS1;
        runtime.enemyTransitionCountdown[freeSlot] = 0;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.dyingCountdown[freeSlot] = 0;
        runtime.powerRecoilDeath[freeSlot] = false;
        runtime.enemyPhysicsFlags[freeSlot] = GopongaProjectileMotion.INITIAL_PHYSICS_FLAGS;
        runtime.enemyHealth[freeSlot] = GopongaProjectileMotion.HEALTH_OVERRIDE;
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = GopongaProjectileMotion.INITIAL_IGNORE_HITS_COUNTDOWN;
        runtime.enemyRecoilMotion.clear(freeSlot);
        runtime.gopongaProjectileMotion.initializeSpawn(freeSlot, spawn.speedX(), spawn.speedY(), true);
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = true;
    }

    public void spawnZombieChild(ZombieMotion.SpawnRequest spawn) {
        int freeSlot = runtime.findFreeEntitySlotAtMost(0x05);
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(runtime.ENTITY_ZOMBIE);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity child = new RoomEntity(
                freeSlot,
                -1,
                runtime.ENTITY_ZOMBIE,
                spawn.x(),
                spawn.y(),
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                0,
                -1,
                false);
        runtime.slots[freeSlot] = child;
        runtime.initializeHealthGroup(freeSlot);
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityOptions1Override[freeSlot] = -1;
        runtime.enemyTransitionCountdown[freeSlot] = 0;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.dyingCountdown[freeSlot] = 0;
        runtime.powerRecoilDeath[freeSlot] = false;
        // ConfigureNewEntity starts from the static $52 row; the handler
        // clears projectile noclip before the child is first dispatched.
        runtime.enemyPhysicsFlags[freeSlot] = ZombieMotion.INITIAL_PHYSICS_FLAGS & ~ZombieMotion.PROJECTILE_NOCLIP_FLAG;
        runtime.enemyHitboxFlags[freeSlot] = initialHitboxFlags(runtime.ENTITY_ZOMBIE);
        runtime.enemyHealth[freeSlot] = runtime.initialHealth(runtime.ENTITY_ZOMBIE);
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.enemyRecoilMotion.clear(freeSlot);
        runtime.zombieMotion.initializeChild(freeSlot);
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = true;
    }

    public void spawnPokeySegment(PokeyMotion.SegmentSpawn spawn) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionForPokeySegment();
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity segment = new RoomEntity(
                freeSlot,
                -1,
                runtime.ENTITY_POKEY,
                spawn.x(),
                spawn.y(),
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                spawn.z(),
                -1,
                false);
        runtime.slots[freeSlot] = segment;
        runtime.initializeHealthGroup(freeSlot);
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityOptions1Override[freeSlot] = -1;
        runtime.enemyTransitionCountdown[freeSlot] = spawn.transitionCountdown();
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.dyingCountdown[freeSlot] = 0;
        runtime.powerRecoilDeath[freeSlot] = false;
        runtime.enemyPhysicsFlags[freeSlot] = initialPhysicsFlags(runtime.ENTITY_POKEY);
        runtime.enemyHitboxFlags[freeSlot] = initialHitboxFlags(runtime.ENTITY_POKEY);
        runtime.enemyHealth[freeSlot] = runtime.initialHealth(runtime.ENTITY_POKEY);
        runtime.enemyFlashCountdown[freeSlot] = 0;
        // ConfigureNewEntity seeds one ignored-hit frame; the child handler
        // decrements it before its first interactive collision pass.
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.enemyRecoilMotion.clear(freeSlot);
        runtime.entityInertia[freeSlot] = 0;
        runtime.pokeyMotion.initializeSegment(freeSlot, spawn.speedX(), spawn.speedY());
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = true;
    }

    public void spawnWizrobeProjectile(RoomEntity source, WizrobeMotion.ProjectileSpawn spawn) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition projectileDefinition =
                runtime.slotProperties.spriteDefinitionFor(runtime.ENTITY_WIZROBE_PROJECTILE);
        int projectileVariant = projectileDefinition.supported() ? spawn.direction() : -1;
        RoomEntity projectile = new RoomEntity(
                freeSlot,
                -1,
                runtime.ENTITY_WIZROBE_PROJECTILE,
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
        runtime.slots[freeSlot] = projectile;
        runtime.initializeHealthGroup(freeSlot);
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityOptions1Override[freeSlot] = runtime.WIZROBE_PROJECTILE_OPTIONS1;
        runtime.enemyTransitionCountdown[freeSlot] = 0;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.dyingCountdown[freeSlot] = 0;
        runtime.powerRecoilDeath[freeSlot] = false;
        runtime.enemyPhysicsFlags[freeSlot] = 0x42;
        runtime.enemyHealth[freeSlot] = runtime.initialHealth(runtime.ENTITY_WIZROBE_PROJECTILE);
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.enemyRecoilMotion.clear(freeSlot);
        runtime.wizrobeProjectileMotion.initializeSpawn(freeSlot, spawn.direction());
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = true;
    }

    public boolean spawnEvasiveClone(RoomEntity source, StalfosEvasiveMotion.CloneRequest request) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return false;
        }

        EntitySpriteDefinition cloneDefinition =
                runtime.slotProperties.spriteDefinitionForEvasiveState(source.spriteDefinition(), 1);
        int cloneVariant = cloneDefinition.supported() ? cloneDefinition.initialVariant() : -1;
        RoomEntity clone = new RoomEntity(
                freeSlot,
                -1,
                runtime.ENTITY_STALFOS_EVASIVE,
                request.x(),
                request.y(),
                EntityStatus.ACTIVE,
                cloneDefinition,
                cloneVariant,
                0,
                0,
                request.z(),
                -1,
                false);
        runtime.slots[freeSlot] = clone;
        runtime.initializeHealthGroup(freeSlot);
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityOptions1Override[freeSlot] = runtime.EVASIVE_CLONE_OPTIONS;
        runtime.enemyTransitionCountdown[freeSlot] = 0;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.dyingCountdown[freeSlot] = 0;
        runtime.powerRecoilDeath[freeSlot] = false;
        runtime.enemyPhysicsFlags[freeSlot] = runtime.EVASIVE_CLONE_PHYSICS_FLAGS;
        runtime.enemyHealth[freeSlot] = runtime.initialHealth(runtime.ENTITY_STALFOS_EVASIVE);
        runtime.enemyFlashCountdown[freeSlot] = 0;
        // SpawnNewEntity writes one frame, but the Evasive handler immediately
        // stores register B over it. AnimateEntities keeps B at zero, so this
        // clone enters its fleeing handler with no ignore-hits countdown.
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 0;
        runtime.enemyRecoilMotion.clear(freeSlot);
        runtime.stalfosEvasiveMotion.initializeClone(freeSlot, request.speedX(), request.speedY());
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
                source.slot(), source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x0A));
        return true;
    }

    public void spawnEnemyProjectile(RoomEntity source, RoamingEnemyMotion.LaunchRequest request) {
        spawnEnemyProjectile(source, request, runtime.roamingEnemyMotion.direction(source.slot()));
    }

    public void spawnEnemyProjectile(RoomEntity source, RoamingEnemyMotion.LaunchRequest request, int direction) {
        // Dynamic projectiles must carry the ROM-decoded display definition;
        // a behavior-only runtime without a catalog can still expose the
        // launch request, but must not insert an unrenderable placeholder.
        if (runtime.spriteHandlers == null) {
            return;
        }
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        EnemyProjectileMotion.SpawnData spawn = EnemyProjectileMotion.spawnData(request.projectileType(), direction);
        EntitySpriteDefinition projectileDefinition =
                runtime.slotProperties.spriteDefinitionFor(request.projectileType());
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
        runtime.slots[freeSlot] = projectile;
        runtime.initializeHealthGroup(freeSlot);
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.enemyTransitionCountdown[freeSlot] = 0;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.enemyHealth[freeSlot] = runtime.initialHealth(request.projectileType());
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.enemyRecoilMotion.clear(freeSlot);
        runtime.dyingCountdown[freeSlot] = 0;
        runtime.powerRecoilDeath[freeSlot] = false;
        runtime.enemyProjectileMotion.initializeSpawn(freeSlot, request.projectileType(), direction);
        runtime.enemyProjectileSpawnedThisFrame[freeSlot] = true;
    }

    public void spawnWingedOctorokRock(RoomEntity source, int direction) {
        // SpawnNewEntity returns with carry when no room slot is available.
        // The ROM handler simply abandons the launch in that case.
        if (runtime.spriteHandlers == null) {
            return;
        }
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        EnemyProjectileMotion.SpawnData spawn = EnemyProjectileMotion.spawnData(runtime.ENTITY_OCTOROK_ROCK, direction);
        EntitySpriteDefinition projectileDefinition =
                runtime.slotProperties.spriteDefinitionFor(runtime.ENTITY_OCTOROK_ROCK);
        int projectileVariant = projectileDefinition.supported() ? 1 : -1;
        RoomEntity projectile = new RoomEntity(
                freeSlot,
                -1,
                runtime.ENTITY_OCTOROK_ROCK,
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
        runtime.slots[freeSlot] = projectile;
        runtime.initializeHealthGroup(freeSlot);
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.enemyTransitionCountdown[freeSlot] = 0;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.enemyHealth[freeSlot] = runtime.initialHealth(runtime.ENTITY_OCTOROK_ROCK);
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.enemyRecoilMotion.clear(freeSlot);
        runtime.dyingCountdown[freeSlot] = 0;
        runtime.powerRecoilDeath[freeSlot] = false;
        runtime.enemyProjectileMotion.initializeSpawn(freeSlot, runtime.ENTITY_OCTOROK_ROCK, direction);
        runtime.enemyProjectileSpawnedThisFrame[freeSlot] = true;
    }

    public boolean canSpawnCuccoAngry() {
        for (int slot = 7; slot >= 0; slot--) {
            if (!runtime.slots[slot].loaded()) return true;
        }
        return false;
    }

    public boolean spawnCuccoAngry(CuccoMotion.SpawnRequest request) {
        int freeSlot = runtime.findFreeEntitySlotAtMost(7);
        if (freeSlot < 0) {
            return false;
        }

        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(runtime.ENTITY_CUCCO);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity angry = new RoomEntity(
                freeSlot,
                -1,
                runtime.ENTITY_CUCCO,
                request.x(),
                request.y(),
                EntityStatus.ACTIVE,
                definition,
                variant,
                0,
                0,
                request.z(),
                -1,
                false);
        runtime.slots[freeSlot] = angry;
        runtime.initializeHealthGroup(freeSlot);
        runtime.cuccoMotion.initializeAngry(freeSlot, request.speedX(), request.speedY());
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.entityOptions1Override[freeSlot] = CuccoMotion.ANGRY_OPTIONS1;
        runtime.enemyPhysicsFlags[freeSlot] = CuccoMotion.ANGRY_PHYSICS_FLAGS;
        runtime.enemyHitboxFlags[freeSlot] = CuccoMotion.ANGRY_HITBOX_FLAGS;
        runtime.enemyHealth[freeSlot] = CuccoMotion.HEALTH_OVERRIDE;
        runtime.enemyTransitionCountdown[freeSlot] = 0;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 0;
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        return true;
    }

    void spawnLaserSensor(RoomEntity parent) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return;
        }

        RoomEntity sensor = new RoomEntity(
                freeSlot,
                -1,
                runtime.ENTITY_LASER,
                parent.x(),
                parent.y(),
                EntityStatus.ACTIVE,
                EntitySpriteDefinition.unsupported(runtime.ENTITY_LASER),
                -1,
                0,
                0,
                parent.z(),
                -1,
                false);
        runtime.slots[freeSlot] = sensor;
        runtime.initializeHealthGroup(freeSlot);
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.enemyTransitionCountdown[freeSlot] = 0;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.enemyHealth[freeSlot] = 0;
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.dyingCountdown[freeSlot] = 0;
        runtime.powerRecoilDeath[freeSlot] = false;
        runtime.laserMotion.initializeSensor(freeSlot, parent.slot(), runtime.laserMotion.direction(parent.slot()));
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = true;
    }

    boolean spawnLaserBeam(RoomEntity parent) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0) {
            return false;
        }

        RoomEntity beam = new RoomEntity(
                freeSlot,
                -1,
                runtime.ENTITY_LASER_BEAM,
                parent.x(),
                parent.y(),
                EntityStatus.ACTIVE,
                EntitySpriteDefinition.unsupported(runtime.ENTITY_LASER_BEAM),
                -1,
                0,
                0,
                parent.z(),
                -1,
                false);
        runtime.slots[freeSlot] = beam;
        runtime.initializeHealthGroup(freeSlot);
        runtime.baseEntityFlipAttribute[freeSlot] = 0;
        runtime.enemyTransitionCountdown[freeSlot] = 0;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.enemyHealth[freeSlot] = 0;
        runtime.enemyFlashCountdown[freeSlot] = 0;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.dyingCountdown[freeSlot] = 0;
        runtime.powerRecoilDeath[freeSlot] = false;
        runtime.laserMotion.initializeBeam(
                freeSlot,
                runtime.laserMotion.direction(parent.slot()),
                runtime.laserMotion.speedX(parent.slot()),
                runtime.laserMotion.speedY(parent.slot()));
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        return true;
    }
}
