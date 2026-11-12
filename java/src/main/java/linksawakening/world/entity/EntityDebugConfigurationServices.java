package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityDebugConfiguration; resolved lazily to preserve initialization order. */
interface EntityDebugConfigurationServices {

    linksawakening.world.entity.EntityCombatTables combatTables();

    linksawakening.world.entity.EntityMotionRegistry motions();

    static EntityDebugConfigurationServices bind(RoomEntityRuntime r) {
        return new EntityDebugConfigurationServices() {

            public linksawakening.world.entity.EntityCombatTables combatTables() {
                return r.entityState.combatTables;
            }

            public linksawakening.world.entity.EntityMotionRegistry motions() {
                return r.entityState.motions;
            }
        };
    }
}
