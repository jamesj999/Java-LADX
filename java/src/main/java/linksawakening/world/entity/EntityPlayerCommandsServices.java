package linksawakening.world.entity;

/** Dependencies used by EntityPlayerCommands; resolved lazily to preserve initialization order. */
interface EntityPlayerCommandsServices {
    void setBombFinalPresentationPending(int slot, boolean value);

    boolean bombFinalPresentationPending(int slot);

    linksawakening.world.entity.EntityBombInteractions bombInteractions();

    linksawakening.world.entity.EntityCombatTables combatTables();

    linksawakening.world.entity.EntityEventChannels events();

    linksawakening.world.entity.EntityLiftThrowController liftThrow();

    linksawakening.world.entity.EntityMotionSet motionSet();

    linksawakening.world.entity.EntityPhysicsTables physicsTables();

    linksawakening.world.entity.EntityPresentationState presentationState();

    linksawakening.world.entity.EntityRewardController rewards();

    linksawakening.world.entity.EntityRoomInspection inspection();

    linksawakening.world.entity.EntitySlotProperties slotProperties();

    linksawakening.world.entity.RoomEntity[] slots();

    static EntityPlayerCommandsServices bind(RoomEntityRuntime r) {
        return new EntityPlayerCommandsServices() {
            public void setBombFinalPresentationPending(int slot, boolean value) {
                r.bombInteractions.setBombFinalPresentationPending(slot, value);
            }

            public boolean bombFinalPresentationPending(int slot) {
                return r.bombInteractions.bombFinalPresentationPending(slot);
            }

            public linksawakening.world.entity.EntityBombInteractions bombInteractions() {
                return r.bombInteractions;
            }

            public linksawakening.world.entity.EntityCombatTables combatTables() {
                return r.entityState.combatTables;
            }

            public linksawakening.world.entity.EntityEventChannels events() {
                return r.entityState.events;
            }

            public linksawakening.world.entity.EntityLiftThrowController liftThrow() {
                return r.liftThrow;
            }

            public linksawakening.world.entity.EntityMotionSet motionSet() {
                return r.entityState.motionSet;
            }

            public linksawakening.world.entity.EntityPhysicsTables physicsTables() {
                return r.entityState.physicsTables;
            }

            public linksawakening.world.entity.EntityPresentationState presentationState() {
                return r.entityState.presentationState;
            }

            public linksawakening.world.entity.EntityRewardController rewards() {
                return r.rewards;
            }

            public linksawakening.world.entity.EntityRoomInspection inspection() {
                return r.inspection();
            }

            public linksawakening.world.entity.EntitySlotProperties slotProperties() {
                return r.slotProperties;
            }

            public linksawakening.world.entity.RoomEntity[] slots() {
                return r.slots;
            }
        };
    }
}
