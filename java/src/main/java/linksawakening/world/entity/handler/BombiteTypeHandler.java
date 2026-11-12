package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.BOMBITE_EXPLOSION_COUNTDOWN;
import static linksawakening.world.entity.EntityRomConstants.BOMBITE_EXPLOSION_SOUND_ID;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_BOMB;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_BOUNCING_BOMBITE;
import static linksawakening.world.entity.EntitySnapshots.withVariant;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.BombiteMotion;

/** ROM handler stages for BombiteType. */
final class BombiteTypeHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_BOUNCING_BOMBITE,
                EntityTickStage.PHASE_2_AFTER_INITIALIZE,
                9,
                BombiteTypeHandler::phase2BombiteType);
        registry.registerTick(
                ENTITY_TIMER_BOMBITE,
                EntityTickStage.PHASE_2_AFTER_INITIALIZE,
                9,
                BombiteTypeHandler::phase2BombiteType);
        registry.registerTick(
                ENTITY_BOUNCING_BOMBITE,
                EntityTickStage.PHASE_3_BEFORE_LASER,
                0,
                BombiteTypeHandler::phase3BombiteType);
        registry.registerTick(
                ENTITY_TIMER_BOMBITE, EntityTickStage.PHASE_3_BEFORE_LASER, 0, BombiteTypeHandler::phase3BombiteType);
    }

    private static boolean phase2BombiteType(EntityFrameExecution state) {
        state.bombiteMotion().initialize(state.entity.slot());
        state.slot().setSlowTimerInitialized(state.entity.slot(), true);

        return true;
    }

    private static boolean phase3BombiteType(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            BombiteMotion.Update bombiteUpdate = state.bombiteMotion()
                    .advance(
                            state.entity,
                            state.frame,
                            state.linkEntityX,
                            state.linkEntityY,
                            state.slot().transitionCountdown(state.entity.slot()),
                            state.slot().slowTransitionCountdown(state.entity.slot()),
                            state.bombInteractions().bombPrivateCountdown1(state.entity.slot()),
                            state.slot().ignoreHits(state.entity.slot()),
                            state.link().runningWithPegasusBoots(),
                            state.randomByteSupplier,
                            state.backgroundCollision);
            state.updated = withVariant(bombiteUpdate.entity(), bombiteUpdate.spriteVariant());
            state.preserveBombitePresentation = true;
            state.slot().setTransitionCountdown(state.entity.slot(), bombiteUpdate.transitionCountdown());
            state.slot().setSlowTransitionCountdown(state.entity.slot(), bombiteUpdate.slowTransitionCountdown());
            state.bombInteractions().setBombPrivateCountdown1(state.entity.slot(), bombiteUpdate.privateCountdown1());
            if (bombiteUpdate.bumpJingle()) {
                state.emit(new EntityCombatEvent(
                        state.entity.slot(),
                        state.entity.type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        0x09));
            }
            boolean entityCollisionExplosion = state.entity.type() == ENTITY_BOUNCING_BOMBITE
                    && state.bombiteMotion().state(state.entity.slot()) == 2
                    && state.projectiles().collideBouncingBombiteWithEntities(state.updated, state.frame);
            if (bombiteUpdate.explode() || entityCollisionExplosion) {
                // BombiteExplode writes the already-positioned source
                // coordinates into a fresh type-$02 enemy bomb and then
                // lets ConfigureNewEntity supply z=0 and ignore-hits=1.
                int bombSlot = state.enemySpawns()
                        .spawnEnemyBomb(state.updated.x(), state.updated.y(), 0, BOMBITE_EXPLOSION_COUNTDOWN);
                if (bombSlot >= 0) {
                    state.slot().setIgnoreHits(bombSlot, 0x01);
                    state.emit(new EntityCombatEvent(
                            bombSlot,
                            ENTITY_BOMB,
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.NOISE,
                            BOMBITE_EXPLOSION_SOUND_ID));
                }
                state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
                return false;
            }
        }
        return true;
    }
}
