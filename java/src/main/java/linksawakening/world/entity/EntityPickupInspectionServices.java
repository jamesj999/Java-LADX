package linksawakening.world.entity;

/** Dependencies used by EntityPickupInspection; resolved lazily to preserve initialization order. */
interface EntityPickupInspectionServices {
    linksawakening.world.entity.EntityBombInteractions bombInteractions();

    linksawakening.world.entity.EntityMotionSet motionSet();

    linksawakening.world.entity.EntityPhysicsTables physicsTables();

    static EntityPickupInspectionServices bind(RoomEntityRuntime r) {
        return new EntityPickupInspectionServices() {
            public linksawakening.world.entity.EntityBombInteractions bombInteractions() {
                return r.bombInteractions;
            }

            public linksawakening.world.entity.EntityMotionSet motionSet() {
                return r.entityState.motionSet;
            }

            public linksawakening.world.entity.EntityPhysicsTables physicsTables() {
                return r.entityState.physicsTables;
            }
        };
    }
}
