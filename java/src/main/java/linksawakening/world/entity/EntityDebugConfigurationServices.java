package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityDebugConfiguration; resolved lazily to preserve initialization order. */
interface EntityDebugConfigurationServices {

    EntityCombatTables combatTables();

    EntityMotionRegistry motions();

    static EntityDebugConfigurationServices bind(RoomEntityRuntime r) {
        return new EntityDebugConfigurationServices() {

            public EntityCombatTables combatTables() {
                return r.entityState.combatTables;
            }

            public EntityMotionRegistry motions() {
                return r.entityState.motions;
            }
        };
    }
}
