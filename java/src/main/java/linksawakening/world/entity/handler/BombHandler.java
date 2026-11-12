package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_BOMB;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.BombMotion;

/** ROM handler stages for Bomb. */
final class BombHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_BOMB, EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE, 11, BombHandler::advanceActiveBomb);
        registry.registerTick(ENTITY_BOMB, EntityTickStage.PHASE_2_THROWN, 0, BombHandler::advanceBombPresentation);
        registry.registerTick(ENTITY_BOMB, EntityTickStage.PHASE_2_STUNNED, 0, BombHandler::advanceStunnedBomb);
    }

    private static boolean advanceActiveBomb(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE) {
            BombMotion.Decision bombDecision = state.bombInteractions()
                    .advanceBombEntity(
                            slotState.index(),
                            slotState.entity(),
                            frameInput.projectileEvents(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.projectileLinkState());
            slotState.entity(state.entityAt(slotState.index()));
            if (bombDecision.phase() == BombMotion.Phase.NORMAL) {
                state.replaceEntity(
                        state.bombInteractions()
                                                .bombPrivateState4(
                                                        slotState.entity().slot())
                                        != 0
                                ? state.liftThrow()
                                        .advanceEnemyBombEntity(
                                                slotState.entity(),
                                                frameInput.backgroundCollision(),
                                                frameInput.frame())
                                : state.liftThrow()
                                        .advancePlacedBombEntity(
                                                slotState.entity(),
                                                frameInput.backgroundCollision(),
                                                frameInput.frame()));
            }
            state.liftThrow()
                    .tryLiftBombIfRequested(
                            slotState.index(),
                            bombDecision,
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            frameInput.linkZ(),
                            frameInput.romLinkDirection());
            return false;
        }
        return true;
    }

    private static boolean advanceBombPresentation(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        BombMotion.Decision bombDecision = state.bombInteractions()
                .advanceBombEntity(
                        slotState.index(),
                        slotState.entity(),
                        frameInput.projectileEvents(),
                        frameInput.linkEntityX(),
                        frameInput.linkEntityY(),
                        frameInput.projectileLinkState());
        slotState.entity(state.entityAt(slotState.index()));
        if (bombDecision.unloadAfterPresentation()) {
            return false;
        }

        return true;
    }

    private static boolean advanceStunnedBomb(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (!advanceBombPresentation(state)) return false;
        slotState.entity(state.bombInteractions()
                .applyBombStunnedPostActiveWork(
                        slotState.entity(), frameInput.backgroundCollision(), frameInput.frame()));
        state.replaceEntity(slotState.entity());

        return true;
    }
}
