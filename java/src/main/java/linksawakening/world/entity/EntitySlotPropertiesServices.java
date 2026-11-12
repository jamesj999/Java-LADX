package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntitySlotProperties; resolved lazily to preserve initialization order. */
interface EntitySlotPropertiesServices {
    boolean indoorRoom();

    int clearEntity(int slot);

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    EntityBossRewards bossRewards();

    EntityCombatTables combatTables();

    EntityEventChannels events();

    EntityLiftThrowController liftThrow();

    EntityLinkState linkState();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntityPresentationState presentationState();

    EntityProjectileController projectiles();

    EntityRoomInspection inspection();

    EntityRoomInspection roomInspection();

    EntityRoomVariables roomVariables();

    RoomEntity[] slots();

    static EntitySlotPropertiesServices bind(RoomEntityRuntime r) {
        return new Binding(() -> r);
    }

    final class Binding implements EntitySlotPropertiesServices {
        private final java.util.function.Supplier<RoomEntityRuntime> room;

        Binding(java.util.function.Supplier<RoomEntityRuntime> room) {
            this.room = room;
        }

        public boolean indoorRoom() {
            return room.get().indoorRoom;
        }

        public int clearEntity(int slot) {
            return room.get().clearEntity(slot);
        }

        public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
            return room.get().spriteHandlers;
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

        public EntityPresentationState presentationState() {
            return room.get().entityState.presentationState;
        }

        public EntityProjectileController projectiles() {
            return room.get().projectiles;
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

        public RoomEntity[] slots() {
            return room.get().slots;
        }
    }
}
