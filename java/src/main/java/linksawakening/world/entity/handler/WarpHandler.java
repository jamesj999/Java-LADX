package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntitySnapshots.withPositionAndVariant;

import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.WarpLinkStateRequest;
import linksawakening.world.entity.motion.WarpMotion;

/** ROM handler stages for Warp. */
final class WarpHandler {
    static void install(EntityHandlerRegistration registry) {
        for (int type = 0; type < 256; type++) {
            if ((type == WarpMotion.ENTITY_TYPE)) {
                registry.registerTick(type, EntityTickStage.PHASE_1_TYPE, state -> {
                    if (state.status == EntityStatus.ACTIVE
                            && state.roomInspection().indoorRoom()
                            && state.warpMotion() != null) {
                        int slot = state.entity.slot();
                        state.replaceEntity(withPositionAndVariant(
                                state.entity,
                                state.entity.x(),
                                state.entity.y(),
                                state.roomInspection().dungeonMinibossReady() ? (state.frame >> 3) & 7 : -1));
                        WarpMotion.Update warpUpdate = state.warpMotion()
                                .advance(
                                        state.entity,
                                        state.lifecycle().warpState(slot),
                                        state.lifecycle().warpTransitionCountdown(slot),
                                        state.roomInspection().entityMapId(),
                                        state.roomInspection().entityRoomId(),
                                        state.roomInspection().dungeonMinibossReady(),
                                        state.linkEntityX,
                                        state.linkEntityY,
                                        state.linkZ);
                        state.lifecycle().setWarpState(slot, warpUpdate.state());
                        state.lifecycle().setWarpTransitionCountdown(slot, warpUpdate.countdown());
                        if (warpUpdate.musicTrack() >= 0) {
                            state.requestMusic(warpUpdate.musicTrack());
                        }
                        if (warpUpdate.jingleId() >= 0) {
                            state.emit(new EntityCombatEvent(
                                    slot,
                                    state.entity.type(),
                                    0,
                                    false,
                                    EntityCombatEvent.SoundChannel.JINGLE,
                                    warpUpdate.jingleId()));
                        }
                        if (warpUpdate.motionBlocked()) {
                            state.emit(new LinkMotionBlockRequest(slot, 0, 0, 1));
                        }
                        if (warpUpdate.state() == WarpMotion.STATE_TRANSITION) {
                            state.emit(new WarpLinkStateRequest(
                                    slot, state.entity.x(), state.entity.y(), warpUpdate.immunityCountdown()));
                        }
                        if (warpUpdate.destinationReady()) {
                            state.emit(new DungeonWarpRequest(slot, warpUpdate.destination()));
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
