package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_MOBLIN;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_OCTOROK;

import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityTickStage;

/** ROM handler stages for Moblin. */
final class MoblinHandler {
    static void install(EntityHandlerRegistration registry) {
        for (int type = 0; type < 256; type++) {
            if (((type == ENTITY_OCTOROK || type == ENTITY_MOBLIN))) {
                registry.registerTick(type, EntityTickStage.PHASE_2_MOBLIN, state -> {
                    {
                        state.roamingEnemyMotion().initialize(state.entity.slot());
                    }
                    return true;
                });
            }
        }
    }
}
