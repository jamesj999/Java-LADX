package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.BOMBER_THROW_JINGLE_ID;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_BOMB;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_MAD_BOMBER;
import static linksawakening.world.entity.EntitySnapshots.withVariant;

import java.util.function.IntSupplier;
import linksawakening.world.entity.*;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.MadBomberMotion;

/** ROM handler stages for MadBomber. */
final class MadBomberHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerInitializer(ENTITY_MAD_BOMBER, (entity, random) -> initialize(registry, entity, random));
        registry.registerTick(
                ENTITY_MAD_BOMBER, EntityTickStage.PHASE_2_AFTER_LEEVER_CONTACT, 1, MadBomberHandler::phase2MadBomber);
    }

    private static boolean phase2MadBomber(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            // MadBomberEntityHandler rewrites this handler-owned drop
            // byte on every interactive pass before its state dispatch.
            state.lifecycle().setDroppedItemBySlot(slotState.entity().slot(), 0x3C);
            MadBomberMotion.Update madBomberUpdate = state.motion(MadBomberMotion.class, MadBomberMotion::new)
                    .advance(
                            slotState.entity(),
                            state.slot().transitionCountdown(slotState.entity().slot()),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            state.slot().flashCountdown(slotState.entity().slot()),
                            frameInput.randomByteSupplier());
            slotState.updated(withVariant(madBomberUpdate.entity(), madBomberUpdate.spriteVariant()));
            presentationFlags.preserveMadBomberPresentation(true);
            state.slot().setTransitionCountdown(slotState.entity().slot(), madBomberUpdate.transitionCountdown());
            if (madBomberUpdate.bombSpawn() != null) {
                MadBomberMotion.BombSpawn spawn = madBomberUpdate.bombSpawn();
                int bombSlot = state.enemySpawns()
                        .spawnEnemyBomb(
                                spawn.x(),
                                spawn.y(),
                                spawn.z(),
                                spawn.transitionCountdown(),
                                spawn.speedX(),
                                spawn.speedY(),
                                spawn.speedZ());
                if (bombSlot >= 0 && madBomberUpdate.playJingle()) {
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

    private static void initialize(EntityHandlerRegistration registry, RoomEntity entity, IntSupplier random) {
        registry.motion(MadBomberMotion.class, MadBomberMotion::new).initialize(entity.slot());
    }
}
