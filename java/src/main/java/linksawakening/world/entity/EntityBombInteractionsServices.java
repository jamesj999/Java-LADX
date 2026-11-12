package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityBombInteractions; resolved lazily to preserve initialization order. */
interface EntityBombInteractionsServices {

    int clearEntity(int slot);

    int options1(int slot);

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    EntityBackgroundMechanics backgroundMechanics();

    EntityCombatTables combatTables();

    EntityEventChannels events();

    EntityLiftThrowController liftThrow();

    EntityLinkState linkState();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntitySlotProperties slotProperties();

    RomEnemyCombatTables enemyCombatTables();

    RoomEntity[] slots();

    static EntityBombInteractionsServices bind(RoomEntityRuntime r) {
        return new Binding(() -> r);
    }

    final class Binding implements EntityBombInteractionsServices {
        private final java.util.function.Supplier<RoomEntityRuntime> room;

        Binding(java.util.function.Supplier<RoomEntityRuntime> room) {
            this.room = room;
        }

        public int clearEntity(int slot) {
            return room.get().clearEntity(slot);
        }

        public int options1(int slot) {
            return room.get().options1(slot);
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

        public EntitySlotProperties slotProperties() {
            return room.get().slotProperties;
        }

        public RomEnemyCombatTables enemyCombatTables() {
            return room.get().enemyCombatTables;
        }

        public RoomEntity[] slots() {
            return room.get().slots;
        }
    }
}
