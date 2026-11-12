package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_CRYSTAL_SWITCH;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.EntityCombatEvent;

/** ROM handler stages for CrystalSwitch. */
final class CrystalSwitchHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_CRYSTAL_SWITCH,
                EntityTickStage.PHASE_2_AFTER_RECOIL_APPLICATION,
                4,
                CrystalSwitchHandler::phase2CrystalSwitch);
    }

    private static boolean phase2CrystalSwitch(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            // CrystalSwitchEntityHandler writes $FF before dispatching
            // its normal collision helper, so sword damage can never
            // turn this entity into a dying state.
            state.slot().setHealth(state.entity.slot(), 0xFF);
            if (state.slot().flashCountdown(state.entity.slot()) > 0) {
                // The handler consumes the collision flash as its trigger.
                state.slot().setFlashCountdown(state.entity.slot(), 0);
                if (!state.background().switchBlockAnimationActive()) {
                    state.slot().setTransitionCountdown(state.entity.slot(), 0x18);
                    state.background().requestSwitchBlockAnimation();
                    state.emit(new EntityCombatEvent(
                            state.entity.slot(),
                            state.entity.type(),
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.WAVE,
                            0x0E));
                }
            }
        }
        return true;
    }
}
