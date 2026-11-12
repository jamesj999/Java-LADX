package linksawakening.world.entity;

/** Dependencies used by EntityLiftThrowController; resolved lazily to preserve initialization order. */
interface EntityLiftThrowControllerServices {
    boolean beginLift(int slot, int romDirection);

    boolean indoorRoom();

    int clearEntity(int slot);

    int findFreeEntitySlot();

    int initialHealth(int type);

    int spawnLiftableRockSmash(int entityX, int entityY, int sourceSpriteVariant);

    linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers();

    linksawakening.world.entity.EntityBombInteractions bombInteractions();

    linksawakening.world.entity.EntityCombatTables combatTables();

    linksawakening.world.entity.EntityEventChannels events();

    linksawakening.world.entity.EntityLinkState linkState();

    linksawakening.world.entity.EntityMotionSet motionSet();

    linksawakening.world.entity.EntityPhysicsTables physicsTables();

    linksawakening.world.entity.EntityPickupController pickups();

    linksawakening.world.entity.EntityPresentationState presentationState();

    linksawakening.world.entity.EntityProjectileController projectiles();

    linksawakening.world.entity.EntitySlotProperties slotProperties();

    linksawakening.world.entity.GenieEntityController genie();

    linksawakening.world.entity.RoomEntity[] slots();

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

            public linksawakening.world.entity.EntityBombInteractions bombInteractions() {
                return r.bombInteractions;
            }

            public linksawakening.world.entity.EntityCombatTables combatTables() {
                return r.entityState.combatTables;
            }

            public linksawakening.world.entity.EntityEventChannels events() {
                return r.entityState.events;
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

            public linksawakening.world.entity.EntitySlotProperties slotProperties() {
                return r.slotProperties;
            }

            public linksawakening.world.entity.GenieEntityController genie() {
                return r.genie;
            }

            public linksawakening.world.entity.RoomEntity[] slots() {
                return r.slots;
            }

            public void initializeHealthGroup(int slot) {
                r.initializeHealthGroup(slot);
            }
        };
    }
}
