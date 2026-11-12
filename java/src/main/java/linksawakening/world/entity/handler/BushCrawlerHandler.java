package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_BUSH_CRAWLER;
import static linksawakening.world.entity.EntitySnapshots.withDefinition;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityCombatRules;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BushCrawlerMotion;

/** ROM handler stages for BushCrawler. */
final class BushCrawlerHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_BUSH_CRAWLER, EntityTickStage.PHASE_3_BEFORE_LASER, 13, BushCrawlerHandler::phase3BushCrawler);
    }

    private static boolean phase3BushCrawler(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();
        var presentationFlags = state.presentation();

        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && timerGates.handlerLinkCollisionEnabled()) {

            BushCrawlerMotion.Update bushUpdate = state.motion(BushCrawlerMotion.class, BushCrawlerMotion::new)
                    .advance(
                            slotState.entity(),
                            frameInput.frame(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.randomByteSupplier(),
                            frameInput.backgroundCollision(),
                            state.slot().transitionCountdown(slotState.entity().slot()),
                            RoomEntityCombatRules.overlapsLink(
                                    slotState.entity(), frameInput.linkEntityX(), frameInput.linkEntityY()),
                            state.link().actionButtonAHeld(),
                            state.link().actionButtonBHeld(),
                            state.link().linkItemA(),
                            state.link().linkItemB());
            state.slot().setTransitionCountdown(slotState.entity().slot(), bushUpdate.transitionCountdown());
            state.background().setFallingVisualYOffset(slotState.entity().slot(), bushUpdate.visualYOffset());
            slotState.updated(bushUpdate.entity());
            if (bushUpdate.specialState()) {
                slotState.updated(withDefinition(
                        slotState.updated(),
                        state.slotProperties().spriteDefinitionForBushCrawlerState(0x02),
                        slotState.updated().spriteVariant()));
            } else if (bushUpdate.crawlOverlayRendered()) {
                slotState.updated(withDefinition(
                        slotState.updated(),
                        state.slotProperties().spriteDefinitionForBushCrawlerCrawlState(bushUpdate.privateState4()),
                        slotState.updated().spriteVariant()));
            } else {
                slotState.updated(withDefinition(
                        slotState.updated(),
                        state.slotProperties().spriteDefinitionFor(ENTITY_BUSH_CRAWLER),
                        slotState.updated().spriteVariant()));
            }
            if (!bushUpdate.specialState()) {
                // BushCrawlerEntityHandler writes $04 immediately before
                // its default enemy collision path on every interactive
                // normal pass. The static health group is $00.
                state.slot().setHealth(slotState.entity().slot(), 0x04);
            }
            presentationFlags.preserveBushCrawlerPresentation(true);
            if (bushUpdate.liftRequested()
                    && state.liftThrow()
                            .beginBushCrawlerLift(
                                    slotState.index(),
                                    slotState.updated(),
                                    bushUpdate.privateState4(),
                                    frameInput.romLinkDirection())) {
                return false;
            }
        }
        return true;
    }
}
