package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityPlayerSpawns; resolved lazily to preserve initialization order. */
interface EntityPlayerSpawnsServices {
    void setBombFinalPresentationPending(int slot, boolean value);

    boolean beginLift(int slot, int romDirection);

    boolean indoorRoom();

    int findFreeEntitySlot();

    int initialHealth(int type);

    java.util.function.IntSupplier defaultRandomByteSupplier();

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    EntityBombInteractions bombInteractions();

    EntityBossRewards bossRewards();

    EntityCombatTables combatTables();

    EntityEventChannels events();

    EntityLiftThrowController liftThrow();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntityPickupController pickups();

    EntityPresentationState presentationState();

    EntityProjectileController projectiles();

    EntityRoomInspection inspection();

    EntityRoomInspection roomInspection();

    EntityRoomVariables roomVariables();

    EntitySlotLifecycle lifecycle();

    EntitySlotProperties slotProperties();

    RoomEntity[] slots();

    linksawakening.world.room.ChestContentsTable chestContentsTable();

    void initializeHealthGroup(int slot);

    static EntityPlayerSpawnsServices bind(RoomEntityRuntime r) {
        return new Binding(() -> r);
    }

    final class Binding implements EntityPlayerSpawnsServices {
        private final java.util.function.Supplier<RoomEntityRuntime> room;

        Binding(java.util.function.Supplier<RoomEntityRuntime> room) {
            this.room = room;
        }

        public void setBombFinalPresentationPending(int slot, boolean value) {
            room.get().bombInteractions.setBombFinalPresentationPending(slot, value);
        }

        public boolean beginLift(int slot, int romDirection) {
            return room.get().beginLift(slot, romDirection);
        }

        public boolean indoorRoom() {
            return room.get().indoorRoom;
        }

        public int findFreeEntitySlot() {
            return room.get().findFreeEntitySlot();
        }

        public int initialHealth(int type) {
            return room.get().initialHealth(type);
        }

        public java.util.function.IntSupplier defaultRandomByteSupplier() {
            return room.get().defaultRandomByteSupplier;
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

        public EntityPresentationState presentationState() {
            return room.get().entityState.presentationState;
        }

        public EntityProjectileController projectiles() {
            return room.get().projectiles;
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

        public EntitySlotLifecycle lifecycle() {
            return room.get().lifecycle;
        }

        public EntitySlotProperties slotProperties() {
            return room.get().slotProperties;
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
