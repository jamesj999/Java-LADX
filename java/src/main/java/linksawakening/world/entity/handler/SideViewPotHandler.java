package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.SideViewPotContact;

import linksawakening.world.entity.motion.SideViewPotMotion;

/** ROM handler stages for SideViewPot. */
final class SideViewPotHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_SIDE_VIEW_POT,
        EntityTickStage.PHASE_1_SIDE_VIEW_POT,
        state -> {
          if (state.status == EntityStatus.ACTIVE

              && !state.runtime.sideViewPotMotion.active(state.entity.slot())) {
            SideViewPotContact.Result contact = SideViewPotContact.Result.none();
            int effectiveLinkX = state.linkEntityX;
            int effectiveLinkY = state.linkEntityY;
            if (state.sideViewPotLinkCollisionInteractive) {
              contact =
                  SideViewPotContact.resolve(
                      state.entity,
                      state.linkEntityX,
                      state.linkEntityY,
                      state.linkAirborne,
                      state.link().currentLinkSpeedY());
              if (contact.collided()) {
                state.liftThrow().pendingLinkPotContact(state.entity.slot(), contact);
                if (contact.restoreFinalPositionX()) {
                  effectiveLinkX =
                      state.link().currentLinkFinalPositionXConfigured()
                          ? state.link().currentLinkFinalPositionX()
                          : state.linkEntityX;
                }
                if (contact.snapTop()) {
                  effectiveLinkY = contact.positionY();
                }
              }
            }
            if (state.runtime.groundInteractionSideScrolling
                && state.sideViewPotInteractive
                && SideViewPotContact.liftWindow(state.entity, effectiveLinkX, effectiveLinkY)
                && state.liftThrow().sideViewPotPowerBraceletHeld()
                && state.link().linkAttackStepAnimationCountdown() == 0
                && state.runtime.beginLift(state.entity.slot(), state.romLinkDirection)) {
              state.emit(
                  new EntityCombatEvent(
                      state.entity.slot(),
                      state.entity.type(),
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.WAVE,
                      0x02));
            }
            return false;
          }
          return true;
        });
    registry.registerTick(
        RoomEntityRuntime.ENTITY_SIDE_VIEW_POT,
        EntityTickStage.PHASE_1_SIDE_VIEW_POT_2,
        state -> {
          if (state.status == EntityStatus.ACTIVE

              && state.runtime.sideViewPotMotion.active(state.entity.slot())) {
            if (state.sideViewPotInteractive) {
              SideViewPotMotion.Update potUpdate =
                  state.runtime.sideViewPotMotion.advance(state.entity);
              RoomEntity movedPot = potUpdate.entity();
              boolean collision =
                  state.liftThrow().sideViewPotBackgroundCollision(
                      state.entity, movedPot, state.backgroundCollision, state.frame);
              if (collision) {
                state.runtime.spawnLiftableRockSmash(
                    movedPot.x(),
                    (movedPot.y() - movedPot.z()) & 0xFF,
                    state.runtime.LIFTABLE_ROCK_SMASH_MODE_ROCK);
                state.runtime.clearEntity(state.entity.slot());
              } else {
                state.replaceEntity(movedPot);
              }
            }
            // ReturnIfNonInteractive_19 leaves the rendered pot and all
            // dedicated motion state untouched while Link is gated.
            return false;
          }
          return true;
        });
    for (int type = 0; type < 256; type++) {
      if ((type == RoomEntityRuntime.ENTITY_SIDE_VIEW_POT)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_SIDE_VIEW_POT,
            state -> {
              if (state.slot().transitionCountdown(state.entity.slot()) != 0) {
                state.replaceEntity(state.lifted);
                return false;
              }
              return true;
            });
      }
    }
  }
}
