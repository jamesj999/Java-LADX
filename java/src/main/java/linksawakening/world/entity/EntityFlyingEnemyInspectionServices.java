package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityFlyingEnemyInspection; resolved lazily to preserve initialization order. */
interface EntityFlyingEnemyInspectionServices {
    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    static EntityFlyingEnemyInspectionServices bind(RoomEntityRuntime r) {
        return new Binding(() -> r);
    }

    final class Binding implements EntityFlyingEnemyInspectionServices {
        private final java.util.function.Supplier<RoomEntityRuntime> room;

        Binding(java.util.function.Supplier<RoomEntityRuntime> room) {
            this.room = room;
        }

        public EntityMotionRegistry motions() {
            return room.get().entityState.motions;
        }

        public EntityPhysicsTables physicsTables() {
            return room.get().entityState.physicsTables;
        }
    }
}
