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
        return new EntityPlayerCommandsServices() {
            public void setBombFinalPresentationPending(int slot, boolean value) {
                r.bombInteractions.setBombFinalPresentationPending(slot, value);
            }

            public boolean bombFinalPresentationPending(int slot) {
                return r.bombInteractions.bombFinalPresentationPending(slot);
            }

            public EntityBombInteractions bombInteractions() {
                return r.bombInteractions;
            }

            public EntityCombatTables combatTables() {
                return r.entityState.combatTables;
            }

            public EntityEventChannels events() {
                return r.entityState.events;
            }

            public EntityLiftThrowController liftThrow() {
                return r.liftThrow;
            }

            public EntityMotionRegistry motions() {
                return r.entityState.motions;
            }

            public EntityPhysicsTables physicsTables() {
                return r.entityState.physicsTables;
            }

            public EntityPresentationState presentationState() {
                return r.entityState.presentationState;
            }

            public EntityRewardController rewards() {
                return r.rewards;
            }

            public EntityRoomInspection inspection() {
                return r.inspection();
            }

            public EntitySlotProperties slotProperties() {
                return r.slotProperties;
            }

            public RoomEntity[] slots() {
                return r.slots;
            }
        };
    }
}
