package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_PAIRODD;

import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.PairoddMotion;

/** ROM handler stages for Pairodd. */
final class PairoddHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerInitializer(ENTITY_PAIRODD, (entity, random) -> initialize(registry, entity, random));
        registry.registerTick(ENTITY_PAIRODD, EntityTickStage.PHASE_3_BEFORE_LASER, 8, PairoddHandler::phase3Pairodd);
    }

    private static boolean phase3Pairodd(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            PairoddMotion.Update pairoddUpdate = state.motion(PairoddMotion.class, PairoddMotion::new)
                    .advance(
                            slotState.entity(),
                            frameInput.frame(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.randomByteSupplier(),
                            state.slot().flashCountdown(slotState.entity().slot()) > 0);
            slotState.updated(pairoddUpdate.entity());
            if (pairoddUpdate.spawnProjectile()) {
                state.enemySpawns()
                        .spawnPairoddProjectile(slotState.entity(), frameInput.linkEntityX(), frameInput.linkEntityY());
            }
            if (pairoddUpdate.teleportJingle()) {
                state.emit(new EntityCombatEvent(
                        slotState.entity().slot(),
                        slotState.entity().type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        0x3C));
            }
        }
        return true;
    }

    private static void initialize(EntityHandlerRegistration registry, RoomEntity entity, IntSupplier random) {
        registry.motion(PairoddMotion.class, PairoddMotion::new).initialize(entity.slot(), random);
    }
}
