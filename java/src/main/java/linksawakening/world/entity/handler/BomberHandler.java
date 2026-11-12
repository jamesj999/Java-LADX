package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.BOMBER_THROW_JINGLE_ID;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_BOMB;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_BOMBER;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BomberMotion;

/** ROM handler stages for Bomber. */
final class BomberHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_BOMBER, EntityTickStage.PHASE_2_AFTER_LEEVER_CONTACT, 0, BomberHandler::phase2Bomber);
    }

    private static boolean phase2Bomber(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            BomberMotion.Update bomberUpdate = state.motion(BomberMotion.class, BomberMotion::new)
                    .advance(
                            slotState.entity(),
                            state.slot().transitionCountdown(slotState.entity().slot()),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.romLinkDirection(),
                            frameInput.swordCollisionActive(),
                            frameInput.randomByteSupplier(),
                            frameInput.backgroundCollision());
            slotState.updated(bomberUpdate.entity());
            state.slot().setTransitionCountdown(slotState.entity().slot(), bomberUpdate.transitionCountdown());
            if (bomberUpdate.bombSpawn() != null) {
                BomberMotion.BombSpawn spawn = bomberUpdate.bombSpawn();
                int bombSlot = state.enemySpawns()
                        .spawnEnemyBomb(
                                spawn.x(),
                                spawn.y(),
                                spawn.z(),
                                spawn.transitionCountdown(),
                                spawn.speedX(),
                                spawn.speedY(),
                                spawn.speedZ());
                if (bombSlot >= 0 && bomberUpdate.playJingle()) {
                    state.emit(new EntityCombatEvent(
                            bombSlot,
                            ENTITY_BOMB,
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.JINGLE,
                            BOMBER_THROW_JINGLE_ID));
                }
            }
        }
        return true;
    }
}
