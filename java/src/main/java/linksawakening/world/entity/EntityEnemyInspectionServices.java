package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityEnemyInspection; resolved lazily to preserve initialization order. */
interface EntityEnemyInspectionServices {
    EntityBossRewards bossRewards();

    EntityCombatTables combatTables();

    EntityEventChannels events();

    EntityLiftThrowController liftThrow();

    EntityLinkState linkState();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntityRoomInspection inspection();

    EntityRoomInspection roomInspection();

    EntityRoomVariables roomVariables();

    EntitySlotProperties slotProperties();

    RoomEntity[] slots();

    static EntityEnemyInspectionServices bind(RoomEntityRuntime r) {
        return new Binding(() -> r);
    }

    final class Binding implements EntityEnemyInspectionServices {
        private final java.util.function.Supplier<RoomEntityRuntime> room;

        Binding(java.util.function.Supplier<RoomEntityRuntime> room) {
            this.room = room;
        }

        public EntityBossRewards bossRewards() {
            return room.get().bossRewards;
        }

        public EntityCombatTables combatTables() {
            return room.get().entityState.combatTables;
        }

        public EntityEventChannels events() {
            return room.get().entityState.events;
        }

        public EntityLiftThrowController liftThrow() {
            return room.get().liftThrow;
        }

        public EntityLinkState linkState() {
            return room.get().entityState.linkState;
        }

        public EntityMotionRegistry motions() {
            return room.get().entityState.motions;
        }

        public EntityPhysicsTables physicsTables() {
            return room.get().entityState.physicsTables;
        }

        public EntityRoomInspection inspection() {
            return room.get().inspection();
        }

        public EntityRoomInspection roomInspection() {
            return room.get().roomInspection;
        }

        public EntityRoomVariables roomVariables() {
            return room.get().entityState.roomVariables;
        }

        public EntitySlotProperties slotProperties() {
            return room.get().slotProperties;
        }

        public RoomEntity[] slots() {
            return room.get().slots;
        }
    }
}
