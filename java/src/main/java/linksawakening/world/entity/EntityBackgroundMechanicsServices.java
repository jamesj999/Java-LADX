package linksawakening.world.entity;

/** Dependencies used by EntityBackgroundMechanics; resolved lazily to preserve initialization order. */
interface EntityBackgroundMechanicsServices {
    int clearEntity(int slot);

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    linksawakening.world.entity.EntityBossRewards bossRewards();

    linksawakening.world.entity.EntityCombatTables combatTables();

    linksawakening.world.entity.EntityEnemyInspection enemyInspection();

    linksawakening.world.entity.EntityEventChannels events();

    linksawakening.world.entity.EntityMotionSet motionSet();

    linksawakening.world.entity.EntityPhysicsTables physicsTables();

    linksawakening.world.entity.EntityRoomInspection roomInspection();

    linksawakening.world.entity.EntityRoomVariables roomVariables();

    linksawakening.world.entity.RoomEntity[] slots();

    static EntityBackgroundMechanicsServices bind(RoomEntityRuntime r) {
        return new EntityBackgroundMechanicsServices() {
            public int clearEntity(int slot) {
                return r.clearEntity(slot);
            }

            public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
                return r.spriteHandlers;
            }

            public linksawakening.world.entity.EntityBossRewards bossRewards() {
                return r.bossRewards;
            }

            public linksawakening.world.entity.EntityCombatTables combatTables() {
                return r.entityState.combatTables;
            }

            public linksawakening.world.entity.EntityEnemyInspection enemyInspection() {
                return r.enemyInspection;
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

            public linksawakening.world.entity.EntityRoomInspection roomInspection() {
                return r.roomInspection;
            }

            public linksawakening.world.entity.EntityRoomVariables roomVariables() {
                return r.entityState.roomVariables;
            }

            public linksawakening.world.entity.RoomEntity[] slots() {
                return r.slots;
            }
        };
    }
}
