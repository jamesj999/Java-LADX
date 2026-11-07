package linksawakening.world.entity;

import linksawakening.world.entity.motion.BowWowMotion;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;
import static linksawakening.world.entity.RoomEntityRuntime.*;

import java.util.Arrays;

/** Ordered slot dispatch phase 4; false preserves the ROM handler early return. */
final class EntitySlotPhase4 {
  static boolean run(EntityFrameExecution state) {

    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_4_GENIE, state)) return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_4_ARMOS_KNIGHT, state))
      return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_4_HINOX, state)) return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_4_ROLLING_BONES, state))
      return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_4_ROLLING_BONES_BAR, state))
      return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_4_MOBLIN_KING, state))
      return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_4_GOPONGA_FLOWER, state))
      return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_4_GIANT_GOPONGA_FLOWER, state))
      return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_4_POKEY, state)) return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_4_PIRANHA_PLANT, state))
      return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_4_ZORA, state)) return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_4_ZOMBIE, state)) return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_4_BUZZ_BLOB, state)) return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_4_SAND_CRAB, state)) return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_4_DOG, state)) return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_4_TARIN, state)) return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_4_WITCH, state)) return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_4_URCHIN, state)) return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_4_WITCH_RAT, state)) return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_4_GHINI_TYPE, state)) return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_4_HARDHAT_BEETLE, state))
      return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_4_POLS_VOICE, state)) return false;
    if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_4_SPIKED_BEETLE, state))
      return false;
    if (state.status == EntityStatus.ACTIVE
        && !state.wasInitializing
        && (isDynamicFollowingNpc(state.entity)
            || state.entity.type() == state.runtime.ENTITY_BOW_WOW)) {
      if (state.entity.type() == state.runtime.ENTITY_BOW_WOW) {
        BowWowMotion.Update bowWowUpdate =
            state.runtime.bowWowMotion.advanceAfterTimers(
                state.entity,
                state.frame,
                state.linkEntityX,
                state.linkEntityY,
                state.runtime.followingLinkZ,
                state.randomByteSupplier,
                state.backgroundCollision,
                Arrays.asList(state.runtime.slots),
                state.runtime.enemyCombatTables == null
                    ? type -> false
                    : state.runtime.enemyCombatTables::canBowWowEatEntity,
                state.runtime.bowWowState);
        state.updated = bowWowUpdate.entity();
        if (bowWowUpdate.targetContact() != null) {
          state.runtime.lifecycle.handleBowWowTargetContact(
              state.entity, bowWowUpdate.targetContact(), state.randomByteSupplier);
        }
      } else {
        state.updated =
            state.runtime.followingNpcMotion.advance(
                state.entity,
                state.frame,
                state.linkEntityX,
                state.linkEntityY,
                state.runtime.followingLinkZ,
                state.runtime.followingLinkDirection,
                state.runtime.followingEntityYOffset,
                state.runtime.followingLinkPositionHistory,
                state.backgroundCollision);
      }
    }
    return true;
  }
}
