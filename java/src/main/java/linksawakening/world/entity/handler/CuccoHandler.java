package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_CUCCO;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.CuccoMotion;

/** ROM handler stages for Cucco. */
final class CuccoHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_CUCCO, EntityTickStage.PHASE_3_AFTER_LASER, 5, CuccoHandler::phase3Cucco);
    }

    private static boolean phase3Cucco(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing && state.handlerLinkCollisionEnabled) {

            CuccoMotion.Update cuccoUpdate = state.cuccoMotion()
                    .advance(
                            state.entity,
                            state.frame,
                            state.slot().transitionCountdown(state.entity.slot()),
                            state.slot().flashCountdown(state.entity.slot()),
                            state.linkEntityX,
                            state.linkEntityY,
                            state.linkZ,
                            state.handlerLinkCollisionEnabled,
                            state.liftThrow().cuccoPowerBraceletHeld(),
                            state.link().linkAttackStepAnimationCountdown() != 0,
                            state.randomByteSupplier,
                            state.backgroundCollision,
                            state.roomInspection().indoorRoom(),
                            state.story().followingNpcState().marinFollowing(),
                            state.lifecycle().transitionSequenceCounter(),
                            state.enemySpawns().canSpawnCuccoAngry());
            state.updated = cuccoUpdate.entity();
            state.slot().setTransitionCountdown(state.entity.slot(), cuccoUpdate.transitionCountdown());
            state.slot().setFlashCountdown(state.entity.slot(), cuccoUpdate.flashCountdown());
            state.slot().setHealth(state.entity.slot(), CuccoMotion.HEALTH_OVERRIDE);
            state.preserveCuccoPresentation = true;
            if (cuccoUpdate.dialogLowId() >= 0) {
                state.emit(new DialogRequest(cuccoUpdate.dialogTableId(), cuccoUpdate.dialogLowId()));
            }
            if (cuccoUpdate.unloaded()) {
                state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
                return false;
            }
            if (cuccoUpdate.liftRequested()
                    && state.liftThrow().beginLift(state.entity.slot(), state.romLinkDirection)) {
                state.emit(new EntityCombatEvent(
                        state.entity.slot(),
                        state.entity.type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.WAVE,
                        CuccoMotion.LIFT_WAVE_SFX));
                state.replaceEntity(state.liftThrow()
                        .advanceLiftedEntity(
                                state.liftThrow().renderLiftedEntity(state.entityAt(state.index), state.frame),
                                state.linkEntityX,
                                state.linkEntityY,
                                state.linkZ,
                                state.romLinkDirection));
                return false;
            }
            if (cuccoUpdate.spawnRequest() != null && state.enemySpawns().spawnCuccoAngry(cuccoUpdate.spawnRequest())) {
                state.emit(new EntityCombatEvent(
                        state.entity.slot(),
                        state.entity.type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.WAVE,
                        CuccoMotion.CUCCO_HURT_WAVE_SFX));
            }
            if (cuccoUpdate.boomerangSound()) {
                state.projectiles().playBoomerangSoundIfDue(state.updated);
            }
        }
        return true;
    }
}
