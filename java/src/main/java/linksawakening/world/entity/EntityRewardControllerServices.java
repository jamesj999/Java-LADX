package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityRewardController; resolved lazily to preserve initialization order. */
interface EntityRewardControllerServices {
    EntityMotionRegistry motions();

    int clearEntity(int slot);

    int findFreeEntitySlot();

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    EntityCombatTables combatTables();

    EntityEventChannels events();

    EntityLinkState linkState();

    EntityPhysicsTables physicsTables();

    EntityPresentationState presentationState();

    EntityRoomVariables roomVariables();

    EntitySlotLifecycle lifecycle();

    EntityStoryController story();

    RoomEntity[] slots();

    void holdSwordAboveLink(RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ, boolean finalPose);

    void initializeHealthGroup(int slot);

    static EntityRewardControllerServices bind(RoomEntityRuntime r) {
        return new Binding(() -> r);
    }

    final class Binding implements EntityRewardControllerServices {
        private final java.util.function.Supplier<RoomEntityRuntime> room;

        Binding(java.util.function.Supplier<RoomEntityRuntime> room) {
            this.room = room;
        }

        public EntityMotionRegistry motions() {
            return room.get().entityState.motions;
        }

        public int clearEntity(int slot) {
            return room.get().clearEntity(slot);
        }

        public int findFreeEntitySlot() {
            return room.get().findFreeEntitySlot();
        }

        public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
            return room.get().spriteHandlers;
        }

        public EntityCombatTables combatTables() {
            return room.get().entityState.combatTables;
        }

        public EntityEventChannels events() {
            return room.get().entityState.events;
        }

        public EntityLinkState linkState() {
            return room.get().entityState.linkState;
        }

        public EntityPhysicsTables physicsTables() {
            return room.get().entityState.physicsTables;
        }

        public EntityPresentationState presentationState() {
            return room.get().entityState.presentationState;
        }

        public EntityRoomVariables roomVariables() {
            return room.get().entityState.roomVariables;
        }

        public EntitySlotLifecycle lifecycle() {
            return room.get().lifecycle;
        }

        public EntityStoryController story() {
            return room.get().story;
        }

        public RoomEntity[] slots() {
            return room.get().slots;
        }

        public void holdSwordAboveLink(
                RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ, boolean finalPose) {
            room.get().holdSwordAboveLink(entity, linkEntityX, linkEntityY, linkZ, finalPose);
        }

        public void initializeHealthGroup(int slot) {
            room.get().initializeHealthGroup(slot);
        }
    }
}
