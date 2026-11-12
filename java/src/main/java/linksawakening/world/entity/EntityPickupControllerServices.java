package linksawakening.world.entity;

/** Dependencies used by EntityPickupController; resolved lazily to preserve initialization order. */
interface EntityPickupControllerServices {
    boolean indoorRoom();

    int clearEntity(int slot);

    int findFreeEntitySlot();

    int initialHealth(int type);

    linksawakening.world.entity.EntityBossRewards bossRewards();

    linksawakening.world.entity.EntityCombatTables combatTables();

    linksawakening.world.entity.EntityEventChannels events();

    linksawakening.world.entity.EntityLinkState linkState();

    linksawakening.world.entity.EntityMotionSet motionSet();

    linksawakening.world.entity.EntityPhysicsTables physicsTables();

    linksawakening.world.entity.EntityPresentationState presentationState();

    linksawakening.world.entity.EntityRoomVariables roomVariables();

    linksawakening.world.entity.EntitySlotLifecycle lifecycle();

    linksawakening.world.entity.EntitySlotProperties slotProperties();

    linksawakening.world.entity.EntityStoryController story();

    linksawakening.world.entity.RoomEntity[] slots();

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

            public linksawakening.world.entity.EntityBossRewards bossRewards() {
                return r.bossRewards;
            }

            public linksawakening.world.entity.EntityCombatTables combatTables() {
                return r.entityState.combatTables;
            }

            public linksawakening.world.entity.EntityEventChannels events() {
                return r.entityState.events;
            }

            public linksawakening.world.entity.EntityLinkState linkState() {
                return r.entityState.linkState;
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

            public linksawakening.world.entity.EntityRoomVariables roomVariables() {
                return r.entityState.roomVariables;
            }

            public linksawakening.world.entity.EntitySlotLifecycle lifecycle() {
                return r.lifecycle;
            }

            public linksawakening.world.entity.EntitySlotProperties slotProperties() {
                return r.slotProperties;
            }

            public linksawakening.world.entity.EntityStoryController story() {
                return r.story;
            }

            public linksawakening.world.entity.RoomEntity[] slots() {
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
