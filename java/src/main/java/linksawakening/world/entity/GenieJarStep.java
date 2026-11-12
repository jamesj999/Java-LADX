package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.GenieMotion;

/** Per-invocation scratch for GenieJarBehavior.advanceGenieJar; values are assigned in source order. */
final class GenieJarStep {
    private linksawakening.world.entity.GenieEntityController owner;
    private linksawakening.world.entity.RoomEntity jarEntity;
    private int linkEntityY;
    private linksawakening.world.entity.GenieEntityControllerServices runtime;
    private int slot;
    private int freeSlots;
    private boolean bodySpawnSucceeded;
    private boolean rockSpawnSucceeded;
    private linksawakening.world.entity.motion.GenieMotion.Update update;
    private linksawakening.world.entity.motion.GenieMotion.JarState jar;
    private linksawakening.world.entity.RoomEntity fallingJar;
    private int z;
    private boolean landed;
    private boolean activatedThisTick;

    GenieJarStep(
            linksawakening.world.entity.GenieEntityController owner,
            linksawakening.world.entity.RoomEntity jarEntity,
            int linkEntityY) {
        this.owner = owner;
        this.jarEntity = jarEntity;
        this.linkEntityY = linkEntityY;
    }

    void run() {
        if (!advanceJarPhysics()) return;
        if (!advanceJarAttract()) return;
        if (!emitSpawnEffects()) return;
    }

    private boolean advanceJarPhysics() {

        runtime = owner.runtime;
        slot = jarEntity.slot();
        freeSlots = 0;
        for (RoomEntity candidate : runtime.slots()) {
            if (!candidate.loaded()) {
                freeSlots++;
            }
        }
        bodySpawnSucceeded = freeSlots >= 1;
        rockSpawnSucceeded = freeSlots >= 2;
        update = runtime.motions()
                .get(GenieMotion.class, GenieMotion::new)
                .advanceState0(
                        slot,
                        jarEntity.x(),
                        jarEntity.y(),
                        jarEntity.z(),
                        owner.geniePrivateState4[slot],
                        bodySpawnSucceeded,
                        rockSpawnSucceeded);
        jar = update.jar();
        owner.geniePrivateState1[slot] = jar.privateState1();
        runtime.combatTables().writeEnemyHealth(slot, jar.health());
        runtime.physicsTables().writeEnemyPhysicsFlags(slot, jar.physicsFlags());
        runtime.combatTables().writeEnemyHitboxFlags(slot, jar.hitboxFlags());

        fallingJar = runtime.slots()[slot];
        z = addFallingSpeedToPosition(fallingJar.z(), owner.genieSpeedZ[slot], owner.genieSpeedZAccumulator, slot);
        owner.genieSpeedZ[slot] = (owner.genieSpeedZ[slot] - 2) & 0xFF;
        landed = (z & 0x80) != 0;
        if (landed) {
            z = 0;
            owner.genieSpeedZ[slot] = 0;
        }
        runtime.slots()[slot] = withZ(fallingJar, z);

        activatedThisTick = false;
        if (update.bodySpawn() == null
                && owner.geniePrivateState4[slot] < GenieMotion.JAR_HEALTH_THRESHOLD
                && owner.genieActiveState[slot] == 0
                && (linkEntityY & 0xFF) < 0x70) {
            owner.genieActiveState[slot] = 1;
            runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xFF);
            activatedThisTick = true;
        }
        return true;
    }

    private boolean advanceJarAttract() {

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
        return true;
    }

    private boolean emitSpawnEffects() {

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
        return true;
    }
}
