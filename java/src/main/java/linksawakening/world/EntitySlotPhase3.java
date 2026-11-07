package linksawakening.world;

import static linksawakening.world.EntityMotionMath.*;
import static linksawakening.world.EntitySnapshots.*;
import static linksawakening.world.EntityTypeRules.*;
import linksawakening.entity.EntitySpriteSelection;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import static linksawakening.world.RoomEntityRuntime.*;

/** Ordered slot dispatch phase 3; false preserves the ROM handler early return. */
final class EntitySlotPhase3 {
    static boolean run(EntityFrameExecution state) {

            if (!state.runtime.entityHandlers.tick(133, state)) return false;
            if (!state.runtime.entityHandlers.tick(134, state)) return false;
            if (!state.runtime.entityHandlers.tick(44, state)) return false;
            if (!state.runtime.entityHandlers.tick(45, state)) return false;



            if (!state.runtime.entityHandlers.tick(135, state)) return false;
            if (!state.runtime.entityHandlers.tick(136, state)) return false;
            if (!state.runtime.entityHandlers.tick(46, state)) return false;
            if (!state.runtime.entityHandlers.tick(47, state)) return false;
            if (!state.runtime.entityHandlers.tick(48, state)) return false;
            if (!state.runtime.entityHandlers.tick(49, state)) return false;
            if (!state.runtime.entityHandlers.tick(50, state)) return false;
            if (!state.runtime.entityHandlers.tick(51, state)) return false;
            if (!state.runtime.entityHandlers.tick(52, state)) return false;
            if (!state.runtime.entityHandlers.tick(53, state)) return false;
            if (!state.runtime.entityHandlers.tick(54, state)) return false;
            if (!state.runtime.entityHandlers.tick(55, state)) return false;
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.runtime.laserMotion.isParent(state.entity.slot())) {
                LaserMotion.ParentUpdate laserUpdate = state.runtime.laserMotion.advanceParent(state.entity);
                state.updated = laserUpdate.entity();
                if (laserUpdate.spawnSensor()) {
                    state.runtime.entityEnemySpawns.spawnLaserSensor(state.entity);
                }
                if (laserUpdate.spawnBeam()) {
                    if (state.runtime.entityEnemySpawns.spawnLaserBeam(state.entity)) {
                        state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                            state.entity.slot(), state.entity.type(), 0, false,
                            EntityCombatEvent.SoundChannel.NOISE, 0x08));
                    }
                }
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.runtime.laserMotion.isSensor(state.entity.slot())) {
                int ownerSlot = state.runtime.laserMotion.parentSlot(state.entity.slot());
                RoomEntity owner = ownerSlot >= 0 && ownerSlot < state.runtime.slots.length
                    ? state.runtime.slots[ownerSlot] : null;
                LaserMotion.SensorUpdate sensorUpdate = state.runtime.laserMotion.advanceSensor(
                    state.entity, owner, state.linkEntityX, state.linkEntityY,
                    state.projectileLinkState.invincibilityCounter());
                if (sensorUpdate.triggeredParent() && ownerSlot >= 0 && ownerSlot < state.runtime.slots.length) {
                    // LaserLinkSensorHandler starts the parent's visible
                    // pre-fire flash at the same time as its $20 countdown.
                    state.runtime.enemyFlashCountdown[ownerSlot] = 0x10;
                }
                if (sensorUpdate.unloaded()) {
                    state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
                state.updated = sensorUpdate.entity();
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.runtime.laserMotion.isBeam(state.entity.slot())) {
                var collisionEvent = EnemyProjectileCollision.check(state.entity,
                    state.runtime.laserMotion.direction(state.entity.slot()), state.projectileLinkState);
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
                    LaserMotion.BeamUpdate beamUpdate = state.runtime.laserMotion.advanceBeam(
                        state.entity, state.backgroundCollision);
                    if (beamUpdate.unloaded()) {
                        state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                        return false;
                    }
                    state.updated = beamUpdate.entity();
                    state.runtime.laserMotion.reflectBeam(state.entity.slot(), state.projectileLinkState.direction());
                    if (event.swordPokeVfx()) {
                        state.runtime.transientVfxRequests.add(new TransientVfxRequest(
                            TransientVfxType.SWORD_POKE, state.updated.x(), state.updated.y()));
                    }
                } else {
                    LaserMotion.BeamUpdate beamUpdate = state.runtime.laserMotion.advanceBeam(
                        state.entity, state.backgroundCollision);
                    if (beamUpdate.unloaded()) {
                        state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                        return false;
                    }
                    state.updated = beamUpdate.entity();
                    state.runtime.transientVfxRequests.add(new TransientVfxRequest(
                        TransientVfxType.LASER_BEAM, state.updated.x() + 0x04, state.updated.y()));
                }
            }
            if (!state.runtime.entityHandlers.tick(137, state)) return false;
            if (!state.runtime.entityHandlers.tick(56, state)) return false;
            if (!state.runtime.entityHandlers.tick(57, state)) return false;
            if (!state.runtime.entityHandlers.tick(58, state)) return false;
            if (!state.runtime.entityHandlers.tick(59, state)) return false;
            if (!state.runtime.entityHandlers.tick(60, state)) return false;
            if (!state.runtime.entityHandlers.tick(61, state)) return false;
            if (!state.runtime.entityHandlers.tick(62, state)) return false;
            if (!state.runtime.entityHandlers.tick(63, state)) return false;
            if (!state.runtime.entityHandlers.tick(64, state)) return false;
            if (!state.runtime.entityHandlers.tick(65, state)) return false;

            if (!state.runtime.entityHandlers.tick(66, state)) return false;
            if (!state.runtime.entityHandlers.tick(67, state)) return false;
            if (!state.runtime.entityHandlers.tick(68, state)) return false;
            if (!state.runtime.entityHandlers.tick(69, state)) return false;
            if (!state.runtime.entityHandlers.tick(70, state)) return false;
            if (!state.runtime.entityHandlers.tick(71, state)) return false;
        return true;
    }
}
