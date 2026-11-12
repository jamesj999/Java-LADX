package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_ROLLING_BONES;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_ROLLING_BONES_BAR;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.RollingBonesMotion;

/** ROM handler stages for RollingBones. */
final class RollingBonesHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_ROLLING_BONES, EntityTickStage.PHASE_2_DYING, 2, RollingBonesHandler::phase2RollingBones);
        registry.registerTick(
                ENTITY_ROLLING_BONES,
                EntityTickStage.PHASE_4_BEFORE_FOLLOWER,
                3,
                RollingBonesHandler::phase4RollingBones);
    }

    private static boolean phase2RollingBones(EntityFrameExecution state) {
        state.bossRewards().advanceRollingBonesDestruction(state.entity, state.randomByteSupplier);
        return false;
    }

    private static boolean phase4RollingBones(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            RoomEntity bar = state.roomInspection().firstLoadedEntityOfType(ENTITY_ROLLING_BONES_BAR);
            if (bar != null) {
                RollingBonesMotion.BossUpdate bossUpdate = state.rollingBonesMotion()
                        .advanceBoss(
                                state.entity,
                                bar,
                                state.slot().transitionCountdown(state.entity.slot()),
                                state.slot().health(state.entity.slot()),
                                state.backgroundCollision);
                state.updated = bossUpdate.entity();
                state.slot().setTransitionCountdown(state.entity.slot(), bossUpdate.transitionCountdown());
                state.preserveRollingBonesPresentation = true;
                if (bossUpdate.jingleId() >= 0) {
                    state.emit(new EntityCombatEvent(
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
    }
}
