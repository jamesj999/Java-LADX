package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityPickupInspection; resolved lazily to preserve initialization order. */
interface EntityPickupInspectionServices {
    EntityBombInteractions bombInteractions();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    static EntityPickupInspectionServices bind(RoomEntityRuntime r) {
        return new EntityPickupInspectionServices() {
            public EntityBombInteractions bombInteractions() {
                return r.bombInteractions;
            }

            public EntityMotionRegistry motions() {
                return r.entityState.motions;
            }

            public EntityPhysicsTables physicsTables() {
                return r.entityState.physicsTables;
            }
        };
    }
}
