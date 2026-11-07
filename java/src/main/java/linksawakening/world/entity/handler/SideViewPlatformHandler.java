package linksawakening.world.entity.handler;

import linksawakening.world.EntityTickStage;
import linksawakening.world.RoomEntity;
import linksawakening.world.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.EntityCombatEvent;







import linksawakening.world.entity.motion.SideViewPlatformMotion;

import linksawakening.world.entity.event.SideViewPlatformLinkRequest;
import static linksawakening.world.EntityMotionMath.sideViewPlatformOverlapsLink;

/** ROM handler stages for SideViewPlatform. */
final class SideViewPlatformHandler {
  static void install(EntityHandlerRegistration registry) {
    for (int type = 0; type < 256; type++) {
      if ((type == SideViewPlatformMotion.ENTITY_TYPE)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_TYPE,
            state -> {
              if (state.status == EntityStatus.ACTIVE
                  && !state.wasInitializing

                  && state.handlerLinkCollisionEnabled
                  && state.runtime.backgroundMechanics.entityTimersInteractive()
                  && !state.creditsGameplay
                  && state.runtime.transitionSequenceCounter == 0x04) {
                SideViewPlatformMotion.Frame platformFrame =
                    state.runtime.sideViewPlatformMotion.beginFrame(state.entity);
                RoomEntity movedPlatform = platformFrame.entity();
                if (platformFrame.horizontalDelta() != 0
                    && state.backgroundCollision != null
                    && state.backgroundCollision.blocks(
                        state.entity,
                        platformFrame.horizontalDelta() < 0 ? 1 : 0,
                        movedPlatform.x(),
                        movedPlatform.y())) {
                  platformFrame =
                      state.runtime.sideViewPlatformMotion.restoreHorizontalPosition(
                          state.entity, platformFrame);
                  movedPlatform = platformFrame.entity();
                }
                if (platformFrame.verticalDelta() != 0 && state.backgroundCollision != null) {
                  int direction = platformFrame.verticalDelta() < 0 ? 2 : 3;
                  if (state.backgroundCollision.blocks(
                      state.entity, direction, movedPlatform.x(), movedPlatform.y())) {
                    platformFrame =
                        state.runtime.sideViewPlatformMotion.restoreVerticalPosition(
                            state.entity, platformFrame);
                    movedPlatform = platformFrame.entity();
                  }
                }
                boolean standing =
                    state.runtime.groundInteractionSideScrolling
                        && (state.linkZ & 0xFF) == 0
                        && (state.runtime.currentLinkSpeedY & 0x80) == 0
                        && sideViewPlatformOverlapsLink(
                            movedPlatform, state.linkEntityX, state.linkEntityY)
                        && (((state.linkEntityY - movedPlatform.y() + 0x08) & 0x80) != 0);
                boolean activationAllowed =
                    state.runtime.entityRoomId == 0x3B || state.carryingEntity;
                if (standing) {
                  state.emit(
                      new SideViewPlatformLinkRequest(
                          state.entity.slot(),
                          platformFrame.horizontalDelta(),
                          (movedPlatform.y() - 0x10) & 0xFF,
                          0x02,
                          true,
                          activationAllowed));
                }
                SideViewPlatformMotion.Update platformUpdate =
                    state.runtime.sideViewPlatformMotion.finishFrame(
                        movedPlatform, platformFrame, state.frame, standing, activationAllowed);
                state.updated = platformUpdate.entity();
                if (platformUpdate.rumble()) {
                  state.emit(
                      new EntityCombatEvent(
                          state.entity.slot(),
                          state.entity.type(),
                          0,
                          false,
                          EntityCombatEvent.SoundChannel.NOISE,
                          0x11));
                }
              }
              return true;
            });
      }
    }
  }
}
