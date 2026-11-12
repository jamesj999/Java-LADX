package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityBossRewards; resolved lazily to preserve initialization order. */
interface EntityBossRewardsServices {
    int findFreeEntitySlot();

    int initialHealth(int type);

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    EntityBackgroundMechanics backgroundMechanics();

    EntityCombatTables combatTables();

    EntityEventChannels events();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntityPickupController pickups();

    EntityRoomInspection roomInspection();

    EntityRoomVariables roomVariables();

    EntitySlotLifecycle lifecycle();

    EntitySlotProperties slotProperties();

    RoomEntity[] slots();

    void initializeHealthGroup(int slot);

    static EntityBossRewardsServices bind(RoomEntityRuntime r) {
        return new Binding(() -> r);
    }

    final class Binding implements EntityBossRewardsServices {
        private final java.util.function.Supplier<RoomEntityRuntime> room;

        Binding(java.util.function.Supplier<RoomEntityRuntime> room) {
            this.room = room;
        }

        public int findFreeEntitySlot() {
            return room.get().findFreeEntitySlot();
        }

        public int initialHealth(int type) {
            return room.get().initialHealth(type);
        }

        public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
            return room.get().spriteHandlers;
        }

        public EntityBackgroundMechanics backgroundMechanics() {
            return room.get().backgroundMechanics;
        }

        public EntityCombatTables combatTables() {
            return room.get().entityState.combatTables;
        }

        public EntityEventChannels events() {
            return room.get().entityState.events;
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

        public EntityRoomInspection roomInspection() {
            return room.get().roomInspection;
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

        public RoomEntity[] slots() {
            return room.get().slots;
        }

        public void initializeHealthGroup(int slot) {
            room.get().initializeHealthGroup(slot);
        }
    }
}
