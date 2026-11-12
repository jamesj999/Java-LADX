package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityEnemyInspection; resolved lazily to preserve initialization order. */
interface EntityEnemyInspectionServices {
    EntityBossRewards bossRewards();

    EntityCombatTables combatTables();

    EntityEventChannels events();

    EntityLiftThrowController liftThrow();

    EntityLinkState linkState();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntityRoomInspection inspection();

    EntityRoomInspection roomInspection();

    EntityRoomVariables roomVariables();

    EntitySlotProperties slotProperties();

    RoomEntity[] slots();

    static EntityEnemyInspectionServices bind(RoomEntityRuntime r) {
        return new EntityEnemyInspectionServices() {
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

            public EntityLinkState linkState() {
                return r.entityState.linkState;
            }

            public EntityMotionRegistry motions() {
                return r.entityState.motions;
            }

            public EntityPhysicsTables physicsTables() {
                return r.entityState.physicsTables;
            }

            public EntityRoomInspection inspection() {
                return r.inspection();
            }

            public EntityRoomInspection roomInspection() {
                return r.roomInspection;
            }

            public EntityRoomVariables roomVariables() {
                return r.entityState.roomVariables;
            }

            public EntitySlotProperties slotProperties() {
                return r.slotProperties;
            }

            public RoomEntity[] slots() {
                return r.slots;
            }
        };
    }
}
