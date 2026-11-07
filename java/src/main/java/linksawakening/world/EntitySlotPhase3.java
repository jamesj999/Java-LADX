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

            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && isBombiteType(state.entity.type())) {
                BombiteMotion.Update bombiteUpdate = state.runtime.bombiteMotion.advance(
                    state.entity, state.frame, state.linkEntityX, state.linkEntityY,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.runtime.slowTransitionCountdown[state.entity.slot()],
                    state.runtime.bombPrivateCountdown1[state.entity.slot()],
                    state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()],
                    state.runtime.runningWithPegasusBoots, state.randomByteSupplier, state.backgroundCollision);
                state.updated = withVariant(bombiteUpdate.entity(), bombiteUpdate.spriteVariant());
                state.preserveBombitePresentation = true;
                state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                    bombiteUpdate.transitionCountdown();
                state.runtime.slowTransitionCountdown[state.entity.slot()] =
                    bombiteUpdate.slowTransitionCountdown();
                state.runtime.bombPrivateCountdown1[state.entity.slot()] = bombiteUpdate.privateCountdown1();
                if (bombiteUpdate.bumpJingle()) {
                    state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                        state.entity.slot(), state.entity.type(), 0, false,
                        EntityCombatEvent.SoundChannel.JINGLE, 0x09));
                }
                boolean entityCollisionExplosion = state.entity.type() == state.runtime.ENTITY_BOUNCING_BOMBITE
                    && state.runtime.bombiteMotion.state(state.entity.slot()) == 2
                    && state.runtime.projectiles.collideBouncingBombiteWithEntities(state.updated, state.frame);
                if (bombiteUpdate.explode() || entityCollisionExplosion) {
                    // BombiteExplode writes the already-positioned source
                    // coordinates into a fresh type-$02 enemy bomb and then
                    // lets ConfigureNewEntity supply z=0 and ignore-hits=1.
                    int bombSlot = state.runtime.spawnEnemyBomb(state.updated.x(), state.updated.y(), 0,
                        state.runtime.BOMBITE_EXPLOSION_COUNTDOWN);
                    if (bombSlot >= 0) {
                        state.runtime.enemyIgnoreHitsCountdown[bombSlot] = 0x01;
                        state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                            bombSlot, state.runtime.ENTITY_BOMB, 0, false,
                            EntityCombatEvent.SoundChannel.NOISE,
                            state.runtime.BOMBITE_EXPLOSION_SOUND_ID));
                    }
                    state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && isRoamingEnemyType(state.entity.type())) {
                if (state.entity.type() == state.runtime.ENTITY_IRON_MASK
                    && state.runtime.ironMaskPrivateState2[state.entity.slot()] != 0) {
                    UnmaskedIronMaskMotion.Update unmaskedUpdate =
                        state.runtime.unmaskedIronMaskMotion.advance(state.entity, state.frame, state.randomByteSupplier,
                            state.runtime.backgroundInteraction, state.backgroundCollision,
                            state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()], state.frame);
                    state.updated = unmaskedUpdate.entity();
                    state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                        unmaskedUpdate.transitionCountdown();
                } else {
                    RoamingEnemyMotion.Update roamingUpdate = state.runtime.backgroundInteraction == null
                        ? state.runtime.roamingEnemyMotion.advance(state.entity, state.linkEntityX, state.linkEntityY,
                            state.randomByteSupplier, state.backgroundCollision, state.creditsGameplay)
                        : state.runtime.roamingEnemyMotion.advanceWithInteraction(state.entity, state.linkEntityX,
                            state.linkEntityY, state.randomByteSupplier, state.runtime.backgroundInteraction,
                            state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()], state.frame, state.creditsGameplay);
                    state.updated = roamingUpdate.entity();
                    if (roamingUpdate.launchRequest() != null) {
                        state.runtime.projectileLaunchRequests.add(roamingUpdate.launchRequest());
                        state.runtime.entityEnemySpawns.spawnEnemyProjectile(state.entity, roamingUpdate.launchRequest());
                    }
                }
            }
            if (!state.runtime.entityHandlers.tick(44, state)) return false;
            if (!state.runtime.entityHandlers.tick(45, state)) return false;



            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing && isSparkType(state.entity.type())) {
                state.updated = state.runtime.sparkMotion.advance(state.entity, state.frame, state.backgroundCollision);
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing && state.handlerInteractive
                && isZolGelType(state.entity.type())) {
                RoomEntityBackgroundInteraction zolGelBackgroundInteraction = state.runtime.backgroundInteraction;
                if (zolGelBackgroundInteraction == null && state.backgroundCollision != null) {
                    zolGelBackgroundInteraction = RoomEntityBackgroundInteraction.fromBoolean(
                        state.backgroundCollision);
                }
                ZolGelMotion.Update zolGelUpdate = state.runtime.zolGelMotion.advance(state.entity, state.linkEntityX,
                    state.linkEntityY, state.linkZ, state.randomByteSupplier, zolGelBackgroundInteraction,
                    state.frame, state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()],
                    state.runtime.joypadHeld || state.runtime.actionButtonsHeld);
                state.updated = zolGelUpdate.entity();
                state.runtime.gelClingingToLink |= zolGelUpdate.clinging();
                if (zolGelUpdate.split() != null) {
                    state.updated = state.runtime.lifecycle.applyZolSplit(state.entity, state.updated, zolGelUpdate.split());
                }
            }
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
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && isEnemyProjectileType(state.entity.type())) {
                boolean preexistingCollision =
                    state.runtime.enemyProjectileMotion.pendingLinkCollision(state.entity.slot());
                boolean hasLinkCollision = state.runtime.enemyProjectileMotion.transitionCountdown(state.entity.slot()) == 0;
                if (hasLinkCollision) {
                    var collisionEvent = EnemyProjectileCollision.check(state.entity,
                        state.runtime.enemyProjectileMotion.direction(state.entity.slot()), state.projectileLinkState);
                    if (collisionEvent.isPresent()) {
                        EntityProjectileEvent event = collisionEvent.orElseThrow();
                        state.projectileEvents.add(event);
                        if (event.remove()) {
                            state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                            return false;
                        }
                        preexistingCollision |= event.startsWallTransition();
                    }
                }
                EnemyProjectileMotion.Update projectileUpdate = state.runtime.enemyProjectileMotion.advance(
                    state.entity, state.backgroundCollision, preexistingCollision, state.frame,
                    state.runtime.entityGroundStatus[state.entity.slot()], state.runtime.groundInteractionSideScrolling,
                    state.handlerInteractive, state.projectileTimersInteractive);
                if (projectileUpdate.unloaded()) {
                    state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
                if (projectileUpdate.collidedWithWall()) {
                    // $6B0C skips the poke jingle when Link collision supplied
                    // $FF (including shield blocks, which already play a ting).
                    if (!preexistingCollision) {
                        state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                            state.entity.slot(), state.entity.type(), 0, false,
                            EntityCombatEvent.SoundChannel.JINGLE, 0x07));
                    }
                    state.runtime.swordMoblinAlertingSoundCounter = 0x04;
                }
                state.updated = projectileUpdate.entity();
            }
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
