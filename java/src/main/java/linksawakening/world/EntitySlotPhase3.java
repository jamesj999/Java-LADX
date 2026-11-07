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
                    && state.runtime.collideBouncingBombiteWithEntities(state.updated, state.frame);
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
                    state.runtime.disableEntityWithoutPersistence(state.entity.slot());
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
                        state.runtime.spawnEnemyProjectile(state.entity, roamingUpdate.launchRequest());
                    }
                }
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_IRON_MASK) {
                int privateState2 = state.runtime.ironMaskPrivateState2[state.entity.slot()];
                int variant = privateState2 == 0
                    ? state.updated.spriteVariant() : ((state.frame >>> 4) & 0x01);
                if (state.runtime.spriteHandlers != null) {
                    state.updated = withDefinition(state.updated,
                        state.runtime.spriteHandlers.forIronMaskState(privateState2), variant);
                }
                state.preserveIronMaskPresentation = true;
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && !state.runtime.dialogActive && !state.runtime.inventoryAppearing && !state.runtime.roomTransitionActive
                && state.runtime.gameplayWorld && state.runtime.transitionSequenceCounter == 0x04 && !state.runtime.witchGotItemPresentationActive()
                && state.entity.type() == state.runtime.ENTITY_MOBLIN_SWORD) {
                RoomEntityBackgroundInteraction swordBackgroundInteraction = state.runtime.backgroundInteraction;
                if (swordBackgroundInteraction == null && state.backgroundCollision != null) {
                    swordBackgroundInteraction = RoomEntityBackgroundInteraction.fromBoolean(
                        state.backgroundCollision);
                }
                MoblinSwordMotion.Update moblinSwordUpdate = state.runtime.moblinSwordMotion.advance(
                    state.entity, state.linkEntityX, state.linkEntityY,
                    swordBackgroundInteraction, state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()],
                    state.runtime.swordMoblinAlertingSoundCounter, state.runtime.entityMapId,
                    state.runtime.transitionSequenceCounter, state.frame);
                state.updated = moblinSwordUpdate.entity();
                if (moblinSwordUpdate.dialogRequested()) {
                    state.runtime.pendingDialogRequests.add(new DialogRequest(1, state.runtime.DIALOG_MOBLIN));
                }
            }



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
                    state.updated = state.runtime.applyZolSplit(state.entity, state.updated, zolGelUpdate.split());
                }
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_HIDING_ZOL) {
                RoomEntityBackgroundInteraction hidingZolBackgroundInteraction =
                    state.runtime.backgroundInteraction;
                if (hidingZolBackgroundInteraction == null && state.backgroundCollision != null) {
                    hidingZolBackgroundInteraction = RoomEntityBackgroundInteraction.fromBoolean(
                        state.backgroundCollision);
                }
                HidingZolMotion.Update hidingZolUpdate = state.runtime.hidingZolMotion.advance(
                    state.entity, state.linkEntityX, state.linkEntityY, state.randomByteSupplier,
                    hidingZolBackgroundInteraction, state.frame);
                state.updated = hidingZolUpdate.entity();
                if (hidingZolUpdate.clearsIgnoreHitsCountdown()) {
                    // func_007_73F7 temporarily writes $03 around the
                    // background helper, then clears the shared byte.
                    state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()] = 0;
                }
                if (hidingZolUpdate.jumpJingle()) {
                    state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                        state.entity.slot(), state.entity.type(), 0, false,
                        EntityCombatEvent.SoundChannel.JINGLE, 0x24));
                }
                if (hidingZolUpdate.physicsFlags() >= 0) {
                    state.runtime.enemyPhysicsFlags[state.entity.slot()] = hidingZolUpdate.physicsFlags();
                }
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_SPIKE_TRAP) {
                SpikeTrapMotion.Update spikeTrapUpdate = state.runtime.spikeTrapMotion.advance(
                    state.entity, state.linkEntityX, state.linkEntityY, state.backgroundCollision);
                state.updated = spikeTrapUpdate.entity();
                if (spikeTrapUpdate.soundChannel() != EntityCombatEvent.SoundChannel.NONE) {
                    state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                        state.entity.slot(), state.entity.type(), 0, false,
                        spikeTrapUpdate.soundChannel(), spikeTrapUpdate.soundId()));
                }
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_PAIRODD) {
                PairoddMotion.Update pairoddUpdate = state.runtime.pairoddMotion.advance(state.entity, state.frame,
                    state.linkEntityX, state.linkEntityY, state.randomByteSupplier,
                    state.runtime.enemyFlashCountdown[state.entity.slot()] > 0);
                state.updated = pairoddUpdate.entity();
                if (pairoddUpdate.spawnProjectile()) {
                    state.runtime.spawnPairoddProjectile(state.entity, state.linkEntityX, state.linkEntityY);
                }
                if (pairoddUpdate.teleportJingle()) {
                    state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                        state.entity.slot(), state.entity.type(), 0, false,
                        EntityCombatEvent.SoundChannel.JINGLE, 0x3C));
                }
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_STAR && state.handlerLinkCollisionEnabled) {
                RoomEntityBackgroundInteraction starBackgroundInteraction = state.runtime.backgroundInteraction;
                if (starBackgroundInteraction == null && state.backgroundCollision != null) {
                    starBackgroundInteraction = RoomEntityBackgroundInteraction.fromBoolean(
                        state.backgroundCollision);
                }
                state.updated = state.runtime.starMotion.advance(state.entity, state.frame, state.randomByteSupplier,
                    starBackgroundInteraction, state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()]);
                state.preserveStarPresentation = true;
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_BLOOPER && state.handlerLinkCollisionEnabled) {
                RoomEntityBackgroundInteraction blooperBackgroundInteraction = state.runtime.backgroundInteraction;
                if (blooperBackgroundInteraction == null && state.backgroundCollision != null) {
                    blooperBackgroundInteraction = RoomEntityBackgroundInteraction.fromBoolean(
                        state.backgroundCollision);
                }
                BlooperMotion.Movement movement = state.runtime.blooperMotion.move(state.entity, state.frame,
                    blooperBackgroundInteraction, state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()]);
                RoomEntityGroundInteraction.Result groundResult = Objects.requireNonNull(
                    state.runtime.groundInteraction.apply(movement.entity(), state.frame,
                        state.runtime.entityGroundStatus[state.entity.slot()], state.runtime.verticalSpeedZ(movement.entity()),
                        state.runtime.groundInteractionSideScrolling),
                    "Room entity ground interaction returned null");
                BlooperMotion.Update blooperUpdate = state.runtime.blooperMotion.finish(movement, groundResult,
                    state.frame, state.linkEntityX, state.linkEntityY);
                state.updated = blooperUpdate.entity();
                state.preAppliedGroundResult = blooperUpdate.groundResult();
                state.preserveBlooperPresentation = true;
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_PINCER && state.handlerLinkCollisionEnabled) {
                // PincerEntityHandler clears its stale ground status before
                // rendering and does not call the generic background helper.
                state.runtime.entityGroundStatus[state.entity.slot()] = 0;
                PincerMotion.Update pincerUpdate = state.runtime.pincerMotion.advance(
                    state.entity, state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.runtime.enemyPhysicsFlags[state.entity.slot()], state.linkEntityX, state.linkEntityY);
                state.updated = pincerUpdate.entity();
                state.runtime.enemyTransitionCountdown[state.entity.slot()] = pincerUpdate.transitionCountdown();
                state.runtime.enemyPhysicsFlags[state.entity.slot()] = pincerUpdate.physicsFlags();
                state.preservePincerPresentation = true;
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_WINGED_OCTOROK && state.handlerLinkCollisionEnabled) {
                WingedOctorokMotion.Update wingedUpdate = state.runtime.wingedOctorokMotion.advance(
                    state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.randomByteSupplier,
                    state.backgroundCollision, state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()], state.runtime.actionButtonAHeld,
                    state.runtime.actionButtonBHeld,
                    state.runtime.linkItemA, state.runtime.linkItemB);
                state.updated = wingedUpdate.entity();
                state.runtime.enemyTransitionCountdown[state.entity.slot()] = wingedUpdate.transitionCountdown();
                state.preserveWingedOctorokPresentation = true;
                state.runtime.wingedStateTwoThisFrame[state.entity.slot()] = wingedUpdate.skippedDefaultCollision();
                if (wingedUpdate.rockSpawn() != null) {
                    state.runtime.spawnWingedOctorokRock(state.updated, wingedUpdate.rockSpawn().direction());
                }
                if (wingedUpdate.jumped()) {
                    state.runtime.wingedSwordAttackThisFrame[state.entity.slot()] = true;
                    state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                        state.entity.slot(), state.entity.type(), 0, false,
                        EntityCombatEvent.SoundChannel.JINGLE, 0x24));
                }
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_BUSH_CRAWLER && state.handlerLinkCollisionEnabled) {
                BushCrawlerMotion.Update bushUpdate = state.runtime.bushCrawlerMotion.advance(
                    state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.randomByteSupplier,
                    state.backgroundCollision, state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    RoomEntityCombatRules.overlapsLink(state.entity, state.linkEntityX, state.linkEntityY),
                    state.runtime.actionButtonAHeld, state.runtime.actionButtonBHeld, state.runtime.linkItemA, state.runtime.linkItemB);
                state.runtime.enemyTransitionCountdown[state.entity.slot()] = bushUpdate.transitionCountdown();
                state.runtime.fallingVisualYOffset[state.entity.slot()] = bushUpdate.visualYOffset();
                state.updated = bushUpdate.entity();
                if (bushUpdate.specialState()) {
                    state.updated = withDefinition(state.updated,
                        state.runtime.spriteDefinitionForBushCrawlerState(0x02), state.updated.spriteVariant());
                } else if (bushUpdate.crawlOverlayRendered()) {
                    state.updated = withDefinition(state.updated,
                        state.runtime.spriteDefinitionForBushCrawlerCrawlState(bushUpdate.privateState4()),
                        state.updated.spriteVariant());
                } else {
                    state.updated = withDefinition(state.updated, state.runtime.spriteDefinitionFor(state.runtime.ENTITY_BUSH_CRAWLER),
                        state.updated.spriteVariant());
                }
                if (!bushUpdate.specialState()) {
                    // BushCrawlerEntityHandler writes $04 immediately before
                    // its default enemy collision path on every interactive
                    // normal pass. The static health group is $00.
                    state.runtime.enemyHealth[state.entity.slot()] = 0x04;
                }
                state.preserveBushCrawlerPresentation = true;
                if (bushUpdate.liftRequested()
                    && state.runtime.beginBushCrawlerLift(state.index, state.updated, bushUpdate.privateState4(),
                        state.romLinkDirection)) {
                    return false;
                }
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_PAIRODD_PROJECTILE) {
                state.updated = state.runtime.pairoddProjectileMotion.advance(state.entity, state.frame);
                boolean objectCollisionHit = state.objectCollision != null
                    && state.objectCollision.collides(state.updated);
                boolean removePairoddProjectile = false;
                boolean swordPokeVfxQueued = false;
                var collisionEvent = EnemyProjectileCollision.check(state.updated,
                    state.runtime.pairoddProjectileMotion.direction(state.entity.slot()), state.projectileLinkState);
                if (collisionEvent.isPresent()) {
                    EntityProjectileEvent event = collisionEvent.orElseThrow();
                    state.projectileEvents.add(event);
                    if (event.swordPokeVfx()) {
                        state.runtime.transientVfxRequests.add(new TransientVfxRequest(
                            TransientVfxType.SWORD_POKE, event.swordPokeX(), event.swordPokeY()));
                        swordPokeVfxQueued = true;
                    }
                    if (event.remove()) {
                        removePairoddProjectile = true;
                    }
                }
                var swordCollisionEvent = EnemyProjectileCollision.checkSwordCollision(
                    state.updated, state.swordCollisionActive, state.swordX, state.swordWidth, state.swordY, state.swordHeight);
                if (swordCollisionEvent.isPresent()) {
                    EntityProjectileEvent event = swordCollisionEvent.orElseThrow();
                    state.projectileEvents.add(event);
                    if (event.swordPokeVfx() && !swordPokeVfxQueued) {
                        state.runtime.transientVfxRequests.add(new TransientVfxRequest(
                            TransientVfxType.SWORD_POKE, event.swordPokeX(), event.swordPokeY()));
                        swordPokeVfxQueued = true;
                    }
                    removePairoddProjectile = true;
                }
                if (objectCollisionHit) {
                    if (!swordPokeVfxQueued) {
                        state.runtime.transientVfxRequests.add(new TransientVfxRequest(
                            TransientVfxType.SWORD_POKE, state.updated.x(),
                            (state.updated.y() - state.updated.z()) & 0xFF));
                    }
                    removePairoddProjectile = true;
                }
                if (removePairoddProjectile) {
                    state.runtime.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_WIZROBE_PROJECTILE) {
                var collisionEvent = EnemyProjectileCollision.check(state.entity,
                    state.runtime.wizrobeProjectileMotion.direction(state.entity.slot()), state.projectileLinkState);
                if (collisionEvent.isPresent()) {
                    state.projectileEvents.add(collisionEvent.orElseThrow());
                }
                state.updated = state.runtime.wizrobeProjectileMotion.advance(state.entity);
                state.updated = withFlipAttribute(state.updated,
                    state.runtime.wizrobeProjectileMotion.flipAttribute(state.frame));
                state.preserveWizrobeProjectilePresentation = true;
                boolean objectCollisionHit = state.objectCollision != null
                    && state.objectCollision.collides(state.updated);
                if (objectCollisionHit
                    || collisionEvent.map(EntityProjectileEvent::remove).orElse(false)) {
                    state.runtime.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.runtime.laserMotion.isParent(state.entity.slot())) {
                LaserMotion.ParentUpdate laserUpdate = state.runtime.laserMotion.advanceParent(state.entity);
                state.updated = laserUpdate.entity();
                if (laserUpdate.spawnSensor()) {
                    state.runtime.spawnLaserSensor(state.entity);
                }
                if (laserUpdate.spawnBeam()) {
                    if (state.runtime.spawnLaserBeam(state.entity)) {
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
                    state.runtime.disableEntityWithoutPersistence(state.entity.slot());
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
                        state.runtime.disableEntityWithoutPersistence(state.entity.slot());
                        return false;
                    }
                    // Bank-$15 moves the beam before consuming the collision
                    // byte.  A reflected beam therefore advances once in
                    // this frame, then reverses the selected speed axis.
                    LaserMotion.BeamUpdate beamUpdate = state.runtime.laserMotion.advanceBeam(
                        state.entity, state.backgroundCollision);
                    if (beamUpdate.unloaded()) {
                        state.runtime.disableEntityWithoutPersistence(state.entity.slot());
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
                        state.runtime.disableEntityWithoutPersistence(state.entity.slot());
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
                            state.runtime.disableEntityWithoutPersistence(state.entity.slot());
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
                    state.runtime.disableEntityWithoutPersistence(state.entity.slot());
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
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_GOPONGA_FLOWER_PROJECTILE) {
                GopongaProjectileMotion.Update projectileUpdate = state.runtime.gopongaProjectileMotion.advance(
                    state.entity, state.frame, state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()], state.handlerLinkCollisionEnabled);
                state.runtime.enemyHealth[state.entity.slot()] = GopongaProjectileMotion.HEALTH_OVERRIDE;
                state.runtime.enemyTransitionCountdown[state.entity.slot()] = projectileUpdate.transitionCountdown();
                state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()] = projectileUpdate.ignoreHitsCountdown();
                if (projectileUpdate.unloadRequested()) {
                    state.runtime.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
                state.updated = projectileUpdate.entity();
                state.preserveGopongaProjectilePresentation = true;
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_WATER_TEKTITE) {
                state.updated = state.runtime.waterTektiteMotion.advance(state.entity, state.frame, state.randomByteSupplier,
                    state.backgroundCollision);
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_FISH) {
                FishMotion.Update fishUpdate = state.runtime.fishMotion.advance(state.entity,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()], state.randomByteSupplier,
                    state.backgroundCollision);
                state.updated = fishUpdate.entity();
                state.runtime.enemyTransitionCountdown[state.entity.slot()] = fishUpdate.transitionCountdown();
                state.runtime.enemyPhysicsFlags[state.entity.slot()] = fishUpdate.physicsFlags();
                if (fishUpdate.waterSplash()) {
                    state.runtime.transientVfxRequests.add(new TransientVfxRequest(
                        TransientVfxType.WATER_SPLASH, state.updated.x(), state.updated.y()));
                    state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                        state.updated.slot(), state.updated.type(), 0, false,
                        EntityCombatEvent.SoundChannel.JINGLE, 0x0E));
                }
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_CROW) {
                CrowMotion.Update crowUpdate = state.runtime.crowMotion.advance(state.entity,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()], state.frame, state.linkEntityX, state.linkEntityY,
                    state.runtime.entityRoomId != 0x58, state.runtime.kanaletCrowTakeoffTriggered(state.entity));
                state.updated = crowUpdate.entity();
                state.runtime.enemyTransitionCountdown[state.entity.slot()] = crowUpdate.transitionCountdown();
                state.runtime.enemyPhysicsFlags[state.entity.slot()] = crowUpdate.physicsFlags();
                if (crowUpdate.boomerangSound()) {
                    state.runtime.playBoomerangSoundIfDue(state.updated);
                }
                if (crowUpdate.unloaded()) {
                    state.runtime.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_CUCCO && state.handlerLinkCollisionEnabled) {
                CuccoMotion.Update cuccoUpdate = state.runtime.cuccoMotion.advance(state.entity, state.frame,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()], state.runtime.enemyFlashCountdown[state.entity.slot()],
                    state.linkEntityX, state.linkEntityY, state.linkZ, state.handlerLinkCollisionEnabled,
                    state.runtime.cuccoPowerBraceletHeld(), state.runtime.linkAttackStepAnimationCountdown != 0,
                    state.randomByteSupplier, state.backgroundCollision,
                    state.runtime.indoorRoom, state.runtime.followingNpcState.marinFollowing(), state.runtime.transitionSequenceCounter);
                state.updated = cuccoUpdate.entity();
                state.runtime.enemyTransitionCountdown[state.entity.slot()] = cuccoUpdate.transitionCountdown();
                state.runtime.enemyFlashCountdown[state.entity.slot()] = cuccoUpdate.flashCountdown();
                state.runtime.enemyHealth[state.entity.slot()] = CuccoMotion.HEALTH_OVERRIDE;
                state.preserveCuccoPresentation = true;
                if (cuccoUpdate.dialogLowId() >= 0) {
                    state.runtime.pendingDialogRequests.add(new DialogRequest(
                        cuccoUpdate.dialogTableId(), cuccoUpdate.dialogLowId()));
                }
                if (cuccoUpdate.unloaded()) {
                    state.runtime.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
                if (cuccoUpdate.liftRequested()
                    && state.runtime.beginLift(state.entity.slot(), state.romLinkDirection)) {
                    state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                        state.entity.slot(), state.entity.type(), 0, false,
                        EntityCombatEvent.SoundChannel.WAVE, CuccoMotion.LIFT_WAVE_SFX));
                    state.runtime.slots[state.index] = state.runtime.advanceLiftedEntity(
                        state.runtime.renderLiftedEntity(state.runtime.slots[state.index], state.frame), state.linkEntityX, state.linkEntityY,
                        state.linkZ, state.romLinkDirection);
                    return false;
                }
                if (cuccoUpdate.spawnRequest() != null
                    && state.runtime.spawnCuccoAngry(cuccoUpdate.spawnRequest())) {
                    state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                        state.entity.slot(), state.entity.type(), 0, false,
                        EntityCombatEvent.SoundChannel.WAVE, CuccoMotion.CUCCO_HURT_WAVE_SFX));
                }
                if (cuccoUpdate.boomerangSound()) {
                    state.runtime.playBoomerangSoundIfDue(state.updated);
                }
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_BOO_BUDDY) {
                BooBuddyMotion.Update booBuddyUpdate = state.runtime.booBuddyMotion.advanceAfterTimers(state.entity, state.frame,
                    state.linkEntityX, state.linkEntityY, state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.runtime.booBuddyTriggerCount, state.swordCollisionActive, state.randomByteSupplier);
                state.updated = booBuddyUpdate.entity();
                state.runtime.enemyTransitionCountdown[state.entity.slot()] = booBuddyUpdate.transitionCountdown();
                if (booBuddyUpdate.healthOverride() >= 0) {
                    state.runtime.enemyHealth[state.entity.slot()] = booBuddyUpdate.healthOverride();
                }
                if (booBuddyUpdate.unloaded()) {
                    state.runtime.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_DROPPABLE_FAIRY) {
                FairyMotion.Update fairyUpdate = state.runtime.fairyMotion.advance(state.entity, state.frame,
                    state.linkEntityX, state.linkEntityY, state.randomByteSupplier,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()]);
                state.updated = fairyUpdate.entity();
                state.runtime.enemyTransitionCountdown[state.entity.slot()] = fairyUpdate.transitionCountdown();
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_STALFOS_AGGRESSIVE) {
                state.updated = state.runtime.stalfosAggressiveMotion.advance(state.entity, state.frame, state.linkEntityX, state.linkEntityY,
                    state.randomByteSupplier, state.backgroundCollision);
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_STALFOS_EVASIVE) {
                StalfosEvasiveMotion.Update evasiveUpdate = state.runtime.stalfosEvasiveMotion.advance(
                    state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.runtime.actionButtonsHeld,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()], state.randomByteSupplier,
                    state.backgroundCollision, state.runtime.entityMapId);
                state.updated = evasiveUpdate.entity();
                state.runtime.enemyTransitionCountdown[state.entity.slot()] = evasiveUpdate.transitionCountdown();
                if (evasiveUpdate.cloneRequest() != null) {
                    state.runtime.spawnEvasiveClone(state.entity, evasiveUpdate.cloneRequest());
                }
                if (evasiveUpdate.swordPokeRequested()) {
                    state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                        state.entity.slot(), state.entity.type(), 0, false,
                        EntityCombatEvent.SoundChannel.JINGLE, 0x07));
                    state.runtime.transientVfxRequests.add(new TransientVfxRequest(
                        TransientVfxType.SWORD_POKE, state.updated.x(),
                        (state.updated.y() - state.updated.z()) & 0xFF));
                }
                if (evasiveUpdate.unloadRequested()) {
                    state.runtime.disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
                if (state.runtime.spriteHandlers != null) {
                    EntitySpriteDefinition evasiveDefinition =
                        state.runtime.spriteHandlers.forStalfosEvasiveState(
                            state.runtime.stalfosEvasiveMotion.privateState1(state.entity.slot()));
                    state.updated = withDefinition(state.updated, evasiveDefinition, state.updated.spriteVariant());
                }
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_THREE_OF_A_KIND
                && state.threeOfAKindInteractive) {
                state.runtime.enemyHealth[state.entity.slot()] = 0x20;
                ThreeOfAKindMotion.Update cardUpdate = state.runtime.threeOfAKindMotion.advance(
                    state.entity, state.frame, state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()], state.randomByteSupplier,
                    state.backgroundCollision);
                state.updated = cardUpdate.entity();
                state.runtime.enemyTransitionCountdown[state.entity.slot()] = cardUpdate.transitionCountdown();
                state.preserveThreeOfAKindPresentation = true;
            }

            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_LIKE_LIKE) {
                LikeLikeMotion.Update likeLikeUpdate = state.runtime.likeLikeMotion.advance(
                    state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.linkZ,
                    state.projectileLinkState.motionState(), state.runtime.actionButtonsHeld,
                    state.runtime.linkItemA, state.runtime.linkItemB, state.projectileLinkState.shieldLevel(),
                    state.runtime.slowTransitionCountdown[state.entity.slot()], state.randomByteSupplier,
                    state.backgroundCollision);
                state.updated = likeLikeUpdate.entity();
                state.runtime.slowTransitionCountdown[state.entity.slot()] = likeLikeUpdate.slowTransitionCountdown();
                if (likeLikeUpdate.event() != null) {
                    state.runtime.pendingLikeLikeEvents.add(likeLikeUpdate.event());
                    if (likeLikeUpdate.event().kind()
                        == LikeLikeEvent.Kind.RELEASE) {
                        state.runtime.slowTimerInitialized[state.entity.slot()] = true;
                    }
                }
                if (likeLikeUpdate.spriteVariant() >= 0) {
                    state.updated = withVariant(state.updated, likeLikeUpdate.spriteVariant());
                }
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_GOOMBA) {
                GoombaMotion.Update goombaUpdate = state.runtime.goombaMotion.advance(
                    state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.randomByteSupplier,
                    state.backgroundCollision, state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.runtime.groundInteractionSideScrolling);
                state.updated = goombaUpdate.entity();
                state.runtime.enemyTransitionCountdown[state.entity.slot()] = goombaUpdate.transitionCountdown();
                if (goombaUpdate.startDying()) {
                    // func_007_666B writes ENTITY_DROPPABLE_HEART and the
                    // private-countdown-3 death window before setting status
                    // DYING and physics flags to 4. Keep both the source
                    // table and the runtime death countdown synchronized;
                    // the latter drives the shared Java death presentation.
                    state.runtime.droppedItemBySlot[state.entity.slot()] = 0x2D;
                    state.runtime.dropPrivateCountdown3[state.entity.slot()] = 0x0C;
                    state.runtime.dyingCountdown[state.entity.slot()] = 0x0C;
                    state.runtime.enemyPhysicsFlags[state.entity.slot()] = 0x04;
                    state.status = EntityStatus.DYING;
                }
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_SNAKE) {
                SnakeMotion.Update snakeUpdate = state.runtime.snakeMotion.advance(
                    state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.randomByteSupplier,
                    state.backgroundCollision, state.runtime.enemyTransitionCountdown[state.entity.slot()]);
                state.updated = snakeUpdate.entity();
                state.runtime.enemyTransitionCountdown[state.entity.slot()] = snakeUpdate.transitionCountdown();
                state.preserveSnakePresentation = true;
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_WIZROBE) {
                WizrobeMotion.Update wizrobeUpdate = state.runtime.wizrobeMotion.advance(
                    state.entity, state.linkEntityX, state.linkEntityY, state.randomByteSupplier,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.runtime.enemyPhysicsFlags[state.entity.slot()]);
                state.updated = wizrobeUpdate.entity();
                state.runtime.enemyTransitionCountdown[state.entity.slot()] = wizrobeUpdate.transitionCountdown();
                state.runtime.enemyPhysicsFlags[state.entity.slot()] = wizrobeUpdate.physicsFlags();
                state.preserveWizrobePresentation = true;
                if (wizrobeUpdate.projectileSpawn() != null) {
                    state.runtime.spawnWizrobeProjectile(state.updated, wizrobeUpdate.projectileSpawn());
                }
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_PEAHAT) {
                state.updated = state.runtime.peaHatMotion.advance(state.entity, state.frame, state.randomByteSupplier,
                    state.backgroundCollision);
            }
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && state.entity.type() == state.runtime.ENTITY_ARMOS_STATUE) {
                RoomEntityBackgroundInteraction armosBackgroundInteraction = state.runtime.backgroundInteraction;
                if (armosBackgroundInteraction == null && state.backgroundCollision != null) {
                    armosBackgroundInteraction = RoomEntityBackgroundInteraction.fromBoolean(
                        state.backgroundCollision);
                }
                ArmosMotion.Update armosUpdate = state.runtime.armosMotion.advance(
                    state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.randomByteSupplier,
                    state.handlerLinkCollisionEnabled,
                    armosBackgroundInteraction, state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()]);
                state.updated = armosUpdate.entity();
                if (armosUpdate.linkFinalPositionCopyRequested()
                    && state.projectileLinkState.motionState()
                        < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE) {
                    state.runtime.requestLinkPush(state.entity, EntityLinkCollisionRules.RESTORE_ONLY);
                }
                if (armosUpdate.woke()) {
                    // ArmosStatueEntityHandler's wake branch starts the same
                    // $18 visible flash used by the ROM's enemy hit handlers.
                    state.runtime.enemyFlashCountdown[state.entity.slot()] = 0x18;
                }
                if (armosUpdate.activated()) {
                    // State 1 clears the harmless physics bit and enables the
                    // normal hitbox/sword path for state 2.
                    state.runtime.enemyPhysicsFlags[state.entity.slot()] &= 0x7F;
                    state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()] = 0;
                }
            }
        return true;
    }
}
