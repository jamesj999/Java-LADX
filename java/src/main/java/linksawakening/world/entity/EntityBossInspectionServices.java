package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityBossInspection; resolved lazily to preserve initialization order. */
interface EntityBossInspectionServices {
    EntityBossRewards bossRewards();

    EntityEnemyInspection enemyInspection();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    static EntityBossInspectionServices bind(RoomEntityRuntime r) {
        return new Binding(() -> r);
    }

    final class Binding implements EntityBossInspectionServices {
        private final java.util.function.Supplier<RoomEntityRuntime> room;

        Binding(java.util.function.Supplier<RoomEntityRuntime> room) {
            this.room = room;
        }

        public EntityBossRewards bossRewards() {
            return room.get().bossRewards;
        }

        public EntityEnemyInspection enemyInspection() {
            return room.get().enemyInspection;
        }

        public EntityMotionRegistry motions() {
            return room.get().entityState.motions;
        }

        public EntityPhysicsTables physicsTables() {
            return room.get().entityState.physicsTables;
        }
    }
}
