package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by GenieEntityController; resolved lazily to preserve initialization order. */
interface GenieEntityControllerServices {
    int findFreeEntitySlot();

    linksawakening.world.entity.EntityBossRewards bossRewards();

    linksawakening.world.entity.EntityCombatTables combatTables();

    linksawakening.world.entity.EntityEventChannels events();

    linksawakening.world.entity.EntityLiftThrowController liftThrow();

    linksawakening.world.entity.EntityMotionRegistry motions();

    linksawakening.world.entity.EntityPhysicsTables physicsTables();

    linksawakening.world.entity.EntityPlayerSpawns entityPlayerSpawns();

    linksawakening.world.entity.EntitySlotLifecycle lifecycle();

    linksawakening.world.entity.EntitySlotProperties slotProperties();

    linksawakening.world.entity.RoomEntity[] slots();

    void initializeHealthGroup(int slot);

    static GenieEntityControllerServices bind(RoomEntityRuntime r) {
        return new GenieEntityControllerServices() {
            public int findFreeEntitySlot() {
                return r.findFreeEntitySlot();
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

            public linksawakening.world.entity.EntityLiftThrowController liftThrow() {
                return r.liftThrow;
            }

            public linksawakening.world.entity.EntityMotionRegistry motions() {
                return r.entityState.motions;
            }

            public linksawakening.world.entity.EntityPhysicsTables physicsTables() {
                return r.entityState.physicsTables;
            }

            public linksawakening.world.entity.EntityPlayerSpawns entityPlayerSpawns() {
                return r.entityPlayerSpawns;
            }

            public linksawakening.world.entity.EntitySlotLifecycle lifecycle() {
                return r.lifecycle;
            }

            public linksawakening.world.entity.EntitySlotProperties slotProperties() {
                return r.slotProperties;
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
