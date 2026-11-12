package linksawakening.world.entity;

/** Dependencies used by EntityRoomInitialization; resolved lazily to preserve initialization order. */
interface EntityRoomInitializationServices {
    void clearBombFinalPresentations();

    EntityWarpController warpState();

    boolean indoorRoom();

    int initialHealth(int type);

    linksawakening.world.entity.EntityBombInteractions bombInteractions();

    linksawakening.world.entity.EntityBossRewards bossRewards();

    linksawakening.world.entity.EntityCombatTables combatTables();

    linksawakening.world.entity.EntityLiftThrowController liftThrow();

    linksawakening.world.entity.EntityMotionSet motionSet();

    linksawakening.world.entity.EntityPhysicsTables physicsTables();

    linksawakening.world.entity.EntityPickupController pickups();

    linksawakening.world.entity.EntityRewardController rewards();

    linksawakening.world.entity.EntitySlotProperties slotProperties();

    linksawakening.world.entity.EntityStoryController story();

    linksawakening.world.entity.GenieEntityController genie();

    linksawakening.world.entity.RoomEntity[] slots();

    void initializeHealthGroup(int slot);

    static EntityRoomInitializationServices bind(RoomEntityRuntime r) {
        return new EntityRoomInitializationServices() {
            public void clearBombFinalPresentations() {
                r.bombInteractions.clearBombFinalPresentations();
            }

            public EntityWarpController warpState() {
                return r.warpState;
            }

            public boolean indoorRoom() {
                return r.indoorRoom;
            }

            public int initialHealth(int type) {
                return r.initialHealth(type);
            }

            public linksawakening.world.entity.EntityBombInteractions bombInteractions() {
                return r.bombInteractions;
            }

            public linksawakening.world.entity.EntityBossRewards bossRewards() {
                return r.bossRewards;
            }

            public linksawakening.world.entity.EntityCombatTables combatTables() {
                return r.entityState.combatTables;
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

            public linksawakening.world.entity.EntityPickupController pickups() {
                return r.pickups;
            }

            public linksawakening.world.entity.EntityRewardController rewards() {
                return r.rewards;
            }

            public linksawakening.world.entity.EntitySlotProperties slotProperties() {
                return r.slotProperties;
            }

            public linksawakening.world.entity.EntityStoryController story() {
                return r.story;
            }

            public linksawakening.world.entity.GenieEntityController genie() {
                return r.genie;
            }

            public linksawakening.world.entity.RoomEntity[] slots() {
                return r.slots;
            }

            public void initializeHealthGroup(int slot) {
                r.initializeHealthGroup(slot);
            }
        };
    }
}
