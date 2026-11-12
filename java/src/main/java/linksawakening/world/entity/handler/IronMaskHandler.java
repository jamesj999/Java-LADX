package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_IRON_MASK;
import static linksawakening.world.entity.EntitySnapshots.withDefinition;

import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.*;

/** ROM handler stages for IronMask. */
final class IronMaskHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerInitializer(ENTITY_IRON_MASK, (entity, random) -> initialize(registry, entity, random));
        registry.registerTick(
                ENTITY_IRON_MASK, EntityTickStage.PHASE_3_BEFORE_LASER, 2, IronMaskHandler::phase3IronMask);
    }

    private static boolean phase3IronMask(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            int privateState2 =
                    state.slot().ironMaskPrivateState2(slotState.entity().slot());
            int variant =
                    privateState2 == 0 ? slotState.updated().spriteVariant() : ((frameInput.frame() >>> 4) & 0x01);
            if (state.sprites() != null) {
                slotState.updated(
                        withDefinition(slotState.updated(), state.sprites().forIronMaskState(privateState2), variant));
            }
            presentationFlags.preserveIronMaskPresentation(true);
        }
        return true;
    }

    private static void initialize(EntityHandlerRegistration registry, RoomEntity entity, IntSupplier random) {
        registry.motion(RoamingEnemyMotion.class, RoamingEnemyMotion::new).initialize(entity.slot());
    }
}
