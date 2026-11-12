package linksawakening.world.entity;

import linksawakening.world.entity.motion.*;

/** Dependencies used by EntityPlayerSpawns; resolved lazily to preserve initialization order. */
interface EntityPlayerSpawnsServices {
    void setBombFinalPresentationPending(int slot, boolean value);

    boolean beginLift(int slot, int romDirection);

    boolean indoorRoom();

    int findFreeEntitySlot();

    int initialHealth(int type);

    java.util.function.IntSupplier defaultRandomByteSupplier();

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    EntityBombInteractions bombInteractions();

    EntityBossRewards bossRewards();

    EntityCombatTables combatTables();

    EntityEventChannels events();

    EntityLiftThrowController liftThrow();

    EntityMotionRegistry motions();

    EntityPhysicsTables physicsTables();

    EntityPickupController pickups();

    EntityPresentationState presentationState();

    EntityProjectileController projectiles();

    EntityRoomInspection inspection();

    EntityRoomInspection roomInspection();

    EntityRoomVariables roomVariables();

    EntitySlotLifecycle lifecycle();

    EntitySlotProperties slotProperties();

    RoomEntity[] slots();

    linksawakening.world.room.ChestContentsTable chestContentsTable();

    void initializeHealthGroup(int slot);

    static EntityPlayerSpawnsServices bind(RoomEntityRuntime r) {
        return new EntityPlayerSpawnsServices() {
            public void setBombFinalPresentationPending(int slot, boolean value) {
                r.bombInteractions.setBombFinalPresentationPending(slot, value);
            }

            public boolean beginLift(int slot, int romDirection) {
                return r.beginLift(slot, romDirection);
            }

            public boolean indoorRoom() {
                return r.indoorRoom;
            }

            public int findFreeEntitySlot() {
                return r.findFreeEntitySlot();
            }

            public int initialHealth(int type) {
                return r.initialHealth(type);
            }

            public java.util.function.IntSupplier defaultRandomByteSupplier() {
                return r.defaultRandomByteSupplier;
            }

            public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
                return r.spriteHandlers;
            }

            public EntityBombInteractions bombInteractions() {
                return r.bombInteractions;
            }

            public EntityBossRewards bossRewards() {
                return r.bossRewards;
            }

            public EntityCombatTables combatTables() {
                return r.entityState.combatTables;
            }

            public EntityEventChannels events() {
                return r.entityState.events;
            }

            public EntityLiftThrowController liftThrow() {
                return r.liftThrow;
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

            public EntityRoomInspection inspection() {
                return r.inspection();
            }

            public EntityRoomInspection roomInspection() {
                return r.roomInspection;
            }

            public EntityRoomVariables roomVariables() {
                return r.entityState.roomVariables;
            }

            public EntitySlotLifecycle lifecycle() {
                return r.lifecycle;
            }

            public EntitySlotProperties slotProperties() {
                return r.slotProperties;
            }

            public RoomEntity[] slots() {
                return r.slots;
            }

            public linksawakening.world.room.ChestContentsTable chestContentsTable() {
                return r.chestContentsTable;
            }

            public void initializeHealthGroup(int slot) {
                r.initializeHealthGroup(slot);
            }
        };
    }
}
