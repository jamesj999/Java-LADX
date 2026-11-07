package linksawakening.world;

import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.KeyQuicksandEvent;
import linksawakening.world.entity.event.KeyRewardEvent;
import linksawakening.world.entity.event.RoosterLinkStateRequest;
import linksawakening.world.entity.event.SideViewPlatformLinkRequest;

import static linksawakening.world.EntityMotionMath.*;
import static linksawakening.world.EntitySnapshots.*;
import static linksawakening.world.EntityTypeRules.*;
import static linksawakening.world.RoomEntityRuntime.*;

/** Registered ROM type callbacks for ordered slot phase 2. */
final class EntityPhase2Handlers {
  static void install(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_CHEST_WITH_ITEM,
        EntityTickStage.PHASE_2_CHEST_WITH_ITEM,
        state -> {
          if (state.wasInitializing) {
            state.runtime.pickups.initializeChestEntity(state.entity);
            state.updated =
                withPositionAndVariant(
                    state.entity,
                    state.entity.x(),
                    (state.entity.y() - 0x08) & 0xFF,
                    state.entity.spriteVariant());
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_ZOMBIE,
        EntityTickStage.PHASE_2_ZOMBIE,
        state -> {
          if (state.wasInitializing) {
            // The room-loaded Zombie is only a hidden spawn marker; its
            // bank-$18 handler never renders the parent entity.
            state.updated = withVariant(state.entity, -1);
            state.preserveZombiePresentation = true;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_CHEST_WITH_ITEM,
        EntityTickStage.PHASE_2_CHEST_WITH_ITEM_2,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            if (state.runtime.pickups.advanceChestEntity(state.index, state.entity)) {
              return false;
            }
            state.updated = state.runtime.slots[state.index];
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_DROPPABLE_SECRET_SEASHELL,
        EntityTickStage.PHASE_2_DROPPABLE_SECRET_SEASHELL,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            if (state.runtime.chestSwordLevel >= 0x02
                || (state.runtime.entityRoomStatus & 0x10) != 0
                || (state.runtime.entityRoomId == 0xE3
                    && (state.runtime.entityRoomStatus & 0x40) == 0)) {
              // DroppableSeashellEntityHandler uses UnloadEntityAndReturn
              // for these gates, which deliberately does not mark the
              // source load-order bit as completed.
              state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
              return false;
            }
            RoomEntityObjectSample objectUnderEntity =
                state.runtime.objectQuery == null
                    ? null
                    : state.runtime.objectQuery.sample(state.entity);
            SecretSeashellMotion.Update shellUpdate =
                state.runtime.secretSeashellMotion.advance(
                    state.entity,
                    state.runtime.entityRoomId,
                    state.frame,
                    false,
                    state.runtime.secretSeashellScreenShakeActive,
                    state.runtime.secretSeashellPegasusCollisionActive,
                    state.runtime.secretSeashellPegasusCollisionCoordinatesKnown,
                    state.runtime.secretSeashellPegasusCollisionX,
                    state.runtime.secretSeashellPegasusCollisionY,
                    state.linkEntityX,
                    state.linkEntityY,
                    objectUnderEntity);
            state.updated = shellUpdate.entity();
            state.runtime.entityOptions1Override[state.entity.slot()] =
                state.runtime.secretSeashellMotion.privateState3(state.entity.slot()) != 0
                    ? state.runtime.SECRET_SEASHELL_HIDDEN_OPTIONS1
                    : state.runtime.SECRET_SEASHELL_REVEALED_OPTIONS1;
            state.preserveSecretSeashellPresentation = true;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_HIDING_SLIME_KEY,
        EntityTickStage.PHASE_2_HIDING_SLIME_KEY,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing
             
              && (state.runtime.entityRoomStatus & 0x10) != 0) {
            // HidingSlimeKeyEntityHandler checks ROOM_STATUS_EVENT_1 before
            // calling the shared reveal helper and unloads without touching
            // the source persistence mask.
            state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
            return false;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_HIDING_SLIME_KEY,
        EntityTickStage.PHASE_2_HIDING_SLIME_KEY_2,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            int slot = state.entity.slot();
            SlimeKeyTransitionUpdate keyUpdate =
                state.runtime.pickups.advanceHidingSlimeKey(
                    state.updated, slot, state.linkEntityX, state.linkEntityY, state.linkZ);
            state.runtime.enemyTransitionCountdown[slot] = keyUpdate.nextTransitionCountdown();
            if (keyUpdate.reward() != null) {
              state.runtime.pendingSlimeKeyRewardEvents.add(keyUpdate.reward());
              state.runtime.pendingDialogRequests.add(
                  new DialogRequest(0, keyUpdate.reward().dialogLowId()));
            }
            if (keyUpdate.unload()) {
              state.runtime.lifecycle.disableEntityWithoutPersistence(slot);
              return false;
            }
            state.keyDropTransitionActive = keyUpdate.holdAboveLink();
            state.updated = keyUpdate.entity();
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_KEY_DROP_POINT,
        EntityTickStage.PHASE_2_KEY_DROP_POINT,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            // decrementEnemyStatusCountdowns mirrors UpdateEntityTimers'
            // shared transition-byte decrement before bank-$03's handler
            // reads it.
            int slot = state.entity.slot();
            if (state.runtime.pickups.checkForKeyDropQuicksandHole(state.updated)) {
              state.runtime.pendingKeyQuicksandEvents.add(new KeyQuicksandEvent(slot));
              state.runtime.slots[state.index] = withStatus(state.updated, EntityStatus.FALLING);
              state.runtime.enemyTransitionCountdown[slot] = 0x2F;
              state.runtime.fallingTargetX[slot] = 0x50;
              state.runtime.fallingTargetY[slot] = 0x48;
              state.runtime.fallingSpeedX[slot] = 0;
              state.runtime.fallingSpeedY[slot] = 0;
              state.runtime.fallingSpeedXAccumulator[slot] = 0;
              state.runtime.fallingSpeedYAccumulator[slot] = 0;
              state.runtime.fallingVisualYOffset[slot] = 0;
              state.runtime.pendingEntityEvents.add(
                  new EntityCombatEvent(
                      slot,
                      state.runtime.ENTITY_KEY_DROP_POINT,
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.JINGLE,
                      0x18));
              return false;
            }
            boolean hookshotRoom = state.runtime.entityRoomId == 0x80;
            if (hookshotRoom) {
              state.updated =
                  withDefinition(
                      state.updated,
                      state.runtime.slotProperties.spriteDefinitionFor(
                          state.runtime.ENTITY_HOOKSHOT_CHAIN),
                      0);
            }
            KeyDropPointMotion.Update keyUpdate =
                KeyDropPointMotion.advance(
                    state.runtime.enemyTransitionCountdown[slot],
                    state.updated.spriteVariant(),
                    hookshotRoom);
            state.runtime.enemyTransitionCountdown[slot] = keyUpdate.nextTransitionCountdown();
            if (keyUpdate.dialogLowId() >= 0) {
              state.runtime.pendingDialogRequests.add(
                  new DialogRequest(KeyDropPointMotion.DIALOG_TABLE, keyUpdate.dialogLowId()));
            }
            if (keyUpdate.rewardItemType() >= 0) {
              state.runtime.pendingKeyRewardEvents.add(
                  new KeyRewardEvent(slot, keyUpdate.rewardItemType()));
            }
            state.keyDropTransitionActive = keyUpdate.holdAboveLink();
            if (state.keyDropTransitionActive) {
              state.updated =
                  withPositionAndVariant(
                      state.updated,
                      state.linkEntityX,
                      state.linkEntityY - 0x0C,
                      state.updated.spriteVariant());
              state.updated = withZ(state.updated, state.linkZ);
            }
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_WIZROBE,
        EntityTickStage.PHASE_2_WIZROBE,
        state -> {
          if (state.wasInitializing) {
            // EntityInitWizrobe decrements the initial display-list variant
            // before the first active handler dispatch.
            state.updated = withVariant(state.entity, -1);
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_POLS_VOICE,
        EntityTickStage.PHASE_2_POLS_VOICE,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing
             
              && state.runtime.enemyInspection.polsVoiceBalladOcarinaTrigger()) {
            // The handler checks this before rendering, recoil, movement,
            // or the ordinary collision path. Its private countdown $1F
            // is the shared EntityDeathHandler's visible death timer.
            state.runtime.dyingCountdown[state.entity.slot()] = 0x1F;
            state.runtime.enemyPhysicsFlags[state.entity.slot()] = 0x04;
            state.status = EntityStatus.DYING;
            state.updated = withStatus(state.entity, EntityStatus.DYING);
            state.runtime.pendingEntityEvents.add(
                new EntityCombatEvent(
                    state.entity.slot(),
                    state.entity.type(),
                    0,
                    false,
                    EntityCombatEvent.SoundChannel.NOISE,
                    0x13));
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MINI_MOLDORM,
        EntityTickStage.PHASE_2_MINI_MOLDORM,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            int slot = state.entity.slot();
            // RenderMiniMoldorm records the history before the bank-$04
            // recoil helper runs, so keep this ordering explicit.
            state.runtime.miniMoldormMotion.beginFrame(
                state.entity, state.runtime.enemyIgnoreHitsCountdown[slot] != 0);
            EnemyRecoilMotion.Update recoil =
                state.runtime.backgroundMechanics.applyEnemyRecoilIfNeeded(
                    state.entity, state.backgroundCollision);
            state.entity = recoil.entity();
            state.updated = state.entity;
            RoomEntityBackgroundInteraction miniBackgroundInteraction =
                state.runtime.backgroundInteraction;
            if (miniBackgroundInteraction == null && state.backgroundCollision != null) {
              miniBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            int transitionForMotion = state.runtime.enemyTransitionCountdown[slot];
            if (transitionForMotion != 0) {
              // The common runtime timer has already performed the ROM
              // pre-handler decrement; MiniMoldormMotion mirrors that
              // decrement for its direct source-derived API as well.
              transitionForMotion = (transitionForMotion + 1) & 0xFF;
            }
            MiniMoldormMotion.Update miniUpdate =
                state.runtime.miniMoldormMotion.advanceAfterHistory(
                    state.entity,
                    transitionForMotion,
                    state.runtime.enemyIgnoreHitsCountdown[slot],
                    state.runtime.enemyFlashCountdown[slot],
                    miniBackgroundInteraction,
                    state.randomByteSupplier,
                    state.frame);
            state.updated = miniUpdate.entity();
            state.runtime.enemyTransitionCountdown[slot] = miniUpdate.transitionCountdown();
            if (state.runtime.spriteHandlers != null) {
              state.updated =
                  withDefinition(
                      state.updated,
                      state.runtime.spriteHandlers.forMiniMoldormState(
                          miniUpdate.headSpriteVariant(),
                          state.updated.x(),
                          (state.updated.y() - state.updated.z()) & 0xFF,
                          miniUpdate.segment1X(),
                          miniUpdate.segment1Y(),
                          miniUpdate.segment2X(),
                          miniUpdate.segment2Y()),
                      0);
            } else if (state.updated.spriteDefinition().supported()
                && miniUpdate.headSpriteVariant()
                    < state.updated.spriteDefinition().variantCount()) {
              state.updated = withVariant(state.updated, miniUpdate.headSpriteVariant());
            }
            state.preserveMiniMoldormPresentation = true;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MOLDORM,
        EntityTickStage.PHASE_2_MOLDORM,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            int slot = state.entity.slot();
            if (!state.handlerInteractive
                || state.countdownBlocksInteraction
                || state.moldormIntroOpenedThisFrame) {
              if (state.runtime.spriteHandlers != null) {
                state.runtime.slots[state.index] =
                    withDefinition(
                        state.entity,
                        state.runtime.spriteHandlers.forMoldormState(
                            state.runtime.moldormMotion.headSpriteVariant(slot),
                            state.entity.x(),
                            (state.entity.y() - state.entity.z()) & 0xFF,
                            state.runtime.moldormMotion.currentTailSegments(slot),
                            0,
                            state.frame),
                        0);
              }
              if (state.runtime.roomTransitionActive)
                state.runtime.moldormMotion.resetHistory(slot);
              return false;
            }
            RoomEntityBackgroundInteraction moldormBackgroundInteraction =
                state.runtime.backgroundInteraction;
            if (moldormBackgroundInteraction == null && state.backgroundCollision != null) {
              moldormBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            int transitionForMotion = state.runtime.enemyTransitionCountdown[slot];
            if (transitionForMotion != 0) {
              // Common countdowns have already ticked before dispatch;
              // the direct motion API performs the same decrement.
              transitionForMotion = (transitionForMotion + 1) & 0xFF;
            }
            MoldormMotion.Update moldormUpdate =
                state.runtime.moldormMotion.advance(
                    state.entity,
                    state.frame,
                    transitionForMotion,
                    state.runtime.enemyIgnoreHitsCountdown[slot],
                    state.runtime.enemyFlashCountdown[slot],
                    state.runtime.enemyHealth[slot],
                    state.randomByteSupplier,
                    moldormBackgroundInteraction);
            state.updated = moldormUpdate.entity();
            state.runtime.enemyTransitionCountdown[slot] = moldormUpdate.transitionCountdown();
            if (moldormUpdate.noiseRequested()) {
              state.runtime.pendingEntityEvents.add(
                  new EntityCombatEvent(
                      slot,
                      state.entity.type(),
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.NOISE,
                      0x1B));
            }
            if (state.runtime.spriteHandlers != null) {
              MoldormMotion.Presentation presentation = moldormUpdate.presentation();
              state.updated =
                  withDefinition(
                      state.updated,
                      state.runtime.spriteHandlers.forMoldormPresentation(
                          presentation.headSpriteVariant(),
                          presentation.headX(),
                          presentation.headY(),
                          state.updated.x(),
                          (state.updated.y() - state.updated.z()) & 0xFF,
                          presentation.segments(),
                          0,
                          state.frame),
                      0);
            }
            state.runtime.enemyPhysicsFlags[slot] = 0x02;
            state.preserveMoldormPresentation = true;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MIMIC,
        EntityTickStage.PHASE_2_MIMIC,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            int slot = state.entity.slot();
            RoomEntityBackgroundInteraction mimicBackgroundInteraction =
                state.runtime.backgroundInteraction;
            if (mimicBackgroundInteraction == null && state.backgroundCollision != null) {
              mimicBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            MimicMotion.Update mimicUpdate =
                state.runtime.mimicMotion.advance(
                    state.entity,
                    state.frame,
                    state.runtime.linkPressedButtonsMask,
                    state.romCollisionType,
                    mimicBackgroundInteraction,
                    state.runtime.enemyIgnoreHitsCountdown[slot],
                    state.frame);
            state.updated = mimicUpdate.entity();
            state.preserveMimicPresentation = true;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MASKED_MIMIC_GORIYA,
        EntityTickStage.PHASE_2_MASKED_MIMIC_GORIYA,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing
             
              && state.runtime.entityMapId != 0x1F) {
            int slot = state.entity.slot();
            RoomEntityBackgroundInteraction maskedMimicBackgroundInteraction =
                state.runtime.backgroundInteraction;
            if (maskedMimicBackgroundInteraction == null && state.backgroundCollision != null) {
              maskedMimicBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            MaskedMimicMotion.Update maskedMimicUpdate =
                state.runtime.maskedMimicMotion.advance(
                    state.entity,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.romLinkDirection,
                    state.runtime.linkPressedButtonsMask,
                    state.romCollisionType,
                    maskedMimicBackgroundInteraction,
                    state.runtime.enemyIgnoreHitsCountdown[slot],
                    state.frame);
            state.updated = maskedMimicUpdate.entity();
            state.runtime.entityOptions1Override[slot] = maskedMimicUpdate.options1();
            state.preserveMaskedMimicPresentation = true;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_CRYSTAL_SWITCH,
        EntityTickStage.PHASE_2_CRYSTAL_SWITCH,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            // CrystalSwitchEntityHandler writes $FF before dispatching
            // its normal collision helper, so sword damage can never
            // turn this entity into a dying state.
            state.runtime.enemyHealth[state.entity.slot()] = 0xFF;
            if (state.runtime.enemyFlashCountdown[state.entity.slot()] > 0) {
              // The handler consumes the collision flash as its trigger.
              state.runtime.enemyFlashCountdown[state.entity.slot()] = 0;
              if (!state.runtime.switchBlockAnimationActive) {
                state.runtime.enemyTransitionCountdown[state.entity.slot()] = 0x18;
                state.runtime.pendingSwitchBlockAnimationRequest = true;
                state.runtime.pendingEntityEvents.add(
                    new EntityCombatEvent(
                        state.entity.slot(),
                        state.entity.type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.WAVE,
                        0x0E));
              }
            }
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_LEEVER,
        EntityTickStage.PHASE_2_LEEVER,
        state -> {
          if (state.wasInitializing) {
            // EntityInitLeever calls SetEntitySpriteVariant($FF) before
            // the first active handler dispatch.
            state.updated = withVariant(state.entity, -1);
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_BOMBER,
        EntityTickStage.PHASE_2_BOMBER,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            BomberMotion.Update bomberUpdate =
                state.runtime.bomberMotion.advance(
                    state.entity,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.linkEntityX,
                    state.linkEntityY,
                    state.romLinkDirection,
                    state.swordCollisionActive,
                    state.randomByteSupplier,
                    state.backgroundCollision);
            state.updated = bomberUpdate.entity();
            state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                bomberUpdate.transitionCountdown();
            if (bomberUpdate.bombSpawn() != null) {
              BomberMotion.BombSpawn spawn = bomberUpdate.bombSpawn();
              int bombSlot =
                  state.runtime.spawnEnemyBomb(
                      spawn.x(),
                      spawn.y(),
                      spawn.z(),
                      spawn.transitionCountdown(),
                      spawn.speedX(),
                      spawn.speedY(),
                      spawn.speedZ());
              if (bombSlot >= 0 && bomberUpdate.playJingle()) {
                state.runtime.pendingEntityEvents.add(
                    new EntityCombatEvent(
                        bombSlot,
                        state.runtime.ENTITY_BOMB,
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        state.runtime.BOMBER_THROW_JINGLE_ID));
              }
            }
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MAD_BOMBER,
        EntityTickStage.PHASE_2_MAD_BOMBER,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            // MadBomberEntityHandler rewrites this handler-owned drop
            // byte on every interactive pass before its state dispatch.
            state.runtime.droppedItemBySlot[state.entity.slot()] = 0x3C;
            MadBomberMotion.Update madBomberUpdate =
                state.runtime.madBomberMotion.advance(
                    state.entity,
                    state.runtime.enemyTransitionCountdown[state.entity.slot()],
                    state.linkEntityX,
                    state.linkEntityY,
                    state.runtime.enemyFlashCountdown[state.entity.slot()],
                    state.randomByteSupplier);
            state.updated = withVariant(madBomberUpdate.entity(), madBomberUpdate.spriteVariant());
            state.preserveMadBomberPresentation = true;
            state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                madBomberUpdate.transitionCountdown();
            if (madBomberUpdate.bombSpawn() != null) {
              MadBomberMotion.BombSpawn spawn = madBomberUpdate.bombSpawn();
              int bombSlot =
                  state.runtime.spawnEnemyBomb(
                      spawn.x(),
                      spawn.y(),
                      spawn.z(),
                      spawn.transitionCountdown(),
                      spawn.speedX(),
                      spawn.speedY(),
                      spawn.speedZ());
              if (bombSlot >= 0 && madBomberUpdate.playJingle()) {
                state.runtime.pendingEntityEvents.add(
                    new EntityCombatEvent(
                        bombSlot,
                        state.runtime.ENTITY_BOMB,
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        state.runtime.BOMBER_THROW_JINGLE_ID));
              }
            }
          }
          return true;
        });
    for (int type = 0; type < 256; type++) {
      if ((type == RoomEntityRuntime.ENTITY_SIDE_VIEW_POT)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_SIDE_VIEW_POT,
            state -> {
              if (state.runtime.enemyTransitionCountdown[state.entity.slot()] != 0) {
                state.runtime.slots[state.index] = state.lifted;
                return false;
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_ROOSTER))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_ROOSTER,
            state -> {
              {
                RoosterMotion.Update roosterUpdate =
                    RoosterMotion.advanceLifted(
                        state.frame,
                        state.linkZ & 0xFF,
                        state.runtime.linkPressedButtonsMask,
                        state.romCollisionType,
                        state.runtime.currentLinkSpeedX,
                        state.runtime.currentLinkSpeedY,
                        state.romLinkDirection);
                state.lifted = withVariant(state.lifted, roosterUpdate.spriteVariant());
                state.runtime.liftedSourceDirection[state.entity.slot()] =
                    roosterUpdate.romDirection();
                state.liftedRomDirection = roosterUpdate.romDirection();
                state.runtime.pendingRoosterLinkStateRequests.add(
                    new RoosterLinkStateRequest(
                        state.entity.slot(),
                        roosterUpdate.positionZ(),
                        roosterUpdate.velocityZ(),
                        roosterUpdate.speedX(),
                        roosterUpdate.speedY(),
                        roosterUpdate.romDirection()));
                state.runtime.projectiles.playBoomerangSoundIfDue(state.entity);
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_BOMB))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_BOMB,
            state -> {
              {
                BombMotion.Decision bombDecision =
                    state.runtime.bombInteractions.advanceBombEntity(
                        state.index,
                        state.entity,
                        state.projectileEvents,
                        state.linkEntityX,
                        state.linkEntityY,
                        state.projectileLinkState);
                state.entity = state.runtime.slots[state.index];
                if (bombDecision.unloadAfterPresentation()) {
                  return false;
                }
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((ColorShellMotion.isColorShellType(type))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_COLOR_SHELL_TYPE,
            state -> {
              if (state.runtime.entityMapId == state.runtime.MAP_COLOR_DUNGEON) {
                state.runtime.colorShellMotion.enterState6(state.entity);
                RoomEntity activeShell =
                    state.runtime.slotProperties.refreshColorShellDisplay(
                        withStatus(state.entity, EntityStatus.ACTIVE), EntityStatus.ACTIVE);
                state.runtime.slots[state.index] = activeShell;
                return false;
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_MOBLIN_KING))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_MOBLIN_KING,
            state -> {
              {
                state.runtime.bossRewards.advanceMoblinKingDestruction(
                    state.entity, state.frame, state.randomByteSupplier);
                return false;
              }
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((type == RoomEntityRuntime.ENTITY_GENIE)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_GENIE,
            state -> {
              if (state.runtime.geniePrivateState1[state.entity.slot()] == 1) {
                state.runtime.genie.advanceGenieDestruction(
                    state.entity, state.frame, state.randomByteSupplier);
                return false;
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_ROLLING_BONES))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_ROLLING_BONES,
            state -> {
              {
                state.runtime.bossRewards.advanceRollingBonesDestruction(
                    state.entity, state.randomByteSupplier);
                return false;
              }
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_MOLDORM))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_MOLDORM_2,
            state -> {
              {
                state.runtime.bossRewards.advanceMoldormDestruction(state.entity, state.frame);
                return false;
              }
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_BOMB))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_BOMB_2,
            state -> {
              {
                BombMotion.Decision bombDecision =
                    state.runtime.bombInteractions.advanceBombEntity(
                        state.index,
                        state.entity,
                        state.projectileEvents,
                        state.linkEntityX,
                        state.linkEntityY,
                        state.projectileLinkState);
                state.entity = state.runtime.slots[state.index];
                if (bombDecision.unloadAfterPresentation()) {
                  return false;
                }
                state.entity =
                    state.runtime.bombInteractions.applyBombStunnedPostActiveWork(
                        state.entity, state.backgroundCollision, state.frame);
                state.runtime.slots[state.index] = state.entity;
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((type == RoomEntityRuntime.ENTITY_HIDING_SLIME_KEY)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_HIDING_SLIME_KEY_3,
            state -> {
              if ((state.runtime.entityRoomStatus & 0x10) != 0) {
                // The handler's first instruction is the completed-room
                // gate, including the INIT dispatch path.
                state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                return false;
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_OCTOROK || type == RoomEntityRuntime.ENTITY_MOBLIN))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_MOBLIN,
            state -> {
              {
                state.runtime.roamingEnemyMotion.initialize(state.entity.slot());
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_MOBLIN_SWORD))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_MOBLIN_SWORD,
            state -> {
              {
                state.runtime.moblinSwordMotion.initialize(state.entity.slot(), state.entity.x());
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if (((isSparkType(type)))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_SPARK_TYPE,
            state -> {
              {
                state.runtime.sparkMotion.initialize(state.entity.slot(), state.entity.type());
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if (((isZolGelType(type)))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_ZOL_GEL_TYPE,
            state -> {
              {
                state.runtime.zolGelMotion.initialize(
                    state.entity.slot(), state.entity.type(), state.randomByteSupplier);
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_MINI_MOLDORM))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_MINI_MOLDORM_2,
            state -> {
              {
                state.runtime.miniMoldormMotion.initialize(state.entity);
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_MOLDORM))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_MOLDORM_3,
            state -> {
              {
                state.runtime.moldormMotion.initialize(state.entity);
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_WIZROBE))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_WIZROBE_2,
            state -> {
              {
                state.runtime.wizrobeMotion.initializeFromRoom(
                    state.entity.slot(), state.entity.spriteVariant());
                state.runtime.enemyTransitionCountdown[state.entity.slot()] = 0x80;
                state.preserveWizrobePresentation = true;
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_WIZROBE_PROJECTILE))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_WIZROBE_PROJECTILE,
            state -> {
              {
                state.runtime.wizrobeProjectileMotion.initialize(
                    state.entity.slot(), state.randomByteSupplier);
                state.preserveWizrobeProjectilePresentation = true;
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if (((isGhiniType(type)))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_GHINI_TYPE,
            state -> {
              {
                state.runtime.ghiniMotion.initialize(state.entity.slot(), state.entity.type());
                if (isHidingGhiniType(state.entity.type())) {
                  state.preserveGhiniPresentation = true;
                }
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if (((isBombiteType(type)))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_BOMBITE_TYPE,
            state -> {
              {
                state.runtime.bombiteMotion.initialize(state.entity.slot());
                state.runtime.slowTimerInitialized[state.entity.slot()] = true;
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if (((isFollowingNpcType(type)))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_FOLLOWING_NPC_TYPE,
            state -> {
              {
                if (state.entity.type() == state.runtime.ENTITY_BOW_WOW) {
                  state.runtime.bowWowMotion.initialize(state.entity.slot());
                } else {
                  state.runtime.followingNpcMotion.initialize(
                      state.entity.slot(), state.entity.type());
                }
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if (((ColorShellMotion.isColorShellType(type)))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_COLOR_SHELL_TYPE_2,
            state -> {
              {
                state.runtime.colorShellMotion.initialize(state.entity.slot());
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((type == SideViewPlatformMotion.ENTITY_TYPE)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_TYPE,
            state -> {
              if (state.status == EntityStatus.ACTIVE
                  && !state.wasInitializing
                 
                  && state.handlerLinkCollisionEnabled
                  && state.runtime.backgroundMechanics.entityTimersInteractive()
                  && !state.creditsGameplay
                  && state.runtime.transitionSequenceCounter == 0x04) {
                SideViewPlatformMotion.Frame platformFrame =
                    state.runtime.sideViewPlatformMotion.beginFrame(state.entity);
                RoomEntity movedPlatform = platformFrame.entity();
                if (platformFrame.horizontalDelta() != 0
                    && state.backgroundCollision != null
                    && state.backgroundCollision.blocks(
                        state.entity,
                        platformFrame.horizontalDelta() < 0 ? 1 : 0,
                        movedPlatform.x(),
                        movedPlatform.y())) {
                  platformFrame =
                      state.runtime.sideViewPlatformMotion.restoreHorizontalPosition(
                          state.entity, platformFrame);
                  movedPlatform = platformFrame.entity();
                }
                if (platformFrame.verticalDelta() != 0 && state.backgroundCollision != null) {
                  int direction = platformFrame.verticalDelta() < 0 ? 2 : 3;
                  if (state.backgroundCollision.blocks(
                      state.entity, direction, movedPlatform.x(), movedPlatform.y())) {
                    platformFrame =
                        state.runtime.sideViewPlatformMotion.restoreVerticalPosition(
                            state.entity, platformFrame);
                    movedPlatform = platformFrame.entity();
                  }
                }
                boolean standing =
                    state.runtime.groundInteractionSideScrolling
                        && (state.linkZ & 0xFF) == 0
                        && (state.runtime.currentLinkSpeedY & 0x80) == 0
                        && sideViewPlatformOverlapsLink(
                            movedPlatform, state.linkEntityX, state.linkEntityY)
                        && (((state.linkEntityY - movedPlatform.y() + 0x08) & 0x80) != 0);
                boolean activationAllowed =
                    state.runtime.entityRoomId == 0x3B || state.carryingEntity;
                if (standing) {
                  state.runtime.pendingSideViewPlatformLinkRequests.add(
                      new SideViewPlatformLinkRequest(
                          state.entity.slot(),
                          platformFrame.horizontalDelta(),
                          (movedPlatform.y() - 0x10) & 0xFF,
                          0x02,
                          true,
                          activationAllowed));
                }
                SideViewPlatformMotion.Update platformUpdate =
                    state.runtime.sideViewPlatformMotion.finishFrame(
                        movedPlatform, platformFrame, state.frame, standing, activationAllowed);
                state.updated = platformUpdate.entity();
                if (platformUpdate.rumble()) {
                  state.runtime.pendingEntityEvents.add(
                      new EntityCombatEvent(
                          state.entity.slot(),
                          state.entity.type(),
                          0,
                          false,
                          EntityCombatEvent.SoundChannel.NOISE,
                          0x11));
                }
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((isCommonDroppableType(type))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_COMMON_DROPPABLE_TYPE,
            state -> {
              if (state.wasInitializing
                 
                  && state.runtime.droppablePrivateState3[state.entity.slot()] != 0) {
                // The init handler has already set the hidden options. The
                // corresponding active handler will be entered on the next
                // entity tick, so suppress this first display-list pass too.
                state.updated = withVariant(state.entity, -1);
                state.preserveDroppablePresentation = true;
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((isCommonDroppableType(type))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_COMMON_DROPPABLE_TYPE_2,
            state -> {
              if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
                CommonDroppableUpdate droppableUpdate =
                    state.runtime.pickups.advanceCommonDroppable(
                        state.entity,
                        state.linkEntityX,
                        state.linkEntityY,
                        state.runtime.objectQuery == null
                            ? null
                            : state.runtime.objectQuery.sample(state.entity));
                state.updated = droppableUpdate.entity();
                if (droppableUpdate.hidden()) {
                  // DroppableRevealOrReturnIfNeeded returns past the caller's
                  // remaining handler. In particular, no rendering, pickup,
                  // movement, or background interaction may run while hidden.
                  state.runtime.slots[state.index] =
                      withStatus(withVariant(state.updated, -1), state.status);
                  return false;
                }
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_ROLLING_BONES
          || type == RoomEntityRuntime.ENTITY_ROLLING_BONES_BAR))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_ROLLING_BONES_BAR,
            state -> {
              if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
                // BossIntro runs before ReturnIfNonInteractive_06.
                if (state.entity.type() == state.runtime.ENTITY_ROLLING_BONES
                    && !state.runtime.roomTransitionActive
                    && !state.runtime.inventoryAppearing) {
                  BossIntroMotion.Update intro =
                      state.runtime.bossIntroMotion.advance(
                          state.runtime.options1(state.entity.slot()),
                          state.entity.type(),
                          state.runtime.entityMapId,
                          state.runtime.transitionSequenceCounter);
                  if (intro.musicTrack() >= 0) state.runtime.pendingMusicTrack = intro.musicTrack();
                }
                // The bar's room-clear check precedes its interaction guard.
                if (state.entity.type() == state.runtime.ENTITY_ROLLING_BONES_BAR
                    && (state.runtime.entityRoomStatus & 0x20) != 0) {
                  state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
                  return false;
                }
                // ReturnIfNonInteractive_06 reads the pre-decrement wC1A8.
                if (!state.handlerInteractive || state.countdownBlocksInteraction) return false;
                if (state.entity.type() == state.runtime.ENTITY_ROLLING_BONES
                    && !state.rollingBonesHitThisFrame
                    && state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()] != 0) {
                  // ApplyRecoilIfNeeded_06 pops the handler return address:
                  // no jump physics or AI movement occurs during recoil.
                  state.runtime.slots[state.index] =
                      state
                          .runtime
                          .backgroundMechanics
                          .applyEnemyRecoilIfNeeded(state.entity, state.backgroundCollision)
                          .entity();
                  return false;
                }
              }
              return true;
            });
      }
    }
    for (int type = 0; type < 256; type++) {
      if ((isSparkType(type))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_SPARK_TYPE_2,
            state -> {
              if (state.wasInitializing) {
                state.updated = state.runtime.sparkMotion.applyInitializationOffset(state.entity);
              }
              return true;
            });
      }
    }
  }
}
