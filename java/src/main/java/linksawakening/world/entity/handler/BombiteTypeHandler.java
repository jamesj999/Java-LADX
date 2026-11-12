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
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BombiteFrame;
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
        var slotState = state.slotFrame();

        state.motion(BombiteMotion.class, BombiteMotion::new)
                .initialize(slotState.entity().slot());
        state.slot().setSlowTimerInitialized(slotState.entity().slot(), true);

        return true;
    }

    private static boolean phase3BombiteType(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            BombiteMotion.Update bombiteUpdate = state.motion(BombiteMotion.class, BombiteMotion::new)
                    .advance(new BombiteFrame(
                            slotState.entity(),
                            frameInput.frame(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            state.slot().transitionCountdown(slotState.entity().slot()),
                            state.slot()
                                    .slowTransitionCountdown(slotState.entity().slot()),
                            state.bombInteractions()
                                    .bombPrivateCountdown1(slotState.entity().slot()),
                            state.slot().ignoreHits(slotState.entity().slot()),
                            state.link().runningWithPegasusBoots(),
                            frameInput.randomByteSupplier(),
                            frameInput.backgroundCollision()));
            slotState.updated(withVariant(bombiteUpdate.entity(), bombiteUpdate.spriteVariant()));
            presentationFlags.preserveBombitePresentation(true);
            state.slot().setTransitionCountdown(slotState.entity().slot(), bombiteUpdate.transitionCountdown());
            state.slot().setSlowTransitionCountdown(slotState.entity().slot(), bombiteUpdate.slowTransitionCountdown());
            state.bombInteractions()
                    .setBombPrivateCountdown1(slotState.entity().slot(), bombiteUpdate.privateCountdown1());
            if (bombiteUpdate.bumpJingle()) {
                state.emit(new EntityCombatEvent(
                        slotState.entity().slot(),
                        slotState.entity().type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        0x09));
            }
            boolean entityCollisionExplosion = slotState.entity().type() == ENTITY_BOUNCING_BOMBITE
                    && state.motion(BombiteMotion.class, BombiteMotion::new)
                                    .state(slotState.entity().slot())
                            == 2
                    && state.projectiles().collideBouncingBombiteWithEntities(slotState.updated(), frameInput.frame());
            if (bombiteUpdate.explode() || entityCollisionExplosion) {
                // BombiteExplode writes the already-positioned source
                // coordinates into a fresh type-$02 enemy bomb and then
                // lets ConfigureNewEntity supply z=0 and ignore-hits=1.
                int bombSlot = state.enemySpawns()
                        .spawnEnemyBomb(
                                slotState.updated().x(), slotState.updated().y(), 0, BOMBITE_EXPLOSION_COUNTDOWN);
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
                state.lifecycle()
                        .disableEntityWithoutPersistence(slotState.entity().slot());
                return false;
            }
        }
        return true;
    }
}
