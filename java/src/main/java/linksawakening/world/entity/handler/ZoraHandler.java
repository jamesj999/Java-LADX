package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_ZORA;

import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityLinkCollisionRules;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityCombatRules;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.ZoraMotion;

/** ROM handler stages for Zora. */
final class ZoraHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_ZORA, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 10, ZoraHandler::phase4Zora);
    }

    private static boolean phase4Zora(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && !frameInput.creditsGameplay()
                && timerGates.handlerLinkCollisionEnabled()) {
            int slot = slotState.entity().slot();
            if (RoomEntityCombatRules.overlapsLink(
                    slotState.entity(), frameInput.linkEntityX(), frameInput.linkEntityY())) {
                // Zora writes ENTITY_PHYSICS_HARMLESS immediately before
                // this collision check, then copies hLinkFinalPosition
                // and calls ResetPegasusBoots. It does not use the common
                // hookshot-chain reset helper.
                state.background().requestLinkPush(slotState.entity(), EntityLinkCollisionRules.ZORA);
            }
            ZoraMotion.Update zoraUpdate = state.motion(ZoraMotion.class, ZoraMotion::new)
                    .advance(
                            slotState.updated(),
                            state.slot().transitionCountdown(slot),
                            state.slot().physicsFlags(slot),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.randomByteSupplier(),
                            state.background().objectQuery());
            slotState.updated(zoraUpdate.entity());
            state.slot().setTransitionCountdown(slot, zoraUpdate.transitionCountdown());
            state.slot().setPhysicsFlags(slot, zoraUpdate.physicsFlags());
            if (zoraUpdate.projectileSpawn() != null) {
                state.enemySpawns().spawnZoraProjectile(zoraUpdate.projectileSpawn());
            }
            if (zoraUpdate.splash()) {
                state.emit(new TransientVfxRequest(
                        TransientVfxType.WATER_SPLASH,
                        slotState.updated().x(),
                        (slotState.updated().y() - slotState.updated().z()) & 0xFF));
                state.emit(new EntityCombatEvent(
                        slot, slotState.entity().type(), 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x0E));
            }
            presentationFlags.preserveZoraPresentation(true);
        }
        return true;
    }
}
