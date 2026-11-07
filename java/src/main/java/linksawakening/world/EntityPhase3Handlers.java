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

/** Registered ROM type callbacks for ordered slot phase 3. */
final class EntityPhase3Handlers {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(RoomEntityRuntime.ENTITY_IRON_MASK, 44, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
                int privateState2 = state.runtime.ironMaskPrivateState2[state.entity.slot()];
                int variant = privateState2 == 0
                    ? state.updated.spriteVariant() : ((state.frame >>> 4) & 0x01);
                if (state.runtime.spriteHandlers != null) {
                    state.updated = withDefinition(state.updated,
                        state.runtime.spriteHandlers.forIronMaskState(privateState2), variant);
                }
                state.preserveIronMaskPresentation = true;
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_MOBLIN_SWORD, 45, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && !state.runtime.dialogActive && !state.runtime.inventoryAppearing && !state.runtime.roomTransitionActive
                && state.runtime.gameplayWorld && state.runtime.transitionSequenceCounter == 0x04 && !state.runtime.witchGotItemPresentationActive()
                && true) {
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_HIDING_ZOL, 46, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_SPIKE_TRAP, 47, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
                SpikeTrapMotion.Update spikeTrapUpdate = state.runtime.spikeTrapMotion.advance(
                    state.entity, state.linkEntityX, state.linkEntityY, state.backgroundCollision);
                state.updated = spikeTrapUpdate.entity();
                if (spikeTrapUpdate.soundChannel() != EntityCombatEvent.SoundChannel.NONE) {
                    state.runtime.pendingEntityEvents.add(new EntityCombatEvent(
                        state.entity.slot(), state.entity.type(), 0, false,
                        spikeTrapUpdate.soundChannel(), spikeTrapUpdate.soundId()));
                }
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_PAIRODD, 48, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_STAR, 49, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true && state.handlerLinkCollisionEnabled) {
                RoomEntityBackgroundInteraction starBackgroundInteraction = state.runtime.backgroundInteraction;
                if (starBackgroundInteraction == null && state.backgroundCollision != null) {
                    starBackgroundInteraction = RoomEntityBackgroundInteraction.fromBoolean(
                        state.backgroundCollision);
                }
                state.updated = state.runtime.starMotion.advance(state.entity, state.frame, state.randomByteSupplier,
                    starBackgroundInteraction, state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()]);
                state.preserveStarPresentation = true;
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_BLOOPER, 50, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true && state.handlerLinkCollisionEnabled) {
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_PINCER, 51, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true && state.handlerLinkCollisionEnabled) {
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_WINGED_OCTOROK, 52, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true && state.handlerLinkCollisionEnabled) {
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_BUSH_CRAWLER, 53, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true && state.handlerLinkCollisionEnabled) {
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_PAIRODD_PROJECTILE, 54, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_WIZROBE_PROJECTILE, 55, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_GOPONGA_FLOWER_PROJECTILE, 56, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_WATER_TEKTITE, 57, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
                state.updated = state.runtime.waterTektiteMotion.advance(state.entity, state.frame, state.randomByteSupplier,
                    state.backgroundCollision);
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_FISH, 58, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_CROW, 59, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_CUCCO, 60, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true && state.handlerLinkCollisionEnabled) {
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_BOO_BUDDY, 61, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_DROPPABLE_FAIRY, 62, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
                FairyMotion.Update fairyUpdate = state.runtime.fairyMotion.advance(state.entity, state.frame,
                    state.linkEntityX, state.linkEntityY, state.randomByteSupplier,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()]);
                state.updated = fairyUpdate.entity();
                state.runtime.enemyTransitionCountdown[state.entity.slot()] = fairyUpdate.transitionCountdown();
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_STALFOS_AGGRESSIVE, 63, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
                state.updated = state.runtime.stalfosAggressiveMotion.advance(state.entity, state.frame, state.linkEntityX, state.linkEntityY,
                    state.randomByteSupplier, state.backgroundCollision);
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_STALFOS_EVASIVE, 64, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_THREE_OF_A_KIND, 65, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_LIKE_LIKE, 66, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_GOOMBA, 67, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_SNAKE, 68, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
                SnakeMotion.Update snakeUpdate = state.runtime.snakeMotion.advance(
                    state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.randomByteSupplier,
                    state.backgroundCollision, state.runtime.enemyTransitionCountdown[state.entity.slot()]);
                state.updated = snakeUpdate.entity();
                state.runtime.enemyTransitionCountdown[state.entity.slot()] = snakeUpdate.transitionCountdown();
                state.preserveSnakePresentation = true;
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_WIZROBE, 69, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
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
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_PEAHAT, 70, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
                state.updated = state.runtime.peaHatMotion.advance(state.entity, state.frame, state.randomByteSupplier,
                    state.backgroundCollision);
            }
            return true;
        });
        registry.registerTick(RoomEntityRuntime.ENTITY_ARMOS_STATUE, 71, state -> {
if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && true) {
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
        });
    }
}
