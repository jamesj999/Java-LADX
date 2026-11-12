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
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (!state.runtime().entityHandlers.tick(EntityTickStage.PHASE_4_BEFORE_FOLLOWER, state)) return false;
        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && (isDynamicFollowingNpc(slotState.entity())
                        || slotState.entity().type() == EntityRomConstants.ENTITY_BOW_WOW)) {
            if (slotState.entity().type() == EntityRomConstants.ENTITY_BOW_WOW) {
                BowWowMotion.Update bowWowUpdate = state.runtime()
                        .entityState
                        .motions
                        .get(BowWowMotion.class, BowWowMotion::new)
                        .advanceAfterTimers(
                                slotState.entity(),
                                frameInput.frame(),
                                frameInput.linkEntityX(),
                                frameInput.linkEntityY(),
                                state.runtime().entityState.linkState.followingLinkZ,
                                frameInput.randomByteSupplier(),
                                frameInput.backgroundCollision(),
                                Arrays.asList(state.runtime().slots),
                                state.runtime().enemyCombatTables == null
                                        ? type -> false
                                        : state.runtime().enemyCombatTables::canBowWowEatEntity,
                                state.runtime()
                                        .entityState
                                        .motions
                                        .get(BowWowMotion.class, BowWowMotion::new)
                                        .readBowWowState());
                slotState.updated(bowWowUpdate.entity());
                if (bowWowUpdate.targetContact() != null) {
                    state.runtime()
                            .lifecycle
                            .handleBowWowTargetContact(
                                    slotState.entity(), bowWowUpdate.targetContact(), frameInput.randomByteSupplier());
                }
            } else {
                slotState.updated(state.runtime()
                        .entityState
                        .motions
                        .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                        .advance(
                                slotState.entity(),
                                frameInput.frame(),
                                frameInput.linkEntityX(),
                                frameInput.linkEntityY(),
                                state.runtime().entityState.linkState.followingLinkZ,
                                state.runtime().entityState.linkState.followingLinkDirection,
                                state.runtime().entityState.linkState.followingEntityYOffset,
                                state.runtime()
                                        .entityState
                                        .motions
                                        .get(FollowingNpcMotion.class, FollowingNpcMotion::new)
                                        .readFollowingLinkPositionHistory(),
                                frameInput.backgroundCollision()));
            }
        }
        return true;
    }
}
