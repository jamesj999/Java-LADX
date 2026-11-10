package linksawakening.world.entity;

import linksawakening.world.entity.event.EntityCombatEvent;

import linksawakening.world.entity.motion.EnemyRecoilMotion;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;
import static linksawakening.world.entity.RoomEntityRuntime.*;

/** Ordered slot dispatch phase 2; false preserves the ROM handler early return. */
final class EntitySlotPhase2 {
  static boolean run(EntityFrameExecution state) {
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_SLIME_EYE, state)) return false;

    if (state.status == EntityStatus.LIFTED) {
      state.lifted = state.runtime.liftThrow.renderLiftedEntity(state.entity, state.frame);
      // Side-view pot pickup enters status LIFTED with the ROM's
      // transition countdown set to $02. UpdateEntityTimers has
      // already decremented it for this frame; EntityLiftedHandler
      // does not advance the pot until that countdown reaches zero.
      // Other state.lifted entities retain their established generic
      // presentation timing here.
      if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_SIDE_VIEW_POT, state))
        return false;
      state.liftedRomDirection = state.romLinkDirection;
      if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_ROOSTER, state)) return false;
      state.runtime.slots[state.index] =
          state.runtime.liftThrow.advanceLiftedEntity(
              state.lifted,
              state.linkEntityX,
              state.linkEntityY,
              state.linkZ,
              state.liftedRomDirection);
      return false;
    } else if (state.status == EntityStatus.THROWN) {
      if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_BOMB, state)) return false;
      RoomEntity thrown =
          state.runtime.liftThrow.advanceThrownEntity(
              state.entity,
              state.backgroundCollision,
              state.runtime.groundInteractionSideScrolling,
              state.romLinkDirection,
              state.frame);
      state.runtime.slots[state.index] = thrown;
      if (thrown.status() == EntityStatus.THROWN) {
        state.runtime.liftThrow.collideThrownEntityWithEntities(thrown, state.frame);
      }
      return false;
    } else if (state.status == EntityStatus.FALLING) {
      if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_COLOR_SHELL_TYPE, state))
        return false;
      RoomEntity falling = state.runtime.backgroundMechanics.advanceFallingEntity(state.entity);
      if (falling == null) {
        state.runtime.lifecycle.disableEntityWithoutPersistence(state.entity.slot());
        return false;
      }
      int renderFlipAttribute = state.runtime.baseEntityFlipAttribute[state.entity.slot()];
      if (state.runtime.enemyFlashCountdown[state.entity.slot()] > 0) {
        renderFlipAttribute ^= (state.runtime.enemyFlashCountdown[state.entity.slot()] << 2) & 0x10;
      }
      state.runtime.slots[state.index] =
          new RoomEntity(
              falling.slot(),
              falling.sourceLoadOrder(),
              falling.type(),
              falling.x(),
              falling.y(),
              EntityStatus.FALLING,
              falling.spriteDefinition(),
              falling.spriteVariant(),
              renderFlipAttribute,
              falling.spriteTileOffset(),
              falling.z(),
              -1,
              false);
      return false;
    } else if (state.status == EntityStatus.DYING) {
      if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_MOBLIN_KING, state))
        return false;
      if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_GENIE, state)) return false;
      if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_ROLLING_BONES, state))
        return false;
      if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_MOLDORM_2, state))
        return false;
      if (isBossKeyDropProducer(state.entity.type())) {
        state.runtime.bossRewards.advanceBossKeyDropProducer(
            state.entity, state.randomByteSupplier);
        return false;
      }
      if (state.runtime.dyingCountdown[state.entity.slot()] == 0) {
        state.runtime.bossRewards.handleTerminalEnemyDeath(state.entity, state.randomByteSupplier);
      } else {
        int slot = state.entity.slot();
        int presentationCountdown = state.runtime.dyingCountdown[slot];
        // EntityDeathHandler ($03:557D): the initial counter is a
        // ceiling, not a fixed wait. Begin destruction once the
        // hit recoil has expired; bosses use their handlers above.
        if (presentationCountdown >= 0x20 && state.runtime.enemyIgnoreHitsCountdown[slot] == 0) {
          state.runtime.dyingCountdown[slot] = 0x1F;
          state.runtime.events.pendingEntityEvents.add(
              new EntityCombatEvent(
                  slot, state.entity.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13));
        }
        RoomEntity updated =
            state.runtime.enemyIgnoreHitsCountdown[slot] > 0
                ? state
                    .runtime
                    .backgroundMechanics
                    .applyEnemyRecoilIfNeeded(state.entity, state.backgroundCollision)
                    .entity()
                : state.entity;
        updated = state.runtime.slotProperties.refreshColorShellDisplay(updated, state.status);
        state.runtime.slots[state.index] =
            withDeathPresentation(
                updated,
                EntityMotionMath.deathSpriteVariantForCountdown(presentationCountdown),
                state.runtime.powerRecoilDeath[slot]);
      }
      return false;
    } else if (state.status == EntityStatus.BURNING) {
      if (state.runtime.enemyTransitionCountdown[state.entity.slot()] == 0) {
        state.runtime.lifecycle.finishBurning(state.entity);
      } else {
        state.runtime.slots[state.index] =
            state.runtime.slotProperties.refreshColorShellDisplay(state.entity, state.status);
      }
      return false;
    } else if (state.status == EntityStatus.STUNNED) {
      if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_BOMB_2, state)) return false;
      if (state.runtime.linkState.powerBraceletButtonHeld
          && isLiftableEntity(state.entity)
          && (state.entity.type() == state.runtime.ENTITY_BOMB
              || (state.runtime.enemyPhysicsFlags[state.entity.slot()]
                      & state.runtime.ENTITY_PHYSICS_GRABBABLE)
                  != 0)
          && RoomEntityPickupRules.overlapsLink(
              state.entity, state.linkEntityX, state.linkEntityY)) {
        if (state.runtime.beginLift(state.entity.slot(), state.romLinkDirection)) {
          state.runtime.slots[state.index] =
              state.runtime.liftThrow.advanceLiftedEntity(
                  state.runtime.liftThrow.renderLiftedBomb(state.runtime.slots[state.index]),
                  state.linkEntityX,
                  state.linkEntityY,
                  state.linkZ,
                  state.romLinkDirection);
        }
        return false;
      }
      if (state.runtime.enemyStunnedCountdown[state.entity.slot()] == 0) {
        state.runtime.slots[state.index] =
            state.runtime.slotProperties.refreshColorShellDisplay(
                withStatus(state.entity, EntityStatus.ACTIVE), EntityStatus.ACTIVE);
      } else {
        state.runtime.slots[state.index] =
            state.runtime.slotProperties.refreshColorShellDisplay(state.entity, state.status);
      }
      return false;
    } else if (state.status == EntityStatus.INIT) {
      if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_HIDING_SLIME_KEY_3, state))
        return false;
      if (!state.runtime.initializeBossBattle(state.entity)) return false;
      state.status = EntityStatus.ACTIVE;
      state.runtime.pickups.initializeEntityTimers(state.entity);
      state.runtime.entityHandlers.initialize(state.entity, state.randomByteSupplier);

      if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_MOBLIN, state)) return false;

      if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_MOBLIN_SWORD, state))
        return false;

      if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_SPARK_TYPE, state))
        return false;
      if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_ZOL_GEL_TYPE, state))
        return false;

      if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_MINI_MOLDORM_2, state))
        return false;
      if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_MOLDORM_3, state))
        return false;

      if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_WIZROBE_2, state))
        return false;
      if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_WIZROBE_PROJECTILE, state))
        return false;

      if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_GHINI_TYPE, state))
        return false;

      if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_BOMBITE_TYPE, state))
        return false;
      if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_FOLLOWING_NPC_TYPE, state))
        return false;
      if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_COLOR_SHELL_TYPE_2, state))
        return false;
    } else if (state.status == EntityStatus.ACTIVE) {
      if (!state.freezeTarinTimers && !state.freezeMoblinKingTimers) {
        state.runtime.lifecycle.decrementSlowTransitionCountdown(state.entity.slot(), state.frame);
      }
      if (state.runtime.lifecycle.shouldDisappear(state.entity)) {
        if (state.runtime.slowTransitionCountdown[state.entity.slot()] == 0) {
          state.runtime.clearEntity(state.entity.slot());
          return false;
        }
      }
    }
    state.updated = state.entity;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_TYPE, state)) return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_CHEST_WITH_ITEM, state))
      return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_COMMON_DROPPABLE_TYPE, state))
      return false;
    if (state.preserveGhiniPresentation) {
      state.updated = withVariant(state.entity, -1);
    }
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_ZOMBIE, state)) return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_CHEST_WITH_ITEM_2, state))
      return false;
    if (!state.runtime.entityHandlers.tick(
        EntityTickStage.PHASE_2_DROPPABLE_SECRET_SEASHELL, state)) return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_HIDING_SLIME_KEY, state))
      return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_COMMON_DROPPABLE_TYPE_2, state))
      return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_HIDING_SLIME_KEY_2, state))
      return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_KEY_DROP_POINT, state))
      return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_WIZROBE, state)) return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_POLS_VOICE, state)) return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_ROLLING_BONES_BAR, state))
      return false;
    if (state.status == EntityStatus.ACTIVE
        && !state.wasInitializing
        && (usesSharedRecoil(state.entity.type())
            || (state.entity.type() == state.runtime.ENTITY_POKEY
                && state.runtime.pokeyMotion.isSegment(state.entity.slot())
                && state.handlerLinkCollisionEnabled))
        && state.entity.type() != state.runtime.ENTITY_ROLLING_BONES
        && state.entity.type() != state.runtime.ENTITY_MINI_MOLDORM
        && state.entity.type() != state.runtime.ENTITY_BOO_BUDDY
        && (state.entity.type() != state.runtime.ENTITY_MASKED_MIMIC_GORIYA
            || state.runtime.entityMapId != 0x1F)
        && ((state.entity.type() != state.runtime.ENTITY_STAR
                && state.entity.type() != state.runtime.ENTITY_BLOOPER)
            || state.handlerLinkCollisionEnabled)
        && ((state.entity.type() != state.runtime.ENTITY_ZOMBIE
                && state.entity.type() != state.runtime.ENTITY_BUZZ_BLOB
                && state.entity.type() != state.runtime.ENTITY_SAND_CRAB)
            || state.handlerLinkCollisionEnabled)) {
      // Bank-$03 AnimateRoamingEnemy and the bank-$04/$06/$07
      // handlers apply the shared recoil before their own movement.
      EnemyRecoilMotion.Update recoil =
          state.runtime.backgroundMechanics.applyEnemyRecoilIfNeeded(
              state.entity, state.backgroundCollision);
      state.entity = recoil.entity();
      state.updated = state.entity;
    }
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_MINI_MOLDORM, state))
      return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_MOLDORM, state)) return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_MIMIC, state)) return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_MASKED_MIMIC_GORIYA, state))
      return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_CRYSTAL_SWITCH, state))
      return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_LEEVER, state)) return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_SPARK_TYPE_2, state))
      return false;
    if (state.status == EntityStatus.ACTIVE
        && !state.wasInitializing
        && state.runtime.entityHandlers.hasAdvance(state.entity.type())) {
      state.updated =
          state.runtime.entityHandlers.advance(
              state.entity,
              state.context.withRuntimeInputs(
                  state.frame, state.linkEntityX, state.linkEntityY, state.backgroundCollision));
      if (state.entity.type() == state.runtime.ENTITY_LEEVER
          && state.handlerInteractive && state.handlerLinkCollisionEnabled) {
        state.runtime.finishLeeverContactRecoil(state.updated);
      }
    }

    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_BOMBER, state)) return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_2_MAD_BOMBER, state)) return false;
    return true;
  }
}
