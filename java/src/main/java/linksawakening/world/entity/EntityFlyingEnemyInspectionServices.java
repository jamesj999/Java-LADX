package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityFlyingEnemyInspection; resolved lazily to preserve initialization order. */
interface EntityFlyingEnemyInspectionServices {
    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    static EntityFlyingEnemyInspectionServices bind(RoomEntityRuntime r) {
        return new EntityFlyingEnemyInspectionServices() {
            public EntityMotionRegistry motions() {
                return r.entityState.motions;
            }

            public EntityPhysicsTables physicsTables() {
                return r.entityState.physicsTables;
            }
        };
    }
}
