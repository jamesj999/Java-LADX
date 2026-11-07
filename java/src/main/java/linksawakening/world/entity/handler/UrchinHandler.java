package linksawakening.world.entity.handler;

import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.EntityLinkCollisionRules;

import linksawakening.world.entity.motion.UrchinMotion;

import linksawakening.entity.EntitySpriteDefinition;
import static linksawakening.world.entity.EntitySnapshots.withDefinition;

/** ROM handler stages for Urchin. */
final class UrchinHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_URCHIN,
        EntityTickStage.PHASE_4_URCHIN,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing

              && state.handlerLinkCollisionEnabled) {
            int slot = state.entity.slot();
            RoomEntityBackgroundInteraction urchinBackgroundInteraction =
                state.runtime.backgroundInteraction;
            if (urchinBackgroundInteraction == null && state.backgroundCollision != null) {
              urchinBackgroundInteraction =
                  RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            UrchinMotion.Update urchinUpdate =
                state.runtime.urchinMotion.advance(
                    state.updated,
                    state.frame,
                    state.runtime.enemyPhysicsFlags[slot],
                    state.linkEntityX,
                    state.linkEntityY,
                    state.romLinkDirection,
                    state.projectileLinkState.usingShield(),
                    state.swordCollisionActive,
                    state.handlerLinkCollisionEnabled,
                    urchinBackgroundInteraction);
            state.updated = urchinUpdate.entity();
            state.runtime.enemyPhysicsFlags[slot] = urchinUpdate.physicsFlags();
            if (urchinUpdate.pushed()) {
              // UrchinEntityHandler writes $03 for its background probe,
              // then clears wEntitiesIgnoreHitsCountdown immediately.
              state.runtime.enemyIgnoreHitsCountdown[slot] = 0;
            }
            state.applyGenericGroundInteraction = urchinUpdate.appliesBackgroundInteraction();
            if (urchinUpdate.linkCollision()) {
              state.runtime.backgroundMechanics.requestLinkPush(
                  state.updated, EntityLinkCollisionRules.URCHIN);
            }
            if (urchinUpdate.jingleId() >= 0) {
              state.emit(
                  new EntityCombatEvent(
                      slot,
                      state.entity.type(),
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.JINGLE,
                      urchinUpdate.jingleId()));
            }
            if (state.runtime.spriteHandlers != null) {
              EntitySpriteDefinition urchinDefinition =
                  state.runtime.spriteHandlers.forUrchinState(state.creditsGameplay);
              state.updated =
                  withDefinition(state.updated, urchinDefinition, state.updated.spriteVariant());
            }
            state.preserveUrchinPresentation = true;
          }
          return true;
        });
  }
}
