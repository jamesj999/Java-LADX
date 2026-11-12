package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityRoomInitialization; resolved lazily to preserve initialization order. */
interface EntityRoomInitializationServices {
    void clearBombFinalPresentations();

    EntityWarpController warpState();

    boolean indoorRoom();

    int initialHealth(int type);

    EntityBombInteractions bombInteractions();

    EntityBossRewards bossRewards();

    EntityCombatTables combatTables();

    EntityLiftThrowController liftThrow();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntityPickupController pickups();

    EntityRewardController rewards();

    EntitySlotProperties slotProperties();

    EntityStoryController story();

    GenieEntityController genie();

    RoomEntity[] slots();

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

            public EntityBombInteractions bombInteractions() {
                return r.bombInteractions;
            }

            public EntityBossRewards bossRewards() {
                return r.bossRewards;
            }

            public EntityCombatTables combatTables() {
                return r.entityState.combatTables;
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

            public EntityPickupController pickups() {
                return r.pickups;
            }

            public EntityRewardController rewards() {
                return r.rewards;
            }

            public EntitySlotProperties slotProperties() {
                return r.slotProperties;
            }

            public EntityStoryController story() {
                return r.story;
            }

            public GenieEntityController genie() {
                return r.genie;
            }

            public RoomEntity[] slots() {
                return r.slots;
            }

            public void initializeHealthGroup(int slot) {
                r.initializeHealthGroup(slot);
            }
        };
    }
}
