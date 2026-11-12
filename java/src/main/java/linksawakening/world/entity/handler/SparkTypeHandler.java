package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityTypeRules.isSparkType;

import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for SparkType. */
final class SparkTypeHandler {
    static void install(EntityHandlerRegistration registry) {
        for (int type = 0; type < 256; type++) {
            if (((isSparkType(type)))) {
                registry.registerTick(type, EntityTickStage.PHASE_2_SPARK_TYPE, state -> {
                    {
                        state.sparkMotion().initialize(state.entity.slot(), state.entity.type());
                    }
                    return true;
                });
            }
        }
        for (int type = 0; type < 256; type++) {
            if ((isSparkType(type))) {
                registry.registerTick(type, EntityTickStage.PHASE_2_SPARK_TYPE_2, state -> {
                    if (state.wasInitializing) {
                        state.updated = state.sparkMotion().applyInitializationOffset(state.entity);
                    }
                    return true;
                });
            }
        }
        for (int type = 0; type < 256; type++) {
            if ((isSparkType(type))) {
                registry.registerTick(type, EntityTickStage.PHASE_3_SPARK_TYPE, state -> {
                    if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
                        state.updated =
                                state.sparkMotion().advance(state.entity, state.frame, state.backgroundCollision);
                    }
                    return true;
                });
            }
        }
    }
}
