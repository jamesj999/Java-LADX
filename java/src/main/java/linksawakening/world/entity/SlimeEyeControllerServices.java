package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by SlimeEyeController; resolved lazily to preserve initialization order. */
interface SlimeEyeControllerServices {
    int findFreeEntitySlot();

    int initialHealth(int type);

    java.util.List<RoomEntity> slotEntities();

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    EntityBackgroundMechanics backgroundMechanics();

    EntityBossRewards bossRewards();

    EntityCombatTables combatTables();

    EntityEventChannels events();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntityRoomVariables roomVariables();

    EntitySlotLifecycle lifecycle();

    EntitySlotProperties slotProperties();

    RomEnemyCombatTables enemyCombatTables();

    RoomEntity[] slots();

    void initializeHealthGroup(int slot);

    static SlimeEyeControllerServices bind(RoomEntityRuntime r) {
        return new Binding(() -> r);
    }

    final class Binding implements SlimeEyeControllerServices {
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

        public java.util.List<RoomEntity> slotEntities() {
            return room.get().slotEntities;
        }

        public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
            return room.get().spriteHandlers;
        }

        public EntityBackgroundMechanics backgroundMechanics() {
            return room.get().backgroundMechanics;
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

        public EntityMotionRegistry motions() {
            return room.get().entityState.motions;
        }

        public EntityPhysicsTables physicsTables() {
            return room.get().entityState.physicsTables;
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

        public RomEnemyCombatTables enemyCombatTables() {
            return room.get().enemyCombatTables;
        }

        public RoomEntity[] slots() {
            return room.get().slots;
        }

        public void initializeHealthGroup(int slot) {
            room.get().initializeHealthGroup(slot);
        }
    }
}
