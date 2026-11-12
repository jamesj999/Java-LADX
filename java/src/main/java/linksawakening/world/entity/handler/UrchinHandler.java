package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_URCHIN;
import static linksawakening.world.entity.EntitySnapshots.withDefinition;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityLinkCollisionRules;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.UrchinMotion;

/** ROM handler stages for Urchin. */
final class UrchinHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_URCHIN, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 17, UrchinHandler::phase4Urchin);
    }

    private static boolean phase4Urchin(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing && state.handlerLinkCollisionEnabled) {

            int slot = state.entity.slot();
            RoomEntityBackgroundInteraction urchinBackgroundInteraction =
                    state.background().interaction();
            if (urchinBackgroundInteraction == null && state.backgroundCollision != null) {
                urchinBackgroundInteraction = RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            UrchinMotion.Update urchinUpdate = state.urchinMotion()
                    .advance(
                            state.updated,
                            state.frame,
                            state.slot().physicsFlags(slot),
                            state.linkEntityX,
                            state.linkEntityY,
                            state.romLinkDirection,
                            state.projectileLinkState.usingShield(),
                            state.swordCollisionActive,
                            state.handlerLinkCollisionEnabled,
                            urchinBackgroundInteraction);
            state.updated = urchinUpdate.entity();
            state.slot().setPhysicsFlags(slot, urchinUpdate.physicsFlags());
            if (urchinUpdate.pushed()) {
                // UrchinEntityHandler writes $03 for its background probe,
                // then clears wEntitiesIgnoreHitsCountdown immediately.
                state.slot().setIgnoreHits(slot, 0);
            }
            state.applyGenericGroundInteraction = urchinUpdate.appliesBackgroundInteraction();
            if (urchinUpdate.linkCollision()) {
                state.background().requestLinkPush(state.updated, EntityLinkCollisionRules.URCHIN);
            }
            if (urchinUpdate.jingleId() >= 0) {
                state.emit(new EntityCombatEvent(
                        slot,
                        state.entity.type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        urchinUpdate.jingleId()));
            }
            if (state.sprites() != null) {
                EntitySpriteDefinition urchinDefinition = state.sprites().forUrchinState(state.creditsGameplay);
                state.updated = withDefinition(state.updated, urchinDefinition, state.updated.spriteVariant());
            }
            state.preserveUrchinPresentation = true;
        }
        return true;
    }
}
