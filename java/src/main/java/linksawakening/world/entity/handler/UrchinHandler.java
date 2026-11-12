package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_URCHIN;
import static linksawakening.world.entity.EntitySnapshots.withDefinition;

import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.*;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityLinkCollisionRules;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.UrchinFrame;
import linksawakening.world.entity.motion.UrchinMotion;

/** ROM handler stages for Urchin. */
final class UrchinHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerInitializer(ENTITY_URCHIN, (entity, random) -> initialize(registry, entity, random));
        registry.registerTick(ENTITY_URCHIN, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 17, UrchinHandler::phase4Urchin);
    }

    private static boolean phase4Urchin(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && timerGates.handlerLinkCollisionEnabled()) {

            int slot = slotState.entity().slot();
            RoomEntityBackgroundInteraction urchinBackgroundInteraction =
                    state.background().interaction();
            if (urchinBackgroundInteraction == null && frameInput.backgroundCollision() != null) {
                urchinBackgroundInteraction =
                        RoomEntityBackgroundInteraction.fromBoolean(frameInput.backgroundCollision());
            }
            UrchinMotion.Update urchinUpdate = state.motion(UrchinMotion.class, UrchinMotion::new)
                    .advance(new UrchinFrame(
                            slotState.updated(),
                            frameInput.frame(),
                            state.slot().physicsFlags(slot),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.romLinkDirection(),
                            frameInput.projectileLinkState().usingShield(),
                            frameInput.swordCollisionActive(),
                            timerGates.handlerLinkCollisionEnabled(),
                            urchinBackgroundInteraction));
            slotState.updated(urchinUpdate.entity());
            state.slot().setPhysicsFlags(slot, urchinUpdate.physicsFlags());
            if (urchinUpdate.pushed()) {
                // UrchinEntityHandler writes $03 for its background probe,
                // then clears wEntitiesIgnoreHitsCountdown immediately.
                state.slot().setIgnoreHits(slot, 0);
            }
            slotState.applyGenericGroundInteraction(urchinUpdate.appliesBackgroundInteraction());
            if (urchinUpdate.linkCollision()) {
                state.background().requestLinkPush(slotState.updated(), EntityLinkCollisionRules.URCHIN);
            }
            if (urchinUpdate.jingleId() >= 0) {
                state.emit(new EntityCombatEvent(
                        slot,
                        slotState.entity().type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        urchinUpdate.jingleId()));
            }
            if (state.sprites() != null) {
                EntitySpriteDefinition urchinDefinition = state.sprites().forUrchinState(frameInput.creditsGameplay());
                slotState.updated(withDefinition(
                        slotState.updated(),
                        urchinDefinition,
                        slotState.updated().spriteVariant()));
            }
            presentationFlags.preserveUrchinPresentation(true);
        }
        return true;
    }

    private static void initialize(EntityHandlerRegistration registry, RoomEntity entity, IntSupplier random) {
        registry.motion(UrchinMotion.class, UrchinMotion::new).initialize(entity.slot());
    }
}
