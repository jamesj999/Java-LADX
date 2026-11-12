package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.*;

/** ROM handler stages for SparkType. */
final class SparkTypeHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_SPARK_COUNTER_CLOCKWISE,
                EntityTickStage.PHASE_2_AFTER_INITIALIZE,
                2,
                SparkTypeHandler::phase2SparkType);
        registry.registerTick(
                ENTITY_SPARK_CLOCKWISE, EntityTickStage.PHASE_2_AFTER_INITIALIZE, 2, SparkTypeHandler::phase2SparkType);
        registry.registerTick(
                ENTITY_SPARK_COUNTER_CLOCKWISE,
                EntityTickStage.PHASE_2_AFTER_RECOIL_APPLICATION,
                6,
                SparkTypeHandler::phase2SparkType2);
        registry.registerTick(
                ENTITY_SPARK_CLOCKWISE,
                EntityTickStage.PHASE_2_AFTER_RECOIL_APPLICATION,
                6,
                SparkTypeHandler::phase2SparkType2);
        registry.registerTick(
                ENTITY_SPARK_COUNTER_CLOCKWISE,
                EntityTickStage.PHASE_3_BEFORE_LASER,
                4,
                SparkTypeHandler::phase3SparkType);
        registry.registerTick(
                ENTITY_SPARK_CLOCKWISE, EntityTickStage.PHASE_3_BEFORE_LASER, 4, SparkTypeHandler::phase3SparkType);
    }

    private static boolean phase2SparkType(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        state.motion(SparkMotion.class, SparkMotion::new)
                .initialize(slotState.entity().slot(), slotState.entity().type());

        return true;
    }

    private static boolean phase2SparkType2(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        if (slotState.wasInitializing()) {
            slotState.updated(
                    state.motion(SparkMotion.class, SparkMotion::new).applyInitializationOffset(slotState.entity()));
        }
        return true;
    }

    private static boolean phase3SparkType(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            slotState.updated(state.motion(SparkMotion.class, SparkMotion::new)
                    .advance(slotState.entity(), frameInput.frame(), frameInput.backgroundCollision()));
        }
        return true;
    }
}
