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
        return new Binding(() -> r);
    }

    final class Binding implements EntityRuntimeConfigurationServices {
        private final java.util.function.Supplier<RoomEntityRuntime> room;

        Binding(java.util.function.Supplier<RoomEntityRuntime> room) {
            this.room = room;
        }

        public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
            return room.get().spriteHandlers;
        }

        public EntityBossRewards bossRewards() {
            return room.get().bossRewards;
        }

        public EntityLiftThrowController liftThrow() {
            return room.get().liftThrow;
        }

        public EntityLinkState linkState() {
            return room.get().entityState.linkState;
        }

        public EntityMotionRegistry motions() {
            return room.get().entityState.motions;
        }

        public EntityPhysicsTables physicsTables() {
            return room.get().entityState.physicsTables;
        }

        public EntityPickupController pickups() {
            return room.get().pickups;
        }

        public EntityPresentationState presentationState() {
            return room.get().entityState.presentationState;
        }

        public EntityProjectileController projectiles() {
            return room.get().projectiles;
        }

        public EntityRoomVariables roomVariables() {
            return room.get().entityState.roomVariables;
        }

        public EntityStoryController story() {
            return room.get().story;
        }

        public RoomEntity[] slots() {
            return room.get().slots;
        }

        public SlimeEyeController slimeEye() {
            return room.get().slimeEye;
        }
    }
}
