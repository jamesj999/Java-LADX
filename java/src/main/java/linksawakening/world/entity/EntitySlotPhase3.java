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
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (!state.runtime().entityHandlers.tick(EntityTickStage.PHASE_3_BEFORE_LASER, state)) return false;
        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && state.runtime()
                        .entityState
                        .motions
                        .get(LaserMotion.class, LaserMotion::new)
                        .isParent(slotState.entity().slot())) {
            LaserMotion.ParentUpdate laserUpdate = state.runtime()
                    .entityState
                    .motions
                    .get(LaserMotion.class, LaserMotion::new)
                    .advanceParent(slotState.entity());
            slotState.updated(laserUpdate.entity());
            if (laserUpdate.spawnSensor()) {
                state.runtime().entityEnemySpawns.spawnLaserSensor(slotState.entity());
            }
            if (laserUpdate.spawnBeam()) {
                if (state.runtime().entityEnemySpawns.spawnLaserBeam(slotState.entity())) {
                    state.runtime()
                            .entityState
                            .events
                            .pendingEntityEvents
                            .add(new EntityCombatEvent(
                                    slotState.entity().slot(),
                                    slotState.entity().type(),
                                    0,
                                    false,
                                    EntityCombatEvent.SoundChannel.NOISE,
                                    0x08));
                }
            }
        }
        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && state.runtime()
                        .entityState
                        .motions
                        .get(LaserMotion.class, LaserMotion::new)
                        .isSensor(slotState.entity().slot())) {
            int ownerSlot = state.runtime()
                    .entityState
                    .motions
                    .get(LaserMotion.class, LaserMotion::new)
                    .parentSlot(slotState.entity().slot());
            RoomEntity owner = ownerSlot >= 0 && ownerSlot < state.runtime().slots.length
                    ? state.runtime().slots[ownerSlot]
                    : null;
            LaserMotion.SensorUpdate sensorUpdate = state.runtime()
                    .entityState
                    .motions
                    .get(LaserMotion.class, LaserMotion::new)
                    .advanceSensor(
                            slotState.entity(),
                            owner,
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.projectileLinkState().invincibilityCounter());
            if (sensorUpdate.triggeredParent() && ownerSlot >= 0 && ownerSlot < state.runtime().slots.length) {
                // LaserLinkSensorHandler starts the parent's visible
                // pre-fire flash at the same time as its $20 countdown.
                state.runtime().entityState.combatTables.writeEnemyFlashCountdown(ownerSlot, 0x10);
            }
            if (sensorUpdate.unloaded()) {
                state.runtime()
                        .lifecycle
                        .disableEntityWithoutPersistence(slotState.entity().slot());
                return false;
            }
            slotState.updated(sensorUpdate.entity());
        }
        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && state.runtime()
                        .entityState
                        .motions
                        .get(LaserMotion.class, LaserMotion::new)
                        .isBeam(slotState.entity().slot())) {
            var collisionEvent = EnemyProjectileCollision.check(
                    slotState.entity(),
                    state.runtime()
                            .entityState
                            .motions
                            .get(LaserMotion.class, LaserMotion::new)
                            .direction(slotState.entity().slot()),
                    frameInput.projectileLinkState());
            if (collisionEvent.isPresent()) {
                EntityProjectileEvent event = collisionEvent.orElseThrow();
                frameInput.projectileEvents().add(event);
                if (event.remove()) {
                    state.runtime()
                            .lifecycle
                            .disableEntityWithoutPersistence(slotState.entity().slot());
                    return false;
                }
                // Bank-$15 moves the beam before consuming the collision
                // byte.  A reflected beam therefore advances once in
                // this frame, then reverses the selected speed axis.
                LaserMotion.BeamUpdate beamUpdate = state.runtime()
                        .entityState
                        .motions
                        .get(LaserMotion.class, LaserMotion::new)
                        .advanceBeam(slotState.entity(), frameInput.backgroundCollision());
                if (beamUpdate.unloaded()) {
                    state.runtime()
                            .lifecycle
                            .disableEntityWithoutPersistence(slotState.entity().slot());
                    return false;
                }
                slotState.updated(beamUpdate.entity());
                state.runtime()
                        .entityState
                        .motions
                        .get(LaserMotion.class, LaserMotion::new)
                        .reflectBeam(
                                slotState.entity().slot(),
                                frameInput.projectileLinkState().direction());
                if (event.swordPokeVfx()) {
                    state.runtime()
                            .entityState
                            .events
                            .transientVfxRequests
                            .add(new TransientVfxRequest(
                                    TransientVfxType.SWORD_POKE,
                                    slotState.updated().x(),
                                    slotState.updated().y()));
                }
            } else {
                LaserMotion.BeamUpdate beamUpdate = state.runtime()
                        .entityState
                        .motions
                        .get(LaserMotion.class, LaserMotion::new)
                        .advanceBeam(slotState.entity(), frameInput.backgroundCollision());
                if (beamUpdate.unloaded()) {
                    state.runtime()
                            .lifecycle
                            .disableEntityWithoutPersistence(slotState.entity().slot());
                    return false;
                }
                slotState.updated(beamUpdate.entity());
                state.runtime()
                        .entityState
                        .events
                        .transientVfxRequests
                        .add(new TransientVfxRequest(
                                TransientVfxType.LASER_BEAM,
                                slotState.updated().x() + 0x04,
                                slotState.updated().y()));
            }
        }
        if (!state.runtime().entityHandlers.tick(EntityTickStage.PHASE_3_AFTER_LASER, state)) return false;
        return true;
    }
}
