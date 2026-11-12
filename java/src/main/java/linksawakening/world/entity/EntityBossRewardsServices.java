package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityBossRewards; resolved lazily to preserve initialization order. */
interface EntityBossRewardsServices {
    int findFreeEntitySlot();

    int initialHealth(int type);

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    EntityBackgroundMechanics backgroundMechanics();

    EntityCombatTables combatTables();

    EntityEventChannels events();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntityPickupController pickups();

    EntityRoomInspection roomInspection();

    EntityRoomVariables roomVariables();

    EntitySlotLifecycle lifecycle();

    EntitySlotProperties slotProperties();

    RoomEntity[] slots();

    void initializeHealthGroup(int slot);

    static EntityBossRewardsServices bind(RoomEntityRuntime r) {
        return new EntityBossRewardsServices() {
            public int findFreeEntitySlot() {
                return r.findFreeEntitySlot();
            }

            public int initialHealth(int type) {
                return r.initialHealth(type);
            }

            public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
                return r.spriteHandlers;
            }

            public EntityBackgroundMechanics backgroundMechanics() {
                return r.backgroundMechanics;
            }

            public EntityCombatTables combatTables() {
                return r.entityState.combatTables;
            }

            public EntityEventChannels events() {
                return r.entityState.events;
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

            public EntityRoomInspection roomInspection() {
                return r.roomInspection;
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

            public RoomEntity[] slots() {
                return r.slots;
            }

            public void initializeHealthGroup(int slot) {
                r.initializeHealthGroup(slot);
            }
        };
    }
}
