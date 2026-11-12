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
        return new EntityLiftThrowControllerServices() {
            public boolean beginLift(int slot, int romDirection) {
                return r.beginLift(slot, romDirection);
            }

            public boolean indoorRoom() {
                return r.indoorRoom;
            }

            public int clearEntity(int slot) {
                return r.clearEntity(slot);
            }

            public int findFreeEntitySlot() {
                return r.findFreeEntitySlot();
            }

            public int initialHealth(int type) {
                return r.initialHealth(type);
            }

            public int spawnLiftableRockSmash(int entityX, int entityY, int sourceSpriteVariant) {
                return r.spawnLiftableRockSmash(entityX, entityY, sourceSpriteVariant);
            }

            public linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers() {
                return r.spriteHandlers;
            }

            public EntityBombInteractions bombInteractions() {
                return r.bombInteractions;
            }

            public EntityCombatTables combatTables() {
                return r.entityState.combatTables;
            }

            public EntityEventChannels events() {
                return r.entityState.events;
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

            public EntitySlotProperties slotProperties() {
                return r.slotProperties;
            }

            public GenieEntityController genie() {
                return r.genie;
            }

            public RoomEntity[] slots() {
                return r.slots;
            }

            public void initializeHealthGroup(int slot) {
                r.initializeHealthGroup(slot);
            }
        };
    }
}
