package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.Arrays;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BowWowMotion;

/** Ordered slot dispatch phase 4; false preserves the ROM handler early return. */
final class EntitySlotPhase4 {
    static boolean run(EntityFrameExecution state) {

        if (!state.runtime.entityHandlers.tick(EntityTickStage.PHASE_4_BEFORE_FOLLOWER, state)) return false;
        if (state.status == EntityStatus.ACTIVE
                && !state.wasInitializing
                && (isDynamicFollowingNpc(state.entity) || state.entity.type() == EntityRomConstants.ENTITY_BOW_WOW)) {
            if (state.entity.type() == EntityRomConstants.ENTITY_BOW_WOW) {
                BowWowMotion.Update bowWowUpdate = state.runtime
                        .entityState
                        .motions
                        .get(BowWowMotion.class, BowWowMotion::new)
                        .advanceAfterTimers(
                                state.entity,
                                state.frame,
                                state.linkEntityX,
                                state.linkEntityY,
                                state.runtime.entityState.linkState.followingLinkZ,
                                state.randomByteSupplier,
                                state.backgroundCollision,
                                Arrays.asList(state.runtime.slots),
                                state.runtime.enemyCombatTables == null
                                        ? type -> false
                                        : state.runtime.enemyCombatTables::canBowWowEatEntity,
                                state.runtime
                                        .entityState
                                        .motions
                                        .get(BowWowMotion.class, BowWowMotion::new)
                                        .readBowWowState());
                state.updated = bowWowUpdate.entity();
                if (bowWowUpdate.targetContact() != null) {
                    state.runtime.lifecycle.handleBowWowTargetContact(
                            state.entity, bowWowUpdate.targetContact(), state.randomByteSupplier);
                }
            } else {
                state.updated = state.runtime
                        .entityState
                        .motions
                        .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                        .advance(
                                state.entity,
                                state.frame,
                                state.linkEntityX,
                                state.linkEntityY,
                                state.runtime.entityState.linkState.followingLinkZ,
                                state.runtime.entityState.linkState.followingLinkDirection,
                                state.runtime.entityState.linkState.followingEntityYOffset,
                                state.runtime
                                        .entityState
                                        .motions
                                        .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                                        .readFollowingLinkPositionHistory(),
                                state.backgroundCollision);
            }
        }
        return true;
    }
}
