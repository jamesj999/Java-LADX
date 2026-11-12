package linksawakening.world.entity;

/** Dependencies used by SlimeEyeController; resolved lazily to preserve initialization order. */
interface SlimeEyeControllerServices {
    int findFreeEntitySlot();

    int initialHealth(int type);

    java.util.List<linksawakening.world.entity.RoomEntity> slotEntities();

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    linksawakening.world.entity.EntityBackgroundMechanics backgroundMechanics();

    linksawakening.world.entity.EntityBossRewards bossRewards();

    linksawakening.world.entity.EntityCombatTables combatTables();

    linksawakening.world.entity.EntityEventChannels events();

    linksawakening.world.entity.EntityMotionSet motionSet();

    linksawakening.world.entity.EntityPhysicsTables physicsTables();

    linksawakening.world.entity.EntityRoomVariables roomVariables();

    linksawakening.world.entity.EntitySlotLifecycle lifecycle();

    linksawakening.world.entity.EntitySlotProperties slotProperties();

    linksawakening.world.entity.RomEnemyCombatTables enemyCombatTables();

    linksawakening.world.entity.RoomEntity[] slots();

    void initializeHealthGroup(int slot);

    static SlimeEyeControllerServices bind(RoomEntityRuntime r) {
        return new SlimeEyeControllerServices() {
            public int findFreeEntitySlot() {
                return r.findFreeEntitySlot();
            }

            public int initialHealth(int type) {
                return r.initialHealth(type);
            }

            public java.util.List<linksawakening.world.entity.RoomEntity> slotEntities() {
                return r.slotEntities;
            }

            public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
                return r.spriteHandlers;
            }

            public linksawakening.world.entity.EntityBackgroundMechanics backgroundMechanics() {
                return r.backgroundMechanics;
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

            public linksawakening.world.entity.EntityMotionSet motionSet() {
                return r.entityState.motionSet;
            }

            public linksawakening.world.entity.EntityPhysicsTables physicsTables() {
                return r.entityState.physicsTables;
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

            public linksawakening.world.entity.RomEnemyCombatTables enemyCombatTables() {
                return r.enemyCombatTables;
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
