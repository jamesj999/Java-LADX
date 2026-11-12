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
        if (state.status == EntityStatus.ACTIVE) {
            if (state.roomInspection().chestSwordLevel() == 0 && state.sprites() != null) {
                state.entity = withDefinition(
                        state.entity, state.sprites().forSwordShieldPickup(true), state.entity.spriteVariant());
                state.replaceEntity(state.entity);
            }
            if (state.rewards().swordPickupSequenceActive(state.entity.slot())) {
                state.rewards()
                        .advanceSwordPickup(
                                state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.linkZ);
                return false;
            }
        }
        return true;
    }
}
