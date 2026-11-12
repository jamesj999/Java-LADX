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
    private final EntityEnemySpawnsServices runtime;

    EntityEnemySpawns(EntityEnemySpawnsServices runtime) {
        this.runtime = runtime;
    }

    public int spawnEnemyBomb(int entityX, int entityY, int entityZ, int transitionCountdown) {
        return EnemyBombSpawns.spawnEnemyBomb(runtime, entityX, entityY, entityZ, transitionCountdown);
    }

    public int spawnEnemyBomb(
            int entityX, int entityY, int entityZ, int transitionCountdown, int speedX, int speedY, int speedZ) {
        return EnemyBombSpawns.spawnEnemyBomb(
                runtime, entityX, entityY, entityZ, transitionCountdown, speedX, speedY, speedZ);
    }

    public int spawnEnemyBomb(EnemyBombSpawn inputs) {
        return EnemyBombSpawns.spawnEnemyBomb(runtime, inputs);
    }

    public int spawnTarinTransformationBomb(RoomEntity tarin) {
        return EnemyBombSpawns.spawnTarinTransformationBomb(runtime, tarin);
    }

    public boolean spawnKanaletWallMoblin(RoomEntity wall) {
        return EnemyBombSpawns.spawnKanaletWallMoblin(runtime, wall);
    }

    public void spawnPairoddProjectile(RoomEntity source, int linkEntityX, int linkEntityY) {
        EnemyProjectileSpawns.spawnPairoddProjectile(runtime, source, linkEntityX, linkEntityY);
    }

    public void spawnGopongaProjectile(RoomEntity source, GiantGopongaMotion.ProjectileSpawn spawn) {
        EnemyProjectileSpawns.spawnGopongaProjectile(runtime, source, spawn);
    }

    public void spawnZoraProjectile(ZoraMotion.ProjectileSpawn spawn) {
        EnemyProjectileSpawns.spawnZoraProjectile(runtime, spawn);
    }

    public void spawnZombieChild(ZombieMotion.SpawnRequest spawn) {
        EnemyChildSpawns.spawnZombieChild(runtime, spawn);
    }

    public void spawnPokeySegment(PokeyMotion.SegmentSpawn spawn) {
        EnemyChildSpawns.spawnPokeySegment(runtime, spawn);
    }

    public void spawnWizrobeProjectile(RoomEntity source, WizrobeMotion.ProjectileSpawn spawn) {
        EnemyProjectileSpawns.spawnWizrobeProjectile(runtime, source, spawn);
    }

    public boolean spawnEvasiveClone(RoomEntity source, StalfosEvasiveMotion.CloneRequest request) {
        return EnemyChildSpawns.spawnEvasiveClone(runtime, source, request);
    }

    public void spawnEnemyProjectile(RoomEntity source, RoamingEnemyMotion.LaunchRequest request) {
        EnemyProjectileSpawns.spawnEnemyProjectile(runtime, source, request);
    }

    public void spawnEnemyProjectile(RoomEntity source, RoamingEnemyMotion.LaunchRequest request, int direction) {
        EnemyProjectileSpawns.spawnEnemyProjectile(runtime, source, request, direction);
    }

    public void spawnWingedOctorokRock(RoomEntity source, int direction) {
        EnemyProjectileSpawns.spawnWingedOctorokRock(runtime, source, direction);
    }

    public boolean canSpawnCuccoAngry() {
        return EnemyChildSpawns.canSpawnCuccoAngry(runtime);
    }

    public boolean spawnCuccoAngry(CuccoMotion.SpawnRequest request) {
        return EnemyChildSpawns.spawnCuccoAngry(runtime, request);
    }

    void spawnLaserSensor(RoomEntity parent) {
        LaserSpawns.spawnLaserSensor(runtime, parent);
    }

    boolean spawnLaserBeam(RoomEntity parent) {
        return LaserSpawns.spawnLaserBeam(runtime, parent);
    }
}
