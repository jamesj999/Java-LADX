package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_HEART_CONTAINER;

import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for HeartContainer. */
final class HeartContainerHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_HEART_CONTAINER, EntityTickStage.PHASE_1_HEART_CONTAINER, state -> {
            if (state.status == EntityStatus.ACTIVE && state.slot().transitionCountdown(state.entity.slot()) != 0) {

                state.bossRewards()
                        .advanceCollectedHeartContainer(
                                state.entity, state.linkEntityX, state.linkEntityY, state.linkZ);
                return false;
            }
            return true;
        });
    }
}
