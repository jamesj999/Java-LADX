package linksawakening.world.entity.handler;

import linksawakening.world.EntityTickStage;
import linksawakening.world.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.EntityCombatEvent;







import linksawakening.world.entity.motion.WarpMotion;

import linksawakening.world.transition.Warp;

import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.WarpLinkStateRequest;
import static linksawakening.world.EntitySnapshots.withPositionAndVariant;

/** ROM handler stages for Warp. */
final class WarpHandler {
  static void install(EntityHandlerRegistration registry) {
    for (int type = 0; type < 256; type++) {
      if ((type == WarpMotion.ENTITY_TYPE)) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_1_TYPE,
            state -> {
              if (state.status == EntityStatus.ACTIVE
                  && state.runtime.indoorRoom

                  && state.runtime.warpMotion != null) {
                int slot = state.entity.slot();
                state.replaceEntity(withPositionAndVariant(
                        state.entity,
                        state.entity.x(),
                        state.entity.y(),
                        state.runtime.dungeonMinibossReady ? (state.frame >> 3) & 7 : -1));
                WarpMotion.Update warpUpdate =
                    state.runtime.warpMotion.advance(
                        state.entity,
                        state.runtime.warpState[slot],
                        state.runtime.warpTransitionCountdown[slot],
                        state.runtime.entityMapId,
                        state.runtime.entityRoomId,
                        state.runtime.dungeonMinibossReady,
                        state.linkEntityX,
                        state.linkEntityY,
                        state.linkZ);
                state.runtime.warpState[slot] = warpUpdate.state();
                state.runtime.warpTransitionCountdown[slot] = warpUpdate.countdown();
                if (warpUpdate.musicTrack() >= 0) {
                  state.runtime.pendingMusicTrack = warpUpdate.musicTrack();
                }
                if (warpUpdate.jingleId() >= 0) {
                  state.emit(
                      new EntityCombatEvent(
                          slot,
                          state.entity.type(),
                          0,
                          false,
                          EntityCombatEvent.SoundChannel.JINGLE,
                          warpUpdate.jingleId()));
                }
                if (warpUpdate.motionBlocked()) {
                  state.emit(
                      new LinkMotionBlockRequest(slot));
                }
                if (warpUpdate.state() == WarpMotion.STATE_TRANSITION) {
                  state.emit(
                      new WarpLinkStateRequest(
                          slot,
                          state.entity.x(),
                          state.entity.y(),
                          warpUpdate.immunityCountdown()));
                }
                if (warpUpdate.destinationReady()) {
                  state.emit(
                      new DungeonWarpRequest(slot, warpUpdate.destination()));
                  state.lifecycle().disableEntityWithoutPersistence(slot);
                }
                return false;
              }
              return true;
            });
      }
    }
  }
}
