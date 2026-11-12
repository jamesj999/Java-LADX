package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_MOBLIN_KING;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.*;

/** ROM handler stages for MoblinKing. */
final class MoblinKingHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_MOBLIN_KING, EntityTickStage.PHASE_2_DYING, 0, MoblinKingHandler::phase2MoblinKing);
        registry.registerTick(
                ENTITY_MOBLIN_KING, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 5, MoblinKingHandler::phase4MoblinKing);
    }

    private static boolean phase2MoblinKing(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        state.bossRewards()
                .advanceMoblinKingDestruction(slotState.entity(), frameInput.frame(), frameInput.randomByteSupplier());
        return false;
    }

    private static boolean phase4MoblinKing(EntityFrameExecution state) {
        return new MoblinKingHandlerFrameStep(state).run();
    }
}
