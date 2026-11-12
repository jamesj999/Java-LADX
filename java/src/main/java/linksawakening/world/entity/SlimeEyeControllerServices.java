package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by SlimeEyeController; resolved lazily to preserve initialization order. */
interface SlimeEyeControllerServices {
    int findFreeEntitySlot();

    int initialHealth(int type);

    java.util.List<RoomEntity> slotEntities();

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    EntityBackgroundMechanics backgroundMechanics();

    EntityBossRewards bossRewards();

    EntityCombatTables combatTables();

    EntityEventChannels events();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntityRoomVariables roomVariables();

    EntitySlotLifecycle lifecycle();

    EntitySlotProperties slotProperties();

    RomEnemyCombatTables enemyCombatTables();

    RoomEntity[] slots();

    void initializeHealthGroup(int slot);

    static SlimeEyeControllerServices bind(RoomEntityRuntime r) {
        return new SlimeEyeControllerServices() {
            public int findFreeEntitySlot() {
                return r.findFreeEntitySlot();
            }

            public int initialHealth(int type) {
                return r.initialHealth(type);
            }

            public java.util.List<RoomEntity> slotEntities() {
                return r.slotEntities;
            }

            public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
                return r.spriteHandlers;
            }

            public EntityBackgroundMechanics backgroundMechanics() {
                return r.backgroundMechanics;
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

            public EntityMotionRegistry motions() {
                return r.entityState.motions;
            }

            public EntityPhysicsTables physicsTables() {
                return r.entityState.physicsTables;
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

            public RomEnemyCombatTables enemyCombatTables() {
                return r.enemyCombatTables;
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
