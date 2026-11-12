package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by GenieEntityController; resolved lazily to preserve initialization order. */
interface GenieEntityControllerServices {
    int findFreeEntitySlot();

    EntityBossRewards bossRewards();

    EntityCombatTables combatTables();

    EntityEventChannels events();

    EntityLiftThrowController liftThrow();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntityPlayerSpawns entityPlayerSpawns();

    EntitySlotLifecycle lifecycle();

    EntitySlotProperties slotProperties();

    RoomEntity[] slots();

    void initializeHealthGroup(int slot);

    static GenieEntityControllerServices bind(RoomEntityRuntime r) {
        return new GenieEntityControllerServices() {
            public int findFreeEntitySlot() {
                return r.findFreeEntitySlot();
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

            public EntityLiftThrowController liftThrow() {
                return r.liftThrow;
            }

            public EntityMotionRegistry motions() {
                return r.entityState.motions;
            }

            public EntityPhysicsTables physicsTables() {
                return r.entityState.physicsTables;
            }

            public EntityPlayerSpawns entityPlayerSpawns() {
                return r.entityPlayerSpawns;
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
