package linksawakening.world;

import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.HookshotBridgeUpdate;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.event.WarpLinkStateRequest;

import static linksawakening.world.EntityMotionMath.*;
import static linksawakening.world.EntitySnapshots.*;
import static linksawakening.world.EntityTypeRules.*;
import static linksawakening.world.RoomEntityRuntime.*;

import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.vfx.TransientVfxType;

/** Registered ROM type callbacks for ordered slot phase 1. */
final class EntityPhase1Handlers {
  static void install(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_KANALET_BOMBABLE_WALL,
        EntityTickStage.PHASE_1_KANALET_BOMBABLE_WALL,
        state -> {
          {
            if ((state.runtime.entityRoomStatus & 0x10) != 0) {
              state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
              return false;
            }
            state.runtime.droppedItemBySlot[state.entity.slot()] =
                state.runtime.ENTITY_HIDING_SLIME_KEY;
            if (state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()] != 0) {
              if (state.runtime.entityEnemySpawns.spawnKanaletWallMoblin(state.entity)) {
                state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                return false;
              }
            }
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_TARIN,
        EntityTickStage.PHASE_1_TARIN,
        state -> {
          if (!state.runtime.indoorRoom && (state.runtime.entityRoomStatus & 0x10) != 0) {
            state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
            return false;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MARIN_INDOOR,
        EntityTickStage.PHASE_1_MARIN_INDOOR,
        state -> {
          if (state.runtime.indoorRoom
             
              && state.runtime.chestPlayerLevelsKnown
              && state.runtime.chestSwordLevel != 0) {
            state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
            return false;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_BOO_BUDDY,
        EntityTickStage.PHASE_1_BOO_BUDDY,
        state -> {
          if (state.initialStatus == EntityStatus.ACTIVE) {
            int slot = state.entity.slot();
            // Rendering precedes ReturnIfNonInteractive_06 and recoil.
            state.entity =
                withVariant(state.entity, state.runtime.booBuddyMotion.selectedVariant(slot));
            state.runtime.slots[state.index] = state.entity;
            if (state.projectileTimersInteractive) {
              state.runtime.booBuddyMotion.tickTimers(slot);
            }
            if (!state.handlerInteractive) {
              return false;
            }
            if (state.runtime.enemyIgnoreHitsCountdown[slot] != 0) {
              // $06:64F7 consumes one ignore-hits tick, moves with recoil
              // speeds, and pops the handler return address. OPTIONS1
              // bit $20 skips background collision for Boo Buddy.
              state.runtime.enemyIgnoreHitsCountdown[slot]--;
              state.runtime.slots[state.index] =
                  state.runtime.booBuddyMotion.advanceRecoil(
                      state.entity,
                      state.runtime.enemyRecoilMotion.recoilSpeedX(slot),
                      state.runtime.enemyRecoilMotion.recoilSpeedY(slot));
              return false;
            }
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_BOW_WOW,
        EntityTickStage.PHASE_1_BOW_WOW,
        state -> {
          if (state.initialStatus == EntityStatus.ACTIVE) {
            int slot = state.entity.slot();
            if (state.projectileTimersInteractive) {
              state.runtime.bowWowMotion.tickTimers(slot);
            }
            // $05:4054 selects the following tiles for every nonzero
            // quest state, including kidnapped ($80), before any gates.
            int spriteAddress = state.runtime.bowWowState != 0 ? 0x401C : 0x4000;
            if (state.runtime.spriteHandlers != null
                && (state.entity.spriteDefinition().bank() != 0x05
                    || state.entity.spriteDefinition().address() != spriteAddress)) {
              state.entity =
                  withDefinition(
                      state.entity,
                      state.runtime.bowWowState != 0
                          ? state.runtime.spriteHandlers.forFollowerEntityType(
                              state.runtime.ENTITY_BOW_WOW)
                          : state.runtime.spriteHandlers.forEntityType(
                              state.runtime.ENTITY_BOW_WOW, EntityRoomLoader.RoomTable.OVERWORLD),
                      state.entity.spriteVariant());
            }
            state.entity =
                withVariant(state.entity, state.runtime.bowWowMotion.selectedVariant(slot));
            state.runtime.slots[state.index] = state.entity;
            if (!state.bowWowHandlerInteractive) {
              return false;
            }
            if (state.runtime.enemyIgnoreHitsCountdown[slot] != 0) {
              state.runtime.enemyIgnoreHitsCountdown[slot]--;
            }
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MOLDORM,
        EntityTickStage.PHASE_1_MOLDORM,
        state -> {
          if (!state.wasInitializing
             
              && !state.runtime.roomTransitionActive
              && !state.runtime.inventoryAppearing) {
            BossIntroMotion.Update intro =
                state.runtime.bossIntroMotion.advance(
                    state.runtime.options1(state.entity.slot()),
                    state.entity.type(),
                    state.runtime.entityMapId,
                    state.runtime.transitionSequenceCounter);
            if (intro.musicTrack() >= 0) state.runtime.pendingMusicTrack = intro.musicTrack();
            if (intro.dialogLowId() >= 0) {
              state.runtime.pendingDialogRequests.add(
                  new DialogRequest(intro.dialogTableId(), intro.dialogLowId()));
              state.moldormIntroOpenedThisFrame = true;
            }
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_SIDE_VIEW_POT,
        EntityTickStage.PHASE_1_SIDE_VIEW_POT,
        state -> {
          if (state.status == EntityStatus.ACTIVE
             
              && !state.runtime.sideViewPotMotion.active(state.entity.slot())) {
            SideViewPotContact.Result contact = SideViewPotContact.Result.none();
            int effectiveLinkX = state.linkEntityX;
            int effectiveLinkY = state.linkEntityY;
            if (state.sideViewPotLinkCollisionInteractive) {
              contact =
                  SideViewPotContact.resolve(
                      state.entity,
                      state.linkEntityX,
                      state.linkEntityY,
                      state.linkAirborne,
                      state.runtime.currentLinkSpeedY);
              if (contact.collided()) {
                state.runtime.liftThrow.pendingLinkPotContact(state.entity.slot(), contact);
                if (contact.restoreFinalPositionX()) {
                  effectiveLinkX =
                      state.runtime.currentLinkFinalPositionXConfigured
                          ? state.runtime.currentLinkFinalPositionX
                          : state.linkEntityX;
                }
                if (contact.snapTop()) {
                  effectiveLinkY = contact.positionY();
                }
              }
            }
            if (state.runtime.groundInteractionSideScrolling
                && state.sideViewPotInteractive
                && SideViewPotContact.liftWindow(state.entity, effectiveLinkX, effectiveLinkY)
                && state.runtime.liftThrow.sideViewPotPowerBraceletHeld()
                && state.runtime.linkAttackStepAnimationCountdown == 0
                && state.runtime.beginLift(state.entity.slot(), state.romLinkDirection)) {
              state.runtime.pendingEntityEvents.add(
                  new EntityCombatEvent(
                      state.entity.slot(),
                      state.entity.type(),
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.WAVE,
                      0x02));
            }
            return false;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_SIDE_VIEW_POT,
        EntityTickStage.PHASE_1_SIDE_VIEW_POT_2,
        state -> {
          if (state.status == EntityStatus.ACTIVE
             
              && state.runtime.sideViewPotMotion.active(state.entity.slot())) {
            if (state.sideViewPotInteractive) {
              SideViewPotMotion.Update potUpdate =
                  state.runtime.sideViewPotMotion.advance(state.entity);
              RoomEntity movedPot = potUpdate.entity();
              boolean collision =
                  state.runtime.liftThrow.sideViewPotBackgroundCollision(
                      state.entity, movedPot, state.backgroundCollision, state.frame);
              if (collision) {
                state.runtime.spawnLiftableRockSmash(
                    movedPot.x(),
                    (movedPot.y() - movedPot.z()) & 0xFF,
                    state.runtime.LIFTABLE_ROCK_SMASH_MODE_ROCK);
                state.runtime.clearEntity(state.entity.slot());
              } else {
                state.runtime.slots[state.index] = movedPot;
              }
            }
            // ReturnIfNonInteractive_19 leaves the rendered pot and all
            // dedicated motion state untouched while Link is gated.
            return false;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_DOG,
        EntityTickStage.PHASE_1_DOG,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            // DogEntityHandler writes this before ReturnIfNonInteractive_19.
            state.runtime.enemyHealth[state.entity.slot()] = DogMotion.HANDLER_HEALTH;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_HOOKSHOT_BRIDGE,
        EntityTickStage.PHASE_1_HOOKSHOT_BRIDGE,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            state.runtime.backgroundMechanics.advanceHookshotBridgeEntity(
                state.index, state.entity);
            return false;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_HOOKSHOT_CHAIN,
        EntityTickStage.PHASE_1_HOOKSHOT_CHAIN,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            HookshotChainMotion.State hookshotState =
                state.runtime.hookshotChainMotion.state(state.entity.slot());
            if (hookshotState == null) {
              state.runtime.clearEntity(state.entity.slot());
              return false;
            }
            // $7BE3 cancels the chain before rendering or the pause gate.
            if (state.runtime.linkPlayingOcarinaCountdown != 0) {
              state.runtime.clearEntity(state.entity.slot());
              return false;
            }
            if (state.projectileTimersInteractive) {
              hookshotState = HookshotChainMotion.decrementTransitionCountdown(hookshotState);
              state.runtime.hookshotChainMotion.setState(state.entity.slot(), hookshotState);
            }
            // Bank $18 has no credits exception in ReturnIfNonInteractive.
            if (!state.handlerInteractive || state.creditsGameplay) {
              return false;
            }

            if ((state.frame & 0x03) == 0) {
              state.runtime.pendingEntityEvents.add(
                  new EntityCombatEvent(
                      state.entity.slot(),
                      state.entity.type(),
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.NOISE,
                      0x0B));
            }

            if (hookshotState.entityState() == HookshotChainMotion.PULLING_STATE) {
              if (state.handlerLinkCollisionEnabled
                  && HookshotChainMotion.overlapsLink(
                      hookshotState, state.linkEntityX, state.linkEntityY)) {
                state.runtime.clearEntity(state.entity.slot());
                return false;
              }
              HookshotChainMotion.PullSpeed pullSpeed =
                  HookshotChainMotion.pullLinkSpeed(
                      hookshotState, state.linkEntityX, state.linkEntityY);
              state.projectileEvents.add(
                  EntityProjectileEvent.hookshotPull(
                      state.entity.slot(), pullSpeed.speedX(), pullSpeed.speedY()));
              state.runtime.slots[state.index] = state.entity;
              return false;
            }

            HookshotChainMotion.Position nextPosition =
                HookshotChainMotion.nextPosition(hookshotState);
            HookshotChainMotion.Step step =
                HookshotChainMotion.advance(
                    hookshotState, state.linkEntityX, state.linkEntityY, false, false);
            if (step.returning()) {
              state.runtime.hookshotChainMotion.setState(state.entity.slot(), step.state());
              if (state.handlerLinkCollisionEnabled && step.reachedLink()) {
                state.runtime.clearEntity(state.entity.slot());
              } else {
                state.runtime.slots[state.index] =
                    withPositionAndVariant(
                        state.entity,
                        step.state().x(),
                        step.state().y(),
                        state.entity.spriteVariant());
              }
              return false;
            }
            // $7C59 checks enemies before background interaction. The
            // collision pass may clear the timer, but this pass still
            // follows the already-selected outgoing handler branch.
            boolean unmasked =
                state.runtime.projectiles.collideHookshotWithEntities(
                    state.entity, step.state(), state.frame);
            if (unmasked) {
              step =
                  new HookshotChainMotion.Step(
                      HookshotChainMotion.completeWallPoke(step.state()),
                      step.collided(),
                      step.reachedLink(),
                      step.returning());
            }
            if (hookshotState.wallCollisionPending()) {
              state.runtime.hookshotChainMotion.setState(
                  state.entity.slot(), HookshotChainMotion.completeWallPoke(step.state()));
              state.runtime.pendingEntityEvents.add(
                  new EntityCombatEvent(
                      state.entity.slot(),
                      state.entity.type(),
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.JINGLE,
                      0x07));
              state.runtime.transientVfxRequests.add(
                  new TransientVfxRequest(
                      TransientVfxType.SWORD_POKE,
                      state.entity.x(),
                      (state.entity.y() - state.entity.z()) & 0xFF));
              state.runtime.slots[state.index] =
                  withPositionAndVariant(
                      state.entity,
                      step.state().x(),
                      step.state().y(),
                      state.entity.spriteVariant());
              return false;
            }
            EntityBackgroundCollisionResult collisionResult = null;
            boolean blocked;
            if (state.runtime.backgroundInteraction != null) {
              collisionResult =
                  state.runtime.backgroundInteraction.probe(
                      state.entity,
                      hookshotState.direction(),
                      nextPosition.x(),
                      nextPosition.y(),
                      state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()],
                      state.frame);
              blocked = collisionResult.blocked();
            } else {
              blocked =
                  state.backgroundCollision != null
                      && state.backgroundCollision.blocks(
                          state.entity,
                          hookshotState.direction(),
                          nextPosition.x(),
                          nextPosition.y());
            }
            if (blocked) {
              HookshotChainMotion.State stopped =
                  HookshotChainMotion.rollbackTo(
                      step.state(), hookshotState.x(), hookshotState.y());
              if (collisionResult != null && collisionResult.physicsFlag() == 0x60) {
                if (HookshotChainMotion.shouldUnloadForHookshotable(
                    step.state().transitionCountdown())) {
                  state.runtime.clearEntity(state.entity.slot());
                  return false;
                }
                state.runtime.hookshotChainMotion.setState(
                    state.entity.slot(), HookshotChainMotion.enterPulling(stopped));
              } else {
                state.runtime.hookshotChainMotion.setState(
                    state.entity.slot(), HookshotChainMotion.deferWallPoke(stopped));
              }
              state.runtime.slots[state.index] = state.entity;
              return false;
            }

            if (state.runtime.indoorRoom
                && hookshotState.speedY() != 0
                && state.runtime.objectQuery != null) {
              RoomEntity movedChain =
                  withPositionAndVariant(
                      state.entity,
                      step.state().x(),
                      step.state().y(),
                      state.entity.spriteVariant());
              RoomEntityObjectSample object = state.runtime.objectQuery.sample(movedChain);
              if (object != null) {
                int bridgeObject =
                    (hookshotState.speedY() & 0x80) != 0
                        ? state.runtime.OBJECT_HOOKSHOT_BRIDGE_PULL_DOWN
                        : state.runtime.OBJECT_HOOKSHOT_BRIDGE_PULL_UP;
                if (object.objectId() == bridgeObject
                    && state.runtime.entityPlayerSpawns.spawnHookshotBridge(
                        object.objectLeft() + 0x08,
                        object.objectTop() + 0x10,
                        bridgeObject == state.runtime.OBJECT_HOOKSHOT_BRIDGE_PULL_DOWN ? 0 : 1,
                        step.state().transitionCountdown())) {
                  state.runtime.hookshotBridgeUpdates.add(
                      new HookshotBridgeUpdate(
                          object.objectLeft(),
                          object.objectTop(),
                          bridgeObject == state.runtime.OBJECT_HOOKSHOT_BRIDGE_PULL_DOWN ? 0 : 1));
                }
              }
            }

            state.runtime.hookshotChainMotion.setState(state.entity.slot(), step.state());
            state.runtime.slots[state.index] =
                withPositionAndVariant(
                    state.entity, step.state().x(), step.state().y(), state.entity.spriteVariant());
            return false;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_BOOMERANG,
        EntityTickStage.PHASE_1_BOOMERANG,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            state.runtime.projectiles.advanceBoomerangEntity(
                state.index,
                state.entity,
                state.frame,
                state.linkEntityX,
                state.linkEntityY,
                state.linkZ,
                state.projectileLinkState.motionState(),
                state.handlerInteractive,
                state.projectileTimersInteractive);
            return false;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_SWORD_BEAM,
        EntityTickStage.PHASE_1_SWORD_BEAM,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            state.runtime.projectiles.advanceSwordBeamEntity(
                state.index,
                state.entity,
                state.frame,
                state.linkEntityX,
                state.linkEntityY,
                state.romLinkDirection,
                state.handlerInteractive);
            return false;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MAGIC_ROD_FIREBALL,
        EntityTickStage.PHASE_1_MAGIC_ROD_FIREBALL,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            state.runtime.projectiles.advanceMagicRodFireballEntity(
                state.index,
                state.entity,
                state.frame,
                state.handlerInteractive,
                state.projectileTimersInteractive);
            return false;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MAGIC_POWDER_SPRINKLE,
        EntityTickStage.PHASE_1_MAGIC_POWDER_SPRINKLE,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            state.runtime.projectiles.advanceMagicPowderSprinkleEntity(
                state.index,
                state.entity,
                state.frame,
                state.projectileLinkState.motionState(),
                state.handlerInteractive && !state.creditsGameplay,
                state.projectileTimersInteractive);
            return false;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MUSICAL_NOTE,
        EntityTickStage.PHASE_1_MUSICAL_NOTE,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            RoomEntity note = state.runtime.slotProperties.advanceMusicalNoteEntity(state.entity);
            if (note == null) {
              return false;
            }
            state.runtime.slots[state.index] = note;
            return false;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_BOMB,
        EntityTickStage.PHASE_1_BOMB,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            BombMotion.Decision bombDecision =
                state.runtime.bombInteractions.advanceBombEntity(
                    state.index,
                    state.entity,
                    state.projectileEvents,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.projectileLinkState);
            state.entity = state.runtime.slots[state.index];
            if (bombDecision.phase() == BombMotion.Phase.NORMAL) {
              state.runtime.slots[state.index] =
                  state.runtime.bombPrivateState4[state.entity.slot()] != 0
                      ? state.runtime.liftThrow.advanceEnemyBombEntity(
                          state.entity, state.backgroundCollision, state.frame)
                      : state.runtime.liftThrow.advancePlacedBombEntity(
                          state.entity, state.backgroundCollision, state.frame);
            }
            state.runtime.liftThrow.tryLiftBombIfRequested(
                state.index,
                bombDecision,
                state.linkEntityX,
                state.linkEntityY,
                state.linkZ,
                state.romLinkDirection);
            return false;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_ARROW,
        EntityTickStage.PHASE_1_ARROW,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            if (state.runtime.playerArrowBombArrow[state.entity.slot()]
                && state.runtime.playerArrowMotion.transitionCountdown(state.entity.slot()) != 0) {
              int explosionSlot =
                  state.runtime.entityPlayerSpawns.spawnBombFromBombArrow(state.entity);
              if (explosionSlot >= 0) {
                state.runtime.pendingEntityEvents.add(
                    new EntityCombatEvent(
                        explosionSlot,
                        state.runtime.ENTITY_BOMB,
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.NOISE,
                        0x0C));
              }
              state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
              return false;
            }
            // ArrowEntityHandler jumps straight to rendering/rebound
            // while its transition countdown is nonzero ($6A3D-$6A40).
            boolean hitEntity =
                state.runtime.playerArrowMotion.transitionCountdown(state.entity.slot()) == 0
                    && state.runtime.projectiles.collidePlayerArrowWithEntities(
                        state.entity, state.frame);
            if (hitEntity && !state.runtime.playerArrowBombArrow[state.entity.slot()]) {
              // func_003_75A2 reaches jr_003_7737 after an ordinary
              // arrow target hit. With no wall-collision byte set on
              // the active arrow, the state-0 arrow unloads there.
              state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
              return false;
            }
            PlayerArrowMotion.Update arrowUpdate =
                state.runtime.playerArrowMotion.advance(
                    state.entity,
                    state.backgroundCollision,
                    state.frame,
                    state.runtime.entityGroundStatus[state.entity.slot()],
                    state.runtime.groundInteractionSideScrolling,
                    state.handlerInteractive,
                    state.projectileTimersInteractive);
            if (arrowUpdate.unloaded()) {
              state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
              return false;
            }
            if (arrowUpdate.collidedWithWall()) {
              state.runtime.pendingEntityEvents.add(
                  new EntityCombatEvent(
                      state.entity.slot(),
                      state.runtime.ENTITY_ARROW,
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.JINGLE,
                      0x07));
              // AlertSwordMoblins is shared by player arrows, enemy
              // projectiles, and bomb explosions.
              state.runtime.swordMoblinAlertingSoundCounter = 0x04;
            }
            state.runtime.slots[state.index] = arrowUpdate.entity();
            return false;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_LIFTABLE_ROCK,
        EntityTickStage.PHASE_1_LIFTABLE_ROCK,
        state -> {
          if (state.status == EntityStatus.ACTIVE
             
              && state.runtime.liftableRockSmashActive[state.entity.slot()]) {
            state.runtime.liftThrow.advanceLiftableRockSmash(
                state.index, state.entity, state.frame);
            return false;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_HEART_CONTAINER,
        EntityTickStage.PHASE_1_HEART_CONTAINER,
        state -> {
          if (state.status == EntityStatus.ACTIVE
             
              && state.runtime.enemyTransitionCountdown[state.entity.slot()] != 0) {
            state.runtime.bossRewards.advanceCollectedHeartContainer(
                state.entity, state.linkEntityX, state.linkEntityY, state.linkZ);
            return false;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MADAM_MEOWMEOW,
        EntityTickStage.PHASE_1_MADAM_MEOWMEOW,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            state.runtime.story.advanceMadamMeowMeow(
                state.entity,
                state.frame,
                state.linkEntityX,
                state.linkEntityY,
                state.linkAirborne);
            return false;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_RICHARD,
        EntityTickStage.PHASE_1_RICHARD,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            state.runtime.story.advanceRichard(
                state.entity,
                state.frame,
                state.linkEntityX,
                state.linkEntityY,
                state.linkAirborne);
            return false;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_BOW_WOW,
        EntityTickStage.PHASE_1_BOW_WOW_2,
        state -> {
          if (state.status == EntityStatus.ACTIVE
             
              && state.entity.sourceLoadOrder() >= 0
              && state.runtime.bowWowState == 0x80
              && state.handlerLinkCollisionEnabled
              && state.romCollisionType != 0
              && withinUnsignedWindow(state.linkEntityX, 0x88, 0x10)
              && withinUnsignedWindow(state.linkEntityY, 0x40, 0x10)) {
            // BowWowEntityHandler's kidnapped branch checks the handler's
            // collision byte only after Link enters the $88/$40 pen.
            state.runtime.bowWowState = 0x01;
            if (state.runtime.spriteHandlers != null) {
              state.entity =
                  withDefinition(
                      state.entity,
                      state.runtime.spriteHandlers.forFollowerEntityType(
                          state.runtime.ENTITY_BOW_WOW),
                      state.entity.spriteVariant());
              state.runtime.slots[state.index] = state.entity;
            }
            state.runtime.followingNpcState =
                new FollowingNpcState(
                    state.runtime.followingNpcState.roosterFollowing(),
                    state.runtime.followingNpcState.ghostFollowingState(),
                    state.runtime.followingNpcState.marinFollowing(),
                    true,
                    state.runtime.followingNpcState.instrument4Flags(),
                    state.runtime.followingNpcState.powerBraceletLevel(),
                    state.runtime.followingNpcState.db10());
            state.runtime.pendingMusicTrack = 0x10;
            state.runtime.pendingDialogRequests.add(new DialogRequest(1, 0x6C));
            state.runtime.pendingEntityEvents.add(
                new EntityCombatEvent(
                    state.entity.slot(),
                    state.entity.type(),
                    0,
                    false,
                    EntityCombatEvent.SoundChannel.WAVE,
                    0x18));
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_SWORD_SHIELD_PICKUP,
        EntityTickStage.PHASE_1_SWORD_SHIELD_PICKUP,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            if (state.runtime.chestSwordLevel == 0 && state.runtime.spriteHandlers != null) {
              state.entity =
                  withDefinition(
                      state.entity,
                      state.runtime.spriteHandlers.forSwordShieldPickup(true),
                      state.entity.spriteVariant());
              state.runtime.slots[state.index] = state.entity;
            }
            if (state.runtime.swordPickupSequenceActive[state.entity.slot()]) {
              state.runtime.rewards.advanceSwordPickup(
                  state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.linkZ);
              return false;
            }
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_SLEEPY_TOADSTOOL,
        EntityTickStage.PHASE_1_SLEEPY_TOADSTOOL,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            if (!state.runtime.toadstoolPickupSequenceActive[state.entity.slot()]
                && (state.runtime.playerHasToadstool
                    || state.runtime.playerMagicPowderCount != 0)) {
              state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
              return false;
            }
            if (state.runtime.toadstoolPickupSequenceActive[state.entity.slot()]) {
              state.runtime.rewards.advanceToadstoolPickup(
                  state.entity, state.linkEntityX, state.linkEntityY, state.linkZ);
              return false;
            }
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_OWL_EVENT,
        EntityTickStage.PHASE_1_OWL_EVENT,
        state -> {
          if (state.status == EntityStatus.ACTIVE) {
            state.runtime.story.advanceOwlEvent(
                state.entity,
                state.frame,
                state.linkEntityX,
                state.linkEntityY,
                state.handlerLinkCollisionEnabled);
            return false;
          }
          return true;
        });
    for (int type = 0; type < 256; type++) {
      if ((type == 0x35)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_TYPE_0X35,
            state -> {
              if ((state.runtime.entityRoomStatus & 0x10) != 0) {
                state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                return false;
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((type == RoomEntityRuntime.ENTITY_CROW)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_CROW,
            state -> {
              if (state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()] >= 0x10) {
                state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()] >>>= 2;
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((type == RoomEntityRuntime.ENTITY_GENIE)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_GENIE,
            state -> {
              if (state.runtime.geniePrivateCountdown1[state.entity.slot()] > 0) {
                state.runtime.geniePrivateCountdown1[state.entity.slot()]--;
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((type == RoomEntityRuntime.ENTITY_GENIE)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_GENIE_2,
            state -> {
              if (state.runtime.geniePrivateCountdown2[state.entity.slot()] > 0) {
                state.runtime.geniePrivateCountdown2[state.entity.slot()]--;
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_ROLLING_BONES
          || type == RoomEntityRuntime.ENTITY_MOLDORM))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_MOLDORM_2,
            state -> {
              if (!state.wasInitializing
                  && !state.runtime.roomTransitionActive
                  && !state.runtime.freeMovementMode
                 ) {
                int dx = 0;
                int dy = 0;
                if (((state.linkEntityX - 0x11) & 0xFF) >= 0x7E) {
                  dx = state.linkEntityX < 0x50 ? 1 : -1;
                } else if (((state.linkEntityY - 0x16) & 0xFF) >= 0x5E) {
                  dy = -1;
                }
                if (dx != 0 || dy != 0) {
                  state.runtime.pendingLinkMotionBlockRequests.add(
                      new LinkMotionBlockRequest(state.entity.slot(), dx, dy));
                  state.linkEntityX = (state.linkEntityX + dx) & 0xFF;
                  state.linkEntityY = (state.linkEntityY + dy) & 0xFF;
                  state.runtime.entityInteractionCountdown = 2;
                }
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((type == WarpMotion.ENTITY_TYPE)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_TYPE,
            state -> {
              if (state.status == EntityStatus.ACTIVE
                  && state.runtime.indoorRoom
                 
                  && state.runtime.warpMotion != null) {
                int slot = state.entity.slot();
                state.runtime.slots[state.index] =
                    withPositionAndVariant(
                        state.entity,
                        state.entity.x(),
                        state.entity.y(),
                        state.runtime.dungeonMinibossReady ? (state.frame >> 3) & 7 : -1);
                WarpMotion.Update warpUpdate =
                    state.runtime.warpMotion.advance(
                        state.entity,
                        state.runtime.warpState[slot],
                        state.runtime.warpTransitionCountdown[slot],
                        state.runtime.entityMapId,
                        state.runtime.entityRoomId,
                        state.runtime.dungeonMinibossReady,
                        state.linkEntityX,
                        state.linkEntityY,
                        state.linkZ);
                state.runtime.warpState[slot] = warpUpdate.state();
                state.runtime.warpTransitionCountdown[slot] = warpUpdate.countdown();
                if (warpUpdate.musicTrack() >= 0) {
                  state.runtime.pendingMusicTrack = warpUpdate.musicTrack();
                }
                if (warpUpdate.jingleId() >= 0) {
                  state.runtime.pendingEntityEvents.add(
                      new EntityCombatEvent(
                          slot,
                          state.entity.type(),
                          0,
                          false,
                          EntityCombatEvent.SoundChannel.JINGLE,
                          warpUpdate.jingleId()));
                }
                if (warpUpdate.motionBlocked()) {
                  state.runtime.pendingLinkMotionBlockRequests.add(
                      new LinkMotionBlockRequest(slot));
                }
                if (warpUpdate.state() == WarpMotion.STATE_TRANSITION) {
                  state.runtime.pendingWarpLinkStateRequests.add(
                      new WarpLinkStateRequest(
                          slot,
                          state.entity.x(),
                          state.entity.y(),
                          warpUpdate.immunityCountdown()));
                }
                if (warpUpdate.destinationReady()) {
                  state.runtime.pendingDungeonWarpRequests.add(
                      new DungeonWarpRequest(slot, warpUpdate.destination()));
                  state.runtime.lifecycle.disableEntityWithoutPersistence(slot);
                }
                return false;
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((type == 0x42)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_TYPE_0X42,
            state -> {
              if (state.status == EntityStatus.ACTIVE
                 
                  && state.runtime.indoorRoom
                  && state.runtime.entityMapId != 0x16) {
                state.runtime.story.advanceOwlStatue(
                    state.entity,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.linkZ,
                    state.projectileLinkState.motionState(),
                    state.linkAirborne);
                return false;
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_KID_71 || type == RoomEntityRuntime.ENTITY_KID_72))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_KID_72,
            state -> {
              if (state.status == EntityStatus.ACTIVE
                 
                  && state.runtime.bowWowState == 0x80) {
                state.runtime.story.advanceKidnapWarningKid(
                    state.entity, state.frame, state.linkEntityX, state.linkEntityY);
                return false;
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((type == 0x35)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_TYPE_0X35_2,
            state -> {
              if (state.status == EntityStatus.ACTIVE
                 
                  && state.runtime.heartPiecePickup[state.entity.slot()] != null) {
                state.runtime.pickups.advanceHeartPiecePickup(
                    state.entity, state.linkEntityX, state.linkEntityY, state.linkZ);
                return false;
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((type == 0x39)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_TYPE_0X39,
            state -> {
              if (state.status == EntityStatus.ACTIVE
                 
                  && state.runtime.instrumentParticleKind[state.entity.slot()] == 0
                  && state.runtime.instrumentPickupState[state.entity.slot()] < 3
                  && state.runtime.transitionSequenceCounter == 4
                  && state.runtime.spriteSelection != null) {
                int[][] palettes = state.runtime.spriteSelection.objectPalettes();
                int offset = ((state.frame >> 1) & 0x3C) / 2;
                palettes[1][2] = state.runtime.instrumentPaletteCycle[offset];
                palettes[1][3] = state.runtime.instrumentPaletteCycle[offset + 1];
                state.runtime.spriteSelection =
                    state.runtime.spriteSelection.withObjectPalettes(palettes);
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((type == EntitySpriteHandlerCatalog.ENTITY_INSTRUMENT_OF_THE_SIRENS)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_INSTRUMENT_OF_THE_SIRENS,
            state -> {
              if (state.status == EntityStatus.ACTIVE
                 
                  && state.runtime.instrumentParticleKind[state.entity.slot()] == 1) {
                state.runtime.rewards.advanceInstrumentParticle(state.entity);
                return false;
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((type == EntitySpriteHandlerCatalog.ENTITY_INSTRUMENT_OF_THE_SIRENS)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_INSTRUMENT_OF_THE_SIRENS_2,
            state -> {
              if (state.status == EntityStatus.ACTIVE
                 
                  && state.runtime.instrumentPickupSequenceActive[state.entity.slot()]) {
                state.runtime.rewards.advanceInstrumentPickup(
                    state.entity,
                    state.frame,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.linkZ,
                    state.randomByteSupplier);
                return false;
              }
              return true;
            });
      }
    }
  }
}
