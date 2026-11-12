package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.GenieMotion;

/** Focused behavior using the state owned by GenieEntityController. */
final class GenieJarBehavior {
    static void advanceGenieJar(GenieEntityController owner, RoomEntity jarEntity, int linkEntityY) {
        var runtime = owner.runtime;
        int slot = jarEntity.slot();
        int freeSlots = 0;
        for (RoomEntity candidate : runtime.slots()) {
            if (!candidate.loaded()) {
                freeSlots++;
            }
        }
        boolean bodySpawnSucceeded = freeSlots >= 1;
        boolean rockSpawnSucceeded = freeSlots >= 2;
        GenieMotion.Update update = runtime.motions()
                .get(GenieMotion.class, GenieMotion::new)
                .advanceState0(
                        slot,
                        jarEntity.x(),
                        jarEntity.y(),
                        jarEntity.z(),
                        owner.geniePrivateState4[slot],
                        bodySpawnSucceeded,
                        rockSpawnSucceeded);
        GenieMotion.JarState jar = update.jar();
        owner.geniePrivateState1[slot] = jar.privateState1();
        runtime.combatTables().writeEnemyHealth(slot, jar.health());
        runtime.physicsTables().writeEnemyPhysicsFlags(slot, jar.physicsFlags());
        runtime.combatTables().writeEnemyHitboxFlags(slot, jar.hitboxFlags());

        RoomEntity fallingJar = runtime.slots()[slot];
        int z = addFallingSpeedToPosition(fallingJar.z(), owner.genieSpeedZ[slot], owner.genieSpeedZAccumulator, slot);
        owner.genieSpeedZ[slot] = (owner.genieSpeedZ[slot] - 2) & 0xFF;
        boolean landed = (z & 0x80) != 0;
        if (landed) {
            z = 0;
            owner.genieSpeedZ[slot] = 0;
        }
        runtime.slots()[slot] = withZ(fallingJar, z);

        boolean activatedThisTick = false;
        if (update.bodySpawn() == null
                && owner.geniePrivateState4[slot] < GenieMotion.JAR_HEALTH_THRESHOLD
                && owner.genieActiveState[slot] == 0
                && (linkEntityY & 0xFF) < 0x70) {
            owner.genieActiveState[slot] = 1;
            runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xFF);
            activatedThisTick = true;
        }
        if (update.bodySpawn() == null
                && owner.geniePrivateState4[slot] < GenieMotion.JAR_HEALTH_THRESHOLD
                && owner.genieActiveState[slot] == 1
                && !activatedThisTick) {
            if (landed && owner.geniePrivateState3[slot] == 0) {
                owner.genieSpeedZ[slot] = 0x10;
                runtime.events()
                        .pendingEntityEvents
                        .add(new EntityCombatEvent(
                                slot,
                                EntityRomConstants.ENTITY_GENIE,
                                0,
                                false,
                                EntityCombatEvent.SoundChannel.JINGLE,
                                0x20));
            }
            RoomEntity movedJar = owner.advanceGenieJarAttract(runtime.slots()[slot]);
            runtime.slots()[slot] = movedJar;
            if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 1) {
                owner.spawnGenieEntity(
                        movedJar.x(),
                        movedJar.y() - 0x26,
                        2,
                        0x47,
                        GenieMotion.INITIAL_HEALTH,
                        GenieMotion.INITIAL_PHYSICS_FLAGS,
                        GenieMotion.INITIAL_HITBOX_FLAGS);
                runtime.events()
                        .pendingEntityEvents
                        .add(new EntityCombatEvent(
                                slot,
                                EntityRomConstants.ENTITY_GENIE,
                                0,
                                false,
                                EntityCombatEvent.SoundChannel.JINGLE,
                                0x06));
            }
        }

        if (update.bodySpawn() != null) {
            owner.spawnGenieBody(update.bodySpawn());
        }
        if (update.rockSpawn() != null && update.rockSpawnSucceeded()) {
            GenieMotion.RockSpawnRequest rock = update.rockSpawn();
            runtime.entityPlayerSpawns()
                    .spawnLiftableRockAnimation(
                            rock.x(),
                            rock.y(),
                            rock.spriteVariant(),
                            rock.privateCountdown1(),
                            rock.physicsFlags(),
                            -1);
        }
        if (update.noiseSfx() >= 0) {
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            slot,
                            EntityRomConstants.ENTITY_GENIE,
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.NOISE,
                            update.noiseSfx()));
        }
        if (update.sourceUnloaded()) {
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
        }
    }

    static RoomEntity registerGenieJarImpact(GenieEntityController owner, RoomEntity jar) {
        var runtime = owner.runtime;
        int slot = jar.slot();
        if (owner.genieImpactRegisteredThisFrame[slot]) {
            return jar;
        }
        owner.genieImpactRegisteredThisFrame[slot] = true;
        owner.geniePrivateState4[slot] = (owner.geniePrivateState4[slot] + 1) & 0xFF;
        runtime.combatTables().writeEnemyFlashCountdown(slot, 0x20);
        runtime.events()
                .pendingEntityEvents
                .add(new EntityCombatEvent(
                        slot, EntityRomConstants.ENTITY_GENIE, 0, false, EntityCombatEvent.SoundChannel.WAVE, 0x07));
        if (owner.geniePrivateState4[slot] != GenieMotion.JAR_HEALTH_THRESHOLD) {
            return jar;
        }
        owner.genieActiveState[slot] = 1;
        owner.geniePrivateState3[slot] = 0;
        runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x80);
        runtime.motions().get(ThrownEntityMotion.class, ThrownEntityMotion::new).clear(slot);
        runtime.liftThrow().writeThrownMotionInitialized(slot, false);
        return withStatus(jar, EntityStatus.ACTIVE);
    }
}
