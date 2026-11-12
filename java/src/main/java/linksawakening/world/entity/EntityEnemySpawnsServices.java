package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityEnemySpawns; resolved lazily to preserve initialization order. */
interface EntityEnemySpawnsServices {
    void setBombFinalPresentationPending(int slot, boolean value);

    int findFreeEntitySlot();

    int findFreeEntitySlotAtMost(int highestSlot);

    int initialHealth(int type);

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    EntityBombInteractions bombInteractions();

    EntityBossRewards bossRewards();

    EntityCombatTables combatTables();

    EntityEventChannels events();

    EntityLiftThrowController liftThrow();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntityPickupController pickups();

    EntitySlotProperties slotProperties();

    RoomEntity[] slots();

    void initializeHealthGroup(int slot);

    static EntityEnemySpawnsServices bind(RoomEntityRuntime r) {
        return new EntityEnemySpawnsServices() {
            public void setBombFinalPresentationPending(int slot, boolean value) {
                r.bombInteractions.setBombFinalPresentationPending(slot, value);
            }

            public int findFreeEntitySlot() {
                return r.findFreeEntitySlot();
            }

            public int findFreeEntitySlotAtMost(int highestSlot) {
                return r.findFreeEntitySlotAtMost(highestSlot);
            }

            public int initialHealth(int type) {
                return r.initialHealth(type);
            }

            public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
                return r.spriteHandlers;
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

            public EntityPickupController pickups() {
                return r.pickups;
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
