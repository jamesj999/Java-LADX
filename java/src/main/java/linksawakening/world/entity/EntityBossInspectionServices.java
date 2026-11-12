package linksawakening.world.entity;

/** Dependencies used by EntityBossInspection; resolved lazily to preserve initialization order. */
interface EntityBossInspectionServices {
    linksawakening.world.entity.EntityBossRewards bossRewards();

    linksawakening.world.entity.EntityEnemyInspection enemyInspection();

    linksawakening.world.entity.EntityMotionSet motionSet();

    linksawakening.world.entity.EntityPhysicsTables physicsTables();

    linksawakening.world.entity.GenieEntityController genie();

    static EntityBossInspectionServices bind(RoomEntityRuntime r) {
        return new EntityBossInspectionServices() {
            public linksawakening.world.entity.EntityBossRewards bossRewards() {
                return r.bossRewards;
            }

            public linksawakening.world.entity.EntityEnemyInspection enemyInspection() {
                return r.enemyInspection;
            }

            public linksawakening.world.entity.EntityMotionSet motionSet() {
                return r.entityState.motionSet;
            }

            public linksawakening.world.entity.EntityPhysicsTables physicsTables() {
                return r.entityState.physicsTables;
            }

            public linksawakening.world.entity.GenieEntityController genie() {
                return r.genie;
            }
        };
    }
}
