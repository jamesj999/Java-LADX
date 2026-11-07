package linksawakening.world;

import static linksawakening.world.EntityMotionMath.*;
import static linksawakening.world.EntitySnapshots.*;
import static linksawakening.world.EntityTypeRules.*;
import static linksawakening.world.RoomEntityRuntime.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.vfx.TransientVfxType;

/** Registered ROM type callbacks for ordered slot phase 4. */
final class EntityPhase4Handlers {
  static void install(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_GENIE,
        EntityTickStage.PHASE_4_GENIE,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing && true) {
            BossIntroMotion.Update bossIntro =
                !state.runtime.inventoryAppearing && !state.runtime.roomTransitionActive
                    ? state.runtime.bossIntroMotion.advance(
                        state.runtime.options1(state.entity.slot()),
                        state.entity.type(),
                        state.runtime.entityMapId,
                        state.runtime.transitionSequenceCounter)
                    : BossIntroMotion.Update.none();
            if (bossIntro.musicTrack() >= 0) {
              state.runtime.pendingMusicTrack = bossIntro.musicTrack();
            }
            if (bossIntro.dialogLowId() >= 0) {
              state.runtime.pendingDialogRequests.add(
                  new DialogRequest(bossIntro.dialogTableId(), bossIntro.dialogLowId()));
            }
            boolean genieThresholdReached =
                state.runtime.geniePrivateState4[state.entity.slot()]
                    >= GenieMotion.JAR_HEALTH_THRESHOLD;
            if (state.runtime.geniePrivateState1[state.entity.slot()] == 0
                && !genieThresholdReached) {
              state.runtime.genieControllerSlot = state.entity.slot();
            }
            if (state.runtime.geniePrivateState1[state.entity.slot()] == 0
                && (genieThresholdReached
                    || state.runtime.roomInspection.genieState0Interactive(
                        state.creditsGameplay))) {
              state.runtime.genie.advanceGenieJar(state.entity, state.linkEntityY);
              if (!state.runtime.slots[state.entity.slot()].loaded()) {
                return false;
              }
              state.updated = state.runtime.slots[state.entity.slot()];
            }
            if (state.runtime.geniePrivateState1[state.entity.slot()] == 2
                && state.runtime.roomInspection.genieState0Interactive(state.creditsGameplay)) {
              state.runtime.genie.advanceGenieAppearance(state.entity);
              if (!state.runtime.slots[state.entity.slot()].loaded()) {
                return false;
              }
              state.updated = state.runtime.slots[state.entity.slot()];
            }
            if (state.runtime.geniePrivateState1[state.entity.slot()] == 3
                && state.runtime.roomInspection.genieState0Interactive(state.creditsGameplay)) {
              state.runtime.genie.advanceGenieSideProjectile(state.entity, state.frameCounter);
              if (!state.runtime.slots[state.entity.slot()].loaded()) {
                return false;
              }
              state.updated = state.runtime.slots[state.entity.slot()];
            }
            if (state.runtime.geniePrivateState1[state.entity.slot()] == 4
                && state.runtime.roomInspection.genieState0Interactive(state.creditsGameplay)) {
              if (state.handlerLinkCollisionEnabled
                  && RoomEntityCombatRules.overlapsLink(
                      state.entity, state.linkEntityX, state.linkEntityY)) {
                state.runtime.backgroundMechanics.requestLinkPush(
                    state.entity, EntityLinkCollisionRules.STANDARD_PUSH);
              }
              state.runtime.genie.advanceGenieFireball(state.entity, state.frameCounter);
              if (!state.runtime.slots[state.entity.slot()].loaded()) {
                return false;
              }
              state.updated = state.runtime.slots[state.entity.slot()];
            }
            if (state.runtime.geniePrivateState1[state.entity.slot()] == 1
                && state.runtime.roomInspection.genieState0Interactive(state.creditsGameplay)) {
              state.runtime.genieSharedBodySpeedX =
                  state.runtime.genieSpeedX[state.entity.slot()] & 0xFF;
              state.runtime.genieSharedBodySpeedY =
                  state.runtime.genieSpeedY[state.entity.slot()] & 0xFF;
              if (state.runtime.genieActiveState[state.entity.slot()] != 5
                  && state.handlerLinkCollisionEnabled
                  && RoomEntityCombatRules.overlapsLink(
                      state.entity, state.linkEntityX, state.linkEntityY)) {
                state.runtime.backgroundMechanics.requestLinkPush(
                    state.entity, EntityLinkCollisionRules.STANDARD_PUSH);
              }
              state.runtime.genie.advanceGenieBody(
                  state.entity,
                  state.frameCounter,
                  state.linkEntityX,
                  state.linkEntityY,
                  state.randomByteSupplier);
              if (!state.runtime.slots[state.entity.slot()].loaded()) {
                return false;
              }
              state.updated = state.runtime.slots[state.entity.slot()];
            }
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_ARMOS_KNIGHT,
        EntityTickStage.PHASE_4_ARMOS_KNIGHT,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing && true) {
            BossIntroMotion.Update bossIntro =
                state.runtime.bossIntroMotion.advance(
                    state.runtime.options1(state.entity.slot()),
                    state.entity.type(),
                    state.runtime.entityMapId,
                    state.runtime.transitionSequenceCounter);
            if (bossIntro.musicTrack() >= 0) {
              state.runtime.pendingMusicTrack = bossIntro.musicTrack();
            }
            if (bossIntro.dialogLowId() >= 0) {
              state.runtime.pendingDialogRequests.add(
                  new DialogRequest(bossIntro.dialogTableId(), bossIntro.dialogLowId()));
            }
            state.runtime.backgroundMechanics.requestArmosLinkPush(
                state.entity,
                state.linkEntityX,
                state.linkEntityY,
                state.projectileLinkState.motionState());
            ArmosKnightMotion.Update armosKnightUpdate =
                state.runtime.armosKnightMotion.advance(
                    state.entity,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.linkZ,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.runtime.enemyPhysicsFlags[state.entity.slot()],
                    state.runtime.enemyHitboxFlags[state.entity.slot()],
                    state.runtime.options1(state.entity.slot()),
                    state.runtime.enemyHealth[state.entity.slot()]);
            state.updated = armosKnightUpdate.entity();
            state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                armosKnightUpdate.transitionCountdown();
            state.runtime.enemyPhysicsFlags[state.entity.slot()] = armosKnightUpdate.physicsFlags();
            state.runtime.enemyHitboxFlags[state.entity.slot()] = armosKnightUpdate.hitboxFlags();
            state.runtime.entityOptions1Override[state.entity.slot()] =
                armosKnightUpdate.options1();
            state.preserveArmosKnightPresentation = true;
            if (armosKnightUpdate.linkMotionBlocked()) {
              state.runtime.pendingLinkMotionBlockRequests.add(
                  new LinkMotionBlockRequest(state.entity.slot()));
              state.runtime.pendingLinkFallenPoseRequests.add(
                  new LinkFallenPoseRequest(state.entity.slot()));
            }
            if (armosKnightUpdate.screenShakeRequest() != null) {
              ArmosKnightMotion.ScreenShakeRequest shake = armosKnightUpdate.screenShakeRequest();
              state.runtime.pendingScreenShakeRequests.add(
                  new ScreenShakeRequest(state.entity.slot(), shake.countdown(), shake.phase()));
            }
            if (armosKnightUpdate.rubbleRequest() != null) {
              ArmosKnightMotion.RubbleRequest rubble = armosKnightUpdate.rubbleRequest();
              int rubbleSlot = state.runtime.spawnLiftableRockRubble(rubble.x(), rubble.y());
              if (rubbleSlot >= 0) {
                state.runtime.transientVfxRequests.add(
                    new TransientVfxRequest(TransientVfxType.POOF, rubble.x(), rubble.y()));
                state.runtime.pendingEntityEvents.add(
                    new EntityCombatEvent(
                        rubbleSlot,
                        state.runtime.ENTITY_LIFTABLE_ROCK,
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.NOISE,
                        0x29));
              }
            }
            if (armosKnightUpdate.jingleId() >= 0) {
              state.runtime.pendingEntityEvents.add(
                  new EntityCombatEvent(
                      state.entity.slot(),
                      state.entity.type(),
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.JINGLE,
                      armosKnightUpdate.jingleId()));
            }
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_HINOX,
        EntityTickStage.PHASE_4_HINOX,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing
              && state.handlerLinkCollisionEnabled
              && state.runtime.backgroundMechanics.entityTimersInteractive()
              && state.runtime.transitionSequenceCounter == 0x04
              && !state.creditsGameplay
              && true) {
            int slot = state.entity.slot();
            BossIntroMotion.Update bossIntro =
                state.runtime.bossIntroMotion.advance(
                    state.runtime.options1(state.entity.slot()),
                    state.entity.type(),
                    state.runtime.entityMapId,
                    state.runtime.transitionSequenceCounter);
            if (bossIntro.musicTrack() >= 0) {
              state.runtime.pendingMusicTrack = bossIntro.musicTrack();
            }
            if (bossIntro.dialogLowId() >= 0) {
              state.runtime.pendingDialogRequests.add(
                  new DialogRequest(bossIntro.dialogTableId(), bossIntro.dialogLowId()));
            }
            HinoxMotion.Update hinoxUpdate =
                state.runtime.hinoxMotion.advance(
                    state.entity,
                    state.runtime.enemyTransitionCountdown[slot],
                    state.frame,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.linkZ,
                    state.projectileLinkState.motionState(),
                    state.randomByteSupplier,
                    state.hinoxBackgroundInteraction,
                    state.runtime.hinoxMotion.collisionFlags(slot),
                    state.runtime.enemyFlashCountdown[slot]);
            state.updated = hinoxUpdate.entity();
            state.runtime.enemyTransitionCountdown[slot] = hinoxUpdate.transitionCountdown();
            state.preserveHinoxPresentation = true;
            if (hinoxUpdate.linkMotionBlocked()) {
              state.runtime.pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
              state.runtime.pendingLinkFallenPoseRequests.add(new LinkFallenPoseRequest(slot));
            }
            if (hinoxUpdate.heldLinkPositionWritten()) {
              state.runtime.pendingHinoxLinkEffectRequests.add(
                  new HinoxLinkEffectRequest(
                      slot,
                      hinoxUpdate.heldLinkX(),
                      hinoxUpdate.heldLinkY(),
                      hinoxUpdate.heldLinkZ(),
                      hinoxUpdate.linkSpeedX(),
                      hinoxUpdate.linkSpeedY(),
                      hinoxUpdate.linkVelocityZ(),
                      hinoxUpdate.linkAirborneState(),
                      hinoxUpdate.linkDamage(),
                      hinoxUpdate.linkMotionBlocked(),
                      hinoxUpdate.applyHeldLinkPose()));
            }
            if (hinoxUpdate.bombSpawn() != null) {
              HinoxMotion.BombSpawn spawn = hinoxUpdate.bombSpawn();
              state.runtime.spawnEnemyBomb(
                  spawn.x(),
                  spawn.y(),
                  spawn.z(),
                  spawn.transitionCountdown(),
                  spawn.speedX(),
                  spawn.speedY(),
                  spawn.speedZ());
            }
            if (!hinoxUpdate.heldLinkPositionWritten()
                && !hinoxUpdate.linkMotionBlocked()
                && (hinoxUpdate.linkSpeedX() != 0
                    || hinoxUpdate.linkSpeedY() != 0
                    || hinoxUpdate.linkVelocityZ() != 0
                    || hinoxUpdate.linkDamage() != 0)) {
              state.runtime.pendingHinoxLinkEffectRequests.add(
                  new HinoxLinkEffectRequest(
                      slot,
                      hinoxUpdate.heldLinkX(),
                      hinoxUpdate.heldLinkY(),
                      hinoxUpdate.heldLinkZ(),
                      hinoxUpdate.linkSpeedX(),
                      hinoxUpdate.linkSpeedY(),
                      hinoxUpdate.linkVelocityZ(),
                      hinoxUpdate.linkAirborneState(),
                      hinoxUpdate.linkDamage(),
                      hinoxUpdate.linkMotionBlocked(),
                      hinoxUpdate.applyHeldLinkPose()));
            }
            if (hinoxUpdate.dustRequested()) {
              state.runtime.transientVfxRequests.add(
                  new TransientVfxRequest(
                      TransientVfxType.PEGASUS_DUST,
                      state.updated.x(),
                      (state.updated.y() + 0x0A) & 0xFF));
            }
            if (hinoxUpdate.jingleId() >= 0) {
              state.runtime.pendingEntityEvents.add(
                  new EntityCombatEvent(
                      slot,
                      state.entity.type(),
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.JINGLE,
                      hinoxUpdate.jingleId()));
            }
            if (hinoxUpdate.waveId() >= 0) {
              state.runtime.pendingEntityEvents.add(
                  new EntityCombatEvent(
                      slot,
                      state.entity.type(),
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.WAVE,
                      hinoxUpdate.waveId()));
            }
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_ROLLING_BONES,
        EntityTickStage.PHASE_4_ROLLING_BONES,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing && true) {
            RoomEntity bar =
                state.runtime.roomInspection.firstLoadedEntityOfType(
                    state.runtime.ENTITY_ROLLING_BONES_BAR);
            if (bar != null) {
              RollingBonesMotion.BossUpdate bossUpdate =
                  state.runtime.rollingBonesMotion.advanceBoss(
                      state.entity,
                      bar,
                      state.runtime.enemyTransitionCountdown[state.entity.slot()],
                      state.runtime.enemyHealth[state.entity.slot()],
                      state.backgroundCollision);
              state.updated = bossUpdate.entity();
              state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                  bossUpdate.transitionCountdown();
              state.preserveRollingBonesPresentation = true;
              if (bossUpdate.jingleId() >= 0) {
                state.runtime.pendingEntityEvents.add(
                    new EntityCombatEvent(
                        state.entity.slot(),
                        state.entity.type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        bossUpdate.jingleId()));
              }
            }
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_ROLLING_BONES_BAR,
        EntityTickStage.PHASE_4_ROLLING_BONES_BAR,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing && true) {
            if ((state.runtime.entityRoomStatus & 0x20) != 0) {
              state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
              return false;
            }
            RollingBonesMotion.BarUpdate barUpdate =
                state.runtime.rollingBonesMotion.advanceBar(
                    state.entity, state.frame, state.backgroundCollision);
            state.updated = barUpdate.entity();
            if (state.runtime.spriteHandlers != null) {
              state.updated =
                  withDefinition(
                      state.updated,
                      state.runtime.spriteHandlers.forRollingBonesBar(
                          0, state.updated.y(), barUpdate.animationVariant()),
                      0);
            }
            state.preserveRollingBonesPresentation = true;
            if (barUpdate.rollingSound()) {
              state.runtime.pendingEntityEvents.add(
                  new EntityCombatEvent(
                      state.entity.slot(),
                      state.entity.type(),
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.WAVE,
                      0x1A));
            }
            if (barUpdate.strongBump()) {
              state.runtime.pendingEntityEvents.add(
                  new EntityCombatEvent(
                      state.entity.slot(),
                      state.entity.type(),
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.JINGLE,
                      0x0B));
              state.runtime.pendingScreenShakeRequests.add(
                  new ScreenShakeRequest(state.entity.slot(), barUpdate.screenShakeCountdown(), 0));
            }
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MOBLIN_KING,
        EntityTickStage.PHASE_4_MOBLIN_KING,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing && true) {
            if (state.runtime.bowWowState != 0x80) {
              state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
              return false;
            }
            if (!state.handlerInteractive) {
              state.preserveMoblinKingPresentation = true;
            } else {
              // ApplyRecoilIfNeeded_15 pops the handler return address:
              // movement/Z physics and AI wait until the recoil ends.
              if (state.runtime.enemyRecoilMotion.isActive(state.entity.slot())
                  && state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()] != 0) {
                state.runtime.slots[state.index] =
                    state
                        .runtime
                        .backgroundMechanics
                        .applyEnemyRecoilIfNeeded(state.entity, state.backgroundCollision)
                        .entity();
                return false;
              }
              MoblinKingMotion.Update kingUpdate =
                  state.runtime.moblinKingMotion.advance(
                      state.entity,
                      state.linkEntityX,
                      state.linkEntityY,
                      state.runtime.enemyTransitionCountdown[state.entity.slot()],
                      state.runtime.slowTransitionCountdown[state.entity.slot()],
                      state.frame,
                      state.randomByteSupplier,
                      state.backgroundCollision,
                      state.handlerLinkCollisionEnabled
                          && RoomEntityCombatRules.collisionCadenceMatches(
                              state.frame, state.entity.slot())
                          && RoomEntityCombatRules.overlapsLink(
                              state.entity, state.linkEntityX, state.linkEntityY));
              state.updated = kingUpdate.entity();
              state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                  kingUpdate.transitionCountdown();
              state.runtime.slowTransitionCountdown[state.entity.slot()] =
                  kingUpdate.slowTransitionCountdown();
              state.runtime.slowTimerInitialized[state.entity.slot()] = true;
              state.runtime.entityOptions1Override[state.entity.slot()] =
                  state.runtime.moblinKingMotion.state(state.entity.slot()) == 5
                      ? 0x84
                      : state.runtime.moblinKingMotion.state(state.entity.slot()) >= 2
                          ? 0xC4
                          : 0x40;
              if (state.runtime.moblinKingMotion.state(state.entity.slot()) == 5) {
                state.runtime.enemyPhysicsFlags[state.entity.slot()] |=
                    state.runtime.ENTITY_PHYSICS_HARMLESS;
              } else {
                state.runtime.enemyPhysicsFlags[state.entity.slot()] &=
                    ~state.runtime.ENTITY_PHYSICS_HARMLESS;
              }
              if (state.runtime.spriteHandlers != null) {
                state.updated =
                    withDefinition(
                        state.updated,
                        state.runtime.spriteHandlers.forMoblinKingState(
                            state.runtime.moblinKingMotion.bodyVariant(state.entity.slot()),
                            state.runtime.moblinKingMotion.weaponVariant(state.entity.slot()),
                            state.updated.z() != 0),
                        0);
              }
              state.preserveMoblinKingPresentation = true;
              if (kingUpdate.openIntroDialog()) {
                // $15:475C clears DIALOG_BOX_BOTTOM_BIT after opening Dialog191.
                state.runtime.pendingDialogRequests.add(new DialogRequest(1, 0x91, true));
              }
              if (kingUpdate.arrowRequest() != null) {
                state.runtime.projectileLaunchRequests.add(
                    new RoamingEnemyMotion.LaunchRequest(
                        state.entity.slot(),
                        state.entity.type(),
                        state.runtime.ENTITY_MOBLIN_ARROW));
                state.runtime.entityEnemySpawns.spawnEnemyProjectile(
                    state.entity,
                    new RoamingEnemyMotion.LaunchRequest(
                        state.entity.slot(),
                        state.entity.type(),
                        state.runtime.ENTITY_MOBLIN_ARROW),
                    kingUpdate.arrowRequest().direction());
              }
              if (kingUpdate.jingleId() >= 0) {
                state.runtime.pendingEntityEvents.add(
                    new EntityCombatEvent(
                        state.entity.slot(),
                        state.entity.type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        kingUpdate.jingleId()));
              }
              if (kingUpdate.screenShakeCountdown() != 0) {
                state.runtime.pendingScreenShakeRequests.add(
                    new ScreenShakeRequest(
                        state.entity.slot(), kingUpdate.screenShakeCountdown(), 0));
              }
            }
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_GOPONGA_FLOWER,
        EntityTickStage.PHASE_4_GOPONGA_FLOWER,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing && true) {
            if (GopongaFlowerMotion.overlapsInteractiveLink(
                state.entity,
                state.linkEntityX,
                state.linkEntityY,
                state.handlerLinkCollisionEnabled)) {
              state.runtime.backgroundMechanics.requestLinkPush(
                  state.entity, EntityLinkCollisionRules.STANDARD_PUSH);
            }
            state.updated =
                withVariant(state.updated, GopongaFlowerMotion.frameVariant(state.frame));
            state.preserveGopongaFlowerPresentation = true;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_GIANT_GOPONGA_FLOWER,
        EntityTickStage.PHASE_4_GIANT_GOPONGA_FLOWER,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing && true) {
            if (GiantGopongaMotion.overlapsInteractiveLink(
                state.entity,
                state.linkEntityX,
                state.linkEntityY,
                state.handlerLinkCollisionEnabled)) {
              state.runtime.backgroundMechanics.requestLinkPush(
                  state.entity, EntityLinkCollisionRules.STANDARD_PUSH);
            }
            GiantGopongaMotion.Update giantUpdate =
                state.runtime.giantGopongaMotion.advance(
                    state.entity,
                    state.frame,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.linkEntityX,
                    state.linkEntityY);
            state.updated = giantUpdate.entity();
            state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                giantUpdate.transitionCountdown();
            if (giantUpdate.projectileSpawn() != null) {
              state.runtime.entityEnemySpawns.spawnGopongaProjectile(
                  state.entity, giantUpdate.projectileSpawn());
            }
            state.preserveGiantGopongaPresentation = true;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_POKEY,
        EntityTickStage.PHASE_4_POKEY,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing && true) {
            int slot = state.entity.slot();
            if (!state.handlerLinkCollisionEnabled) {
              // Both Pokey branches return immediately after their
              // pre-handler display pass when Link is non-interactive.
            } else if (state.runtime.pokeyMotion.isSegment(slot)) {
              PokeyMotion.Update segmentUpdate =
                  state.runtime.pokeyMotion.advanceSegment(
                      state.updated,
                      state.runtime.entityInertia[slot],
                      state.runtime.enemyTransitionCountdown[slot],
                      state.backgroundCollision);
              state.updated = segmentUpdate.entity();
              state.runtime.entityInertia[slot] = segmentUpdate.inertia();
              state.runtime.enemyTransitionCountdown[slot] = segmentUpdate.transitionCountdown();
              if (segmentUpdate.unloadRequested()) {
                state.runtime.transientVfxRequests.add(
                    new TransientVfxRequest(
                        TransientVfxType.POOF,
                        state.updated.x(),
                        (state.updated.y() - state.updated.z()) & 0xFF));
                state.runtime.pendingEntityEvents.add(
                    new EntityCombatEvent(
                        slot,
                        state.entity.type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        0x2F));
                state.runtime.lifecycle.disableEntityWithoutPersistence(slot);
                return false;
              }
              if (segmentUpdate.bumpJingle()) {
                state.runtime.pendingEntityEvents.add(
                    new EntityCombatEvent(
                        slot,
                        state.entity.type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        0x09));
              }
            } else {
              if (state.runtime.entityInertia[slot] < 0x02) {
                // PokeyEntityHandler replaces the normal group-$00
                // health with two hit points until its body is fully
                // separated.
                state.runtime.enemyHealth[slot] = 0x02;
              }
              PokeyMotion.Update pokeyUpdate =
                  state.runtime.pokeyMotion.advanceMain(
                      state.updated,
                      state.frame,
                      state.runtime.entityInertia[slot],
                      state.runtime.enemyTransitionCountdown[slot],
                      state.runtime.enemyFlashCountdown[slot],
                      state.linkEntityX,
                      state.linkEntityY,
                      state.randomByteSupplier,
                      state.backgroundCollision);
              state.updated = pokeyUpdate.entity();
              state.runtime.entityInertia[slot] = pokeyUpdate.inertia();
              state.runtime.enemyTransitionCountdown[slot] = pokeyUpdate.transitionCountdown();
              state.runtime.enemyFlashCountdown[slot] = pokeyUpdate.flashCountdown();
              if (pokeyUpdate.segmentSpawn() != null) {
                state.runtime.entityEnemySpawns.spawnPokeySegment(pokeyUpdate.segmentSpawn());
              }
              if (state.runtime.spriteHandlers != null) {
                state.updated =
                    withDefinition(
                        state.updated,
                        state.runtime.spriteHandlers.forPokeyState(pokeyUpdate.renderInertia()),
                        state.updated.spriteVariant());
              }
            }
            state.preservePokeyPresentation = true;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_PIRANHA_PLANT,
        EntityTickStage.PHASE_4_PIRANHA_PLANT,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing
              && true
              && !state.creditsGameplay
              && state.runtime.transitionSequenceCounter == 0x04
              && state.handlerLinkCollisionEnabled) {
            int slot = state.entity.slot();
            PiranhaMotion.Update piranhaUpdate =
                state.runtime.piranhaMotion.advance(
                    state.updated,
                    state.runtime.enemyTransitionCountdown[slot],
                    state.linkEntityX,
                    state.linkEntityY);
            state.updated = piranhaUpdate.entity();
            state.runtime.enemyTransitionCountdown[slot] = piranhaUpdate.transitionCountdown();
            state.preservePiranhaPresentation = true;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_ZORA,
        EntityTickStage.PHASE_4_ZORA,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing
              && true
              && !state.creditsGameplay
              && state.handlerLinkCollisionEnabled) {
            int slot = state.entity.slot();
            if (RoomEntityCombatRules.overlapsLink(
                state.entity, state.linkEntityX, state.linkEntityY)) {
              // Zora writes ENTITY_PHYSICS_HARMLESS immediately before
              // this collision check, then copies hLinkFinalPosition
              // and calls ResetPegasusBoots. It does not use the common
              // hookshot-chain reset helper.
              state.runtime.backgroundMechanics.requestLinkPush(
                  state.entity, EntityLinkCollisionRules.ZORA);
            }
            ZoraMotion.Update zoraUpdate =
                state.runtime.zoraMotion.advance(
                    state.updated,
                    state.runtime.enemyTransitionCountdown[slot],
                    state.runtime.enemyPhysicsFlags[slot],
                    state.linkEntityX,
                    state.linkEntityY,
                    state.randomByteSupplier,
                    state.runtime.objectQuery);
            state.updated = zoraUpdate.entity();
            state.runtime.enemyTransitionCountdown[slot] = zoraUpdate.transitionCountdown();
            state.runtime.enemyPhysicsFlags[slot] = zoraUpdate.physicsFlags();
            if (zoraUpdate.projectileSpawn() != null) {
              state.runtime.entityEnemySpawns.spawnZoraProjectile(zoraUpdate.projectileSpawn());
            }
            if (zoraUpdate.splash()) {
              state.runtime.transientVfxRequests.add(
                  new TransientVfxRequest(
                      TransientVfxType.WATER_SPLASH,
                      state.updated.x(),
                      (state.updated.y() - state.updated.z()) & 0xFF));
              state.runtime.pendingEntityEvents.add(
                  new EntityCombatEvent(
                      slot,
                      state.entity.type(),
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.JINGLE,
                      0x0E));
            }
            state.preserveZoraPresentation = true;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_ZOMBIE,
        EntityTickStage.PHASE_4_ZOMBIE,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing
              && true
              && !state.creditsGameplay
              && state.handlerLinkCollisionEnabled) {
            int slot = state.entity.slot();
            RoomEntityBackgroundInteraction zombieBackgroundInteraction =
                state.runtime.backgroundInteraction;
            if (zombieBackgroundInteraction == null && state.backgroundCollision != null) {
              zombieBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            ZombieMotion.Update zombieUpdate =
                state.runtime.zombieMotion.advance(
                    state.updated,
                    state.frame,
                    state.runtime.enemyTransitionCountdown[slot],
                    state.runtime.enemyPhysicsFlags[slot],
                    state.linkEntityX,
                    state.linkEntityY,
                    state.randomByteSupplier,
                    state.runtime.objectQuery,
                    zombieBackgroundInteraction,
                    state.runtime.enemyIgnoreHitsCountdown[slot]);
            state.updated = zombieUpdate.entity();
            state.runtime.enemyTransitionCountdown[slot] = zombieUpdate.transitionCountdown();
            state.runtime.enemyPhysicsFlags[slot] = zombieUpdate.physicsFlags();
            state.applyGenericGroundInteraction = zombieUpdate.appliesBackgroundInteraction();
            if (zombieUpdate.spawnRequest() != null) {
              state.runtime.entityEnemySpawns.spawnZombieChild(zombieUpdate.spawnRequest());
            }
            if (zombieUpdate.unloadRequested()) {
              state.runtime.clearEntity(slot);
              return false;
            }
            state.preserveZombiePresentation = true;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_BUZZ_BLOB,
        EntityTickStage.PHASE_4_BUZZ_BLOB,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing
              && true
              && !state.creditsGameplay
              && state.handlerLinkCollisionEnabled) {
            int slot = state.entity.slot();
            RoomEntityBackgroundInteraction buzzBlobBackgroundInteraction =
                state.runtime.backgroundInteraction;
            if (buzzBlobBackgroundInteraction == null && state.backgroundCollision != null) {
              buzzBlobBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            BuzzBlobMotion.Update buzzBlobUpdate =
                state.runtime.buzzBlobMotion.advance(
                    state.updated,
                    state.frame,
                    state.runtime.enemyTransitionCountdown[slot],
                    state.randomByteSupplier,
                    buzzBlobBackgroundInteraction);
            state.updated = buzzBlobUpdate.entity();
            state.runtime.enemyTransitionCountdown[slot] = buzzBlobUpdate.transitionCountdown();
            state.applyGenericGroundInteraction = buzzBlobUpdate.appliesBackgroundInteraction();
            state.preserveBuzzBlobPresentation = true;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_SAND_CRAB,
        EntityTickStage.PHASE_4_SAND_CRAB,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing
              && true
              && !state.creditsGameplay
              && state.handlerLinkCollisionEnabled) {
            int slot = state.entity.slot();
            RoomEntityBackgroundInteraction sandCrabBackgroundInteraction =
                state.runtime.backgroundInteraction;
            if (sandCrabBackgroundInteraction == null && state.backgroundCollision != null) {
              sandCrabBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            SandCrabMotion.Update sandCrabUpdate =
                state.runtime.sandCrabMotion.advance(
                    state.updated,
                    state.frame,
                    state.runtime.enemyTransitionCountdown[slot],
                    state.randomByteSupplier,
                    sandCrabBackgroundInteraction);
            state.updated = sandCrabUpdate.entity();
            state.runtime.enemyTransitionCountdown[slot] = sandCrabUpdate.transitionCountdown();
            state.applyGenericGroundInteraction = sandCrabUpdate.appliesBackgroundInteraction();
            state.preserveSandCrabPresentation = true;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_DOG,
        EntityTickStage.PHASE_4_DOG,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing
              && true
              && !state.creditsGameplay
              && state.handlerLinkCollisionEnabled) {
            int slot = state.entity.slot();
            RoomEntityBackgroundInteraction dogBackgroundInteraction =
                state.runtime.backgroundInteraction;
            if (dogBackgroundInteraction == null && state.backgroundCollision != null) {
              dogBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            DogMotion.Update dogUpdate =
                state.runtime.dogMotion.advance(
                    state.updated,
                    state.frame,
                    state.runtime.enemyTransitionCountdown[slot],
                    state.runtime.enemyPhysicsFlags[slot],
                    state.linkEntityX,
                    state.linkEntityY,
                    state.romLinkDirection,
                    state.runtime.actionButtonAHeld,
                    state.runtime.dialogActive,
                    state.randomByteSupplier,
                    dogBackgroundInteraction);
            state.updated = dogUpdate.entity();
            state.runtime.enemyTransitionCountdown[slot] = dogUpdate.transitionCountdown();
            state.runtime.enemyPhysicsFlags[slot] = dogUpdate.physicsFlags();
            state.applyGenericGroundInteraction = dogUpdate.appliesBackgroundInteraction();
            if (dogUpdate.dialogRequested()) {
              state.runtime.pendingDialogRequests.add(
                  new DialogRequest(0, dogUpdate.dialogLowId()));
            }
            state.preserveDogPresentation = true;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_WITCH,
        EntityTickStage.PHASE_4_WITCH,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing && true) {
            state.runtime.story.advanceWitchGotItemPresentation(state.entity.slot());
            WitchMotion.Update witchUpdate =
                state.runtime.witchMotion.advance(
                    state.updated,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    new WitchMotion.Input(
                        state.runtime.playerHasToadstool,
                        state.runtime.linkItemB,
                        state.runtime.linkItemA,
                        state.runtime.actionButtonAHeld,
                        state.runtime.actionButtonBHeld,
                        state.runtime.dialogActive,
                        state.linkZ != 0,
                        state.linkEntityX,
                        state.linkEntityY,
                        state.romLinkDirection,
                        state.runtime.transitionSequenceCounter,
                        state.runtime.booBuddyTriggerCount,
                        state.runtime.bgPaletteEffectAddress,
                        state.runtime.paletteDataFlags,
                        state.runtime.defaultMusicTrack));
            state.updated = witchUpdate.entity();
            state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                witchUpdate.transitionCountdown();
            state.preserveWitchPresentation = true;
            if (witchUpdate.blockLink()) {
              state.runtime.pendingLinkMotionBlockRequests.add(
                  new LinkMotionBlockRequest(state.entity.slot()));
            }
            if (witchUpdate.dialogGlobalId() >= 0) {
              state.runtime.pendingDialogRequests.add(
                  new DialogRequest(
                      witchUpdate.dialogGlobalId() >>> 8, witchUpdate.dialogGlobalId() & 0xFF));
            }
            if (witchUpdate.musicTrack() >= 0) {
              state.runtime.pendingMusicTrack = witchUpdate.musicTrack();
            }
            if (witchUpdate.jingleId() >= 0) {
              state.runtime.pendingEntityEvents.add(
                  new EntityCombatEvent(
                      state.entity.slot(),
                      state.entity.type(),
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.JINGLE,
                      witchUpdate.jingleId()));
            }
            if (witchUpdate.exchangeStarted()) {
              int inventorySlot = witchUpdate.clearedSlot() == WitchMotion.InventorySlot.B ? 0 : 1;
              state.runtime.pendingWitchExchangeEvents.add(
                  new WitchExchangeEvent(state.entity.slot(), inventorySlot));
              state.runtime.playerHasToadstool = false;
            }
            if (witchUpdate.grantMagicPowder()) {
              state.runtime.pendingWitchRewardEvents.add(new WitchRewardEvent(state.entity.slot()));
              state.runtime.witchGotItemCountdown[state.entity.slot()] = 0x2A;
              state.runtime.presentWitchGotItem(state.entity.slot());
            }
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_URCHIN,
        EntityTickStage.PHASE_4_URCHIN,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing
              && true
              && state.handlerLinkCollisionEnabled) {
            int slot = state.entity.slot();
            RoomEntityBackgroundInteraction urchinBackgroundInteraction =
                state.runtime.backgroundInteraction;
            if (urchinBackgroundInteraction == null && state.backgroundCollision != null) {
              urchinBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            UrchinMotion.Update urchinUpdate =
                state.runtime.urchinMotion.advance(
                    state.updated,
                    state.frame,
                    state.runtime.enemyPhysicsFlags[slot],
                    state.linkEntityX,
                    state.linkEntityY,
                    state.romLinkDirection,
                    state.projectileLinkState.usingShield(),
                    state.swordCollisionActive,
                    state.handlerLinkCollisionEnabled,
                    urchinBackgroundInteraction);
            state.updated = urchinUpdate.entity();
            state.runtime.enemyPhysicsFlags[slot] = urchinUpdate.physicsFlags();
            if (urchinUpdate.pushed()) {
              // UrchinEntityHandler writes $03 for its background probe,
              // then clears wEntitiesIgnoreHitsCountdown immediately.
              state.runtime.enemyIgnoreHitsCountdown[slot] = 0;
            }
            state.applyGenericGroundInteraction = urchinUpdate.appliesBackgroundInteraction();
            if (urchinUpdate.linkCollision()) {
              state.runtime.backgroundMechanics.requestLinkPush(
                  state.updated, EntityLinkCollisionRules.URCHIN);
            }
            if (urchinUpdate.jingleId() >= 0) {
              state.runtime.pendingEntityEvents.add(
                  new EntityCombatEvent(
                      slot,
                      state.entity.type(),
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.JINGLE,
                      urchinUpdate.jingleId()));
            }
            if (state.runtime.spriteHandlers != null) {
              EntitySpriteDefinition urchinDefinition =
                  state.runtime.spriteHandlers.forUrchinState(state.creditsGameplay);
              state.updated =
                  withDefinition(state.updated, urchinDefinition, state.updated.spriteVariant());
            }
            state.preserveUrchinPresentation = true;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_WITCH_RAT,
        EntityTickStage.PHASE_4_WITCH_RAT,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing
              && true
              && state.handlerLinkCollisionEnabled) {
            int slot = state.entity.slot();
            RoomEntityBackgroundInteraction witchRatBackgroundInteraction =
                state.runtime.backgroundInteraction;
            if (witchRatBackgroundInteraction == null && state.backgroundCollision != null) {
              witchRatBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            WitchRatMotion.Update witchRatUpdate =
                state.runtime.witchRatMotion.advance(
                    state.updated,
                    state.frame,
                    state.runtime.enemyTransitionCountdown[slot],
                    state.randomByteSupplier,
                    witchRatBackgroundInteraction);
            state.updated = witchRatUpdate.entity();
            state.runtime.enemyTransitionCountdown[slot] = witchRatUpdate.transitionCountdown();
            state.applyGenericGroundInteraction = witchRatUpdate.appliesBackgroundInteraction();
            state.preserveWitchRatPresentation = true;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_HARDHAT_BEETLE,
        EntityTickStage.PHASE_4_HARDHAT_BEETLE,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing && true) {
            state.updated =
                state.runtime.hardHatMotion.advance(
                    state.entity,
                    state.frame,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.randomByteSupplier,
                    state.backgroundCollision);
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_POLS_VOICE,
        EntityTickStage.PHASE_4_POLS_VOICE,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing && true) {
            // PolsVoiceEntityHandler deliberately writes the temporary
            // ignore-hits byte after moving, so the background helper sees
            // $01, then clears it with B (zero) before the default enemy
            // collision pass. The shared Java movement probe is the
            // handler's background-interaction seam, so bracket it here.
            state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()] = 0x01;
            try {
              PolsVoiceMotion.Update polsVoiceUpdate =
                  state.runtime.polsVoiceMotion.advance(
                      state.entity,
                      state.linkEntityX,
                      state.linkEntityY,
                      state.randomByteSupplier,
                      state.backgroundCollision,
                      state.runtime.enemyTransitionCountdown[state.entity.slot()]);
              state.updated = polsVoiceUpdate.entity();
              state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                  polsVoiceUpdate.transitionCountdown();
            } finally {
              state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()] = 0;
            }
            state.preservePolsVoicePresentation = true;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_SPIKED_BEETLE,
        EntityTickStage.PHASE_4_SPIKED_BEETLE,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing && true) {
            SpikedBeetleMotion.Update spikedBeetleUpdate =
                state.runtime.spikedBeetleMotion.advance(
                    state.entity,
                    state.frame,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.randomByteSupplier,
                    state.backgroundCollision,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()]);
            state.updated = spikedBeetleUpdate.entity();
            state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                spikedBeetleUpdate.transitionCountdown();
            state.runtime.entityOptions1Override[state.entity.slot()] =
                spikedBeetleUpdate.options1();
            state.runtime.enemyHitboxFlags[state.entity.slot()] = spikedBeetleUpdate.hitboxFlags();
            state.preserveSpikedBeetlePresentation = true;
          }
          return true;
        });
    for (int type = 0; type < 256; type++) {
      if ((type == RoomEntityRuntime.ENTITY_TARIN)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_4_TARIN,
            state -> {
              if (state.status == EntityStatus.ACTIVE
                  && !state.wasInitializing
                  && !state.runtime.indoorRoom
                  && true
                  && (state.runtime.tarinRaccoonMotion.state(state.entity.slot()) == 0
                      || state.runtime.roomInspection.tarinTransformationHandlerInteractive())) {
                RoomEntityBackgroundInteraction tarinBackgroundInteraction =
                    state.runtime.backgroundInteraction;
                if (tarinBackgroundInteraction == null && state.backgroundCollision != null) {
                  tarinBackgroundInteraction =
                      RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
                }
                TarinRaccoonMotion.Update tarinUpdate =
                    state.runtime.tarinRaccoonMotion.advance(
                        state.updated,
                        new TarinRaccoonMotion.Input(
                            state.frame,
                            state.linkEntityX,
                            state.linkEntityY,
                            state.romLinkDirection,
                            state.runtime.actionButtonAHeld,
                            state.runtime.dialogActive,
                            false,
                            state.runtime.linkAttackStepAnimationCountdown,
                            state.linkAirborne,
                            state.runtime.inventoryAppearing,
                            state.runtime.dialogCooldown,
                            state.runtime.windowY,
                            state.runtime.slowTransitionCountdown[state.entity.slot()],
                            state.runtime.enemyTransitionCountdown[state.entity.slot()]),
                        tarinBackgroundInteraction);
                state.updated = tarinUpdate.entity();
                state.runtime.slowTransitionCountdown[state.entity.slot()] =
                    tarinUpdate.slowTransitionCountdown();
                state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                    tarinUpdate.transitionCountdown();
                state.preserveTarinPresentation = true;
                state.runtime.shouldGetLostInMysteriousWoods = tarinUpdate.shouldGetLost();
                if (tarinUpdate.linkMotionBlocked()) {
                  state.runtime.pendingLinkMotionBlockRequests.add(
                      new LinkMotionBlockRequest(state.entity.slot()));
                }
                if (tarinUpdate.dialogGlobalId() >= 0) {
                  state.runtime.pendingDialogRequests.add(
                      new DialogRequest(
                          tarinUpdate.dialogGlobalId() >>> 8, tarinUpdate.dialogGlobalId() & 0xFF));
                }
                if (tarinUpdate.clearLinkAttack()) {
                  state.runtime.pendingLinkAttackClearRequests.add(
                      new LinkAttackClearRequest(state.entity.slot()));
                }
                if (tarinUpdate.linkFacingDirection() >= 0) {
                  state.runtime.pendingLinkFacingRequests.add(
                      new LinkFacingRequest(
                          state.entity.slot(), tarinUpdate.linkFacingDirection(), true));
                }
                if (tarinUpdate.pushLink()) {
                  if (RoomEntityCombatRules.overlapsLink(
                      state.updated, state.linkEntityX, state.linkEntityY)) {
                    state.runtime.backgroundMechanics.requestLinkPush(
                        state.updated, EntityLinkCollisionRules.STANDARD_PUSH);
                  }
                }
                if (tarinUpdate.soundId() >= 0) {
                  state.runtime.pendingEntityEvents.add(
                      new EntityCombatEvent(
                          state.entity.slot(),
                          state.entity.type(),
                          0,
                          false,
                          EntityCombatEvent.SoundChannel.JINGLE,
                          tarinUpdate.soundId()));
                  if (tarinUpdate.soundId() == 0x23) {
                    state.runtime.pendingMusicTrack = state.runtime.defaultMusicTrack;
                  }
                }
                if (tarinUpdate.spawnBomb()) {
                  state.runtime.entityEnemySpawns.spawnTarinTransformationBomb(state.updated);
                }
                if (tarinUpdate.roomChanged() || tarinUpdate.tarinFlag()) {
                  state.runtime.pendingRoomStatusPersistenceRequests.add(
                      new RoomStatusPersistenceRequest(state.entity.slot(), 0x10, 1));
                }
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((isGhiniType(type))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_4_GHINI_TYPE,
            state -> {
              if (state.status == EntityStatus.ACTIVE && !state.wasInitializing && true) {
                state.updated =
                    state.runtime.ghiniMotion.advance(
                        state.entity,
                        state.frame,
                        state.entity.type(),
                        state.linkEntityX,
                        state.linkEntityY,
                        state.romCollisionType,
                        state.randomByteSupplier);
                state.preserveGhiniPresentation = true;
              }
              return true;
            });
      }
    }
  }
}
