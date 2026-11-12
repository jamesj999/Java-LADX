package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityStoryController; resolved lazily to preserve initialization order. */
interface EntityStoryControllerServices {
    EntityMotionRegistry motions();

    int findFreeEntitySlot();

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    EntityBackgroundMechanics backgroundMechanics();

    EntityBossRewards bossRewards();

    EntityCombatTables combatTables();

    EntityEventChannels events();

    EntityLinkState linkState();

    EntityPhysicsTables physicsTables();

    EntityPickupController pickups();

    EntityPresentationState presentationState();

    EntityRoomInspection inspection();

    EntityRoomInspection roomInspection();

    EntityRoomVariables roomVariables();

    EntitySlotLifecycle lifecycle();

    RoomEntity[] slots();

    void blockLinkForOwl(int slot);

    void initializeHealthGroup(int slot);

    void presentWitchGotItem(int slot);

    EntitySpriteViews spriteViews();

    static EntityStoryControllerServices bind(RoomEntityRuntime r) {
        return new Binding(() -> r);
    }

    final class Binding implements EntityStoryControllerServices {
        private final java.util.function.Supplier<RoomEntityRuntime> room;

        Binding(java.util.function.Supplier<RoomEntityRuntime> room) {
            this.room = room;
        }

        public EntitySpriteViews spriteViews() {
            return room.get().slotProperties.spriteViews();
        }

        public EntityMotionRegistry motions() {
            return room.get().entityState.motions;
        }

        public int findFreeEntitySlot() {
            return room.get().findFreeEntitySlot();
        }

        public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
            return room.get().spriteHandlers;
        }

        public EntityBackgroundMechanics backgroundMechanics() {
            return room.get().backgroundMechanics;
        }

        public EntityBossRewards bossRewards() {
            return room.get().bossRewards;
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

        public EntityPickupController pickups() {
            return room.get().pickups;
        }

        public EntityPresentationState presentationState() {
            return room.get().entityState.presentationState;
        }

        public EntityRoomInspection inspection() {
            return room.get().inspection();
        }

        public EntityRoomInspection roomInspection() {
            return room.get().roomInspection;
        }

        public EntityRoomVariables roomVariables() {
            return room.get().entityState.roomVariables;
        }

        public EntitySlotLifecycle lifecycle() {
            return room.get().lifecycle;
        }

        public RoomEntity[] slots() {
            return room.get().slots;
        }

        public void blockLinkForOwl(int slot) {
            room.get().blockLinkForOwl(slot);
        }

        public void initializeHealthGroup(int slot) {
            room.get().initializeHealthGroup(slot);
        }

        public void presentWitchGotItem(int slot) {
            room.get().presentWitchGotItem(slot);
        }
    }
}
