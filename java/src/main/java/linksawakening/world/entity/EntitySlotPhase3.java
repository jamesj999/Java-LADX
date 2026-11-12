package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.LaserMotion;

/** Ordered slot dispatch phase 3; false preserves the ROM handler early return. */
final class EntitySlotPhase3 {
    static boolean run(EntityFrameExecution state) {

        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_BEFORE_LASER, state)) return false;
        if (state.status == EntityStatus.ACTIVE
                && !state.wasInitializing
                && state.runtime
                        .entityState
                        .motions
                        .get(LaserMotion.class, LaserMotion::new)
                        .isParent(state.entity.slot())) {
            LaserMotion.ParentUpdate laserUpdate = state.runtime
                    .entityState
                    .motions
                    .get(LaserMotion.class, LaserMotion::new)
                    .advanceParent(state.entity);
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
                && state.runtime
                        .entityState
                        .motions
                        .get(LaserMotion.class, LaserMotion::new)
                        .isSensor(state.entity.slot())) {
            int ownerSlot = state.runtime
                    .entityState
                    .motions
                    .get(LaserMotion.class, LaserMotion::new)
                    .parentSlot(state.entity.slot());
            RoomEntity owner =
                    ownerSlot >= 0 && ownerSlot < state.runtime.slots.length ? state.runtime.slots[ownerSlot] : null;
            LaserMotion.SensorUpdate sensorUpdate = state.runtime
                    .entityState
                    .motions
                    .get(LaserMotion.class, LaserMotion::new)
                    .advanceSensor(
                            state.entity,
                            owner,
                            state.linkEntityX,
                            state.linkEntityY,
                            state.projectileLinkState.invincibilityCounter());
            if (sensorUpdate.triggeredParent() && ownerSlot >= 0 && ownerSlot < state.runtime.slots.length) {
                // LaserLinkSensorHandler starts the parent's visible
                // pre-fire flash at the same time as its $20 countdown.
                state.runtime.entityState.combatTables.writeEnemyFlashCountdown(ownerSlot, 0x10);
            }
            if (sensorUpdate.unloaded()) {
                state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                return false;
            }
            state.updated = sensorUpdate.entity();
        }
        if (state.status == EntityStatus.ACTIVE
                && !state.wasInitializing
                && state.runtime
                        .entityState
                        .motions
                        .get(LaserMotion.class, LaserMotion::new)
                        .isBeam(state.entity.slot())) {
            var collisionEvent = EnemyProjectileCollision.check(
                    state.entity,
                    state.runtime
                            .entityState
                            .motions
                            .get(LaserMotion.class, LaserMotion::new)
                            .direction(state.entity.slot()),
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
                LaserMotion.BeamUpdate beamUpdate = state.runtime
                        .entityState
                        .motions
                        .get(LaserMotion.class, LaserMotion::new)
                        .advanceBeam(state.entity, state.backgroundCollision);
                if (beamUpdate.unloaded()) {
                    state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
                state.updated = beamUpdate.entity();
                state.runtime
                        .entityState
                        .motions
                        .get(LaserMotion.class, LaserMotion::new)
                        .reflectBeam(state.entity.slot(), state.projectileLinkState.direction());
                if (event.swordPokeVfx()) {
                    state.runtime.entityState.events.transientVfxRequests.add(
                            new TransientVfxRequest(TransientVfxType.SWORD_POKE, state.updated.x(), state.updated.y()));
                }
            } else {
                LaserMotion.BeamUpdate beamUpdate = state.runtime
                        .entityState
                        .motions
                        .get(LaserMotion.class, LaserMotion::new)
                        .advanceBeam(state.entity, state.backgroundCollision);
                if (beamUpdate.unloaded()) {
                    state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
                state.updated = beamUpdate.entity();
                state.runtime.entityState.events.transientVfxRequests.add(new TransientVfxRequest(
                        TransientVfxType.LASER_BEAM, state.updated.x() + 0x04, state.updated.y()));
            }
        }
        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_3_AFTER_LASER, state)) return false;
        return true;
    }
}
