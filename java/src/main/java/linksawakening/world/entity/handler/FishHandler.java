package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_FISH;

import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.FishMotion;

/** ROM handler stages for Fish. */
final class FishHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_FISH, EntityTickStage.PHASE_3_FISH, state -> {
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
                FishMotion.Update fishUpdate = state.fishMotion()
                        .advance(
                                state.entity,
                                state.slot().transitionCountdown(state.entity.slot()),
                                state.randomByteSupplier,
                                state.backgroundCollision);
                state.updated = fishUpdate.entity();
                state.slot().setTransitionCountdown(state.entity.slot(), fishUpdate.transitionCountdown());
                state.slot().setPhysicsFlags(state.entity.slot(), fishUpdate.physicsFlags());
                if (fishUpdate.waterSplash()) {
                    state.emit(new TransientVfxRequest(
                            TransientVfxType.WATER_SPLASH, state.updated.x(), state.updated.y()));
                    state.emit(new EntityCombatEvent(
                            state.updated.slot(),
                            state.updated.type(),
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.JINGLE,
                            0x0E));
                }
            }
            return true;
        });
    }
}
