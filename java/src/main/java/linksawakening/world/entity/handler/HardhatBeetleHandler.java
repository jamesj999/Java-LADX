package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_HARDHAT_BEETLE;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.*;

/** ROM handler stages for HardhatBeetle. */
final class HardhatBeetleHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_HARDHAT_BEETLE,
                EntityTickStage.PHASE_4_BEFORE_FOLLOWER,
                20,
                HardhatBeetleHandler::phase4HardhatBeetle);
    }

    private static boolean phase4HardhatBeetle(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            slotState.updated(state.motion(HardHatMotion.class, HardHatMotion::new)
                    .advance(
                            slotState.entity(),
                            frameInput.frame(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.randomByteSupplier(),
                            frameInput.backgroundCollision()));
        }
        return true;
    }
}
