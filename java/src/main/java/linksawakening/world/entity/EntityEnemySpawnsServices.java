package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityEnemySpawns; resolved lazily to preserve initialization order. */
interface EntityEnemySpawnsServices {
    void setBombFinalPresentationPending(int slot, boolean value);

    int findFreeEntitySlot();

    int findFreeEntitySlotAtMost(int highestSlot);

    int initialHealth(int type);

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    EntityBombInteractions bombInteractions();

    EntityBossRewards bossRewards();

    EntityCombatTables combatTables();

    EntityEventChannels events();

    EntityLiftThrowController liftThrow();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntityPickupController pickups();

    EntitySlotProperties slotProperties();

    RoomEntity[] slots();

    void initializeHealthGroup(int slot);

    static EntityEnemySpawnsServices bind(RoomEntityRuntime r) {
        return new Binding(() -> r);
    }

    final class Binding implements EntityEnemySpawnsServices {
        private final java.util.function.Supplier<RoomEntityRuntime> room;

        Binding(java.util.function.Supplier<RoomEntityRuntime> room) {
            this.room = room;
        }

        public void setBombFinalPresentationPending(int slot, boolean value) {
            room.get().bombInteractions.setBombFinalPresentationPending(slot, value);
        }

        public int findFreeEntitySlot() {
            return room.get().findFreeEntitySlot();
        }

        public int findFreeEntitySlotAtMost(int highestSlot) {
            return room.get().findFreeEntitySlotAtMost(highestSlot);
        }

        public int initialHealth(int type) {
            return room.get().initialHealth(type);
        }

        public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
            return room.get().spriteHandlers;
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

        public EntityPickupController pickups() {
            return room.get().pickups;
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
