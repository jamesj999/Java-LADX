package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityPickupController; resolved lazily to preserve initialization order. */
interface EntityPickupControllerServices {
    boolean indoorRoom();

    int clearEntity(int slot);

    int findFreeEntitySlot();

    int initialHealth(int type);

    EntityBossRewards bossRewards();

    EntityCombatTables combatTables();

    EntityEventChannels events();

    EntityLinkState linkState();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntityPresentationState presentationState();

    EntityRoomVariables roomVariables();

    EntitySlotLifecycle lifecycle();

    EntitySlotProperties slotProperties();

    EntityStoryController story();

    RoomEntity[] slots();

    linksawakening.world.room.ChestContentsTable chestContentsTable();

    void initializeHealthGroup(int slot);

    static EntityPickupControllerServices bind(RoomEntityRuntime r) {
        return new Binding(() -> r);
    }

    final class Binding implements EntityPickupControllerServices {
        private final java.util.function.Supplier<RoomEntityRuntime> room;

        Binding(java.util.function.Supplier<RoomEntityRuntime> room) {
            this.room = room;
        }

        public boolean indoorRoom() {
            return room.get().indoorRoom;
        }

        public int clearEntity(int slot) {
            return room.get().clearEntity(slot);
        }

        public int findFreeEntitySlot() {
            return room.get().findFreeEntitySlot();
        }

        public int initialHealth(int type) {
            return room.get().initialHealth(type);
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

        public EntityLinkState linkState() {
            return room.get().entityState.linkState;
        }

        public EntityMotionRegistry motions() {
            return room.get().entityState.motions;
        }

        public EntityPhysicsTables physicsTables() {
            return room.get().entityState.physicsTables;
        }

        public EntityPresentationState presentationState() {
            return room.get().entityState.presentationState;
        }

        public EntityRoomVariables roomVariables() {
            return room.get().entityState.roomVariables;
        }

        public EntitySlotLifecycle lifecycle() {
            return room.get().lifecycle;
        }

        public EntitySlotProperties slotProperties() {
            return room.get().slotProperties;
        }

        public EntityStoryController story() {
            return room.get().story;
        }

        public RoomEntity[] slots() {
            return room.get().slots;
        }

        public linksawakening.world.room.ChestContentsTable chestContentsTable() {
            return room.get().chestContentsTable;
        }

        public void initializeHealthGroup(int slot) {
            room.get().initializeHealthGroup(slot);
        }
    }
}
