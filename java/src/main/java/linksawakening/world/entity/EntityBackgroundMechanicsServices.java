package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityBackgroundMechanics; resolved lazily to preserve initialization order. */
interface EntityBackgroundMechanicsServices {
    int clearEntity(int slot);

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    EntityBossRewards bossRewards();

    EntityCombatTables combatTables();

    EntityEnemyInspection enemyInspection();

    EntityEventChannels events();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntityRoomInspection roomInspection();

    EntityRoomVariables roomVariables();

    RoomEntity[] slots();

    static EntityBackgroundMechanicsServices bind(RoomEntityRuntime r) {
        return new EntityBackgroundMechanicsServices() {
            public int clearEntity(int slot) {
                return r.clearEntity(slot);
            }

            public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
                return r.spriteHandlers;
            }

            public EntityBossRewards bossRewards() {
                return r.bossRewards;
            }

            public EntityCombatTables combatTables() {
                return r.entityState.combatTables;
            }

            public EntityEnemyInspection enemyInspection() {
                return r.enemyInspection;
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

            public EntityRoomInspection roomInspection() {
                return r.roomInspection;
            }

            public EntityRoomVariables roomVariables() {
                return r.entityState.roomVariables;
            }

            public RoomEntity[] slots() {
                return r.slots;
            }
        };
    }
}
