package linksawakening.world.entity;

/** Dependencies used by EntityEnemySpawns; resolved lazily to preserve initialization order. */
interface EntityEnemySpawnsServices {
    void setBombFinalPresentationPending(int slot, boolean value);

    int findFreeEntitySlot();

    int findFreeEntitySlotAtMost(int highestSlot);

    int initialHealth(int type);

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    linksawakening.world.entity.EntityBombInteractions bombInteractions();

    linksawakening.world.entity.EntityBossRewards bossRewards();

    linksawakening.world.entity.EntityCombatTables combatTables();

    linksawakening.world.entity.EntityEventChannels events();

    linksawakening.world.entity.EntityLiftThrowController liftThrow();

    linksawakening.world.entity.EntityMotionSet motionSet();

    linksawakening.world.entity.EntityPhysicsTables physicsTables();

    linksawakening.world.entity.EntityPickupController pickups();

    linksawakening.world.entity.EntitySlotProperties slotProperties();

    linksawakening.world.entity.RoomEntity[] slots();

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

            public linksawakening.world.entity.EntityBombInteractions bombInteractions() {
                return r.bombInteractions;
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

            public linksawakening.world.entity.EntityMotionSet motionSet() {
                return r.entityState.motionSet;
            }

            public linksawakening.world.entity.EntityPhysicsTables physicsTables() {
                return r.entityState.physicsTables;
            }

            public linksawakening.world.entity.EntityPickupController pickups() {
                return r.pickups;
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
