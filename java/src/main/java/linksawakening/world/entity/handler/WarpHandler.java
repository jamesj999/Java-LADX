package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntitySnapshots.withPositionAndVariant;

import linksawakening.world.entity.EntityFrameExecution;
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
        registry.registerTick(
                WarpMotion.ENTITY_TYPE, EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE, 5, WarpHandler::phase1Type);
    }

    private static boolean phase1Type(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE
                && state.roomInspection().indoorRoom()
                && state.warpMotion() != null) {
            int slot = slotState.entity().slot();
            state.replaceEntity(withPositionAndVariant(
                    slotState.entity(),
                    slotState.entity().x(),
                    slotState.entity().y(),
                    state.roomInspection().dungeonMinibossReady() ? (frameInput.frame() >> 3) & 7 : -1));
            WarpMotion.Update warpUpdate = state.warpMotion()
                    .advance(
                            slotState.entity(),
                            state.lifecycle().warpState(slot),
                            state.lifecycle().warpTransitionCountdown(slot),
                            state.roomInspection().entityMapId(),
                            state.roomInspection().entityRoomId(),
                            state.roomInspection().dungeonMinibossReady(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.linkZ());
            state.lifecycle().setWarpState(slot, warpUpdate.state());
            state.lifecycle().setWarpTransitionCountdown(slot, warpUpdate.countdown());
            if (warpUpdate.musicTrack() >= 0) {
                state.requestMusic(warpUpdate.musicTrack());
            }
            if (warpUpdate.jingleId() >= 0) {
                state.emit(new EntityCombatEvent(
                        slot,
                        slotState.entity().type(),
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
                        slot, slotState.entity().x(), slotState.entity().y(), warpUpdate.immunityCountdown()));
            }
            if (warpUpdate.destinationReady()) {
                state.emit(new DungeonWarpRequest(slot, warpUpdate.destination()));
                state.lifecycle().disableEntityWithoutPersistence(slot);
            }
            return false;
        }
        return true;
    }
}
