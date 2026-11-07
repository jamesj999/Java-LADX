package linksawakening.world;

import static linksawakening.world.EntityTypeRules.isSparkType;

/** ROM handler stages for SparkType. */
final class SparkTypeHandler {
  static void installPhase2(EntityHandlerRegistry registry) {
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
  }

  static void installPhase2Part2(EntityHandlerRegistry registry) {
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

  static void installPhase3(EntityHandlerRegistry registry) {
    for (int type = 0; type < 256; type++) {
      if ((isSparkType(type))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_3_SPARK_TYPE,
            state -> {
              if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
                state.updated =
                    state.runtime.sparkMotion.advance(
                        state.entity, state.frame, state.backgroundCollision);
              }
              return true;
            });
      }
    }
  }
}
