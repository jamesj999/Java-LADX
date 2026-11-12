package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityBossInspection; resolved lazily to preserve initialization order. */
interface EntityBossInspectionServices {
    EntityBossRewards bossRewards();

    EntityEnemyInspection enemyInspection();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    static EntityBossInspectionServices bind(RoomEntityRuntime r) {
        return new EntityBossInspectionServices() {
            public EntityBossRewards bossRewards() {
                return r.bossRewards;
            }

            public EntityEnemyInspection enemyInspection() {
                return r.enemyInspection;
            }

            public EntityMotionRegistry motions() {
                return r.entityState.motions;
            }

            public EntityPhysicsTables physicsTables() {
                return r.entityState.physicsTables;
            }
        };
    }
}
