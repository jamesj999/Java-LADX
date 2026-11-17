package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_IRON_MASK;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.motion.RoamingEnemyMotion;

/** ROM handler stages for IronMask. */
final class IronMaskHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerInitializer(ENTITY_IRON_MASK, (entity, random) -> initialize(registry, entity));
        registry.registerTick(
                ENTITY_IRON_MASK, EntityTickStage.PHASE_3_BEFORE_LASER, 2, IronMaskHandler::phase3IronMask);
    }

    private static boolean phase3IronMask(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            // ROM privateState2: handler-dependent register; a narrower meaning has not been established.
            int privateState2 =
                    state.slot().ironMaskPrivateState2(slotState.entity().slot());
            int variant =
                    privateState2 == 0 ? slotState.updated().spriteVariant() : ((frameInput.frame() >>> 4) & 0x01);
            {
                slotState.updated(state.sprites()
                        .present(slotState.updated(), state.spriteViews().forIronMaskState(privateState2), variant));
            }
            presentationFlags.preserveIronMaskPresentation(true);
        }
        return true;
    }

    private static void initialize(EntityHandlerRegistry registry, RoomEntity entity) {
        registry.motion(RoamingEnemyMotion.class, RoamingEnemyMotion::new).initialize(entity.slot());
    }
}
