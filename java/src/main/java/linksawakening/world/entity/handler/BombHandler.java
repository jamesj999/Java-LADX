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
        if (state.status == EntityStatus.ACTIVE) {
            BombMotion.Decision bombDecision = state.bombInteractions()
                    .advanceBombEntity(
                            state.index,
                            state.entity,
                            state.projectileEvents,
                            state.linkEntityX,
                            state.linkEntityY,
                            state.projectileLinkState);
            state.entity = state.entityAt(state.index);
            if (bombDecision.phase() == BombMotion.Phase.NORMAL) {
                state.replaceEntity(
                        state.bombInteractions().bombPrivateState4(state.entity.slot()) != 0
                                ? state.liftThrow()
                                        .advanceEnemyBombEntity(state.entity, state.backgroundCollision, state.frame)
                                : state.liftThrow()
                                        .advancePlacedBombEntity(state.entity, state.backgroundCollision, state.frame));
            }
            state.liftThrow()
                    .tryLiftBombIfRequested(
                            state.index,
                            bombDecision,
                            state.linkEntityX,
                            state.linkEntityY,
                            state.linkZ,
                            state.romLinkDirection);
            return false;
        }
        return true;
    }

    private static boolean advanceBombPresentation(EntityFrameExecution state) {
        BombMotion.Decision bombDecision = state.bombInteractions()
                .advanceBombEntity(
                        state.index,
                        state.entity,
                        state.projectileEvents,
                        state.linkEntityX,
                        state.linkEntityY,
                        state.projectileLinkState);
        state.entity = state.entityAt(state.index);
        if (bombDecision.unloadAfterPresentation()) {
            return false;
        }

        return true;
    }

    private static boolean advanceStunnedBomb(EntityFrameExecution state) {
        if (!advanceBombPresentation(state)) return false;
        state.entity = state.bombInteractions()
                .applyBombStunnedPostActiveWork(state.entity, state.backgroundCollision, state.frame);
        state.replaceEntity(state.entity);

        return true;
    }
}
