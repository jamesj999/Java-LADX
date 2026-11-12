package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_STALFOS_EVASIVE;
import static linksawakening.world.entity.EntitySnapshots.withDefinition;

import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.*;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.StalfosEvasiveFrame;
import linksawakening.world.entity.motion.StalfosEvasiveMotion;

/** ROM handler stages for StalfosEvasive. */
final class StalfosEvasiveHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerInitializer(ENTITY_STALFOS_EVASIVE, (entity, random) -> initialize(registry, entity, random));
        registry.registerTick(
                ENTITY_STALFOS_EVASIVE,
                EntityTickStage.PHASE_3_AFTER_LASER,
                9,
                StalfosEvasiveHandler::phase3StalfosEvasive);
    }

    private static boolean phase3StalfosEvasive(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            StalfosEvasiveMotion.Update evasiveUpdate = state.motion(
                            StalfosEvasiveMotion.class, StalfosEvasiveMotion::new)
                    .advance(new StalfosEvasiveFrame(
                            slotState.entity(),
                            frameInput.frame(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            state.link().actionButtonsHeld(),
                            state.slot().transitionCountdown(slotState.entity().slot()),
                            state.slot().ignoreHits(slotState.entity().slot()),
                            frameInput.randomByteSupplier(),
                            frameInput.backgroundCollision(),
                            state.roomInspection().entityMapId()));
            slotState.updated(evasiveUpdate.entity());
            state.slot().setTransitionCountdown(slotState.entity().slot(), evasiveUpdate.transitionCountdown());
            if (evasiveUpdate.cloneRequest() != null) {
                state.enemySpawns().spawnEvasiveClone(slotState.entity(), evasiveUpdate.cloneRequest());
            }
            if (evasiveUpdate.swordPokeRequested()) {
                state.emit(new EntityCombatEvent(
                        slotState.entity().slot(),
                        slotState.entity().type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        0x07));
                state.emit(new TransientVfxRequest(
                        TransientVfxType.SWORD_POKE,
                        slotState.updated().x(),
                        (slotState.updated().y() - slotState.updated().z()) & 0xFF));
            }
            if (evasiveUpdate.unloadRequested()) {
                state.lifecycle()
                        .disableEntityWithoutPersistence(slotState.entity().slot());
                return false;
            }
            if (state.sprites() != null) {
                EntitySpriteDefinition evasiveDefinition = state.spriteViews()
                        .forStalfosEvasiveState(state.motion(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new)
                                .privateState1(slotState.entity().slot()));
                slotState.updated(withDefinition(
                        slotState.updated(),
                        evasiveDefinition,
                        slotState.updated().spriteVariant()));
            }
        }
        return true;
    }

    private static void initialize(EntityHandlerRegistration registry, RoomEntity entity, IntSupplier random) {
        registry.motion(StalfosEvasiveMotion.class, StalfosEvasiveMotion::new).initialize(entity.slot());
    }
}
