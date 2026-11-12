package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityEnemyInspection; resolved lazily to preserve initialization order. */
interface EntityEnemyInspectionServices {
    linksawakening.world.entity.EntityBossRewards bossRewards();

    linksawakening.world.entity.EntityCombatTables combatTables();

    linksawakening.world.entity.EntityEventChannels events();

    linksawakening.world.entity.EntityLiftThrowController liftThrow();

    linksawakening.world.entity.EntityLinkState linkState();

    linksawakening.world.entity.EntityMotionRegistry motions();

    linksawakening.world.entity.EntityPhysicsTables physicsTables();

    linksawakening.world.entity.EntityProjectileController projectiles();

    linksawakening.world.entity.EntityRoomInspection inspection();

    linksawakening.world.entity.EntityRoomInspection roomInspection();

    linksawakening.world.entity.EntityRoomVariables roomVariables();

    linksawakening.world.entity.EntitySlotProperties slotProperties();

    linksawakening.world.entity.RoomEntity[] slots();

    static EntityEnemyInspectionServices bind(RoomEntityRuntime r) {
        return new EntityEnemyInspectionServices() {
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

            public linksawakening.world.entity.EntityLinkState linkState() {
                return r.entityState.linkState;
            }

            public linksawakening.world.entity.EntityMotionRegistry motions() {
                return r.entityState.motions;
            }

            public linksawakening.world.entity.EntityPhysicsTables physicsTables() {
                return r.entityState.physicsTables;
            }

            public linksawakening.world.entity.EntityProjectileController projectiles() {
                return r.projectiles;
            }

            public linksawakening.world.entity.EntityRoomInspection inspection() {
                return r.inspection();
            }

            public linksawakening.world.entity.EntityRoomInspection roomInspection() {
                return r.roomInspection;
            }

            public linksawakening.world.entity.EntityRoomVariables roomVariables() {
                return r.entityState.roomVariables;
            }

            public linksawakening.world.entity.EntitySlotProperties slotProperties() {
                return r.slotProperties;
            }

            public linksawakening.world.entity.RoomEntity[] slots() {
                return r.slots;
            }
        };
    }
}
