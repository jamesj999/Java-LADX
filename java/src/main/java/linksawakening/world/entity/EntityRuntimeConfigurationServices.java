package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityRuntimeConfiguration; resolved lazily to preserve initialization order. */
interface EntityRuntimeConfigurationServices {
    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    EntityBossRewards bossRewards();

    EntityLiftThrowController liftThrow();

    EntityLinkState linkState();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntityPickupController pickups();

    EntityPresentationState presentationState();

    EntityProjectileController projectiles();

    EntityRoomVariables roomVariables();

    EntityStoryController story();

    RoomEntity[] slots();

    SlimeEyeController slimeEye();

    static EntityRuntimeConfigurationServices bind(RoomEntityRuntime r) {
        return new EntityRuntimeConfigurationServices() {
            public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
                return r.spriteHandlers;
            }

            public EntityBossRewards bossRewards() {
                return r.bossRewards;
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

            public EntityPickupController pickups() {
                return r.pickups;
            }

            public EntityPresentationState presentationState() {
                return r.entityState.presentationState;
            }

            public EntityProjectileController projectiles() {
                return r.projectiles;
            }

            public EntityRoomVariables roomVariables() {
                return r.entityState.roomVariables;
            }

            public EntityStoryController story() {
                return r.story;
            }

            public RoomEntity[] slots() {
                return r.slots;
            }

            public SlimeEyeController slimeEye() {
                return r.slimeEye;
            }
        };
    }
}
