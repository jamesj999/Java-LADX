package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by GenieEntityController; resolved lazily to preserve initialization order. */
interface GenieEntityControllerServices {
    int findFreeEntitySlot();

    EntityBossRewards bossRewards();

    EntityCombatTables combatTables();

    EntityEventChannels events();

    EntityLiftThrowController liftThrow();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntityPlayerSpawns entityPlayerSpawns();

    EntitySlotLifecycle lifecycle();

    EntitySlotProperties slotProperties();

    RoomEntity[] slots();

    void initializeHealthGroup(int slot);

    static GenieEntityControllerServices bind(RoomEntityRuntime r) {
        return new Binding(() -> r);
    }

    final class Binding implements GenieEntityControllerServices {
        private final java.util.function.Supplier<RoomEntityRuntime> room;

        Binding(java.util.function.Supplier<RoomEntityRuntime> room) {
            this.room = room;
        }

        public int findFreeEntitySlot() {
            return room.get().findFreeEntitySlot();
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

        public EntityMotionRegistry motions() {
            return room.get().entityState.motions;
        }

        public EntityPhysicsTables physicsTables() {
            return room.get().entityState.physicsTables;
        }

        public EntityPlayerSpawns entityPlayerSpawns() {
            return room.get().entityPlayerSpawns;
        }

        public EntitySlotLifecycle lifecycle() {
            return room.get().lifecycle;
        }

        public EntitySlotProperties slotProperties() {
            return room.get().slotProperties;
        }

        public RoomEntity[] slots() {
            return room.get().slots;
        }

        public void initializeHealthGroup(int slot) {
            room.get().initializeHealthGroup(slot);
        }
    }
}
