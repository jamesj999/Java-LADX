package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityDebugConfiguration; resolved lazily to preserve initialization order. */
interface EntityDebugConfigurationServices {
    linksawakening.world.entity.EntityBossRewards bossRewards();

    linksawakening.world.entity.EntityCombatTables combatTables();

    linksawakening.world.entity.EntityLinkState linkState();

    linksawakening.world.entity.EntityMotionRegistry motions();

    linksawakening.world.entity.EntityPhysicsTables physicsTables();

    linksawakening.world.entity.EntityRoomInspection roomInspection();

    linksawakening.world.entity.EntityRoomVariables roomVariables();

    linksawakening.world.entity.EntityRuntimeConfiguration configuration();

    linksawakening.world.entity.GenieEntityController genie();

    linksawakening.world.entity.RoomEntity[] slots();

    static EntityDebugConfigurationServices bind(RoomEntityRuntime r) {
        return new EntityDebugConfigurationServices() {
            public linksawakening.world.entity.EntityBossRewards bossRewards() {
                return r.bossRewards;
            }

            public linksawakening.world.entity.EntityCombatTables combatTables() {
                return r.entityState.combatTables;
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

            public linksawakening.world.entity.EntityRoomInspection roomInspection() {
                return r.roomInspection;
            }

            public linksawakening.world.entity.EntityRoomVariables roomVariables() {
                return r.entityState.roomVariables;
            }

            public linksawakening.world.entity.EntityRuntimeConfiguration configuration() {
                return r.configuration();
            }

            public linksawakening.world.entity.GenieEntityController genie() {
                return r.genie;
            }

            public linksawakening.world.entity.RoomEntity[] slots() {
                return r.slots;
            }
        };
    }
}
