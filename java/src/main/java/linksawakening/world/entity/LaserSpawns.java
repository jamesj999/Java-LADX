package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.motion.*;

/** Source-ordered allocation and initialization for this spawn family. */
final class LaserSpawns {
    static void spawnLaserSensor(EntityEnemySpawnsServices runtime, RoomEntity parent) {
        int freeSlot = runtime.findFreeEntitySlot();
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
        runtime.slots()[freeSlot] = sensor;
        runtime.initializeHealthGroup(freeSlot);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyHealth(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        runtime.motions()
                .get(LaserMotion.class, LaserMotion::new)
                .initializeSensor(
                        freeSlot,
                        parent.slot(),
                        runtime.motions()
                                .get(LaserMotion.class, LaserMotion::new)
                                .direction(parent.slot()));
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
    }

    static boolean spawnLaserBeam(EntityEnemySpawnsServices runtime, RoomEntity parent) {
        int freeSlot = runtime.findFreeEntitySlot();
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
        runtime.slots()[freeSlot] = beam;
        runtime.initializeHealthGroup(freeSlot);
        runtime.physicsTables().writeBaseEntityFlipAttribute(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyHealth(freeSlot, 0);
        runtime.combatTables().writeEnemyFlashCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writePowerRecoilDeath(freeSlot, false);
        runtime.motions()
                .get(LaserMotion.class, LaserMotion::new)
                .initializeBeam(
                        freeSlot,
                        runtime.motions()
                                .get(LaserMotion.class, LaserMotion::new)
                                .direction(parent.slot()),
                        runtime.motions()
                                .get(LaserMotion.class, LaserMotion::new)
                                .speedX(parent.slot()),
                        runtime.motions()
                                .get(LaserMotion.class, LaserMotion::new)
                                .speedY(parent.slot()));
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
        return true;
    }
}
