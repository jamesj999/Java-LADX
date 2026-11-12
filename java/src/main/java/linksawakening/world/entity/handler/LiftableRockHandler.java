package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_LIFTABLE_ROCK;

import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for LiftableRock. */
final class LiftableRockHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_LIFTABLE_ROCK, EntityTickStage.PHASE_1_LIFTABLE_ROCK, state -> {
            if (state.status == EntityStatus.ACTIVE && state.liftThrow().liftableRockSmashActive(state.entity.slot())) {

                state.liftThrow().advanceLiftableRockSmash(state.index, state.entity, state.frame);
                return false;
            }
            return true;
        });
    }
}
