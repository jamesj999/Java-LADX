package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for SparkType. */
final class SparkTypeHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_SPARK_COUNTER_CLOCKWISE, EntityTickStage.PHASE_2_SPARK_TYPE, SparkTypeHandler::phase2SparkType);
        registry.registerTick(
                ENTITY_SPARK_CLOCKWISE, EntityTickStage.PHASE_2_SPARK_TYPE, SparkTypeHandler::phase2SparkType);
        registry.registerTick(
                ENTITY_SPARK_COUNTER_CLOCKWISE,
                EntityTickStage.PHASE_2_SPARK_TYPE_2,
                SparkTypeHandler::phase2SparkType2);
        registry.registerTick(
                ENTITY_SPARK_CLOCKWISE, EntityTickStage.PHASE_2_SPARK_TYPE_2, SparkTypeHandler::phase2SparkType2);
        registry.registerTick(
                ENTITY_SPARK_COUNTER_CLOCKWISE, EntityTickStage.PHASE_3_SPARK_TYPE, SparkTypeHandler::phase3SparkType);
        registry.registerTick(
                ENTITY_SPARK_CLOCKWISE, EntityTickStage.PHASE_3_SPARK_TYPE, SparkTypeHandler::phase3SparkType);
    }

    private static boolean phase2SparkType(EntityFrameExecution state) {
        state.sparkMotion().initialize(state.entity.slot(), state.entity.type());

        return true;
    }

    private static boolean phase2SparkType2(EntityFrameExecution state) {
        if (state.wasInitializing) {
            state.updated = state.sparkMotion().applyInitializationOffset(state.entity);
        }
        return true;
    }

    private static boolean phase3SparkType(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            state.updated = state.sparkMotion().advance(state.entity, state.frame, state.backgroundCollision);
        }
        return true;
    }
}
