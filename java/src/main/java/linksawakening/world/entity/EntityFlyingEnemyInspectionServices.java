package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityFlyingEnemyInspection; resolved lazily to preserve initialization order. */
interface EntityFlyingEnemyInspectionServices {
    linksawakening.world.entity.EntityMotionRegistry motions();

    linksawakening.world.entity.EntityPhysicsTables physicsTables();

    static EntityFlyingEnemyInspectionServices bind(RoomEntityRuntime r) {
        return new EntityFlyingEnemyInspectionServices() {
            public linksawakening.world.entity.EntityMotionRegistry motions() {
                return r.entityState.motions;
            }

            public linksawakening.world.entity.EntityPhysicsTables physicsTables() {
                return r.entityState.physicsTables;
            }
        };
    }
}
