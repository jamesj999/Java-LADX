package linksawakening.world.entity;

/** Dependencies used by EntityFlyingEnemyInspection; resolved lazily to preserve initialization order. */
interface EntityFlyingEnemyInspectionServices {
    linksawakening.world.entity.EntityMotionSet motionSet();

    linksawakening.world.entity.EntityPhysicsTables physicsTables();

    static EntityFlyingEnemyInspectionServices bind(RoomEntityRuntime r) {
        return new EntityFlyingEnemyInspectionServices() {
            public linksawakening.world.entity.EntityMotionSet motionSet() {
                return r.entityState.motionSet;
            }

            public linksawakening.world.entity.EntityPhysicsTables physicsTables() {
                return r.entityState.physicsTables;
            }
        };
    }
}
