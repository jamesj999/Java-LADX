package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.byteValue;
import static linksawakening.world.entity.EntityMotionMath.signedByte;
import static linksawakening.world.entity.EntityTypeRules.initialHitboxFlags;
import static linksawakening.world.entity.EntityTypeRules.initialPhysicsFlags;

import java.util.Objects;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.CuccoMotion;
import linksawakening.world.entity.motion.EnemyProjectileMotion;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.GiantGopongaMotion;
import linksawakening.world.entity.motion.GopongaProjectileMotion;
import linksawakening.world.entity.motion.LaserMotion;
import linksawakening.world.entity.motion.PairoddProjectileMotion;
import linksawakening.world.entity.motion.PokeyMotion;
import linksawakening.world.entity.motion.RoamingEnemyMotion;
import linksawakening.world.entity.motion.StalfosEvasiveMotion;
import linksawakening.world.entity.motion.ThrownEntityMotion;
import linksawakening.world.entity.motion.WizrobeMotion;
import linksawakening.world.entity.motion.WizrobeProjectileMotion;
import linksawakening.world.entity.motion.ZombieMotion;
import linksawakening.world.entity.motion.ZoraMotion;

/** SpawnNewEntity / SpawnNewEntityInRange and each enemy handler’s child initialization path. */
public final class EntityEnemySpawns {
    // Optional ROM catalog for ROM-free entity fixtures.
    final linksawakening.world.entity.EntitySpriteViews spriteHandlers;
    final EntityBombState bombInteractions;
    final EntityBossState bossRewards;
    final EntityCombatTables combatTables;
    final EntityEventChannels events;
    final EntityLiftState liftThrow;
    final EntityMotionRegistry motions;
    final EntityPhysicsTables physicsTables;
    final EntityPickupController pickups;
    final EntitySlotProperties slotProperties;
    final EntitySlots slots;
    final EntitySlotLifecycle lifecycle;

    EntityEnemySpawns(
            linksawakening.world.entity.EntitySpriteViews spriteHandlers,
            EntityBombState bombInteractions,
            EntityBossState bossRewards,
            EntityEventChannels events,
            EntityLiftState liftThrow,
            EntityMotionRegistry motions,
            EntityPickupController pickups,
            EntitySlotProperties slotProperties,
            EntitySlots slots,
            EntitySlotLifecycle lifecycle) {
        EntityCombatTables combatTables = slots.combat();
        EntityPhysicsTables physicsTables = slots.physics();
        this.spriteHandlers = Objects.requireNonNull(spriteHandlers, "sprite source");
        this.bombInteractions = Objects.requireNonNull(bombInteractions, "bombInteractions");
        this.bossRewards = Objects.requireNonNull(bossRewards, "bossRewards");
        this.combatTables = Objects.requireNonNull(combatTables, "combatTables");
        this.events = Objects.requireNonNull(events, "events");
        this.liftThrow = Objects.requireNonNull(liftThrow, "liftThrow");
        this.motions = Objects.requireNonNull(motions, "motions");
        this.physicsTables = Objects.requireNonNull(physicsTables, "physicsTables");
        this.pickups = Objects.requireNonNull(pickups, "pickups");
        this.slotProperties = Objects.requireNonNull(slotProperties, "slotProperties");
        this.slots = Objects.requireNonNull(slots, "slots");
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
    }

    public int spawnEnemyBomb(int entityX, int entityY, int entityZ, int transitionCountdown) {
        return EntityEnemySpawns.enemyBombSpawnsSpawnEnemyBomb(this, entityX, entityY, entityZ, transitionCountdown);
    }

    public int spawnEnemyBomb(
            int entityX, int entityY, int entityZ, int transitionCountdown, int speedX, int speedY, int speedZ) {
        return EntityEnemySpawns.enemyBombSpawnsSpawnEnemyBomb(
                this, entityX, entityY, entityZ, transitionCountdown, speedX, speedY, speedZ);
    }

    public int spawnEnemyBomb(EnemyBombSpawn inputs) {
        return EntityEnemySpawns.enemyBombSpawnsSpawnEnemyBomb(this, inputs);
    }

    public int spawnTarinTransformationBomb(RoomEntity tarin) {
        return EntityEnemySpawns.enemyBombSpawnsSpawnEnemyBomb(
                this, new EnemyBombSpawn(tarin.x(), tarin.y(), tarin.z(), 0x20, 0, 0, 0, false, 0x4C));
    }

    public boolean spawnKanaletWallMoblin(RoomEntity wall) {
        int freeSlot = this.slots.findFree(this.slots.size() - 1);
        if (freeSlot < 0) {
            return false;
        }
        EntitySpriteDefinition definition =
                this.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_MOBLIN_SWORD);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        this.slots.replace(
                freeSlot,
                new RoomEntity(
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
                        false));
        this.lifecycle.initializeHealthGroup(freeSlot);
        this.pickups.resetEnemyDropState(freeSlot);
        this.bossRewards.writeDroppedItemBySlot(freeSlot, EntityRomConstants.ENTITY_HIDING_SLIME_KEY);
        this.physicsTables.writeEnemyPhysicsFlags(
                freeSlot, initialPhysicsFlags(EntityRomConstants.ENTITY_MOBLIN_SWORD));
        this.combatTables.writeEnemyHealth(
                freeSlot, this.slotProperties.initialHealth(EntityRomConstants.ENTITY_MOBLIN_SWORD));
        this.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        this.physicsTables.writeEntityOptions1Override(freeSlot, -1);
        this.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return true;
    }

    public void spawnPairoddProjectile(RoomEntity source, int linkEntityX, int linkEntityY) {
        int freeSlot = this.slots.findFree(this.slots.size() - 1);
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition projectileDefinition =
                this.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_PAIRODD_PROJECTILE);
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
        this.slots.replace(freeSlot, projectile);
        this.lifecycle.initializeHealthGroup(freeSlot);
        this.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        this.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        this.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        this.combatTables.writeEnemyHealth(
                freeSlot, this.slotProperties.initialHealth(EntityRomConstants.ENTITY_PAIRODD_PROJECTILE));
        this.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        this.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        var enemyRecoilMotion = this.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        enemyRecoilMotion.clear(freeSlot);
        this.combatTables.writeDyingCountdown(freeSlot, 0);
        this.combatTables.writePowerRecoilDeath(freeSlot, false);
        var pairoddProjectileMotion = this.motions.get(PairoddProjectileMotion.class, PairoddProjectileMotion::new);
        pairoddProjectileMotion.initializeSpawn(freeSlot, source, linkEntityX, linkEntityY);
    }

    public void spawnGopongaProjectile(RoomEntity source, GiantGopongaMotion.ProjectileSpawn spawn) {
        int freeSlot = this.slots.findFree(this.slots.size() - 1);
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition projectileDefinition =
                this.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_GOPONGA_FLOWER_PROJECTILE);
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
        this.slots.replace(freeSlot, projectile);
        this.lifecycle.initializeHealthGroup(freeSlot);
        this.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        this.physicsTables.writeEntityOptions1Override(freeSlot, GopongaProjectileMotion.OPTIONS1);
        this.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        this.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        this.combatTables.writeDyingCountdown(freeSlot, 0);
        this.combatTables.writePowerRecoilDeath(freeSlot, false);
        this.physicsTables.writeEnemyPhysicsFlags(freeSlot, GopongaProjectileMotion.INITIAL_PHYSICS_FLAGS);
        this.combatTables.writeEnemyHealth(freeSlot, GopongaProjectileMotion.HEALTH_OVERRIDE);
        this.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        this.combatTables.writeEnemyIgnoreHitsCountdown(
                freeSlot, GopongaProjectileMotion.INITIAL_IGNORE_HITS_COUNTDOWN);
        var enemyRecoilMotion = this.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        enemyRecoilMotion.clear(freeSlot);
        var gopongaProjectileMotion = this.motions.get(GopongaProjectileMotion.class, GopongaProjectileMotion::new);
        gopongaProjectileMotion.initializeSpawn(freeSlot, spawn.speedX(), spawn.speedY());
        this.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
    }

    public void spawnZoraProjectile(ZoraMotion.ProjectileSpawn spawn) {
        int freeSlot = this.slots.findFree(this.slots.size() - 1);
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition projectileDefinition =
                this.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_GOPONGA_FLOWER_PROJECTILE);
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
        this.slots.replace(freeSlot, projectile);
        this.lifecycle.initializeHealthGroup(freeSlot);
        this.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        this.physicsTables.writeEntityOptions1Override(freeSlot, GopongaProjectileMotion.OPTIONS1);
        this.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        this.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        this.combatTables.writeDyingCountdown(freeSlot, 0);
        this.combatTables.writePowerRecoilDeath(freeSlot, false);
        this.physicsTables.writeEnemyPhysicsFlags(freeSlot, GopongaProjectileMotion.INITIAL_PHYSICS_FLAGS);
        this.combatTables.writeEnemyHealth(freeSlot, GopongaProjectileMotion.HEALTH_OVERRIDE);
        this.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        this.combatTables.writeEnemyIgnoreHitsCountdown(
                freeSlot, GopongaProjectileMotion.INITIAL_IGNORE_HITS_COUNTDOWN);
        var enemyRecoilMotion = this.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        enemyRecoilMotion.clear(freeSlot);
        var gopongaProjectileMotion = this.motions.get(GopongaProjectileMotion.class, GopongaProjectileMotion::new);
        gopongaProjectileMotion.initializeSpawn(freeSlot, spawn.speedX(), spawn.speedY(), true);
        this.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
    }

    public void spawnZombieChild(ZombieMotion.SpawnRequest spawn) {
        int freeSlot = this.slots.findFree(0x05);
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition definition = this.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_ZOMBIE);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity child = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_ZOMBIE,
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
        this.slots.replace(freeSlot, child);
        this.lifecycle.initializeHealthGroup(freeSlot);
        this.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        this.physicsTables.writeEntityOptions1Override(freeSlot, -1);
        this.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        this.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        this.combatTables.writeDyingCountdown(freeSlot, 0);
        this.combatTables.writePowerRecoilDeath(freeSlot, false);
        // ConfigureNewEntity starts from the static $52 row; the handler
        // clears projectile noclip before the child is first dispatched.
        this.physicsTables.writeEnemyPhysicsFlags(
                freeSlot, ZombieMotion.INITIAL_PHYSICS_FLAGS & ~ZombieMotion.PROJECTILE_NOCLIP_FLAG);
        this.combatTables.writeEnemyHitboxFlags(freeSlot, initialHitboxFlags(EntityRomConstants.ENTITY_ZOMBIE));
        this.combatTables.writeEnemyHealth(
                freeSlot, this.slotProperties.initialHealth(EntityRomConstants.ENTITY_ZOMBIE));
        this.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        this.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        var enemyRecoilMotion = this.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        enemyRecoilMotion.clear(freeSlot);
        var zombieMotion = this.motions.get(ZombieMotion.class, ZombieMotion::new);
        zombieMotion.initializeChild(freeSlot);
        this.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
    }

    public void spawnPokeySegment(PokeyMotion.SegmentSpawn spawn) {
        int freeSlot = this.slots.findFree(this.slots.size() - 1);
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition definition = this.slotProperties.spriteDefinitionForPokeySegment();
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity segment = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_POKEY,
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
        this.slots.replace(freeSlot, segment);
        this.lifecycle.initializeHealthGroup(freeSlot);
        this.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        this.physicsTables.writeEntityOptions1Override(freeSlot, -1);
        this.physicsTables.writeEnemyTransitionCountdown(freeSlot, spawn.transitionCountdown());
        this.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        this.combatTables.writeDyingCountdown(freeSlot, 0);
        this.combatTables.writePowerRecoilDeath(freeSlot, false);
        this.physicsTables.writeEnemyPhysicsFlags(freeSlot, initialPhysicsFlags(EntityRomConstants.ENTITY_POKEY));
        this.combatTables.writeEnemyHitboxFlags(freeSlot, initialHitboxFlags(EntityRomConstants.ENTITY_POKEY));
        this.combatTables.writeEnemyHealth(
                freeSlot, this.slotProperties.initialHealth(EntityRomConstants.ENTITY_POKEY));
        this.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        // ConfigureNewEntity seeds one ignored-hit frame; the child handler
        // decrements it before its first interactive collision pass.
        this.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        var enemyRecoilMotion = this.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        enemyRecoilMotion.clear(freeSlot);
        this.physicsTables.writeEntityInertia(freeSlot, 0);
        var pokeyMotion = this.motions.get(PokeyMotion.class, PokeyMotion::new);
        pokeyMotion.initializeSegment(freeSlot, spawn.speedX(), spawn.speedY());
        this.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
    }

    public void spawnWizrobeProjectile(RoomEntity source, WizrobeMotion.ProjectileSpawn spawn) {
        int freeSlot = this.slots.findFree(this.slots.size() - 1);
        if (freeSlot < 0) {
            return;
        }

        EntitySpriteDefinition projectileDefinition =
                this.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_WIZROBE_PROJECTILE);
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
        this.slots.replace(freeSlot, projectile);
        this.lifecycle.initializeHealthGroup(freeSlot);
        this.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        this.physicsTables.writeEntityOptions1Override(freeSlot, EntityRomConstants.WIZROBE_PROJECTILE_OPTIONS1);
        this.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        this.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        this.combatTables.writeDyingCountdown(freeSlot, 0);
        this.combatTables.writePowerRecoilDeath(freeSlot, false);
        this.physicsTables.writeEnemyPhysicsFlags(freeSlot, 0x42);
        this.combatTables.writeEnemyHealth(
                freeSlot, this.slotProperties.initialHealth(EntityRomConstants.ENTITY_WIZROBE_PROJECTILE));
        this.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        this.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        var enemyRecoilMotion = this.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        enemyRecoilMotion.clear(freeSlot);
        var wizrobeProjectileMotion = this.motions.get(WizrobeProjectileMotion.class, WizrobeProjectileMotion::new);
        wizrobeProjectileMotion.initializeSpawn(freeSlot, spawn.direction());
        this.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
    }

    public boolean spawnEvasiveClone(RoomEntity source, StalfosEvasiveMotion.CloneRequest request) {
        int freeSlot = this.slots.findFree(this.slots.size() - 1);
        if (freeSlot < 0) {
            return false;
        }

        EntitySpriteDefinition cloneDefinition =
                this.slotProperties.spriteDefinitionForEvasiveState(source.spriteDefinition(), 1);
        int cloneVariant = cloneDefinition.supported() ? cloneDefinition.initialVariant() : -1;
        RoomEntity clone = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_STALFOS_EVASIVE,
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
        this.slots.replace(freeSlot, clone);
        this.lifecycle.initializeHealthGroup(freeSlot);
        this.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        this.physicsTables.writeEntityOptions1Override(freeSlot, EntityRomConstants.EVASIVE_CLONE_OPTIONS);
        this.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        this.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        this.combatTables.writeDyingCountdown(freeSlot, 0);
        this.combatTables.writePowerRecoilDeath(freeSlot, false);
        this.physicsTables.writeEnemyPhysicsFlags(freeSlot, EntityRomConstants.EVASIVE_CLONE_PHYSICS_FLAGS);
        this.combatTables.writeEnemyHealth(
                freeSlot, this.slotProperties.initialHealth(EntityRomConstants.ENTITY_STALFOS_EVASIVE));
        this.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        // SpawnNewEntity writes one frame, but the Evasive handler immediately
        // stores register B over it. AnimateEntities keeps B at zero, so this
        // clone enters its fleeing handler with no ignore-hits countdown.
        this.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        var enemyRecoilMotion = this.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        enemyRecoilMotion.clear(freeSlot);
        var stalfosEvasiveMotion = this.motions.get(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new);
        stalfosEvasiveMotion.initializeClone(freeSlot, request.speedX(), request.speedY());
        this.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        this.events.sound(source.slot(), source.type(), EntityCombatEvent.SoundChannel.NOISE, 0x0A);
        return true;
    }

    public void spawnEnemyProjectile(RoomEntity source, RoamingEnemyMotion.LaunchRequest request) {
        EntityEnemySpawns.enemyProjectileSpawnsSpawnEnemyProjectile(this, source, request);
    }

    public void spawnEnemyProjectile(RoomEntity source, RoamingEnemyMotion.LaunchRequest request, int direction) {
        EntityEnemySpawns.enemyProjectileSpawnsSpawnEnemyProjectile(this, source, request, direction);
    }

    public void spawnWingedOctorokRock(RoomEntity source, int direction) {
        // SpawnNewEntity returns with carry when no room slot is available.
        // The ROM handler simply abandons the launch in that case.
        if (!this.spriteHandlers.supportsPresentation()) {
            return;
        }
        int freeSlot = this.slots.findFree(this.slots.size() - 1);
        if (freeSlot < 0) {
            return;
        }

        EnemyProjectileMotion.SpawnData spawn =
                EnemyProjectileMotion.spawnData(EntityRomConstants.ENTITY_OCTOROK_ROCK, direction);
        EntitySpriteDefinition projectileDefinition =
                this.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_OCTOROK_ROCK);
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
        this.slots.replace(freeSlot, projectile);
        this.lifecycle.initializeHealthGroup(freeSlot);
        this.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        this.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        this.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        this.combatTables.writeEnemyHealth(
                freeSlot, this.slotProperties.initialHealth(EntityRomConstants.ENTITY_OCTOROK_ROCK));
        this.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        this.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        var enemyRecoilMotion = this.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        enemyRecoilMotion.clear(freeSlot);
        this.combatTables.writeDyingCountdown(freeSlot, 0);
        this.combatTables.writePowerRecoilDeath(freeSlot, false);
        var enemyProjectileMotion = this.motions.get(EnemyProjectileMotion.class, EnemyProjectileMotion::new);
        enemyProjectileMotion.initializeSpawn(freeSlot, EntityRomConstants.ENTITY_OCTOROK_ROCK, direction);
        this.physicsTables.writeEnemyProjectileSpawnedThisFrame(freeSlot, true);
    }

    public boolean canSpawnCuccoAngry() {
        for (int slot = 7; slot >= 0; slot--) {
            if (!this.slots.get(slot).loaded()) return true;
        }
        return false;
    }

    public boolean spawnCuccoAngry(CuccoMotion.SpawnRequest request) {
        int freeSlot = this.slots.findFree(7);
        if (freeSlot < 0) {
            return false;
        }

        EntitySpriteDefinition definition = this.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_CUCCO);
        int variant = definition.supported() ? definition.initialVariant() : -1;
        RoomEntity angry = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_CUCCO,
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
        this.slots.replace(freeSlot, angry);
        this.lifecycle.initializeHealthGroup(freeSlot);
        var cuccoMotion = this.motions.get(CuccoMotion.class, CuccoMotion::new);
        cuccoMotion.initializeAngry(freeSlot, request.speedX(), request.speedY());
        this.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        this.physicsTables.writeEntityOptions1Override(freeSlot, CuccoMotion.ANGRY_OPTIONS1);
        this.physicsTables.writeEnemyPhysicsFlags(freeSlot, CuccoMotion.ANGRY_PHYSICS_FLAGS);
        this.combatTables.writeEnemyHitboxFlags(freeSlot, CuccoMotion.ANGRY_HITBOX_FLAGS);
        this.combatTables.writeEnemyHealth(freeSlot, CuccoMotion.HEALTH_OVERRIDE);
        this.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        this.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        this.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        this.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 0);
        this.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return true;
    }

    void spawnLaserSensor(RoomEntity parent) {
        int freeSlot = this.slots.findFree(this.slots.size() - 1);
        if (freeSlot < 0) {
            return;
        }

        RoomEntity sensor = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_LASER,
                parent.x(),
                parent.y(),
                EntityStatus.ACTIVE,
                EntitySpriteDefinition.unsupported(EntityRomConstants.ENTITY_LASER),
                -1,
                0,
                0,
                parent.z(),
                -1,
                false);
        this.slots.replace(freeSlot, sensor);
        this.lifecycle.initializeHealthGroup(freeSlot);
        this.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        this.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        this.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        this.combatTables.writeEnemyHealth(freeSlot, 0);
        this.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        this.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        this.combatTables.writeDyingCountdown(freeSlot, 0);
        this.combatTables.writePowerRecoilDeath(freeSlot, false);
        var laserMotion = this.motions.get(LaserMotion.class, LaserMotion::new);
        laserMotion.initializeSensor(freeSlot, parent.slot(), laserMotion.direction(parent.slot()));
        this.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
    }

    boolean spawnLaserBeam(RoomEntity parent) {
        int freeSlot = this.slots.findFree(this.slots.size() - 1);
        if (freeSlot < 0) {
            return false;
        }

        RoomEntity beam = new RoomEntity(
                freeSlot,
                -1,
                EntityRomConstants.ENTITY_LASER_BEAM,
                parent.x(),
                parent.y(),
                EntityStatus.ACTIVE,
                EntitySpriteDefinition.unsupported(EntityRomConstants.ENTITY_LASER_BEAM),
                -1,
                0,
                0,
                parent.z(),
                -1,
                false);
        this.slots.replace(freeSlot, beam);
        this.lifecycle.initializeHealthGroup(freeSlot);
        this.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        this.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        this.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        this.combatTables.writeEnemyHealth(freeSlot, 0);
        this.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        this.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        this.combatTables.writeDyingCountdown(freeSlot, 0);
        this.combatTables.writePowerRecoilDeath(freeSlot, false);
        var laserMotion = this.motions.get(LaserMotion.class, LaserMotion::new);
        laserMotion.initializeBeam(
                freeSlot,
                laserMotion.direction(parent.slot()),
                laserMotion.speedX(parent.slot()),
                laserMotion.speedY(parent.slot()));
        this.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return true;
    }

    static int enemyBombSpawnsSpawnEnemyBomb(
            EntityEnemySpawns runtime, int entityX, int entityY, int entityZ, int transitionCountdown) {
        return EntityEnemySpawns.enemyBombSpawnsSpawnEnemyBomb(
                runtime, new EnemyBombSpawn(entityX, entityY, entityZ, transitionCountdown, 0, 0, 0, false, 0x01));
    }

    static int enemyBombSpawnsSpawnEnemyBomb(
            EntityEnemySpawns runtime,
            int entityX,
            int entityY,
            int entityZ,
            int transitionCountdown,
            int speedX,
            int speedY,
            int speedZ) {
        return EntityEnemySpawns.enemyBombSpawnsSpawnEnemyBomb(
                runtime,
                new EnemyBombSpawn(entityX, entityY, entityZ, transitionCountdown, speedX, speedY, speedZ, true, 0x01));
    }

    static int enemyBombSpawnsSpawnEnemyBomb(EntityEnemySpawns runtime, EnemyBombSpawn inputs) {
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
        int freeSlot = runtime.slots.findFree(runtime.slots.size() - 1);
        if (freeSlot < 0) {
            return -1;
        }

        EntitySpriteDefinition definition = runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_BOMB);
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
        runtime.slots.replace(freeSlot, bomb);
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        runtime.bombInteractions.writeBombDirection(freeSlot, 0xFF);
        runtime.bombInteractions.writePlayerArrowBombArrow(freeSlot, false);
        runtime.bombInteractions.setBombFinalPresentationPending(freeSlot, false);
        runtime.bombInteractions.writeBombPrivateState4(freeSlot, privateState4 & 0xFF);
        runtime.bombInteractions.writeBombPrivateCountdown1(freeSlot, 0);
        runtime.bombInteractions.writeBombPrivateCountdown3(freeSlot, 0);
        runtime.bombInteractions.writePlacedBombMotionInitialized(freeSlot, false);
        runtime.bombInteractions.writeEnemyBombMotionInitialized(freeSlot, initializeMotion);
        if (initializeMotion) {
            var thrownEntityMotion = runtime.motions.get(ThrownEntityMotion.class, ThrownEntityMotion::new);
            thrownEntityMotion.startEnemyBomb(freeSlot, speedX, speedY, speedZ);
        } else {
            var thrownEntityMotion = runtime.motions.get(ThrownEntityMotion.class, ThrownEntityMotion::new);
            thrownEntityMotion.clear(freeSlot);
        }
        runtime.liftThrow.writeThrownMotionInitialized(freeSlot, false);
        runtime.combatTables.writeEnemyHitboxFlags(freeSlot, 0);
        runtime.physicsTables.writeEnemyTransitionCountdown(freeSlot, transitionCountdown);
        runtime.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyHealth(
                freeSlot, runtime.slotProperties.initialHealth(EntityRomConstants.ENTITY_BOMB));
        runtime.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, initializeMotion ? 1 : 0);
        runtime.physicsTables.writeEnemyPhysicsFlags(freeSlot, EntityRomConstants.BOMB_INITIAL_PHYSICS_FLAGS);
        runtime.physicsTables.writeEntityGroundStatus(freeSlot, 0);
        runtime.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables.writeEntityOptions1Override(freeSlot, EntityRomConstants.BOMB_OPTIONS1);
        runtime.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return freeSlot;
    }

    static void enemyProjectileSpawnsSpawnEnemyProjectile(
            EntityEnemySpawns runtime, RoomEntity source, RoamingEnemyMotion.LaunchRequest request) {
        var roamingEnemyMotion = runtime.motions.get(RoamingEnemyMotion.class, RoamingEnemyMotion::new);
        EntityEnemySpawns.enemyProjectileSpawnsSpawnEnemyProjectile(
                runtime, source, request, roamingEnemyMotion.direction(source.slot()));
    }

    static void enemyProjectileSpawnsSpawnEnemyProjectile(
            EntityEnemySpawns runtime, RoomEntity source, RoamingEnemyMotion.LaunchRequest request, int direction) {
        // Dynamic projectiles must carry the ROM-decoded display definition;
        // a behavior-only runtime without a catalog can still expose the
        // launch request, but must not insert an unrenderable placeholder.
        if (!runtime.spriteHandlers.supportsPresentation()) {
            return;
        }
        int freeSlot = runtime.slots.findFree(runtime.slots.size() - 1);
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
        runtime.slots.replace(freeSlot, projectile);
        runtime.lifecycle.initializeHealthGroup(freeSlot);
        runtime.physicsTables.writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyHealth(freeSlot, runtime.slotProperties.initialHealth(request.projectileType()));
        runtime.combatTables.writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        var enemyRecoilMotion = runtime.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        enemyRecoilMotion.clear(freeSlot);
        runtime.combatTables.writeDyingCountdown(freeSlot, 0);
        runtime.combatTables.writePowerRecoilDeath(freeSlot, false);
        var enemyProjectileMotion = runtime.motions.get(EnemyProjectileMotion.class, EnemyProjectileMotion::new);
        enemyProjectileMotion.initializeSpawn(freeSlot, request.projectileType(), direction);
        runtime.physicsTables.writeEnemyProjectileSpawnedThisFrame(freeSlot, true);
    }
}
