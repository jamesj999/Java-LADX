package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityRoomInitialization; resolved lazily to preserve initialization order. */
interface EntityRoomInitializationServices {
    void clearBombFinalPresentations();

    EntityWarpController warpState();

    boolean indoorRoom();

    int initialHealth(int type);

    EntityBombInteractions bombInteractions();

    EntityBossRewards bossRewards();

    EntityCombatTables combatTables();

    EntityLiftThrowController liftThrow();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntityPickupController pickups();

    EntityRewardController rewards();

    EntitySlotProperties slotProperties();

    EntityStoryController story();

    GenieEntityController genie();

    RoomEntity[] slots();

    void initializeHealthGroup(int slot);

    static EntityRoomInitializationServices bind(RoomEntityRuntime r) {
        return new Binding(() -> r);
    }

    final class Binding implements EntityRoomInitializationServices {
        private final java.util.function.Supplier<RoomEntityRuntime> room;

        Binding(java.util.function.Supplier<RoomEntityRuntime> room) {
            this.room = room;
        }

        public void clearBombFinalPresentations() {
            room.get().bombInteractions.clearBombFinalPresentations();
        }

        public EntityWarpController warpState() {
            return room.get().warpState;
        }

        public boolean indoorRoom() {
            return room.get().indoorRoom;
        }

        public int initialHealth(int type) {
            return room.get().initialHealth(type);
        }

        public EntityBombInteractions bombInteractions() {
            return room.get().bombInteractions;
        }

        public EntityBossRewards bossRewards() {
            return room.get().bossRewards;
        }

        public EntityCombatTables combatTables() {
            return room.get().entityState.combatTables;
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

        public EntityPickupController pickups() {
            return room.get().pickups;
        }

        public EntityRewardController rewards() {
            return room.get().rewards;
        }

        public EntitySlotProperties slotProperties() {
            return room.get().slotProperties;
        }

        public EntityStoryController story() {
            return room.get().story;
        }

        public GenieEntityController genie() {
            return room.get().genie;
        }

        public RoomEntity[] slots() {
            return room.get().slots;
        }

        public void initializeHealthGroup(int slot) {
            room.get().initializeHealthGroup(slot);
        }
    }
}
