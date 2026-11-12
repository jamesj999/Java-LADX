package linksawakening.world.entity;

/** Dependencies used by EntityEffectConsumption; resolved lazily to preserve initialization order. */
interface EntityEffectConsumptionServices {
    EntityEventChannels events();

    EntityPresentationState presentationState();

    EntityRoomVariables roomVariables();

    static EntityEffectConsumptionServices bind(RoomEntityRuntime r) {
        return new EntityEffectConsumptionServices() {
            public EntityEventChannels events() {
                return r.entityState.events;
            }

            public EntityPresentationState presentationState() {
                return r.entityState.presentationState;
            }

            public EntityRoomVariables roomVariables() {
                return r.entityState.roomVariables;
            }
        };
    }
}
