package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.event.EntityCombatEvent;









/** ROM handler stages for CrystalSwitch. */
final class CrystalSwitchHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_CRYSTAL_SWITCH,
        EntityTickStage.PHASE_2_CRYSTAL_SWITCH,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            // CrystalSwitchEntityHandler writes $FF before dispatching
            // its normal collision helper, so sword damage can never
            // turn this entity into a dying state.
            state.runtime.enemyHealth[state.entity.slot()] = 0xFF;
            if (state.runtime.enemyFlashCountdown[state.entity.slot()] > 0) {
              // The handler consumes the collision flash as its trigger.
              state.runtime.enemyFlashCountdown[state.entity.slot()] = 0;
              if (!state.runtime.switchBlockAnimationActive) {
                state.runtime.enemyTransitionCountdown[state.entity.slot()] = 0x18;
                state.runtime.pendingSwitchBlockAnimationRequest = true;
                state.emit(
                    new EntityCombatEvent(
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
        });
  }
}
