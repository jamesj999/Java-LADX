package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.CuccoMotion;
import linksawakening.world.entity.motion.GiantGopongaMotion;
import linksawakening.world.entity.motion.PokeyMotion;
import linksawakening.world.entity.motion.RoamingEnemyMotion;
import linksawakening.world.entity.motion.StalfosEvasiveMotion;
import linksawakening.world.entity.motion.WizrobeMotion;
import linksawakening.world.entity.motion.ZombieMotion;
import linksawakening.world.entity.motion.ZoraMotion;

/** ROM slot allocation and enemy child spawning. */
public final class EntityEnemySpawns {
    // Optional ROM catalog for ROM-free entity fixtures.
    final linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers;
    final EntityBombInteractions bombInteractions;
    final EntityBossRewards bossRewards;
    final EntityCombatTables combatTables;
    final EntityEventChannels events;
    final EntityLiftThrowController liftThrow;
    final EntityMotionRegistry motions;
    final EntityPhysicsTables physicsTables;
    final EntityPickupController pickups;
    final EntitySlotProperties slotProperties;
    final RoomEntity[] slots;
    final EntitySlotLifecycle lifecycle;

    EntityEnemySpawns(
            linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers,
            EntityBombInteractions bombInteractions,
            EntityBossRewards bossRewards,
            EntityCombatTables combatTables,
            EntityEventChannels events,
            EntityLiftThrowController liftThrow,
            EntityMotionRegistry motions,
            EntityPhysicsTables physicsTables,
            EntityPickupController pickups,
            EntitySlotProperties slotProperties,
            RoomEntity[] slots,
            EntitySlotLifecycle lifecycle) {
        this.spriteHandlers = spriteHandlers;
        this.bombInteractions = java.util.Objects.requireNonNull(bombInteractions, "bombInteractions");
        this.bossRewards = java.util.Objects.requireNonNull(bossRewards, "bossRewards");
        this.combatTables = java.util.Objects.requireNonNull(combatTables, "combatTables");
        this.events = java.util.Objects.requireNonNull(events, "events");
        this.liftThrow = java.util.Objects.requireNonNull(liftThrow, "liftThrow");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
        this.pickups = java.util.Objects.requireNonNull(pickups, "pickups");
        this.slotProperties = java.util.Objects.requireNonNull(slotProperties, "slotProperties");
        this.slots = java.util.Objects.requireNonNull(slots, "slots");
        this.lifecycle = java.util.Objects.requireNonNull(lifecycle, "lifecycle");
    }

    public int spawnEnemyBomb(int entityX, int entityY, int entityZ, int transitionCountdown) {
        return EnemyBombSpawns.spawnEnemyBomb(this, entityX, entityY, entityZ, transitionCountdown);
    }

    public int spawnEnemyBomb(
            int entityX, int entityY, int entityZ, int transitionCountdown, int speedX, int speedY, int speedZ) {
        return EnemyBombSpawns.spawnEnemyBomb(
                this, entityX, entityY, entityZ, transitionCountdown, speedX, speedY, speedZ);
    }

    public int spawnEnemyBomb(EnemyBombSpawn inputs) {
        return EnemyBombSpawns.spawnEnemyBomb(this, inputs);
    }

    public int spawnTarinTransformationBomb(RoomEntity tarin) {
        return EnemyBombSpawns.spawnTarinTransformationBomb(this, tarin);
    }

    public boolean spawnKanaletWallMoblin(RoomEntity wall) {
        return EnemyBombSpawns.spawnKanaletWallMoblin(this, wall);
    }

    public void spawnPairoddProjectile(RoomEntity source, int linkEntityX, int linkEntityY) {
        EnemyProjectileSpawns.spawnPairoddProjectile(this, source, linkEntityX, linkEntityY);
    }

    public void spawnGopongaProjectile(RoomEntity source, GiantGopongaMotion.ProjectileSpawn spawn) {
        EnemyProjectileSpawns.spawnGopongaProjectile(this, source, spawn);
    }

    public void spawnZoraProjectile(ZoraMotion.ProjectileSpawn spawn) {
        EnemyProjectileSpawns.spawnZoraProjectile(this, spawn);
    }

    public void spawnZombieChild(ZombieMotion.SpawnRequest spawn) {
        EnemyChildSpawns.spawnZombieChild(this, spawn);
    }

    public void spawnPokeySegment(PokeyMotion.SegmentSpawn spawn) {
        EnemyChildSpawns.spawnPokeySegment(this, spawn);
    }

    public void spawnWizrobeProjectile(RoomEntity source, WizrobeMotion.ProjectileSpawn spawn) {
        EnemyProjectileSpawns.spawnWizrobeProjectile(this, source, spawn);
    }

    public boolean spawnEvasiveClone(RoomEntity source, StalfosEvasiveMotion.CloneRequest request) {
        return EnemyChildSpawns.spawnEvasiveClone(this, source, request);
    }

    public void spawnEnemyProjectile(RoomEntity source, RoamingEnemyMotion.LaunchRequest request) {
        EnemyProjectileSpawns.spawnEnemyProjectile(this, source, request);
    }

    public void spawnEnemyProjectile(RoomEntity source, RoamingEnemyMotion.LaunchRequest request, int direction) {
        EnemyProjectileSpawns.spawnEnemyProjectile(this, source, request, direction);
    }

    public void spawnWingedOctorokRock(RoomEntity source, int direction) {
        EnemyProjectileSpawns.spawnWingedOctorokRock(this, source, direction);
    }

    public boolean canSpawnCuccoAngry() {
        return EnemyChildSpawns.canSpawnCuccoAngry(this);
    }

    public boolean spawnCuccoAngry(CuccoMotion.SpawnRequest request) {
        return EnemyChildSpawns.spawnCuccoAngry(this, request);
    }

    void spawnLaserSensor(RoomEntity parent) {
        LaserSpawns.spawnLaserSensor(this, parent);
    }

    boolean spawnLaserBeam(RoomEntity parent) {
        return LaserSpawns.spawnLaserBeam(this, parent);
    }
}
