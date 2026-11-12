package linksawakening.world.entity;

/** Dependencies used by EntityRuntimeConfiguration; resolved lazily to preserve initialization order. */
interface EntityRuntimeConfigurationServices {
    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    linksawakening.world.entity.EntityBossRewards bossRewards();

    linksawakening.world.entity.EntityLiftThrowController liftThrow();

    linksawakening.world.entity.EntityLinkState linkState();

    linksawakening.world.entity.EntityMotionSet motionSet();

    linksawakening.world.entity.EntityPhysicsTables physicsTables();

    linksawakening.world.entity.EntityPickupController pickups();

    linksawakening.world.entity.EntityPresentationState presentationState();

    linksawakening.world.entity.EntityProjectileController projectiles();

    linksawakening.world.entity.EntityRoomVariables roomVariables();

    linksawakening.world.entity.EntityStoryController story();

    linksawakening.world.entity.RoomEntity[] slots();

    linksawakening.world.entity.SlimeEyeController slimeEye();

    static EntityRuntimeConfigurationServices bind(RoomEntityRuntime r) {
        return new EntityRuntimeConfigurationServices() {
            public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
                return r.spriteHandlers;
            }

            public linksawakening.world.entity.EntityBossRewards bossRewards() {
                return r.bossRewards;
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

            public linksawakening.world.entity.EntityPickupController pickups() {
                return r.pickups;
            }

            public linksawakening.world.entity.EntityPresentationState presentationState() {
                return r.entityState.presentationState;
            }

            public linksawakening.world.entity.EntityProjectileController projectiles() {
                return r.projectiles;
            }

            public linksawakening.world.entity.EntityRoomVariables roomVariables() {
                return r.entityState.roomVariables;
            }

            public linksawakening.world.entity.EntityStoryController story() {
                return r.story;
            }

            public linksawakening.world.entity.RoomEntity[] slots() {
                return r.slots;
            }

            public linksawakening.world.entity.SlimeEyeController slimeEye() {
                return r.slimeEye;
            }
        };
    }
}
