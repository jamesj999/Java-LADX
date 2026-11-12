package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_SWORD_SHIELD_PICKUP;
import static linksawakening.world.entity.EntitySnapshots.withDefinition;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for SwordShieldPickup. */
final class SwordShieldPickupHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_SWORD_SHIELD_PICKUP,
                EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE,
                24,
                SwordShieldPickupHandler::phase1SwordShieldPickup);
    }

    private static boolean phase1SwordShieldPickup(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE) {
            if (state.roomInspection().chestSwordLevel() == 0 && state.sprites() != null) {
                slotState.entity(withDefinition(
                        slotState.entity(),
                        state.sprites().forSwordShieldPickup(true),
                        slotState.entity().spriteVariant()));
                state.replaceEntity(slotState.entity());
            }
            if (state.rewards().swordPickupSequenceActive(slotState.entity().slot())) {
                state.rewards()
                        .advanceSwordPickup(
                                slotState.entity(),
                                frameInput.frame(),
                                frameInput.linkEntityX(),
                                frameInput.linkEntityY(),
                                frameInput.linkZ());
                return false;
            }
        }
        return true;
    }
}
