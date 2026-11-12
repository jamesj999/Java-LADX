package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityDebugConfiguration; resolved lazily to preserve initialization order. */
interface EntityDebugConfigurationServices {

    EntityCombatTables combatTables();

    EntityMotionRegistry motions();

    static EntityDebugConfigurationServices bind(RoomEntityRuntime r) {
        return new Binding(() -> r);
    }

    final class Binding implements EntityDebugConfigurationServices {
        private final java.util.function.Supplier<RoomEntityRuntime> room;

        Binding(java.util.function.Supplier<RoomEntityRuntime> room) {
            this.room = room;
        }

        public EntityCombatTables combatTables() {
            return room.get().entityState.combatTables;
        }

        public EntityMotionRegistry motions() {
            return room.get().entityState.motions;
        }
    }
}
