package linksawakening.world.entity;

/** Dependencies used by EntityEffectConsumption; resolved lazily to preserve initialization order. */
interface EntityEffectConsumptionServices {
    EntityEventChannels events();

    EntityPresentationState presentationState();

    EntityRoomVariables roomVariables();

    static EntityEffectConsumptionServices bind(RoomEntityRuntime r) {
        return new Binding(() -> r);
    }

    final class Binding implements EntityEffectConsumptionServices {
        private final java.util.function.Supplier<RoomEntityRuntime> room;

        Binding(java.util.function.Supplier<RoomEntityRuntime> room) {
            this.room = room;
        }

        public EntityEventChannels events() {
            return room.get().entityState.events;
        }

        public EntityPresentationState presentationState() {
            return room.get().entityState.presentationState;
        }

        public EntityRoomVariables roomVariables() {
            return room.get().entityState.roomVariables;
        }
    }
}
