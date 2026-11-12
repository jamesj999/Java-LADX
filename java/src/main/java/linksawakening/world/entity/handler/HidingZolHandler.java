package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_HIDING_ZOL;

import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.HidingZolMotion;

/** ROM handler stages for HidingZol. */
final class HidingZolHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerInitializer(ENTITY_HIDING_ZOL, (entity, random) -> initialize(registry, entity, random));
        registry.registerTick(
                ENTITY_HIDING_ZOL, EntityTickStage.PHASE_3_BEFORE_LASER, 6, HidingZolHandler::phase3HidingZol);
    }

    private static boolean phase3HidingZol(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            RoomEntityBackgroundInteraction hidingZolBackgroundInteraction =
                    state.background().interaction();
            if (hidingZolBackgroundInteraction == null && frameInput.backgroundCollision() != null) {
                hidingZolBackgroundInteraction =
                        RoomEntityBackgroundInteraction.fromBoolean(frameInput.backgroundCollision());
            }
            HidingZolMotion.Update hidingZolUpdate = state.motion(HidingZolMotion.class, HidingZolMotion::new)
                    .advance(
                            slotState.entity(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.randomByteSupplier(),
                            hidingZolBackgroundInteraction,
                            frameInput.frame());
            slotState.updated(hidingZolUpdate.entity());
            if (hidingZolUpdate.clearsIgnoreHitsCountdown()) {
                // func_007_73F7 temporarily writes $03 around the
                // background helper, then clears the shared byte.
                state.slot().setIgnoreHits(slotState.entity().slot(), 0);
            }
            if (hidingZolUpdate.jumpJingle()) {
                state.emit(new EntityCombatEvent(
                        slotState.entity().slot(),
                        slotState.entity().type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        0x24));
            }
            if (hidingZolUpdate.physicsFlags() >= 0) {
                state.slot().setPhysicsFlags(slotState.entity().slot(), hidingZolUpdate.physicsFlags());
            }
        }
        return true;
    }

    private static void initialize(EntityHandlerRegistration registry, RoomEntity entity, IntSupplier random) {
        registry.motion(HidingZolMotion.class, HidingZolMotion::new).initialize(entity.slot());
    }
}
