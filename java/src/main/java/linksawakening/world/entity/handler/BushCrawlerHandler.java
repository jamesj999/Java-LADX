package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_BUSH_CRAWLER;


import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.RoomEntityCombatRules;

import linksawakening.world.entity.motion.BushCrawlerMotion;

import static linksawakening.world.entity.EntitySnapshots.withDefinition;

/** ROM handler stages for BushCrawler. */
final class BushCrawlerHandler {
  static void install(EntityHandlerRegistration registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_BUSH_CRAWLER,
        EntityTickStage.PHASE_3_BUSH_CRAWLER,
        state -> {
          if (state.status == EntityStatus.ACTIVE
              && !state.wasInitializing

              && state.handlerLinkCollisionEnabled) {
            BushCrawlerMotion.Update bushUpdate =
                state.bushCrawlerMotion().advance(
                    state.entity,
                    state.frame,
                    state.linkEntityX,
                    state.linkEntityY,
                    state.randomByteSupplier,
                    state.backgroundCollision,
                    state.slot().transitionCountdown(state.entity.slot()),
                    RoomEntityCombatRules.overlapsLink(
                        state.entity, state.linkEntityX, state.linkEntityY),
                    state.link().actionButtonAHeld(),
                    state.link().actionButtonBHeld(),
                    state.link().linkItemA(),
                    state.link().linkItemB());
            state.slot().setTransitionCountdown(state.entity.slot(), bushUpdate.transitionCountdown());
            state.runtime.fallingVisualYOffset[state.entity.slot()] = bushUpdate.visualYOffset();
            state.updated = bushUpdate.entity();
            if (bushUpdate.specialState()) {
              state.updated =
                  withDefinition(
                      state.updated,
                      state.slotProperties().spriteDefinitionForBushCrawlerState(0x02),
                      state.updated.spriteVariant());
            } else if (bushUpdate.crawlOverlayRendered()) {
              state.updated =
                  withDefinition(
                      state.updated,
                      state.slotProperties().spriteDefinitionForBushCrawlerCrawlState(
                          bushUpdate.privateState4()),
                      state.updated.spriteVariant());
            } else {
              state.updated =
                  withDefinition(
                      state.updated,
                      state.slotProperties().spriteDefinitionFor(
                          ENTITY_BUSH_CRAWLER),
                      state.updated.spriteVariant());
            }
            if (!bushUpdate.specialState()) {
              // BushCrawlerEntityHandler writes $04 immediately before
              // its default enemy collision path on every interactive
              // normal pass. The static health group is $00.
              state.slot().setHealth(state.entity.slot(), 0x04);
            }
            state.preserveBushCrawlerPresentation = true;
            if (bushUpdate.liftRequested()
                && state.liftThrow().beginBushCrawlerLift(
                    state.index,
                    state.updated,
                    bushUpdate.privateState4(),
                    state.romLinkDirection)) {
              return false;
            }
          }
          return true;
        });
  }
}
