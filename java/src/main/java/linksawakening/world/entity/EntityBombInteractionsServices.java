package linksawakening.world.entity;

/** Dependencies used by EntityBombInteractions; resolved lazily to preserve initialization order. */
interface EntityBombInteractionsServices {

    int clearEntity(int slot);

    int options1(int slot);

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    linksawakening.world.entity.EntityBackgroundMechanics backgroundMechanics();

    linksawakening.world.entity.EntityCombatTables combatTables();

    linksawakening.world.entity.EntityEventChannels events();

    linksawakening.world.entity.EntityLiftThrowController liftThrow();

    linksawakening.world.entity.EntityLinkState linkState();

    linksawakening.world.entity.EntityMotionSet motionSet();

    linksawakening.world.entity.EntityPhysicsTables physicsTables();

    linksawakening.world.entity.EntitySlotProperties slotProperties();

    linksawakening.world.entity.RomEnemyCombatTables enemyCombatTables();

    linksawakening.world.entity.RoomEntity[] slots();

    static EntityBombInteractionsServices bind(RoomEntityRuntime r) {
        return new EntityBombInteractionsServices() {

            public int clearEntity(int slot) {
                return r.clearEntity(slot);
            }

            public int options1(int slot) {
                return r.options1(slot);
            }

            public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
                return r.spriteHandlers;
            }

            public linksawakening.world.entity.EntityBackgroundMechanics backgroundMechanics() {
                return r.backgroundMechanics;
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

            public linksawakening.world.entity.EntityLinkState linkState() {
                return r.entityState.linkState;
            }

            public linksawakening.world.entity.EntityMotionSet motionSet() {
                return r.entityState.motionSet;
            }

            public linksawakening.world.entity.EntityPhysicsTables physicsTables() {
                return r.entityState.physicsTables;
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
        };
    }
}
