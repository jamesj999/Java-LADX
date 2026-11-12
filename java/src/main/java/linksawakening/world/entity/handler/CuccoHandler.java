package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_CUCCO;

import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.CuccoInteractionFrame;
import linksawakening.world.entity.motion.CuccoMotion;

/** ROM handler stages for Cucco. */
final class CuccoHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerInitializer(ENTITY_CUCCO, (entity, random) -> initialize(registry, entity, random));
        registry.registerTick(ENTITY_CUCCO, EntityTickStage.PHASE_3_AFTER_LASER, 5, CuccoHandler::phase3Cucco);
    }

    private static boolean phase3Cucco(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && timerGates.handlerLinkCollisionEnabled()) {

            CuccoMotion.Update cuccoUpdate = state.motion(CuccoMotion.class, CuccoMotion::new)
                    .advance(new CuccoInteractionFrame(
                            slotState.entity(),
                            frameInput.frame(),
                            state.slot().transitionCountdown(slotState.entity().slot()),
                            state.slot().flashCountdown(slotState.entity().slot()),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.linkZ(),
                            timerGates.handlerLinkCollisionEnabled(),
                            state.liftThrow().cuccoPowerBraceletHeld(),
                            state.link().linkAttackStepAnimationCountdown() != 0,
                            frameInput.randomByteSupplier(),
                            frameInput.backgroundCollision(),
                            state.roomInspection().indoorRoom(),
                            state.story().followingNpcState().marinFollowing(),
                            state.lifecycle().transitionSequenceCounter(),
                            state.enemySpawns().canSpawnCuccoAngry()));
            slotState.updated(cuccoUpdate.entity());
            state.slot().setTransitionCountdown(slotState.entity().slot(), cuccoUpdate.transitionCountdown());
            state.slot().setFlashCountdown(slotState.entity().slot(), cuccoUpdate.flashCountdown());
            state.slot().setHealth(slotState.entity().slot(), CuccoMotion.HEALTH_OVERRIDE);
            presentationFlags.preserveCuccoPresentation(true);
            if (cuccoUpdate.dialogLowId() >= 0) {
                state.emit(new DialogRequest(cuccoUpdate.dialogTableId(), cuccoUpdate.dialogLowId()));
            }
            if (cuccoUpdate.unloaded()) {
                state.lifecycle()
                        .disableEntityWithoutPersistence(slotState.entity().slot());
                return false;
            }
            if (cuccoUpdate.liftRequested()
                    && state.liftThrow().beginLift(slotState.entity().slot(), frameInput.romLinkDirection())) {
                state.emit(new EntityCombatEvent(
                        slotState.entity().slot(),
                        slotState.entity().type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.WAVE,
                        CuccoMotion.LIFT_WAVE_SFX));
                state.replaceEntity(state.liftThrow()
                        .advanceLiftedEntity(
                                state.liftThrow()
                                        .renderLiftedEntity(state.entityAt(slotState.index()), frameInput.frame()),
                                frameInput.linkEntityX(),
                                frameInput.linkEntityY(),
                                frameInput.linkZ(),
                                frameInput.romLinkDirection()));
                return false;
            }
            if (cuccoUpdate.spawnRequest() != null && state.enemySpawns().spawnCuccoAngry(cuccoUpdate.spawnRequest())) {
                state.emit(new EntityCombatEvent(
                        slotState.entity().slot(),
                        slotState.entity().type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.WAVE,
                        CuccoMotion.CUCCO_HURT_WAVE_SFX));
            }
            if (cuccoUpdate.boomerangSound()) {
                state.projectiles().playBoomerangSoundIfDue(slotState.updated());
            }
        }
        return true;
    }

    private static void initialize(EntityHandlerRegistration registry, RoomEntity entity, IntSupplier random) {
        registry.motion(CuccoMotion.class, CuccoMotion::new).initialize(entity.slot());
    }
}
