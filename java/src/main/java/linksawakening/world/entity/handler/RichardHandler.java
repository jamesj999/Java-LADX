package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_GRANDPA_ULRIRA;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_RICHARD;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_RICHARD_FROG;

import linksawakening.world.entity.EntityFrameInputLatch;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntitySlotExecution;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityStoryController;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntity;

/** ROM handler stages for Richard. */
final class RichardHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(ENTITY_GRANDPA_ULRIRA, EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE, frame -> {
            var slot = frame.slotFrame();
            if (slot.status() != EntityStatus.ACTIVE) return true;

            var input = frame.inputs();
            frame.story().advanceGrandpaUlrira(slot.entity(), input.frame(), input.linkEntityX(), input.linkEntityY());
            return false;
        });
        registry.registerInitializer(
                ENTITY_RICHARD_FROG, (entity, random) -> registry.motion(FrogSlots.class, FrogSlots::new)
                        .initialize(entity.slot()));
        registry.registerTick(ENTITY_RICHARD_FROG, EntityTickStage.PHASE_2_BEFORE_RECOIL, frame -> {
            var slot = frame.slotFrame();
            if (slot.status() != EntityStatus.ACTIVE) return true;

            if (!slot.wasInitializing() && frame.gates().handlerInteractive()) {
                var input = frame.inputs();
                RoomEntity frog = slot.updated();
                slot.updated(frame.motion(FrogSlots.class, FrogSlots::new)
                        .motions[frog.slot()]
                        .tick(frog, input.randomByteSupplier(), input.hinoxBackgroundInteraction(), input.frame()));
            }
            return true;
        });
        registry.registerTick(
                ENTITY_RICHARD,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                16,
                frame -> phase1Richard(frame.slotFrame(), frame.inputs(), frame.story()));
    }

    private static final class FrogSlots {
        private final RichardFrogMotion[] motions = new RichardFrogMotion[RoomEntity.SLOT_COUNT];

        void initialize(int slot) {
            motions[slot] = new RichardFrogMotion();
        }
    }

    private static boolean phase1Richard(
            EntitySlotExecution slotState, EntityFrameInputLatch frameInput, EntityStoryController story) {
        if (slotState.status() == EntityStatus.ACTIVE) {
            story.advanceRichard(
                    slotState.entity(),
                    frameInput.frame(),
                    frameInput.linkEntityX(),
                    frameInput.linkEntityY(),
                    frameInput.linkAirborne());
            return false;
        }
        return true;
    }
}
