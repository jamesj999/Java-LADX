package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityPlayerCommands; resolved lazily to preserve initialization order. */
interface EntityPlayerCommandsServices {
    void setBombFinalPresentationPending(int slot, boolean value);

    boolean bombFinalPresentationPending(int slot);

    EntityBombInteractions bombInteractions();

    EntityCombatTables combatTables();

    EntityEventChannels events();

    EntityLiftThrowController liftThrow();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntityPresentationState presentationState();

    EntityRewardController rewards();

    EntityRoomInspection inspection();

    EntitySlotProperties slotProperties();

    RoomEntity[] slots();

    static EntityPlayerCommandsServices bind(RoomEntityRuntime r) {
        return new Binding(() -> r);
    }

    final class Binding implements EntityPlayerCommandsServices {
        private final java.util.function.Supplier<RoomEntityRuntime> room;

        Binding(java.util.function.Supplier<RoomEntityRuntime> room) {
            this.room = room;
        }

        public void setBombFinalPresentationPending(int slot, boolean value) {
            room.get().bombInteractions.setBombFinalPresentationPending(slot, value);
        }

        public boolean bombFinalPresentationPending(int slot) {
            return room.get().bombInteractions.bombFinalPresentationPending(slot);
        }

        public EntityBombInteractions bombInteractions() {
            return room.get().bombInteractions;
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

        public EntityPresentationState presentationState() {
            return room.get().entityState.presentationState;
        }

        public EntityRewardController rewards() {
            return room.get().rewards;
        }

        public EntityRoomInspection inspection() {
            return room.get().inspection();
        }

        public EntitySlotProperties slotProperties() {
            return room.get().slotProperties;
        }

        public RoomEntity[] slots() {
            return room.get().slots;
        }
    }
}
