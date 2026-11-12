package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityPickupInspection; resolved lazily to preserve initialization order. */
interface EntityPickupInspectionServices {
    linksawakening.world.entity.EntityBombInteractions bombInteractions();

    linksawakening.world.entity.EntityMotionRegistry motions();

    linksawakening.world.entity.EntityPhysicsTables physicsTables();

    static EntityPickupInspectionServices bind(RoomEntityRuntime r) {
        return new EntityPickupInspectionServices() {
            public linksawakening.world.entity.EntityBombInteractions bombInteractions() {
                return r.bombInteractions;
            }

            public linksawakening.world.entity.EntityMotionRegistry motions() {
                return r.entityState.motions;
            }

            public linksawakening.world.entity.EntityPhysicsTables physicsTables() {
                return r.entityState.physicsTables;
            }
        };
    }
}
