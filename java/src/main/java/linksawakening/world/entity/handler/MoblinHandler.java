package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_MOBLIN;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_OCTOROK;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistry;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.motion.RoamingEnemyMotion;

/** ROM handler stages for Moblin. */
final class MoblinHandler {
    static void install(EntityHandlerRegistry registry) {
        registry.registerTick(ENTITY_OCTOROK, EntityTickStage.PHASE_2_AFTER_INITIALIZE, 0, MoblinHandler::phase2Moblin);
        registry.registerTick(ENTITY_MOBLIN, EntityTickStage.PHASE_2_AFTER_INITIALIZE, 0, MoblinHandler::phase2Moblin);
    }

    private static boolean phase2Moblin(EntityFrameExecution state) {
        var slotState = state.slotFrame();

        state.motion(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                .initializeWithProjectileDelay(slotState.entity().slot());

        return true;
    }
}
