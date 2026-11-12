package linksawakening.world.entity;

/** Dependencies used by EntityEffectConsumption; resolved lazily to preserve initialization order. */
interface EntityEffectConsumptionServices {
    linksawakening.world.entity.EntityEventChannels events();

    linksawakening.world.entity.EntityPresentationState presentationState();

    linksawakening.world.entity.EntityRoomVariables roomVariables();

    static EntityEffectConsumptionServices bind(RoomEntityRuntime r) {
        return new EntityEffectConsumptionServices() {
            public linksawakening.world.entity.EntityEventChannels events() {
                return r.entityState.events;
            }

            public linksawakening.world.entity.EntityPresentationState presentationState() {
                return r.entityState.presentationState;
            }

            public linksawakening.world.entity.EntityRoomVariables roomVariables() {
                return r.entityState.roomVariables;
            }
        };
    }
}
