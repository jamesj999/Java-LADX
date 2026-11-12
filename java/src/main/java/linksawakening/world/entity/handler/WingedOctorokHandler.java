package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_WINGED_OCTOROK;

import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.WingedOctorokInteractionFrame;
import linksawakening.world.entity.motion.WingedOctorokMotion;

/** ROM handler stages for WingedOctorok. */
final class WingedOctorokHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerInitializer(ENTITY_WINGED_OCTOROK, (entity, random) -> initialize(registry, entity, random));
        registry.registerTick(
                ENTITY_WINGED_OCTOROK,
                EntityTickStage.PHASE_3_BEFORE_LASER,
                12,
                WingedOctorokHandler::phase3WingedOctorok);
    }

    private static boolean phase3WingedOctorok(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && timerGates.handlerLinkCollisionEnabled()) {

            WingedOctorokMotion.Update wingedUpdate = state.motion(WingedOctorokMotion.class, WingedOctorokMotion::new)
                    .advance(new WingedOctorokInteractionFrame(
                            slotState.entity(),
                            frameInput.frame(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.randomByteSupplier(),
                            frameInput.backgroundCollision(),
                            state.slot().transitionCountdown(slotState.entity().slot()),
                            state.slot().ignoreHits(slotState.entity().slot()),
                            state.link().actionButtonAHeld(),
                            state.link().actionButtonBHeld(),
                            state.link().linkItemA(),
                            state.link().linkItemB()));
            slotState.updated(wingedUpdate.entity());
            state.slot().setTransitionCountdown(slotState.entity().slot(), wingedUpdate.transitionCountdown());
            presentationFlags.preserveWingedOctorokPresentation(true);
            state.projectiles()
                    .setWingedStateTwoThisFrame(slotState.entity().slot(), wingedUpdate.skippedDefaultCollision());
            if (wingedUpdate.rockSpawn() != null) {
                state.enemySpawns()
                        .spawnWingedOctorokRock(
                                slotState.updated(), wingedUpdate.rockSpawn().direction());
            }
            if (wingedUpdate.jumped()) {
                state.projectiles()
                        .setWingedSwordAttackThisFrame(slotState.entity().slot(), true);
                state.emit(new EntityCombatEvent(
                        slotState.entity().slot(),
                        slotState.entity().type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        0x24));
            }
        }
        return true;
    }

    private static void initialize(EntityHandlerRegistration registry, RoomEntity entity, IntSupplier random) {
        registry.motion(WingedOctorokMotion.class, WingedOctorokMotion::new).initialize(entity.slot());
    }
}
