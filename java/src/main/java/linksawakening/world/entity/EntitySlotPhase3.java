package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.LaserMotion;

/** Ordered slot dispatch phase 3; false preserves the ROM handler early return. */
final class EntitySlotPhase3 {
    static boolean run(EntityFrameExecution state) {

        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_BOMBITE_TYPE, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_ROAMING_ENEMY_TYPE, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_IRON_MASK, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_MOBLIN_SWORD, state)) return false;

        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_SPARK_TYPE, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_ZOL_GEL_TYPE, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_HIDING_ZOL, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_SPIKE_TRAP, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_PAIRODD, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_STAR, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_BLOOPER, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_PINCER, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_WINGED_OCTOROK, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_BUSH_CRAWLER, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_PAIRODD_PROJECTILE, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_WIZROBE_PROJECTILE, state)) return false;
        if (state.status == EntityStatus.ACTIVE
                && !state.wasInitializing
                && state.runtime.entityState.laserMotion.isParent(state.entity.slot())) {
            LaserMotion.ParentUpdate laserUpdate = state.runtime.entityState.laserMotion.advanceParent(state.entity);
            state.updated = laserUpdate.entity();
            if (laserUpdate.spawnSensor()) {
                state.runtime.entityEnemySpawns.spawnLaserSensor(state.entity);
            }
            if (laserUpdate.spawnBeam()) {
                if (state.runtime.entityEnemySpawns.spawnLaserBeam(state.entity)) {
                    state.runtime.entityState.events.pendingEntityEvents.add(new EntityCombatEvent(
                            state.entity.slot(),
                            state.entity.type(),
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.NOISE,
                            0x08));
                }
            }
        }
        if (state.status == EntityStatus.ACTIVE
                && !state.wasInitializing
                && state.runtime.entityState.laserMotion.isSensor(state.entity.slot())) {
            int ownerSlot = state.runtime.entityState.laserMotion.parentSlot(state.entity.slot());
            RoomEntity owner =
                    ownerSlot >= 0 && ownerSlot < state.runtime.slots.length ? state.runtime.slots[ownerSlot] : null;
            LaserMotion.SensorUpdate sensorUpdate = state.runtime.entityState.laserMotion.advanceSensor(
                    state.entity,
                    owner,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.projectileLinkState.invincibilityCounter());
            if (sensorUpdate.triggeredParent() && ownerSlot >= 0 && ownerSlot < state.runtime.slots.length) {
                // LaserLinkSensorHandler starts the parent's visible
                // pre-fire flash at the same time as its $20 countdown.
                state.runtime.entityState.enemyFlashCountdown[ownerSlot] = 0x10;
            }
            if (sensorUpdate.unloaded()) {
                state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                return false;
            }
            state.updated = sensorUpdate.entity();
        }
        if (state.status == EntityStatus.ACTIVE
                && !state.wasInitializing
                && state.runtime.entityState.laserMotion.isBeam(state.entity.slot())) {
            var collisionEvent = EnemyProjectileCollision.check(
                    state.entity,
                    state.runtime.entityState.laserMotion.direction(state.entity.slot()),
                    state.projectileLinkState);
            if (collisionEvent.isPresent()) {
                EntityProjectileEvent event = collisionEvent.orElseThrow();
                state.projectileEvents.add(event);
                if (event.remove()) {
                    state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
                // Bank-$15 moves the beam before consuming the collision
                // byte.  A reflected beam therefore advances once in
                // this frame, then reverses the selected speed axis.
                LaserMotion.BeamUpdate beamUpdate =
                        state.runtime.entityState.laserMotion.advanceBeam(state.entity, state.backgroundCollision);
                if (beamUpdate.unloaded()) {
                    state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
                state.updated = beamUpdate.entity();
                state.runtime.entityState.laserMotion.reflectBeam(
                        state.entity.slot(), state.projectileLinkState.direction());
                if (event.swordPokeVfx()) {
                    state.runtime.entityState.events.transientVfxRequests.add(
                            new TransientVfxRequest(TransientVfxType.SWORD_POKE, state.updated.x(), state.updated.y()));
                }
            } else {
                LaserMotion.BeamUpdate beamUpdate =
                        state.runtime.entityState.laserMotion.advanceBeam(state.entity, state.backgroundCollision);
                if (beamUpdate.unloaded()) {
                    state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
                state.updated = beamUpdate.entity();
                state.runtime.entityState.events.transientVfxRequests.add(new TransientVfxRequest(
                        TransientVfxType.LASER_BEAM, state.updated.x() + 0x04, state.updated.y()));
            }
        }
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_ENEMY_PROJECTILE_TYPE, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_GOPONGA_FLOWER_PROJECTILE, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_WATER_TEKTITE, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_FISH, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_CROW, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_CUCCO, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_BOO_BUDDY, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_DROPPABLE_FAIRY, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_STALFOS_AGGRESSIVE, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_STALFOS_EVASIVE, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_THREE_OF_A_KIND, state)) return false;

        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_LIKE_LIKE, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_GOOMBA, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_SNAKE, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_WIZROBE, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_PEAHAT, state)) return false;
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_ARMOS_STATUE, state)) return false;
        return true;
    }
}
