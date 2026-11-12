package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityPickupController; resolved lazily to preserve initialization order. */
interface EntityPickupControllerServices {
    boolean indoorRoom();

    int clearEntity(int slot);

    int findFreeEntitySlot();

    int initialHealth(int type);

    EntityBossRewards bossRewards();

    EntityCombatTables combatTables();

    EntityEventChannels events();

    EntityLinkState linkState();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntityPresentationState presentationState();

    EntityRoomVariables roomVariables();

    EntitySlotLifecycle lifecycle();

    EntitySlotProperties slotProperties();

    EntityStoryController story();

    RoomEntity[] slots();

    linksawakening.world.room.ChestContentsTable chestContentsTable();

    void initializeHealthGroup(int slot);

    static EntityPickupControllerServices bind(RoomEntityRuntime r) {
        return new EntityPickupControllerServices() {
            public boolean indoorRoom() {
                return r.indoorRoom;
            }

            public int clearEntity(int slot) {
                return r.clearEntity(slot);
            }

            public int findFreeEntitySlot() {
                return r.findFreeEntitySlot();
            }

            public int initialHealth(int type) {
                return r.initialHealth(type);
            }

            public EntityBossRewards bossRewards() {
                return r.bossRewards;
            }

            public EntityCombatTables combatTables() {
                return r.entityState.combatTables;
            }

            public EntityEventChannels events() {
                return r.entityState.events;
            }

            public EntityLinkState linkState() {
                return r.entityState.linkState;
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

            public EntityRoomVariables roomVariables() {
                return r.entityState.roomVariables;
            }

            public EntitySlotLifecycle lifecycle() {
                return r.lifecycle;
            }

            public EntitySlotProperties slotProperties() {
                return r.slotProperties;
            }

            public EntityStoryController story() {
                return r.story;
            }

            public RoomEntity[] slots() {
                return r.slots;
            }

            public linksawakening.world.room.ChestContentsTable chestContentsTable() {
                return r.chestContentsTable;
            }

            public void initializeHealthGroup(int slot) {
                r.initializeHealthGroup(slot);
            }
        };
    }
}
