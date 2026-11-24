package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_SPARK_CLOCKWISE;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_SPARK_COUNTER_CLOCKWISE;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.SparkMotion;

/** ROM handler stages for SparkType. */
final class SparkTypeHandler {
    static void install(EntityHandlerRegistry registry) {
        for (int type : new int[] {ENTITY_SPARK_COUNTER_CLOCKWISE, ENTITY_SPARK_CLOCKWISE}) {
            registry.registerTick(type, EntityTickStage.PHASE_1_AFTER_STATUS_TIMERS, 0, SparkTypeHandler::tickTimer);
            registry.registerTick(type, EntityTickStage.PHASE_2_AFTER_INITIALIZE, 2, SparkTypeHandler::initialize);
            registry.registerTick(
                    type, EntityTickStage.PHASE_2_AFTER_RECOIL_APPLICATION, 6, SparkTypeHandler::applyOffset);
            registry.registerTick(type, EntityTickStage.PHASE_3_BEFORE_LASER, 4, SparkTypeHandler::advance);
        }
    }

    private static boolean tickTimer(EntityFrameExecution state) {
        state.motion(SparkMotion.class, SparkMotion::new)
                .tickTimer(state.slotFrame().index());
        return true;
    }

    private static boolean initialize(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        state.motion(SparkMotion.class, SparkMotion::new)
                .initialize(slotState.entity().slot(), slotState.entity().type());

        return true;
    }

    private static boolean applyOffset(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if (slotState.wasInitializing()) {
            slotState.updated(
                    state.motion(SparkMotion.class, SparkMotion::new).applyInitializationOffset(slotState.entity()));
        }
        return true;
    }

    private static boolean advance(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            // $06:6643 renders before either non-interaction or incoming recoil returns.
            var entity = slotState.updated();
            if (state.gates().handlerInteractive()
                    && !state.gates().countdownBlocksInteraction()
                    && slotState.ignoreHitsBeforeRecoil() == 0) {
                entity = state.motion(SparkMotion.class, SparkMotion::new)
                        .advance(entity, frameInput.backgroundCollision());
            }
            slotState.updated(EntitySnapshots.withVariant(entity, (frameInput.frame() >>> 1) & 1));
        }
        return true;
    }
}
