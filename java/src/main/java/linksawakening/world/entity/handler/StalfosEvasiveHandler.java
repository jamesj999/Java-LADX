package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_STALFOS_EVASIVE;
import static linksawakening.world.entity.EntitySnapshots.withDefinition;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.StalfosEvasiveMotion;

/** ROM handler stages for StalfosEvasive. */
final class StalfosEvasiveHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_STALFOS_EVASIVE,
                EntityTickStage.PHASE_3_AFTER_LASER,
                9,
                StalfosEvasiveHandler::phase3StalfosEvasive);
    }

    private static boolean phase3StalfosEvasive(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            StalfosEvasiveMotion.Update evasiveUpdate = state.stalfosEvasiveMotion()
                    .advance(
                            state.entity,
                            state.frame,
                            state.linkEntityX,
                            state.linkEntityY,
                            state.link().actionButtonsHeld(),
                            state.slot().transitionCountdown(state.entity.slot()),
                            state.slot().ignoreHits(state.entity.slot()),
                            state.randomByteSupplier,
                            state.backgroundCollision,
                            state.roomInspection().entityMapId());
            state.updated = evasiveUpdate.entity();
            state.slot().setTransitionCountdown(state.entity.slot(), evasiveUpdate.transitionCountdown());
            if (evasiveUpdate.cloneRequest() != null) {
                state.enemySpawns().spawnEvasiveClone(state.entity, evasiveUpdate.cloneRequest());
            }
            if (evasiveUpdate.swordPokeRequested()) {
                state.emit(new EntityCombatEvent(
                        state.entity.slot(),
                        state.entity.type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        0x07));
                state.emit(new TransientVfxRequest(
                        TransientVfxType.SWORD_POKE,
                        state.updated.x(),
                        (state.updated.y() - state.updated.z()) & 0xFF));
            }
            if (evasiveUpdate.unloadRequested()) {
                state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
                return false;
            }
            if (state.sprites() != null) {
                EntitySpriteDefinition evasiveDefinition = state.sprites()
                        .forStalfosEvasiveState(state.stalfosEvasiveMotion().privateState1(state.entity.slot()));
                state.updated = withDefinition(state.updated, evasiveDefinition, state.updated.spriteVariant());
            }
        }
        return true;
    }
}
