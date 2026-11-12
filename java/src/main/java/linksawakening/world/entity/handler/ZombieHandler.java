package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_ZOMBIE;
import static linksawakening.world.entity.EntitySnapshots.withVariant;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.ZombieFrame;
import linksawakening.world.entity.motion.ZombieMotion;

/** ROM handler stages for Zombie. */
final class ZombieHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_ZOMBIE, EntityTickStage.PHASE_2_BEFORE_RECOIL_APPLICATION, 0, ZombieHandler::phase2Zombie);
        registry.registerTick(ENTITY_ZOMBIE, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 11, ZombieHandler::phase4Zombie);
    }

    private static boolean phase2Zombie(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var presentationFlags = state.presentation();

        if (slotState.wasInitializing()) {
            // The room-loaded Zombie is only a hidden spawn marker; its
            // bank-$18 handler never renders the parent entity.
            slotState.updated(withVariant(slotState.entity(), -1));
            presentationFlags.preserveZombiePresentation(true);
        }
        return true;
    }

    private static boolean phase4Zombie(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && !frameInput.creditsGameplay()
                && timerGates.handlerLinkCollisionEnabled()) {
            int slot = slotState.entity().slot();
            RoomEntityBackgroundInteraction zombieBackgroundInteraction =
                    state.background().interaction();
            if (zombieBackgroundInteraction == null && frameInput.backgroundCollision() != null) {
                zombieBackgroundInteraction =
                        RoomEntityBackgroundInteraction.fromBoolean(frameInput.backgroundCollision());
            }
            ZombieMotion.Update zombieUpdate = state.motion(ZombieMotion.class, ZombieMotion::new)
                    .advance(new ZombieFrame(
                            slotState.updated(),
                            frameInput.frame(),
                            state.slot().transitionCountdown(slot),
                            state.slot().physicsFlags(slot),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.randomByteSupplier(),
                            state.background().objectQuery(),
                            zombieBackgroundInteraction,
                            state.slot().ignoreHits(slot)));
            slotState.updated(zombieUpdate.entity());
            state.slot().setTransitionCountdown(slot, zombieUpdate.transitionCountdown());
            state.slot().setPhysicsFlags(slot, zombieUpdate.physicsFlags());
            slotState.applyGenericGroundInteraction(zombieUpdate.appliesBackgroundInteraction());
            if (zombieUpdate.spawnRequest() != null) {
                state.enemySpawns().spawnZombieChild(zombieUpdate.spawnRequest());
            }
            if (zombieUpdate.unloadRequested()) {
                state.lifecycle().clearEntity(slot);
                return false;
            }
            presentationFlags.preserveZombiePresentation(true);
        }
        return true;
    }
}
