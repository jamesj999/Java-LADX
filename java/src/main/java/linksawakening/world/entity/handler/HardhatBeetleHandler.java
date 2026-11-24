package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_HARDHAT_BEETLE;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.motion.HardHatMotion;

/** ROM handler stages for HardhatBeetle. */
final class HardhatBeetleHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerInitializer(ENTITY_HARDHAT_BEETLE, (entity, random) -> initialize(registry, entity));
        registry.registerTick(
                ENTITY_HARDHAT_BEETLE,
                EntityTickStage.PHASE_2_BEFORE_RECOIL_APPLICATION,
                5,
                frame -> frame.slotFrame().wasInitializing() || frame.gates().handlerInteractive());
        registry.registerTick(
                ENTITY_HARDHAT_BEETLE,
                EntityTickStage.PHASE_4_BEFORE_FOLLOWER,
                20,
                HardhatBeetleHandler::phase4HardhatBeetle);
    }

    private static boolean phase4HardhatBeetle(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        // $06:653F discards the caller address even on the last recoil frame.
        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && slotState.ignoreHitsBeforeRecoil() == 0) {
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

    private static void initialize(EntityHandlerRegistry registry, RoomEntity entity) {
        registry.motion(HardHatMotion.class, HardHatMotion::new).initialize(entity.slot());
    }
}
