package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityLiftThrowController; resolved lazily to preserve initialization order. */
interface EntityLiftThrowControllerServices {
    boolean beginLift(int slot, int romDirection);

    boolean indoorRoom();

    int clearEntity(int slot);

    int findFreeEntitySlot();

    int initialHealth(int type);

    int spawnLiftableRockSmash(int entityX, int entityY, int sourceSpriteVariant);

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    EntityBombInteractions bombInteractions();

    EntityCombatTables combatTables();

    EntityEventChannels events();

    EntityLinkState linkState();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntityPickupController pickups();

    EntityPresentationState presentationState();

    EntityProjectileController projectiles();

    EntitySlotProperties slotProperties();

    GenieEntityController genie();

    RoomEntity[] slots();

    void initializeHealthGroup(int slot);

    static EntityLiftThrowControllerServices bind(RoomEntityRuntime r) {
        return new Binding(() -> r);
    }

    final class Binding implements EntityLiftThrowControllerServices {
        private final java.util.function.Supplier<RoomEntityRuntime> room;

        Binding(java.util.function.Supplier<RoomEntityRuntime> room) {
            this.room = room;
        }

        public boolean beginLift(int slot, int romDirection) {
            return room.get().beginLift(slot, romDirection);
        }

        public boolean indoorRoom() {
            return room.get().indoorRoom;
        }

        public int clearEntity(int slot) {
            return room.get().clearEntity(slot);
        }

        public int findFreeEntitySlot() {
            return room.get().findFreeEntitySlot();
        }

        public int initialHealth(int type) {
            return room.get().initialHealth(type);
        }

        public int spawnLiftableRockSmash(int entityX, int entityY, int sourceSpriteVariant) {
            return room.get().spawnLiftableRockSmash(entityX, entityY, sourceSpriteVariant);
        }

        public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
            return room.get().spriteHandlers;
        }

        public EntityBombInteractions bombInteractions() {
            return room.get().bombInteractions;
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

        public EntitySlotProperties slotProperties() {
            return room.get().slotProperties;
        }

        public GenieEntityController genie() {
            return room.get().genie;
        }

        public RoomEntity[] slots() {
            return room.get().slots;
        }

        public void initializeHealthGroup(int slot) {
            room.get().initializeHealthGroup(slot);
        }
    }
}
