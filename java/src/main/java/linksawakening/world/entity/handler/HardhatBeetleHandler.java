package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_HARDHAT_BEETLE;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for HardhatBeetle. */
final class HardhatBeetleHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_HARDHAT_BEETLE,
                EntityTickStage.PHASE_4_HARDHAT_BEETLE,
                HardhatBeetleHandler::phase4HardhatBeetle);
    }

    private static boolean phase4HardhatBeetle(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            state.updated = state.hardHatMotion()
                    .advance(
                            state.entity,
                            state.frame,
                            state.linkEntityX,
                            state.linkEntityY,
                            state.randomByteSupplier,
                            state.backgroundCollision);
        }
        return true;
    }
}
