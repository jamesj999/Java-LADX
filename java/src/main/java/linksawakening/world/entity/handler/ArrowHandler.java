package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_ARROW;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_BOMB;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.PlayerArrowMotion;

/** ROM handler stages for Arrow. */
final class ArrowHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_ARROW, EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE, 12, ArrowHandler::phase1Arrow);
    }

    private static boolean phase1Arrow(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();

        if (slotState.status() == EntityStatus.ACTIVE) {
            if (state.projectiles().playerArrowBombArrow(slotState.entity().slot())
                    && state.motion(PlayerArrowMotion.class, PlayerArrowMotion::new)
                                    .transitionCountdown(slotState.entity().slot())
                            != 0) {
                int explosionSlot = state.playerSpawns().spawnBombFromBombArrow(slotState.entity());
                if (explosionSlot >= 0) {
                    state.emit(new EntityCombatEvent(
                            explosionSlot, ENTITY_BOMB, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x0C));
                }
                state.lifecycle()
                        .disableEntityWithoutPersistence(slotState.entity().slot());
                return false;
            }
            // ArrowEntityHandler jumps straight to rendering/rebound
            // while its transition countdown is nonzero ($6A3D-$6A40).
            boolean hitEntity = state.motion(PlayerArrowMotion.class, PlayerArrowMotion::new)
                                    .transitionCountdown(slotState.entity().slot())
                            == 0
                    && state.projectiles().collidePlayerArrowWithEntities(slotState.entity(), frameInput.frame());
            if (hitEntity
                    && !state.projectiles()
                            .playerArrowBombArrow(slotState.entity().slot())) {
                // func_003_75A2 reaches jr_003_7737 after an ordinary
                // arrow target hit. With no wall-collision byte set on
                // the active arrow, the state-0 arrow unloads there.
                state.lifecycle()
                        .disableEntityWithoutPersistence(slotState.entity().slot());
                return false;
            }
            PlayerArrowMotion.Update arrowUpdate = state.motion(PlayerArrowMotion.class, PlayerArrowMotion::new)
                    .advance(
                            slotState.entity(),
                            frameInput.backgroundCollision(),
                            frameInput.frame(),
                            state.background()
                                    .entityGroundStatus(slotState.entity().slot()),
                            state.background().groundInteractionSideScrolling(),
                            timerGates.handlerInteractive(),
                            timerGates.projectileTimersInteractive());
            if (arrowUpdate.unloaded()) {
                state.lifecycle()
                        .disableEntityWithoutPersistence(slotState.entity().slot());
                return false;
            }
            if (arrowUpdate.collidedWithWall()) {
                state.emit(new EntityCombatEvent(
                        slotState.entity().slot(),
                        ENTITY_ARROW,
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        0x07));
                // AlertSwordMoblins is shared by player arrows, enemy
                // projectiles, and bomb explosions.
                state.projectiles().setSwordMoblinAlertingSoundCounter(0x04);
            }
            state.replaceEntity(arrowUpdate.entity());
            return false;
        }
        return true;
    }
}
